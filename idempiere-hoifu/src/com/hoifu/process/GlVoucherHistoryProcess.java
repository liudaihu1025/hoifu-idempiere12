package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MDocType;
import org.compiere.model.MFactAcct;
import org.compiere.model.MJournal;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;  
  
/**  
 * 为历史已过账单据批量生成 Gl_Voucher 凭证。  
 * 扫描 Fact_Acct 中尚未关联 Gl_Voucher 的记录，  
 * 按 (AD_Table_ID, Record_ID) 分组，逐组创建凭证。  
 */  
@org.adempiere.base.annotation.Process  
public class GlVoucherHistoryProcess extends SvrProcess {  
  
    private Timestamp p_DateAcct_From = null;  
    private Timestamp p_DateAcct_To = null;  
    private int p_AD_Table_ID = 0;  
  
    private int m_created = 0;  
    private int m_errors = 0;  
	private int m_deleted = 0;
  
    @Override  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (int i = 0; i < para.length; i++) {  
            String name = para[i].getParameterName();  
            if (para[i].getParameter() == null && para[i].getParameter_To() == null)  
                continue;  
            if ("DateAcct".equals(name)) {  
                p_DateAcct_From = (Timestamp) para[i].getParameter();  
                p_DateAcct_To = (Timestamp) para[i].getParameter_To();  
            } else if ("AD_Table_ID".equals(name)) {  
                p_AD_Table_ID = para[i].getParameterAsInt();  
            } else {  
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);  
            }  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        if (log.isLoggable(Level.INFO))  
            log.info("DateAcct=" + p_DateAcct_From + " -> " + p_DateAcct_To  
                    + ", AD_Table_ID=" + p_AD_Table_ID);  
  
        StringBuilder sql = new StringBuilder();  
        sql.append("SELECT DISTINCT fa.AD_Table_ID, fa.Record_ID ");  
        sql.append("FROM Fact_Acct fa ");  
        sql.append("WHERE fa.AD_Client_ID=? ");  
        sql.append("AND NOT EXISTS (SELECT 1 FROM Gl_Voucher v ");  
        sql.append("  WHERE v.AD_Table_ID=fa.AD_Table_ID AND v.Record_ID=fa.Record_ID) ");  
  
        if (p_AD_Table_ID > 0)  
            sql.append("AND fa.AD_Table_ID=").append(p_AD_Table_ID).append(" ");  
        if (p_DateAcct_From != null)  
            sql.append("AND TRUNC(fa.DateAcct) >= ").append(DB.TO_DATE(p_DateAcct_From)).append(" ");  
        if (p_DateAcct_To != null)  
            sql.append("AND TRUNC(fa.DateAcct) <= ").append(DB.TO_DATE(p_DateAcct_To)).append(" ");  
  
