package com.hoifu.callout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;  
  
@Callout(tableName = "C_ReconciliationLine", columnName = {"qtytoreconcile", "pricetoreconcile"})  
public class ReconciliationLineCallout implements IColumnCallout {  
      
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
        // 获取对账数量和对账价格  
        BigDecimal qtyToReconcile = (BigDecimal) mTab.getValue("qtytoreconcile");  
        BigDecimal priceToReconcile = (BigDecimal) mTab.getValue("pricetoreconcile");  
          
        if (qtyToReconcile == null)  
            qtyToReconcile = Env.ZERO;  
        if (priceToReconcile == null)  
            priceToReconcile = Env.ZERO;  
          
        // 计算对账金额  
        BigDecimal reconciledAmt = qtyToReconcile.multiply(priceToReconcile);  
          
        // 设置精度  
        if (reconciledAmt.scale() > 2)  
            reconciledAmt = reconciledAmt.setScale(2, RoundingMode.HALF_UP);  
          
        // 更新字段  
        mTab.setValue("reconciledamt", reconciledAmt);  
          
        return "";  
    }  
}