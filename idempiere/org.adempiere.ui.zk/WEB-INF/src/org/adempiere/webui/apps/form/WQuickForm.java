/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *                                                                            * 
 * Contributor:                                                               * 
 *   Andreas Sumerauer                                                        * 
 *****************************************************************************/

package org.adempiere.webui.apps.form;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.util.Callback;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.IADTabbox;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.adwindow.QuickGridView;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WebEditorFactory;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.GridTabDataBinder;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.CustomizeGridViewDialog;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkforge.keylistener.Keylistener;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.Notification;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

/**
 * Quick entry form.
 * 
 * @author Logilite Technologies
 * @since Nov 03, 2017
 */
public class WQuickForm extends Window implements IQuickForm
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -5363771364595732977L;

	/** Main layout of form */
	private Borderlayout			mainLayout			= new Borderlayout();
	/** Calling ADWindowContent instance */
	private AbstractADWindowContent	adWinContent		= null;
	/** Center of {@link #mainLayout}. Grid/List view for multi record entry. */
	private QuickGridView			quickGridView		= null;
	/** Current selected grid tab of {@link #adWinContent} */
	private GridTab					gridTab;

	/** Action buttons panel. South of {@link #mainLayout} */
	private ConfirmPanel			confirmPanel		= new ConfirmPanel(true, true, false, false, false, false);
	private Button					bDelete				= confirmPanel.createButton(ConfirmPanel.A_DELETE);
	private Button					bSave				= confirmPanel.createButton("Save");
	private Button					bIgnore				= confirmPanel.createButton("Ignore");
	private Button					bCustomize			= confirmPanel.createButton("Customize");
	private Button					bUnSort				= confirmPanel.createButton("UnSort");
	private Button bNewRow = confirmPanel.createButton(ConfirmPanel.A_NEW); // 简易窗体 新建按钮

	private boolean					onlyCurrentRows		= false;

	private int						onlyCurrentDays		= 0;

	protected QuickGridView			prevQGV				= null;

	private int						windowNo;

	private boolean stayInParent;
	/* SysConfig USE_ESC_FOR_TAB_CLOSING */
	private boolean isUseEscForTabClosing = MSysConfig.getBooleanValue(MSysConfig.USE_ESC_FOR_TAB_CLOSING, false, Env.getAD_Client_ID(Env.getCtx()));

	/** 凭证头信息面板 */
	private Panel voucherHeaderPanel = null;

	/** 凭证头GridTab */
	private GridTab voucherHeaderTab = null;
	
	/** 借贷汇总面板 */  
	private org.zkoss.zul.Hbox summaryPanel = null;  
	/** 借方合计标签 */  
	private org.zkoss.zul.Label lblTotalDebit = null;  
	/** 贷方合计标签 */  
	private org.zkoss.zul.Label lblTotalCredit = null;  
	/** 差额标签 */  
	private org.zkoss.zul.Label lblDifference = null;
	/** 借贷汇总 TableModel 监听器 */  
	private javax.swing.event.TableModelListener summaryListener;
	
	/**
	 * @param winContent
	 * @param m_onlyCurrentRows
	 * @param m_onlyCurrentDays
	 */
	public WQuickForm(AbstractADWindowContent winContent, boolean m_onlyCurrentRows, int m_onlyCurrentDays)
	{
		super();

		this.setMode(Mode.POPUP);
		LayoutUtils.addSclass("quick-form", this);
		windowNo = SessionManager.getAppDesktop().registerWindow(this);
		adWinContent = winContent;
		onlyCurrentRows = m_onlyCurrentRows;
		onlyCurrentDays = m_onlyCurrentDays;
		this.gridTab = adWinContent.getADTab().getSelectedGridTab();

		// 获取凭证头信息
		if (isVoucherLineWindow()) {
			voucherHeaderTab = getVoucherHeaderTab();
		}
		
		this.quickGridView = new QuickGridView(adWinContent, gridTab, this);
		this.quickGridView.setVisible(true);
		initForm();
		gridTab.isQuickForm = true;

		gridTab.addDataStatusListener(this);

		// To maintain parent-child Quick Form
		prevQGV = adWinContent.getCurrQGV();
		adWinContent.setCurrQGV(quickGridView);
		
		addCallback(AFTER_PAGE_DETACHED, t -> adWinContent.focusToLastFocusEditor());
	}

	/**
	 * 判断是否为凭证分录窗口
	 */
	private boolean isVoucherLineWindow() {
		if (gridTab == null)
			return false;
		String tableName = gridTab.getTableName();
		return "GL_JournalLine".equals(tableName);
	}

	/**
	 * 获取凭证头GridTab
	 */
	private GridTab getVoucherHeaderTab() {
		if (adWinContent != null && adWinContent.getADTab() != null) {
			IADTabbox tabbox = adWinContent.getADTab();
			if (tabbox.getTabCount() > 0) {
				IADTabpanel tabpanel = tabbox.getADTabpanel(0);
				return tabpanel.getGridTab();
			}
		}
		return null;
	}
	
	/**
	 * 创建凭证头信息面板（两列布局）
	 */
	private void createVoucherHeaderPanel() {
		voucherHeaderPanel = new Panel();
		voucherHeaderPanel
				.setStyle("border: 1px solid #ccc; padding: 10px; margin-bottom: 5px; background-color: #f9f9f9;");

		Grid headerGrid = new Grid();
		headerGrid.setWidth("100%");

		// 4列：标签1(12%) 值1(38%) 标签2(12%) 值2(38%)
		Columns columns = new Columns();
		headerGrid.appendChild(columns);
		String[] widths = { "12%", "38%", "12%", "38%" };
		for (String width : widths) {
			Column column = new Column();
			ZKUpdateUtil.setWidth(column, width);
			columns.appendChild(column);
		}

		Rows rows = new Rows();
		headerGrid.appendChild(rows);

		addHeaderRow(rows, "会计账套", "C_AcctSchema_ID", "组织", "AD_Org_ID");
		
		// 凭证号取凭证单号-切割后的数字，只读
		String documentNo = voucherHeaderTab != null ? (String) voucherHeaderTab.getValue("DocumentNo") : null;
		String lastNo = "";
		if (documentNo != null && !documentNo.isEmpty()) {
			String[] parts = documentNo.split("-");
			lastNo = parts[parts.length - 1].replaceFirst("^0+", "");
			if (lastNo.isEmpty())
				lastNo = "0"; // 防止全是0的情况变成空字符串
		}
		addHeaderRowWithReadonlyValue(rows, "凭证字", "Gl_VoucherType", "凭证号", lastNo);
		addHeaderRow(rows, "凭证日期", "DateDoc", "附件张数", "AttachmentCount");

		voucherHeaderPanel.appendChild(headerGrid);

		North north = new North();
		north.appendChild(voucherHeaderPanel);
		mainLayout.insertBefore(north, mainLayout.getCenter());
	}

	/**
	 * 添加两列字段行（标签1 值1 标签2 值2）
	 */
	private void addHeaderRow(Rows rows, String label1, String col1, String label2, String col2) {
		Row row = new Row();
		Label lbl1 = new Label(label1 + ":");
		lbl1.setStyle("font-weight: bold; white-space: nowrap;");
		row.appendChild(lbl1);
		row.appendChild(createEditorComponent(col1));

		Label lbl2 = new Label(label2 + ":");
		lbl2.setStyle("font-weight: bold; white-space: nowrap;");
		row.appendChild(lbl2);
		row.appendChild(createEditorComponent(col2));

		rows.appendChild(row);
	}

    /**
     * 添加两列字段行，右侧值为自定义只读文本（不绑定字段）
     */
    private void addHeaderRowWithReadonlyValue(Rows rows, String label1, String col1, String label2, String customValue) {
        Row row = new Row();
        Label lbl1 = new Label(label1 + ":");
        lbl1.setStyle("font-weight: bold; white-space: nowrap;");
        row.appendChild(lbl1);
        row.appendChild(createEditorComponent(col1));

        Label lbl2 = new Label(label2 + ":");
        lbl2.setStyle("font-weight: bold; white-space: nowrap;");
        row.appendChild(lbl2);

        Textbox txt = new Textbox(customValue != null ? customValue : "");
        txt.setReadonly(true);
        ZKUpdateUtil.setWidth(txt, "100%");
        row.appendChild(txt);

        rows.appendChild(row);
    }
	
	/**
	 * 添加跨两列的字段行（标签 + 值跨3列）
	 */
	private void addHeaderRowFull(Rows rows, String label, String columnName) {
		Row row = new Row();
		Label lbl = new Label(label + ":");
		lbl.setStyle("font-weight: bold; white-space: nowrap;");
		row.appendChild(lbl);

		Cell cell = new Cell();
		cell.setColspan(3);
		cell.appendChild(createEditorComponent(columnName));
		row.appendChild(cell);

		rows.appendChild(row);
	}

	/**
	 * 创建字段编辑器组件（含数据绑定）
	 */
	private Component createEditorComponent(String columnName) {
		if (voucherHeaderTab == null)
			return new Label("");

		GridField field = voucherHeaderTab.getField(columnName);
		if (field == null) {
			Object value = voucherHeaderTab.getValue(columnName);
			return new Label(value != null ? value.toString() : "");
		}

		WEditor editor = WebEditorFactory.getEditor(field, false);
		if (editor == null) {
			Object value = voucherHeaderTab.getValue(columnName);
			return new Label(value != null ? value.toString() : "");
		}

		// 关键步骤1：设置当前值（没有这步编辑器是空的）
		Object value = voucherHeaderTab.getValue(columnName);
		editor.setValue(value);

		// 关键步骤2：绑定属性变更监听（字段值变化时自动刷新编辑器显示）
		field.addPropertyChangeListener(editor);

		// 关键步骤3：触发动态显示（处理只读/必填等状态）
		editor.dynamicDisplay();

		boolean isEditable = field.isEditable(true);
		editor.setReadWrite(isEditable);
		editor.fillHorizontal();

	    // GridTabDataBinder 会调用 mTable.setValueAt()，正确触发 callout  
	    editor.addValueChangeListener(new GridTabDataBinder(voucherHeaderTab));  

		return editor.getComponent();
	}

	/**
	 * Initialize form.
	 */
	protected void initForm( )
	{
		initZk();
		
		// 如果是凭证分录窗口，添加凭证头信息
		if (isVoucherLineWindow() && voucherHeaderTab != null) {
			createVoucherHeaderPanel();
		}
		
		createNewRow();
		quickGridView.refresh(gridTab);
	    // 启用弹性拉伸模式（修复空白行时的不自适应问题）
	    quickGridView.setStretchMode(true);

		// 如果已有数据，跳转到最后一页
		if (isVoucherLineWindow() && gridTab.getRowCount() > 0) {
			int lastRow = gridTab.getRowCount() - 1;
			gridTab.navigate(lastRow);
			quickGridView.updateListIndex();
		}

		// 统一注册一次，所有数据变化自动触发汇总更新
		if (isVoucherLineWindow()) {
			summaryListener = e -> {
				if (Executions.getCurrent() != null && summaryPanel != null) {
					updateSummaryPanel();
				}
			};
			gridTab.getTableModel().addTableModelListener(summaryListener);
			updateSummaryPanel(); // 初始化时手动触发一次（此时监听器刚注册，不会重复）
		}

	    // 改成触发事件：  
	    Events.echoEvent("onFocusToDescription", this, null);  
	}

	/**
	 * Layout form.
	 */
	private void initZk( )
	{
		// Center
		Panel Center = new Panel();
		Center.appendChild(quickGridView);

		// South
		Panel south = new Panel();

		if (isVoucherLineWindow()) {
			// 状态栏和汇总面板同行：状态栏靠左，汇总面板靠右
			org.zkoss.zul.Hbox statusRow = new org.zkoss.zul.Hbox();
			statusRow.setWidth("100%");
			statusRow.setStyle("display: flex; align-items: center;");

			// 状态栏靠左，占据剩余空间
			ZKUpdateUtil.setHflex(adWinContent.getStatusBarQF(), "1");
			statusRow.appendChild(adWinContent.getStatusBarQF());

			// 汇总面板靠右，自适应宽度
			summaryPanel = createSummaryPanel();
			statusRow.appendChild(summaryPanel);

			south.appendChild(statusRow);
		} else {
			south.appendChild(adWinContent.getStatusBarQF());
		}

		// Adding statusBar for Quick Form
//		south.appendChild(adWinContent.getStatusBarQF());
		south.appendChild(confirmPanel);

		bSave.setEnabled(!gridTab.isReadOnly());
		bDelete.setEnabled(!gridTab.isReadOnly());
		bIgnore.setEnabled(!gridTab.isReadOnly());
		bUnSort.setEnabled(!gridTab.isReadOnly());

		bSave.addEventListener(Events.ON_CLICK, this);
		bDelete.addEventListener(Events.ON_CLICK, this);
		bIgnore.addEventListener(Events.ON_CLICK, this);
		bCustomize.addEventListener(Events.ON_CLICK, this);
		bUnSort.addEventListener(Events.ON_CLICK, this);
		
		bNewRow.setEnabled(!gridTab.isReadOnly());
		bNewRow.addEventListener(Events.ON_CLICK, this);

		Button bRefresh = confirmPanel.getButton(ConfirmPanel.A_REFRESH);
		Button bCancel = confirmPanel.getButton(ConfirmPanel.A_CANCEL);
		Button bOk = confirmPanel.getButton(ConfirmPanel.A_OK);

		// Set tool-tip information
		bSave.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormSave")); // 'Alt + S'
		bDelete.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormDelete")); // 'Alt + D'
		bIgnore.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormIgnore")); // 'Alt + Z'
		bUnSort.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormUnSort")); // 'Alt + R'
		bCustomize.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormCustomize")); // 'Alt + L'
		bOk.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormOk")); // 'Alt + K' - Save_Close
		bCancel.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormCancel")); // 'Alt + X'
		bRefresh.setTooltiptext(Msg.translate(Env.getCtx(), "QuickFormRefresh")); // 'Alt + E'

		confirmPanel.addComponentsLeft(bSave);
		confirmPanel.addComponentsLeft(bDelete);
		confirmPanel.addComponentsLeft(bIgnore);
		confirmPanel.addComponentsLeft(bCustomize);
		confirmPanel.addComponentsLeft(bUnSort);
		confirmPanel.addComponentsLeft(bNewRow);
		confirmPanel.addActionListener(this);

		mainLayout.appendCenter(Center);
		mainLayout.appendSouth(south);

		this.appendChild(mainLayout);

		// 注册光标聚焦事件监听器
		this.addEventListener("onFocusToDescription", this);
	} // initZk

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_CANCEL))
		{
			onCancel();
		}
		else if (event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_REFRESH))
		{
			quickGridView.getRenderer().setCurrentCell(null);
			onRefresh();
		}
		if (event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_OK))
		{
			onSave();
			dispose();
		}
		else if (event.getTarget() == confirmPanel.getButton("Save"))
		{
			quickGridView.getRenderer().setCurrentCell(null);
			onSave();
		}
		else if (event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_DELETE))
		{
			quickGridView.getRenderer().setCurrentCell(null);
			onDelete();
		}
		else if (event.getTarget() == confirmPanel.getButton("Ignore"))
		{
			quickGridView.getRenderer().setCurrentCell(null);
			onIgnore();
		}
		else if (event.getTarget() == confirmPanel.getButton("Customize"))
		{
			onCustomize();
		}
		else if (event.getTarget() == confirmPanel.getButton("UnSort"))
		{
			onUnSort();
		}
		else if (event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_NEW)) {
			onNewRow();
		}
		// 光标聚焦到分录信息的描述字段
		else if ("onFocusToDescription".equals(event.getName())) {
			GridField[] fields = quickGridView.getGridField();
			int descColIndex = -1;
			if (fields != null) {
				for (int i = 0; i < fields.length; i++) {
					if ("Description".equalsIgnoreCase(fields[i].getColumnName())) {
						descColIndex = i + 1; // +1 是因为第一列是 Checkbox
						break;
					}
				}
			}
			if (descColIndex > 0) {
				quickGridView.getRenderer().setCurrentCell(0, descColIndex, QuickGridView.NAVIGATE_CODE);
			} else {
				// 降级：聚焦到第一个数据列
				Events.echoEvent(QuickGridView.EVENT_ON_SET_FOCUS_TO_FIRST_CELL, quickGridView, null);
			}
		}
		
		event.stopPropagation();
	} // onEvent

	public void onNewRow() {
		if (!gridTab.isInsertRecord()) {
			adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "NewError"), true);
			return;
		}

		// 处理未保存的行
		if (!quickGridView.isNewLineSaved) {
			int rowChanged = gridTab.getTableModel().getRowChanged();
			if (rowChanged == gridTab.getCurrentRow()) {
				// 有变更，尝试保存
				if (!quickGridView.dataSave(0)) {
					// 保存失败，丢弃当前行
					gridTab.dataIgnore();
				}
				// dataSave 成功时内部已设置 isNewLineSaved = true
				// 不要再调用 dataRefreshAll()，否则会重置 renderer 状态导致混乱
			} else {
				// 无变更，丢弃空白行
				gridTab.dataIgnore();
			}
			quickGridView.isNewLineSaved = true;
		}

		// 先移动光标到最后一行的第一列
		int lastRowIndex = gridTab.getRowCount() - 1;
		if (lastRowIndex >= 0) {
			quickGridView.updateModelIndex(lastRowIndex);
			quickGridView.getRenderer().setCurrentCell(lastRowIndex, 1, QuickGridView.NAVIGATE_CODE);

			// 移动光标后强制刷新数据模型，确保数据正确显示
			gridTab.dataRefreshAll();
		}
		// 新增新行
		quickGridView.createNewLine();

		// 手动更新 renderer.currentRowIndex 为新行的索引
		quickGridView.getRenderer().currentRowIndex = gridTab.getCurrentRow();

		quickGridView.updateListIndex();
		quickGridView.toggleSelectionForAll(false);
		Events.echoEvent(QuickGridView.EVENT_ON_SET_FOCUS_TO_FIRST_CELL, quickGridView, null);
	}
	
	/**
	 * Cancel/Close form.
	 */
	@Override
	public void onCancel( )
	{
		// 如果 title 是"凭证分录"，改为调用删除逻辑
		if ("凭证分录".equals(getTitle())) {
			javax.swing.table.TableModel tableModel = gridTab.getTableModel();
			// 是否有分录数据
			boolean hasDBRows = java.util.stream.IntStream.range(0, tableModel.getRowCount())
					.anyMatch(i -> gridTab.getKeyID(i) > 0);

			if (!hasDBRows) {
				voucherHeaderTab.dataDelete(); // 不显示确认弹窗
				dispose();
				return;
			}
		}

		if (gridTab.getTableModel().getRowChanged() > -1)
		{
			Dialog.ask(windowNo, "SaveChanges?", new Callback <Boolean>() {

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
						onSave();
					dispose();
				}
			});
		}
		else
		{
			dispose();
		}
	} // onCancel

	/**
	 * Reset sort state
	 */
	@Override
	public void onUnSort( )
	{
		adWinContent.getActiveGridTab().getTableModel().resetCacheSortState();
		Column sortColumn = quickGridView.findCurrentSortColumn();

		onRefresh();

		if (sortColumn != null)
			sortColumn.setSortDirection("natural");

		adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "UnSort"), false);
	} // onUnSort

	/**
	 * Open {@link CustomizeGridViewDialog} for {@link #quickGridView}.
	 */
	@Override
	public void onCustomize( )
	{
		onSave();
		//
		Columns columns = quickGridView.getListbox().getColumns();
		List <Component> columnList = columns.getChildren();
		GridField[] fields = quickGridView.getGridField();
		Map <Integer, String> columnsWidth = new HashMap <Integer, String>();
		ArrayList <Integer> gridFieldIds = new ArrayList <Integer>();

		for (int i = 0; i < fields.length; i++)
		{
			Column column = (Column) columnList.get(i + 1);
			String width = column.getWidth();
			columnsWidth.put(fields[i].getAD_Field_ID(), width);
			gridFieldIds.add(fields[i].getAD_Field_ID());
		}

		ZKUpdateUtil.setWidth(quickGridView, getWidth());
		ZKUpdateUtil.setHeight(quickGridView, getHeight());

		CustomizeGridViewDialog.showCustomize(0, gridTab.getAD_Tab_ID(), columnsWidth, gridFieldIds, null, quickGridView, true, null);
	} // onCustomize

	/**
	 * Ignore/Undo changes
	 */
	@Override
	public void onIgnore( )
	{
		gridTab.dataIgnore();
		gridTab.dataRefreshAll();
		adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "Ignored"), false);
		quickGridView.isNewLineSaved = true;
		// Create new record if no record present.
		if (gridTab.getRowCount() <= 0)
			createNewRow();
		quickGridView.updateListIndex();
		Events.echoEvent(QuickGridView.EVENT_ON_SET_FOCUS_TO_FIRST_CELL, quickGridView, null);
	} // onIgnore

	/**
	 * Delete selected rows.
	 */
	@Override
	public void onDelete( )
	{
		if (gridTab == null || !quickGridView.isNewLineSaved)
			return;

		// if no any row selected then delete current record
		if (gridTab.getSelection().length == 0)
			gridTab.addToSelection(quickGridView.getRenderer().getCurrentRowIndex());

		final int[] indices = gridTab.getSelection();
		if (indices.length > 0)
		{
			Dialog.ask(windowNo, "DeleteRecord?", new Callback <Boolean>() {

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						boolean isAllSelected = quickGridView.isAllSelected();
						quickGridView.isNewLineSaved = true;
						gridTab.clearSelection();
						quickGridView.toggleSelectionForAll(false);
						Arrays.sort(indices);
						int count = 0;
						for (int i = 0, offset = 0; i < indices.length; i++)
						{
							gridTab.navigate(indices[i] - offset);
							if (gridTab.dataDelete())
							{
								offset++;
								count++;
							}
							else
							{
								break;
							}
						}

						adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "Deleted") + ": " + count + " / " + indices.length, false);

						// if all records is deleted then it will show default with new record.
						if (gridTab.getRowCount() <= 0)
							quickGridView.createNewLine();
						quickGridView.updateListIndex();

						// Set focus on the first row if all Row's are selected.
						if (isAllSelected)
							Events.echoEvent(QuickGridView.EVENT_ON_PAGE_NAVIGATE, quickGridView, null);
						else
							Events.echoEvent(QuickGridView.EVENT_ON_SET_FOCUS_TO_FIRST_CELL, quickGridView, null);
					}
					else
					{
						gridTab.clearSelection();
						quickGridView.toggleSelectionForAll(false);
					}
				}
			});
		}
	} // onDelete

	/**
	 * Save {@link #quickGridView} changes.
	 */
	@Override
	public void onSave( )
	{
		// 验证借贷平衡
		validateDebitCreditBalance();
		
		// 先保存凭证头（如果有修改）  
	    if (voucherHeaderTab != null) {  
	        voucherHeaderTab.dataSave(false);  
	    }  
		
		if (gridTab.getTableModel().getRowChanged() == gridTab.getCurrentRow())
		{
			if (quickGridView.dataSave(0))
			{
				gridTab.dataRefreshAll();
				adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "Saved"), false);
				Events.echoEvent(QuickGridView.EVENT_ON_SET_FOCUS_TO_FIRST_CELL, quickGridView, null);
			}
		}
		else
		{
			onIgnore();
		}
	} // onSave

	/**
	 * Refresh {@link #gridTab} and {@link #quickGridView}.
	 */
	@Override
	public void onRefresh( )
	{
		gridTab.dataRefreshAll();
		adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "Refresh"), false);
		quickGridView.isNewLineSaved = true;
		quickGridView.updateListIndex();
		Events.echoEvent(QuickGridView.EVENT_ON_SET_FOCUS_TO_FIRST_CELL, quickGridView, null);
		// Create new record if no record present.
		if (gridTab.getRowCount() <= 0)
			createNewRow();
	} // onRefresh

	/**
	 * Close form.
	 */
	@Override
	public void dispose( )
	{
		// 移除汇总监听器
		if (summaryListener != null) {
			gridTab.getTableModel().removeTableModelListener(summaryListener);
			summaryListener = null;
		}

		super.dispose();

		// do not allow to close tab for Events.ON_CTRL_KEY event
		if(isUseEscForTabClosing)
			SessionManager.getAppDesktop().setCloseTabWithShortcut(false);
		
		gridTab.setQuickForm(false);
		onIgnore();
		gridTab.removeDataStatusListener(this);
		adWinContent.closeQuickFormTab(gridTab.getAD_Tab_ID());
		quickGridView.getRenderer().clearMaps();
		int tabLevel = adWinContent.getToolbar().getQuickFormTabHrchyLevel();
		if (tabLevel > 0)
		{
			adWinContent.getToolbar().setQuickFormTabHrchyLevel(tabLevel - 1);
			Keylistener keyListener = SessionManager.getSessionApplication().getKeylistener();
			keyListener.setCtrlKeys(keyListener.getCtrlKeys() + QuickGridView.CNTRL_KEYS);

			// Add Key-listener of parent Quick Form
			if (prevQGV != null)
			{
				adWinContent.onParentRecord();
				SessionManager.getSessionApplication().getKeylistener().addEventListener(Events.ON_CTRL_KEY, prevQGV);
				// need to set focus on last focused row of parent Form.
				Events.echoEvent(QuickGridView.EVENT_ON_PAGE_NAVIGATE, prevQGV, null);
			}
			adWinContent.setCurrQGV(prevQGV);
		}
		else
		{
			adWinContent.setCurrQGV(null);
		}
		adWinContent.getADTab().getSelectedTabpanel().query(onlyCurrentRows, onlyCurrentDays, adWinContent.getADTab().getSelectedTabpanel().getGridTab().getMaxQueryRecords()); // autoSize

		if (stayInParent) {
			adWinContent.onParentRecord();
		}
	} // dispose

	/**
	 * Add new row to {@link #quickGridView}.
	 */
	private void createNewRow( )
	{
		int row = gridTab.getRowCount();
		// creating the first record from the parent tab first record is duplicated on KEY DOWN.
		// If a grid does not have a record or blank record then create a new row.
		if (row <= 0 || (gridTab.isNew() && row == 1))
		{
			gridTab.dataIgnore();
			if (gridTab.isInsertRecord())
			{
				quickGridView.createNewLine();
			}
			else
			{
				adWinContent.getStatusBarQF().setStatusLine(Msg.getMsg(Env.getCtx(), "NewError"), true);
			}
		}
	} // createNewRow

	@Override
	public void dataStatusChanged(DataStatusEvent e)
	{
		// ignore background event
		if (Executions.getCurrent() == null || e.isInitEdit())
			return;

		// update Dynamic display on data Status change.
		int col = e.getChangedColumn();
		quickGridView.dynamicDisplay(col);
	} // dataStatusChanged

	/**
	 * If stayInParent is true, {@link #adWinContent} should navigate to parent record after closing this form instance.
	 * @param stayInParent
	 */
	public void setStayInParent(boolean stayInParent) {
		this.stayInParent = stayInParent;
	}

	/**
	 * 验证借贷平衡
	 * 
	 * @return true if balanced, false otherwise
	 */
	private boolean validateDebitCreditBalance() {
		if (!isVoucherLineWindow()) {
			return true;
		}

		BigDecimal totalDebit = BigDecimal.ZERO;
		BigDecimal totalCredit = BigDecimal.ZERO;

		// 直接从 TableModel 读取，不改变当前行
		javax.swing.table.TableModel tableModel = gridTab.getTableModel();
		int drColIndex = -1, crColIndex = -1;
		for (int c = 0; c < tableModel.getColumnCount(); c++) {
			String colName = tableModel.getColumnName(c);
			if ("AmtSourceDr".equals(colName))
				drColIndex = c;
			if ("AmtSourceCr".equals(colName))
				crColIndex = c;
		}

		if (drColIndex >= 0 && crColIndex >= 0) {
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				BigDecimal amtSourceDr = (BigDecimal) tableModel.getValueAt(i, drColIndex);
				BigDecimal amtSourceCr = (BigDecimal) tableModel.getValueAt(i, crColIndex);
				if (amtSourceDr != null) {
					totalDebit = totalDebit.add(amtSourceDr);
				}
				if (amtSourceCr != null) {
					totalCredit = totalCredit.add(amtSourceCr);
				}
			}
		}

		BigDecimal difference = totalDebit.subtract(totalCredit);
		if (difference.compareTo(BigDecimal.ZERO) != 0) {
			Notification.show("借贷不平！借方合计：" + totalDebit + "，贷方合计：" + totalCredit + "，差额: " + difference, "error", adWinContent.getComponent(), "top_left", 3500, true);
			return false;
		}

		return true;
	}

	/**
	 * 创建借贷汇总面板
	 * @Title: createSummaryPanel
	 * @return
	 * @return org.zkoss.zul.Hbox
	 */
	private org.zkoss.zul.Hbox createSummaryPanel() {
		org.zkoss.zul.Hbox hbox = new org.zkoss.zul.Hbox();
		hbox.setStyle("padding: 4px 8px; white-space: nowrap;");
		hbox.setAlign("center");

		org.zkoss.zul.Label lbl1 = new org.zkoss.zul.Label("借方合计：");
		lbl1.setStyle("font-weight: bold; margin-right: 4px;");
		lblTotalDebit = new org.zkoss.zul.Label("0.00");
		lblTotalDebit.setStyle("color: #333; margin-right: 20px; min-width: 100px;");

		org.zkoss.zul.Label lbl2 = new org.zkoss.zul.Label("贷方合计：");
		lbl2.setStyle("font-weight: bold; margin-right: 4px;");
		lblTotalCredit = new org.zkoss.zul.Label("0.00");
		lblTotalCredit.setStyle("color: #333; margin-right: 20px; min-width: 100px;");

		org.zkoss.zul.Label lbl3 = new org.zkoss.zul.Label("差额：");
		lbl3.setStyle("font-weight: bold; margin-right: 4px;");
		lblDifference = new org.zkoss.zul.Label("0.00");
		lblDifference.setStyle("min-width: 100px;");

		hbox.appendChild(lbl1);
		hbox.appendChild(lblTotalDebit);
		hbox.appendChild(lbl2);
		hbox.appendChild(lblTotalCredit);
		hbox.appendChild(lbl3);
		hbox.appendChild(lblDifference);

		return hbox;
	}

	/**
	 * 更新借贷汇总面板显示
	 * 
	 * @Title: updateSummaryPanel
	 * @return void
	 */
	public void updateSummaryPanel() {
		if (!isVoucherLineWindow() || summaryPanel == null)
			return;

		java.math.BigDecimal totalDebit = java.math.BigDecimal.ZERO;
		java.math.BigDecimal totalCredit = java.math.BigDecimal.ZERO;

		// 直接从 TableModel 读取，不改变当前行
		javax.swing.table.TableModel tableModel = gridTab.getTableModel();
		int drColIndex = -1, crColIndex = -1;
		for (int c = 0; c < tableModel.getColumnCount(); c++) {
			String colName = tableModel.getColumnName(c);
			if ("AmtSourceDr".equals(colName))
				drColIndex = c;
			if ("AmtSourceCr".equals(colName))
				crColIndex = c;
		}

		if (drColIndex >= 0 && crColIndex >= 0) {
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				java.math.BigDecimal dr = (java.math.BigDecimal) tableModel.getValueAt(i, drColIndex);
				java.math.BigDecimal cr = (java.math.BigDecimal) tableModel.getValueAt(i, crColIndex);
				if (dr != null)
					totalDebit = totalDebit.add(dr);
				if (cr != null)
					totalCredit = totalCredit.add(cr);
			}
		}

		java.math.BigDecimal diff = totalDebit.subtract(totalCredit);
		java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0.00");
		lblTotalDebit.setValue(fmt.format(totalDebit));
		lblTotalCredit.setValue(fmt.format(totalCredit));
		lblDifference.setValue(fmt.format(diff));

		if (diff.compareTo(java.math.BigDecimal.ZERO) != 0) {
			lblDifference.setStyle("color: red; font-weight: bold; min-width: 100px;");
		} else {
			lblDifference.setStyle("color: green; font-weight: bold; min-width: 100px;");
		}
	}
}
