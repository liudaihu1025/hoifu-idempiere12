package com.hoifu.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MDocType;
import org.compiere.model.MFactAcct;
import org.compiere.model.MJournal;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.MGlVoucher;
import com.hoifu.service.IGlVoucherService;

public class GlVoucherServiceImpl implements IGlVoucherService {

	public void deleteGlVoucher(PO po) {
		String trxName = po.get_TrxName();
		int AD_Table_ID = po.get_Table_ID();
		int Record_ID = po.get_ID();

		List<PO> vouchers = new Query(po.getCtx(), "Gl_Voucher", "AD_Table_ID=? AND Record_ID=?", trxName)
				.setParameters(AD_Table_ID, Record_ID).list();
		for (PO voucher : vouchers) {
			voucher.deleteEx(true);
		}
	}

	public void createGlVoucher(PO po) {
		String trxName = po.get_TrxName();
		int AD_Table_ID = po.get_Table_ID();
		int Record_ID = po.get_ID();

		// 删除旧凭证（重新过账时）
		List<MGlVoucher> oldVouchers = new Query(po.getCtx(), "Gl_Voucher", "AD_Table_ID=? AND Record_ID=?", trxName)
				.setParameters(AD_Table_ID, Record_ID).list();
		for (MGlVoucher old : oldVouchers) {
			old.deleteEx(true);
		}

		List<MFactAcct> factAccts = new Query(po.getCtx(), MFactAcct.Table_Name, "AD_Table_ID=? AND Record_ID=?",
				trxName).setParameters(AD_Table_ID, Record_ID).setOrderBy("Fact_Acct_ID").list();

		if (factAccts == null || factAccts.isEmpty())
			return;

		// 按 AD_Org_ID 分组
		Map<Integer, List<MFactAcct>> orgGroups = new HashMap<>();
		for (MFactAcct fa : factAccts) {
			orgGroups.computeIfAbsent(fa.getAD_Org_ID(), k -> new ArrayList<>()).add(fa);
		}

		for (Map.Entry<Integer, List<MFactAcct>> entry : orgGroups.entrySet()) {
			createVoucherForOrg(po, entry.getKey(), entry.getValue());
		}
	}

	public void refreshVoucherAfterReversal(PO po) {
		String trxName = po.get_TrxName();

		PO originalVoucher = new Query(po.getCtx(), "Gl_Voucher", "AD_Table_ID=? AND Record_ID=?", trxName)
				.setParameters(po.get_Table_ID(), po.get_ID()).first();

		if (originalVoucher == null)
			return;

		originalVoucher.set_ValueNoCheck("Description", buildDescription(po));
		originalVoucher.set_ValueNoCheck("DocStatus", getDocStatusFromPO(po));
		originalVoucher.saveEx();
	}

	private void createVoucherForOrg(PO po, int orgId, List<MFactAcct> orgFactAccts) {
		String trxName = po.get_TrxName();

		// 借方优先排序
		orgFactAccts.sort((fa1, fa2) -> {
			boolean fa1IsDebit = fa1.getAmtAcctDr().signum() > 0;
			boolean fa2IsDebit = fa2.getAmtAcctDr().signum() > 0;
			if (fa1IsDebit && !fa2IsDebit)
				return -1;
			if (!fa1IsDebit && fa2IsDebit)
				return 1;
			return Integer.compare(fa1.getFact_Acct_ID(), fa2.getFact_Acct_ID());
		});

		MGlVoucher voucher = new MGlVoucher(po.getCtx(), 0, trxName);
		voucher.setAD_Org_ID(orgId);
		voucher.setAD_Table_ID(po.get_Table_ID());
		voucher.setRecord_ID(po.get_ID());
		voucher.setPosted(true);
		voucher.setDateDoc(getDateDocFromPO(po));
		voucher.setDateAcct(getDateAcctFromPO(po));
		voucher.setGL_Category_ID(getGLCategoryFromPO(po));

		if (po instanceof MJournal) {
			voucher.setAttachmentCount((Integer) po.get_Value("AttachmentCount"));
		}

		String glVoucherType = getGLVoucherTypeFromPO(po);
		if (glVoucherType != null)
			voucher.setGl_VoucherType(glVoucherType);

		int C_AcctSchema_ID = Env.getContextAsInt(po.getCtx(), "$C_AcctSchema_ID");
		if (C_AcctSchema_ID <= 0)
			C_AcctSchema_ID = orgFactAccts.get(0).getC_AcctSchema_ID();
		voucher.setC_AcctSchema_ID(C_AcctSchema_ID);

		MFactAcct firstFact = orgFactAccts.get(0);
		voucher.setC_Period_ID(firstFact.getC_Period_ID());
		voucher.setPostingType(firstFact.getPostingType());
		voucher.setC_Currency_ID(firstFact.getC_Currency_ID());

		int docTypeId = getDocTypeFromPO(po);
		if (docTypeId > 0)
			voucher.setSource_DocType_ID(docTypeId);

		String sql = "SELECT C_DocType_ID FROM C_DocType WHERE AD_Client_ID=? AND Name=? AND IsActive='Y'";
		int voucherDocTypeId = DB.getSQLValue(trxName, sql, po.getAD_Client_ID(), "凭证");
		if (voucherDocTypeId > 0)
			voucher.setC_DocType_ID(voucherDocTypeId);

		String description = buildDescription(po) + " [" + getOrgName(po.getCtx(), orgId, trxName) + "]";
		voucher.setDescription(description);
		voucher.setAD_User_ID(po.getUpdatedBy());
		voucher.setPostedBy(po.getUpdatedBy());

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
		voucher.setDocStatus(docStatus);
		voucher.saveEx();

		String documentNo = (String) voucher.get_Value("DocumentNo");
		voucher.setSeqNo(extractSeqNo(documentNo));
		voucher.saveEx();

		BigDecimal totalDr = BigDecimal.ZERO;
		BigDecimal totalCr = BigDecimal.ZERO;
		int lineSeqNo = 1;
		for (MFactAcct fa : orgFactAccts) {
			fa.set_ValueOfColumn("Gl_Voucher_ID", voucher.get_ID());
			fa.set_ValueOfColumn("SeqNo", lineSeqNo++);
			fa.saveEx();
			totalDr = totalDr.add(fa.getAmtAcctDr());
			totalCr = totalCr.add(fa.getAmtAcctCr());
		}
		voucher.setTotalDr(totalDr);
		voucher.setTotalCr(totalCr);
		voucher.saveEx();
	}

