package com.hoifu.model;  
  
import java.io.File;  
import java.math.BigDecimal;  
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;  
  
import org.adempiere.model.DocActionDelegate;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;  
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;  
  
public class MInOutRequisition extends X_M_InOut_Requisition implements DocAction, DocOptions {  
  
    private static final long serialVersionUID = 1L;  
  
    private DocActionDelegate<MInOutRequisition> docActionDelegate;

    public static final String IN_ORDER_IN = "ORDER_IN";
    public static final String IN_PP_MATERIA_IN = "PP_MATERIA_IN";
    public static final String IN_SUBCONTRAC_IN = "SUBCONTRAC_IN";
    public static final String IN_RMA_IN = "RMA_IN";
    public static final String IN_PP_IN = "PP_IN";
    public static final String IN_INV_IU_IN = "INV_IU_IN";
    public static final String IN_OTHER_IN = "OTHER_IN";

    public static final String OUT_INV_IU_OUT = "INV_IU_OUT";
    public static final String OUT_PP_MATERIA_OUT = "PP_MATERIA_OUT";
    public static final String OUT_ORDER_OUT = "ORDER_OUT";
    public static final String OUT_RMA_OUT = "RMA_OUT";
    public static final String OUT_OTHER_OUT = "OTHER_OUT";

    
    public MInOutRequisition(Properties ctx, int M_InOut_Requisition_ID, String trxName) {  
        super(ctx, M_InOut_Requisition_ID, trxName);  
        if (M_InOut_Requisition_ID == 0)  
            setInitialDefaults();  
        init();  
    }  
  
    public MInOutRequisition(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
        init();  
    }  
  
    private void setInitialDefaults() {  
        setProcessed(false);  

    }  
  
