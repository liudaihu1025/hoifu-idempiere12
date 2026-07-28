package com.hoifu.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MProduct;
import org.compiere.util.Env;

/**
 * 退库单明细 - 选择关联领用明细后自动回填物料、单位
 */
@Callout(tableName = "M_InventoryLine", columnName = { "ref_inventoryline_id" })
public class ReturnInventoryLineCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {

		// 1. 只在退库单中生效
		int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
		if (docTypeId <= 0)
			return "";

		MDocType docType = MDocType.get(ctx, docTypeId);
		if (docType == null || !"退库单".equals(docType.getName()))
			return "";

		// 2. 获取选中的领用明细 ID
		Integer refLineId = (Integer) value;
		if (refLineId == null || refLineId <= 0)
			return "";

		// 3. 加载被引用的领用明细行
		MInventoryLine refLine = new MInventoryLine(ctx, refLineId, null);
		if (refLine.get_ID() == 0)
			return "";

		// 4. 回填物料
		int productId = refLine.getM_Product_ID();
		if (productId > 0) {
			mTab.setValue("M_Product_ID", productId);

			// 5. 回填单位（从产品主数据取）
			MProduct product = MProduct.get(ctx, productId);
			if (product != null && product.getC_UOM_ID() > 0) {
				mTab.setValue("C_UOM_ID", product.getC_UOM_ID());
			}
		}

		return "";
	}
}