	// ---- 辅助方法 ----

	private Timestamp getDateDocFromPO(PO po) {
		for (String col : new String[] { "DateDoc", "DateInvoiced", "DateOrdered", "MovementDate", "DateTrx",
				"StatementDate" }) {
			int idx = po.get_ColumnIndex(col);
			if (idx >= 0) {
				Timestamp d = (Timestamp) po.get_Value(idx);
				if (d != null)
					return d;
			}
		}
		return new Timestamp(System.currentTimeMillis());
	}

	private Timestamp getDateAcctFromPO(PO po) {
		int idx = po.get_ColumnIndex("DateAcct");
		if (idx >= 0) {
			Timestamp d = (Timestamp) po.get_Value(idx);
			if (d != null)
				return d;
		}
		return getDateDocFromPO(po);
	}

	private int getGLCategoryFromPO(PO po) {
		int docTypeId = getDocTypeFromPO(po);
		if (docTypeId > 0) {
			MDocType dt = MDocType.get(po.getCtx(), docTypeId);
			if (dt != null)
				return dt.getGL_Category_ID();
		}
		String sql = "SELECT GL_Category_ID FROM GL_Category WHERE AD_Client_ID=? ORDER BY IsDefault DESC";
		return DB.getSQLValue(null, sql, po.getAD_Client_ID());
	}

	public int getDocTypeFromPO(PO po) {
		int idx = po.get_ColumnIndex("C_DocType_ID");
		if (idx >= 0) {
			int docTypeId = po.get_ValueAsInt("C_DocType_ID");
			if (docTypeId == 0) {
				idx = po.get_ColumnIndex("C_DocTypeTarget_ID");
				if (idx >= 0)
					docTypeId = po.get_ValueAsInt("C_DocTypeTarget_ID");
			}
			if (docTypeId > 0)
				return docTypeId;
		}

		String docBaseType = null;
		String tableName = po.get_TableName();
		if ("M_MatchInv".equals(tableName))
			docBaseType = "MXI";
		else if ("M_MatchPO".equals(tableName))
			docBaseType = "MXP";
		else if ("C_AllocationHdr".equals(tableName))
			docBaseType = "CMA";
		else if ("C_Cash".equals(tableName))
			docBaseType = "CMC";
		else if ("C_ProjectIssue".equals(tableName))
			docBaseType = "PJI";

		if (docBaseType != null) {
			String sql = "SELECT C_DocType_ID FROM C_DocType "
					+ "WHERE AD_Client_ID=? AND DocBaseType=? AND IsActive='Y' "
					+ "ORDER BY IsDefault DESC, C_DocType_ID";
			int docTypeId = DB.getSQLValue(null, sql, po.getAD_Client_ID(), docBaseType);
			if (docTypeId > 0)
				return docTypeId;
		}
		return 0;
	}

	private String getGLVoucherTypeFromPO(PO po) {
		if (po instanceof MJournal)
			return (String) po.get_Value("GL_VoucherType");
		int docTypeId = getDocTypeFromPO(po);
		if (docTypeId > 0) {
			MDocType dt = MDocType.get(po.getCtx(), docTypeId);
			if (dt != null)
				return (String) dt.get_Value("GL_VoucherType");
		}
		return null;
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
		if (idx >= 0)
			docTypeId = po.get_ValueAsInt("C_DocTypeTarget_ID");
		if (docTypeId <= 0) {
			idx = po.get_ColumnIndex("C_DocType_ID");
			if (idx >= 0)
				docTypeId = po.get_ValueAsInt("C_DocType_ID");
		}
		if (docTypeId > 0) {
			MDocType dt = MDocType.get(po.getCtx(), docTypeId);
			if (dt != null)
				sb.append(dt.getNameTrl());
		}
		idx = po.get_ColumnIndex("DocumentNo");
		if (idx >= 0)
			sb.append(",").append(po.get_Value(idx));
		idx = po.get_ColumnIndex("Description");
		if (idx >= 0 && po.get_Value(idx) != null)
			sb.append(",").append(po.get_Value(idx));
		return sb.toString();
	}

	private int extractSeqNo(String documentNo) {
		if (documentNo == null)
			return 0;
		StringBuilder digits = new StringBuilder();
		for (int i = documentNo.length() - 1; i >= 0; i--) {
			char c = documentNo.charAt(i);
			if (Character.isDigit(c))
				digits.insert(0, c);
			else if (digits.length() > 0)
				break;
		}
		try {
			return Integer.parseInt(digits.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String getOrgName(Properties ctx, int orgId, String trxName) {
		return DB.getSQLValueString(trxName, "SELECT Name FROM AD_Org WHERE AD_Org_ID=?", orgId);
	}
}