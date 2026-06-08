package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

public class MQC_IQCLine extends X_QC_IQCLine implements IQCLineQualifiable {
	private static final long serialVersionUID = 1L;

	public MQC_IQCLine(Properties ctx, int QC_IQCLine_ID, String trxName) {
		super(ctx, QC_IQCLine_ID, trxName);
	}

	public MQC_IQCLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 自动计算实测平均值
		if (getMeasuredValMin() != null && getMeasuredValMax() != null) {
			BigDecimal avg = getMeasuredValMin().add(getMeasuredValMax()).divide(BigDecimal.valueOf(2), 4,
					BigDecimal.ROUND_HALF_UP);
			setMeasuredValAvg(avg);
		}
		return true;
	}
}