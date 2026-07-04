package com.hoifu.process;

import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class DYPublishSamplingRequest extends SvrProcess {

	@Override
	protected void prepare() {
		// 无参数
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		if (recordId <= 0)
			throw new IllegalArgumentException("No Record_ID");

		// 校验当前状态
		String status = DB.getSQLValueString(get_TrxName(),
				"SELECT SamplingStatus FROM DY_SamplingRequest WHERE DY_SamplingRequest_ID=?", recordId);
		if (!"DR".equals(status))
			throw new Exception("只有草稿状态才能发布");

		// 更新状态为已发布
		int updated = DB
				.executeUpdateEx(
						"UPDATE DY_SamplingRequest SET SamplingStatus='PB', Updated=now(), UpdatedBy=? "
								+ "WHERE DY_SamplingRequest_ID=?",
						new Object[] { getAD_User_ID(), recordId }, get_TrxName());

		if (updated != 1)
			throw new Exception("更新失败");

		return "需求已发布";
	}
}