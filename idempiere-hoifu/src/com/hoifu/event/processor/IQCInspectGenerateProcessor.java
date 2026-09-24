package com.hoifu.event.processor;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.PO;
import org.compiere.process.DocAction;

import com.hoifu.service.qc.IIQCInspectService;

/**
 * 收货单完成后，为需要来料检验的明细行自动生成检验单（QC_IQCInspect）
 *
 * 仅处理采购收货方向（非销售出库），且仅当 DocStatus 刚变为 'CO' 时触发。
 * 与旧检验体系（InOutEventProcessor 中的 IQC/OQC 校验逻辑）完全隔离。
 */
public class IQCInspectGenerateProcessor implements IEventProcessor {

	private final IIQCInspectService iqcInspectService;

	public IQCInspectGenerateProcessor(IIQCInspectService iqcInspectService) {
		this.iqcInspectService = iqcInspectService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOut;
	}

	@Override
	public void process(PO po, String topic) {
		MInOut inout = (MInOut) po;

		// 仅处理 PO_AFTER_CHANGE 事件
		if (!IEventTopics.PO_AFTER_CHANGE.equals(topic))
			return;

		// 仅当 DocStatus 刚变为 Completed 时触发
		if (!inout.is_ValueChanged(MInOut.COLUMNNAME_DocStatus))
			return;
		if (!DocAction.STATUS_Completed.equals(inout.getDocStatus()))
			return;

		// 仅处理采购收货（非销售出库）
		if (inout.isSOTrx())
			return;

		// 遍历明细行，为需要检验的物料生成检验单
		for (MInOutLine line : inout.getLines()) {
			iqcInspectService.createFromCompletedReceiptLine(line);
		}
	}
}
