package com.hoifu.utils;

/**
 * 通用退避重试策略。 
 * 异步任务内的"快速重试"用此算法， 采用指数退避 + 封顶，避免因WMS等外部系统长时间不可用导致重试风暴。
 * @ClassName: RetryBackoffPolicy
 * @author ldh
 * @date 2026年8月19日
 */
public final class RetryBackoffPolicy {

	private RetryBackoffPolicy() {
	}

	/** 
	 * 快速重试(同一异步任务内)的基础间隔，毫秒 
	 */
	private static final long QUICK_RETRY_BASE_MS = 500L;

	/**
	 * 快速重试(线程内 sleep)间隔：500ms * attempt，线性增长即可， 因为快速重试只发生在同一次同步的几次尝试内，量级很小。
	 * 
	 * @param attempt 当前是第几次重试（从1开始）
	 */
	public static long calcQuickRetryDelayMs(int attempt) {
		return QUICK_RETRY_BASE_MS * attempt;
	}

}