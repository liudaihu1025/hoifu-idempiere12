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
 * 海富计价单系统
 * 
 * @ClassName: HFPricingSheetSystemForm
 * @author ldh
 * @date 2026年7月29日
 */
@org.idempiere.ui.zk.annotation.Form
public class HFPricingSheetSystemForm extends ADForm {

	private static final long serialVersionUID = 443856189700880853L;
	private static final CLogger log = CLogger.getCLogger(HFPricingSheetSystemForm.class);

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
	 * @return 完整的跳转 URL；如果无法生成（配置不完整、缺少 ldapUser、接口未返回 redirect_url 等），返回 null
	 */
	private String generateSsoRedirectUrl() throws Exception {
		String apiDomainUrl = HFSysConfigEnum.HF_PRICING_SSO_DOMAIN_URL.getValue();
		String apiKey = HFSysConfigEnum.HF_PRICING_SSO_API_KEY.getValue();
		String redirectPage = HFSysConfigEnum.HF_PRICING_SSO_REDIRECT_PAGE.getValue();
		String env = HFSysConfigEnum.HF_PRICING_SSO_ENVIRONMENT.getValue();

		if (apiKey == null || apiKey.isEmpty()) {
			log.warning("SSO 配置不完整，请检查 MSysConfig: " + HFSysConfigEnum.HF_PRICING_SSO_DOMAIN_URL.getKey() + ", "
					+ HFSysConfigEnum.HF_PRICING_SSO_API_KEY.getKey());
			return null;
		}

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

		JSONObject requestBody = new JSONObject();
		requestBody.put("username", ldapUser);
		requestBody.put("env", env);
		requestBody.put("companyCode", adOrgId);
		requestBody.put("redirect_page", redirectPage);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-SSO-API-Key", apiKey);

		String response = HFSsoUtils.callSsoApiIgnoreSSL(apiDomainUrl + "/api/auth/sso/generate", requestBody, headers);

		JSONObject responseJson = new JSONObject(response);
		String redirectUrl = responseJson.optString("redirect_url", null);

		if (redirectUrl == null || redirectUrl.isEmpty()) {
			log.warning("SSO 接口未返回 redirect_url，response=" + response);
			return null;
		}

		return redirectUrl;
	}
}