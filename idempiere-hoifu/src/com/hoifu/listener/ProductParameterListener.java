package com.hoifu.listener;

import org.adempiere.webui.apps.IProcessParameterListener;
import org.adempiere.webui.apps.ProcessParameterPanel;
import org.adempiere.webui.editor.WEditor;
import org.compiere.process.ProcessInfo;
import org.osgi.service.component.annotations.Component;

@Component(service = IProcessParameterListener.class, property = { "process.className=com.hoifu.process.RequisitionDetailPOCreate" })
public class ProductParameterListener implements IProcessParameterListener {


	@Override
	public void onInit(ProcessParameterPanel parameterPanel) {
		ProcessInfo pi = parameterPanel.getProcessInfo();

		// 条件绑定：只在指定流程或信息窗口中生效
		boolean isTargetProcess = pi.getAD_Process_ID() ==  123;

		if (isTargetProcess) {
			addNewProductButtonForProductField(parameterPanel);
		}
	}

	private void addNewProductButtonForProductField(ProcessParameterPanel panel) {
		// 实现添加新建物料按钮的逻辑
	}

	@Override
	public void onChange(ProcessParameterPanel parameterPanel, String columnName, WEditor editor) {
		// TODO 自动生成的方法存根
		
	}
}