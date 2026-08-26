package com.hoifu.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.panel.ADForm;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
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
import org.zkoss.zul.Vlayout;

import com.hoifu.config.TrackingFieldConfig;
import com.hoifu.config.TrackingFieldDef;
import com.hoifu.model.MHFPPOrderNodeTracking;
import com.hoifu.service.ProcessTrackService;
import com.hoifu.service.ProcessTrackService.OperationClassInfo;

/**
 * 打样追踪详情
 */
@org.idempiere.ui.zk.annotation.Form(name = "com.hoifu.form.WPPOrderTrackingDetail")
public class WPPOrderTrackingDetail extends ADForm {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(WPPOrderTrackingDetail.class);

	private final ProcessTrackService service = new ProcessTrackService();

	private Textbox lblOrderNo = new Textbox();
	private Textbox lblProductDesc = new Textbox();
	private Vlayout groupsContainer = new Vlayout();
	private Button btnRefresh = new Button("刷新");

	/** 当前展示的工单 ID，缓存下来供"刷新"按钮重新查询使用，避免重复解析 T_Selection */
	private int currentPPOrderId = 0;

	@Override
	protected void initForm() {
		Borderlayout layout = new Borderlayout();
		layout.setStyle("height:100%;width:100%;");
		appendChild(layout);

		// ── 表头：工单号 + 产品描述 + 刷新按钮 ──
		North north = new North();
		north.setStyle("padding:8px 12px;border-bottom:1px solid #ddd;");
		Hbox headerBox = new Hbox();
		headerBox.setSpacing("32px");
		headerBox.setAlign("center");

		Hbox orderNoGroup = new Hbox();
		orderNoGroup.setSpacing("6px");
		orderNoGroup.setAlign("center");
		Label lblOrderNoTitle = new Label("工单号：");

		lblOrderNoTitle.setStyle("font-weight:bold;");
		lblOrderNo.setReadonly(true);
		lblOrderNo.setWidth("160px");
		orderNoGroup.appendChild(lblOrderNoTitle);
		orderNoGroup.appendChild(lblOrderNo);

		Hbox productGroup = new Hbox();
		productGroup.setSpacing("6px");
		productGroup.setAlign("center");
		Label lblProductTitle = new Label("产品描述：");
		lblProductTitle.setStyle("font-weight:bold;");
		lblProductDesc.setReadonly(true);
		lblProductDesc.setWidth("320px");
		productGroup.appendChild(lblProductTitle);
		productGroup.appendChild(lblProductDesc);

		headerBox.appendChild(orderNoGroup);
		headerBox.appendChild(productGroup);

		btnRefresh.setTooltiptext("重新加载最新的打样追踪数据");
		btnRefresh.addEventListener(Events.ON_CLICK, e -> onRefresh());
		headerBox.appendChild(btnRefresh);

		north.appendChild(headerBox);
		layout.appendChild(north);

		// ── 中间：各工序组分组表格容器 ──
		Center center = new Center();
		center.setAutoscroll(true);
		groupsContainer.setStyle("padding:8px;");
		center.appendChild(groupsContainer);
		layout.appendChild(center);

		// ── 从 T_Selection 取出触发该表单时选中的 PP_Order_ID ──
		int ppOrderId = resolvePPOrderIdFromSelection();
		if (ppOrderId <= 0) {
			Messagebox.show("未能获取到选中的工单信息，请返回工单列表重新选择！");
			return;
		}
		currentPPOrderId = ppOrderId;
		loadAndRender(ppOrderId);
	}

	/**
	 * 刷新按钮点击处理： 直接用已缓存的 currentPPOrderId
	 */
	private void onRefresh() {
		if (currentPPOrderId <= 0) {
			Messagebox.show("当前没有可刷新的工单信息！");
			return;
		}
		groupsContainer.getChildren().clear();
		loadAndRender(currentPPOrderId);
	}

