/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *                 Teo Sarca, http://www.arhipac.ro                           *
 ******************************************************************************/

package org.libero.form;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.apps.form.GenForm;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPCostCollector;
import org.libero.model.MPPMaterialRequisition;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOMLine;

/**
 * 
 * @author victor.perez@e-evolution.com http://www.e-evolution.com
 */

public class OrderReceiptIssue extends GenForm {

	/** Logger */
	protected static CLogger log = CLogger.getCLogger(OrderReceiptIssue.class);
	String m_sql = "";

	private int m_nodeFilter = 0; // 0=不过滤，>0=只显示该工序和无工序的子件

	private boolean m_isOnlyReceipt = false;

	private boolean m_OnlyIssue = false;

	protected boolean m_IsBackflush = false;

	protected Timestamp m_movementDate = null;

	protected BigDecimal m_orderedQty = Env.ZERO;

	protected BigDecimal m_DeliveredQty = Env.ZERO;

	protected BigDecimal m_toDeliverQty = Env.ZERO;

	protected BigDecimal m_scrapQty = Env.ZERO;

	protected BigDecimal m_rejectQty = Env.ZERO;

	protected BigDecimal m_openQty = Env.ZERO;

	protected BigDecimal m_qtyBatchs = Env.ZERO;

	protected BigDecimal m_qtyBatchSize = Env.ZERO;

	protected int m_M_AttributeSetInstance_ID = 0;

	protected int m_M_Locator_ID = 0;

	private int m_PP_Order_ID = 0;

	// 用于在 cmd_process 执行后传递给弹窗
	protected ProcessInfo lastProcessInfo;

	private MPPOrder m_PP_order = null;
	// 成本归集类型常量
	public static final String COSTCOLLECTORTYPE_PRODUCTION_RECEIPT = "100"; // 生产收货
	public static final String COSTCOLLECTORTYPE_PRODUCTION_ISSUE = "110"; // 生产发料
	public static final String COSTCOLLECTORTYPE_PRODUCTION_RETURN = "115"; // 生产退料
	public static final String COSTCOLLECTORTYPE_PRODUCTION_REPLENISHMENT = "116"; // 生产补领
	public static final String COSTCOLLECTORTYPE_MIX = "120"; // 混合

	// 在现有常量后添加
	public static final String COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE = "131"; // 委外发料
	public static final String COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN = "132"; // 委外退料
	public static final String COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT = "133"; // 委外补领

	// 添加新的成员变量
	protected boolean m_IsProductionReplenishment = false;

	protected boolean m_IsProductionReturn = false;

	// 添加新的成员变量
	protected boolean m_IsSubcontracting = false;

	// 新增生产/委外领料补领是否限制超领
	public static final String SYSCONFIG_RESTRICT_OVER_ISSUE = "MFG.RestrictOverIssue";

	// 添加成员变量
	protected String m_fulfilledFilter = null; // null=全部, "Y"=是, "N"=否

	// 添加 setter
	protected void setFulfilledFilter(String filter) {
		m_fulfilledFilter = filter;
	}

	// 添加getter和setter方法
	protected boolean isSubcontracting() {
		return m_IsSubcontracting;
	}

	protected void setIsSubcontracting(boolean isSubcontracting) {
		m_IsSubcontracting = isSubcontracting;
	}

	String costCollectorType = null;

	// 添加getter和setter方法
	protected boolean isProductionReplenishment() {
		return m_IsProductionReplenishment;
	}

	protected void setIsProductionReplenishment(boolean isProductionReplenishment) {
		m_IsProductionReplenishment = isProductionReplenishment;
	}

	protected boolean isProductionReturn() {
		return m_IsProductionReturn;
	}

	protected void setIsProductionReturn(boolean isProductionReturn) {
		m_IsProductionReturn = isProductionReturn;
	}

	public void configureMiniTable(IMiniTable issue) {
		// 按照图片中的顺序添加列，现在共16列
		issue.addColumn("关键部件"); // 0 - 复选框列
		issue.addColumn("物料编码"); // 1
		issue.addColumn("物料"); // 2
		issue.addColumn("规格");// 3
		issue.addColumn("单位"); // 4
		issue.addColumn("批次"); // 5
		issue.addColumn("需求数量"); // 6
		issue.addColumn("已领数量"); // 7
		issue.addColumn("仓库申请数量"); // 8 - 可编辑
		issue.addColumn("线边仓申请数量"); // 9 - 只读，自动回显
		issue.addColumn("仓库库存"); // 10
		issue.addColumn("预留数量"); // 11
		issue.addColumn("可用数量"); // 12
		issue.addColumn("仓库"); // 13
		issue.addColumn("bom数量"); // 14
		issue.addColumn("线边仓库存"); // 15
		issue.addColumn("主料信息"); // 16

		issue.setMultiSelection(true);

		// 设置列属性
		issue.setColumnClass(0, IDColumn.class, true, "关键部件");
		issue.setColumnClass(1, String.class, true, "物料编码");
		issue.setColumnClass(2, KeyNamePair.class, true, "物料");
		issue.setColumnClass(3, String.class, true, "规格");
		issue.setColumnClass(4, KeyNamePair.class, true, "单位");
		issue.setColumnClass(5, String.class, true, "批次");
		issue.setColumnClass(6, BigDecimal.class, true, "需求数量");
		issue.setColumnClass(7, BigDecimal.class, true, "已领数量");
		issue.setColumnClass(8, BigDecimal.class, false, "仓库申请数量"); // 可编辑
		issue.setColumnClass(9, BigDecimal.class, true, "线边仓申请数量"); // 只读
		issue.setColumnClass(10, BigDecimal.class, true, "仓库库存");
		issue.setColumnClass(11, BigDecimal.class, true, "预留数量");
		issue.setColumnClass(12, BigDecimal.class, true, "可用数量");
		issue.setColumnClass(13, KeyNamePair.class, true, "仓库");
		issue.setColumnClass(14, BigDecimal.class, true, "bom数量");
		issue.setColumnClass(15, BigDecimal.class, true, "线边仓库存");
		issue.setColumnClass(16, String.class, true, "主料信息");

		issue.autoSize();
		issue.setRowCount(0);

		// 更新SQL查询，移除库位相关的JOIN
		m_sql = "SELECT " + "obl.PP_Order_BOMLine_ID," // 1 - ID
				+ "obl.IsCritical," // 2 - 是否关键部件
				+ "p.Value," // 3 - 物料编码
				+ "obl.M_Product_ID,p.Name," // 4,5 - 物料ID和名称
				+ "p.C_UOM_ID,u.Name," // 6,7 - 单位ID和名称
				+ "obl.QtyRequiered," // 8 - 需求数量
				+ "obl.QtyDelivered," // 9 - 已领数量
				+ "obl.QtyBom," // 10 - bom数量
				+ "obl.QtyReserved," // 11 - 预留数量
				+ "bomQtyAvailable(obl.M_Product_ID,obl.M_Warehouse_ID,0 ) AS QtyAvailable," // 12 - 可用数量
				+ "bomQtyOnHand(obl.M_Product_ID,obl.M_Warehouse_ID,0) AS QtyOnHand," // 13 - 库存数量
				+ "obl.M_Warehouse_ID,w.Name," // 14,15 - 仓库ID和名称
				+ "obl.ComponentType" // 16 - 组件类型
				+ " FROM PP_Order_BOMLine obl" + " INNER JOIN M_Product p ON (obl.M_Product_ID = p.M_Product_ID) "
				+ " INNER JOIN C_UOM u ON (p.C_UOM_ID = u.C_UOM_ID) "
				+ " INNER JOIN M_Warehouse w ON (w.M_Warehouse_ID = obl.M_Warehouse_ID) " + " WHERE obl.PP_Order_ID = ?"
				+ " ORDER BY obl." + MPPOrderBOMLine.COLUMNNAME_Line;
	}

	// dynInit
	private String createHTMLTable(String[][] table) {
		StringBuffer html = new StringBuffer("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"0\">");

		for (int i = 0; i < table.length; i++) {
			if (table[i] != null) {
				html.append("<tr>");
				for (int j = 0; j < table[i].length; j++) {
					html.append("<td>");
					if (table[i][j] != null) {
						html.append(table[i][j]);
					}
					html.append("</td>");
				}
				html.append("</tr>");
			}
		}
		html.append("</table>");

		return html.toString();
	}

