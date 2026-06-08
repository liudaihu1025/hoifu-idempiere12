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
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MConversionType;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.hoifu.model.MReconciliation;
import com.hoifu.model.MReconciliationLine;  
  
@org.adempiere.base.annotation.Process    
public class ReconciliationCreateInvoiceProcess extends SvrProcess {    
    private int p_C_Reconciliation_ID = 0;    
        
    protected void prepare() {    
        p_C_Reconciliation_ID = getRecord_ID();    
    }    
        
    protected String doIt() throws Exception {    
        MReconciliation reconciliation = new MReconciliation(getCtx(), p_C_Reconciliation_ID, get_TrxName());    
        if (reconciliation.get_ID() == 0)    
            throw new IllegalArgumentException("Reconciliation not found");    
                
        // 按单据类型分组创建发票/贷项通知单    
        Map<String, List<MReconciliationLine>> groupedLines = groupLinesByDocType(reconciliation);    
          
        // 检查对账明细是否为空    
        boolean hasLines = groupedLines.values().stream().anyMatch(list -> !list.isEmpty());    
        if (!hasLines) {    
            return "对账单明细为空，无法创建发票";    
        }   
          
        Map<String, List<String>> invoiceNos = new HashMap<>();    
        invoiceNos.put("AR_INVOICE", new ArrayList<>());    
        invoiceNos.put("AP_INVOICE", new ArrayList<>());    
        invoiceNos.put("AR_CREDIT_MEMO", new ArrayList<>());    
        invoiceNos.put("AP_CREDIT_MEMO", new ArrayList<>());   
          
        // 遍历每个分组，为不同类型的单据分别创建发票或贷项通知单     
        for (Map.Entry<String, List<MReconciliationLine>> entry : groupedLines.entrySet()) {    
            String docType = entry.getKey();    
            List<MReconciliationLine> lines = entry.getValue();    
                
            if (!lines.isEmpty()) {    
                // 为当前组创建发票/贷项通知单并获取单据号  
                String invoiceNo = createInvoiceForGroup(reconciliation, lines, docType);    
                invoiceNos.get(docType).add(invoiceNo);  
            }    
        }    
            
        return formatReturnMessage(invoiceNos);      
    }    
        
    private Map<String, List<MReconciliationLine>> groupLinesByDocType(MReconciliation reconciliation) {    
        // 创建分组Map：按单据类型分组  
        Map<String, List<MReconciliationLine>> grouped = new HashMap<>();    
        grouped.put("AR_INVOICE", new ArrayList<>());      // 应收发票  
        grouped.put("AP_INVOICE", new ArrayList<>());      // 应付发票  
        grouped.put("AR_CREDIT_MEMO", new ArrayList<>());  // 应收单(红字)(客户退货)  
        grouped.put("AP_CREDIT_MEMO", new ArrayList<>());  // 应付单(红字)(供应商退货)  
          
        // SQL查询：获取对账单明细中有关联收发行记录的明细ID，包含文档类型信息  
        String sql = "SELECT rl.C_ReconciliationLine_ID " +    
                    "FROM C_ReconciliationLine rl " +    
                    "INNER JOIN M_InOutLine iol ON rl.M_InOutLine_ID = iol.M_InOutLine_ID " +    
                    "INNER JOIN M_InOut io ON iol.M_InOut_ID = io.M_InOut_ID " +    
                    "INNER JOIN C_DocType dt ON io.C_DocType_ID = dt.C_DocType_ID " +    
                    "WHERE rl.C_Reconciliation_ID = ? " +    
                    "AND rl.M_InOutLine_ID > 0";    
            
        PreparedStatement pstmt = null;    
        ResultSet rs = null;    
        try {    
            pstmt = DB.prepareStatement(sql, get_TrxName());    
            pstmt.setInt(1, reconciliation.getC_Reconciliation_ID());    
            rs = pstmt.executeQuery();    
              
            // 遍历查询结果   
            while (rs.next()) {    
                int reconLineID = rs.getInt(1);    
                MReconciliationLine reconLine = new MReconciliationLine(getCtx(), reconLineID, get_TrxName());    
                    
                if (reconLine.getM_InOutLine_ID() > 0) {    
                    // 获取关联的收发行和收发货单  
                    MInOutLine inoutLine = new MInOutLine(getCtx(), reconLine.getM_InOutLine_ID(), get_TrxName());    
                    MInOut inout = new MInOut(getCtx(), inoutLine.getM_InOut_ID(), get_TrxName());    
                      
                    // 根据收发货单的事务类型和文档基础类型进行分组  
                    String groupKey = determineGroupKey(inout.isSOTrx(), inout.getC_DocType().getDocBaseType());    
                    grouped.get(groupKey).add(reconLine);    
                }    
            }    
        } catch (SQLException e) {    
            log.log(Level.SEVERE, sql, e);    
        } finally {    
            DB.close(rs, pstmt);    
        }    
            
        return grouped;    
    }    
        
