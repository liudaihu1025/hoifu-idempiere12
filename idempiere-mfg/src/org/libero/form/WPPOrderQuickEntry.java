// idempiere-mfg/src/org/libero/form/WPPOrderQuickEntry.java  
package org.libero.form;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ComboEditorBox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.libero.form.vo.BOMLineVO;
import org.libero.form.vo.RoutingNodeVO;
import org.libero.model.MPPOrder;
import org.libero.service.PPOrderBOMService;
import org.libero.service.PPOrderRoutingService;
import org.libero.service.PPOrderService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.North;
import org.zkoss.zul.Vbox;

/**
 * 工单快速录入窗体
 * <p>
 * 布局（从上到下）：
 * <ol>
 * <li>查询条件 + 查询按钮</li>
 * <li>工单信息（表头，4字段一列）</li>
 * <li>BOM物料列表</li>
 * <li>工单工序列表</li>
 * </ol>
 */
@org.idempiere.ui.zk.annotation.Form
public class WPPOrderQuickEntry extends ADForm implements EventListener<Event>, ValueChangeListener {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(WPPOrderQuickEntry.class);

	// ── 服务层 ──────────────────────────────────────────────────────────────
	private final PPOrderService orderService = new PPOrderService();
	private final PPOrderBOMService bomService = new PPOrderBOMService();
	private final PPOrderRoutingService routingService = new PPOrderRoutingService();

	// ── 查询与操作区 ───────────────────────────────────────────────────────────────
	private Textbox txtQueryOrderNo = new Textbox();
	private Button btnQuery = new Button("查询");
	private Button btnSubmit = new Button("提交");
	private Button btnCopy = new Button("复制");
	private Button btnZoom = new Button("透视");
	private Button btnNew = new Button("新建");

	// ── 表头字段 ─────────────────────────────────────────────────────────────
	private Textbox txtOrderNo = new Textbox();
	private WSearchEditor fProduct;
	private WDateEditor fDateStart = new WDateEditor("DateStartSchedule", true, false, true, "计划开工日期");
	private WTableDirEditor fDocType;
	private Textbox txtProductValue = new Textbox();
	private WDateEditor fDatePromised = new WDateEditor("DatePromised", true, false, true, "计划交货日期");
	private WTableDirEditor fOrderLine;
	private WNumberEditor fQtyEntered = new WNumberEditor("QtyEntered", true, false, true, DisplayType.Quantity,
			"生产数量");
	private WTableDirEditor fResource;
	private Textbox txtDocStatus = new Textbox();
	private WTableDirEditor fUOM;
	private WTableDirEditor fPriority;
	private Decimalbox fImpositionCount = new Decimalbox();
	private Decimalbox fBOMRatio = new Decimalbox();
	private Textbox txtDescription = new Textbox();

	// ── BOM列表 ──────────────────────────────────────────────────────────────
	private Listbox bomListbox = new Listbox();
	private Button btnAddBOM = new Button("添加BOM物料");
	private final List<BOMLineVO> bomLines = new ArrayList<>();

	// ── 工序列表 ─────────────────────────────────────────────────────────────
	private Listbox routingListbox = new Listbox();
	private Button btnAddRouting = new Button("添加工序");
	private Button btnScrapCalc = new Button("损耗计算");
	private final List<RoutingNodeVO> routingNodes = new ArrayList<>();

	// ── 当前工单 ─────────────────────────────────────────────────────────────
	private MPPOrder currentOrder = null;
	/** true = 当前是在本窗体新建的（未持久化） */
	private boolean isNewOrder = true;
	private int currentProductId = 0;
	// ── Orderstatus 引用列表 AD_Reference_Value_ID（运行时从 AD_Column 动态获取）
	private static int orderstatusRefId = -1;

	private Textbox txtProductName = new Textbox(); // 产品名称（只读）
	private boolean formReadOnly = false; // 提交/查询后全部只读
	private int defaultResourceId = 0;
	private MLookup bomProductLookup; // BOM物料列表专用

	// ── BOM物料工序节点下拉数据缓存）────────────────────────────────────────
	private final java.util.LinkedHashMap<Integer, String> routingNodeMap = new java.util.LinkedHashMap<>();
	// 工序节点下拉数据缓存
	private final java.util.LinkedHashMap<Integer, String> allRoutingNodeMap = new java.util.LinkedHashMap<>();
	// 当前销售订单对应的已审批效果列表，每项为 [dy_graphicdesigneffect_id, name]
	private final List<String> effectNameOptions = new ArrayList<>();
	private final java.util.LinkedHashMap<Integer, String> allOperationClassMap = new java.util.LinkedHashMap<>();

	// ════════════════════════════════════════════════════════════════════════
	// 初始化
	// ════════════════════════════════════════════════════════════════════════

