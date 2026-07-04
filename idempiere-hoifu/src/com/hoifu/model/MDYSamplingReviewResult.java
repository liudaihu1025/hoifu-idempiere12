package com.hoifu.model;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.compiere.model.MUser;
import org.compiere.util.DB;

/**
 * 打样评审结果 Model 类
 * 
 * <p>
 * 重写 {@link #afterSave} 实现以下通用联动逻辑：
 * <ol>
 * <li>同一评分人对同一明细多次打分，只保留最新一条有效记录（旧记录设为 isactive='N'）</li>
 * <li>拼接评审意见（留痕，最新在最上面；格式：更新时间_参与人\n评审意见内容）； 无论新建还是修改，均追加，保留所有历史意见</li>
 * </ol>
 * 
 * <p>
 * 角色相关的业务逻辑（总监终审状态更新、参与人平均分计算）由流程类 {@code SamplingReviewProcess} 负责。
 */
public class MDYSamplingReviewResult extends X_dy_samplingreviewresult {

	private static final long serialVersionUID = 1L;

	public MDYSamplingReviewResult(Properties ctx, int dy_samplingreviewresult_id, String trxName) {
		super(ctx, dy_samplingreviewresult_id, trxName);
	}

	public MDYSamplingReviewResult(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		// ==================== 1. 获取关联评审明细 ====================
		int lineID = get_ValueAsInt("dy_samplingreviewline_id");
		if (lineID <= 0)
			return success;

		MDYSamplingReviewLine line = new MDYSamplingReviewLine(getCtx(), lineID, get_TrxName());
		if (line.get_ID() <= 0)
			return success;

		// ==================== 2. 新建时，将该评分人对该明细的所有旧记录设为 isactive='N'
		// ====================
		// 同一评分人对同一明细多次打分，只保留最新一条有效记录
		if (newRecord) {
			DB.executeUpdateEx(
					"UPDATE dy_samplingreviewresult SET isactive='N' "
							+ "WHERE dy_samplingreviewline_id=? AND ad_user_id=? AND isactive='Y' "
							+ "AND dy_samplingreviewresult_id<>?",
					new Object[] { lineID, getAD_User_ID(), get_ID() }, get_TrxName());
		}

		// ==================== 3. 拼接评审意见（留痕，最新在最上面）====================
		// 无论新建还是修改，均追加一条新记录，保留所有历史意见
		String comment = getreviewcomment();
		if (comment == null || comment.trim().isEmpty())
			return success; // 总监不输入意见，跳过拼接

		String reviewer = "";
		MUser user = MUser.get(getCtx(), getAD_User_ID());
		if (user != null)
			reviewer = user.getName();
		String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
		String newEntry = dateStr + "_" + reviewer + "\n" + comment;
		String existing = line.getreviewcomment();
		String merged = (existing == null || existing.trim().isEmpty()) ? newEntry : newEntry + "\n\n" + existing;
		line.setreviewcomment(merged);

		// ==================== 4. 保存评审明细（只更新 reviewcomment）====================
		line.saveEx();
		return success;
	}
}