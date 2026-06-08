package com.hoifu.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MLookup;

@Callout(tableName = "PA_ReportSource", columnName = "SecondaryElementType")
public class ReportSourceCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		String elementType = (String) value;
		GridField groupField = mTab.getField("GroupByColumn");

		if (groupField != null && groupField.getLookup() instanceof MLookup) {
			MLookup groupLookup = (MLookup) groupField.getLookup();

			// 直接修改MLookupInfo中的ValidationCode
			String validation = getValidationForElementType(elementType);
			groupLookup.getLookupInfo().ValidationCode = validation;
			groupLookup.getLookupInfo().IsValidated = false;

			// 刷新lookup
			groupLookup.refresh();

			// 清空当前选择
			mTab.setValue("GroupByColumn", null);
		}

		return "";
	}

	private String getValidationForElementType(String elementType) {
		if ("C_BPartner".equals(elementType)) {
			return "Value = 'C_BP_Group_ID'";
		} else if ("Fact_Acct".equals(elementType)) {
			return "Value IN ('C_Period_ID')";
		} else if ("M_Product".equals(elementType)) {
			return "Value IN ('M_Product_Category_ID', 'M_Product_Category_ID_L1', 'M_Product_Category_ID_L2')";
		}
		return "1=2"; // 不显示任何选项
	}
}