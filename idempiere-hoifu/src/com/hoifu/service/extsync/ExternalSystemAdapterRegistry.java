package com.hoifu.service.extsync;

import java.util.List;
import java.util.Optional;

import com.hoifu.service.extsync.wms.WmsSystemAdapter;

/**
 * 外部系统适配器注册表。
 * 新增系统（如未来接入MES/SRM）时只需在下方List追加一个实例， 触发层/日志层/开关服务/重试进程均无需改动 ——开闭原则的核心落地点。
 * @ClassName: ExternalSystemAdapterRegistry
 * @author ldh
 * @date 2026年8月19日
 */
public class ExternalSystemAdapterRegistry {

	private final List<IExternalSystemAdapter> adapters = List.of(new WmsSystemAdapter()
	// 未来扩展示例: , new com.hoifu.service.extsync.mes.MesSystemAdapter()
	);

	public Optional<IExternalSystemAdapter> findAdapter(String systemType, String businessType) {
		return adapters.stream()
				.filter(a -> a.getSystemType().equals(systemType) && a.supports(businessType))
				.findFirst();
	}

	/** 
	 * 返回当前系统中登记的全部 systemType（供兜底重试进程遍历所有系统，去重后逐个处理） 
	 */
	public java.util.Set<String> getAllSystemTypes() {
		return adapters.stream()
				.map(IExternalSystemAdapter::getSystemType)
				.collect(java.util.stream.Collectors.toSet());
	}

	public List<IExternalSystemAdapter> getAllAdapters() {
		return adapters;
	}
}