    private String determineGroupKey(boolean isSOTrx, String docBaseType) {    
        if (isSOTrx) {    
            // 销售事务：客户退货单生成应收贷项通知单，其他生成应收发票  
        	return MDocType.DOCBASETYPE_MaterialReceipt.equals(docBaseType) ? "AR_CREDIT_MEMO" : "AR_INVOICE";
        } else {    
            // 采购事务：供应商退货单生成应付贷项通知单，其他生成应付发票  
        	return MDocType.DOCBASETYPE_MaterialDelivery.equals(docBaseType) ? "AP_CREDIT_MEMO" : "AP_INVOICE";   
        }    
    }    
        
    private String createInvoiceForGroup(MReconciliation reconciliation,     
                                       List<MReconciliationLine> lines, String docType) throws Exception {    
        // 创建发票/贷项通知单头    
        MInvoice invoice = createInvoiceHeader(reconciliation, docType);    
            
        // 创建发票/贷项通知单明细    
        createInvoiceLines(invoice, lines);    
            
        invoice.saveEx(get_TrxName());    
            
        addLog(invoice.getC_Invoice_ID(), invoice.getDateInvoiced(), invoice.getGrandTotal(),     
               invoice.getDocumentNo(), invoice.get_Table_ID(), invoice.getC_Invoice_ID());    
            
        return invoice.getDocumentNo();    
    }    
        