	@Override
	protected void initForm() {
		try {
			this.setStyle("position:relative; overflow: hidden;");
			dynInit();
			zkInit();
			fQtyEntered.addValueChangeListener(this);
			fDateStart.addValueChangeListener(this);
			fBOMRatio.addEventListener(Events.ON_CHANGE, e -> refreshBOMList());

			// 若从标准工单窗口透视过来，自动加载对应工单
			ProcessInfo pi = getProcessInfo();
			if (pi != null && pi.getRecord_ID() > 0 && MPPOrder.Table_ID == pi.getTable_ID()) {
				MPPOrder order = new MPPOrder(Env.getCtx(), pi.getRecord_ID(), null);
				if (order.getPP_Order_ID() > 0) {
					loadOrder(order);
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "initForm error", e);
		}
	}

	private void dynInit() throws Exception {
		Properties ctx = Env.getCtx();
		int windowNo = m_WindowNo;

		bomProductLookup = MLookupFactory.get(ctx, windowNo, MColumn.getColumn_ID(MProduct.Table_Name, "M_Product_ID"),
				DisplayType.Search, Env.getLanguage(ctx), MProduct.COLUMNNAME_M_Product_ID, 0, false,
				" M_Product.IsActive = 'Y'");

		MLookup productLookup = MLookupFactory.get(ctx, windowNo, 0, MColumn.getColumn_ID("PP_Order", "M_Product_ID"),
				DisplayType.Search);
		fProduct = new WSearchEditor("M_Product_ID", true, false, true, productLookup);
		fProduct.addValueChangeListener(this);

		MLookup docTypeLookup = MLookupFactory.get(ctx, windowNo,
				MColumn.getColumn_ID("PP_Order", "C_DocTypeTarget_ID"), DisplayType.TableDir, Env.getLanguage(ctx),
				"C_DocTypeTarget_ID", 0, // AD_Reference_Value_ID
				false, // IsParent
				"C_DocType.AD_Client_ID=@#AD_Client_ID@ AND C_DocType.DocBaseType IN ('MOP','MOF','MQO')");
		fDocType = new WTableDirEditor("C_DocTypeTarget_ID", true, false, true, docTypeLookup);

		// 初始化上下文变量（无产品时=0，where clause 中用 @M_Product_ID@）
		Env.setContext(ctx, windowNo, "M_Product_ID", 0);

		MLookup orderLineLookup = MLookupFactory.get(ctx, windowNo, MColumn.getColumn_ID("PP_Order", "C_OrderLine_ID"),
				DisplayType.TableDir, Env.getLanguage(ctx), "C_OrderLine_ID", 0, false,
				"EXISTS (SELECT 1 FROM C_Order o " + "WHERE o.C_Order_ID=C_OrderLine.C_Order_ID "
						+ "  AND o.DocStatus='CO' AND o.IsSOTrx='Y' " + "  AND o.AD_Client_ID=@#AD_Client_ID@) "
						+ "AND C_OrderLine.IsActive='Y' "
						+ "AND (@M_Product_ID@=0 OR C_OrderLine.M_Product_ID=@M_Product_ID@)");
		fOrderLine = new WTableDirEditor("C_OrderLine_ID", false, false, true, orderLineLookup);

		MLookup resourceLookup = MLookupFactory.get(ctx, windowNo, 0, MColumn.getColumn_ID("PP_Order", "S_Resource_ID"),
				DisplayType.TableDir);
		fResource = new WTableDirEditor("S_Resource_ID", false, false, true, resourceLookup);
		// 默认选中"10号厂房"
		defaultResourceId = DB.getSQLValue(null,
				"SELECT S_Resource_ID FROM S_Resource " + "WHERE Name='10号厂房' AND IsActive='Y' AND AD_Client_ID=?",
				Env.getAD_Client_ID(Env.getCtx()));
		if (defaultResourceId > 0) {
			fResource.setValue(defaultResourceId);
		}
		// BOM比例默认10000
		fBOMRatio.setValue(BigDecimal.valueOf(10000));

		MLookup uomLookup = MLookupFactory.get(ctx, windowNo, 0, MColumn.getColumn_ID("PP_Order", "C_UOM_ID"),
				DisplayType.TableDir);
		fUOM = new WTableDirEditor("C_UOM_ID", true, false, true, uomLookup);

		MLookup priorityLookup = MLookupFactory.get(ctx, windowNo, 0, MColumn.getColumn_ID("PP_Order", "PriorityRule"),
				DisplayType.List);
		fPriority = new WTableDirEditor("PriorityRule", false, false, true, priorityLookup);
		fPriority.setValue("5"); // ← 默认"中"

		// ↓ 关键：让 Datebox 显示时分秒
		((org.adempiere.webui.component.Datebox) fDateStart.getComponent()).setFormat("yyyy/MM/dd HH:mm:ss");
		((org.adempiere.webui.component.Datebox) fDatePromised.getComponent()).setFormat("yyyy/MM/dd HH:mm:ss");

		txtOrderNo.setReadonly(true);
		txtOrderNo.setPlaceholder("自动生成");
		txtProductValue.setReadonly(true);
		txtProductName.setReadonly(true);
		txtDocStatus.setReadonly(true);
		txtDocStatus.setValue(getOrderstatusName("Ready"));
		fDateStart.setValue(new Timestamp(System.currentTimeMillis()));
//		txtDescription.setMultiline(true);
//		txtDescription.setRows(3);
		fOrderLine.addValueChangeListener(this);
		loadAllOperationClasses();

		loadRoutingNodeData();
	}

	private void loadRoutingNodeData() {
		allRoutingNodeMap.clear();
		String sql = "SELECT AD_Routing_Node_ID, Name FROM AD_Routing_Node "
				+ "WHERE IsActive='Y' AND AD_Client_ID=? ORDER BY Value";
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				allRoutingNodeMap.put(rs.getInt(1), rs.getString(2));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "loadRoutingNodeData error", e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	private void loadAllOperationClasses() {
		allOperationClassMap.clear();
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try {
			ps = DB.prepareStatement("SELECT operationclass_id, name FROM operationclass "
					+ "WHERE isactive='Y' AND ad_client_id=? ORDER BY value", null);
			ps.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			rs = ps.executeQuery();
			while (rs.next())
				allOperationClassMap.put(rs.getInt(1), rs.getString(2));
		} catch (Exception e) {
			log.log(Level.WARNING, "loadAllOperationClasses error", e);
		} finally {
			DB.close(rs, ps);
		}
	}

	private java.util.LinkedHashMap<Integer, String> getRoutingNodesByOperationClass(int operationClassId) {
		java.util.LinkedHashMap<Integer, String> map = new java.util.LinkedHashMap<>();
		if (operationClassId <= 0)
			return map;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try {
			ps = DB.prepareStatement("SELECT AD_Routing_Node_ID, Name FROM AD_Routing_Node "
					+ "WHERE operationclass_ID=? AND IsActive='Y' AND AD_Client_ID=? ORDER BY Value", null);
			ps.setInt(1, operationClassId);
			ps.setInt(2, Env.getAD_Client_ID(Env.getCtx()));
			rs = ps.executeQuery();
			while (rs.next())
				map.put(rs.getInt(1), rs.getString(2));
		} catch (Exception e) {
			log.log(Level.WARNING, "getRoutingNodesByOperationClass error", e);
		} finally {
			DB.close(rs, ps);
		}
		return map;
	}

	// ════════════════════════════════════════════════════════════════════════
	// 布局
	// ════════════════════════════════════════════════════════════════════════

	private void zkInit() {
		Borderlayout layout = new Borderlayout();
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		appendChild(layout);

		North north = new North();
		north.setCollapsible(false);
		layout.appendChild(north);
		north.appendChild(buildQuerySection());

		Center center = new Center();
		center.setAutoscroll(true);
		layout.appendChild(center);

		Vbox content = new Vbox();
		ZKUpdateUtil.setWidth(content, "100%");
		content.setStyle("padding:8px; overflow: hidden;");
		center.appendChild(content);

		content.appendChild(sectionTitle("工单信息"));
		content.appendChild(buildHeaderSection());
//		content.appendChild(buildActionButtons());
		content.appendChild(sectionTitle("工单工序"));
		content.appendChild(buildRoutingSection());
		content.appendChild(sectionTitle("BOM物料"));
		content.appendChild(buildBOMSection());

		Clients.evalJavaScript("if(!document.getElementById('woCurrentNodeStyle')){"
				+ "  var s=document.createElement('style');" + "  s.id='woCurrentNodeStyle';" + "  s.textContent='"
				+ "    .wo-current-node .z-listcell," + "    .wo-current-node.z-listitem-selected .z-listcell"
				+ "    { background-color:#e8f5e9 !important; }" + "  ';" + "  document.head.appendChild(s);" + "}");
	}

	private Div buildQuerySection() {
		Div div = new Div();
		div.setStyle("padding:8px;background:#f5f5f5;border-bottom:1px solid #ddd;");
		Hbox hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setSpacing("8px");
		hbox.appendChild(new org.zkoss.zul.Label("工程单号："));
		txtQueryOrderNo.setWidth("200px");
		txtQueryOrderNo.setPlaceholder("输入工程单号查询");
		hbox.appendChild(txtQueryOrderNo);
		btnQuery.addEventListener(Events.ON_CLICK, this);
		btnSubmit.addEventListener(Events.ON_CLICK, this);
		btnCopy.addEventListener(Events.ON_CLICK, this);
		btnNew.addEventListener(Events.ON_CLICK, this);
		btnZoom.addEventListener(Events.ON_CLICK, this);
		hbox.appendChild(btnQuery);
		hbox.appendChild(btnSubmit);
		hbox.appendChild(btnCopy);
		hbox.appendChild(btnNew);
		hbox.appendChild(btnZoom);
		div.appendChild(hbox);
		updateButtonState();
		return div;
	}

	private Grid buildHeaderSection() {
		Grid grid = GridFactory.newGridLayout();
		ZKUpdateUtil.setWidth(grid, "100%");

		org.adempiere.webui.component.Columns cols = new org.adempiere.webui.component.Columns();
		// 8列：标签-值-标签-值-标签-值-标签-值
		for (String w : new String[] { "8%", "17%", "8%", "17%", "8%", "17%", "8%", "17%" }) {
			org.adempiere.webui.component.Column c = new org.adempiere.webui.component.Column();
			c.setWidth(w);
			cols.appendChild(c);
		}
		grid.appendChild(cols);

		Rows rows = new Rows();
		grid.appendChild(rows);
		grid.setStyle("margin:0; padding:0;");

		// 行1: 工程单号 | 值 | 产品 | 值 | 计划开工日期 | 值 | 工单类型 | 值
		Row r1 = rows.newRow();
		r1.setStyle("height: 30px; line-height: 30px; padding: 3;");
		r1.appendCellChild(lbl("工程单号"));
		ZKUpdateUtil.setHflex(txtOrderNo, "true");
		r1.appendCellChild(txtOrderNo);
		r1.appendCellChild(lbl("产品 *"));
		ZKUpdateUtil.setHflex(fProduct.getComponent(), "true");
		r1.appendCellChild(fProduct.getComponent());
		r1.appendCellChild(lbl("计划开工日期 *"));
		ZKUpdateUtil.setHflex(fDateStart.getComponent(), "true");
		r1.appendCellChild(fDateStart.getComponent());
		r1.appendCellChild(lbl("工单类型 *"));
		ZKUpdateUtil.setHflex(fDocType.getComponent(), "true");
		r1.appendCellChild(fDocType.getComponent());

		// Row 2（8 cells）：产品编码 | 产品名称 | 计划交货日期 | 关联销售订单
		Row r2 = rows.newRow();
		r2.setStyle("height: 30px; line-height: 30px; padding: 3;");
		r2.appendCellChild(lbl("产品编码"));
		ZKUpdateUtil.setHflex(txtProductValue, "true");
		r2.appendCellChild(txtProductValue);
		r2.appendCellChild(lbl("产品名称"));
		ZKUpdateUtil.setHflex(txtProductName, "true");
		r2.appendCellChild(txtProductName);
		r2.appendCellChild(lbl("计划交货日期 *"));
		ZKUpdateUtil.setHflex(fDatePromised.getComponent(), "true");
		r2.appendCellChild(fDatePromised.getComponent());
		r2.appendCellChild(lbl("关联销售订单 *"));
		ZKUpdateUtil.setHflex(fOrderLine.getComponent(), "true");
		r2.appendCellChild(fOrderLine.getComponent());

		// Row 3（8 cells）：生产数量 | 单位 | 生产线/车间 | 工单状态
		Row r3 = rows.newRow();
		r3.setStyle("height: 30px; line-height: 30px; padding: 3;");
		r3.appendCellChild(lbl("工单状态"));
		ZKUpdateUtil.setHflex(txtDocStatus, "true");
		r3.appendCellChild(txtDocStatus);
		r3.appendCellChild(lbl("单位 *"));
		ZKUpdateUtil.setHflex(fUOM.getComponent(), "true");
		r3.appendCellChild(fUOM.getComponent());
		r3.appendCellChild(lbl("生产数量 *"));
		ZKUpdateUtil.setHflex(fQtyEntered.getComponent(), "true");
		r3.appendCellChild(fQtyEntered.getComponent());
		r3.appendCellChild(lbl("生产线/车间"));
		ZKUpdateUtil.setHflex(fResource.getComponent(), "true");
		r3.appendCellChild(fResource.getComponent());

		// Row 4（8 cells）：优先级 | 拼版联数 | BOM比例 | 备注(span 3)
		Row r4 = rows.newRow();
		r4.setStyle("height: 30px; line-height: 30px; padding: 3;");
		r4.appendCellChild(lbl("备注"));
		ZKUpdateUtil.setHflex(txtDescription, "true");
		r4.appendCellChild(txtDescription);
		r4.appendCellChild(lbl("拼版联数 *"));
		ZKUpdateUtil.setHflex(fImpositionCount, "true");
		r4.appendCellChild(fImpositionCount);
		r4.appendCellChild(lbl("BOM比例 *"));
		ZKUpdateUtil.setHflex(fBOMRatio, "true");
		r4.appendCellChild(fBOMRatio);
		r4.appendCellChild(lbl("优先级"));
		ZKUpdateUtil.setHflex(fPriority.getComponent(), "true");
		r4.appendCellChild(fPriority.getComponent());

		return grid;
	}

	private Hbox buildActionButtons() {
		Hbox hbox = new Hbox();
		hbox.setStyle("padding:8px 0;");
		hbox.setSpacing("8px");
		btnSubmit.addEventListener(Events.ON_CLICK, this);
		btnCopy.addEventListener(Events.ON_CLICK, this);
		btnNew.addEventListener(Events.ON_CLICK, this);
		btnZoom.addEventListener(Events.ON_CLICK, this);
		hbox.appendChild(btnSubmit);
		hbox.appendChild(btnCopy);
		hbox.appendChild(btnNew);
		hbox.appendChild(btnZoom);
		updateButtonState();
		return hbox;
	}

	// ════════════════════════════════════════════════════════════════════════
	// BOM列表
	// ════════════════════════════════════════════════════════════════════════

	private Div buildBOMSection() {
		Div div = new Div();
		ZKUpdateUtil.setWidth(div, "100%");
		div.setStyle("overflow: hidden;"); // ← 新增
		Hbox toolbar = new Hbox();
		toolbar.setSpacing("8px");
		toolbar.setStyle("padding:4px 0 6px 0;");
		btnAddBOM.addEventListener(Events.ON_CLICK, this);
		toolbar.appendChild(btnAddBOM);
		div.appendChild(toolbar);

		// 用 Div 包裹 listbox，允许横向滚动
		Div listWrapper = new Div();
		listWrapper.setStyle("width:100%; overflow-x:auto;");
		bomListbox.setWidth("100%");
		bomListbox.setSizedByContent(false); // 不按内容撑宽
		bomListbox.setSpan(true); // 剩余空间自动填充无固定宽度的列
		buildBOMListHead();
		listWrapper.appendChild(bomListbox);
		div.appendChild(listWrapper);
		return div;
	}

	private void buildBOMListHead() {
		bomListbox.setWidth("100%"); // ← 用 width 而不是 hflex
		Listhead head = new Listhead();
		head.setSizable(true); // 允许用户拖拽调整列宽
		bomListbox.appendChild(head);

		// 先加"序号"
		Listheader hSeq = new Listheader("序号");
		hSeq.setWidth("30px");
		head.appendChild(hSeq);

		// "物料"列：弹性填充剩余空间
		Listheader hProduct = new Listheader("物料");
//		hProduct.setHflex("1"); // ← 弹性列，自动填满剩余宽度
//		hProduct.setWidth("150px");
		head.appendChild(hProduct);

		// 其余固定宽度列
		String[] hdrs = { "物料编码", "物料名称", "效果名称", "所属工序", "BOM数量", "比例用量", "需求用量", "单位", "领用数量", "库存数量", "操作" };
		String[] wids = { "150px", "250px", "100px", "100px", "100px", "100px", "100px", "80px", "100px", "100px", "100px" };
		for (int i = 0; i < hdrs.length; i++) {
			Listheader h = new Listheader(hdrs[i]);
			h.setWidth(wids[i]);
			head.appendChild(h);
		}
	}

	/**
	 * 刷新 BOM 物料列表 列：序号 | 物料(搜索) | 物料编码 | 物料名称 | 所属工序 | BOM数量 | 比例用量 | 需求用量 | 单位 |
	 * 领用数量 | 操作
	 */
	private void refreshBOMList() {
		rebuildRoutingNodeMap();
		new ArrayList<>(bomListbox.getItems()).forEach(Listitem::detach);
		int seq = 1;
		for (int i = 0; i < bomLines.size(); i++) {
			final int idx = i;
			BOMLineVO vo = bomLines.get(idx);
			if (vo.isDeleted)
				continue;

			// 新增行（未保存）始终可编辑；已保存行在 formReadOnly 时只读
			boolean rowReadOnly = formReadOnly && vo.ppOrderBOMLineId > 0;

			Listitem item = new Listitem();

			// 序号
			item.appendChild(cell(String.valueOf(seq++)));

			// 物料（搜索框 or 只读文本）
			if (rowReadOnly) {
				String display = bomProductLookup != null ? bomProductLookup.getDisplay(vo.productId) : vo.productValue;
				item.appendChild(cell(display != null ? display : vo.productValue));
			} else {
				item.appendChild(buildProductSearchCell(vo, idx));
			}

			// 物料编码（只读）
			item.appendChild(cell(vo.productValue));

			// 物料名称（只读）
			item.appendChild(cell(vo.productName));
			
			// 效果名称（下拉可选）
			if (rowReadOnly) {
				item.appendChild(cell(vo.effectName));
			} else {
				item.appendChild(buildEffectNameCell(vo.effectName, v -> bomLines.get(idx).effectName = v));
			}

			// 所属工序（下拉 or 只读）
			if (rowReadOnly) {
				item.appendChild(cell(vo.routingNodeName));
			} else {
				item.appendChild(buildRoutingNodeComboCell(vo, idx, true));
			}

			// BOM数量（只读，实时计算）
			BigDecimal bomRatio = getBOMRatio();
			BigDecimal qtyBOM = BigDecimal.ZERO;
			if (vo.nQtyBOM != null && bomRatio != null && bomRatio.compareTo(BigDecimal.ZERO) > 0) {
				qtyBOM = vo.nQtyBOM.divide(bomRatio, 7, RoundingMode.HALF_UP);
				vo.qtyBOM = qtyBOM; // 同步到 VO
			}

			// 用变量持有引用，供比例用量 onChange 更新
			Listcell bomQtyCell = new Listcell(vo.qtyBOM != null ? vo.qtyBOM.toPlainString() : "");
			item.appendChild(bomQtyCell);

			// 比例用量（可编辑 or 只读）
			if (rowReadOnly) {
				item.appendChild(cell(vo.nQtyBOM != null ? vo.nQtyBOM.toPlainString() : ""));
			} else {
				item.appendChild(buildDecimalCell(vo.nQtyBOM, v -> {
					bomLines.get(idx).nQtyBOM = v;
					BigDecimal ratio = getBOMRatio();
					if (ratio != null && ratio.compareTo(BigDecimal.ZERO) > 0) {
						BigDecimal newQtyBOM = v.divide(ratio, 7, RoundingMode.HALF_UP);
						bomLines.get(idx).qtyBOM = newQtyBOM;
						bomQtyCell.setLabel(newQtyBOM.toPlainString()); // 只更新这一格
					}
				}));
			}

			// 需求用量（可编辑）
			if (rowReadOnly) {
				item.appendChild(cell(vo.qtyRequiered != null ? vo.qtyRequiered.toPlainString() : ""));
			} else {
				item.appendChild(buildDecimalCell(vo.qtyRequiered, v -> bomLines.get(idx).qtyRequiered = v));
			}
			// 单位（只读）
			item.appendChild(cell(vo.uomName));

			// 领用数量（只读）
			item.appendChild(cell(vo.qtyDelivered != null ? vo.qtyDelivered.toPlainString() : ""));
			
			// 库存数量（只读，按当前组织汇总）  
			item.appendChild(cell(vo.qtyOnHand != null ? vo.qtyOnHand.toPlainString() : "0"));

			// 操作（删除按钮）
			item.appendChild(buildDeleteBOMCell(idx, vo));

			bomListbox.appendChild(item);
		}
	}

	private BigDecimal getBOMRatio() {
		if (fBOMRatio.getValue() == null)
			return BigDecimal.ONE;
		try {
			return new BigDecimal(fBOMRatio.getValue().toString());
		} catch (Exception e) {
			return BigDecimal.ONE;
		}
	}

	private Listcell buildProductSearchCell(BOMLineVO vo, int idx) {
		Listcell cell = new Listcell();
		cell.setStyle("overflow: hidden;");
		try {
			Properties ctx = Env.getCtx();

			WSearchEditor se = new WSearchEditor("M_Product_ID", false, false, true, bomProductLookup) {
				@Override
				public void setValue(Object value) {
					super.setValue(value);
					this.fireValueChange(new ValueChangeEvent(this, this.getColumnName(), getValue(), value));
				}
			};

			if (vo.productId > 0)
				se.setValue(vo.productId);

			// 正确方式：用 setTableEditor(true)，它内部调用 setTableEditorMode(true)
			// 不要再调 ZKUpdateUtil.setWidth(comp, "100%")，否则会清除 hflex="0"
			se.setTableEditor(true);

			se.addValueChangeListener(evt -> {
				Object newVal = evt.getNewValue();
				if (newVal instanceof Integer) {
					int pid = (Integer) newVal;
					MProduct p = MProduct.get(ctx, pid);
					if (p != null) {
						BOMLineVO v = bomLines.get(idx);
						v.productId = pid;
						v.productValue = p.getValue();
						v.productName = p.getName();
						v.uomId = p.getC_UOM_ID();
						v.uomName = DB.getSQLValueString(null, "SELECT UOMSymbol FROM C_UOM WHERE C_UOM_ID=?",
								p.getC_UOM_ID());
						v.qtyOnHand = getQtyOnHand(pid);
						Executions.schedule(bomListbox.getDesktop(), e2 -> refreshBOMList(), new Event("onRefresh"));
					}
				}
			});

			// ✅ 用 comp 变量，不要重复调用 se.getComponent()
			ComboEditorBox comp = se.getComponent();
			cell.appendChild(comp);
		} catch (Exception e) {
			log.log(Level.SEVERE, "buildProductSearchCell error", e);
		}
		return cell;
	}
	
	private Listcell buildOperationClassCell(RoutingNodeVO vo, int idx, boolean rowReadOnly) {
		Listcell cell = new Listcell();
		Combobox cmb = new Combobox();
		cmb.setWidth("100%");
		cmb.setAutocomplete(false);
		cmb.setAutodrop(true);

		// 空选项
		Comboitem emptyItem = new Comboitem("");
		emptyItem.setValue(0);
		cmb.appendChild(emptyItem);

		for (java.util.Map.Entry<Integer, String> entry : allOperationClassMap.entrySet()) {
			Comboitem ci = new Comboitem(entry.getValue());
			ci.setValue(entry.getKey());
			cmb.appendChild(ci);
			if (entry.getKey() == vo.operationClassId)
				cmb.setSelectedItem(ci);
		}

		cmb.addEventListener(Events.ON_SELECT, e -> {
			Comboitem sel = cmb.getSelectedItem();
			int newClassId = (sel != null && sel.getValue() instanceof Integer) ? (Integer) sel.getValue() : 0;
			RoutingNodeVO rvo = routingNodes.get(idx);
			rvo.operationClassId = newClassId;
			rvo.operationClassName = newClassId > 0 ? allOperationClassMap.getOrDefault(newClassId, "") : "";
			// 工序组变更，清空已选工序及相关字段
			rvo.adRoutingNodeId = 0;
			rvo.routingNodeName = "";
			rvo.description = "";
			rvo.processdifficulty = "";
			rvo.difficultyfactor = BigDecimal.ONE;
			refreshRoutingList();
		});

		cmb.addEventListener(Events.ON_CHANGE, e -> {
			if (cmb.getSelectedItem() == null)
				cmb.setValue("");
		});

		if (rowReadOnly)
			cmb.setDisabled(true);
		cell.appendChild(cmb);
		return cell;
	}

	private Listcell buildRoutingNodeComboCell(Object vo, int idx, boolean isBOM) {
		Listcell cell = new Listcell();
		Combobox cmb = new Combobox();
		cmb.setWidth("100%");
		cmb.setAutocomplete(false);
		cmb.setAutodrop(true);
		cmb.setAttribute("colType", "routingNode");

		int selectedId = isBOM ? ((BOMLineVO) vo).routingNodeId : ((RoutingNodeVO) vo).adRoutingNodeId;

		// ← 关键改动：工序列表按工序组过滤，BOM列表仍用 routingNodeMap
		java.util.Map<Integer, String> sourceMap;
		if (isBOM) {
			sourceMap = routingNodeMap;
		} else {
			int classId = ((RoutingNodeVO) vo).operationClassId;
			if (classId > 0) {
				sourceMap = getRoutingNodesByOperationClass(classId); // 有工序组：按组过滤
			} else {
				sourceMap = allRoutingNodeMap; // 无工序组：显示全部（兼容旧数据）
			}
		}

		java.util.Set<Integer> usedNodeIds = java.util.Collections.emptySet();
		if (!isBOM) {
			usedNodeIds = routingNodes.stream()
					.filter(n -> !n.isDeleted && n.adRoutingNodeId > 0 && n.adRoutingNodeId != selectedId)
					.map(n -> n.adRoutingNodeId).collect(java.util.stream.Collectors.toSet());
		}
		final java.util.Set<Integer> finalUsedIds = usedNodeIds;

		for (java.util.Map.Entry<Integer, String> entry : sourceMap.entrySet()) {
			if (!isBOM && finalUsedIds.contains(entry.getKey()))
				continue;
			Comboitem ci = new Comboitem(entry.getValue());
			ci.setValue(entry.getKey());
			cmb.appendChild(ci);
			if (entry.getKey().equals(selectedId))
				cmb.setSelectedItem(ci);
		}

		cmb.addEventListener(Events.ON_SELECT, e -> {
			Comboitem sel = cmb.getSelectedItem();
			if (sel == null)
				return;
			int nodeId = (Integer) sel.getValue();
			String nodeName = sel.getLabel();
			if (isBOM) {
				bomLines.get(idx).routingNodeId = nodeId;
				bomLines.get(idx).routingNodeName = nodeName;
			} else {
				routingNodes.get(idx).adRoutingNodeId = nodeId;
				routingNodes.get(idx).routingNodeName = nodeName;
				routingNodes.get(idx).description = "";
				routingNodes.get(idx).processdifficulty = "";
				routingNodes.get(idx).difficultyfactor = BigDecimal.ONE;
				refreshRoutingList();
			}
		});

		cmb.addEventListener(Events.ON_CHANGE, e -> {
			if (cmb.getSelectedItem() == null)
				cmb.setValue("");
		});

		cell.appendChild(cmb);
		return cell;
	}

	private Listcell buildDecimalCell(BigDecimal value, java.util.function.Consumer<BigDecimal> onChange) {
		Listcell cell = new Listcell();
		Decimalbox db = new Decimalbox();
		db.setValue(value);
		db.setWidth("90%");
		db.addEventListener(Events.ON_CHANGE, e -> {
			if (db.getValue() != null)
				onChange.accept(db.getValue());
		});
		cell.appendChild(db);
		return cell;
	}

	private Listcell buildEffectNameCell(String currentValue, java.util.function.Consumer<String> onChange) {
		Listcell cell = new Listcell();
		Combobox cmb = new Combobox();
		cmb.setWidth("100%");
		cmb.setAutocomplete(false);
		cmb.setAutodrop(true);
		cmb.setAttribute("colType", "effectName");

		// 空选项（不选）
		Comboitem emptyItem = new Comboitem("");
		emptyItem.setValue("");
		cmb.appendChild(emptyItem);

		for (String name : effectNameOptions) {
			Comboitem ci = new Comboitem(name);
			ci.setValue(name);
			cmb.appendChild(ci);
			if (name.equals(currentValue)) {
				cmb.setSelectedItem(ci);
			}
		}
		// 若当前值不在选项中（历史数据），直接显示文本
		if (currentValue != null && !currentValue.isEmpty() && cmb.getSelectedItem() == null) {
			cmb.setValue(currentValue);
		}

		cmb.addEventListener(Events.ON_SELECT, e -> {
			Comboitem sel = cmb.getSelectedItem();
			onChange.accept(sel != null ? sel.getValue().toString() : "");
		});
		cmb.addEventListener(Events.ON_CHANGE, e -> {
			// 允许手动输入（历史数据兼容）
			onChange.accept(cmb.getValue());
		});
		cell.appendChild(cmb);
		return cell;
	}

	/**
	 * 构建 BOM 物料删除按钮单元格 条件：formReadOnly=true 或 canEditBOMAndRouting=false 时不显示；
	 * 有领料成本归集单（CostCollectorType='110'或'116'）时禁用。
	 */
	private Listcell buildDeleteBOMCell(int idx, BOMLineVO vo) {
		Listcell cell = new Listcell();
		cell.setStyle("text-align:center;");
		if (!getBtnDisabledStatus())
			return cell;

		Button btn = new Button("删除");
		btn.setStyle("font-size:13px");
		btn.addEventListener(Events.ON_CLICK, e -> {
			// 点击时校验：是否有领料成本归集单
			if (vo.ppOrderBOMLineId > 0 && currentOrder != null) {
				int cnt = DB.getSQLValue(null,
						"SELECT COUNT(*) FROM PP_Cost_Collector " + "WHERE PP_Order_ID=? AND M_Product_ID=? "
								+ "AND CostCollectorType IN ('110','116') AND IsActive='Y'",
						currentOrder.getPP_Order_ID(), vo.productId);
				if (cnt > 0) {
					Dialog.error(m_WindowNo, "", "该物料已有领料记录，不允许删除");
					return;
				}
			}
			bomLines.get(idx).isDeleted = true;
			refreshBOMList();
		});
		cell.appendChild(btn);
		return cell;
	}

	// ════════════════════════════════════════════════════════════════════════
	// 工序列表
	// ════════════════════════════════════════════════════════════════════════
	private Div buildRoutingSection() {
		Div div = new Div();
		ZKUpdateUtil.setWidth(div, "100%");
		div.setStyle("overflow: hidden;");
		Hbox toolbar = new Hbox();
		toolbar.setSpacing("8px");
		toolbar.setStyle("padding:4px 0 6px 0;");
		btnAddRouting.addEventListener(Events.ON_CLICK, this);
		btnScrapCalc.addEventListener(Events.ON_CLICK, this);
		toolbar.appendChild(btnAddRouting);
		toolbar.appendChild(btnScrapCalc);
		div.appendChild(toolbar);

		// ✅ 用 Div 包裹 listbox，允许横向滚动
		Div listWrapper = new Div();
		listWrapper.setStyle("width:100%; overflow-x:auto; overflow-y:visible; height:auto;");
		routingListbox.setWidth("100%");
		routingListbox.setSizedByContent(false);
		routingListbox.setSpan(true);
		buildRoutingListHead();
		listWrapper.appendChild(routingListbox);
		div.appendChild(listWrapper);
		return div;
	}

	private void buildRoutingListHead() {
		routingListbox.setWidth("100%");
		Listhead head = new Listhead();
		head.setSizable(true);
		routingListbox.appendChild(head);

		String[] hdrs = { "序号", "效果名称", "工序组", "工序名称", "工序编码", "工序描述", "工艺难度", "色数", "工序放损数", "标准加工数", "报工数量", "废品数量", "合格率", "操作" };
		String[] wids = { "30px", "90px", "90px", "90px", "60px",  null, "60px", "40px", "70px", "70px", "70px", "70px", "70px", "100px" };
		for (int i = 0; i < hdrs.length; i++) {
			Listheader h = new Listheader(hdrs[i]);
			if (Objects.nonNull(wids[i]))
				h.setWidth(wids[i]);
			head.appendChild(h);
		}
	}

	/**
	 * 刷新工单工序列表 列：序号 | 工序(下拉) | 工序编码 | 工序名称 | 工序描述 | 工艺难度 | 色数 | 工序数量 | 报工数量 | 废品数量
	 * | 合格率 | 操作
	 */
	private void refreshRoutingList() {
		new ArrayList<>(routingListbox.getItems()).forEach(Listitem::detach);
		int seq = 1;
		for (int i = 0; i < routingNodes.size(); i++) {
			final int idx = i;
			RoutingNodeVO vo = routingNodes.get(idx);
			if (vo.isDeleted)
				continue;

			boolean rowReadOnly = formReadOnly && vo.ppOrderNodeId > 0;
			Listitem item = new Listitem();
			// 当前生产工序高亮（绿色背景）
			if (vo.isCurrentNode) {
//				item.setStyle("background-color:#e8f5e9;");
				item.setSclass("wo-current-node");
			}

			// 序号
			item.appendChild(cell(String.valueOf(seq++)));
			
			// 效果名称（下拉可选）
			if (rowReadOnly) {
				item.appendChild(cell(vo.effectName));
			} else {
				item.appendChild(buildEffectNameCell(vo.effectName, v -> routingNodes.get(idx).effectName = v));
			}
			
			// 工序组下拉
			if (rowReadOnly) {
				item.appendChild(cell(vo.operationClassName));
			} else {
				item.appendChild(buildOperationClassCell(vo, idx, false));
			}

			// 工序（下拉 or 只读）
			if (rowReadOnly) {
				item.appendChild(cell(vo.routingNodeName));
			} else {
				item.appendChild(buildRoutingNodeComboCell(vo, idx, false));
			}

			// 工序编码（只读）
			item.appendChild(cell(vo.routingNodeValue));

			// 工序名称（只读）
//			item.appendChild(cell(vo.routingNodeName));

			// 工序描述（可编辑 or 只读）
			if (rowReadOnly) {
				item.appendChild(cell(vo.description));
			} else {
				item.appendChild(buildTextCell(vo.description, v -> routingNodes.get(idx).description = v));
			}

			// 工艺难度（Combobox，根据工序动态加载 processdifficulty）
			if (rowReadOnly) {
				item.appendChild(cell(vo.processdifficulty));
			} else {
				item.appendChild(buildProcessDifficultyCell(vo, idx));
			}

			// 色数（可编辑 or 只读）
			if (rowReadOnly) {
				item.appendChild(cell(String.valueOf(vo.colorCount)));
			} else {
				item.appendChild(buildDecimalCell(vo.colorCount, v -> routingNodes.get(idx).colorCount = v));
			}

			// 工序放损数（只读，由损耗计算得出）
			item.appendChild(cell(vo.qtyPaperScrap != null ? vo.qtyPaperScrap.toPlainString() : ""));

			// 标准加工数（可编辑 or 只读）
			if (rowReadOnly) {
				item.appendChild(cell(vo.qtyRequiered != null ? vo.qtyRequiered.toPlainString() : ""));
			} else {
				item.appendChild(buildDecimalCell(vo.qtyRequiered, v -> routingNodes.get(idx).qtyRequiered = v));
			}

			// 报工数量（只读，已报工）
			item.appendChild(cell(vo.qtyDelivered != null ? vo.qtyDelivered.toPlainString() : ""));

			// 废品数量（只读，已报工）
			item.appendChild(cell(vo.qtyScrap != null ? vo.qtyScrap.toPlainString() : ""));

			// 合格率（只读，计算值）
			item.appendChild(cell(calcPassRate(vo)));

			// 操作（删除按钮）
			item.appendChild(buildDeleteRoutingCell(idx, vo));

			routingListbox.appendChild(item);
		}

		// 工序列表变化后，同步更新 BOM 的所属工序下拉
		rebuildRoutingNodeMap();
//		refreshBOMList();
		// 只同步 BOM 行的工序下拉，不重建整个 BOM 列表
		syncBOMRoutingDropdowns();

	}

	private Listcell buildProcessDifficultyCell(RoutingNodeVO vo, int idx) {
		Listcell cell = new Listcell();
		Combobox cmb = new Combobox();
		cmb.setWidth("100%");
		cmb.setAutocomplete(false);
		cmb.setAutodrop(true);

		// 根据工序加载 processdifficulty 选项
		if (vo.adRoutingNodeId > 0) {
			loadProcessDifficultyOptions(cmb, vo.adRoutingNodeId, vo.processdifficulty);
		}

		cmb.addEventListener(Events.ON_SELECT, e -> {
			if (cmb.getSelectedItem() != null) {
				String pd = cmb.getSelectedItem().getValue().toString();
				routingNodes.get(idx).processdifficulty = pd;
				// 从数据库读取 difficultyfactor
				BigDecimal df = DB.getSQLValueBD(null,
						"SELECT difficultyfactor FROM c_paperscrapstd "
								+ "WHERE AD_Routing_Node_ID=? AND processdifficulty=? "
								+ "  AND IsActive='Y' AND AD_Client_ID=? "
								+ "ORDER BY c_paperscrapstd_id FETCH FIRST 1 ROWS ONLY",
						vo.adRoutingNodeId, pd, Env.getAD_Client_ID(Env.getCtx()));
				routingNodes.get(idx).difficultyfactor = df != null ? df : BigDecimal.ONE;
			}
		});
		cell.appendChild(cmb);
		return cell;
	}

	private void loadProcessDifficultyOptions(Combobox cmb, int adRoutingNodeId, String selectedPd) {
		cmb.getChildren().clear();
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try {
			ps = DB.prepareStatement("SELECT DISTINCT ps.processdifficulty FROM c_paperscrapstd ps "
					+ "WHERE ps.operationclass_ID = (" + "    SELECT rn.operationclass_ID FROM AD_Routing_Node rn "
					+ "    WHERE rn.AD_Routing_Node_ID=? AND rn.IsActive='Y'" + ") "
					+ "AND ps.IsActive='Y' AND ps.AD_Client_ID=? " + "ORDER BY ps.processdifficulty",
					null);
			ps.setInt(1, adRoutingNodeId);
			ps.setInt(2, Env.getAD_Client_ID(Env.getCtx()));
			rs = ps.executeQuery();
			while (rs.next()) {
				String pd = rs.getString(1);
				Comboitem ci = new Comboitem(pd);
				ci.setValue(pd);
				cmb.appendChild(ci);
				if (pd.equals(selectedPd))
					cmb.setSelectedItem(ci);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "loadProcessDifficultyOptions error", e);
		} finally {
			DB.close(rs, ps);
		}
	}

	/**
	 * 重建工序下拉数据源（从当前 routingNodes 列表动态构建，供 BOM 所属工序列使用）
	 */
	private void rebuildRoutingNodeMap() {
		routingNodeMap.clear();
		for (RoutingNodeVO vo : routingNodes) {
			if (!vo.isDeleted && vo.adRoutingNodeId > 0) {
				routingNodeMap.put(vo.adRoutingNodeId, vo.routingNodeName);
			}
		}
	}

	private Listcell buildTextCell(String value, java.util.function.Consumer<String> onChange) {
		Listcell cell = new Listcell();
		Textbox txt = new Textbox(value != null ? value : "");
		txt.setWidth("99%");
		txt.setRows(3);
		txt.addEventListener(Events.ON_CHANGE, e -> onChange.accept(txt.getValue()));
		cell.appendChild(txt);
		return cell;
	}

	/**
	 * 构建工单工序删除按钮单元格 条件：formReadOnly=true 或 canEditBOMAndRouting=false 时不显示；
	 * 有报工成本归集单（CostCollectorType='160'）时禁用。
	 */
	private Listcell buildDeleteRoutingCell(int idx, RoutingNodeVO vo) {
		Listcell cell = new Listcell();
		cell.setStyle("text-align:center;");
		if (!getBtnDisabledStatus())
			return cell;

		final int finalIdx = idx;

		// 第一行：删除按钮
		Button btn = new Button("删除");
		btn.setStyle("font-size:13px;");
		btn.setWidth("93%");
		btn.addEventListener(Events.ON_CLICK, e -> {
			if (vo.ppOrderNodeId > 0) {
				int cnt = DB.getSQLValue(null, "SELECT COUNT(*) FROM PP_Cost_Collector " + "WHERE PP_Order_Node_ID=? "
						+ "AND CostCollectorType='160' AND IsActive='Y'", vo.ppOrderNodeId);
				if (cnt > 0) {
					Dialog.error(m_WindowNo, "", "该工序已有报工记录，不允许删除");
					return;
				}
			}
			routingNodes.get(idx).isDeleted = true;
			renumberRoutingNodes();
			refreshRoutingList();
		});

		// 第二行：上移 + 下移按钮
		Button btnUp = new Button("↑");
		btnUp.setWidth("42%");
		btnUp.addEventListener(Events.ON_CLICK, e -> onMoveRoutingUp(finalIdx));

		Button btnDown = new Button("↓");
		btnDown.setWidth("42%");
		btnDown.addEventListener(Events.ON_CLICK, e -> onMoveRoutingDown(finalIdx));

		org.zkoss.zul.Hbox hbox = new org.zkoss.zul.Hbox();
		hbox.setStyle("justify-content:center;");
		hbox.setWidth("100%");
		hbox.appendChild(btnUp);
		hbox.appendChild(btnDown);

		org.zkoss.zul.Vbox vbox = new org.zkoss.zul.Vbox();
		vbox.setStyle("align-items:center;width:100%;");
		vbox.appendChild(btn);
		vbox.appendChild(hbox);

		cell.appendChild(vbox);
		return cell;
	}

	/** 计算合格率（完工/（完工+损耗）） */
	private String calcPassRate(RoutingNodeVO vo) {
		BigDecimal delivered = vo.qtyDelivered != null ? vo.qtyDelivered : BigDecimal.ZERO;
		BigDecimal scrap = vo.qtyScrap != null ? vo.qtyScrap : BigDecimal.ZERO;
		BigDecimal total = delivered.add(scrap);
		if (total.compareTo(BigDecimal.ZERO) == 0)
			return "-";
		BigDecimal rate = delivered.multiply(new BigDecimal("100")).divide(total, 2, java.math.RoundingMode.HALF_UP);
		return rate.toPlainString() + "%";
	}

	// ════════════════════════════════════════════════════════════════════════
	// 事件处理
	// ════════════════════════════════════════════════════════════════════════

	@Override
	public void onEvent(Event event) throws Exception {
		Object src = event.getTarget();
		if (src == btnNew)
			onNew();
		else if (src == btnQuery)
			onQuery();
		else if (src == btnAddBOM)
			onAddBOM();
		else if (src == btnAddRouting)
			onAddRouting();
		else if (src == btnSubmit)
			onSubmit();
		else if (src == btnCopy)
			onCopy();
		else if (src == btnZoom)
			onZoom();
		else if (src == btnScrapCalc)
			onScrapCalc();
		else
			super.onEvent(event);
	}

	@Override
	public void valueChange(ValueChangeEvent evt) {
		if ("M_Product_ID".equals(evt.getPropertyName())) {
			onProductChange(evt.getNewValue());
		} else if ("C_OrderLine_ID".equals(evt.getPropertyName())) {
			onOrderLineChange(evt.getNewValue());
		}
//		else if ("QtyEntered".equals(evt.getPropertyName())) {
//			recalcAllBOMQty();
//		}
	}

	private void onNew() {
		currentOrder = null;
		isNewOrder = true;
		currentProductId = 0;
		formReadOnly = false;

		txtOrderNo.setValue("");
		txtProductValue.setValue("");
		txtProductName.setValue("");
		txtDocStatus.setValue(getOrderstatusName("Ready"));
		fProduct.setValue(null);
		fDocType.setValue(null);
		fDateStart.setValue(new Timestamp(System.currentTimeMillis()));
		fDatePromised.setValue(null);
		fQtyEntered.setValue(null);
		fUOM.setValue(null);
		fResource.setValue(defaultResourceId > 0 ? defaultResourceId : null);
		fPriority.setValue("5");
		Env.setContext(Env.getCtx(), m_WindowNo, "M_Product_ID", 0);
		fOrderLine.getLookup().refresh();
		fOrderLine.setValue(null);
		fImpositionCount.setValue((BigDecimal) null);
		txtDescription.setValue("");
		fBOMRatio.setValue(BigDecimal.valueOf(10000));

		bomLines.clear();
		routingNodes.clear();
//		refreshBOMList();
//		refreshRoutingList();
		setHeaderReadOnly(false);
		updateButtonState();
	}

	private void setHeaderReadOnly(boolean ro) {
		formReadOnly = ro;
		fProduct.setReadWrite(!ro);
		fDocType.setReadWrite(!ro);
		fDateStart.setReadWrite(!ro);
		fDatePromised.setReadWrite(!ro);
		fQtyEntered.setReadWrite(!ro);
		fUOM.setReadWrite(!ro);
		fResource.setReadWrite(!ro);
		fPriority.setReadWrite(!ro);
		fOrderLine.setReadWrite(!ro);
		fImpositionCount.setReadonly(ro);
		fBOMRatio.setReadonly(ro);
		txtDescription.setReadonly(ro);
		// 刷新列表（使列表单元格也变为只读）
		refreshBOMList();
		refreshRoutingList();
	}

	private void onProductChange(Object newValue) {
		if (newValue == null) {
			txtProductValue.setValue("");
			currentProductId = 0;
			return;
		}
		currentProductId = newValue instanceof Integer ? (Integer) newValue : 0;
		Env.setContext(Env.getCtx(), m_WindowNo, "M_Product_ID", currentProductId);
		fOrderLine.getLookup().refresh();
		fOrderLine.setValue(null);
		MProduct p = MProduct.get(Env.getCtx(), currentProductId);
		if (p != null) {
			txtProductValue.setValue(p.getValue());
			txtProductName.setValue(p.getName());
			fUOM.setValue(p.getC_UOM_ID());
		}
	}

	private void onOrderLineChange(Object newValue) {
		int orderLineId = (newValue instanceof Integer) ? (Integer) newValue : 0;
		loadEffectOptions(orderLineId);
		// 清空各行已选的效果名称（新订单的效果与原订单不同）
		bomLines.forEach(vo -> vo.effectName = "");
		routingNodes.forEach(vo -> vo.effectName = "");
		// 刷新列表，使效果名称下拉选项更新
		refreshBOMList();
		refreshRoutingList();
	}

	private void onQuery() {
		String docNo = txtQueryOrderNo.getValue();
		if (docNo == null || docNo.trim().isEmpty()) {
			Dialog.error(m_WindowNo, "", "请输入工程单号");
			return;
		}
		MPPOrder order = orderService.findByDocumentNo(Env.getCtx(), docNo.trim());
		if (order == null) {
			Dialog.error(m_WindowNo, "", "未找到工单：" + docNo);
			return;
		}
		loadOrder(order);
	}

	private void loadOrder(MPPOrder order) {
		if (order == null)
			return;
		currentOrder = order;
		isNewOrder = false;

		// ── 表头字段回填 ──────────────────────────────────────────────────────
		txtOrderNo.setValue(order.getDocumentNo());

		// 产品
		currentProductId = order.getM_Product_ID();
		if (fProduct != null)
			fProduct.setValue(currentProductId);
		MProduct p = MProduct.get(Env.getCtx(), currentProductId);
		if (p != null) {
			txtProductValue.setValue(p.getValue());
			txtProductName.setValue(p.getName());
		}

		// 工单类型
		fDocType.setValue(order.getC_DocTypeTarget_ID());

		// 计划日期
		fDateStart.setValue(order.getDateStartSchedule());
		fDatePromised.setValue(order.getDatePromised());

		// 生产数量 & 单位
		fQtyEntered.setValue(order.getQtyOrdered());
		fUOM.setValue(order.getC_UOM_ID());

		// 生产线/车间
		fResource.setValue(order.getS_Resource_ID());

		// 优先级：标准字段，直接用 getPriorityRule()
		String priority = order.getPriorityRule();
		if (priority != null)
			fPriority.setValue(priority);

		// 工单状态（用 orderStatus 自定义字段）
		String statusCode = order.get_ValueAsString("Orderstatus");
		txtDocStatus.setValue(getOrderstatusName(statusCode));

		// 销售订单行
		Env.setContext(Env.getCtx(), m_WindowNo, "M_Product_ID", order.getM_Product_ID());
		fOrderLine.getLookup().refresh();
		int savedOLId = order.getC_OrderLine_ID();
		fOrderLine.setValue(savedOLId > 0 ? savedOLId : null);
		// 加载效果名称选项（基于已保存的销售订单行）
		loadEffectOptions(savedOLId);

		// 拼版联数：标准字段，用 getQtyBatchSize()
		BigDecimal batchSize = order.getQtyBatchSize();
		if (batchSize != null && batchSize.compareTo(BigDecimal.ZERO) > 0)
			fImpositionCount.setValue(batchSize);

		// BOM比例：从关联的 PP_Product_BOM 读取自定义字段 nRate
		int bomId = order.getPP_Product_BOM_ID();
		if (bomId > 0) {
			MPPProductBOM bom = new MPPProductBOM(Env.getCtx(), bomId, null);
			Object nRateObj = bom.get_Value("nRate");

			// nRate 是 Integer 类型，转为 BigDecimal 用于显示
			BigDecimal nRate = BigDecimal.ZERO;
			if (nRateObj instanceof Number) {
				nRate = BigDecimal.valueOf(((Number) nRateObj).longValue());
			}
			fBOMRatio.setValue(nRate);
		}

		// 描述
		txtDescription.setValue(order.getDescription() != null ? order.getDescription() : "");

		// ── 加载 BOM 和工序列表 ───────────────────────────────────────────────
		bomLines.clear();
		routingNodes.clear();

		bomService.loadBOMLines(Env.getCtx(), order.getPP_Order_ID(), bomLines);
		bomLines.forEach(vo -> vo.qtyOnHand = getQtyOnHand(vo.productId));
		routingService.loadRoutingNodes(Env.getCtx(), order.getPP_Order_ID(), routingNodes);
		renumberRoutingNodes();

//		refreshBOMList();
//		refreshRoutingList();
		setHeaderReadOnly(true);
		updateButtonState();
	}

	private void onAddBOM() {
		BOMLineVO vo = new BOMLineVO();
		vo.lineNo = bomLines.size() + 1;
		bomLines.add(vo);
		refreshBOMList();
	}

	private void onAddRouting() {
		RoutingNodeVO vo = new RoutingNodeVO();
		vo.lineNo = routingNodes.size() + 1;
		routingNodes.add(vo);
		renumberRoutingNodes();
		refreshRoutingList();
	}

	private void onSubmit() {
		// ── 已持久化工单：不允许修改 ──────────────────────
		if (!isNewOrder && currentOrder != null) {
			Dialog.error(m_WindowNo, "", "工单已存在，不允许再次提交");
			return;
//			if (!isEditableBOMAndRouting(Env.getCtx(), currentOrder)) {
//				Dialog.error(m_WindowNo, "", "该工单类型不允许修改BOM和工序");
//				return;
//			}
//			try {
//				orderService.updateOrderBOMAndRouting(Env.getCtx(), currentOrder.getPP_Order_ID(), bomLines,
//						routingNodes);
//				Dialog.info(m_WindowNo, "", "BOM和工序已更新");
//			} catch (Exception e) {
//				log.log(Level.SEVERE, "updateOrderBOMAndRouting error", e);
//				Dialog.error(m_WindowNo, "", e.getMessage());
//			}
//			return;
		}

		// ── 新建工单：校验 ────────────────────────────────────────────────────
		if (!validateHeader())
			return;

		if (bomLines.stream().allMatch(v -> v.isDeleted || v.productId <= 0)) {
			Dialog.error(m_WindowNo, "", "BOM物料列表不能为空");
			return;
		}
		if (routingNodes.stream().allMatch(v -> v.isDeleted || v.adRoutingNodeId <= 0)) {
			Dialog.error(m_WindowNo, "", "工艺工序列表不能为空");
			return;
		}

		// 判断当前选择的工单类型是否为打样(MOF)或研发(MQO)
		boolean relaxed = isPrototypeOrRD();
		if (!validateBOMLines(relaxed))
			return;
		if (!validateRoutingNodes(relaxed))
			return;

		// 拼版联数
		BigDecimal batchSize = BigDecimal.ONE;
		if (fImpositionCount.getValue() != null) {
			try {
				batchSize = (BigDecimal) fImpositionCount.getValue();
			} catch (NumberFormatException ignored) {
			}
		}
		if (batchSize.compareTo(BigDecimal.ZERO) <= 0)
			batchSize = BigDecimal.ONE;

		// BOM比例
		BigDecimal nRate = BigDecimal.ZERO;
		if (fBOMRatio.getValue() != null) {
			try {
				nRate = new BigDecimal(fBOMRatio.getValue().toString());
			} catch (NumberFormatException ignored) {
			}
		}

		// 创建人姓名
		String createdByName = org.compiere.model.MUser.get(Env.getCtx(), Env.getAD_User_ID(Env.getCtx())).getName();

		// ── 提交 ─────────────────────────────────────────────────────────────
		try {
			MPPOrder order = orderService.submitNewOrder(Env.getCtx(), Env.getAD_Org_ID(Env.getCtx()), currentProductId,
					getIntValue(fDocType), (Timestamp) fDateStart.getValue(), // 5: dateStart
					(Timestamp) fDatePromised.getValue(), // 6: datePromised
					(BigDecimal) fQtyEntered.getValue(), // 7: qtyOrdered
					getIntValue(fUOM), // 8: uomId
					getIntValue(fResource), // 9: resourceId
					getDefaultWarehouseId(), // 10: warehouseId
					getPriorityValue(), // 11: priorityRule
					getIntValue(fOrderLine), // 12: orderLineId
					batchSize, // 13: qtyBatchSize
					nRate, // 14: nRate
					txtDescription.getValue(), // 15: description
					0, // 16: existingBOMId（新建=0）
					0, // 17: existingWFId（新建=0）
					bomLines, routingNodes, createdByName // 20: createdByName
			);

			currentOrder = order;
			isNewOrder = false;
			txtOrderNo.setValue(order.getDocumentNo());
			txtDocStatus.setValue(getOrderstatusName(order.get_ValueAsString("Orderstatus")));
			updateButtonState();
			setHeaderReadOnly(true);
			Dialog.info(m_WindowNo, "", "工单已提交并发布：" + order.getDocumentNo());

		} catch (Exception e) {
			log.log(Level.SEVERE, "onSubmit error", e);
			Dialog.error(m_WindowNo, "", e.getMessage());
		}
	}

	private void onCopy() {
		List<BOMLineVO> newBOM = new ArrayList<>();
		for (BOMLineVO vo : bomLines) {
			if (vo.isDeleted)
				continue;
			BOMLineVO c = new BOMLineVO();
			c.productId = vo.productId;
			c.productValue = vo.productValue;
			c.productName = vo.productName;
			c.uomId = vo.uomId;
			c.uomName = vo.uomName;
			c.nQtyBOM = vo.nQtyBOM;
			c.routingNodeId = vo.routingNodeId;
			c.routingNodeName = vo.routingNodeName;
			c.qtyRequiered = vo.qtyRequiered;
			c.qtyPaperTotalScrap = vo.qtyPaperTotalScrap;
			c.effectName = vo.effectName;
			newBOM.add(c);
		}
		List<RoutingNodeVO> newRouting = new ArrayList<>();
		for (RoutingNodeVO vo : routingNodes) {
			if (vo.isDeleted)
				continue;
			RoutingNodeVO c = new RoutingNodeVO();
			c.adRoutingNodeId = vo.adRoutingNodeId;
			c.routingNodeValue = vo.routingNodeValue;
			c.routingNodeName = vo.routingNodeName;
			c.description = vo.description;
			c.processdifficulty = vo.processdifficulty;
			c.colorCount = vo.colorCount;
			c.qtyRequiered = vo.qtyRequiered;
			c.qtyPaperScrap = vo.qtyPaperScrap;
			c.qtyPaperTotalScrap = vo.qtyPaperTotalScrap;
			c.effectName = vo.effectName;
			c.operationClassId = vo.operationClassId;  
			c.operationClassName = vo.operationClassName;
			newRouting.add(c);
		}
		currentOrder = null;
		isNewOrder = true;
		txtOrderNo.setValue("");
		txtDocStatus.setValue(getOrderstatusName("Ready"));
		fQtyEntered.setValue(null);
		fDateStart.setValue(new Timestamp(System.currentTimeMillis()));
		fDatePromised.setValue(null);
		fOrderLine.setValue(null);
		effectNameOptions.clear();
		bomLines.clear();
		bomLines.addAll(newBOM);
		bomLines.forEach(vo -> vo.qtyOnHand = getQtyOnHand(vo.productId));
		routingNodes.clear();
		routingNodes.addAll(newRouting);
//		refreshBOMList();
//		refreshRoutingList();
		setHeaderReadOnly(false);
		updateButtonState();
		Dialog.info(m_WindowNo, "", "已复制工单，请填写生产数量和日期后提交");
	}

	private void onZoom() {
		if (currentOrder == null) {
			Dialog.error(m_WindowNo, "", "请先查询或提交一个工单");
			return;
		}
		AEnv.zoom(MPPOrder.Table_ID, currentOrder.getPP_Order_ID());
	}

	private void onScrapCalc() {
		BigDecimal qtyOrdered = getOrderQty();
		if (qtyOrdered == null || qtyOrdered.compareTo(BigDecimal.ZERO) <= 0) {
			Dialog.error(m_WindowNo, "", "请先输入生产数量");
			return;
		}

		// 校验：工序必须有工序选择
		for (RoutingNodeVO vo : routingNodes) {
			if (vo.isDeleted)
				continue;
			if (vo.adRoutingNodeId <= 0) {
				Dialog.error(m_WindowNo, "", "工单工序列表，请先选择工序");
				return;
			}
		}
		// 注意：联数是 fImpositionCount，不是 fBOMRatio
		BigDecimal impositionCount = BigDecimal.ONE;
		if (fImpositionCount.getValue() != null) {
			try {
				impositionCount = new BigDecimal(fImpositionCount.getValue().toString());
			} catch (Exception ignored) {
			}
		}
		if (impositionCount.compareTo(BigDecimal.ZERO) <= 0)
			impositionCount = BigDecimal.ONE;

		// 调用损耗计算（calcQtyRequiered=true，同时计算标准加工数）
		orderService.applyScrapCalculation(Env.getCtx(), 0, bomLines, routingNodes, qtyOrdered, impositionCount, null,
				true);

		refreshRoutingList();
		refreshBOMList();
	}

	// ════════════════════════════════════════════════════════════════════════
	// 辅助方法
	// ════════════════════════════════════════════════════════════════════════

	private boolean validateHeader() {
		if (currentProductId <= 0) {
			Dialog.error(m_WindowNo, "", "请选择产品");
			return false;
		}
		if (getIntValue(fDocType) <= 0) {
			Dialog.error(m_WindowNo, "", "请选择工单类型");
			return false;
		}
		if (fDateStart.getValue() == null) {
			Dialog.error(m_WindowNo, "", "请填写计划开工日期");
			return false;
		}
		if (fDatePromised.getValue() == null) {
			Dialog.error(m_WindowNo, "", "请填写计划交货日期");
			return false;
		}
		BigDecimal qty = (BigDecimal) fQtyEntered.getValue();
		if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
			Dialog.error(m_WindowNo, "", "生产数量必须大于0");
			return false;
		}
		if (getIntValue(fUOM) <= 0) {
			Dialog.error(m_WindowNo, "", "请选择单位");
			return false;
		}
		// 关联销售订单：非打样/研发工单必填
		if (!isPrototypeOrRD() && getIntValue(fOrderLine) <= 0) {
			Dialog.error(m_WindowNo, "", "请选择关联销售订单");
			return false;
		}
		// 拼版联数必填（字段名替换为实际的，如 fQtyBatchSize）
		BigDecimal qtyBatchSize = (BigDecimal) fImpositionCount.getValue();
		if (qtyBatchSize == null || qtyBatchSize.compareTo(BigDecimal.ZERO) <= 0) {
			Dialog.error(m_WindowNo, "", "拼版联数必须大于0");
			return false;
		}

		// BOM比例必填（如果有对应的UI字段）
		BigDecimal nRate = (BigDecimal) fBOMRatio.getValue();
		if (nRate == null || nRate.compareTo(BigDecimal.ZERO) <= 0) {
			Dialog.error(m_WindowNo, "", "BOM比例必须大于0");
			return false;
		}
		return true;
	}

	private void updateButtonState() {
		boolean canSubmit = getBtnDisabledStatus();

		// 提交按钮：仅新建状态可用
//		btnSubmit.setDisabled(!canSubmit);
		btnSubmit.setVisible(canSubmit);

		// 工序/BOM物料 新增按钮：仅新建状态可用
		btnAddBOM.setVisible(canSubmit);
		btnAddRouting.setVisible(canSubmit);

		// 复制/透视按钮
		btnCopy.setVisible(currentOrder != null);
		btnZoom.setVisible(currentOrder != null);
		// 损耗计算按钮
		btnScrapCalc.setVisible(canSubmit);
	}

	private boolean getBtnDisabledStatus() {
//		return isNewOrder || (currentOrder != null && isEditableBOMAndRouting(Env.getCtx(), currentOrder));
		// 调整为新建工单才展示按钮
		return isNewOrder;
	}

	private int getIntValue(WTableDirEditor ed) {
		if (ed == null)
			return 0;
		Object v = ed.getValue();
		return v instanceof Integer ? (Integer) v : 0;
	}

	private BigDecimal getOrderQty() {
		Object v = fQtyEntered.getValue();
		return v instanceof BigDecimal ? (BigDecimal) v : BigDecimal.ZERO;
	}

	private int getDefaultWarehouseId() {
		return DB.getSQLValue(null,
				"SELECT M_Warehouse_ID FROM M_Warehouse WHERE AD_Org_ID=? AND IsActive='Y' ORDER BY Created",
				Env.getAD_Org_ID(Env.getCtx()));
	}

	private String getPriorityValue() {
		if (fPriority == null)
			return "5";
		Object v = fPriority.getValue();
		return v != null ? v.toString() : "5";
	}

	private static Label lbl(String text) {
		Label l = new Label(text);
		l.setStyle("font-weight:bold;text-align:right;");
		return l;
	}

	private static org.zkoss.zul.Label sectionTitle(String title) {
		org.zkoss.zul.Label l = new org.zkoss.zul.Label(title);
		l.setStyle("font-size:14px;font-weight:bold;padding:4px 0 4px 0;"
				+ "border-bottom:2px solid #1976d2;display:block;margin-top:4px;");
		return l;
	}

	private static Listcell cell(String text) {
		return new Listcell(text != null ? text : "");
	}

	/**
	 * 获取 Orderstatus 字段的 AD_Reference_Value_ID 从 AD_Column 中查询 PP_Order 表的
	 * Orderstatus 列
	 */
	private static int getOrderstatusRefId() {
		if (orderstatusRefId <= 0) {
			orderstatusRefId = DB.getSQLValue(null,
					"SELECT AD_Reference_Value_ID FROM AD_Column " + "WHERE ColumnName='Orderstatus' "
							+ "  AND AD_Table_ID=(SELECT AD_Table_ID FROM AD_Table WHERE TableName='PP_Order') "
							+ "  AND IsActive='Y'");
		}
		return orderstatusRefId;
	}

	/**
	 * 将 Orderstatus 的 code 转为 AD_Ref_List 中的 Name（中文）
	 */
	private static String getOrderstatusName(String code) {
		if (code == null || code.isEmpty())
			return "待发布";
		int refId = getOrderstatusRefId();
		if (refId > 0) {
			String name = DB.getSQLValueString(null,
					"SELECT Name FROM AD_Ref_List WHERE AD_Reference_ID=? AND Value=? AND IsActive='Y'", refId, code);
			if (name != null && !name.isEmpty())
				return name;
		}
		// fallback：refId 找不到时用硬编码映射
		switch (code) {
		case "Ready":
			return "待发布";
		case "Released":
			return "已发布";
		case "Started":
			return "已开工";
		case "InProgress":
			return "执行中";
		case "Paused":
			return "已暂停";
		case "Completed":
			return "已完成";
		case "Stored":
			return "已入库";
		case "InECNChange":
			return "ECN变更中";
		case "ChangeExecuted":
			return "已变更";
		default:
			return code;
		}
	}

	/**
	 * 工序编码生成
	 */
	private void renumberRoutingNodes() {
		int seq = 1;
		for (RoutingNodeVO vo : routingNodes) {
			if (!vo.isDeleted) {
				vo.routingNodeValue = String.format("%02d", seq);
				vo.lineNo = seq;
				seq++;
			}
		}
	}

	/**
	 * 上移
	 */
	private void onMoveRoutingUp(int listIdx) {
		// 找前一个未删除的行
		int prevIdx = -1;
		for (int i = listIdx - 1; i >= 0; i--) {
			if (!routingNodes.get(i).isDeleted) {
				prevIdx = i;
				break;
			}
		}
		if (prevIdx < 0)
			return;
		RoutingNodeVO tmp = routingNodes.get(listIdx);
		routingNodes.set(listIdx, routingNodes.get(prevIdx));
		routingNodes.set(prevIdx, tmp);
		renumberRoutingNodes();
		refreshRoutingList();
	}

	/**
	 * 下移
	 */
	private void onMoveRoutingDown(int listIdx) {
		// 找后一个未删除的行
		int nextIdx = -1;
		for (int i = listIdx + 1; i < routingNodes.size(); i++) {
			if (!routingNodes.get(i).isDeleted) {
				nextIdx = i;
				break;
			}
		}
		if (nextIdx < 0)
			return;
		RoutingNodeVO tmp = routingNodes.get(listIdx);
		routingNodes.set(listIdx, routingNodes.get(nextIdx));
		routingNodes.set(nextIdx, tmp);
		renumberRoutingNodes();
		refreshRoutingList();
	}

	/**
	 * 判断是否允许编辑 BOM/工序（打样/研发工单且非入库状态）
	 */
	private boolean isEditableBOMAndRouting(Properties ctx, MPPOrder order) {
		if (order == null)
			return false;
		MDocType docType = MDocType.get(ctx, order.getC_DocTypeTarget_ID());
		if (docType == null)
			return false;
		String name = docType.getName();
		boolean isPrototype = "打样工单".equals(name) || "研发工单".equals(name);
		boolean notInStorage = !"Stored".equals(order.get_ValueAsString("Orderstatus"));
		return isPrototype && notInStorage;
	}

	/**
	 * 判断当前选择的工单类型是否为打样(MOF)或研发(MQO)
	 */
	private boolean isPrototypeOrRD() {
		int docTypeId = getIntValue(fDocType);
		if (docTypeId <= 0)
			return false;
		String docTypeName = DB.getSQLValueString(null, "SELECT Name FROM C_DocType WHERE C_DocType_ID=?", docTypeId);
		return "打样工单".equals(docTypeName) || "研发工单".equals(docTypeName);
	}

	/**
	 * 校验BOM物料行：非打样/研发工单，比例用量必须 > 0
	 */
	private boolean validateBOMLines(boolean relaxed) {
		for (BOMLineVO vo : bomLines) {
			if (vo.isDeleted)
				continue;
			// 物料必填
			if (vo.productId <= 0) {
				Dialog.error(m_WindowNo, "", "BOM物料不能为空");
				return false;
			}
			// 所属工序必填
			if (vo.routingNodeId <= 0) {
				Dialog.error(m_WindowNo, "", "物料[" + vo.productName + "]的所属工序不能为空");
				return false;
			}
			if (!relaxed && (vo.nQtyBOM == null || vo.nQtyBOM.compareTo(BigDecimal.ZERO) <= 0)) {
				Dialog.error(m_WindowNo, "", "非打样/研发工单，物料[" + vo.productName + "]的比例用量必须大于0");
				return false;
			}

			if ((vo.qtyRequiered == null || vo.qtyRequiered.compareTo(BigDecimal.ZERO) <= 0)) {
				Dialog.error(m_WindowNo, "", "物料[" + vo.productName + "]的需求用量必须大于0");
				return false;
			}
		}
		return true;
	}

	/**
	 * 校验工序行
	 */
	private boolean validateRoutingNodes(boolean relaxed) {
		// 校验重复工序
		Set<Integer> seenNodeIds = new HashSet<>();
		for (RoutingNodeVO vo : routingNodes) {
			if (vo.isDeleted)
				continue;
//			if (relaxed)
//				continue;
			// 工序必填
			if (vo.adRoutingNodeId <= 0) {
				Dialog.error(m_WindowNo, "", "工序不能为空，请选择工序");
				return false;
			}
			// 标准加工数必填且 > 0
			if (vo.qtyRequiered == null || vo.qtyRequiered.compareTo(BigDecimal.ZERO) <= 0) {
				Dialog.error(m_WindowNo, "", "工序[" + vo.routingNodeName + "]的标准加工数必须大于0");
				return false;
			}
			// 印刷工序色数必填
			if ("印刷".equals(vo.routingNodeName)
					&& (vo.colorCount == null || vo.colorCount.compareTo(BigDecimal.ZERO) <= 0)) {
				Dialog.error(m_WindowNo, "", "[" + vo.routingNodeName + "]工序的色数必须大于0");
				return false;
			}
			if (!seenNodeIds.add(vo.adRoutingNodeId)) {
				Dialog.error(m_WindowNo, "", "工序[" + vo.routingNodeName + "]重复，工序列表不允许出现重复工序");
				return false;
			}
		}
		return true;
	}

	/**
	 * 仅更新 BOM 列表中"所属工序"下拉的选项，不重建整个列表（避免 WSearchEditor focus 引起滚动）
	 */
	private void syncBOMRoutingDropdowns() {
		rebuildRoutingNodeMap();
		for (Listitem item : bomListbox.getItems()) {
			// 找到"所属工序"那列的 Combobox（按列索引，根据实际列顺序调整）
			// 假设"所属工序"是第 N 列（0-based）
			List<Component> cells = item.getChildren();
			for (Component cell : cells) {
				if (!(cell instanceof Listcell))
					continue;
				Component child = ((Listcell) cell).getFirstChild();
				if (!(child instanceof Combobox))
					continue;
				Combobox cmb = (Combobox) child;
				if (!"routingNode".equals(cmb.getAttribute("colType")))
					continue;
				// 记住当前选中值
				Object selectedVal = cmb.getSelectedItem() != null ? cmb.getSelectedItem().getValue() : null;
				// 重建选项
				cmb.getChildren().clear();
				for (java.util.Map.Entry<Integer, String> entry : routingNodeMap.entrySet()) {
					Comboitem ci = new Comboitem(entry.getValue());
					ci.setValue(entry.getKey());
					cmb.appendChild(ci);
					if (entry.getKey().equals(selectedVal))
						cmb.setSelectedItem(ci);
				}
			}
		}
	}

	/**
	 * 根据当前组织和物料查询累计库存数量
	 * 
	 * @Title: getQtyOnHand
	 * @param productId
	 * @return
	 * @return BigDecimal
	 */
	private BigDecimal getQtyOnHand(int productId) {
		if (productId <= 0)
			return BigDecimal.ZERO;
		int orgId = (currentOrder != null) ? currentOrder.getAD_Org_ID() : Env.getAD_Org_ID(Env.getCtx());
		BigDecimal qty = DB.getSQLValueBD(null,
				"SELECT COALESCE(SUM(oh.QtyOnHand), 0) FROM M_StorageOnHand oh "
						+ "JOIN M_Locator loc ON oh.M_Locator_ID = loc.M_Locator_ID "
						+ "JOIN M_Warehouse wh ON loc.M_Warehouse_ID = wh.M_Warehouse_ID "
						+ "WHERE oh.M_Product_ID = ? AND wh.AD_Org_ID = ? AND loc.IsActive = 'Y' "
						+ "AND wh.IsActive = 'Y' AND wh.AD_Client_ID = ?",
				productId, orgId, Env.getAD_Client_ID(Env.getCtx()));
		return qty != null ? qty : BigDecimal.ZERO;
	}

	/**
	 * 根据销售订单行加载已审批效果名称列表 路径：C_OrderLine_ID → C_OrderLine.C_Order_ID →
	 * dy_samplingdemand.c_order_id → dy_graphicdesigneffect.effectstatus='RE'
	 */
	private void loadEffectOptions(int orderLineId) {
		effectNameOptions.clear();
		if (orderLineId <= 0)
			return;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try {
			ps = DB.prepareStatement("SELECT e.name " + "FROM dy_graphicdesigneffect e "
					+ "JOIN dy_samplingdemand sd ON e.dy_samplingdemand_id = sd.dy_samplingdemand_id "
					+ "WHERE sd.c_order_id = (SELECT c_order_id FROM c_orderline WHERE c_orderline_id = ?) "
					+ "  AND e.effectstatus = 'RE' " + "  AND e.isactive = 'Y' " + "  AND e.ad_client_id = ? "
					+ "ORDER BY e.name", null);
			ps.setInt(1, orderLineId);
			ps.setInt(2, Env.getAD_Client_ID(Env.getCtx()));
			rs = ps.executeQuery();
			while (rs.next()) {
				effectNameOptions.add(rs.getString(1));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "loadEffectOptions error", e);
		} finally {
			DB.close(rs, ps);
		}
	}

}