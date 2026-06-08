package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

public class MQC_TemplateIndex extends X_QC_TemplateIndex {
	private static final long serialVersionUID = 1L;

	public MQC_TemplateIndex(Properties ctx, int QC_TemplateIndex_ID, String trxName) {
		super(ctx, QC_TemplateIndex_ID, trxName);
	}

	public MQC_TemplateIndex(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 可添加校验，如标准值必须为正数等
		return true;
	}
}