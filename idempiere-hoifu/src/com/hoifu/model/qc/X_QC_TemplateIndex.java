package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_TemplateIndex")
public class X_QC_TemplateIndex extends PO implements I_QC_TemplateIndex, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_TemplateIndex(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_TemplateIndex(Properties ctx, int id, String trxName, String... v) {
		super(ctx, id, trxName, v);
	}

	public X_QC_TemplateIndex(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_TemplateIndex(Properties ctx, String uuid, String trxName, String... v) {
		super(ctx, uuid, trxName, v);
	}

	public X_QC_TemplateIndex(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_TemplateIndex[" + get_ID() + "]";
	}

	@Override
	public void setQC_TemplateIndex_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_TemplateIndex_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_TemplateIndex_ID, v);
	}

	@Override
	public int getQC_TemplateIndex_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_TemplateIndex_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_TemplateIndex_UU(String v) {
		set_Value(COLUMNNAME_QC_TemplateIndex_UU, v);
	}

	@Override
	public String getQC_TemplateIndex_UU() {
		return (String) get_Value(COLUMNNAME_QC_TemplateIndex_UU);
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
	public void setQC_Index_ID(int v) {
		set_Value(COLUMNNAME_QC_Index_ID, v);
	}

	@Override
	public int getQC_Index_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Index_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public I_QC_Index getQC_Index() {
		return (I_QC_Index) MTable.get(getCtx(), I_QC_Index.Table_Name).getPO(getQC_Index_ID(), get_TrxName());
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
	public void setCheckMethod(String v) {
		set_Value(COLUMNNAME_CheckMethod, v);
	}

	@Override
	public String getCheckMethod() {
		return (String) get_Value(COLUMNNAME_CheckMethod);
	}

	@Override
	public void setStanderVal(BigDecimal v) {
		set_Value(COLUMNNAME_StanderVal, v);
	}

	@Override
	public BigDecimal getStanderVal() {
		return (BigDecimal) get_Value(COLUMNNAME_StanderVal);
	}

	@Override
	public void setUnitOfMeasure(String v) {
		set_Value(COLUMNNAME_UnitOfMeasure, v);
	}

	@Override
	public String getUnitOfMeasure() {
		return (String) get_Value(COLUMNNAME_UnitOfMeasure);
	}

	@Override
	public void setThresholdMax(BigDecimal v) {
		set_Value(COLUMNNAME_ThresholdMax, v);
	}

	@Override
	public BigDecimal getThresholdMax() {
		return (BigDecimal) get_Value(COLUMNNAME_ThresholdMax);
	}

	@Override
	public void setThresholdMin(BigDecimal v) {
		set_Value(COLUMNNAME_ThresholdMin, v);
	}

	@Override
	public BigDecimal getThresholdMin() {
		return (BigDecimal) get_Value(COLUMNNAME_ThresholdMin);
	}

	@Override
	public void setDocURL(String v) {
		set_Value(COLUMNNAME_DocURL, v);
	}

	@Override
	public String getDocURL() {
		return (String) get_Value(COLUMNNAME_DocURL);
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