    private MInvoice createInvoiceHeader(MReconciliation reconciliation, String docType) {    
        // 创建新的发票/贷项通知单实例    
        MInvoice invoice = new MInvoice(getCtx(), 0, get_TrxName());    
        invoice.setClientOrg(reconciliation.getAD_Client_ID(), reconciliation.getAD_Org_ID());    
        invoice.setC_BPartner_ID(reconciliation.getC_BPartner_ID());    
          
        // 根据单据类型设置相应的文档类型和事务标志  
        switch (docType) {    
            case "AR_INVOICE":    
                invoice.setIsSOTrx(true);    
                invoice.setC_DocTypeTarget_ID(MDocType.getDocType(MDocType.DOCBASETYPE_ARInvoice));    
                break;    
            case "AP_INVOICE":    
                invoice.setIsSOTrx(false);    
                invoice.setC_DocTypeTarget_ID(MDocType.getDocType(MDocType.DOCBASETYPE_APInvoice));    
                break;    
            case "AR_CREDIT_MEMO":    
                invoice.setIsSOTrx(true);    
                invoice.setC_DocTypeTarget_ID(MDocType.getDocType(MDocType.DOCBASETYPE_ARCreditMemo));    
                break;    
            case "AP_CREDIT_MEMO":    
                invoice.setIsSOTrx(false);    
                invoice.setC_DocTypeTarget_ID(MDocType.getDocType(MDocType.DOCBASETYPE_APCreditMemo));    
                break;    
        }    
          
        // 直接从对账单获取地址和联系人    
        Integer locationID = (Integer)reconciliation.get_Value("C_BPartner_Location_ID");    
        if (locationID != null && locationID > 0) {    
            invoice.setC_BPartner_Location_ID(locationID);    
        }    
            
        Integer userID = (Integer)reconciliation.get_Value("AD_User_ID");    
        if (userID != null && userID > 0) {    
            invoice.setAD_User_ID(userID);    
            invoice.setSalesRep_ID(userID);    
        }    
            
        // 设置支付条款    
        MBPartner bp = new MBPartner(getCtx(), reconciliation.getC_BPartner_ID(), get_TrxName());     
        if (bp.getC_PaymentTerm_ID() > 0) {    
            invoice.setC_PaymentTerm_ID(bp.getC_PaymentTerm_ID());    
        }    
            
        // 根据应收应付类型设置价格表    
        if (invoice.isSOTrx()) {    
            if (bp.getM_PriceList_ID() > 0) {    
                invoice.setM_PriceList_ID(bp.getM_PriceList_ID());    
            }    
        } else {    
            if (bp.getPO_PriceList_ID() > 0) {    
                invoice.setM_PriceList_ID(bp.getPO_PriceList_ID());    
            }    
        }    
            
        // 设置发票日期和会计日期    
        invoice.setDateInvoiced(new Timestamp(System.currentTimeMillis()));    
        invoice.setDateAcct(invoice.getDateInvoiced()); 
        
        // 设置付款事项字段  
        String reconPeriod = (String) reconciliation.get_Value("ReconPeroid");  
        if (reconPeriod != null && !reconPeriod.trim().isEmpty()) {  
            String rInfoValue = reconPeriod + "对账单";  
            invoice.set_ValueOfColumn("R_Info", rInfoValue);;  
        }

        // 设置对账月字段
        String reconciliationMonth = (String) reconciliation.get_Value("ReconPeroid");
        if (reconciliationMonth != null && !reconciliationMonth.trim().isEmpty()) {
            invoice.set_ValueOfColumn("reconciliationmonth", reconciliationMonth);
        }

        //设置默认汇率类型  
        invoice.setC_ConversionType_ID(MConversionType.getDefault(reconciliation.getAD_Client_ID()));
            
        invoice.saveEx();    
        return invoice;    
    }    
        
    private void createInvoiceLines(MInvoice invoice, List<MReconciliationLine> lines) {    
        for (MReconciliationLine reconLine : lines) {    
            if (reconLine.getM_InOutLine_ID() > 0) {    
                MInOutLine inoutLine = new MInOutLine(getCtx(), reconLine.getM_InOutLine_ID(), get_TrxName());    
                MInvoiceLine invoiceLine = new MInvoiceLine(invoice);    
                    
                // 使用setShipLine设置基本信息     
                invoiceLine.setShipLine(inoutLine);    
                
				BigDecimal qtyToReconcile = reconLine.getQtyToReconcile();
				BigDecimal priceToReconcile = reconLine.getPriceToReconcile();

				invoiceLine.setQtyEntered(qtyToReconcile);
				invoiceLine.setQtyInvoiced(qtyToReconcile);
				invoiceLine.setPriceActual(priceToReconcile);
                                
                // 关联发票明细的对账明细ID    
                invoiceLine.set_ValueOfColumn("C_ReconciliationLine_ID", reconLine.getC_ReconciliationLine_ID());    
                invoiceLine.saveEx();    
            }    
        }    
    }    
        
    private String formatReturnMessage(Map<String, List<String>> invoiceNos) {    
        StringBuilder message = new StringBuilder("创建成功");    
          
        if (!invoiceNos.get("AP_INVOICE").isEmpty()) {    
            message.append("，应付单：").append(String.join("、", invoiceNos.get("AP_INVOICE")));    
        }    
        if (!invoiceNos.get("AP_CREDIT_MEMO").isEmpty()) {    
            message.append("，应付单(红字)：").append(String.join("、", invoiceNos.get("AP_CREDIT_MEMO")));    
        }    
        if (!invoiceNos.get("AR_INVOICE").isEmpty()) {    
            message.append("，应收单：").append(String.join("、", invoiceNos.get("AR_INVOICE")));    
        }    
        if (!invoiceNos.get("AR_CREDIT_MEMO").isEmpty()) {    
            message.append("，应收单(红字)：").append(String.join("、", invoiceNos.get("AR_CREDIT_MEMO")));    
        }    
            
        return message.toString();    
    }    
}