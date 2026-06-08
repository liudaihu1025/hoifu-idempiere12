package org.libero.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MDocType;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.tables.X_PP_Engineering_Change_Notice;

public class MPP_Engineering_Change_Notice extends X_PP_Engineering_Change_Notice implements DocAction {

	private static final long serialVersionUID = 1L;

	/** Just Prepared Flag */
	private boolean m_justPrepared = false;

	// ECN状态常量
	public static final String ECNSTATUS_Draft = "Draft";
	public static final String ECNSTATUS_PendingApproval = "PendingApproval";
	public static final String ECNSTATUS_Rejected = "Rejected";
	public static final String ECNSTATUS_Approved = "Approved";
	public static final String ECNSTATUS_InExecution = "InExecution";
	public static final String ECNSTATUS_Confirming = "Confirming";
	public static final String ECNSTATUS_Cancelled = "Canceled";
	public static final String ECNSTATUS_Completed = "Completed";

	// 标准构造函数
	public MPP_Engineering_Change_Notice(Properties ctx, int PP_Engineering_Change_Notice_ID, String trxName) {
		super(ctx, PP_Engineering_Change_Notice_ID, trxName);
		if (PP_Engineering_Change_Notice_ID == 0) {
			setDefault();
		}
	}

	// 加载构造函数
	public MPP_Engineering_Change_Notice(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	// 设置默认值
	private void setDefault() {
		setDocStatus(DOCSTATUS_Drafted);
		setDocAction(DOCACTION_Complete);
		setProcessed(false);
		setIsApproved(false);
		setECNStatus(ECNSTATUS_Draft);
	}



	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {

		log.info("afterSave called - newRecord=" + newRecord + ", success=" + success);

		if (!success) {
			return false;
		}


		// 只有在PP_Order_ID有值且是新记录或PP_Order_ID发生变化时才复制
		if (getPP_Order_ID() > 0 && (newRecord || is_ValueChanged("PP_Order_ID"))) {
			copyOrderData();
		}

		return success;
	}

	private void copyOrderData() {
		try {
			// 获取关联的工单
			MPPOrder order = new MPPOrder(getCtx(), getPP_Order_ID(), get_TrxName());

			// 复制BOM数据
			copyBOMData(order);

			// 复制工艺路线数据
			copyWorkflowData(order);



		} catch (Exception e) {
			log.severe("复制工单数据失败: " + e.getMessage());
		}
	}

	private void copyBOMData(MPPOrder order) throws Exception {

		// 获取工单的BOM
		MPPOrderBOM orderBOM = order.getMPPOrderBOM();

		// 创建新的BOM记录
		MPPProductBOM newBOM = new MPPProductBOM(getCtx(), 0, get_TrxName());
		MPPProductBOM.copyValues(orderBOM, newBOM);

		// 修改BOM类型避免冲突
		newBOM.setBOMType("E"); // 改为"历史有效"类型
		newBOM.set_ValueOfColumn("bomstatus", "Released");
		newBOM.setBOMUse(MPPProductBOM.BOMUSE_Master); // 改为"Engineering"用途
		newBOM.setIsActive(true);

		// 生成唯一的Value
		String originalValue = newBOM.getValue();
		String uniqueValue = originalValue + "_ECN" + get_ID();
		newBOM.setValue(uniqueValue);

		// 设置ECN关联
		newBOM.set_ValueOfColumn("PP_Engineering_Change_Notice_ID", get_ID());
		newBOM.saveEx();

		// 复制BOM明细
		copyBOMLines(orderBOM, newBOM);

	}

	private void copyBOMLines(MPPOrderBOM orderBOM, MPPProductBOM newBOM) throws Exception {
		MPPOrderBOMLine[] orderBOMLines = orderBOM.getLines();
		for (MPPOrderBOMLine orderBOMLine : orderBOMLines) {
			MPPProductBOMLine newBOMLine = new MPPProductBOMLine(getCtx(), 0, get_TrxName());
			MPPProductBOMLine.copyValues(orderBOMLine, newBOMLine);
			newBOMLine.setPP_Product_BOM_ID(newBOM.getPP_Product_BOM_ID());
			newBOMLine.saveEx();
		}
	}

	private void copyWorkflowData(MPPOrder order) throws Exception {
		// 获取工单工艺流程
		MPPOrderWorkflow orderWorkflow = order.getMPPOrderWorkflow();
		if (orderWorkflow == null) {
			return;
		}

		// 获取对应的产品工艺流程
//		MWorkflow productWorkflow = new MWorkflow(getCtx(), order.getAD_Workflow_ID(), get_TrxName());
//		if (productWorkflow == null || productWorkflow.get_ID() == 0) {
//			return;
//		}
		MWorkflow productWorkflow = new MWorkflow(getCtx(), 0, get_TrxName());
		// 复制工艺流程主字段
		copyWorkflowMainFields(orderWorkflow, productWorkflow);

		// 设置ECN关联
		productWorkflow.set_ValueOfColumn("PP_Engineering_Change_Notice_ID", get_ID());

		productWorkflow.saveEx();
		// 删除现有节点
		//deleteWorkflowNodes(productWorkflow);

		// 复制节点数据
		copyOrderWorkflowNodesToProductWorkflow(orderWorkflow, productWorkflow);

		productWorkflow.saveEx();
	}

	private void copyWorkflowMainFields(MPPOrderWorkflow orderWorkflow, MWorkflow productWorkflow) {
		MWorkflow.copyValues(orderWorkflow, productWorkflow);
		productWorkflow.setName(orderWorkflow.getName() + '(' + getDocumentNo() + ')');
		productWorkflow.setValue(orderWorkflow.getValue() + '(' + getDocumentNo() + ')');
		productWorkflow.setPublishStatus("R");
		productWorkflow.set_ValueOfColumn("VersionStatus", "E");//ECN变更
	}

	private void deleteWorkflowNodes(MWorkflow productWorkflow) throws Exception {
		MWFNode[] nodes = productWorkflow.getNodes(false, productWorkflow.getAD_Client_ID());
		for (MWFNode node : nodes) {
			deleteNodeTransitions(node);
			node.deleteEx(true);
		}
	}

	private void deleteNodeTransitions(MWFNode node) throws Exception {
		MWFNodeNext[] transitions = node.getTransitions(node.getAD_Client_ID());
		for (MWFNodeNext transition : transitions) {
			transition.deleteEx(true);
		}
	}

	private void copyOrderWorkflowNodesToProductWorkflow(MPPOrderWorkflow orderWorkflow, MWorkflow productWorkflow)
			throws Exception {
		MPPOrderNode[] orderNodes = orderWorkflow.getNodes(false, orderWorkflow.getAD_Client_ID());

		// 核心映射结构：
		// 1. 工单节点ID -> 新产品节点
		Map<Integer, MWFNode> orderNodeIdToProductNode = new HashMap<>();

		// 2. 工单节点ID -> 工单节点对象
		Map<Integer, MPPOrderNode> orderNodeMap = new HashMap<>();

		// 构建工单节点映射
		for (MPPOrderNode orderNode : orderNodes) {
			orderNodeMap.put(orderNode.getPP_Order_Node_ID(), orderNode);
		}

		// 创建所有产品节点
		for (MPPOrderNode orderNode : orderNodes) {
			MWFNode productNode = new MWFNode(productWorkflow.getCtx(), 0, productWorkflow.get_TrxName());

			copyOrderWorkflowNodeToProductWorkflowNode(orderNode, productNode);
			productNode.setAD_Workflow_ID(productWorkflow.getAD_Workflow_ID());
			productNode.saveEx();

			// 建立映射：工单节点ID -> 新产品节点
			orderNodeIdToProductNode.put(orderNode.getPP_Order_Node_ID(), productNode);

			// 复制节点产品记录
			copyOrderNodeProductsToProductWorkflow(orderNode, productNode);
			// 复制节点资产记录
			copyOrderNodeAssetsToProductWorkflow(orderNode, productNode);
		}

		// 设置起始节点
		if (orderWorkflow.getPP_Order_Node_ID() > 0) {
			MWFNode firstProductNode = orderNodeIdToProductNode.get(orderWorkflow.getPP_Order_Node_ID());
			if (firstProductNode != null) {
				productWorkflow.setAD_WF_Node_ID(firstProductNode.getAD_WF_Node_ID());
			}
		}

		// 复制节点转换记录
		copyAllNodeTransitionsToProductWorkflow(orderNodeMap, orderNodeIdToProductNode);
	}

	private void copyAllNodeTransitionsToProductWorkflow(Map<Integer, MPPOrderNode> orderNodeMap,
			Map<Integer, MWFNode> orderNodeIdToProductNode) throws Exception {

		// 第一步：先创建所有转换记录，但AD_WF_Next_ID先设置为0
		Map<MWFNode, List<MWFNodeNext>> transitionsByProductNode = new HashMap<>();
		Map<MWFNode, List<MPPOrderNodeNext>> orderTransitionsByProductNode = new HashMap<>();

		// 收集所有转换关系
		for (Map.Entry<Integer, MPPOrderNode> entry : orderNodeMap.entrySet()) {
			Integer orderNodeId = entry.getKey();
			MPPOrderNode orderNode = entry.getValue();
			MWFNode productNode = orderNodeIdToProductNode.get(orderNodeId);

			if (productNode == null)
				continue;

			List<MWFNodeNext> productTransitions = new ArrayList<>();
			List<MPPOrderNodeNext> orderTransitions = new ArrayList<>();

			MPPOrderNodeNext[] orderNodeTransitions = orderNode.getTransitions(orderNode.getAD_Client_ID());

			for (MPPOrderNodeNext orderTransition : orderNodeTransitions) {
				MWFNodeNext productTransition = new MWFNodeNext(productNode.getCtx(), 0, productNode.get_TrxName());

				productTransition.setAD_WF_Node_ID(productNode.getAD_WF_Node_ID());
				productTransition.setDescription(orderTransition.getDescription());
				productTransition.setSeqNo(orderTransition.getSeqNo());
				productTransition.setTransitionCode(orderTransition.getTransitionCode());

				// 暂时不设置AD_WF_Next_ID，先保存
				productTransition.setAD_WF_Next_ID(orderTransition.getPP_Order_Node_ID());
				productTransition.saveEx();

				productTransitions.add(productTransition);
				orderTransitions.add(orderTransition);
			}

			if (!productTransitions.isEmpty()) {
				transitionsByProductNode.put(productNode, productTransitions);
				orderTransitionsByProductNode.put(productNode, orderTransitions);
			}
		}

		// 第二步：批量更新AD_WF_Next_ID
		for (MWFNode productNode : transitionsByProductNode.keySet()) {
			List<MWFNodeNext> productTransitions = transitionsByProductNode.get(productNode);
			List<MPPOrderNodeNext> orderTransitions = orderTransitionsByProductNode.get(productNode);

			for (int i = 0; i < orderTransitions.size(); i++) {
				MPPOrderNodeNext orderTransition = orderTransitions.get(i);
				MWFNodeNext productTransition = productTransitions.get(i);

				// 获取目标工单节点ID
				Integer targetOrderNodeId = orderTransition.getPP_Order_Next_ID();

				// 直接通过目标工单节点ID找到对应的新产品节点
				MWFNode targetProductNode = orderNodeIdToProductNode.get(targetOrderNodeId);

				if (targetProductNode != null) {
					productTransition.setAD_WF_Next_ID(targetProductNode.getAD_WF_Node_ID());
					productTransition.saveEx();
				} else {
					log.warning("无法找到目标产品节点: PP_Order_Next_ID=" + targetOrderNodeId);
				}
			}
		}
	}

	private void copyOrderWorkflowNodeToProductWorkflowNode(MPPOrderNode orderNode, MWFNode productNode) {
		productNode.setName(orderNode.getName());
		productNode.setValue(orderNode.getValue());
		productNode.setDescription(orderNode.getDescription());
		productNode.setHelp(orderNode.getHelp());
		productNode.setAction(orderNode.getAction());
		productNode.setDuration(orderNode.getDuration());
		productNode.setCost(orderNode.getCost());
		productNode.setWaitingTime(orderNode.getWaitingTime());
		productNode.setWorkingTime(orderNode.getWorkingTime());
		productNode.setSetupTime(orderNode.getSetupTime());
		productNode.setMovingTime(orderNode.getMovingTime());
		productNode.setQueuingTime(orderNode.getQueuingTime());
		productNode.setS_Resource_ID(orderNode.getS_Resource_ID());
		productNode.setPriority(orderNode.getPriority());
		productNode.setJoinElement(orderNode.getJoinElement());
		productNode.setSplitElement(orderNode.getSplitElement());
		productNode.setXPosition(orderNode.getXPosition());
		productNode.setYPosition(orderNode.getYPosition());
		productNode.setOverlapUnits(orderNode.getOverlapUnits());
		productNode.setUnitsCycles(BigDecimal.valueOf(orderNode.getUnitsCycles()));
		Integer AD_Routing_Node_ID = (Integer) orderNode.get_Value("AD_Routing_Node_ID");
		productNode.set_ValueOfColumn("AD_Routing_Node_ID", AD_Routing_Node_ID);
	}

	private void copyOrderNodeProductsToProductWorkflow(MPPOrderNode orderNode, MWFNode productNode) {
		// 基于 PP_Order_Node_ID 获取工单节点的产品
		for (MPPOrderNodeProduct orderProduct : MPPOrderNodeProduct.forPP_Order_Node_ID(orderNode.getCtx(),
				orderNode.getPP_Order_Node_ID())) {
			MPPWFNodeProduct productWorkflowProduct = new MPPWFNodeProduct(productNode.getCtx(), 0,
					productNode.get_TrxName());

			// 复制产品信息
			productWorkflowProduct.setM_Product_ID(orderProduct.getM_Product_ID());
			productWorkflowProduct.setQty(orderProduct.getQty());
			productWorkflowProduct.setQty(orderProduct.getQty());
			productWorkflowProduct.setSeqNo(orderProduct.getSeqNo());
			productWorkflowProduct.setIsSubcontracting(orderProduct.isSubcontracting());
			productWorkflowProduct.setIsActive(orderProduct.isActive());

			productWorkflowProduct.setAD_WF_Node_ID(productNode.getAD_WF_Node_ID());
			productWorkflowProduct.saveEx();
		}
	}

	private void copyOrderNodeAssetsToProductWorkflow(MPPOrderNode orderNode, MWFNode productNode) {
		// 基于 PP_Order_Node_ID 获取工单节点的资产
		for (MPPOrderNodeAsset orderAsset : MPPOrderNodeAsset.forPP_Order_Node_ID(orderNode.getCtx(),
				orderNode.getPP_Order_Node_ID())) {
			MPPWFNodeAsset productWorkflowAsset = new MPPWFNodeAsset(productNode.getCtx(), 0,
					productNode.get_TrxName());

			// 复制资产信息
			productWorkflowAsset.setA_Asset_ID(orderAsset.getA_Asset_ID());
			productWorkflowAsset.setIsActive(orderAsset.isActive());
			productWorkflowAsset.setSeqNo(1);

			productWorkflowAsset.setAD_WF_Node_ID(productNode.getAD_WF_Node_ID());
			productWorkflowAsset.saveEx();
		}
	}

	// DocAction接口实现 - 工作流回调方法
	@Override
	public boolean approveIt() {
		setECNStatus(ECNSTATUS_Approved);
		//setDocStatus(DOCSTATUS_InProgress);
		Boolean isCompletedApproved = (Boolean) get_Value("iscompletedapproved");
		setIsApproved(true);
		if (!isCompletedApproved) {
			set_ValueOfColumn("iscompletedapproved", true);
			setIsApproved(false);
		}

		saveEx();
		return true;
	}

	@Override
	public boolean rejectIt() {
		setECNStatus(ECNSTATUS_Rejected);
		setIsApproved(false);
		Boolean isCompletedApproved = (Boolean) get_Value("iscompletedapproved");
		if (isCompletedApproved) {
			setECNStatus(ECNSTATUS_InExecution);
		}
		rollbackOrderStatusForCancellation(this);
 		saveEx();
		return true;
	}

	@Override
	public String completeIt() {
		// 检查准备状态
		if (!m_justPrepared) {
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status)) {
				return status;
			}
		}

