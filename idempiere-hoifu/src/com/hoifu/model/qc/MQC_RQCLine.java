package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

public class MQC_RQCLine extends X_QC_RQCLine implements IQCLineQualifiable {
	private static final long serialVersionUID = 1L;

	public MQC_RQCLine(Properties ctx, int QC_RQCLine_ID, String trxName) {
		super(ctx, QC_RQCLine_ID, trxName);
	}

	public MQC_RQCLine(Properties ctx, ResultSet rs, String trxName) {
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