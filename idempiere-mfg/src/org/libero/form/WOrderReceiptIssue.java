/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.libero.form;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.ProcessInfoDialog;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WLocatorEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WPAttributeEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MStorageReservation;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTab;
import org.compiere.model.MUOM;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
import org.compiere.wf.MWFProcess;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.I_PP_Order_BOMLine;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Window;
/**
 *  @author Cristina Ghita, www.arhipac.ro
 *  @author Adi Takacs, www.arhipac.ro
 *  @author victor.perez@e-evolution.com, www.e-evolution.com
 */

public class WOrderReceiptIssue extends OrderReceiptIssue  implements IFormController, EventListener,  
ValueChangeListener,Serializable,WTableModelListener  
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3451096834043054791L;
	private static final CLogger log = CLogger.getCLogger(WOrderReceiptIssue.class);
	
	/**	Window No			*/
	private int m_WindowNo = 0;
	private String m_sql;
	private MPPOrder m_PP_order = null;
	
	private Panel Generate = new Panel();
	private Panel PanelBottom = new Panel();
	private Panel mainPanel = new Panel();
	private Panel northPanel = new Panel();
	private Button Process = new Button();
	
	private Label attributeLabel = new Label();
	private Label orderedQtyLabel = new Label();
	private Label deliveredQtyLabel = new Label();
	private Label openQtyLabel = new Label();
	private Label orderLabel = new Label();
	private Label toDeliverQtyLabel = new Label();
	private Label movementDateLabel = new Label();
	private Label rejectQtyLabel = new Label();
	private Label resourceLabel = new Label();
	private Label nodeLabel = new Label("工序"); // 添加工序标签
	
	private CustomForm form = new CustomForm();
	private Borderlayout ReceiptIssueOrder = new Borderlayout();
	private Tabbox TabsReceiptsIssue = new Tabbox();
	private Html info = new Html();
	private Grid fieldGrid = GridFactory.newGridLayout();
	private WPAttributeEditor attribute = null;
	
	private Label warehouseLabel = new Label();
	private Label scrapQtyLabel = new Label();
	private Label productLabel = new Label(Msg.translate(Env.getCtx(),"M_Product_ID"));
	private Label uomLabel = new Label(Msg.translate(Env.getCtx(), "C_UOM_ID"));
	private Label uomorderLabel = new Label(Msg.translate(Env.getCtx(), "Altert UOM"));
	private Label locatorLabel = new Label(Msg.translate(Env.getCtx(), "M_Locator_ID"));
	private Label backflushGroupLabel = new Label(Msg.translate(Env.getCtx(), "BackflushGroup"));
	private Label labelcombo = new Label(Msg.translate(Env.getCtx(), "DeliveryRule"));
	private Label QtyBatchsLabel = new Label();
	private Label QtyBatchSizeLabel = new Label();
	
	private Textbox backflushGroup = new Textbox();
	
	private WNumberEditor orderedQtyField = new WNumberEditor("QtyOrdered", false, false, false, DisplayType.Quantity, "QtyOrdered");
	private WNumberEditor deliveredQtyField = new WNumberEditor("QtyDelivered", false, false, false, DisplayType.Quantity, "QtyDelivered");
	private WNumberEditor openQtyField = new WNumberEditor("QtyOpen", false, false, false, DisplayType.Quantity, "QtyOpen");
	private WNumberEditor toDeliverQty = new WNumberEditor("QtyToDeliver", true, false, true, DisplayType.Quantity, "QtyToDeliver");
	private WNumberEditor rejectQty = new WNumberEditor("Qtyreject", false, false, true, DisplayType.Quantity, "QtyReject");
	private WNumberEditor scrapQtyField = new WNumberEditor("Qtyscrap", false, false, true, DisplayType.Quantity, "Qtyscrap");
	private WNumberEditor qtyBatchsField = new WNumberEditor("QtyBatchs", false, false, false, DisplayType.Quantity, "QtyBatchs");
	private WNumberEditor qtyBatchSizeField = new WNumberEditor("QtyBatchSize", false, false, false, DisplayType.Quantity, "QtyBatchSize");
	
	private WSearchEditor orderField = null;
	private Combobox nodeCombo = new Combobox(); // 改为Combobox下拉框
//	private WSearchEditor resourceField = null;
	private Combobox resourceCombo = new Combobox();

	private WSearchEditor warehouseField = null;
	private WSearchEditor productField = null;
	private WSearchEditor uomField = null;
	private WSearchEditor uomorderField = null;
	
	private WListbox issue = ListboxFactory.newDataTable();
	private WDateEditor movementDateField = new WDateEditor("MovementDate", true, false, true,  "MovementDate");	
	
	private WLocatorEditor locatorField = null;
	private Label activityLabel = new Label("部门");
	private Combobox activityCombo = new Combobox();
	
	// 在类开头，添加以下成员变量
	private Map<Integer, BigDecimal> originalValues = new HashMap<>(); // 保存编辑前的值
	private boolean isRestoringValue = false; // 标记是否正在恢复值
	private int currentEditingRow = -1; // 当前正在编辑的行
	private BigDecimal currentEditingOriginalValue = null; // 当前编辑的原始值
	
	private Combobox pickcombo = new Combobox();

	private Label fulfilledFilterLabel = new Label("仅显示已领物料");
	private Combobox fulfilledFilterCombo = new Combobox();

	private Button addMaterialButton = new Button();
	private Button deleteMaterialButton = new Button("删除物料");
	private Button cancelButton = new Button("取消");
	private Button viewOrderButton = new Button("查看工单");
	private Button refreshButton = new Button("刷新");
	private Button generalIssueButton = new Button("通用物料领用");

	/**
	 *	Initialize Panel
	 *  @param WindowNo window
	 *  @param frame frame
	 */
	
	public WOrderReceiptIssue() 
	{
		Env.setContext(Env.getCtx(), form.getWindowNo(), "IsSOTrx", "Y");
		try 
		{
			//	UI
			fillPicks();
			jbInit();
			//
			dynInit();
			pickcombo.addEventListener(Events.ON_CHANGE, this);

		} 
		catch (Exception e) 
		{
			throw new AdempiereException(e);
		}
	} //	init

	/**
	 *	Fill Picks
	 *		Column_ID from C_Order
	 *	This is only run as part of the windows initialization process
	 *  @throws Exception if Lookups cannot be initialized
	 */
	private void fillPicks() throws Exception 
	{

		Properties ctx = Env.getCtx();
		Language language = Language.getLoginLanguage(); // Base Language

		// 工序字段改为Combobox下拉框
		nodeCombo.setReadonly(false);
		nodeCombo.addEventListener(Events.ON_CHANGE, new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				onNodeComboChanged();
			}
		});

		MLookup orderLookup = MLookupFactory.get(ctx, m_WindowNo,
											MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_PP_Order_ID),
											DisplayType.Search, language, "PP_Order_ID", 0, false,
											"PP_Order.DocStatus = '" + MPPOrder.DOCACTION_Complete + "'");

		orderField = new WSearchEditor(MPPOrder.COLUMNNAME_PP_Order_ID, false, false, true, orderLookup);
		orderField.addValueChangeListener(this);

