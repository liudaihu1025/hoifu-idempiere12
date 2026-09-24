package com.hoifu.listener;

import java.util.List;

import org.adempiere.webui.apps.IProcessParameterListener;
import org.adempiere.webui.apps.ProcessParameterPanel;
import org.adempiere.webui.editor.WEditor;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

@Component(service = IProcessParameterListener.class, property = {
		"ProcessClass=com.hoifu.process.AssetUsageChangeProcess" })
public class AssetUsageChangeParameterListener implements IProcessParameterListener {

	// 信息窗口多选上下文变量
	private static final String CTX_KEY_ASSET_SELECTED_COUNT = "_IWInfo_AssetUsageChange_SelectedCount";

	/** 需要在多选场景下被清空的参数字段列名（对应 AD_Process_Para.ColumnName） */
	private static final String[] COLUMNS_TO_CLEAR_ON_MULTI_SELECT = { "C_Activity_ID", // 新部门
			"C_BPartner_ID", // 新使用单位
			"AD_User_ID", // 新使用人
			"C_BPartner_Location_ID", // 新使用地址
			"M_Locator_ID" // 新使用位置
	};

	/**
	 * 参数面板初始化完成后触发。清空字段的默认值
	 * 
	 * @param parameterPanel 流程参数面板
	 */
	@Override
	public void onInit(ProcessParameterPanel parameterPanel) {
		if (!isMultiSelect(parameterPanel))
			return;

		for (String columnName : COLUMNS_TO_CLEAR_ON_MULTI_SELECT) {
			WEditor editor = parameterPanel.getEditor(columnName);
			if (editor != null) {
				editor.setValue(null);
			}
		}
	}

	/**
	 * 判断本次运行流程是否选中了多条资产记录（批量场景）。 - 普通窗口：ProcessInfo.getRecord_IDs()
	 * 在参数面板弹出前已经被赋值，直接判断。 - 信息窗口（InfoWindow）：ProcessInfo.getRecord_IDs() 不会被赋值，
	 * 改为读取 AssetChangeInfoWindow.preRunProcess() 提前写入 Env context 的选中数量。
	 * 
	 * @param parameterPanel 流程参数面板
	 * @return true 表示本次选中的资产记录数 &gt; 1（批量场景）
	 */
	private boolean isMultiSelect(ProcessParameterPanel parameterPanel) {
		ProcessInfo processInfo = parameterPanel.getProcessInfo();
		if (processInfo == null)
			return false;

		// 1) 普通窗口场景：直接看 Record_IDs
		List<Integer> recordIds = processInfo.getRecord_IDs();
		if (recordIds != null && recordIds.size() > 1)
			return true;
		if (recordIds != null && recordIds.size() == 1)
			return false;

		// 2) 信息窗口场景：读取 AssetChangeInfoWindow.preRunProcess 写入的选中数量
		String countStr = Env.getContext(Env.getCtx(), parameterPanel.getWindowNo(), CTX_KEY_ASSET_SELECTED_COUNT);
		if (countStr != null && !countStr.isEmpty()) {
			try {
				return Integer.parseInt(countStr) > 1;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * 参数字段值变化时触发。 本监听器只需要在初始化阶段清空默认值，不需要响应用户手动修改 某个字段之后联动其它字段，因此这里留空实现。
	 * 
	 * @param parameterPanel 流程参数面板
	 * @param columnName     发生变化的参数列名
	 * @param editor         对应的编辑器
	 */
	@Override
	public void onChange(ProcessParameterPanel parameterPanel, String columnName, WEditor editor) {
		// 不需要处理
	}

}