package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

public class MQC_OQCLine extends X_QC_OQCLine implements IQCLineQualifiable {
	private static final long serialVersionUID = 1L;

	public MQC_OQCLine(Properties ctx, int QC_OQCLine_ID, String trxName) {
		super(ctx, QC_OQCLine_ID, trxName);
	}

	public MQC_OQCLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getMeasuredValMin() != null && getMeasuredValMax() != null) {
			BigDecimal avg = getMeasuredValMin().add(getMeasuredValMax()).divide(BigDecimal.valueOf(2), 4,
					BigDecimal.ROUND_HALF_UP);
			setMeasuredValAvg(avg);
		}
		return true;
	}
}