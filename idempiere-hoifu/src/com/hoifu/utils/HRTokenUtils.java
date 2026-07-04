package com.hoifu.utils;

import java.util.HashMap;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * HR系统接口工具类 - Token获取（每次重新登录，不缓存） - 部门/职位/员工数据拉取 所有配置从 MSysConfig 读取
 * 
 * @author liudaihu
 */
public class HRTokenUtils {

	private static final CLogger log = CLogger.getCLogger(HRTokenUtils.class);

	// HR API 路径
	private static final String API_AUTH = "/api/Auth/login";
	private static final String API_DEPT = "/api/Employees/Departments";
	private static final String API_JOB = "/api/Employees/Jobs";
	private static final String API_EMP = "/api/Employees/Employees";

	// =========================================================
	// Token 获取（每次重新登录，不缓存）
	// =========================================================

	/**
	 * 获取HR系统Token（每次重新登录，不缓存）
	 * 
	 * @param clientId 租户ID，用于读取MSysConfig
	 */
	public static String fetchToken(int clientId) throws Exception {
		String baseUrl = getBaseUrl(clientId);
		String userId = MSysConfig.getValue(MSysConfig.HR_API_USER_ID, "api", clientId);
		String password = MSysConfig.getValue(MSysConfig.HR_API_PASSWORD, "ABCabc123", clientId);
		int companyId = MSysConfig.getIntValue(MSysConfig.HR_API_COMPANY_ID_JS, 5, clientId);

		JSONObject body = new JSONObject();
		body.put("userID", userId);
		body.put("password", password);
		body.put("companyID", companyId);

		String resp = HttpClientUtils.post(baseUrl + API_AUTH, body.toString(), null);
		JSONObject json = new JSONObject(resp);
		if (json.optInt("code", -1) != 200)
			throw new AdempiereException("HR登录失败: " + json.optString("message"));

		String token = json.getJSONObject("data").getString("token");
		log.info("HR Token获取成功");
		return token;
	}

	// =========================================================
	// 部门接口
	// =========================================================

	/**
	 * 拉取HR系统部门列表
	 */
	public static JSONArray fetchDepartments(String token, int clientId) throws Exception {
		String resp = doPost(API_DEPT, token, clientId);
		JSONObject json = new JSONObject(resp);
		if (json.optInt("code", -1) != 200)
			throw new AdempiereException("获取部门列表失败: " + json.optString("message"));
		return json.getJSONArray("data");
	}

	// =========================================================
	// 职位接口
	// =========================================================

	/**
	 * 拉取HR系统职位列表
	 */
	public static JSONArray fetchJobs(String token, int clientId) throws Exception {
		String resp = doPost(API_JOB, token, clientId);
		JSONObject json = new JSONObject(resp);
		if (json.optInt("code", -1) != 200)
			throw new AdempiereException("获取职位列表失败: " + json.optString("message"));
		return json.getJSONArray("data");
	}

	// =========================================================
	// 员工接口
	// =========================================================

	/**
	 * 拉取HR系统员工列表
	 */
	public static JSONArray fetchEmployees(String token, int clientId) throws Exception {
		String resp = doPost(API_EMP, token, clientId);
		JSONObject json = new JSONObject(resp);
		if (json.optInt("code", -1) != 200)
			throw new AdempiereException("获取员工列表失败: " + json.optString("message"));
		return json.getJSONArray("data");
	}

	// =========================================================
	// 私有工具方法
	// =========================================================

	private static String doPost(String apiPath, String token, int clientId) throws Exception {
		String baseUrl = getBaseUrl(clientId);
		String userId = MSysConfig.getValue(MSysConfig.HR_API_USER_ID, "api", clientId);
		int companyId = MSysConfig.getIntValue(MSysConfig.HR_API_COMPANY_ID_JS, 5, clientId);

		JSONObject basClass = new JSONObject();
		basClass.put("companyID", companyId);
		basClass.put("userID", userId);
		JSONObject body = new JSONObject();
		body.put("basClass", basClass);

		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Bearer " + token);
		headers.put("Content-Type", "application/json");

		return HttpClientUtils.post(baseUrl + apiPath, body.toString(), headers);
	}

	private static String getBaseUrl(int clientId) {
		String url = MSysConfig.getValue(MSysConfig.HR_API_BASE_URL, null, clientId);
		if (url == null || url.isEmpty())
			throw new AdempiereException("HR_API_BASE_URL 未配置，请检查 MSysConfig");
		return url;
	}
}