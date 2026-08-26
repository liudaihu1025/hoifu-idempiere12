package com.hoifu.callout;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 办公领用单明细：
 * 1. 选择物料后自动回显最近一次采购单价（触发字段：M_Product_ID）。
 *    单价取自最近一张已完成/关闭的采购单（C_OrderLine.PriceEntered），无采购记录时为 0。
 *    同时赋值给 PriceStd 和 PriceActual。
 * 2. 当数量变化时，自动计算行金额 LineNetAmt = Qty * PriceActual（触发字段：Qty）。
 */
@Callout(tableName = "M_RequisitionLine", columnName = { "M_Product_ID", "Qty" })
public class RequisitionLineProductCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		String columnName = mField.getColumnName();

		// 选择物料时，自动回显最近一次采购单价，同时赋值给 PriceStd 和 PriceActual
		if ("M_Product_ID".equals(columnName)) {
			Integer pId = (Integer) mTab.getValue("M_Product_ID");

			if (pId == null || pId.intValue() == 0) {
				return "";
			}

			String sql = "SELECT ol.PriceEntered FROM C_OrderLine ol "
					+ "JOIN C_Order o ON o.C_Order_ID = ol.C_Order_ID "
					+ "WHERE ol.M_Product_ID = ? AND o.IsActive='Y' AND ol.IsActive='Y' AND o.IsSOTrx = 'N' AND o.DocStatus IN ('CO','CL') "
					+ "ORDER BY o.DateOrdered DESC, ol.C_OrderLine_ID DESC FETCH FIRST 1 ROWS ONLY";

			BigDecimal price = DB.getSQLValueBD(null, sql, pId.intValue());
			if (price == null) {
				price = Env.ZERO;
			}

			mTab.setValue("PriceStd", price);
			mTab.setValue("PriceActual", price);
		}

		// 数量变化时，自动计算行金额 = Qty * PriceActual
		BigDecimal qty = (BigDecimal) mTab.getValue("Qty");
		BigDecimal priceActual = (BigDecimal) mTab.getValue("PriceActual");

		if (qty == null) {
			qty = Env.ZERO;
		}
		if (priceActual == null) {
			priceActual = Env.ZERO;
		}

		BigDecimal lineNetAmt = qty.multiply(priceActual);
		mTab.setValue("LineNetAmt", lineNetAmt);

		return "";
	}
}
