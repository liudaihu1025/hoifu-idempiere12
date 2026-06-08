package com.hoifu.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

/**
 * 生产工单-工序物料表新增时工单工序callout
 * 
 * @ClassName: PPOrderNodeProductCallout
 * @author ldh
 * @date 2026年3月21日
 */
@Callout(tableName = "PP_Order_Node_Product", columnName = { "PP_Order_Node_ID" })
public class PPOrderNodeProductCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		return ppOrderNode(ctx, WindowNo, mTab, mField, value, oldValue);
	}

	public String ppOrderNode(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value,
			Object oldValue) {
		// 只在新增记录时设置父ID
		if (mTab.isNew()) {
			GridTab parentTab = mTab.getParentTab();
			if (parentTab != null && parentTab.getTableName().equals("PP_Order_Node")) {
				Integer parentNodeID = (Integer) parentTab.getValue("PP_Order_Node_ID");
				if (parentNodeID != null && parentNodeID > 0) {
					mTab.setValue("PP_Order_Node_ID", parentNodeID);
				}
			}
		}
		return "";
	}
}