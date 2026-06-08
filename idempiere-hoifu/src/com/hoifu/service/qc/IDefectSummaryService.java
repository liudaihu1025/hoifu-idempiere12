package com.hoifu.service.qc;

import org.compiere.model.PO;

/**
 * 缺陷记录汇总服务接口
 * 
 * @ClassName: IDefectSummaryService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IDefectSummaryService {

	/**
	 * 缺陷记录新增/变更后，汇总到对应检验单头
	 * 
	 * @Title: aggregate
	 * @param defectRecord
	 * @return void
	 */
	void aggregate(PO defectRecord);
}