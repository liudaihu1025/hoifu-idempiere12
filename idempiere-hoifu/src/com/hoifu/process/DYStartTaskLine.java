package com.hoifu.process;

import org.adempiere.base.annotation.Process;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@Process
public class DYStartTaskLine extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		if (recordId <= 0)
			throw new Exception("未找到记录");

		// 检查事项是否已开始
		String status = DB.getSQLValueString(get_TrxName(),
				"SELECT itemstatus FROM DY_SamplingTaskLine WHERE DY_SamplingTaskLine_ID=?", recordId);
		if (status != null && !"".equals(status))
			throw new Exception("事项已开始，不能重复操作");

		// 更新事项状态
		DB.executeUpdateEx("UPDATE DY_SamplingTaskLine "
				+ "SET ActualStartDate=now(), itemstatus='IP', Updated=now(), UpdatedBy=? "
				+ "WHERE DY_SamplingTaskLine_ID=?", new Object[] { getAD_User_ID(), recordId }, get_TrxName());

		// 第一个事项开始时，自动更新任务的实际开始时间，并更新任务状态为'进行中'
		int taskId = DB.getSQLValue(get_TrxName(),
				"SELECT DY_SamplingTask_ID FROM DY_SamplingTaskLine WHERE DY_SamplingTaskLine_ID=?", recordId);

		if (taskId > 0) {
			DB.executeUpdateEx(
					"UPDATE DY_SamplingTask " + "SET ActualStartDate=now(), TaskStatus='IP', "
							+ "    Updated=now(), UpdatedBy=? " + "WHERE DY_SamplingTask_ID=? "
							+ "  AND ActualStartDate IS NULL", // 只在第一个事项开始时更新
					new Object[] { getAD_User_ID(), taskId }, get_TrxName());
		}

		return "事项已开始";
	}
}