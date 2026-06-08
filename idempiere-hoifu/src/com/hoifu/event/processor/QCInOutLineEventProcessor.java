package com.hoifu.event.processor;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;

import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;

public class QCInOutLineEventProcessor implements IEventProcessor {

	private final IIQCService iqcService;
	private final IOQCService oqcService;
	private final IRQCService rqcService;

	public QCInOutLineEventProcessor(IIQCService iqcService, IOQCService oqcService, IRQCService rqcService) {
		this.iqcService = iqcService;
		this.oqcService = oqcService;
		this.rqcService = rqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOutLine && IEventTopics.PO_AFTER_NEW.equals(topic);
	}

	@Override
	public void process(PO po, String topic) {
		// 收发单行新增行后自动创建对应的检验单
		if (IEventTopics.PO_AFTER_NEW.equals(topic)) {
			if (MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false)) {
				MInOutLine line = (MInOutLine) po;
				MInOut parent = (MInOut) line.getParent();

				if (rqcService.isReturnDocument(parent))
					rqcService.createFromLine(parent, line);
				else if (!parent.isSOTrx())
					iqcService.createFromReceiptLine(line);
				else
					oqcService.createFromShipmentLine(line);
			}
		}
	}
}