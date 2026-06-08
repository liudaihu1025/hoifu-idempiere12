package org.libero.process;

import org.compiere.process.DocAction;
import org.compiere.process.SvrProcess;
import org.compiere.wf.MWorkflow;

@org.adempiere.base.annotation.Process
public class AD_Workflow_RequestChange extends SvrProcess {

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

		// 验证工作流状态 - 支持已发布和已完成状态
		if (!"R".equals(workflow.getPublishStatus()) && !DocAction.STATUS_Completed.equals(workflow.getDocStatus())) {
			return "只有已发布或已完成的工作流才能申请变更";
		}

		// 强制解锁到草稿状态
		String result = forceUnlockToDraft(workflow);
		if (!result.contains("成功")) {
			return "解锁失败: " + result;
		}
		// 更新工作流验证状态
		workflow.setIsValid(false);
		// 更新工作流状态为"变更中"
		workflow.set_ValueOfColumn("PublishStatus", "U");
		workflow.set_ValueOfColumn("ProcessStatus", "C");
		workflow.saveEx();

		return "变更申请成功";
	}

	private String forceUnlockToDraft(MWorkflow workflow) throws Exception {
		workflow.setDocStatus(DocAction.STATUS_Drafted);
		workflow.setProcessed(false);
		workflow.setDocAction(DocAction.ACTION_Complete);
		workflow.saveEx();
		return "强制解锁成功，状态: " + workflow.getDocStatus();
	}
}