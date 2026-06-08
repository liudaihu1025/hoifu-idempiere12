package com.hoifu.service.qc;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;

/**
 * 来料检验服务接口
 * 
 * @ClassName: IIQCService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IIQCService {

	/**
	 * 采购收货行新增后，创建来料检验单（IQC）
	 * 
	 * @Title: createFromReceiptLine
	 * @param line
	 * @return void
	 */
	void createFromReceiptLine(MInOutLine line);

	/**
	 * 收货单完成前，校验所有行是否通过 IQC（新增）
	 * 
	 * @Title: validateBeforeReceipt
	 * @param receipt
	 * @return void
	 */
	void validateBeforeReceipt(MInOut receipt);
}