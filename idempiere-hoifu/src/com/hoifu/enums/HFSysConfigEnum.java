package com.hoifu.enums;

import java.math.BigDecimal;

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

	// 人工成本核算，员工月工作时长（小时） 租户级
	HF_MONTHLY_WORK_HOURS("HF_MONTHLY_WORK_HOURS", "260"),

	//人力资源类型编码
	HF_HR_RESOURCE_TYPE_VALUE("HF_HR_RESOURCE_TYPE_VALUE", "1000000"),

	// 人工成本维护企业微信群机器人 Webhook 地址
	HF_WECHAT_ROBOT_LABOR_COST_WEBHOOK_URL("HF_WECHAT_ROBOT_LABOR_COST_WEBHOOK_URL",
			"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=0571413e-224a-4c43-8c31-afc774866195"),

	// 直接资源成本要素ID
	HF_DIRECT_LABOR_COST_ELEMENT_ID("HF_DIRECT_LABOR_COST_ELEMENT_ID", "1000023"),

	// 生产报工班组成员直接人工成本数据未维护则限制报工开关（Y检查，N不检查）
	HF_ENABLE_DIRECT_LABOR_COST_CHECK("HF_ENABLE_DIRECT_LABOR_COST_CHECK", "Y"),

	// UReport报表系统URL
	HF_UREPORT_URL("HF_UREPORT_URL", "http://192.168.1.118:8090/"),

	/**
	 * HR同步排除的部门代码列表，命中该列表中的部门代码时不同步该员工。 支持配置多个部门，英文逗号分隔，例如：D001,D002,D003
	 * 默认空，表示不排除任何部门。
	 */
	HR_SYNC_EXCLUDE_DEPT_CODES("HR_SYNC_EXCLUDE_DEPT_CODES", ""),

	// 收取确认类型ID：发货确认
	HF_REVENUE_RECOGNITION_SHIPMENT_TYPE_ID("HF_REVENUE_RECOGNITION_SHIPMENT_TYPE_ID", "1000004"),

	//暂估应收冲减方式   A=优先冲暂估应收，R=优先冲正式应收
	HF_ACCRUED_RECEIVABLE_REVERSAL_TYPE("HF_AccruedReceivableReversalType", "A"),

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

	/** 按指定 AD_Client_ID 读取配置，转为 int，使用枚举自带的默认值（默认值需能转成 int） */
	public int getIntValue(int adClientId) {
		return MSysConfig.getIntValue(key, Integer.parseInt(defaultValue), adClientId);
	}

	/** 按指定 AD_Client_ID 读取配置并转为 boolean（"Y"视为true，其余视为false），使用枚举自带的默认值 */
	public boolean getBooleanValue(int adClientId) {
		boolean defaultBool = "Y".equalsIgnoreCase(defaultValue);
		return MSysConfig.getBooleanValue(key, defaultBool, adClientId);
	}

	/** 按指定 AD_Client_ID 读取配置并转为 BigDecimal，若配置项非法数字则退回默认值对应的 BigDecimal */
	public BigDecimal getBigDecimalValue(int adClientId) {
		String value = getValue(adClientId);
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return new BigDecimal(defaultValue);
		}
	}

	// =========按租户+组织查询=========

	/** 按指定 AD_Client_ID + AD_Org_ID 读取配置，使用枚举自带的默认值 */
	public String getValue(int adClientId, int adOrgId) {
		return MSysConfig.getValue(key, defaultValue, adClientId, adOrgId);
	}

	/** 按指定 AD_Client_ID + AD_Org_ID 读取配置，转为 int，使用枚举自带的默认值 */
	public int getIntValue(int adClientId, int adOrgId) {
		return MSysConfig.getIntValue(key, Integer.parseInt(defaultValue), adClientId, adOrgId);
	}
}