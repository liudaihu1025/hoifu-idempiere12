package com.hoifu.service;

import org.compiere.model.PO;

/**
 * 凭证服务接口
 * 
 * @ClassName: IGlVoucherService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IGlVoucherService {

	/**
	 * 过账后创建凭证
	 * 
	 * @Title: createGlVoucher
	 * @param po
	 * @return void
	 */
	void createGlVoucher(PO po);

	/**
	 * 重新激活前 / 取消支付分配时删除凭证
	 * 
	 * @Title: deleteGlVoucher
	 * @param po
	 * @return void
	 */
	void deleteGlVoucher(PO po);

	/**
	 * 借贷/红字更正后刷新凭证描述和状态
	 * 
	 * @Title: refreshVoucherAfterReversal
	 * @param po
	 * @return void
	 */
	void refreshVoucherAfterReversal(PO po);
}