package com.hoifu.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 收货单明细 - 免检物料目标库位覆盖库位字段
 *
 * 来料检验物料（InspectionType='IQC'）：M_Locator_ID 已通过 AD 字典默认值设为"预留区"，无需监听
 * 免检物料（InspectionType!='IQC'）：IntendedLocation_ID 变化时覆盖 M_Locator_ID，入库到目标库位
 */
@Callout(tableName = "M_InOutLine", columnName = { "IntendedLocation_ID" })
public class InOutLineInspectionLocatorCallout implements IColumnCallout {

	private static final String INSPECTIONTYPE_IQC = "IQC";
	private static final String LOCATORTYPE_PENDING_INSPECTION = "待检库位";

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Product_ID");
		if (M_Product_ID <= 0)
			return "";

		MProduct product = MProduct.get(ctx, M_Product_ID);
		if (product == null || product.get_ID() <= 0)
			return "";

		String inspectionType = (String) product.get_Value("InspectionType");

		if (value == null) {
			mTab.setValue("M_Locator_ID", null);
			return "";
		}

		int intendedLocatorId;
		try {
			intendedLocatorId = ((Number) value).intValue();
		} catch (Exception e) {
			return "";
		}
		if (intendedLocatorId <= 0)
			return "";

		// 来料检验物料：查询该仓库下"待检库位"类型的库位，赋值给 M_Locator_ID；目标库位保留用户原始选择，不覆盖
		if (INSPECTIONTYPE_IQC.equals(inspectionType)) {
			MLocator intendedLocator = MLocator.get(ctx, intendedLocatorId);
			if (intendedLocator == null || intendedLocator.get_ID() <= 0)
				return "";

			int warehouseId = intendedLocator.getM_Warehouse_ID();
			int pendingLocatorId = DB.getSQLValueEx(null,
					"SELECT l.M_Locator_ID FROM M_Locator l"
							+ " JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID)"
							+ " WHERE l.M_Warehouse_ID=? AND lt.Name=? AND l.IsActive='Y' AND lt.IsActive='Y'",
					warehouseId, LOCATORTYPE_PENDING_INSPECTION);

			if (pendingLocatorId <= 0)
				return "@M_Locator_ID@ (" + LOCATORTYPE_PENDING_INSPECTION + ") @NotFound@ (M_Warehouse_ID="
						+ warehouseId + ")";

			mTab.setValue("M_Locator_ID", pendingLocatorId);
			return "";
		}

		// 免检物料：目标库位覆盖库位字段
		mTab.setValue("M_Locator_ID", intendedLocatorId);
		return "";
	}
}
