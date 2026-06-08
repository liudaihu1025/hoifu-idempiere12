package com.hoifu.service;

import java.util.HashMap;
import java.util.Map;

import org.compiere.util.CLogger;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;

import com.hoifu.utils.HttpClientUtils;

/**
 * @Description: OA集成服务
 * @author ldh
 * @date 2025年11月5日
 */
@Component(immediate = true, service = OAIntegrationService.class)
public class OAIntegrationService {

	private static final CLogger logger = CLogger.getCLogger(OAIntegrationService.class);

	/**
	 * 发送数据到OA系统(不带Token)
	 * 
	 * @param apiUrl OA接口URL
	 * @param data   请求数据
	 * @return OA响应
	 */
	public String sendToOA(String apiUrl, JSONObject data) throws Exception {
		logger.info("发送数据到OA系统: " + apiUrl);
		logger.fine("请求数据: " + data.toString());

		return HttpClientUtils.post(apiUrl, data.toString(), null);
	}

	/**
	 * 发送数据到OA系统(带Token认证)
	 * 
	 * @param apiUrl OA接口URL
	 * @param data   请求数据
	 * @param token  Bearer Token
	 * @return OA响应
	 */
	public String sendToOAWithToken(String apiUrl, JSONObject data, String token) throws Exception {
		logger.info("发送数据到OA系统(带Token): " + apiUrl);
		logger.fine("请求数据: " + data.toString());

		// 构建请求头,添加Authorization
		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Bearer " + token);

		return HttpClientUtils.post(apiUrl, data.toString(), headers);
	}

	/**
	 * 发送数据到OA系统(自定义请求头)
	 * 
	 * @param apiUrl  OA接口URL
	 * @param data    请求数据
	 * @param headers 自定义请求头
	 * @return OA响应
	 */
	public String sendToOA(String apiUrl, JSONObject data, Map<String, String> headers) throws Exception {
		logger.info("发送数据到OA系统(自定义请求头): " + apiUrl);
		logger.fine("请求数据: " + data.toString());

		return HttpClientUtils.post(apiUrl, data.toString(), headers);
	}
}