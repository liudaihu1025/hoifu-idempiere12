package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/**
 * Generated Model for Ext_System_Sync_Log   AD字典中登记的
 * Ext_System_Sync_Log表结构一一对应，仅做字段存取，不含任何业务方法（业务方法放到 M_Ext_System_Sync_Log）。
 */
@org.adempiere.base.Model(table = "Ext_System_Sync_Log")
public class X_Ext_System_Sync_Log extends PO implements I_Ext_System_Sync_Log, I_Persistent {
	private static final long serialVersionUID = 20260818L;

	public static final String Table_Name = "Ext_System_Sync_Log";

	/** AD_Table_ID 需与 AD 字典中注册的一致，此处通过 MTable 动态解析，避免硬编码 */
	public static final int Table_ID = MTable.getTable_ID(Table_Name);

	/** Standard Constructor */
	public X_Ext_System_Sync_Log(Properties ctx, int Ext_System_Sync_Log_ID, String trxName) {
		super(ctx, Ext_System_Sync_Log_ID, trxName);
		if (Ext_System_Sync_Log_ID == 0) {
			setIsSuccess(false);
			setStatus("PENDING");
			setRetry_Count(0);
		}
	}

	/** Load Constructor */
	public X_Ext_System_Sync_Log(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/** AccessLevel: 3 - Client - Org */
	protected int get_AccessLevel() {
		return accessLevel.intValue();
	}

	protected POInfo initPO(Properties ctx) {
		return POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("X_Ext_System_Sync_Log[").append(get_ID()).append(",SystemType=")
				.append(getSystem_Type()).append(",BusinessType=").append(getBusiness_Type()).append("]");
		return sb.toString();
	}

	@Override
	public void setSystem_Type(String System_Type) {
		set_ValueNoCheck(COLUMNNAME_System_Type, System_Type);
	}

	@Override
	public String getSystem_Type() {
		return (String) get_Value(COLUMNNAME_System_Type);
	}

	@Override
	public void setAD_Table_ID(int AD_Table_ID) {
		set_ValueNoCheck(COLUMNNAME_AD_Table_ID, AD_Table_ID);
	}

	@Override
	public int getAD_Table_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_AD_Table_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setRecord_ID(int Record_ID) {
		set_ValueNoCheck(COLUMNNAME_Record_ID, Record_ID);
	}

	@Override
	public int getRecord_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_Record_ID);
		return ii == null ? 0 : ii;
	}

	@Override
	public void setBusiness_Type(String Business_Type) {
		set_ValueNoCheck(COLUMNNAME_Business_Type, Business_Type);
	}

	@Override
	public String getBusiness_Type() {
		return (String) get_Value(COLUMNNAME_Business_Type);
	}

	@Override
	public void setEvent_Type(String Event_Type) {
		set_ValueNoCheck(COLUMNNAME_Event_Type, Event_Type);
	}

	@Override
	public String getEvent_Type() {
		return (String) get_Value(COLUMNNAME_Event_Type);
	}

	@Override
	public void setAPI_URL(String API_URL) {
		set_ValueNoCheck(COLUMNNAME_API_URL, API_URL);
	}

	@Override
	public String getAPI_URL() {
		return (String) get_Value(COLUMNNAME_API_URL);
	}

	@Override
	public void setRequest_JSON(String Request_JSON) {
		set_ValueNoCheck(COLUMNNAME_Request_JSON, Request_JSON);
	}

	@Override
	public String getRequest_JSON() {
		return (String) get_Value(COLUMNNAME_Request_JSON);
	}

	@Override
	public void setResponse_JSON(String Response_JSON) {
		set_ValueNoCheck(COLUMNNAME_Response_JSON, Response_JSON);
	}

	@Override
	public String getResponse_JSON() {
		return (String) get_Value(COLUMNNAME_Response_JSON);
	}

	@Override
	public void setIsSuccess(boolean IsSuccess) {
		set_ValueNoCheck(COLUMNNAME_IsSuccess, IsSuccess);
	}

	@Override
	public boolean isSuccess() {
		Object oo = get_Value(COLUMNNAME_IsSuccess);
		if (oo instanceof Boolean)
			return (Boolean) oo;
		return "Y".equals(oo);
	}

	@Override
	public void setError_Message(String Error_Message) {
		set_ValueNoCheck(COLUMNNAME_Error_Message, Error_Message);
	}

	@Override
	public String getError_Message() {
		return (String) get_Value(COLUMNNAME_Error_Message);
	}

	@Override
	public void setExecution_Time(BigDecimal Execution_Time) {
		set_ValueNoCheck(COLUMNNAME_Execution_Time, Execution_Time);
	}

	@Override
	public BigDecimal getExecution_Time() {
		return (BigDecimal) get_Value(COLUMNNAME_Execution_Time);
	}

	@Override
	public void setStatus(String Status) {
		set_ValueNoCheck(COLUMNNAME_Status, Status);
	}

	@Override
	public String getStatus() {
		return (String) get_Value(COLUMNNAME_Status);
	}

	@Override
	public void setRetry_Count(int Retry_Count) {
		set_ValueNoCheck(COLUMNNAME_Retry_Count, Retry_Count);
	}

	@Override
	public int getRetry_Count() {
		Integer ii = (Integer) get_Value(COLUMNNAME_Retry_Count);
		return ii == null ? 0 : ii;
	}
}