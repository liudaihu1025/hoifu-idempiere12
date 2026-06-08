package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_Template")
public class X_QC_Template extends PO implements I_QC_Template, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_Template(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_Template(Properties ctx, int id, String trxName, String... virtualColumns) {
		super(ctx, id, trxName, virtualColumns);
	}

	public X_QC_Template(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_Template(Properties ctx, String uuid, String trxName, String... virtualColumns) {
		super(ctx, uuid, trxName, virtualColumns);
	}

	public X_QC_Template(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_Template[" + get_ID() + ",TemplateCode=" + getTemplateCode() + "]";
	}

	@Override
	public void setQC_Template_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_Template_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_Template_ID, v);
	}

	@Override
	public int getQC_Template_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Template_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_Template_UU(String v) {
		set_Value(COLUMNNAME_QC_Template_UU, v);
	}

	@Override
	public String getQC_Template_UU() {
		return (String) get_Value(COLUMNNAME_QC_Template_UU);
	}

	@Override
	public void setTemplateCode(String v) {
		set_Value(COLUMNNAME_TemplateCode, v);
	}

	@Override
	public String getTemplateCode() {
		return (String) get_Value(COLUMNNAME_TemplateCode);
	}

	@Override
	public void setTemplateName(String v) {
		set_Value(COLUMNNAME_TemplateName, v);
	}

	@Override
	public String getTemplateName() {
		return (String) get_Value(COLUMNNAME_TemplateName);
	}

	@Override
	public void setQCTypes(String v) {
		set_Value(COLUMNNAME_QCTypes, v);
	}

	@Override
	public String getQCTypes() {
		return (String) get_Value(COLUMNNAME_QCTypes);
	}

	@Override
	public void setDescription(String v) {
		set_Value(COLUMNNAME_Description, v);
	}

	@Override
	public String getDescription() {
		return (String) get_Value(COLUMNNAME_Description);
	}

	// ---- AQL 相关字段 ----
	@Override
	public void setQC_AQL_Standard_ID(int v) {
		if (v < 1)
			set_Value(COLUMNNAME_QC_AQL_Standard_ID, null);
		else
			set_Value(COLUMNNAME_QC_AQL_Standard_ID, Integer.valueOf(v));
	}

	@Override
	public int getQC_AQL_Standard_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_AQL_Standard_ID);
		return ii == null ? 0 : ii.intValue();
	}

	@Override
	public void setInspectionLevel(String v) {
		set_Value(COLUMNNAME_InspectionLevel, v);
	}

	@Override
	public String getInspectionLevel() {
		return (String) get_Value(COLUMNNAME_InspectionLevel);
	}

	@Override
	public void setAQL_CR(BigDecimal v) {
		set_Value(COLUMNNAME_AQL_CR, v);
	}

	@Override
	public BigDecimal getAQL_CR() {
		return (BigDecimal) get_Value(COLUMNNAME_AQL_CR);
	}

	@Override
	public void setAQL_MAJ(BigDecimal v) {
		set_Value(COLUMNNAME_AQL_MAJ, v);
	}

	@Override
	public BigDecimal getAQL_MAJ() {
		return (BigDecimal) get_Value(COLUMNNAME_AQL_MAJ);
	}

	@Override
	public void setAQL_MIN(BigDecimal v) {
		set_Value(COLUMNNAME_AQL_MIN, v);
	}

	@Override
	public BigDecimal getAQL_MIN() {
		return (BigDecimal) get_Value(COLUMNNAME_AQL_MIN);
	}
}