    private void init() {  
        docActionDelegate = new DocActionDelegate<>(this);  
        docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());  
        docActionDelegate.setActionCallable(DocAction.ACTION_ReActivate, () -> doReActivate());  
        // Completed 状态作废时，DocActionDelegate 会走 reverseCorrect/reverseAccrual 分支  
        docActionDelegate.setActionCallable(DocAction.ACTION_Reverse_Correct, () -> doVoid());  
        //docActionDelegate.setActionCallable(DocAction.ACTION_Reverse_Accrual, () -> doVoid());  

    } 
  
    @Override  
    protected boolean afterSave(boolean newRecord, boolean success) {  
        if (!success)  
            return false;  

        return true;  
    }  
  
    /**
     * 重新激活回调
     */
    private String doReActivate() {  
        return null;
    }
    
    /**
     * 单据完成回调
     * @return
     */
    private String doComplete() {  
        return null;  
    }  
  
    /**
     * 已完成的单据作废回调
     */
    private String doVoid() {  
        for (MInOutRequisitionLine line : getLines()) {  
            BigDecimal qtyGenerated = line.getQtyGenerated();  
            if (qtyGenerated != null && qtyGenerated.signum() > 0) {  
                return "第 " + line.getLine() + " 行已生成下游单据（QtyGenerated=" + qtyGenerated + "），不能自动作废，请人工处理";  
            }  
        }  
        return null;
    }
    
    
    // ── 辅助方法────────────────────────────  
    
    public List<MInOutRequisitionLine> getLines() {  
        List<MInOutRequisitionLine> list = new Query(getCtx(), MInOutRequisitionLine.Table_Name,  
                "M_InOut_Requisition_ID = ?", get_TrxName())  
                .setParameters(getM_InOut_Requisition_ID())  
                .setOrderBy("Line").list();  
        return list;  
    }  
  
    public int resolveInboundDeliveryType() {  
		String inOutType = getInOutType();  
		if (IN_ORDER_IN.equals(inOutType)) return 10;
		if (IN_PP_MATERIA_IN.equals(inOutType)) return 20;
		if (IN_SUBCONTRAC_IN.equals(inOutType)) return 30;
		if (IN_RMA_IN.equals(inOutType)) return 50;
		if (IN_PP_IN.equals(inOutType)) return 60;  
		if (IN_INV_IU_IN.equals(inOutType)) return 100;  
		return 90;  
	}  
  
	public int resolveOutboundDeliveryType() {  
		String inOutType = getInOutType();  
		if (OUT_INV_IU_OUT.equals(inOutType)) return 20;  
		if (OUT_PP_MATERIA_OUT.equals(inOutType)) return 30;  
		if (OUT_ORDER_OUT.equals(inOutType)) return 40;  
		if (OUT_RMA_OUT.equals(inOutType)) return 10;  
		return 90;  
	}  
  
	/**  
	 * 判断该申请单是否为出库方向。  
	 * 优先用 InOutType 判断：命中出库集合 -> true，命中入库集合 -> false；  
	 * InOutType 为空/不在已知集合内（脏数据、新增类型未维护）时，兜底用 IsSOTrx 判断方向。  
	 */  
	public  boolean isOutStock() {  
		String inOutType = getInOutType();  
  
		if (isOutboundType(inOutType)) {  
			return true;  
		}  
		if (isInboundType(inOutType)) {  
			return false;  
		}  
		// 未命中任何已知 InOutType，兜底用 IsSOTrx  
		return isSOTrx();  
	}  
	
	/**  
	 * 入库类型集合：ORDER_IN、PP_MATERIA_IN、SUBCONTRAC_IN、RMA_IN、PP_IN、INV_IU_IN、OTHER_IN
	 */  
	private static boolean isInboundType(String inOutType) {  
		return IN_ORDER_IN.equals(inOutType)  
				|| IN_PP_MATERIA_IN.equals(inOutType)  
				|| IN_SUBCONTRAC_IN.equals(inOutType)  
				|| IN_RMA_IN.equals(inOutType)  
				|| IN_PP_IN.equals(inOutType)  
				|| IN_PP_IN.equals(inOutType)  
				|| IN_OTHER_IN.equals(inOutType)  
				|| IN_INV_IU_IN.equals(inOutType);  
	}  
	  
	/**  
	 * 出库类型集合：INV_IU_OUT、PP_MATERIA_OUT、ORDER_OUT、RMA_OUT、OTHER_OUT
	 */  
	private static boolean isOutboundType(String inOutType) {  
		return OUT_INV_IU_OUT.equals(inOutType)  
				|| OUT_PP_MATERIA_OUT.equals(inOutType)  
				|| OUT_ORDER_OUT.equals(inOutType)  
				|| OUT_OTHER_OUT.equals(inOutType)  
				|| OUT_RMA_OUT.equals(inOutType);  
	}
	
	
	
    // ── DocAction 接口方法全部委托 ────────────────────────────  
  
    @Override  
    public boolean processIt(String action) {  
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
        return docActionDelegate.reActivateIt();  
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
        return getAD_User_ID();
    }  
  
    @Override  
    public BigDecimal getApprovalAmt() {  
        return null;  
    }  
  
    @Override  
    public int customizeValidActions(String docStatus, Object processing, String orderType, String isSOTrx,  
            int AD_Table_ID, String[] docAction, String[] options, int index) {  
        if (DocumentEngine.STATUS_Completed.equals(docStatus)  
                && DocumentEngine.canReactivateThisDocType(getC_DocType_ID())) {  
            options[index++] = DocumentEngine.ACTION_ReActivate;  
        }  
        if (DocumentEngine.STATUS_Completed.equals(docStatus)  
                && DocumentEngine.canReactivateThisDocType(getC_DocType_ID())) {  
            options[index++] = DocumentEngine.ACTION_Void;  
        }  
        if (DocumentEngine.STATUS_InProgress.equals(docStatus)  
                && DocumentEngine.canReactivateThisDocType(getC_DocType_ID())) {  
            options[index++] = DocumentEngine.ACTION_Unlock;  
        }  
        return index;  
    }
}