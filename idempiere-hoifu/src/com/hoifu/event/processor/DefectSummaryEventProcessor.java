package com.hoifu.event.processor;

import java.util.Set;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.PO;

import com.hoifu.model.qc.X_QC_DefectRecord;
import com.hoifu.service.qc.IDefectSummaryService;

public class DefectSummaryEventProcessor implements IEventProcessor {

	private static final Set<String> SUPPORTED_TOPICS = Set.of(
			IEventTopics.PO_AFTER_NEW, 
			IEventTopics.PO_AFTER_CHANGE);

	private final IDefectSummaryService defectService;

	public DefectSummaryEventProcessor(IDefectSummaryService defectService) {
		this.defectService = defectService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return X_QC_DefectRecord.Table_Name.equals(po.get_TableName()) && SUPPORTED_TOPICS.contains(topic);
	}

	@Override
	public void process(PO po, String topic) {
		defectService.aggregate(po);
	}
}