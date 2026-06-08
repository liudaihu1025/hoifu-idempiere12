package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_Index")
public class X_QC_Index extends PO implements I_QC_Index, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_Index(Properties ctx, int QC_Index_ID, String trxName) {
		super(ctx, QC_Index_ID, trxName);
	}

	public X_QC_Index(Properties ctx, int QC_Index_ID, String trxName, String... virtualColumns) {
		super(ctx, QC_Index_ID, trxName, virtualColumns);
	}

	public X_QC_Index(Properties ctx, String QC_Index_UU, String trxName) {
		super(ctx, QC_Index_UU, trxName);
	}

	public X_QC_Index(Properties ctx, String QC_Index_UU, String trxName, String... virtualColumns) {
		super(ctx, QC_Index_UU, trxName, virtualColumns);
	}

	public X_QC_Index(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_Index[" + get_ID() + ",IndexCode=" + getIndexCode() + "]";
	}

	@Override
	public void setQC_Index_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_Index_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_Index_ID, v);
	}

	@Override
	public int getQC_Index_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Index_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_Index_UU(String v) {
		set_Value(COLUMNNAME_QC_Index_UU, v);
	}

	@Override
	public String getQC_Index_UU() {
		return (String) get_Value(COLUMNNAME_QC_Index_UU);
	}

	@Override
	public void setIndexCode(String v) {
		set_Value(COLUMNNAME_IndexCode, v);
	}

	@Override
	public String getIndexCode() {
		return (String) get_Value(COLUMNNAME_IndexCode);
	}

	@Override
	public void setIndexName(String v) {
		set_Value(COLUMNNAME_IndexName, v);
	}

	@Override
	public String getIndexName() {
		return (String) get_Value(COLUMNNAME_IndexName);
	}

	@Override
	public void setIndexType(String v) {
		set_Value(COLUMNNAME_IndexType, v);
	}

	@Override
	public String getIndexType() {
		return (String) get_Value(COLUMNNAME_IndexType);
	}

	@Override
	public void setIndexValueType(String v) {
		set_Value(COLUMNNAME_IndexValueType, v);
	}

	@Override
	public String getIndexValueType() {
		return (String) get_Value(COLUMNNAME_IndexValueType);
	}

	@Override
	public void setQCTool(String v) {
		set_Value(COLUMNNAME_QCTool, v);
	}

	@Override
	public String getQCTool() {
		return (String) get_Value(COLUMNNAME_QCTool);
	}

	@Override
	public void setDescription(String v) {
		set_Value(COLUMNNAME_Description, v);
	}

	@Override
	public String getDescription() {
		return (String) get_Value(COLUMNNAME_Description);
	}
}