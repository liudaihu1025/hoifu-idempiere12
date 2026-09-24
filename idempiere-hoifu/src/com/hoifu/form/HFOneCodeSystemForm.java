package com.hoifu.form;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.window.Dialog;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONObject;
import org.zkoss.zul.Iframe;

import com.hoifu.enums.HFSysConfigEnum;
import com.hoifu.utils.HFSsoUtils;

/**
 * 海富一物一码系统
 * 
 * @ClassName: HFOneCodeSystemForm
 * @author ldh
 * @date 2026年7月29日
 */
@org.idempiere.ui.zk.annotation.Form
public class HFOneCodeSystemForm extends ADForm {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(HFOneCodeSystemForm.class);

	@Override
	protected void initForm() {
		String redirectUrl = null;
		try {
			redirectUrl = generateSsoRedirectUrl();
		} catch (Exception e) {
			log.log(Level.SEVERE, "SSO 单点登录接口调用失败", e);
		}

		if (redirectUrl == null || redirectUrl.isEmpty()) {
			// 无法完成 SSO 跳转，提示用户使用 AD 账号登录，不再跳转 iframe
			Dialog.warn(0, "无法自动登录，请使用 AD 账号登录");
			return;
		}

		Iframe iframe = new Iframe();
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		iframe.setSrc(redirectUrl);
		this.appendChild(iframe);
	}

	/**
	 * 生成 SSO 跳转地址
	 * 
	 * @return 完整的跳转 URL；如果无法生成（缺少 ldapUser、接口异常、缺少 loginUrl 等），返回 null
	 */
	private String generateSsoRedirectUrl() throws Exception {
		String apiDomainUrl = HFSysConfigEnum.HF_ONECODE_SSO_DOMAIN_URL.getValue();
		String environment = HFSysConfigEnum.HF_ONECODE_SSO_ENVIRONMENT.getValue();

		String ldapUser = HFSsoUtils.getCurrentLdapUser();
		if (ldapUser == null) {
			log.warning("未获取到当前用户的 LDAP 账号，无法进行 SSO 登录");
			return null;
		}
		int adOrgId = Env.getAD_Org_ID(Env.getCtx());
		if (adOrgId < 0) {
			log.warning("无法获取当前登录组织 ID");
			return null;
		}

		// 1. 生成签名
		JSONObject requestBody = new JSONObject();
		requestBody.put("username", ldapUser);
		requestBody.put("environment", environment);
		requestBody.put("companyCode", adOrgId);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		String response = HFSsoUtils.callSsoApiIgnoreSSL(apiDomainUrl + "/api/v1/sso/erp/generate-signature",
				requestBody, headers);

		JSONObject responseJson = new JSONObject(response);
		int code = responseJson.optInt("code", -1);
		if (code != 200) {
			log.warning("SSO 生成签名接口返回异常，code=" + code + ", response=" + response);
			return null;
		}

		JSONObject data = responseJson.optJSONObject("data");
		if (data == null) {
			log.warning("SSO 生成签名接口未返回 data，response=" + response);
			return null;
		}

		// data.loginUrl 是相对路径，形如：
		// /api/v1/sso/erp-callback?username=...&timestamp=...&signature=...
		String relativeLoginUrl = data.optString("loginUrl", null);
		if (relativeLoginUrl == null || relativeLoginUrl.isEmpty()) {
			log.warning("SSO 生成签名接口未返回 loginUrl，response=" + response);
			return null;
		}

		// 2. 拼出完整跳转地址（浏览器/iframe 直接访问该地址即完成登录）
		return apiDomainUrl + relativeLoginUrl;
	}
}