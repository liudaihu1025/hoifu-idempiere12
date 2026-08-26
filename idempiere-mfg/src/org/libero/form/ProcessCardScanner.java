package org.libero.form;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Timestamp;  
import java.util.ArrayList;  
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  
import java.util.Vector;  
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.component.Borderlayout;  
import org.adempiere.webui.component.Button;  
import org.adempiere.webui.component.ConfirmPanel;  
import org.adempiere.webui.component.GridFactory;  
import org.adempiere.webui.component.Label;  
import org.adempiere.webui.component.ListModelTable;  
import org.adempiere.webui.component.ListboxFactory;  
import org.adempiere.webui.component.NumberBox;  
import org.adempiere.webui.component.Panel;  
import org.adempiere.webui.component.Row;  
import org.adempiere.webui.component.Rows;  
import org.adempiere.webui.component.WListbox;  
import org.adempiere.webui.editor.WSearchEditor;  
import org.adempiere.webui.panel.ADForm;  
import org.adempiere.webui.panel.CustomForm;  
import org.adempiere.webui.panel.IFormController;  
import org.adempiere.webui.util.ZKUpdateUtil;  
import org.adempiere.webui.window.Dialog;  
import org.compiere.minigrid.IDColumn;  
import org.compiere.model.MColumn;  
import org.compiere.model.MLookup;  
import org.compiere.model.MLookupFactory;  
import org.compiere.model.MRefList;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.DisplayType;  
import org.compiere.util.Env;  
import org.compiere.util.Language;  
import org.compiere.util.Msg;  
import org.compiere.util.Trx;  
import org.compiere.util.Util;  
import org.libero.model.MPPOrderNode;  
import org.libero.model.MPPProcessCard;  
import org.libero.model.MPPProcessCardRecord;  
import org.zkoss.zk.ui.event.Event;  
import org.zkoss.zk.ui.event.EventListener;  
import org.zkoss.zk.ui.event.Events;  
import org.zkoss.zul.Center;  
import org.zkoss.zul.Column;  
import org.zkoss.zul.Columns;  
import org.zkoss.zul.Grid;  
import org.zkoss.zul.North;  
import org.zkoss.zul.Paging;  
import org.zkoss.zul.Separator;  
import org.zkoss.zul.South;  
import org.zkoss.zul.Textbox;  
import org.zkoss.zul.Vbox;  
import org.zkoss.zul.Vlayout;  
import org.zkoss.zul.Window;  
import org.zkoss.zul.event.ZulEvents;  
  
/**  
 * 流程卡生产记录录入窗体 (PP_Process_Card_Record)。  
 * <p>  
 * 三段式布局（组合方式持有 CustomForm 字段，而不是 extends ADForm）：  
 * <ul>  
 *   <li>North: 参数区，分两排 —— 第一排 AD_Routing_Node_ID / S_Resource_ID / C_WorkTeam_ID / AD_User_ID  
 *       （均为可选查询过滤条件，AD_User_ID 变更时会自动带出另外三个默认值）；  
 *       第二排是一个跨列的 CardNo 文本框，回车触发开工/完工逻辑（无独立"开始"按钮）</li>  
 *   <li>Center: 已存在的 PP_Process_Card_Record 记录列表（含分页），数据源为 RV_Process_Card_Scanner 视图；  
 *       列头/参数名称全部为硬编码中文（不走 AD_Element/Msg.translate），DateStart/DateFinish 按  
 *       "日期+时间"格式展示</li>  
 *   <li>South: 操作按钮区（刷新 / 完成），参照信息窗口（InfoPanel/InfoGeneralPanel）的样式</li>  
 * </ul>  
 */  
public class ProcessCardScanner implements IFormController, EventListener<Event>  
{  
	private static final CLogger log = CLogger.getCLogger(ProcessCardScanner.class);  
  
	private static final String STATUS_INPROGRESS = "InProgress";  
	private static final String STATUS_COMPLETED = "Completed";  
  
	/** PP_Cost_Collector.CostCollectorType 中代表"报工"类型的值，用于查询默认参数 */  
	private static final String COST_COLLECTOR_TYPE_ACTIVITY_CONTROL = "160";  
	private static final String SYSCONFIG_FINISH_GAP_MINUTES = "PP_PROCESS_CARD_FINISH_GAP_MINUTES";  
	private static final int DEFAULT_FINISH_GAP_MINUTES = 8;
	/** 分页每页行数 */  
	private static final int PAGE_SIZE = 20;  
  
	/** UI Form 实例 - 组合方式持有，而不是 extends ADForm/CustomForm */  
	private CustomForm form = new CustomForm();  
  
	private int m_WindowNo;  
  
	// ---- North 第一排：4 个可选查询过滤参数 ----  
	private Panel parameterPanel = new Panel();  
  
	private Label routingNodeLabel = new Label();  
	/** AD_Routing_Node_ID（标准工序）参数（可选） */  
	private WSearchEditor routingNodeField;  
  
	private Label resourceLabel = new Label();  
	/** S_Resource_ID 参数（可选） */  
	private WSearchEditor resourceField;  
  
	private Label workTeamLabel = new Label();  
	/** C_WorkTeam_ID 参数（可选） */  
	private WSearchEditor workTeamField;  
  
	private Label userLabel = new Label();  
	/** AD_User_ID（作业员）参数（可选），默认取当前登录用户；变更时自动带出上面三个字段的默认值 */  
	private WSearchEditor userField;  
  
	// ---- North 第二排：CardNo 文本框 ----  
	private Label cardNoLabel = new Label();  
	/** 生产流程卡号，用于查询 PP_Process_Card_ID；回车触发开工/完工逻辑（无独立"开始"按钮） */  
	private Textbox cardNoField = new Textbox();  
  
	// ---- Center: 已存在的 PP_Process_Card_Record 列表（来自 RV_Process_Card_Scanner 视图） ----  
	/** 自适应内容宽度的数据表，效果类似信息窗口子表格 */  
	private WListbox recordTable = ListboxFactory.newDataTableAutoSize();  
  
	/** 分页组件，参考信息窗口 InfoPanel 的用法 */  
	private Paging paging = new Paging();  
	/** 当前查询总行数 */  
	private int m_count = 0;  
	/** 当前页号（0-based） */  
	private int m_pageNo = 0;  
  
	/** 每一行渲染出来的原始 CardStatus 值（未翻译），key=PP_Process_Card_Record_ID，用于按钮启用判定 */  
	private Map<Integer, String> m_rowStatusMap = new HashMap<>();  
  
	/** 是否已经初始化过 recordTable 的列头（只需要建一次，之后只刷新数据） */  
	private boolean m_columnsInitialized = false;  
  
	// ---- South: 操作按钮（参照信息窗口样式：Separator + ConfirmPanel），只保留 刷新 / 完成 两个按钮 ----  
	private ConfirmPanel confirmPanel;  
	/** 刷新按钮 */  
	private Button bRefresh = new Button();  
	/** "完成"按钮：需要先在列表中选中一条 CardStatus=InProgress 的记录才能点击 */  
	private Button bFinish = new Button();  
  
