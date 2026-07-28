package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Timestamp;  
import java.util.ArrayList;  
import java.util.LinkedHashMap;  
import java.util.List;  
import java.util.Map;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MInvoice;  
import org.compiere.model.MPayment;  
import org.compiere.model.MPaymentAllocate;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.compiere.util.Util;  
  
import com.hoifu.model.MPaymentRequestLine;  
  
@org.adempiere.base.annotation.Process  
public class PaymentCreateByRequest extends SvrProcess {  
  
    public static final String COLUMNNAME_PaymentCategory = "PaymentCategory";  
  
    private int    p_C_BankAccount_ID = 0;  
    private String p_TenderType       = null;  
    private String p_PaymentCategory  = null;  // 保留参数，优先使用  
  
    private List<Integer> selectedLineIds = new ArrayList<>();  
  
    @Override  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (int i = 0; i < para.length; i++) {  
            String name = para[i].getParameterName();  
            if (para[i].getParameter() == null)  
                ;  
            else if (name.equals(MPayment.COLUMNNAME_C_BankAccount_ID))  
                p_C_BankAccount_ID = para[i].getParameterAsInt();  
            else if (name.equals(MPayment.COLUMNNAME_TenderType))  
                p_TenderType = (String) para[i].getParameter();  
            else if (name.equals(COLUMNNAME_PaymentCategory))  
                p_PaymentCategory = (String) para[i].getParameter();  
        }  
        loadSelectedRecords();  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        if (selectedLineIds.isEmpty())  
            throw new AdempiereException("未选中任何记录");  
  
        // ── 第一步：加载明细行，校验重复 ──────────────────────────  
        List<MPaymentRequestLine> lines = new ArrayList<>();  
        for (int lineId : selectedLineIds) {  
            MPaymentRequestLine line = new MPaymentRequestLine(getCtx(), lineId, get_TrxName());  
            if (line.getC_Payment_ID() != 0)  
                throw new AdempiereException("行 " + line.getLine() + " 已关联付款单 [" + line.getC_Payment_ID() + "]，不能重复创建");  
            if (line.getC_Invoice_ID() == 0)  
                throw new AdempiereException("行 " + line.getLine() + " 未关联应付单");  
            lines.add(line);  
        }  
  
        // ── 第二步：加载发票，校验往来单位/货币一致性 ─────────────  
        Map<Integer, MInvoice> invoiceMap = new LinkedHashMap<>();  
        int bPartnerId = 0;  
        int currencyId = 0;  
        for (MPaymentRequestLine line : lines) {  
            MInvoice invoice = new MInvoice(getCtx(), line.getC_Invoice_ID(), get_TrxName());  
            invoiceMap.put(line.getC_Invoice_ID(), invoice);  
            if (bPartnerId == 0) {  
                bPartnerId = invoice.getC_BPartner_ID();  
                currencyId = invoice.getC_Currency_ID();  
            } else if (bPartnerId != invoice.getC_BPartner_ID()) {  
                throw new AdempiereException("选中行的供应商不一致（行 " + line.getLine() + "），无法合并创建付款单");  
            }  
        }  
  
        // ── 第三步：汇总 ApprovedAmt ───────────────────────────────  
        BigDecimal totalAmt = Env.ZERO;  
        for (MPaymentRequestLine line : lines)  
            totalAmt = totalAmt.add(line.getApprovedAmt());  
  
        // ── 第四步：确定 PaymentCategory ──────────────────────────  
        // 优先使用参数值；参数为空时，取 ApprovedAmt 合计最大的付款申请单的 PaymentCategory  
        String paymentCategory = p_PaymentCategory;  
        if (Util.isEmpty(paymentCategory))  
            paymentCategory = getPaymentCategoryFromMaxRequest(lines);  
  
        // ── 第五步：创建付款单（草稿） ────────────────────────────  
        MPayment payment = new MPayment(getCtx(), 0, get_TrxName());  
        payment.setAD_Org_ID(lines.get(0).getAD_Org_ID());  
        payment.setC_BankAccount_ID(p_C_BankAccount_ID);  
        payment.setC_DocType_ID(false); // false = AP Payment（付款，非收款）  
        payment.setTenderType(p_TenderType != null ? p_TenderType : MPayment.TENDERTYPE_DirectDeposit);  
        payment.setC_BPartner_ID(bPartnerId);  
        payment.setC_Currency_ID(currencyId);  
        payment.setPayAmt(totalAmt);  
        payment.setDiscountAmt(Env.ZERO);  
        payment.setWriteOffAmt(Env.ZERO);  
        payment.setOverUnderAmt(Env.ZERO);  
        Timestamp now = new Timestamp(System.currentTimeMillis());  
        payment.setDateTrx(now);  
        payment.setDateAcct(now);  
  
