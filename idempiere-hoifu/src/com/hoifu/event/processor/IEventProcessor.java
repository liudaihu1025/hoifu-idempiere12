package com.hoifu.event.processor;

import org.compiere.model.PO;

public interface IEventProcessor {
	
	/**
	 * 判断是否处理该事件
	 * 
	 * @Title: supports
	 * @param po
	 * @param topic
	 * @return boolean
	 */
	boolean supports(PO po, String topic);

	/**
	 * 执行处理逻辑
	 * 
	 * @Title: process
	 * @param po
	 * @param topic
	 * @return void
	 */
	void process(PO po, String topic);
}