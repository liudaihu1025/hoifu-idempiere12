package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

public class MQC_DefectRecord extends X_QC_DefectRecord {
	private static final long serialVersionUID = 1L;

	public MQC_DefectRecord(Properties ctx, int QC_DefectRecord_ID, String trxName) {
		super(ctx, QC_DefectRecord_ID, trxName);
	}

	public MQC_DefectRecord(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 可添加缺陷名规范性校验
		return true;
	}
}