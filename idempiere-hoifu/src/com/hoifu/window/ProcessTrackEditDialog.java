package com.hoifu.window;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.hoifu.config.TrackingFieldConfig;
import com.hoifu.config.TrackingFieldDef;
import com.hoifu.enums.FieldControlType;
import com.hoifu.model.MHFPPOrderNodeTracking;
import com.hoifu.service.ProcessTrackService;

/**
 * "打样追踪"编辑弹窗——在 PPOrderNodeInfoWindow 选中一条 PP_Order_Node 记录后 点击"打样追踪"按钮打开
 */
public class ProcessTrackEditDialog extends Window implements EventListener<Event> {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(ProcessTrackEditDialog.class);

	private final ProcessTrackService service = new ProcessTrackService();

	private final int ppOrderId;
	private final int ppOrderNodeId;
	private final int operationClassId;
	private final String operationClassValue;
	private final List<TrackingFieldDef> fieldDefs;

	private Rows rows;
	/** 当前仍保留在界面上的行：Row 组件、对应的编辑器列表、对应的 PO，三个 List 下标一一对应 */
	private final List<Row> rowList = new ArrayList<>();
	private final List<List<Textbox>> editorList = new ArrayList<>();
	private final List<MHFPPOrderNodeTracking> poList = new ArrayList<>();
	/** 已点击删除、但尚未真正落库的记录，等待"确认"时统一物理删除 */
	private final List<MHFPPOrderNodeTracking> pendingDeleteList = new ArrayList<>();

	private ConfirmPanel confirmPanel;
	private Button btnAdd;

	// ── 表头只读展示字段 ——
	private Textbox lblOrderNo;
	private Textbox lblNodeName;
	private Textbox lblProductDesc;

	/**
	 * @param ppOrderId     工单ID，来自选中行所属工单
	 * @param ppOrderNodeId 工序实例ID，来自选中行（PPOrderNodeInfoWindow 已强制单选）
	 */
	public ProcessTrackEditDialog(int ppOrderId, int ppOrderNodeId) {
		this.ppOrderId = ppOrderId;
		this.ppOrderNodeId = ppOrderNodeId;

		ProcessTrackService.OperationClassInfo ocInfo = service.getOperationClassInfo(ppOrderNodeId);
		if (ocInfo == null) {
			throw new IllegalStateException("未能查询到该工序对应的工序组信息，PP_Order_Node_ID=" + ppOrderNodeId);
		}
		this.operationClassId = ocInfo.operationClassId;
		this.operationClassValue = ocInfo.value;
		this.fieldDefs = TrackingFieldConfig.getFields(ocInfo.value);
		if (fieldDefs.isEmpty()) {
			throw new IllegalStateException("该工序组（Name=" + ocInfo.name + "）未配置打样追踪字段模板，不支持打样追踪");
		}

		init();
		loadHeaderInfo();
		loadExistingData();
	}

	private void init() {
		setTitle("打样追踪");
		setSclass("popup-dialog");
		setBorder("normal");
		setClosable(true);
		setSizable(true);
		setShadow(true);
		setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);
		ZKUpdateUtil.setWindowWidthX(this, 1200);
		ZKUpdateUtil.setWindowHeightX(this, 700);

		Borderlayout layout = new Borderlayout();
		layout.setHflex("1");
		layout.setVflex("1");
		appendChild(layout);

		// ── 顶部区域：第一行表头信息（只读），第二行工具栏（新增按钮） ──
		North north = new North();
		Vlayout northLayout = new Vlayout();
		northLayout.setStyle("width:100%;");

		// 第一行：工单号 / 工序名称 / 产品名称，只读文本框展示
		Hbox headerBox = new Hbox();
		headerBox.setStyle("padding: 6px 8px;");
		headerBox.setSpacing("32px"); // 组间距：工单号组/工序名称组/产品名称组之间
		headerBox.setAlign("center");

		Hbox orderNoGroup = new Hbox();
		orderNoGroup.setSpacing("6px"); // 标签值间距：本组内"工单号："与其值之间
		orderNoGroup.setAlign("center");
		Label lblOrderNoTitle = new Label("工单号：");
		lblOrderNoTitle.setStyle("font-weight:bold;");
		lblOrderNo = new Textbox();
		lblOrderNo.setReadonly(true);
		ZKUpdateUtil.setWidth(lblOrderNo, "150px");
		orderNoGroup.appendChild(lblOrderNoTitle);
		orderNoGroup.appendChild(lblOrderNo);

		Hbox nodeNameGroup = new Hbox();
		nodeNameGroup.setSpacing("6px");
		nodeNameGroup.setAlign("center");
		Label lblNodeNameTitle = new Label("工序名称：");
		lblNodeNameTitle.setStyle("font-weight:bold;");
		lblNodeName = new Textbox();
		lblNodeName.setReadonly(true);
		ZKUpdateUtil.setWidth(lblNodeName, "150px");
		nodeNameGroup.appendChild(lblNodeNameTitle);
		nodeNameGroup.appendChild(lblNodeName);

