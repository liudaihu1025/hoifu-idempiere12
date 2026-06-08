package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_TemplateProduct")
public class X_QC_TemplateProduct extends PO implements I_QC_TemplateProduct, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_TemplateProduct(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_TemplateProduct(Properties ctx, int id, String trxName, String... v) {
		super(ctx, id, trxName, v);
	}

	public X_QC_TemplateProduct(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_TemplateProduct(Properties ctx, String uuid, String trxName, String... v) {
		super(ctx, uuid, trxName, v);
	}

	public X_QC_TemplateProduct(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_TemplateProduct[" + get_ID() + "]";
	}

	@Override
	public void setQC_TemplateProduct_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_TemplateProduct_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_TemplateProduct_ID, v);
	}

	@Override
	public int getQC_TemplateProduct_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_TemplateProduct_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_TemplateProduct_UU(String v) {
		set_Value(COLUMNNAME_QC_TemplateProduct_UU, v);
	}

	@Override
	public String getQC_TemplateProduct_UU() {
		return (String) get_Value(COLUMNNAME_QC_TemplateProduct_UU);
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
	public void setM_Product_ID(int v) {
		set_Value(COLUMNNAME_M_Product_ID, v);
	}

	@Override
	public int getM_Product_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_M_Product_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setDescription(String v) {
		set_Value(COLUMNNAME_Description, v);
	}

	@Override
	public String getDescription() {
		return (String) get_Value(COLUMNNAME_Description);
	}

	// ---- AQL 产品级别覆盖字段 ----
	// 注意：getter 返回 null（不返回 ZERO），以便区分"未配置"和"配置为0"
	@Override
	public void setAQL_CR(BigDecimal v) {
		set_Value(COLUMNNAME_AQL_CR, v);
	}

	@Override
	public BigDecimal getAQL_CR() {
		return (BigDecimal) get_Value(COLUMNNAME_AQL_CR); // 可能返回 null
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