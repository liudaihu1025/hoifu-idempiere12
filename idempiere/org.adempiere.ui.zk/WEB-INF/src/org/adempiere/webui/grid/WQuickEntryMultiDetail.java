package org.adempiere.webui.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.ADWindowContent;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WebEditorFactory;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.MField;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Separator;

/**
 * Quick Entry Window with support for multiple detail rows.
 * 
 * <p>
 * Header Tab (TabLevel=0) keeps the original label+editor layout. Detail Tabs
 * (TabLevel>0) use a ZK Grid table with add/remove row support.
 * </p>
 * 
 * <p>
 * <b>Constructor note:</b> Uses {@code super(AD_Window_ID)} intentionally. The
 * 3-arg WQuickEntry constructor calls {@link #initPOs()} inside its body, but
 * Java initializes subclass instance fields AFTER the superclass constructor
 * returns — so the overridden initPOs() would see null Maps and throw NPE. We
 * use the 1-arg constructor (which does NOT call initPOs()), then manually call
 * initLayout() and initPOs() once subclass fields are ready.
 * </p>
 */
public class WQuickEntryMultiDetail extends WQuickEntry {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(WQuickEntryMultiDetail.class);

	// ── Detail Tab data structures ─────────────────────────────────────────────

	/** Detail Tab column field definitions (column order = table header order) */
	private final Map<GridTab, List<GridField>> detailTabFields = new LinkedHashMap<>();

	/**
	 * Detail Tab row editors: Tab → [row0:[ed0,ed1,...], row1:[ed0,ed1,...], ...]
	 */
	private final Map<GridTab, List<List<WEditor>>> detailRowEditors = new LinkedHashMap<>();

	/** Detail Tab row POs: Tab → [PO0, PO1, ...] */
	private final Map<GridTab, List<PO>> detailRowPOs = new LinkedHashMap<>();

	/** Detail Tab ZK Grid Rows component (for dynamic row append/remove) */
	private final Map<GridTab, Rows> detailGridRows = new LinkedHashMap<>();

	/** Parent column name derived from header tab, e.g. "C_Order_ID" */
	private String parentColumnName;

	/** 待删除的明细行 PO，点击 OK 时才真正删除数据库记录 */
	private final Map<GridTab, List<PO>> detailRowPOsToDelete = new LinkedHashMap<>();

	// ── Constructors ───────────────────────────────────────────────────────────

	/**
	 * Full constructor. See class-level Javadoc for why super(AD_Window_ID) is
	 * used.
	 */
	public WQuickEntryMultiDetail(int WindowNo, int TabNo, int AD_Window_ID) {
		super(AD_Window_ID); // sets m_AD_Window_ID only; does NOT call initPOs()

		parent_WindowNo = WindowNo;
		parent_TabNo = TabNo;
		m_WindowNo = SessionManager.getAppDesktop().registerWindow(this);

		try {
			initLayout();
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage());
		}

		Env.setContext(Env.getCtx(), m_WindowNo, QUICK_ENTRY_MODE, "Y");
		Env.setContext(Env.getCtx(), m_WindowNo, QUICK_ENTRY_CALLER_WINDOW, parent_WindowNo);
		Env.setContext(Env.getCtx(), m_WindowNo, QUICK_ENTRY_CALLER_TAB, parent_TabNo);
		initPOs();
		adjustDialogWidth();
		ZKUpdateUtil.setWindowHeightX(this, 600);

