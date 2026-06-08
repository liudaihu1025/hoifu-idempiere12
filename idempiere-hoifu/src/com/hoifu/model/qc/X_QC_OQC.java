package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_OQC")
public class X_QC_OQC extends PO implements I_QC_OQC, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_OQC(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_OQC(Properties ctx, int id, String trxName, String... v) {
		super(ctx, id, trxName, v);
	}

	public X_QC_OQC(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_OQC(Properties ctx, String uuid, String trxName, String... v) {
		super(ctx, uuid, trxName, v);
	}

	public X_QC_OQC(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected int get_AccessLevel() {
		return accessLevel.intValue();
	}

	@Override
	protected POInfo initPO(Properties ctx) {
		return POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
	}

	@Override
	public String toString() {
		return "X_QC_OQC[" + get_ID() + ",DocumentNo=" + getDocumentNo() + "]";
	}

	@Override
	public void setQC_OQC_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_OQC_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_OQC_ID, v);
	}

	@Override
	public int getQC_OQC_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_OQC_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_OQC_UU(String v) {
		set_Value(COLUMNNAME_QC_OQC_UU, v);
	}

	@Override
	public String getQC_OQC_UU() {
		return (String) get_Value(COLUMNNAME_QC_OQC_UU);
	}

	@Override
	public void setDocumentNo(String v) {
		set_ValueNoCheck(COLUMNNAME_DocumentNo, v);
	}

	@Override
	public String getDocumentNo() {
		return (String) get_Value(COLUMNNAME_DocumentNo);
	}

	@Override
	public void setDocStatus(String v) {
		set_Value(COLUMNNAME_DocStatus, v);
	}

	@Override
	public String getDocStatus() {
		return (String) get_Value(COLUMNNAME_DocStatus);
	}

	@Override
	public void setDocAction(String v) {
		set_Value(COLUMNNAME_DocAction, v);
	}

	@Override
	public String getDocAction() {
		return (String) get_Value(COLUMNNAME_DocAction);
	}

	@Override
	public void setQC_Template_ID(int v) {
		set_Value(COLUMNNAME_QC_Template_ID, v);
	}

	@Override
	public int getQC_Template_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Template_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public I_QC_Template getQC_Template() {
		return (I_QC_Template) MTable.get(getCtx(), I_QC_Template.Table_Name).getPO(getQC_Template_ID(), get_TrxName());
	}

	@Override
	public void setSourceDocID(int v) {
		set_Value(COLUMNNAME_SourceDocID, v);
	}

	@Override
	public int getSourceDocID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_SourceDocID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setSourceDocTypeID(int v) {
		set_Value(COLUMNNAME_SourceDocTypeID, v);
	}

	@Override
	public int getSourceDocTypeID() {
		return (int) get_Value(COLUMNNAME_SourceDocTypeID);
	}
	
	@Override
	public void setSourceDocCode(String v) {
		set_Value(COLUMNNAME_SourceDocCode, v);
	}

	@Override
	public String getSourceDocCode() {
		return (String) get_Value(COLUMNNAME_SourceDocCode);
	}

	@Override
	public void setSourceLineID(int v) {
		set_Value(COLUMNNAME_SourceLineID, v);
	}

	@Override
	public int getSourceLineID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_SourceLineID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setC_BPartner_ID(int v) {
		set_Value(COLUMNNAME_C_BPartner_ID, v);
	}

	@Override
	public int getC_BPartner_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_C_BPartner_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setBatchCode(String v) {
		set_Value(COLUMNNAME_BatchCode, v);
	}

	@Override
	public String getBatchCode() {
		return (String) get_Value(COLUMNNAME_BatchCode);
	}

	@Override
	public void setM_Product_ID(int v) {
		set_Value(COLUMNNAME_M_Product_ID, v);
	}

	@Override
	public int getM_Product_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_M_Product_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQuantityMinCheck(BigDecimal v) {
		set_Value(COLUMNNAME_QuantityMinCheck, v);
	}

	@Override
	public BigDecimal getQuantityMinCheck() {
		return (BigDecimal) get_Value(COLUMNNAME_QuantityMinCheck);
	}

	@Override
	public void setQuantityMaxUnqualified(BigDecimal v) {
		set_Value(COLUMNNAME_QuantityMaxUnqualified, v);
	}

	@Override
	public BigDecimal getQuantityMaxUnqualified() {
		return (BigDecimal) get_Value(COLUMNNAME_QuantityMaxUnqualified);
	}

	@Override
	public void setQuantityOut(BigDecimal v) {
		set_Value(COLUMNNAME_QuantityOut, v);
	}

	@Override
	public BigDecimal getQuantityOut() {
		return (BigDecimal) get_Value(COLUMNNAME_QuantityOut);
	}

	@Override
	public void setQuantityCheck(BigDecimal v) {
		set_Value(COLUMNNAME_QuantityCheck, v);
	}

	@Override
	public BigDecimal getQuantityCheck() {
		return (BigDecimal) get_Value(COLUMNNAME_QuantityCheck);
	}

	@Override
	public void setQuantityUnqualified(BigDecimal v) {
		set_Value(COLUMNNAME_QuantityUnqualified, v);
	}

	@Override
	public BigDecimal getQuantityUnqualified() {
		return (BigDecimal) get_Value(COLUMNNAME_QuantityUnqualified);
	}

	@Override
	public void setQuantityQualified(BigDecimal v) {
		set_Value(COLUMNNAME_QuantityQualified, v);
	}

	@Override
	public BigDecimal getQuantityQualified() {
		return (BigDecimal) get_Value(COLUMNNAME_QuantityQualified);
	}

	@Override
	public void setCR_Rate(BigDecimal v) {
		set_Value(COLUMNNAME_CR_Rate, v);
	}

	@Override
	public BigDecimal getCR_Rate() {
		return (BigDecimal) get_Value(COLUMNNAME_CR_Rate);
	}

	@Override
	public void setMAJ_Rate(BigDecimal v) {
		set_Value(COLUMNNAME_MAJ_Rate, v);
	}

	@Override
	public BigDecimal getMAJ_Rate() {
		return (BigDecimal) get_Value(COLUMNNAME_MAJ_Rate);
	}

	@Override
	public void setMIN_Rate(BigDecimal v) {
		set_Value(COLUMNNAME_MIN_Rate, v);
	}

	@Override
	public BigDecimal getMIN_Rate() {
		return (BigDecimal) get_Value(COLUMNNAME_MIN_Rate);
	}

	@Override
	public void setCR_Quantity(BigDecimal v) {
		set_Value(COLUMNNAME_CR_Quantity, v);
	}

	@Override
	public BigDecimal getCR_Quantity() {
		return (BigDecimal) get_Value(COLUMNNAME_CR_Quantity);
	}

	@Override
	public void setMAJ_Quantity(BigDecimal v) {
		set_Value(COLUMNNAME_MAJ_Quantity, v);
	}

	@Override
	public BigDecimal getMAJ_Quantity() {
		return (BigDecimal) get_Value(COLUMNNAME_MAJ_Quantity);
	}

	@Override
	public void setMIN_Quantity(BigDecimal v) {
		set_Value(COLUMNNAME_MIN_Quantity, v);
	}

	@Override
	public BigDecimal getMIN_Quantity() {
		return (BigDecimal) get_Value(COLUMNNAME_MIN_Quantity);
	}

	@Override
	public void setCheckResult(String v) {
		set_Value(COLUMNNAME_CheckResult, v);
	}

	@Override
	public String getCheckResult() {
		return (String) get_Value(COLUMNNAME_CheckResult);
	}

	@Override
	public void setOutDate(Timestamp v) {
		set_Value(COLUMNNAME_OutDate, v);
	}

	@Override
	public Timestamp getOutDate() {
		return (Timestamp) get_Value(COLUMNNAME_OutDate);
	}

	@Override
	public void setInspectDate(Timestamp v) {
		set_Value(COLUMNNAME_InspectDate, v);
	}

	@Override
	public Timestamp getInspectDate() {
		return (Timestamp) get_Value(COLUMNNAME_InspectDate);
	}

	@Override
	public void setInspector(String v) {
		set_Value(COLUMNNAME_Inspector, v);
	}

	@Override
	public String getInspector() {
		return (String) get_Value(COLUMNNAME_Inspector);
	}

	@Override
	public void setDescription(String v) {
		set_Value(COLUMNNAME_Description, v);
	}

	@Override
	public String getDescription() {
		return (String) get_Value(COLUMNNAME_Description);
	}

	@Override
	public void setProcessed(boolean Processed) {
		set_ValueNoCheck(COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	@Override
	public boolean isProcessed() {
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	@Override
	public void setProcessedOn(BigDecimal v) {
		set_Value(COLUMNNAME_ProcessedOn, v);
	}

	@Override
	public BigDecimal getProcessedOn() {
		return (BigDecimal) get_Value(COLUMNNAME_ProcessedOn);
	}

	@Override
	public void setProcessing(boolean v) {
		set_Value(COLUMNNAME_Processing, Boolean.valueOf(v));
	}

	@Override
	public boolean isProcessing() {
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	@Override
	public void setIsApproved(boolean IsApproved) {
		set_ValueNoCheck(COLUMNNAME_IsApproved, Boolean.valueOf(IsApproved));
	}

	@Override
	public boolean isApproved() {
		Object oo = get_Value(COLUMNNAME_IsApproved);
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	@Override
	public void setC_DocType_ID(int C_DocType_ID) {
		if (C_DocType_ID < 0)
			set_ValueNoCheck(COLUMNNAME_C_DocType_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	@Override
	public int getC_DocType_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	@Override
	public void setC_DocTypeTarget_ID(int v) {
		set_Value(COLUMNNAME_C_DocTypeTarget_ID, v);
	}

	@Override
	public int getC_DocTypeTarget_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_C_DocTypeTarget_ID);
		return ii == null ? 0 : ii;
	}
}