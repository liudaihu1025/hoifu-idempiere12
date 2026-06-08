package com.hoifu.model.qc;

import java.math.BigDecimal;

public interface IQCLineQualifiable {
	BigDecimal getMeasuredValMin();

	BigDecimal getMeasuredValMax();

	BigDecimal getMeasuredValAvg();

	BigDecimal getStanderVal();

	BigDecimal getThresholdMax();

	BigDecimal getThresholdMin();

	void setMeasuredValAvg(BigDecimal avg);

	void setIsQualified(boolean qualified);

	void saveEx();
}