package com.hoifu.validator;

import java.util.logging.Level;  
  
import org.compiere.model.MClient;  
import org.compiere.model.MInvoice;   
import org.compiere.model.ModelValidationEngine;  
import org.compiere.model.ModelValidator;  
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;  
  
public class InvoiceReconciliationValidator implements ModelValidator {  
	
	private static final CLogger log = CLogger.getCLogger(InvoiceReconciliationValidator.class);
      
    private int m_AD_Client_ID = -1;  
      
    @Override  
    public void initialize(ModelValidationEngine engine, MClient client) {  
        if(client != null) {  
            m_AD_Client_ID = client.getAD_Client_ID();  
        }  
        // 注册应收/应付单表的模型变更事件  
        engine.addModelChange("C_Invoice", this);  
    }  
      
    @Override  
    public int getAD_Client_ID() {  
        return m_AD_Client_ID;  
    }  
      
    @Override  
    public String modelChange(PO po, int type) throws Exception {  
        // 监听应收/应付单的状态变更  
        if (type == ModelValidator.TYPE_AFTER_CHANGE && po instanceof MInvoice) {  
            MInvoice invoice = (MInvoice) po;  
            String docStatus = (String) po.get_Value("DocStatus");  
            String oldDocStatus = (String) po.get_ValueOld("DocStatus");  
              
            // 判定单据状态是否从非完成变为完成  
            if (!"CO".equals(oldDocStatus) && "CO".equals(docStatus)) {  
                return updateReconciliationLines(invoice);  
            }  
        }  
        return null;  
    }  
      
    @Override  
    public String docValidate(PO po, int timing) {  
        return null; // 不使用文档验证  
    }  
      
    /**  
     * 更新对账明细表的发票明细ID字段  
     */  
    private String updateReconciliationLines(MInvoice invoice) {  
        try {  
            //批量更新,一次性更新所有相关记录  
            String updateSql = "UPDATE C_ReconciliationLine rl " +  
                              "SET C_InvoiceLine_ID = il.C_InvoiceLine_ID " +  
                              "FROM C_InvoiceLine il " +  
                              "WHERE il.C_Invoice_ID = ? " +  
                              "AND rl.C_ReconciliationLine_ID = il.C_ReconciliationLine_ID";  
              
            int no = DB.executeUpdateEx(updateSql,   
                new Object[]{invoice.getC_Invoice_ID()},   
                invoice.get_TrxName());  
              
            if (log.isLoggable(Level.FINE)) log.fine("Updated " + no + " reconciliation lines");  
            return null;  
        } catch (Exception e) {  
            return e.getMessage();  
        }  
    }   
      
    @Override  
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {  
        return null;  
    }  
}
