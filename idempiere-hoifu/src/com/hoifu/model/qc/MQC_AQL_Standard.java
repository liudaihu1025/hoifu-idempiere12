package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

public class MQC_AQL_Standard extends X_QC_AQL_Standard {
	private static final long serialVersionUID = 1L;

	public MQC_AQL_Standard(Properties ctx, int QC_AQL_Standard_ID, String trxName) {
		super(ctx, QC_AQL_Standard_ID, trxName);
	}

	public MQC_AQL_Standard(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MQC_AQL_Standard(Properties ctx, String QC_AQL_Standard_UU, String trxName) {
		super(ctx, QC_AQL_Standard_UU, trxName);
	}

	/**
	 * 根据 StandardCode 查询标准（优先返回公司定制，其次系统预置）
	 */
	public static MQC_AQL_Standard getByCode(Properties ctx, String standardCode, int AD_Client_ID, String trxName) {
		// 优先返回公司定制（AD_Client_ID 匹配），其次系统预置（AD_Client_ID=0）
		return new Query(ctx, Table_Name, "StandardCode=? AND AD_Client_ID IN (0,?) AND IsActive='Y'", trxName)
				.setParameters(standardCode, AD_Client_ID).setOrderBy("AD_Client_ID DESC") // 公司定制优先
				.first();
	}

	/**
	 * 获取所有可用标准（系统预置 + 当前公司定制）
	 */
	public static List<MQC_AQL_Standard> getAll(Properties ctx, int AD_Client_ID, String trxName) {
		return new Query(ctx, Table_Name, "AD_Client_ID IN (0,?) AND IsActive='Y'", trxName).setParameters(AD_Client_ID)
				.setOrderBy("AD_Client_ID, Name").list();
	}

	/**
	 * 获取该标准下所有字码记录
	 */
	public List<MQC_AQL_SampleCode> getSampleCodes(String trxName) {
		return new Query(getCtx(), MQC_AQL_SampleCode.Table_Name, "QC_AQL_Standard_ID=? AND IsActive='Y'", trxName)
				.setParameters(get_ID()).setOrderBy("InspectionLevel, LotSizeMin").list();
	}
}