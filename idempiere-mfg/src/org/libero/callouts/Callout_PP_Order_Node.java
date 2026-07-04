package org.libero.callouts;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.IColumnCallout;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOMLine;
  
public class Callout_PP_Order_Node extends CalloutEngine implements IColumnCallout {  

    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  
    	if (mField.getColumnName().equals("AD_Routing_Node_ID")) {  
    	    updateQtyRequired(ctx, WindowNo, mTab, mField, value);  
    	    // ★ 新增：如果当前有 ScrapType，用新的 AD_Routing_Node_ID 重新查询放损数据  
    	    Object currentScrapType = mTab.getValue("ScrapType");  
    	    if (currentScrapType != null && !currentScrapType.toString().trim().isEmpty()) {  
    	        return scrapType(ctx, WindowNo, mTab, mField, currentScrapType);  
			}
			return "";
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

	// ==================== 原有方法（不变）====================

    private String updateQtyRequired(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value) {  

        String whereClause = "PP_Order_ID=? AND KeyMat='Y'";  
        MPPOrderBOMLine keyMatLine = new Query(ctx, MPPOrderBOMLine.Table_Name, whereClause, null)  
                .setParameters(mTab.getValue("PP_Order_ID"))  
                .firstOnly();  
  
        if (keyMatLine == null) {  
            throw new AdempiereException("工单BOM未维护主物料");  
        }  

        BigDecimal keyMatQtyRequiered = keyMatLine.getQtyRequiered();  
        BigDecimal calculatedQty = keyMatQtyRequiered;  

        Integer AD_Routing_Node_ID = (Integer) value;  
  
        if (AD_Routing_Node_ID != null && AD_Routing_Node_ID > 0) {  
  
            String name = DB.getSQLValueString(null,  
                    "SELECT Name FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'",  
                    AD_Routing_Node_ID);  
            if (name != null) {  
                mTab.setValue("Name", name);  
            }  

			String routingWhereClause = "AD_Routing_Node_ID=? AND IsBatchCalculation='Y' and IsActive='Y'";
            MTable routingTable = MTable.get(ctx, "AD_Routing_Node");  
            PO routingNode = new Query(ctx, routingTable, routingWhereClause, null)  
                    .setParameters(AD_Routing_Node_ID)  
                    .firstOnly();  

            if (routingNode != null) {  
                Integer PP_Order_Workflow_ID = (Integer) mTab.getValue("PP_Order_Workflow_ID");  
                if (PP_Order_Workflow_ID != null && PP_Order_Workflow_ID > 0) {  
                    String workflowWhereClause = "PP_Order_Workflow_ID=?";  
                    PO workflow = new Query(ctx, "PP_Order_Workflow", workflowWhereClause, null)  
                            .setParameters(PP_Order_Workflow_ID)  
                            .firstOnly();  

                    if (workflow != null) {  
                        BigDecimal qtyBatchSize = (BigDecimal) workflow.get_Value("QtyBatchSize");  
                        if (qtyBatchSize != null && qtyBatchSize.compareTo(BigDecimal.ZERO) > 0) {  
                            calculatedQty = calculatedQty.multiply(qtyBatchSize);  
                        }  
                    }  
                }  
            }  
        }  

        mTab.setValue("QtyRequiered", calculatedQty);  
        return "";  
    }  

	// ==================== ★ 新增方法 ====================

	/**
	 * 触发字段：ScrapType（工艺难度） 查询 C_PaperScrapStd，填充 ScrapFactor / QtyPaperNodeScrap /
	 * RatePaperNodeScrap 然后触发重新计算
	 */
	private String scrapType(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		String scrapType = (String) value;
		if (scrapType == null || scrapType.isEmpty())
			return "";

		Integer nodeId = (Integer) mTab.getValue("AD_Routing_Node_ID");
		if (nodeId == null || nodeId <= 0)
			return "";

		// 先清空计算结果字段，防止切换后无匹配数据时残留旧值
		mTab.setValue("QtyPaperScrap", null);
		mTab.setValue("QtyPaperTotalScrap", null);
		mTab.setValue("RatePaperTotalScrap", null);

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

		if (stdBaseQty == null || stdScrapRate == null || diffFactor == null || qtyColor == null) {
			return;
		}

		Integer ppOrderId = (Integer) mTab.getValue("PP_Order_ID");
		if (ppOrderId == null || ppOrderId <= 0)
			return;

		MPPOrder order = new MPPOrder(ctx, ppOrderId, null);
		BigDecimal qtyBatchSize = order.getQtyBatchSize();
		if (qtyBatchSize == null || qtyBatchSize.signum() == 0)
			qtyBatchSize = BigDecimal.ONE;
		BigDecimal qtyPerBatch = order.getQtyEntered().divide(qtyBatchSize, 6, RoundingMode.HALF_UP);

		// StdScrapRate 单位是 ‰，除以 1000 转为实际比率
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