package org.libero.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MDocType;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPCostCollector;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderNode;

/**
 * 生产工序任务 Process —— 从信息窗口选中工序，创建报工单并修改工序状态。
 * 
 * ActionType 参数：
 *   OPEN          - 生产报工开工（默认）
 *   NON_ACTIVITY  - 非生产报工
 *   COMPLETE      - 生产完工
 */
@org.adempiere.base.annotation.Process
public class PPOrderNodeWorkReportingProcess extends SvrProcess {

	// 参数名：操作类型（OPEN/NON_ACTIVITY/COMPLETE）
	private static final String PARAM_ACTION_TYPE = "p_ActionType";

	// 消息内容
	private String p_ActionType = "OPEN"; // 操作类型，默认生产报工
	private int userId;                   // 当前登录用户
	private int nodeId;                   // 选中的工序节点 ID


	@Override
	protected void prepare() {
		userId = Env.getAD_User_ID(getCtx());
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (PARAM_ACTION_TYPE.equals(name))
				p_ActionType = para.getParameterAsString();
		}
		if (p_ActionType == null || p_ActionType.isEmpty())
			p_ActionType = "OPEN"; // 未传参数时默认为生产报工
	}

	@Override
	protected String doIt() throws Exception {
		nodeId = getNodeId();
		if (nodeId <= 0)
			throw new AdempiereUserError("请选择一条工序记录");

		if ("OPEN".equalsIgnoreCase(p_ActionType))
			return doOpen();
		if ("NON_ACTIVITY".equalsIgnoreCase(p_ActionType))
			return doNonActivity();
		if ("COMPLETE".equalsIgnoreCase(p_ActionType))
			return doComplete();

		throw new AdempiereUserError("未知操作类型: " + p_ActionType);
	}

	/**
	 * 三段式获取选中的 PP_Order_Node_ID：多选 → 单选 → T_Selection
	 */
	private int getNodeId() {
		List<Integer> ids = getRecord_IDs();
		if (ids == null || ids.isEmpty()) {
			int recordId = getRecord_ID();
			if (recordId > 0) {
				ids = List.of(recordId);
			} else {
				int[] selIds = DB.getIDsEx(get_TrxName(),
						"SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?", getAD_PInstance_ID());
				if (selIds != null && selIds.length > 0) {
					ids = new ArrayList<>();
					for (int id : selIds)
						ids.add(id);
				}
			}
		}
		if (ids == null || ids.isEmpty())
			return -1;
		return ids.get(0);
	}

	/**
	 * 生产报工开工：创建报工单
	 */
	private String doOpen() throws Exception {
		MPPOrderNode node = new MPPOrderNode(getCtx(), nodeId, get_TrxName()); // 加载当前工序节点

		// 校验：已完工/已关闭/已作废的工序不允许开工
		String docStatus = node.getDocStatus(); // 工序当前状态
		if (MPPOrderNode.DOCSTATUS_Completed.equals(docStatus)    // CO = 已完工
				|| MPPOrderNode.DOCSTATUS_Closed.equals(docStatus)   // CL = 已关闭
				|| MPPOrderNode.DOCSTATUS_Voided.equals(docStatus))  // VO = 已作废
			throw new AdempiereUserError("该工序状态为已完成或者已关闭，不允许操作");

		// 查询同工序 + 同操作人员是否有未完成的生产报工单（排除已完成/已关闭/已作废）
		String sql = "SELECT PP_Cost_Collector_ID FROM PP_Cost_Collector "
				+ "WHERE CostCollectorType = ? "       // 工序报工类型=160
				+ "  AND PP_Order_Node_ID = ? "        // 当前工序
				+ "  AND AD_User_ID = ? "              // 当前操作人员
				+ "  AND DocStatus NOT IN ('CO','CL','VO') " // 排除已完成/关闭/作废
				+ "ORDER BY Created DESC LIMIT 1";

		int existingCcId = DB.getSQLValue(get_TrxName(), sql,
				MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl, nodeId, userId);

		// 已有未完成单则直接跳转，不重复创建
		if (existingCcId > 0) {
			addLog(0, null, null, "已有未完成报工单，点击跳转", MPPCostCollector.Table_ID, existingCcId);
			return "已有未完成报工单";
		}

		// ========== 1. 创建报工单 ==========
		MPPOrder order = new MPPOrder(getCtx(), node.getPP_Order_ID(), get_TrxName()); // 加载生产工单

		// 查询当前用户最近一次报工所用的班组
		int teamId = DB.getSQLValue(get_TrxName(),
				"SELECT C_WorkTeam_ID FROM PP_Cost_Collector "
				+ "WHERE AD_User_ID = ? AND CostCollectorType = ? "
				+ "  AND C_WorkTeam_ID IS NOT NULL AND DocStatus NOT IN ('VO') "
				+ "ORDER BY Created DESC LIMIT 1",
				userId, MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl);

		MPPCostCollector cc = new MPPCostCollector(getCtx(), 0, get_TrxName()); // 新建报工单

		// --- 工单关联字段 ---
		cc.setPP_Order_ID(order.getPP_Order_ID());                      // 生产工单
		cc.setAD_Org_ID(order.getAD_Org_ID());                          // 组织
		cc.setM_Warehouse_ID(order.getM_Warehouse_ID());                // 仓库
		cc.setM_Product_ID(order.getM_Product_ID());                    // 产品
		cc.setC_UOM_ID(order.getC_UOM_ID());                            // 单位
		cc.setM_AttributeSetInstance_ID(order.getM_AttributeSetInstance_ID()); // 属性集实例
		cc.setS_Resource_ID(order.getS_Resource_ID());                  // 资源/设备
		cc.setC_Activity_ID(order.getC_Activity_ID());                  // 活动

		// --- 工序关联字段 ---
		cc.setPP_Order_Node_ID(nodeId);                                 // 工序节点

		cc.setPP_Order_Workflow_ID(node.getPP_Order_Workflow_ID());     // 工单工艺流程

		// --- 单据类型 ---
		cc.setCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl); // 成本收集类型=工序报工(160)

		cc.setHF_WorkReportType(MPPCostCollector.HF_WORKREPORTTYPE_Produce);          // 报工子类型=生产报工(SC)

		int docTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_ManufacturingCostCollector); // 获取制造报工单据类型ID

		cc.setC_DocType_ID(docTypeId);         // 单据类型

		cc.setC_DocTypeTarget_ID(docTypeId);   // 目标单据类型

		// --- 单据状态 ---
		cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);    // 单据状态=草稿

		cc.setDocAction(MPPCostCollector.DOCACTION_Prepare);    // 下一步操作=准备

		cc.setDateAcct(new Timestamp(System.currentTimeMillis())); // 记账日期=当前时间

		// --- 报工人员信息 ---
		cc.setAD_User_ID(userId);              // 报工人

		if (teamId > 0)
			cc.set_ValueOfColumn("C_WorkTeam_ID", teamId); // 班组（取最近一次报工的班组）

		// --- 数量 ---
		cc.setMovementQty(Env.ZERO); // 报工数量（初始为0）

		cc.set_ValueOfColumn("QtyRequiered", node.getQtyRequiered());      // 标准加工数（从工序带入）

		cc.saveEx(get_TrxName()); // 保存报工单

		// ========== 2. 工序状态改为"进行中" ==========
