package com.hoifu.service.extsync;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.compiere.model.MSysConfig;
import org.compiere.util.Env;

/**
 * 通用外部系统同步开关服务。 
 * 1.Key按 {systemType}_SYNC_ENABLED /{systemType}_SYNC_ORG_IDS 统一拼接， 
 * 2.新增系统（如MES）只需要在AD_SysConfig里维护MES_SYNC_ENABLED / MES_SYNC_ORG_IDS 两条配置， 本类代码零改动
 * @ClassName: ExtSyncSwitchService
 * @author ldh
 * @date 2026年8月19日
 */
public class ExtSyncSwitchService {

	private static final String ORG_ALL = "-1";
	private static final String KEY_SUFFIX_ENABLED = "_SYNC_ENABLED";
	private static final String KEY_SUFFIX_ORG_IDS = "_SYNC_ORG_IDS";

	public boolean isSyncEnabled(String systemType) {
		String key = systemType + KEY_SUFFIX_ENABLED;
		String value = MSysConfig.getValue(key, "N", Env.getAD_Client_ID(Env.getCtx()));
		return "Y".equalsIgnoreCase(value);
	}

	public boolean isOrgAllowed(String systemType, int adOrgId) {
		String key = systemType + KEY_SUFFIX_ORG_IDS;
		String value = MSysConfig.getValue(key, ORG_ALL, Env.getAD_Client_ID(Env.getCtx()));

		if (value == null || value.trim().isEmpty() || ORG_ALL.equals(value.trim())) {
			return true;
		}
		Set<String> allowedOrgIds = Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());
		return allowedOrgIds.contains(String.valueOf(adOrgId));
	}
}