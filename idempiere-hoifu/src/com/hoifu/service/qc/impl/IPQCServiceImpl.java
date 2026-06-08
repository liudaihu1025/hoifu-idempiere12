package com.hoifu.service.qc.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MProduct;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import com.hoifu.engine.AQLEngine;
import com.hoifu.model.qc.MQC_IPQC;
import com.hoifu.model.qc.MQC_Template;
import com.hoifu.model.qc.MQC_TemplateProduct;
import com.hoifu.service.qc.IIPQCService;

public class IPQCServiceImpl implements IIPQCService {

	private static final CLogger log = CLogger.getCLogger(IPQCServiceImpl.class);
	private final QCLineGeneratorServiceImpl lineGenerator = new QCLineGeneratorServiceImpl();

	@Override
	public boolean createFromPPOrder(PO ppOrder) {
		if (existsForPPOrder(ppOrder))
			return true;

		String trxName = Trx.createTrxName("IPQC");
		Trx trx = Trx.get(trxName, true);
		try {
			int productId = ppOrder.get_ValueAsInt("M_Product_ID");
			MQC_Template template = MQC_Template.getForProduct(ppOrder.getCtx(), productId, "IPQC", trxName);
			if (template == null) {
				log.info("产品 " + productId + " 无 IPQC 模板，跳过创建");
				return false;
			}

			MQC_TemplateProduct templateProduct = MQC_TemplateProduct.get(ppOrder.getCtx(), productId,
					template.get_ID(), trxName);
			if (templateProduct == null)
				return false;

			// 从工单取计划数量，作为检验数量
			BigDecimal qtyOrdered = (BigDecimal) ppOrder.get_Value("QtyEntered");
			if (qtyOrdered == null || qtyOrdered.signum() <= 0)
				qtyOrdered = BigDecimal.ONE;

			String ppDocNo = ppOrder.get_ValueAsString("DocumentNo");
			MProduct product = MProduct.get(ppOrder.getCtx(), productId);

			MQC_IPQC ipqc = new MQC_IPQC(ppOrder.getCtx(), 0, trxName);

			// 组织 / 产品
			ipqc.setAD_Org_ID(ppOrder.get_ValueAsInt("AD_Org_ID"));
			ipqc.setM_Product_ID(productId);

			// 关联工单
			ipqc.setPP_Order_ID(ppOrder.get_ValueAsInt("PP_Order_ID"));

			// 来源单据
			ipqc.setSourceDocTypeID(ppOrder.get_ValueAsInt("C_DocTypeTarget_ID"));
			ipqc.setSourceDocID(ppOrder.get_ValueAsInt("PP_Order_ID"));
			ipqc.setSourceDocCode(ppDocNo);
			// SourceLineID 工单无行，保持 0

			// 模板方案
			ipqc.setQC_Template_ID(template.getQC_Template_ID());

			// 检验名称 / 类型
			ipqc.setIPQCType("FIRST"); // 检验类型：FIRST-首检/ROUTINE-巡检/FINAL -完工检

			// 计算 AQL 抽样数量
			int lotSize = qtyOrdered.intValue();
			AQLEngine aqlEngine = new AQLEngine();
			AQLEngine.AQLResult aql = aqlEngine.calculate(ppOrder.getCtx(), lotSize, template, templateProduct,
					trxName);

			// 检验数量（初始值）根据AQL进行赋值
			ipqc.setQuantityCheck(BigDecimal.valueOf(aql.getSampleSize()));
			ipqc.setQuantityReceived(qtyOrdered);
			ipqc.setQuantityQualified(BigDecimal.ZERO);
			ipqc.setQuantityUnqualified(BigDecimal.ZERO);

			// 缺陷数量 / 比率（初始值）
			ipqc.setCR_Quantity(BigDecimal.ZERO);
			ipqc.setMAJ_Quantity(BigDecimal.ZERO);
			ipqc.setMIN_Quantity(BigDecimal.ZERO);
			ipqc.setCR_Rate(BigDecimal.ZERO);
			ipqc.setMAJ_Rate(BigDecimal.ZERO);
			ipqc.setMIN_Rate(BigDecimal.ZERO);

			// 检验日期（创建时默认今天，完成时由 completeIt 覆盖）
			ipqc.setInspectDate(new Timestamp(System.currentTimeMillis()));

			// 检验员（当前登录用户）
			MUser user = MUser.get(ppOrder.getCtx());
			if (user != null && user.get_ID() > 0)
				ipqc.setInspector(user.getName());

			// 工作站（工单资源 S_Resource_ID）
			int resourceId = ppOrder.get_ValueAsInt("S_Resource_ID");
			if (resourceId > 0)
				ipqc.setS_Resource_ID(resourceId);
			ipqc.setDescription("过程检验 - " + product.getName() + " - 生产工单: " + ppDocNo);

			// 单据状态
			ipqc.setDocStatus(DocAction.STATUS_Drafted);
			ipqc.setDocAction(DocAction.ACTION_Complete);

			// 处理状态（初始值，DDL DEFAULT 已是 N，显式写出意图更清晰）
			ipqc.setProcessed(false);
			ipqc.setProcessing(false);
			ipqc.setIsApproved(false);

			ipqc.saveEx();
			lineGenerator.generateLines(ipqc, template, trxName);
			trx.commit();
			log.info("创建 IPQC: " + ipqc.getDocumentNo());
			return true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "创建 IPQC 失败，不影响工单主业务: " + e.getMessage(), e);
			try {
				trx.rollback();
			} catch (Exception ex) {
			}
			return false;
		} finally {
			trx.close();
		}
	}

	@Override
	public boolean existsForPPOrder(PO ppOrder) {
		int id = ppOrder.get_ValueAsInt("PP_Order_ID");
		String sql = "SELECT COUNT(*) FROM QC_IPQC WHERE PP_Order_ID=? AND AD_Client_ID=?";
		return DB.getSQLValueEx(null, sql, id, ppOrder.getAD_Client_ID()) > 0;
	}

	@Override
	public boolean isCompletedAndPassed(PO ppOrder) {
		int id = ppOrder.get_ValueAsInt("PP_Order_ID");
		String sql = "SELECT COUNT(*) FROM QC_IPQC "
				+ "WHERE PP_Order_ID=? AND DocStatus='CO' AND CheckResult='PASS' AND AD_Client_ID=?";
		return DB.getSQLValueEx(null, sql, id, ppOrder.getAD_Client_ID()) > 0;
	}

}