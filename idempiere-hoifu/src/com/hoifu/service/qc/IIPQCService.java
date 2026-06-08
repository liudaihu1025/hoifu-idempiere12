package com.hoifu.service.qc;

import org.compiere.model.PO;

/**
 * 过程检验服务接口
 * 
 * @ClassName: IIPQCService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IIPQCService {

	/**
	 * 工单完成前，创建制程检验单（IPQC）
	 * 
	 * @Title: createFromPPOrder
	 * @param ppOrder
	 * @return void
	 */
	boolean createFromPPOrder(PO ppOrder);

	/**
	 * 判断工单是否已存在 IPQC
	 * 
	 * @Title: existsForPPOrder
	 * @param ppOrder
	 * @return
	 * @return boolean
	 */
	boolean existsForPPOrder(PO ppOrder);

	/**
	 * 判断工单对应的 IPQC 是否已完成（DocStatus='CO'）
	 * 
	 * @Title: isCompleted
	 * @param ppOrder
	 * @return
	 * @return boolean
	 */
	boolean isCompletedAndPassed(PO ppOrder);
}