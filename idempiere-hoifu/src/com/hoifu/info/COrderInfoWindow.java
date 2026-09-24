package com.hoifu.info;

import org.compiere.model.GridField;

/**
 * C_Order 信息窗口：在 AbstractAllNumericTotalsInfoWindow 基础上增加全表合计行。
 */
public class COrderInfoWindow extends AbstractAllNumericTotalsInfoWindow {

	private static final long serialVersionUID = 1L;

	public COrderInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}
	
}