	/**
	 * Performs what ever task is attached to the combo box
	 * 
	 * @return Whether the process was successful or not
	 */
	/*
	 * public boolean cmd_process(final boolean isCloseDocument, final IMiniTable
	 * issue) {
	 * 
	 * if (isOnlyReceipt() || isBackflush()) { if (getM_Locator_ID() <= 0) {
	 * //JOptionPane.showMessageDialog(null, Msg.getMsg(Env.getCtx(),"NoLocator"),
	 * "Info", JOptionPane.INFORMATION_MESSAGE); showMessage(
	 * Msg.getMsg(Env.getCtx(),"NoLocator"), false); } } if (getPP_Order() == null
	 * || getMovementDate() == null) { return false; } try { Trx.run(new
	 * TrxRunnable() { public void run(String trxName) { MPPOrder order = new
	 * MPPOrder(Env.getCtx(), getPP_Order_ID(), trxName); if (isBackflush() ||
	 * isOnlyIssue()) { createIssue(order, issue); } if (isOnlyReceipt() ||
	 * isBackflush()) { MPPOrder.createReceipt(order, getMovementDate(),
	 * getDeliveredQty(), getToDeliverQty(), getScrapQty(), getRejectQty(),
	 * getM_Locator_ID(), getM_AttributeSetInstance_ID() ); if (isCloseDocument) {
	 * order.setDateFinish(getMovementDate()); order.closeIt(); order.saveEx(); } }
	 * }}); } catch (Exception e) { showMessage(e.getLocalizedMessage(), true);
	 * return false; } finally { m_PP_order = null; }
	 * 
	 * return true; }
	 */
	public void createIssue(MPPOrder order, IMiniTable issue, String costCollectorType) {

		// 判断是否为退料操作（特定标识判断）
		boolean isReturnOperation = isReturnOperation(issue, costCollectorType);

		// 添加工单状态校验
		validateOrderStatusForIssue(order, isReturnOperation);

		validateQuantities(issue, costCollectorType);

		Timestamp movementDate = getMovementDate();

		Timestamp minGuaranteeDate = movementDate;

		// 退料按最小包装拆分：暂存线边仓零头退料数量
		Map<Integer, BigDecimal[]> returnLineSideMap = new HashMap<>();
		// Key: PP_Order_BOMLine_ID, Value: [lineSideQty, M_Product_ID]

		boolean isCompleteQtyDeliver = false;

		// 获取工序和机台ID
		int nodeId = 0;
		int resourceId = 0;
		int activityId = 0;
		if (this instanceof WOrderReceiptIssue) {
			WOrderReceiptIssue wForm = (WOrderReceiptIssue) this;
			nodeId = wForm.getPP_Order_Node_ID(); // 直接使用，已经是 PP_Order_Node_ID
			resourceId = wForm.getS_Resource_ID();
			int selectedActivityId = wForm.getC_Activity_ID();
			if (selectedActivityId > 0) {
				activityId = selectedActivityId;
			}
		}

		// 记录日志
		log.info("生成生产发料单，工单ID: " + order.get_ID() + ", PP_Order_Node_ID: " + nodeId + ", 机台ID: " + resourceId);

		// 新增：创建主表（Draft）
		MPPMaterialRequisition mr = new MPPMaterialRequisition(Env.getCtx(), 0, order.get_TrxName());
		mr.setAD_Org_ID(order.getAD_Org_ID());
		mr.setPP_Order_ID(order.get_ID());
		mr.setM_Product_ID(order.getM_Product_ID());
		mr.setMovementDate(movementDate);
		mr.setPP_Order_Node_ID(nodeId);
		mr.setS_Resource_ID(resourceId);
		mr.setM_Warehouse_ID(order.getM_Warehouse_ID());
		mr.set_ValueOfColumn("C_Activity_ID", activityId);
		mr.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
		mr.setDocAction(MPPOrder.DOCACTION_Complete);
		// 委外工单的业务伙伴信息获取 - 只对委外单据设置
		boolean isSubcontracting = OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE.equals(costCollectorType)
				|| OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN.equals(costCollectorType)
				|| OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT.equals(costCollectorType);

		if (isSubcontracting && order.getC_OrderLine_ID() > 0) {
			try {
				if (order.getC_OrderLine_ID() > 0) {
					I_C_OrderLine orderLine = order.getC_OrderLine();
					if (orderLine != null) {
						int orderId = orderLine.getC_Order_ID();
						MOrder subcontractOrder = new MOrder(Env.getCtx(), orderId, order.get_TrxName());
						if (subcontractOrder != null) {
							mr.set_ValueOfColumn("C_BPartner_ID", subcontractOrder.getC_BPartner_ID());
							mr.set_ValueOfColumn("C_BPartner_Location_ID",
									subcontractOrder.getC_BPartner_Location_ID());
							mr.set_ValueOfColumn("AD_User_ID", subcontractOrder.getAD_User_ID());
						}
					}
				}
			} catch (Exception e) {
				log.warning("获取委外订单业务伙伴信息失败: " + e.getMessage());
			}
		}

		// 判断是否为纯线边仓领料（所有选中行仓库申请数量=0，线边仓申请数量>0）
		boolean isLineSideOnly = isProductionIssue(costCollectorType) && isAllLineSideRequest(issue);

		// 判断生产退料是否全部为零头（纯线边仓退料）
		boolean isReturnLineSideOnly = false;
		if (MPPCostCollector.COSTCOLLECTORTYPE_ProductionReturn.equals(costCollectorType)) {
			isReturnLineSideOnly = !hasReturnWarehouseQty(issue);
		}

		// 根据成本归集类型设置单据类型
		String docTypeName = null;
		if (isLineSideOnly) {
			docTypeName = "生产线边仓领料";
		} else if (MPPCostCollector.COSTCOLLECTORTYPE_ComponentIssue.equals(costCollectorType)
				|| OrderReceiptIssue.COSTCOLLECTORTYPE_PRODUCTION_ISSUE.equals(costCollectorType)) {
			docTypeName = "领料单";
		} else if (MPPCostCollector.COSTCOLLECTORTYPE_ProductionReturn.equals(costCollectorType)) {
			docTypeName = isReturnLineSideOnly ? "生产退料线边仓" : "退料单";
		} else if (MPPCostCollector.COSTCOLLECTORTYPE_ProductionReplenishment.equals(costCollectorType)) {
			docTypeName = "补领单";
		} else if (OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE.equals(costCollectorType)) {
			docTypeName = "委外领料单";
		} else if (OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN.equals(costCollectorType)) {
			docTypeName = "委外退料单";
		} else if (OrderReceiptIssue.COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT.equals(costCollectorType)) {
			docTypeName = "委外补领单";
		}

		if (docTypeName != null) {
			String sql = "SELECT C_DocType_ID FROM C_DocType " + "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,?) "
					+ "AND Name=? AND IsActive='Y' " + "ORDER BY IsDefault DESC, AD_Org_ID DESC";
			int docTypeId = DB.getSQLValue(order.get_TrxName(), sql, order.getAD_Client_ID(), order.getAD_Org_ID(),
					docTypeName);
			if (docTypeId > 0) {
				mr.setC_DocType_ID(docTypeId);
			} else {
				log.warning("未找到单据类型: " + docTypeName);
			}
		}

		mr.saveEx(order.get_TrxName());

		try {
			// createIssue 末尾，mr 已 saveEx 并完成工作流后
			MTable table = MTable.get(Env.getCtx(), "PP_Material_Requisition");
			int tableId = table != null ? table.getAD_Table_ID() : 0;
			if (tableId > 0) {
				if (lastProcessInfo == null) {
					lastProcessInfo = new ProcessInfo("领退料申请结果", 0);
				}
				lastProcessInfo.addLog(mr.get_ID(), // P_ID
						null, // P_Date
						null, // P_Number
						"已创建" + (docTypeName != null ? docTypeName : "单据") + ": " + mr.getDocumentNo(), // P_Msg
						tableId, // AD_Table_ID → 触发 DocumentLink
						mr.get_ID() // Record_ID
				);
			}
		} catch (Exception e) {

		}

		ArrayList[][] m_issue = new ArrayList[issue.getRowCount()][1];

		int row = 0;
		for (int i = 0; i < issue.getRowCount(); i++) {
			ArrayList<Object> data = new ArrayList<Object>();
			IDColumn id = (IDColumn) issue.getValueAt(i, 0);
			KeyNamePair key = new KeyNamePair(id.getRecord_ID(), id.isSelected() ? "Y" : "N");
			data.add(key); // 0 - ID

			boolean isCritical = id.isSelected();
			data.add(isCritical); // 1 - IsCritical (Boolean类型)

			data.add(issue.getValueAt(i, 1)); // 2 - 物料编码
			data.add(issue.getValueAt(i, 2)); // 3 - KeyNamePair 物料
			// 纯线边仓时用量来自线边仓申请数量（列9），否则来自仓库申请数量（列8）
			data.add(isLineSideOnly ? getValueBigDecimal(issue, i, 9) : getValueBigDecimal(issue, i, 8)); // 4 - 领取数量
			data.add(getValueBigDecimal(issue, i, 5)); // 5 - 批次
			data.add(getValueBigDecimal(issue, i, 9)); // 6 - 线边仓申请数量

			m_issue[row][0] = data;
			row++;
		}

		isCompleteQtyDeliver = MPPOrder.isQtyAvailable(order, m_issue, minGuaranteeDate);
		for (int i = 0; i < m_issue.length; i++) {
			KeyNamePair dataKey = (KeyNamePair) m_issue[i][0].get(0);
			if (dataKey == null) {
				continue;
			}

			boolean isSelected = dataKey.getName().equals("Y");
			if (!isSelected) {
				continue;
			}

			Object criticalObj = m_issue[i][0].get(1);
			Boolean isCritical = false;
			if (criticalObj instanceof Boolean) {
				isCritical = (Boolean) criticalObj;
			} else if (criticalObj instanceof String) {
				isCritical = "Y".equals(criticalObj) || "true".equalsIgnoreCase((String) criticalObj);
			}

			Object valueObj = m_issue[i][0].get(2);
			String value = "";
			if (valueObj instanceof String) {
				value = (String) valueObj;
			} else if (valueObj != null) {
				value = valueObj.toString();
			}

			Object productObj = m_issue[i][0].get(3);
			int M_Product_ID = 0;
			KeyNamePair productkey = null;
			if (productObj instanceof KeyNamePair) {
				productkey = (KeyNamePair) productObj;
				M_Product_ID = productkey.getKey();
			} else {
				continue;
			}

			MPPOrderBOMLine orderbomLine = null;
			int PP_Order_BOMLine_ID = 0;
			int M_AttributeSetInstance_ID = 0;

			Object qtyObj = m_issue[i][0].get(4);
			BigDecimal qtyToDeliver = Env.ZERO;
			if (qtyObj instanceof BigDecimal) {
				qtyToDeliver = (BigDecimal) qtyObj;
			} else if (qtyObj instanceof String) {
				try {
					qtyToDeliver = new BigDecimal((String) qtyObj);
				} catch (NumberFormatException e) {
					log.warning("领取数量格式错误: " + qtyObj);
					qtyToDeliver = Env.ZERO;
				}
			} else if (qtyObj instanceof Number) {
				qtyToDeliver = new BigDecimal(qtyObj.toString());
			}

			Object batchObj = m_issue[i][0].get(5);
			String batchInfo = "";
			if (batchObj != null) {
				batchInfo = batchObj.toString();
			}

			MProduct product = MProduct.get(order.getCtx(), M_Product_ID);
			if (product != null && product.get_ID() != 0 && product.isStocked()) {

				if (value == null && isSelected) {
					M_AttributeSetInstance_ID = (Integer) dataKey.getKey();
					orderbomLine = MPPOrderBOMLine.forM_Product_ID(Env.getCtx(), order.get_ID(), M_Product_ID,
							order.get_TrxName());
					if (orderbomLine != null) {
						PP_Order_BOMLine_ID = orderbomLine.get_ID();
					}
				} else if (value != null && isSelected) {
					PP_Order_BOMLine_ID = (Integer) dataKey.getKey();
					if (PP_Order_BOMLine_ID > 0) {
						orderbomLine = new MPPOrderBOMLine(order.getCtx(), PP_Order_BOMLine_ID, order.get_TrxName());
						M_AttributeSetInstance_ID = orderbomLine.getM_AttributeSetInstance_ID();
					}
				}

				// 获取当前选择的库位ID（声明为 final）
				final int locatorId;
				if (this instanceof WOrderReceiptIssue) {
					locatorId = ((WOrderReceiptIssue) this).getM_Locator_ID();
				} else {
					locatorId = 0;
				}

				// 库位选择逻辑：生产退料使用用户选择的库位，领料/补领自动选择
				MStorageOnHand[] storages;
				boolean draftOnly;

				// 判断是否发生了替代（列16有主料信息则说明当前行已被替代）
				Object mainProductInfo = issue.getValueAt(i, 16);
				boolean isSubstituted = mainProductInfo != null && !mainProductInfo.toString().isEmpty();
				int substituteProductId = isSubstituted ? M_Product_ID : 0;

				if (MPPCostCollector.COSTCOLLECTORTYPE_ProductionReturn.equals(costCollectorType)) {

					// 生产退料：UI已按最小包装拆分，列8=仓库退料，列9=线边仓退料，此处直接读取不再重复计算
					BigDecimal warehouseQty = qtyToDeliver; // 列8，已是整包装退仓库数量

					BigDecimal lineSideQty = getValueBigDecimal(issue, i, 9); // 列9，已是零头退线边仓数量

						// 物料分组不启用线边仓时，零头也归入仓库
						if (!isLineSideWarehouseProduct(M_Product_ID)) {
							warehouseQty = warehouseQty.add(lineSideQty);
							lineSideQty = Env.ZERO;
						}

					// 零头暂存到线边仓退料Map
					if (lineSideQty.compareTo(Env.ZERO) > 0) {
						returnLineSideMap.put(PP_Order_BOMLine_ID,
								new BigDecimal[]{lineSideQty, new BigDecimal(M_Product_ID)});
					}

					// 仓库部分：沿用现有逻辑
					if (warehouseQty.compareTo(Env.ZERO) > 0) {
						if (locatorId <= 0) {
							throw new AdempiereException("生产退料必须选择库位");
						}
						final int finalM_AttributeSetInstance_ID = M_AttributeSetInstance_ID;
						storages = new MStorageOnHand[] { new MStorageOnHand(Env.getCtx(), 0, order.get_TrxName()) {
							public int getM_Locator_ID() {
								return locatorId;
							}

							public int getM_AttributeSetInstance_ID() {
								return finalM_AttributeSetInstance_ID;
							}

							public BigDecimal getQtyOnHand() {
								return Env.ZERO;
							}
						} };
						draftOnly = true; //
						MPPOrder.createIssue(order, PP_Order_BOMLine_ID, movementDate, warehouseQty, Env.ZERO, Env.ZERO,
								storages, false, costCollectorType, nodeId, resourceId, draftOnly, locatorId,
								substituteProductId);
					} else if (isReturnLineSideOnly && lineSideQty.compareTo(Env.ZERO) > 0) {
							// 纯线边仓退料：全部退到线边仓库位，使用14参方法(退料专用)
							int lineSideLocatorId = getLineSideLocatorId(order.get_TrxName(), order.getAD_Org_ID());
							if (lineSideLocatorId <= 0) {
								throw new AdempiereException("未找到线边仓库位，无法退料");
							}
							draftOnly = true;
							MPPOrder.createIssue(order, PP_Order_BOMLine_ID, movementDate, lineSideQty, Env.ZERO, Env.ZERO,
									null, false, costCollectorType, nodeId, resourceId, draftOnly, lineSideLocatorId,
									substituteProductId);
						}
				} else {
					// 领料/补领：纯线边仓领料使用线边仓库位，普通领料使用仓库所有库位
					if (isLineSideOnly) {
						storages = getLineSideStorages(M_Product_ID, order.get_TrxName(), order.getAD_Org_ID());
					} else {

						storages = MPPOrder.getStorages(Env.getCtx(), M_Product_ID, order.getM_Warehouse_ID(),
								M_AttributeSetInstance_ID, minGuaranteeDate, order.get_TrxName());

						// 过滤掉待检库位类型的库存，来料检验待检区不允许用于领料
						storages = filterReservedLocator(storages, order.getM_Warehouse_ID(), order.get_TrxName());

						// 过滤后检查库存是否足够
						BigDecimal availableQty = Env.ZERO;
						for (MStorageOnHand soh : storages) {
							availableQty = availableQty.add(soh.getQtyOnHand());
						}
						if (availableQty.compareTo(qtyToDeliver) < 0) {
                            throw new AdempiereException("物料【" + product.getName() + "】库存不足（已排除待检库位），可用: "
                                    + availableQty + "，需要: " + qtyToDeliver);
						}
					}
					draftOnly = true; // 仅保存草稿，由MR的processIt统一驱动完成
					// 调用12参数方法创建明细
					MPPOrder.createIssue(order, PP_Order_BOMLine_ID, movementDate, qtyToDeliver, Env.ZERO, Env.ZERO,
							storages, false, costCollectorType, nodeId, resourceId, draftOnly, substituteProductId);
				}

			}
		}

		// 修改关联明细的查询条件，增加时间过滤
		List<MPPCostCollector> list = new Query(Env.getCtx(), MPPCostCollector.Table_Name,
				"PP_Order_ID=? AND DocStatus='DR' AND Created>=?", order.get_TrxName())
				.setParameters(order.get_ID(), mr.getCreated()).list();
		log.info("关联主表前 Draft 明细数量: " + list.size());
		for (MPPCostCollector cc : list) {
			cc.setPP_Material_Requisition_ID(mr.getPP_Material_Requisition_ID());
			cc.setC_Activity_ID(activityId);
			cc.saveEx(order.get_TrxName());
		}
		log.info("关联主表完成");
		if (isSubcontracting) {
			for (MPPCostCollector cc : list) {
				cc.setIsSubcontracting(true);

				cc.saveEx(order.get_TrxName());
			}
		}

			// 统一走审批工作流（线边仓也要审批），委外业务保持草稿
			if (!isSubcontracting) {
				try {
					mr.load(order.get_TrxName());
					ProcessInfo info = MWorkflow.runDocumentActionWorkflow(mr, DocAction.ACTION_Complete);
					if (info.isError()) {
						log.warning("领退料单工作流启动失败: " + info.getSummary());
					} else {
						mr.load(order.get_TrxName());
						log.info("单据[" + mr.getDocumentNo() + "]已提交审批，状态: " + mr.getDocStatus());
					}
				} catch (Exception e) {
					log.severe("领退料单工作流异常: " + e.getMessage());
				}
			} else {
				// 委外业务保持草稿状态，不发起审批
				log.info("委外单据[" + mr.getDocumentNo() + "]已创建，保持草稿状态");
			}

		    // === 线边仓额外处理：有线边仓申请数量时创建线边仓领料单 ===
		    if (isProductionIssue(costCollectorType) && !isLineSideOnly && hasLineSideQuantities(issue)) {
			    log.info("检测到线边仓申请数量，创建线边仓领料单");
			    MPPMaterialRequisition mrLineSide = new MPPMaterialRequisition(Env.getCtx(), 0, order.get_TrxName());
			    mrLineSide.setAD_Org_ID(order.getAD_Org_ID());
			    mrLineSide.setPP_Order_ID(order.get_ID());
			    mrLineSide.setM_Product_ID(order.getM_Product_ID());
			    mrLineSide.setMovementDate(movementDate);
			    mrLineSide.setPP_Order_Node_ID(nodeId);
			    mrLineSide.setS_Resource_ID(resourceId);
			    mrLineSide.setM_Warehouse_ID(order.getM_Warehouse_ID());
			    mrLineSide.set_ValueOfColumn("C_Activity_ID", activityId);
			    mrLineSide.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
			    mrLineSide.setDocAction(MPPOrder.DOCACTION_Complete);
			    setDocTypeByName(mrLineSide, "生产线边仓领料", order);
			    mrLineSide.saveEx(order.get_TrxName());
			    Timestamp mrLineSideCreated = mrLineSide.getCreated();
			    
			    // 为每行线边仓数量创建成本归集
			    for (int k = 0; k < m_issue.length; k++) {
				    KeyNamePair dk = (KeyNamePair) m_issue[k][0].get(0);
				    if (dk == null || !dk.getName().equals("Y")) continue;
				    BigDecimal lsQty = (BigDecimal) m_issue[k][0].get(6);
				    if (lsQty == null || lsQty.compareTo(Env.ZERO) <= 0) continue;
				    KeyNamePair pk = (KeyNamePair) m_issue[k][0].get(3);
				    if (pk == null) continue;
				    int pid = pk.getKey();

						    // 解析BOM行ID（复用主循环逻辑）
						    Object vObj2 = m_issue[k][0].get(2);
						    String v2 = (vObj2 instanceof String) ? (String) vObj2 : null;
						    int oblId = 0;
						    if (v2 == null) {
							    MPPOrderBOMLine obl2 = MPPOrderBOMLine.forM_Product_ID(
							    	Env.getCtx(), order.get_ID(), pid, order.get_TrxName());
							    if (obl2 != null) oblId = obl2.get_ID();
						    } else {
							    oblId = dk.getKey();
						    }

						    MStorageOnHand[] lsStorages = getLineSideStorages(pid, order.get_TrxName(), order.getAD_Org_ID());
						    MPPOrder.createIssue(order, oblId, movementDate, lsQty, Env.ZERO, Env.ZERO,
						    	lsStorages, false, costCollectorType, nodeId, resourceId, true, 0);
			    }
			    
			    // 关联线边仓成本归集
			    List<MPPCostCollector> lsList = new Query(Env.getCtx(), MPPCostCollector.Table_Name,
				    "PP_Order_ID=? AND DocStatus='DR' AND Created>=?", order.get_TrxName())
				    .setParameters(order.get_ID(), mrLineSideCreated).list();
			    for (MPPCostCollector cc : lsList) {
				    cc.setPP_Material_Requisition_ID(mrLineSide.getPP_Material_Requisition_ID());
				    cc.setC_Activity_ID(activityId);
				    cc.saveEx(order.get_TrxName());
			    }
			    
				    // 线边仓领料单发起审批工作流
				    try {
					    mrLineSide.load(order.get_TrxName());
					    ProcessInfo infoLS = MWorkflow.runDocumentActionWorkflow(mrLineSide, DocAction.ACTION_Complete);
					    if (infoLS.isError()) {
						    log.warning("线边仓领料单工作流启动失败: " + infoLS.getSummary());
					    } else {
						    mrLineSide.load(order.get_TrxName());
						    log.info("线边仓领料单[" + mrLineSide.getDocumentNo() + "]已提交审批，状态: " + mrLineSide.getDocStatus());
					    }
					    // 返回线边仓领料单链接
					    MTable tableLS = MTable.get(Env.getCtx(), "PP_Material_Requisition");
					    int tableIdLS = tableLS != null ? tableLS.getAD_Table_ID() : 0;
					    if (tableIdLS > 0 && lastProcessInfo != null) {
						    lastProcessInfo.addLog(mrLineSide.get_ID(), null, null,
							    "已创建线边仓领料: " + mrLineSide.getDocumentNo(),
							    tableIdLS, mrLineSide.get_ID());
					    }
				    } catch (Exception e) {
					    log.severe("线边仓领料单异常: " + e.getMessage());
				    }
		    }
			// === 生产退料线边仓额外处理：有零头时创建线边仓退料单 ===
			if (MPPCostCollector.COSTCOLLECTORTYPE_ProductionReturn.equals(costCollectorType)
					&& !isReturnLineSideOnly && !returnLineSideMap.isEmpty()) {
				log.info("检测到退料零头，创建生产退料线边仓单");

				MPPMaterialRequisition mrReturnLineSide = new MPPMaterialRequisition(Env.getCtx(), 0, order.get_TrxName());
				mrReturnLineSide.setAD_Org_ID(order.getAD_Org_ID());
				mrReturnLineSide.setPP_Order_ID(order.get_ID());
				mrReturnLineSide.setM_Product_ID(order.getM_Product_ID());
				mrReturnLineSide.setMovementDate(movementDate);
				mrReturnLineSide.setPP_Order_Node_ID(nodeId);
				mrReturnLineSide.setS_Resource_ID(resourceId);
				mrReturnLineSide.setM_Warehouse_ID(order.getM_Warehouse_ID());
				mrReturnLineSide.set_ValueOfColumn("C_Activity_ID", activityId);
				mrReturnLineSide.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
				mrReturnLineSide.setDocAction(MPPOrder.DOCACTION_Complete);
				setDocTypeByName(mrReturnLineSide, "生产退料线边仓", order);
				mrReturnLineSide.saveEx(order.get_TrxName());
				Timestamp mrReturnLineSideCreated = mrReturnLineSide.getCreated();

				// 为每条退料零头创建成本归集
				for (Map.Entry<Integer, BigDecimal[]> entry : returnLineSideMap.entrySet()) {
					int oblId = entry.getKey();
					BigDecimal lsQty = entry.getValue()[0];
					int pid = entry.getValue()[1].intValue();

				int lineSideLocatorId = getLineSideLocatorId(order.get_TrxName(), order.getAD_Org_ID());
					if (lineSideLocatorId > 0) {
						MPPOrder.createIssue(order, oblId, movementDate, lsQty, Env.ZERO, Env.ZERO,
								null, false, costCollectorType, nodeId, resourceId, true, lineSideLocatorId, 0);
					}
				}

				// 关联线边仓退料成本归集
				List<MPPCostCollector> lsReturnList = new Query(Env.getCtx(), MPPCostCollector.Table_Name,
						"PP_Order_ID=? AND DocStatus='DR' AND Created>=?", order.get_TrxName())
						.setParameters(order.get_ID(), mrReturnLineSideCreated).list();
				for (MPPCostCollector cc : lsReturnList) {
					cc.setPP_Material_Requisition_ID(mrReturnLineSide.getPP_Material_Requisition_ID());
					cc.setC_Activity_ID(activityId);
					cc.saveEx(order.get_TrxName());
				}

					// 线边仓退料单发起审批工作流
					try {
						mrReturnLineSide.load(order.get_TrxName());
						ProcessInfo infoRLS = MWorkflow.runDocumentActionWorkflow(mrReturnLineSide, DocAction.ACTION_Complete);
						if (infoRLS.isError()) {
							log.warning("生产退料线边仓单工作流启动失败: " + infoRLS.getSummary());
						} else {
							mrReturnLineSide.load(order.get_TrxName());
							log.info("生产退料线边仓单[" + mrReturnLineSide.getDocumentNo() + "]已提交审批，状态: " + mrReturnLineSide.getDocStatus());
						}
						// 返回生产退料线边仓单链接
						MTable tableRLS = MTable.get(Env.getCtx(), "PP_Material_Requisition");
						int tableIdRLS = tableRLS != null ? tableRLS.getAD_Table_ID() : 0;
						if (tableIdRLS > 0 && lastProcessInfo != null) {
							lastProcessInfo.addLog(mrReturnLineSide.get_ID(), null, null,
								"已创建线边仓退料: " + mrReturnLineSide.getDocumentNo(),
								tableIdRLS, mrReturnLineSide.get_ID());
						}
					} catch (Exception e) {
						log.severe("生产退料线边仓单异常: " + e.getMessage());
					}
			}
		}
	private void validateQuantities(IMiniTable issue, String costCollectorType) {

		// 读取系统配置：Y=限制超领（默认），N=允许超领
		boolean restrictOverIssue = MSysConfig.getBooleanValue(SYSCONFIG_RESTRICT_OVER_ISSUE, true,
				Env.getAD_Client_ID(Env.getCtx()));

		for (int i = 0; i < issue.getRowCount(); i++) {
			IDColumn id = (IDColumn) issue.getValueAt(i, 0);
			if (id == null || !id.isSelected()) {
				continue;
			}

			BigDecimal qtyRequired = getValueBigDecimal(issue, i, 6);
			BigDecimal qtyDelivered = getValueBigDecimal(issue, i, 7);
			BigDecimal qtyToDeliver = getValueBigDecimal(issue, i, 8);
			BigDecimal qtyOnHand = getValueBigDecimal(issue, i, 10);

			KeyNamePair productKey = (KeyNamePair) issue.getValueAt(i, 2);
			String productName = productKey != null ? productKey.getName() : "未知物料";

			if (COSTCOLLECTORTYPE_PRODUCTION_ISSUE.equals(costCollectorType)) {
				BigDecimal maxAllowedQty = qtyRequired.subtract(qtyDelivered);
				if (maxAllowedQty.compareTo(BigDecimal.ZERO) < 0) {
					maxAllowedQty = BigDecimal.ZERO;
				}

//	            if (qtyToDeliver.compareTo(maxAllowedQty) > 0) {  
//	                throw new IllegalArgumentException("物料【" + productName + "】的领取数量"  +   
//	                    "不能超过需求数量减去已领数量的数量");  
//	            }  

				if (restrictOverIssue && qtyToDeliver.compareTo(qtyOnHand) > 0) {
					throw new IllegalArgumentException("物料【" + productName + "】的库存数量不足");
				}

			} else if (COSTCOLLECTORTYPE_PRODUCTION_RETURN.equals(costCollectorType)) {
				if (qtyToDeliver.compareTo(qtyDelivered) > 0) {
					throw new IllegalArgumentException("物料【" + productName + "】的退料数量不能超过已领数量");
				}

			} else if (COSTCOLLECTORTYPE_PRODUCTION_REPLENISHMENT.equals(costCollectorType)) {
				if (restrictOverIssue && qtyToDeliver.compareTo(qtyOnHand) > 0) {
					throw new IllegalArgumentException("物料【" + productName + "】的补领数量超过库存数量");
				}
			}
		}
	}

