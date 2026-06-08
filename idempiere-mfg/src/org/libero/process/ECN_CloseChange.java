package org.libero.process;

import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;
import org.libero.model.MPP_Engineering_Change_Notice;

public class ECN_CloseChange extends SvrProcess {

	@Override
	protected void prepare() {
		// 准备逻辑
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		MPP_Engineering_Change_Notice ecn = new MPP_Engineering_Change_Notice(getCtx(), recordId, get_TrxName());

		// 检查当前状态是否允许取消
		if (!canCancel(ecn)) {
			return "当前状态不允许取消变更";
		}

		// 执行取消操作
		if (ecn.voidIt()) {

			// 回滚产品BOM和工艺路线状态
			//rollbackBOMAndWorkflowStatus(ecn);
			return "变更已成功取消";
		} else {
			return "取消变更失败";
		}
	}

	/**
	 * 检查是否可以取消
	 */
	private boolean canCancel(MPP_Engineering_Change_Notice ecn) {
		String currentStatus = ecn.getECNStatus();
		// 只有特定状态才能取消
		return "Approved".equals(currentStatus) || "InExecution".equals(currentStatus);
	}

	/**
	 * 回滚产品BOM和工艺路线状态
	 */
	private void rollbackBOMAndWorkflowStatus(MPP_Engineering_Change_Notice ecn) {
		if (ecn.getPP_Order_ID() > 0) {
			MPPOrder order = new MPPOrder(ecn.getCtx(), ecn.getPP_Order_ID(), ecn.get_TrxName());

			// 回滚产品BOM状态
			rollbackBOMStatus(order);

			// 回滚工艺路线状态
			rollbackWorkflowStatus(order);

			addLog(0, null, null, "已回滚BOM和工艺路线状态");
		}
	}

	/**
	 * 回滚产品BOM状态
	 */
	private void rollbackBOMStatus(MPPOrder order) {
		int bomId = order.getPP_Product_BOM_ID();
		if (bomId > 0) {
			// 将BOM状态从"InECNChange"回滚为"Released"
			String sql = "UPDATE PP_Product_BOM SET bomstatus = 'Released' "
					+ "WHERE PP_Product_BOM_ID = ? AND bomstatus = 'InECNChange'";
			int updated = DB.executeUpdate(sql, new Object[] { bomId }, false, get_TrxName());
			if (updated > 0) {
				addLog(0, null, null, "已回滚BOM状态为已发布");
			}
		}
	}

	/**
	 * 回滚工艺路线状态
	 */
	private void rollbackWorkflowStatus(MPPOrder order) {
		int workflowId = order.getAD_Workflow_ID();
		if (workflowId > 0) {
			// 将工艺路线状态从"C"（变更中）回滚为"R"（已发布）
			String sql = "UPDATE AD_Workflow SET PublishStatus = 'R' "
					+ "WHERE AD_Workflow_ID = ? AND PublishStatus = 'C'";
			int updated = DB.executeUpdate(sql, new Object[] { workflowId }, false, get_TrxName());
			if (updated > 0) {
				addLog(0, null, null, "已回滚工艺路线状态为已发布");
			}
		}
	}
}