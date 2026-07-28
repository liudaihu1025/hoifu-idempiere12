package org.libero.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.libero.tables.X_PP_Material_Requisition;
  
public class MPPMaterialRequisition extends X_PP_Material_Requisition implements DocAction, DocOptions {
	private static final long serialVersionUID = 1L;
  
	public MPPMaterialRequisition(Properties ctx, int PP_Material_Requisition_ID, String trxName) {
		super(ctx, PP_Material_Requisition_ID, trxName);
    }
    public MPPMaterialRequisition(Properties ctx, ResultSet rs, String trxName) 
	{
		super(ctx, rs, trxName);
	}
  
	protected boolean beforeSave(boolean newRecord) {
		if (getDocStatus() == null) {
            setDocStatus(MPPOrder.DOCSTATUS_Drafted);  
            setDocAction(MPPOrder.DOCACTION_Complete);  
        }  
		return true;
    }  
  
	public String completeIt() {
		if (!m_justPrepared) {
            String status = prepareIt();  
			if (!MPPOrder.DOCSTATUS_InProgress.equals(status))
				return status;
        }  
  
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return MPPOrder.DOCSTATUS_Invalid;

        // 2. 批量完成明细并扣库存  
		for (MPPCostCollector cc : getLines()) {
			if (!cc.processIt(MPPOrder.DOCACTION_Complete)) {
				throw new AdempiereException(cc.getProcessMsg());
            }  
            cc.saveEx(get_TrxName());  
        }  
  
        // 3. 更新主表状态  
		setProcessed(true);
        setDocStatus(MPPOrder.DOCSTATUS_Completed);  
        setDocAction(MPPOrder.DOCACTION_Close);  
		return MPPOrder.DOCSTATUS_Completed;
    }  
  
	public List<MPPCostCollector> getLines() {
		String where = "PP_Material_Requisition_ID=?";
		return new Query(getCtx(), MPPCostCollector.Table_Name, where, get_TrxName())
                .setParameters(getPP_Material_Requisition_ID())  
                .list();  
    }  
  
	public boolean processIt(String processAction) {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(processAction, getDocAction());
    }  
  
	public boolean unlockIt() {
		setProcessing(false);
		return true;
    }  
  
	public boolean invalidateIt() {
        setDocAction(MPPOrder.DOCACTION_Prepare);  
		return true;
    }  
  
	public String prepareIt() {
		// 同步关联明细状态为处理中
		for (MPPCostCollector cc : getLines()) {
			if (MPPOrder.DOCSTATUS_Drafted.equals(cc.getDocStatus())) {
				if (!cc.processIt(MPPOrder.DOCACTION_Prepare)) {
					throw new AdempiereException(cc.getProcessMsg());
				}
				cc.saveEx(get_TrxName());
			}
		}

		m_justPrepared = true;
		setDocAction(MPPOrder.DOCACTION_Complete);
		return MPPOrder.DOCSTATUS_InProgress;
    }  
  
	public boolean approveIt() {
		setIsApproved(true);
		return true;
    }  
  
	public boolean rejectIt() {
		setIsApproved(false);
		return true;
    }  
  
	@Override  
	public boolean voidIt() {  
	    log.info("voidIt - " + toString());  
	  
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);  
	    if (m_processMsg != null)  
	        return false;  
	  
	    if (MPPOrder.DOCSTATUS_Completed.equals(getDocStatus())  
	            || MPPOrder.DOCSTATUS_Closed.equals(getDocStatus())) {  
	        // 已完成/已关闭：CC 行已过账，需要走完整作废流程  
	        for (MPPCostCollector cc : getLines()) {  
	            if (MPPCostCollector.DOCSTATUS_Voided.equals(cc.getDocStatus()))  
	                continue;  
	            boolean wasPosted = cc.isPosted(); 
	            if (!cc.processIt(MPPCostCollector.DOCACTION_Void)) {  
	                m_processMsg = cc.getProcessMsg();  
	                return false;  
	            }  
	            cc.saveEx(get_TrxName());  
	         // 作废 CC 后，重新过账以冲销会计分录  
	            if (wasPosted) {  
	                String error = DocumentEngine.postImmediate(  
	                    cc.getCtx(), cc.getAD_Client_ID(),  
	                    MPPCostCollector.Table_ID, cc.get_ID(),  
	                    true, cc.get_TrxName()  // force=true 强制重新过账  
	                );  
	                if (!Util.isEmpty(error)) {  
	                    m_processMsg = "Re-post error: " + error;  
	                    return false;  
	                }  
	            }
	        }  
	    } else {  
	        // 草稿/进行中等：CC 行尚未完成，直接标记作废即可  
	        for (MPPCostCollector cc : getLines()) {  
	            if (MPPCostCollector.DOCSTATUS_Voided.equals(cc.getDocStatus()))  
	                continue;  
	            cc.setDocStatus(MPPCostCollector.DOCSTATUS_Voided);  
	            cc.setDocAction(MPPCostCollector.DOCACTION_None);  
	            cc.setProcessed(true);  
	            cc.saveEx(get_TrxName());  
	        }  
	    }  
	  
