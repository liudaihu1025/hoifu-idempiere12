package com.hoifu.callout;

import java.util.Objects;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.model.Query;

/**
 * 订单明细表-根据输入的客户料号自动带出关联的物料
 */
@Callout(tableName = "C_OrderLine", columnName = { "ProductNoCust" })
public class OrderLineCustProductCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		// 获取客户料号
		String productNoCust = (String) mTab.getValue("ProductNoCust");

		if (productNoCust != null && !productNoCust.isEmpty()) {
			// 使用客户料号查询产品
			String whereClause = "productnocust=?";
			MProduct productByCust = new Query(ctx, MProduct.Table_Name, whereClause, null).setParameters(productNoCust)
					.setOnlyActiveRecords(true).first();

			if (Objects.nonNull(productByCust)) {
				Integer productId = productByCust.get_ValueAsInt("M_Product_ID");
				// 将值设置到订单明细的对应字段
				if (productId != null && productId.intValue() != 0) {
					mTab.setValue("M_Product_ID", productId);
					return "";
				}
			}
			mTab.setValue("M_Product_ID", null);
		}

		return "";
	}

}