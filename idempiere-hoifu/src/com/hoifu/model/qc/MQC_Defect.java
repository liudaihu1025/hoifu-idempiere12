package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

public class MQC_Defect extends X_QC_Defect {
	private static final long serialVersionUID = 1L;

	public MQC_Defect(Properties ctx, int QC_Defect_ID, String trxName) {
		super(ctx, QC_Defect_ID, trxName);
	}

	public MQC_Defect(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 可校验缺陷等级的有效性
		return true;
	}
}