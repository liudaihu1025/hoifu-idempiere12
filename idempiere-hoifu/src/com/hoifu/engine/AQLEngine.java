package com.hoifu.engine;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.util.CLogger;

import com.hoifu.model.qc.MQC_AQL_AcceptPlan;
import com.hoifu.model.qc.MQC_AQL_SampleCode;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateProduct;

public class AQLEngine {

	private static final CLogger log = CLogger.getCLogger(AQLEngine.class);

	// ---- 内部 DTO ----

	/** 单个缺陷类型的 AQL 计划（字码 + 样本量 + 接收/拒收数） */
	public static class AQLPlan {
		private final String sampleCode;
		private final int sampleSize;
		private final int acceptQty;
		private final int rejectQty;

		public AQLPlan(String sampleCode, int sampleSize, int acceptQty, int rejectQty) {
			this.sampleCode = sampleCode;
			this.sampleSize = sampleSize;
			this.acceptQty = acceptQty;
			this.rejectQty = rejectQty;
		}

		public String getSampleCode() {
			return sampleCode;
		}

		public int getSampleSize() {
			return sampleSize;
		}

		public int getAcceptQty() {
			return acceptQty;
		}

		public int getRejectQty() {
			return rejectQty;
		}
	}

	/** 完整的 AQL 计算结果 */
	public static class AQLResult {
		private final int sampleSize; // 三者中最大的样本量
		private final String sampleCode; // 对应最大样本量的字码
		private final AQLPlan crPlan;
		private final AQLPlan majPlan;
		private final AQLPlan minPlan;

		public AQLResult(int sampleSize, String sampleCode, AQLPlan crPlan, AQLPlan majPlan, AQLPlan minPlan) {
			this.sampleSize = sampleSize;
			this.sampleCode = sampleCode;
			this.crPlan = crPlan;
			this.majPlan = majPlan;
			this.minPlan = minPlan;
		}

		public int getSampleSize() {
			return sampleSize;
		}

		public String getSampleCode() {
			return sampleCode;
		}

		public AQLPlan getCrPlan() {
			return crPlan;
		}

		public AQLPlan getMajPlan() {
			return majPlan;
		}

		public AQLPlan getMinPlan() {
			return minPlan;
		}
	}

	// ---- 核心计算入口 ----

	/**
	 * 根据批量、模板（方案）、模板产品（可为 null）计算 AQL 抽样计划。
	 * 
	 * @param ctx      上下文
	 * @param lotSize  批量
	 * @param template 检验方案（含 AQL 标准、检验水平、方案级 AQL 值）
	 * @param tp       模板产品（可为 null，为 null 时使用方案级 AQL 值）
	 * @param trxName  事务名
	 * @return AQLResult
	 */
	public AQLResult calculate(Properties ctx, int lotSize, MQC_Template template, MQC_TemplateProduct tp,
			String trxName) {
		if (template == null)
			throw new AdempiereException("检验方案不能为空");

		int standardId = template.getQC_AQL_Standard_ID();
		if (standardId <= 0)
			throw new AdempiereException("检验方案未配置 AQL 标准");

		String inspectionLevel = template.getInspectionLevel();
		if (inspectionLevel == null || inspectionLevel.isEmpty())
			throw new AdempiereException("检验方案未配置检验水平");

		// 1. 产品级别 AQL 值优先，否则使用方案级别
		BigDecimal aqlCR = resolveAQL(tp != null ? tp.getAQL_CR() : null, template.getAQL_CR());
		BigDecimal aqlMAJ = resolveAQL(tp != null ? tp.getAQL_MAJ() : null, template.getAQL_MAJ());
		BigDecimal aqlMIN = resolveAQL(tp != null ? tp.getAQL_MIN() : null, template.getAQL_MIN());

		// 2. 查字码表，获取字码和样本量
		MQC_AQL_SampleCode scCR = querySampleCode(ctx, standardId, inspectionLevel, lotSize, trxName);
		MQC_AQL_SampleCode scMAJ = scCR; // 同一批量/检验水平，字码相同
		MQC_AQL_SampleCode scMIN = scCR;

		if (scCR == null)
			throw new AdempiereException("未找到 AQL 字码：标准=" + standardId + " 检验水平=" + inspectionLevel + " 批量=" + lotSize);

		// 3. 查接收数计划
		AQLPlan crPlan = queryAcceptPlan(ctx, standardId, scCR.getSampleCode(), scCR.getSampleSize(), aqlCR, "CR",
				trxName);
		AQLPlan majPlan = queryAcceptPlan(ctx, standardId, scMAJ.getSampleCode(), scMAJ.getSampleSize(), aqlMAJ, "MAJ",
				trxName);
		AQLPlan minPlan = queryAcceptPlan(ctx, standardId, scMIN.getSampleCode(), scMIN.getSampleSize(), aqlMIN, "MIN",
				trxName);

		if (crPlan == null || majPlan == null || minPlan == null)
			throw new AdempiereException("未找到 AQL 接收数计划：字码=" + scCR.getSampleCode());

		// 4. 取三者中最大的样本量（最严格）
		int maxSampleSize = Math.max(crPlan.getSampleSize(),
				Math.max(majPlan.getSampleSize(), minPlan.getSampleSize()));

		// 确定最大样本量对应的字码（用于展示）
		String maxSampleCode = scCR.getSampleCode();

		return new AQLResult(maxSampleSize, maxSampleCode, crPlan, majPlan, minPlan);
	}

