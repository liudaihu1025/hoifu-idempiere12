package com.hoifu.info;

import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MInOutLineInfoWindow extends InfoWindow {

	// 完整构造函数 - 10个参数
	public MInOutLineInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override  
	protected void enableButtons() {  
	    super.enableButtons();  
	  
	    int selectedCount = contentPanel.getSelectedCount();  
	  
	    for (Button btProcess : btProcessList) {  
	        Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);  
	        if (processId != null) {  
	            MProcess process = MProcess.get(Env.getCtx(), processId);  
	            String classname = process.getClassname();  
	            if (classname == null) continue;  

	            if (classname.equals("com.hoifu.process.CreateLogisticsByDelivery")  
	                    || classname.equals("com.hoifu.process.AddLogisticsLineFromDelivery")) {  
	                boolean enabled = selectedCount > 0 && isNoneLinked();  
	                btProcess.setEnabled(enabled);  
	                btProcess.setTooltiptext(enabled ? "" : "选中的发货单中存在已关联物流单的记录，无法操作");  
	            }  
	        }  
	    }  
	}
	
	/**  
	 * 检查当前选中的所有 M_InOutLine，是否全部未关联任何物流单明细。  
	 * @return true = 全部未关联，false = 存在已关联物流单的记录  
	 */  
	private boolean isNoneLinked() {  
	    List<Integer> selectedIds = getSelectedRowKeys();  
	    if (selectedIds == null || selectedIds.isEmpty())  
	        return true;  
	  
	    StringBuilder inClause = new StringBuilder();  
	    for (int i = 0; i < selectedIds.size(); i++) {  
	        if (i > 0) inClause.append(",");  
	        inClause.append(selectedIds.get(i));  
	    }  
	  
	    int count = DB.getSQLValue(null,  
	            "SELECT COUNT(1) FROM M_LogisticsLine "  
	            + "WHERE M_InOutLine_ID IN (" + inClause + ") "  
	            + "AND IsActive = 'Y'");  
	    return count == 0;  
	}

}
