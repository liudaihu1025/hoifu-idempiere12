package com.hoifu.info;

import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;

/**
 * 《申购物料信息》信息窗口
 */
public class PurchaseRequisitionMaterialInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 1L;

	public PurchaseRequisitionMaterialInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public PurchaseRequisitionMaterialInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override
	protected void renderWindow() {
		super.renderWindow();
		// 移除左下角的“穿透”按钮
		confirmPanel.setVisible(ConfirmPanel.A_ZOOM, false);
		//打开信息窗口直接执行查询
		onUserQuery();

	}


	@Override
	protected boolean hasZoom() {
		// 禁用双击穿透（非 lookup 模式下双击默认会调用 zoom()）
		return false;
	}


}