		// 触发完成前验证事件
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}

		// 设置完成状态
		setECNStatus(ECNSTATUS_Completed);
		setDocStatus(DOCSTATUS_Completed);
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		saveEx();


		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null) {
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}

		return DocAction.STATUS_Completed;
	}

	@Override
	public boolean voidIt() {
		log.info("voidIt - " + toString());

		// 设置ECN状态为已取消
		setECNStatus("Canceled");
		setDocStatus(DOCSTATUS_Voided);
		setDocAction(DOCACTION_None);

		// 回滚关联工单状态
		rollbackOrderStatusForCancellation(this);

		saveEx();
		return true;
	}

	@Override
	public boolean processIt(String processAction) {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(processAction, getDocAction());
	}

	@Override
	public String prepareIt() {
		return DocAction.STATUS_InProgress;
	}

	@Override
	public boolean unlockIt() {
		setProcessing(false);
		return true;
	}

	@Override
	public boolean invalidateIt() {
		setDocAction(DOCACTION_Prepare);
		return true;
	}


	@Override
	public boolean closeIt() {
		setDocStatus(DOCSTATUS_Closed);
		return true;
	}

	@Override
	public boolean reverseCorrectIt() {
		return voidIt();
	}

	@Override
	public boolean reverseAccrualIt() {
		return false;
	}

	@Override
	public boolean reActivateIt() {
		setDocStatus(DOCSTATUS_Drafted);
		setProcessed(false);
		return true;
	}

	@Override
	public String getSummary() {
		return getDescription();
	}

	@Override
	public String getProcessMsg() {
		return m_processMsg;
	}

	@Override
	public int getDoc_User_ID() {
		return getCreatedBy();
	}

	@Override
	public int getC_Currency_ID() {
		return 0;
	}

	@Override
	public File createPDF() {
		try {
			File temp = File.createTempFile(get_TableName() + get_ID() + "_", ".pdf");
			return createPDF(temp);
		} catch (Exception e) {
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}

	public File createPDF(File file) {
		// PDF生成逻辑
		return null;
	}

	@Override
	public String getDocumentInfo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}

	@Override
	public BigDecimal getApprovalAmt() {
		return Env.ZERO;
	}

	// 状态控制方法
	public void setECNStatus(String ecnStatus) {
		set_Value("ECNStatus", ecnStatus);
	}

	public String getECNStatus() {
		return (String) get_Value("ECNStatus");
	}

	/**
	 * ECN取消时回滚工单状态到原始状态
	 */
	private void rollbackOrderStatusForCancellation(MPP_Engineering_Change_Notice ecn) {

		if (ecn.getPP_Order_ID() > 0) {
			// 重新加载ECN对象确保获取最新数据
			MPP_Engineering_Change_Notice freshEcn = new MPP_Engineering_Change_Notice(ecn.getCtx(), ecn.get_ID(),
					ecn.get_TrxName());

			String originalStatus = (String) freshEcn.get_Value("OriginalOrderStatus");
			log.info("从数据库读取的原始状态: " + originalStatus);

			if (originalStatus == null || originalStatus.trim().isEmpty()) {
				originalStatus = "Released";
			}

			MPPOrder order = new MPPOrder(ecn.getCtx(), ecn.getPP_Order_ID(), ecn.get_TrxName());
			order.set_ValueOfColumn("Orderstatus", originalStatus);
			order.setDocAction(MPPOrder.DOCACTION_Complete);
			order.saveEx();

			log.info("工单状态已回滚: " + order.getDocumentNo() + " -> " + originalStatus);
		}
	}

	// 私有成员变量
	private String m_processMsg = null;
}