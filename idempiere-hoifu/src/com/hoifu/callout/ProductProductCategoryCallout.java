package com.hoifu.callout;  
  
import java.math.BigDecimal;
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
@Callout(tableName = "M_Product", columnName = {"AD_Org_ID"})  
public class ProductProductCategoryCallout implements IColumnCallout {  
  
	 private static final int TARGET_WINDOW_ID = 1000001; // 产品资料 
	 private static final String TARGET_WINDOW_NAME = "Product Finished Goods";  
	 private static final String TARGET_WINDOW_UUID = "a939f56d-2fac-4d31-9580-650c9db31864"; 
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  
  
    	if (mTab.getAD_Window_ID() != TARGET_WINDOW_ID) {  
    	    String uuid = DB.getSQLValueString(null,  
    	            "SELECT AD_Window_UU FROM AD_Window WHERE AD_Window_ID=?",  
    	            mTab.getAD_Window_ID());  
    	    if (!TARGET_WINDOW_UUID.equals(uuid)) {  
    	        return null;  
    	    }  
    	} 

        if (value == null) {  
            return null;  
        }  

        int orgId = (Integer) value;  

        // 查询当前组织的 Value  
        String orgValue = DB.getSQLValueString(null,  
                "SELECT Value FROM AD_Org WHERE AD_Org_ID=?", orgId);  
  
        if (!"0213".equals(orgValue)) {  
            // 非目标组织，不做任何操作  
            return null;  
        }  
  
        int clientId = Env.getAD_Client_ID(ctx);  
  
        // M_Product_Category_ID_L1 -> Value='ZX'  
        int catL1 = DB.getSQLValue(null,  
                "SELECT M_Product_Category_ID FROM M_Product_Category WHERE Value=? AND AD_Client_ID=?",  
                "ZX", clientId);  
        if (catL1 > 0) {  
            mTab.setValue("M_Product_Category_ID_L1", catL1);  
        }  
  
        // M_Product_Category_ID_L2 -> Value='ZX01'  
        int catL2 = DB.getSQLValue(null,  
                "SELECT M_Product_Category_ID FROM M_Product_Category WHERE Value=? AND AD_Client_ID=?",  
                "ZX01", clientId);  
        if (catL2 > 0) {  
            mTab.setValue("M_Product_Category_ID_L2", catL2);  
        }  
  
        // M_Product_Category_ID -> Value='ZX0101'  
        int cat = DB.getSQLValue(null,  
                "SELECT M_Product_Category_ID FROM M_Product_Category WHERE Value=? AND AD_Client_ID=?",  
                "ZX0101", clientId);  
        if (cat > 0) {  
            mTab.setValue("M_Product_Category_ID", cat);  
        }
        //纸箱时，模默认为1
        mTab.setValue("Imposition", BigDecimal.ONE);
  
        // 清空尺寸字段  
        mTab.setValue("Length", null);  
        mTab.setValue("Width",  null);  
        mTab.setValue("Height", null);  
          
        // 查询 X12DE355='ge' 对应的 C_UOM_ID  
        int uomId = DB.getSQLValue(null,  
                "SELECT C_UOM_ID FROM C_UOM WHERE X12DE355=? AND AD_Client_ID=? AND IsActive='Y'",  
                "ge", clientId);  
        if (uomId > 0) {  
            mTab.setValue("C_UOM_ID", uomId);  
        }
        
        return null;  
    }  
}