package com.hoifu.info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridField;
import org.compiere.model.MTable;
import org.compiere.util.DB;

public class RVMTransactionDetailInfo extends InfoWindow {
	
	// 完整构造函数 - 10个参数
	public RVMTransactionDetailInfo(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
		setMultipleSelection(false);
	}

	@Override  
	protected boolean hasZoom() {  
	    return true; // 绕过 isView() 的限制  
	}  
	  
	@Override  
	public void zoom() {  
	    int row = contentPanel.getSelectedRow();  
	    if (row < 0) return;  
	  
	    // 读取 key column（M_Transaction_ID），它一定在 columnInfos[0]  
	    Object keyValue = contentPanel.getValueAt(row, 0);  
	    if (keyValue == null) return;  
	  
	    int mTransactionId = 0;  
	    if (keyValue instanceof IDColumn)  
	        mTransactionId = ((IDColumn) keyValue).getRecord_ID();  
	    else if (keyValue instanceof Integer)  
	        mTransactionId = (Integer) keyValue;  
	  
	    if (mTransactionId <= 0) return;  
	  
	    // 重新查询视图获取 TableName 和 Record_ID  
	    String sql = "SELECT TableName, Record_ID FROM RV_M_Transaction_Detail WHERE M_Transaction_ID = ?";  
	    try (PreparedStatement pstmt = DB.prepareStatement(sql, null);) {  
	        pstmt.setInt(1, mTransactionId);  
	        ResultSet rs = pstmt.executeQuery();  
	        if (rs.next()) {  
	            String tableName = rs.getString(1);  
	            int recordId     = rs.getInt(2);  
	            if (tableName != null && recordId > 0) {  
	                int AD_Table_ID = MTable.getTable_ID(tableName);  
	                if (AD_Table_ID > 0)  
	                    AEnv.zoom(AD_Table_ID, recordId);  
	            }  
	        }  
	        rs.close();  
	    } catch (Exception e) {  
	        log.log(Level.SEVERE, sql, e);  
	    }  
	}
}
