package org.idempiere.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderWorkflow;
import org.libero.model.MPPWFNodeAsset;
import org.libero.model.MPPWFNodeProduct;
import org.osgi.service.event.Event;

public class WorkflowDetailValidator extends AbstractEventHandler {
	private static CLogger log = CLogger.getCLogger(WorkflowDetailValidator.class);

	@Override
	protected void initialize() {
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, "AD_Workflow");
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, "AD_Workflow");
	}

	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		PO po = getPO(event);
		if (po instanceof MWorkflow) {
			MWorkflow workflow = (MWorkflow) po;
			if (IEventTopics.DOC_AFTER_COMPLETE.equals(type)) {
				// 审批通过后创建历史记录并复制明细
				if ("R".equals(workflow.getPublishStatus()) && "CO".equals(workflow.getDocStatus())) {
					handleWorkflowApproval(workflow);
				}
			} else if (IEventTopics.PO_AFTER_CHANGE.equals(type)) {
				// 处理取消变更 - 直接检查相关字段
				if (workflow.is_ValueChanged("cancelchange")
						&& "Y".equals(workflow.get_ValueAsString("cancelchange"))) {
					handleCancelChange(workflow);
				}
			}
		}
	}

	/**
	 * 处理工作流审批通过
	 */
	private void handleWorkflowApproval(MWorkflow workflow) {
		try {
			// 创建历史记录并完整复制明细
			createHistoryWithDetails(workflow);
			log.info("工作流审批通过，历史记录创建成功: " + workflow.getValue());
			// 激活对应的工单工艺流程
			activateOrderWorkflow(workflow);

		} catch (Exception e) {
			log.log(Level.SEVERE, "创建工作流历史记录失败", e);
		}
	}

	private void handleCancelChange(MWorkflow workflow) {
		try {
			// 立即重置标志，防止重复触发
			workflow.set_ValueOfColumn("cancelchange", "N");
			workflow.saveEx();

			// 1. 查找历史工作流记录
			MWorkflow historyWorkflow = findHistoryWorkflow(workflow);
			if (historyWorkflow == null) {
				log.warning("未找到历史工作流记录");
				return;
			}

			// 2. 恢复主数据字段
			restoreWorkflowMainData(workflow, historyWorkflow);

			// 3. 删除当前所有节点明细
			deleteAllCurrentNodeDetails(workflow);

			// 4. 从历史记录复制节点明细
			// copyDetailsFromHistory(workflow, historyWorkflow);
			copyAllNodeDetailsToHistory(historyWorkflow, workflow);
			// 5. 更新最终状态
			workflow.set_ValueOfColumn("PublishStatus", "R");
			workflow.setDocStatus(DocAction.STATUS_Completed);
			workflow.setDocAction(DocAction.ACTION_Close);
			workflow.saveEx();

			log.info("工作流已恢复到历史版本: " + historyWorkflow.getVersion());

		} catch (Exception e) {
			log.log(Level.SEVERE, "取消变更失败", e);
		}
	}
	/**
	 * 查找历史工作流记录
	 */
	private MWorkflow findHistoryWorkflow(MWorkflow workflow) {
		String whereClause = "name LIKE ? AND PublishStatus = 'V' AND VersionStatus = 'H'";
		return new Query(workflow.getCtx(), MWorkflow.Table_Name, whereClause, workflow.get_TrxName())
				.setParameters(workflow.getName() + "_HIST_%").setOrderBy("created DESC").first();
	}

	/**
	 * 创建历史记录并复制所有明细
	 */
	private void createHistoryWithDetails(MWorkflow workflow) {
		// 1. 创建历史工作流记录
		MWorkflow historyWorkflow = createHistoryWorkflow(workflow);

		// 2. 完整复制节点明细
		copyAllNodeDetailsToHistory(workflow, historyWorkflow);

		// 3. 更新当前工作流版本号 - 添加这行
		int currentVersion = workflow.getVersion();
		int newVersion = currentVersion > 0 ? currentVersion + 1 : 1;
		workflow.setVersion(newVersion);
		workflow.saveEx();

		log.info("工作流版本已更新: " + currentVersion + " -> " + newVersion);
	}

	/**
	 * 创建历史工作流记录
	 */
	private MWorkflow createHistoryWorkflow(MWorkflow workflow) {
		// 获取当前版本号
		int currentVersion = workflow.getVersion();
		int historyVersion = currentVersion > 0 ? currentVersion : 1;
		MWorkflow historyWorkflow = new MWorkflow(workflow.getCtx(), 0, workflow.get_TrxName());
		// 复制当前工作流的基本信息
		MWorkflow.copyValues(workflow, historyWorkflow);
		historyWorkflow.setVersion(historyVersion);
		// 设置历史工作流的Value = 原Value + "_HIST_" + 当前版本号 + 时间戳，确保唯一性
		String originalValue = workflow.getValue();
		String timestamp = String.valueOf(System.currentTimeMillis());
		historyWorkflow.setValue(originalValue + "_HIST_" + historyVersion + "_" + timestamp);
		// 同时修改 name 字段确保唯一性
		historyWorkflow.setName(workflow.getName() + "_HIST_" + historyVersion + "_" + timestamp);
		historyWorkflow.setPublishStatus("V"); // 设置为已作废状态
		historyWorkflow.set_ValueOfColumn("VersionStatus", "H"); // 标记为历史版本
		historyWorkflow.setIsActive(false);
		historyWorkflow.setDocStatus(DocAction.STATUS_Completed);
		historyWorkflow.setDocAction(DocAction.ACTION_Close);
		historyWorkflow.saveEx();
		return historyWorkflow;
	}

	/**
	 * 完整复制所有节点明细
	 */
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
	 * 从历史工作流复制详细信息
	 */
	private void copyDetailsFromHistory(MWorkflow workflow, MWorkflow historyWorkflow) {
		// 复制工作流节点
		copyWorkflowNodesFromHistory(workflow, historyWorkflow);
		// 复制节点相关的产品记录
		copyNodeProductsFromHistory(workflow, historyWorkflow);
		// 复制节点相关的资产记录
		copyNodeAssetsFromHistory(workflow, historyWorkflow);
		// 复制节点转换记录
		copyNodeTransitionsFromHistory(workflow, historyWorkflow);
	}

	private void copyWorkflowNodesFromHistory(MWorkflow workflow, MWorkflow historyWorkflow) {
		MWFNode[] historyNodes = historyWorkflow.getNodes(false, historyWorkflow.getAD_Client_ID());
		List<MWFNode> newNodes = new ArrayList<>();

		for (MWFNode historyNode : historyNodes) {
			MWFNode newNode = new MWFNode(workflow.getCtx(), 0, workflow.get_TrxName());
			MWFNode.copyValues(historyNode, newNode);
			newNode.setAD_Workflow_ID(workflow.getAD_Workflow_ID());

			// 关键：保存历史节点的ID，用于后续建立映射
			newNode.set_ValueOfColumn("Original_AD_WF_Node_ID", historyNode.getAD_WF_Node_ID());

			// 恢复原始名称（移除_HIST_后缀）
			String originalName = historyNode.getName();
			if (originalName.contains("_HIST_")) {
				originalName = originalName.substring(0, originalName.indexOf("_HIST_"));
			}
			newNode.setName(originalName);

			newNode.saveEx();
			newNodes.add(newNode);
		}

		// 注意：这里不设置起始节点，在copyNodeTransitionsFromHistory中统一处理
	}
	/**
	 * 从历史工作流复制节点产品记录
	 */
	private void copyNodeProductsFromHistory(MWorkflow workflow, MWorkflow historyWorkflow) {
		// 查询历史工作流的所有节点
		MWFNode[] historyNodes = historyWorkflow.getNodes(false, historyWorkflow.getAD_Client_ID());
		for (MWFNode historyNode : historyNodes) {
			// 查询该节点的产品记录
			Collection<MPPWFNodeProduct> historyProducts = MPPWFNodeProduct.forAD_WF_Node_ID(historyWorkflow.getCtx(),
					historyNode.getAD_WF_Node_ID());
			// 查找对应的新节点
			MWFNode newNode = findCorrespondingNode(workflow, historyNode);
			if (newNode != null) {
				for (MPPWFNodeProduct historyProduct : historyProducts) {
					MPPWFNodeProduct newProduct = new MPPWFNodeProduct(workflow.getCtx(), 0, workflow.get_TrxName());
					MPPWFNodeProduct.copyValues(historyProduct, newProduct);
					newProduct.setAD_WF_Node_ID(newNode.getAD_WF_Node_ID());
					newProduct.saveEx();
				}
			}
		}
	}

	/**
	 * 从历史工作流复制节点资产记录
	 */
	private void copyNodeAssetsFromHistory(MWorkflow workflow, MWorkflow historyWorkflow) {
		MWFNode[] historyNodes = historyWorkflow.getNodes(false, historyWorkflow.getAD_Client_ID());
		for (MWFNode historyNode : historyNodes) {
			Collection<MPPWFNodeAsset> historyAssets = MPPWFNodeAsset.forAD_WF_Node_ID(historyWorkflow.getCtx(),
					historyNode.getAD_WF_Node_ID());
			MWFNode newNode = findCorrespondingNode(workflow, historyNode);
			if (newNode != null) {
				for (MPPWFNodeAsset historyAsset : historyAssets) {
					MPPWFNodeAsset newAsset = new MPPWFNodeAsset(workflow.getCtx(), 0, workflow.get_TrxName());
					MPPWFNodeAsset.copyValues(historyAsset, newAsset);
					newAsset.setAD_WF_Node_ID(newNode.getAD_WF_Node_ID());
					newAsset.saveEx();
				}
			}
		}
	}

	private void copyNodeTransitionsFromHistory(MWorkflow workflow, MWorkflow historyWorkflow) {
		// 建立历史节点ID到当前新节点ID的准确映射
		Map<Integer, Integer> nodeIdMapping = new HashMap<>();

		// 获取所有节点
		MWFNode[] historyNodes = historyWorkflow.getNodes(false, historyWorkflow.getAD_Client_ID());
		MWFNode[] currentNodes = workflow.getNodes(false, workflow.getAD_Client_ID());

		// 建立映射关系：通过Original_AD_WF_Node_ID字段
		for (MWFNode historyNode : historyNodes) {
			for (MWFNode currentNode : currentNodes) {
				// 获取当前节点保存的历史节点ID
				Object originalId = currentNode.get_Value("Original_AD_WF_Node_ID");
				if (originalId != null) {
					int histNodeId = ((Number) originalId).intValue();
					if (histNodeId == historyNode.getAD_WF_Node_ID()) {
						nodeIdMapping.put(historyNode.getAD_WF_Node_ID(), currentNode.getAD_WF_Node_ID());
						break;
					}
				}
			}
		}

		// 复制所有节点的流转关系
		for (MWFNode historyNode : historyNodes) {
			MWFNodeNext[] historyTransitions = historyNode.getTransitions(historyWorkflow.getAD_Client_ID());

			for (MWFNodeNext historyTransition : historyTransitions) {
				MWFNodeNext newTransition = new MWFNodeNext(workflow.getCtx(), 0, workflow.get_TrxName());
				MWFNodeNext.copyValues(historyTransition, newTransition);

				// 获取映射后的节点ID
				Integer sourceNodeId = nodeIdMapping.get(historyTransition.getAD_WF_Node_ID());
				Integer targetNodeId = nodeIdMapping.get(historyTransition.getAD_WF_Next_ID());

				if (sourceNodeId != null) {
					newTransition.setAD_WF_Node_ID(sourceNodeId);
				}
				if (targetNodeId != null) {
					newTransition.setAD_WF_Next_ID(targetNodeId);
				}

				newTransition.saveEx();
			}
		}

		// 更新工作流的起始节点
		MWFNode startNode = null;
		for (MWFNode historyNode : historyNodes) {
			if (historyWorkflow.getAD_WF_Node_ID() == historyNode.getAD_WF_Node_ID()) {
				// 找到历史起始节点对应的当前节点
				Integer currentStartNodeId = nodeIdMapping.get(historyNode.getAD_WF_Node_ID());
				if (currentStartNodeId != null) {
					for (MWFNode currentNode : currentNodes) {
						if (currentNode.getAD_WF_Node_ID() == currentStartNodeId) {
							startNode = currentNode;
							break;
						}
					}
				}
				break;
			}
		}

		if (startNode != null) {
			workflow.setAD_WF_Node_ID(startNode.getAD_WF_Node_ID());
			workflow.saveEx();
		}
	}

	private MWFNode findCorrespondingNode(MWorkflow workflow, MWFNode historyNode) {
		MWFNode[] currentNodes = workflow.getNodes(false, workflow.getAD_Client_ID());
		for (MWFNode currentNode : currentNodes) {
			// 移除历史节点名称中的 _HIST_ 后缀再匹配
			String historyNodeName = historyNode.getName();
			if (historyNodeName.contains("_HIST_")) {
				historyNodeName = historyNodeName.substring(0, historyNodeName.indexOf("_HIST_"));
			}

			if (historyNodeName.equals(currentNode.getName())
					|| historyNode.getValue().equals(currentNode.getValue())) {
				return currentNode;
			}
		}
		return null;
	}

	/**
	 * 恢复工作流主数据
	 */
	private void restoreWorkflowMainData(MWorkflow workflow, MWorkflow historyWorkflow) {
		try {

			// 只恢复需要恢复的业务字段
			workflow.setWorkflowType(historyWorkflow.getWorkflowType());
			workflow.setDurationUnit(historyWorkflow.getDurationUnit());
			workflow.setDuration(historyWorkflow.getDuration());
			workflow.setEntityType(historyWorkflow.getEntityType());
			workflow.setAccessLevel(historyWorkflow.getAccessLevel());
			workflow.setAuthor(historyWorkflow.getAuthor());
			workflow.setS_Resource_ID(historyWorkflow.getS_Resource_ID());
			workflow.setLimit(historyWorkflow.getLimit());
			workflow.setPriority(historyWorkflow.getPriority());
			workflow.setQueuingTime(historyWorkflow.getQueuingTime());
			workflow.setSetupTime(historyWorkflow.getSetupTime());
			workflow.setMovingTime(historyWorkflow.getMovingTime());
			workflow.setProcessType(historyWorkflow.getProcessType());
			workflow.setCost(historyWorkflow.getCost());
			workflow.setWaitingTime(historyWorkflow.getWaitingTime());
			workflow.setWorkingTime(historyWorkflow.getWorkingTime());
			workflow.setValidFrom(historyWorkflow.getValidFrom());
			workflow.setValidTo(historyWorkflow.getValidTo());
			workflow.setDescription(historyWorkflow.getDescription());
			workflow.setHelp(historyWorkflow.getHelp());
			workflow.setQtyBatchSize(historyWorkflow.getQtyBatchSize());
			workflow.setOverlapUnits(historyWorkflow.getOverlapUnits());
			workflow.setYield(historyWorkflow.getYield());

			// 保存主数据更改
			workflow.saveEx();
		} catch (Exception e) {
			log.log(Level.SEVERE, "恢复工作流主数据失败", e);
			throw new RuntimeException("恢复工作流主数据失败: " + e.getMessage());
		}
	}

	/**
	 * 激活对应的工单工艺流程
	 */
	private void activateOrderWorkflow(MWorkflow workflow) {
		try {
			// 查找使用此产品工作流的所有工单
			String whereClause = "AD_Workflow_ID = ? AND DocStatus = ?";
			List<MPPOrder> orders = new Query(workflow.getCtx(), MPPOrder.Table_Name, whereClause,
					workflow.get_TrxName()).setParameters(workflow.getAD_Workflow_ID(), DocAction.STATUS_Drafted)
					.list();

			for (MPPOrder order : orders) {
				MPPOrderWorkflow orderWorkflow = order.getMPPOrderWorkflow();
				if (orderWorkflow != null && !orderWorkflow.isActive()) {
					// 重新激活工单工艺流程
					orderWorkflow.setIsActive(true);
					// 同步产品工作流的版本号到工单工艺流程
					orderWorkflow.setVersion(workflow.getVersion());
					orderWorkflow.saveEx();

					log.info("已激活工单 " + order.getDocumentNo() + " 的工艺流程");
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "激活工单工艺流程失败", e);
		}
	}
}