package org.libero.process;

import org.adempiere.util.Callback;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.wf.MWorkflow;

@org.adempiere.base.annotation.Process
public class AD_Workflow_SubmitForApproval extends SvrProcess {

	protected void prepare() {
		int recordId = getRecord_ID();
		if (recordId <= 0) {
			throw new IllegalArgumentException("No record ID");
		}
		if (getProcessInfo() != null) {
			getProcessInfo().setShowHelp("N");
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

		// 验证工作流校验状态
		if (!workflow.isValid()) {
			return "请先验证工作流";
		}
		
		// 验证工作流状态
		if (!"U".equals(workflow.getPublishStatus())) {
			return "只有修订中的工作流才能提交审批";
		}

		// 构建确认信息
		StringBuilder message = new StringBuilder();
		message.append("工作流: ").append(workflow.getName()).append("\n");
		message.append("版本: ").append(workflow.getVersion()).append("\n");
		message.append("请重点核对工作流节点和流程，提交后工作流将锁定，不可编辑\n");
		message.append("确定要提交审批吗？");

		// 使用processUI显示确认对话框
		final StringBuffer userResponse = new StringBuffer();

		if (processUI != null) {
			processUI.ask(message.toString(), new Callback<Boolean>() {
				@Override
				public void onCallback(Boolean result) {
					if (result != null && result) {
						userResponse.append("CONFIRMED");
					} else {
						userResponse.append("CANCELLED");
					}
				}
			});

			while (userResponse.length() == 0) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					return "操作已取消";
				}
			}

			if (!"CONFIRMED".equals(userResponse.toString())) {
				return "操作已取消";
			}
		} else {
			addLog(0, null, null, "无UI界面，直接提交审批");
		}

		// 验证工作流结构
		if (!validateWorkflowStructure(workflow)) {
			return "工作流结构验证失败";
		}

		// 检查DocAction列是否存在
		MTable table = MTable.get(Env.getCtx(), MWorkflow.Table_ID);
		MColumn docActionColumn = table.getColumn("DocAction");
		if (docActionColumn == null) {
			throw new Exception("AD_Workflow表缺少DocAction列");
		}

		// 设置状态为待审核
		workflow.set_ValueOfColumn("PublishStatus", "P");

		// 运行工作流 - 使用Prepare动作
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(workflow, DocAction.ACTION_Prepare);
		if (pi.isError()) {
			handleRejection(workflow);
			return pi.getSummary();
		}

		workflow.setProcessed(true);
		workflow.saveEx();

		// 记录提交信息
		int userId = Env.getAD_User_ID(getCtx());
		workflow.set_ValueOfColumn("SubmittedBy", userId);
		workflow.saveEx();

		sendNotificationToApprovers(workflow);

		return "提交成功";
	}

	private boolean validateWorkflowStructure(MWorkflow workflow) {
		// 检查工作流是否有节点
		if (workflow.getNodes(false, workflow.getAD_Client_ID()) == null
				|| workflow.getNodes(false, workflow.getAD_Client_ID()).length == 0) {
			return false;
		}
		return true;
	}

	private void sendNotificationToApprovers(MWorkflow workflow) {
		addLog(0, null, null, "已发送通知给审核人");
	}

	private void handleRejection(MWorkflow workflow) {
		try {
			workflow.set_ValueOfColumn("PublishStatus", "U");
			workflow.set_ValueOfColumn("ProcessStatus", "");
			workflow.setDocStatus(DocAction.STATUS_Drafted);
			workflow.setDocAction(DocAction.ACTION_Complete);
			workflow.saveEx();
			addLog(0, null, null, "审核失败，状态已回退");
		} catch (Exception e) {
			addLog(0, null, null, "状态回退失败: " + e.getMessage());
		}
	}
}