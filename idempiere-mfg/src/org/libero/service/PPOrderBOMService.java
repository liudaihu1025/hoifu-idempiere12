// idempiere-mfg/src/org/libero/service/PPOrderBOMService.java  
package org.libero.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.form.vo.BOMLineVO;
import org.libero.model.MPPOrderBOMLine;

/**
 * BOM模板（PP_Product_BOM）及工单BOM（PP_Order_BOM）操作服务
 */
public class PPOrderBOMService {

	private static final CLogger log = CLogger.getCLogger(PPOrderBOMService.class);

	/**
	 * 创建或更新产品BOM模板。
	 * <ul>
	 * <li>若 existingBOMId <= 0，新建 MPPProductBOM</li>
	 * <li>若 existingBOMId > 0，更新已有 MPPProductBOM 的明细</li>
	 * </ul>
	 * 
	 * @param ctx           上下文
	 * @param trxName       事务名
	 * @param orgId         组织ID
	 * @param productId     产品ID
	 * @param uomId         单位ID
	 * @param dateStart     计划开工日期
	 * @param createdByName 创建人姓名（用于描述）
	 * @param existingBOMId 已关联的 PP_Product_BOM_ID，0 表示新建
	 * @param bomLines      BOM物料行列表
	 * @return 保存后的 PP_Product_BOM_ID
	 */
	public int createOrUpdateBOMTemplate(Properties ctx, String trxName, int orgId, int productId, int uomId,
			Timestamp dateStart, BigDecimal nRate, String createdByName, int existingBOMId, // > 0 表示更新已有模板
			List<BOMLineVO> bomLines) {

		MProduct product = MProduct.get(ctx, productId);
		String productValue = product.getValue();
		String productName = product.getName();

		MPPProductBOM bom;

		if (existingBOMId > 0) {
			// ── 更新已有模板 ──────────────────────────────────────────────────
			bom = new MPPProductBOM(ctx, existingBOMId, trxName);
			// 删除旧明细，重新写入
			for (MPPProductBOMLine oldLine : bom.getLines()) {
				oldLine.deleteEx(true);
			}
		} else {
			// ── 新建模板：生成唯一 Value ──────────────────────────────────────
			bom = new MPPProductBOM(ctx, 0, trxName);
			bom.setAD_Org_ID(orgId);
			bom.setM_Product_ID(productId);
			bom.setC_UOM_ID(uomId);

			// ✅ 关键：加时间戳后缀，避免 ppproductbomunique 冲突
			String dateStr = new java.text.SimpleDateFormat("yyMMddHHmmssSSS").format(new java.util.Date());
			bom.setValue(productValue + "_WO_" + dateStr);

			// 名称：产品名称 + 日期（需求文档格式）
//			String dateDisplay = new java.text.SimpleDateFormat("yyMMdd")
//					.format(dateStart != null ? dateStart : new java.util.Date());
			bom.setName(productName + dateStr);
			bom.setDescription(productValue + " " + productName + " " + dateStr + " " + createdByName);

			// 使用 Manufacturing 用途，避免 BOMTYPE_CurrentActive+BOMUSE_Master 的唯一性校验
			bom.setBOMType(MPPProductBOM.BOMTYPE_CurrentActive); // 'A'
			bom.setBOMUse(MPPProductBOM.BOMUSE_Manufacturing); // 'M'（不是 Master='A'）
			bom.setValidFrom(dateStart);
			bom.set_ValueOfColumn("bomstatus", "Released");
			bom.set_ValueOfColumn("nRate", nRate);
		}

		bom.saveEx(trxName);

		// ── 写入 BOM 明细 ─────────────────────────────────────────────────────
		int lineNo = 10;
		boolean isFirst = true;
		for (BOMLineVO vo : bomLines) {
			if (vo.isDeleted)
				continue;
			MPPProductBOMLine line = new MPPProductBOMLine(bom);
			line.setAD_Org_ID(orgId);
			line.setM_Product_ID(vo.productId);
			line.setLine(lineNo);
			line.setQtyBOM(vo.qtyBOM != null ? vo.qtyBOM : BigDecimal.ONE);
			line.set_ValueOfColumn("nQtyBOM", vo.nQtyBOM != null ? vo.nQtyBOM : BigDecimal.ONE);
			line.setQtyBatch(BigDecimal.ZERO);
			line.setC_UOM_ID(vo.uomId);
			line.setComponentType("CO");
			line.setIssueMethod("1");
			line.setValidFrom(dateStart);
			line.setKeymat(isFirst); // 第一行为关键物料，explosionWorkflow() 需要
			isFirst = false;
			if (vo.routingNodeId > 0)
				line.set_ValueOfColumn("AD_Routing_Node_ID", vo.routingNodeId);
			line.saveEx(trxName);
			vo.ppProductBOMLineId = line.getPP_Product_BOMLine_ID();
			lineNo += 10;
		}

		return bom.getPP_Product_BOM_ID();
	}

