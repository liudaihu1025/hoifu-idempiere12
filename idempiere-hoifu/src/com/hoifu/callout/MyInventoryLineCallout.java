package com.hoifu.callout;  
  
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;  
  
@Callout(tableName = "M_InventoryLine", columnName = { "M_Product_ID" })
public class MyInventoryLineCallout implements IColumnCallout {


	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		// 1. 获取父表单据类型 ID
        int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");  
		if (docTypeId <= 0)
			return "";

		// 2. 通过 ID 获取单据类型对象并检查名称
		org.compiere.model.MDocType docType = org.compiere.model.MDocType.get(ctx, docTypeId);
		String docTypeName = docType.getName();

		// 如果名字是“行政物料领用单”则跳过
		if ("行政物料领用单".equals(docTypeName))
            return "";  

		// 3. 获取选中的物料 ID
        Integer productId = (Integer) value;  
        if (productId == null || productId == 0)  
            return "";  

		// 4. 从父表上下文获取仓库 ID
        int warehouseId = Env.getContextAsInt(ctx, WindowNo, "M_Warehouse_ID");  
        if (warehouseId == 0)  
            return "";  

		// 5. 调用自定义数据库函数获取推荐库位
        int locatorId = DB.getSQLValue(null,  
                "SELECT get_recommended_locator(?, ?, ?)",  
                productId, warehouseId, "N");  

        if (locatorId > 0) {  
            mTab.setValue("M_Locator_ID", locatorId);  
        }  

        return "";  
	}
}