	/** 当前用户在列表中手动选中的记录主键（0 表示未选中任何行） */  
	private int m_selectedRecord_ID = 0;  

	/** CardStatus 字段对应的 AD_Reference_Value_ID（List 参照），用于把存储值转换为展示名称 */  
	private int m_cardStatusRefId = 0;  
  
	/** 各外键列对应的 AD_Table_ID，预先缓存，用于 MLookup.getIdentifier() 转换标识列文本 */  
	private int m_orgTableId;  
	private int m_orderTableId;  
	private int m_cardTableId;  
	private int m_nodeTableId;  
	private int m_workTeamTableId;  
	private int m_resourceTableId;  
	/** AD_Routing_Node（标准工序）对应的 AD_Table_ID */  
	private int m_routingNodeTableId;  
	/** AD_User 对应的 AD_Table_ID */  
	private int m_userTableId;  
  
	/**  
	 * 固定查询基础 SQL（不含 WHERE），供列表查询与 COUNT 查询共用。  
	 * 注意：AD_Client_ID 已不再需要（不展示、不使用），CardNo 保留在 SELECT 里  
	 * 仅用于 CardNo 文本框的模糊过滤条件，本身不再作为一列展示。  
	 */  
	private static final String BASE_SELECT =  
			"SELECT PP_Process_Card_Record_ID, AD_Org_ID, PP_Order_ID, "  
			+ "PP_Process_Card_ID, CardNo, PP_Order_Node_ID, AD_Routing_Node_ID, "  
			+ "C_WorkTeam_ID, S_Resource_ID, AD_User_ID, "  
			+ "DateStart, MovementQty, ScrappedQty, DateFinish, CardStatus,Qty,IsWorkReport "  
			+ "FROM RV_Process_Card_Scanner";  
  
	/**  
	 * 列表列头（硬编码中文，不走 AD_Element/Msg.translate），与  
	 * {@link #renderColumnValues(ResultSet)} 里 Vector 组装顺序严格一一对应。  
	 * 第一列是隐藏的单选框列，标题为空字符串。  
	 */  
	private static final String[] COLUMN_HEADERS = {  
			"",          // 单选框列  
			"组织",  
			"生产工单",  
			"流程卡号",  
			"工单工序",  
			"标准工序",  
			"机台",  
			"班组",  
			"作业员",  
			"开始时间",  
			"标准卡板数",
			"正品数量",  
			"废品数量",  
			"完成时间",  
			"流程卡状态",
			"是否报工"
	};  
  
	/**  
	 * 与 {@link #COLUMN_HEADERS} 一一对应的 AD_Reference_ID，用于控制单元格渲染格式；  
	 * 只有"开始时间"/"完成时间"两列需要指定 {@link DisplayType#DateTime}（日期+时间格式），  
	 * 其余列传 0（默认格式）。  
	 */  
	private static final int[] COLUMN_REF_IDS = {  
			0,  
			0,  
			0,  
			0,  
			0,  
			0,  
			0,  
			0,  
			0,  
			DisplayType.DateTime, // 开始时间  
			0, 
			0,  
			0,  
			DisplayType.DateTime, // 完成时间  
			0,
			0
	};  
  
	/**  
	 * 默认构造函数  
	 */  
	public ProcessCardScanner()  
	{  
		super();  
		try  
		{  
			m_WindowNo = form.getWindowNo();  
  
			// 预取 CardStatus 的 List 参照 ID  
			MColumn statusColumn = MColumn.get(Env.getCtx(),  
					MColumn.getColumn_ID(MPPProcessCardRecord.Table_Name, "CardStatus"));  
			if (statusColumn != null)  
				m_cardStatusRefId = statusColumn.getAD_Reference_Value_ID();  
  
			// 预取各外键指向表的 AD_Table_ID，供 MLookup.getIdentifier() 使用  
			m_orgTableId = MTable.get(Env.getCtx(), "AD_Org").getAD_Table_ID();  
			m_orderTableId = MTable.get(Env.getCtx(), "PP_Order").getAD_Table_ID();  
			m_cardTableId = MTable.get(Env.getCtx(), MPPProcessCard.Table_Name).getAD_Table_ID();  
			m_nodeTableId = MTable.get(Env.getCtx(), MPPOrderNode.Table_Name).getAD_Table_ID();  
			m_workTeamTableId = MTable.get(Env.getCtx(), "C_WorkTeam").getAD_Table_ID();  
			m_resourceTableId = MTable.get(Env.getCtx(), "S_Resource").getAD_Table_ID();  
			m_routingNodeTableId = MTable.get(Env.getCtx(), "AD_Routing_Node").getAD_Table_ID();  
			m_userTableId = MTable.get(Env.getCtx(), "AD_User").getAD_Table_ID();  
  
			initLookups();  
			zkInit();  
  
			// 首次打开时，用当前登录用户回填默认查询参数  
			int currentUserId = Env.getAD_User_ID(Env.getCtx());  
			if (currentUserId > 0)  
			{  
				userField.setValue(currentUserId);  
				applyCostCollectorDefaults(currentUserId);  
			}  
  
			loadRecordTable();  
		}  
		catch (Exception e)  
		{  
			log.log(Level.SEVERE, "", e);  
		}  
	}  
  
	@Override  
	public ADForm getForm()  
	{  
		// 注意：这里返回持有的 form 字段，而不是 this  
		return form;  
	}  
  
	/**  
	 * 初始化各参数字段的 Lookup  
	 */  
	private void initLookups() throws Exception  
	{  
		Language language = Env.getLanguage(Env.getCtx());  
  
		// AD_Routing_Node_ID（标准工序） -> search editor（主数据表，不需要上下文过滤）  
		MLookup routingNodeLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,  
				MColumn.getColumn_ID("AD_Routing_Node", "AD_Routing_Node_ID"),  
				DisplayType.Search, language, "AD_Routing_Node_ID", 0, false, null);  
		routingNodeField = new WSearchEditor("AD_Routing_Node_ID", true, false, true, routingNodeLookup);  
		routingNodeField.addValueChangeListener(e -> { m_pageNo = 0; loadRecordTable(); });  
  
