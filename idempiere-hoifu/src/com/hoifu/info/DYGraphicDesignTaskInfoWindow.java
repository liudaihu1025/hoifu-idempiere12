package com.hoifu.info;

import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;

/**
 * 平面设计任务列表信息窗口 - 打开时自动执行查询
 */
public class DYGraphicDesignTaskInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 1L;

	public DYGraphicDesignTaskInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override
	protected void renderWindow() {
		super.renderWindow();
		// 打开时自动触发查询（AuEcho 异步，构造完成后才回调，安全）
		onUserQuery();
	}
}