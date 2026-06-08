package com.hoifu.service.qc;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;

/**
 * 退货检验单服务接口
 * 
 * @ClassName: IRQCService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IRQCService {

	/**
	 * 退货行新增后，为单行创建退货检验单（RQC）
	 * 
	 * @Title: createFromReturnInOut
	 * @param io
	 * @return void
	 */
	void createFromLine(MInOut io, MInOutLine line);

	/**
	 * 判断是否是退货单（RMA/RTV），供事件监听器路由判断
	 * 
	 * @Title: isReturnDocument
	 * @param io
	 * @return
	 * @return boolean
	 */
	boolean isReturnDocument(MInOut io);

	/**
	 * 退货单完成前，校验所有行的 RQC 是否已完成（新增）
	 * 
	 * @Title: validateBeforeReturn
	 * @param io
	 * @return void
	 */
	void validateBeforeReturn(MInOut io);
}