//		MLookup resourceLookup = MLookupFactory.get(ctx, m_WindowNo, 0,
//											   MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_S_Resource_ID),
//											   DisplayType.TableDir);
//		resourceField = new WSearchEditor(MPPOrder.COLUMNNAME_S_Resource_ID, false, false, false, resourceLookup);

		MLookup warehouseLookup = MLookupFactory.get(ctx, m_WindowNo, 0,
												MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_M_Warehouse_ID),
												DisplayType.TableDir);
		warehouseField = new WSearchEditor(MPPOrder.COLUMNNAME_M_Warehouse_ID, false, false, false, warehouseLookup);

		MLookup productLookup = MLookupFactory.get(ctx, m_WindowNo, 0,
											  	   MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_M_Product_ID),
											  	   DisplayType.TableDir);
		productField = new WSearchEditor(MPPOrder.COLUMNNAME_M_Product_ID, false, false, false, productLookup);

		MLookup uomLookup = MLookupFactory.get(ctx, m_WindowNo, 0,
											   MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_C_UOM_ID),
											   DisplayType.TableDir);
		uomField = new WSearchEditor(MPPOrder.COLUMNNAME_C_UOM_ID, false, false, false, uomLookup);

		MLookup uomOrderLookup = MLookupFactory.get(ctx, m_WindowNo, 0,
													MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_C_UOM_ID),
													DisplayType.TableDir);
		
		uomorderField = new WSearchEditor(MPPOrder.COLUMNNAME_C_UOM_ID, false, false, false, uomOrderLookup);

		MLocatorLookup locatorL = new MLocatorLookup(ctx, m_WindowNo);
		locatorField = new WLocatorEditor(MLocator.COLUMNNAME_M_Locator_ID, true, false, true, locatorL, m_WindowNo);

		
		//  Tab, Window
		int m_Window = MWindow.getWindow_ID("Manufacturing Order");
		GridFieldVO vo = GridFieldVO.createStdField(ctx, m_WindowNo, 0,m_Window, MTab.getTab_ID(m_Window, "Manufacturing Order"), 
													false, false, false);
		vo.AD_Column_ID = MColumn.getColumn_ID(MPPOrder.Table_Name, MPPOrder.COLUMNNAME_M_AttributeSetInstance_ID);
		vo.ColumnName = MPPOrder.COLUMNNAME_M_AttributeSetInstance_ID;
		vo.displayType = DisplayType.PAttribute;  

		GridField field = new GridField(vo);
		// M_AttributeSetInstance_ID
		attribute = new WPAttributeEditor(field.getGridTab(),field);
		attribute.setValue(0);
		// 4Layers - Further init
		scrapQtyField.setValue(Env.ZERO);
		rejectQty.setValue(Env.ZERO);
		// 4Layers - end
		// 修改这里：调整为生产领料、生产补领、生产退料
	    pickcombo.appendItem("生产领料", 1);           // 修改：生产发料改为生产领料
	    pickcombo.appendItem("生产补领", 2);           // 生产补领
	    pickcombo.appendItem("生产退料", 3);           // 生产退料

		pickcombo.appendItem("委外发料", 4);
		pickcombo.appendItem("委外补领", 5);
		pickcombo.appendItem("委外退料", 6);
		pickcombo.addEventListener(Events.ON_CHANGE, this);
		Process.addActionListener(this);
		toDeliverQty.addValueChangeListener(this);
		scrapQtyField.addValueChangeListener(this);

		loadActivityComboData();
	} //	fillPicks
	
	/**
	 * 静态初始化方法
	 * 放置静态视觉元素到窗口中
	 * 这仅在窗口初始化过程中运行
	 * <pre>
	 * mainPanel
	 *     northPanel
	 *     centerPanel
	 *         xMatched
	 *         xPanel
	 *         xMathedTo
	 *     southPanel
	 * </pre>
	 * @throws Exception
	 */
	private void jbInit() throws Exception {
		Center center = new Center();
		South south = new South();
		North north = new North();
		form.appendChild(mainPanel);
		mainPanel.appendChild(TabsReceiptsIssue);
		mainPanel.setStyle("width: 100%; height: 100%; padding: 0; margin: 0");
		ReceiptIssueOrder.setWidth("100%");
		ReceiptIssueOrder.setHeight("99%");
		ReceiptIssueOrder.appendChild(north);
		north.appendChild(northPanel);
		northPanel.appendChild(fieldGrid);

		// 设置标签文本为中文
		orderLabel.setText("生产工单");
		nodeLabel.setText("工序");
		deliveredQtyLabel.setText("工单数量");
		resourceLabel.setText("机台");
		productLabel.setText("产品");
		uomLabel.setText("单位");
		warehouseLabel.setText("仓库");
		labelcombo.setText("领退类型");
		movementDateLabel.setText("领料日期");
		locatorLabel.setText("库位");

		// 设置领料日期默认值为当前日期
		Timestamp currentDate = new Timestamp(System.currentTimeMillis());
		movementDateField.setValue(currentDate);
		setMovementDate(currentDate);

		Rows tmpRows = fieldGrid.newRows();

		// 第1行：生产工单 | 工序 | 工单数量 | 机台
		Row tmpRow = tmpRows.newRow();
		tmpRow.appendChild(orderLabel.rightAlign());
		tmpRow.appendChild(orderField.getComponent());
		orderField.setReadWrite(true);

		tmpRow.appendChild(deliveredQtyLabel.rightAlign());
		tmpRow.appendChild(deliveredQtyField.getComponent());
		deliveredQtyField.setReadWrite(false);

		tmpRow.appendChild(nodeLabel.rightAlign());
		tmpRow.appendChild(nodeCombo);
		nodeCombo.setReadonly(false);

		tmpRow.appendChild(resourceLabel.rightAlign());
		tmpRow.appendChild(resourceCombo);
		resourceCombo.setReadonly(false);
		resourceCombo.addEventListener(Events.ON_CHANGE, this);
		loadResourceComboData();

		// 第2行：产品 | 单位 | 仓库
		tmpRow = tmpRows.newRow();
		tmpRow.appendChild(productLabel.rightAlign());
		tmpRow.appendChild(productField.getComponent());
		productField.setReadWrite(false);
		tmpRow.appendChild(uomLabel.rightAlign());
		tmpRow.appendChild(uomField.getComponent());
		uomField.setReadWrite(false);
		tmpRow.appendChild(warehouseLabel.rightAlign());
		tmpRow.appendChild(warehouseField.getComponent());
		warehouseField.setReadWrite(false);

		tmpRow.appendChild(activityLabel.rightAlign());
		tmpRow.appendChild(activityCombo);
		activityCombo.setReadonly(false);

		// 第3行：领退料类型 | 领料日期 | 库位 | 是否满足领料需求
		tmpRow = tmpRows.newRow();
		tmpRow.appendChild(labelcombo.rightAlign());
		tmpRow.appendChild(pickcombo);
		tmpRow.appendChild(movementDateLabel.rightAlign());
		tmpRow.appendChild(movementDateField.getComponent());
		movementDateField.setReadWrite(true);
		tmpRow.appendChild(locatorLabel.rightAlign());
		tmpRow.appendChild(locatorField.getComponent());
		locatorField.setReadWrite(true);

		tmpRow.appendChild(fulfilledFilterLabel.rightAlign());
		tmpRow.appendChild(fulfilledFilterCombo);
		fulfilledFilterCombo.appendItem("全部", null);
		fulfilledFilterCombo.appendItem("是", "Y");
		fulfilledFilterCombo.appendItem("否", "N");
		fulfilledFilterCombo.setSelectedIndex(0);
		fulfilledFilterCombo.addEventListener(Events.ON_CHANGE, this);

		// ↓↓↓ 修改 PanelBottom 布局 ↓↓↓
		PanelBottom.setWidth("100%");
		PanelBottom.setStyle("padding:5px");

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setAlign("center");

		// 左列：新增物料 + 删除物料
		Div leftDiv = new Div();
		leftDiv.setHflex("1");
		leftDiv.setStyle("text-align:left");
		addMaterialButton.setLabel("新增物料");
		addMaterialButton.addEventListener(Events.ON_CLICK, this);
		deleteMaterialButton.setEnabled(false);
		deleteMaterialButton.setLabel("删除物料");
		deleteMaterialButton.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				// 获取所有选中的行
				final List<Integer> selectedIds = new ArrayList<Integer>();
				for (int i = 0; i < issue.getRowCount(); i++) {
					IDColumn idColumn = (IDColumn) issue.getValueAt(i, 0);
					if (idColumn != null && idColumn.isSelected()) {
						selectedIds.add(idColumn.getRecord_ID());
					}
				}

				if (selectedIds.isEmpty()) {
					Messagebox.show("请先勾选要删除的物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
					return;
				}

				// ↓↓↓ 改为回调方式 ↓↓↓
				Messagebox.show("确认删除选中的 " + selectedIds.size() + " 个BOM子件吗？", "确认", Messagebox.YES | Messagebox.NO,
						Messagebox.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
							public void onEvent(Event evt) throws Exception {
								if ("onYes".equals(evt.getName())) {
									deleteProductFromOrderBOM(selectedIds);
								}
								// onNo 或关闭弹框后，executeQuery() 刷新表格
								executeQuery();
							}
						});
			}
		});
		leftDiv.appendChild(addMaterialButton);
		leftDiv.appendChild(deleteMaterialButton);

		viewOrderButton.setEnabled(false); // 初始置灰
		viewOrderButton.addEventListener(Events.ON_CLICK, this);
		leftDiv.appendChild(viewOrderButton);
		refreshButton.addEventListener(Events.ON_CLICK, this);
		leftDiv.appendChild(refreshButton);
		generalIssueButton.addEventListener(Events.ON_CLICK, this);
		leftDiv.appendChild(generalIssueButton);

		// 中列：占位（保持对称）
		Div centerDiv = new Div();
		centerDiv.setHflex("1");

		// 右列：取消 + 确定（靠右）
		Div rightDiv = new Div();
		rightDiv.setHflex("1");
		rightDiv.setStyle("text-align:right");
		cancelButton.setLabel("取消");
		cancelButton.addEventListener(Events.ON_CLICK, this);
		Process.setLabel(Msg.translate(Env.getCtx(), "确定"));
		rightDiv.appendChild(cancelButton);
		rightDiv.appendChild(Process);

		hbox.appendChild(leftDiv);
		hbox.appendChild(centerDiv);
		hbox.appendChild(rightDiv);

		PanelBottom.appendChild(hbox);
		// ↑↑↑ 修改结束 ↑↑↑

		ReceiptIssueOrder.appendChild(center);
		center.appendChild(issue);
		ReceiptIssueOrder.appendChild(south);
		south.appendChild(PanelBottom);

		// 标签页设置
		Tabs tabs = new Tabs();
		Tab tab1 = new Tab();
		tab1.setLabel("领退料申请");
		tabs.appendChild(tab1);
		TabsReceiptsIssue.appendChild(tabs);
		Tabpanels tabps = new Tabpanels();
		Tabpanel tabp1 = new Tabpanel();
		TabsReceiptsIssue.appendChild(tabps);
		TabsReceiptsIssue.setWidth("100%");
		TabsReceiptsIssue.setHeight("100%");
		tabps.appendChild(tabp1);
		tabp1.appendChild(ReceiptIssueOrder);
		tabp1.setWidth("100%");
		tabp1.setHeight("100%");
		TabsReceiptsIssue.addEventListener(Events.ON_CHANGE, this);

	}

	public void dynInit() {  
	    disableToDeliver();  
	    prepareTable(issue);  
	    issue.autoSize();  
	      
	    // 先加载数据  
	    executeQuery();  
	    
	    // 添加表格模型监听器  
	    if (issue.getModel() != null) {  
	        issue.getModel().addTableModelListener(this);  
	    }  
	      
	    issue.setRowCount(0);  
	    
	}
	
	
	@Override  
	public void tableChanged(WTableModelEvent event) {  

		// 1. 检查是否有选中的行，控制删除按钮状态
		boolean hasSelection = false;
		for (int i = 0; i < issue.getRowCount(); i++) {
			IDColumn idColumn = (IDColumn) issue.getValueAt(i, 0);
			if (idColumn != null && idColumn.isSelected()) {
				hasSelection = true;
				break;
			}
		}
		deleteMaterialButton.setEnabled(hasSelection);
		viewOrderButton.setEnabled(hasSelection);

	    if (isRestoringValue) return; // 避免递归调用  
	      
	    int row = event.getFirstRow();  
	    int column = event.getColumn();  
	      
		// 2. 处理领取数量列的变化
	    if (column == 7 && row >= 0) {   
	        try {  
	            Object value = issue.getValueAt(row, column);  
	            BigDecimal newValue = convertToBigDecimal(value);  
	            validateAndAdjustQuantity(row, column, newValue);  
	        } catch (Exception ex) {  
	            log.severe("表格变化处理出错: " + ex.getMessage());  
	        }  
	    }  

	}
	private BigDecimal convertToBigDecimal(Object value) {  
	    if (value == null) return Env.ZERO;  
	    if (value instanceof BigDecimal) return (BigDecimal) value;  
	    if (value instanceof Number) return new BigDecimal(value.toString());  
	    if (value instanceof String) {  
	        try {  
	            String str = ((String) value).trim();  
	            return str.isEmpty() ? Env.ZERO : new BigDecimal(str);  
	        } catch (NumberFormatException e) {  
	            return Env.ZERO;  
	        }  
	    }  
	    return Env.ZERO;  
	}

	

	public void prepareTable(IMiniTable miniTable)
	{
		configureMiniTable(miniTable);
	}
	
	/**
	 * Called when events occur in the window
	 */
	
	/**
	 * Called when events occur in the window
	 */
	public void onEvent(Event e) throws Exception 
	{
	    if (e.getName().equals(Events.ON_CANCEL))
	    {
	        dispose();
	        return;
	    }

	    if (e.getTarget().equals(Process))
	    {
	        // 首先检查是否有选择生产工单
	        if (getPP_Order_ID() <= 0) {
	            Messagebox.show("请先选择生产工单", "提示", Messagebox.OK, Messagebox.INFORMATION);
	            return;
	        }
	        
	        if (getMovementDate() == null)
	        {
	            Messagebox.show(Msg.getMsg(Env.getCtx(), "日期为空"), "提示", Messagebox.OK, Messagebox.INFORMATION);
	            return;
	        }
	        
	        // 检查领料类型选择
	        if (pickcombo.getSelectedIndex() < 0) {
	            Messagebox.show("请选择领退料类型", "提示", Messagebox.OK, Messagebox.INFORMATION);
	            return;
	        }
			// 添加机台必填校验
			if (getS_Resource_ID() <= 0) {
				Messagebox.show("机台字段为必填项，请选择机台", "提示", Messagebox.OK, Messagebox.INFORMATION);
				return;
			}
			if (getC_Activity_ID() <= 0) {
				Messagebox.show("部门字段为必填项，请选择部门", "提示", Messagebox.OK, Messagebox.INFORMATION);
				return;
			}


	        // 根据领料类型检查库位
	        String selectedType = pickcombo.getSelectedItem().getLabel();
	        if ("生产退料".equals(selectedType) && getM_Locator_ID() <= 0) 
	        {
	            Messagebox.show(Msg.getMsg(Env.getCtx(), "库位为空"), "提示", Messagebox.OK, Messagebox.INFORMATION);
	            return;
	        }
	        
	        // 检查是否至少勾选了一行物料
	        boolean hasSelectedMaterial = false;
	        for (int i = 0; i < issue.getRowCount(); i++) {
	            IDColumn idColumn = (IDColumn) issue.getValueAt(i, 0);
	            if (idColumn != null && idColumn.isSelected()) {
	                hasSelectedMaterial = true;
	                break;
	            }
	        }
	        
	        if (!hasSelectedMaterial) {
	            Messagebox.show("请至少勾选一行物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
	            return;
	        }
	        
	        // 检查是否所有勾选的行都有有效的领取数量
	        boolean hasValidQty = true;
	        StringBuilder errorMessage = new StringBuilder();
	        for (int i = 0; i < issue.getRowCount(); i++) {
	            IDColumn idColumn = (IDColumn) issue.getValueAt(i, 0);
	            if (idColumn != null && idColumn.isSelected()) {
	                // 获取领取数量（第7列）
	                Object qtyObj = issue.getValueAt(i, 7);
	                BigDecimal qtyToDeliver = Env.ZERO;
	                
	                if (qtyObj instanceof BigDecimal) {
	                    qtyToDeliver = (BigDecimal) qtyObj;
	                } else if (qtyObj instanceof String) {
	                    try {
	                        qtyToDeliver = new BigDecimal((String) qtyObj);
	                    } catch (NumberFormatException ex) {
	                        // 忽略格式错误，后面会检查是否为0
	                    }
	                } else if (qtyObj instanceof Number) {
	                    qtyToDeliver = new BigDecimal(qtyObj.toString());
	                }
	                
	                // 获取物料名称（第1列或第2列，根据具体实现）
	                String materialCode = "";
	                Object materialCodeObj = issue.getValueAt(i, 1);
	                if (materialCodeObj != null) {
	                    materialCode = materialCodeObj.toString();
	                }
	                
	                // 检查领取数量是否大于0
	                if (qtyToDeliver.compareTo(Env.ZERO) <= 0) {
	                    hasValidQty = false;
	                    errorMessage.append("物料[").append(materialCode).append("]的领取数量必须大于0\n");
	                }
	                
	                // 获取需求数量和已领数量
	                Object requiredQtyObj = issue.getValueAt(i, 5);
	                Object deliveredQtyObj = issue.getValueAt(i, 6);
	                BigDecimal requiredQty = Env.ZERO;
	                BigDecimal deliveredQty = Env.ZERO;
	                
	                if (requiredQtyObj instanceof BigDecimal) {
	                    requiredQty = (BigDecimal) requiredQtyObj;
	                }
	                if (deliveredQtyObj instanceof BigDecimal) {
	                    deliveredQty = (BigDecimal) deliveredQtyObj;
	                }
	                
//	                // 检查领取数量是否超过待领数量（需求数量 - 已领数量）
//	                BigDecimal availableQty = requiredQty.subtract(deliveredQty);
//	                if (qtyToDeliver.compareTo(availableQty) > 0) {
//	                    hasValidQty = false;
//	                    errorMessage.append("物料[").append(materialCode).append("]的领取数量(")
//	                               .append(qtyToDeliver).append(")超过可领取数量(").append(availableQty).append(")\n");
//	                }
	            }
	        }
	        
	        if (!hasValidQty) {
	            Messagebox.show("以下物料存在问题：\n" + errorMessage.toString(), "物料数据错误", Messagebox.OK, Messagebox.ERROR);
	            return;
	        }
	        
	        // 检查是否有可领取的物料（领取数量大于0）
	        boolean hasQtyToDeliver = false;
	        for (int i = 0; i < issue.getRowCount(); i++) {
	            IDColumn idColumn = (IDColumn) issue.getValueAt(i, 0);
	            if (idColumn != null && idColumn.isSelected()) {
	                Object qtyObj = issue.getValueAt(i, 7);
	                BigDecimal qtyToDeliver = Env.ZERO;
	                
	                if (qtyObj instanceof BigDecimal) {
	                    qtyToDeliver = (BigDecimal) qtyObj;
	                } else if (qtyObj instanceof String) {
	                    try {
	                        qtyToDeliver = new BigDecimal((String) qtyObj);
	                    } catch (NumberFormatException ex) {
	                        // 忽略格式错误
	                    }
	                }
	                
	                if (qtyToDeliver.compareTo(Env.ZERO) > 0) {
	                    hasQtyToDeliver = true;
	                    break;
	                }
	            }
	        }
	        
	        if (!hasQtyToDeliver) {
	            Messagebox.show("所有勾选物料的领取数量都为0，无需提交", "提示", Messagebox.OK, Messagebox.INFORMATION);
	            return;
	        }
	        
	        // 异步确认对话框
	        Messagebox.show("确认提交领退料申请？",  
	        	    "确认", Messagebox.OK | Messagebox.CANCEL,  
	        	    Messagebox.QUESTION,  
	        	    new org.zkoss.zk.ui.event.EventListener() {  
	        	        public void onEvent(Event e) {  
	        	            if ("onOK".equals(e.getName())) {  
	        	                if (cmd_process(false, issue)) {  
	        	                    // 用 ProcessInfoDialog 替换原来的 Messagebox  
	        	                    if (lastProcessInfo != null) {  
	        	                        lastProcessInfo.setSummary("领退料申请已成功提交！");  
	        	                        ProcessInfoDialog dialog = ProcessInfoDialog.showProcessInfo(  
	        	                            lastProcessInfo,  
	        	                            m_WindowNo,   // 你的 Form 的 windowNo  
	        	                            form,  // Form 组件本身（Component）  
	        	                            false         // 不从 DB 重新加载 log  
	        	                        );  
	        	                        dialog.setAutoCloseAfterZoom(true); // 点击链接后自动关闭弹窗  
	        	                    }  
	        	  
	        	                    Integer currentOrderId = getPP_Order_ID();  
	        	                    if (currentOrderId != null && currentOrderId > 0) {  
	        	                        reloadOrderData(currentOrderId);  
	        	                    }  
	        	                }  
	        	            }  
	        	        }  
	        	    }  
	        	);
	    }    

		if (e.getTarget().equals(pickcombo)) {
			// 委外退料显示库位
			if (isSubcontractingReturn()) {
	            locatorLabel.setVisible(true);  
	            locatorField.setVisible(true);  
	            issue.setVisible(true);  
	            executeQuery();  
	        }  
			// 委外发料/委外补领隐藏库位
			else if (isSubcontractingIssue() || isSubcontractingReplenishment()) {
				locatorLabel.setVisible(false);
	            locatorField.setVisible(false);  
	            issue.setVisible(true);  
	            executeQuery();  
	        }  
			// 生产退料显示库位
			else if (isProductionReturn()) {
				locatorLabel.setVisible(true);
				locatorField.setVisible(true);
				issue.setVisible(true);
				executeQuery();
			}
			// 生产领料/生产补领隐藏库位
			else if (isOnlyIssue() || isProductionReplenishment()) {
				locatorLabel.setVisible(false);
	            locatorField.setVisible(false);  
	            issue.setVisible(true);  
	            executeQuery();  
	        }  
	    }

		if (e.getTarget().equals(fulfilledFilterCombo)) {
			executeQuery();
		}
		if (e.getTarget().equals(addMaterialButton)) {
			onAddMaterial();
		}
		if (e.getTarget().equals(cancelButton)) {
			dispose();
		}
		if (e.getTarget().equals(viewOrderButton)) {
			int selectedBOMLineId = -1;
			for (int i = 0; i < issue.getRowCount(); i++) {
				IDColumn idColumn = (IDColumn) issue.getValueAt(i, 0);
				if (idColumn != null && idColumn.isSelected()) {
					selectedBOMLineId = idColumn.getRecord_ID();
					break;
				}
			}
			if (selectedBOMLineId <= 0)
				return;

			// 直接用 Table_ID + Record_ID，内部自动处理 ZoomTableName/ZoomColumnName
			AEnv.zoom(I_PP_Order_BOMLine.Table_ID, selectedBOMLineId);
		}
		if (e.getTarget().equals(refreshButton)) {
			executeQuery();
		}
		if (e.getTarget().equals(generalIssueButton)) {
			onGeneralIssue();
		}
	}
	
	
	/**
	 * 重新加载工单数据
	 * @param orderId 工单ID
	 */
	private void reloadOrderData(int orderId) {
	    try {
	        // 1. 保存当前选中的工序
			Integer selectedNodeId = getPP_Order_Node_ID();
	        
	        // 2. 重新加载工单基本信息
	        MPPOrder pp_order = new MPPOrder(Env.getCtx(), orderId, null);
	        if (pp_order != null) {
	            // 更新工单数量相关字段
	            setDeliveredQty(pp_order.getQtyDelivered());
	            setOrderedQty(pp_order.getQtyOrdered());
	            setQtyBatchs(pp_order.getQtyBatchs());
	            setQtyBatchSize(pp_order.getQtyBatchSize());
	            setDeliveredQty(pp_order.getQtyOrdered());
	            setToDeliverQty(getOpenQty());
	            
	            // 更新产品信息
	            setM_Product_ID(pp_order.getM_Product_ID());
	            MProduct m_product = MProduct.get(Env.getCtx(), pp_order.getM_Product_ID());
	            setC_UOM_ID(m_product.getC_UOM_ID());
	            setOrder_UOM_ID(pp_order.getC_UOM_ID());
	            
	            // 3. 重新加载工序下拉框数据
				int ppOrderId = pp_order.get_ID();
				loadNodeComboData(ppOrderId);
	            
	            // 4. 恢复之前选中的工序
	            if (selectedNodeId != null && selectedNodeId > 0) {
	                // 查找并选中原来的工序
	                for (int i = 0; i < nodeCombo.getItemCount(); i++) {
	                    org.zkoss.zul.Comboitem item = nodeCombo.getItemAtIndex(i);
	                    Object itemValue = item.getValue();
	                    if (itemValue != null && itemValue instanceof Integer) {
	                        if (((Integer) itemValue).intValue() == selectedNodeId) {
	                            nodeCombo.setSelectedIndex(i);
	                            // 触发工序变更事件，自动带出机台
	                            onNodeComboChanged();
	                            break;
	                        }
	                    }
	                }
	            }
	            
	            // 5. 重新查询并加载物料表格数据
	            executeQuery();
	            
				updateMaterialButtonsVisibility();

	            log.info("工单数据已重新加载，工单ID: " + orderId);
	        }
	    } catch (Exception ex) {
	        log.severe("重新加载工单数据时出错: " + ex.getMessage());
	        // 如果重新加载失败，至少清空表格并显示错误
	        issue.clearTable();
	        issue.setRowCount(0);
	        issue.repaint();
	    }
	}
	
	/**
	 * 加载机台下拉框数据
	 */
	private void loadResourceComboData() {
		resourceCombo.getItems().clear();
		resourceCombo.appendItem("请选择机台", null);

		// 从当前选中的 PP_Order 获取 AD_Org_ID\AD_Client_ID
	    MPPOrder ppOrder = getPP_Order();  
	    int orgId    = (ppOrder != null) ? ppOrder.getAD_Org_ID() : 0;  
	    int clientId = Env.getAD_Client_ID(Env.getCtx());  
	  
	    String sql = "SELECT S_Resource_ID, Name FROM S_Resource "  
	               + "WHERE IsActive='Y' "  
	               + "AND AD_Client_ID = ? "  
	               + "AND (AD_Org_ID = ? OR AD_Org_ID = 0) "  
	               + "ORDER BY Name";  

		//String sql = "SELECT S_Resource_ID, Name FROM S_Resource WHERE IsActive='Y' AND S_ResourceType_ID = 1000003 ORDER BY Name";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, null);
	        pstmt.setInt(1, clientId);  
	        pstmt.setInt(2, orgId); 
			rs = pstmt.executeQuery();

			while (rs.next()) {
				int resourceId = rs.getInt("S_Resource_ID");
				String resourceName = rs.getString("Name");
				if (resourceName == null) {
					resourceName = "Unnamed";
				}
				resourceCombo.appendItem(resourceName, resourceId);
			}

			resourceCombo.setSelectedIndex(0);

		} catch (SQLException e) {
			log.severe("加载机台数据时出错: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	/**
	 * 加载工序下拉框数据
	 * 
	 * @param ppOrderId 工单ID
	 */
	private void loadNodeComboData(int ppOrderId) {
		// 清空当前选项
		nodeCombo.getItems().clear();
		
		if (ppOrderId <= 0) {
			// 添加工序为空的选项
			nodeCombo.appendItem("请选择工序", null);
			nodeCombo.setSelectedIndex(0);
			return;
		}
		
		
		// 查询该工单的所有工序节点
		String sql = "SELECT PP_Order_Node_ID, Name FROM PP_Order_Node WHERE PP_Order_ID = ? AND IsActive='Y' ORDER BY Value";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, ppOrderId);
			rs = pstmt.executeQuery();
			
			// 添加工序为空的选项
			nodeCombo.appendItem("请选择工序", null);
			
			boolean hasData = false;
			while (rs.next()) {
				int nodeId = rs.getInt("PP_Order_Node_ID");
				String nodeName = rs.getString("Name");
				if (nodeName == null) {
					nodeName = "Unnamed";
				}
				nodeCombo.appendItem(nodeName, nodeId);
				hasData = true;
			}
			
			// 默认选择第一个工序（如果有）
			if (hasData) {
				nodeCombo.setSelectedIndex(1); // 跳过"请选择工序"
				// 自动触发机台加载
				onNodeComboChanged();
			} else {
				nodeCombo.setSelectedIndex(0);
			}
			
		} catch (SQLException e) {
			log.severe("加载工序数据时出错: " + e.getMessage());
			nodeCombo.appendItem("加载工序失败", null);
			nodeCombo.setSelectedIndex(0);
		} finally {
			// 关闭资源
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}
	
	/**
	 * 工序下拉框变更监听方法
	 * 当工序选择变更时，自动带出机台数据
	 */
	private void onNodeComboChanged() {
		if (nodeCombo.getSelectedItem() == null) {
			// 如果工序为空，清空机台字段
//			resourceField.setValue(null);\
			resourceCombo.setSelectedIndex(0);
			executeQuery();
			return;
		}
		
		Object nodeObj = nodeCombo.getSelectedItem().getValue();
		if (nodeObj != null && nodeObj instanceof Integer) {
			Integer nodeId = (Integer) nodeObj;
			if (nodeId > 0) {
				// 根据工序ID获取机台信息 - 使用 DB.getSQLValue
				// String sql = "SELECT S_Resource_ID FROM AD_WF_Node WHERE AD_WF_Node_ID = ?";
				String sql = "SELECT S_Resource_ID FROM PP_Order_Node WHERE PP_Order_Node_ID = ?";
				int resourceId = DB.getSQLValue(null, sql, nodeId);
				
				if (resourceId > 0) {
					// 设置机台字段
//					resourceField.setValue(resourceId);
					setS_Resource_ID(resourceId);

					// 记录日志
					String nodeName = nodeCombo.getSelectedItem().getLabel();
					log.info("选择工序: " + nodeName + ", 自动带出机台ID: " + resourceId);
				} else {
					// 如果工序没有关联机台，清空机台字段
//					resourceField.setValue(null);
					resourceCombo.setSelectedIndex(0);
				}
			}
		}
		executeQuery();
	}
	
	public void enableToDeliver()
	{
		setToDeliver(true);
	}

	public void disableToDeliver()
	{
		setToDeliver(false);
	}
	
	private void setToDeliver(Boolean state)
	{
		toDeliverQty.getComponent().setEnabled(state); 
		scrapQtyLabel.setVisible(state);
		scrapQtyField.setVisible(state);
		rejectQtyLabel.setVisible(state);
		rejectQty.setVisible(state);
	}

	/**
	 * 查询并填充屏幕下半部分的表格
	 * 仅当 isBackflush() 或 isOnlyIssue 时运行
	 */
	public void executeQuery()
	{
		// 重置表格
		issue.clearTable();
		issue.setRowCount(0);
		// ↓↓↓ 新增：设置过滤条件 ↓↓↓
		String filterValue = null;
		if (fulfilledFilterCombo.getSelectedItem() != null) {
			filterValue = (String) fulfilledFilterCombo.getSelectedItem().getValue();
		}
		setFulfilledFilter(filterValue); // 传给基类
		// ↑↑↑ 新增结束 ↑↑↑
		
		// ↓ 新增：工序过滤
		setNodeFilter(getPP_Order_Node_ID());

		// 重新执行查询
		super.executeQuery(issue);

		// 新增：替代料规则应用
		this.applySubstituteRules();

		issue.repaint();
		
		// 确保表格正确显示
		if (issue.getRowCount() == 0) {
			// 如果没有数据，显示提示信息
			log.info("当前工单没有物料数据或已全部领料完成");
		}
	} //  executeQuery

	public void valueChange(ValueChangeEvent e) {
		String name = e.getPropertyName();
		Object value = e.getNewValue();

		if (value == null)
			return;

		// PP_Order_ID
		if (name.equals("PP_Order_ID")) {
			orderField.setValue(value);

			MPPOrder pp_order = getPP_Order();
			if (pp_order != null) {
				// 检查是否为委外工单
				boolean isSubcontractingOrder = false;
				MDocType docType = MDocType.get(Env.getCtx(), pp_order.getC_DocTypeTarget_ID());
				if (docType != null && docType.getName().contains("委外工单")) {
					isSubcontractingOrder = true;
				}

				// 先设置状态 - 关键修复
				if (isSubcontractingOrder) {
					setIsOnlyIssue(true);
					setIsBackflush(false);
					setIsOnlyReceipt(false);
				}

				// 清空并重新填充下拉选项
				pickcombo.getItems().clear();

				String defaultSelection = "";

				if (isSubcontractingOrder) {
					// 委外工单的选项
					setIsOnlyIssue(true); // 委外发料类似于OnlyIssue
					setIsBackflush(false);
					setIsOnlyReceipt(false);
					pickcombo.appendItem("委外发料", 4);
					pickcombo.appendItem("委外补领", 5);
					pickcombo.appendItem("委外退料", 6);
					defaultSelection = "委外发料";
				} else {
					// 普通生产工单的选项
					pickcombo.appendItem("生产领料", 1);
					pickcombo.appendItem("生产补领", 2);
					pickcombo.appendItem("生产退料", 3);
					defaultSelection = "生产领料";
				}

				// 通过标签名称设置默认选择，而不是使用索引
				for (int i = 0; i < pickcombo.getItemCount(); i++) {
					if (pickcombo.getItemAtIndex(i).getLabel().equals(defaultSelection)) {
						pickcombo.setSelectedIndex(i);
						break;
					}
				}

				// 改为获取工单的工艺路线，获取工单id
				int ppOrderId = pp_order.get_ID();

				// 加载工序下拉框数据
				loadNodeComboData(ppOrderId);
				loadResourceComboData();
				setS_Resource_ID(pp_order.getS_Resource_ID());
				setM_Warehouse_ID(pp_order.getM_Warehouse_ID());
				setDeliveredQty(pp_order.getQtyDelivered());
				setOrderedQty(pp_order.getQtyOrdered());
				setQtyBatchs(pp_order.getQtyBatchs());
				setQtyBatchSize(pp_order.getQtyBatchSize());
				setDeliveredQty(pp_order.getQtyOrdered());
				setToDeliverQty(getOpenQty());
				setM_Product_ID(pp_order.getM_Product_ID());
				MProduct m_product = MProduct.get(Env.getCtx(), pp_order.getM_Product_ID());
				setC_UOM_ID(m_product.getC_UOM_ID());
				setOrder_UOM_ID(pp_order.getC_UOM_ID());
				setM_AttributeSetInstance_ID(pp_order.getM_Product().getM_AttributeSetInstance_ID());

				// ✅ 工单变化后，重新加载部门下拉框（使用工单的组织）
				loadActivityComboData();

				// 触发下拉框变更事件
				Event ev = new Event(Events.ON_CHANGE, pickcombo);
				try {
					onEvent(ev);
				} catch (Exception e1) {
					throw new AdempiereException(e1);
				}

				// 清空已领数量和领取数量字段
				toDeliverQty.setValue(Env.ZERO);
				scrapQtyField.setValue(Env.ZERO);
				rejectQty.setValue(Env.ZERO);

				// 根据单据类型控制新增/删除物料按钮显隐
				updateMaterialButtonsVisibility();
			}
		} // PP_Order_ID

		if (name.equals(toDeliverQty.getColumnName()) || name.equals(scrapQtyField.getColumnName())) {
			if (getPP_Order_ID() > 0 && isBackflush()) {
				executeQuery();
			}
		}
	}
	
	public void showMessage(String message, boolean error)
	{
		try
		{
			if(!error)
				Messagebox.show(message, "Info",Messagebox.OK, Messagebox.INFORMATION);
			else
				Messagebox.show(message,"",Messagebox.OK,Messagebox.ERROR);
		}
		catch(Exception e)
		{
			
		}
	}

	/**
	 * 判断是否为"OnlyReciept"模式
	 * @return	
	 */
	protected boolean isOnlyReceipt() 
	{
		super.setIsOnlyReceipt("OnlyReceipt".equals(pickcombo.getText()));
		return super.isOnlyReceipt();
	}
	
	/**
	 * 判断是否为"OnlyIssue"模式
	 * @return	
	 */
	protected boolean isOnlyIssue() {  
	    String selectedType = pickcombo.getSelectedItem() != null ? pickcombo.getSelectedItem().getLabel() : "";  
	      
	    // 如果是委外工单，使用基类中已设置的值  
	    if (selectedType.contains("委外")) {  
	        return super.isOnlyIssue();  
	    }  
	      
	    // 普通工单：根据下拉框文本设置  
	    super.setIsOnlyIssue("生产领料".equals(selectedType));  
	    return super.isOnlyIssue();  
	}
	/**
	 * 判断是否为"isBackflush"模式
	 * @return	
	 */
	protected boolean isBackflush()
	{
		super.setIsBackflush("IsBackflush".equals(pickcombo.getText()));
		return super.isBackflush();
	}

	protected Timestamp getMovementDate()
	{
		return (Timestamp) movementDateField.getValue();
	}

	
	protected BigDecimal getOrderedQty()
	{
		BigDecimal bd = (BigDecimal) orderedQtyField.getValue();
		return bd != null ? bd : Env.ZERO;
	}

	protected void setOrderedQty(BigDecimal qty)
	{
		this.orderedQtyField.setValue(qty);
	}

	protected BigDecimal getDeliveredQty()
	{
		BigDecimal bd = (BigDecimal) deliveredQtyField.getValue();
		return bd != null ? bd : Env.ZERO;
	}
	
	protected void setDeliveredQty(BigDecimal qty)
	{
		deliveredQtyField.setValue(qty);
	}

	protected BigDecimal getToDeliverQty()
	{
		BigDecimal bd = (BigDecimal) toDeliverQty.getValue();
		return bd != null ? bd : Env.ZERO;
	}
	
	protected void setToDeliverQty(BigDecimal qty)
	{
		toDeliverQty.setValue(qty);
	}

	protected BigDecimal getScrapQty()
	{
		BigDecimal bd = (BigDecimal) scrapQtyField.getValue();
		return bd != null ? bd : Env.ZERO;
	}

	protected BigDecimal getRejectQty() 
	{
		BigDecimal bd = (BigDecimal) rejectQty.getValue();
		return bd != null ? bd : Env.ZERO;
	}

	protected BigDecimal getOpenQty()
	{
		BigDecimal bd = (BigDecimal) openQtyField.getValue();
		return bd != null ? bd : Env.ZERO;
	}
	protected void setOpenQty(BigDecimal qty)
	{
		openQtyField.setValue(qty);
	}
	
	protected BigDecimal getQtyBatchs()
	{
		BigDecimal bd = (BigDecimal) qtyBatchsField.getValue();
		return bd != null ? bd : Env.ZERO;
	}
	protected void setQtyBatchs(BigDecimal qty)
	{
		qtyBatchsField.setValue(qty);
	}
	
	protected BigDecimal getQtyBatchSize()
	{
		BigDecimal bd = (BigDecimal) qtyBatchSizeField.getValue();
		return bd != null ? bd : Env.ZERO;
	}
	
	protected void setQtyBatchSize(BigDecimal qty)
	{
		qtyBatchSizeField.setValue(qty);
	}

	protected int getM_AttributeSetInstance_ID()
	{
		Integer ii = (Integer) attribute.getValue();
		return ii != null ? ii.intValue() : 0;
	}
	
	protected void setM_AttributeSetInstance_ID(int M_AttributeSetInstance_ID)
	{
		attribute.setValue(M_AttributeSetInstance_ID);
	}

	protected int getM_Locator_ID()
	{
		Integer ii = (Integer) locatorField.getValue();
		return ii != null ? ii.intValue() : 0;
	}
	
	protected void setM_Locator_ID(int M_Locator_ID)
	{
		locatorField.setValue(M_Locator_ID);
	}

	protected int getPP_Order_ID()
	{
		Integer ii = (Integer) orderField.getValue();
		return ii != null ? ii.intValue() : 0;
	}	
	
	protected MPPOrder getPP_Order()
	{
		int id = getPP_Order_ID();
		if (id <= 0)
		{
			m_PP_order = null;
			return null;
		}
		if (m_PP_order == null || m_PP_order.get_ID() != id)
		{
			
			m_PP_order = new MPPOrder(Env.getCtx(), id, null);
		}
		return m_PP_order;
	}
	
	
	
	protected void setS_Resource_ID(int S_Resource_ID)
	{
//		resourceField.setValue(S_Resource_ID);
		for (int i = 0; i < resourceCombo.getItemCount(); i++) {
			org.zkoss.zul.Comboitem item = resourceCombo.getItemAtIndex(i);
			Object itemValue = item.getValue();
			if (itemValue != null && itemValue instanceof Integer) {
				if (((Integer) itemValue).intValue() == S_Resource_ID) {
					resourceCombo.setSelectedIndex(i);
					break;
				}
			}
		}
	}
	
	protected int getM_Warehouse_ID()
	{
		Integer ii = (Integer) warehouseField.getValue();
		return ii != null ? ii.intValue() : 0;
	}
	
	protected void setM_Warehouse_ID(int M_Warehouse_ID)
	{
		warehouseField.setValue(M_Warehouse_ID);
		// 设置上下文
		Env.setContext(Env.getCtx(), m_WindowNo, "M_Warehouse_ID", M_Warehouse_ID);
	}
	
	protected int getM_Product_ID()
	{
		Integer ii = (Integer) productField.getValue();
		return ii != null ? ii.intValue() : 0;
	}
	
	protected void setM_Product_ID(int M_Product_ID)
	{
		productField.setValue(M_Product_ID);
		// Env.setContext(Env.getCtx(), m_WindowNo, "M_Product_ID", M_Product_ID);
	}
	
	protected int getC_UOM_ID()
	{
		Integer ii = (Integer) uomField.getValue();
		return ii != null ? ii.intValue() : 0;
	}
	
	protected void setC_UOM_ID(int C_UOM_ID)
	{
		uomField.setValue(C_UOM_ID);
	}
	
	protected int getOrder_UOM_ID()
	{
		Integer ii = (Integer) uomorderField.getValue();
		return ii != null ? ii.intValue() : 0;
	}
	
	protected void setOrder_UOM_ID(int C_UOM_ID)
	{
		uomorderField.setValue(C_UOM_ID);
	}
	

	public void dispose()
	{
		SessionManager.getAppDesktop().closeActiveWindow();
	}	//	dispose
	
	public ADForm getForm() 
	{
		return form;
	}

	
	
	
	public boolean cmd_process(final boolean isCloseDocument, final IMiniTable issue)
	{
		 lastProcessInfo = null; // 每次执行前重置  
	    if (isOnlyReceipt() || isBackflush() || isProductionReturn()) 
	    {
	        if (getM_Locator_ID() <= 0)
	        {
	            showMessage( Msg.getMsg(Env.getCtx(),"NoLocator"), false);
	            return false;
	        }
	    }
	    if (getPP_Order() == null || getMovementDate() == null)
	    {
	        return false;
	    }    
	    
	    // 获取工序和机台ID
		int nodeId = getPP_Order_Node_ID();
	    int resourceId = getS_Resource_ID();
	    
	    // 记录选择的工序和机台
	    if (nodeId > 0) {
	        log.info("选择的工序ID: " + nodeId);
	    }
	    if (resourceId > 0) {
	        log.info("选择的机台ID: " + resourceId);
	    }
	    
	    try
	    {
	        Trx.run(new TrxRunnable() {
	            public void run(String trxName)
	            {
	                MPPOrder order = new MPPOrder(Env.getCtx(), getPP_Order_ID(), trxName);
	                if (isOnlyIssue() || isProductionReplenishment() || isProductionReturn()) 
	                {
	                    // 根据不同类型创建不同单据
	                    String costCollectorType = null;
						if (isSubcontractingIssue()) {
							costCollectorType = OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE; // 130
						} else if (isSubcontractingReplenishment()) {
							costCollectorType = OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT; // 136
						} else if (isSubcontractingReturn()) {
							costCollectorType = OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN; // 135
						} else if (isProductionReplenishment()) {
							costCollectorType = OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_REPLENISHMENT; // 116
						} else if (isProductionReturn()) {
							costCollectorType = OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_RETURN; // 115
						} else if (isOnlyIssue()) {
							costCollectorType = OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_ISSUE; // 110
						}
	                    
	                    // 创建发料单，传递工序和机台ID
	                    createIssue(order, issue, costCollectorType);
	                }
	                if (isOnlyReceipt() || isBackflush()) 
	                {
	                    MPPOrder.createReceipt(order,
	                            getMovementDate(),
	                            getDeliveredQty(),
	                            getToDeliverQty(), 
	                            getScrapQty(),
	                            getRejectQty(),
	                            getM_Locator_ID(),
	                            getM_AttributeSetInstance_ID()
	                    );
	                    if (isCloseDocument)
	                    {
	                        order.setDateFinish(getMovementDate());
	                        order.closeIt();
	                        order.saveEx();
	                    }
	                }
	            }});
	    }
	    catch (Exception e)
	    {
	        showMessage(e.getLocalizedMessage(), true);
	        return false;
	    }
	    finally
	    {
	        m_PP_order = null;
	    }

	    return true;
	}
	
	protected boolean isProductionReplenishment() 
	{
	    super.setIsProductionReplenishment("生产补领".equals(pickcombo.getText()));
	    return super.isProductionReplenishment();
	}

	protected boolean isProductionReturn() 
	{
	    super.setIsProductionReturn("生产退料".equals(pickcombo.getText()));
	    return super.isProductionReturn();
	}
	
	/**
	 * 获取成本归集类型
	 * @return 成本归集类型常量
	 */
	public String getCostCollectorType() {
		if (isSubcontractingIssue()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE; // 130
		} else if (isSubcontractingReplenishment()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT; // 136
		} else if (isSubcontractingReturn()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN; // 135
		} else if (isProductionReplenishment()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_REPLENISHMENT; // 116
		} else if (isProductionReturn()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_RETURN; // 115
		} else if (isOnlyIssue()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_ISSUE; // 110
		} else if (isOnlyReceipt()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_RECEIPT; // 100
		} else if (isBackflush()) {
			return OrderReceiptIssue.COSTCOLLECTORTYPE_MIX; // 120
		}
		return null;
	}
	
	// 在 WOrderReceiptIssue 类中添加这个方法
	public void setupTableListeners(final IMiniTable issue) {
	    try {
	        // 尝试使用 Swing 的方式获取表格模型
	        // 首先尝试将 issue 转换为 JTable 或获取其 TableModel
	        java.awt.Component component = (java.awt.Component) issue;
	        
	        if (component instanceof javax.swing.JTable) {
	            javax.swing.JTable table = (javax.swing.JTable) component;
	            
	            // 获取表格模型并添加监听器
	            table.getModel().addTableModelListener(new javax.swing.event.TableModelListener() {
	                @Override
	                public void tableChanged(javax.swing.event.TableModelEvent e) {
	                    if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
	                        int row = e.getFirstRow();
	                        int col = e.getColumn();
	                        
	                        // 检查是否是已领数量列（第6列）
	                        if (col == 6) {
	                            try {
	                                BigDecimal newDeliveredQty = (BigDecimal) issue.getValueAt(row, col);
	                                if (newDeliveredQty == null) {
	                                    newDeliveredQty = Env.ZERO;
	                                }
	                                
	                                BigDecimal requiredQty = (BigDecimal) issue.getValueAt(row, 5);
	                                if (requiredQty == null) {
	                                    requiredQty = Env.ZERO;
	                                }
	                                
	                                // 计算新的领取数量 = 需求数量 - 新的已领数量
	                                BigDecimal newToDeliverQty = requiredQty.subtract(newDeliveredQty);
	                                if (newToDeliverQty.compareTo(BigDecimal.ZERO) < 0) {
	                                    newToDeliverQty = BigDecimal.ZERO;
	                                }
	                                
	                                // 更新领取数量列（第7列）
	                                issue.setValueAt(newToDeliverQty, row, 7);
	                            } catch (Exception ex) {
	                                log.severe("表格监听器出错: " + ex.getMessage());
	                            }
	                        }
	                    }
	                }
	            });
	        } else {
	            log.severe("无法获取表格模型，issue 不是 JTable 类型");
	            // 尝试其他方式
	        }
	    } catch (Exception e) {
	        log.severe("设置表格监听器失败: " + e.getMessage());
	    }
	}
	/**
	 * 获取工序ID
	 * 从工序下拉框获取选择的工序ID
	 * 这个值会被传递到生成的生产发料单的"工单工序"字段
	 * 
	 * @return 工序ID (PP_Order_Node_ID)
	 */
	protected int getPP_Order_Node_ID() {
	    if (nodeCombo.getSelectedItem() != null && nodeCombo.getSelectedItem().getValue() != null) {
	        Object value = nodeCombo.getSelectedItem().getValue();
	        if (value instanceof Integer) {
	            return (Integer) value;
	        } else if (value instanceof String) {
	            try {
	                return Integer.parseInt((String) value);
	            } catch (NumberFormatException e) {
	                log.severe("工序ID格式错误: " + value);
	                return 0;
	            }
	        }
	    }
	    return 0;
	}

	/**
	 * 获取机台ID
	 * 从机台字段获取机台ID
	 * 这个值会被传递到生成的生产发料单的"资源"字段
	 * 
	 * @return 机台ID (S_Resource_ID)
	 */
	protected int getS_Resource_ID() {
//	    Integer ii = (Integer) resourceField.getValue();
//	    if (ii != null) {
//	        return ii.intValue();
//	    }
		if (resourceCombo.getSelectedItem() != null && resourceCombo.getSelectedItem().getValue() != null) {
			Object value = resourceCombo.getSelectedItem().getValue();
			if (value instanceof Integer) {
				return (Integer) value;
			}
		}
	    return 0;
	}

	private void validateAndAdjustQuantity(int row, int column, BigDecimal inputValue) {

		// 读取系统配置
		boolean restrictOverIssue = MSysConfig.getBooleanValue(OrderReceiptIssue.SYSCONFIG_RESTRICT_OVER_ISSUE, true,
				Env.getAD_Client_ID(Env.getCtx()));

		try {
			// 获取产品ID
			KeyNamePair productKey = (KeyNamePair) issue.getValueAt(row, 2);
			int productId = productKey.getKey();

			// 获取包装数量
			BigDecimal unitsPerPack = getUnitsPerPack(productId);

			// 获取需求数量和已领数量
			BigDecimal requiredQty = convertToBigDecimal(issue.getValueAt(row, 5));
			BigDecimal deliveredQty = convertToBigDecimal(issue.getValueAt(row, 6));



			// 计算向上取整的最大值
			BigDecimal maxQtyWithPack = calculateMaxQtyWithPack(requiredQty, unitsPerPack);

			// 获取领退类型
			String selectedType = pickcombo.getSelectedItem() != null ? pickcombo.getSelectedItem().getLabel() : "";

			BigDecimal maxQty = BigDecimal.ZERO;

			if ("生产领料".equals(selectedType)) {
				// 领料：最大值 = min(向上取整的最大值 - 已领数量, 库存数量)
				BigDecimal n = calculateIssueQty(requiredQty, deliveredQty, unitsPerPack);
				// 新上限 = min(N + 最小包装数量, 库存数量)
				BigDecimal safeUnitsPerPack = (unitsPerPack == null || unitsPerPack.compareTo(Env.ZERO) <= 0)
						? BigDecimal.ONE
						: unitsPerPack;
				BigDecimal nPlusOnePack = n.add(safeUnitsPerPack);

				// 检查库存数量 - 内联获取逻辑
				BigDecimal stockQty = Env.ZERO;
				try {
					Object stockObj = issue.getValueAt(row, 8);
					log.info("第" + row + "行库存数量: " + stockObj);
					if (stockObj instanceof BigDecimal) {
						stockQty = (BigDecimal) stockObj;
					} else if (stockObj != null) {
						stockQty = new BigDecimal(stockObj.toString());
					}
				} catch (Exception e) {
					log.warning("获取库存数量出错: " + e.getMessage());
				}
				maxQty = nPlusOnePack.compareTo(stockQty) > 0 ? stockQty : nPlusOnePack;
				// 新下限：输入值必须 > 0
				if (inputValue.compareTo(Env.ZERO) <= 0) {
					BigDecimal restoreValue = n.compareTo(stockQty) > 0 ? stockQty : n;
					isRestoringValue = true;
					issue.setValueAt(restoreValue, row, column);
					isRestoringValue = false;
					log.info("领退数量必须大于0，已恢复为: " + restoreValue);
					return;
				}
			} else if ("生产退料".equals(selectedType)) {
				// 退料：最大值 = min(已领数量, 向上取整的最大值)
				maxQty = deliveredQty;

			} else if ("生产补领".equals(selectedType)) {
				// 补领：最大值 = min(向上取整的最大值 - 已领数量, 库存数量)
				BigDecimal calculatedQty = calculateIssueQty(requiredQty, deliveredQty, unitsPerPack);
				BigDecimal stockQty = convertToBigDecimal(issue.getValueAt(row, 8));
				maxQty = stockQty;
			}
			else if ("委外发料".equals(selectedType)) {
				// 委外发料：考虑包装规格的最大值
				BigDecimal remainingQty = requiredQty.subtract(deliveredQty);
				if (remainingQty.compareTo(Env.ZERO) > 0) {
					maxQty = calculateMaxQtyWithPack(remainingQty, unitsPerPack);
				} else {
					maxQty = Env.ZERO;
				}
			} else if ("委外补领".equals(selectedType)) {
				// 委外补领：考虑包装规格的最大值
				BigDecimal stockQty = convertToBigDecimal(issue.getValueAt(row, 8));
				maxQty = stockQty;
			} else if ("委外退料".equals(selectedType)) {
				// 退料：最大值 = min(已领数量, 向上取整的最大值)
				maxQty = deliveredQty;

			}
			// 修改点：退料始终限制；领料/补领根据配置参数决定
			boolean isReturnType = "生产退料".equals(selectedType) || "委外退料".equals(selectedType);
			boolean shouldRestrict = isReturnType || restrictOverIssue;

			if (shouldRestrict && inputValue.compareTo(maxQty) > 0) {
				isRestoringValue = true;
				issue.setValueAt(maxQty, row, column);
				isRestoringValue = false;

				String materialName = productKey.getName();
				log.info("物料[" + materialName + "]的领取数量已自动调整为最大值: " + maxQty);
			}

		} catch (Exception ex) {
			log.severe("校验数量时出错: " + ex.getMessage());
		}
	}

	/**
	 * 获取产品的包装数量
	 * 
	 * @param productId 产品ID
	 * @return 包装数量
	 */
	private BigDecimal getUnitsPerPack(int productId) {
		String sql = "SELECT UnitsPerPack FROM M_Product WHERE M_Product_ID = ?";
		return DB.getSQLValueBD(null, sql, productId);
	}

	protected boolean isSubcontractingIssue() {
		super.setIsSubcontracting("委外发料".equals(pickcombo.getText()));
		return super.isSubcontracting();
	}

	protected boolean isSubcontractingReplenishment() {
		return "委外补领".equals(pickcombo.getText());
	}

	protected boolean isSubcontractingReturn() {
		return "委外退料".equals(pickcombo.getText());
	}

	private void loadActivityComboData() {
		activityCombo.getItems().clear();
		activityCombo.appendItem("请选择部门", null);


		int clientId = Env.getAD_Client_ID(Env.getCtx());
		// 从当前选中的 PP_Order 获取 AD_Org_ID
		MPPOrder ppOrder = getPP_Order();
		int orgId = (ppOrder != null) ? ppOrder.getAD_Org_ID() : Env.getAD_Org_ID(Env.getCtx());
		String sql = "SELECT C_Activity_ID, Name FROM C_Activity " + "WHERE IsActive='Y' " + "AND AD_Client_ID = ? "
				+ "AND (AD_Org_ID = ? OR AD_Org_ID = 0) " + "ORDER BY Name";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, clientId);
			pstmt.setInt(2, orgId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				activityCombo.appendItem(rs.getString("Name"), rs.getInt("C_Activity_ID"));
			}
			activityCombo.setSelectedIndex(0); // 默认空
		} catch (SQLException e) {
			log.severe("加载活动数据时出错: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	protected int getC_Activity_ID() {
		if (activityCombo.getSelectedItem() != null && activityCombo.getSelectedItem().getValue() != null) {
			Object value = activityCombo.getSelectedItem().getValue();
			if (value instanceof Integer) {
				return (Integer) value;
			}
		}
		return 0;
	}

	private void onAddMaterial() {
		if (getPP_Order_ID() <= 0) {
			Messagebox.show("请先选择生产工单", "提示", Messagebox.OK, Messagebox.INFORMATION);
			return;
		}

		// 已选物料数据：[productId(Integer), productValue(String), productName(String),
		// uomName(String), qty(BigDecimal)]
		final List<Object[]> selectedProducts = new ArrayList<Object[]>();

		final Window outerDialog = new Window();
		outerDialog.setTitle("新增物料");
		outerDialog.setWidth("720px");
		outerDialog.setHeight("480px");
		outerDialog.setBorder("normal");
		outerDialog.setClosable(true);
		outerDialog.setSizable(true);

		// 顶部工具栏：+ 和 - 按钮（靠右）
		Hbox toolbar = new Hbox();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:4px; text-align:right; background:#f5f5f5; border-bottom:1px solid #ddd");
		toolbar.setPack("end");
		final Button addBtn = new Button("+");
		final Button removeBtn = new Button("-");
		addBtn.setTooltiptext("从物料列表中选择物料");
		removeBtn.setTooltiptext("删除选中的物料");
		toolbar.appendChild(addBtn);
		toolbar.appendChild(removeBtn);

		// 外层物料列表
		final WListbox outerTable = new WListbox();
		outerTable.setWidth("100%");
		outerTable.setHeight("330px");
		outerTable.setMultiSelection(true);
		outerTable.setMultiple(true); // 开启多选
		// outerTable.setCheckmark(true); // 显示复选框（可选，但更直观）

		// 初始化空表格
		refreshOuterTable(outerTable, selectedProducts);

		// 底部按钮
		Hbox bottomBar = new Hbox();
		bottomBar.setWidth("100%");
		bottomBar.setStyle("padding:5px; text-align:right");
		bottomBar.setPack("end");
		Button cancelBtn = new Button("取消");
		Button confirmBtn = new Button("确定");
		bottomBar.appendChild(cancelBtn);
		bottomBar.appendChild(confirmBtn);

		// 整体布局
		org.zkoss.zul.Vlayout vlayout = new org.zkoss.zul.Vlayout();
		vlayout.setWidth("100%");
		vlayout.appendChild(toolbar);
		vlayout.appendChild(outerTable);
		vlayout.appendChild(bottomBar);
		outerDialog.appendChild(vlayout);

		// + 按钮：直接打开物料信息窗口（InfoPanel），选完自动填入外层列表
		addBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				// 设置仓库上下文，InfoProductPanel 需要用来显示库存
				Env.setContext(Env.getCtx(), m_WindowNo, "M_Warehouse_ID", getM_Warehouse_ID());

				MLookup productLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0,
						MColumn.getColumn_ID(MProduct.Table_Name, MProduct.COLUMNNAME_M_Product_ID),
						DisplayType.ChosenMultipleSelectionSearch);

				final org.adempiere.webui.panel.InfoPanel ip = org.adempiere.webui.factory.InfoManager
						.create(productLookup, null, "M_Product", "M_Product_ID", null, true, "");
				if (ip == null)
					return;

				ip.setVisible(true);
				ip.setClosable(true);
				ip.addEventListener(org.adempiere.webui.event.DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {
					public void onEvent(Event event) throws Exception {
						if (ip.isCancelled())
							return;
						Object[] keys = ip.getSelectedKeys();
						if (keys == null || keys.length == 0)
							return;

						java.util.Set<Integer> existingIds = new java.util.HashSet<Integer>();
						for (Object[] p : selectedProducts) {
							existingIds.add((Integer) p[0]);
						}

						int added = 0;
						for (Object key : keys) {
							if (key == null)
								continue;
							int productId;
							try {
								productId = Integer.parseInt(key.toString());
							} catch (NumberFormatException ex) {
								continue;
							}
							if (existingIds.contains(productId))
								continue;

							MProduct product = MProduct.get(Env.getCtx(), productId);
							if (product == null)
								continue;
							org.compiere.model.MUOM uom = org.compiere.model.MUOM.get(Env.getCtx(),
									product.getC_UOM_ID());
							String uomName = uom != null ? uom.getName() : "";

							selectedProducts.add(new Object[] { productId, product.getValue(), product.getName(),
									uomName, BigDecimal.ONE });
							existingIds.add(productId);
							added++;
						}

						if (added > 0) {
							refreshOuterTable(outerTable, selectedProducts);
						}
					}
				});
				ip.setId("InfoProduct_" + m_WindowNo);
				org.adempiere.webui.apps.AEnv.showWindow(ip);
			}
		});

		// - 按钮：删除勾选行（从后往前删，避免索引错位）
		removeBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				List<Integer> toRemove = new ArrayList<Integer>();
				for (int i = 0; i < outerTable.getRowCount(); i++) {
					IDColumn idCol = (IDColumn) outerTable.getValueAt(i, 0);
					if (idCol != null && idCol.isSelected()) {
						toRemove.add(0, i); // 倒序插入
					}
				}
				if (toRemove.isEmpty()) {
					Messagebox.show("请先勾选要删除的物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
					return;
				}
				for (int idx : toRemove) {
					selectedProducts.remove(idx);
				}
				refreshOuterTable(outerTable, selectedProducts);
			}
		});

		// 取消
		cancelBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				outerDialog.detach();
			}
		});

		// 确定：读取表格中最新的领用数量，批量添加到BOM
		confirmBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				if (outerTable.getRowCount() == 0) {
					Messagebox.show("请先添加物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
					return;
				}
				final List<Object[]> finalProducts = new ArrayList<Object[]>();
				for (int i = 0; i < outerTable.getRowCount(); i++) {
					IDColumn idCol = (IDColumn) outerTable.getValueAt(i, 0);
					if (idCol == null)
						continue;
					int productId = idCol.getRecord_ID();
					Object qtyObj = outerTable.getValueAt(i, 5);
					BigDecimal qty = BigDecimal.ONE;
					if (qtyObj instanceof BigDecimal) {
						qty = (BigDecimal) qtyObj;
					} else if (qtyObj != null) {
						try {
							qty = new BigDecimal(qtyObj.toString());
						} catch (Exception ex) {
							/* ignore */
						}
					}
					if (qty.compareTo(Env.ZERO) <= 0)
						qty = BigDecimal.ONE;
					finalProducts.add(new Object[] { productId, qty });
				}
				outerDialog.detach();
				addProductsToOrderBOM(finalProducts);
			}
		});

		outerDialog.setPage(form.getPage());
		outerDialog.doModal();
	}

	private void refreshOuterTable(WListbox outerTable, List<Object[]> selectedProducts) {
		Vector<String> colNames = new Vector<String>();
		colNames.add(" ");
		colNames.add("序号");
		colNames.add("物料编码");
		colNames.add("物料名称");
		colNames.add("单位");
		colNames.add("领用数量");

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		for (int i = 0; i < selectedProducts.size(); i++) {
			Object[] p = selectedProducts.get(i);
			Vector<Object> row = new Vector<Object>();
			IDColumn idCol = new IDColumn((Integer) p[0]);
			idCol.setSelected(false);
			row.add(idCol);
			row.add(i + 1);
			row.add(p[1]);
			row.add(p[2]);
			row.add(p[3]);
			row.add(p[4]);
			data.add(row);
		}

		ListModelTable model = new ListModelTable(data);
		outerTable.setData(model, colNames);
		outerTable.setColumnClass(0, IDColumn.class, false, " ");
		outerTable.setColumnClass(1, Integer.class, true, "序号");
		outerTable.setColumnClass(2, String.class, true, "物料编码");
		outerTable.setColumnClass(3, String.class, true, "物料名称");
		outerTable.setColumnClass(4, String.class, true, "单位");
		outerTable.setColumnClass(5, BigDecimal.class, false, "领用数量");
		outerTable.autoSize();

		// ★ 关键：setData 内部 setModel 会重置 multiple，必须在 setData 之后重新设置
		outerTable.setMultiple(true);
	}


	

	private void addProductsToOrderBOM(final List<Object[]> products) {
		try {
			final MPPOrder order = getPP_Order();
			if (order == null)
				return;

			final MPPOrderBOM orderBOM = new Query(Env.getCtx(), MPPOrderBOM.Table_Name, "PP_Order_ID=?", null)
					.setParameters(order.get_ID()).first();
			if (orderBOM == null) {
				Messagebox.show("未找到工单对应的BOM，无法新增物料", "错误", Messagebox.OK, Messagebox.ERROR);
				return;
			}

			final List<String> skipped = new ArrayList<String>();
			final List<String> added = new ArrayList<String>();

			Trx.run(new TrxRunnable() {
				public void run(String trxName) {
					for (Object[] p : products) {
						int productId = (Integer) p[0];
						BigDecimal qty = (BigDecimal) p[1];

						// 防止重复添加
						MPPOrderBOMLine existing = MPPOrderBOMLine.forM_Product_ID(Env.getCtx(), order.get_ID(),
								productId, trxName);
						if (existing != null) {
							MProduct prod = MProduct.get(Env.getCtx(), productId);
							skipped.add(prod != null ? prod.getValue() : String.valueOf(productId));
							continue;
						}

						MProduct product = MProduct.get(Env.getCtx(), productId);
						if (product == null)
							continue;

						MPPOrderBOMLine newLine = new MPPOrderBOMLine(Env.getCtx(), 0, trxName);
						newLine.setAD_Org_ID(order.getAD_Org_ID());
						newLine.setPP_Order_ID(order.get_ID());
						newLine.setPP_Order_BOM_ID(orderBOM.get_ID());
						newLine.setM_Product_ID(productId);
						newLine.setC_UOM_ID(product.getC_UOM_ID());
						newLine.setM_Warehouse_ID(order.getM_Warehouse_ID());
						newLine.setQtyBatch(Env.ZERO);
						newLine.setIsQtyPercentage(false);
						newLine.setQtyBOM(qty);
						newLine.setComponentType(MPPOrderBOMLine.COMPONENTTYPE_Component);
						newLine.setQtyPlusScrap(order.getQtyOrdered().multiply(qty));
						newLine.setValidFrom(new Timestamp(System.currentTimeMillis()));
						newLine.saveEx(trxName);

						added.add(product.getValue());
					}
				}
			});

			StringBuilder msg = new StringBuilder();
			if (!added.isEmpty()) {
				msg.append("已成功添加 ").append(added.size()).append(" 个物料：").append(String.join(", ", added));
			}
			if (!skipped.isEmpty()) {
				if (msg.length() > 0)
					msg.append("\n");
				msg.append("以下物料已存在，跳过：").append(String.join(", ", skipped));
			}
			if (msg.length() == 0)
				msg.append("无物料被添加");

			Messagebox.show(msg.toString(), "结果", Messagebox.OK, Messagebox.INFORMATION);
			executeQuery(); // 刷新主表格

		} catch (Exception ex) {
			log.severe("批量新增物料到工单BOM失败: " + ex.getMessage());
			Messagebox.show("新增物料失败: " + ex.getMessage(), "错误", Messagebox.OK, Messagebox.ERROR);
		}
	}

	private void deleteProductFromOrderBOM(List<Integer> ppOrderBOMLineIds) {
		try {
			MPPOrder order = getPP_Order();
			if (order == null)
				return;

			Trx.run(new TrxRunnable() {
				public void run(String trxName) {
					for (int id : ppOrderBOMLineIds) {
						MPPOrderBOMLine line = new MPPOrderBOMLine(Env.getCtx(), id, trxName);
						if (line.get_ID() > 0) {
							line.deleteEx(false);
						}
					}
				}
			});

			Messagebox.show("物料已成功从工单BOM中删除", "成功", Messagebox.OK, Messagebox.INFORMATION);
			executeQuery(); // 刷新领退料窗口的物料列表

		} catch (Exception ex) {
			log.severe("删除工单BOM物料失败: " + ex.getMessage());
			Messagebox.show("删除物料失败: " + ex.getMessage(), "错误", Messagebox.OK, Messagebox.ERROR);
		}
	}

	
	// ==================== 替代料相关方法 ====================
	
	/**
	 * 对表格中每行物料应用替代料规则： - 查询该主料的有效替代规则（SubstituteStatus='A'=已审批，在有效期内） -
	 * 根据替代方式（S=替代料优先/M=主料优先）决定是否替换 - 按优先级遍历替代料，过滤省份限制，找第一条有库存的替代料 -
	 * 修改表格中的物料和库存相关数量
	 */
	private void applySubstituteRules() {
		int warehouseId = getM_Warehouse_ID();
		if (warehouseId <= 0)
			return;

		Timestamp today = new Timestamp(System.currentTimeMillis());
		int regionId = getRegionByWarehouse(warehouseId);

		isRestoringValue = true;
		try {
			for (int row = 0; row < issue.getRowCount(); row++) {
				Object productObj = issue.getValueAt(row, 2);
				if (!(productObj instanceof KeyNamePair))
					continue;
				KeyNamePair productKey = (KeyNamePair) productObj;
				int mainProductId = productKey.getKey();
				if (mainProductId <= 0)
					continue;

				SubstituteResult result = resolveSubstitute(mainProductId, warehouseId, regionId, today);
				if (result == null)
					continue; // 1.无替代料，2.主料优先且主料有库存，3.替代料优先 但是替代料无库存或被省份限制日期范围限制，以上三种情况不替代

				MProduct subProduct = MProduct.get(Env.getCtx(), result.substituteProductId);
				MProduct mainProduct = MProduct.get(Env.getCtx(), mainProductId);

				// 列1：物料编码
				issue.setValueAt(subProduct.getValue(), row, 1);

				// 列2：产品 KeyNamePair（createIssue 从此列读取产品ID）
				issue.setValueAt(new KeyNamePair(result.substituteProductId, subProduct.getName()), row, 2);

				// 列3：单位（替代料单位可能与主料不同）
				int subUomId = subProduct.getC_UOM_ID();
				MUOM subUom = MUOM.get(Env.getCtx(), subUomId);
				String uomName = subUom != null ? subUom.getName() : "";
				issue.setValueAt(new KeyNamePair(subUomId, uomName), row, 3);

				// 列4：批次/ASI 清空（替代料批次与主料不同，让用户在发料时重新选择）
				issue.setValueAt(null, row, 4);

				// 列5：需求数量（替代比例≠1时按比例调整）
				BigDecimal origRequired = convertToBigDecimal(issue.getValueAt(row, 5));
				BigDecimal newRequired = origRequired.multiply(result.ratio);
				issue.setValueAt(newRequired, row, 5);

				// 列6：已领数量 — 不改（属于BOM行的历史记录）

				// 列7：领退数量（替代比例≠1时按比例调整）
				BigDecimal origToDeliver = convertToBigDecimal(issue.getValueAt(row, 7));
				BigDecimal newToDeliver = origToDeliver.multiply(result.ratio);
				issue.setValueAt(newToDeliver, row, 7);

				// 列8：库存数量（替代料在当前仓库的库存）
				BigDecimal subOnHand = MStorageOnHand.getQtyOnHand(result.substituteProductId, warehouseId, 0, null);
				issue.setValueAt(subOnHand, row, 8);

				// 列9：预留数量（替代料在当前仓库的预留数量）
				// isSOTrx=false 表示查询采购/生产侧预留
				MStorageReservation reservation = MStorageReservation.get(Env.getCtx(), warehouseId,
						result.substituteProductId, 0, false, null);
				BigDecimal subReserved = (reservation != null) ? reservation.getQty() : BigDecimal.ZERO;
				if (subReserved == null)
					subReserved = BigDecimal.ZERO;
				issue.setValueAt(subReserved, row, 9);

				// 列10：可用数量 = 库存 - 预留
				BigDecimal subAvailable = subOnHand.subtract(subReserved);
				issue.setValueAt(subAvailable, row, 10);

				// 列11：仓库 — 不改（仓库不变）

				// 列12：BOM数量 — 不改（BOM定义的数量不变）

				// 列13：主料信息（新增列，显示被替代的主料）
				issue.setValueAt(mainProduct.getValue() + " - " + mainProduct.getName(), row, 13);

				log.info("替代料应用: 主料[" + mainProduct.getValue() + "] → 替代料[" + subProduct.getValue() + "] 比例="
						+ result.ratio);
			}
		} finally {
			isRestoringValue = false;
		}
	}

	/**
	 * 替代料解析核心逻辑
	 * 
	 * @return SubstituteResult（找到替代料时），null（使用主料时）
	 */
	private SubstituteResult resolveSubstitute(int mainProductId, int warehouseId, int regionId, Timestamp today) {

		// 先按优先级，再按最早入库日期 FIFO
		String sql = "SELECT s.Substitute_ID, s.SubstituteRatio, earliest.EarliestDate FROM M_Substitute s "
				+ "LEFT JOIN ( " + "    SELECT oh.M_Product_ID, MIN(oh.DateMaterialPolicy) AS EarliestDate "
				+ "    FROM M_StorageOnHand oh  JOIN M_Locator loc ON oh.M_Locator_ID = loc.M_Locator_ID "
				+ "    WHERE loc.M_Warehouse_ID = ?   AND oh.QtyOnHand > 0  GROUP BY oh.M_Product_ID "
				+ ") earliest ON earliest.M_Product_ID = s.Substitute_ID WHERE s.M_Product_ID=? "
				+ "  AND s.SubstituteStatus='A'  AND (s.ValidFrom IS NULL OR s.ValidFrom <= ?) "
				+ "  AND (s.ValidTo IS NULL OR s.ValidTo > ?)  AND s.IsActive='Y'  AND s.AD_Client_ID=? "
				+ "ORDER BY s.Priority NULLS LAST, earliest.EarliestDate ASC NULLS LAST";

		List<Object[]> substitutes = new ArrayList<>();
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, warehouseId); // LEFT JOIN 子查询中的 M_Warehouse_ID
			pstmt.setInt(2, mainProductId); // WHERE s.M_Product_ID=?
			pstmt.setTimestamp(3, today); // ValidFrom <= ?
			pstmt.setTimestamp(4, today); // ValidTo > ?
			pstmt.setInt(5, Env.getAD_Client_ID(Env.getCtx())); // AD_Client_ID
			rs = pstmt.executeQuery();
			while (rs.next()) {
				substitutes.add(new Object[] { rs.getInt(1), // Substitute_ID
						rs.getBigDecimal(2) // SubstituteRatio
				});
			}
		} catch (Exception e) {
			log.severe("查询替代料失败: " + e.getMessage());
			return null;
		} finally {
			DB.close(rs, pstmt);
		}

		if (substitutes.isEmpty())
			return null;

		// 获取替代方式
		MProduct mainProduct = MProduct.get(Env.getCtx(), mainProductId);
		String substituteType = (String) mainProduct.get_Value("SubstituteType");
		if (substituteType == null || substituteType.isEmpty())
			substituteType = "S"; // 默认替代料优先

		// 主料优先：主料有库存则直接使用主料，不替换
		if ("M".equals(substituteType)) {
			BigDecimal mainOnHand = MStorageOnHand.getQtyOnHand(mainProductId, warehouseId, 0, null);
			if (mainOnHand.compareTo(BigDecimal.ZERO) > 0) {
				return null;
			}
		}

		// 按优先级遍历替代料，找第一条有库存且不在省份限制范围内的
		for (Object[] sub : substitutes) {
			int subProductId = (Integer) sub[0];

			// 检查省份限制（在限制范围内则跳过该替代料）
			if (regionId > 0 && isRegionRestricted(regionId, today)) {
				log.info("替代料[" + subProductId + "]因省份限制被过滤");
				continue;
			}

			BigDecimal subOnHand = MStorageOnHand.getQtyOnHand(subProductId, warehouseId, 0, null);
			if (subOnHand.compareTo(BigDecimal.ZERO) > 0) {
				SubstituteResult result = new SubstituteResult();
				result.substituteProductId = subProductId;
				// 从查询结果中取替代比例（sub[1] = SubstituteRatio）
				BigDecimal ratio = (BigDecimal) sub[1];
				result.ratio = (ratio != null && ratio.compareTo(BigDecimal.ZERO) > 0) ? ratio : BigDecimal.ONE;
				return result;
			}
		}

		return null; // 无可用替代料，回退到主料
	}

	/**
	 * 检查省份是否在限制替代日期范围内 在范围内（restrictFrom <= today < restrictTo）则不可替代，返回 true
	 */
	private boolean isRegionRestricted(int regionId, Timestamp today) {
		String sql = "SELECT SubstituteRestrictFrom, SubstituteRestrictTo " + "FROM C_Region WHERE C_Region_ID=?";
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, regionId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				Timestamp from = rs.getTimestamp(1);
				Timestamp to = rs.getTimestamp(2);
				if (from != null && to != null) {
					return !today.before(from) && today.before(to);
				}
			}
		} catch (Exception e) {
			log.severe("查询省份限制失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return false;
	}

	/**
	 * 通过仓库ID获取省份ID（C_Region_ID） 路径：M_Warehouse → C_Location → C_Region
	 */
	private int getRegionByWarehouse(int warehouseId) {
		String sql = "SELECT l.C_Region_ID " + "FROM M_Warehouse w "
				+ "JOIN C_Location l ON l.C_Location_ID = w.C_Location_ID " + "WHERE w.M_Warehouse_ID=?";
		int regionId = DB.getSQLValue(null, sql, warehouseId);
		return regionId > 0 ? regionId : 0;
	}

	/**
	 * 替代料解析结果
	 * @ClassName: SubstituteResult
	 * @author ldh
	 * @date 2026年6月2日
	 */
	private static class SubstituteResult {
		int substituteProductId;
	    BigDecimal ratio = BigDecimal.ONE; // 替代比例，默认1:1
	}
	
	// 判断当前工单是否为"生产工单"类型
	private boolean isStandardProductionOrder() {
		MPPOrder order = getPP_Order();
		if (order == null)
			return false;
		return order.getC_DocTypeTarget_ID() == 1000740;
	}

	// 添加按钮显隐控制方法
	private void updateMaterialButtonsVisibility() {
		boolean hide = isStandardProductionOrder();
		addMaterialButton.setVisible(!hide);
		deleteMaterialButton.setVisible(!hide);
	}

	/**
	 * 通用物料领用入口 不依赖工单，直接打开物料信息窗口（过滤YL_原材料大类，排除YL01/YL02/YL04/YL05）
	 */
	private void onGeneralIssue() {
		final List<Object[]> selectedProducts = new ArrayList<Object[]>();

		final Window outerDialog = new Window();
		outerDialog.setTitle("通用物料领用");
		outerDialog.setWidth("720px");
		outerDialog.setHeight("480px");
		outerDialog.setBorder("normal");
		outerDialog.setClosable(true);
		outerDialog.setSizable(true);

		Hbox toolbar = new Hbox();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:4px; text-align:right; background:#f5f5f5; border-bottom:1px solid #ddd");
		toolbar.setPack("end");
		final Button addBtn = new Button("+");
		final Button removeBtn = new Button("-");
		addBtn.setTooltiptext("从物料列表中选择物料");
		removeBtn.setTooltiptext("删除选中的物料");
		toolbar.appendChild(addBtn);
		toolbar.appendChild(removeBtn);

		final WListbox outerTable = new WListbox();
		outerTable.setWidth("100%");
		outerTable.setHeight("330px");
		outerTable.setMultiSelection(true);
		outerTable.setMultiple(true);   
		// outerTable.setCheckmark(true);
		refreshOuterTable(outerTable, selectedProducts);

		Hbox bottomBar = new Hbox();
		bottomBar.setWidth("100%");
		bottomBar.setStyle("padding:5px; text-align:right");
		bottomBar.setPack("end");
		Button cancelBtn = new Button("取消");
		Button confirmBtn = new Button("确定");
		bottomBar.appendChild(confirmBtn);
		bottomBar.appendChild(cancelBtn);


		org.zkoss.zul.Vlayout vlayout = new org.zkoss.zul.Vlayout();
		vlayout.setWidth("100%");
		vlayout.appendChild(toolbar);
		vlayout.appendChild(outerTable);
		vlayout.appendChild(bottomBar);
		outerDialog.appendChild(vlayout);

		// + 按钮：直接打开物料信息窗口，带物料组过滤
		addBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				int clientId = Env.getAD_Client_ID(Env.getCtx());
				int warehouseId = getDefaultWarehouseId();
				Env.setContext(Env.getCtx(), m_WindowNo, "M_Warehouse_ID", warehouseId);

				// 1. 查出 Value='CP' 的成品大类 ID，用于排除
				int cpId = DB.getSQLValue(null, "SELECT M_Product_Category_ID FROM M_Product_Category "
						+ "WHERE Value='CP' AND IsActive='Y' AND AD_Client_ID=" + clientId);

				// 2. 查出需要排除的 L2 子类 ID（YL01/YL02/YL04/YL05）
				StringBuilder excludeIds = new StringBuilder();
				java.sql.PreparedStatement pstmt = null;
				java.sql.ResultSet rs = null;
				try {
					pstmt = DB.prepareStatement("SELECT M_Product_Category_ID FROM M_Product_Category "
							+ "WHERE Value IN ('YL01','YL02','YL04','YL05') " + "AND IsActive='Y' AND AD_Client_ID="
							+ clientId, null);
					rs = pstmt.executeQuery();
					while (rs.next()) {
						if (excludeIds.length() > 0)
							excludeIds.append(",");
						excludeIds.append(rs.getInt(1));
					}
				} catch (Exception ex) {
					log.severe("查询排除物料组失败: " + ex.getMessage());
				} finally {
					DB.close(rs, pstmt);
				}

				// 3. 构建产品列表的 whereClause ★ 修改此处
				StringBuilder whereClause = new StringBuilder();

				// ★ 排除成品大类（CP）的物料
				if (cpId > 0) {
					whereClause.append("(p.M_Product_Category_ID_L1 IS NULL OR p.M_Product_Category_ID_L1 <> ")
							.append(cpId).append(")");
				}

				// 排除 L2 子类（YL01/YL02/YL04/YL05）
				if (excludeIds.length() > 0) {
					if (whereClause.length() > 0)
						whereClause.append(" AND ");
					whereClause.append("(p.M_Product_Category_ID_L2 IS NULL OR p.M_Product_Category_ID_L2 NOT IN (")
							.append(excludeIds).append("))");
				}

				MLookup productLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0,
						MColumn.getColumn_ID(MProduct.Table_Name, MProduct.COLUMNNAME_M_Product_ID),
						DisplayType.ChosenMultipleSelectionSearch);

				final org.adempiere.webui.panel.InfoPanel ip = org.adempiere.webui.factory.InfoManager
						.create(productLookup, null, "M_Product", "M_Product_ID", null, true, whereClause.toString());
				if (ip == null)
					return;

				// 反射操作：限制 L2 下拉 + 排除 L1 中的 CP 大类
				if (ip instanceof org.adempiere.webui.info.InfoWindow) {
					try {
						java.lang.reflect.Field editorsField = org.adempiere.webui.info.InfoWindow.class
								.getDeclaredField("editors");
						editorsField.setAccessible(true);
						@SuppressWarnings("unchecked")
						java.util.List<org.adempiere.webui.editor.WEditor> editorList = (java.util.List<org.adempiere.webui.editor.WEditor>) editorsField
								.get(ip);

						if (editorList != null) {
							// 第一步：修改 L2 的 ValidationCode，排除 YL01/YL02/YL04/YL05
							if (excludeIds.length() > 0) {
								for (org.adempiere.webui.editor.WEditor editor : editorList) {
									if (editor.getGridField() != null && "M_Product_Category_ID_L2"
											.equals(editor.getGridField().getColumnName())) {
										if (editor instanceof org.adempiere.webui.editor.WTableDirEditor) {
											org.compiere.model.Lookup lkp = ((org.adempiere.webui.editor.WTableDirEditor) editor)
													.getLookup();
											if (lkp instanceof org.compiere.model.MLookup) {
												org.compiere.model.MLookupInfo info = ((org.compiere.model.MLookup) lkp)
														.getLookupInfo();
												String excludeClause = "M_Product_Category.M_Product_Category_ID NOT IN ("
														+ excludeIds + ")";
												String existing = info.ValidationCode != null
														? info.ValidationCode.trim()
														: "";
												info.ValidationCode = existing.length() > 0
														? existing + " AND " + excludeClause
														: excludeClause;
												info.IsValidated = false;
											}
										}
										break;
									}
								}
							}

							// 第二步：修改 L1 的 ValidationCode，排除 CP 大类
							if (cpId > 0) {
								for (org.adempiere.webui.editor.WEditor editor : editorList) {
									if (editor.getGridField() != null && "M_Product_Category_ID_L1"
											.equals(editor.getGridField().getColumnName())) {
										if (editor instanceof org.adempiere.webui.editor.WTableDirEditor) {
											org.adempiere.webui.editor.WTableDirEditor l1TableDir = (org.adempiere.webui.editor.WTableDirEditor) editor;
											org.compiere.model.Lookup lkp = l1TableDir.getLookup();
											if (lkp instanceof org.compiere.model.MLookup) {
												org.compiere.model.MLookupInfo info = ((org.compiere.model.MLookup) lkp)
														.getLookupInfo();
												String excludeClause = "M_Product_Category.M_Product_Category_ID <> "
														+ cpId;
												String existing = info.ValidationCode != null
														? info.ValidationCode.trim()
														: "";
												info.ValidationCode = existing.length() > 0
														? existing + " AND " + excludeClause
														: excludeClause;
												info.IsValidated = false;
												// 立即刷新 L1 下拉列表，使新 ValidationCode 生效
												l1TableDir.actionRefresh();
											}
										}
										break;
									}
								}
							}
						}
					} catch (Exception ex) {
						log.warning("设置物料组默认值失败: " + ex.getMessage());
					}
				}

				ip.setVisible(true);
				ip.setClosable(true);
				ip.addEventListener(org.adempiere.webui.event.DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {
					public void onEvent(Event event) throws Exception {
						if (ip.isCancelled())
							return;
						Object[] keys = ip.getSelectedKeys();
						if (keys == null || keys.length == 0)
							return;

						java.util.Set<Integer> existingIds = new java.util.HashSet<Integer>();
						for (Object[] p : selectedProducts) {
							existingIds.add((Integer) p[0]);
						}

						int added = 0;
						for (Object key : keys) {
							if (key == null)
								continue;
							int productId;
							try {
								productId = Integer.parseInt(key.toString());
							} catch (NumberFormatException ex) {
								continue;
							}
							if (existingIds.contains(productId))
								continue;

							MProduct product = MProduct.get(Env.getCtx(), productId);
							if (product == null)
								continue;
							org.compiere.model.MUOM uom = org.compiere.model.MUOM.get(Env.getCtx(),
									product.getC_UOM_ID());
							String uomName = uom != null ? uom.getName() : "";

							selectedProducts.add(new Object[] { productId, product.getValue(), product.getName(),
									uomName, BigDecimal.ONE });
							existingIds.add(productId);
							added++;
						}

						if (added > 0) {
							refreshOuterTable(outerTable, selectedProducts);
						}
					}
				});
				ip.setId("InfoProduct_" + m_WindowNo);
				org.adempiere.webui.apps.AEnv.showWindow(ip);
			}
		});
		// - 按钮：删除勾选行
		removeBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				List<Integer> toRemove = new ArrayList<Integer>();
				for (int i = 0; i < outerTable.getRowCount(); i++) {
					IDColumn idCol = (IDColumn) outerTable.getValueAt(i, 0);
					if (idCol != null && idCol.isSelected()) {
						toRemove.add(0, i);
					}
				}
				if (toRemove.isEmpty()) {
					Messagebox.show("请先勾选要删除的物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
					return;
				}
				for (int idx : toRemove) {
					selectedProducts.remove(idx);
				}
				refreshOuterTable(outerTable, selectedProducts);
			}
		});

		cancelBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				outerDialog.detach();
			}
		});

		// 确定：生成领用单
		confirmBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				if (outerTable.getRowCount() == 0) {
					Messagebox.show("请先添加物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
					return;
				}
				final List<Object[]> finalProducts = new ArrayList<Object[]>();
				for (int i = 0; i < outerTable.getRowCount(); i++) {
					IDColumn idCol = (IDColumn) outerTable.getValueAt(i, 0);
					if (idCol == null)
						continue;
					int productId = idCol.getRecord_ID();
					Object qtyObj = outerTable.getValueAt(i, 5);
					BigDecimal qty = BigDecimal.ONE;
					if (qtyObj instanceof BigDecimal) {
						qty = (BigDecimal) qtyObj;
					} else if (qtyObj != null) {
						try {
							qty = new BigDecimal(qtyObj.toString());
						} catch (Exception ex) {
							/* ignore */ }
					}
					if (qty.compareTo(Env.ZERO) <= 0)
						qty = BigDecimal.ONE;
					finalProducts.add(new Object[] { productId, qty });
				}
				outerDialog.detach();
				createGeneralInventory(finalProducts);
			}
		});

		outerDialog.setPage(form.getPage());
		outerDialog.doModal();
	}

	/**
	 * 获取当前组织的默认仓库ID
	 */
	private int getDefaultWarehouseId() {
		int orgId = Env.getAD_Org_ID(Env.getCtx());
		return DB.getSQLValue(null, "SELECT M_Warehouse_ID FROM M_Warehouse "
				+ "WHERE AD_Org_ID=? AND IsActive='Y' AND IsInTransit='N' " + "FETCH FIRST 1 ROWS ONLY", orgId);
	}

	/**
	 * 生成《领用单》表头+明细，并触发工作流审批
	 * 
	 * @param products [productId(Integer), qty(BigDecimal)]
	 */
	private void createGeneralInventory(final List<Object[]> products) {
		final String[] resultDocNo = { null };
		final int[] resultInventoryId = { 0 };

		try {
			Trx.run(new TrxRunnable() {
				public void run(String trxName) {
					int orgId = Env.getAD_Org_ID(Env.getCtx());
					int clientId = Env.getAD_Client_ID(Env.getCtx());
					int userId = Env.getAD_User_ID(Env.getCtx());
					int warehouseId = getDefaultWarehouseId();

					if (warehouseId <= 0)
						throw new AdempiereException("未找到当前组织的默认仓库，请先配置仓库");

					// 查找单据类型（领用单）
					int docTypeId = DB.getSQLValue(null, "SELECT C_DocType_ID FROM C_DocType "
							+ "WHERE Name='领用单' AND DocSubTypeInv='IU' AND IsActive='Y' AND AD_Client_ID=" + clientId);

					if (docTypeId <= 0)
						throw new AdempiereException("未找到单据类型：领用单，请先在系统中配置");

					// 查找费用项目（500101 生产成本_材料成本）
					int chargeId = DB.getSQLValue(trxName,
							"SELECT C_Charge_ID FROM C_Charge "
									+ "WHERE AD_Client_ID=? AND Name='500101 生产成本_材料成本' AND IsActive='Y' "
									+ "FETCH FIRST 1 ROWS ONLY",
							clientId);

					// ── 库存预校验（在创建 MInventory 之前）──
					for (Object[] p : products) {
						int productId = (Integer) p[0];
						BigDecimal qty = (BigDecimal) p[1];

						BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHand(productId, warehouseId, 0, // M_AttributeSetInstance_ID
																										// = 0 表示不限批次
								trxName);

						if (qtyOnHand.compareTo(qty) < 0) {
							MProduct product = MProduct.get(Env.getCtx(), productId);
							throw new AdempiereException("物料[" + product.getValue() + " - " + product.getName() + "]"
									+ " 库存不足，需要: " + qty + "，当前在手库存: " + qtyOnHand);
						}
					}

					// ── 创建领用单表头 ──
					MInventory inventory = new MInventory(Env.getCtx(), 0, trxName);
					inventory.setAD_Org_ID(orgId);
					inventory.setC_DocType_ID(docTypeId);
					inventory.setMovementDate(new Timestamp(System.currentTimeMillis()));
					inventory.setM_Warehouse_ID(warehouseId);
					inventory.set_ValueOfColumn("AD_User_ID", userId);
					inventory.setDocStatus(MInventory.DOCSTATUS_Drafted);
					inventory.setDocAction(MInventory.DOCACTION_Complete);

					// ★ 查出"烟包生产部"的 C_Activity_ID 并设置
					int activityId = DB.getSQLValue(trxName,
							"SELECT C_Activity_ID FROM C_Activity "
									+ "WHERE Name='烟包生产部' AND IsActive='Y' AND AD_Client_ID=?",
							Env.getAD_Client_ID(Env.getCtx()));
					if (activityId > 0) {
						inventory.setC_Activity_ID(activityId);
					} else {
						log.warning("未找到 C_Activity Name='烟包生产部'，C_Activity_ID 未设置");
					}
					inventory.saveEx(trxName);

					// ── 创建领用单明细 ──
					int lineNo = 10;
					for (Object[] p : products) {
						int productId = (Integer) p[0];
						BigDecimal qty = (BigDecimal) p[1];

						// 通过SQL函数获取推荐库位
						int locatorId = DB.getSQLValue(trxName, "SELECT get_recommended_locator(?, ?, ?)", productId,
								warehouseId, "N");

						if (locatorId <= 0) {
							// 回退：取仓库默认库位
							locatorId = DB.getSQLValue(trxName,
									"SELECT M_Locator_ID FROM M_Locator "
											+ "WHERE M_Warehouse_ID=? AND IsDefault='Y' AND IsActive='Y' "
											+ "FETCH FIRST 1 ROWS ONLY",
									warehouseId);
						}
						if (locatorId <= 0)
							throw new AdempiereException("物料[ID=" + productId + "]未找到有效库位，请检查库存或库位配置");

						MInventoryLine line = new MInventoryLine(Env.getCtx(), 0, trxName);
						line.setM_Inventory_ID(inventory.getM_Inventory_ID());
						line.setAD_Org_ID(orgId);
						line.setM_Locator_ID(locatorId);
						line.setM_Product_ID(productId);
						line.setM_AttributeSetInstance_ID(0);
						line.setLine(lineNo);
						line.setInventoryType(MInventoryLine.INVENTORYTYPE_ChargeAccount);
						line.setQtyInternalUse(qty);
						line.setQtyBook(Env.ZERO);
						line.setQtyCount(Env.ZERO);
						if (chargeId > 0) {
							line.setC_Charge_ID(chargeId);
						}
						line.saveEx(trxName);
						lineNo += 10;
					}

					// ── 触发工作流审批 ──
					try {
						MWorkflow wf = new Query(Env.getCtx(), MWorkflow.Table_Name, "Value=? AND IsActive='Y'",
								trxName).setParameters("inventory").setClient_ID().first();
						if (wf == null) {
							log.warning("未找到工作流 Value='inventory'，跳过审批");
						} else {
							ProcessInfo pi = new ProcessInfo(inventory.getDocumentNo(), 0, inventory.get_Table_ID(),
									inventory.getM_Inventory_ID());
							pi.setTransactionName(trxName);
							pi.setPO(inventory);

							MWFProcess wfProcess = ProcessUtil.startWorkFlow(Env.getCtx(), pi, wf.getAD_Workflow_ID());
							if (wfProcess == null || pi.isError()) {
								log.warning("领用单工作流启动失败: " + pi.getSummary());
							} else {
								inventory.load(trxName);
								log.info("领用单[" + inventory.getDocumentNo() + "]工作流已启动，状态: " + wfProcess.getWFState());
							}
						}
					} catch (Exception wfEx) {
						log.severe("领用单工作流异常: " + wfEx.getMessage());
					}

					resultDocNo[0] = inventory.getDocumentNo();
					resultInventoryId[0] = inventory.getM_Inventory_ID();

				}
			});

			if (resultDocNo[0] != null) {
				// ★ 改用 org.adempiere.webui.component.Window，而不是 org.zkoss.zul.Window
				final org.adempiere.webui.component.Window successDialog = new org.adempiere.webui.component.Window();
				successDialog.setTitle("成功");
				successDialog.setWidth("420px");
				successDialog.setBorder("normal");
				successDialog.setClosable(true);

				org.zkoss.zul.Hbox content = new org.zkoss.zul.Hbox();
				content.setStyle("padding:12px 10px; align-items:center");
				content.setAlign("center");
				content.appendChild(new org.zkoss.zul.Label("领用单 "));
				final int inventoryTableId = org.compiere.model.MTable.getTable_ID("M_Inventory");
				final int invId = resultInventoryId[0];
				org.adempiere.webui.component.DocumentLink docLink = new org.adempiere.webui.component.DocumentLink(
						resultDocNo[0], inventoryTableId, invId, new EventListener<Event>() {
							public void onEvent(Event event) throws Exception {
								successDialog.detach(); // ★ 先关闭弹窗
								if (inventoryTableId > 0 && invId > 0)
									org.adempiere.webui.apps.AEnv.zoom(inventoryTableId, invId); // ★ 再跳转
							}
						});
				content.appendChild(docLink);
				content.appendChild(new org.zkoss.zul.Label(" 已成功创建并提交审批"));

				Button okBtn = new Button("确定");
				okBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
					public void onEvent(Event event) throws Exception {
						successDialog.detach();
					}
				});
				org.zkoss.zul.Hbox btnBox = new org.zkoss.zul.Hbox();
				btnBox.setStyle("padding:5px");
				btnBox.setPack("end");
				btnBox.setWidth("100%");
				btnBox.appendChild(okBtn);

				org.zkoss.zul.Vlayout vlayout = new org.zkoss.zul.Vlayout();
				vlayout.appendChild(content);
				vlayout.appendChild(btnBox);
				successDialog.appendChild(vlayout);

				org.adempiere.webui.apps.AEnv.showCenterScreen(successDialog);
			}


		} catch (Exception ex) {
			log.severe("创建领用单失败: " + ex.getMessage());
			Messagebox.show("创建领用单失败: " + ex.getMessage(), "错误", Messagebox.OK, Messagebox.ERROR);
		}
	}

}