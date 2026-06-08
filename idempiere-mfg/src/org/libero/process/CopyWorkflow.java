package org.libero.process;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPWFNodeAsset;
import org.libero.model.MPPWFNodeProduct;

public class CopyWorkflow extends SvrProcess {

	private int p_Record_ID = 0;
	private int nodeCount = 0;
	private int nodeProductCount = 0;
	private int nodeAssetCount = 0;
	private int nodeNextCount = 0;

	@Override
	protected void prepare() {
		p_Record_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		Properties ctx = getCtx();
		String trxName = get_TrxName();

		// =============================================
		// Step 1: 加载源工艺路线
		// =============================================
		MWorkflow fromWF = new MWorkflow(ctx, p_Record_ID, trxName);
		if (fromWF.getAD_Workflow_ID() <= 0) {
			throw new Exception("找不到工艺路线记录: " + p_Record_ID);
		}

		// =============================================
		// Step 2: 复制 AD_Workflow 主记录
		// =============================================
		MWorkflow newWF = new MWorkflow(ctx, 0, trxName);
		PO.copyValues(fromWF, newWF);

		// 起始节点先置 null（循环引用，Step 5 再更新）
		newWF.set_ValueOfColumn("AD_WF_Node_ID", null);

		// value 拼接时间戳，name 拼接时间戳保证唯一
		String ts = String.valueOf(System.currentTimeMillis());
		newWF.setValue(fromWF.getValue() + "-" + ts);
		newWF.setName(fromWF.getName() + "-" + ts);

		// 重置状态字段
		newWF.setDocStatus("DR");
		newWF.setProcessed(false);
		newWF.setIsApproved(false);
		newWF.setIsValid(false);

		// 保持与原记录相同的组织
		newWF.setAD_Org_ID(fromWF.getAD_Org_ID());

		newWF.saveEx();

		// =============================================
		// Step 3: 复制 AD_WF_Node（第一遍：保存节点，建立 ID 映射）
		// 按 value 升序排序，保证新节点 AD_WF_Node_ID 与原 value 顺序一致，
		// 使 renumberAndUpdateStartNode 重编号后结果与原工艺路线匹配
		// =============================================
		Map<Integer, Integer> nodeIdMap = new HashMap<>();

		MWFNode[] fromNodes = fromWF.getNodes(false, getAD_Client_ID());
		Arrays.sort(fromNodes, (a, b) -> a.getValue().compareTo(b.getValue()));
		for (MWFNode fromNode : fromNodes) {
			MWFNode newNode = new MWFNode(ctx, 0, trxName);
			PO.copyValues(fromNode, newNode);
			newNode.setAD_Workflow_ID(newWF.getAD_Workflow_ID());
			newNode.setAD_Org_ID(fromNode.getAD_Org_ID()); // ← 显式保持原组织
			newNode.setValue(fromNode.getValue()); // 显式设置，防止 IsAllowCopy='N' 跳过
			newNode.setName(fromNode.getName()); // 同上
			newNode.saveEx();
			nodeCount++;

			// 记录旧节点 ID → 新节点 ID 的映射
			nodeIdMap.put(fromNode.getAD_WF_Node_ID(), newNode.getAD_WF_Node_ID());

			// Step 3a: 复制 PP_WF_Node_Product（工序物料）
			Collection<MPPWFNodeProduct> fromProducts = MPPWFNodeProduct.forAD_WF_Node_ID(ctx,
					fromNode.getAD_WF_Node_ID());
			for (MPPWFNodeProduct fromNP : fromProducts) {
				MPPWFNodeProduct newNP = new MPPWFNodeProduct(ctx, 0, trxName);
				PO.copyValues(fromNP, newNP);
				newNP.setAD_WF_Node_ID(newNode.getAD_WF_Node_ID());
				newNP.setAD_Org_ID(fromNP.getAD_Org_ID()); // ← 显式保持原组织
				newNP.saveEx();
				nodeProductCount++;
			}

			// Step 3b: 复制 PP_WF_Node_Asset（工序工具）
			Collection<MPPWFNodeAsset> fromAssets = MPPWFNodeAsset.forAD_WF_Node_ID(ctx, fromNode.getAD_WF_Node_ID());
			for (MPPWFNodeAsset fromNA : fromAssets) {
				MPPWFNodeAsset newNA = new MPPWFNodeAsset(ctx, 0, trxName);
				PO.copyValues(fromNA, newNA);
				newNA.setAD_WF_Node_ID(newNode.getAD_WF_Node_ID());
				newNA.setAD_Org_ID(fromNA.getAD_Org_ID()); // ← 显式保持原组织
				newNA.saveEx();
				nodeAssetCount++;
			}
		}

		// =============================================
		// Step 4: 复制 AD_WF_NodeNext（第二遍：所有节点已保存，ID 映射完整）
		// =============================================
		for (MWFNode fromNode : fromNodes) {
			Integer newSourceNodeId = nodeIdMap.get(fromNode.getAD_WF_Node_ID());
			if (newSourceNodeId == null)
				continue;

			MWFNodeNext[] fromNexts = fromNode.getTransitions(getAD_Client_ID());
			for (MWFNodeNext fromNext : fromNexts) {
				Integer newTargetNodeId = nodeIdMap.get(fromNext.getAD_WF_Next_ID());
				if (newTargetNodeId == null)
					continue;

				MWFNodeNext newNext = new MWFNodeNext(ctx, 0, trxName);
				PO.copyValues(fromNext, newNext);
				newNext.setAD_WF_Node_ID(newSourceNodeId);
				newNext.setAD_WF_Next_ID(newTargetNodeId);
				newNext.setAD_Org_ID(fromNext.getAD_Org_ID()); // ← 显式保持原组织
				newNext.saveEx();
				nodeNextCount++;
			}
		}

		// =============================================
		// Step 5: 更新 AD_Workflow 起始节点
		// =============================================
		Integer newFirstNodeId = nodeIdMap.get(fromWF.getAD_WF_Node_ID());
		if (newFirstNodeId != null && newFirstNodeId > 0) {
			newWF.setAD_WF_Node_ID(newFirstNodeId);
			newWF.saveEx();
		}

		return "已复制工艺路线: " + fromWF.getValue() + " → " + newWF.getValue() + "，工序 " + nodeCount + " 条" + "，工序物料 "
				+ nodeProductCount + " 条" + "，工序工具 " + nodeAssetCount + " 条" + "，流转 " + nodeNextCount + " 条";
	}

	@Override
	protected void postProcess(boolean success) {
		this.addLog("@Copied@ Node=" + nodeCount + ", NodeProduct=" + nodeProductCount + ", NodeAsset=" + nodeAssetCount
				+ ", NodeNext=" + nodeNextCount);
	}
}