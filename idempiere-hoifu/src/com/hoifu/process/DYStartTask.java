package com.hoifu.process;

import org.adempiere.base.annotation.Process;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@Process
public class DYStartTask extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		if (recordId <= 0)
			throw new Exception("未找到记录");

		String status = DB.getSQLValueString(get_TrxName(),
				"SELECT TaskStatus FROM DY_SamplingTask WHERE DY_SamplingTask_ID=?", recordId);
		if (status != null && !"".equals(status))
			throw new Exception("任务已开始，不能重复操作");

		DB.executeUpdateEx(
				"UPDATE DY_SamplingTask " + "SET ActualStartDate=now(), TaskStatus='IP', Updated=now(), UpdatedBy=? "
						+ "WHERE DY_SamplingTask_ID=?",
				new Object[] { getAD_User_ID(), recordId }, get_TrxName());

		// 第一个任务开始时，自动更新阶段的实际开始时间
		int phaseId = DB.getSQLValue(get_TrxName(),
				"SELECT DY_SamplingPhase_ID FROM DY_SamplingTask WHERE DY_SamplingTask_ID=?", recordId);

		if (phaseId > 0) {
			DB.executeUpdateEx(
					"UPDATE DY_SamplingPhase " + "SET ActualStartDate=now(), PhaseStatus='IP', "
							+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingPhase_ID=? "
							+ "  AND ActualStartDate IS NULL", // 只在第一个任务开始时更新
					new Object[] { getAD_User_ID(), phaseId }, get_TrxName());
		}

		return "任务已开始";
	}
}