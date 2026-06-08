package com.hoifu.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;

/**
 * @Description: HTTP客户端 - 使用HttpURLConnection实现
 * @author ldh
 * @date 2025年11月11日
 */
public class HttpClientUtils {

	private static final CLogger log = CLogger.getCLogger(HttpClientUtils.class);
	private static final int TIMEOUT = 30000; // 30秒超时

	/**
	 * 
	 * @Description: 发送POST请求
	 * @param url      API接口URL
	 * @param jsonBody 请求体参数
	 * @param headers  请求头参数
	 * @return
	 * @throws Exception
	 */
	public static String post(String url, String jsonBody, Map<String, String> headers) throws Exception {
		HttpURLConnection conn = null;
		try {
			// 创建URL对象并打开连接
			URL urlObj = new URL(url);
			conn = (HttpURLConnection) urlObj.openConnection();

			// 设置请求方法和基本属性
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);

			// 添加自定义请求头(如Authorization)
			if (headers != null) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			if (log.isLoggable(Level.FINE)) {
				log.fine("POST URL: " + url);
				log.fine("Request Body: " + jsonBody);
			}

			// 发送请求体
			if (jsonBody != null && !jsonBody.isEmpty()) {
				try (OutputStream os = conn.getOutputStream()) {
					byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
					os.write(input, 0, input.length);
					os.flush();
				}
			}

			// 获取响应码
			int responseCode = conn.getResponseCode();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Response Code: " + responseCode);
			}

			// 读取响应内容
			InputStream is = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

			if (is == null) {
				throw new AdempiereException("No response from server");
			}

			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					response.append(line);
				}
			}

			String responseStr = response.toString();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Response: " + responseStr);
			}

			// 检查HTTP错误
			if (responseCode >= 400) {
				throw new AdempiereException("HTTP Error " + responseCode + ": " + responseStr);
			}

			return responseStr;

		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to call OA API: " + url, e);
			throw new AdempiereException("Failed to call OA API: " + e.getMessage(), e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	/**
	 * 
	 * HTTP POST 文件上传 请求方法
	 * 
	 * @param url
	 * @param jsonBody
	 * @param headers
	 * @param requestBody
	 * @return
	 * @throws Exception
	 */
	public static String postUpload(String url, Map<String, String> headers, byte[] requestBody) throws Exception {
		HttpURLConnection conn = null;
		try {
			// 创建URL对象并打开连接
			URL urlObj = new URL(url);
			conn = (HttpURLConnection) urlObj.openConnection();

			// 设置请求方法和基本属性
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			// 文件上传请求
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);

			// 添加自定义请求头
			if (headers != null) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			// 发送请求体
			if (requestBody != null) {
				try (OutputStream os = conn.getOutputStream()) {
					os.write(requestBody, 0, requestBody.length);
					os.flush();
				}
			}

			// 获取响应码
			int responseCode = conn.getResponseCode();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Response Code: " + responseCode);
			}

			// 读取响应内容
			InputStream is = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

			if (is == null) {
				throw new AdempiereException("No response from server");
			}

			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					response.append(line);
				}
			}

			String responseStr = response.toString();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Response: " + responseStr);
			}

			// 检查HTTP错误
			if (responseCode >= 400) {
				throw new AdempiereException("HTTP Error " + responseCode + ": " + responseStr);
			}

			return responseStr;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to call OA API: " + url, e);
			throw new AdempiereException("Failed to call OA API: " + e.getMessage(), e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	/**
	 * 
	 * @Description: 发送GET请求
	 * @param url
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	public static String get(String url, Map<String, String> headers) throws Exception {
		HttpURLConnection conn = null;
		try {
			URL urlObj = new URL(url);
			conn = (HttpURLConnection) urlObj.openConnection();

			conn.setRequestMethod("GET");
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);
			conn.setDoInput(true);

			// 添加自定义请求头
			if (headers != null) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			if (log.isLoggable(Level.FINE)) {
				log.fine("GET URL: " + url);
			}

			int responseCode = conn.getResponseCode();

			InputStream is = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

			if (is == null) {
				throw new AdempiereException("No response from server");
			}

			StringBuilder response = new StringBuilder();
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = is.read(buffer)) != -1) {
				for (int i = 0; i < bytesRead; i++) {
					if (buffer[i] != 10) { // 跳过换行符
						response.append((char) buffer[i]);
					}
				}
			}

			String responseStr = response.toString();

			if (responseCode >= 400) {
				throw new AdempiereException("HTTP Error " + responseCode + ": " + responseStr);
			}

			return responseStr;

		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to call OA API: " + url, e);
			throw new AdempiereException("Failed to call OA API: " + e.getMessage(), e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
}