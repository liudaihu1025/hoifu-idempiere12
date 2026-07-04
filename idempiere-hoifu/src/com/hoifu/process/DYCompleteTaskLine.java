package com.hoifu.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MAttachment;
import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@Process
public class DYCompleteTaskLine extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		if (recordId <= 0)
			throw new Exception("未找到记录");

		// ── 1. 完成前检查：必须有附件 ────────────────────────────────
		int tableId = MTable.getTable_ID("DY_SamplingTaskLine");
		MAttachment attachment = MAttachment.get(getCtx(), tableId, recordId);
		if (attachment == null || attachment.getEntryCount() == 0)
			throw new AdempiereUserError("请先上传附件，再完成事项");

		// ── 2. 更新事项状态为 CO ──────────────────────────────────────
		DB.executeUpdateEx(
				"UPDATE DY_SamplingTaskLine " + "SET itemstatus='CO', " + "    ActualEndDate=now(), "
						+ "    TotalHours=CEILING(EXTRACT(EPOCH FROM (now() - ActualStartDate))/3600), "
						+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingTaskLine_ID=?",
				new Object[] { getAD_User_ID(), recordId }, get_TrxName());

		// ── 3. 获取所属任务 ID ────────────────────────────────────────
		int taskId = DB.getSQLValueEx(get_TrxName(),
				"SELECT DY_SamplingTask_ID FROM DY_SamplingTaskLine WHERE DY_SamplingTaskLine_ID=?", recordId);

		// ── 4. 检查该任务下是否还有未完成的事项 ──────────────────────
		int unfinishedLines = DB.getSQLValue(get_TrxName(), "SELECT COUNT(*) FROM DY_SamplingTaskLine "
				+ "WHERE DY_SamplingTask_ID=? AND IsActive='Y' " + "  AND (itemstatus IS NULL OR itemstatus <> 'CO')",
				taskId);

		if (unfinishedLines == 0) {
			// ── 5. 所有事项完成，自动完成任务 ────────────────────────
			DB.executeUpdateEx(
					"UPDATE DY_SamplingTask " + "SET TaskStatus='CO', " + "    ActualEndDate=now(), "
							+ "    TotalHours=CEILING(EXTRACT(EPOCH FROM (now() - ActualStartDate))/3600), "
							+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingTask_ID=?",
					new Object[] { getAD_User_ID(), taskId }, get_TrxName());

			// ── 6. 获取所属阶段 ID ────────────────────────────────────
			int phaseId = DB.getSQLValue(get_TrxName(),
					"SELECT DY_SamplingPhase_ID FROM DY_SamplingTask WHERE DY_SamplingTask_ID=?", taskId);

			// ── 7. 检查该阶段下是否还有未完成的任务 ──────────────────
			int unfinishedTasks = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM DY_SamplingTask " + "WHERE DY_SamplingPhase_ID=? AND IsActive='Y' "
							+ "  AND (TaskStatus IS NULL OR TaskStatus <> 'CO')",
					phaseId);

			// ── 8. 所有任务完成，自动完成阶段 ────────────────────────
			if (unfinishedTasks == 0) {
				DB.executeUpdateEx(
						"UPDATE DY_SamplingPhase " + "SET ActualEndDate=now(), PhaseStatus='CO', "
								+ "    TotalHours=CEILING(EXTRACT(EPOCH FROM (now() - ActualStartDate))/3600), "
								+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingPhase_ID=?",
						new Object[] { getAD_User_ID(), phaseId }, get_TrxName());
			}
		}

		return "事项已完成";
	}
}