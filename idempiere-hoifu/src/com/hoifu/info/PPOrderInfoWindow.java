package com.hoifu.info;  
  
import java.io.Serializable;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 工单信息窗口 - 控制三个流程按钮的启用/禁用状态 - 流程发布 (PPOrderReleaseProcess): 所有选中工单 DocStatus =
 * 'DR' - 流程开工 (PPOrderStartWorkProcess): 所有选中工单 OrderStatus IN ('Released',
 * 'Paused') - 流程发货 (PPOrderMarkShippedProcess): 所有选中工单 OrderStatus = 'Stored'
 */
public class PPOrderInfoWindow extends InfoWindow {  

	private static final long serialVersionUID = 1L;

	public PPOrderInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause,
            int AD_InfoWindow_ID) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID);  
    }  

	public PPOrderInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause,
            int AD_InfoWindow_ID, boolean lookup) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup);  
    }  

	public PPOrderInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause,
            int AD_InfoWindow_ID, boolean lookup, GridField field) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup, field);  
    }  

	public PPOrderInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);  
    }  

    @Override  
    protected void enableButtons() {  
        super.enableButtons();  

        int selectedCount = contentPanel.getSelectedCount();  

		// 预先计算，避免每个按钮重复查询数据库
		boolean canRelease = selectedCount > 0 && areAllSelectedOrdersReleasable();
		boolean canStartWork = selectedCount > 0 && areAllSelectedOrdersStartable();
		boolean canShip = selectedCount > 0 && areAllSelectedOrdersShippable();

        for (Button btProcess : btProcessList) {  
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
			if (processId == null)
				continue;

			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process == null || process.getClassname() == null)
				continue;

			String className = process.getClassname();

			if (className.equals("org.libero.process.PPOrderReleaseProcess")) {
				btProcess.setEnabled(canRelease);
				btProcess.setTooltiptext("发布工单 - 仅支持草稿状态的工单");

			} else if (className.equals("org.libero.process.PPOrderStartWorkProcess")) {
				btProcess.setEnabled(canStartWork);
				btProcess.setTooltiptext("开工 - 仅支持已发布或已暂停状态的工单");

			} else if (className.equals("org.libero.process.PPOrderMarkShippedProcess")) {
				btProcess.setEnabled(canShip);
				btProcess.setTooltiptext("发货 - 仅支持已入库状态的工单");
            }  
        }  
	}

	/**
	 * 所有选中工单 DocStatus = 'DR'（草稿）才可发布
	 */
	private boolean areAllSelectedOrdersReleasable() {
		List<Serializable> keys = getSelectedRowKeys();
		if (keys == null || keys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM PP_Order WHERE PP_Order_ID IN (");
		for (int i = 0; i < keys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND DocStatus != ?");

		Object[] params = new Object[keys.size() + 1];
		for (int i = 0; i < keys.size(); i++)
			params[i] = keys.get(i);
		params[keys.size()] = "DR";

		return DB.getSQLValueEx(null, sql.toString(), params) == 0;
	}

	/**
	 * 所有选中工单 OrderStatus IN ('Released', 'Paused') 才可开工
	 */
	private boolean areAllSelectedOrdersStartable() {
		List<Serializable> keys = getSelectedRowKeys();
		if (keys == null || keys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM PP_Order WHERE PP_Order_ID IN (");
		for (int i = 0; i < keys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND OrderStatus NOT IN (?, ?)");

		Object[] params = new Object[keys.size() + 2];
		for (int i = 0; i < keys.size(); i++)
			params[i] = keys.get(i);
		params[keys.size()] = "Released";
		params[keys.size() + 1] = "Paused";

		return DB.getSQLValueEx(null, sql.toString(), params) == 0;
	}

	/**
	 * 所有选中工单 OrderStatus = 'Stored'（已入库）才可发货
	 */
	private boolean areAllSelectedOrdersShippable() {
		List<Serializable> keys = getSelectedRowKeys();
		if (keys == null || keys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM PP_Order WHERE PP_Order_ID IN (");
		for (int i = 0; i < keys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND OrderStatus != ?");

		Object[] params = new Object[keys.size() + 1];
		for (int i = 0; i < keys.size(); i++)
			params[i] = keys.get(i);
		params[keys.size()] = "Stored";

		return DB.getSQLValueEx(null, sql.toString(), params) == 0;
	}
}