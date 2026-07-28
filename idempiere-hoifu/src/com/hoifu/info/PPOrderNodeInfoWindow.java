package com.hoifu.info;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.NamePair;
import org.zkoss.zul.Messagebox;

public class PPOrderNodeInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 1L;

	// 对应 create(Lookup lookup, ...) 的调用
	public PPOrderNodeInfoWindow(int WindowNo, String tableName, String keyColumn,
								 String queryValue, boolean multipleSelection, String whereClause,
								 int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,
				whereClause, AD_InfoWindow_ID, lookup, field);
	}

	// 对应 create(int windowNo, int AD_InfoWindow_ID, ...) 和 create(int WindowNo, String tableName, ...) 的调用
	public PPOrderNodeInfoWindow(int WindowNo, String tableName, String keyColumn,
								 String queryValue, boolean multipleSelection, String whereClause,
								 int AD_InfoWindow_ID, boolean lookup, GridField field,
								 String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,
				whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);

		// 强制单选（覆盖父类因 haveProcess 而设置的 true）
		setMultipleSelection(false);

		// 窗口打开时自动查询数据（仅当 queryValue 为空时，避免重复查询）
		if (queryValue == null || queryValue.trim().isEmpty()) {
			executeQuery();
			renderItems();
			((ListModelTable) contentPanel.getModel()).setMultiple(false);
			bindInfoProcessBt();
		}
	}

	// ==================== 单选修复 ====================

	/**
	 * 重写 getSaveKeys，修复 p_multipleSelection=false 时父类直接返回 null 的问题。
	 */
	@Override
	public Collection<NamePair> getSaveKeys(int infoColumnId) {
		setMultipleSelection(true);
		try {
			Collection<NamePair> result = super.getSaveKeys(infoColumnId);
			return result != null ? result : Collections.emptyList();
		} finally {
			setMultipleSelection(false);
		}
	}

	/**
	 * 重写 saveResultSelection，修复 p_multipleSelection=false 时父类不填充 m_values 的问题。
	 */
	@Override
	protected void saveResultSelection(int infoColumnId) {
		setMultipleSelection(true);
		try {
			super.saveResultSelection(infoColumnId);
		} finally {
			setMultipleSelection(false);
		}

		// 修复 NPE：m_keyColumnIndex == -1 时父类直接 return，m_values 保持 null
		if (m_values == null) {
			m_values = new LinkedHashMap<>();
			return;
		}

		// 强制单选：只保留第一条记录（防御性代码）
		if (m_values.size() > 1) {
			NamePair firstKey = m_values.keySet().iterator().next();
			LinkedHashMap<NamePair, LinkedHashMap<String, Object>> single = new LinkedHashMap<>();
			single.put(firstKey, m_values.get(firstKey));
			m_values = single;
		}
	}

	@Override
	protected void preRunProcess(Integer processId) {
		MProcess process = MProcess.get(Env.getCtx(), processId);

		if (process != null && "org.libero.process.PPOrderNodeWorkReportingProcess".equals(process.getClassname())
					&& "生产完工".equals(process.getName())) {  // 只在生产完工时显示确认框

				List<Serializable> keys = getSelectedRowKeys();
			if (keys != null && !keys.isEmpty()) {
				int nodeId = (Integer) keys.get(0);

				BigDecimal movementQty = DB.getSQLValueBD(null,
						"SELECT COALESCE(SUM(MovementQty), 0) FROM PP_Cost_Collector "
								+ "WHERE PP_Order_Node_ID = ? AND CostCollectorType = '160' "
								+ "AND DocStatus NOT IN ('DR', 'VO')",
						nodeId);

				BigDecimal qtyRequiered = DB.getSQLValueBD(null,
						"SELECT QtyRequiered FROM PP_Order_Node WHERE PP_Order_Node_ID = ?", nodeId);

				if (movementQty == null) movementQty = BigDecimal.ZERO;

				if (qtyRequiered == null) qtyRequiered = BigDecimal.ZERO;

				boolean qualified = movementQty.compareTo(qtyRequiered) >= 0;

				String msg = "当前工序完工条件：\n"
						+ "  报工数：" + movementQty + "\n"
						+ "  标准加工数：" + qtyRequiered + "\n"
						+ (qualified ? "数量符合，可以完工。" : "数量不符合，强制完工请点确认按钮。");

				final Integer pid = processId;
				Messagebox.show(msg, "完工条件确认",
						Messagebox.OK | Messagebox.CANCEL,
						qualified ? Messagebox.INFORMATION : Messagebox.EXCLAMATION,
						evt -> {
							if (Messagebox.ON_OK.equals(evt.getName())) {
								PPOrderNodeInfoWindow.super.preRunProcess(pid);
							} else {
								enableButtons();
							}
						});
				return;
			}
		}
		super.preRunProcess(processId);
	}
}