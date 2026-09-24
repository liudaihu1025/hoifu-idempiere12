package org.libero.callouts;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.libero.model.PPOrderRepairInfoHelper;
import org.libero.model.PPOrderRepairInfoHelper.RepairAllocation;

public class Callout_PP_Order extends CalloutOrder implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("QtyEntered")) {
			qty(ctx, WindowNo, mTab, mField,value);
			String result = qtyBatch(ctx, WindowNo, mTab, mField,value);
			// 数量变化时，如果已选择关联订单行，重新计算补数信息
			Object orderLineObj = mTab.getValue("C_OrderLine_ID");
			if (orderLineObj instanceof Number && ((Number) orderLineObj).intValue() > 0) {
				fillRepairInfo(ctx, mTab, ((Number) orderLineObj).intValue());
			}
			return result;
		}
		if (mField.getColumnName().equals("M_Product_ID"))
			return product(ctx, WindowNo, mTab, mField,value);

		// 选择关联订单行后，自动带出补数信息
		if (mField.getColumnName().equals("C_OrderLine_ID"))
			return orderLineRepairInfo(ctx, WindowNo, mTab, mField, value);

		return null;
	}

	/**
	 * 选择 C_OrderLine_ID 后，查询随销单补数申请单，自动回填补数信息
	 */
	private String orderLineRepairInfo(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value) {
		if (isCalloutActive() || value == null)
			return "";

		int orderLineId = ((Number) value).intValue();
		if (orderLineId <= 0) {
			// 清空补数字段
			mTab.setValue("RepairQty", null);
			mTab.setValue("RepairMethod", null);
			mTab.setValue("Shortage_PP_Order_ID", null);
			return "";
		}

		fillRepairInfo(ctx, mTab, orderLineId);
		return "";
	}

	private static int s_repairDocTypeId = -1;

	private int getRepairDocTypeTargetId(Properties ctx) {
		if (s_repairDocTypeId < 0) {
			s_repairDocTypeId = DB.getSQLValueEx(null,
					"SELECT C_DocType_ID FROM C_DocType WHERE Name=? AND IsActive='Y' AND AD_Client_ID=?",
					"补数工单", Env.getAD_Client_ID(ctx));
		}
		return s_repairDocTypeId;
	}

	/**
	 * 根据关联订单行分摊补数信息（供 C_OrderLine_ID Callout 和 QtyEntered Callout 共用）
	 */
	private void fillRepairInfo(Properties ctx, GridTab mTab, int orderLineId) {

		// 排除"补数工单"类型
		Object docTypeTargetObj = mTab.getValue("C_DocTypeTarget_ID");
		if (docTypeTargetObj instanceof Number
				&& ((Number) docTypeTargetObj).intValue() == getRepairDocTypeTargetId(ctx)) {
			return;
		}

		BigDecimal qtyEntered = Env.ZERO;
		Object qtyObj = mTab.getValue("QtyEntered");
		if (qtyObj instanceof BigDecimal) {
			qtyEntered = (BigDecimal) qtyObj;
		}
		if (qtyEntered.signum() <= 0) {
			return;
		}

		// 获取当前工单ID（排除自身统计）
		int currentOrderId = 0;
		Object idObj = mTab.getValue("PP_Order_ID");
		if (idObj instanceof Number) {
			currentOrderId = ((Number) idObj).intValue();
		}

		// 调用公共方法分摊补数数量
		RepairAllocation allocation = PPOrderRepairInfoHelper.allocateRepairQty(
				ctx, orderLineId, qtyEntered, currentOrderId, null);

		if (allocation.allocated) {
			mTab.setValue("RepairQty", allocation.repairQty);
			mTab.setValue("RepairMethod", allocation.repairMethod);
			mTab.setValue("Shortage_PP_Order_ID", allocation.shortageOrderId);

		} else {
			mTab.setValue("RepairQty", null);
			mTab.setValue("RepairMethod", null);
			mTab.setValue("Shortage_PP_Order_ID", null);

		}
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
