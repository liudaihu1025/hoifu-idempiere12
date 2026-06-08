package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

@org.adempiere.base.Model(table = "QC_DefectRecord")
public class X_QC_DefectRecord extends PO implements I_QC_DefectRecord, I_Persistent {
	private static final long serialVersionUID = 20250512L;

	public X_QC_DefectRecord(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public X_QC_DefectRecord(Properties ctx, int id, String trxName, String... v) {
		super(ctx, id, trxName, v);
	}

	public X_QC_DefectRecord(Properties ctx, String uuid, String trxName) {
		super(ctx, uuid, trxName);
	}

	public X_QC_DefectRecord(Properties ctx, String uuid, String trxName, String... v) {
		super(ctx, uuid, trxName, v);
	}

	public X_QC_DefectRecord(Properties ctx, ResultSet rs, String trxName) {
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
		return "X_QC_DefectRecord[" + get_ID() + ",DefectName=" + getDefectName() + "]";
	}

	@Override
	public void setQC_DefectRecord_ID(int v) {
		if (v < 1)
			set_ValueNoCheck(COLUMNNAME_QC_DefectRecord_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_DefectRecord_ID, v);
	}

	@Override
	public int getQC_DefectRecord_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_DefectRecord_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_DefectRecord_UU(String v) {
		set_Value(COLUMNNAME_QC_DefectRecord_UU, v);
	}

	@Override
	public String getQC_DefectRecord_UU() {
		return (String) get_Value(COLUMNNAME_QC_DefectRecord_UU);
	}

	@Override
	public void setQC_Type(String v) {
		set_Value(COLUMNNAME_QC_Type, v);
	}

	@Override
	public String getQC_Type() {
		return (String) get_Value(COLUMNNAME_QC_Type);
	}

	@Override
	public void setQC_Record_ID(int v) {
		set_Value(COLUMNNAME_QC_Record_ID, v);
	}

	@Override
	public int getQC_Record_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Record_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setQC_Line_ID(int v) {
		set_Value(COLUMNNAME_QC_Line_ID, v);
	}

	@Override
	public int getQC_Line_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_Line_ID);
		return ii == null ? 0 : ii;
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
	public void setDefectLevel(String v) {
		set_Value(COLUMNNAME_DefectLevel, v);
	}

	@Override
	public String getDefectLevel() {
		return (String) get_Value(COLUMNNAME_DefectLevel);
	}

	@Override
	public void setDefectQuantity(int v) {
		set_Value(COLUMNNAME_DefectQuantity, v);
	}

	@Override
	public int getDefectQuantity() {
		Integer ii = (Integer) get_Value(COLUMNNAME_DefectQuantity);
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
}