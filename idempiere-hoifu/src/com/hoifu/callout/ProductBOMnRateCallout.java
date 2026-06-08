package com.hoifu.callout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;

/**
 * 物料BOM-根据用量比例计算BOM数量
 */
@Callout(tableName = "PP_Product_BOM", columnName = { "nRate" })
public class ProductBOMnRateCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		Integer nRate = value instanceof Number n ? n.intValue() : 0;
		// 检查nRate是否有效
		if (nRate == null || nRate <= 0) {
			return "";
		}

		// 获取当前主表记录的ID
		Number num = (Number) mTab.getValue("PP_Product_BOM_ID");
		Integer PP_Product_BOM_ID = num != null ? num.intValue() : 0;
		
		if (PP_Product_BOM_ID == null || PP_Product_BOM_ID <= 0)
			return "";

		//在Callout开始时检查主表记录是否为新建或已修改但未保存状态,如果是则不执行子表更新:
			// 在获取PP_Product_BOM_ID之后添加  
			//if (mTab.isNew() || mTab.needSave(false, false)) {  
			    // 主表未保存,不更新子表  
			   // return "";  
			//}
		// 通过GridWindow找到子表
		GridWindow window = mTab.getGridWindow();
		if (window == null)
			return "";

		int currentTabLevel = mTab.getTabLevel();
		int currentTabIndex = window.getTabIndex(mTab);

		// 遍历后续的tabs,找到直接子表
		for (int i = currentTabIndex + 1; i < window.getTabCount(); i++) {
			GridTab childTab = window.getTab(i);

			// 只处理直接子表
			if (childTab.getTabLevel() <= currentTabLevel)
				break;

			if (childTab.getTabLevel() != currentTabLevel + 1)
				continue;

			if (!"PP_Product_BOMLine".equals(childTab.getTableName()))
				continue;

			// 确保子表已加载
			if (!childTab.isLoadComplete()) {
				childTab.initTab(false);
				childTab.query(false, 0, 0);
			}

			// 更新子表的所有行
			int rowCount = childTab.getRowCount();
			int currentRow = childTab.getCurrentRow();

			try {
				for (int row = 0; row < rowCount; row++) {
					childTab.setCurrentRow(row);

					BigDecimal nQtyBOM = (BigDecimal) childTab.getValue("nQtyBOM");
					if (nQtyBOM == null)
						continue;

					BigDecimal result = nQtyBOM.divide(BigDecimal.valueOf(nRate), 7, RoundingMode.HALF_UP);

					// 设置值
					childTab.setValue("QtyBOM", result);
				}
			} finally {
				childTab.setCurrentRow(currentRow);
			}

		}

		return "";

	}
}
