package com.hoifu.callout;  
  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MProduct;  
  
@Callout(tableName = "M_Product", columnName = {"CartonMaterial_ID"})  
public class ProductCartonMaterialCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
        // value 为 null 表示清空了 CartonMaterial_ID  
        if (value == null) {  
            mTab.setValue("Lengbie", null);  
            return null;  
        }  
  
        int cartonMaterialId = (Integer) value;  
        if (cartonMaterialId <= 0) {  
            mTab.setValue("Lengbie", null);  
            return null;  
        }  
  
        // CartonMaterial_ID 本质上是另一个 M_Product 的 ID  
        MProduct materialProduct = MProduct.get(ctx, cartonMaterialId);  
        if (materialProduct == null) {  
            return null;  
        }  
  
        // 自定义字段没有 getter，用 get_Value 取值  
        Object lengbie = materialProduct.get_Value("Lengbie");  
  
        // 写入当前产品 tab 的 Lengbie 字段  
        mTab.setValue("Lengbie", lengbie);  
  
        return null;  
    }  
}