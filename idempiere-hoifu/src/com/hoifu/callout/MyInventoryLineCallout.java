package com.hoifu.callout;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.util.DB;
import org.compiere.util.Env;  
  
@Callout(tableName = "M_InventoryLine", columnName = { "M_Product_ID", "C_UOM_ID", "QtyEntered", "QtyInternalUse",
		"QtyCount" })
public class MyInventoryLineCallout implements IColumnCallout {

	// 单据名称 → 目标数量字段的映射
	private String getTargetQtyField(String docTypeName) {
		if ("领用单".equals(docTypeName) || "行政物料领用单".equals(docTypeName))
			return "QtyInternalUse";
		if ("库存盘点单".equals(docTypeName))
			return "QtyCount";

		return null; // 其他单据不处理
	}

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (value == null)
			return "";

		String col = mField.getColumnName();
		if ("M_Product_ID".equals(col))
			return handleProduct(ctx, WindowNo, mTab, value);
		if ("C_UOM_ID".equals(col) || "QtyEntered".equals(col))
			return handleQtyUOM(ctx, WindowNo, mTab, mField, value);
		if ("QtyInternalUse".equals(col))
	            return handleQtyInternalUse(ctx, WindowNo, mTab, value);  
		if ("QtyCount".equals(col))
				return handleQtyCount(ctx, WindowNo, mTab, value);
		return "";
	}

	private String handleProduct(Properties ctx, int WindowNo, GridTab mTab, Object value) {
        int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");  
		if (docTypeId <= 0)
			return "";
  
		MDocType docType = MDocType.get(ctx, docTypeId);
        String docTypeName = docType.getName();  
  
		// "领用单"才清空 PP_Order_ID
		if ("领用单".equals(docTypeName))
			mTab.setValue("PP_Order_ID", null);
  
        Integer productId = (Integer) value;  
		if (productId == null || productId == 0)
			return "";
  
		// 推荐库位逻辑（原有）
        int warehouseId = Env.getContextAsInt(ctx, WindowNo, "M_Warehouse_ID");  
		if (warehouseId > 0) {
			int locatorId = DB.getSQLValue(null, "SELECT get_recommended_locator(?, ?, ?)", productId, warehouseId,
					"N");
			if (locatorId > 0)
				mTab.setValue("M_Locator_ID", locatorId);
		}

		// 切换产品时重置单位和数量
		String targetQtyField = getTargetQtyField(docTypeName);
		if (targetQtyField != null) {
			MProduct product = MProduct.get(ctx, productId);
			if (product != null) {
				mTab.setValue("C_UOM_ID", product.getC_UOM_ID());
				mTab.setValue("QtyEntered", Env.ZERO);
				mTab.setValue(targetQtyField, Env.ZERO);
			}
		}

		return "";
	}

	private String handleQtyUOM(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
		if (docTypeId <= 0)
			return "";

		MDocType docType = MDocType.get(ctx, docTypeId);
		String docTypeName = docType.getName();

		// 只处理三种单据
		String targetQtyField = getTargetQtyField(docTypeName);
		if (targetQtyField == null)
			return "";

		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Product_ID");
		if (M_Product_ID == 0)
			return "";

		int C_UOM_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "C_UOM_ID");
		if (C_UOM_ID == 0)
			return "";
  
		BigDecimal QtyEntered;
  
		if ("C_UOM_ID".equals(mField.getColumnName())) {
			// 单位变化：用新单位精度重新四舍五入 QtyEntered
			int newUOM = ((Integer) value).intValue();
			QtyEntered = (BigDecimal) mTab.getValue("QtyEntered");
			if (QtyEntered == null)
				QtyEntered = Env.ZERO;
			BigDecimal scaled = QtyEntered.setScale(MUOM.getPrecision(ctx, newUOM), RoundingMode.HALF_UP);
			if (QtyEntered.compareTo(scaled) != 0) {
				QtyEntered = scaled;
				mTab.setValue("QtyEntered", QtyEntered);
			}
			C_UOM_ID = newUOM;
		} else {
			// QtyEntered 变化：用当前单位精度四舍五入
			QtyEntered = (BigDecimal) value;
			BigDecimal scaled = QtyEntered.setScale(MUOM.getPrecision(ctx, C_UOM_ID), RoundingMode.HALF_UP);
			if (QtyEntered.compareTo(scaled) != 0) {
				QtyEntered = scaled;
				mTab.setValue("QtyEntered", QtyEntered);
			}
        }  
  
		// 录入单位 → 产品基础单位换算，写入目标字段
		BigDecimal qtyConverted = MUOMConversion.convertProductFrom(ctx, M_Product_ID, C_UOM_ID, QtyEntered);
		if (qtyConverted == null) {
			// 找不到换算率，返回错误信息
			return "录入单位与产品基础单位之间没有配置换算规则，请先在计量单位页签配置换算规则";
		}
		mTab.setValue(targetQtyField, qtyConverted);
        return "";  
    } 
	   private String handleQtyInternalUse(Properties ctx, int WindowNo, GridTab mTab, Object value) {  
	        int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");  
	        if (docTypeId <= 0)  
	            return "";  
	  
	        MDocType docType = MDocType.get(ctx, docTypeId);  
	        String docTypeName = docType.getName();  
	  
	        // 只处理领用单  
	        if (!"领用单".equals(docTypeName) && !"行政物料领用单".equals(docTypeName))  
	            return "";  
	  
	        int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Product_ID");  
	        if (M_Product_ID == 0)  
	            return "";  
	  
	        int C_UOM_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "C_UOM_ID");  
	        if (C_UOM_ID == 0)  
	            return "";  
	  
	        BigDecimal qtyInternalUse = (BigDecimal) value;  
	        if (qtyInternalUse == null)  
	            qtyInternalUse = Env.ZERO;  
	  
	        // 基础单位 → 录入单位（反向换算）  
	        BigDecimal qtyEntered = MUOMConversion.convertProductTo(ctx, M_Product_ID, C_UOM_ID, qtyInternalUse);  
	        if (qtyEntered == null)  
	        	return "录入单位与产品基础单位之间没有配置换算规则，请先在计量单位页签配置换算规则";  
	  
	        // 用录入单位精度四舍五入  
	        qtyEntered = qtyEntered.setScale(MUOM.getPrecision(ctx, C_UOM_ID), RoundingMode.HALF_UP);  
	        mTab.setValue("QtyEntered", qtyEntered);  
	  
	        return "";  
		}

		private String handleQtyCount(Properties ctx, int WindowNo, GridTab mTab, Object value) {
			int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
			if (docTypeId <= 0)
				return "";

			MDocType docType = MDocType.get(ctx, docTypeId);
			String docTypeName = docType.getName();

			// 只处理库存盘点单
			if (!"库存盘点单".equals(docTypeName))
				return "";

			int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Product_ID");
			if (M_Product_ID == 0)
				return "";

			int C_UOM_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "C_UOM_ID");
			if (C_UOM_ID == 0)
				return "";

			BigDecimal qtyCount = (BigDecimal) value;
			if (qtyCount == null)
				qtyCount = Env.ZERO;

			// 基础单位 → 录入单位（反向换算）
			BigDecimal qtyEntered = MUOMConversion.convertProductTo(ctx, M_Product_ID, C_UOM_ID, qtyCount);
			if (qtyEntered == null)
				return "录入单位与产品基础单位之间没有配置换算规则，请先在计量单位页签配置换算规则";

			// 用录入单位精度四舍五入
			qtyEntered = qtyEntered.setScale(MUOM.getPrecision(ctx, C_UOM_ID), RoundingMode.HALF_UP);
			mTab.setValue("QtyEntered", qtyEntered);

			return "";
		}
	}