        if (!Util.isEmpty(paymentCategory))  
            payment.set_ValueOfColumn(COLUMNNAME_PaymentCategory, paymentCategory);  
  
        // ── 第六步：单行直接关联发票 ──────────────────────────────  
        if (lines.size() == 1) {  
            MPaymentRequestLine line = lines.get(0);  
            MInvoice invoice = invoiceMap.get(line.getC_Invoice_ID());  
            payment.setC_Invoice_ID(line.getC_Invoice_ID());  
            payment.setDiscountAmt(line.getDiscountAmt());  
            BigDecimal overUnder = invoice.getOpenAmt()  
                    .subtract(line.getApprovedAmt())  
                    .subtract(line.getDiscountAmt());  
            payment.setOverUnderAmt(overUnder);  
        }  
  
        // ── 第七步：拼接 R_Info（直接从行读取，不再绕回发票） ─────  
        StringBuilder rInfoBuilder = new StringBuilder();  
        for (MPaymentRequestLine line : lines) {  
            String lineRInfo = line.get_ValueAsString("R_Info");  
            if (!Util.isEmpty(lineRInfo)) {  
                if (rInfoBuilder.length() > 0)  
                    rInfoBuilder.append("+");  
                rInfoBuilder.append(lineRInfo);  
            }  
        }  
        if (rInfoBuilder.length() > 0)  
            payment.setR_Info(rInfoBuilder.toString());  
  
        payment.saveEx();  
  
        // ── 第八步：多行创建 MPaymentAllocate ────────────────────  
        if (lines.size() > 1) {  
            for (MPaymentRequestLine line : lines) {  
                MInvoice invoice = invoiceMap.get(line.getC_Invoice_ID());  
                BigDecimal approvedAmt = line.getApprovedAmt();  
                BigDecimal discountAmt = line.getDiscountAmt();  
                BigDecimal openAmt     = invoice.getOpenAmt();  
                BigDecimal overUnder   = openAmt.subtract(approvedAmt).subtract(discountAmt);  
  
                MPaymentAllocate pa = new MPaymentAllocate(getCtx(), 0, get_TrxName());  
                pa.setC_Payment_ID(payment.getC_Payment_ID());  
                pa.setC_Invoice_ID(line.getC_Invoice_ID());  
                pa.setInvoiceAmt(openAmt);  
                pa.setAmount(approvedAmt);  
                pa.setDiscountAmt(discountAmt);  
                pa.setWriteOffAmt(Env.ZERO);  
                pa.setOverUnderAmt(overUnder);  
                pa.saveEx();  
            }  
        }  
  
        // ── 第九步：回填 C_Payment_ID 到明细行 ───────────────────  
        for (MPaymentRequestLine line : lines) {  
            line.setC_Payment_ID(payment.getC_Payment_ID());  
            line.saveEx();  
        }  
  
        addLog(payment.getC_Payment_ID(), now, totalAmt,  
                "@C_Payment_ID@ = " + payment.getDocumentNo(),  
                MPayment.Table_ID, payment.getC_Payment_ID());  
  
        return "@Created@ = 1, @C_Payment_ID@ = " + payment.getDocumentNo();  
    }  
  
    /**  
     * 当参数 PaymentCategory 为空时，按 C_PaymentRequest_ID 分组汇总 ApprovedAmt，  
     * 取合计最大的那张付款申请单的 PaymentCategory。  
     */  
    private String getPaymentCategoryFromMaxRequest(List<MPaymentRequestLine> lines) {  
        // 按 C_PaymentRequest_ID 分组汇总  
        Map<Integer, BigDecimal> requestAmtMap = new LinkedHashMap<>();  
        for (MPaymentRequestLine line : lines) {  
            int requestId = line.getC_PaymentRequest_ID();  
            BigDecimal current = requestAmtMap.getOrDefault(requestId, BigDecimal.ZERO);  
            requestAmtMap.put(requestId, current.add(line.getApprovedAmt()));  
        }  
  
        // 找出合计最大的 C_PaymentRequest_ID  
        int maxRequestId = -1;  
        BigDecimal maxAmt = BigDecimal.ZERO;  
        for (Map.Entry<Integer, BigDecimal> entry : requestAmtMap.entrySet()) {  
            if (entry.getValue().compareTo(maxAmt) > 0) {  
                maxAmt = entry.getValue();  
                maxRequestId = entry.getKey();  
            }  
        }  
  
        if (maxRequestId <= 0)  
            return null;  
  
        return DB.getSQLValueString(get_TrxName(),  
                "SELECT PaymentCategory FROM C_PaymentRequest WHERE C_PaymentRequest_ID = ?",  
                maxRequestId);  
    }  
  
    private void loadSelectedRecords() {  
        String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
            while (rs.next()) {  
                selectedLineIds.add(rs.getInt(1));  
            }  
        } catch (SQLException e) {  
            throw new AdempiereException("获取选中记录失败", e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }  
}