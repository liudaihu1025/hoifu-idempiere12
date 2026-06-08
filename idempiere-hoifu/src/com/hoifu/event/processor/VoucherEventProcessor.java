package com.hoifu.event.processor;

import java.util.Set;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.PO;

import com.hoifu.service.IGlVoucherService;

public class VoucherEventProcessor implements IEventProcessor {

	private static final Set<String> SUPPORTED_TOPICS = Set.of(
			IEventTopics.DOC_AFTER_POST,
			IEventTopics.DOC_BEFORE_REACTIVATE, 
			IEventTopics.DOC_AFTER_REVERSECORRECT,
			IEventTopics.DOC_AFTER_REVERSEACCRUAL, 
			IEventTopics.PO_BEFORE_DELETE);

	private final IGlVoucherService voucherService;

	public VoucherEventProcessor(IGlVoucherService voucherService) {
		this.voucherService = voucherService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		if (!SUPPORTED_TOPICS.contains(topic))
			return false;
		// PO_BEFORE_DELETE 只处理 MAllocationHdr
		if (IEventTopics.PO_BEFORE_DELETE.equals(topic))
			return po instanceof MAllocationHdr;
		return true;
	}

	@Override
	public void process(PO po, String topic) {
		switch (topic) {
			case IEventTopics.DOC_BEFORE_REACTIVATE -> voucherService.deleteGlVoucher(po);
			case IEventTopics.DOC_AFTER_POST -> voucherService.createGlVoucher(po);
			case IEventTopics.DOC_AFTER_REVERSEACCRUAL -> voucherService.refreshVoucherAfterReversal(po);
			case IEventTopics.PO_BEFORE_DELETE -> voucherService.deleteGlVoucher(po);
			case IEventTopics.DOC_AFTER_REVERSECORRECT -> {
				if (po instanceof MAllocationHdr)
					voucherService.deleteGlVoucher(po);
				else
					voucherService.refreshVoucherAfterReversal(po);
			}
		}
	}
}