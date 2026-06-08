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
 * 订单明细表-根据选择的物料自动带出关联的客户料号
 */
@Callout(tableName = "C_OrderLine", columnName = { "M_Product_ID" })
public class OrderLineProductCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		// 获取各个字段的值
		Integer pId = (Integer) mTab.getValue("M_Product_ID");


		if (pId != null && pId.intValue() != 0) {
			// 通过ID获取MProduct对象
			MProduct product = MProduct.get(ctx, pId.intValue());

			if (Objects.nonNull(product)) {
				// 获取客户料号
				String customerProductNo = product.get_ValueAsString("ProductNoCust");

				// 将值设置到订单明细的对应字段
				mTab.setValue("ProductNoCust", customerProductNo);
			}
		}


		return "";
	}

}