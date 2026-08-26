package com.hoifu.model;

import java.math.BigDecimal;

/**
 * Generated-style Model Interface for Ext_System_Sync_Log.
 * 通用外部系统集成调用日志表接口，供X_Ext_System_Sync_Log 实现。
 */
public interface I_Ext_System_Sync_Log {

	public static final String Table_Name = "Ext_System_Sync_Log";

	/** AccessLevel = 3 (Client - Organization) */
	BigDecimal accessLevel = BigDecimal.valueOf(3);

	// ===== System_Type =====
	String COLUMNNAME_System_Type = "System_Type";

	void setSystem_Type(String System_Type);

	String getSystem_Type();

	// ===== AD_Table_ID =====
	String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	void setAD_Table_ID(int AD_Table_ID);

	int getAD_Table_ID();

	// ===== Record_ID =====
	String COLUMNNAME_Record_ID = "Record_ID";

	void setRecord_ID(int Record_ID);

	int getRecord_ID();

	// ===== Business_Type =====
	String COLUMNNAME_Business_Type = "Business_Type";

	void setBusiness_Type(String Business_Type);

	String getBusiness_Type();

	// ===== Event_Type =====
	String COLUMNNAME_Event_Type = "Event_Type";

	void setEvent_Type(String Event_Type);

	String getEvent_Type();

	// ===== API_URL =====
	String COLUMNNAME_API_URL = "API_URL";

	void setAPI_URL(String API_URL);

	String getAPI_URL();

	// ===== Request_JSON =====
	String COLUMNNAME_Request_JSON = "Request_JSON";

	void setRequest_JSON(String Request_JSON);

	String getRequest_JSON();

	// ===== Response_JSON =====
	String COLUMNNAME_Response_JSON = "Response_JSON";

	void setResponse_JSON(String Response_JSON);

	String getResponse_JSON();

	// ===== IsSuccess =====
	String COLUMNNAME_IsSuccess = "IsSuccess";

	void setIsSuccess(boolean IsSuccess);

	boolean isSuccess();

	// ===== Error_Message =====
	String COLUMNNAME_Error_Message = "Error_Message";

	void setError_Message(String Error_Message);

	String getError_Message();

	// ===== Execution_Time =====
	String COLUMNNAME_Execution_Time = "Execution_Time";

	void setExecution_Time(BigDecimal Execution_Time);

	BigDecimal getExecution_Time();

	// ===== Status =====
	String COLUMNNAME_Status = "Status";

	void setStatus(String Status);

	String getStatus();

	// ===== Retry_Count =====
	String COLUMNNAME_Retry_Count = "Retry_Count";

	void setRetry_Count(int Retry_Count);

	int getRetry_Count();

}