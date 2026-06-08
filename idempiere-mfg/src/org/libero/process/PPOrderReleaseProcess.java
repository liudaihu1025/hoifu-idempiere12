package org.libero.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;
/**
 * 新增一个《工单信息》的info界面，并在界面里增加一个“发布”按钮；用户选择需要发布的工单，一个或者多个点击“发布”按钮后，系统打开对话框前，判断工单是否符合“发布”操作要求
 * 如果通过校验，弹出一个“工单发布”流程参数窗口，用户可以在窗口内选择“优先级”和“资源”
 * 在《工单发布》窗口确认后，更新对应工单的“优先级”和“资源”字段，“单据状态”从【草稿】更新为【处理中】
 */
@org.adempiere.base.annotation.Process
public class PPOrderReleaseProcess extends SvrProcess {

	private String p_PriorityRule = null;
	private int p_S_Resource_ID = 0;
	private List<Integer> selectedOrderIds = new ArrayList<>();

	/**
	 * 收集用户选择的参数
	 */
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("PriorityRule"))
				p_PriorityRule = para[i].getParameterAsString();
			else if (name.equals("S_Resource_ID"))
				p_S_Resource_ID = para[i].getParameterAsInt();
		}
	}

	/**
	 * 发布执行
	 */
	protected String doIt() throws Exception {
		// 从T_Selection表获取选择的记录
		loadSelectedOrders();
		if (selectedOrderIds.isEmpty()) {
			throw new IllegalArgumentException("请选择要发布的工单");
		}

		// 校验所有选中的工单状态
		validateOrderStatus();

		// 新增：校验父件物料的有效字段
		validateBOMActivity();

		// 新增：校验工艺路线的有效字段
		validateWorkflowActivity();

		// 更新工单信息
		int updatedCount = updateOrders();

		return "成功发布 " + updatedCount + " 个工单";
	}

	private void loadSelectedOrders() {
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				selectedOrderIds.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取工单选项失败，请联系管理员处理。");
		} finally {
			DB.close(rs, pstmt);
		}
	}

	private void validateOrderStatus() throws Exception {
		String sql = "SELECT PP_Order_ID, DocumentNo FROM PP_Order " + "WHERE PP_Order_ID IN ("
				+ selectedOrderIds.stream().map(String::valueOf).collect(Collectors.joining(","))
				+ ") AND DocStatus != ?";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<String> nonDraftOrders = new ArrayList<>();

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setString(1, DocAction.STATUS_Drafted);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				nonDraftOrders.add(rs.getString("DocumentNo"));
			}
		} finally {
			DB.close(rs, pstmt);
		}

		if (!nonDraftOrders.isEmpty()) {
			throw new IllegalArgumentException(
					"您选择的工单中包含以下\"非草稿\"状态的条目：" + String.join(", ", nonDraftOrders) + "。只有状态为【草稿】的工单才允许提交。");
		}
	}

	private int updateOrders() {
		int count = 0;
		for (Integer orderId : selectedOrderIds) {
			MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());
			if (order.get_ID() > 0) {
				// 更新优先级和资源
				order.setPriorityRule(p_PriorityRule);
				order.setS_Resource_ID(p_S_Resource_ID);
                // 更新工单状态为已发布  
                order.set_ValueOfColumn("Orderstatus", "Released"); 
				order.saveEx();

				// 使用工作流处理状态变更
				if (DocAction.STATUS_Drafted.equals(order.getDocStatus())) {

					// 设置文档动作并处理
					order.setDocAction(DocAction.ACTION_Prepare);
					if (order.processIt(DocAction.ACTION_Prepare)) {
						order.saveEx();
						count++;
					} else {
						log.warning("更新工单 " + order.getDocumentNo() + " 失败: " + order.getProcessMsg());
						throw new IllegalArgumentException("准备工单失败: " + order.getProcessMsg());
					}
				}
			}
		}
		return count;
	}

	/**
	 * 校验工单父件物料的有效字段
	 */
	private void validateBOMActivity() throws Exception {
		String sql = "SELECT o.PP_Order_ID, o.DocumentNo FROM PP_Order o "
				+ "INNER JOIN PP_Order_BOM ob ON o.PP_Order_ID = ob.PP_Order_ID " + "WHERE o.PP_Order_ID IN ("
				+ selectedOrderIds.stream().map(String::valueOf).collect(Collectors.joining(","))
				+ ") AND ob.IsActive = 'N'";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<String> inactiveBOMOrders = new ArrayList<>();

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				inactiveBOMOrders.add(rs.getString("DocumentNo"));
			}
		} finally {
			DB.close(rs, pstmt);
		}

		if (!inactiveBOMOrders.isEmpty()) {
			throw new IllegalArgumentException(
					"您选择的工单中包含以下父件物料未激活的条目：" + String.join(", ", inactiveBOMOrders) + "。请先激活父件物料后再发布工单。");
		}
	}

	/**
	 * 校验工单工艺路线的有效字段
	 */
	private void validateWorkflowActivity() throws Exception {
		String sql = "SELECT o.PP_Order_ID, o.DocumentNo FROM PP_Order o "
				+ "INNER JOIN PP_Order_Workflow ow ON o.PP_Order_ID = ow.PP_Order_ID " + "WHERE o.PP_Order_ID IN ("
				+ selectedOrderIds.stream().map(String::valueOf).collect(Collectors.joining(","))
				+ ") AND ow.IsActive = 'N'";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<String> inactiveWorkflowOrders = new ArrayList<>();

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				inactiveWorkflowOrders.add(rs.getString("DocumentNo"));
			}
		} finally {
			DB.close(rs, pstmt);
		}

		if (!inactiveWorkflowOrders.isEmpty()) {
			throw new IllegalArgumentException(
					"您选择的工单中包含以下工艺路线未激活的条目：" + String.join(", ", inactiveWorkflowOrders) + "。请先激活工艺路线后再发布工单。");
		}
	}
}