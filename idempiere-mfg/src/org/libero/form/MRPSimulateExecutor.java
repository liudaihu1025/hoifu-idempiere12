package org.libero.form;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MResource;
import org.compiere.model.MWarehouse;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.eevolution.model.MPPProductPlanning;
import org.libero.process.MRPWithFlags;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.Vlayout;

/**
 * MRP 模拟执行器 — 独立表单
 *
 * 两阶段交互：
 *   阶段一：点击「开始计算」后，始终以预览模式（IsSimulate=true）调用 MRPWithFlags 进程，
 *           结果不落库（MRP.doIt() 内部会自动 rollback），仅展示汇总信息供用户参考。
 *   阶段二：预览结果出现后，显示「是否删除以前MRP结果」「是否生成生产工单」勾选框及「生成单据」按钮，
 *           用户确认后才真正落库执行（IsSimulate=false）。
 *
 * 不修改 MRP.java 源码，通过子类 MRPWithFlags 扩展 GenerateWO 参数支持。
 */
public class MRPSimulateExecutor implements IFormController, EventListener<Event>, ValueChangeListener
{
	private static final CLogger log = CLogger.getCLogger(MRPSimulateExecutor.class);

	/** 表单框架 */
	private CustomForm m_frame = new CustomForm();

	// ==================== 阶段一：参数区控件 ====================

	/** 组织 */
	private Label lOrg_ID = new Label(Msg.translate(Env.getCtx(), "AD_Org_ID"));
	private WTableDirEditor fOrg_ID;

	/** 资源 */
	private Label lResource_ID = new Label(Msg.translate(Env.getCtx(), "S_Resource_ID"));
	private WTableDirEditor fResource_ID;

