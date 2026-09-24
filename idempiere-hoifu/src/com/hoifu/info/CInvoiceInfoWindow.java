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
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CInvoiceInfoWindow extends AbstractAllNumericTotalsInfoWindow {

	// 完整构造函数 - 10个参数
	public CInvoiceInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override  
	protected void enableButtons() {  
	    super.enableButtons();  
	  
	    int selectedCount = contentPanel.getSelectedCount();  
	  
	    for (Button btProcess : btProcessList) {  
	        Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);  
	        if (processId == null) continue;  
	  
	        MProcess process = MProcess.get(Env.getCtx(), processId);  
	        String classname = process.getClassname();  
	        if (classname == null) continue;  
	  
	        if (classname.equals("com.hoifu.process.CreatePaymentByInvoicesProcess")) {  
	            boolean enabled = selectedCount > 0 && isSameBPartner();  
	            btProcess.setEnabled(enabled);  
	            btProcess.setTooltiptext("仅相同业务伙伴时可合并。");  
	  
	        } 
	        if (classname.equals("com.hoifu.process.PaymentRequestCreateByInvoice")) {  
	            // ── 付款申请单流程 ────────────────────────────────────  
	            ValidationResult result = selectedCount > 0  
	                    ? validateForPaymentRequest()  
	                    : new ValidationResult(false, "请先选择应付单");  
	            btProcess.setEnabled(result.valid);  
	            btProcess.setTooltiptext(result.message);  
	        }  
	    }  
	}  
	  
	/**  
	 * 一次 SQL 检查付款申请单流程的所有前置条件。  
	 */  
	private ValidationResult validateForPaymentRequest() {  
	    List<Serializable> ids = getSelectedRowKeys();  
	    if (ids == null || ids.isEmpty())  
	        return new ValidationResult(false, "请先选择应付单");  
	  
	    StringBuilder inClause = new StringBuilder();  
	    for (Serializable id : ids) {  
	        if (inClause.length() > 0) inClause.append(',');  
	        inClause.append(id);  
	    }  
	  
	  //[1] 往来单位种数、[2] 货币种数、[3] 状态不合格数、[4] 已结清数、[5] 应收单数（只能是应付单） 、[6] 已无可申请余额的发票数
	    String sql = "SELECT "  
	    	    + "COUNT(DISTINCT i.C_BPartner_ID), "  
	    	    + "COUNT(DISTINCT i.C_Currency_ID), "  
	    	    + "COUNT(CASE WHEN i.DocStatus NOT IN ('CO','CL') THEN 1 END), "  
	    	    + "COUNT(CASE WHEN i.IsPaid = 'Y' THEN 1 END), "  
	    	    + "COUNT(CASE WHEN i.IsSOTrx = 'Y' THEN 1 END), "  
	    	    + "COUNT(CASE WHEN i.PayAmt - COALESCE(("  
	    	    +     "SELECT SUM(prl.RequestAmt) "  
	    	    +     "FROM C_PaymentRequestLine prl "  
	    	    +     "INNER JOIN C_PaymentRequest pr "  
	    	    +     "       ON prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID "  
	    	    +     "WHERE prl.C_Invoice_ID = i.C_Invoice_ID "  
	    	    +     "  AND pr.DocStatus NOT IN ('VO','RE') "  
	    	    +     "  AND prl.IsActive = 'Y'"  
	    	    + "), 0) <= 0 THEN 1 END) "  
	    	    + "FROM C_Invoice i WHERE i.C_Invoice_ID IN (" + inClause + ")";
	    
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try {  
	        pstmt = DB.prepareStatement(sql, null);  
	        rs = pstmt.executeQuery();  
	        if (rs.next()) {  
	            int distinctBP       = rs.getInt(1);  
	            int distinctCurrency = rs.getInt(2);  
	            int badStatus        = rs.getInt(3);  
	            int paidCount        = rs.getInt(4);  
	            int soTrxCount       = rs.getInt(5);  
	            int noQuotaCount = rs.getInt(6);  
	            
	            if (soTrxCount > 0)  
	                return new ValidationResult(false, "选中记录包含销售单，请仅选择应付单");  
	            if (badStatus > 0)  
	                return new ValidationResult(false, "存在未完成的应付单，请仅选择已完成(CO)或已关闭(CL)的应付单");  
	            if (paidCount > 0)  
	                return new ValidationResult(false, "存在已结清的应付单，无法申请付款");  
	            if (distinctBP > 1)  
	                return new ValidationResult(false, "选中的应付单往来单位不一致，请选择相同往来单位的应付单");  
	            if (distinctCurrency > 1)  
	                return new ValidationResult(false, "选中的应付单货币不一致，请选择相同货币的应付单");  
	            if (noQuotaCount > 0)  
	                return new ValidationResult(false, "存在已无可申请余额的应付单，请重新选择");
	            return new ValidationResult(true, "创建付款申请单");  
	        }  
	    } catch (SQLException e) {  
	        log.log(Level.SEVERE, "validateForPaymentRequest", e);  
	    } finally {  
	        DB.close(rs, pstmt);  
	    }  
	    return new ValidationResult(false, "校验失败，请查看日志");  
	}  
	  
	private static final class ValidationResult {  
	    final boolean valid;  
	    final String  message;  
	    ValidationResult(boolean valid, String message) {  
	        this.valid   = valid;  
	        this.message = message;  
	    }  
	}
	private boolean isSameBPartner() {
		List<Serializable> invoiceIds = getSelectedRowKeys();

		if (invoiceIds == null || invoiceIds.isEmpty()) {
			return false;
		}
		// 只有一张发票时，直接通过
		if (invoiceIds.size() == 1) {
			return true;
		}

		// 构建 IN 子句（ID 均为整数，无 SQL 注入风险）
		StringBuilder inClause = new StringBuilder();
		for (Serializable id : invoiceIds) {
			if (inClause.length() > 0)
				inClause.append(",");
			inClause.append(id.toString());
		}

		String sql = "SELECT COUNT(DISTINCT C_BPartner_ID) FROM C_Invoice WHERE C_Invoice_ID IN (" + inClause + ")";

		int distinctCount = DB.getSQLValue(null, sql);
		return distinctCount == 1;
	}

}
