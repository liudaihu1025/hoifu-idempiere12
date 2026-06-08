package com.hoifu.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 物料BOM部件明细-根据用量比例计算BOM数量
 */
@Callout(tableName = "PP_Product_BOMLine", columnName = { "nQtyBOM" })
public class ProductBOMLineNQtyBOMCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		// 获取nQtyBOM字段的值（浮点数）
		BigDecimal nQtyBOM = (BigDecimal) mTab.getValue("nQtyBOM");

		// 获取父表的nRate字段值 （整数）
		Number num = (Number) mTab.getParentTab().getValue("nRate");
		Integer nRate = num != null ? num.intValue() : 0;  
		
		if (nQtyBOM != null && nRate != null && nRate != 0) {

			BigDecimal result = nQtyBOM.divide(new BigDecimal(nRate), 7, RoundingMode.HALF_UP);
			System.out.println(result);
			mTab.setValue("QtyBOM", result);
		}

		return "";
	}

}
