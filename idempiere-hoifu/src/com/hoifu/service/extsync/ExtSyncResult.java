package com.hoifu.service.extsync;

/**
 * 外部系统调用的统一结果封装。 不同外部系统的成功/失败判定标准（状态码、字段名、取值含义）完全不同， 由各自的
 * IExternalSystemAdapter 在 send() 内部解析原始响应后转换成这个统一结构，
 * 上层(触发处理器/重试进程/日志)只认这一份结构，不需要感知任何具体系统的状态码含义。
 * @ClassName: ExtSyncResult
 * @author ldh
 * @date 2026年8月19日
 */
public class ExtSyncResult {

	/** 是否业务成功（不是HTTP状态码成功，是"这次同步请求WMS/MES真正处理成功"） */
	private final boolean success;

	/** 原始返回码（各系统含义不同，仅作记录/排查用，如WMS的"0"/"1"，MES可能是"200"/"500"） */
	private final String rawCode;

	/** 失败时的错误描述（用于Error_Message落库，成功时可为空） */
	private final String message;

	/** 完整原始响应体（用于Response_JSON落库，便于人工排查） */
	private final String rawResponse;

	private ExtSyncResult(boolean success, String rawCode, String message, String rawResponse) {
		this.success = success;
		this.rawCode = rawCode;
		this.message = message;
		this.rawResponse = rawResponse;
	}

	public static ExtSyncResult success(String rawCode, String rawResponse) {
		return new ExtSyncResult(true, rawCode, null, rawResponse);
	}

	public static ExtSyncResult failure(String rawCode, String message, String rawResponse) {
		return new ExtSyncResult(false, rawCode, message, rawResponse);
	}

	public boolean isSuccess() {
		return success;
	}

	public String getRawCode() {
		return rawCode;
	}

	public String getMessage() {
		return message;
	}

	public String getRawResponse() {
		return rawResponse;
	}
}