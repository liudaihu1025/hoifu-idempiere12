package com.hoifu.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.model.DocActionDelegate;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;

public class MLogistics extends X_M_Logistics implements DocAction , DocOptions {

	private static final long serialVersionUID = 1L;

	private DocActionDelegate<MLogistics> docActionDelegate;

	public MLogistics(Properties ctx, int M_Logistics_ID, String trxName) {
		super(ctx, M_Logistics_ID, trxName);
		if (M_Logistics_ID == 0)
			setInitialDefaults();
		init();
	}

	public MLogistics(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		init();
	}

	private void setInitialDefaults() {
		setProcessed(false);
	}

	private void init() {
		docActionDelegate = new DocActionDelegate<>(this);
		docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		return true;
	}

	private String doComplete() {  
	    setLogisticsStatus("Shipped");
		markInOutsAsDelivered(); 
	    return null;  
	}

	@Override  
	protected boolean beforeSave(boolean newRecord) {  
		calculateTotalPrice();
	    return true;  
	}
	
    // 私有辅助方法 -------------------------------------------------------------------------  
	
    /** TotalPrice = FreightCharges + Surcharges */  
    private void calculateTotalPrice() {  
        BigDecimal freight   = getFreightCharges();  
        BigDecimal surcharge = getSurcharges();  
        if (freight   == null) freight   = BigDecimal.ZERO;  
        if (surcharge == null) surcharge = BigDecimal.ZERO;  
	    setTotalPrice(freight.add(surcharge));  
    }  
  
    /**  
     * 单据完成时，将此物流单明细关联的所有发货单标记为已发货（IsDelivered='Y'）。  
     */  
    private void markInOutsAsDelivered() {  
        int updated = DB.executeUpdateEx(  
                "UPDATE M_InOut SET IsDelivered='Y' "  
                + "WHERE M_InOut_ID IN ("  
                + "  SELECT DISTINCT M_InOut_ID FROM M_LogisticsLine "  
                + "  WHERE M_Logistics_ID=? AND IsActive='Y'"  
                + ")",  
                new Object[]{ getM_Logistics_ID() },  
                get_TrxName());  
  
        if (log.isLoggable(Level.INFO))  
            log.info("IsDelivered updated: " + updated + " M_InOut(s)");  
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