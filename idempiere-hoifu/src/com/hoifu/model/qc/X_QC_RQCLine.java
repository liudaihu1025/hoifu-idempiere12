package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_RQCLine")
public class X_QC_RQCLine extends PO implements I_QC_RQCLine, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_RQCLine(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_RQCLine(Properties ctx, int id, String trxName, String... v) {
		super(ctx, id, trxName, v);
	}

	public X_QC_RQCLine(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_RQCLine(Properties ctx, String uuid, String trxName, String... v) {
		super(ctx, uuid, trxName, v);
	}

	public X_QC_RQCLine(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_RQCLine[" + get_ID() + "]";
	}

	@Override
	public void setQC_RQCLine_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_RQCLine_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_RQCLine_ID, v);
	}

	@Override
	public int getQC_RQCLine_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_RQCLine_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_RQCLine_UU(String v) {
		set_Value(COLUMNNAME_QC_RQCLine_UU, v);
	}

	@Override
	public String getQC_RQCLine_UU() {
		return (String) get_Value(COLUMNNAME_QC_RQCLine_UU);
	}

	@Override
	public void setQC_RQC_ID(int v) {
		set_Value(COLUMNNAME_QC_RQC_ID, v);
	}

	@Override
	public int getQC_RQC_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_RQC_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public I_QC_RQC getQC_RQC() {
		return (I_QC_RQC) MTable.get(getCtx(), I_QC_RQC.Table_Name).getPO(getQC_RQC_ID(), get_TrxName());
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
	public void setMeasuredValMin(BigDecimal v) {
		set_Value(COLUMNNAME_MeasuredValMin, v);
	}

	@Override
	public BigDecimal getMeasuredValMin() {
		return (BigDecimal) get_Value(COLUMNNAME_MeasuredValMin);
	}

	@Override
	public void setMeasuredValMax(BigDecimal v) {
		set_Value(COLUMNNAME_MeasuredValMax, v);
	}

	@Override
	public BigDecimal getMeasuredValMax() {
		return (BigDecimal) get_Value(COLUMNNAME_MeasuredValMax);
	}

	@Override
	public void setMeasuredValAvg(BigDecimal v) {
		set_Value(COLUMNNAME_MeasuredValAvg, v);
	}

	@Override
	public BigDecimal getMeasuredValAvg() {
		return (BigDecimal) get_Value(COLUMNNAME_MeasuredValAvg);
	}

	@Override
	public void setIsQualified(boolean v) {
		set_Value(COLUMNNAME_IsQualified, v ? "Y" : "N");
	}

	@Override
	public boolean isQualified() {
		return "Y".equals(get_Value(COLUMNNAME_IsQualified));
	}

	@Override
	public void setCheckComment(String v) {
		set_Value(COLUMNNAME_CheckComment, v);
	}

	@Override
	public String getCheckComment() {
		return (String) get_Value(COLUMNNAME_CheckComment);
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
	public void setDescription(String v) {
		set_Value(COLUMNNAME_Description, v);
	}

	@Override
	public String getDescription() {
		return (String) get_Value(COLUMNNAME_Description);
	}
}