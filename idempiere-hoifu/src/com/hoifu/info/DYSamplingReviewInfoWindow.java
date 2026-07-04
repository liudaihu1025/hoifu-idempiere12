package com.hoifu.info;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

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

import com.hoifu.utils.DYSamplingUtil;

public class DYSamplingReviewInfoWindow extends InfoWindow {

	/** 防止 clearSelection() 触发递归的标志 */
	private boolean enforcingSingleSelection = false;

	/**
	 * 记录流程操作前选中的记录主键（Integer 整数主键 或 String UUID）。
	 * 
	 * 由 {@link #preRunProcess} 在流程执行前设置 由 {@link #onEvent}
	 * 在用户主动点击新行时清除（避免后续刷新覆盖用户的新选择） 由 {@link #onQueryCallback} 在列表刷新后用于恢复光标，不在此处重置
	 * （解决 ADForm 路径触发两次 onQueryCallback 的问题）
	 * 
	 */
	private Object lastSelectedKey = null;

	public DYSamplingReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);

		// 注入合并单元格渲染器（列名配置在 DYSamplingMergeRenderer 内部）
		DYSamplingMergeRenderer mergeRenderer = new DYSamplingMergeRenderer(this);
		mergeRenderer.setColumnInfos(columnInfos);
		contentPanel.setItemRenderer(mergeRenderer);

		// 强制单选
		// 注意：p_multipleSelection 仍需在 getSaveKeys/saveResultSelection 中临时恢复为 true，
		// 否则父类会返回 null 或不填充 m_values，导致流程提交时 NPE。
		setMultipleSelection(false);

		// 窗口打开时自动查询数据（仅当 queryValue 为空时，避免重复查询）
		if (queryValue == null || queryValue.trim().isEmpty()) {
			executeQuery();
			renderItems();
			// ★ bug 修复：renderItems() 内部用 p_multipleSelection 初始化 model.setMultiple()，
			// 但 super() 中 haveProcess=true 会强制 p_multipleSelection=true，
			// 导致 renderItems() 把 model 设为多选模式。
			// 在 renderItems() 之后手动将 model 改回单选，确保 listbox 真正单选。
			((ListModelTable) contentPanel.getModel()).setMultiple(false);
			bindInfoProcessBt(); // 查询完成后立即更新按钮显示/隐藏，无需手动刷新
		}
	}

	/**
	 * 重写 getSaveKeys，修复 p_multipleSelection=false 时父类直接返回 null 的问题。
	 * 
	 * 
	 * 父类注释："should never reach here, when have process, p_multipleSelection is
	 * always true"，但我们强制了单选，所以需要临时恢复多选让父类正常处理，再恢复单选。
	 * 
	 * 调用时机：用户点击流程按钮时，{@code InfoPanel.runProcess} 调用此方法将选中记录写入 {@code T_Selection}
	 * 表，供流程/ADForm 读取。
	 */
	@Override
	public Collection<NamePair> getSaveKeys(int infoColumnId) {
		// 临时恢复多选，让父类走 if (p_multipleSelection) 分支，返回 m_viewIDMap（非 null）
		setMultipleSelection(true);
		try {
			Collection<NamePair> result = super.getSaveKeys(infoColumnId);
			// 防御性兜底：理论上不会为 null，但以防万一
			return result != null ? result : Collections.emptyList();
		} finally {
			// 无论是否抛异常，都恢复单选状态
			setMultipleSelection(false);
		}
	}

	/**
	 * 重写 saveResultSelection，修复 p_multipleSelection=false 时父类不填充 m_values 的问题。
	 * 
	 * 父类在 p_multipleSelection=false 时，{@code m_values} 保持为空 map，后续
	 * {@code createT_Selection_InfoWindow} 在 {@code m_values.entrySet()} 上会 NPE。
	 * 
	 * 调用时机：普通流程（非 ADForm）提交时，{@code InfoPanel.runProcess} 调用此方法将选中记录的列数据写入
	 * {@code T_Selection_InfoWindow} 表。
	 */
	@Override
	protected void saveResultSelection(int infoColumnId) {
		// 临时恢复多选，让父类正常填充 m_values
		setMultipleSelection(true);
		try {
			super.saveResultSelection(infoColumnId);
		} finally {
			// 无论是否抛异常，都恢复单选状态
			setMultipleSelection(false);
		}

		// 修复 NPE：m_keyColumnIndex == -1 时父类直接 return，m_values 保持 null，
		// createT_Selection_InfoWindow 会在 m_values.entrySet() 上 NPE
		if (m_values == null) {
			m_values = new LinkedHashMap<>();
			return;
		}

		// 强制单选：只保留第一条记录（正常情况下 listbox 单选模式已保证只有一条，此处为防御性代码）
		if (m_values.size() > 1) {
			NamePair firstKey = m_values.keySet().iterator().next();
			LinkedHashMap<NamePair, LinkedHashMap<String, Object>> single = new LinkedHashMap<>();
			single.put(firstKey, m_values.get(firstKey));
			m_values = single;
		}
	}

	/**
	 * 重写 preRunProcess，在流程执行前保存当前选中的记录主键， 供 {@link #onQueryCallback} 在列表刷新后恢复光标位置。
	 */
	@Override
	protected void preRunProcess(Integer processId) {
		// 保存当前选中的记录主键（Integer 整数主键 或 String UUID）
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys != null && selectedKeys.size() == 1) {
			Object key = selectedKeys.get(0);
			try {
				// 整数主键（如 dy_samplingreviewline_id）
				lastSelectedKey = Integer.parseInt(key.toString());
			} catch (NumberFormatException e) {
				// UUID 主键，保持 String 类型
				lastSelectedKey = key.toString();
			}
			Env.setContext(Env.getCtx(), p_WindowNo, "_IWInfo_dy_samplingreviewline_ID", lastSelectedKey.toString());
		}
		super.preRunProcess(processId);
	}

	/**
	 * 重写 onQueryCallback，在列表刷新后恢复光标到操作前选中的记录。
	 * 
	 *
	 * 父类的 {@code restoreSelectedInPage()} 在 {@code p_multipleSelection=false} 时直接
	 * return， 不恢复选中状态，导致光标跳到第一条记录。此重写在父类处理完后调用 {@link #restoreSingleSelectionByKey}
	 * 恢复光标。
	 *
	 * 注意：ADForm 路径关闭时会触发两次 onQueryCallback，此处不重置 lastSelectedKey，
	 * 每次调用都尝试恢复，确保两次调用都能正确恢复光标。 lastSelectedKey 由 {@link #onEvent} 在用户主动点击新行时清除。
	 */
	@Override
	public void onQueryCallback(Event event) {
		super.onQueryCallback(event);
		// 流程操作完成后，恢复光标到操作前选中的记录
		restoreSingleSelectionByKey(lastSelectedKey);
	}

	/**
	 * 在刷新后的列表中找到主键匹配的行并选中，恢复光标位置。
	 * 
	 * 如果该记录在刷新后不再出现在列表中（如状态变化导致被过滤），则不做任何操作。
	 * 
	 * @param keyToRestore 要恢复选中的记录主键（Integer 或 String UUID），null 时不做任何操作
	 */
	@SuppressWarnings("unchecked")
	private void restoreSingleSelectionByKey(Object keyToRestore) {
		if (keyToRestore == null)
			return;
		ListModelTable lmt = (ListModelTable) contentPanel.getModel();
		if (lmt == null)
			return;

		int keyColIdx = contentPanel.getKeyColumnIndex();
		if (keyColIdx < 0)
			return;

		for (int rowIndex = 0; rowIndex < lmt.getRowCount(); rowIndex++) {
			Object data = lmt.getValueAt(rowIndex, keyColIdx);
			Object rowKey = null;
			if (data instanceof IDColumn) {
				rowKey = ((IDColumn) data).getRecord_ID();
			} else if (data instanceof UUIDColumn) {
				rowKey = ((UUIDColumn) data).getRecord_UU();
			}

			if (keyToRestore.equals(rowKey)) {
				// 找到了，选中这一行
				enforcingSingleSelection = true;
				try {
					final int idx = rowIndex;

					// 更新 IDColumn/UUIDColumn 的选中状态（复选框视觉渲染）
					for (int i = 0; i < lmt.getRowCount(); i++) {
						Object d = lmt.getValueAt(i, keyColIdx);
						if (d instanceof IDColumn) {
							((IDColumn) d).setSelected(i == idx);
						} else if (d instanceof UUIDColumn) {
							((UUIDColumn) d).setSelected(i == idx);
						}
					}

					// 更新 listbox 模型的选中状态
					lmt.clearSelection();
					List<Object> single = new ArrayList<>();
					single.add(lmt.getElementAt(idx));
					lmt.setSelection(single);

					// 更新 recordSelectedData，确保 enableButtons() 能读到正确的选中状态
					recordSelectedData.clear();
					recordSelectedData.put(rowKey, (List<Object>) lmt.get(rowIndex));

					// 修复：同步更新 m_rowSelectionOrder，否则 getLastSelectedRow() 仍返回 null
					m_rowSelectionOrder.clear();
					m_rowSelectionOrder.add(rowKey);

				} finally {
					enforcingSingleSelection = false;
				}
				break;
			}
		}
	}

	/**
	 * 重写 onEvent，在 UI 层面强制单选。
	 * 
	 * 
	 * 虽然 {@code model.setMultiple(false)} 已经让 listbox 本身是单选模式，但此方法作为兜底保障， 确保
	 * {@code recordSelectedData} 中始终只有一条记录，防止边缘情况（如程序化选中）导致多条记录被传递给流程。
	 * 
	 * 
	 * 注意：{@code InfoWindow.onEvent} 没有 throws 子句，此重写也不能声明 throws Exception。
	 */
	@Override
	public void onEvent(Event event) {
		// 防止 clearSelection()/setSelection() 触发递归
		if (enforcingSingleSelection) {
			super.onEvent(event);
			return;
		}

		// 拦截 listbox 的选中事件，强制单选
		if (event.getTarget() == contentPanel && Events.ON_SELECT.equals(event.getName())) {
			SelectEvent<?, ?> selectEvent = (SelectEvent<?, ?>) event;
			int selectedIndex = -1;
			// 兼容 Java 8+，不使用 pattern matching instanceof（Java 16+）
			if (selectEvent.getReference() instanceof Listitem) {
				Listitem li = (Listitem) selectEvent.getReference();
				selectedIndex = li.getIndex();
			}

			if (selectedIndex >= 0) {
				enforcingSingleSelection = true;
				try {
					final int idx = selectedIndex;

					// 清除所有行的 IDColumn/UUIDColumn 选中状态（更新复选框视觉渲染）
					int keyColIdx = contentPanel.getKeyColumnIndex();
					if (keyColIdx >= 0) {
						for (int i = 0; i < contentPanel.getModel().getRowCount(); i++) {
							Object data = contentPanel.getModel().getValueAt(i, keyColIdx);
							if (data instanceof IDColumn) {
								IDColumn idc = (IDColumn) data;
								idc.setSelected(i == idx);
							} else if (data instanceof UUIDColumn) {
								UUIDColumn uuc = (UUIDColumn) data;
								uuc.setSelected(i == idx);
							}
						}
					}

					// 清除 recordSelectedData，确保只有当前行的数据
					recordSelectedData.clear();

					// 用 contentPanel.getModel() 代替私有字段 model（model 是 InfoPanel 的 private 字段）
					ListModelTable lmt = (ListModelTable) contentPanel.getModel();
					lmt.clearSelection();
					List<Object> single = new ArrayList<>();
					single.add(lmt.getElementAt(idx));
					lmt.setSelection(single);

				} finally {
					enforcingSingleSelection = false;
				}
				// 用户主动点击行，清除 lastSelectedKey，避免后续刷新覆盖用户的新选择
				lastSelectedKey = null;
			}
		}
		super.onEvent(event);
	}

	/**
	 * 重写 bindInfoProcessBt，在父类评估 DisplayLogic 之前，向 infoContext 注入 IS_DIRECTOR
	 * 变量（Y/N），供各流程按钮的 DisplayLogic 使用：
	 *
	 * 参与人评审按钮：DisplayLogic = {@code @IS_DIRECTOR@='N'} 总监终审按钮：DisplayLogic =
	 * {@code @IS_DIRECTOR@='Y'} 提交终审意见按钮：DisplayLogic = {@code @IS_DIRECTOR@='Y'}
	 * 
	 */
	@Override
	protected void bindInfoProcessBt() {
		int userID = Env.getAD_User_ID(Env.getCtx());
		boolean director = DYSamplingUtil.hasJobPosition(userID, "J", "6");
		// 注入 IS_DIRECTOR 变量，供 DisplayLogic 表达式 @IS_DIRECTOR@='Y'/'N' 使用
		Env.setContext(Env.getCtx(), getWindowNo(), "IS_DIRECTOR", director ? "Y" : "N");
		super.bindInfoProcessBt();
	}

	/**
	 * 重写 enableButtons，根据选中行的明细状态和当前用户身份，精细控制各流程按钮的 enable/disable 状态。
	 */
	@Override
	protected void enableButtons() {
		// 先让父类统一处理（有选中行则 enable，无选中行则 disable）
		super.enableButtons();

		// 获取选中行的主键（dy_samplingreviewline_id）
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.size() != 1)
			return; // 未选中或多选时，不做精细控制（父类已 disable 按钮）

		int selectedId;
		try {
			selectedId = Integer.parseInt(selectedKeys.get(0).toString());
		} catch (NumberFormatException e) {
			return; // UUID 主键，无法精细控制，保持父类状态
		}
		int userID = Env.getAD_User_ID(Env.getCtx());
		boolean director = DYSamplingUtil.hasJobPosition(userID, "J", "6");

		// 查询选中明细的状态
		String lineStatus = DB.getSQLValueString(null,
				"SELECT reviewstatus FROM dy_samplingreviewline WHERE dy_samplingreviewline_id=?", selectedId);

		// 查询选中明细对应的主表 ID
		int reviewId = DB.getSQLValue(null,
				"SELECT dy_samplingreview_id FROM dy_samplingreviewline WHERE dy_samplingreviewline_id=?", selectedId);

		// 查询主表状态
		String reviewStatus = reviewId > 0 ? DB.getSQLValueString(null,
				"SELECT reviewstatus FROM dy_samplingreview WHERE dy_samplingreview_id=?", reviewId) : null;

		for (Button bt : btProcessList) {
			if (!bt.isVisible())
				continue; // 跳过已被 DisplayLogic 隐藏的按钮

			Integer processId = (Integer) bt.getAttribute(PROCESS_ID_KEY);
			if (processId == null)
				continue;

			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process == null)
				continue;

			String className = process.getClassname();
			if (className == null)
				continue; // 关联 AD_Form 的按钮（如"查看附件"）className 为 null，保持父类状态

			if ("com.hoifu.process.SamplingReviewProcess".equals(className)) {
				// 参与人看到的是"参与人评审"，总监看到的是"总监终审"
				if (director) {
					// 总监：除已关闭（CL）外任何状态均可操作
					bt.setEnabled(!"CL".equals(lineStatus));
				} else {
					// 参与人：只要不是终审（FA）或已关闭（CL），均可提交评审
					bt.setEnabled(!"FA".equals(lineStatus) && !"CL".equals(lineStatus));
				}
			} else if ("com.hoifu.process.FinalCommentProcess".equals(className)) {
				// 提交终审意见：主表未关闭，且该主表下所有明细均已终审（FA）
				boolean notClosed = !"CL".equals(reviewStatus);
				boolean allFA = allLinesFA(reviewId);
				bt.setEnabled(notClosed && allFA);
			}
		}
	}

	/**
	 * 判断指定评审单下所有明细是否均已终审（FA）。
	 * 
	 * @param reviewId dy_samplingreview 主表 ID
	 * @return true 表示所有明细均已终审
	 */
	private boolean allLinesFA(int reviewId) {
		if (reviewId <= 0)
			return false;
		int notFACount = DB.getSQLValue(null, "SELECT COUNT(1) FROM dy_samplingreviewline "
				+ "WHERE dy_samplingreview_id=? AND reviewstatus <> 'FA' AND isactive='Y'", reviewId);
		return notFACount == 0;
	}

}