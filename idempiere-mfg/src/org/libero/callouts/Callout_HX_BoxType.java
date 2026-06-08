package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.wf.MWorkflow;

public class Callout_HX_BoxType implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("AD_Workflow_ID")) {
			return workflow(ctx, WindowNo, mTab, mField, value);
		}
		return null;
	}

	public String workflow(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		if (value == null) {
			mTab.setValue("ad_workflow_value", null);
			mTab.setValue("ad_workflow_name", null);
			return "";
		}
		Integer AD_Workflow_ID = (Integer) value;
		if (AD_Workflow_ID <= 0) {
			mTab.setValue("ad_workflow_value", null);
			mTab.setValue("ad_workflow_name", null);
			return "";
		}
		MWorkflow workflow = MWorkflow.get(ctx, AD_Workflow_ID);
		if (workflow == null || workflow.get_ID() <= 0) {
			return "";
		}
		mTab.setValue("ad_workflow_value", workflow.getValue());
		mTab.setValue("ad_workflow_name", workflow.getName());
		return "";
	}

}
