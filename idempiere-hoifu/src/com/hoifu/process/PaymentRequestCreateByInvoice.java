package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Timestamp;  
import java.util.ArrayList;  
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MInvoice;  
import org.compiere.model.MProcessPara;  
import org.compiere.process.DocAction;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
import com.hoifu.model.MPaymentRequest;  
import com.hoifu.model.MPaymentRequestLine;  
  
@org.adempiere.base.annotation.Process  
public class PaymentRequestCreateByInvoice extends SvrProcess {  
  
    // 付款申请单单据类型 UUID  
    private static final String PaymentRequest_DOCTYPE_UU = "91619d7c-412f-4f21-a477-74bd1960fd34";  
  
    // 自定义字段名常量  
    private static final String COLUMNNAME_Rebate      = "Rebate";  
    private static final String COLUMNNAME_DiscountAmt = "DiscountAmt";  
    private static final String COLUMNNAME_R_Info      = "R_Info";  
    private static final String COLUMNNAME_PayAmt      = "PayAmt";  
  
    /** 流程参数 */  
    private String    p_PaymentCategory = null;  
    private Timestamp p_DatePromised    = null;  
    private String    p_PaymentMonth    = null;  
    private String    p_Description    = null;  
  
    /** 选中的应付单ID列表 */  
    private List<Integer> selectedInvoiceIds = new ArrayList<>();  
  
    /** 用户在信息窗口中填写的申请金额 Map: C_Invoice_ID -> RequestAmt */  
    private Map<Integer, BigDecimal> requestAmtMap = new HashMap<>();  
    /** 用户在信息窗口中填写的申请事项 Map: C_Invoice_ID -> R_Info */  
    private Map<Integer, String> rInfoMap = new HashMap<>();
  
