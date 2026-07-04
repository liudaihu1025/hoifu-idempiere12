package com.hoifu.info;

import java.util.Date;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.GridField;
import org.compiere.model.MProduct;
import org.compiere.model.MQuery;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**  
 * 时序库存信息窗口
 */  
public class RVProductInfoWindow extends InfoWindow {  
    
    private static final long serialVersionUID = 1L;  
  
    public RVProductInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID, boolean lookup) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,   
              whereClause, AD_InfoWindow_ID, lookup);  
    }  
      
    public RVProductInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID, boolean lookup, GridField field) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,   
              whereClause, AD_InfoWindow_ID, lookup, field);  
    }  
      
    public RVProductInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID, boolean lookup, GridField field, String predefinedContextVariables) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,   
              whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);  
    }  
      
    /**  
     * 重写hasZoom方法，启用透视功能  
     */  
    @Override  
    protected boolean hasZoom() {  
        return true;  
    }  
      
    /**  
     * 重写zoom方法，实现透视到物料管理窗口  
     */  
    @Override  
    public void zoom() {  
    	Integer M_Product_ID = getIntSelectedRowKey(MProduct.Table_ID); 
        if (M_Product_ID > 0) {  
            MQuery query = new MQuery("M_Product");  
            query.addRestriction("M_Product_ID", MQuery.EQUAL, M_Product_ID);  
            AEnv.zoom(query);  
        } else {  
            Dialog.error(getWindowNo(), "PleaseSelectRecord");  
        }  
    }   
    
	@Override
	protected void executeQuery() {
		// 1. 从 infoContext 读取用户输入的 Created（valueChange() 写入的）
		String queryDate = Env.getContext(infoContext, p_WindowNo, "Created");

		// 2. 未输入时，默认当前时间，并写入 infoContext 供 prepareTable() 的 Env.parseContext() 使用
		if (queryDate == null || queryDate.isEmpty()) {
			queryDate = DisplayType.getTimestampFormat_Default().format(new Date());
			Env.setContext(infoContext, p_WindowNo, "Created", queryDate);
		}

		try {
			super.executeQuery();
		} finally {
			// 3. 清除，避免影响其他逻辑
			Env.setContext(infoContext, p_WindowNo, "Created", "");
		}
	}
}