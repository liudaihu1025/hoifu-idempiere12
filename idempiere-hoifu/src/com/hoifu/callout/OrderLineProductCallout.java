package com.hoifu.callout;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 订单明细表-根据选择的物料自动带出关联的客户料号、申购明细单价、标准价格，并计算明细净额。
 * 触发字段：M_Product_ID。
 * - PriceEntered：取自最近一张申购明细的单价
 * - PriceActual：取自申购明细的 PriceActual
 * - PriceStd：取自价格表（M_ProductPrice.PriceStd），无价格表记录时为 0
 * - LineNetAmt = QtyEntered × PriceEntered
 */
@Callout(tableName = "C_OrderLine", columnName = { "M_Product_ID", "QtyEntered" })
public class OrderLineProductCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		String columnName = mField.getColumnName();

		if ("QtyEntered".equals(columnName)) {
			return updateQtyDeliverAllTotal(mTab, value);
		}

		Integer pId = (Integer) mTab.getValue("M_Product_ID");

		if (pId == null || pId.intValue() == 0) {
			return "";
		}

		// 通过ID获取MProduct对象
		MProduct product = MProduct.get(ctx, pId.intValue());

		if (Objects.nonNull(product)) {
			// 获取客户料号
			String customerProductNo = product.get_ValueAsString("ProductNoCust");
			mTab.setValue("ProductNoCust", customerProductNo);

		}

		int warehouseId = Env.getContextAsInt(ctx, WindowNo, "M_Warehouse_ID");
		if (warehouseId <= 0) {
			// 兜底：如果订单头上下文没有仓库，尝试从行上取（如果 C_OrderLine 有 M_Warehouse_ID 字段）
			Integer whFromLine = (Integer) mTab.getValue("M_Warehouse_ID");
			if (whFromLine != null) {
				warehouseId = whFromLine.intValue();
			}
		}

		if (warehouseId > 0) {
			Integer asiId = (Integer) mTab.getValue("M_AttributeSetInstance_ID");
			int M_AttributeSetInstance_ID = (asiId == null) ? 0 : asiId.intValue();

			// getQtyOnHand: 汇总仓库下所有货位的 QtyOnHand，不扣减 QtyReserved
			BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHand(pId.intValue(), warehouseId, M_AttributeSetInstance_ID,
					null);
			if (qtyOnHand == null) {
				qtyOnHand = Env.ZERO;
			}
			mTab.setValue("QtyOnHandInfo", qtyOnHand);
		} else {
			mTab.setValue("QtyOnHandInfo", Env.ZERO);
		}

		// 查询申购明细里该物料的单价，回显到 PriceEntered
		String sql = "SELECT PriceActual FROM M_RequisitionLine "
				+ "WHERE M_Product_ID = ? AND PriceActual IS NOT NULL "
				+ "ORDER BY M_RequisitionLine_ID DESC FETCH FIRST 1 ROWS ONLY";
		BigDecimal PriceActual = DB.getSQLValueBD(null, sql, pId.intValue());
		if (PriceActual == null) {
			PriceActual = Env.ZERO;
		}
		mTab.setValue("PriceEntered", PriceActual);

		// 计算明细净额 = 数量 × 单价
		BigDecimal qtyEntered = (BigDecimal) mTab.getValue("QtyEntered");
		if (qtyEntered == null) {
			qtyEntered = Env.ZERO;
		}
		BigDecimal lineNetAmt = qtyEntered.multiply(PriceActual);
		mTab.setValue("LineNetAmt", lineNetAmt);
		mTab.setValue("PriceActual", PriceActual);

		// 查询价格表中的标准价格，赋值给 PriceStd
		// 从上下文获取订单头的价格表ID和业务日期，确保取到正确的价格表版本
		int priceListId = Env.getContextAsInt(ctx, mTab.getWindowNo(), "M_PriceList_ID");
		Timestamp dateOrdered = Env.getContextAsDate(ctx, mTab.getWindowNo(), "DateOrdered");

		if (priceListId > 0) {
			String priceStdSql = "SELECT pp.PriceStd FROM M_ProductPrice pp "
					+ "JOIN M_PriceList_Version plv ON plv.M_PriceList_Version_ID = pp.M_PriceList_Version_ID "
					+ "JOIN M_PriceList pl ON pl.M_PriceList_ID = plv.M_PriceList_ID "
					+ "WHERE pp.M_Product_ID = ? AND pp.IsActive = 'Y' "
					+ "AND plv.IsActive = 'Y' AND pl.IsActive = 'Y' "
					+ "AND pl.M_PriceList_ID = ? "
					+ "AND plv.ValidFrom <= ? "
					+ "ORDER BY plv.ValidFrom DESC FETCH FIRST 1 ROWS ONLY";
			BigDecimal priceStd = DB.getSQLValueBD(null, priceStdSql,
					pId, priceListId, dateOrdered);
			if (priceStd == null) {
				priceStd = Env.ZERO;
			}
			mTab.setValue("PriceStd", priceStd);
		} else {
			mTab.setValue("PriceStd", Env.ZERO);
		}

		return "";
	}

	/**
	 * QtyEntered 变化时，重新计算/更新 QtyDeliverAllTotal
	 */
	private String updateQtyDeliverAllTotal(GridTab mTab, Object value) {
		BigDecimal qtyEntered = value instanceof BigDecimal ? (BigDecimal) value : Env.ZERO;
		if (qtyEntered == null) {
			qtyEntered = Env.ZERO;
		}

		BigDecimal qtyDeliverAllTotal = qtyEntered;

		mTab.setValue("QtyDeliverAllTotal", qtyDeliverAllTotal);
		return "";
	}

}