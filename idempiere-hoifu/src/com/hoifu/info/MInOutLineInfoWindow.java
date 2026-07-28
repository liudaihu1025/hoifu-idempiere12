package com.hoifu.info;

import java.util.List;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Event;

public class MInOutLineInfoWindow extends InfoWindow {

	// 完整构造函数 - 10个参数
	public MInOutLineInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	//收发明细批量设置意向库位流程
	private static final String InOutLineBatchSetIntendedLocator_process_classname = "com.hoifu.process.InOutLineBatchSetIntendedLocator";
	//收发明细批量更新库位流程
	private static final String InOutLineBatchUpdateLocator_process_classname = "com.hoifu.process.InOutLineBatchUpdateLocator";
	//根据信息窗口AD_InfoColumn.ColumnName='IntendedLocation_ID'、AD_InfoColumn.ColumnName='QtyEntered'更新明细库位和数量
	private static final String InOutLineBatchUpdateProcess_process_classname = "com.hoifu.process.InOutLineBatchUpdateProcess";
	
	@Override  
	protected void renderWindow() {  
	    super.renderWindow();  
	    //打开信息窗口直接执行查询
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
	protected void runProcess(Object processIdObj) {
		if (processIdObj instanceof Integer) {
			int processId = (Integer) processIdObj;
			MProcess process = MProcess.get(Env.getCtx(), processId);
            String classname = process.getClassname();  
			if (classname != null && classname.equals(InOutLineBatchSetIntendedLocator_process_classname)) {
				// 批量设置库位流程：执行后不关闭窗口，刷新数据
				setCloseAfterExecutionOfProcess(false);
			} else {
				// 其他流程正常关闭窗口
				setCloseAfterExecutionOfProcess(true);
            }  
        }  
		super.runProcess(processIdObj);
    } 
    
	/**
	 * 重写：将流程按钮放到左下角，而不是右下角
	 */
	@Override
	public void moveProcessButtonsToBeforeRight() {
		if (btProcessList == null || btProcessList.isEmpty())
			return;
		for (Button btProcess : btProcessList) {
			// 从按钮属性中取出对应的 MProcess 对象
	        Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);  
			if (processId != null) {
	            MProcess process = MProcess.get(Env.getCtx(), processId);  
	            String classname = process.getClassname();  
	            if (classname == null) continue;  
				if (classname.equals(InOutLineBatchSetIntendedLocator_process_classname)) {
					// 批量设置意向库位按钮 → 放左边
					confirmPanel.addComponentsLeft(btProcess);
					continue;
				}
				if (classname.equals(InOutLineBatchUpdateLocator_process_classname)) {
					// 执行更新库位放右边
					confirmPanel.addComponentsBeforeRight(btProcess);
					continue;
				}
				if (classname.equals(InOutLineBatchUpdateProcess_process_classname)) {
					confirmPanel.addComponentsBeforeRight(btProcess);
					continue;
				}
			}
		}
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
