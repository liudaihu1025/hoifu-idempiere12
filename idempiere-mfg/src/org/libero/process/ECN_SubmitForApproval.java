package org.libero.process;

import org.adempiere.util.Callback;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPOrder;
import org.libero.model.MPP_Engineering_Change_Notice;

@org.adempiere.base.annotation.Process
public class ECN_SubmitForApproval extends SvrProcess {

	@Override
	protected void prepare() {
		// 标准准备逻辑
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		MPP_Engineering_Change_Notice ecn = new MPP_Engineering_Change_Notice(getCtx(), recordId, get_TrxName());


		// 校验工艺路线是否已验证
		Object isVerifiedObj = ecn.get_Value("IsWorkflowVerified");
		boolean isVerified = false;

		if (isVerifiedObj instanceof Boolean) {
			isVerified = ((Boolean) isVerifiedObj).booleanValue();
		} else if (isVerifiedObj instanceof String) {
			isVerified = "Y".equals(isVerifiedObj);
		}

		if (!isVerified) {
			return "请先验证工艺路线后再提交审核";
		}

		// 用户确认对话框
		if (!confirmSubmission(ecn)) {
			return "操作已取消";
		}

		// 更新工单状态为ECN变更中
		MPPOrder order = new MPPOrder(getCtx(), ecn.getPP_Order_ID(), get_TrxName());
		
		String orderStatus = (String) order.get_Value("Orderstatus");
		if (!"Released".equals(orderStatus) && !"Started".equals(orderStatus) && !"InProgress".equals(orderStatus)) {
		    return "只有【已发布】、【已开工】或【执行中】状态的工单才能提交审核";
		}

		order.set_ValueOfColumn("Orderstatus", "InECNChange");
		order.saveEx();

		
		// 更新BOM和工艺路线状态
		//updateBOMAndWorkflowStatus(ecn);

		// 设置状态为待审核
		ecn.setECNStatus(MPP_Engineering_Change_Notice.ECNSTATUS_PendingApproval);
		ecn.setDocStatus(DocAction.STATUS_Drafted);
		// 保存原始工单状态
		ecn.set_ValueOfColumn("OriginalOrderStatus", orderStatus);


		// 启动工作流
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(ecn, DocAction.ACTION_Prepare);
		if (pi.isError()) {
			ecn.setECNStatus(MPP_Engineering_Change_Notice.ECNSTATUS_Draft);
			ecn.saveEx();
			return pi.getSummary();
		}
		// 关键：查询并保存工作流进程ID
		int wfProcessId = DB.getSQLValue(get_TrxName(),
				"SELECT AD_WF_Process_ID FROM AD_WF_Process WHERE AD_Table_ID=? AND Record_ID=? ORDER BY AD_WF_Process_ID DESC",
				ecn.get_Table_ID(), ecn.get_ID());

		if (wfProcessId > 0) {
			ecn.set_ValueOfColumn("AD_WF_Process_ID", wfProcessId);
		}

		ecn.saveEx();

		return "提交审核成功";
	}

	private boolean confirmSubmission(MPP_Engineering_Change_Notice ecn) {
		if (processUI == null)
			return true;

		StringBuilder message = new StringBuilder();
		message.append("ECN变更单: ").append(ecn.getDocumentNo()).append("\n");
		message.append("名称: ").append(ecn.getName()).append("\n");
		message.append("提交后将进入审批流程，不可编辑\n");
		message.append("确定要提交审核吗？");

		final StringBuffer userResponse = new StringBuffer();

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
				return false;
			}
		}

		return "CONFIRMED".equals(userResponse.toString());
	}

	private void updateBOMAndWorkflowStatus(MPP_Engineering_Change_Notice ecn) throws Exception {
		// 获取ECN关联的工单
		if (ecn.getPP_Order_ID() <= 0) {
			return; // 没有关联工单，跳过
		}

		MPPOrder order = new MPPOrder(getCtx(), ecn.getPP_Order_ID(), get_TrxName());


		// 更新产品的最新已发布BOM状态
		updateLatestBOMStatus(order);

		// 更新对应的最新工艺路线状态
		updateLatestWorkflowStatus(order);
	}

	private void updateLatestBOMStatus(MPPOrder order) {  
	    int bomId = order.getPP_Product_BOM_ID();  
	    if (bomId > 0) {  
			String sql = "UPDATE PP_Product_BOM SET bomstatus = 'InECNChange' WHERE PP_Product_BOM_ID = ? AND BOMStatus = 'Released' AND PP_Product_BOM.BOMType='A'";
	        DB.executeUpdate(sql, new Object[] { bomId }, false, get_TrxName());  
	        addLog(0, null, null, "已更新BOM状态为ECN变更中");  
	    }  
	}

	/**  
	 * 更新对应的最新工艺路线状态为InECNChange  
	 */  
	private void updateLatestWorkflowStatus(MPPOrder order) {
		int workflowId = order.getAD_Workflow_ID();
		if (workflowId > 0) {
			String sql = "UPDATE AD_Workflow SET PublishStatus = 'C' WHERE AD_Workflow_ID = ? AND PublishStatus = 'R' AND AD_Workflow.VersionStatus='C'";
			DB.executeUpdate(sql, new Object[] { workflowId }, false, get_TrxName());
			addLog(0, null, null, "已更新工艺路线状态为ECN变更中");
		}
	}

	private void saveOriginalOrderStatus(MPP_Engineering_Change_Notice ecn) {
		if (ecn.getPP_Order_ID() > 0) {
			MPPOrder order = new MPPOrder(ecn.getCtx(), ecn.getPP_Order_ID(), ecn.get_TrxName());
			String originalStatus = (String) order.get_Value("Orderstatus");
			ecn.set_ValueOfColumn("OriginalOrderStatus", originalStatus);
			ecn.saveEx();
			addLog(0, null, null, "已保存工单原始状态: " + order.getDocumentNo() + " -> " + originalStatus);
		}
	}

}