		Hbox productGroup = new Hbox();
		productGroup.setSpacing("6px");
		productGroup.setAlign("center");
		Label lblProductTitle = new Label("产品名称：");
		lblProductTitle.setStyle("font-weight:bold;");
		lblProductDesc = new Textbox();
		lblProductDesc.setReadonly(true);
		ZKUpdateUtil.setWidth(lblProductDesc, "350px");
		productGroup.appendChild(lblProductTitle);
		productGroup.appendChild(lblProductDesc);

		headerBox.appendChild(orderNoGroup);
		headerBox.appendChild(nodeNameGroup);
		headerBox.appendChild(productGroup);
		northLayout.appendChild(headerBox);

		// 第二行：新增按钮工具栏
		Hbox toolbar = new Hbox();
		toolbar.setStyle("padding: 6px 8px;");
		btnAdd = new Button("新增");
		btnAdd.addEventListener(Events.ON_CLICK, e -> onAddRow());
		toolbar.appendChild(btnAdd);
		northLayout.appendChild(toolbar);

		north.appendChild(northLayout);
		layout.appendChild(north);

		// ── 中间内容区：动态生成的表格 ──
		Center center = new Center();
		center.setStyle("padding: 8px;");
		Grid grid = new Grid();
		ZKUpdateUtil.setWidth(grid, "100%");

		Columns columns = new Columns();
		columns.setSizable(true); // 允许用户拖拽调整列宽
		grid.appendChild(columns);

		// 本组是否包含至少一个 TEXTAREA 字段——只有包含时才把 TEXT 列固定宽度，
		// 否则全部交给 hflex 自适应
		boolean hasTextarea = false;
		for (TrackingFieldDef def : fieldDefs) {
			if (def.controlType == FieldControlType.TEXTAREA) {
				hasTextarea = true;
				break;
			}
		}

		for (TrackingFieldDef def : fieldDefs) {
			Column col = new Column(def.mandatory ? def.label + " *" : def.label);
			if (hasTextarea) {
				if (def.controlType == FieldControlType.TEXTAREA) {
					col.setHflex("1"); // 文本域占据弹性空间
				} else {
					col.setWidth("100px"); // 本组存在文本域时，普通文本列固定宽度，把空间让给文本域
				}
			} else {
				col.setHflex("1"); // 本组全是普通文本字段，全部自适应，不做固定
			}
			columns.appendChild(col);
		}
		// 末尾一列放"删除"按钮
		Column opCol = new Column("操作");
		opCol.setWidth("90px");
		columns.appendChild(opCol);

		rows = new Rows();
		grid.appendChild(rows);
		center.appendChild(grid);
		layout.appendChild(center);

		// ── 底部按钮区：确认/取消 ──
		South south = new South();
		south.setSclass("dialog-footer");
		confirmPanel = new ConfirmPanel(true);
		confirmPanel.addActionListener(this);
		south.appendChild(confirmPanel);
		layout.appendChild(south);

