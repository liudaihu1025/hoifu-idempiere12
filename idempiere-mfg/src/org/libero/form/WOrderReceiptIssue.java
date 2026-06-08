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

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
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
import org.compiere.model.MLocator;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTab;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
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
		fulfilledFilterCombo.setSelectedIndex(2);
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
	}
	
	
	/**
	 * 重新加载工单数据
	 * @param orderId 工单ID
	 */
	private void reloadOrderData(int orderId) {
	    try {
	        // 1. 保存当前选中的工序
	        Integer selectedNodeId = getAD_WF_Node_ID();
	        
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
	            int workflowId = pp_order.getAD_Workflow_ID();
	            loadNodeComboData(workflowId);
	            
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
	 * @param workflowId 工艺路线ID
	 */
	private void loadNodeComboData(int workflowId) {
		// 清空当前选项
		nodeCombo.getItems().clear();
		
		if (workflowId <= 0) {
			// 添加工序为空的选项
			nodeCombo.appendItem("请选择工序", null);
			nodeCombo.setSelectedIndex(0);
			return;
		}
		
		// 查询该工艺路线的所有工序
		String sql = "SELECT AD_WF_Node_ID, Name FROM AD_WF_Node WHERE AD_Workflow_ID = ? AND IsActive='Y' ORDER BY value";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, workflowId);
			rs = pstmt.executeQuery();
			
			// 添加工序为空的选项
			nodeCombo.appendItem("请选择工序", null);
			
			boolean hasData = false;
			while (rs.next()) {
				int nodeId = rs.getInt("AD_WF_Node_ID");
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
			return;
		}
		
		Object nodeObj = nodeCombo.getSelectedItem().getValue();
		if (nodeObj != null && nodeObj instanceof Integer) {
			Integer nodeId = (Integer) nodeObj;
			if (nodeId > 0) {
				// 根据工序ID获取机台信息 - 使用 DB.getSQLValue
				String sql = "SELECT S_Resource_ID FROM AD_WF_Node WHERE AD_WF_Node_ID = ?";
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
		
		// 重新执行查询
		super.executeQuery(issue);
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

				// 获取工单的工艺路线ID
				int workflowId = pp_order.getAD_Workflow_ID();

				// 加载工序下拉框数据
				loadNodeComboData(workflowId);
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
	    int nodeId = getAD_WF_Node_ID();
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
	 * @return 工序ID (AD_WF_Node_ID)
	 */
	protected int getAD_WF_Node_ID() {
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
				BigDecimal calculatedQty = calculateIssueQty(requiredQty, deliveredQty, unitsPerPack);

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
				maxQty = calculatedQty.compareTo(stockQty) > 0 ? stockQty : calculatedQty;

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

		// 创建弹窗
		Window dialog = new Window();
		dialog.setTitle("新增物料");
		dialog.setWidth("500px");
		dialog.setBorder("normal");
		dialog.setClosable(true);
		dialog.setSizable(true);

		// 创建产品搜索字段（会自动关联 AD_InfoWindow_ID=200000）
		MLookup productLookup = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0,
				MColumn.getColumn_ID(MProduct.Table_Name, MProduct.COLUMNNAME_M_Product_ID), DisplayType.Search);
		WSearchEditor productSearchField = new WSearchEditor(MProduct.COLUMNNAME_M_Product_ID, false, false, true,
				productLookup);

		// 布局
		Grid grid = GridFactory.newGridLayout();
		Rows rows = grid.newRows();
		Row row = rows.newRow();
		row.appendChild(new Label("物料").rightAlign());
		row.appendChild(productSearchField.getComponent());

		// 按钮
		Button confirmBtn = new Button("确认");
		Button cancelBtn = new Button("取消");
		Panel btnPanel = new Panel();
		btnPanel.setStyle("text-align:center; padding:5px");
		btnPanel.appendChild(confirmBtn);
		btnPanel.appendChild(cancelBtn);

		dialog.appendChild(grid);
		dialog.appendChild(btnPanel);

		confirmBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				Integer productId = (Integer) productSearchField.getValue();
				if (productId != null && productId > 0) {
					dialog.detach();
					addProductToOrderBOM(productId);
				} else {
					Messagebox.show("请选择物料", "提示", Messagebox.OK, Messagebox.INFORMATION);
				}
			}
		});

		cancelBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event e) throws Exception {
				dialog.detach();
			}
		});

		dialog.setPage(form.getPage());
		dialog.doModal();
	}

	private void addProductToOrderBOM(int productId) {
		try {
			MPPOrder order = getPP_Order();
			if (order == null)
				return;

			// 1. 防止重复添加
			MPPOrderBOMLine existing = MPPOrderBOMLine.forM_Product_ID(Env.getCtx(), order.get_ID(), productId, null);
			if (existing != null) {
				Messagebox.show("该物料已存在于工单BOM中，无法重复添加", "提示", Messagebox.OK, Messagebox.INFORMATION);
				return;
			}

			// 2. 获取工单对应的 PP_Order_BOM_ID
			MPPOrderBOM orderBOM = new Query(Env.getCtx(), MPPOrderBOM.Table_Name, "PP_Order_ID=?", null)
					.setParameters(order.get_ID()).first();
			if (orderBOM == null) {
				Messagebox.show("未找到工单对应的BOM，无法新增物料", "错误", Messagebox.OK, Messagebox.ERROR);
				return;
			}

			// 3. 获取物料信息
			MProduct product = MProduct.get(Env.getCtx(), productId);
			if (product == null)
				return;

			// 4. 创建新的 MPPOrderBOMLine
			Trx.run(new TrxRunnable() {
				public void run(String trxName) {
					MPPOrderBOMLine newLine = new MPPOrderBOMLine(Env.getCtx(), 0, trxName);
					newLine.setAD_Org_ID(order.getAD_Org_ID());
					newLine.setPP_Order_ID(order.get_ID());
					newLine.setPP_Order_BOM_ID(orderBOM.get_ID());
					newLine.setM_Product_ID(productId);
					newLine.setC_UOM_ID(product.getC_UOM_ID());
					newLine.setM_Warehouse_ID(order.getM_Warehouse_ID());
					newLine.setQtyBatch(Env.ZERO);
					newLine.setIsQtyPercentage(false);
					newLine.setQtyBOM(BigDecimal.ONE); // BOM数量默认1
					// ComponentType 默认空（不设置，使用默认值）
					// 用工单数量 × BOM数量(1) 计算需求数量
					newLine.setComponentType(MPPOrderBOMLine.COMPONENTTYPE_Component);
					newLine.setQtyPlusScrap(order.getQtyOrdered());
					newLine.setValidFrom(new Timestamp(System.currentTimeMillis()));
					newLine.saveEx(trxName);
				}
			});

			// 5. 刷新物料表格
			Messagebox.show("物料已成功添加到工单BOM", "成功", Messagebox.OK, Messagebox.INFORMATION);
			executeQuery(); // 刷新领退料窗口的物料列表

		} catch (Exception ex) {
			log.severe("新增物料到工单BOM失败: " + ex.getMessage());
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
	
}