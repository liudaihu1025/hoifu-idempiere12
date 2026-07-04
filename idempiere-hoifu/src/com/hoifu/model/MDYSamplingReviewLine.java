package com.hoifu.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

/**
 * 打样评审明细 Model 类
 * 
 * <p>
 * 重写 {@link #afterSave} 实现以下联动逻辑：
 * <ul>
 * <li>评审状态变更时，同步更新对应任务（dy_samplingtask）的 TaskStatus</li>
 * <li>所有明细评审状态统一达成"已评审"（RD）→ 更新主表评审状态为 RD</li>
 * <li>所有明细评审状态统一达成"已终审"（FA）→ 更新主表评审状态为 FA</li>
 * </ul>
 * 
 * <p>
 * 审核人和审核时间由 {@code FinalCommentProcess} 负责更新，不在此处处理。
 * 
 * <p>
 * 配置：在 iDempiere 中将 {@code dy_samplingreviewline} 表的 {@code AD_Table.ClassName}
 * 设置为 {@code com.hoifu.model.MDYSamplingReviewLine}
 */
public class MDYSamplingReviewLine extends X_dy_samplingreviewline {

	private static final long serialVersionUID = 1L;

	public MDYSamplingReviewLine(Properties ctx, int dy_samplingreviewline_id, String trxName) {
		super(ctx, dy_samplingreviewline_id, trxName);
	}

	public MDYSamplingReviewLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		// 保存失败或新建记录时直接返回
		// 新建时初始状态为"评审中"（RV），不可能满足"统一达成"条件，跳过可避免批量新建时的无效查询
		if (!success || newRecord)
			return success;

		// 只有 reviewstatus 字段发生变化时才需要检查，其他字段更新时直接跳过
		if (!is_ValueChanged("reviewstatus"))
			return success;

		// ==================== 1. 获取主表 ID ====================
		int reviewID = getdy_samplingreview_ID();
		if (reviewID <= 0)
			return success;

		// ==================== 2. 查询该评审单下所有明细的评审状态 ====================
		List<MDYSamplingReviewLine> lines = new Query(getCtx(), Table_Name,
				"dy_samplingreview_id=? AND reviewstatus <> 'CL'", get_TrxName()).setParameters(reviewID)
				.setOnlyActiveRecords(true).list();

		if (lines.isEmpty())
			return success;

		// ==================== 3. 判断是否统一达成同一状态 ====================
		boolean allRD = lines.stream().allMatch(l -> X_dy_samplingreview.REVIEWSTATUS_已评审.equals(l.getreviewstatus()));
		boolean allFA = lines.stream().allMatch(l -> X_dy_samplingreview.REVIEWSTATUS_已终审.equals(l.getreviewstatus()));

		if (!allRD && !allFA)
			return success; // 尚未统一达成，不更新主表

		// ==================== 4. 加载主表并更新评审状态 ====================
		MDYSamplingReview review = new MDYSamplingReview(getCtx(), reviewID, get_TrxName());

		if (allFA) {
			// 统一达成"已终审"：只更新评审状态
			// 审核人和审核时间由 FinalCommentProcess 负责更新
			review.setreviewstatus(X_dy_samplingreview.REVIEWSTATUS_已终审);
		} else {
			// 统一达成"已评审"：只更新评审状态
			review.setreviewstatus(X_dy_samplingreview.REVIEWSTATUS_已评审);
		}

		review.saveEx();
		return success;
	}
}