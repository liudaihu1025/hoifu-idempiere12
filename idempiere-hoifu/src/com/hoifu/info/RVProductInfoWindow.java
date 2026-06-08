package com.hoifu.info;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.GridField;
import org.compiere.model.MProduct;
import org.compiere.model.MQuery;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

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
}