package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.eevolution.model.MPPProductBOM;

public class Callout_PP_Order extends CalloutOrder implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("QtyEntered")) {
			qty(ctx, WindowNo, mTab, mField,value);
			return qtyBatch(ctx, WindowNo, mTab, mField,value);
		}
		if (mField.getColumnName().equals("M_Product_ID"))
			return product(ctx, WindowNo, mTab, mField,value);

		// 添加BOM状态验证
		if (mField.getColumnName().equals("PP_Product_BOM_ID")) {
			return validateBOMStatus(ctx, WindowNo, mTab, mField, value, oldValue);
		}

		return null;
	}

	private String validateBOMStatus(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value,
			Object oldValue) {
		if (isCalloutActive() || value == null || (Integer) value <= 0) {
			return "";
		}

		int PP_Product_BOM_ID = (Integer) value;
		MPPProductBOM bom = MPPProductBOM.get(ctx, PP_Product_BOM_ID);

		if (bom != null) {
			String bomStatus = bom.get_ValueAsString("bomstatus");
			if (!"released".equals(bomStatus)) {
				mTab.setValue("PP_Product_BOM_ID", oldValue);
	//			throw new AdempiereException("BOM状态不是已发布状态，请选择已发布的BOM");
			}
		}

		return "";
	}

}
