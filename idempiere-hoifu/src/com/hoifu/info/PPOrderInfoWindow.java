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
 * 工单信息窗口 - 控制流程按钮的启用/禁用状态
 * - 流程发布 (PPOrderReleaseProcess): 所有选中工单 DocStatus = 'DR'
 * - 流程开工 (PPOrderStartWorkProcess): 所有选中工单 OrderStatus IN ('Released', 'Paused')
 * - 流程发货 (PPOrderMarkShippedProcess): 所有选中工单 OrderStatus = 'Stored'
 * - 流程卡打印 (ProcessCardPrintByPPOrder): 所有选中工单 OrderStatus = 'Started'（已开工）
 * - 流程卡信息窗口 (OpenProcessCardInfo): 所有选中工单 OrderStatus = 'Started'（已开工）
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

		// 打样追踪详情：只能单选，且工单状态、工单类型都满足才可点击
		boolean canTrack = selectedCount == 1 && isSelectedOrderTrackable();

		// 预先计算，避免每个按钮重复查询数据库
		boolean canRelease = selectedCount > 0 && areAllSelectedOrdersReleasable();
		boolean canStartWork = selectedCount > 0 && areAllSelectedOrdersStartable();
		boolean canShip = selectedCount > 0 && areAllSelectedOrdersShippable();
		boolean canPrintCard = selectedCount == 1 && areAllSelectedOrdersPrintableForNewCard();
		boolean canOpenCardInfo = selectedCount > 0 && areAllSelectedOrdersHaveCardInfo();

        for (Button btProcess : btProcessList) {  
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
			if (processId == null)
				continue;

			MProcess process = MProcess.get(Env.getCtx(), processId);

			if (process == null)
				continue;

			String value = process.getValue();

			// 打样追踪详情
			if ("打样追踪详情".equals(process.getName())) {
				btProcess.setEnabled(canTrack);
				btProcess.setTooltiptext("打样追踪详情 - 仅支持已开工/已完工/已入库状态的打样工单或研发工单，且只能单选");
				continue;
			}

			if ("OpenProcessCardInfo".equals(value)) {

				btProcess.setEnabled(canOpenCardInfo);
				btProcess.setTooltiptext("流程卡信息窗口 - 仅支持已存在流程卡记录的工单");
			}

			if (process.getClassname() == null) {
				continue;
			}
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

			} else if (className.equals("org.libero.process.ProcessCardPrintByPPOrder")) {
				btProcess.setEnabled(canPrintCard);
				btProcess.setTooltiptext("流程卡打印 - 仅支持已开工且尚未生成流程卡的工单");

			}
        }  
	}

	/**
	 * 打样追踪详情”按钮的启用条件
	 */
	private boolean isSelectedOrderTrackable() {
		List<Serializable> keys = getSelectedRowKeys();
		if (keys == null || keys.size() != 1)
			return false;

		String sql = "SELECT COUNT(*) FROM PP_Order WHERE PP_Order_ID=? "
				+ "AND OrderStatus IN (?, ?, ?) AND C_DocTypeTarget_ID IN (?, ?)";
		return DB.getSQLValueEx(null, sql, keys.get(0), "Started", "Completed", "Stored", 1000755, 1000758) > 0;
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
	 * 所有选中工单必须同时满足：  
	 * 1) OrderStatus = 'Started'（已开工）  
	 * 2) 在 PP_Process_Card 中尚无对应记录（未打印过流程卡）  
	 * 才可执行"流程卡打印"  
	 */  
	private boolean areAllSelectedOrdersPrintableForNewCard() {  
	    List<Serializable> keys = getSelectedRowKeys();  
	    if (keys == null || keys.isEmpty())  
	        return false;  
	  
	    StringBuilder sql = new StringBuilder(  
	        "SELECT COUNT(*) FROM PP_Order o WHERE o.PP_Order_ID IN (");  
	    for (int i = 0; i < keys.size(); i++) {  
	        if (i > 0)  
	            sql.append(",");  
	        sql.append("?");  
	    }  
	    sql.append(") AND (o.OrderStatus != ? " +  
	               "OR EXISTS (SELECT 1 FROM PP_Process_Card pc WHERE pc.PP_Order_ID = o.PP_Order_ID))");  
	  
	    Object[] params = new Object[keys.size() + 1];  
	    for (int i = 0; i < keys.size(); i++)  
	        params[i] = keys.get(i);  
	    params[keys.size()] = "Started";  
	  
	    // 返回 0 表示所有选中工单都是"已开工 且 尚未打印流程卡"  
	    return DB.getSQLValueEx(null, sql.toString(), params) == 0;  
	}  
	  
	/**  
	 * 所有选中工单在 PP_Process_Card 中都已存在记录，才可打开"流程卡信息窗口"  
	 */  
	private boolean areAllSelectedOrdersHaveCardInfo() {  
	    List<Serializable> keys = getSelectedRowKeys();  
	    if (keys == null || keys.isEmpty())  
	        return false;  
	  
	    StringBuilder sql = new StringBuilder(  
	        "SELECT COUNT(*) FROM PP_Order o WHERE o.PP_Order_ID IN (");  
	    for (int i = 0; i < keys.size(); i++) {  
	        if (i > 0)  
	            sql.append(",");  
	        sql.append("?");  
	    }  
	    sql.append(") AND NOT EXISTS (SELECT 1 FROM PP_Process_Card pc WHERE pc.PP_Order_ID = o.PP_Order_ID)");  
	  
	    Object[] params = new Object[keys.size()];  
	    for (int i = 0; i < keys.size(); i++)  
	        params[i] = keys.get(i);  
	  
	    // 返回 0 表示所有选中工单都已存在流程卡记录  
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

	@Override
	protected void renderWindow() {
		super.renderWindow();

		// 移除全选 / 取消全选按钮
		confirmPanel.setVisible("SelectAll", false);
		confirmPanel.setVisible("DeSelectAll", false);

	}
}