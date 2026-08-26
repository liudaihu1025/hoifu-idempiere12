package com.hoifu.service.extsync;

import org.compiere.model.PO;
import org.json.JSONObject;

/**
 * 外部系统适配器接口。 每接入一个新的外部系统（WMS/MES/SRM...），只需新增一个实现类， 不需要改动触发层/日志层/开关服务任何代码，符合开闭原则。
 * @ClassName: IExternalSystemAdapter
 * @author ldh
 * @date 2026年8月19日
 */
public interface IExternalSystemAdapter {

	/**
	 * 系统标识，与 AD_SysConfig Key 前缀、Ext_System_Sync_Log.System_Type 保持一致，如 "WMS"
	 */
	String getSystemType();

	/**
	 * 判断该适配器是否处理某个业务类型的同步（如 PRODUCT/BPARTNER/INOUT...）。 每个适配器只关心自己系统需要同步的业务对象子集。
	 */
	boolean supports(String businessType);

	/**
	 * 构建请求报文。字段固定，直接在实现类里用代码 DTO 拼装， 不走 OA 那套配置表驱动的 FieldMapping 机制。
	 */
	String buildRequest(PO po, String businessType, String eventType);

	/**
	 * 获取该业务类型对应的固定 API 地址（可来自 MSysConfig 前缀 + 固定后缀常量）
	 */
	String resolveApiUrl(String businessType, String eventType);

	/**
	 * 发起实际调用并解析该系统自己的成功/失败判定标准，统一转换为 ExtSyncResult。
	 * 具体状态码含义、字段名（如WMS用"code":"0"/"1"，MES可能用"status":"200"/"500"或"success":true/false）
	 * 完全由各Adapter实现自行解析，上层不感知任何具体系统的返回码约定。
	 * 
	 * 网络层错误（超时/连接失败/HTTP>=400）仍然通过抛异常表达，由上层统一走"异常重试"分支；
	 * 只有"HTTP请求本身成功、但业务处理失败"这种场景才通过 ExtSyncResult.failure() 表达。
	 */
	ExtSyncResult send(String apiUrl, String request) throws Exception;
}