	/**
	 * 计算BOM行的需求用量（与工单创建窗口 CalloutBOM.qtyLine 逻辑一致）
	 */
	public BigDecimal calculateQtyRequiered(Properties ctx, int productId, int uomId, BigDecimal qtyBOM,
			BigDecimal orderQty) {
		if (qtyBOM == null || qtyBOM.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		if (orderQty == null || orderQty.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		return qtyBOM.multiply(orderQty);
	}

	/**
	 * 从已有工单加载BOM行到VO列表（用于查询展示）
	 */
	public void loadBOMLines(Properties ctx, int ppOrderId, List<BOMLineVO> target) {
		target.clear();

		// 先获取工单关联的 PP_Product_BOM_ID
		int ppProductBOMId = DB.getSQLValue(null, "SELECT PP_Product_BOM_ID FROM PP_Order WHERE PP_Order_ID=?",
				ppOrderId);

		String where = "PP_Order_ID=? AND IsActive='Y'";
		List<MPPOrderBOMLine> lines = new Query(ctx, MPPOrderBOMLine.Table_Name, where, null).setParameters(ppOrderId)
				.setOrderBy("Line").list();

		int seq = 1;
		for (MPPOrderBOMLine l : lines) {
			BOMLineVO vo = new BOMLineVO();
			vo.lineNo = seq++;
			vo.productId = l.getM_Product_ID();
			MProduct p = MProduct.get(ctx, l.getM_Product_ID());
			vo.productValue = p.getValue();
			vo.productName = p.getName();
			vo.uomId = l.getC_UOM_ID();
			vo.uomName = DB.getSQLValueString(null, "SELECT UOMSymbol FROM C_UOM WHERE C_UOM_ID=?", l.getC_UOM_ID());
			vo.qtyBOM = l.getQtyBOM();
			vo.qtyPaperTotalScrap = new BigDecimal(l.get_ValueAsInt("QtyPaperTotalScrap"));
			vo.qtyRequiered = l.getQtyRequiered();
			vo.qtyDelivered = l.getQtyDelivered();
			vo.ppOrderBOMLineId = l.getPP_Order_BOMLine_ID();
			vo.ppOrderNodeId = l.get_ValueAsInt("PP_Order_Node_ID");
			if (l.get_ValueAsString("EffectName") != null)
				vo.effectName = l.get_ValueAsString("EffectName");
			
			if (vo.ppOrderNodeId > 0) {
				// 一次查询同时取 Name 和 AD_Routing_Node_ID
				java.sql.PreparedStatement ps = null;
				java.sql.ResultSet rs2 = null;
				try {
					ps = DB.prepareStatement(
							"SELECT Name, AD_Routing_Node_ID FROM PP_Order_Node WHERE PP_Order_Node_ID=?", null);
					ps.setInt(1, vo.ppOrderNodeId);
					rs2 = ps.executeQuery();
					if (rs2.next()) {
						vo.routingNodeName = rs2.getString(1);
						vo.routingNodeId = rs2.getInt(2); // ✅ 新增
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "loadBOMLines routingNode error", e);
				} finally {
					DB.close(rs2, ps);
				}
			}

			// 从 PP_Product_BOMLine 获取 nQtyBOM（比例用量）
			if (ppProductBOMId > 0) {
				BigDecimal nQtyBOM = DB.getSQLValueBD(null,
						"SELECT nQtyBOM FROM PP_Product_BOMLine "
								+ "WHERE PP_Product_BOM_ID=? AND M_Product_ID=? AND IsActive='Y'",
						ppProductBOMId, l.getM_Product_ID());
				vo.nQtyBOM = nQtyBOM != null ? nQtyBOM : BigDecimal.ONE;
			}
			target.add(vo);
		}
	}

}