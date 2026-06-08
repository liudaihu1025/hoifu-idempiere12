package com.hoifu.validator;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MClient;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPayment;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class TimeExpenseValidator implements ModelValidator {  
    
    private int m_AD_Client_ID = -1;  
      
    @Override  
    public void initialize(ModelValidationEngine engine, MClient client) {  
    	if(client != null) {
    		m_AD_Client_ID = client.getAD_Client_ID();
    	}
        // 注册费用单表的模型变更事件
        engine.addModelChange("S_TimeExpense", this);  
    }  
      
    @Override  
    public int getAD_Client_ID() {  
        return m_AD_Client_ID;  
    }  
      
    @Override  
    public String modelChange(PO po, int type) throws Exception {  
        //监听OA生成的费用单
        if (type == ModelValidator.TYPE_AFTER_CHANGE && po instanceof MTimeExpense) { 
            String docStatus = (String) po.get_Value("DocStatus");  
            String oldDocStatus = (String) po.get_ValueOld("DocStatus");  
            //判定设置对账单单据状态是否为完成
            if (!"CO".equals(oldDocStatus) && "CO".equals(docStatus)) { 
            	MTimeExpense expense = (MTimeExpense) po; 
                return processExpenseReport(expense);
            }  
//            MTimeExpense expense = (MTimeExpense) po;  
//            return processExpenseReport(expense);  
        }  
        return null;  
    }  
      
    @Override  
    public String docValidate(PO po, int timing) {  
        return null; // 不使用文档验证  
    }  
      
    private String processExpenseReport(MTimeExpense expense) {  
        try {  
            // 检查单据状态是否为已完成  
            if (!DocAction.STATUS_Completed.equals(expense.getDocStatus())) {  
                return null;  
            }  
              
            // 查询name=API_OA的用户ID  
            int apiOAUserId = getAPIOAUserId(expense.getCtx());  
            if (apiOAUserId == -1) {  
                return "未找到name=API_OA的用户";  
            }  
              
            // 检查创建人是否为API_OA  
            if (expense.getCreatedBy() != apiOAUserId) {  
                return null;  
            }  
              
            // a. 生成已完成状态的应付单  
            MInvoice invoice = createAPInvoice(expense);  
            if (invoice == null) {  
                return "生成应付单失败";  
            }  
              
            // b. 生成草稿状态的付款单  
            MPayment payment = createPayment(invoice);  
            if (payment == null) {  
                return "生成付款单失败";  
            }  
              
            return null; // 成功  
        } catch (Exception e) {  
            return e.getMessage();  
        }  
    }  
      
    /**  
     * 查询name=API_OA的用户ID  
     */  
    private int getAPIOAUserId(Properties ctx) {  
        String sql = "SELECT AD_User_ID FROM AD_User WHERE Name = ? AND AD_Client_ID = ?";  
        return DB.getSQLValue(null, sql, "API_OA", Env.getAD_Client_ID(ctx));  
    }  
      
    /**  
     * 获取AP发票文档类型  
     */  
    private int getAPInvoiceDocType(Properties ctx) {   
    	String sql = "SELECT C_DocType_ID FROM C_DocType " +  
                     "WHERE DocBaseType = 'API' AND IsActive = 'Y' AND  AD_Client_ID = ? " +  
                     "ORDER BY IsDefault DESC LIMIT 1";  
        return DB.getSQLValue(null, sql, Env.getAD_Client_ID(ctx));  
    }  
      
    /**  
     * 根据费用单生成应付单（已完成状态）  
     */  
    private MInvoice createAPInvoice(MTimeExpense expense) {  
        MInvoice invoice = new MInvoice(expense.getCtx(), 0, expense.get_TrxName());  
          
        // 设置基本字段  
        invoice.setClientOrg(expense.getAD_Client_ID(), expense.getAD_Org_ID());  
        invoice.setC_BPartner_ID(expense.getC_BPartner_ID());  
        invoice.setDateAcct(expense.getDateReport());  
        invoice.setDateInvoiced(expense.getDateReport());
        
        // 设置付款事项字段 
        String rInfoValue = expense.getDocumentNo() + "(费用单号),OA审批通过后自动创建" ;  
        invoice.set_ValueOfColumn("R_Info", rInfoValue);
          
        // 设置文档类型和状态  
        invoice.setC_DocTypeTarget_ID(getAPInvoiceDocType(expense.getCtx()));  
        invoice.setDocStatus(DocAction.STATUS_Drafted);  
        invoice.setDocAction(DocAction.ACTION_Complete);  
        invoice.setIsSOTrx(false); // AP Invoice  
          
        // 设置价格表和货币  
        invoice.setM_PriceList_ID(expense.getM_PriceList_ID());  
        invoice.setC_PaymentTerm_ID(getDefaultPaymentTerm(expense.getCtx()));  
          
        // 设置销售代表  
        invoice.setSalesRep_ID(expense.getCreatedBy());  
          
        // 设置标志字段  
        invoice.setIsTaxIncluded(false);  
        invoice.setIsApproved(false);  
        invoice.setProcessed(false);  
          
        // 初始化金额  
        invoice.setChargeAmt(Env.ZERO);  
        invoice.setTotalLines(Env.ZERO);  
        invoice.setGrandTotal(Env.ZERO);  
          
        // 先保存发票头  
        invoice.saveEx();  
          
        // 复制费用单行到应付单行  
        MTimeExpenseLine[] lines = expense.getLines(false);  
        BigDecimal totalLines = Env.ZERO;  
        int validLines = 0;  
          
        for (MTimeExpenseLine line : lines) {  
            // 跳过已开票的行  
            if (line.getC_InvoiceLine_ID() != 0) {  
                continue;  
            }  
              
            // 检查报销金额 - 使用表中的字段  
            BigDecimal qtyReimbursed = line.getQtyReimbursed();  
            BigDecimal priceReimbursed = line.getPriceReimbursed();  
              
            if (qtyReimbursed == null || priceReimbursed == null ||  
                qtyReimbursed.signum() == 0 || priceReimbursed.signum() == 0) {  
                continue;  
            }  
              
            MInvoiceLine invoiceLine = new MInvoiceLine(invoice);  
            invoiceLine.setLine(line.getLine());  
              
            // 设置产品  
            if (line.getM_Product_ID() != 0) {  
                invoiceLine.setM_Product_ID(line.getM_Product_ID(), true);  
            }  
              
            // 设置数量和价格 - 确保不为0  
            BigDecimal qty = qtyReimbursed.signum() == 0 ? Env.ONE : qtyReimbursed;  
            BigDecimal price = priceReimbursed.signum() == 0 ? Env.ONE : priceReimbursed;  
              
            invoiceLine.setQtyEntered(qty);  
            invoiceLine.setQtyInvoiced(qty);  
            invoiceLine.setPrice(price);  
            invoiceLine.setDescription(line.getDescription());  
              
            // 设置项目维度  
            invoiceLine.setC_Project_ID(line.getC_Project_ID());  
            invoiceLine.setC_Activity_ID(line.getC_Activity_ID());  
            invoiceLine.setC_Campaign_ID(line.getC_Campaign_ID());  
              
            // 设置税  
            invoiceLine.setTax();  
            invoiceLine.saveEx();  
              
            totalLines = totalLines.add(qty.multiply(price));  
            validLines++;  
        }  
          
        // 检查是否有有效行  
        if (validLines == 0) {  
            throw new AdempiereException("费用单没有有效的报销明细行");  
        }  
          
        // 设置最终金额  
        invoice.setTotalLines(totalLines);  
        invoice.setGrandTotal(totalLines);  
        invoice.saveEx();  
          
        // 完成应付单  
        invoice.setDocAction(DocAction.ACTION_Complete);  
        if (!invoice.processIt(DocAction.ACTION_Complete)) {  
            throw new AdempiereException("完成应付单失败: " + invoice.getProcessMsg());  
        }  
        invoice.saveEx();  
          
        return invoice;  
    }  
      
    /**  
     * 根据应付单生成付款单（草稿状态）  
     */  
    private MPayment createPayment(MInvoice invoice) {  
        MPayment payment = new MPayment(invoice.getCtx(), 0, invoice.get_TrxName());  
          
        // 查找默认银行账户  
        MBankAccount bankAccount = getDefaultBankAccount(invoice.getCtx(), invoice.getAD_Org_ID());  
        if (bankAccount == null) {  
            throw new AdempiereException("未找到组织默认银行账户");  
        }  
          
        // 获取AP付款单据类型  
        int docTypeID = getAPPaymentDocType(invoice.getCtx());  
        if (docTypeID == -1) {  
            throw new AdempiereException("未找到AP付款单据类型");  
        }  
          
        // 设置付款单字段  
        payment.setAD_Org_ID(invoice.getAD_Org_ID());  
        payment.setC_BankAccount_ID(bankAccount.getC_BankAccount_ID());  
        payment.setC_BPartner_ID(invoice.getC_BPartner_ID());  
        payment.setC_Invoice_ID(invoice.getC_Invoice_ID());  
        payment.setC_Currency_ID(invoice.getC_Currency_ID());  
        payment.setC_DocType_ID(docTypeID);  
        payment.setPayAmt(invoice.getGrandTotal());  
        payment.setTenderType(MPayment.TENDERTYPE_Check);  
        payment.setDateAcct(invoice.getDateAcct());  
        payment.setDateTrx(invoice.getDateInvoiced());  
        payment.setDocStatus(DocAction.STATUS_Drafted);  
        payment.setDocAction(DocAction.ACTION_Complete); 
        
        // 设置付款事项字段，从应付单复制  
        payment.setR_Info((String) invoice.get_Value("R_Info")); 
        
        // 设置付款类别为'E'(费用报销)
        payment.set_ValueOfColumn("PaymentCategory", "E");  
          
        payment.saveEx();  
        return payment;  
    }  
    
    /**
     * 查找默认付款条款
     */
    private int getDefaultPaymentTerm(Properties ctx) {  
        String sql = "SELECT C_PaymentTerm_ID FROM C_PaymentTerm " +  
                     "WHERE IsActive = 'Y' AND AD_Client_ID = ? " + 
        		      "ORDER BY IsDefault DESC LIMIT 1";  
        return DB.getSQLValue(null, sql, Env.getAD_Client_ID(ctx));  
    }  
    
    /**
     * 查找默认银行账户
     */
    private MBankAccount getDefaultBankAccount(Properties ctx, int adOrgId) {  
        String sql = "SELECT C_BankAccount_ID FROM C_BankAccount WHERE AD_Org_ID=? AND IsActive='Y' LIMIT 1";  
        int bankAccountId = DB.getSQLValue(null, sql, adOrgId);  
        if (bankAccountId > 0) {  
            return new MBankAccount(ctx, bankAccountId, null);  
        }  
        return null; 
    }  
    
    /**
     * 获取AP付款单据类型 
     */
    private int getAPPaymentDocType(Properties ctx) {  
        String sql = "SELECT C_DocType_ID FROM C_DocType " +  
                     "WHERE DocBaseType = 'APP' AND IsActive = 'Y' AND AD_Client_ID = ? " +  
                     "ORDER BY IsDefault DESC LIMIT 1";  
        return DB.getSQLValue(null, sql, Env.getAD_Client_ID(ctx));  
    }  

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		return null;
	}  
}