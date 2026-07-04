package org.idempiere.component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderNodeAsset;
import org.libero.model.MPPOrderNodeNext;
import org.libero.model.MPPOrderNodeProduct;
import org.libero.model.MPPOrderWorkflow;
import org.libero.model.MPPWFNodeAsset;
import org.libero.model.MPPWFNodeProduct;
import org.libero.model.MPP_Engineering_Change_Notice;
import org.osgi.service.event.Event;

public class ECNCompletionValidator extends AbstractEventHandler {
	private static CLogger log = CLogger.getCLogger(ECNCompletionValidator.class);

	@Override
	protected void initialize() {
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MPP_Engineering_Change_Notice.Table_Name);
	}

	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		PO po = getPO(event);

		if (po instanceof MPP_Engineering_Change_Notice && IEventTopics.DOC_AFTER_COMPLETE.equals(type)) {
			MPP_Engineering_Change_Notice ecn = (MPP_Engineering_Change_Notice) po;

			// 检查触发条件：单据状态为CO且ECNStatus为Completed
			if (DocAction.STATUS_Completed.equals(ecn.getDocStatus()) && "Completed".equals(ecn.getECNStatus())) {
				handleECNCompletion(ecn);
			}
		}
	}

	/**
	 * 处理ECN完成变更
	 */
	private void handleECNCompletion(MPP_Engineering_Change_Notice ecn) {
		try {
			// 在处理开始前验证所有相关产品
			verifyProductsForECN(ecn);

			log.info("开始处理ECN完成变更: " + ecn.getDocumentNo());

			// 第一步：同步BOM和工艺路线到产品最新版本（使用现有方法）
			// syncBOMAndWorkflowToProduct(ecn);

			// 从ECN关联的工单获取产品ID
			Integer orderId = (Integer) ecn.get_Value("PP_Order_ID");
			if (orderId == null || orderId == 0) {
				log.warning("ECN未关联工单，跳过工艺路线同步");
				return;
			}

			MPPOrder order = new MPPOrder(ecn.getCtx(), orderId, ecn.get_TrxName());
			int productId = order.getM_Product_ID(); // 从工单获取产品ID

			// 获取该产品的最新BOM
			MPPProductBOM latestBOM = getLatestBOMForProduct(productId);

			// 获取该产品的最新工艺路线
			MWorkflow latestWorkflow = getLatestWorkflowForProduct(productId, ecn.get_TrxName());

			// 第二步：基于原工单生成新工单
			MPPOrder newOrder = createNewOrderFromOriginal(ecn, latestBOM, latestWorkflow);

			// 第三步：同步最新BOM和工艺路线到新工单
			//syncLatestBOMAndWorkflowToOrder(newOrder, ecn);

			// 第四步：更新原工单状态为ChangeExecuted
			updateOriginalOrderStatus(ecn);

			log.info("ECN完成变更处理完成: " + ecn.getDocumentNo());

		} catch (Exception e) {
			log.log(Level.SEVERE, "ECN完成变更处理失败", e);
		}
	}

	private void verifyProductsForECN(MPP_Engineering_Change_Notice ecn) {
		// 获取原工单并验证产品
		Integer originalOrderId = (Integer) ecn.get_Value("PP_Order_ID");
		if (originalOrderId != null && originalOrderId > 0) {
			MPPOrder originalOrder = new MPPOrder(ecn.getCtx(), originalOrderId, ecn.get_TrxName());
			verifyProductBeforeOrderCreation(originalOrder.getM_Product_ID(), ecn.get_TrxName());
		}
	}

	private void verifyProductBeforeOrderCreation(int productId, String trxName) {
		// 使用新的产品对象实例避免不可变问题
		MProduct product = new MProduct(Env.getCtx(), productId, trxName);
		if (!product.isVerified()) {
			product.setIsVerified(true);
			product.saveEx(trxName);
			log.info("产品已验证: " + product.getValue());
		}
	}

	/**
	 * 第一步：同步BOM和工艺路线到产品最新版本
	 */
	private void syncBOMAndWorkflowToProduct(MPP_Engineering_Change_Notice ecn) {
		try {
			// 处理BOM同步
			syncBOMToProduct(ecn);

			// 处理工艺路线同步
			syncWorkflowToProduct(ecn);

			log.info("BOM和工艺路线同步完成");
		} catch (Exception e) {
			log.log(Level.SEVERE, "BOM和工艺路线同步失败", e);
			throw new RuntimeException("同步失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 同步BOM到产品最新版本
	 */
	private void syncBOMToProduct(MPP_Engineering_Change_Notice ecn) {
		String bomSQL = "SELECT PP_Product_BOM_ID FROM PP_Product_BOM WHERE pp_engineering_change_notice_id = ?";
		int[] bomIds = DB.getIDsEx(ecn.get_TrxName(), bomSQL, ecn.get_ID());

		for (int bomId : bomIds) {
			MPPProductBOM ecnBOM = new MPPProductBOM(ecn.getCtx(), bomId, ecn.get_TrxName());

			// 获取该产品的最新BOM
			MPPProductBOM latestBOM = getLatestBOMForProduct(ecnBOM.getM_Product_ID());
			if (latestBOM == null) {
				log.warning("未找到产品的最新BOM，跳过");
				break;
			}

			// 将ECN BOM的数据复制到最新BOM（排除特定字段）
			copyBOMDataFromECN(ecnBOM, latestBOM);
			latestBOM.setBOMStatus("Released");
			// 让最新BOM创建历史记录和更新版本
			latestBOM.createHistoryAndUpdateVersion();
			log.info("BOM版本已更新: " + latestBOM.getValue());
		}
	}

	/**
	 * 同步工艺路线到产品最新版本
	 */
	/**
	 * 同步工艺路线到产品最新版本
	 */
	private void syncWorkflowToProduct(MPP_Engineering_Change_Notice ecn) {
		String workflowSQL = "SELECT AD_Workflow_ID FROM AD_Workflow WHERE pp_engineering_change_notice_id = ?";
		int[] workflowIds = DB.getIDsEx(ecn.get_TrxName(), workflowSQL, ecn.get_ID());

		for (int workflowId : workflowIds) {
			MWorkflow ecnWorkflow = new MWorkflow(ecn.getCtx(), workflowId, ecn.get_TrxName());

			// 从ECN关联的工单获取产品ID
			Integer orderId = (Integer) ecn.get_Value("PP_Order_ID");
			if (orderId == null || orderId == 0) {
				log.warning("ECN未关联工单，跳过工艺路线同步");
				continue;
			}

			MPPOrder order = new MPPOrder(ecn.getCtx(), orderId, ecn.get_TrxName());
			int productId = order.getM_Product_ID(); // 从工单获取产品ID

			// 获取该产品的最新工艺路线
			MWorkflow latestWorkflow = getLatestWorkflowForProduct(productId, ecn.get_TrxName());
			if (latestWorkflow == null) {
				log.warning("未找到产品的最新工艺路线，跳过");
				continue;
			}

			// 3. 删除当前所有节点明细
			deleteAllCurrentNodeDetails(latestWorkflow);

			// 4. 从历史记录复制节点明细
			// copyDetailsFromHistory(workflow, historyWorkflow);
			copyAllNodeDetailsToHistory(ecnWorkflow, latestWorkflow);

			// 将ECN工艺路线数据复制到最新工艺路线（排除特定字段）
			copyWorkflowDataToLatest(ecnWorkflow, latestWorkflow);

			// 创建历史记录并更新版本
			// createWorkflowHistoryWithDetails(latestWorkflow);
			updateWorkflowVersion(latestWorkflow);

			log.info("工艺路线版本已更新: " + latestWorkflow.getName());
		}
	}

	/**
	 * 将ECN工艺路线数据复制到最新工艺路线
	 */
	private void copyWorkflowDataToLatest(MWorkflow sourceWorkflow, MWorkflow targetWorkflow) {
		// 保存不需要复制的字段
		String originalValue = targetWorkflow.getValue();
		String originalName = targetWorkflow.getName();
		int currentVersion = targetWorkflow.getVersion();

		// 复制所有字段值
		MWorkflow.copyValues(sourceWorkflow, targetWorkflow);

		// 恢复不需要复制的字段
		targetWorkflow.setValue(originalValue);
		targetWorkflow.setName(originalName);
		int newVersion = currentVersion > 0 ? currentVersion + 1 : 1;
		targetWorkflow.setVersion(newVersion);
		targetWorkflow.setPublishStatus("R"); // Released
		targetWorkflow.set_ValueOfColumn("VersionStatus", "C"); // Current

		targetWorkflow.saveEx();
	}

	/**
	 * 更新工艺路线版本
	 */
	private void updateWorkflowVersion(MWorkflow workflow) {
		int currentVersion = workflow.getVersion();
		int newVersion = currentVersion > 0 ? currentVersion + 1 : 1;
		workflow.setVersion(newVersion);
		workflow.setPublishStatus("R"); // Released
		workflow.set_ValueOfColumn("VersionStatus", "C"); // Current
		workflow.saveEx();
	}

	/**
	 * 完整删除当前所有节点明细
	 */
	private void deleteAllCurrentNodeDetails(MWorkflow workflow) {
		try {
			// 1. 先清除工作流中的节点引用
			String sql = "UPDATE AD_Workflow SET AD_WF_Node_ID = NULL WHERE AD_Workflow_ID = ?";
			DB.executeUpdateEx(sql, new Object[] { workflow.getAD_Workflow_ID() }, workflow.get_TrxName());

			// 2. 然后删除节点相关记录
			MWFNode[] currentNodes = workflow.getNodes(false, workflow.getAD_Client_ID());
			for (MWFNode node : currentNodes) {
				// 删除节点资产记录 (pp_wf_node_asset)
				deleteNodeAssets(node.getAD_WF_Node_ID(), workflow.get_TrxName());

				deleteNodeProducts(node.getAD_WF_Node_ID(), workflow.get_TrxName());
				deleteNodeTransitions(node.getAD_WF_Node_ID(), workflow.get_TrxName());
				node.deleteEx(true);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "删除节点明细失败", e);
			throw new RuntimeException("删除节点明细失败: " + e.getMessage());
		}
	}

	/**
	 * 删除节点资产记录 (pp_wf_node_asset)
	 */
	private void deleteNodeAssets(int ad_wf_node_id, String trxName) {
		String whereClause = "AD_WF_Node_ID=?";
		List<MPPWFNodeAsset> assets = new Query(Env.getCtx(), MPPWFNodeAsset.Table_Name, whereClause, trxName)
				.setParameters(ad_wf_node_id).list();

		for (MPPWFNodeAsset asset : assets) {
			asset.deleteEx(true);
		}
	}

	/**
	 * 删除节点产品记录
	 */
	private void deleteNodeProducts(int ad_wf_node_id, String trxName) {
		for (MPPWFNodeProduct product : MPPWFNodeProduct.forAD_WF_Node_ID(Env.getCtx(), ad_wf_node_id)) {
			product.deleteEx(true);
		}
	}

	/**
	 * 删除节点转换记录
	 */
	private void deleteNodeTransitions(int ad_wf_node_id, String trxName) {
		String whereClause = "AD_WF_Node_ID=?";
		List<MWFNodeNext> transitions = new Query(Env.getCtx(), MWFNodeNext.Table_Name, whereClause, trxName)
				.setParameters(ad_wf_node_id).list();

		for (MWFNodeNext transition : transitions) {
			transition.deleteEx(true);
		}
	}

	/**
	 * 创建工艺路线历史记录并复制所有明细
	 */
	private void createWorkflowHistoryWithDetails(MWorkflow workflow) {
		try {
			// 1. 创建历史工作流记录
			MWorkflow historyWorkflow = createHistoryWorkflow(workflow);

			// 2. 完整复制节点明细
			copyAllNodeDetailsToHistory(workflow, historyWorkflow);

			log.info("工艺路线历史记录创建成功: " + workflow.getValue());
		} catch (Exception e) {
			log.log(Level.SEVERE, "创建工艺路线历史记录失败", e);
			throw new RuntimeException("创建工艺路线历史记录失败: " + e.getMessage());
		}
	}

	/**
	 * 创建历史工作流记录
	 */
	private MWorkflow createHistoryWorkflow(MWorkflow workflow) {
		int currentVersion = workflow.getVersion();
		int historyVersion = currentVersion > 0 ? currentVersion : 1;

		MWorkflow historyWorkflow = new MWorkflow(workflow.getCtx(), 0, workflow.get_TrxName());
		MWorkflow.copyValues(workflow, historyWorkflow);
		historyWorkflow.setVersion(historyVersion);

		// 设置唯一标识
		String originalValue = workflow.getValue();
		String timestamp = String.valueOf(System.currentTimeMillis());
		historyWorkflow.setValue(originalValue + "_HIST_" + historyVersion + "_" + timestamp);
		historyWorkflow.setName(workflow.getName() + "_HIST_" + historyVersion + "_" + timestamp);

		// 设置历史状态
		historyWorkflow.setPublishStatus("V"); // Void
		historyWorkflow.set_ValueOfColumn("VersionStatus", "H"); // Historical
		historyWorkflow.setIsActive(false);
		historyWorkflow.setDocStatus(DocAction.STATUS_Completed);
		historyWorkflow.setDocAction(DocAction.ACTION_Close);
		historyWorkflow.saveEx();

		return historyWorkflow;
	}

	private void copyAllNodeDetailsToHistory(MWorkflow workflow, MWorkflow historyWorkflow) {
		MWFNode[] currentNodes = workflow.getNodes(false, workflow.getAD_Client_ID());
		Map<Integer, Integer> nodeMapping = new HashMap<>();

		List<MWFNode> nodes = new ArrayList<>();
		for (MWFNode currentNode : currentNodes) {
			// 复制节点
			MWFNode historyNode = new MWFNode(workflow.getCtx(), 0, workflow.get_TrxName());
			MWFNode.copyValues(currentNode, historyNode);
			historyNode.setAD_Workflow_ID(historyWorkflow.getAD_Workflow_ID());
			historyNode.set_ValueOfColumn("Original_AD_WF_Node_ID", currentNode.getAD_WF_Node_ID()); // 标记为历史版本)
			historyNode.saveEx();

			nodes.add(historyNode);

			nodeMapping.put(currentNode.getAD_WF_Node_ID(), historyNode.getAD_WF_Node_ID());
			// 复制节点产品记录
			copyNodeProductsToHistory(currentNode, historyNode);
			// 复制节点资产记录
			copyNodeAssetsToHistory(currentNode, historyNode);
			// 复制节点转换记录
			copyNodeTransitionsToHistory(currentNode, historyNode);

		}
		historyWorkflow.setAD_WF_Node_ID(nodeMapping.get(workflow.getAD_WF_Node_ID()));
		historyWorkflow.saveEx();
		for (MWFNode node : nodes) {
			// 复制节点
			for (MWFNodeNext next : node.getTransitions(workflow.getAD_Client_ID())) {
				next.setAD_WF_Next_ID(nodeMapping.get(next.getAD_WF_Next_ID()));
				next.saveEx(workflow.get_TrxName());
			}
		}
	}

	/**
	 * 复制节点产品记录
	 */
	private void copyNodeProductsToHistory(MWFNode currentNode, MWFNode historyNode) {
		for (MPPWFNodeProduct product : MPPWFNodeProduct.forAD_WF_Node_ID(currentNode.getCtx(),
				currentNode.getAD_WF_Node_ID())) {
			MPPWFNodeProduct historyProduct = new MPPWFNodeProduct(historyNode.getCtx(), 0, historyNode.get_TrxName());
			MPPWFNodeProduct.copyValues(product, historyProduct);
			historyProduct.setAD_WF_Node_ID(historyNode.getAD_WF_Node_ID());
			historyProduct.saveEx();
		}
	}

	/**
	 * 复制节点资产记录
	 */
	private void copyNodeAssetsToHistory(MWFNode currentNode, MWFNode historyNode) {
		for (MPPWFNodeAsset asset : MPPWFNodeAsset.forAD_WF_Node_ID(currentNode.getCtx(),
				currentNode.getAD_WF_Node_ID())) {
			MPPWFNodeAsset historyAsset = new MPPWFNodeAsset(historyNode.getCtx(), 0, historyNode.get_TrxName());
			MPPWFNodeAsset.copyValues(asset, historyAsset);
			historyAsset.setAD_WF_Node_ID(historyNode.getAD_WF_Node_ID());
			historyAsset.saveEx();
		}
	}

	/**
	 * 复制节点转换记录
	 */
	private void copyNodeTransitionsToHistory(MWFNode currentNode, MWFNode historyNode) {
		MWFNodeNext[] transitions = currentNode.getTransitions(currentNode.getAD_Client_ID());
		for (MWFNodeNext transition : transitions) {
			MWFNodeNext historyTransition = new MWFNodeNext(historyNode.getCtx(), 0, historyNode.get_TrxName());
			MWFNodeNext.copyValues(transition, historyTransition);
			historyTransition.setAD_WF_Node_ID(historyNode.getAD_WF_Node_ID());
			historyTransition.saveEx();
		}
	}

	/**
	 * 第二步：基于原工单生成新工单
	 * 
	 * @param latestWorkflow
	 * @param latestBOM
	 */
	private MPPOrder createNewOrderFromOriginal(MPP_Engineering_Change_Notice ecn, MPPProductBOM latestBOM,
			MWorkflow latestWorkflow) {
		Integer originalOrderId = (Integer) ecn.get_Value("PP_Order_ID");
		if (originalOrderId == null || originalOrderId == 0) {
			throw new RuntimeException("ECN未关联原工单");
		}

		String workflowSQL = "SELECT AD_Workflow_ID FROM AD_Workflow WHERE pp_engineering_change_notice_id = ?";
		int[] workflowIds = DB.getIDsEx(ecn.get_TrxName(), workflowSQL, ecn.get_ID());

		String bomSQL = "SELECT PP_Product_BOM_ID FROM PP_Product_BOM WHERE pp_engineering_change_notice_id = ?";
		int[] bomIds = DB.getIDsEx(ecn.get_TrxName(), bomSQL, ecn.get_ID());

		Integer EcnWorkflowId = workflowIds[0];
		Integer EcnBomId = bomIds[0];

		if (EcnWorkflowId == null || EcnBomId == null) {
			throw new RuntimeException("ECN未关联工作流或BOM明细");
		}
		//updateECNDetailsStatus(EcnWorkflowId, EcnBomId, ecn);
		
		MPPOrder originalOrder = new MPPOrder(ecn.getCtx(), originalOrderId, ecn.get_TrxName());

		// 在创建新工单前验证产品
		verifyProductBeforeOrderCreation(originalOrder.getM_Product_ID(), ecn.get_TrxName());

		// 生成新工单号：原工单号+C+两位流水
		String newDocumentNo = generateChangeDocumentNo(originalOrder.getDocumentNo(), ecn.get_TrxName());

	
		// 创建新工单
		MPPOrder newOrder = new MPPOrder(ecn.getCtx(), 0, ecn.get_TrxName());
		MPPOrder.copyValues(originalOrder, newOrder);
		// 设置新工单的基本信息
		newOrder.setDocumentNo(newDocumentNo);
		newOrder.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
		newOrder.setDocAction(MPPOrder.DOCACTION_Complete);
		newOrder.setProcessed(false);
		newOrder.setProcessing(false);
		newOrder.setPP_Product_BOM_ID(EcnBomId);
		newOrder.setAD_Workflow_ID(EcnWorkflowId);
		newOrder.set_ValueOfColumn("CreationSource", "ECN_DERIVED");
		newOrder.setAD_Org_ID(originalOrder.getAD_Org_ID());
		newOrder.saveEx();

		//newOrder.setPP_Product_BOM_ID(originalOrder.getPP_Product_BOM_ID());
		//newOrder.setAD_Workflow_ID(originalOrder.getAD_Workflow_ID());
		newOrder.saveEx();
		// 更新原工单状态
		originalOrder.setDocAction(MPPOrder.DOCACTION_None);
		originalOrder.set_ValueOfColumn("Orderstatus", "ChangeExecuted");
		originalOrder.saveEx();

		log.info("新工单已创建: " + newOrder.getDocumentNo());
		return newOrder;
	}

//	private void updateECNDetailsStatus(Integer ecnWorkflowId, Integer ecnBomId, MPP_Engineering_Change_Notice ecn) {
//		MPPProductBOM bom = new MPPProductBOM(ecn.getCtx(), ecnBomId, ecn.get_TrxName());
//		bom.set_ValueOfColumn("bomstatus", "Released");
//		bom.saveEx();
//		MWorkflow workflow = new MWorkflow(ecn.getCtx(), ecnWorkflowId, ecn.get_TrxName());
//		workflow.set_ValueOfColumn("PublishStatus", "R");
//		workflow.saveEx();
//	}

	private String generateChangeDocumentNo(String originalDocNo, String trxName) {
	    if (originalDocNo == null || originalDocNo.isEmpty()) {
	        return originalDocNo;
	    }
	    
	    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("-C(\\d+)$");
	    java.util.regex.Matcher matcher = pattern.matcher(originalDocNo);
	    
	    if (matcher.find()) {
	        // 提取现有后缀中的数字部分
	        String numStr = matcher.group(1);
	        int num = Integer.parseInt(numStr);
	        int newNum = num + 1;
	        
	        // 保持原始数字的位数（例如01加1后变为02，99加1后变为100）
	        String newNumStr = String.format("%0" + numStr.length() + "d", newNum);
	        
	        // 获取前缀（从开头到"-C"之前的部分）
	        String prefix = originalDocNo.substring(0, matcher.start());
	        return prefix + "-C" + newNumStr;
	    } else {
	        // 无后缀时，直接添加"-C01"
	        return originalDocNo + "-C01";
	    }
	}

	/**
	 * 第三步：同步最新BOM和工艺路线到新工单
	 */
	private void syncLatestBOMAndWorkflowToOrder(MPPOrder newOrder, MPP_Engineering_Change_Notice ecn) {
		// 获取产品的最新BOM
		// 只需要设置BOM和工艺路线ID，系统会自动创建
		MPPProductBOM latestBOM = getLatestBOMForProduct(newOrder.getM_Product_ID());
		if (latestBOM != null) {
			// 只设置BOM ID，让系统自动创建BOM
			newOrder.setPP_Product_BOM_ID(latestBOM.getPP_Product_BOM_ID());
		}

		// 获取产品的最新工艺路线
		MWorkflow latestWorkflow = getLatestWorkflowForProduct(newOrder.getM_Product_ID(), newOrder.get_TrxName());
		if (latestWorkflow != null) {
			// 创建工单工艺路线
			MPPOrderWorkflow orderWorkflow = new MPPOrderWorkflow(latestWorkflow, newOrder.getPP_Order_ID(),
					newOrder.get_TrxName());
			orderWorkflow.setAD_Org_ID(newOrder.getAD_Org_ID());
			orderWorkflow.saveEx(newOrder.get_TrxName());

			// 复制工艺路线节点
			copyWorkflowNodesToOrder(latestWorkflow, orderWorkflow, newOrder.getQtyOrdered());
			newOrder.setAD_Workflow_ID(latestWorkflow.getAD_Workflow_ID());
		}

		newOrder.saveEx();
		log.info("最新BOM和工艺路线已同步到新工单");
	}

	// 辅助方法
	private MPPProductBOM getLatestBOMForProduct(int productId) {
		String whereClause = "M_Product_ID=? AND BOMType='A' AND BOMUse='A' AND IsActive='Y'";
		return new Query(Env.getCtx(), MPPProductBOM.Table_Name, whereClause, null).setParameters(productId)
				.setOrderBy("Created DESC").first();
	}

	private MWorkflow getLatestWorkflowForProduct(int productId, String trxName) {
		// 通过工单查找产品的工艺路线，筛选最新有效版本
		String sql = "SELECT w.AD_Workflow_ID FROM AD_Workflow w "
				+ "INNER JOIN PP_Order o ON w.AD_Workflow_ID = o.AD_Workflow_ID " + "WHERE o.M_Product_ID = ? "
				+ "AND w.VersionStatus = 'C' " // Current
				+ "AND w.IsActive = 'Y' " + "ORDER BY w.ValidFrom DESC, w.Created DESC";

		int workflowId = DB.getSQLValue(trxName, sql, productId);

		if (workflowId > 0) {
			return new MWorkflow(Env.getCtx(), workflowId, trxName);
		}

		return null;
	}

	private void copyWorkflowNodesToOrder(MWorkflow workflow, MPPOrderWorkflow orderWorkflow, BigDecimal qty) {
		// 参考MPPOrder的explosion方法实现
		Map<Integer, Integer> nodeMapping = new HashMap<Integer, Integer>();

		for (MWFNode node : workflow.getNodes(false, workflow.getAD_Client_ID())) {
			MPPOrderNode orderNode = new MPPOrderNode(node, orderWorkflow, qty, orderWorkflow.get_TrxName());
			orderNode.setAD_Org_ID(orderWorkflow.getAD_Org_ID());
			orderNode.saveEx(orderWorkflow.get_TrxName());

			// 保存节点映射关系，用于后续设置转换关系
			nodeMapping.put(node.getAD_WF_Node_ID(), orderNode.getPP_Order_Node_ID());

			// 复制节点产品
			for (MPPWFNodeProduct wfnp : MPPWFNodeProduct.forAD_WF_Node_ID(orderWorkflow.getCtx(),
					node.getAD_WF_Node_ID())) {
				MPPOrderNodeProduct nodeOrderProduct = new MPPOrderNodeProduct(wfnp, orderNode);
				nodeOrderProduct.setAD_Org_ID(orderWorkflow.getAD_Org_ID());
				nodeOrderProduct.saveEx(orderWorkflow.get_TrxName());
			}

			// 复制节点资产
			for (MPPWFNodeAsset wfna : MPPWFNodeAsset.forAD_WF_Node_ID(orderWorkflow.getCtx(),
					node.getAD_WF_Node_ID())) {
				MPPOrderNodeAsset nodeorderasset = new MPPOrderNodeAsset(wfna, orderNode);
				nodeorderasset.setAD_Org_ID(orderWorkflow.getAD_Org_ID());
				nodeorderasset.saveEx(orderWorkflow.get_TrxName());
			}
		}

		// 设置节点转换关系
		for (MWFNode node : workflow.getNodes(false, workflow.getAD_Client_ID())) {
			Integer orderNodeId = nodeMapping.get(node.getAD_WF_Node_ID());
			if (orderNodeId != null) {
				MPPOrderNode orderNode = new MPPOrderNode(orderWorkflow.getCtx(), orderNodeId,
						orderWorkflow.get_TrxName());

				for (MWFNodeNext AD_WF_NodeNext : node.getTransitions(workflow.getAD_Client_ID())) {
					Integer nextOrderNodeId = nodeMapping.get(AD_WF_NodeNext.getAD_WF_Next_ID());
					if (nextOrderNodeId != null) {
						MPPOrderNodeNext nodenext = new MPPOrderNodeNext(AD_WF_NodeNext, orderNode);
						nodenext.setPP_Order_Next_ID(nextOrderNodeId);
						nodenext.setAD_Org_ID(orderWorkflow.getAD_Org_ID());
						nodenext.saveEx(orderWorkflow.get_TrxName());
					}
				}
			}
		}

		// 设置起始节点
		workflow.getNodes(false, workflow.getAD_Client_ID()); // 重新查询
		for (MPPOrderNode orderNode : orderWorkflow.getNodes(false, orderWorkflow.getAD_Client_ID())) {
			if (orderWorkflow.getAD_WF_Node_ID() == orderNode.getAD_WF_Node_ID()) {
				orderWorkflow.setPP_Order_Node_ID(orderNode.getPP_Order_Node_ID());
			}
		}
		orderWorkflow.saveEx(orderWorkflow.get_TrxName());
	}

	/**
	 * 将ECN BOM数据复制到最新产品BOM
	 */
	private void copyBOMDataFromECN(MPPProductBOM sourceBOM, MPPProductBOM targetBOM) {
		// 复制基本字段（排除BOMUse, BOMType, Revision）
		targetBOM.setName(sourceBOM.getName());
		targetBOM.setDescription(sourceBOM.getDescription());
		targetBOM.setHelp(sourceBOM.getHelp());
		targetBOM.setValidFrom(sourceBOM.getValidFrom());
		targetBOM.setValidTo(sourceBOM.getValidTo());
		targetBOM.setM_AttributeSetInstance_ID(sourceBOM.getM_AttributeSetInstance_ID());
		targetBOM.setC_UOM_ID(sourceBOM.getC_UOM_ID());

		// 清空现有明细并复制新的明细
		clearBOMLines(targetBOM);
		copyBOMLinesFromHistory(sourceBOM, targetBOM);

		targetBOM.saveEx();
	}

	/**
	 * 清空BOM明细
	 */
	private void clearBOMLines(MPPProductBOM bom) {
		MPPProductBOMLine[] currentLines = bom.getLines();
		for (MPPProductBOMLine line : currentLines) {
			line.deleteEx(true);
		}
	}

	/**
	 * 从源BOM复制明细到目标BOM
	 */
	private void copyBOMLinesFromHistory(MPPProductBOM sourceBOM, MPPProductBOM targetBOM) {
		MPPProductBOMLine[] sourceLines = sourceBOM.getLines();
		for (MPPProductBOMLine sourceLine : sourceLines) {
			MPPProductBOMLine targetLine = new MPPProductBOMLine(targetBOM.getCtx(), 0, targetBOM.get_TrxName());
			MPPProductBOMLine.copyValues(sourceLine, targetLine);
			targetLine.setPP_Product_BOM_ID(targetBOM.getPP_Product_BOM_ID());
			targetLine.setM_Product_ID(sourceLine.getM_Product_ID());

			// 获取关联工单的计划开始时间
			Timestamp scheduleDate = getOrderScheduleDate(targetBOM);
			if (scheduleDate != null) {
				// 设置有效期为计划开始时间之前，确保能通过isValidFromTo检查
				targetLine.setValidFrom(new Timestamp(scheduleDate.getTime() - 24 * 60 * 60 * 1000)); // 计划开始时间前1天
				targetLine.setValidTo(new Timestamp(scheduleDate.getTime() + 365L * 24 * 60 * 60 * 1000)); // 计划开始时间后1年
			} else {
				// 如果无法获取计划时间，使用当前时间作为备选
				targetLine.setValidFrom(new Timestamp(System.currentTimeMillis()));
				targetLine.setValidTo(new Timestamp(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
			}

			targetLine.saveEx();
		}
	}

	/**
	 * 获取BOM关联工单的计划开始时间
	 */
	private Timestamp getOrderScheduleDate(MPPProductBOM bom) {
		String sql = "SELECT DateStartSchedule FROM PP_Order WHERE PP_Product_BOM_ID = ?";
		return DB.getSQLValueTS(bom.get_TrxName(), sql, bom.getPP_Product_BOM_ID());
	}

	/**
	 * 更新原工单状态为ChangeExecuted
	 */
	private void updateOriginalOrderStatus(MPP_Engineering_Change_Notice ecn) {
		if (ecn.getPP_Order_ID() > 0) {
			MPPOrder originalOrder = new MPPOrder(ecn.getCtx(), ecn.getPP_Order_ID(), ecn.get_TrxName());
			String currentStatus = (String) originalOrder.get_Value("Orderstatus");

			// 只有在原工单状态为InECNChange时才更新
			if ("InECNChange".equals(currentStatus)) {
				originalOrder.set_ValueOfColumn("Orderstatus", "ChangeExecuted");
				originalOrder.setDocAction(MPPOrder.DOCACTION_None);
				originalOrder.saveEx();

				log.info("原工单状态已更新为ChangeExecuted: " + originalOrder.getDocumentNo());
			}
		}
	}
}