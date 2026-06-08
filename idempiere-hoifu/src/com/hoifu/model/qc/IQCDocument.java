package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.util.Properties;

public interface IQCDocument {
	Properties getCtx();

	String get_TrxName();

	int getQC_Template_ID();

	int getM_Product_ID();

	BigDecimal getQuantityCheck(); // 抽样数（已由 service 设置）

	BigDecimal getCR_Quantity();

	BigDecimal getMAJ_Quantity();

	BigDecimal getMIN_Quantity();

	void setQuantityCheck(BigDecimal v);

	void setCheckResult(String v);

	void setCR_Rate(BigDecimal v);

	void setMAJ_Rate(BigDecimal v);

	void setMIN_Rate(BigDecimal v);

	void setQuantityQualified(BigDecimal v);

	void setQuantityUnqualified(BigDecimal v);

	/** 钩子：PASS 时的额外处理，默认空实现，RQC 覆盖 */
	default void onPass() {
	}

	/** 批量（用于 AQL 计算），各类型返回不同字段 */
	BigDecimal getLotSize();
}