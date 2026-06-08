package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

public class MQC_Index extends X_QC_Index {
	private static final long serialVersionUID = 1L;

	public MQC_Index(Properties ctx, int QC_Index_ID, String trxName) {
		super(ctx, QC_Index_ID, trxName);
	}

	public MQC_Index(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 可以添加校验逻辑，如需自动生成编码等
		return true;
	}
}