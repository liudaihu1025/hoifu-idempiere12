package com.hoifu.model;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.compiere.model.Query;
import org.compiere.util.DB;

/**
 * 打样评审单 Model 类
 * 
 * <p>
 * 重写 {@link #afterSave} 实现以下联动逻辑：
 * <ul>
 * <li>评审状态变更为"已终审"（FA）时，同步更新对应任务（dy_samplingtask）的 TaskStatus 为"已完成"（CO）</li>
 * <li>工艺/打样评审单（主表有 dy_samplingtask_id）：直接更新该任务状态</li>
 * <li>设计评审单（主表无 dy_samplingtask_id）：从明细获取所有关联任务 ID，逐个更新状态</li>
 * </ul>
 * 
 * <p>
 * 配置：在 iDempiere 中将 {@code dy_samplingreview} 表的 {@code AD_Table.ClassName} 设置为
 * {@code com.hoifu.model.MDYSamplingReview}
 */
public class MDYSamplingReview extends X_dy_samplingreview {

	private static final long serialVersionUID = 1L;

	public MDYSamplingReview(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public MDYSamplingReview(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		// 只在 reviewstatus 变化时触发，避免其他字段保存时重复执行
		if (!is_ValueChanged("reviewstatus"))
			return true;

		// 只有评审单整体终审（FA）时，才同步更新任务状态
		if (!"FA".equals(getreviewstatus()))
			return true;

		// ── 获取主表关联的任务 ID ──────────────────────────────────────
		// 工艺/打样评审单：主表有 dy_samplingtask_id，直接获取
		// 设计评审单：主表无 dy_samplingtask_id（值为 0），需从明细获取
		int taskId = get_ValueAsInt("dy_samplingtask_id");

		if (taskId > 0) {
			// 工艺/打样评审单：主表直接关联任务，更新该任务状态为已终审（FA）
			DB.executeUpdateEx("UPDATE dy_samplingtask SET taskstatus='FA', updated=now(), updatedby=? "
					+ "WHERE dy_samplingtask_id=?", new Object[] { getUpdatedBy(), taskId }, get_TrxName());
		} else {
			// 设计评审单：主表无任务关联，从明细的 dy_samplingtask_id 获取所有关联任务
			// 一张设计评审单可能包含多个任务的明细（多任务合并评审），需逐个更新
			List<X_dy_samplingreviewline> lines = new Query(getCtx(), X_dy_samplingreviewline.Table_Name,
					"dy_samplingreview_id=? AND isactive='Y' AND dy_samplingtask_id > 0", get_TrxName())
					.setParameters(get_ID()).list();

			// 收集所有不重复的任务 ID（同一任务可能对应多条明细）
			Set<Integer> taskIds = new HashSet<>();
			for (X_dy_samplingreviewline line : lines) {
				int lineTaskId = line.getdy_samplingtask_ID();
				if (lineTaskId > 0)
					taskIds.add(lineTaskId);
			}

			// 逐个更新设计任务状态为已终审（FA）
			for (int tid : taskIds) {
				DB.executeUpdateEx("UPDATE dy_samplingtask SET taskstatus='FA', updated=now(), updatedby=? "
						+ "WHERE dy_samplingtask_id=?", new Object[] { getUpdatedBy(), tid }, get_TrxName());
			}
		}

		return true;
	}
}
