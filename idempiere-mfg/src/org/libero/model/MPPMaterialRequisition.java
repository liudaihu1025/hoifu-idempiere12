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
import org.compiere.process.DocumentEngine;
import org.compiere.util.Env;
import org.libero.tables.X_PP_Material_Requisition;
  
public class MPPMaterialRequisition extends X_PP_Material_Requisition implements DocAction {
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
  
        // 1. 库存可用性检查（可选）  
//		for (MPPCostCollector cc : getLines()) {
//			// 生产退料不需要检查库存
//			if (MPPCostCollector.COSTCOLLECTORTYPE_ProductionReturn.equals(cc.getCostCollectorType())) {
//				continue;
//			}
//
//			if (!MPPOrder.isQtyAvailable(cc.getPP_Order(), cc.getPP_Order_BOMLine())) {
//				throw new AdempiereException("库存不足: " + cc.getM_Product().getName());
//            }  
//        }  
//  
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
  
	public boolean voidIt() {
		return false;
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
        setDocAction(MPPOrder.DOCACTION_Complete);  
		setProcessed(false);
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
}