package org.libero.process;

import org.compiere.process.SvrProcess;
import org.compiere.wf.MWorkflow;

@org.adempiere.base.annotation.Process
public class AD_Workflow_CancelChange extends SvrProcess {

	@Override
	protected void prepare() {
		int recordId = getRecord_ID();
		if (recordId <= 0) {
			throw new IllegalArgumentException("No record ID");
		}
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();

		// 获取工作流对象
		MWorkflow workflow = new MWorkflow(getCtx(), recordId, get_TrxName());
		if (workflow.get_ID() == 0) {
			throw new IllegalArgumentException("Workflow not found");
		}

		// 验证工作流状态 - 应该是申请变更后的状态
		if (!"U".equals(workflow.getPublishStatus()) || !"DR".equals(workflow.getDocStatus())) {
			return "只有修订中且草稿状态的工作流才能取消变更";
		}

		// 设置取消变更标志，触发校验器处理
		workflow.set_ValueOfColumn("cancelchange", "Y");
		// 更新工作流验证状态
		workflow.setIsValid(true);
		workflow.saveEx();

		return "工艺路线回滚到历史最新版本";
	}
}