package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_Defect")
public class X_QC_Defect extends PO implements I_QC_Defect, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_Defect(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_Defect(Properties ctx, int id, String trxName, String... v) {
		super(ctx, id, trxName, v);
	}

	public X_QC_Defect(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_Defect(Properties ctx, String uuid, String trxName, String... v) {
		super(ctx, uuid, trxName, v);
	}

	public X_QC_Defect(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_Defect[" + get_ID() + ",DefectCode=" + getDefectCode() + "]";
	}

	@Override
	public void setQC_Defect_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_Defect_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_Defect_ID, v);
	}

	@Override
	public int getQC_Defect_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Defect_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_Defect_UU(String v) {
		set_Value(COLUMNNAME_QC_Defect_UU, v);
	}

	@Override
	public String getQC_Defect_UU() {
		return (String) get_Value(COLUMNNAME_QC_Defect_UU);
	}

	@Override
	public void setDefectCode(String v) {
		set_Value(COLUMNNAME_DefectCode, v);
	}

	@Override
	public String getDefectCode() {
		return (String) get_Value(COLUMNNAME_DefectCode);
	}

	@Override
	public void setDefectName(String v) {
		set_Value(COLUMNNAME_DefectName, v);
	}

	@Override
	public String getDefectName() {
		return (String) get_Value(COLUMNNAME_DefectName);
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
	public void setDefectLevel(String v) {
		set_Value(COLUMNNAME_DefectLevel, v);
	}

	@Override
	public String getDefectLevel() {
		return (String) get_Value(COLUMNNAME_DefectLevel);
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