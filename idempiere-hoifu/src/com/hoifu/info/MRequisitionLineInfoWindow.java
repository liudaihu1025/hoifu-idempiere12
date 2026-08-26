package com.hoifu.info;

import java.util.List;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Event;

public class MRequisitionLineInfoWindow extends InfoWindow {

	// 完整构造函数 - 10个参数
	public MRequisitionLineInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	// 创建采购
	private static final String RequisitionDetailPOCreate_process_classname = "com.hoifu.process.RequisitionDetailPOCreate";
	// 调整申购行物料
	private static final String RequisitionProductProcess_process_classname = "com.hoifu.process.RequisitionProductProcess";
	// 关闭申购行
	private static final String RequisitionLineCloseProcess_process_classname = "com.hoifu.process.RequisitionLineCloseProcess";
	// 重新创建采购订单
	private static final String PurchaseOrderRecreationProcess_process_classname = "com.hoifu.process.PurchaseOrderRecreationProcess";
	// 拆分申购行
	private static final String RequisitionLineSplitterProcess_process_classname = "com.hoifu.process.RequisitionLineSplitterProcess";

	@Override
	protected void renderWindow() {
		super.renderWindow();
		// 打开信息窗口直接执行查询
		onUserQuery();
	}

	@Override
	public void onQueryCallback(Event event) {
		super.onQueryCallback(event);
		// 信息窗口弹窗居中
		if (this.getParent() != null) {
			LayoutUtils.positionWindow(this.getParent(), this, "middle_center");
		}
	}