	    setProcessed(true);  
	    setDocStatus(MPPOrder.DOCSTATUS_Voided);  
	    setDocAction(MPPOrder.DOCACTION_None);  
	  
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);  
	    if (m_processMsg != null)  
	        return false;  
	  
	    return true;  
	}
  
	public boolean closeIt() {
        setDocAction(MPPOrder.DOCACTION_None);  
		return true;
    }  
  
	public boolean reverseCorrectIt() {
		return false;
    }  
  
	public boolean reverseAccrualIt() {
		return false;
    }  
  
	public boolean reActivateIt() {  
	    log.info("reActivateIt - " + toString());  
	  
	    // Before ReActivate  
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);  
	    if (m_processMsg != null)  
	        return false;  
	  
	    // 只有已作废的单据才能重新激活  
	    if (!MPPOrder.DOCSTATUS_Voided.equals(getDocStatus())) {  
	        m_processMsg = "已作废的单据才能重新激活!";  
	        return false;  
	    }  
	  
	    // 同步子表状态：将已作废的明细行重新激活  
	    for (MPPCostCollector cc : getLines()) {  
	        if (MPPCostCollector.DOCSTATUS_Voided.equals(cc.getDocStatus())) {  
	            if (!cc.processIt(DocumentEngine.ACTION_ReActivate)) {  
	                m_processMsg = cc.getProcessMsg();  
	                return false;  
	            }  
	            cc.saveEx(get_TrxName());  
	        }  
	    }  
	  
	    // 恢复主表状态  
	    setDocStatus(MPPOrder.DOCSTATUS_InProgress);  
	    setDocAction(MPPOrder.DOCACTION_Complete);  
	    setProcessed(false);  
	  
	    // After ReActivate  
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);  
	    if (m_processMsg != null)  
	        return false;  
	  
	    return true;  
	}
  
	public String getSummary() {
		return getDescription() != null ? getDescription() : "";
    }  
  
	public String getProcessMsg() {
		return m_processMsg;
    }  
  
	public int getDoc_User_ID() {
		return getCreatedBy();
    }  
  
	public int getC_Currency_ID() {
		return 0;
    }  
  
	public BigDecimal getApprovalAmt() {
		return Env.ZERO;
    }  
  
	public File createPDF() {
		try {
			File temp = File.createTempFile(get_TableName() + get_ID() + "_", ".pdf");
			return createPDF(temp);
		} catch (Exception e) {
			return null;
        }  
    }  
  
	public File createPDF(File file) {
		return null;
    }  
  
	public String getDocumentInfo() {
        MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());  
		return dt.getName() + " " + getDocumentNo();
    }  
  
	private String m_processMsg;
	private boolean m_justPrepared = false;

	 @Override  
	    public int customizeValidActions(String docStatus, Object processing,  
	            String orderType, String isSOTrx, int AD_Table_ID,  
	            String[] docAction, String[] options, int index) { 
	        if (DocumentEngine.STATUS_Drafted.equals(docStatus)  
	                || DocumentEngine.STATUS_InProgress.equals(docStatus)  
	                || DocumentEngine.STATUS_Invalid.equals(docStatus)) {  
	            // 从 options 中移除 ACTION_Void  
	            for (int i = 0; i < index; i++) {  
	                if (DocumentEngine.ACTION_Void.equals(options[i])) {  
	                    // 将后面的元素前移  
	                    for (int j = i; j < index - 1; j++) {  
	                        options[j] = options[j + 1];  
	                    }  
	                    options[index - 1] = null;  
	                    index--;  
	                    break;  
	                }  
	            }  
	        }  
	        if (DocumentEngine.STATUS_Completed.equals(docStatus)) {  
	            options[index++] = DocumentEngine.ACTION_Void;  
	        }
	        return index;  
	    }
}