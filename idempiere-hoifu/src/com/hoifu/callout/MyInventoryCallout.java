package com.hoifu.callout;  
  
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

@Callout(tableName = "M_Inventory", columnName = { "C_WorkTeam_ID" })
public class MyInventoryCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
  
		// 班组变更时，清空作业员字段，避免残留不属于新班组的人
		Integer workTeamId = (Integer) value;
		if (workTeamId == null || workTeamId == 0) {
			// 班组清空：作业员也清空（重新从雇员列表选）
			mTab.setValue("Operator_ID", null);
		} else {
			// 班组切换：清空旧的作业员，强制重新选择
			mTab.setValue("Operator_ID", null);
        }  

        return "";  
	}
}