package org.libero.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.compiere.model.MProcess;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderNodeAsset;
import org.libero.model.MPPOrderNodeNext;
import org.libero.model.MPPOrderNodeProduct;
import org.libero.model.MPPOrderWorkflow;
import org.libero.model.MPPWFNodeAsset;
import org.libero.model.MPPWFNodeProduct;

@org.adempiere.base.annotation.Process
public class PPOrderWorkflowSyncProcess extends SvrProcess {

	protected void prepare() {
		// 此流程没有参数
	}

	protected String doIt() throws Exception {
		List<Integer> orderIds = getRecord_IDs();
		if (orderIds == null || orderIds.isEmpty()) {
			int recordId = getRecord_ID();
			if (recordId > 0) {
				orderIds = List.of(recordId);
			}
		}

		if (orderIds.isEmpty()) {
			throw new IllegalArgumentException("请选择要更新工艺流程的工单");
		}

		validateOrderStatus(orderIds);
		int updatedCount = updateOrders(orderIds);

		return "成功更新 " + updatedCount + " 个工单的工艺流程信息";
	}

	private void validateOrderStatus(List<Integer> orderIds) throws Exception {
		List<String> invalidOrders = orderIds.stream().map(orderId -> {
			MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());
			if (order.get_ID() > 0) {
				String orderStatus = (String) order.get_Value("Orderstatus");
				String docStatus = order.getDocStatus();
				// 只允许Orderstatus='Ready'且DocStatus='DR'的工单更新工艺流程
				if (!"Ready".equals(orderStatus) || !DocAction.STATUS_Drafted.equals(docStatus)) {
					return order.getDocumentNo();
				}
			}
			return null;
		}).filter(docNo -> docNo != null).collect(Collectors.toList());

