package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

public class MCashFlowItem extends PO {

	private static final long serialVersionUID = -7319197506993536939L;
	public static final String Table_Name = "C_CashFlow_Item";
	public static final int Table_ID = getTableID(Table_Name);

	// 添加静态方法
	public static int getTableID(String tableName) {
		return MTable.getTable_ID(tableName);
	}

	public MCashFlowItem(Properties ctx, int C_CashFlow_Item_ID, String trxName) {
		super(ctx, C_CashFlow_Item_ID, trxName);
	}

	public MCashFlowItem(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	protected POInfo initPO(Properties ctx) {
		POInfo poi = POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
		return poi;
	}

	// 标准字段
	public int getAD_Org_ID() {
		return get_ValueAsInt("AD_Org_ID");
	}

	public void setIsActive(String IsActive) {
		set_Value("IsActive", IsActive);
	}

	public void setCreated(java.sql.Timestamp Created) {
		set_Value("Created", Created);
	}

	public void setCreatedBy(int CreatedBy) {
		set_Value("CreatedBy", CreatedBy);
	}

	public void setUpdated(java.sql.Timestamp Updated) {
		set_Value("Updated", Updated);
	}

	// 业务字段
	public void setValue(String Value) {
		set_Value("Value", Value);
	}

	public String getValue() {
		return get_ValueAsString("Value");
	}

	public void setName(String Name) {
		set_Value("Name", Name);
	}

	public String getName() {
		return get_ValueAsString("Name");
	}

	public void setDescription(String Description) {
		set_Value("Description", Description);
	}

	public String getDescription() {
		return get_ValueAsString("Description");
	}

	public void setCashFlow_Type(String CashFlow_Type) {
		set_Value("CashFlow_Type", CashFlow_Type);
	}

	public String getCashFlow_Type() {
		return get_ValueAsString("CashFlow_Type");
	}

	public void setIs_CashIn(Boolean Is_CashIn) {
		set_Value ("Is_CashIn", Boolean.valueOf(Is_CashIn));  
	}

	public Boolean is_CashIn() {
		Object oo = get_Value("Is_CashIn");  
	    if (oo != null)  
	    {  
	        if (oo instanceof Boolean)  
	            return ((Boolean)oo).booleanValue();  
	        return "Y".equals(oo);  
	    }  
	    return false; 
	}

	public void setSequence(int Sequence) {
		set_Value("Sequence", Sequence);
	}

	public int getSequence() {
		return get_ValueAsInt("Sequence");
	}
	
	public void setIsSummary(Boolean IsSummary) {
		set_Value ("IsSummary", Boolean.valueOf(IsSummary));  
	}

	public Boolean isSummary() {
		Object oo = get_Value("IsSummary");  
	    if (oo != null)  
	    {  
	        if (oo instanceof Boolean)  
	            return ((Boolean)oo).booleanValue();  
	        return "Y".equals(oo);  
	    }  
	    return false; 
	}

	public void setParent_ID(int Parent_ID) {
		set_Value("Parent_ID", Parent_ID);
	}

	public int getParent_ID() {
		return get_ValueAsInt("Parent_ID");
	}

	public String getUUID() {
		return get_ValueAsString("C_CashFlow_Item_UU");
	}

	@Override
	protected int get_AccessLevel() {
		return 3;
	}
}