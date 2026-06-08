package org.libero.process;

import org.compiere.process.DocAction;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.wf.MWFProcess;
import org.libero.model.MPPOrder;
import org.libero.model.MPP_Engineering_Change_Notice;

@org.adempiere.base.annotation.Process
public class ECN_CancelApproval extends SvrProcess {

	@Override
	protected void prepare() {
		// 标准准备逻辑
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		MPP_Engineering_Change_Notice ecn = new MPP_Engineering_Change_Notice(getCtx(), recordId, get_TrxName());

		String currentStatus = ecn.getECNStatus();
		if (!MPP_Engineering_Change_Notice.ECNSTATUS_PendingApproval.equals(currentStatus)
				&& !MPP_Engineering_Change_Notice.ECNSTATUS_Approved.equals(currentStatus)) {
			return "只有待审核或已批准状态的ECN才能取消审核";
		}

		// 取消工作流进程和活动
		cancelWorkflowProcess(ecn);

		rollbackOrderStatusForCancellation(ecn);
		// 复原BOM和工艺路线状态
		//restoreBOMAndWorkflowStatus(ecn);

		// 更新ECN状态
		ecn.setECNStatus(MPP_Engineering_Change_Notice.ECNSTATUS_Draft);
		ecn.setDocStatus(DocAction.STATUS_Drafted);
		ecn.setIsApproved(false);
		ecn.saveEx(); // 不设置AD_WF_Process_ID字段

		return "取消审核成功";
	}

	/**
	 * ECN取消时回滚工单状态到原始状态
	 */
	private void rollbackOrderStatusForCancellation(MPP_Engineering_Change_Notice ecn) {

		if (ecn.getPP_Order_ID() > 0) {
			// 重新加载ECN对象确保获取最新数据
			MPP_Engineering_Change_Notice freshEcn = new MPP_Engineering_Change_Notice(ecn.getCtx(), ecn.get_ID(),
					ecn.get_TrxName());

			String originalStatus = (String) freshEcn.get_Value("OriginalOrderStatus");
			log.info("从数据库读取的原始状态: " + originalStatus);

			if (originalStatus == null || originalStatus.trim().isEmpty()) {
				originalStatus = "Released";
			}

			MPPOrder order = new MPPOrder(ecn.getCtx(), ecn.getPP_Order_ID(), ecn.get_TrxName());
			order.set_ValueOfColumn("Orderstatus", originalStatus);
			order.setDocAction(MPPOrder.DOCACTION_Complete);
			order.saveEx();

			log.info("工单状态已回滚: " + order.getDocumentNo() + " -> " + originalStatus);
		}
	}
	
	private void cancelWorkflowProcess(MPP_Engineering_Change_Notice ecn) {
		try {
			// 获取工作流进程ID
			Integer wfProcessId = (Integer) ecn.get_Value("AD_WF_Process_ID");
			if (wfProcessId != null && wfProcessId > 0) {
				MWFProcess wfProcess = new MWFProcess(getCtx(), wfProcessId, get_TrxName());
				wfProcess.setProcessed(true);
				wfProcess.saveEx();

				// 关键：删除相关的工作流活动
				String sql = "DELETE FROM AD_WF_Activity WHERE AD_WF_Process_ID = ?";
				DB.executeUpdate(sql, new Object[] { wfProcessId }, false, get_TrxName());

				addLog(0, null, null, "已取消工作流进程和相关活动");
			}
		} catch (Exception e) {
			log.severe("取消工作流失败: " + e.getMessage());
		}
	}

	private void restoreBOMAndWorkflowStatus(MPP_Engineering_Change_Notice ecn) {
		try {
			// 获取ECN关联的工单
			if (ecn.getPP_Order_ID() <= 0) {
				return; // 没有关联工单，跳过
			}

			MPPOrder order = new MPPOrder(getCtx(), ecn.getPP_Order_ID(), get_TrxName());

			// 复原BOM状态
			restoreBOMStatus(order);

			// 复原工艺路线状态
			restoreWorkflowStatus(order);
		} catch (Exception e) {
			log.severe("复原BOM和工艺路线状态失败: " + e.getMessage());
		}
	}

	private void restoreBOMStatus(MPPOrder order) {
		int bomId = order.getPP_Product_BOM_ID();
		if (bomId > 0) {
			// 将BOM状态从'InECNChange'复原为'Released'
			String sql = "UPDATE PP_Product_BOM SET BOMStatus = 'Released' WHERE PP_Product_BOM_ID = ? AND BOMStatus = 'InECNChange'";
			DB.executeUpdate(sql, new Object[] { bomId }, false, get_TrxName());
			addLog(0, null, null, "已复原BOM状态为已发布");
		}
	}

	private void restoreWorkflowStatus(MPPOrder order) {
		int workflowId = order.getAD_Workflow_ID();
		if (workflowId > 0) {
			// 将工艺路线状态从'C'复原为'R'
			String sql = "UPDATE AD_Workflow SET PublishStatus = 'R' WHERE AD_Workflow_ID = ? AND PublishStatus = 'C'";
			DB.executeUpdate(sql, new Object[] { workflowId }, false, get_TrxName());
			addLog(0, null, null, "已复原工艺路线状态为已发布");
		}
	}
}