	public void setNodeFilter(int nodeId) {
		m_nodeFilter = nodeId;
	}

	public void executeQuery(IMiniTable issue) {
		// 改为 StringBuilder 动态拼接
		StringBuilder sqlBuilder = new StringBuilder("SELECT " + "obl.PP_Order_BOMLine_ID," // 1 - ID
				+ "obl.IsCritical," // 2 - 是否关键部件
				+ "p.Value," // 3 - 物料编码
				+ "obl.M_Product_ID,p.Name," // 4,5 - 物料ID和名称
				+ "p.C_UOM_ID,u.Name," // 6,7 - 单位ID和名称
				+ "obl.QtyRequiered," // 8 - 需求数量
				+ "obl.QtyDelivered," // 9 - 已领数量
				+ "obl.QtyBom," // 10 - bom数量
				+ "obl.QtyReserved," // 11 - 预留数量
				+ "bomQtyAvailable(obl.M_Product_ID,obl.M_Warehouse_ID,0) AS QtyAvailable," // 12 - 可用数量
				+ "bomQtyOnHand(obl.M_Product_ID,obl.M_Warehouse_ID,0) AS QtyOnHand," // 13 - 库存数量
				+ "obl.M_Warehouse_ID,w.Name," // 14,15 - 仓库ID和名称
				+ "obl.ComponentType," // 16 - 组件类型
				+ "p.UnitsPerPack," // 17 - 包装数量（不显示在表格中）
				+ "COALESCE((SELECT SUM(soh.QtyOnHand) " // 18 - 线边仓库存
				+ "FROM M_StorageOnHand soh " + "JOIN M_Locator loc ON soh.M_Locator_ID = loc.M_Locator_ID "
				+ "JOIN M_LocatorType lt ON loc.M_LocatorType_ID = lt.M_LocatorType_ID "
				+ "JOIN M_Warehouse wh ON loc.M_Warehouse_ID = wh.M_Warehouse_ID "
				+ "JOIN M_Product_Category pc ON p.M_Product_Category_ID_L2 = pc.M_Product_Category_ID "
				+ "WHERE soh.M_Product_ID = obl.M_Product_ID " + "AND lt.Name = '生产线边仓' " + "AND loc.IsActive = 'Y' " + "AND pc.Is_Line_Side_Warehouse = 'Y' "
				+ "AND wh.AD_Org_ID = w.AD_Org_ID "
				+ "), 0) AS LineSideOnHand," // 线边仓库存
				+ "p.Specification" // 规格
				+ " FROM PP_Order_BOMLine obl" + " INNER JOIN M_Product p ON (obl.M_Product_ID = p.M_Product_ID) "
				+ " INNER JOIN C_UOM u ON (p.C_UOM_ID = u.C_UOM_ID) "
				+ " INNER JOIN M_Warehouse w ON (w.M_Warehouse_ID = obl.M_Warehouse_ID) "
				+ " WHERE obl.PP_Order_ID = ?");

		// 新增：根据 m_fulfilledFilter 追加过滤条件
		if ("Y".equals(m_fulfilledFilter)) {
			sqlBuilder.append(" AND obl.QtyDelivered >= obl.QtyRequiered");
		} else if ("N".equals(m_fulfilledFilter)) {
			sqlBuilder.append(" AND obl.QtyDelivered < obl.QtyRequiered");
		}
		// ↓ 新增：工序过滤
		if (m_nodeFilter > 0) {
			sqlBuilder.append(" AND (obl.PP_Order_Node_ID = ").append(m_nodeFilter)
					.append(" OR obl.PP_Order_Node_ID IS NULL").append(" OR obl.PP_Order_Node_ID = 0)");
		}
		sqlBuilder.append(" ORDER BY obl.").append(MPPOrderBOMLine.COLUMNNAME_Line);
		String sql = sqlBuilder.toString();

		// reset table
		int row = 0;
		issue.setRowCount(row);
		// Execute
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, getPP_Order_ID());
			rs = pstmt.executeQuery();

