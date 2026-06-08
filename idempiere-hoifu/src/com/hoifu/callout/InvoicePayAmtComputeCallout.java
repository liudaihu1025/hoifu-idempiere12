package com.hoifu.callout;  
  
import java.math.BigDecimal;  
import java.util.Properties;  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.util.Env;  
  
/**  
 * 根据折扣金额等，自动计算支付金额PayAmt=TotalLines（明细总额）-Rebate（不良品扣款或返点）-WriteOffAmt（核销金额）-DiscountAmt（折扣金额）
 */  
@Callout(tableName = "C_Invoice", columnName = { "Rebate","WriteOffAmt","DiscountAmt" })  
public class InvoicePayAmtComputeCallout implements IColumnCallout {  
      
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
        // 获取TotalLines金额  
        BigDecimal totalLines = (BigDecimal) mTab.getValue("TotalLines");  
        if (totalLines == null) {  
            totalLines = Env.ZERO;  
        }  
          
        // 获取Rebate金额，如果为空则按0计算  
        BigDecimal rebate = (BigDecimal) mTab.getValue("Rebate");  
        if (rebate == null) {  
            rebate = Env.ZERO;  
        }  
          
        // 获取WriteOffAmt金额，如果为空则按0计算  
        BigDecimal writeOffAmt = (BigDecimal) mTab.getValue("WriteOffAmt");  
        if (writeOffAmt == null) {  
            writeOffAmt = Env.ZERO;  
        }  
          
        // 获取DiscountAmt金额，如果为空则按0计算  
        BigDecimal discountAmt = (BigDecimal) mTab.getValue("DiscountAmt");  
        if (discountAmt == null) {  
            discountAmt = Env.ZERO;  
        }  
          
        // 计算PayAmt = TotalLines - Rebate - WriteOffAmt - DiscountAmt  
        BigDecimal payAmt = totalLines.subtract(rebate).subtract(writeOffAmt).subtract(discountAmt);  
          
        // 设置计算后的PayAmt值  
        mTab.setValue("PayAmt", payAmt);  
          
        return "";  
    }  
}