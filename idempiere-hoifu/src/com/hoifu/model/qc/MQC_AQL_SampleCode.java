package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;

public class MQC_AQL_SampleCode extends X_QC_AQL_SampleCode {
	private static final long serialVersionUID = 1L;

	/** 检验水平常量 */
	public static final String INSPECTIONLEVEL_I = "I";
	public static final String INSPECTIONLEVEL_II = "II";
	public static final String INSPECTIONLEVEL_III = "III";
	public static final String INSPECTIONLEVEL_S1 = "S1";
	public static final String INSPECTIONLEVEL_S2 = "S2";
	public static final String INSPECTIONLEVEL_S3 = "S3";
	public static final String INSPECTIONLEVEL_S4 = "S4";

	public MQC_AQL_SampleCode(Properties ctx, int QC_AQL_SampleCode_ID, String trxName) {
		super(ctx, QC_AQL_SampleCode_ID, trxName);
	}

	public MQC_AQL_SampleCode(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MQC_AQL_SampleCode(Properties ctx, String QC_AQL_SampleCode_UU, String trxName) {
		super(ctx, QC_AQL_SampleCode_UU, trxName);
	}

	/**
	 * 根据标准ID、检验水平、批量查找对应字码记录
	 * 
	 * @param standardId      QC_AQL_Standard_ID
	 * @param inspectionLevel 检验水平（I/II/III/S1/S2/S3/S4）
	 * @param lotSize         批量
	 */
	public static MQC_AQL_SampleCode lookup(Properties ctx, int standardId, String inspectionLevel, int lotSize,
			String trxName) {
		return new Query(ctx, Table_Name,
				"QC_AQL_Standard_ID=? AND InspectionLevel=? AND LotSizeMin<=? AND LotSizeMax>=? AND IsActive='Y'",
				trxName).setParameters(standardId, inspectionLevel, lotSize, lotSize).first();
	}
}