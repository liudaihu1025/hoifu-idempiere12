package com.hoifu.service.qc.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUser;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import com.hoifu.engine.AQLEngine;
import com.hoifu.model.qc.MQC_RQC;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateProduct;
import com.hoifu.service.qc.IRQCService;

public class RQCServiceImpl implements IRQCService {

	private static final CLogger log = CLogger.getCLogger(RQCServiceImpl.class);
	private final QCLineGeneratorServiceImpl lineGenerator = new QCLineGeneratorServiceImpl();

	@Override
	public void createFromLine(MInOut io, MInOutLine line) {
		// 按行判断，移除原来的按单头 exists() 判断（会导致第一行已存在时后续行全部跳过）
		if (lineRqcExists(io, line))
			return;

		String trxName = Trx.createTrxName("IQC");
		Trx trx = Trx.get(trxName, true);
		int productId = line.getM_Product_ID();

		try {
			MQC_Template template = MQC_Template.getForProduct(io.getCtx(), productId, "RQC", trxName);
			if (template == null)
				return; // 该产品无模板，跳过

			MQC_TemplateProduct templateProduct = MQC_TemplateProduct.get(line.getCtx(), line.getM_Product_ID(),
					template.get_ID(), trxName);
			if (templateProduct == null)
				return;

			MInOut returnOrder = (MInOut) line.getParent();
			MProduct product = MProduct.get(line.getCtx(), line.getM_Product_ID());
			MQC_RQC rqc = new MQC_RQC(io.getCtx(), 0, trx.getTrxName());

			// ===== 组织/业务伙伴/产品 =====
			rqc.setAD_Org_ID(io.getAD_Org_ID());
			rqc.setC_BPartner_ID(io.getC_BPartner_ID());
			rqc.setM_Product_ID(productId);

			// ===== 退货类型 =====
			rqc.setRQCType(io.isSOTrx() ? "SALES" : "PRODUCTION");

			// 计算 AQL 抽样数量
			AQLEngine aqlEngine = new AQLEngine();
			AQLEngine.AQLResult aql = aqlEngine.calculate(line.getCtx(), line.getMovementQty().intValue(), template,
					templateProduct, trxName);

			// ===== 数量 =====
			rqc.setQuantityApply(line.getMovementQty());
			rqc.setQuantityCheck(BigDecimal.valueOf(aql.getSampleSize()));
			rqc.setQuantityQualified(BigDecimal.ZERO);
			rqc.setQuantityUnqualified(BigDecimal.ZERO);

			// ===== 模板 =====
			rqc.setQC_Template_ID(template.getQC_Template_ID());

			// ===== 来源单据 =====
			rqc.setSourceDocID(line.getM_InOut_ID());
			rqc.setSourceLineID(line.getM_InOutLine_ID());
			rqc.setSourceDocTypeID(io.getC_DocType_ID());
			rqc.setSourceDocCode(io.getDocumentNo());

			// ===== 批次号（写入 Description，I_QC_RQC 无独立 BatchCode 字段）=====
			int asiId = line.getM_AttributeSetInstance_ID();
			if (asiId > 0) {
				MAttributeSetInstance asi = new MAttributeSetInstance(io.getCtx(), asiId, io.get_TrxName());
				String lot = asi.getLot();
				if (lot != null && !lot.isEmpty())
					rqc.setDescription("批次: " + lot);
			}

			// ===== 检验日期 / 检验员 =====
			rqc.setInspectDate(new Timestamp(System.currentTimeMillis()));
			MUser user = MUser.get(io.getCtx());
			if (user != null && user.get_ID() > 0)
				rqc.setInspector(user.getName());
			rqc.setDescription("退料检验 - " + product.getName() + " - 退货单: " + returnOrder.getDocumentNo());

			// ===== 单据状态 =====
			rqc.setDocStatus(DocAction.STATUS_Drafted);
			rqc.setDocAction(DocAction.ACTION_Complete);
			rqc.setProcessed(false);
			rqc.setProcessing(false);
			rqc.setIsApproved(false);

			rqc.saveEx();
			lineGenerator.generateLines(rqc, template, trx.getTrxName());
			trx.commit();
			log.info("创建 RQC: " + rqc.getDocumentNo());
		} catch (Exception e) {
			log.severe("创建 RQC 失败，不影响退货主业务: " + e.getMessage());
			try {
				trx.rollback();
			} catch (Exception ex) {
				/* ignore */ }
		} finally {
			trx.close();
		}
	}

	@Override
	public void validateBeforeReturn(MInOut io) {
		for (MInOutLine line : io.getLines()) {
			String countSql = "SELECT COUNT(*) FROM QC_RQC WHERE SourceDocID=? AND SourceLineID=? AND AD_Client_ID=?";
			int total = DB.getSQLValueEx(null, countSql, io.getM_InOut_ID(), line.getM_InOutLine_ID(),
					io.getAD_Client_ID());
			if (total == 0)
				continue; // 该产品无 RQC 模板，跳过

			// 必须有已完成的 RQC
			String doneSql = "SELECT Disposition FROM QC_RQC "
					+ "WHERE SourceDocID=? AND SourceLineID=? AND AD_Client_ID=? AND DocStatus='CO' AND CheckResult='PASS'  "
					+ "ORDER BY QC_RQC_ID DESC FETCH FIRST 1 ROWS ONLY";
			String disposition = DB.getSQLValueString(null, doneSql, io.getM_InOut_ID(), line.getM_InOutLine_ID(),
					io.getAD_Client_ID()); // 补充 AD_Client_ID

			if (disposition == null) {
				throw new RuntimeException("退货单 " + io.getDocumentNo() + " 物料 [" + line.getM_Product().getName()
						+ "] 退货检验（RQC）未通过，禁止完成退货。");
			}
			if ("REJECT".equals(disposition)) {
				throw new RuntimeException("退货单 " + io.getDocumentNo() + " 物料 [" + line.getM_Product().getName()
						+ "] 退货检验（RQC）结论为拒绝退货，请人工处理。");
			}
		}
	}

	@Override
	public boolean isReturnDocument(MInOut io) {
		int docTypeId = io.getC_DocType_ID();
		if (docTypeId <= 0)
			return false;
		MDocType dt = MDocType.get(io.getCtx(), docTypeId);
		return dt != null && ("客户退货单".equals(dt.getName()) || "供应商退货单".equals(dt.getName()));
	}

	private boolean lineRqcExists(MInOut io, MInOutLine line) {
		String sql = "SELECT COUNT(*) FROM QC_RQC WHERE SourceDocID=? AND SourceLineID=? AND AD_Client_ID=?";
		return DB.getSQLValueEx(null, sql, io.getM_InOut_ID(), line.getM_InOutLine_ID(), io.getAD_Client_ID()) > 0;
	}
}