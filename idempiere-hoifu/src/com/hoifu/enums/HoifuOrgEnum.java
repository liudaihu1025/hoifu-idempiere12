package com.hoifu.enums;

public enum HoifuOrgEnum {

	YANBAO_ZHUSHUJU("0411", "烟包主数据"),
	JIANGSU_HAIFU_YANBAO("0211", "江苏海富烟包");

	private final String value;
	private final String orgName;

	HoifuOrgEnum(String value, String orgName) {
		this.value = value;
		this.orgName = orgName;
	}

	public String getValue() {
		return value;
	}

	public String getOrgName() {
		return orgName;
	}

	public static HoifuOrgEnum getByValue(String value) {
		for (HoifuOrgEnum e : values()) {
			if (e.getValue().equals(value)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * 判断 orgValue 是否属于给定的目标组织集合中的任意一个。
	 * 调用方显式传入本次业务规则关心的组织，而不是隐式依赖枚举里定义的"全部"组织，
	 * 这样即使以后枚举里新增其他用途的组织常量，也不会影响到已有规则的判断范围。
	 */
	public static boolean isMemberValue(String orgValue, HoifuOrgEnum... targets) {
		if (orgValue == null || targets == null) {
			return false;
		}
		for (HoifuOrgEnum t : targets) {
			if (t.getValue().equals(orgValue)) {
				return true;
			}
		}
		return false;
	}
}