		// S_Resource_ID -> search editor  
		MLookup resourceLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,  
				MColumn.getColumn_ID("S_Resource", "S_Resource_ID"),  
				DisplayType.Search, language, "S_Resource_ID", 0, false, null);  
		resourceField = new WSearchEditor("S_Resource_ID", true, false, true, resourceLookup);  
		resourceField.addValueChangeListener(e -> { m_pageNo = 0; loadRecordTable(); });  
  
		// C_WorkTeam_ID -> search editor  
		MLookup workTeamLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,  
				MColumn.getColumn_ID("C_WorkTeam", "C_WorkTeam_ID"),  
				DisplayType.Search, language, "C_WorkTeam_ID", 0, false, null);  
		workTeamField = new WSearchEditor("C_WorkTeam_ID", true, false, true, workTeamLookup);  
		workTeamField.addValueChangeListener(e -> { m_pageNo = 0; loadRecordTable(); });  
  
		// AD_User_ID -> search editor  
		MLookup userLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,  
				MColumn.getColumn_ID("AD_User", "AD_User_ID"),  
				DisplayType.Search, language, "AD_User_ID", 0, false, null);  
		userField = new WSearchEditor("AD_User_ID", true, false, true, userLookup);  
		// 切换作业员时，除刷新列表外，还要自动带出该作业员最近一次报工对应的工序/机台/班组。  
		// 注意：WSearchEditor.actionCombo() 是先 fireValueChange(evt)（此时内部值仍是旧值），  
		// 再 setValue(value) 才真正更新内部状态，所以这里必须用 evt.getNewValue()，  
		// 不能再调用 userField.getValue()（会读到"慢一拍"的旧值）。  
		userField.addValueChangeListener(evt -> {  
			Object newVal = evt.getNewValue();  
			int userId = (newVal instanceof Integer) ? ((Integer) newVal).intValue() : 0;  
			if (userId > 0)  
				applyCostCollectorDefaults(userId);  
			m_pageNo = 0;  
			loadRecordTable();  
		});  
	}  
  
	/**  
	 * 搭建界面布局  
	 */  
	private void zkInit()  
	{  
		Borderlayout mainLayout = new Borderlayout();  
		mainLayout.setStyle("width: 100%; height: 100%;");  
		form.appendChild(mainLayout);  
  
		// ---- North: 参数区，分两排 ----  
		North north = new North();  
		mainLayout.appendChild(north);  
  
		// 参数名称全部硬编码中文，不再走 Msg.translate（不依赖 AD_Element 全局字典）  
		routingNodeLabel.setText("标准工序");  
		resourceLabel.setText("机台");  
		workTeamLabel.setText("班组");  
		userLabel.setText("作业员");  
		cardNoLabel.setText("流程卡号");  
  
		Grid parameterGrid = GridFactory.newGridLayout();  
		Columns columns = new Columns();  
		parameterGrid.appendChild(columns);  
		// 第一排 4 组 label/field，共 8 列  
		for (int i = 0; i < 8; i++)  
		{  
			Column column = new Column();  
			column.setWidth(i % 2 == 0 ? "8%" : "17%");  
			columns.appendChild(column);  
		}  
  
		Rows rows = new Rows();  
		parameterGrid.appendChild(rows);  
  
		// 第一排：AD_Routing_Node_ID / S_Resource_ID / C_WorkTeam_ID / AD_User_ID  
		Row row1 = new Row();  
		row1.appendChild(routingNodeLabel.rightAlign());  
		row1.appendChild(routingNodeField.getComponent());  
		row1.appendChild(resourceLabel.rightAlign());  
		row1.appendChild(resourceField.getComponent());  
		row1.appendChild(workTeamLabel.rightAlign());  
		row1.appendChild(workTeamField.getComponent());  
		row1.appendChild(userLabel.rightAlign());  
		row1.appendChild(userField.getComponent());  
		rows.appendChild(row1);
  
		// 第二排：CardNo 文本框，跨列占满剩余宽度  
		Row row2 = new Row();  
		cardNoField.setWidth("100%");  
		cardNoField.setPlaceholder("请输入流程卡号后回车");  
		// 回车 == 触发开工/完工逻辑（无独立"开始"按钮）  
		cardNoField.addEventListener(Events.ON_OK, e -> cmd_start());  
		row2.appendCellChild(cardNoLabel.rightAlign(), 1);   // label 占 1 列  
		row2.appendCellChild(cardNoField, 3);   // 输入框 3 列
		rows.appendChild(row2);  
  
		parameterPanel.appendChild(parameterGrid);  
		north.appendChild(parameterPanel);  
  
		// ---- Center: 记录列表 + 分页 ----  
		Center center = new Center();  
		center.setStyle("height: 100%;");  
  
		Vlayout centerLayout = new Vlayout();  
		centerLayout.setStyle("width: 100%; height: 100%;");  
		centerLayout.appendChild(recordTable);  
  
		paging.setPageSize(PAGE_SIZE);  
		paging.setTotalSize(0);  
		paging.setDetailed(true);  
		paging.addEventListener(ZulEvents.ON_PAGING, this);  
		centerLayout.appendChild(paging);  
  
		center.appendChild(centerLayout);  
		mainLayout.appendChild(center);  
  
		// 列头只需要初始化一次（带上 AD_Reference_ID，供 DateStart/DateFinish 走"日期+时间"格式）  
		initTableColumns();  
  
		// ---- South: 操作按钮，参照信息窗口样式（Separator + ConfirmPanel），只保留 刷新 / 完成 两个按钮 ----  
		South south = new South();  
		mainLayout.appendChild(south);  
  
		Vbox southBody = new Vbox();  
		ZKUpdateUtil.setWidth(southBody, "100%");  
		south.appendChild(southBody);  
		southBody.appendChild(new Separator());  
  
		confirmPanel = new ConfirmPanel(false, false, false, false, false, false);  
  
		bRefresh.setLabel(Msg.getMsg(Env.getCtx(), "Refresh"));  
		bRefresh.addEventListener(Events.ON_CLICK, this);  
  
		bFinish.setLabel("完成");  
		bFinish.addEventListener(Events.ON_CLICK, this);  
		bFinish.setDisabled(true);  
  
		confirmPanel.addComponentsLeft(bRefresh);  
		confirmPanel.addComponentsLeft(bFinish);  
  
		southBody.appendChild(confirmPanel);  
		ZKUpdateUtil.setVflex(south, "min");  
	}  
  
	/**  
	 * 初始化 {@link #recordTable} 的列头（只需要建一次）。  
	 * 使用 WListbox.addColumn(header, description, AD_Reference_ID) 这个重载，  
	 * 才能让 WListItemRenderer 在渲染 Timestamp 单元格时，按 AD_Reference_ID  
	 * 判断是"仅日期"还是"日期+时间"格式（DateStart/DateFinish 传 DisplayType.DateTime）。  
	 */  
	private void initTableColumns()  
	{  
		if (m_columnsInitialized)  
			return;  
  
		for (int i = 0; i < COLUMN_HEADERS.length; i++)  
		{  
			recordTable.addColumn(COLUMN_HEADERS[i], null, COLUMN_REF_IDS[i]);  
		}  
		m_columnsInitialized = true;  
	}  
  
	/**  
	 * 用指定作业员（AD_User_ID）查询其最近一次 CostCollectorType='160' 的报工记录，  
	 * 自动带出对应的 AD_Routing_Node_ID / C_WorkTeam_ID / S_Resource_ID 到参数区。  
	 * 查不到时不改变现有字段的值。  
	 */  
	private void applyCostCollectorDefaults(int userId)  
	{  
		if (userId <= 0)  
			return;  
  
		String sql = "SELECT pon.AD_Routing_Node_ID, pcc.C_WorkTeam_ID, pcc.S_Resource_ID "  
				+ "FROM PP_Cost_Collector pcc "  
				+ "JOIN PP_Order_Node pon ON pon.PP_Order_Node_ID = pcc.PP_Order_Node_ID "  
				+ "WHERE pcc.AD_User_ID=? AND pcc.CostCollectorType=? AND pcc.IsActive='Y' "  
				+ "ORDER BY pcc.Created DESC FETCH FIRST 1 ROWS ONLY";  
  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		try  
		{  
			pstmt = DB.prepareStatement(sql, null);  
			pstmt.setInt(1, userId);  
			pstmt.setString(2, COST_COLLECTOR_TYPE_ACTIVITY_CONTROL);  
			rs = pstmt.executeQuery();  
			if (rs.next())  
			{  
				int routingNodeId = rs.getInt("AD_Routing_Node_ID");  
				int workTeamId = rs.getInt("C_WorkTeam_ID");  
				int resourceId = rs.getInt("S_Resource_ID");  
  
				if (routingNodeId > 0)  
					routingNodeField.setValue(routingNodeId);  
				if (workTeamId > 0)  
					workTeamField.setValue(workTeamId);  
				if (resourceId > 0)  
					resourceField.setValue(resourceId);  
			}  
		}  
		catch (SQLException e)  
		{  
			log.log(Level.SEVERE, sql, e);  
		}  
		finally  
		{  
			DB.close(rs, pstmt);  
		}  
	}  
  
	/**  
	 * 按 CardNo 查询对应的 PP_Process_Card_ID。  
	 */  
	private int resolveCardIdByCardNo(String cardNo)  
	{  
		if (Util.isEmpty(cardNo, true))  
			return 0;  
  
		String sql = "SELECT PP_Process_Card_ID FROM PP_Process_Card "  
				+ "WHERE CardNo=? ORDER BY Created DESC FETCH FIRST 1 ROWS ONLY";  
  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		try  
		{  
			pstmt = DB.prepareStatement(sql, null);  
			pstmt.setString(1, cardNo.trim());  
			rs = pstmt.executeQuery();  
			if (rs.next())  
				return rs.getInt(1);  
		}  
		catch (SQLException e)  
		{  
			log.log(Level.SEVERE, sql, e);  
			Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
		}  
		finally  
		{  
			DB.close(rs, pstmt);  
		}  
		return 0;  
	}  
  
	/**  
	 * 根据 North 区已填写的参数，动态拼接可选过滤条件的 WHERE 子句。  
	 */  
	/**  
	 * 根据 North 区已填写的参数，动态拼接可选过滤条件的 WHERE 子句；  
	 * 同时加入固定的"当前班次"过滤：只显示 DateFinish 落在当前班次内的记录，  
	 * 以及尚未完工（DateFinish IS NULL）的记录（不受班次限制，始终展示）。  
	 */  
	private String buildWhereClause(List<Object> params)  
	{  
	    StringBuilder where = new StringBuilder();  
	  
	    Integer routingNodeId = (Integer) routingNodeField.getValue();  
	    if (routingNodeId != null && routingNodeId.intValue() > 0)  
	    {  
	        where.append(where.length() == 0 ? " WHERE " : " AND ").append("AD_Routing_Node_ID=?");  
	        params.add(routingNodeId);  
	    }  
	  
	    Integer resourceId = (Integer) resourceField.getValue();  
	    if (resourceId != null && resourceId.intValue() > 0)  
	    {  
	        where.append(where.length() == 0 ? " WHERE " : " AND ").append("S_Resource_ID=?");  
	        params.add(resourceId);  
	    }  
	  
//	    Integer workTeamId = (Integer) workTeamField.getValue();  
//	    if (workTeamId != null && workTeamId.intValue() > 0)  
//	    {  
//	        where.append(where.length() == 0 ? " WHERE " : " AND ").append("C_WorkTeam_ID=?");  
//	        params.add(workTeamId);  
//	    }  
	  
	    Integer userId = (Integer) userField.getValue();  
	    if (userId != null && userId.intValue() > 0)  
	    {  
	        where.append(where.length() == 0 ? " WHERE " : " AND ").append("AD_User_ID=?");  
	        params.add(userId);  
	    }  
	  
	    // 固定条件：只查询"结束时间落在当前班次内"的记录，未完工（DateFinish为空）的始终展示  
	    Timestamp[] shiftRange = calculateShiftRange();  
	    where.append(where.length() == 0 ? " WHERE " : " AND ")  
	         .append("(DateFinish IS NULL OR (DateFinish >= ? AND DateFinish < ?))");  
	    params.add(shiftRange[0]);  
	    params.add(shiftRange[1]);  
	  
	    return where.toString();  
	}
  
	/**  
	 * 计算"当前班次"的起止时间：  
	 * 每天 08:00-20:00 为白班，20:00-次日08:00 为夜班。  
	 * 返回长度为 2 的数组：[0]=班次开始时间, [1]=班次结束时间（不含）。  
	 */  
	private Timestamp[] calculateShiftRange()  
	{  
	    java.util.Calendar cal = java.util.Calendar.getInstance();  
	    java.util.Date now = cal.getTime();  
	  
	    java.util.Calendar today8 = (java.util.Calendar) cal.clone();  
	    today8.set(java.util.Calendar.HOUR_OF_DAY, 8);  
	    today8.set(java.util.Calendar.MINUTE, 0);  
	    today8.set(java.util.Calendar.SECOND, 0);  
	    today8.set(java.util.Calendar.MILLISECOND, 0);  
	  
	    java.util.Calendar today20 = (java.util.Calendar) today8.clone();  
	    today20.set(java.util.Calendar.HOUR_OF_DAY, 20);  
	  
	    Timestamp shiftStart;  
	    Timestamp shiftEnd;  
	  
	    if (!now.before(today8.getTime()) && now.before(today20.getTime()))  
	    {  
	        // 当前处于白班：今天 08:00 ~ 今天 20:00  
	        shiftStart = new Timestamp(today8.getTimeInMillis());  
	        shiftEnd = new Timestamp(today20.getTimeInMillis());  
	    }  
	    else if (!now.before(today20.getTime()))  
	    {  
	        // 当前处于夜班（当天20:00之后）：今天 20:00 ~ 明天 08:00  
	        java.util.Calendar tomorrow8 = (java.util.Calendar) today8.clone();  
	        tomorrow8.add(java.util.Calendar.DAY_OF_MONTH, 1);  
	        shiftStart = new Timestamp(today20.getTimeInMillis());  
	        shiftEnd = new Timestamp(tomorrow8.getTimeInMillis());  
	    }  
	    else  
	    {  
	        // 当前处于夜班（凌晨0点~8点之间，属于"昨天20:00"开始的班次）：昨天 20:00 ~ 今天 08:00  
	        java.util.Calendar yesterday20 = (java.util.Calendar) today20.clone();  
	        yesterday20.add(java.util.Calendar.DAY_OF_MONTH, -1);  
	        shiftStart = new Timestamp(yesterday20.getTimeInMillis());  
	        shiftEnd = new Timestamp(today8.getTimeInMillis());  
	    }  
	  
	    return new Timestamp[] { shiftStart, shiftEnd };  
	}
	
	/**  
	 * 查询总行数（用于分页组件 setTotalSize），复用 buildWhereClause 的过滤条件  
	 */  
	private int queryRowCount(String whereClause, List<Object> params)  
	{  
		String sql = "SELECT COUNT(*) FROM RV_Process_Card_Scanner" + whereClause;  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		int count = 0;  
		try  
		{  
			pstmt = DB.prepareStatement(sql, null);  
			for (int i = 0; i < params.size(); i++)  
				pstmt.setObject(i + 1, params.get(i));  
			rs = pstmt.executeQuery();  
			if (rs.next())  
				count = rs.getInt(1);  
		}  
		catch (SQLException e)  
		{  
			log.log(Level.SEVERE, sql, e);  
			Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
		}  
		finally  
		{  
			DB.close(rs, pstmt);  
		}  
		return count;  
	}  
  
	/**  
	 * 刷新中间列表：所有参数均为可选过滤条件，全部为空时查询全部记录（分页展示，防止数据量过大）。  
	 * 数据源为 RV_Process_Card_Scanner 视图。列表不再展示 CardNo/AD_Client_ID，新增展示 AD_User_ID。  
	 * DateStart/DateFinish 已在 initTableColumns() 里指定 DisplayType.DateTime，会自动按"日期+时间"渲染。  
	 */  
	private void loadRecordTable()  
	{  
		Integer routingNodeVal = (Integer) routingNodeField.getValue();  
		int routingNodeId = routingNodeVal != null ? routingNodeVal.intValue() : 0;  
  
		m_selectedRecord_ID = 0;  
		m_rowStatusMap.clear();  
		bFinish.setDisabled(true);  
  
		List<Object> countParams = new ArrayList<>();  
		String whereClause = buildWhereClause(countParams);  
  
		m_count = queryRowCount(whereClause, countParams);  
		paging.setTotalSize(m_count);  
		if (m_pageNo * PAGE_SIZE >= m_count && m_pageNo > 0)  
			m_pageNo = 0;  
		paging.setActivePage(m_pageNo);  
		paging.setVisible(m_count > PAGE_SIZE);  
  
		Vector<Vector<Object>> data = new Vector<>();  
  
		if (m_count > 0)  
		{  
			List<Object> dataParams = new ArrayList<>();  
			String dataWhere = buildWhereClause(dataParams);  
  
			String sql = BASE_SELECT + dataWhere + " ORDER BY DateStart DESC";  
			int start = m_pageNo * PAGE_SIZE;  
			int end = start + PAGE_SIZE;  
			sql = DB.getDatabase().addPagingSQL(sql, start, end);  
  
			PreparedStatement pstmt = null;  
			ResultSet rs = null;  
			try  
			{  
				pstmt = DB.prepareStatement(sql, null);  
				for (int i = 0; i < dataParams.size(); i++)  
					pstmt.setObject(i + 1, dataParams.get(i));  
				rs = pstmt.executeQuery();  
				while (rs.next())  
				{  
					Vector<Object> line = new Vector<>();  
  
					int recId = rs.getInt("PP_Process_Card_Record_ID");  
					line.add(new IDColumn(recId)); // 第一列渲染成单选框  
  
					line.add(MLookup.getIdentifier(m_orgTableId, rs.getInt("AD_Org_ID")));  
					line.add(MLookup.getIdentifier(m_orderTableId, rs.getInt("PP_Order_ID")));  
					line.add(MLookup.getIdentifier(m_cardTableId, rs.getInt("PP_Process_Card_ID")));  
					line.add(MLookup.getIdentifier(m_nodeTableId, rs.getInt("PP_Order_Node_ID")));  
					line.add(MLookup.getIdentifier(m_routingNodeTableId, rs.getInt("AD_Routing_Node_ID")));  
					line.add(MLookup.getIdentifier(m_resourceTableId, rs.getInt("S_Resource_ID")));  
					line.add(MLookup.getIdentifier(m_workTeamTableId, rs.getInt("C_WorkTeam_ID")));  
					line.add(MLookup.getIdentifier(m_userTableId, rs.getInt("AD_User_ID")));  
  
					line.add(rs.getTimestamp("DateStart"));  
					line.add(rs.getBigDecimal("Qty"));
					line.add(rs.getBigDecimal("MovementQty"));  
					line.add(rs.getBigDecimal("ScrappedQty"));  
					line.add(rs.getTimestamp("DateFinish"));  
					String status = rs.getString("CardStatus");  
					line.add(m_cardStatusRefId > 0  
							? MRefList.getListName(Env.getCtx(), m_cardStatusRefId, status)  
							: status);  
  
					String isWorkReport = rs.getString("IsWorkReport"); // Y/N char(1)  
					line.add(Boolean.valueOf("Y".equals(isWorkReport)));
					data.add(line);  
  
					m_rowStatusMap.put(recId, status);  
				}  
			}  
			catch (SQLException e)  
			{  
				log.log(Level.SEVERE, sql, e);  
				Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
			}  
			finally  
			{  
				DB.close(rs, pstmt);  
			}  
		}  
  
		ListModelTable model = new ListModelTable(data);  
		model.setMultiple(false); // 单选而不是多选  
		recordTable.setData(model, null);  
		recordTable.addEventListener(Events.ON_SELECT, this);  
	}  
  
	@Override  
	public void onEvent(Event event) throws Exception  
	{  
		if (event.getTarget() == bRefresh)  
		{  
			m_pageNo = 0;  
			loadRecordTable();  
		}  
		else if (event.getTarget() == bFinish)  
			cmd_finish();  
		else if (event.getTarget() == recordTable && Events.ON_SELECT.equals(event.getName()))  
			onRecordSelected();  
		else if (event.getTarget() == paging && ZulEvents.ON_PAGING.equals(event.getName()))  
		{  
			m_pageNo = paging.getActivePage();  
			loadRecordTable();  
		}  
	}  
  
	/**  
	 * 用户在 {@link #recordTable} 里手动点选某一行时触发：  
	 * "完成"按钮只有在选中行的 CardStatus=InProgress 时才启用。  
	 */  
	private void onRecordSelected()  
	{  
		int index = recordTable.getSelectedIndex();  
		if (index < 0)  
	        {  
			m_selectedRecord_ID = 0;  
	        bFinish.setDisabled(true);  
	        return;  
	    }  
  
		Object key = recordTable.getValueAt(index, 0);  
		if (key instanceof IDColumn)  
		{  
			int recId = ((IDColumn) key).getRecord_ID();  
			m_selectedRecord_ID = recId;  
	    String status = m_rowStatusMap.get(recId);  
	    bFinish.setDisabled(!STATUS_INPROGRESS.equals(status));  
	}  
		else  
	{  
			m_selectedRecord_ID = 0;  
			bFinish.setDisabled(true);  
	}
	}  
  
	/**
	 * 根据"标准工序（AD_Routing_Node_ID）+ 生产订单（PP_Order_ID）"解析出实际的 PP_Order_Node_ID。  
	 */  
	private int resolveOrderNodeId(int routingNodeId, int orderId)  
	{  
		if (routingNodeId <= 0 || orderId <= 0)  
			return 0;  
  
		String sql = "SELECT PP_Order_Node_ID FROM PP_Order_Node "  
				+ "WHERE AD_Routing_Node_ID=? AND PP_Order_ID=? AND IsActive='Y' "  
				+ "ORDER BY Created DESC FETCH FIRST 1 ROWS ONLY";  
  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		try  
		{  
			pstmt = DB.prepareStatement(sql, null);  
			pstmt.setInt(1, routingNodeId);  
			pstmt.setInt(2, orderId);  
			rs = pstmt.executeQuery();  
			if (rs.next())  
				return rs.getInt(1);  
		}  
		catch (SQLException e)  
		{  
			log.log(Level.SEVERE, sql, e);  
			Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
		}  
		finally  
		{  
			DB.close(rs, pstmt);  
		}  
		return 0;  
	}  
  
	/**  
	 * CardNo 文本框回车触发的开工/完工逻辑（已无独立"开始"按钮）。  
	 */  
	private void cmd_start()  
	{  
	    String cardNo = cardNoField.getValue();  
	    Integer resourceIdObj = (Integer) resourceField.getValue();  
	    int resourceId = resourceIdObj != null ? resourceIdObj.intValue() : 0;  
	  
	    if (Util.isEmpty(cardNo, true))  
	    {  
	        Dialog.error(m_WindowNo, "FillMandatory", "流程卡号");  
	        return;  
	    }  
	  
	    int cardId = resolveCardIdByCardNo(cardNo);  
	    if (cardId <= 0)  
	    {  
	        Dialog.error(m_WindowNo, "Error", "未找到该生产流程卡");  
	        return;  
	    }  
	  
	    if (!validateParameters())  
	        return;  
	  
	    int routingNodeId = (Integer) routingNodeField.getValue();  
	  
	    MPPProcessCard card = new MPPProcessCard(Env.getCtx(), cardId, null);  
	    int orderId = card.getPP_Order_ID();  
	  
	    int nodeId = resolveOrderNodeId(routingNodeId, orderId);  
	    if (nodeId <= 0)  
	    {  
	        Dialog.error(m_WindowNo, "Error", "未找到匹配的工单工序（PP_Order_Node），请确认该流程卡与所选标准工序是否匹配");  
	        return;  
	    }  
	  
	    int activeId = findActiveRecord(cardId, nodeId);  
	    if (activeId > 0)  
	    {  
	        confirmAndOpenFinishQtyDialog(activeId, null);  
	        return;  
	    }  
	  
	    // 开工前校验同工单+工序+机台是否已被其他流程卡占用：  
	    // 不再直接报错，而是先弹出该冲突卡的完工数量录入弹窗，  
	    // 用户确认完工保存成功后，再自动创建当前卡的 InProgress 记录。  
	    int conflictingRecordId = findConflictingInProgressRecordId(cardId, orderId, nodeId, resourceId, STATUS_INPROGRESS);  
	    if (conflictingRecordId > 0)  
	    {  
	        final int fCardId = cardId;  
	        final int fOrderId = orderId;  
	        final int fNodeId = nodeId;  
	        final int fResourceId = resourceId;  
	        confirmAndOpenFinishQtyDialog(conflictingRecordId,  
	                () -> createNewProcessCardRecord(fCardId, fOrderId, fNodeId, fResourceId));  
	        return;  
	    }  
	  
	    // 开工前校验同工单+工序+机台是否已生产完成  
	    String completedCardNo = findConflictingCompletedCardNo(cardId, orderId, nodeId, resourceId, STATUS_COMPLETED);  
	    if (completedCardNo != null)  
	    {  
	        Dialog.error(m_WindowNo, "Error", "该卡版【" + completedCardNo + "】已生产完成!");  
	        return;  
	    }  
	  
	    createNewProcessCardRecord(cardId, orderId, nodeId, resourceId);  
	} 
  
	/**  
	 * "完成"按钮：只针对列表中手动选中、且状态为 InProgress 的记录，弹窗录入数量后完工。  
	 */  
	private void cmd_finish()  
	{  
	    if (m_selectedRecord_ID <= 0)  
	    {  
	        Dialog.error(m_WindowNo, "SaveErrorRowNotFound");  
	        return;  
	    }  
	    String status = m_rowStatusMap.get(m_selectedRecord_ID);  
	    if (!STATUS_INPROGRESS.equals(status))  
	    {  
	        Dialog.error(m_WindowNo, "Error", "只能对状态为\"生产中\"的记录执行完成操作");  
	        return;  
	    }  
	    confirmAndOpenFinishQtyDialog(m_selectedRecord_ID, null);  
	}
  
	/**  
	 * 按 PP_Process_Card_Record_ID 查询其所属流程卡的 CardNo，用于完工数量弹窗标题展示。  
	 */  
	private String getRecordCardNo(int recordId)  
	{  
	    String sql = "SELECT c.CardNo FROM PP_Process_Card_Record r "  
	            + "JOIN PP_Process_Card c ON c.PP_Process_Card_ID = r.PP_Process_Card_ID "  
	            + "WHERE r.PP_Process_Card_Record_ID=?";  
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try  
	    {  
	        pstmt = DB.prepareStatement(sql, null);  
	        pstmt.setInt(1, recordId);  
	        rs = pstmt.executeQuery();  
	        if (rs.next())  
	            return rs.getString(1);  
	    }  
	    catch (SQLException e)  
	    {  
	        log.log(Level.SEVERE, sql, e);  
	    }  
	    finally  
	    {  
	        DB.close(rs, pstmt);  
	    }  
	    return null;  
	}
	
	/**  
	 * 校验同一 工单+工单工序+机台 下是否已存在其他 InProgress 流程卡（排除当前卡自身）。  
	 * 存在则返回该冲突记录的 PP_Process_Card_Record_ID；不存在返回 0。  
	 */  
	private int findConflictingInProgressRecordId(int cardId, int orderId, int nodeId, int resourceId, String cardStatus)  
	{  
	    String sql = "SELECT PP_Process_Card_Record_ID FROM RV_Process_Card_Scanner "  
	            + "WHERE PP_Process_Card_ID<>? AND PP_Order_ID=? AND PP_Order_Node_ID=? "  
	            + "AND S_Resource_ID=? AND CardStatus=? "  
	            + "ORDER BY DateStart DESC FETCH FIRST 1 ROWS ONLY";  
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try  
	    {  
	        pstmt = DB.prepareStatement(sql, null);  
	        pstmt.setInt(1, cardId);  
	        pstmt.setInt(2, orderId);  
	        pstmt.setInt(3, nodeId);  
	        pstmt.setInt(4, resourceId);  
	        pstmt.setString(5, cardStatus);  
	        rs = pstmt.executeQuery();  
	        if (rs.next())  
	            return rs.getInt(1);  
	    }  
	    catch (SQLException e)  
	    {  
	        log.log(Level.SEVERE, sql, e);  
	        Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
	    }  
	    finally  
	    {  
	        DB.close(rs, pstmt);  
	    }  
	    return 0;  
	}
	
	/**  
	 * 校验同一 工单+工单工序+机台 下是否已存在其他 Completed 流程卡  
	 * 存在则返回冲突卡号；不存在返回 null。  
	 */  
	private String findConflictingCompletedCardNo(int cardId, int orderId, int nodeId, int resourceId, String cardStatus)  
	{  
	    String sql = "SELECT CardNo FROM RV_Process_Card_Scanner "  
	            + "WHERE PP_Process_Card_ID=? AND PP_Order_ID=? AND PP_Order_Node_ID=? "  
	            + "AND S_Resource_ID=? AND CardStatus=? "  
	            + "ORDER BY DateStart DESC FETCH FIRST 1 ROWS ONLY";  
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try  
	    {  
	        pstmt = DB.prepareStatement(sql, null);  
	        pstmt.setInt(1, cardId);  
	        pstmt.setInt(2, orderId);  
	        pstmt.setInt(3, nodeId);  
	        pstmt.setInt(4, resourceId);  
	        pstmt.setString(5, cardStatus);  
	        rs = pstmt.executeQuery();  
	        if (rs.next())  
	            return rs.getString(1);  
	    }  
	    catch (SQLException e)  
	    {  
	        log.log(Level.SEVERE, sql, e);  
	        Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
	    }  
	    finally  
	    {  
	        DB.close(rs, pstmt);  
	    }  
	    return null;  
	}
	
	/**  
	 * 查询指定流程卡 + 工单工序（PP_Order_Node_ID）下，状态为 InProgress 的记录主键；不存在返回 0。  
	 */  
	private int findActiveRecord(int cardId, int nodeId)  
	{  
		String sql = "SELECT PP_Process_Card_Record_ID FROM PP_Process_Card_Record "  
				+ "WHERE PP_Process_Card_ID=? AND PP_Order_Node_ID=? AND CardStatus=? "  
				+ "ORDER BY Created DESC LIMIT 1";  
		return DB.getSQLValue(null, sql, cardId, nodeId, STATUS_INPROGRESS);  
	}  
  
	/**  
	 * 创建一条新的 InProgress 流程卡生产记录（原 cmd_start 中内联的 Trx.run 逻辑抽取而来，  
	 * 供正常开工路径、以及"先完工冲突卡再自动开工当前卡"的回调路径共用）。  
	 */  
	private void createNewProcessCardRecord(int cardId, int orderId, int nodeId, int resourceId)  
	{  
	    try  
	    {  
	        Trx.run(trxName -> {  
	            MPPProcessCardRecord record = new MPPProcessCardRecord(Env.getCtx(), 0, trxName);  
	            record.setPP_Process_Card_ID(cardId);  
	            record.setPP_Order_ID(orderId);  
	            record.setPP_Order_Node_ID(nodeId);  
	            record.setS_Resource_ID(resourceId);  
	            record.setC_WorkTeam_ID((Integer) workTeamField.getValue());  
	            record.setAD_User_ID((Integer) userField.getValue());  
	            record.setDateStart(new Timestamp(System.currentTimeMillis()));  
	            record.setDateFinish(null);  
	            record.setCardStatus(STATUS_INPROGRESS);  
	            record.saveEx();  
	        });  
	    }  
	    catch (Exception e)  
	    {  
	        log.log(Level.SEVERE, "createNewProcessCardRecord", e);  
	        Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage());  
	        return;  
	    }  
	  
	    m_pageNo = 0;  
	    loadRecordTable();  
	}
	
	/**  
	 * 完工前置校验：若 当前时间-DateStart < 配置分钟数，先弹二次确认；否则直接打开完工数量录入弹窗。  
	 * @param recordId  要完工的 PP_Process_Card_Record_ID  
	 * @param onSuccess 完工数量录入并保存成功后要执行的回调（可为 null）；  
	 *                  用户点击弹窗关闭/取消时不会触发该回调。  
	 */  
	private void confirmAndOpenFinishQtyDialog(final int recordId, final Runnable onSuccess)  
	{  
	    Timestamp dateStart = getRecordDateStart(recordId);  
	    if (dateStart == null)  
	    {  
	        openFinishQtyDialog(recordId, onSuccess);  
	        return;  
	    }  
	  
	    long gapMinutes = (System.currentTimeMillis() - dateStart.getTime()) / 60000L;  
	    int thresholdMinutes = MSysConfig.getIntValue(SYSCONFIG_FINISH_GAP_MINUTES, DEFAULT_FINISH_GAP_MINUTES);  
	  
	    if (gapMinutes >= thresholdMinutes)  
	    {  
	        openFinishQtyDialog(recordId, onSuccess);  
	        return;  
	    }  
	  
	    // Dialog 在 ZK 里是异步的，必须用回调，不能写成同步 if  
	    Dialog.ask(m_WindowNo, "当前卡板是否已生产完成？", new Callback<Boolean>() {  
	        @Override  
	        public void onCallback(Boolean result)  
	        {  
	            if (Boolean.TRUE.equals(result))  
	                openFinishQtyDialog(recordId, onSuccess);  
	            // 取消：什么都不做，相当于关闭提示；onSuccess 不会被触发  
	        }  
	    });  
	}
	
	private Timestamp getRecordDateStart(int recordId)  
	{  
	    String sql = "SELECT DateStart FROM PP_Process_Card_Record WHERE PP_Process_Card_Record_ID=?";  
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try  
	    {  
	        pstmt = DB.prepareStatement(sql, null);  
	        pstmt.setInt(1, recordId);  
	        rs = pstmt.executeQuery();  
	        if (rs.next())  
	            return rs.getTimestamp(1);  
	    }  
	    catch (SQLException e)  
	    {  
	        log.log(Level.SEVERE, sql, e);  
	    }  
	    finally  
	    {  
	        DB.close(rs, pstmt);  
	    }  
	    return null;  
	}
	
	/**  
	 * 弹出一个小对话框，录入 MovementQty（正品数量）、ScrappedQty（废品数量），确认后完工。  
	 * 正品数量默认值为标准卡板数；两个数量输入框均为必填，为空时实时显示红色边框。  
	 * @param recordId  要完工的 PP_Process_Card_Record_ID  
	 * @param onSuccess 保存成功后要执行的回调（可为 null）；用户取消弹窗时不会触发。  
	 */  
	private void openFinishQtyDialog(final int recordId, final Runnable onSuccess)  
	{  
		final Window win = new Window();  
		String cardNo = getRecordCardNo(recordId);  
		win.setTitle(Util.isEmpty(cardNo, true) ? "完工数量录入" : ("完工数量录入 - 卡号：" + cardNo));  
		win.setBorder("normal");  
		win.setClosable(true);  
		win.setWidth("330px");   // 原来是 320px，调大  
		//win.setHeight("260px");  // 没有设置高度，默认由内容撑开，现在固定一个更宽松的高度  
		win.setPosition("center");
	  
	    Grid grid = GridFactory.newGridLayout();  
	    Columns columns = new Columns();  
	    Column c1 = new Column();  
	    c1.setWidth("40%");  
	    Column c2 = new Column();  
	    c2.setWidth("60%");  
	    columns.appendChild(c1);  
	    columns.appendChild(c2);  
	    grid.appendChild(columns);  
	  
	    Rows rows = new Rows();  
	    grid.appendChild(rows);  
	  
	    // 正品数量（MovementQty），默认取标准卡板数  
	    Label movementLabel = new Label("正品数量");  
	    final NumberBox movementQtyBox = new NumberBox(false); 
	    movementQtyBox.getButton().setVisible(false);  
	    BigDecimal standardQty = getRecordStandardQty(recordId);  
	    movementQtyBox.setValue(standardQty != null ? standardQty : BigDecimal.ZERO);  
	    // 实时校验：值变化时立即刷新红框状态  
	    movementQtyBox.addEventListener(Events.ON_CHANGE, e -> toggleMandatoryStyle(movementQtyBox));  
	    toggleMandatoryStyle(movementQtyBox); // 初始化一次  
	    Row row1 = new Row();  
	    row1.appendChild(movementLabel);  
	    row1.appendChild(movementQtyBox);  
	    rows.appendChild(row1);  
	  
	    // 废品数量（ScrappedQty）  
	    Label scrappedLabel = new Label("废品数量");  
	    final NumberBox scrappedQtyBox = new NumberBox(false);  
	    scrappedQtyBox.getButton().setVisible(false);
	    scrappedQtyBox.setValue(BigDecimal.ZERO);  
	    scrappedQtyBox.addEventListener(Events.ON_CHANGE, e -> toggleMandatoryStyle(scrappedQtyBox));  
	    toggleMandatoryStyle(scrappedQtyBox); // 初始化一次  
	    Row row2 = new Row();  
	    row2.appendChild(scrappedLabel);  
	    row2.appendChild(scrappedQtyBox);  
	    rows.appendChild(row2);  
	  
	    win.appendChild(grid);  
	  
	    ConfirmPanel dialogConfirmPanel = new ConfirmPanel(true);  
	    win.appendChild(dialogConfirmPanel);  
	  
	    dialogConfirmPanel.addActionListener(new EventListener<Event>() {  
	        @Override  
	        public void onEvent(Event e) throws Exception {  
	            if (e.getTarget().equals(dialogConfirmPanel.getButton(ConfirmPanel.A_OK)))  
	            {  
	                // 点击确认时再兜底校验一次（防止用户直接点确认，ON_CHANGE 未被触发的边界情况）  
	                boolean hasEmpty = false;  
	                if (movementQtyBox.getValue() == null)  
	                {  
	                    toggleMandatoryStyle(movementQtyBox);  
	                    hasEmpty = true;  
	                }  
	                if (scrappedQtyBox.getValue() == null)  
	                {  
	                    toggleMandatoryStyle(scrappedQtyBox);  
	                    hasEmpty = true;  
	                }  
	                if (hasEmpty)  
	                {  
	                    Dialog.error(m_WindowNo, "FillMandatory", "正品数量/废品数量");  
	                    return; // 不 detach，保留弹窗让用户补填  
	                }  
	  
	                BigDecimal movementQty = movementQtyBox.getValue();  
	                BigDecimal scrappedQty = scrappedQtyBox.getValue();  
	  
	                if (movementQty.compareTo(BigDecimal.ZERO) <= 0)  
	                {  
	                    Dialog.error(m_WindowNo, "Error", "正品数量必须大于0");  
	                    return; // 不 detach，让弹窗保留，方便用户重新输入  
	                }  
	  
	                boolean saved = true;  
	                try  
	                {  
	                    Trx.run(trxName -> {  
	                        MPPProcessCardRecord record = new MPPProcessCardRecord(Env.getCtx(), recordId, trxName);  
	                        record.setMovementQty(movementQty);  
	                        record.setScrappedQty(scrappedQty);  
	                        record.setDateFinish(new Timestamp(System.currentTimeMillis()));  
	                        record.setCardStatus(STATUS_COMPLETED);  
	                        record.saveEx();  
	                    });  
	                }  
	                catch (Exception ex)  
	                {  
	                    saved = false;  
	                    log.log(Level.SEVERE, "openFinishQtyDialog", ex);  
	                    Dialog.error(m_WindowNo, "Error", ex.getLocalizedMessage());  
	                }  
	                finally  
	                {  
	                    win.detach();  
	                    m_selectedRecord_ID = 0;  
	                    loadRecordTable();  
	                }  
	  
	                if (saved && onSuccess != null)  
	                    onSuccess.run();  
	            }  
	            else  
	            {  
	                win.detach();  
	                // 用户点击取消：不做任何后续处理，onSuccess 不触发  
	            }  
	        }  
	    });  
	  
	    try  
	    {  
	        win.setPage(form.getPage());  
	        win.doModal();  
	    }  
	    catch (Exception e)  
	    {  
	        log.log(Level.SEVERE, "openFinishQtyDialog.doModal", e);  
	    }  
	}  
	
	/**  
	 * 根据 NumberBox 当前值是否为空，切换"必填未填"的红色边框样式。  
	 * 复用 iDempiere 主题自带的 idempiere-mandatory 样式类（该类在 WEditor 里也是这么用的）。  
	 */  
	private void toggleMandatoryStyle(NumberBox box)  
	{  
	    if (box.getValue() == null)  
	        box.setSclass("idempiere-mandatory");  
	    else  
	        box.setSclass(null);  
	}
  
	/**  
	 * 按 PP_Process_Card_Record_ID 查询其对应的"标准卡板数"（RV_Process_Card_Scanner.Qty），  
	 * 用于完工数量录入弹窗里 MovementQty（正品数量）的默认值。查不到返回 null。  
	 */  
	private BigDecimal getRecordStandardQty(int recordId)  
	{  
	    String sql = "SELECT Qty FROM RV_Process_Card_Scanner WHERE PP_Process_Card_Record_ID=?";  
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try  
	    {  
	        pstmt = DB.prepareStatement(sql, null);  
	        pstmt.setInt(1, recordId);  
	        rs = pstmt.executeQuery();  
	        if (rs.next())  
	            return rs.getBigDecimal("Qty");  
	    }  
	    catch (SQLException e)  
	    {  
	        log.log(Level.SEVERE, sql, e);  
	    }  
	    finally  
	    {  
	        DB.close(rs, pstmt);  
	    }  
	    return null;  
	}
	
	/**  
	 * 校验开工所需的必填参数（CardNo 已在 cmd_start 前置校验并解析成 cardId，这里只校验其余三项）。  
	 * 逐个判断，缺哪个提示哪个的中文名称，而不是笼统报错。  
	 */  
	private boolean validateParameters()  
	{  
	    if (routingNodeField.getValue() == null)  
	    {  
	        Dialog.error(m_WindowNo, "FillMandatory", "标准工序");  
	        return false;  
	    }  
	    if (resourceField.getValue() == null)  
	    {  
	        Dialog.error(m_WindowNo, "FillMandatory", "机台");  
	        return false;  
	    }  
	    if (workTeamField.getValue() == null)  
	    {  
	        Dialog.error(m_WindowNo, "FillMandatory", "班组");  
	        return false;  
	    }  
	    if (userField.getValue() == null)  
	    {  
	        Dialog.error(m_WindowNo, "FillMandatory", "作业员");  
	        return false;  
	    }  
	    return true;  
	}
}