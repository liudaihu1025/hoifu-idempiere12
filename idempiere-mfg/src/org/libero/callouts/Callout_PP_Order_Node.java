package org.libero.callouts;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;
  
public class Callout_PP_Order_Node extends CalloutEngine implements IColumnCallout {  

    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  
		if (mField.getColumnName().equals("AD_Routing_Node_ID")) {
			updateQtyRequired(ctx, WindowNo, mTab, mField, value);
			return scrapType(ctx, WindowNo, mTab, mField, mTab.getValue("ScrapType"));
		}

		// ★ 新增 start
		if (mField.getColumnName().equals("ScrapType")) {
			return scrapType(ctx, WindowNo, mTab, mField, value);
		}
		if (mField.getColumnName().equals("QtyColor")) {
			return qtyColor(ctx, WindowNo, mTab, mField, value);
		}
		// ★ 新增 end
        return null;  
    }

	private String updateQtyRequired(Properties ctx, int WindowNo, GridTab mTab,
									 GridField mField, Object value) {

		Integer AD_Routing_Node_ID = (Integer) value;
		if (AD_Routing_Node_ID == null || AD_Routing_Node_ID <= 0)
			return "";

		// 1. 带出工序节点名称
		String name = DB.getSQLValueString(null,
				"SELECT Name FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'",
				AD_Routing_Node_ID);
		if (name != null)
			mTab.setValue("Name", name);

		// 新增：带出 NodeType，驱动 QtyColor 的只读逻辑实时生效
		String nodeType = DB.getSQLValueString(null,
				"SELECT NodeType FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'", AD_Routing_Node_ID);
		mTab.setValue("NodeType", nodeType != null ? nodeType : "");

		// 2. 取工单基础数据
		Integer ppOrderId = (Integer) mTab.getValue("PP_Order_ID");
		if (ppOrderId == null || ppOrderId <= 0)
			return "";

		MPPOrder order = new MPPOrder(ctx, ppOrderId, null);
		BigDecimal qtyEntered = order.getQtyEntered();
		BigDecimal qtyBatchSize = order.getQtyBatchSize();
		if (qtyEntered == null || qtyBatchSize == null || qtyBatchSize.signum() <= 0)
			return "";

		// 3. 取当前节点的累计放损数（数据库旧值）
		Integer nodeId = (Integer) mTab.getValue("PP_Order_Node_ID");
		BigDecimal nodeTotalScrap = BigDecimal.ZERO;
		if (nodeId != null && nodeId > 0) {
			BigDecimal dbVal = DB.getSQLValueBD(null,
					"SELECT COALESCE(QtyPaperTotalScrap, 0) FROM PP_Order_Node WHERE PP_Order_Node_ID=?", nodeId);
			if (dbVal != null)
				nodeTotalScrap = dbVal;
		}

		// 4. 计算并写入 QtyRequiered
		applyQtyRequiered(mTab, qtyEntered, qtyBatchSize, nodeTotalScrap, ppOrderId);
		return "";
	}

	private void applyQtyRequiered(GridTab mTab, BigDecimal qtyEntered, BigDecimal qtyBatchSize, BigDecimal currentNodeTotalScrap, Integer ppOrderId) {
		// 取主物料 BOM 行的总累计放损数
		BigDecimal totalScrap = DB.getSQLValueBD(null,
				"SELECT COALESCE(QtyPaperTotalScrap, 0) FROM PP_Order_BOMLine WHERE PP_Order_ID=? AND Keymat='Y' AND IsActive='Y'",
				ppOrderId);
		if (totalScrap == null) totalScrap = BigDecimal.ZERO;

		// 累计放损-当前工序累计放损
		BigDecimal scrapDiff = totalScrap.subtract(currentNodeTotalScrap != null ? currentNodeTotalScrap : BigDecimal.ZERO);

		// 判断大张/小张
		Integer routingNodeId = (Integer) mTab.getValue("AD_Routing_Node_ID");
		String isBatchCalc = "N";
		if (routingNodeId != null && routingNodeId > 0) {
			isBatchCalc = DB.getSQLValueString(null,
					"SELECT COALESCE(IsBatchCalculation, 'N') FROM AD_Routing_Node WHERE AD_Routing_Node_ID=?",
					routingNodeId);
		}

		BigDecimal qtyRequiered;
		if ("Y".equals(isBatchCalc)) {
			// 小张：QtyEntered + scrapDiff × QtyBatchSize
			qtyRequiered = qtyEntered.add(scrapDiff.multiply(qtyBatchSize))
					.setScale(0, RoundingMode.CEILING);
		} else {
			// 大张：QtyEntered / QtyBatchSize + scrapDiff
			qtyRequiered = qtyEntered.divide(qtyBatchSize, 8, RoundingMode.HALF_UP)
					.add(scrapDiff).setScale(0, RoundingMode.CEILING);
		}
		mTab.setValue("QtyRequiered", qtyRequiered);
	}

	// ==================== ★ 新增方法 ====================

	/**
	 * 触发字段：ScrapType（工艺难度） 查询 C_PaperScrapStd，填充 ScrapFactor / QtyPaperNodeScrap /
	 * RatePaperNodeScrap 然后触发重新计算
	 */
	private String scrapType(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		String scrapType = (String) value;
		Integer nodeId = (Integer) mTab.getValue("AD_Routing_Node_ID");
		if (nodeId == null || nodeId <= 0)
			return "";

		// 先清空计算结果字段，防止切换后无匹配数据时残留旧值
		mTab.setValue("QtyPaperScrap", null);
		mTab.setValue("QtyPaperTotalScrap", null);
		mTab.setValue("RatePaperTotalScrap", null);

		String sql = "SELECT cps.DifficultyFactor, cps.StdBaseQty, cps.StdScrapRate "
				+ "FROM C_PaperScrapStd cps "
				+ "JOIN AD_Routing_Node arn ON arn.operationclass_ID = cps.operationclass_ID "
				+ "WHERE arn.AD_Routing_Node_ID=? AND cps.ProcessDifficulty=? AND cps.IsActive='Y' ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, nodeId);
			pstmt.setString(2, scrapType);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal difficultyFactor = rs.getBigDecimal("DifficultyFactor");
				mTab.setValue("ScrapFactor", difficultyFactor != null ? difficultyFactor.toPlainString() : null);
				mTab.setValue("QtyPaperNodeScrap", rs.getBigDecimal("StdBaseQty"));
				mTab.setValue("RatePaperNodeScrap", rs.getBigDecimal("StdScrapRate"));
			} else {
				mTab.setValue("ScrapFactor", null);
				mTab.setValue("QtyPaperNodeScrap", null);
				mTab.setValue("RatePaperNodeScrap", null);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
			return e.getLocalizedMessage();
		} finally {
			DB.close(rs, pstmt);
		}

		recalculatePaperScrap(ctx, mTab);
		return "";
	}

	/**
	 * 触发字段：QtyColor（色数） 重新计算纸张放损相关字段
	 */
	private String qtyColor(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		recalculatePaperScrap(ctx, mTab);
		return "";
	}

	/**
	 * 核心计算： QtyPaperScrap = (StdBaseQty + StdScrapRate‰ * 工单数量/联数) * QtyColor *
	 * DifficultyFactor QtyPaperTotalScrap = QtyPaperScrap + 上道工序.QtyPaperTotalScrap
	 * RatePaperTotalScrap% = QtyPaperTotalScrap / (QtyPaperTotalScrap + 工单数量/联数) *
	 * 100
	 * 
	 * 工单数量/联数 = MPPOrder.getQtyBatchs()（即 QtyOrdered / QtyBatchSize）
	 */
	private void recalculatePaperScrap(Properties ctx, GridTab mTab) {
		BigDecimal stdBaseQty = getBDValue(mTab, "QtyPaperNodeScrap");
		BigDecimal stdScrapRate = getBDValue(mTab, "RatePaperNodeScrap");
		BigDecimal diffFactor = getBDValue(mTab, "ScrapFactor"); // ScrapFactor 是 String，getBDValue 会自动转换
		BigDecimal qtyColor = getBDValue(mTab, "QtyColor");

		if (qtyColor == null || qtyColor.signum() == 0)
			qtyColor = BigDecimal.ONE;

		if (stdBaseQty == null || stdScrapRate == null || diffFactor == null) {
			return;
		}

		Integer ppOrderId = (Integer) mTab.getValue("PP_Order_ID");
		if (ppOrderId == null || ppOrderId <= 0)
			return;

		MPPOrder order = new MPPOrder(ctx, ppOrderId, null);
		BigDecimal qtyBatchSize = order.getQtyBatchSize();
		if (qtyBatchSize == null || qtyBatchSize.signum() == 0)
			qtyBatchSize = BigDecimal.ONE;
		BigDecimal qtyEntered = order.getQtyEntered();
		BigDecimal qtyPerBatch = qtyEntered .divide(qtyBatchSize, 6, RoundingMode.HALF_UP);

		// StdScrapRate 单位是 ‰，除以 1000 转为实际比率
		BigDecimal scrapRateActual = stdScrapRate.divide(new BigDecimal("1000"), 10, RoundingMode.HALF_UP);

		// 纸张放损数（取整）
		BigDecimal qtyPaperScrap = stdBaseQty.add(scrapRateActual.multiply(qtyPerBatch)).multiply(qtyColor)
				.multiply(diffFactor).setScale(0, RoundingMode.CEILING);
		mTab.setValue("QtyPaperScrap", qtyPaperScrap);

		// 提前取 currentNodeId，并查 DB 旧值（在 mTab.setValue 之前）
		Integer currentNodeId = (Integer) mTab.getValue("PP_Order_Node_ID");
		BigDecimal oldCurrentNodeTotalScrap = DB.getSQLValueBD(null,
				"SELECT COALESCE(QtyPaperTotalScrap, 0) FROM PP_Order_Node WHERE PP_Order_Node_ID=?", currentNodeId);
		if (oldCurrentNodeTotalScrap == null)
			oldCurrentNodeTotalScrap = BigDecimal.ZERO;

		// 上道工序累计放损数
		BigDecimal prevTotal = getPrevNodeTotalScrap(currentNodeId, ppOrderId);

		// 累计纸张放损数
		BigDecimal qtyPaperTotalScrap = qtyPaperScrap.add(prevTotal);
		mTab.setValue("QtyPaperTotalScrap", qtyPaperTotalScrap);

		// 累计纸张放损率%
		BigDecimal denominator = qtyPaperTotalScrap.add(qtyPerBatch);
		if (denominator.signum() != 0) {
			BigDecimal rate = qtyPaperTotalScrap.divide(denominator, 8, RoundingMode.HALF_UP)
					.multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP);
			mTab.setValue("RatePaperTotalScrap", rate);
		}
		// 新增：同步更新 QtyRequiered
		// 传 DB 旧值，不传新计算的 qtyPaperTotalScrap
		applyQtyRequiered(mTab, qtyEntered, qtyBatchSize, oldCurrentNodeTotalScrap, ppOrderId);
	}
	/**
	 * 通过 PP_Order_Node_Next 找到当前节点的前驱节点，取其 QtyPaperTotalScrap
	 */
	/**
	 * 按 Value 字段排序，找到当前节点的上道工序（Value 比当前小且最接近），取其 QtyPaperTotalScrap
	 */
	private BigDecimal getPrevNodeTotalScrap(Integer currentNodeId, Integer ppOrderId) {
		if (currentNodeId == null || currentNodeId <= 0)
			return BigDecimal.ZERO;

		String currentValue = DB.getSQLValueString(null,
				"SELECT Value FROM PP_Order_Node WHERE PP_Order_Node_ID=? AND IsActive='Y'", currentNodeId);
		if (currentValue == null || currentValue.trim().isEmpty())
			return BigDecimal.ZERO;

		// ★ 加上 AND QtyPaperTotalScrap IS NOT NULL，跳过没有累计值的工序
		String sql = "SELECT QtyPaperTotalScrap FROM PP_Order_Node "
				+ "WHERE PP_Order_ID=? AND Value < ? AND IsActive='Y' AND QtyPaperTotalScrap IS NOT NULL "
				+ "ORDER BY Value DESC " + "FETCH FIRST 1 ROWS ONLY";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, ppOrderId);
			pstmt.setString(2, currentValue);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal val = rs.getBigDecimal(1);
				return val != null ? val : BigDecimal.ZERO;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "getPrevNodeTotalScrap", e);
		} finally {
			DB.close(rs, pstmt);
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal getBDValue(GridTab mTab, String columnName) {
		Object val = mTab.getValue(columnName);
		if (val == null)
			return null;
		if (val instanceof BigDecimal)
			return (BigDecimal) val;
		if (val instanceof Integer)
			return new BigDecimal((Integer) val);
		if (val instanceof String) {
			String s = ((String) val).trim();
			if (s.isEmpty())
				return null;
			try {
				return new BigDecimal(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}