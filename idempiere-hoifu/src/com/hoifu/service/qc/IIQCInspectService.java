package com.hoifu.service.qc;

import org.compiere.model.MInOutLine;

/**
 * 来料检验单生成服务（新检验体系，与旧 IIQCService 完全隔离）
 */
public interface IIQCInspectService {

	/**
	 * 收货单完成后，为需要检验（M_Product.InspectionType='IQC'）的明细行生成来料检验单
	 *
	 * @param line 收货单明细行
	 */
	void createFromCompletedReceiptLine(MInOutLine line);
}