		addEventListener(Events.ON_CANCEL, e -> detach());
	}

	/**
	 * 加载表头三个只读字段：工单号、工序名称、产品描述。
	 */
	private void loadHeaderInfo() {
		String sql = "SELECT o.DocumentNo, n.Name AS NodeName, p.Value AS ProductValue, p.Name AS ProductName "
				+ "FROM PP_Order o " + "JOIN PP_Order_Node n ON n.PP_Order_ID = o.PP_Order_ID "
				+ "JOIN M_Product p ON p.M_Product_ID = o.M_Product_ID "
				+ "WHERE o.PP_Order_ID = ? AND n.PP_Order_Node_ID = ? AND o.AD_Client_ID = ?";
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, ppOrderId);
			pstmt.setInt(2, ppOrderNodeId);
			pstmt.setInt(3, Env.getAD_Client_ID(Env.getCtx()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				lblOrderNo.setValue(rs.getString("DocumentNo"));
				lblNodeName.setValue(rs.getString("NodeName"));
				lblProductDesc.setValue(rs.getString("ProductValue") + "_" + rs.getString("ProductName"));
			}
		} catch (Exception e) {
			log.severe("加载工单/工序表头信息失败(PP_Order_ID=" + ppOrderId + ", PP_Order_Node_ID=" + ppOrderNodeId + "): "
					+ e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/** 打开弹窗时按 PP_Order_Node_ID 查询已有记录，逐条渲染为初始行 */
	private void loadExistingData() {
		List<MHFPPOrderNodeTracking> existing = service.findByOrderNode(Env.getCtx(), ppOrderNodeId, null);
		for (MHFPPOrderNodeTracking po : existing) {
			appendRow(po);
		}
	}

	/** "新增"：内存追加一个空白行（未持久化），三个外键预先填好 */
	private void onAddRow() {
		MHFPPOrderNodeTracking po = service.newRecord(Env.getCtx(), ppOrderId, ppOrderNodeId, operationClassId, null);
		appendRow(po);
	}

	/** 把一条 PO（新增的空白行 或 查询回显的已有行）渲染成一行编辑器 */
	private void appendRow(MHFPPOrderNodeTracking po) {
		Row row = new Row();
		List<Textbox> editors = new ArrayList<>();

		for (TrackingFieldDef def : fieldDefs) {
			Textbox tb = new Textbox();
			if (def.controlType == FieldControlType.TEXTAREA) {
				tb.setMultiline(true);
				tb.setRows(3);
			}
			String val = po.get_ValueAsString(def.columnName);
			if (val != null) {
				tb.setValue(val);
			}
			ZKUpdateUtil.setHflex(tb, "1");
			editors.add(tb);
			row.appendChild(tb);
		}

		// 删除按钮：点击即从界面移除并标记待删除
		Button btnDel = new Button("删除");
		btnDel.setIconSclass("z-icon-trash");
		btnDel.addEventListener(Events.ON_CLICK, e -> onDeleteRow(row));
		row.appendChild(btnDel);

		rows.appendChild(row);
		rowList.add(row);
		editorList.add(editors);
		poList.add(po);
	}

	/**
	 * 删除：不再弹二次确认，行为与新增/编辑一致——只在内存中操作。 已持久化记录（get_ID() > 0）从界面移除后放入
	 * pendingDeleteList， 等待"确认"时统一物理删除；未持久化的新增行直接从内存丢弃。
	 */
	private void onDeleteRow(Row row) {
		int idx = rowList.indexOf(row);
		if (idx < 0)
			return;

		MHFPPOrderNodeTracking po = poList.get(idx);
		if (po.get_ID() > 0) {
			pendingDeleteList.add(po);
		}

		rowList.remove(idx);
		editorList.remove(idx);
		poList.remove(idx);
		row.detach();
	}

	@Override
	public void onEvent(Event event) throws Exception {
		String targetId = event.getTarget().getId();
		if (ConfirmPanel.A_OK.equals(targetId)) {
			onConfirm();
		} else if (ConfirmPanel.A_CANCEL.equals(targetId)) {
			// 取消：不做任何持久化，pendingDeleteList 里的"待删除"记录也一并丢弃
			// （数据库记录未被真正删除，界面关闭后如重新打开仍会显示这些记录）
			detach();
		}
	}

	/**
	 * 确认： 1) 先把界面上仍保留的每一行的值写回对应 PO，并逐行做必填校验（不落库）； 2) 校验全部通过后，开启一个显式事务：
	 * 先物理删除pendingDeleteList 中的记录，再保存 poList 中新增/编辑的记录；
	 * 3)任意一步失败，整体回滚，不产生半成品数据；全部成功才提交并关闭弹窗。
	 */
	private void onConfirm() {
		// 第一步：写回界面值 + 逐行校验，不落库
		for (int i = 0; i < poList.size(); i++) {
			MHFPPOrderNodeTracking po = poList.get(i);
			List<Textbox> editors = editorList.get(i);
			for (int j = 0; j < fieldDefs.size(); j++) {
				po.set_ValueOfColumn(fieldDefs.get(j).columnName, editors.get(j).getValue());
			}

			// 只有新增行（尚未持久化）才需要补写外键；已持久化的编辑行不再重复赋值，
			if (po.get_ID() <= 0) {
				po.setPP_Order_ID(ppOrderId);
				po.setPP_Order_Node_ID(ppOrderNodeId);
				po.setoperationclass_ID(operationClassId);
			}

			String error = service.validate(po, operationClassValue);
			if (error != null) {
				Messagebox.show("第 " + (i + 1) + " 行：" + error);
				return; // 校验失败，直接返回，不做任何持久化
			}
		}

		// 第二步：全部校验通过，开启显式事务，删除 + 保存一起提交/回滚
		String trxName = Trx.createTrxName("PPOrderNodeTrack");
		Trx trx = Trx.get(trxName, true);
		try {
			// 2.1 先执行标记过的删除
			for (MHFPPOrderNodeTracking delPo : pendingDeleteList) {
				delPo.set_TrxName(trxName);
				delPo.deleteEx(true);
			}


			// 2.2 再保存所有仍保留的新增/编辑记录
			for (MHFPPOrderNodeTracking po : poList) {
				po.set_TrxName(trxName);
				if (!po.save()) {
					trx.rollback();
					Messagebox.show("保存失败：" + po.get_ValueAsString("EffectName"));
					return;
				}
			}

			trx.commit();
			pendingDeleteList.clear();
			detach();
		} catch (Exception e) {
			trx.rollback();
			log.severe("提交打样追踪信息失败: " + e.getMessage());
			Messagebox.show("保存过程中发生异常: " + e.getMessage());
		} finally {
			trx.close();
		}
	}
}