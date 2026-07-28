package com.hoifu.info;  
  
import java.io.Serializable;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.util.List;  
import java.util.logging.Level;  
  
import org.adempiere.webui.component.Button;  
import org.adempiere.webui.info.InfoWindow;  
import org.compiere.model.GridField;  
import org.compiere.model.MProcess;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
public class MPaymentRequestLineInfoWindow extends InfoWindow {  
  
    private static final CLogger log = CLogger.getCLogger(MPaymentRequestLineInfoWindow.class);  
  
    public MPaymentRequestLineInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,  
            boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,  
            String predefinedContextVariables) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,  
                field, predefinedContextVariables);  
    }  
  
    @Override  
    protected void enableButtons() {  
        super.enableButtons();  
  
        // 跨页选择时 getSelectedRowInfo().size() 比 contentPanel.getSelectedCount() 更准确  
        int selectedCount = Math.max(contentPanel.getSelectedCount(), getSelectedRowInfo().size());  
  
        for (Button btProcess : btProcessList) {  
            Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);  
            if (processId == null)  
                continue;  
            MProcess process = MProcess.get(Env.getCtx(), processId);  
            if ("com.hoifu.process.PaymentCreateByRequest".equals(process.getClassname())) {  
                boolean enabled = selectedCount > 0 && isValidForPayment();  
                btProcess.setEnabled(enabled);  
                btProcess.setTooltiptext( "供应商需一致");  
            }  
        }  
    }  
  
    /**  
     * 一次 SQL 校验所有创建付款单的前置条件：  
     * 1. 所有行都有 C_Invoice_ID（已关联应付单）  
     * 2. 所有行都没有 C_Payment_ID（未重复创建）  
     * 3. 所有行的 ApprovedAmt > 0（有批准金额）  
     * 4. 所有行的 C_Invoice.C_BPartner_ID 一致（同一供应商）  
     */  
    private boolean isValidForPayment() {  
        List<Serializable> lineIds = getSelectedRowKeys();  
        if (lineIds == null || lineIds.isEmpty())  
            return false;  
  
        // ID 均为整数，无 SQL 注入风险  
        StringBuilder inClause = new StringBuilder();  
        for (Serializable id : lineIds) {  
            if (inClause.length() > 0) inClause.append(",");  
            inClause.append(id.toString());  
        }  
  
        // 注意：查询的是 C_PaymentRequestLine，不是 C_Invoice  
        String sql =  
            "SELECT COUNT(*), " +  
            "       COUNT(prl.C_Invoice_ID), " +  
            "       SUM(CASE WHEN prl.C_Payment_ID IS NOT NULL THEN 1 ELSE 0 END), " +  
            "       SUM(CASE WHEN prl.ApprovedAmt <= 0 THEN 1 ELSE 0 END), " +  
            "       COUNT(DISTINCT ci.C_BPartner_ID) " +  
            "FROM C_PaymentRequestLine prl " +  
            "LEFT JOIN C_Invoice ci ON prl.C_Invoice_ID = ci.C_Invoice_ID " +  
            "WHERE prl.C_PaymentRequestLine_ID IN (" + inClause + ")";  
  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, null);  
            rs = pstmt.executeQuery();  
            if (rs.next()) {  
                int total       = rs.getInt(1);  
                int withInvoice = rs.getInt(2); // 有 C_Invoice_ID 的行数  
                int withPayment = rs.getInt(3); // 已有 C_Payment_ID 的行数  
                int noAmt       = rs.getInt(4); // ApprovedAmt <= 0 的行数  
                int distinctBP  = rs.getInt(5); // 不同供应商数量  
  
                return total > 0  
                    && withInvoice == total  // 所有行都有应付单  
                    && withPayment == 0      // 没有行已有付款单  
                    && noAmt == 0           // 所有行都有批准金额  
                    && distinctBP == 1;     // 所有行供应商一致  
            }  
        } catch (SQLException e) {  
            if (log.isLoggable(Level.WARNING))  
                log.warning("isValidForPayment: " + e.getMessage());  
        } finally {  
            DB.close(rs, pstmt);  
        }  
        return false;  
    }  
}