        sql.append("ORDER BY fa.AD_Table_ID, fa.Record_ID");  
  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql.toString(), get_TrxName());  
            pstmt.setInt(1, getAD_Client_ID());  
            rs = pstmt.executeQuery();  
  
            while (rs.next()) {  
                int adTableId = rs.getInt(1);  
                int recordId = rs.getInt(2);  
                processOneDocument(adTableId, recordId);  
            }  
        } finally {  
            DB.close(rs, pstmt);  
        }  
  
		return "已创建凭证=" + m_created + ", 已删除孤立分录=" + m_deleted + ", 错误=" + m_errors;
    }  
  
    private void processOneDocument(int adTableId, int recordId) {  
        String innerTrxName = Trx.createTrxName("GVH");  
        Trx innerTrx = Trx.get(innerTrxName, true);  
        innerTrx.setDisplayName(getClass().getName() + "_processOneDocument");  
  
        try {  
            MTable table = MTable.get(getCtx(), adTableId);  
            if (table == null || table.get_ID() == 0) {  
				// 单据不存在，直接删除孤立分录
				int deleted = MFactAcct.deleteEx(adTableId, recordId, innerTrxName);
				innerTrx.commit(true);
				m_deleted++;
				addLog("已删除孤立分录: AD_Table_ID=" + adTableId + ", Record_ID=" + recordId + ", 分录行数=" + deleted);
                return;  
            }  
  
            PO po = table.getPO(recordId, innerTrxName);  

			// 源单据不存在时，删除孤立分录而不是跳过
			if (po == null || po.get_ID() == 0 || po.get_ID() != recordId) {
				int deleted = MFactAcct.deleteEx(adTableId, recordId, innerTrxName);
				innerTrx.commit(true);
				m_deleted++;
				addLog("已删除孤立分录: " + table.getTableName() + ", Record_ID=" + recordId + ", 分录行数=" + deleted);
                return;  
            }  
  
			// 源单据存在，正常创建凭证
            int voucherId = createGlVoucher(po, innerTrxName);  
  
            if (voucherId > 0) {  
                innerTrx.commit(true);  
                m_created++;  
                addBufferLog(voucherId, null, null,  
						table.getTableName() + " #" + recordId + " -> 凭证 #" + voucherId,
						MTable.getTable_ID("Gl_Voucher"), voucherId);
            } else {  
                innerTrx.rollback();  
            }  
  
        } catch (Exception e) {  
            log.log(Level.SEVERE, "Error processing AD_Table_ID=" + adTableId  
                    + ", Record_ID=" + recordId, e);  
            innerTrx.rollback();  
            m_errors++;  
			addLog("错误: " + MTable.getTableName(getCtx(), adTableId) + " #" + recordId + " - " + e.getMessage());
        } finally {  
            innerTrx.close();  
		}
    }  
  
    // ==================== 凭证创建逻辑 ====================  
  
    private int createGlVoucher(PO po, String trxName) {  
        int AD_Table_ID = po.get_Table_ID();  
        int Record_ID = po.get_ID();  
  
        // 查询 Fact_Acct 记录  
        List<MFactAcct> factAccts = new Query(po.getCtx(), MFactAcct.Table_Name,  
                "AD_Table_ID=? AND Record_ID=?", trxName)  
                .setParameters(AD_Table_ID, Record_ID)  
                .setOrderBy("Fact_Acct_ID").list();  
  
        if (factAccts == null || factAccts.isEmpty())  
            return 0;  
  
        // 创建 Gl_Voucher  
        PO voucher = MTable.get(po.getCtx(), "Gl_Voucher").getPO(0, trxName);  
        voucher.set_ValueNoCheck("AD_Org_ID", po.getAD_Org_ID());  
        voucher.set_ValueNoCheck("AD_Table_ID", AD_Table_ID);  
        voucher.set_ValueNoCheck("Record_ID", Record_ID);  
        voucher.set_ValueNoCheck("Posted", true);  
  
        voucher.set_ValueNoCheck("DateDoc", getDateDocFromPO(po));  
        voucher.set_ValueNoCheck("DateAcct", getDateAcctFromPO(po));  
        voucher.set_ValueNoCheck("GL_Category_ID", getGLCategoryFromPO(po));  
  
        int C_AcctSchema_ID = Env.getContextAsInt(po.getCtx(), "$C_AcctSchema_ID");  
        if (C_AcctSchema_ID <= 0)  
            C_AcctSchema_ID = factAccts.get(0).getC_AcctSchema_ID();  
        voucher.set_ValueNoCheck("C_AcctSchema_ID", C_AcctSchema_ID);  
  
        MFactAcct firstFact = factAccts.get(0);  
        voucher.set_ValueNoCheck("C_Period_ID", firstFact.getC_Period_ID());  
        voucher.set_ValueNoCheck("PostingType", firstFact.getPostingType());  
        voucher.set_ValueNoCheck("C_Currency_ID", firstFact.getC_Currency_ID());  
  
        int docTypeId = getDocTypeFromPO(po);  
        if (docTypeId > 0)  
            voucher.set_ValueNoCheck("Source_DocType_ID", docTypeId);  
  
        String sql = "SELECT C_DocType_ID FROM C_DocType WHERE AD_Client_ID=? AND Name=? AND IsActive='Y'";  
        int voucherDocTypeId = DB.getSQLValue(trxName, sql, po.getAD_Client_ID(), "凭证");  
        if (voucherDocTypeId > 0)  
            voucher.set_ValueNoCheck("C_DocType_ID", voucherDocTypeId);  
  
        voucher.set_ValueNoCheck("Description", buildDescription(po));  
        voucher.set_ValueNoCheck("AD_User_ID", po.getUpdatedBy());  
        voucher.set_ValueNoCheck("PostedBy", po.getUpdatedBy());  
  
        String docStatus = getDocStatusFromPO(po);  
        int reversalIdx = po.get_ColumnIndex("Reversal_ID");  
        if (reversalIdx >= 0) {  
            Object reversalObj = po.get_Value(reversalIdx);  
            if (reversalObj != null) {  
                int reversalId = ((Number) reversalObj).intValue();  
                if (reversalId > 0 && po.get_ID() > reversalId)  
                    docStatus = "RE";  
            }  
        }  
        voucher.set_ValueNoCheck("DocStatus", docStatus);  
  
        voucher.saveEx();  
  
        // SeqNo: 从 DocumentNo 提取  
        String documentNo = (String) voucher.get_Value("DocumentNo");  
        voucher.set_ValueNoCheck("SeqNo", extractSeqNo(documentNo));  
        voucher.saveEx();  
  
        // 更新 Fact_Acct，设置 Gl_Voucher_ID 和 SeqNo  
        BigDecimal totalDr = BigDecimal.ZERO;  
        BigDecimal totalCr = BigDecimal.ZERO;  
        int lineSeqNo = 1;  
  
        for (MFactAcct fa : factAccts) {  
            fa.set_ValueOfColumn("Gl_Voucher_ID", voucher.get_ID());  
            fa.set_ValueOfColumn("SeqNo", lineSeqNo);  
            fa.saveEx();  
            totalDr = totalDr.add(fa.getAmtAcctDr());  
            totalCr = totalCr.add(fa.getAmtAcctCr());  
            lineSeqNo++;  
        }  
  
        voucher.set_ValueNoCheck("TotalDr", totalDr);  
        voucher.set_ValueNoCheck("TotalCr", totalCr);  
        
        // 凭证字
        voucher.set_ValueNoCheck("Gl_VoucherType", getGLVoucherTypeFromPO(po));
        
		// 总账/手工凭证有附件数，直接取手工凭证的附件数
		if (po instanceof MJournal) {
			voucher.set_ValueNoCheck("AttachmentCount", (Integer) po.get_Value("AttachmentCount"));
		}
		
        voucher.saveEx();  
  
        return voucher.get_ID();  
    }  
  
    // ==================== 辅助方法 ====================  
    
    /**
	 * 从单据类型获取凭证字
	 */
	private String getGLVoucherTypeFromPO(PO po) {
		// 总账/手工凭证有凭证字，则直接取手工凭证的凭证字
		if (po instanceof MJournal) {
			return (String) po.get_Value("GL_VoucherType");
		}

		int docTypeId = getDocTypeFromPO(po);
		if (docTypeId > 0) {
			MDocType dt = MDocType.get(po.getCtx(), docTypeId);
			if (dt != null) {
				return (String) dt.get_Value("GL_VoucherType");
			}
		}
		return null;
	}
	
    private Timestamp getDateDocFromPO(PO po) {  
        for (String col : new String[]{"DateDoc", "DateInvoiced", "DateOrdered",  
                "MovementDate", "DateTrx", "StatementDate"}) {  
            int idx = po.get_ColumnIndex(col);  
            if (idx >= 0) {  
                Timestamp d = (Timestamp) po.get_Value(idx);  
                if (d != null) return d;  
            }  
        }  
        return new Timestamp(System.currentTimeMillis());  
    }  
  
    private Timestamp getDateAcctFromPO(PO po) {  
        int idx = po.get_ColumnIndex("DateAcct");  
        if (idx >= 0) {  
            Timestamp d = (Timestamp) po.get_Value(idx);  
            if (d != null) return d;  
        }  
        return getDateDocFromPO(po);  
    }  
  
    private int getGLCategoryFromPO(PO po) {  
        int docTypeId = getDocTypeFromPO(po);  
        if (docTypeId > 0) {  
            MDocType dt = MDocType.get(po.getCtx(), docTypeId);  
            if (dt != null) return dt.getGL_Category_ID();  
        }  
        String sql = "SELECT GL_Category_ID FROM GL_Category "  
                + "WHERE AD_Client_ID=? ORDER BY IsDefault DESC";  
        return DB.getSQLValue(null, sql, po.getAD_Client_ID());  
    }  
  
    private int getDocTypeFromPO(PO po) {  
        int idx = po.get_ColumnIndex("C_DocType_ID");  
        if (idx >= 0) {  
            int docTypeId = po.get_ValueAsInt("C_DocType_ID");  
            if (docTypeId == 0) {  
                idx = po.get_ColumnIndex("C_DocTypeTarget_ID");  
                if (idx >= 0)  
                    docTypeId = po.get_ValueAsInt("C_DocTypeTarget_ID");  
            }  
            if (docTypeId > 0) return docTypeId;  
        }  
  
        String docBaseType = null;  
        String tableName = po.get_TableName();  
        if ("M_MatchInv".equals(tableName)) docBaseType = "MXI";  
        else if ("M_MatchPO".equals(tableName)) docBaseType = "MXP";  
        else if ("C_AllocationHdr".equals(tableName)) docBaseType = "CMA";  
        else if ("C_Cash".equals(tableName)) docBaseType = "CMC";  
        else if ("C_ProjectIssue".equals(tableName)) docBaseType = "PJI";  
  
        if (docBaseType != null) {  
            String sql = "SELECT C_DocType_ID FROM C_DocType "  
                    + "WHERE AD_Client_ID=? AND DocBaseType=? AND IsActive='Y' "  
                    + "ORDER BY IsDefault DESC, C_DocType_ID";  
            int docTypeId = DB.getSQLValue(null, sql, po.getAD_Client_ID(), docBaseType);  
            if (docTypeId > 0) return docTypeId;  
        }  
        return 0;  
    }  
  
    private String getDocStatusFromPO(PO po) {  
        int idx = po.get_ColumnIndex("DocStatus");  
        if (idx >= 0 && po.get_Value(idx) != null)  
            return po.get_Value(idx).toString();  
        return "CO";  
    }  
  
    private String buildDescription(PO po) {  
        StringBuilder sb = new StringBuilder();  
        int docTypeId = 0;  
        int idx = po.get_ColumnIndex("C_DocTypeTarget_ID");  
        if (idx >= 0) docTypeId = po.get_ValueAsInt("C_DocTypeTarget_ID");  
        if (docTypeId <= 0) {  
            idx = po.get_ColumnIndex("C_DocType_ID");  
            if (idx >= 0) docTypeId = po.get_ValueAsInt("C_DocType_ID");  
        }  
        if (docTypeId > 0) {  
            MDocType dt = MDocType.get(po.getCtx(), docTypeId);  
            if (dt != null) sb.append(dt.getNameTrl());  
        }  
        idx = po.get_ColumnIndex("DocumentNo");  
        if (idx >= 0) sb.append(",").append(po.get_Value(idx));  
        idx = po.get_ColumnIndex("Description");  
        if (idx >= 0 && po.get_Value(idx) != null) sb.append(",").append(po.get_Value(idx));  
        return sb.toString();  
    }  
  
    private int extractSeqNo(String documentNo) {  
        if (documentNo == null) return 0;  
        StringBuilder digits = new StringBuilder();  
        for (int i = documentNo.length() - 1; i >= 0; i--) {  
            char c = documentNo.charAt(i);  
            if (Character.isDigit(c)) {  
                digits.insert(0, c);  
            } else {  
                if (digits.length() > 0) break;  
            }  
        }  
        try {  
            return Integer.parseInt(digits.toString());  
        } catch (NumberFormatException e) {  
            return 0;  
        }  
    }  
}