    @Override  
    protected void prepare() {  
        for (ProcessInfoParameter para : getParameter()) {  
            String name = para.getParameterName();  
            if (para.getParameter() == null)  
                ;  
            else if ("PaymentCategory".equals(name))  
                p_PaymentCategory = para.getParameterAsString();  
            else if ("DatePromised".equals(name))  
                p_DatePromised = para.getParameterAsTimestamp();  
            else if ("paymentmonth".equals(name))  
                p_PaymentMonth = para.getParameterAsString();
            else if ("Description".equals(name))  
                p_Description = para.getParameterAsString();  
            else  
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        // 1. 加载选中的应付单ID  
        loadSelectedRecords();  
        if (selectedInvoiceIds.isEmpty()) {  
            throw new AdempiereException("@NoSelection@");  
        }  
  
        // 2. 加载用户在信息窗口中填写
        loadInfoWindowValues();  
  
        // 3. 加载并校验应付单（状态、往来单位、货币）  
        List<MInvoice> invoices = loadAndValidateInvoices();  
  
        // 4. 校验 RequestAmt 不能为0，且不过度申请  
        validateRequestAmts(invoices);  
  
        // 5. 创建付款申请单  
        MPaymentRequest request = createPaymentRequest(invoices);  
  
        addBufferLog(request.getC_PaymentRequest_ID(), null, null,  
                "@Created@ " + request.getDocumentNo(),  
                MPaymentRequest.Table_ID, request.getC_PaymentRequest_ID());  
  
        return "@Created@ " + request.getDocumentNo();  
    }  
  
    // ── 1. 加载选中记录 ──────────────────────────────────────────────────────  
  
    private void loadSelectedRecords() {  
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
            throw new AdempiereException("获取选中记录失败", e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }  
  
    // ── 2. 加载用户填写的 RequestAmt ─────────────────────────────────────────  
  
    private void loadRequestAmts() {  
        String sql = "SELECT T_Selection_ID, VALUE_NUMBER "  
                   + "FROM T_Selection_InfoWindow "  
                   + "WHERE AD_PInstance_ID = ? AND COLUMNNAME = 'RequestAmt'";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
            while (rs.next()) {  
                int invoiceId = rs.getInt(1);  
                BigDecimal amt = rs.getBigDecimal(2);  
                if (amt != null) {  
                    requestAmtMap.put(invoiceId, amt);  
                }  
            }  
        } catch (SQLException e) {  
            throw new AdempiereException("获取申请金额失败", e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }  
    
    private void loadInfoWindowValues() {  
        String sql = "SELECT T_Selection_ID, COLUMNNAME, VALUE_NUMBER, VALUE_STRING "  
                   + "FROM T_Selection_InfoWindow "  
                   + "WHERE AD_PInstance_ID = ? AND COLUMNNAME IN ('RequestAmt', 'R_Info')";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
            while (rs.next()) {  
                int invoiceId = rs.getInt("T_Selection_ID");  
                String columnName = rs.getString("COLUMNNAME");  
                if ("RequestAmt".equals(columnName)) {  
                    BigDecimal amt = rs.getBigDecimal("VALUE_NUMBER");  
                    if (amt != null)  
                        requestAmtMap.put(invoiceId, amt);  
                } else if ("R_Info".equals(columnName)) {  
                    String rInfo = rs.getString("VALUE_STRING");  
                    if (rInfo != null)  
                        rInfoMap.put(invoiceId, rInfo);  
                }  
            }  
        } catch (SQLException e) {  
            throw new AdempiereException("获取信息窗口数据失败", e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }
  
    // ── 3. 加载并校验应付单 ───────────────────────────────────────────────────  
  
    private List<MInvoice> loadAndValidateInvoices() {  
        List<MInvoice> invoices = new ArrayList<>();  
        int firstBPartnerId = -1;  
        int firstCurrencyId = -1;  
  
        for (int invoiceId : selectedInvoiceIds) {  
            MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());  
  
            // 校验：应付单（非销售）  
            if (invoice.isSOTrx()) {  
                throw new AdempiereException("[" + invoice.getDocumentNo() + "] 不是应付单");  
            }  
  
            // 校验：已完成（CO）或已关闭（CL）  
            String docStatus = invoice.getDocStatus();  
            if (!DocAction.STATUS_Completed.equals(docStatus)  
                    && !DocAction.STATUS_Closed.equals(docStatus)) {  
                throw new AdempiereException(  
                        "应付单 [" + invoice.getDocumentNo() + "] 状态不是已完成，无法申请付款");  
            }  
  
            // 校验：未结清  
            if (invoice.isPaid()) {  
                throw new AdempiereException(  
                        "应付单 [" + invoice.getDocumentNo() + "] 已结清，无法申请付款");  
            }  
  
            // 校验：往来单位一致  
            if (firstBPartnerId == -1) {  
                firstBPartnerId = invoice.getC_BPartner_ID();  
            } else if (invoice.getC_BPartner_ID() != firstBPartnerId) {  
                throw new AdempiereException("选中的应付单往来单位不一致，请选择相同往来单位的应付单");  
            }  
  
            // 校验：货币一致  
            if (firstCurrencyId == -1) {  
                firstCurrencyId = invoice.getC_Currency_ID();  
            } else if (invoice.getC_Currency_ID() != firstCurrencyId) {  
                throw new AdempiereException("选中的应付单货币不一致，请选择相同货币的应付单");  
            }  
  
            invoices.add(invoice);  
        }  
  
        return invoices;  
    }  
  
    // ── 4. 校验申请金额 ───────────────────────────────────────────────────────  
  
    private void validateRequestAmts(List<MInvoice> invoices) {  
        for (MInvoice invoice : invoices) {  
            int invoiceId = invoice.getC_Invoice_ID();  
            BigDecimal requestAmt = requestAmtMap.get(invoiceId);  
  
            // 校验：RequestAmt 不能为空或 <= 0  
            if (requestAmt == null || requestAmt.compareTo(BigDecimal.ZERO) <= 0) {  
                throw new AdempiereException(  
                        "应付单 [" + invoice.getDocumentNo() + "] 的申请金额不能为空或0");  
            }  
  
            if (rInfoMap.get(invoiceId) == null || rInfoMap.get(invoiceId).isBlank())  
                throw new AdempiereException("应付单 [" + invoice.getDocumentNo() + "] 的付款事项不能为空");
            
            // 校验：不过度申请（RequestAmt + 已申请金额 <= PayAmt）  
            BigDecimal requestedAmt = getAlreadyRequestedAmt(invoiceId);  
            BigDecimal payAmt = nullSafe((BigDecimal) invoice.get_Value(COLUMNNAME_PayAmt));  
            BigDecimal available = payAmt.subtract(requestedAmt);  
  
            if (requestAmt.compareTo(available) > 0) {  
                throw new AdempiereException(  
                        "应付单 [" + invoice.getDocumentNo() + "] 申请金额超出可申请范围。"  
                        + "应付金额: " + payAmt.toPlainString()  
                        + "，已申请: " + requestedAmt.toPlainString()  
                        + "，可申请: " + available.toPlainString()  
                        + "，本次申请: " + requestAmt.toPlainString());  
            }  
        }  
    }  
  
    /**  
     * 查询该应付单在其他有效付款申请单中已申请的金额之和  
     * （排除已作废 VO 和已冲销 RE 的申请单）  
     */  
    private BigDecimal getAlreadyRequestedAmt(int invoiceId) {  
        BigDecimal amt = DB.getSQLValueBD(get_TrxName(),  
                "SELECT COALESCE(SUM(prl.RequestAmt), 0) "  
                + "FROM C_PaymentRequestLine prl "  
                + "INNER JOIN C_PaymentRequest pr "  
                + "       ON prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID "  
                + "WHERE prl.C_Invoice_ID = ? "  
                + "  AND pr.DocStatus NOT IN ('VO', 'RE') "  
                + "  AND prl.IsActive = 'Y'",  
                invoiceId);  
        return amt != null ? amt : BigDecimal.ZERO;  
    }  
  
    // ── 5. 创建付款申请单 ─────────────────────────────────────────────────────  
  
    private MPaymentRequest createPaymentRequest(List<MInvoice> invoices) {  
        MInvoice firstInvoice = invoices.get(0);  
  
        // 获取单据类型ID（通过固定 UUID）  
        int docTypeId = DB.getSQLValue(get_TrxName(),  
                "SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU = ?",  
                PaymentRequest_DOCTYPE_UU);  
        if (docTypeId <= 0) {  
            throw new AdempiereException("未找到付款申请单单据类型，请检查配置");  
        }  
  
        // ── 汇总金额 + 拼接申请事项 ──────────────────────────────────────────  
        BigDecimal totalAmt        = BigDecimal.ZERO; // 应付单合计（各发票 GrandTotal 之和）  
        BigDecimal totalPayAmt     = BigDecimal.ZERO; // 应付金额之和  
        BigDecimal totalRebateAmt  = BigDecimal.ZERO; // 不良品扣款/返点之和  
        BigDecimal totalDiscountAmt= BigDecimal.ZERO; // 折扣金额之和  
        BigDecimal totalRequestAmt = BigDecimal.ZERO; // 申请金额之和  
        StringBuilder rInfoBuilder = new StringBuilder();  
        
        for (MInvoice inv : invoices) {  
            totalAmt         = totalAmt.add(inv.getGrandTotal());  
            totalPayAmt      = totalPayAmt.add(nullSafe((BigDecimal) inv.get_Value(COLUMNNAME_PayAmt)));  
            totalRebateAmt   = totalRebateAmt.add(nullSafe((BigDecimal) inv.get_Value(COLUMNNAME_Rebate)));  
            totalDiscountAmt = totalDiscountAmt.add(nullSafe((BigDecimal) inv.get_Value(COLUMNNAME_DiscountAmt)));  
            totalRequestAmt  = totalRequestAmt.add(requestAmtMap.get(inv.getC_Invoice_ID()));  
  
            String rInfo = rInfoMap.get(inv.getC_Invoice_ID()); 
            if (rInfo != null && !rInfo.isEmpty()) {  
                if (rInfoBuilder.length() > 0)  
                    rInfoBuilder.append("+");  
                rInfoBuilder.append(rInfo);  
            }  
        }  
  
        // DifferenceAmt（主表）= PayAmt 合计 - RequestAmt 合计  
        BigDecimal totalDifferenceAmt = totalPayAmt.subtract(totalRequestAmt);  
  
        // ── 创建主表 ──────────────────────────────────────────────────────────  
        MPaymentRequest request = new MPaymentRequest(getCtx(), 0, get_TrxName());  
        request.setAD_Org_ID(firstInvoice.getAD_Org_ID());  
        request.setC_DocType_ID(docTypeId);  
        request.setDateTrx(new Timestamp(System.currentTimeMillis()));  
        request.setC_BPartner_ID(firstInvoice.getC_BPartner_ID());  
        request.setC_BPartner_Location_ID(firstInvoice.getC_BPartner_Location_ID());  
        request.setC_Currency_ID(firstInvoice.getC_Currency_ID());  
        request.setAD_User_ID(Env.getAD_User_ID(getCtx()));  
  
        // 金额汇总  
        request.setTotalAmt(totalAmt);  
        request.setPayAmt(totalPayAmt);  
        request.setRebateAmt(totalRebateAmt);  
        request.setDiscountAmt(totalDiscountAmt);  
        request.setDifferenceAmt(totalDifferenceAmt);  
        request.setRequestAmt(totalRequestAmt);  
  
        // 支付方式（取第一张发票）  
        String paymentRule = firstInvoice.getPaymentRule();  
        if (paymentRule != null)  
            request.setPaymentRule(paymentRule);  
  
        // 申请事项（拼接各发票 R_Info）  
        if (rInfoBuilder.length() > 0) {  
            String rInfo = rInfoBuilder.toString();  
            if (rInfo.length() > 2000)  
                rInfo = rInfo.substring(0, 2000);  
            request.set_ValueOfColumn(COLUMNNAME_R_Info, rInfo);  
        }  
  
        // 流程参数  
        if (p_PaymentCategory != null)  
            request.set_ValueOfColumn("PaymentCategory", p_PaymentCategory);  
        if (p_DatePromised != null)  
            request.setDatePromised(p_DatePromised);  
        if (p_PaymentMonth != null)  
            request.set_ValueOfColumn("paymentmonth", p_PaymentMonth);  
        if (p_Description != null)  
            request.setDescription(p_Description);  
  
        request.saveEx();  
  
        // ── 查询往来单位的银行账户（与 DefaultValue 逻辑保持一致）──────────────  
        int bpBankAccountId = DB.getSQLValue(get_TrxName(),  
            "SELECT MAX(C_BP_BankAccount_ID) FROM C_BP_BankAccount "  
            + "WHERE C_BPartner_ID=? AND IsActive='Y'",  
            firstInvoice.getC_BPartner_ID()); 
        
        // ── 创建明细行 ────────────────────────────────────────────────────────  
        for (MInvoice inv : invoices) {  
            BigDecimal linePayAmt      = nullSafe((BigDecimal) inv.get_Value(COLUMNNAME_PayAmt));  
            BigDecimal lineRebateAmt   = nullSafe((BigDecimal) inv.get_Value(COLUMNNAME_Rebate));  
            BigDecimal lineDiscountAmt = nullSafe((BigDecimal) inv.get_Value(COLUMNNAME_DiscountAmt));  
            BigDecimal lineRequestAmt  = requestAmtMap.get(inv.getC_Invoice_ID());  
            // DifferenceAmt（行）= PayAmt - RequestAmt  
            BigDecimal lineDifferenceAmt = linePayAmt.subtract(lineRequestAmt);  
  
            MPaymentRequestLine line = new MPaymentRequestLine(getCtx(), 0, get_TrxName());  
            line.setAD_Org_ID(inv.getAD_Org_ID());  
            line.setC_PaymentRequest_ID(request.getC_PaymentRequest_ID());  
            line.setC_Invoice_ID(inv.getC_Invoice_ID());  
  
            // 金额  
            line.set_ValueOfColumn("GrandTotal", inv.getGrandTotal()); // 应付单总金额  
            line.setPayAmt(linePayAmt);  
            line.setRebateAmt(lineRebateAmt);  
            line.setDiscountAmt(lineDiscountAmt);  
            line.setDifferenceAmt(lineDifferenceAmt);  
            line.setRequestAmt(lineRequestAmt);  
  
            // 从应付单复制文本字段  
            line.set_ValueOfColumn(COLUMNNAME_R_Info, rInfoMap.get(inv.getC_Invoice_ID()));  
            line.setDescription(inv.getDescription());  
  
            // 银行账户默认值  
            if (bpBankAccountId > 0)  
                line.setC_BP_BankAccount_ID(bpBankAccountId); 
            
            // 支付方式  
            String linePaymentRule = inv.getPaymentRule();  
            if (linePaymentRule != null)  
                line.setPaymentRule(linePaymentRule);  
  
            line.saveEx();  
        }  
  
        return request;  
    }  
  
    // ── 工具方法 ──────────────────────────────────────────────────────────────  
  
    /** 将 null 的 BigDecimal 转为 ZERO，避免 NPE */  
    private BigDecimal nullSafe(BigDecimal value) {  
        return value != null ? value : BigDecimal.ZERO;  
    }  
}