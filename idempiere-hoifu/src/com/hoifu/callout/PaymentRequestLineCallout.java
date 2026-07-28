package com.hoifu.callout;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.util.Properties;  
import java.util.logging.Level;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
  
@Callout(tableName = "C_PaymentRequestLine", columnName = {"C_Invoice_ID", "PayAmt", "RequestAmt"})  
public class PaymentRequestLineCallout implements IColumnCallout {  
  
    private static final CLogger log = CLogger.getCLogger(PaymentRequestLineCallout.class);  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
        String col = mField.getColumnName();  
        if ("C_Invoice_ID".equals(col))  
            return onInvoiceChanged(mTab, value);  
        if ("PayAmt".equals(col) || "RequestAmt".equals(col))  
            return onAmtChanged(mTab, col, value);  
        return null;  
    }  
  
    /**  
     * 选择应付单后，带出 R_Info / GrandTotal / PayAmt / DiscountAmt，  
     * 并同步计算 DifferenceAmt（因 PayAmt callout 受无限循环保护跳过，需在此处主动计算）。  
     */  
    private String onInvoiceChanged(GridTab mTab, Object value) {  
        Integer C_Invoice_ID = (Integer) value;  
        if (C_Invoice_ID == null || C_Invoice_ID == 0) {  
            mTab.setValue("R_Info",       null);  
            mTab.setValue("GrandTotal",   BigDecimal.ZERO);  
            mTab.setValue("PayAmt",       BigDecimal.ZERO);  
            mTab.setValue("DiscountAmt",  BigDecimal.ZERO);  
            mTab.setValue("DifferenceAmt", BigDecimal.ZERO);  
            return null;  
        }  
  
        String sql = "SELECT R_Info, GrandTotal, PayAmt, DiscountAmt FROM C_Invoice WHERE C_Invoice_ID=?";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, null);  
            pstmt.setInt(1, C_Invoice_ID);  
            rs = pstmt.executeQuery();  
            if (rs.next()) {  
                BigDecimal payAmt = rs.getBigDecimal("PayAmt");  
                if (payAmt == null) payAmt = BigDecimal.ZERO;  
  
                mTab.setValue("R_Info",      rs.getString("R_Info"));  
                mTab.setValue("GrandTotal",  rs.getBigDecimal("GrandTotal"));  
                mTab.setValue("PayAmt",      payAmt);  
                mTab.setValue("DiscountAmt", rs.getBigDecimal("DiscountAmt"));  
  
                // 同步 DifferenceAmt = PayAmt - RequestAmt（RequestAmt 由用户填写，初始为 0）  
                Object req = mTab.getValue("RequestAmt");  
                BigDecimal requestAmt = (req instanceof BigDecimal) ? (BigDecimal) req : BigDecimal.ZERO;  
                mTab.setValue("DifferenceAmt", payAmt.subtract(requestAmt));  
            }  
        } catch (Exception e) {  
            log.log(Level.SEVERE, sql, e);  
            return e.getLocalizedMessage();  
        } finally {  
            DB.close(rs, pstmt);  
        }  
        return null;  
    }  
  
    /**  
     * PayAmt 或 RequestAmt 变化时，重新计算 DifferenceAmt = PayAmt - RequestAmt。  
     */  
    private String onAmtChanged(GridTab mTab, String col, Object value) {  
        BigDecimal changed = (value instanceof BigDecimal) ? (BigDecimal) value : BigDecimal.ZERO;  
  
        BigDecimal payAmt;  
        BigDecimal requestAmt;  
        if ("PayAmt".equals(col)) {  
            payAmt     = changed;  
            Object req = mTab.getValue("RequestAmt");  
            requestAmt = (req instanceof BigDecimal) ? (BigDecimal) req : BigDecimal.ZERO;  
        } else { // RequestAmt  
            requestAmt = changed;  
            Object pay = mTab.getValue("PayAmt");  
            payAmt     = (pay instanceof BigDecimal) ? (BigDecimal) pay : BigDecimal.ZERO;  
        }  
  
        mTab.setValue("DifferenceAmt", payAmt.subtract(requestAmt));  
        return null;  
    }  
}