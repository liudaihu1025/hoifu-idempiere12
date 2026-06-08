package com.hoifu.utils;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.json.JSONObject;

/**
 * @Description: OA Token管理服务 - 使用CCache缓存Token
 * @author ldh
 * @date 2025年11月11日
 */
public class OATokenUtils {

	private static final CLogger log = CLogger.getCLogger(OATokenUtils.class);

	// Token缓存 - 键为configId, 值为TokenInfo对象, 100容量,60分钟过期
	private static final CCache<Integer, TokenInfo> tokenCache = new CCache<>("OA_Token_Cache", 100, 60, false);

	// OA接口URL-获取token
	public static final String OA_API_URL_TOKEN = "/api/auth/token";

	/**
	 * 
	 * @Description: Token信息内部类
	 * @author ldh
	 * @date 2025年11月11日
	 */
	private static class TokenInfo {
		String accessToken;
		long timestamp;
		long expireSeconds;

		TokenInfo(String accessToken, long expireSeconds) {
			this.accessToken = accessToken;
			this.timestamp = System.currentTimeMillis();
			this.expireSeconds = expireSeconds;
		}

		boolean isExpired() {
			long expireTime = timestamp + (expireSeconds * 1000);
			return System.currentTimeMillis() >= expireTime;
		}
	}

	/**
	 * 
	 * @Description: 获取有效的Token,如果缓存中的Token未过期则直接返回,否则重新获取
	 * @param configId
	 * @return
	 * @throws Exception
	 */
	public static String getValidToken(int configId) throws Exception {

		// 1. 从缓存中获取Token
		TokenInfo tokenInfo = tokenCache.get(configId);

		// 2. 检查Token是否有效
		if (tokenInfo != null && !tokenInfo.isExpired()) {
			log.fine("Using cached token for config: " + configId);
			return tokenInfo.accessToken;
		}

		// 3. Token不存在或已过期,重新获取
		return refreshToken(configId);
	}

	/**
	 * 
	 * @Description: 刷新Token - 从MSysConfig读取配置并调用OA Token接口获取新Token
	 * @param ctx
	 * @param configId
	 * @param trxName
	 * @return
	 * @throws Exception
	 */
	private static String refreshToken(int configId) throws Exception {

		// 1. 从MSysConfig读取Token接口配置
		String oaApiUrlPrefix = MSysConfig.getValue(MSysConfig.OA_API_URL_PREFIX, null, 0);
		String userID = MSysConfig.getValue(MSysConfig.OA_TOKEN_USER_ID, null, 0);
		String password = MSysConfig.getValue(MSysConfig.OA_TOKEN_PASSWORD, null, 0);
		int expireSeconds = MSysConfig.getIntValue(MSysConfig.OA_TOKEN_EXPIRE_SECONDS, 3600, 0);

		if (oaApiUrlPrefix == null || userID == null || password == null) {
			throw new AdempiereException("接口URL配置不完整,请检查MSysConfig中的配置");
		}

		// 2. 构建Token请求
		JSONObject tokenRequest = new JSONObject();
		tokenRequest.put("userID", userID);
		tokenRequest.put("password", password);

		// 3. 调用Token接口
		String tokenResponse = HttpClientUtils.post(oaApiUrlPrefix + OA_API_URL_TOKEN, tokenRequest.toString(), null);

		// 4. 解析Token响应
		JSONObject responseJson = new JSONObject(tokenResponse);
		String accessToken = responseJson.optString("access_token");
		if (accessToken == null || accessToken.isEmpty()) {
			accessToken = responseJson.optString("token");
		}

		if (accessToken == null || accessToken.isEmpty()) {
			throw new AdempiereException("Failed to get access token from response: " + tokenResponse);
		}

		// 5. 缓存Token
		TokenInfo tokenInfo = new TokenInfo(accessToken, expireSeconds);
		tokenCache.put(configId, tokenInfo);

		log.info("Successfully refreshed token for config: " + configId);
		return accessToken;
	}

	/**
	 * 
	 * @Description: 清除指定配置的Token缓存
	 * @param configId
	 */
	public static void clearToken(int configId) {
		tokenCache.remove(configId);
	}

	/**
	 * 清除所有Token缓存
	 */
	public static void clearAllTokens() {
		tokenCache.clear();
	}
}