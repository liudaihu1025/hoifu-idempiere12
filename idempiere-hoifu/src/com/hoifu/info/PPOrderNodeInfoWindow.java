package com.hoifu.info;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.UUIDColumn;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.NamePair;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.hoifu.window.ProcessTrackEditDialog;

public class PPOrderNodeInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 1L;

	/** 自定义按钮 ID，避免和 ConfirmPanel 内置按钮（Ok/Cancel/Zoom...）ID 冲突 */
	private static final String BTN_PROCESS_TRACK = "ProcessTrack";

	/** "打样追踪"按钮引用，renderWindow() 执行后才会被赋值 */
	private Button btnProcessTrack;

	// 防递归标志字段
	private boolean enforcingSingleSelection = false;

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

	/**
	 * 新增打样追踪按钮，弹出编辑窗口
	 */
	@Override
	protected void renderWindow() {
		super.renderWindow();

		btnProcessTrack = confirmPanel.createButton(BTN_PROCESS_TRACK);
		btnProcessTrack.setLabel("打样追踪");
		btnProcessTrack.setTooltiptext("录入/编辑当前选中工序的打样追踪参数");
		btnProcessTrack.setDisabled(true); // 初始未选中任何行，置灰
		btnProcessTrack.addEventListener(Events.ON_CLICK, this);
		confirmPanel.addComponentsLeft(btnProcessTrack);
	}

	/**
	 * 处理"打样追踪"按钮点击；其余事件交回父类处理
	 */
	@Override
	public void onEvent(Event event) {
		// 防止 clearSelection()/setSelection() 触发递归
		if (enforcingSingleSelection) {
			super.onEvent(event);
			return;
		}

		// 拦截 listbox 的选中事件，强制单选，确保 recordSelectedData 只保留当前行
		if (event.getTarget() == contentPanel && Events.ON_SELECT.equals(event.getName())) {
			SelectEvent<?, ?> selectEvent = (SelectEvent<?, ?>) event;
			int selectedIndex = -1;
			if (selectEvent.getReference() instanceof Listitem) {
				Listitem li = (Listitem) selectEvent.getReference();
				selectedIndex = li.getIndex();
			}

			if (selectedIndex >= 0) {
				enforcingSingleSelection = true;
				try {
					final int idx = selectedIndex;

					int keyColIdx = contentPanel.getKeyColumnIndex();
					if (keyColIdx >= 0) {
						for (int i = 0; i < contentPanel.getModel().getRowCount(); i++) {
							Object data = contentPanel.getModel().getValueAt(i, keyColIdx);
							if (data instanceof IDColumn) {
								((IDColumn) data).setSelected(i == idx);
							} else if (data instanceof UUIDColumn) {
								((UUIDColumn) data).setSelected(i == idx);
							}
						}
					}

					// 清除 recordSelectedData，确保里面只有当前行的数据
					recordSelectedData.clear();

					ListModelTable lmt = (ListModelTable) contentPanel.getModel();
					lmt.clearSelection();
					List<Object> single = new ArrayList<>();
					single.add(lmt.getElementAt(idx));
					lmt.setSelection(single);

				} finally {
					enforcingSingleSelection = false;
				}
			}
		}

		// 按钮点击处理逻辑
		if (btnProcessTrack != null && event.getTarget() == btnProcessTrack) {
			openProcessTrackDialog();
			return;
		}
		super.onEvent(event);
	}

	/**
	 * 控制"打样追踪"按钮的可用状态：工单类型=打样工单或研发工单
	 */
	@Override
	protected void enableButtons() {
		super.enableButtons();

		int selectedCount = contentPanel.getSelectedCount();
		boolean canTrack = false;

		if (selectedCount > 0 && btnProcessTrack != null) {
			List<Serializable> keys = getSelectedRowKeys();
			if (keys != null && !keys.isEmpty()) {
				int ppOrderNodeId = (Integer) keys.get(0);
				canTrack = DB.getSQLValueEx(null,
						"SELECT COUNT(*) FROM PP_Order o " + "JOIN PP_Order_Node n ON n.PP_Order_ID = o.PP_Order_ID "
								+ "WHERE n.PP_Order_Node_ID = ? " + "AND o.C_DocTypeTarget_ID IN (?, ?)",
						ppOrderNodeId, 1000755, 1000758) > 0;
			}
		}

		if (btnProcessTrack != null) {
			btnProcessTrack.setEnabled(canTrack);
		}
	}

	/**
	 * 打开打样追踪编辑弹窗：取当前选中的 PP_Order_Node_ID（该窗口已强制单选， 只会有一条），再查出对应的 PP_Order_ID，交给
	 * ProcessTrackEditDialog。
	 */
	private void openProcessTrackDialog() {
		List<Serializable> keys = getSelectedRowKeys();
		if (keys == null || keys.isEmpty()) {
			Messagebox.show("请先勾选工序信息！");
			return;
		}

		int ppOrderNodeId = (Integer) keys.get(0);
		int ppOrderId = DB.getSQLValue(null, "SELECT PP_Order_ID FROM PP_Order_Node WHERE PP_Order_Node_ID=?",
				ppOrderNodeId);
		if (ppOrderId <= 0) {
			Messagebox.show("未能查询到该工序对应的工单，请重新选择！");
			return;
		}

		ProcessTrackEditDialog dialog = new ProcessTrackEditDialog(ppOrderId, ppOrderNodeId);
		AEnv.showCenterScreen(dialog);
	}
}