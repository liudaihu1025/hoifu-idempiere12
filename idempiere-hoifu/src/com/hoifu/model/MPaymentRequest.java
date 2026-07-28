package com.hoifu.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.model.DocActionDelegate;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;

public class MPaymentRequest extends X_C_PaymentRequest implements DocAction , DocOptions {

	private static final long serialVersionUID = 1L;

	private DocActionDelegate<MPaymentRequest> docActionDelegate;

	public MPaymentRequest(Properties ctx, int C_PaymentRequest_ID, String trxName) {
		super(ctx, C_PaymentRequest_ID, trxName);
		if (C_PaymentRequest_ID == 0)
			setInitialDefaults();
		init();
	}

	public MPaymentRequest(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		init();
	}

	private void setInitialDefaults() {
		setProcessed(false);
	}

	private void init() {
		docActionDelegate = new DocActionDelegate<>(this);
		docActionDelegate.setActionCallable(DocAction.ACTION_Prepare, () -> doPrepare());  
		docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		return true;
	}

	private String doPrepare() {  
	    int count = DB.getSQLValue(get_TrxName(),  
	        "SELECT COUNT(*) FROM C_PaymentRequestLine "  
	        + "WHERE C_PaymentRequest_ID=? AND IsActive='Y'",  
	        getC_PaymentRequest_ID());  
	    if (count == 0)  
	        return "@NoLines@";  
	    // 校验所有有效行的 R_Info 不能为空  
	    int emptyCount = DB.getSQLValue(get_TrxName(),  
	        "SELECT COUNT(*) FROM C_PaymentRequestLine "  
	        + "WHERE C_PaymentRequest_ID=? AND IsActive='Y' "  
	        + "AND (R_Info IS NULL OR TRIM(R_Info) = '')",  
	        getC_PaymentRequest_ID());  
	    if (emptyCount > 0)  
	        return "付款事项不能为空，确认明细付款事项。";  
	    return null;  
	}
	
	private String doComplete() { 
		approveLines();  
	    return null;  
	}

	@Override  
	protected boolean beforeSave(boolean newRecord) {  

	    return true;  
	}
	
    // 私有辅助方法 -------------------------------------------------------------------------  
	/** 完成时：将所有有效行的 ApprovedAmt 设为 RequestAmt，并同步主表 */  
	private void approveLines() {  
	    String sqlLines = "UPDATE C_PaymentRequestLine "  
	        + "SET ApprovedAmt = RequestAmt "  
	        + "WHERE C_PaymentRequest_ID = ? AND IsActive = 'Y'";  
	    int no = DB.executeUpdateEx(sqlLines,  
	        new Object[]{getC_PaymentRequest_ID()}, get_TrxName());  
	    if (log.isLoggable(Level.INFO))  
	        log.info("approveLines - updated " + no + " lines");  
	    syncApprovedAmt();  
	}  
	  
	/** 从行汇总重新计算主表 ApprovedAmt */  
	private void syncApprovedAmt() {  
	    String sql = "UPDATE C_PaymentRequest "  
	        + "SET ApprovedAmt = ("  
	        +     "SELECT COALESCE(SUM(ApprovedAmt), 0) "  
	        +     "FROM C_PaymentRequestLine "  
	        +     "WHERE C_PaymentRequest_ID = ? AND IsActive = 'Y') "  
	        + "WHERE C_PaymentRequest_ID = ?";  
	    int no = DB.executeUpdateEx(sql,  
	        new Object[]{getC_PaymentRequest_ID(), getC_PaymentRequest_ID()},  
	        get_TrxName());  
	    if (no != 1)  
	        log.log(Level.SEVERE, "syncApprovedAmt #" + no);  
	}

    
	// ── DocAction 接口方法全部委托 ────────────────────────────

	@Override
	public boolean processIt(String action) throws Exception {
		return docActionDelegate.processIt(action);
	}

	@Override
	public boolean unlockIt() {
		return docActionDelegate.unlockIt();
	}

	@Override
	public boolean invalidateIt() {
		return docActionDelegate.invalidateIt();
	}

	@Override
	public String prepareIt() {
		return docActionDelegate.prepareIt();
	}

	@Override
	public boolean approveIt() {
		return docActionDelegate.approveIt();
	}

	@Override
	public boolean rejectIt() {
		return docActionDelegate.rejectIt();
	}

	@Override
	public String completeIt() {
		return docActionDelegate.completeIt();
	}

	@Override
	public boolean voidIt() {
		return docActionDelegate.voidIt();
	}

	@Override
	public boolean closeIt() {
		return docActionDelegate.closeIt();
	}

	@Override
	public boolean reverseCorrectIt() {
		return docActionDelegate.reverseCorrectIt();
	}

	@Override
	public boolean reverseAccrualIt() {
		return docActionDelegate.reverseAccrualIt();
	}

	@Override
	public boolean reActivateIt() {
		return false;
	}

	@Override
	public File createPDF() {
		return docActionDelegate.createPDF();
	}

	@Override
	public String getProcessMsg() {
		return docActionDelegate.getProcessMsg();
	}

	@Override
	public int getC_Currency_ID() {
		return docActionDelegate.getC_Currency_ID();
	}

	@Override
	public String getDocAction() {
		return docActionDelegate.getDocAction();
	}

	@Override
	public void setDocStatus(String s) {
		docActionDelegate.setDocStatus(s);
	}

	@Override
	public String getDocStatus() {
		return docActionDelegate.getDocStatus();
	}

	@Override
	public String getSummary() {
		return getDocumentNo();
	}

	@Override
	public String getDocumentNo() {
		return get_ValueAsString(COLUMNNAME_DocumentNo);
	}

	@Override
	public String getDocumentInfo() {
		return getDocumentNo();
	}

	@Override
	public int getDoc_User_ID() {
		return getCreatedBy();
	}

	@Override
	public BigDecimal getApprovalAmt() {
		return null;
	}

	@Override
	public int customizeValidActions(String docStatus, Object processing, String orderType, String isSOTrx,
			int AD_Table_ID, String[] docAction, String[] options, int index) {
		// 从 options[0..index) 中移除 ACTION_Void
		int newIndex = 0;
		for (int i = 0; i < index; i++) {
			if (!DocumentEngine.ACTION_Void.equals(options[i])) {
				options[newIndex++] = options[i];
			}
		}
		return newIndex;
	}
}