package com.hoifu.info;

import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.Env;

/**
 * 自定义信息窗口：M_InOut_CreateFrom_v 批量设置库位流程执行后不关闭窗口，其他流程（如创建明细）正常关闭。
 */
public class CreateFromInOutInfoWindow extends InfoWindow {  

	private static final long serialVersionUID = 1L;

	/**
	 * 批量设置库位流程在 AD_Process 中配置的 Value 字段值 需要与你在 AD 中配置的 AD_Process.Value 一致
	 */
	private static final String BATCH_SET_LOCATOR_PROCESS_VALUE = "M_InOut_BatchSetLocatorProcess";

	public CreateFromInOutInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

    @Override  
	protected void runProcess(Object processIdObj) {
		if (processIdObj instanceof Integer) {
			int processId = (Integer) processIdObj;
			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process != null && BATCH_SET_LOCATOR_PROCESS_VALUE.equals(process.getValue())) {
				// 批量设置库位流程：执行后不关闭窗口，刷新数据
				setCloseAfterExecutionOfProcess(false);
			} else {
				// 其他流程（如创建明细行）：正常关闭窗口
				setCloseAfterExecutionOfProcess(true);
            }  
        }  
		super.runProcess(processIdObj);
    }  

	/**
	 * 重写：将流程按钮放到左下角，而不是右下角
	 */
	@Override
	public void moveProcessButtonsToBeforeRight() {
		if (btProcessList == null || btProcessList.isEmpty())
			return;
		for (org.adempiere.webui.component.Button btn : btProcessList) {
			// 从按钮属性中取出对应的 MProcess 对象
			Object att = btn.getAttribute(ATT_INFO_PROCESS_KEY);
			if (att instanceof MProcess) {
				MProcess process = (MProcess) att;
				if (BATCH_SET_LOCATOR_PROCESS_VALUE.equals(process.getValue())) {
					// 批量设置库位按钮 → 放左边
					confirmPanel.addComponentsLeft(btn);
					continue;
				}
			}
			// 其他流程（如创建明细）→ 保持右边（原逻辑）
			confirmPanel.addComponentsBeforeRight(btn);
		}
	}
}