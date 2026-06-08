package com.hoifu.event.processor;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.eevolution.model.I_PP_Order;

import com.hoifu.service.qc.IIPQCService;

public class IPQCEventProcessor implements IEventProcessor {

	private final IIPQCService ipqcService;

	public IPQCEventProcessor(IIPQCService ipqcService) {
		this.ipqcService = ipqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof I_PP_Order 
				&& (IEventTopics.DOC_AFTER_PREPARE.equals(topic) || IEventTopics.DOC_BEFORE_COMPLETE.equals(topic));
	}

	@Override
	public void process(PO po, String topic) {
		// 生产工单准备后创建过程检验单
		if (IEventTopics.DOC_AFTER_PREPARE.equals(topic)) {
			if (MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false)) {
				ipqcService.createFromPPOrder(po);
			}
		}
		
		// 生产工单完成前进行检验
		if (IEventTopics.DOC_BEFORE_COMPLETE.equals(topic)) {
			if (MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false)) {
				if (!ipqcService.existsForPPOrder(po)) {
					ipqcService.createFromPPOrder(po);
					throw new RuntimeException("已为工单 [" + po.get_ValueAsString("DocumentNo") + "] 创建过程检验单（IPQC），请完成检验后再完成工单。");
				}
				if (!ipqcService.isCompletedAndPassed(po)) {
					throw new RuntimeException("工单 [" + po.get_ValueAsString("DocumentNo") + "] 的过程检验单（IPQC）尚未通过检验，请先通过检验。");
				}
			}
		}
		
	}
}