package com.hoifu.service.qc;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;

/**
 * 出货检验服务接口
 * 
 * @ClassName: IOQCService
 * @author ldh
 * @date 2026年5月11日
 */
public interface IOQCService {

	/**
	 * 销售发货行新增前，创建出货检验单（OQC）
	 * 
	 * @Title: createFromShipmentLine
	 * @param line
	 * @return void
	 */
	void createFromShipmentLine(MInOutLine line);

	/**
	 * 发货单完成前，校验所有行是否通过 OQC
	 * 
	 * @Title: validateBeforeShipment
	 * @param shipment
	 * @return void
	 */
	void validateBeforeShipment(MInOut shipment);
}