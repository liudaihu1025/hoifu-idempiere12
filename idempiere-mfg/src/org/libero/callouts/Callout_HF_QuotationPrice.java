package org.libero.callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.PO;
import org.compiere.model.Query;

/**
 * Callout for HF_QuotationPrice (计价单) Handles: product field population, paper
 * material price lookup, and all derived calculations.
 */
public class Callout_HF_QuotationPrice implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		String col = mField.getColumnName();
		switch (col) {
		case "M_Product_ID":
			return onProduct(ctx, mTab, value);

		// 报价材质价 = 纸质价 * 排版系数
		case "PriceStd":
		case "LayoutFactor":
			recalcQuoteMaterialPrice(mTab);
			recalcBoardCost(mTab);
			recalcSummary(mTab);
			return "";

		// 含税单价变化 → 重算所有费率相关费用
		case "PriceWithTax":
			recalcAllRateFees(mTab);
			recalcSummary(mTab);
			return "";

		// 各费率变化 → 重算对应费用 → 重算汇总
		case "SalesRate":
			recalcSalesFee(mTab);
			recalcSummary(mTab);
			return "";
		case "RebateRate":
			recalcRebate(mTab);
			recalcSummary(mTab);
			return "";
		case "SpareRate":
			recalcSpareFee(mTab);
			recalcSummary(mTab);
			return "";
		case "WasteRate":
			recalcWasteFee(mTab);
			recalcSummary(mTab);
			return "";
		case "FreightRate":
			recalcFreightAmt(mTab);
			recalcSummary(mTab);
			return "";
		case "ComprehensiveRate":
			recalcComprehensiveFee(mTab);
			recalcSummary(mTab);
			return "";
		case "taxrate":
			recalcTaxAmt(mTab);
			recalcSummary(mTab);
			return "";
		case "InterestRate":
			recalcInterestAmt(mTab);
			recalcSummary(mTab);
			return "";

		// 手工录入的加工费 → 重算汇总
		case "PaperCuttingFee":
		case "PrintingFee":
		case "SlottingFee":
		case "DieCuttingFee":
		case "GluingFee":
		case "StitchingFee":
		case "PackingFee":
		case "WasteCleaningFee":
		case "PlateFee":
		case "DieFee":
		case "OtherProcessFee":
			recalcSummary(mTab);
			return "";
		}
		return null;
	}

	// =========================================================
	// M_Product_ID 触发：带出产品字段 + 查纸质价
	// =========================================================
	private String onProduct(Properties ctx, GridTab mTab, Object value) {
		if (value == null)
			return "";
		int M_Product_ID = (Integer) value;
		if (M_Product_ID <= 0)
			return "";

		MProduct product = MProduct.get(ctx, M_Product_ID);
		if (product == null)
			return "";

		// 从产品带出尺寸（自定义字段，用 get_Value 读取）
		mTab.setValue("Length", product.get_Value("Length"));
		mTab.setValue("Width", product.get_Value("Width"));
		mTab.setValue("Height", product.get_Value("Height"));

		// 从产品带出盒型、客户纸质、纸板面积、生产面积
		Object boxTypeIdObj = product.get_Value("HX_BoxType_ID");
		mTab.setValue("HX_BoxType_ID", boxTypeIdObj);

		if (boxTypeIdObj instanceof Integer) {
			int boxTypeId = (Integer) boxTypeIdObj;
			if (boxTypeId > 0) {
				PO boxType = new Query(ctx, "HX_BoxType", "HX_BoxType_ID = ?", null).setParameters(boxTypeId).first();
				if (boxType != null) {
					mTab.setValue("BoxTypeName", boxType.get_Value("Name"));
				}
			}
		}

		mTab.setValue("PaperMaterialCustomer", product.get_Value("PaperMaterialCustomer"));
		mTab.setValue("CardArea", product.get_Value("CardArea"));
		mTab.setValue("BoxArea", product.get_Value("BoxArea"));

		// 报价纸质（CartonMaterial）
		Object cartonMaterialObj = product.get_Value("CartonMaterial");
		String cartonMaterial = cartonMaterialObj != null ? cartonMaterialObj.toString() : null;
		mTab.setValue("CartonMaterial", cartonMaterial);

		// 根据报价纸质（CartonMaterial = 纸质产品的 Value）查找纸质产品
		if (cartonMaterial != null && !cartonMaterial.isEmpty()) {
			MProduct paperProduct = new Query(ctx, MProduct.Table_Name, "CartonMaterial = ? AND IsActive = 'Y'", null)
					.setParameters(cartonMaterial).setClient_ID().first();

			if (paperProduct != null) {
				// 纸板厚度来自纸质产品
				mTab.setValue("Thickness", paperProduct.get_Value("Thickness"));

				// 纸质价：取纸质产品最新价格表版本中的标准价
				MProductPrice pp = new Query(ctx, MProductPrice.Table_Name,
						MProductPrice.COLUMNNAME_M_Product_ID + " = ?", null)
						.setParameters(paperProduct.getM_Product_ID())
						.setOrderBy(MProductPrice.COLUMNNAME_M_PriceList_Version_ID + " DESC").first();
				if (pp != null) {
					mTab.setValue("PriceStd", pp.getPriceStd());
				}
			}
		}

		// 级联计算
		recalcQuoteMaterialPrice(mTab);
		recalcBoardCost(mTab);
		recalcSummary(mTab);
		return "";
	}

	// =========================================================
	// 中间计算
	// =========================================================

	/** 报价材质价 = 纸质价 × 排版系数 */
	private void recalcQuoteMaterialPrice(GridTab mTab) {
		BigDecimal priceStd = getBD(mTab, "PriceStd");
		BigDecimal layoutFactor = getBD(mTab, "LayoutFactor");
		mTab.setValue("QuoteMaterialPrice", priceStd.multiply(layoutFactor));
	}

	/** 纸板成本 = 报价材质价 × 纸板面积 */
	private void recalcBoardCost(GridTab mTab) {
		BigDecimal quoteMaterialPrice = getBD(mTab, "QuoteMaterialPrice");
		BigDecimal cardArea = getBD(mTab, "CardArea");
		mTab.setValue("BoardCost", quoteMaterialPrice.multiply(cardArea));
	}

	/** 含税单价变化时，重算所有费率相关费用 */
	private void recalcAllRateFees(GridTab mTab) {
		recalcSalesFee(mTab);
		recalcRebate(mTab);
		recalcSpareFee(mTab);
		recalcFreightAmt(mTab);
		recalcComprehensiveFee(mTab);
		recalcTaxAmt(mTab);
		recalcInterestAmt(mTab);
	}

	// 各费率计算（费率字段存储的是百分比值，如 5 表示 5%，故除以 100）
	// 如果费率字段存储的是小数（如 0.05），请去掉 .divide(BD100, ...) 部分
	private static final BigDecimal BD100 = BigDecimal.valueOf(100);

	private void recalcSalesFee(GridTab mTab) {
		BigDecimal price = getBD(mTab, "PriceWithTax");
		BigDecimal rate = getBD(mTab, "SalesRate");
		System.out.println("recalcSalesFee: PriceWithTax=" + price + ", SalesRate=" + rate);
		mTab.setValue("SalesFee", price.multiply(rate).divide(BD100, 6, RoundingMode.HALF_UP));
	}

	private void recalcRebate(GridTab mTab) {
		mTab.setValue("Rebate",
				getBD(mTab, "PriceWithTax").multiply(getBD(mTab, "RebateRate")).divide(BD100, 6, RoundingMode.HALF_UP));
	}

	private void recalcSpareFee(GridTab mTab) {
		mTab.setValue("SpareFee",
				getBD(mTab, "PriceWithTax").multiply(getBD(mTab, "SpareRate")).divide(BD100, 6, RoundingMode.HALF_UP));
	}

	/** 损耗费 = 纸板成本 × 损耗率 */
	private void recalcWasteFee(GridTab mTab) {
		mTab.setValue("WasteFee",
				getBD(mTab, "BoardCost").multiply(getBD(mTab, "WasteRate")).divide(BD100, 6, RoundingMode.HALF_UP));
	}

	private void recalcFreightAmt(GridTab mTab) {
		mTab.setValue("FreightAmt", getBD(mTab, "PriceWithTax").multiply(getBD(mTab, "FreightRate")).divide(BD100, 6,
				RoundingMode.HALF_UP));
	}

	private void recalcComprehensiveFee(GridTab mTab) {
		mTab.setValue("ComprehensiveFee", getBD(mTab, "PriceWithTax").multiply(getBD(mTab, "ComprehensiveRate"))
				.divide(BD100, 6, RoundingMode.HALF_UP));
	}

	private void recalcTaxAmt(GridTab mTab) {
		mTab.setValue("TaxAmt",
				getBD(mTab, "PriceWithTax").multiply(getBD(mTab, "taxrate")).divide(BD100, 6, RoundingMode.HALF_UP));
	}

	private void recalcInterestAmt(GridTab mTab) {
		mTab.setValue("InterestAmt", getBD(mTab, "PriceWithTax").multiply(getBD(mTab, "InterestRate")).divide(BD100, 6,
				RoundingMode.HALF_UP));
	}

	// =========================================================
	// 汇总计算
	// =========================================================

	/**
	 * 纸板成本+加工费 = 纸板成本 + 分纸费 + 印刷费 + 开槽费 + 啤机费 + 粘合费 + 钉箱费 + 清废费 + 打包费 + 损耗费 其他费用 =
	 * 业务费 + 返点 + 备品费 + 胶版费 + 刀模费 + 运费 + 综合费用 + 税费 + 贴息 合计费用 = 纸板成本+加工费 + 其他费用 利润 =
	 * 含税单价 - 合计费用 利润率 = 利润 / 含税单价 × 100
	 */
	private void recalcSummary(GridTab mTab) {
		// 纸板成本+加工费
		BigDecimal boardAndProcessCost = getBD(mTab, "BoardCost").add(getBD(mTab, "PaperCuttingFee"))
				.add(getBD(mTab, "PrintingFee")).add(getBD(mTab, "SlottingFee")).add(getBD(mTab, "DieCuttingFee"))
				.add(getBD(mTab, "GluingFee")).add(getBD(mTab, "StitchingFee")).add(getBD(mTab, "WasteCleaningFee"))
				.add(getBD(mTab, "PackingFee")).add(getBD(mTab, "WasteFee")).add(getBD(mTab, "OtherProcessFee"));
		mTab.setValue("BoardAndProcessCost", boardAndProcessCost);

		// 其他费用
		BigDecimal otherFee = getBD(mTab, "SalesFee").add(getBD(mTab, "Rebate")).add(getBD(mTab, "SpareFee"))
				.add(getBD(mTab, "PlateFee")).add(getBD(mTab, "DieFee")).add(getBD(mTab, "FreightAmt"))
				.add(getBD(mTab, "ComprehensiveFee")).add(getBD(mTab, "TaxAmt")).add(getBD(mTab, "InterestAmt"));
		mTab.setValue("OtherFee", otherFee);

		// 合计费用
		BigDecimal totalFee = boardAndProcessCost.add(otherFee);
		mTab.setValue("TotalFee", totalFee);

		// 利润 & 利润率
		BigDecimal priceWithTax = getBD(mTab, "PriceWithTax");
		BigDecimal profit = priceWithTax.subtract(totalFee);
		mTab.setValue("Profit", profit);

		if (priceWithTax.compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal profitRate = profit.divide(priceWithTax, 6, RoundingMode.HALF_UP).multiply(BD100);
			mTab.setValue("ProfitRate", profitRate);
		} else {
			mTab.setValue("ProfitRate", BigDecimal.ZERO);
		}
	}

	// =========================================================
	// 工具方法
	// =========================================================

	/** 从 GridTab 安全读取 BigDecimal，null 返回 ZERO */
	private BigDecimal getBD(GridTab mTab, String columnName) {
		Object val = mTab.getValue(columnName);
		if (val instanceof BigDecimal)
			return (BigDecimal) val;
		if (val instanceof Number)
			return BigDecimal.valueOf(((Number) val).doubleValue());
		return BigDecimal.ZERO;
	}
}