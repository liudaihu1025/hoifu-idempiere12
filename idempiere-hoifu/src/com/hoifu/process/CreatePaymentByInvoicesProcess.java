package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Timestamp;  
import java.util.ArrayList;  
import java.util.List;  
  
import org.compiere.model.MBankAccount;  
import org.compiere.model.MDocType;  
import org.compiere.model.MInvoice;  
import org.compiere.model.MPayment;  
import org.compiere.model.MPaymentAllocate;  
import org.compiere.model.MProcessPara;  
import org.compiere.model.Query;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.AdempiereUserError;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
@org.adempiere.base.annotation.Process  
public class CreatePaymentByInvoicesProcess extends SvrProcess {  
  
    private int    p_C_BankAccount_ID = 0;  
    private String p_TenderType       = null;  
    private String p_PaymentCategory  = null;  
  
    private final List<Integer> selectedInvoiceIds = new ArrayList<>();  
  
    @Override  
    protected void prepare() {  
        for (ProcessInfoParameter p : getParameter()) {  
            String name = p.getParameterName();  
            if (p.getParameter() == null)  
                ;  
            else if (name.equals("C_BankAccount_ID"))  
                p_C_BankAccount_ID = p.getParameterAsInt();  
            else if (name.equals("TenderType"))  
                p_TenderType = (String) p.getParameter();  
            else if (name.equals("PaymentCategory"))  
                p_PaymentCategory = (String) p.getParameter();  
            else  
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);  
        }  
        loadSelectedLines();  
    }  
  
    private void loadSelectedLines() {  
        String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
            while (rs.next()) {  
                selectedInvoiceIds.add(rs.getInt(1));  
            }  
        } catch (SQLException e) {  
            throw new IllegalArgumentException(e.getMessage());  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        if (p_C_BankAccount_ID == 0)  
            throw new AdempiereUserError("@FillMandatory@ @C_BankAccount_ID@");  
        if (p_TenderType == null || p_TenderType.isEmpty())  
            throw new AdempiereUserError("@FillMandatory@ TenderType");  
        if (selectedInvoiceIds.isEmpty())  
            throw new AdempiereUserError("@NoSelection@");  
  
        MBankAccount ba = MBankAccount.get(getCtx(), p_C_BankAccount_ID);  
        if (ba == null || ba.getC_BankAccount_ID() != p_C_BankAccount_ID)  
            throw new AdempiereUserError("@NotFound@ @C_BankAccount_ID@");  
        int bankCurrencyId = ba.getC_Currency_ID();  
  
        List<MInvoice> invoices = new ArrayList<>();  
        int     commonBPartnerId = 0;  
        Boolean commonIsSOTrx   = null;  
  
        StringBuilder rInfoBuilder = new StringBuilder();  
        StringBuilder descBuilder  = new StringBuilder();  
  
        for (int invoiceId : selectedInvoiceIds) {  
            MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());  
            if (invoice.getC_Invoice_ID() != invoiceId)  
                throw new AdempiereUserError("@NotFound@ @C_Invoice_ID@ " + invoiceId);  
  
            // 根据 isSOTrx 确定单据名称，用于提示  
            String invoiceTypeName = invoice.isSOTrx() ? "应收单" : "应付单";  
  
            if (invoice.getC_Currency_ID() != bankCurrencyId)  
                throw new AdempiereUserError(  
                    invoiceTypeName + " " + invoice.getDocumentNo() + " 的货币与银行账户货币不一致");  
  
            if (commonBPartnerId == 0)  
                commonBPartnerId = invoice.getC_BPartner_ID();  
            else if (commonBPartnerId != invoice.getC_BPartner_ID())  
                throw new AdempiereUserError("选中的" + invoiceTypeName + "业务伙伴不一致");  
  
            if (commonIsSOTrx == null)  
                commonIsSOTrx = invoice.isSOTrx();  
            else if (!commonIsSOTrx.equals(invoice.isSOTrx()))  
                throw new AdempiereUserError("选中单据中应收单与应付单不能混合");  
  
            Object rInfoVal = invoice.get_Value("R_Info");  
            if (rInfoVal != null && !rInfoVal.toString().isEmpty()) {  
                if (rInfoBuilder.length() > 0) rInfoBuilder.append("、");  
                rInfoBuilder.append(rInfoVal.toString());  
            }  
  
            String desc = invoice.getDescription();  
            if (desc != null && !desc.isEmpty()) {  
                if (descBuilder.length() > 0) descBuilder.append("、");  
                descBuilder.append(desc);  
            }  
  
            invoices.add(invoice);  
        }  
  
        // 根据 isSOTrx 确定单据名称  
        String invoiceTypeName = commonIsSOTrx ? "应收单" : "应付单";  
        String paymentTypeName = commonIsSOTrx ? "收款单" : "付款单";  
  
        BigDecimal totalPayAmt = Env.ZERO;  
        for (MInvoice invoice : invoices)  
            totalPayAmt = totalPayAmt.add(invoice.getOpenAmt());  
  
        if (totalPayAmt.signum() == 0)  
            throw new AdempiereUserError("所有选中" + invoiceTypeName + "的未付金额合计为零");  
  
        // 按名称查找单据类型  
        String doctypeName = paymentTypeName; // "收款单" 或 "付款单"  
        MDocType doctype = new Query(getCtx(), MDocType.Table_Name,  
                "Name=? AND IsActive='Y' AND AD_Client_ID=?", get_TrxName())  
                .setParameters(doctypeName, getAD_Client_ID())  
                .first();  
        if (doctype == null)  
            throw new AdempiereUserError("找不到单据类型: " + doctypeName);  
  
        Timestamp now = new Timestamp(System.currentTimeMillis());  
  
        MPayment payment = new MPayment(getCtx(), 0, get_TrxName());  
        payment.setAD_Org_ID(ba.getAD_Org_ID());  
        payment.setC_BankAccount_ID(p_C_BankAccount_ID);  
        payment.setTenderType(p_TenderType);  
        payment.setC_DocType_ID(doctype.getC_DocType_ID());  
        payment.setC_BPartner_ID(commonBPartnerId);  
        payment.setC_Currency_ID(bankCurrencyId);  
        payment.setPayAmt(totalPayAmt);  
        payment.setDateTrx(now);  
        payment.setDateAcct(now);  
        // 不设置 C_Invoice_ID，使用 C_PaymentAllocate 关联多张单据  
  
        if (rInfoBuilder.length() > 0)  
            payment.set_ValueOfColumn("R_Info", rInfoBuilder.toString());  
        if (descBuilder.length() > 0)  
            payment.setDescription(descBuilder.toString());  
        if (p_PaymentCategory != null && !p_PaymentCategory.isEmpty())  
            payment.set_ValueOfColumn("PaymentCategory", p_PaymentCategory);  
  
        payment.saveEx();  
  
        for (MInvoice invoice : invoices) {  
            BigDecimal openAmt = invoice.getOpenAmt();  
            MPaymentAllocate pa = new MPaymentAllocate(getCtx(), 0, get_TrxName());  
            pa.setC_Payment_ID(payment.getC_Payment_ID());  
            pa.setC_Invoice_ID(invoice.getC_Invoice_ID());  
            pa.setInvoiceAmt(openAmt);  
            pa.setAmount(openAmt);  
            pa.setDiscountAmt(Env.ZERO);  
            pa.setWriteOffAmt(Env.ZERO);  
            pa.setOverUnderAmt(Env.ZERO);  
            pa.saveEx();  
        }  
  
        // 输出可穿透到收款单/付款单窗口的链接  
        addLog(payment.getC_Payment_ID(), null, null,  
        		paymentTypeName + ": "+ payment.getDocumentNo(), MPayment.Table_ID, payment.getC_Payment_ID());  
  
        return paymentTypeName + " " + payment.getDocumentNo() + " 创建成功，共关联 "  
               + invoices.size() + " 条" + invoiceTypeName;  
    }  
}