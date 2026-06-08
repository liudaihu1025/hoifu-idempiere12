package com.hoifu.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;

/**
 * 工序-根据选择的路由节点自动带出Name
 */
@Callout(tableName = "AD_WF_Node", columnName = { "AD_Routing_Node_ID" })
public class WFNodeRoutingCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		Integer routingNodeId = (Integer) mTab.getValue("AD_Routing_Node_ID");

		if (routingNodeId != null && routingNodeId.intValue() != 0) {
			// 根据 AD_Routing_Node_ID 引用的实际表名和主键列名修改此 SQL
			String name = DB.getSQLValueString(null,
					"SELECT Name FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'", routingNodeId);

			if (name != null) {
				mTab.setValue("Name", name);
			}
		}

		return "";
	}
}