	// ---- 私有辅助方法 ----

	/** 产品级别优先，为 null 则使用方案级别 */
	private BigDecimal resolveAQL(BigDecimal productLevel, BigDecimal templateLevel) {
		if (productLevel != null && productLevel.compareTo(BigDecimal.ZERO) >= 0)
			return productLevel;
		if (templateLevel != null && templateLevel.compareTo(BigDecimal.ZERO) >= 0)
			return templateLevel;
		throw new AdempiereException("AQL 值未配置");
	}

	/**
	 * 查字码表：根据标准、检验水平、批量，返回字码和样本量。
	 */
	private MQC_AQL_SampleCode querySampleCode(Properties ctx, int standardId, String inspectionLevel, int lotSize,
			String trxName) {
		String where = "QC_AQL_Standard_ID=?" + " AND InspectionLevel=?" + " AND LotSizeMin<=?"
				+ " AND (LotSizeMax IS NULL OR LotSizeMax>=?) AND IsActive ='Y' AND AD_Client_ID = ?";

		int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);

		return new Query(ctx, MQC_AQL_SampleCode.Table_Name, where, trxName)
				.setParameters(standardId, inspectionLevel, lotSize, lotSize, clientId).setOrderBy("Updated DESC")
				.first();
	}

	/**
	 * 查接收数计划：根据标准、字码、AQL值、缺陷类型，返回接收数和拒收数。 0）优先于系统预置（AD_Client_ID = 0）。
	 */
	private AQLPlan queryAcceptPlan(Properties ctx, int standardId, String sampleCode, int sampleSize,
			BigDecimal aqlValue, String defectType, String trxName) {
		String where = "QC_AQL_Standard_ID=?" + " AND SampleCode=?" + " AND AQLValue=?" + " AND DefectType=?"
				+ " AND IsActive = 'Y' AND AD_Client_ID = ?";

		int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);

		MQC_AQL_AcceptPlan plan = new Query(ctx, MQC_AQL_AcceptPlan.Table_Name, where, trxName)
				.setParameters(standardId, sampleCode, aqlValue, defectType, clientId).setOrderBy("Updated DESC")
				.first();

		if (plan == null)
			return null;

		return new AQLPlan(sampleCode, sampleSize, plan.getAcceptQty(), plan.getRejectQty());
	}
}