	/**
	 * 从 AD_Process 携带的 ProcessInfo -> T_Selection 表里取出选中记录的主键。
	 */
	private int resolvePPOrderIdFromSelection() {
		ProcessInfo pi = getProcessInfo();
		if (pi == null) {
			log.warning("ProcessInfo 为空，无法从 T_Selection 取出选中的工单");
			return 0;
		}
		int pInstanceId = pi.getAD_PInstance_ID();
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, pInstanceId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (Exception e) {
			log.severe("读取 T_Selection 失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return 0;
	}

	/** 加载表头信息 + 所有分组数据并渲染 */
	private void loadAndRender(int ppOrderId) {
		Properties ctx = Env.getCtx();
		loadHeaderInfo(ctx, ppOrderId);

		// 1. 一次性查出该工单下所有追踪记录
		List<MHFPPOrderNodeTracking> allRows = service.findByOrder(ctx, ppOrderId, null);
		if (allRows.isEmpty()) {
			groupsContainer.appendChild(new Label("该工单暂无打样追踪记录。"));
			return;
		}

		// 2. 批量加载 operationclass 字典（避免逐组循环单独查库）
		Map<Integer, OperationClassInfo> classMap = service.loadAllOperationClass();

		// 3. 批量加载工序名称（PP_Order_Node.Name）和更新人姓名，避免 N+1
		Map<Integer, String> nodeNameMap = loadNodeNameMap(ctx, ppOrderId);
		Map<Integer, String> userNameMap = loadUserNameMap(ctx, allRows);

		// 4. 不再使用写死的固定顺序，按该工单实际工序编码（PP_Order_Node.Value）
		// 的先后顺序动态计算分组显示顺序，丝印/镭射转印仍合并为一组
		List<String> groupOrder = resolveGroupOrder(ctx, ppOrderId);

		// 4.1 兜底：如果动态顺序里没有覆盖某些实际存在追踪记录的分组
		// （比如 AD_Routing_Node/operationclass 数据异常导致查不到），
		// 把这些遗漏的分组按 operationclass_ID 补到末尾，保证数据不会因为顺序表缺失而消失不显示
		for (MHFPPOrderNodeTracking po : allRows) {
			OperationClassInfo info = classMap.get(po.getoperationclass_ID());
			if (info == null || info.value == null)
				continue;
			String groupKey = TrackingFieldConfig.OC_LASER_TRANSFER.equals(info.value)
					? TrackingFieldConfig.OC_SILKSCREEN
					: info.value;
			if (!groupOrder.contains(groupKey)) {
				groupOrder.add(groupKey);
			}
		}

		// 5. 按算出来的顺序逐组渲染，没有数据的组跳过
		for (String value : groupOrder) {
			List<MHFPPOrderNodeTracking> groupRows = filterByOperationClassValue(allRows, classMap, value);
			if (groupRows.isEmpty())
				continue;
			renderGroup(value, groupRows, nodeNameMap, userNameMap);
		}
	}

	/**
	 * 按该工单实际的工序编码（PP_Order_Node.Value）先后顺序，动态确定各工序组的显示顺序。 查询工单下所有工序，按 Value 升序 join
	 * 出每个工序对应的 operationclass.Value， 按出现顺序去重后即为分组显示顺序。 丝印(6)/镭射转印(4)合并规则：查询到 "4" 时按
	 * "6" 记入顺序表， 只在两者中先出现的那个位置占一个坑，实现"合并组的顺序位置取先出现的编码"。
	 */
	private List<String> resolveGroupOrder(Properties ctx, int ppOrderId) {
		List<String> order = new ArrayList<>();
		String sql = "SELECT oc.Value AS OcValue " + "FROM PP_Order_Node pon "
				+ "JOIN AD_Routing_Node arn ON arn.AD_Routing_Node_ID = pon.AD_Routing_Node_ID "
				+ "JOIN operationclass oc ON oc.operationclass_ID = arn.operationclass_ID "
				+ "WHERE pon.PP_Order_ID=? AND pon.AD_Client_ID=? " + "AND arn.IsActive='Y' AND oc.IsActive='Y' "
				+ "ORDER BY pon.Value";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, ppOrderId);
			pstmt.setInt(2, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String ocValue = rs.getString("OcValue");
				if (ocValue == null)
					continue;
				String groupKey = TrackingFieldConfig.OC_LASER_TRANSFER.equals(ocValue)
						? TrackingFieldConfig.OC_SILKSCREEN
						: ocValue;
				if (!order.contains(groupKey)) {
					order.add(groupKey);
				}
			}
		} catch (Exception e) {
			log.severe("按工序编码加载分组顺序失败(PP_Order_ID=" + ppOrderId + "): " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return order;
	}

	/**
	 * 按 operationclass.Value 过滤记录。"6"(丝印) 分组时一并合并 "4"(镭射转印) 的数据，
	 * 因为两者共用同一套字段模板，在详情页需要合并展示为一张表。 合并后按业务要求排序——丝印记录整体排在镭射转印记录之前；
	 * 同一子分组内部（比如多条丝印记录，即"丝印1、丝印2"）按主键（录入先后顺序）排序。
	 */
	private List<MHFPPOrderNodeTracking> filterByOperationClassValue(List<MHFPPOrderNodeTracking> allRows,
			Map<Integer, OperationClassInfo> classMap, String value) {
		List<MHFPPOrderNodeTracking> result = new ArrayList<>();
		boolean mergeLaserTransfer = TrackingFieldConfig.OC_SILKSCREEN.equals(value);
		for (MHFPPOrderNodeTracking po : allRows) {
			OperationClassInfo info = classMap.get(po.getoperationclass_ID());
			if (info == null)
				continue;
			if (value.equals(info.value)
					|| (mergeLaserTransfer && TrackingFieldConfig.OC_LASER_TRANSFER.equals(info.value))) {
				result.add(po);
			}
		}
		if (mergeLaserTransfer) {
			result.sort(Comparator.comparingInt((MHFPPOrderNodeTracking po) -> {
				OperationClassInfo info = classMap.get(po.getoperationclass_ID());
				String v = info == null ? "" : info.value;
				return TrackingFieldConfig.OC_SILKSCREEN.equals(v) ? 0 : 1;
			}).thenComparingInt(MHFPPOrderNodeTracking::get_ID));
		}
		return result;
	}

	/** 渲染单个工序组的分组标题 + 表格 */
	private void renderGroup(String operationClassValue, List<MHFPPOrderNodeTracking> rows,
			Map<Integer, String> nodeNameMap, Map<Integer, String> userNameMap) {
		Label title = new Label(groupTitle(operationClassValue));
		title.setStyle("font-weight:bold;font-size:14px;margin-top:10px;");
		groupsContainer.appendChild(title);

		List<TrackingFieldDef> fields = TrackingFieldConfig.getFields(operationClassValue);

		boolean hasTextarea = false;
		for (TrackingFieldDef def : fields) {
			if (def.isMultiLine()) {
				hasTextarea = true;
				break;
			}
		}

		Grid grid = new Grid();
		grid.setStyle("width:100%;");

		Columns columns = new Columns();
		columns.setSizable(true);
		Column colNodeName = new Column("工序名称");
		colNodeName.setWidth("100px");
		columns.appendChild(colNodeName);
		for (TrackingFieldDef def : fields) {
			Column col = new Column(def.label);
			if (hasTextarea) {
				if (def.isMultiLine()) {
					col.setHflex("1");
				} else {
					col.setWidth("100px");
				}
			}
			columns.appendChild(col);
		}
		Column colOperator = new Column("操作员");
		colOperator.setWidth("90px");
		columns.appendChild(colOperator);
		Column colOperateTime = new Column("操作时间");
		colOperateTime.setWidth("150px");
		columns.appendChild(colOperateTime);
		grid.appendChild(columns);

		Rows gridRows = new Rows();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (MHFPPOrderNodeTracking po : rows) {
			Row row = new Row();
			Label lblNodeName = new Label(nodeNameMap.getOrDefault(po.getPP_Order_Node_ID(), ""));
			row.appendChild(lblNodeName);
			for (TrackingFieldDef def : fields) {
				String v = po.get_ValueAsString(def.columnName);
				Label lbl = new Label(v == null ? "" : v);
				if (def.isMultiLine())
					lbl.setPre(true);
				row.appendChild(lbl);
			}
			Label lblOperator = new Label(userNameMap.getOrDefault(po.getUpdatedBy(), ""));
			row.appendChild(lblOperator);
			Label lblOperateTime = new Label(po.getUpdated() == null ? "" : sdf.format(po.getUpdated()));
			row.appendChild(lblOperateTime);
			gridRows.appendChild(row);
		}
		grid.appendChild(gridRows);
		groupsContainer.appendChild(grid);
	}

	/**
	 * 分组标题：丝印/镭射转印合并显示为一个标题
	 */
	private String groupTitle(String operationClassValue) {
		if (TrackingFieldConfig.OC_SILKSCREEN.equals(operationClassValue)) {
			return "丝印/镭射转印";
		}
		if (TrackingFieldConfig.OC_OFFSET_PRINT.equals(operationClassValue))
			return "胶印";
		if (TrackingFieldConfig.OC_GRAVURE.equals(operationClassValue))
			return "凹印";
		if (TrackingFieldConfig.OC_INKJET_CODE.equals(operationClassValue))
			return "喷码";
		if (TrackingFieldConfig.OC_SINGLE_GRAVURE.equals(operationClassValue))
			return "单凹";
		if (TrackingFieldConfig.OC_HOT_STAMP.equals(operationClassValue))
			return "烫金";
		if (TrackingFieldConfig.OC_EMBOSS.equals(operationClassValue))
			return "凹凸压纹";
		if (TrackingFieldConfig.OC_DIE_CUT.equals(operationClassValue))
			return "模切";
		return "工序组 " + operationClassValue;
	}

	/** 加载表头信息：工单号 + 产品编码/名称 */
	private void loadHeaderInfo(Properties ctx, int ppOrderId) {
		String sql = "SELECT o.DocumentNo, p.Value AS ProductValue, p.Name AS ProductName "
				+ "FROM PP_Order o JOIN M_Product p ON p.M_Product_ID = o.M_Product_ID "
				+ "WHERE o.PP_Order_ID=? AND o.AD_Client_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, ppOrderId);
			pstmt.setInt(2, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				lblOrderNo.setValue(rs.getString("DocumentNo"));
				lblProductDesc.setValue(rs.getString("ProductValue") + "_" + rs.getString("ProductName"));
			}
		} catch (Exception e) {
			log.severe("加载工单头信息失败(PP_Order_ID=" + ppOrderId + "): " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/** 批量查询该工单下所有工序（PP_Order_Node）的名称，key=PP_Order_Node_ID，避免逐行渲染时 N+1 查询 */
	private Map<Integer, String> loadNodeNameMap(Properties ctx, int ppOrderId) {
		Map<Integer, String> map = new HashMap<>();
		String sql = "SELECT PP_Order_Node_ID, Name FROM PP_Order_Node WHERE PP_Order_ID=? AND AD_Client_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, ppOrderId);
			pstmt.setInt(2, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				map.put(rs.getInt(1), rs.getString(2));
			}
		} catch (Exception e) {
			log.severe("加载工序名称字典失败(PP_Order_ID=" + ppOrderId + "): " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return map;
	}

	/** 批量查询涉及到的更新人姓名，key=AD_User_ID，避免逐行渲染时对 MUser 反复查库 */
	private Map<Integer, String> loadUserNameMap(Properties ctx, List<MHFPPOrderNodeTracking> rows) {
		Map<Integer, String> map = new LinkedHashMap<>();
		for (MHFPPOrderNodeTracking po : rows) {
			int userId = po.getUpdatedBy();
			if (userId <= 0 || map.containsKey(userId))
				continue;
			MUser user = MUser.get(ctx, userId);
			map.put(userId, user == null ? String.valueOf(userId) : user.getName());
		}
		return map;
	}

}