		if (log.isLoggable(Level.INFO))
			log.info("R/O=" + isReadOnly());
	}

	/**
	 * Lightweight constructor for field-count checks only (no UI initialization).
	 * Mirrors WQuickEntry(int AD_Window_ID).
	 */
	public WQuickEntryMultiDetail(int AD_Window_ID) {
		super(AD_Window_ID);
	}

	private void adjustDialogWidth() {
		// 主表：每行2个字段，每个字段（label+editor）约 300px
		int headerFieldCount = quickFields.size();
		int headerWidth = Math.max(1, (headerFieldCount + 1) / 2) * 300 + 60;

		// 明细：每列约 200px，删除按钮 80px，内边距 40px
		int maxDetailCols = detailTabFields.values().stream().mapToInt(List::size).max().orElse(0);
		int detailWidth = maxDetailCols * 200 + 100 + 40;

		int dialogWidth = Math.max(900, Math.min(Math.max(headerWidth, detailWidth), 1600));
		ZKUpdateUtil.setWindowWidthX(this, dialogWidth);
		ZKUpdateUtil.setWindowHeightX(this, 500);
	}

	// ── initPOs ────────────────────────────────────────────────────────────────

	@Override
	protected void initPOs() {
		GridWindow gridwindow = GridWindow.get(Env.getCtx(), m_WindowNo, m_AD_Window_ID);
		this.setTitle(gridwindow.getName());

		boolean newTab = false;
		for (int i = 0; i < gridwindow.getTabCount(); i++) {
			GridTab gridtab = gridwindow.getTab(i);

			if (i == 0) {
				m_readOnly = !MRole.getDefault().canUpdate(Env.getAD_Client_ID(Env.getCtx()),
						Env.getAD_Org_ID(Env.getCtx()), gridtab.getAD_Table_ID(), 0, false);
				parentColumnName = gridtab.getTableName() + "_ID";
			}

			if (!gridtab.isLoadComplete())
				gridwindow.initTab(i);

			if (gridtab.getTabLevel() == 0) {
				// ── Header Tab: 两列 Grid 布局 ──────────────────────────────────
				List<WEditor> headerEditors = new ArrayList<>();

				for (GridField gridfield : gridtab.getFields()) {
					MField field = MField.get(Env.getCtx(), gridfield.getAD_Field_ID());
					if (!field.isQuickEntry())
						continue;
					if (!isValidQuickEntryType(field.getAD_Reference_ID()))
						continue;
					WEditor editor = WebEditorFactory.getEditor(gridfield, false);
					if (isReadOnly())
						editor.setReadWrite(false);
					if (gridfield.isMandatory(false))
						editor.setMandatory(true);
					headerEditors.add(editor);
					quickFields.add(gridfield);
					quickEditors.add(editor);
					editor.addValueChangeListener(this);
					if (!quickTabs.contains(gridtab))
						quickTabs.add(gridtab);
					isHasField = true;
					newTab = false;
				}

				if (!headerEditors.isEmpty()) {
					// 4列：label1 | editor1 | label2 | editor2
					Grid headerGrid = new Grid();
					headerGrid.setSclass("quick-entry-header-grid");
					ZKUpdateUtil.setWidth(headerGrid, "100%");
					headerGrid.setStyle("text-align:left;");

					Columns headerCols = new Columns();
					headerGrid.appendChild(headerCols);
					Column lc1 = new Column();
					lc1.setWidth("100%");
					headerCols.appendChild(lc1);
					Column ec1 = new Column();
					ec1.setHflex("1");
					headerCols.appendChild(ec1);
					Column lc2 = new Column();
					lc2.setWidth("100%");
					headerCols.appendChild(lc2);
					Column ec2 = new Column();
					ec2.setHflex("1");
					headerCols.appendChild(ec2);

					Rows headerRows = new Rows();
					headerGrid.appendChild(headerRows);

					Row currentRow = null;
					for (int fi = 0; fi < headerEditors.size(); fi++) {
						if (fi % 2 == 0) {
							currentRow = new Row();
							currentRow.setStyle("vertical-align:middle;");
							headerRows.appendChild(currentRow);
						}
						WEditor editor = headerEditors.get(fi);
						Label label = editor.getLabel();
						label.setSclass("field-label");
						label.setStyle("text-align:right; padding-right:6px;");
						currentRow.appendChild(label);
						HtmlBasedComponent comp = (HtmlBasedComponent) editor.getComponent();
						ZKUpdateUtil.setHflex(comp, "1");
						comp.setStyle("max-width: 220px;"); // ← 新增这行
						currentRow.appendChild(comp);
					}
					// 奇数个字段时补空单元格
					if (headerEditors.size() % 2 != 0 && currentRow != null) {
						currentRow.appendChild(new org.zkoss.zul.Label(""));
						currentRow.appendChild(new org.zkoss.zul.Label(""));
					}
					centerPanel.appendChild(headerGrid);
				}
			} else {
				// ── Detail Tab: ZK Grid table layout ─────────────────────────
				List<GridField> tabFields = new ArrayList<>();
				for (GridField gridfield : gridtab.getFields()) {
					MField field = MField.get(Env.getCtx(), gridfield.getAD_Field_ID());
					if (field.isQuickEntry() && isValidQuickEntryType(field.getAD_Reference_ID()))
						tabFields.add(gridfield);
				}

				if (!tabFields.isEmpty()) {
					detailTabFields.put(gridtab, tabFields);
					detailRowEditors.put(gridtab, new ArrayList<>());
					detailRowPOs.put(gridtab, new ArrayList<>());
					detailRowPOsToDelete.put(gridtab, new ArrayList<>()); // ← 新增

					// Tab separator + label
					centerPanel.appendChild(new Separator());
					centerPanel.appendChild(new Label(gridtab.getName()));
					Separator bar = new Separator();
					bar.setBar(true);
					centerPanel.appendChild(bar);

					// Grid
					Grid grid = new Grid();
					grid.setSclass("quick-entry-detail-grid");
					ZKUpdateUtil.setWidth(grid, "100%");

					// Column headers
					Columns columns = new Columns();
					grid.appendChild(columns);
					for (GridField gf : tabFields) {
						Column col = new Column(gf.getHeader());
						col.setHflex("1"); // 各字段列等宽自适应
						columns.appendChild(col);
					}
					Column opCol = new Column("操作"); // "删除"
					opCol.setHflex("min"); // 按钮内容自适应，不截断
					columns.appendChild(opCol);

					// Rows container
					Rows rows = new Rows();
					grid.appendChild(rows);
					detailGridRows.put(gridtab, rows);
//					centerPanel.appendChild(grid);

					// "Add Row" button
					final GridTab finalTab = gridtab;
					org.adempiere.webui.component.Button btnAdd = new org.adempiere.webui.component.Button(
							Msg.getMsg(Env.getCtx(), "New") + "" + gridtab.getName());
					btnAdd.setSclass("quick-entry-add-row-btn");
					btnAdd.addEventListener(Events.ON_CLICK, e -> addDetailRow(finalTab));
//					centerPanel.appendChild(btnAdd);

					org.zkoss.zul.Div scrollDiv = new org.zkoss.zul.Div();
					scrollDiv.setStyle("overflow: auto; max-height: 380px; border: 1px solid #ddd;");
					ZKUpdateUtil.setWidth(scrollDiv, "100%");
					scrollDiv.appendChild(grid);
					centerPanel.appendChild(scrollDiv); // 可滚动的表格区域
					// 将新建按钮添加到 confirmPanel 左侧，与 OK/Cancel 并列
					org.adempiere.webui.component.ConfirmPanel cp = (org.adempiere.webui.component.ConfirmPanel) this
							.getFellow("confirmPanel");
					cp.addComponentsLeft(btnAdd);

					isHasField = true;
					addDetailRow(gridtab); // default first empty row
				}
			}
			newTab = true;
		}
	}

	// ── Detail row helpers ─────────────────────────────────────────────────────

	/** Add a new empty row to the detail Grid. */
	private void addDetailRow(GridTab detailTab) {
		PO newPO = MTable.get(Env.getCtx(), detailTab.getTableName()).getPO(0, null);
		appendDetailRow(detailTab, newPO, true);
	}

	/** Add a row pre-filled with an existing PO (Update mode). */
	private void addDetailRowWithPO(GridTab detailTab, PO existingPO) {
		appendDetailRow(detailTab, existingPO, false);
	}

	private void appendDetailRow(GridTab detailTab, PO po, boolean useDefaults) {
		List<GridField> fields = detailTabFields.get(detailTab);
		List<WEditor> rowEditors = new ArrayList<>();
		Row row = new Row();

		for (GridField field : fields) {
			WEditor editor = WebEditorFactory.getEditor(field, false);
			if (po.get_ID() <= 0) {
				editor.setReadWrite(true);
			} else {
				if (isReadOnly())
					editor.setReadWrite(false);
				else
					editor.setReadWrite(field.isEditable(true));
			}

			if (field.isMandatory(false))
				editor.setMandatory(true);

			Object value = useDefaults ? field.getDefault() : po.get_Value(field.getColumnName());
			if (value != null)
				editor.setValue(value);
			rowEditors.add(editor);
			row.appendChild(editor.getComponent());
		}

		// Delete button (disabled in read-only mode)
		Button btnDel = new Button(Msg.getMsg(Env.getCtx(), "Delete"));
		btnDel.setIconSclass("z-icon-trash");
		btnDel.setSclass("btn btn-danger btn-sm quick-entry-del-row-btn");
		btnDel.setStyle("white-space:nowrap;");
		btnDel.setDisabled(isReadOnly());
		btnDel.setDisabled(isReadOnly());
		final Row finalRow = row;
		final List<WEditor> refs = rowEditors;
		btnDel.addEventListener(Events.ON_CLICK, e -> removeDetailRow(detailTab, finalRow, refs));
		row.appendChild(btnDel);

		detailGridRows.get(detailTab).appendChild(row);
		detailRowEditors.get(detailTab).add(rowEditors);
		detailRowPOs.get(detailTab).add(po);
	}

	/** Remove a detail row from the Grid and data structures. */
	private void removeDetailRow(GridTab detailTab, Row row, List<WEditor> rowEditors) {
		List<List<WEditor>> allEditors = detailRowEditors.get(detailTab);
		List<PO> allPOs = detailRowPOs.get(detailTab);
		int idx = allEditors.indexOf(rowEditors);
		if (idx >= 0) {
			PO po = allPOs.get(idx);
			// 已存在于数据库的记录，加入待删除列表，等 OK 时再真正删除
			if (po != null && po.get_ID() > 0) {
				detailRowPOsToDelete.get(detailTab).add(po);
			}
			allEditors.remove(idx);
			allPOs.remove(idx);
		}
		row.detach();
	}

	// ── loadRecord ─────────────────────────────────────────────────────────────

	@Override
	public boolean loadRecord(int Record_ID) {
		// 1. Header tab: delegate to super (fills quickTabs / quickPOs / quickFields)
		// Guard against empty quickTabs (header has no QuickEntry fields)
		if (!quickTabs.isEmpty()) {
			if (!super.loadRecord(Record_ID))
				return false;
		}

		// 2. Detail tabs
		for (GridTab detailTab : detailTabFields.keySet()) {
			detailGridRows.get(detailTab).getChildren().clear();
			detailRowEditors.get(detailTab).clear();
			detailRowPOs.get(detailTab).clear();
			detailRowPOsToDelete.get(detailTab).clear(); // ← 新增

			if (Record_ID > 0) {
				// Update mode: load existing child records
				List<PO> existing = new Query(Env.getCtx(), detailTab.getTableName(),
						parentColumnName + " = " + Record_ID, null).setOnlyActiveRecords(true).<PO>list();

				if (existing.isEmpty())
					addDetailRow(detailTab);
				else
					existing.forEach(p -> addDetailRowWithPO(detailTab, p));
			} else {
				// New mode: one empty row
				addDetailRow(detailTab);
			}
		}
		return true;
	}

	// ── actionSave ─────────────────────────────────────────────────────────────

	@Override
	protected boolean actionSave() {
		if (!super.actionSave())
			return false;

		if (detailTabFields.isEmpty())
			return true;

		int parentID = getRecord_ID();
		if (parentID == 0) {
			boolean hasDetailData = detailRowPOs.values().stream().anyMatch(list -> list != null && !list.isEmpty());
			if (hasDetailData) {
				String headerName = quickTabs.isEmpty() ? "" : quickTabs.get(0).getName();
				Dialog.error(m_WindowNo, "FillMinimumInfo", headerName);
				return false;
			}
			return true;
		}

		// 3. Save each detail tab's rows
		for (Map.Entry<GridTab, List<GridField>> entry : detailTabFields.entrySet()) {
			GridTab detailTab = entry.getKey();
			List<GridField> fields = entry.getValue();
			List<List<WEditor>> allEditors = detailRowEditors.getOrDefault(detailTab, Collections.emptyList());
			List<PO> allPOs = detailRowPOs.getOrDefault(detailTab, Collections.emptyList());

			for (int rowIdx = 0; rowIdx < allPOs.size(); rowIdx++) {
				if (!saveDetailRow(allPOs.get(rowIdx), fields, allEditors.get(rowIdx), parentID))
					return false;
			}
		}

		// 4. Delete pending detail rows
		for (List<PO> toDelete : detailRowPOsToDelete.values()) {
			if (toDelete == null || toDelete.isEmpty())
				continue;
			for (PO po : new ArrayList<>(toDelete)) { // 防止 ConcurrentModificationException
				try {
					po.deleteEx(true);
				} catch (Exception ex) {
					log.log(Level.SEVERE, "Failed to delete " + po, ex);
					Dialog.error(m_WindowNo, "DeleteError", po.toString());
					return false;
				}
			}
			toDelete.clear();
		}

		// 5. 保存/更新成功后，刷新主窗口（包括子页签）
		ADWindow parentWindow = ADWindow.get(parent_WindowNo);
		if (parentWindow != null) {
			ADWindowContent content = parentWindow.getADWindowContent();
			GridTab parentGridTab = content.getActiveGridTab();
			// 父记录未保存（新建/复制状态）时跳过刷新：
			// onRefresh 会丢弃未保存的新记录，导致班组ID被写入错误的旧记录
			if (parentGridTab == null || !parentGridTab.isNew()) {
				content.onRefresh(true, false);
			}
		}
		return true;
	}

	/**
	 * 保存明细行数据
	 * @Title: saveDetailRow
	 * @param po
	 * @param fields
	 * @param rowEditors
	 * @param parentID
	 * @return
	 * @return boolean
	 */
	private boolean saveDetailRow(PO po, List<GridField> fields, List<WEditor> rowEditors, int parentID) {
		if (rowEditors == null || rowEditors.size() < fields.size()) {
			log.warning("Editor list size mismatch for PO: " + po);
			return true; // 跳过异常行，不中断整体保存
		}

		boolean savePO = false;
		boolean fillMandatoryError = false;
		StringBuilder mandatory = new StringBuilder();

		for (int fi = 0; fi < fields.size(); fi++) {
			GridField field = fields.get(fi);
			WEditor editor = rowEditors.get(fi);
			Object value = editor.getValue();

			boolean thisMandatoryError = false;
			if (field.isMandatory(true) && (value == null || value.toString().isEmpty())) {
				fillMandatoryError = true;
				thisMandatoryError = true;
				if (mandatory.length() > 0)
					mandatory.append(", ");
				mandatory.append(Msg.getElement(Env.getCtx(), field.getColumnName()));
			}

			// 已存在记录时，跳过不可更新的列
			if (po.get_ID() > 0 && !field.isUpdateable())
				continue;

			po.set_ValueOfColumn(field.getColumnName(), value);
			if (value != null && !thisMandatoryError)
				savePO = true;
		}

		if (savePO && fillMandatoryError) {
			Dialog.error(m_WindowNo, "FillMandatory", mandatory.toString());
			return false;
		}

		if (savePO) {
			po.set_ValueNoCheck(parentColumnName, parentID);
			try {
				po.saveEx();
			} catch (Exception ex) {
				log.log(Level.SEVERE, ex.getMessage(), ex);
				Dialog.error(m_WindowNo, "Error", ex.getLocalizedMessage());
				return false;
			}
		}
		return true;
	}

	// ── getQuickFields ─────────────────────────────────────────────────────────

	/**
	 * Returns total quick field count including detail tab fields, so the caller
	 * knows the dialog has something to show.
	 */
	@Override
	public int getQuickFields() {
		int count = super.getQuickFields();
		for (List<GridField> fields : detailTabFields.values())
			count += fields.size();
		return count;
	}

	@Override
	protected void initLayout() throws Exception {
		super.initLayout();

		// 将 super 添加的 centerPanel 和 confirmPanel 从 Window 中摘出，
		// 重新用 Borderlayout 组织，使 confirmPanel 固定在底部
		centerPanel.detach();
		getConfirmPanel().detach();

		org.zkoss.zul.Borderlayout bl = new org.zkoss.zul.Borderlayout();
		ZKUpdateUtil.setVflex(bl, "1");
		ZKUpdateUtil.setHflex(bl, "1");

		// Center：内容区，行数增多时自动出现滚动条
		org.zkoss.zul.Center center = new org.zkoss.zul.Center();
		center.setAutoscroll(true);
		center.appendChild(centerPanel);
		bl.appendChild(center);

		// South：按钮区，固定在底部，不随内容滚动
		org.zkoss.zul.South south = new org.zkoss.zul.South();
		south.appendChild(getConfirmPanel());
		bl.appendChild(south);

		this.appendChild(bl);
	}
}