	@Override  
	protected void enableButtons() {  
		super.enableButtons();  
  
		int selectedCount = contentPanel.getSelectedCount();  
		boolean isOrderChecked = isOrderChecked();  
  
		for (Button btProcess : btProcessList) {  
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);  
			if (processId != null) {  
				MProcess process = MProcess.get(Env.getCtx(), processId);  
				String classname = process.getClassname();  
				if (classname == null) continue;  
  
				if (classname.equals(PurchaseOrderRecreationProcess_process_classname)) {  
					// 勾选 isOrder 时：只显示"重新创建采购订单"按钮  
					boolean enabled = isOrderChecked && selectedCount > 0 && isAllLinkedToOrder()  
							&& isAllActive() && isNoneOrderLineDelivered();  
					btProcess.setVisible(isOrderChecked);  
					btProcess.setEnabled(enabled);  
					btProcess.setTooltiptext(enabled ? ""  
							: "选中的申购明细必须均已生成采购单，且对应采购订单行均未收货，才能重新创建采购单");  
					continue;  
				}  
  
				// 未勾选 isOrder 时才显示其余按钮；勾选时全部隐藏  
				btProcess.setVisible(!isOrderChecked);  
				if (isOrderChecked) {  
					continue;  
				}  
  
				if (classname.equals(RequisitionDetailPOCreate_process_classname)) {  
					boolean enabled = selectedCount > 0 && isNoneLinkedToOrder() && isAllActive();  
					btProcess.setEnabled(enabled);  
					btProcess.setTooltiptext(enabled ? "" : "选中的申购明细中存在已生成采购单或已关闭的记录，无法创建采购单");  
				} else if (classname.equals(RequisitionProductProcess_process_classname)) {  
					boolean enabled = selectedCount == 1 && isNoneLinkedToOrder() && isAllActive();  
					btProcess.setEnabled(enabled);  
					btProcess.setTooltiptext(enabled ? "" : "仅支持对未生成采购单且未关闭的单条明细调整物料");  
				} else if (classname.equals(RequisitionLineCloseProcess_process_classname)) {  
					boolean enabled = selectedCount > 0 && isAllActive() && isNoneLinkedToOrder();  
					btProcess.setEnabled(enabled);  
					btProcess.setTooltiptext(enabled ? "" : "选中的申购明细中存在已关闭或已生成采购单的记录，无法关闭");  
				} else if (classname.equals(RequisitionLineSplitterProcess_process_classname)) {  
					boolean enabled = selectedCount == 1 && isNoneLinkedToOrder() && isAllActive();  
					btProcess.setEnabled(enabled);  
					btProcess.setTooltiptext(enabled ? "" : "选中的申购明细中存在已生成采购单或已关闭的记录，无法拆分");  
				}  
			}  
		}  
	}  

	private boolean isOrderChecked() {  
	    if (editors == null)  
	        return false;  
	    for (WEditor editor : editors) {  
	        GridField gf = editor.getGridField();  
	        if (gf != null && "isOrder".equalsIgnoreCase(gf.getColumnName())) {  
	            Object value = editor.getValue();  
	            return value != null && (Boolean.TRUE.equals(value) || "Y".equals(value));  
	        }  
	    }  
	    return false;  
	}
	
	/**
	 * 检查当前选中的所有 M_RequisitionLine，是否全部已关联采购订单行（C_OrderLine_ID 不为空且不为0）。
	 * 
	 * @return true = 全部已关联，false = 存在未关联的记录，或未选中任何记录
	 */
	private boolean isAllLinkedToOrder() {
		List<Integer> selectedIds = getSelectedRowKeys();
		if (selectedIds == null || selectedIds.isEmpty())
			return false;

		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < selectedIds.size(); i++) {
			if (i > 0)
				inClause.append(",");
			inClause.append(selectedIds.get(i));
		}

		int count = DB.getSQLValue(null, "SELECT COUNT(1) FROM M_RequisitionLine " + "WHERE M_RequisitionLine_ID IN ("
				+ inClause + ") " + "AND (C_OrderLine_ID IS NULL OR C_OrderLine_ID = 0)");
		return count == 0;
	}

	/**
	 * 检查当前选中的所有 M_RequisitionLine 所关联的采购订单行，是否全部未收货（QtyDelivered = 0）。
	 * 仅对已关联采购订单行的记录生效，未关联的记录不参与判断。
	 * 
	 * @return true = 全部未收货（或均未关联），false = 存在已收货的关联记录，或未选中任何记录
	 */
	private boolean isNoneOrderLineDelivered() {
		List<Integer> selectedIds = getSelectedRowKeys();
		if (selectedIds == null || selectedIds.isEmpty())
			return false;

		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < selectedIds.size(); i++) {
			if (i > 0)
				inClause.append(",");
			inClause.append(selectedIds.get(i));
		}

		int count = DB.getSQLValue(null,
				"SELECT COUNT(1) FROM M_RequisitionLine rl "
						+ "JOIN C_OrderLine ol ON ol.C_OrderLine_ID = rl.C_OrderLine_ID "
						+ "WHERE rl.M_RequisitionLine_ID IN (" + inClause + ") " + "AND ol.QtyDelivered <> 0");
		return count == 0;
	}

	/**
	 * 检查当前选中的所有 M_RequisitionLine，是否全部未关联采购订单行（C_OrderLine_ID 为空）。
	 * 
	 * @return true = 全部未关联，false = 存在已关联采购单行的记录，或未选中任何记录
	 */
	private boolean isNoneLinkedToOrder() {
		List<Integer> selectedIds = getSelectedRowKeys();
		if (selectedIds == null || selectedIds.isEmpty())
			return false;

		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < selectedIds.size(); i++) {
			if (i > 0)
				inClause.append(",");
			inClause.append(selectedIds.get(i));
		}

		int count = DB.getSQLValue(null, "SELECT COUNT(1) FROM M_RequisitionLine " + "WHERE M_RequisitionLine_ID IN ("
				+ inClause + ") " + "AND C_OrderLine_ID IS NOT NULL AND C_OrderLine_ID <> 0");
		return count == 0;
	}

	/**
	 * 检查当前选中的所有 M_RequisitionLine 是否均为有效状态（IsActive='Y'）。
	 * 
	 * @return true = 全部有效，false = 存在已关闭（IsActive='N'）的记录，或未选中任何记录
	 */
	private boolean isAllActive() {
		List<Integer> selectedIds = getSelectedRowKeys();
		if (selectedIds == null || selectedIds.isEmpty())
			return false;

		StringBuilder inClause = new StringBuilder();
		for (int i = 0; i < selectedIds.size(); i++) {
			if (i > 0)
				inClause.append(",");
			inClause.append(selectedIds.get(i));
		}

		int count = DB.getSQLValue(null, "SELECT COUNT(1) FROM M_RequisitionLine " + "WHERE M_RequisitionLine_ID IN ("
				+ inClause + ") " + "AND IsActive = 'N'");
		return count == 0;
	}
}