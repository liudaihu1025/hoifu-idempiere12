package com.hoifu.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

public class MCashFlowConfig extends PO {

	private static final long serialVersionUID = 4958481207015736579L;

	public static final String Table_Name = "C_CashFlow_Config";
	public static final int Table_ID = getTableID(Table_Name);

	// 添加静态方法
	public static int getTableID(String tableName) {
		return MTable.getTable_ID(tableName);
	}

	public MCashFlowConfig(Properties ctx, int C_CashFlow_Config_ID, String trxName) {
		super(ctx, C_CashFlow_Config_ID, trxName);
	}

	public MCashFlowConfig(Properties ctx, ResultSet rs, String trxName) {
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
	public void setIsEnabled(Boolean IsEnabled) {
		set_Value ("IsEnabled", Boolean.valueOf(IsEnabled));  
	}

	public Boolean isEnabled() {
		Object oo = get_Value("IsEnabled");  
	    if (oo != null)  
	    {  
	        if (oo instanceof Boolean)  
	            return ((Boolean)oo).booleanValue();  
	        return "Y".equals(oo);  
	    }  
	    return false; 
	}

	public void setSyncInterval(int SyncInterval) {
		set_Value("SyncInterval", SyncInterval);
	}

	public int getSyncInterval() {
		return get_ValueAsInt("SyncInterval");
	}

	public void setLastSyncTime(Timestamp LastSyncTime) {
		set_Value("LastSyncTime", LastSyncTime);
	}

	public Timestamp getLastSyncTime() {
		return (Timestamp) get_Value("LastSyncTime");
	}

	public void setCashAccount_Pattern(String CashAccount_Pattern) {
		set_Value("CashAccount_Pattern", CashAccount_Pattern);
	}

	public String getCashAccount_Pattern() {
		return get_ValueAsString("CashAccount_Pattern");
	}

	public String getUUID() {
		return get_ValueAsString("C_CashFlow_Config_UU");
	}

	@Override
	protected int get_AccessLevel() {
		return 3;
	}
}