//		if (!MPPOrderNode.DOCSTATUS_InProgress.equals(docStatus)) {        // 仅当状态不是"进行中"时才更新
//			node.setDocStatus(MPPOrderNode.DOCSTATUS_InProgress);          // 工序状态=进行中(IP)
//			 node.setDocAction(MPPOrderNode.DOCACTION_Complete);            // 下一步=完成
//			node.saveEx(get_TrxName());
//		}

		addLog(0, null, null, "已创建报工单 " + cc.getDocumentNo(), MPPCostCollector.Table_ID, cc.get_ID());
		return "已创建报工单 " + cc.getDocumentNo();
	}

	/**
	 * 非生产报工：直接跳转到报工单窗口，不做数据操作。
	 */
	private String doNonActivity() throws Exception {
		addLog(0, null, null, "点击跳转到非生产报工窗口", MPPCostCollector.Table_ID, 0);
		return "跳转到非生产报工窗口";
	}

	/**
	 * 生产完工：报工数 > 标准加工数，工序状态改为"已完工"。
	 */
	private String doComplete() throws Exception {
		MPPOrderNode node = new MPPOrderNode(getCtx(), nodeId, get_TrxName()); // 加载当前工序节点

		// 校验：已完工无需重复操作
		String docStatus = node.getDocStatus();
		if (MPPOrderNode.DOCSTATUS_Completed.equals(docStatus))   // CO = 已完工
			return "该工序已完工，无需重复操作";

		if (MPPOrderNode.DOCSTATUS_Closed.equals(docStatus)       // CL = 已关闭
				|| MPPOrderNode.DOCSTATUS_Voided.equals(docStatus)) // VO = 已作废
			throw new AdempiereUserError("该工序状态为已完成或者已关闭，不允许操作");


		BigDecimal movementQty = DB.getSQLValueBD(get_TrxName(),
				"SELECT COALESCE(SUM(MovementQty), 0) FROM PP_Cost_Collector "
						+ "WHERE PP_Order_Node_ID = ? AND CostCollectorType = ? "
						+ "AND DocStatus NOT IN ('DR', 'VO')",
				nodeId, MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl);

    	if (movementQty == null){
			 movementQty = Env.ZERO;
		}

		// 校验：必须有有效的生产报工记录（状态不能为草稿或作废）才能完工
		int reportCount = DB.getSQLValue(get_TrxName(),
				"SELECT COUNT(*) FROM PP_Cost_Collector "
				+ "WHERE PP_Order_Node_ID = ? AND CostCollectorType = ? "
				+ "AND DocStatus NOT IN ('DR', 'VO')",
				nodeId, MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl);
		if (reportCount <= 0)
			throw new AdempiereUserError("该工序没有有效的生产报工记录，无法完工");

		BigDecimal qtyRequiered = node.getQtyRequiered();   // 标准加工数

//		if (movementQty.compareTo(qtyRequiered) <= 0)      // 报工数未超过标准加工数则拒绝
//			throw new AdempiereUserError(
//					"工序报工数（" + movementQty + "）未超过标准加工数（" + qtyRequiered + "），无法完工");

		// 执行完工
		node.setDocStatus(MPPOrderNode.DOCSTATUS_Completed);   // 只改状态，不改操作
		node.saveEx(get_TrxName());

		// 更新关联报工单的完成时间和工时
		int ccId = DB.getSQLValue(get_TrxName(),
				"SELECT PP_Cost_Collector_ID FROM PP_Cost_Collector "
				+ "WHERE PP_Order_Node_ID = ? AND CostCollectorType = ? "
				+ "  AND DocStatus NOT IN ('DR', 'VO') "
				+ "ORDER BY Created DESC LIMIT 1",
				nodeId, MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl);

		if (ccId > 0) {

			MPPCostCollector cc = new MPPCostCollector(getCtx(), ccId, get_TrxName());
			Timestamp dateStart = cc.getDateStart();
			Timestamp dateFinish = new Timestamp(System.currentTimeMillis());
			cc.setDateFinish(dateFinish);
			if (dateStart != null) {
				BigDecimal roundedHours = MPPCostCollector.updateDurationRealFromDates(dateStart, dateFinish);
				cc.setDurationReal(roundedHours);
			}
			cc.saveEx(get_TrxName());
		}

		addLog(0, null, null, "工序 " + node.getName() + " 已完工，"
				+ "报工数: " + movementQty + "/" + qtyRequiered);
		return "工序已完工";
	}
}