package com.hoifu.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONObject;

import com.hoifu.utils.RetryBackoffPolicy;

/**
 * Ext_System_Sync_Log 业务模型。 直接承载写日志/查询待重试记录等方法， Status 常量: SUCCESS / FAILED /
 * RETRYING
 */
public class M_Ext_System_Sync_Log extends X_Ext_System_Sync_Log {
	private static final long serialVersionUID = 1L;

	/**
	 * Load Constructor：必须补上，否则 new M_Ext_System_Sync_Log(ctx, rs, trxName)
	 * 这种从ResultSet构造的调用会编译报错
	 */
	public M_Ext_System_Sync_Log(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	private static final CLogger log = CLogger.getCLogger(M_Ext_System_Sync_Log.class);

	public static final String STATUS_SUCCESS = "SUCCESS";
	public static final String STATUS_FAILED = "FAILED";

	public M_Ext_System_Sync_Log(Properties ctx, int Ext_System_Sync_Log_ID, String trxName) {
		super(ctx, Ext_System_Sync_Log_ID, trxName);
	}

	/**
	 * 记录一次成功调用。使用独立事务写入（trxName传null，自动提交）， 避免日志写入受主业务事务或异步任务自身状态影响，保证审计可追溯。
	 */
	public static void logSuccess(String systemType, int tableId, int recordId, String businessType, String eventType,
			String apiUrl, String request, JSONObject response, long executionTimeMs, int adClientId, int adOrgId) {
		M_Ext_System_Sync_Log m = new M_Ext_System_Sync_Log(Env.getCtx(), 0, null);
		fillCommon(m, systemType, tableId, recordId, businessType, eventType, apiUrl, request, adClientId, adOrgId, response);
		m.setIsSuccess(true);
		m.setExecution_Time(java.math.BigDecimal.valueOf(executionTimeMs));
		m.setStatus(STATUS_SUCCESS);
		m.setRetry_Count(0);
		saveQuietly(m);
	}

	/**
	 * 记录一次失败调用，并按退避策略计算下一次重试时间。
	 */
	public static void logFailure(String systemType, int tableId, int recordId, String businessType, String eventType,
			String apiUrl, String request, JSONObject response, String errorMessage, long executionTimeMs, int adClientId, int adOrgId,
			int retryCount) {
		M_Ext_System_Sync_Log m = new M_Ext_System_Sync_Log(Env.getCtx(), 0, null);
		fillCommon(m, systemType, tableId, recordId, businessType, eventType, apiUrl, request, adClientId, adOrgId, response);
		m.setIsSuccess(false);
		m.setError_Message(
				errorMessage != null && errorMessage.length() > 2000 ? errorMessage.substring(0, 2000) : errorMessage);
		m.setExecution_Time(java.math.BigDecimal.valueOf(executionTimeMs));
		m.setStatus(STATUS_FAILED);
		m.setRetry_Count(retryCount);
		saveQuietly(m);
	}

	private static void fillCommon(M_Ext_System_Sync_Log m, String systemType, int tableId, int recordId,
			String businessType, String eventType, String apiUrl, String request, int adClientId, int adOrgId, JSONObject response) {
		m.setAD_Client_ID(adClientId);
		m.setAD_Org_ID(adOrgId);
		m.setSystem_Type(systemType);
		m.setAD_Table_ID(tableId);
		m.setRecord_ID(recordId);
		m.setBusiness_Type(businessType);
		m.setEvent_Type(eventType);
		m.setAPI_URL(apiUrl);
		m.setRequest_JSON(request != null ? request.toString() : null);
		m.setResponse_JSON(response != null ? response.toString() : null);
	}

	/** 日志写入失败不应影响主业务/异步同步流程，仅记录到系统日志 */
	private static void saveQuietly(M_Ext_System_Sync_Log m) {
		try {
			m.saveEx();
		} catch (Exception e) {
			log.severe("写入 Ext_System_Sync_Log 失败: " + e.getMessage());
		}
	}

	/**
	 * 查询指定系统中的失败记录，供手动重试进程使用。
	 */
	public static List<M_Ext_System_Sync_Log> loadPendingRetries(String systemType, int limit) {
		String sql = "SELECT * FROM Ext_System_Sync_Log "
				+ "WHERE System_Type=? AND Status='FAILED'  LIMIT ?";
		List<M_Ext_System_Sync_Log> result = new ArrayList<>();
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setString(1, systemType);
			pstmt.setInt(2, limit);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					result.add(new M_Ext_System_Sync_Log(Env.getCtx(), rs, null));
				}
			}
		} catch (SQLException e) {
			log.severe("查询待重试记录失败: " + e.getMessage());
		}
		return result;
	}

	/** 重试成功后调用 */
	public void markSuccess(String response, long executionTimeMs) {
		setStatus(STATUS_SUCCESS);
		setIsSuccess(true);
		setResponse_JSON(response);
		setExecution_Time(java.math.BigDecimal.valueOf(executionTimeMs));
		setError_Message(null);
		saveEx();
	}

	/** 重试仍失败后调用，Retry_Count+1 */
	public void markRetryFailed(String errorMessage) {
		int nextCount = getRetry_Count() + 1;
		setRetry_Count(nextCount);
		setStatus(STATUS_FAILED);
		setError_Message(
				errorMessage != null && errorMessage.length() > 2000 ? errorMessage.substring(0, 2000) : errorMessage);
		saveEx();
	}
}