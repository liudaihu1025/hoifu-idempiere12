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
import com.hoifu.model.qc.MQC_OQC;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateProduct;
import com.hoifu.service.qc.IOQCService;

public class OQCServiceImpl implements IOQCService {

	private static final CLogger log = CLogger.getCLogger(OQCServiceImpl.class);
	private final QCLineGeneratorServiceImpl lineGenerator = new QCLineGeneratorServiceImpl();

	@Override
	public void createFromShipmentLine(MInOutLine line) {
		if (existsForLine(line))
			return;

		String trxName = Trx.createTrxName("OQC");
		Trx trx = Trx.get(trxName, true);
		try {
			MQC_Template template = MQC_Template.getForProduct(line.getCtx(), line.getM_Product_ID(), "OQC",
					trx.getTrxName());
			if (template == null)
				return;

			MQC_TemplateProduct templateProduct = MQC_TemplateProduct.get(line.getCtx(), line.getM_Product_ID(),
					template.get_ID(), trxName);
			if (templateProduct == null)
				return;

			MInOut shipment = (MInOut) line.getParent();
			MProduct product = MProduct.get(line.getCtx(), line.getM_Product_ID());

			// 批次号：从属性集实例获取
			String batchCode = null;
			if (line.getM_AttributeSetInstance_ID() > 0) {
				MAttributeSetInstance asi = new MAttributeSetInstance(line.getCtx(),
						line.getM_AttributeSetInstance_ID(), trx.getTrxName());
				batchCode = asi.getLot();
			}

			// 检验员：当前登录用户
			String inspector = MUser.get(line.getCtx()).getName();

			MQC_OQC oqc = new MQC_OQC(line.getCtx(), 0, trx.getTrxName());
			oqc.setAD_Org_ID(line.getAD_Org_ID());
			oqc.setC_BPartner_ID(shipment.getC_BPartner_ID());
			oqc.setM_Product_ID(product.getM_Product_ID());

			// 计算 AQL 抽样数量
			AQLEngine aqlEngine = new AQLEngine();
			AQLEngine.AQLResult aql = aqlEngine.calculate(line.getCtx(), line.getMovementQty().intValue(), template,
					templateProduct, trxName);

			// 数量
			oqc.setQuantityOut(line.getMovementQty());
			oqc.setQuantityCheck(BigDecimal.valueOf(aql.getSampleSize()));
			oqc.setQuantityQualified(BigDecimal.ZERO);
			oqc.setQuantityUnqualified(BigDecimal.ZERO);
			
			// 批次
			if (batchCode != null && !batchCode.isEmpty())
				oqc.setBatchCode(batchCode);

			// 日期
			oqc.setOutDate(shipment.getMovementDate());
			oqc.setInspectDate(new Timestamp(System.currentTimeMillis()));

			// 检验员
			if (inspector != null)
				oqc.setInspector(inspector);
			oqc.setDescription("出货检验 - " + product.getName() + " - 发货单: " + shipment.getDocumentNo());

			// 来源单据
			oqc.setQC_Template_ID(template.getQC_Template_ID());
			oqc.setSourceDocID(shipment.getM_InOut_ID());
			oqc.setSourceLineID(line.getM_InOutLine_ID());
			oqc.setSourceDocTypeID(shipment.getC_DocType_ID());
			oqc.setSourceDocCode(shipment.getDocumentNo());

			// 单据状态
			oqc.setDocStatus(DocAction.STATUS_Drafted);
			oqc.setDocAction(DocAction.ACTION_Complete);

			// 标志位
			oqc.setProcessed(false);
			oqc.setProcessing(false);
			oqc.setIsApproved(false);

			oqc.saveEx();
			lineGenerator.generateLines(oqc, template, trx.getTrxName());
			trx.commit();
			log.info("创建 OQC: " + oqc.getDocumentNo());
		} catch (Exception e) {
			log.severe("创建 OQC 失败: " + e.getMessage());
			try {
				trx.rollback();
			} catch (Exception ex) {
				/* ignore */ }
		} finally {
			trx.close();
		}
	}

	@Override
	public void validateBeforeShipment(MInOut shipment) {
		for (MInOutLine line : shipment.getLines()) {
			if (!isOQCPassed(line))
				throw new RuntimeException("发货单 [" + shipment.getDocumentNo() + "] 物料 [" + line.getM_Product().getName()
						+ "] 出货检验（OQC）未通过，禁止出库。");
		}
	}

	private boolean isOQCPassed(MInOutLine line) {
		// 先判断是否存在 OQC 记录
		String countSql = "SELECT COUNT(*) FROM QC_OQC " + "WHERE SourceLineID=? AND AD_Client_ID=?";
		int total = DB.getSQLValueEx(null, countSql, line.getM_InOutLine_ID(), line.getAD_Client_ID());
		if (total == 0)
			return true; // 无 OQC 记录，无需检验，直接放行

		// 存在 OQC 记录，则必须有已完成且通过的记录
		String passSql = "SELECT COUNT(*) FROM QC_OQC " + "WHERE SourceLineID=? AND AD_Client_ID=? "
				+ "AND CheckResult='PASS' AND DocStatus='CO'";
		return DB.getSQLValueEx(null, passSql, line.getM_InOutLine_ID(), line.getAD_Client_ID()) > 0;
	}

	private boolean existsForLine(MInOutLine line) {
		String sql = "SELECT COUNT(*) FROM QC_OQC " + "WHERE SourceLineID=? AND AD_Client_ID=?";
		return DB.getSQLValueEx(null, sql, line.getM_InOutLine_ID(), line.getAD_Client_ID()) > 0;
	}
}