		if (!invalidOrders.isEmpty()) {
			throw new IllegalArgumentException(
					"您选择的工单中包含以下\"非待发布\"状态的条目：" + String.join(", ", invalidOrders) + "。只有状态为【待发布】的工单才允许更新工艺流程。");
		}
	}

    private int updateOrders(List<Integer> orderIds) {  
        int count = 0;  
          
        for (Integer orderId : orderIds) {  
            if (orderId == null || orderId <= 0) {  
                continue;  
            }  
              
            MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
              
            try {  
				// 执行工艺流程同步
				syncOrderWorkflowToProductWorkflow(order);
                count++;  
                  
				log.info("Successfully synced Workflow for Order " + orderId);
                  
            } catch (Exception e) {  
				log.severe("Failed to sync Workflow for Order " + orderId + ": " + e.getMessage());
                // 继续处理下一个工单  
            }  
        }  
          
        return count;  
	}

	/**
	 * 同步工单工艺流程到产品工艺流程
	 */
	/**
	 * 同步工单工艺流程到产品工艺流程
	 */
	private void syncOrderWorkflowToProductWorkflow(MPPOrder order) throws Exception {

		// 1. 获取工单工艺流程
		MPPOrderWorkflow orderWorkflow = order.getMPPOrderWorkflow();
		if (orderWorkflow == null) {
			throw new Exception("工单工艺流程不存在");
		}

		// 2. 取消勾选工单工艺流程的有效字段
		orderWorkflow.setIsActive(false);
		orderWorkflow.saveEx();

		// 3. 获取对应的产品工艺流程
		MWorkflow productWorkflow = new MWorkflow(getCtx(), order.getAD_Workflow_ID(), get_TrxName());
		if (productWorkflow == null || productWorkflow.get_ID() == 0) {
			throw new Exception("产品工艺流程不存在");
		}

		// 4. 先申请变更，将产品工艺流程变为草稿状态
		requestWorkflowChange(productWorkflow);

		// 5. 复制产品工艺流程主字段
		copyMainFields(orderWorkflow, productWorkflow);

		// 6. 删除产品工艺流程节点
		deleteWorkflowNodes(productWorkflow);

		// 7. 复制工单工艺流程节点到产品工艺流程
		// ★ 返回 旧AD_WF_Node_ID → 新AD_WF_Node_ID 映射，同时已更新当前工单节点
		Map<Integer, Integer> oldToNewNodeIdMap = copyOrderWorkflowNodesToProductWorkflow(orderWorkflow,
				productWorkflow);

		// ★ 批量更新其他工单（使用同一产品工艺流程）的 PP_Order_Node.AD_WF_Node_ID
		updateOtherOrderNodeReferences(order.getAD_Workflow_ID(), oldToNewNodeIdMap, order.getPP_Order_ID());

		// 8. 保存产品工艺流程
		productWorkflow.saveEx();

		// 9. 调用提交审核流程
		submitWorkflowForApproval(productWorkflow);
	}
	/**
	 * 复制主字段
	 */
	private void copyMainFields(MPPOrderWorkflow orderWorkflow, MWorkflow productWorkflow) {
		productWorkflow.setHelp(orderWorkflow.getHelp());
		productWorkflow.setName(orderWorkflow.getName());
		productWorkflow.setDescription(orderWorkflow.getDescription());
		productWorkflow.setDuration(orderWorkflow.getDuration());
		productWorkflow.setDurationUnit(orderWorkflow.getDurationUnit());
		productWorkflow.setCost(orderWorkflow.getCost());
		productWorkflow.setWaitingTime(orderWorkflow.getWaitingTime());
		productWorkflow.setWorkingTime(orderWorkflow.getWorkingTime());
		productWorkflow.setSetupTime(orderWorkflow.getSetupTime());
		productWorkflow.setMovingTime(orderWorkflow.getMovingTime());
		productWorkflow.setQueuingTime(orderWorkflow.getQueuingTime());
		productWorkflow.setPriority(orderWorkflow.getPriority());
		productWorkflow.setOverlapUnits(orderWorkflow.getOverlapUnits());
		productWorkflow.setYield(orderWorkflow.getYield());
		productWorkflow.setAuthor(orderWorkflow.getAuthor());
	}

	/**
	 * 删除产品工艺流程节点
	 */
	private void deleteWorkflowNodes(MWorkflow productWorkflow) throws Exception {
		MWFNode[] nodes = productWorkflow.getNodes(false, productWorkflow.getAD_Client_ID());
		for (MWFNode node : nodes) {
			// 先删除节点的转换关系
			deleteNodeTransitions(node);
			// 删除节点
			node.deleteEx(true);
		}
	}

	/**
	 * 删除节点转换关系
	 */
	private void deleteNodeTransitions(MWFNode node) throws Exception {
		MWFNodeNext[] transitions = node.getTransitions(node.getAD_Client_ID());
		for (MWFNodeNext transition : transitions) {
			transition.deleteEx(true);
		}
	}

	private Map<Integer, Integer> copyOrderWorkflowNodesToProductWorkflow(MPPOrderWorkflow orderWorkflow,
			MWorkflow productWorkflow) throws Exception {
	    MPPOrderNode[] orderNodes = orderWorkflow.getNodes(false, orderWorkflow.getAD_Client_ID());  
	  
		// 核心映射结构：
		// 1. 工单节点ID -> 新产品节点
		Map<Integer, MWFNode> orderNodeIdToProductNode = new HashMap<>();

		// 2. 工单节点ID -> 工单节点对象
		Map<Integer, MPPOrderNode> orderNodeMap = new HashMap<>();

		// 3. 新产品节点列表
		List<MWFNode> productNodes = new ArrayList<>();

		// ★ 4. 旧AD_WF_Node_ID → 新AD_WF_Node_ID 映射，不依赖 Value
		Map<Integer, Integer> oldToNewNodeIdMap = new HashMap<>();

		// 构建工单节点映射
	    for (MPPOrderNode orderNode : orderNodes) {  
			orderNodeMap.put(orderNode.getPP_Order_Node_ID(), orderNode);
		}

		// 创建所有产品节点
	    for (MPPOrderNode orderNode : orderNodes) {  
			int oldNodeId = orderNode.getAD_WF_Node_ID(); // ★ 旧 AD_WF_Node_ID（记录已删但字段值还在）

	        MWFNode productNode = new MWFNode(productWorkflow.getCtx(), 0, productWorkflow.get_TrxName());  

	        copyOrderWorkflowNodeToProductWorkflowNode(orderNode, productNode);  
	        productNode.setAD_Workflow_ID(productWorkflow.getAD_Workflow_ID());  
	        productNode.saveEx();  
	  
	        productNodes.add(productNode);  

			// 建立映射：工单节点ID -> 新产品节点
			orderNodeIdToProductNode.put(orderNode.getPP_Order_Node_ID(), productNode);

			// ★ 记录 旧ID → 新ID 映射
			oldToNewNodeIdMap.put(oldNodeId, productNode.getAD_WF_Node_ID());

			// ★ 更新当前工单的 PP_Order_Node.AD_WF_Node_ID 指向新节点
			orderNode.setAD_WF_Node_ID(productNode.getAD_WF_Node_ID());
			orderNode.saveEx();

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

		return oldToNewNodeIdMap; // ★ 返回映射
	}
	/**  
	 * 批量复制所有节点的转换关系
	 */
	private void copyAllNodeTransitionsToProductWorkflow(
	        Map<Integer, MPPOrderNode> orderNodeMap,
	        Map<Integer, MWFNode> orderNodeIdToProductNode) throws Exception {  
	    
	    // 第一步：先创建所有转换记录，但AD_WF_Next_ID先设置为0
	    Map<MWFNode, List<MWFNodeNext>> transitionsByProductNode = new HashMap<>();
	    Map<MWFNode, List<MPPOrderNodeNext>> orderTransitionsByProductNode = new HashMap<>();
	    
	    // 收集所有转换关系
	    for (Map.Entry<Integer, MPPOrderNode> entry : orderNodeMap.entrySet()) {
	        Integer orderNodeId = entry.getKey();
	        MPPOrderNode orderNode = entry.getValue();
	        MWFNode productNode = orderNodeIdToProductNode.get(orderNodeId);
	        
	        if (productNode == null) continue;
	        
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
	
	/**
	 * 复制工单节点到产品节点
	 */
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
		Integer AD_Routing_Node_ID = (Integer) orderNode.get_Value("AD_Routing_Node_ID");
		productNode.set_ValueOfColumn("AD_Routing_Node_ID", AD_Routing_Node_ID);
	}



	/**
	 * 申请工艺流程变更
	 */
	private void requestWorkflowChange(MWorkflow productWorkflow) throws Exception {
		int AD_Process_ID = MProcess.getProcess_ID("AD_Workflow_RequestChange", get_TrxName());
		if (AD_Process_ID <= 0) {
			throw new Exception("未找到工艺流程变更审批进程");
		}

		ProcessInfo pi = new ProcessInfo("工艺流程变更审批", AD_Process_ID);
		pi.setAD_Client_ID(getAD_Client_ID());
		pi.setAD_User_ID(getAD_User_ID());
		pi.setRecord_ID(productWorkflow.getAD_Workflow_ID());
		pi.setTransactionName(get_TrxName());

		ServerProcessCtl.process(pi, Trx.get(get_TrxName(), false), false);

		if (pi.isError()) {
			throw new Exception("申请变更失败: " + pi.getSummary());
		}

		// 重新加载工艺流程以获取更新后的状态
		productWorkflow.load(get_TrxName());
	}

	/**
	 * 提交工艺流程审核
	 */
	private void submitWorkflowForApproval(MWorkflow productWorkflow) throws Exception {
		int AD_Process_ID = MProcess.getProcess_ID("AD_Workflow_SubmitForApproval", get_TrxName());
		if (AD_Process_ID <= 0) {
			throw new Exception("未找到工艺流程变更审批进程");
		}

		ProcessInfo pi = new ProcessInfo("AD_Workflow_SubmitForApproval", AD_Process_ID);
		pi.setAD_Client_ID(getAD_Client_ID());
		pi.setAD_User_ID(getAD_User_ID());
		pi.setRecord_ID(productWorkflow.getAD_Workflow_ID());
		pi.setTransactionName(get_TrxName());

		ServerProcessCtl.process(pi, Trx.get(get_TrxName(), false), false);

		if (pi.isError()) {
			throw new Exception("提交审核失败: " + pi.getSummary());
		}
	}
	/**  
	 * 复制工单节点产品到产品工作流  
	 */  
	private void copyOrderNodeProductsToProductWorkflow(MPPOrderNode orderNode, MWFNode productNode) {    
	    // 基于 PP_Order_Node_ID 获取工单节点的产品  
	    for (MPPOrderNodeProduct orderProduct : MPPOrderNodeProduct.forPP_Order_Node_ID(orderNode.getCtx(), orderNode.getPP_Order_Node_ID())) {    
	        MPPWFNodeProduct productWorkflowProduct = new MPPWFNodeProduct(productNode.getCtx(), 0, productNode.get_TrxName());    
	          
	        // 复制产品信息  
	        productWorkflowProduct.setM_Product_ID(orderProduct.getM_Product_ID());  
	        productWorkflowProduct.setQty(orderProduct.getQty());  
	        productWorkflowProduct.setSeqNo(orderProduct.getSeqNo());  
	        productWorkflowProduct.setIsSubcontracting(orderProduct.isSubcontracting());  
	        productWorkflowProduct.setIsActive(orderProduct.isActive());  
	          
	        productWorkflowProduct.setAD_WF_Node_ID(productNode.getAD_WF_Node_ID());    
	        productWorkflowProduct.saveEx();    
	    }    
	} 
	  
	/**  
	 * 复制工单节点资产到产品工作流  
	 */  
	private void copyOrderNodeAssetsToProductWorkflow(MPPOrderNode orderNode, MWFNode productNode) {    
	    // 基于 PP_Order_Node_ID 获取工单节点的资产  
	    for (MPPOrderNodeAsset orderAsset : MPPOrderNodeAsset.forPP_Order_Node_ID(orderNode.getCtx(), orderNode.getPP_Order_Node_ID())) {    
	        MPPWFNodeAsset productWorkflowAsset = new MPPWFNodeAsset(productNode.getCtx(), 0, productNode.get_TrxName());    
	          
	        // 复制资产信息  
	        productWorkflowAsset.setA_Asset_ID(orderAsset.getA_Asset_ID());  
	        productWorkflowAsset.setIsActive(orderAsset.isActive());  
	        productWorkflowAsset.setSeqNo(1);
	          
	        productWorkflowAsset.setAD_WF_Node_ID(productNode.getAD_WF_Node_ID());    
	        productWorkflowAsset.saveEx();    
	    }    
	} 

	/**
	 * 批量更新其他工单（使用同一产品工艺流程）的 PP_Order_Node.AD_WF_Node_ID 直接通过 旧AD_WF_Node_ID →
	 * 新AD_WF_Node_ID 映射更新，不依赖 Value
	 */
	private void updateOtherOrderNodeReferences(int adWorkflowId, Map<Integer, Integer> oldToNewNodeIdMap,
			int excludeOrderId) {

		if (oldToNewNodeIdMap.isEmpty()) {
			return;
		}

		for (Map.Entry<Integer, Integer> entry : oldToNewNodeIdMap.entrySet()) {
			int oldNodeId = entry.getKey();
			int newNodeId = entry.getValue();

			if (oldNodeId <= 0) {
				continue; // 旧节点本来就没有关联，跳过
			}

			String sql = "UPDATE PP_Order_Node SET AD_WF_Node_ID = ? " + "WHERE AD_WF_Node_ID = ? "
					+ "AND PP_Order_ID != ? " + "AND PP_Order_ID IN ("
					+ "  SELECT PP_Order_ID FROM PP_Order WHERE AD_Workflow_ID = ?" + ")";
			int updated = DB.executeUpdateEx(sql, new Object[] { newNodeId, oldNodeId, excludeOrderId, adWorkflowId },
					get_TrxName());

			if (updated > 0) {
				log.info("已更新 " + updated + " 条 PP_Order_Node.AD_WF_Node_ID: " + oldNodeId + " → " + newNodeId);
			}
		}
	}
	  
}