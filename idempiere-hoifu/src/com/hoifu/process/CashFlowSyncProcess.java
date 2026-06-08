package com.hoifu.process;

import org.compiere.process.SvrProcess;

import com.hoifu.service.CashFlowSyncService;

/**
 * 
 * @Description: 现金流量同步Process
 * @author ldh
 * @date 2025年12月9日
 */
@org.adempiere.base.annotation.Process
public class CashFlowSyncProcess extends SvrProcess {
	@Override
	protected void prepare() {
		// 准备参数
	}

	@Override
	protected String doIt() throws Exception {
		// 执行现金流量同步逻辑
		CashFlowSyncService syncService = new CashFlowSyncService();
		syncService.syncCashFlowData();
		return "现金流量同步完成";
	}
}