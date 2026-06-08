package com.hoifu.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.compiere.util.CLogger;

import com.hoifu.model.qc.IQCDocument;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateProduct;

public class QualityEngine {

	private static final CLogger log = CLogger.getCLogger(QualityEngine.class);
	private static final int SCALE = 4;
	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	private final AQLEngine aqlEngine = new AQLEngine();

	public void determineResult(IQCDocument doc) {
		if (doc == null)
			return;

		BigDecimal qtyCheck = nvl(doc.getQuantityCheck());
		BigDecimal lotSize = nvl(doc.getLotSize());
		if (lotSize.compareTo(BigDecimal.ZERO) <= 0) {
			log.warning("批量为0，跳过检验判定");
			return;
		}

		MQC_Template template = new MQC_Template(doc.getCtx(), doc.getQC_Template_ID(), doc.get_TrxName());
		MQC_TemplateProduct tp = MQC_TemplateProduct.get(doc.getCtx(), doc.getM_Product_ID(), doc.getQC_Template_ID(),
				doc.get_TrxName());

		AQLEngine.AQLResult aql = aqlEngine.calculate(doc.getCtx(), lotSize.intValue(), template, tp,
				doc.get_TrxName());

		doc.setQuantityCheck(BigDecimal.valueOf(aql.getSampleSize()));

		BigDecimal crQty = nvl(doc.getCR_Quantity());
		BigDecimal majQty = nvl(doc.getMAJ_Quantity());
		BigDecimal minQty = nvl(doc.getMIN_Quantity());

		boolean pass = crQty.intValue() <= aql.getCrPlan().getAcceptQty()
				&& majQty.intValue() <= aql.getMajPlan().getAcceptQty()
				&& minQty.intValue() <= aql.getMinPlan().getAcceptQty();

		doc.setCheckResult(pass ? "PASS" : "FAIL");
		doc.setCR_Rate(rateBD(crQty, qtyCheck));
		doc.setMAJ_Rate(rateBD(majQty, qtyCheck));
		doc.setMIN_Rate(rateBD(minQty, qtyCheck));

		BigDecimal unqualified = crQty.add(majQty).add(minQty);
		doc.setQuantityQualified(qtyCheck.subtract(unqualified).max(BigDecimal.ZERO));
		doc.setQuantityUnqualified(unqualified);

		if (pass)
			doc.onPass();
	}

	private BigDecimal rateBD(BigDecimal qty, BigDecimal total) {
		if (qty == null)
			qty = BigDecimal.ZERO;
		if (total == null || total.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		return qty.multiply(BigDecimal.valueOf(100)).divide(total, SCALE, ROUNDING);
	}

	private BigDecimal nvl(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}
}