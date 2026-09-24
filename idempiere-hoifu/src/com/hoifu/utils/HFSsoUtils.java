package com.hoifu.utils;

import java.util.logging.Level;

import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

/**
 * 海富外部系统 SSO 单点登录公共工具类
 * @ClassName: HFSsoUtils
 * @author ldh
 * @date 2026年7月29日
 */
public class HFSsoUtils {

	private static final CLogger log = CLogger.getCLogger(HFSsoUtils.class);

	/**
	 * 获取当前登录用户的 LDAPUser
	 * 
	 * @return LDAPUser，取不到返回 null
	 */
	public static String getCurrentLdapUser() {
		int adUserId = Env.getAD_User_ID(Env.getCtx());
		if (adUserId <= 0) {
			log.warning("无法获取当前登录用户 ID");
			return null;
		}

		MUser user = MUser.get(Env.getCtx(), adUserId);
		String ldapUser = user != null ? user.getLDAPUser() : null;
		if (ldapUser == null || ldapUser.isEmpty()) {
			log.warning("当前用户 LDAPUser 为空，AD_User_ID=" + adUserId);
			return null;
		}
		return ldapUser;
	}

	/**
	 * 调用忽略 SSL 校验的 SSO 接口（POST + JSON）
	 * 
	 * @throws Exception 网络异常或非 2xx 由调用方 catch 并处理回退逻辑
	 */
	public static String callSsoApiIgnoreSSL(String url, org.json.JSONObject requestBody,
			java.util.Map<String, String> headers) throws Exception {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Call SSO API: " + url + ", body=" + requestBody);
		}
		return HttpClientUtils.postIgnoreSSL(url, requestBody.toString(), headers);
	}
}