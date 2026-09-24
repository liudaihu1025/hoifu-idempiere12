package com.hoifu.info;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.Env;

/**
 * 物料检验类型信息窗口自定义子类：
 * 将"新增检验类型"（ProductInspectionTypeSetIQCProcess）对应的流程按钮
 * 始终设为可点击（不依赖是否勾选行），同时保持其他流程按钮沿用框架默认的启用状态。
 *
 * 背景：标准 InfoPanel.enableButtons() 会在 selectedCount == 0 时
 * 将 btProcessList 中所有按钮全部 setEnabled(false)，
 * 但"新增检验类型"操作不依赖已勾选记录（业务语义为"弹出新增界面"），
 * 因此需要通过重写 enableButtons() 强制覆盖该按钮的启用状态。
 */
public class ProductInspectionTypeInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 1L;

	/** "新增检验类型"流程的 ClassName，与 AD_Process 表中配置一致 */
	private static final String NEW_INSPECTION_TYPE_PROCESS_CLASSNAME = "com.hoifu.process.ProductInspectionTypeSetIQCProcess";

	// ========== 构造函数（覆盖 InfoWindow 所有签名，确保框架反射能找到匹配的构造器） ==========

	public ProductInspectionTypeInfoWindow(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public ProductInspectionTypeInfoWindow(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public ProductInspectionTypeInfoWindow(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause, int AD_InfoWindow_ID,
			boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause,
				AD_InfoWindow_ID, lookup, field);
	}

	public ProductInspectionTypeInfoWindow(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause, int AD_InfoWindow_ID,
			boolean lookup, GridField field, String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause,
				AD_InfoWindow_ID, lookup, field, predefinedContextVariables);
	}

	// ========== 核心覆盖：强制启用"新增检验类型"流程按钮 ==========

	/**
	 * 覆盖框架默认行为：在父类按 selectedCount 统一设置按钮状态之后，
	 * 将"新增检验类型"流程按钮强制设为可点击（无论是否勾选了行）。
	 */
	@Override
	protected void enableButtons() {
		super.enableButtons();

		if (btProcessList == null || btProcessList.isEmpty()) {
			return;
		}

		for (Button btProcess : btProcessList) {
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
			if (processId == null) {
				continue;
			}

			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process == null || process.getClassname() == null) {
				continue;
			}

			if (NEW_INSPECTION_TYPE_PROCESS_CLASSNAME.equals(process.getClassname())) {
				btProcess.setEnabled(true);
				btProcess.setTooltiptext("");
			}
		}
	}
}
