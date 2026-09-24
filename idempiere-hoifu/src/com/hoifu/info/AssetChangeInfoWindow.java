package com.hoifu.info;

import java.io.Serializable;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.model.X_A_Asset;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class AssetChangeInfoWindow extends InfoWindow {
	private static final long serialVersionUID = 1L;

	/** 变更资产使用信息流程对应的 Java 类全名，用于在按钮列表里定位目标按钮 */
	private static final String CHANGE_PROCESS_CLASSNAME = "com.hoifu.process.AssetUsageChangeProcess";

	/** 写入 Env 上下文的 Key，供 AssetUsageChangeParameterListener 读取本次选中的资产数量 */
	private static final String CTX_KEY_SELECTED_COUNT = "_IWInfo_AssetUsageChange_SelectedCount";

	public AssetChangeInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override
	protected void renderWindow() {
		super.renderWindow();
		// 打开时自动触发查询（AuEcho 异步，构造完成后才回调，安全）
		onUserQuery();
	}


	@Override
	protected void enableButtons() {

		super.enableButtons();

		List<Serializable> keys = getSelectedRowKeys();
		int selectedCount = keys == null ? 0 : keys.size();

		boolean allowed = selectedCount > 0 && isAllSelectedStatusAllowed(keys);

		for (Button btProcess : btProcessList) {
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
			if (processId == null)
				continue;

			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process == null || process.getClassname() == null)
				continue;

			if (CHANGE_PROCESS_CLASSNAME.equals(process.getClassname())) {
				btProcess.setEnabled(allowed);
				btProcess.setTooltiptext(allowed ? "" : "仅支持已新增/已激活/闲置中状态的资产变更使用信息");
			}
		}
	}

	/**
	 * 重写：在点击"变更资产使用信息"流程按钮、参数面板尚未弹出之前，把本次选中的资产数量 写入 Env 上下文，供
	 * AssetUsageChangeParameterListener.onInit() 在批量场景下清空 默认值。
	 */
	@Override
	protected void preRunProcess(Integer processId) {
		if (processId != null) {
			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process != null && CHANGE_PROCESS_CLASSNAME.equals(process.getClassname())) {
				List<Serializable> keys = getSelectedRowKeys();
				int selectedCount = keys == null ? 0 : keys.size();
				Env.setContext(Env.getCtx(), p_WindowNo, CTX_KEY_SELECTED_COUNT, String.valueOf(selectedCount));
			}
		}
		super.preRunProcess(processId);
	}

	/*
	 * 判断选中记录是否满足变更条件
	 */
	private boolean isAllSelectedStatusAllowed(List<Serializable> keys) {
		if (keys == null || keys.isEmpty())
			return false;

		StringBuilder idList = new StringBuilder();
		for (Serializable key : keys) {
			if (idList.length() > 0)
				idList.append(",");
			idList.append(key);
		}

		String sql = "SELECT COUNT(*) FROM A_Asset WHERE A_Asset_ID IN (" + idList + ") AND A_Asset_Status NOT IN ('"
				+ X_A_Asset.A_ASSET_STATUS_New + "','" + X_A_Asset.A_ASSET_STATUS_Activated + "','"
				+ X_A_Asset.A_ASSET_STATUS_Preservation + "')";

		int notAllowedCount = DB.getSQLValueEx(null, sql);
		return notAllowedCount == 0;
	}
}