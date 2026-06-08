package com.hoifu.service.qc;

import org.compiere.model.PO;

import com.hoifu.model.qc.MQC_Template;

/**
 * 通用检验单生成服务接口
 * 
 * @ClassName: IQCLineGeneratorService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IQCLineGeneratorService {

	/**
	 * 根据检验模板为检验单头生成明细行
	 * 
	 * @Title: generateLines
	 * @param header
	 * @param template
	 * @param trxName
	 * @return void
	 */
	void generateLines(PO header, MQC_Template template, String trxName);
}