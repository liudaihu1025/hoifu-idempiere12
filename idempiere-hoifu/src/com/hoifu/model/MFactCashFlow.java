package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

public class MFactCashFlow extends PO {

	private static final long serialVersionUID = 1932445511482681968L;
	public static final String Table_Name = "Fact_CashFlow";
	public static final int Table_ID = getTableID(Table_Name);

	// 添加静态方法
	public static int getTableID(String tableName) {
		return MTable.getTable_ID(tableName);
	}

	public MFactCashFlow(Properties ctx, int Fact_CashFlow_ID, String trxName) {
		super(ctx, Fact_CashFlow_ID, trxName);
	}

	public MFactCashFlow(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	protected POInfo initPO(Properties ctx) {
		POInfo poi = POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
		return poi;
	}

	public int getAD_Org_ID() {
		return get_ValueAsInt("AD_Org_ID");
	}

	public void setCreated(Timestamp Created) {
		set_Value("Created", Created);
	}

	public void setCreatedBy(int CreatedBy) {
		set_Value("CreatedBy", CreatedBy);
	}

	public void setUpdated(Timestamp Updated) {
		set_Value("Updated", Updated);
	}

	// 业务字段
	public void setFact_Acct_ID(int Fact_Acct_ID) {
		set_Value("Fact_Acct_ID", Fact_Acct_ID);
	}

	public int getFact_Acct_ID() {
		return get_ValueAsInt("Fact_Acct_ID");
	}

	public void setC_CashFlow_Item_ID(int C_CashFlow_Item_ID) {
		set_Value("C_CashFlow_Item_ID", C_CashFlow_Item_ID);
	}

	public int getC_CashFlow_Item_ID() {
		return get_ValueAsInt("C_CashFlow_Item_ID");
	}

	public void setCashFlow_Type(String CashFlow_Type) {
		set_Value("CashFlow_Type", CashFlow_Type);
	}

	public String getCashFlow_Type() {
		return get_ValueAsString("CashFlow_Type");
	}

	public void setIsCashIn(Boolean Is_CashIn) {
		set_Value("Is_CashIn", Boolean.valueOf(Is_CashIn));
	}

	public Boolean isCashIn() {
		Object oo = get_Value("Is_CashIn");
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public void setAmount(BigDecimal Amount) {
		set_Value("Amount", Amount);
	}

	public BigDecimal getAmount() {
		return (BigDecimal) get_Value("Amount");
	}

	public void setC_Currency_ID(int C_Currency_ID) {
		set_Value("C_Currency_ID", C_Currency_ID);
	}

	public int getC_Currency_ID() {
		return get_ValueAsInt("C_Currency_ID");
	}

	public void setDateAcct(Timestamp DateAcct) {
		set_Value("DateAcct", DateAcct);
	}

	public Timestamp getDateAcct() {
		return (Timestamp) get_Value("DateAcct");
	}

	public void setDateDoc(Timestamp DateDoc) {
		set_Value("DateDoc", DateDoc);
	}

	public Timestamp getDateDoc() {
		return (Timestamp) get_Value("DateDoc");
	}

	public void setC_Period_ID(int C_Period_ID) {
		set_Value("C_Period_ID", C_Period_ID);
	}

	public int getC_Period_ID() {
		return get_ValueAsInt("C_Period_ID");
	}

	public void setAD_Table_ID(int AD_Table_ID) {
		set_Value("AD_Table_ID", AD_Table_ID);
	}

	public int getAD_Table_ID() {
		return get_ValueAsInt("AD_Table_ID");
	}

	public void setRecord_ID(int Record_ID) {
		set_Value("Record_ID", Record_ID);
	}

	public int getRecord_ID() {
		return get_ValueAsInt("Record_ID");
	}

	public void setLine_ID(int Line_ID) {
		set_Value("Line_ID", Line_ID);
	}

	public int getLine_ID() {
		return get_ValueAsInt("Line_ID");
	}

	public void setDescription(String Description) {
		set_Value("Description", Description);
	}

	public String getDescription() {
		return get_ValueAsString("Description");
	}

	public void setProcessed(Boolean Processed) {
		set_Value("Processed", Boolean.valueOf(Processed));
	}

	public Boolean isProcessed() {
		Object oo = get_Value("Processed");
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public void setPosted(Boolean Posted) {
		set_Value("Posted", Boolean.valueOf(Posted));
	}

	public Boolean isPosted() {
		Object oo = get_Value("Posted");
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public void setRule_Matched(String Rule_Matched) {
		set_Value("Rule_Matched", Rule_Matched);
	}

	public String getRule_Matched() {
		return get_ValueAsString("Rule_Matched");
	}

	public String getUUID() {
		return get_ValueAsString("Fact_CashFlow_UU");
	}

	@Override
	protected int get_AccessLevel() {
		return 3;
	}
}