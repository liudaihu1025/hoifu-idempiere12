package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;

public class MQC_AQL_AcceptPlan extends X_QC_AQL_AcceptPlan {
	private static final long serialVersionUID = 1L;

	/** 缺陷类型常量 */
	public static final String DEFECTTYPE_CR = "CR";
	public static final String DEFECTTYPE_MAJ = "MAJ";
	public static final String DEFECTTYPE_MIN = "MIN";

	public MQC_AQL_AcceptPlan(Properties ctx, int QC_AQL_AcceptPlan_ID, String trxName) {
		super(ctx, QC_AQL_AcceptPlan_ID, trxName);
	}

	public MQC_AQL_AcceptPlan(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MQC_AQL_AcceptPlan(Properties ctx, String QC_AQL_AcceptPlan_UU, String trxName) {
		super(ctx, QC_AQL_AcceptPlan_UU, trxName);
	}

	/**
	 * 查找接收数计划（公司定制优先于系统预置）
	 * 
	 * @param standardId   QC_AQL_Standard_ID
	 * @param sampleCode   字码（A-R）
	 * @param aqlValue     AQL 值（如 0.065、1.0、4.0）
	 * @param defectType   缺陷类型（CR/MAJ/MIN）
	 * @param AD_Client_ID 当前公司 ID
	 */
	public static MQC_AQL_AcceptPlan lookup(Properties ctx, int standardId, String sampleCode, BigDecimal aqlValue,
			String defectType, int AD_Client_ID, String trxName) {
		return new Query(ctx, Table_Name,
				"QC_AQL_Standard_ID=? AND SampleCode=? AND AQLValue=? AND DefectType=? "
						+ "AND AD_Client_ID IN (0,?) AND IsActive='Y'",
				trxName).setParameters(standardId, sampleCode, aqlValue, defectType, AD_Client_ID)
				.setOrderBy("AD_Client_ID DESC") // 公司定制（Client_ID>0）优先于系统预置（Client_ID=0）
				.first();
	}

	/**
	 * 是否通过（实际缺陷数 <= 接收数 Ac）
	 */
	public boolean isAccepted(int actualDefectQty) {
		return actualDefectQty <= getAcceptQty();
	}

	/**
	 * 是否拒收（实际缺陷数 >= 拒收数 Re）
	 */
	public boolean isRejected(int actualDefectQty) {
		return actualDefectQty >= getRejectQty();
	}
}