package com.hoifu.service.qc.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUser;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import com.hoifu.engine.AQLEngine;
import com.hoifu.model.qc.MQC_IQC;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateProduct;
import com.hoifu.service.qc.IIQCService;

public class IQCServiceImpl implements IIQCService {

	private static final CLogger log = CLogger.getCLogger(IQCServiceImpl.class);
	private final QCLineGeneratorServiceImpl lineGenerator = new QCLineGeneratorServiceImpl();

	@Override
	public void createFromReceiptLine(MInOutLine line) {
		if (exists(line))
			return;

		String trxName = Trx.createTrxName("IQC");
		Trx trx = Trx.get(trxName, true);
		try {
			MQC_Template template = MQC_Template.getForProduct(line.getCtx(), line.getM_Product_ID(), "IQC", trxName);
			if (template == null)
				return;

			MQC_TemplateProduct templateProduct = MQC_TemplateProduct.get(line.getCtx(), line.getM_Product_ID(),
					template.get_ID(), trxName);
			if (templateProduct == null)
				return;

			MInOut receipt = (MInOut) line.getParent();
			MProduct product = MProduct.get(line.getCtx(), line.getM_Product_ID());

			// 供应商批次号（从属性集实例获取）
			String vendorBatch = null;
			if (line.getM_AttributeSetInstance_ID() > 0) {
				MAttributeSetInstance asi = new MAttributeSetInstance(line.getCtx(),
						line.getM_AttributeSetInstance_ID(), trx.getTrxName());
				vendorBatch = asi.getLot(); // getLot() 返回批次号字符串
			}

			// 当前检验员（从上下文用户获取）
			String inspector = null;
			try {
				MUser user = MUser.get(line.getCtx());
				if (user != null && user.get_ID() > 0)
					inspector = user.getName();
			} catch (Exception e) {
				// 检验员非必填，忽略
			}

			MQC_IQC iqc = new MQC_IQC(line.getCtx(), 0, trx.getTrxName());
			iqc.setAD_Org_ID(line.getAD_Org_ID());
			iqc.setC_BPartner_ID(receipt.getC_BPartner_ID()); // 修复：直接取 ID
			iqc.setM_Product_ID(product.getM_Product_ID());
			iqc.setQC_Template_ID(template.getQC_Template_ID());

			
			// 计算 AQL 抽样数量  
			int lotSize = line.getMovementQty().intValue();  
			AQLEngine aqlEngine = new AQLEngine();  
			AQLEngine.AQLResult aql = aqlEngine.calculate(line.getCtx(), lotSize, template, templateProduct, trxName);  
			
			// 数量
			iqc.setQuantityReceived(line.getMovementQty());
			iqc.setQuantityCheck(BigDecimal.valueOf(aql.getSampleSize()));
			iqc.setQuantityQualified(BigDecimal.ZERO);
			iqc.setQuantityUnqualified(BigDecimal.ZERO);
			
			// 来源单据（修复：SourceDocID=主表ID，SourceLineID=行ID）
			iqc.setSourceDocID(line.getM_InOut_ID());
			iqc.setSourceLineID(line.getM_InOutLine_ID());
			iqc.setSourceDocTypeID(receipt.getC_DocType_ID());
			iqc.setSourceDocCode(receipt.getDocumentNo());

			// 日期
			iqc.setReceiveDate(receipt.getMovementDate());
			iqc.setInspectDate(new Timestamp(System.currentTimeMillis()));

			// 可选字段
			if (vendorBatch != null && !vendorBatch.isEmpty())
				iqc.setVendorBatch(vendorBatch);
			if (inspector != null && !inspector.isEmpty())
				iqc.setInspector(inspector);
			iqc.setDescription("来料检验 - " + product.getName() + " - 收货单: " + receipt.getDocumentNo());

			// 单据状态
			iqc.setDocStatus(DocAction.STATUS_Drafted);
			iqc.setDocAction(DocAction.ACTION_Complete);
			iqc.setProcessed(false);
			iqc.setProcessing(false);
			iqc.setIsApproved(false);

			iqc.saveEx();
			lineGenerator.generateLines(iqc, template, trx.getTrxName());
			trx.commit();
			log.info("创建 IQC: " + iqc.getDocumentNo());
		} catch (Exception e) {
			log.severe("创建 IQC 失败，不影响收货主业务: " + e.getMessage());
			try {
				trx.rollback();
			} catch (Exception ex) {
				/* ignore */ }
		} finally {
			trx.close();
		}
	}

	@Override
	public void validateBeforeReceipt(MInOut receipt) {
		for (MInOutLine line : receipt.getLines()) {
			// 无 IQC 记录（该产品无模板），直接放行
			String countSql = "SELECT COUNT(*) FROM QC_IQC "
					+ "WHERE SourceLineID=? AND AD_Client_ID=?";
			int total = DB.getSQLValueEx(null, countSql, line.getM_InOutLine_ID(), line.getAD_Client_ID());
			if (total == 0)
				continue;

			// 有 IQC 记录，必须有已完成且通过的记录
			String passSql = "SELECT COUNT(*) FROM QC_IQC " + "WHERE SourceLineID=? "
					+ "AND CheckResult='PASS' AND DocStatus='CO' AND AD_Client_ID=?";
			int passed = DB.getSQLValueEx(null, passSql, line.getM_InOutLine_ID(), line.getAD_Client_ID());
			if (passed == 0) {
				MProduct product = MProduct.get(receipt.getCtx(), line.getM_Product_ID());
				throw new RuntimeException(
						"收货单 " + receipt.getDocumentNo() + " 物料 [" + product.getName() + "] 来料检验（IQC）未通过，禁止入库。");
			}
		}
	}

	private boolean exists(MInOutLine line) {
		String sql = "SELECT COUNT(*) FROM QC_IQC "
				+ "WHERE SourceLineID=? AND AD_Client_ID=?";
		return DB.getSQLValueEx(null, sql, line.getM_InOutLine_ID(), line.getAD_Client_ID()) > 0;
	}
}