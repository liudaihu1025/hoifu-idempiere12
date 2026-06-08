package com.hoifu.event.processor;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MInOut;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;

import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;

public class QCInOutEventProcessor implements IEventProcessor {

	private final IIQCService iqcService;
	private final IOQCService oqcService;
	private final IRQCService rqcService;

	public QCInOutEventProcessor(IIQCService iqcService, IOQCService oqcService, IRQCService rqcService) {
		this.iqcService = iqcService;
		this.oqcService = oqcService;
		this.rqcService = rqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOut && IEventTopics.DOC_BEFORE_COMPLETE.equals(topic);
	}

	@Override
	public void process(PO po, String topic) {
		// 收发单完成前进行检验
		if (IEventTopics.DOC_BEFORE_COMPLETE.equals(topic)) {
			if (MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false)) {
				MInOut inout = (MInOut) po;
				if (rqcService.isReturnDocument(inout))
					rqcService.validateBeforeReturn(inout);
				else if (inout.isSOTrx())
					oqcService.validateBeforeShipment(inout);
				else
					iqcService.validateBeforeReceipt(inout);
			}
		}
	}

}