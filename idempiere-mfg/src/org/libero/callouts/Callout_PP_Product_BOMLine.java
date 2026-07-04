package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;

public class Callout_PP_Product_BOMLine extends CalloutBOM implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("M_Product_ID")) {
			String result = parent(ctx, WindowNo, mTab, mField, value); // 原有逻辑
			applyRoutingNode(ctx, WindowNo, mTab, mField, value); // 新增逻辑
			return result;

		}
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

		// 3. 直接设置到当前行（产品BOM子件直接存 AD_Routing_Node_ID）
		mTab.setValue("AD_Routing_Node_ID", routingNodeId);
		return "";
	}

}
