package com.hoifu.service.qc.impl;

import org.compiere.model.PO;
import org.compiere.util.CLogger;

import com.hoifu.model.qc.MQC_IPQCLine;
import com.hoifu.model.qc.MQC_IQCLine;
import com.hoifu.model.qc.MQC_OQCLine;
import com.hoifu.model.qc.MQC_RQCLine;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateIndex;
import com.hoifu.service.qc.IQCLineGeneratorService;

public class QCLineGeneratorServiceImpl implements IQCLineGeneratorService {

	private static final CLogger log = CLogger.getCLogger(QCLineGeneratorServiceImpl.class);

	@Override
	public void generateLines(PO header, MQC_Template template, String trxName) {
		String headerTable = header.get_TableName();
		String fkCol;
		switch (headerTable) {
		case "QC_IQC":
			fkCol = "QC_IQC_ID";
			break;
		case "QC_IPQC":
			fkCol = "QC_IPQC_ID";
			break;
		case "QC_OQC":
			fkCol = "QC_OQC_ID";
			break;
		case "QC_RQC":
			fkCol = "QC_RQC_ID";
			break;
		default:
			return;
		}
		for (MQC_TemplateIndex ti : template.getIndexLines()) {
			try {
				PO line = newLineInstance(header, headerTable + "Line", trxName);
				line.set_ValueOfColumn(fkCol, header.get_ID());
				line.set_ValueOfColumn("QC_Index_ID", ti.getQC_Index_ID());
				line.set_ValueOfColumn("CheckMethod", ti.getCheckMethod());
				line.set_ValueOfColumn("StanderVal", ti.getStanderVal());
				line.set_ValueOfColumn("UnitOfMeasure", ti.getUnitOfMeasure());
				line.set_ValueOfColumn("ThresholdMin", ti.getThresholdMin());
				line.set_ValueOfColumn("ThresholdMax", ti.getThresholdMax());
				line.saveEx();
			} catch (Exception e) {
				log.severe("生成明细行失败: " + e.getMessage());
			}
		}
	}

	private PO newLineInstance(PO header, String lineTable, String trxName) {
		switch (lineTable) {
		case "QC_IQCLine":
			return new MQC_IQCLine(header.getCtx(), 0, trxName);
		case "QC_IPQCLine":
			return new MQC_IPQCLine(header.getCtx(), 0, trxName);
		case "QC_OQCLine":
			return new MQC_OQCLine(header.getCtx(), 0, trxName);
		case "QC_RQCLine":
			return new MQC_RQCLine(header.getCtx(), 0, trxName);
		default:
			throw new IllegalArgumentException("未知表: " + lineTable);
		}
	}
}