	/** 仓库 */
	private Label lWarehouse_ID = new Label(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
	private WTableDirEditor fWarehouse_ID;

	/** 是否模拟（预览模式）— 阶段一始终以 true 执行，此控件仅作提示用途 */
	private Checkbox chkIsSimulate = new Checkbox();

	/** 开始计算按钮（阶段一） */
	private Button btnExecute = new Button("开始计算，生成预览效果");

	// ==================== 阶段二：生成单据区控件 ====================

	/** 是否删除以前MRP结果（阶段二显示，默认不勾选） */
	private Checkbox chkDeleteMRP = new Checkbox();

	/** 是否生成生产工单（阶段二显示，默认不勾选） */
	private Checkbox chkGenerateWO = new Checkbox();

	/** 生成单据按钮（阶段二） */
	private Button btnGenerateDoc = new Button("生成单据");

	/** 取消按钮（重置表单） */
	private Button btnCancel = new Button("取消");

	/** 阶段二控件容器（预览成功后才显示） */
	private Vlayout phase2Layout = new Vlayout();
	private Hlayout phase2CheckboxRow = new Hlayout();
	private Hlayout phase2ButtonRow = new Hlayout();

	// ==================== 结果展示区 ====================

	/** 结果展示（支持 HTML 渲染，MRP 返回的文本含 <br> 标签） */
	private Html resultHtml = new Html();

	// ==================== 布局 ====================

	private Borderlayout mainLayout = new Borderlayout();
	private Grid parameterGrid = GridFactory.newGridLayout();

	/** 结果面板（用于动态追加阶段二控件） */
	private Vlayout resultPanel;

	/**
	 * 构造方法 — 初始化界面
	 */
	public MRPSimulateExecutor()
	{
		initUI();
	}

	/**
	 * 初始化 UI 组件和布局
	 */
	private void initUI()
	{
		try
		{
			// --- 设置勾选框文本 ---
//			chkIsSimulate.setText("是否模拟");
			chkDeleteMRP.setText("是否删除以前MRP结果");
			chkGenerateWO.setText("是否生成生产工单");

			// --- 默认值 ---
//			chkIsSimulate.setChecked(false);
			chkDeleteMRP.setChecked(false);
			chkGenerateWO.setChecked(false);

			// --- 创建 Lookup 编辑器 ---
			Language language = Language.getLoginLanguage();

			// 组织（下拉框，与 MRP.java 一致：仅按 AD_Client_ID 过滤）
			MLookup orgL = MLookupFactory.get(Env.getCtx(), 0,
					MColumn.getColumn_ID("AD_Org", "AD_Org_ID"),
					DisplayType.TableDir, language, "AD_Org_ID", 0, false, null);
			fOrg_ID = new WTableDirEditor("AD_Org_ID", false, false, true, orgL);
			fOrg_ID.getComponent().setHflex(null);
			fOrg_ID.getComponent().setWidth("200px");
			// 默认值：当前登录组织
			fOrg_ID.setValue(Env.getAD_Org_ID(Env.getCtx()));
			fOrg_ID.addValueChangeListener(this);

			// 资源（下拉框，与 MRP.java 一致：只列出工厂/车间类型 ManufacturingResourceType=Plant）
			String resourceWhere = "S_Resource.ManufacturingResourceType='"
					+ MResource.MANUFACTURINGRESOURCETYPE_Plant
					+ "' AND S_Resource.AD_Client_ID=" + Env.getAD_Client_ID(Env.getCtx())
					+ " AND S_Resource.IsActive='Y'";
			MLookup resourceL = MLookupFactory.get(Env.getCtx(), 0,
					MColumn.getColumn_ID(MResource.Table_Name, MResource.COLUMNNAME_S_Resource_ID),
					DisplayType.TableDir, language, MResource.COLUMNNAME_S_Resource_ID, 0, false, resourceWhere);
			fResource_ID = new WTableDirEditor("S_Resource_ID", false, false, true, resourceL);
			fResource_ID.getComponent().setHflex(null);
			fResource_ID.getComponent().setWidth("200px");

			// 仓库（下拉框，与 MRP.java 一致：按 AD_Client_ID 过滤）
			MLookup warehouseL = MLookupFactory.get(Env.getCtx(), 0,
					MColumn.getColumn_ID(MWarehouse.Table_Name, MWarehouse.COLUMNNAME_M_Warehouse_ID),
					DisplayType.TableDir, language, MWarehouse.COLUMNNAME_M_Warehouse_ID, 0, false, null);
			fWarehouse_ID = new WTableDirEditor("M_Warehouse_ID", false, false, true, warehouseL);
			fWarehouse_ID.getComponent().setHflex(null);
			fWarehouse_ID.getComponent().setWidth("200px");

			// 初始化时根据默认组织联动带出仓库和资源
			syncDefaultsFromOrg(Env.getAD_Org_ID(Env.getCtx()));

			// --- 参数区：单行横向排列（标签+输入框 成对，最后接按钮） ---
			parameterGrid.setHflex(null);
			Rows rows = parameterGrid.newRows();
			Row paramRow = new Row();
			paramRow.appendChild(lOrg_ID.rightAlign());
			paramRow.appendChild(fOrg_ID.getComponent());
			paramRow.appendChild(lResource_ID.rightAlign());
			paramRow.appendChild(fResource_ID.getComponent());
			paramRow.appendChild(lWarehouse_ID.rightAlign());
			paramRow.appendChild(fWarehouse_ID.getComponent());
			paramRow.appendChild(new Label(""));
			paramRow.appendChild(btnExecute);
			rows.appendChild(paramRow);

			// --- 按钮事件 ---
			btnExecute.addEventListener(Events.ON_CLICK, this);
			btnGenerateDoc.addEventListener(Events.ON_CLICK, this);
			btnCancel.addEventListener(Events.ON_CLICK, this);

			// --- 阶段二控件容器（初始隐藏） ---
			// 第一行：两个勾选框
			phase2CheckboxRow.setValign("middle");
			phase2CheckboxRow.setStyle("display:inline-flex; align-items:center; gap:10px;");
			phase2CheckboxRow.appendChild(chkDeleteMRP);
			phase2CheckboxRow.appendChild(chkGenerateWO);

			// 第二行：两个按钮
			phase2ButtonRow.setValign("middle");
			phase2ButtonRow.setStyle("display:inline-flex; align-items:center; gap:10px; margin-top:8px;");
			phase2ButtonRow.appendChild(btnGenerateDoc);
			phase2ButtonRow.appendChild(btnCancel);

			// 外层纵向容器
			phase2Layout.setStyle("margin-top:10px; text-align:right; padding-right:20px;");
			phase2Layout.appendChild(phase2CheckboxRow);
			phase2Layout.appendChild(phase2ButtonRow);
			phase2Layout.setVisible(false);

			// --- 结果展示区（复刻 ProcessDialog 原生样式） ---
			resultHtml.setContent("<p style='color:gray;'>请设置参数后点击「开始计算」</p>");

			// 结果容器：复用原生 message-parameter CSS 类 + 卡片边框
			org.zkoss.zul.Div resultContainer = new org.zkoss.zul.Div();
			resultContainer.setSclass("message-parameter");
//			resultContainer.setStyle("border:1px solid #ccc; border-radius:4px; padding:10px; margin-bottom:10px; background-color:#f9f9f9;");

			resultContainer.setStyle("border:1px solid #ccc; border-radius:4px; padding:10px; margin-bottom:10px; "
					+ "background-color:#f9f9f9; min-height:400px; max-height:600px; overflow-y:auto;");
			resultContainer.appendChild(resultHtml);

			// --- 主布局 ---
			North north = new North();
			north.appendChild(parameterGrid);
			mainLayout.appendChild(north);

			Center center = new Center();
			center.setAutoscroll(true);
			resultPanel = new Vlayout();
			Label lResult = new Label("执行结果：");
			lResult.setStyle("font-weight:bold; margin-top:5px;");
			resultPanel.appendChild(lResult);
			resultPanel.appendChild(resultContainer);
			resultPanel.appendChild(phase2Layout);
			center.appendChild(resultPanel);
			mainLayout.appendChild(center);

			// --- 表单框架 ---
			m_frame.setWidth("99%");
			m_frame.setHeight("100%");
			m_frame.setStyle("position: absolute; padding: 0; margin: 0");
			m_frame.appendChild(mainLayout);
			mainLayout.setWidth("100%");
			mainLayout.setHeight("100%");
			mainLayout.setStyle("position: absolute");
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "MRPSimulateExecutor.initUI", e);
		}
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget() == btnExecute)
		{
			// 阶段一：预览计算
			executeMRP();
		}
		else if (event.getTarget() == btnGenerateDoc)
		{
			// 阶段二：弹出确认框后再真正生成单据
			Dialog.ask(m_frame.getWindowNo(), "确定要生成单据吗？此操作将正式创建生产工单/申购单等单据，不可预览撤销。",
					new Callback<Boolean>() {
						@Override
						public void onCallback(Boolean result)
						{
							if (Boolean.TRUE.equals(result))
							{
								generateDocuments();
							}
						}
					});
		}
		else if (event.getTarget() == btnCancel)
		{
			resetForm();
		}
	}

	// ==================== 阶段一：预览计算 ====================

	/**
	 * 阶段一：以预览模式（IsSimulate=true）执行 MRP 计算
	 *
	 * 直接实例化 MRPWithFlags 并调用 startProcess()，以便持有实例引用，
	 * 执行完后从实例的 Java 内存中读取预览明细列表（不受 rollback 影响）。
	 */
	private void executeMRP()
	{
		// 校验必填参数
		int AD_Org_ID = getIntValue(fOrg_ID);
		int S_Resource_ID = getIntValue(fResource_ID);
		int M_Warehouse_ID = getIntValue(fWarehouse_ID);

		if (AD_Org_ID <= 0)
		{
			resultHtml.setContent("<p style='color:red;'>请选择组织</p>");
			return;
		}
		if (S_Resource_ID <= 0)
		{
			resultHtml.setContent("<p style='color:red;'>请选择资源</p>");
			return;
		}
		if (M_Warehouse_ID <= 0)
		{
			resultHtml.setContent("<p style='color:red;'>请选择仓库</p>");
			return;
		}

		// 查询 MRP 进程的 AD_Process_ID（用于 ProcessInfo 构造）
		int AD_Process_ID = DB.getSQLValue(null,
				"SELECT AD_Process_ID FROM AD_Process WHERE Classname = ? AND IsActive = 'Y'",
				"org.libero.process.MRP");

		// 构造参数列表 — 阶段一始终 IsSimulate=true，不传 DeleteMRP
		List<ProcessInfoParameter> paramList = new ArrayList<>();
		paramList.add(new ProcessInfoParameter("AD_Org_ID", AD_Org_ID, null, null, null));
		paramList.add(new ProcessInfoParameter("S_Resource_ID", S_Resource_ID, null, null, null));
		paramList.add(new ProcessInfoParameter("M_Warehouse_ID", M_Warehouse_ID, null, null, null));
		paramList.add(new ProcessInfoParameter("IsSimulate", true, null, null, null));

		// 构造 ProcessInfo
		ProcessInfo pi = new ProcessInfo("MRP", Math.max(AD_Process_ID, 0));
		pi.setAD_Client_ID(Env.getAD_Client_ID(Env.getCtx()));
		pi.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
		pi.setParameter(paramList.toArray(new ProcessInfoParameter[0]));

		// 同步执行 — 直接实例化 MRPWithFlags 以便持有引用
		resultHtml.setContent("<p style='color:blue;'>正在执行MRP预览计算，请稍候...</p>");
		m_frame.invalidate();

		MRPWithFlags process = new MRPWithFlags();
		boolean success = false;
		try
		{
			success = process.startProcess(Env.getCtx(), pi, null);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "MRP 预览执行失败", e);
			resultHtml.setContent("<p style='color:red;'>执行异常：" + e.getLocalizedMessage() + "</p>");
			return;
		}

		// 展示预览结果（复刻 ProcessDialog.swithToFinishScreen() 样式）
		String summary = pi.getSummary();
		if (summary == null || summary.isEmpty())
		{
			summary = success ? "预览执行完成（无汇总信息）" : "预览执行失败（无汇总信息）";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<hr><p><font color=\"")
		  .append(pi.isError() ? "#FF0000" : "#0000FF")
		  .append("\">** ")
		  .append(summary)
		  .append("</font></p>");

		// 渲染预览明细表格（从 MRPWithFlags 内存列表读取，不受 rollback 影响）
		sb.append(renderPreviewTables(process));

		resultHtml.setContent(sb.toString());

		// 预览成功后，显示阶段二控件
		phase2Layout.setVisible(true);
	}

	// ==================== 阶段二：生成单据 ====================

	/**
	 * 阶段二：以非模拟模式（IsSimulate=false）真正执行 MRP 生成单据
	 *
	 * 直接实例化 MRPWithFlags，IsSimulate=false 会真正提交事务。
	 * 执行完成后从实例内存读取生成的单据列表并渲染表格。
	 */
	private void generateDocuments()
	{
		int AD_Org_ID = getIntValue(fOrg_ID);
		int S_Resource_ID = getIntValue(fResource_ID);
		int M_Warehouse_ID = getIntValue(fWarehouse_ID);

		if (AD_Org_ID <= 0 || S_Resource_ID <= 0 || M_Warehouse_ID <= 0)
		{
			resultHtml.setContent("<p style='color:red;'>参数缺失，请返回阶段一重新选择</p>");
			return;
		}

		int AD_Process_ID = DB.getSQLValue(null,
				"SELECT AD_Process_ID FROM AD_Process WHERE Classname = ? AND IsActive = 'Y'",
				"org.libero.process.MRP");

		// 构造参数列表 — IsSimulate=false，传入用户勾选的 DeleteMRP、GenerateWO
		List<ProcessInfoParameter> paramList = new ArrayList<>();
		paramList.add(new ProcessInfoParameter("AD_Org_ID", AD_Org_ID, null, null, null));
		paramList.add(new ProcessInfoParameter("S_Resource_ID", S_Resource_ID, null, null, null));
		paramList.add(new ProcessInfoParameter("M_Warehouse_ID", M_Warehouse_ID, null, null, null));
		paramList.add(new ProcessInfoParameter("IsSimulate", false, null, null, null));
		paramList.add(new ProcessInfoParameter("DeleteMRP", chkDeleteMRP.isChecked(), null, null, null));
		paramList.add(new ProcessInfoParameter("GenerateWO", chkGenerateWO.isChecked(), null, null, null));

		ProcessInfo pi = new ProcessInfo("MRP", Math.max(AD_Process_ID, 0));
		pi.setAD_Client_ID(Env.getAD_Client_ID(Env.getCtx()));
		pi.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
		pi.setParameter(paramList.toArray(new ProcessInfoParameter[0]));

		resultHtml.setContent("<p style='color:blue;'>正在生成单据，请稍候...</p>");
		m_frame.invalidate();

		MRPWithFlags process = new MRPWithFlags();
		boolean success = false;
		try
		{
			success = process.startProcess(Env.getCtx(), pi, null);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "MRP 生成单据失败", e);
			resultHtml.setContent("<p style='color:red;'>执行异常：" + e.getLocalizedMessage() + "</p>");
			return;
		}

		String summary = pi.getSummary();
		if (summary == null || summary.isEmpty())
		{
			summary = success ? "单据生成完成（无汇总信息）" : "单据生成失败（无汇总信息）";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<p style='color:green;'><b>【单据已生成】</b></p>");
		sb.append("<hr><p><font color=\"")
		  .append(pi.isError() ? "#FF0000" : "#0000FF")
		  .append("\">** ")
		  .append(summary)
		  .append("</font></p>");

		// 渲染生成的单据明细表格
		sb.append(renderPreviewTables(process));

		resultHtml.setContent(sb.toString());
	}

	// ==================== 重置表单 ====================

	/**
	 * 重置表单到初始状态：清空预览结果、清空参数选择、隐藏阶段二控件
	 */
	private void resetForm()
	{
		// 1. 清空预览结果区域
		resultHtml.setContent("<p style='color:gray;'>请设置参数后点击「开始计算」</p>");

		// 2. 清空三个选择框
		fOrg_ID.setValue(null);
		fResource_ID.setValue(null);
		fWarehouse_ID.setValue(null);

		// 3. 重置勾选框为默认状态
		chkDeleteMRP.setChecked(false);
		chkGenerateWO.setChecked(false);

		// 4. 隐藏阶段二控件
		phase2Layout.setVisible(false);

		m_frame.invalidate(); // 补上这一行，强制刷新整个表单
	}

	// ==================== 值变化联动 ====================

	/**
	 * 组织选中变化时，自动带出该组织的默认仓库，再根据仓库带出关联的工厂资源。
	 */
	@Override
	public void valueChange(ValueChangeEvent e)
	{
		if (e == null)
			return;

		String propertyName = e.getPropertyName();
		if ("AD_Org_ID".equals(propertyName))
		{
			Object newValue = e.getNewValue();
			int orgId = (newValue instanceof Integer) ? (Integer) newValue : 0;
			if (orgId <= 0)
				return;
			syncDefaultsFromOrg(orgId);
		}
	}

	/**
	 * 根据组织自动带出默认仓库，再根据仓库带出关联的工厂资源。
	 * 用于初始化默认值和组织值变化联动。
	 */
	private void syncDefaultsFromOrg(int orgId)
	{
		if (orgId <= 0)
			return;

		// 带出默认仓库
		int warehouseId = 0;
		try
		{
			MOrgInfo orgInfo = MOrgInfo.get(Env.getCtx(), orgId, null);
			if (orgInfo != null)
				warehouseId = orgInfo.getM_Warehouse_ID();
		}
		catch (Exception ex)
		{
			log.log(Level.WARNING, "获取组织默认仓库失败: AD_Org_ID=" + orgId, ex);
		}

		if (warehouseId > 0)
		{
			fWarehouse_ID.setValue(warehouseId);

			// 根据仓库带出关联的工厂资源
			try
			{
				int resourceId = MPPProductPlanning.getPlantForWarehouse(warehouseId);
				if (resourceId > 0)
					fResource_ID.setValue(resourceId);
			}
			catch (Exception ex)
			{
				log.log(Level.WARNING, "获取仓库关联资源失败: M_Warehouse_ID=" + warehouseId, ex);
			}
		}
	}

	// ==================== 工具方法 ====================

	/**
	 * 从 WTableDirEditor 获取整数值
	 */
	private int getIntValue(WTableDirEditor editor)
	{
		Object val = editor.getValue();
		return (val != null && val instanceof Integer) ? (Integer) val : 0;
	}

	@Override
	public ADForm getForm()
	{
		return m_frame;
	}

	// ==================== 预览明细表格渲染 ====================

	/**
	 * 从 MRPWithFlags 实例读取预览/生成的单据列表，渲染为 HTML 表格。 三个表格分别对应：生产工单、申购单、配送订单。
	 */
	private String renderPreviewTables(MRPWithFlags process)
	{
		StringBuilder html = new StringBuilder();
		String tableStyle = "border-collapse:collapse;width:100%;text-align:center;";
		String thStyle = "background:#e8e8e8;padding:4px 6px;border:1px solid #ccc;";
		String tdStyle = "padding:4px 6px;border:1px solid #ccc;";

		// 生产工单
		List<MRPWithFlags.PreviewRow> moList = process.getPreviewMOList();
		if (moList != null && !moList.isEmpty())
		{
			html.append("<h3>生产工单</h3>");
			html.append("<table style='").append(tableStyle).append("'>");
			html.append("<tr>")
			    .append("<th style='").append(thStyle).append("width:12%;'>工程单号</th>")
			    .append("<th style='").append(thStyle).append("width:10%;'>物料编码</th>")
			    .append("<th style='").append(thStyle).append("width:18%;'>物料名称</th>")
			    .append("<th style='").append(thStyle).append("width:12%;'>工单日期</th>")
			    .append("<th style='").append(thStyle).append("width:12%;'>承诺交期</th>")
			    .append("<th style='").append(thStyle).append("width:10%;'>数量</th>")
			    .append("<th style='").append(thStyle).append("width:8%;'>单位</th>")
			    .append("<th style='").append(thStyle).append("width:18%;'>资源</th>")
			    .append("</tr>");
			for (MRPWithFlags.PreviewRow row : moList)
			{
				html.append("<tr>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getDocumentNo())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getProductValue())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getProductName())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getDate1() != null ? row.getDate1() : "").append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getDate2() != null ? row.getDate2() : "").append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getQuantity()).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getUom())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getExtra())).append("</td>")
				    .append("</tr>");
			}
			html.append("</table>");
		}

		// 申购单
		List<MRPWithFlags.PreviewRow> mrList = process.getPreviewMRList();
		if (mrList != null && !mrList.isEmpty())
		{
			html.append("<h3>申购单</h3>");
			html.append("<table style='").append(tableStyle).append("'>");
			html.append("<tr>")
					.append("<th style='").append(thStyle).append("width:10%;'>申购单号</th>")
			    .append("<th style='").append(thStyle).append("width:9%;'>物料编码</th>")
					.append("<th style='").append(thStyle).append("width:16%;'>物料名称</th>")
					.append("<th style='").append(thStyle).append("width:10%;'>申购日期</th>")
					.append("<th style='").append(thStyle).append("width:10%;'>需求日期</th>")
					.append("<th style='").append(thStyle).append("width:9%;'>需求数量</th>")
					.append("<th style='").append(thStyle).append("width:9%;'>在途数量</th>")
			    .append("<th style='").append(thStyle).append("width:7%;'>单位</th>")
					.append("<th style='").append(thStyle).append("width:20%;'>供应商</th>")
			    .append("</tr>");
			for (MRPWithFlags.PreviewRow row : mrList)
			{
				html.append("<tr>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getDocumentNo())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getProductValue())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getProductName())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getDate1() != null ? row.getDate1() : "").append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getDate2() != null ? row.getDate2() : "").append("</td>")
						.append("<td style='").append(tdStyle).append("'>").append(row.getQuantity()).append("</td>")
						.append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getInTransitQty())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getUom())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getExtra())).append("</td>")
				    .append("</tr>");
			}
			html.append("</table>");
		}

		// 配送订单
		List<MRPWithFlags.PreviewRow> doList = process.getPreviewDOList();
		if (doList != null && !doList.isEmpty())
		{
			html.append("<h3>配送订单</h3>");
			html.append("<table style='").append(tableStyle).append("'>");
			html.append("<tr>")
			    .append("<th style='").append(thStyle).append("width:12%;'>配送单号</th>")
			    .append("<th style='").append(thStyle).append("width:10%;'>物料编码</th>")
			    .append("<th style='").append(thStyle).append("width:18%;'>物料名称</th>")
			    .append("<th style='").append(thStyle).append("width:12%;'>配送日期</th>")
			    .append("<th style='").append(thStyle).append("width:12%;'>承诺日期</th>")
			    .append("<th style='").append(thStyle).append("width:10%;'>数量</th>")
			    .append("<th style='").append(thStyle).append("width:8%;'>单位</th>")
			    .append("<th style='").append(thStyle).append("width:18%;'>仓库</th>")
			    .append("</tr>");
			for (MRPWithFlags.PreviewRow row : doList)
			{
				html.append("<tr>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getDocumentNo())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getProductValue())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getProductName())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getDate1() != null ? row.getDate1() : "").append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getDate2() != null ? row.getDate2() : "").append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(row.getQuantity()).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getUom())).append("</td>")
				    .append("<td style='").append(tdStyle).append("'>").append(escapeHtml(row.getExtra())).append("</td>")
				    .append("</tr>");
			}
			html.append("</table>");
		}

		return html.toString();
	}

	/**
	 * 转义 HTML 特殊字符，防止 XSS
	 */
	private String escapeHtml(String text)
	{
		if (text == null)
			return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
