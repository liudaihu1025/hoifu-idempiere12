package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class Callout_PP_Order_BOMLine extends CalloutBOM implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("QtyEntered"))
			return qtyLine(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("QtyRequiered"))
			return qtyLine(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("M_Product_ID")) // ← 新增
			return applyRoutingNode(ctx, WindowNo, mTab, mField, value);
		// ★ 新增
		if (mField.getColumnName().equals("PP_Order_Node_ID"))
			return nodeChanged(mTab, value);
		return null;
		 
	}

	private String applyRoutingNode(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		if (value == null)
			return "";
		int productId = (Integer) value;
		if (productId <= 0)
			return "";

		// 1. 获取物料的物料组
		int categoryId = DB.getSQLValue(null, "SELECT M_Product_Category_ID FROM M_Product WHERE M_Product_ID = ?",
				productId);
		if (categoryId <= 0)
			return "";

		// 2. 查物料组工序，取 NodePriority 最小的 AD_Routing_Node_ID
		int routingNodeId = DB.getSQLValue(null,
				"SELECT AD_Routing_Node_ID FROM PP_ProductCategory_RoutingNode "
						+ "WHERE M_Product_Category_ID = ? AND IsActive='Y' "
						+ "ORDER BY NodePriority ASC FETCH FIRST 1 ROWS ONLY",
				categoryId);
		if (routingNodeId <= 0)
			return "";

		// 3. 找到本工单对应的 PP_Order_Node_ID
		int ppOrderId = Env.getContextAsInt(ctx, WindowNo, "PP_Order_ID");
		int ppOrderNodeId = DB.getSQLValue(null,
				"SELECT PP_Order_Node_ID FROM PP_Order_Node " + "WHERE PP_Order_ID = ? AND AD_Routing_Node_ID = ?",
				ppOrderId, routingNodeId);
		if (ppOrderNodeId <= 0)
			return "";

		// 4. 设置到当前行
		mTab.setValue("PP_Order_Node_ID", ppOrderNodeId);
		return "";
	}

	// ★ 新增方法
	private String nodeChanged(GridTab mTab, Object value) {
		Integer nodeId = (Integer) value;
		// 先清空，防止切换后无数据时残留旧值
		mTab.setValue("QtyPaperTotalScrap", null);
		mTab.setValue("RatePaperTotalScrap", null);

		if (nodeId == null || nodeId <= 0)
			return "";

		String sql = "SELECT QtyPaperTotalScrap, RatePaperTotalScrap "
				+ "FROM PP_Order_Node WHERE PP_Order_Node_ID=? AND IsActive='Y'";
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = org.compiere.util.DB.prepareStatement(sql, null);
			pstmt.setInt(1, nodeId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				mTab.setValue("QtyPaperTotalScrap", rs.getBigDecimal(1));
				mTab.setValue("RatePaperTotalScrap", rs.getBigDecimal(2));
			}
		} catch (Exception e) {
			// ignore
		} finally {
			org.compiere.util.DB.close(rs, pstmt);
		}
		return "";
	}
}
