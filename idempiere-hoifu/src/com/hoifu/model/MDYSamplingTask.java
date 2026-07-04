package com.hoifu.model;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.compiere.util.DB;

public class MDYSamplingTask extends X_dy_samplingtask {

	public MDYSamplingTask(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public MDYSamplingTask(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}


	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		// ── 1. 更新阶段效果数量 ──────────────────────────────────────
		// 注意：isactive 变化（效果合并）不触发此逻辑，合并不改变效果总数
		if (newRecord || is_ValueChanged("QtyDesign")) {
			int phaseId = getdy_samplingphase_ID();
			DB.executeUpdateEx("UPDATE DY_SamplingPhase "
					+ "SET QtyDesign = (SELECT COALESCE(SUM(t.QtyDesign),0) FROM DY_SamplingTask t "
					+ "                 WHERE t.DY_SamplingPhase_ID=? AND t.IsActive='Y') "
					+ "WHERE DY_SamplingPhase_ID=?", new Object[] { phaseId, phaseId }, get_TrxName());
		}

		// ── 2. 效果合并/取消合并联动 ──────────────────────────────────
		// 当 selectedvalue 变化时，联动更新其他工艺任务的 isactive
		if (!newRecord && is_ValueChanged("selectedvalue")) {
			String newVal = (String) get_Value("selectedvalue");
			String oldVal = (String) get_ValueOld("selectedvalue");

			Set<String> newIds = parseCSV(newVal);
			Set<String> oldIds = parseCSV(oldVal);

			int phaseId = getdy_samplingphase_ID();
			int currentId = get_ID();

			// 新增的评审明细 ID → 将对应的其他工艺任务设为无效（合并）
			Set<String> addedIds = new HashSet<>(newIds);
			addedIds.removeAll(oldIds);
			for (String rlId : addedIds) {
				// 被合并的任务：isactive='N'，QtyDesign -1
				int affected = DB.executeUpdateEx(
						"UPDATE dy_samplingtask "
								+ "SET isactive='N', QtyDesign=QtyDesign-1, updated=now(), updatedby=? "
								+ "WHERE dy_samplingphase_id=? AND dy_samplingtask_id!=? " + "  AND isactive='Y' "
								+ "  AND ?=ANY(string_to_array(selectedvalue,','))",
						new Object[] { getUpdatedBy(), phaseId, currentId, rlId }, get_TrxName());

				// 当前任务（合并方）：每合并一个效果 QtyDesign +1
				if (affected > 0) {
					DB.executeUpdateEx(
							"UPDATE dy_samplingtask SET QtyDesign=QtyDesign+1, updated=now(), updatedby=? "
									+ "WHERE dy_samplingtask_id=?",
							new Object[] { getUpdatedBy(), currentId }, get_TrxName());
				}
			}

			// 移除的评审明细 ID → 恢复对应的工艺任务为有效（取消合并）
			Set<String> removedIds = new HashSet<>(oldIds);
			removedIds.removeAll(newIds);
			for (String rlId : removedIds) {
				// 被恢复的任务：isactive='Y'，QtyDesign +1
				int affected = DB.executeUpdateEx(
						"UPDATE dy_samplingtask "
								+ "SET isactive='Y', QtyDesign=QtyDesign+1, updated=now(), updatedby=? "
								+ "WHERE dy_samplingphase_id=? AND dy_samplingtask_id!=? " + "  AND isactive='N' "
								+ "  AND ?=ANY(string_to_array(selectedvalue,','))",
						new Object[] { getUpdatedBy(), phaseId, currentId, rlId }, get_TrxName());

				// 当前任务（合并方）：每取消合并一个效果 QtyDesign -1
				if (affected > 0) {
					DB.executeUpdateEx(
							"UPDATE dy_samplingtask SET QtyDesign=QtyDesign-1, updated=now(), updatedby=? "
									+ "WHERE dy_samplingtask_id=?",
							new Object[] { getUpdatedBy(), currentId }, get_TrxName());
				}
			}
		}

		return true;
	}

	/** 解析逗号分隔的字符串为 Set */
	private Set<String> parseCSV(String csv) {
		Set<String> result = new HashSet<>();
		if (csv == null || csv.trim().isEmpty())
			return result;
		for (String s : csv.split(",")) {
			s = s.trim();
			if (!s.isEmpty())
				result.add(s);
		}
		return result;
	}
}