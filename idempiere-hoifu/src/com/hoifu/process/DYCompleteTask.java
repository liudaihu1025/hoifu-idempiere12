package com.hoifu.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MAttachment;
import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@Process
public class DYCompleteTask extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		if (recordId <= 0)
			throw new Exception("未找到记录");

		// 完成前检查：必须有附件
		int tableId = MTable.getTable_ID("DY_SamplingTask");
		MAttachment attachment = MAttachment.get(getCtx(), tableId, recordId);
		if (attachment == null || attachment.getEntryCount() == 0)
			throw new AdempiereUserError("完成任务前必须上传附件");

		// 兜底检查
		String status = DB.getSQLValueString(get_TrxName(),
				"SELECT TaskStatus FROM DY_SamplingTask WHERE DY_SamplingTask_ID=?", recordId);
		if (!"IP".equals(status))
			throw new Exception("只有进行中的任务才能完成");

		DB.executeUpdateEx(
				"UPDATE DY_SamplingTask " + "SET ActualEndDate=now(), " + "    TaskStatus='CO', "
						+ "    TotalHours=CEILING(EXTRACT(EPOCH FROM (now() - ActualStartDate))/3600), "
						+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingTask_ID=?",
				new Object[] { getAD_User_ID(), recordId }, get_TrxName());

		// 任务全部完成后，自动更新阶段完成日期
		// 查询所属阶段 ID
		int phaseId = DB.getSQLValue(get_TrxName(),
				"SELECT DY_SamplingPhase_ID FROM DY_SamplingTask WHERE DY_SamplingTask_ID=?", recordId);

		if (phaseId > 0) {
			// 查询该阶段下未完成的任务数量（当前任务已更新为 CO，不会被计入）
			int unfinished = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM DY_SamplingTask " + "WHERE DY_SamplingPhase_ID=? AND IsActive='Y' "
							+ "  AND (TaskStatus IS NULL OR TaskStatus <> 'CO')",
					phaseId);

			// 所有任务都完成了，自动完成阶段
			if (unfinished == 0) {
				DB.executeUpdateEx(
						"UPDATE DY_SamplingPhase " + "SET ActualEndDate=now(), PhaseStatus='CO', "
								+ "    TotalHours=CEILING(EXTRACT(EPOCH FROM (now() - ActualStartDate))/3600), "
								+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingPhase_ID=?",
						new Object[] { getAD_User_ID(), phaseId }, get_TrxName());
			}
		}

		return "任务已完成";
	}
}