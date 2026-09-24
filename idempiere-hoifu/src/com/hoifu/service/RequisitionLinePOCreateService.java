package com.hoifu.service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MWarehouse;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 申购单明细转采购订单的公共逻辑，供 RequisitionDetailPOCreate 和 PurchaseOrderRecreationProcess
 * 共用。
 */
public class RequisitionLinePOCreateService {

	private static final String COLUMN_SUBCONTRACTLINE_IDS = "SubcontractLine_IDs";

	/**
	 * 从 T_Selection 读取当前 PInstance 下用户勾选的申购单明细 ID
	 */
	public static List<Integer> loadSelectedRequisitionLines(int AD_PInstance_ID, String trxName) {
		List<Integer> selectedLineIds = new ArrayList<>();
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, AD_PInstance_ID);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				selectedLineIds.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取申购单明细失败");
		} finally {
			DB.close(rs, pstmt);
		}
		return selectedLineIds;
	}

	/**
	 * 根据 UUID 找到采购订单单据类型
	 */
	public static int resolveOrderDocType(Properties ctx, String docTypeUU, String trxName) {
		MDocType docType = new MDocType(ctx, docTypeUU, trxName);
		int docTypeId = docType.getC_DocType_ID();
		if (docTypeId == 0) {
			throw new AdempiereUserError("找不到指定的采购订单单据类型，请检查 UUID 配置:" + docTypeUU);
		}
		return docTypeId;
	}

	/**
	 * 按申购单主表的仓库ID分组
	 */
	public static Map<Integer, List<MRequisitionLine>> groupByWarehouse(List<MRequisitionLine> lines) {
		Map<Integer, List<MRequisitionLine>> warehouseGroups = new HashMap<>();
		for (MRequisitionLine reqLine : lines) {
			MRequisition requisition = reqLine.getParent();
			int warehouseId = requisition.getM_Warehouse_ID();
			warehouseGroups.computeIfAbsent(warehouseId, k -> new ArrayList<>()).add(reqLine);
		}
		return warehouseGroups;
	}

	/**
	 * 为一个仓库分组创建一张草稿采购订单（含订单行合并、回写 C_OrderLine_ID）
	 *
	 * @return 新建订单的 DocumentNo
	 */
	public static CreatedOrderResult createDraftOrderForWarehouse(Properties ctx, String trxName, int docTypeId,
																  MBPartner bpartner, int warehouseId, int salesRepId, String purpose, List<MRequisitionLine> lines) {

		MWarehouse warehouse = MWarehouse.get(ctx, warehouseId);

		// 创建采购订单
		MOrder order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(docTypeId);
		order.setBPartner(bpartner);
		order.setAD_Org_ID(warehouse.getAD_Org_ID());
		order.setSalesRep_ID(salesRepId);
		order.setM_Warehouse_ID(warehouseId);
		order.setDeliveryViaRule("D");

		if (purpose != null && !purpose.isEmpty()) {
			order.set_CustomColumn("Purpose", purpose);
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.add(Calendar.DAY_OF_MONTH, 7);
		order.setDateOrdered(new Timestamp(System.currentTimeMillis()));
		order.setDatePromised(new Timestamp(cal.getTimeInMillis()));

		// POReference：去重后的申购单DocumentNo
		Set<String> documentNos = new HashSet<>();
		for (MRequisitionLine reqLine : lines) {
			MRequisition requisition = reqLine.getParent();
			if (requisition != null && requisition.getDocumentNo() != null) {
				documentNos.add(requisition.getDocumentNo());
			}
		}
		if (!documentNos.isEmpty()) {
			order.set_CustomColumn("POReference", String.join(" | ", documentNos));
		}

		// ENo：从所有 reqLine 的 PP_Order_IDs（逗号分隔）收集去重后的 PP_Order_ID，
		// 再查询对应 DocumentNo 并拼接
		Set<String> enoSet = new LinkedHashSet<>();
		for (MRequisitionLine reqLine : lines) {
			enoSet.addAll(resolveDocumentNos(reqLine, trxName));
		}
		if (!enoSet.isEmpty()) {
			order.set_CustomColumn("ENo", String.join(" | ", enoSet));
		}

		order.saveEx();

		// 按行号排序，再按产品/费用合并
		lines.sort((a, b) -> Integer.compare(a.getLine(), b.getLine()));
		Map<String, List<MRequisitionLine>> productGroups = new LinkedHashMap<>();
		for (MRequisitionLine reqLine : lines) {
			String productKey = reqLine.getM_Product_ID() > 0
					? "P_" + reqLine.getM_Product_ID() + "_" + reqLine.getM_AttributeSetInstance_ID()
					: "C_" + reqLine.getC_Charge_ID();
			productGroups.computeIfAbsent(productKey, k -> new ArrayList<>()).add(reqLine);
		}

		int processedLines = 0;
		for (List<MRequisitionLine> productLines : productGroups.values()) {
			MRequisitionLine firstLine = productLines.get(0);

			MOrderLine orderLine = new MOrderLine(order);
			orderLine.setLine((processedLines + 1) * 10);
			orderLine.setAD_Org_ID(firstLine.getAD_Org_ID());

			if (firstLine.getM_Product_ID() > 0) {
				MProduct product = MProduct.get(ctx, firstLine.getM_Product_ID());
				orderLine.setProduct(product);
				orderLine.setM_AttributeSetInstance_ID(firstLine.getM_AttributeSetInstance_ID());
				orderLine.setC_UOM_ID(product.getC_UOM_ID());
			} else if (firstLine.getC_Charge_ID() > 0) {
				orderLine.setC_Charge_ID(firstLine.getC_Charge_ID());
			}

			BigDecimal totalQty = Env.ZERO;
			for (MRequisitionLine reqLine : productLines) {
				totalQty = totalQty.add(reqLine.getQty());
			}
			orderLine.setQty(totalQty);
//			orderLine.setPrice(order.getM_PriceList_ID());

			// 单价默认取申购明细的单价字段（同物料多行任取第一条即可），不再走价目表引擎取价
			BigDecimal priceActual = (BigDecimal) firstLine.get_Value("PriceActual");
			if (priceActual == null) {
				priceActual = Env.ZERO;
			}
			orderLine.setPriceEntered(priceActual);

			BigDecimal lineNetAmt = (BigDecimal) firstLine.get_Value("LineNetAmt");
			if (lineNetAmt == null) {
				lineNetAmt = Env.ZERO;
			}
			orderLine.setLineNetAmt(lineNetAmt);

			orderLine.setPriceActual(priceActual);

			// 查询价格表中的标准价格，赋值给 PriceStd
			int priceListId = order.getM_PriceList_ID();
			Timestamp dateOrdered = order.getDateOrdered();

			BigDecimal priceStd = Env.ZERO;
			if (orderLine.getM_Product_ID() > 0 && priceListId > 0) {

				String priceStdSql = "SELECT pp.PriceStd FROM M_ProductPrice pp "
						+ "JOIN M_PriceList_Version plv ON plv.M_PriceList_Version_ID = pp.M_PriceList_Version_ID "
						+ "JOIN M_PriceList pl ON pl.M_PriceList_ID = plv.M_PriceList_ID "
						+ "WHERE pp.M_Product_ID = ? AND pp.IsActive = 'Y' "
						+ "AND plv.IsActive = 'Y' AND pl.IsActive = 'Y' " + "AND pl.M_PriceList_ID = ? "
						+ "AND plv.ValidFrom <= ? " + "ORDER BY plv.ValidFrom DESC FETCH FIRST 1 ROWS ONLY";

				BigDecimal result = DB.getSQLValueBD(null, priceStdSql, orderLine.getM_Product_ID(), priceListId,
						dateOrdered);
				if (result != null) {
					priceStd = result;
				}
			}
			orderLine.set_CustomColumn("PriceStd", priceStd);

			orderLine.setDatePromised(order.getDatePromised());
			orderLine.saveEx();

			for (MRequisitionLine reqLine : productLines) {
				reqLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
				reqLine.saveEx();
			}
			processedLines++;
		}

		return new CreatedOrderResult(order.getC_Order_ID(), order.getDocumentNo(), order.getGrandTotal());
	}

	/**
	 * 根据单条申购明细的 IsSubcontracting 标志，解析出对应表（C_Order/PP_Order）的
	 * DocumentNo 集合（已去重，保留查询顺序）。
	 * IsSubcontracting=Y -> 查 PP_Order/C_Order，用 SubcontractLine_IDs
	 * IsSubcontracting=N -> 查 PP_Order，用 PP_Order_IDs
	 */
	public static Set<String> resolveDocumentNos(MRequisitionLine reqLine, String trxName) {
		boolean isSubcontracting = reqLine.get_ValueAsBoolean("IsSubcontracting");

		if (isSubcontracting) {
			// 委外：改用 SubcontractLine_IDs（委外采购订单行 C_OrderLine_ID 集合），
			// 优先取对应生产工单 PP_Order.DocumentNo，找不到则兜底取委外订单 C_Order.DocumentNo
			String idsStr = (String) reqLine.get_Value(COLUMN_SUBCONTRACTLINE_IDS);
			Set<Integer> idSet = parseIdList(idsStr);
			if (idSet.isEmpty())
				return Collections.emptySet();
			return resolveSubcontractDocumentNos(idSet, trxName);
		} else {
			String idsStr = (String) reqLine.get_Value("PP_Order_IDs");
			Set<Integer> idSet = parseIdList(idsStr);
			if (idSet.isEmpty())
				return Collections.emptySet();
			return queryDocumentNos("PP_Order", idSet, trxName);
		}
	}



	/**
	 * 解析逗号分隔的 ID 字符串为去重、保序的 Integer 集合。  
	 * 公共工具方法，供本类及 Callout 等外部调用方复用。  
	 */
	public static Set<Integer> parseIdList(String idsStr) {
		Set<Integer> idSet = new LinkedHashSet<>();
		if (idsStr == null || idsStr.trim().isEmpty())
			return idSet;
		for (String idStr : idsStr.split(",")) {
			idStr = idStr.trim();
			if (idStr.isEmpty())
				continue;
			try {
				int id = Integer.parseInt(idStr);
				if (id > 0)
					idSet.add(id);
			} catch (NumberFormatException e) {
				// 忽略非法数据  
			}
		}
		return idSet;
	}

	/**
	 * 按表名（C_Order 或 PP_Order）批量查询 DocumentNo，仅取 IsActive='Y' 的记录。
	 * tableName 仅限内部白名单调用（"C_Order" / "PP_Order"），不接受外部拼接字符串，避免注入风险。
	 * 公共工具方法，供本类及 Callout 等外部调用方复用。
	 */
	public static Set<String> queryDocumentNos(String tableName, Set<Integer> idSet, String trxName) {
		Set<String> docNoSet = new LinkedHashSet<>();
		if (idSet == null || idSet.isEmpty())
			return docNoSet;

		StringBuilder sql = new StringBuilder("SELECT DocumentNo FROM ")
				.append(tableName)
				.append(" WHERE IsActive='Y' AND ")
				.append(tableName).append("_ID IN (");
		Object[] params = new Object[idSet.size()];
		int i = 0;
		for (Integer id : idSet) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
			params[i] = id;
			i++;
		}
		sql.append(")");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), trxName);
			for (int p = 0; p < params.length; p++)
				pstmt.setObject(p + 1, params[p]);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String docNo = rs.getString(1);
				if (docNo != null && !docNo.trim().isEmpty())
					docNoSet.add(docNo.trim());
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("查询" + tableName + "单号失败", e);
		} finally {
			DB.close(rs, pstmt);
		}
		return docNoSet;
	}

	/**
	 * 根据委外采购订单行 ID 集合（C_OrderLine_ID），解析出对应的 DocumentNo 集合：
	 * 1. 先查每个 C_OrderLine 对应的 Ref_OrderLine_ID（反向/对等的销售订单行）；
	 * 2. 若 Ref_OrderLine_ID > 0，且其下存在已完成（DocStatus='CO'）的 PP_Order，
	 *    取 PP_Order.DocumentNo（优先）；
	 * 3. 否则（无 Ref_OrderLine_ID 或无已完成工单），回退取该 C_OrderLine 所属
	 *    C_Order（委外采购订单表头）的 DocumentNo 作为兜底。
	 */
	public static Set<String> resolveSubcontractDocumentNos(Set<Integer> orderLineIds, String trxName) {
		Set<String> docNoSet = new LinkedHashSet<>();
		if (orderLineIds == null || orderLineIds.isEmpty())
			return docNoSet;

		// 1. 批量查 C_OrderLine -> Ref_OrderLine_ID / C_Order_ID
		Map<Integer, Integer> refOrderLineMap = new HashMap<>();   // C_OrderLine_ID -> Ref_OrderLine_ID
		Map<Integer, Integer> orderIdMap = new HashMap<>();        // C_OrderLine_ID -> C_Order_ID
		{
			StringBuilder sql = new StringBuilder(
					"SELECT C_OrderLine_ID, Ref_OrderLine_ID, C_Order_ID FROM C_OrderLine WHERE C_OrderLine_ID IN (");
			appendPlaceholders(sql, orderLineIds.size());
			sql.append(")");
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql.toString(), trxName);
				bindParams(pstmt, orderLineIds);
				rs = pstmt.executeQuery();
				while (rs.next()) {
					int lineId = rs.getInt(1);
					int refLineId = rs.getInt(2);
					int orderId = rs.getInt(3);
					if (refLineId > 0)
						refOrderLineMap.put(lineId, refLineId);
					orderIdMap.put(lineId, orderId);
				}
			} catch (SQLException e) {
				throw new IllegalArgumentException("查询 C_OrderLine 关联信息失败", e);
			} finally {
				DB.close(rs, pstmt);
			}
		}

		// 2. 用 Ref_OrderLine_ID 批量查已完成的 PP_Order（取 DocumentNo）
		Map<Integer, String> ppOrderDocNoMap = new HashMap<>(); // Ref_OrderLine_ID -> DocumentNo
		Set<Integer> refLineIds = new HashSet<>(refOrderLineMap.values());
		if (!refLineIds.isEmpty()) {
			StringBuilder sql = new StringBuilder(
					"SELECT C_OrderLine_ID, DocumentNo FROM PP_Order WHERE IsActive='Y' AND C_OrderLine_ID IN (");
			appendPlaceholders(sql, refLineIds.size());
			sql.append(")");
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql.toString(), trxName);
				bindParams(pstmt, refLineIds);
				rs = pstmt.executeQuery();
				while (rs.next()) {
					int refLineId = rs.getInt(1);
					String docNo = rs.getString(2);
					if (docNo != null && !docNo.trim().isEmpty())
						ppOrderDocNoMap.put(refLineId, docNo.trim());
				}
			} catch (SQLException e) {
				throw new IllegalArgumentException("查询 PP_Order 单号失败", e);
			} finally {
				DB.close(rs, pstmt);
			}
		}

		// 3. 逐个原始 C_OrderLine_ID 决定取值：优先工单号，否则兜底委外订单号
		Set<Integer> fallbackOrderIds = new LinkedHashSet<>();
		Map<Integer, Integer> fallbackLineToOrder = new HashMap<>(); // 记录哪些行需要兜底
		for (Integer lineId : orderLineIds) {
			Integer refLineId = refOrderLineMap.get(lineId);
			String ppDocNo = refLineId != null ? ppOrderDocNoMap.get(refLineId) : null;
			if (ppDocNo != null) {
				docNoSet.add(ppDocNo);
			} else {
				Integer orderId = orderIdMap.get(lineId);
				if (orderId != null && orderId > 0) {
					fallbackOrderIds.add(orderId);
				}
			}
		}

		// 4. 批量查兜底的委外采购订单表头 DocumentNo
		if (!fallbackOrderIds.isEmpty()) {
			docNoSet.addAll(queryDocumentNos("C_Order", fallbackOrderIds, trxName));
		}

		return docNoSet;
	}

	private static void appendPlaceholders(StringBuilder sql, int count) {
		for (int i = 0; i < count; i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
	}

	private static void bindParams(PreparedStatement pstmt, Set<Integer> ids) throws SQLException {
		int i = 1;
		for (Integer id : ids) {
			pstmt.setInt(i++, id);
		}
	}

	public static class CreatedOrderResult {
		public final int orderId;
		public final String documentNo;
		public final java.math.BigDecimal grandTotal;

		public CreatedOrderResult(int orderId, String documentNo, java.math.BigDecimal grandTotal) {
			this.orderId = orderId;
			this.documentNo = documentNo;
			this.grandTotal = grandTotal;
		}
	}
}