			// 添加委外工单判断
			boolean isSubcontractingOrder = false;
			if (getPP_Order() != null) {
				MDocType docType = MDocType.get(Env.getCtx(), getPP_Order().getC_DocTypeTarget_ID());
				isSubcontractingOrder = docType != null && docType.getName().contains("委外工单");
			}

			while (rs.next()) {
				// extend table
				issue.setRowCount(row + 1);

				// 获取数据
				int ppOrderBOMLineId = rs.getInt(1);
				boolean isCritical = "Y".equals(rs.getString(2));
				String productValue = rs.getString(3);
				int productId = rs.getInt(4);
				String productName = rs.getString(5);
				int uomId = rs.getInt(6);
				String uomName = rs.getString(7);
				BigDecimal qtyRequired = rs.getBigDecimal(8);
				BigDecimal qtyDelivered = rs.getBigDecimal(9);
				BigDecimal qtyBom = rs.getBigDecimal(10);
				BigDecimal qtyReserved = rs.getBigDecimal(11);
				BigDecimal qtyAvailable = rs.getBigDecimal(12);
				BigDecimal qtyOnHand = rs.getBigDecimal(13);
				int warehouseId = rs.getInt(14);
				String warehouseName = rs.getString(15);
				BigDecimal unitsPerPack = rs.getBigDecimal(17); // 获取包装数量
					if (unitsPerPack == null || unitsPerPack.compareTo(BigDecimal.ZERO) <= 0)
						unitsPerPack = BigDecimal.ONE;
				BigDecimal lineSideOnHand = rs.getBigDecimal(18); // 获取线边仓库存
				if (lineSideOnHand == null)
					lineSideOnHand = Env.ZERO;

				String specification = rs.getString(19); // 规格，SQL最后一列

				// 委外工单或OnlyIssue：根据具体类型计算领退数量
				BigDecimal qtyToDeliver;
				if (isSubcontractingOrder) {
					// 委外工单：根据成本归集类型计算
					if (isSubcontractingReturn()) {
						// 委外退料：使用简单减法
						qtyToDeliver = qtyRequired.subtract(qtyDelivered);
						if (qtyToDeliver.compareTo(Env.ZERO) < 0) {
							qtyToDeliver = Env.ZERO;
						}
					} else {
						// 委外发料和委外补领：使用包装规格计算
						qtyToDeliver = calculateIssueQty(qtyRequired, qtyDelivered, unitsPerPack);
					}
				} else if (!isOnlyIssue()) {

					qtyToDeliver = qtyRequired.subtract(qtyDelivered);
					if (qtyToDeliver.compareTo(Env.ZERO) < 0) {
						qtyToDeliver = Env.ZERO;
					}
				} else {
					// 生产领料和补料使用包装规格计算
					qtyToDeliver = calculateIssueQty(qtyRequired, qtyDelivered, unitsPerPack);
				}
				// 创建IDColumn
				IDColumn id = new IDColumn(ppOrderBOMLineId);
				id.setSelected(isCritical); // 关键部件默认选中

				// 按照新的列顺序设置数据
				issue.setValueAt(id, row, 0); // 关键部件（复选框）
				issue.setValueAt(productValue, row, 1); // 物料编码
				issue.setValueAt(new KeyNamePair(productId, productName), row, 2); // 物料
				issue.setValueAt(specification, row, 3); // 规格
				issue.setValueAt(new KeyNamePair(uomId, uomName), row, 4); // 单位
				issue.setValueAt("", row, 5); // 批次（暂时留空）
				issue.setValueAt(qtyRequired, row, 6); // 需求数量
				issue.setValueAt(qtyOnHand, row, 10); // 仓库库存
				issue.setValueAt(qtyReserved, row, 11); // 预留数量
				issue.setValueAt(qtyAvailable, row, 12); // 可用数量
				issue.setValueAt(new KeyNamePair(warehouseId, warehouseName), row, 13); // 仓库
				issue.setValueAt(qtyBom, row, 14); // bom数量
				// === 线边仓三段式逻辑 ===
				BigDecimal lineSideRequestQty = Env.ZERO; // 线边仓申请数量（列13）
				BigDecimal warehouseRequestQty = qtyToDeliver; // 仓库申请数量（列7），默认保持原值
				BigDecimal remainingQty = qtyRequired.subtract(qtyDelivered);
				if (remainingQty.compareTo(Env.ZERO) < 0)
					remainingQty = Env.ZERO;

					if (isProductionReturn()) {
						// 生产退料：不自动计算，等待用户手动输入仓库申请数量
						warehouseRequestQty = Env.ZERO;
						lineSideRequestQty = Env.ZERO;
					} else if (isProductionReplenishment()) {
						// 生产补领：不计算线边仓，仓库 = 取整(需求 - 已领)
						warehouseRequestQty = calculateIssueQty(qtyRequired, qtyDelivered, unitsPerPack);
					} else if (lineSideOnHand.compareTo(Env.ZERO) > 0) {
						if (lineSideOnHand.compareTo(remainingQty) >= 0) {
							// 场景B：线边仓库存 >= 需求，全部从线边仓领
							lineSideRequestQty = remainingQty;
							warehouseRequestQty = Env.ZERO;
						} else {
							// 场景C：0 < 线边仓库存 < 需求，线边仓全领 + 仓库补足
							lineSideRequestQty = lineSideOnHand;
							warehouseRequestQty = calculateIssueQty(qtyRequired, qtyDelivered.add(lineSideOnHand), unitsPerPack);
						}
					}
					// 线边仓库存不满足最小包装数时，不扣线边仓，全部从仓库领
					if (lineSideRequestQty.compareTo(Env.ZERO) > 0 && lineSideRequestQty.compareTo(unitsPerPack) < 0) {
						warehouseRequestQty = calculateIssueQty(qtyRequired, qtyDelivered, unitsPerPack);
						lineSideRequestQty = Env.ZERO;
					}
				// 场景A：线边=0 → lineSideRequestQty=0, warehouseRequestQty保持原值（已按包装取整）
				issue.setValueAt(lineSideRequestQty, row, 9); // 线边仓申请数量
				issue.setValueAt(lineSideOnHand, row, 15); // 线边仓库存
				issue.setValueAt(qtyDelivered, row, 7); // 已领数量 - 可编辑
				issue.setValueAt(warehouseRequestQty, row, 8); // 仓库申请数量

				row++;
			} // while
		} catch (SQLException e) {
			log.severe("执行查询时出错: " + e.getMessage());
			throw new DBException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		issue.autoSize();
	} // executeQuery

	public String generateSummaryTable(IMiniTable issue, String productField, String uomField, String attribute,
			String toDeliverQty, String deliveredQtyField, String scrapQtyField, boolean isBackflush,
			boolean isOnlyIssue, boolean isOnlyReceipt) {

		StringBuffer iText = new StringBuffer();

		iText.append("<b>");
		iText.append(Msg.translate(Env.getCtx(), "IsShipConfirm"));
		iText.append("</b>");
		iText.append("<br />");

		if (isOnlyReceipt || isBackflush) {
			String[][] table = { { "产品", "单位", "批次", "领取数量", "已领数量", "损耗率%" },
					{ productField, uomField, attribute, toDeliverQty, deliveredQtyField, scrapQtyField } };
			iText.append(createHTMLTable(table));
		}

		if (isBackflush || isOnlyIssue) {
			iText.append("<br /><br />");

			ArrayList<String[]> table = new ArrayList<String[]>();

			table.add(new String[] { "物料编码", // 0
					"物料", // 1
					"单位", // 2
					"批次", // 3
					"领取数量", // 4
					"已领数量", // 5
					"库存数量" // 6 - 新增库存数量列
			});

			// check available on hand
			for (int i = 0; i < issue.getRowCount(); i++) {
				IDColumn id = (IDColumn) issue.getValueAt(i, 0);
				if (id != null && id.isSelected()) {
					KeyNamePair m_productkey = (KeyNamePair) issue.getValueAt(i, 2);
					int m_M_Product_ID = m_productkey.getKey();
					KeyNamePair m_uomkey = (KeyNamePair) issue.getValueAt(i, 4);

					if (issue.getValueAt(i, 5) == null) // 批次为空
					{
						Timestamp m_movementDate = getMovementDate();
						Timestamp minGuaranteeDate = m_movementDate;
						MStorageOnHand[] storages = MPPOrder.getStorages(Env.getCtx(), m_M_Product_ID,
								getPP_Order().getM_Warehouse_ID(), 0, minGuaranteeDate, null);

						BigDecimal todelivery = getValueBigDecimal(issue, i, 7); // 领取数量
						BigDecimal toIssue = todelivery;
						for (MStorageOnHand storage : storages) {
							if (storage.getQtyOnHand().signum() == 0)
								continue;

							BigDecimal issueact = toIssue;
							if (issueact.compareTo(storage.getQtyOnHand()) > 0)
								issueact = storage.getQtyOnHand();
							toIssue = toIssue.subtract(issueact);

							String desc = new MAttributeSetInstance(Env.getCtx(),
									storage.getM_AttributeSetInstance_ID(), null).getDescription();

							String[] row = { "", "", "", "", "0.00", "0.00", "0.00" };
							row[0] = issue.getValueAt(i, 1) != null ? issue.getValueAt(i, 1).toString() : "";
							row[1] = m_productkey.toString();
							row[2] = m_uomkey != null ? m_uomkey.toString() : "";
							row[3] = desc != null ? desc : "";
							row[4] = issueact.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
							row[5] = getValueBigDecimal(issue, i, 7).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
							row[6] = getValueBigDecimal(issue, i, 10).toString(); // 库存数量
							table.add(row);

							if (toIssue.signum() <= 0)
								break;
						}
					} else // 批次不为空
					{
						String[] row = { "", "", "", "", "0.00", "0.00", "0.00" };
						row[0] = issue.getValueAt(i, 1) != null ? issue.getValueAt(i, 1).toString() : "";
						row[1] = m_productkey.toString();
						row[2] = m_uomkey != null ? m_uomkey.toString() : "";
						row[3] = issue.getValueAt(i, 5) != null ? issue.getValueAt(i, 5).toString() : "";
						row[4] = getValueBigDecimal(issue, i, 8).toString(); // 领取数量
						row[5] = getValueBigDecimal(issue, i, 7).toString(); // 已领数量
						row[6] = getValueBigDecimal(issue, i, 10).toString(); // 库存数量
						table.add(row);
					}
				}
			}

			String[][] tableArray = table.toArray(new String[table.size()][]);
			iText.append(createHTMLTable(tableArray));
		}

		return iText.toString();
	}

	protected BigDecimal getDeliveredQty() {
		return m_DeliveredQty;
	}

	protected int getM_AttributeSetInstance_ID() {
		return m_M_AttributeSetInstance_ID;
	}

	protected int getM_Locator_ID() {
		return m_M_Locator_ID;
	}

	protected Timestamp getMovementDate() {
		return m_movementDate;
	}

	protected BigDecimal getOpenQty() {
		return m_openQty;
	}

	protected BigDecimal getOrderedQty() {
		return m_orderedQty;
	}

	protected MPPOrder getPP_Order() {
		int id = getPP_Order_ID();
		if (id <= 0) {
			m_PP_order = null;
			return null;
		}
		if (m_PP_order == null || m_PP_order.get_ID() != id) {

			m_PP_order = new MPPOrder(Env.getCtx(), id, null);
		}
		return m_PP_order;
	}

	protected int getPP_Order_ID() {
		return m_PP_Order_ID;
	}

	protected BigDecimal getQtyBatchs() {
		return m_qtyBatchs;
	}

	protected BigDecimal getQtyBatchSize() {
		return m_qtyBatchSize;
	}

	protected BigDecimal getRejectQty() {
		return m_rejectQty;
	}

	protected BigDecimal getScrapQty() {
		return m_scrapQty;
	}

	protected BigDecimal getToDeliverQty() {
		return m_toDeliverQty;
	}

	// 修复String转BigDecimal的错误
	private BigDecimal getValueBigDecimal(IMiniTable issue, int row, int col) {
		Object value = issue.getValueAt(row, col);
		if (value == null) {
			return Env.ZERO;
		}

		// 如果已经是BigDecimal类型，直接返回
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}

		// 如果是String类型，尝试转换为BigDecimal
		if (value instanceof String) {
			try {
				String str = ((String) value).trim();
				if (str.isEmpty()) {
					return Env.ZERO;
				}
				return new BigDecimal(str);
			} catch (NumberFormatException e) {
				log.warning("无法将字符串转换为BigDecimal: " + value);
				return Env.ZERO;
			}
		}

		// 如果是其他数字类型，尝试转换
		if (value instanceof Number) {
			try {
				return new BigDecimal(value.toString());
			} catch (Exception e) {
				log.warning("无法将数值转换为BigDecimal: " + value);
				return Env.ZERO;
			}
		}

		// 其他类型，尝试转换
		try {
			return new BigDecimal(value.toString());
		} catch (Exception e) {
			log.warning("无法转换为BigDecimal: " + value);
			return Env.ZERO;
		}
	}

	protected boolean isBackflush() {
		return m_IsBackflush;
	}

	/**
	 * Determines whether the Delivery Rule is set to 'OnlyIssue'
	 * 
	 * @return
	 */
	protected boolean isOnlyIssue() {
		return m_OnlyIssue;
	}

	/**
	 * Determines whether the Delivery Rule is set to 'OnlyReciept'
	 * 
	 * @return
	 */
	protected boolean isOnlyReceipt() {
		return m_isOnlyReceipt;
	}

	/**
	 * Adds Attribute Set Instances Quantities to table. Extension to
	 * {@link #executeQuery()}
	 * 
	 * @return how many lines were added
	 */

	/**
	 * 校验工单状态是否允许进行领退料操作
	 * 
	 * @param order 制造工单
	 */
	private void validateOrderStatusForIssue(MPPOrder order, boolean isReturnOperation) {
		// 退料操作允许进行，其他操作需要检查工单状态
		if (!isReturnOperation) {
			// 检查自定义状态字段
			String orderStatus = (String) order.get_Value("Orderstatus");
			if ("InECNChange".equals(orderStatus) || "ChangeExecuted".equals(orderStatus)) {
				throw new AdempiereException("工单处于变更中或已变更状态，无法进行领料操作");
			}
		}

		// 检查标准文档状态
		String docStatus = order.getDocStatus();
		if (MPPOrder.DOCSTATUS_InProgress.equals(docStatus)) {
			log.info("工单状态: " + docStatus);
		}
	}

	/**
	 * Save Selection & return selecion Query or ""
	 * 
	 * @return where clause like C_Order_ID IN (...)
	 */
	public void saveSelection(IMiniTable miniTable) {
		log.info("");
		// Array of Integers
		ArrayList<Integer> results = new ArrayList<Integer>();
		setSelection(null);

		// Get selected entries
		int rows = miniTable.getRowCount();
		for (int i = 0; i < rows; i++) {
			IDColumn id = (IDColumn) miniTable.getValueAt(i, 0); // ID in column
																	// 0
			// log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected())
				results.add(id.getRecord_ID());
		}

		if (results.size() == 0)
			return;
		log.config("Selected #" + results.size());
		setSelection(results);
	} // saveSelection

	protected void setDeliveredQty(BigDecimal qty) {
		m_DeliveredQty = qty;
	}

	protected void setIsBackflush(boolean IsBackflush) {
		m_IsBackflush = IsBackflush;
	}

	protected void setIsOnlyIssue(boolean onlyIssue) {
		m_OnlyIssue = onlyIssue;
	}

	protected void setIsOnlyReceipt(boolean isOnlyReceipt) {
		m_isOnlyReceipt = isOnlyReceipt;
	}

	protected void setM_AttributeSetInstance_ID(int M_AttributeSetInstance_ID) {
		m_M_AttributeSetInstance_ID = M_AttributeSetInstance_ID;
	}

	protected void setM_Locator_ID(int M_Locator_ID) {
		m_M_Locator_ID = M_Locator_ID;
	}

	protected void setMovementDate(Timestamp date) {
		m_movementDate = date;
	}

	protected void setOpenQty(BigDecimal qty) {
		m_openQty = qty;
	}

	protected void setOrderedQty(BigDecimal qty) {
		m_orderedQty = qty;
	}

	protected void setPP_Order_ID(int PP_Order_ID) {
		m_PP_Order_ID = PP_Order_ID;
	}

	protected void setQtyBatchs(BigDecimal qty) {
		m_qtyBatchs = qty;
	}

	protected void setQtyBatchSize(BigDecimal qty) {
		m_qtyBatchSize = qty;
	}

	protected void setRejectQty(BigDecimal qty) {
		m_rejectQty = qty;
	}

	protected void setScrapQty(BigDecimal qty) {
		m_scrapQty = qty;
	}

	protected void setToDeliverQty(BigDecimal qty) {
		m_toDeliverQty = qty;
	}

	public void showMessage(String message, boolean error) {
	}

	/**
	 * 判断是否为退料操作
	 */
	private boolean isReturnOperation(IMiniTable issue, String costCollectorType) {
		if (costCollectorType == COSTCOLLECTORTYPE_PRODUCTION_RETURN) {
			return true;
		}
		return false;
	}

	/**
	 * 根据包装规格计算向上取整的最大值
	 * 
	 * @param requiredQty  需求数量
	 * @param unitsPerPack 包装数量
	 * @return 向上取整的最大值
	 */
	protected BigDecimal calculateMaxQtyWithPack(BigDecimal requiredQty, BigDecimal unitsPerPack) {
		if (unitsPerPack == null || unitsPerPack.compareTo(Env.ZERO) <= 0) {
			// 包装数量为0或负数时，以1为最小单位向上取整
			return requiredQty.setScale(0, RoundingMode.CEILING);
		} else {
			// 包装数量大于0时，以包装数量为最小单位向上取整
			BigDecimal division = requiredQty.divide(unitsPerPack, 0, RoundingMode.CEILING);
			return division.multiply(unitsPerPack);
		}
	}

	/**
	 * 计算领退数量
	 * 
	 * @param requiredQty  需求数量
	 * @param deliveredQty 已领数量
	 * @param unitsPerPack 包装数量
	 * @return 领退数量
	 */
	protected BigDecimal calculateIssueQty(BigDecimal requiredQty, BigDecimal deliveredQty, BigDecimal unitsPerPack) {
		log.info("输入参数 - 需求数量: " + requiredQty + ", 已领数量: " + deliveredQty + ", 包装数量: " + unitsPerPack);

		// 计算剩余需求数量
		BigDecimal remainingQty = requiredQty.subtract(deliveredQty);
		log.info("剩余需求数量: " + remainingQty);

		// 对剩余需求数量进行包装向上取整
		BigDecimal maxQty = calculateMaxQtyWithPack(remainingQty, unitsPerPack);
		log.info("向上取整后数量: " + maxQty);

		if (maxQty.compareTo(Env.ZERO) < 0) {
			log.info("领退数量为负数，调整为0");
			return Env.ZERO;
		}

		return maxQty;
	}

	/**
	 * 获取产品的最小包装数量
	 *
	 * @param productId 产品ID
	 * @return 最小包装数量，为 null 或 <= 0 时返回 1
	 */
	protected BigDecimal getUnitsPerPack(int productId) {
		String sql = "SELECT UnitsPerPack FROM M_Product WHERE M_Product_ID = ?";
		BigDecimal pack = DB.getSQLValueBD(null, sql, productId);
		if (pack == null || pack.compareTo(Env.ZERO) <= 0) {
			return BigDecimal.ONE;
		}
		return pack;
	}

	/**
	 * 判断是否为委外发料类型
	 */
	protected boolean isSubcontractingIssue() {
		return false; // 基类默认返回false，子类重写
	}

	/**
	 * 判断是否为委外退料类型
	 */
	protected boolean isSubcontractingReturn() {
		return false; // 基类默认返回false，子类重写

	}

	/**
	 * 判断是否为生产领料类型
	 */
	private boolean isProductionIssue(String costCollectorType) {
		return COSTCOLLECTORTYPE_PRODUCTION_ISSUE.equals(costCollectorType)
				|| MPPCostCollector.COSTCOLLECTORTYPE_ComponentIssue.equals(costCollectorType);
	}

	/**
	 * 判断是否所有选中行的仓库申请数量都为0（纯线边仓领料）
	 */
	private boolean isAllLineSideRequest(IMiniTable issue) {
		boolean hasLineSideQty = false;
		for (int i = 0; i < issue.getRowCount(); i++) {
			IDColumn id = (IDColumn) issue.getValueAt(i, 0);
			if (id != null && id.isSelected()) {
				BigDecimal warehouseQty = getValueBigDecimal(issue, i, 8);
				BigDecimal lineSideQty = getValueBigDecimal(issue, i, 9);
				if (warehouseQty.compareTo(Env.ZERO) > 0) {
					return false; // 有仓库需求量，不是纯线边仓
				}
				if (lineSideQty.compareTo(Env.ZERO) > 0) {
					hasLineSideQty = true;
				}
			}
		}
		return hasLineSideQty;
	}

	private boolean hasLineSideQuantities(IMiniTable issue) {
		for (int i = 0; i < issue.getRowCount(); i++) {
			IDColumn id = (IDColumn) issue.getValueAt(i, 0);
			if (id != null && id.isSelected()) {
				BigDecimal qty = getValueBigDecimal(issue, i, 9);
				if (qty.compareTo(Env.ZERO) > 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * 判断生产退料选中行中是否有仓库退料数量（至少一行整包装部分 > 0）
	 */
	private boolean hasReturnWarehouseQty(IMiniTable issue) {
		for (int i = 0; i < issue.getRowCount(); i++) {
			IDColumn id = (IDColumn) issue.getValueAt(i, 0);
			if (id != null && id.isSelected()) {
				BigDecimal warehouseQty = getValueBigDecimal(issue, i, 8);
				if (warehouseQty.compareTo(Env.ZERO) > 0) return true;
			}
		}
		return false;
	}

	/**
	 * 设置单据类型（根据名称查找C_DocType_ID）
	 */
	private void setDocTypeByName(MPPMaterialRequisition mr, String docTypeName, MPPOrder order) {
		String sql = "SELECT C_DocType_ID FROM C_DocType " + "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,?) "
				+ "AND Name=? AND IsActive='Y' " + "ORDER BY IsDefault DESC, AD_Org_ID DESC";
		int docTypeId = DB.getSQLValue(order.get_TrxName(), sql, order.getAD_Client_ID(), order.getAD_Org_ID(),
				docTypeName);
		if (docTypeId > 0) {
			mr.setC_DocType_ID(docTypeId);
		} else {
			log.warning("未找到单据类型: " + docTypeName);
		}
	}

	/**
	 * 获取线边仓库位ID（退料时只需库位，不要求有库存），按组织过滤
	 */
	private int getLineSideLocatorId(String trxName, int adOrgId) {
		String sql = "SELECT M_Locator_ID FROM M_Locator WHERE IsActive='Y' "
				+ "AND M_LocatorType_ID IN (SELECT M_LocatorType_ID FROM M_LocatorType WHERE Name='生产线边仓') "
				+ "AND M_Warehouse_ID IN (SELECT M_Warehouse_ID FROM M_Warehouse WHERE AD_Org_ID=?) "
				+ "FETCH FIRST 1 ROWS ONLY";
		int locatorId = DB.getSQLValue(trxName, sql, adOrgId);
		return locatorId > 0 ? locatorId : 0;
	}

	/**
	 * 判断物料分组是否启用线边仓
	 */
	protected boolean isLineSideWarehouseProduct(int productId) {
		String sql = "SELECT pc.Is_Line_Side_Warehouse FROM M_Product p "
				+ "JOIN M_Product_Category pc ON p.M_Product_Category_ID_L2 = pc.M_Product_Category_ID "
				+ "WHERE p.M_Product_ID = ?";
		return "Y".equals(DB.getSQLValueString(null, sql, productId));
	}

	/**
	 * 获取线边仓库位的库存记录（M_LocatorType.Name='生产线边仓'），按组织过滤
	 */
	private MStorageOnHand[] getLineSideStorages(int productId, String trxName, int adOrgId) {
		List<MStorageOnHand> result = new ArrayList<MStorageOnHand>();
		String sql = "SELECT M_Locator_ID FROM M_Locator WHERE IsActive='Y' "
				+ "AND M_LocatorType_ID IN (SELECT M_LocatorType_ID FROM M_LocatorType WHERE Name='生产线边仓') "
				+ "AND M_Warehouse_ID IN (SELECT M_Warehouse_ID FROM M_Warehouse WHERE AD_Org_ID=?) "
				+ "FETCH FIRST 1 ROWS ONLY";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, adOrgId);
			rs = pstmt.executeQuery();
			List<Integer> locIds = new ArrayList<Integer>();
			while (rs.next())
				locIds.add(rs.getInt(1));
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
			for (int locId : locIds) {
				// 用库位ID查询（此时仓库ID被忽略），跨仓库获取库存
				MStorageOnHand[] storages = MStorageOnHand.getWarehouse(Env.getCtx(), 0, productId, 0, null,
						true, true, locId, trxName);
				for (MStorageOnHand s : storages) {
					if (s.getQtyOnHand().signum() > 0) {
						result.add(s);
					}
				}
			}
		} catch (Exception e) {
			log.severe("获取线边库位库存失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return result.toArray(new MStorageOnHand[0]);
	}

	/**
	 * 判断是否为委外补领类型
	 */
	protected boolean isSubcontractingReplenishment() {
		return false; // 基类默认返回false，子类重写
	}

	/**
	 * 过滤掉"待检库位"类型库位的库存记录
	 * 根据 M_LocatorType.Name='待检库位' 判断，来料检验待检区的库存不允许用于生产领料
	 *
	 * @param storages      原始库存数组
	 * @param warehouseId   仓库 ID
	 * @param trxName       事务
	 * @return 过滤后的库存数组
	 */
	private MStorageOnHand[] filterReservedLocator(MStorageOnHand[] storages, int warehouseId, String trxName) {
		if (storages == null || storages.length == 0) {
			return storages;
		}
		// 查找"待检库位"类型的 M_LocatorType_ID
		int locatorTypeId = DB.getSQLValueEx(trxName,
				"SELECT M_LocatorType_ID FROM M_LocatorType WHERE Name=? AND IsActive='Y'", "待检库位");
		if (locatorTypeId <= 0) {
			// 未配置待检库位类型，无需过滤
			return storages;
		}
		// 收集该仓库下属于待检类型的库位 ID
		Set<Integer> reservedLocatorIds = new HashSet<>();
		String sql = "SELECT M_Locator_ID FROM M_Locator WHERE M_Warehouse_ID=? AND M_LocatorType_ID=? AND IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, warehouseId);
			pstmt.setInt(2, locatorTypeId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				reservedLocatorIds.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			log.severe("查找待检库位失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		if (reservedLocatorIds.isEmpty()) {
			return storages;
		}
		// 过滤掉待检库位的库存记录
		ArrayList<MStorageOnHand> filtered = new ArrayList<>();
		for (MStorageOnHand soh : storages) {
			if (!reservedLocatorIds.contains(soh.getM_Locator_ID())) {
				filtered.add(soh);
			}
		}
		return filtered.toArray(new MStorageOnHand[0]);
	}

}