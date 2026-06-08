package org.libero.process;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFNodeNext;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPWFNodeAsset;
import org.libero.model.MPPWFNodeProduct;

/**
 * 复制工艺路线的所有子页签内容
 * @ClassName: CopyFromWorkflow
 * @author ldh
 * @date 2026年1月28日
 */
public class CopyFromWorkflow extends SvrProcess {
	private int p_Record_ID = 0;
	private int p_AD_Workflow_ID = 0;
	private int no = 0;
	private Properties ctx = Env.getCtx();
	private Map<Integer, Integer> nodeMap = new HashMap<>();

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Workflow_ID"))
				p_AD_Workflow_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
		p_Record_ID = getRecord_ID();
	}

	protected String doIt() throws Exception {
		log.info("From AD_Workflow_ID=" + p_AD_Workflow_ID + " to " + p_Record_ID);
		if (p_Record_ID == 0)
			throw new IllegalArgumentException("Target AD_Workflow_ID == 0");
		if (p_AD_Workflow_ID == 0)
			throw new IllegalArgumentException("Source AD_Workflow_ID == 0");
		if (p_Record_ID == p_AD_Workflow_ID)
			return "";

		MWorkflow fromWorkflow = new MWorkflow(ctx, p_AD_Workflow_ID, get_TrxName());
		MWorkflow toWorkflow = new MWorkflow(ctx, p_Record_ID, get_TrxName());

		// 检查目标工作流是否已有节点
		if (toWorkflow.getNodes(false, getAD_Client_ID()).length > 0) {
			throw new AdempiereSystemError("@Error@ 目标工艺路线已有节点，不允许复制！");
		}

		// 1. 复制工作流节点 (AD_WF_Node)
		copyWorkflowNodes(fromWorkflow, toWorkflow);

		// 2. 复制节点流转关系 (AD_WF_NodeNext)
		copyNodeTransitions(fromWorkflow, toWorkflow);

		// 3. 复制节点物料 (PP_WF_Node_Product)
		copyNodeProducts(fromWorkflow, toWorkflow);

		// 4. 复制节点工具 (PP_WF_Node_Asset)
		copyNodeAssets(fromWorkflow, toWorkflow);

		return "OK";
	}

	/**
	 * 复制工作流节点
	 * @Title: copyWorkflowNodes
	 * @param fromWorkflow
	 * @param toWorkflow
	 * @return void
	 */
	private void copyWorkflowNodes(MWorkflow fromWorkflow, MWorkflow toWorkflow) {
		MWFNode[] fromNodes = fromWorkflow.getNodes(false, getAD_Client_ID());

		for (MWFNode fromNode : fromNodes) {
			MWFNode toNode = new MWFNode(ctx, 0, get_TrxName());
			MWFNode.copyValues(fromNode, toNode);
			toNode.setAD_Workflow_ID(toWorkflow.getAD_Workflow_ID());
			toNode.saveEx();

			// 保存节点ID映射关系
			nodeMap.put(fromNode.getAD_WF_Node_ID(), toNode.getAD_WF_Node_ID());
			++no;
		}
	}

	/**
	 * 复制节点流转关系
	 * @Title: copyNodeTransitions
	 * @param fromWorkflow
	 * @param toWorkflow
	 * @return void
	 */
	private void copyNodeTransitions(MWorkflow fromWorkflow, MWorkflow toWorkflow) {
		MWFNode[] fromNodes = fromWorkflow.getNodes(false, getAD_Client_ID());

		for (MWFNode fromNode : fromNodes) {
			MWFNodeNext[] fromNexts = fromNode.getTransitions(getAD_Client_ID());
			for (MWFNodeNext fromNext : fromNexts) {
				MWFNodeNext toNext = new MWFNodeNext(ctx, 0, get_TrxName());
				MWFNodeNext.copyValues(fromNext, toNext);

				// 更新节点ID引用
				Integer fromNodeId = nodeMap.get(fromNext.getAD_WF_Node_ID());
				Integer fromNextId = nodeMap.get(fromNext.getAD_WF_Next_ID());

				if (fromNodeId != null && fromNextId != null) {
					toNext.setAD_WF_Node_ID(fromNodeId);
					toNext.setAD_WF_Next_ID(fromNextId);
					toNext.saveEx();
				}
			}
		}
	}

	/**
	 * 复制节点物料
	 * @Title: copyNodeProducts
	 * @param fromWorkflow
	 * @param toWorkflow
	 * @return void
	 */
	private void copyNodeProducts(MWorkflow fromWorkflow, MWorkflow toWorkflow) {
		MWFNode[] fromNodes = fromWorkflow.getNodes(false, getAD_Client_ID());

		for (MWFNode fromNode : fromNodes) {
			// 查询源节点的物料
			Collection<MPPWFNodeProduct> products = MPPWFNodeProduct.forAD_WF_Node_ID(ctx, fromNode.getAD_WF_Node_ID());

			// 获取对应的目标节点ID
			Integer toNodeId = nodeMap.get(fromNode.getAD_WF_Node_ID());
			if (toNodeId == null)
				continue;

			// 复制物料到目标节点
			for (MPPWFNodeProduct fromProduct : products) {
				MPPWFNodeProduct toProduct = new MPPWFNodeProduct(ctx, 0, get_TrxName());
				PO.copyValues(fromProduct, toProduct);
				toProduct.setAD_WF_Node_ID(toNodeId);
				toProduct.saveEx();
			}
		}
	}

	/**
	 * 复制节点工具
	 * @Title: copyNodeAssets
	 * @param fromWorkflow
	 * @param toWorkflow
	 * @return void
	 */
	private void copyNodeAssets(MWorkflow fromWorkflow, MWorkflow toWorkflow) {
		MWFNode[] fromNodes = fromWorkflow.getNodes(false, getAD_Client_ID());

		for (MWFNode fromNode : fromNodes) {
			// 查询源节点的工具
			Collection<MPPWFNodeAsset> assets = MPPWFNodeAsset.forAD_WF_Node_ID(ctx, fromNode.getAD_WF_Node_ID());

			// 获取对应的目标节点ID
			Integer toNodeId = nodeMap.get(fromNode.getAD_WF_Node_ID());
			if (toNodeId == null)
				continue;

			// 复制工具到目标节点
			for (MPPWFNodeAsset fromAsset : assets) {
				MPPWFNodeAsset toAsset = new MPPWFNodeAsset(ctx, 0, get_TrxName());
				PO.copyValues(fromAsset, toAsset);
				toAsset.setAD_WF_Node_ID(toNodeId);
				toAsset.saveEx();
			}
		}
	}

	@Override
	protected void postProcess(boolean success) {
		if (success) {
			addLog("成功复制 " + no + " 个节点及其相关数据");
		} else {
			addLog("复制失败");
		}
	}
}