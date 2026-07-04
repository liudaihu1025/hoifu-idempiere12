package org.libero.callouts;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;

/**
 * 纸张工序放损 Callout 逻辑层
 */
public class CalloutPaperScrap extends CalloutEngine {

	/**
	 * 触发字段：ScrapType（工艺难度） 查询 C_PaperScrapStd，填充 ScrapFactor / QtyPaperNodeScrap /
	 * RatePaperNodeScrap
	 */
	public String scrapType(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		String scrapType = (String) value;
		if (scrapType == null || scrapType.isEmpty())
			return "";

		Integer nodeId = (Integer) mTab.getValue("AD_Routing_Node_ID");
		if (nodeId == null || nodeId <= 0)
			return "";
  
		String sql = "SELECT DifficultyFactor, StdBaseQty, StdScrapRate " + "FROM C_PaperScrapStd "
				+ "WHERE AD_Routing_Node_ID=? AND ProcessDifficulty=? AND IsActive='Y' " + "ORDER BY Created DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, nodeId);
			pstmt.setString(2, scrapType);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				mTab.setValue("ScrapFactor", rs.getBigDecimal("DifficultyFactor"));
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
			rs = null;
			pstmt = null;
        }  

		recalculate(ctx, mTab);
		return "";
	}

	/**
	 * 触发字段：QtyColor（色数） 重新计算 QtyPaperScrap / QtyPaperTotalScrap /
	 * RatePaperTotalScrap
	 */
	public String qtyColor(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		recalculate(ctx, mTab);
		return "";
    }  
  
	/**
	 * 核心计算： QtyPaperScrap = (StdBaseQty + StdScrapRate‰ * QtyOrdered/QtyBatch) *
	 * QtyColor * DifficultyFactor QtyPaperTotalScrap = QtyPaperScrap +
	 * 上道工序.QtyPaperTotalScrap RatePaperTotalScrap% = QtyPaperTotalScrap /
	 * (QtyPaperTotalScrap + QtyOrdered/QtyBatch) * 100
	 */
	private void recalculate(Properties ctx, GridTab mTab) {
		BigDecimal stdBaseQty = (BigDecimal) mTab.getValue("QtyPaperNodeScrap");
		BigDecimal stdScrapRate = (BigDecimal) mTab.getValue("RatePaperNodeScrap");
		BigDecimal diffFactor = (BigDecimal) mTab.getValue("ScrapFactor");
		BigDecimal qtyColor = (BigDecimal) mTab.getValue("QtyColor");

		if (stdBaseQty == null || stdScrapRate == null || diffFactor == null || qtyColor == null) {
			return;
		}

		Integer ppOrderId = (Integer) mTab.getValue("PP_Order_ID");
		if (ppOrderId == null || ppOrderId <= 0)
			return;

		MPPOrder order = new MPPOrder(ctx, ppOrderId, null);
		BigDecimal qtyOrdered = order.getQtyOrdered();
		BigDecimal qtyBatch = order.getQtyBatchs();
		if (qtyBatch == null || qtyBatch.signum() == 0)
			qtyBatch = BigDecimal.ONE;

		// 工单数量 / 联数
		BigDecimal qtyPerBatch = qtyOrdered.divide(qtyBatch, 6, RoundingMode.HALF_UP);

		// StdScrapRate 单位是 ‰，除以 1000 转换为实际比率
		BigDecimal scrapRateActual = stdScrapRate.divide(new BigDecimal("1000"), 10, RoundingMode.HALF_UP);

		// 纸张放损数（取整）
		BigDecimal qtyPaperScrap = stdBaseQty.add(scrapRateActual.multiply(qtyPerBatch)).multiply(qtyColor)
				.multiply(diffFactor).setScale(0, RoundingMode.HALF_UP);
		mTab.setValue("QtyPaperScrap", qtyPaperScrap);

		// 上道工序累计放损数
		Integer currentNodeId = (Integer) mTab.getValue("PP_Order_Node_ID");
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
	}
  
	/**
	 * 通过 PP_Order_Node_Next 找到当前节点的前驱节点，取其 QtyPaperTotalScrap
	 */
	private BigDecimal getPrevNodeTotalScrap(Integer currentNodeId, Integer ppOrderId) {
		if (currentNodeId == null || currentNodeId <= 0)
			return BigDecimal.ZERO;
  
		String sql = "SELECT n.QtyPaperTotalScrap " + "FROM PP_Order_Node n "
				+ "JOIN PP_Order_Node_Next t ON t.PP_Order_Node_ID = n.PP_Order_Node_ID "
				+ "WHERE t.PP_Order_Next_ID=? AND n.PP_Order_ID=? AND n.IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, currentNodeId);
			pstmt.setInt(2, ppOrderId);
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
}