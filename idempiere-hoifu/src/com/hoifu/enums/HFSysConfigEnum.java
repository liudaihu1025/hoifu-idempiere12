package com.hoifu.enums;

import org.compiere.model.MSysConfig;
import org.compiere.util.Env;

/**
 * HF 自定义 AD_SysConfig Key 枚举。 统一管理 com.hoifu 包下用到的 System Configurator key，
 * 避免散落各处的魔法字符串，也方便后续新增/查找配置项。
 */
public enum HFSysConfigEnum {

	/**
	 * 简单查找窗口的级联下拉菜单规则配置。
	 * 格式：TableName|ParentColumn|ChildColumn|ChildLinkColumn|ChildCondition;...
	 * 例如：M_Product|M_Product_Category_ID_L1|M_Product_Category_ID_L2|M_Product_Category.M_Product_Category_Parent_ID|M_Product_Category.Category_Type='B'
	 * AND M_Product_Category.IsActive='Y'
	 */
	HF_FIND_WINDOW_CASCADE_RULES("HF_FIND_WINDOW_CASCADE_RULES", ""),

	// 一物一码系统域名端口  https://app.hoifu.com.cn:8011
	HF_ONECODE_SSO_DOMAIN_URL("HF_ONECODE_SSO_DOMAIN_URL", "https://app.hoifu.com.cn:8011"),

	// 一物一码系统环境  test：测试环境，production：生产环境
	HF_ONECODE_SSO_ENVIRONMENT("HF_ONECODE_SSO_ENVIRONMENT", "test"),

	// 计价单系统域名端口
	HF_PRICING_SSO_DOMAIN_URL("HF_PRICING_SSO_DOMAIN_URL", "https://app.hoifu.com.cn:8010"),

	// 计价单系统API KEY
	HF_PRICING_SSO_API_KEY("HF_PRICING_SSO_API_KEY", ""),

	// 计价单系统重定向页面
	HF_PRICING_SSO_REDIRECT_PAGE("HF_PRICING_SSO_REDIRECT_PAGE", "dashboard"),

	// 计价单系统环境  development：测试环境，production：生产环境
	HF_PRICING_SSO_ENVIRONMENT("HF_PRICING_SSO_ENVIRONMENT", "development"),

	/**
	 * 同步数据到WMS系统开关，默认关
	 */
	WMS_SYNC_ENABLED("WMS_SYNC_ENABLED", "N"),

	/**
	 * 同步数据到WMS系统的对应组织开关，默认-1 全部
	 */
	WMS_SYNC_ORG_IDS("WMS_SYNC_ORG_IDS", "-1"),

	/**
	 * WMS系统API接口前缀，如：https://wms.com.cn
	 */
	WMS_API_URL_PREFIX("WMS_API_URL_PREFIX", ""),
	;

	private final String key;
	private final String defaultValue;

	HFSysConfigEnum(String key, String defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
	}

	public String getKey() {
		return key;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	/** 使用枚举自带的默认值，按当前 Client 读取配置 */
	public String getValue() {
		return getValue(Env.getAD_Client_ID(Env.getCtx()));
	}

	/** 按指定 AD_Client_ID 读取配置，使用枚举自带的默认值 */
	public String getValue(int adClientId) {
		return MSysConfig.getValue(key, defaultValue, adClientId);
	}

	/** 按指定 AD_Client_ID 和自定义默认值读取配置 */
	public String getValue(int adClientId, String customDefault) {
		return MSysConfig.getValue(key, customDefault, adClientId);
	}
}