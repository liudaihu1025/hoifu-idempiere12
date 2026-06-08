package org.libero.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MSequence;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderNodeAsset;
import org.libero.model.MPPOrderNodeNext;
import org.libero.model.MPPOrderNodeProduct;
import org.libero.model.MPPOrderWorkflow;
import org.libero.tables.X_PP_Order_Node_Asset;
import org.libero.tables.X_PP_Order_Node_Product;

/**
 * CopyPPOrder - 复制生产工单及其所有子页签
 * 
 * 复制层级： PP_Order ├── PP_Order_BOM │ └── PP_Order_BOMLine └── PP_Order_Workflow
 * └── PP_Order_Node ├── PP_Order_NodeNext ├── PP_Order_Node_Product └──
 * PP_Order_Node_Asset
 */
public class CopyPPOrder extends SvrProcess {

	private int p_Record_ID = 0;

	// 计数器
	private int bomCount = 0;
	private int bomLineCount = 0;
	private int workflowCount = 0;
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
		// Step 1: 加载原工单
		// =============================================
		MPPOrder fromOrder = new MPPOrder(ctx, p_Record_ID, trxName);
		if (fromOrder.getPP_Order_ID() <= 0) {
			throw new Exception("找不到工单记录: " + p_Record_ID);
		}

		// =============================================
		// Step 2: 复制 PP_Order 主记录
		// =============================================
		MPPOrder newOrder = new MPPOrder(ctx, 0, trxName);
		PO.copyValues(fromOrder, newOrder);

		// IsAllowCopy='N' 的必填字段，需手动补上
		newOrder.setAD_Org_ID(fromOrder.getAD_Org_ID());
		newOrder.setPP_Product_BOM_ID(fromOrder.getPP_Product_BOM_ID());
		newOrder.setAD_Workflow_ID(fromOrder.getAD_Workflow_ID());

		// 生成新文档号（按 C_DocType 关联的序列，保持 MO+日期 格式）
		int docTypeId = fromOrder.getC_DocType_ID() > 0 ? fromOrder.getC_DocType_ID()
				: fromOrder.getC_DocTypeTarget_ID();
		String newDocNo = null;
		if (docTypeId > 0) {
			newDocNo = MSequence.getDocumentNo(docTypeId, trxName, false, newOrder);
		}
		if (newDocNo == null) {
			newDocNo = fromOrder.getDocumentNo() + "-COPY";
		}
		newOrder.setDocumentNo(newDocNo);

		// 重置状态为草稿
		newOrder.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
		newOrder.setDocAction(MPPOrder.DOCACTION_Prepare);
		newOrder.setProcessed(false);
		newOrder.setIsApproved(false);
		newOrder.setProcessing(false);
		newOrder.setPosted(false);
		newOrder.setQtyDelivered(org.compiere.util.Env.ZERO);
		newOrder.setQtyScrap(org.compiere.util.Env.ZERO);
		newOrder.setQtyReject(org.compiere.util.Env.ZERO);
		newOrder.setDateStart(null);
		newOrder.setDateFinish(null);

		// 保存主记录（afterSave → explosion() 自动创建第一批子记录）
		newOrder.saveEx();

		// =============================================
		// Step 3: 删除 explosion() 自动创建的第一批子记录
		// 必须先删翻译表，再删主表，避免 FK 约束失败
		// =============================================
		String delWhere = "PP_Order_ID=? AND AD_Client_ID=?";
		Object[] delParams = new Object[] { newOrder.getPP_Order_ID(), newOrder.getAD_Client_ID() };

		// 先清空 PP_Order_Workflow 的起始节点指针（避免循环引用 FK）
		DB.executeUpdateEx("UPDATE PP_Order_Workflow SET PP_Order_Node_ID=NULL WHERE " + delWhere, delParams, trxName);

		// 删除工艺路线子记录（无翻译表）
		DB.executeUpdateEx("DELETE FROM PP_Order_Node_Asset   WHERE " + delWhere, delParams, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_Node_Product WHERE " + delWhere, delParams, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_NodeNext     WHERE " + delWhere, delParams, trxName);

		// 删除 PP_Order_Node 翻译表，再删主表
		DB.executeUpdateEx("DELETE FROM PP_Order_Node_Trl WHERE PP_Order_Node_ID IN "
				+ "(SELECT PP_Order_Node_ID FROM PP_Order_Node WHERE " + delWhere + ")", delParams, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_Node WHERE " + delWhere, delParams, trxName);

		// 删除 PP_Order_Workflow 翻译表，再删主表
		DB.executeUpdateEx(
				"DELETE FROM PP_Order_Workflow_Trl WHERE PP_Order_Workflow_ID IN "
						+ "(SELECT PP_Order_Workflow_ID FROM PP_Order_Workflow WHERE " + delWhere + ")",
				delParams, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_Workflow WHERE " + delWhere, delParams, trxName);

		// 删除 PP_Order_BOMLine 翻译表，再删主表
		DB.executeUpdateEx(
				"DELETE FROM PP_Order_BOMLine_Trl WHERE PP_Order_BOMLine_ID IN "
						+ "(SELECT PP_Order_BOMLine_ID FROM PP_Order_BOMLine WHERE " + delWhere + ")",
				delParams, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_BOMLine WHERE " + delWhere, delParams, trxName);

		// 删除 PP_Order_BOM 翻译表，再删主表
		DB.executeUpdateEx("DELETE FROM PP_Order_BOM_Trl WHERE PP_Order_BOM_ID IN "
				+ "(SELECT PP_Order_BOM_ID FROM PP_Order_BOM WHERE " + delWhere + ")", delParams, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_BOM WHERE " + delWhere, delParams, trxName);

		// =============================================
		// Step 4: 复制 PP_Order_BOM
		// =============================================
		MPPOrderBOM fromBOM = new Query(ctx, MPPOrderBOM.Table_Name, MPPOrderBOM.COLUMNNAME_PP_Order_ID + "=?", trxName)
				.setParameters(fromOrder.getPP_Order_ID()).firstOnly();

		if (fromBOM != null) {
			MPPOrderBOM newBOM = new MPPOrderBOM(ctx, 0, trxName);
			PO.copyValues(fromBOM, newBOM);
			newBOM.setAD_Org_ID(fromBOM.getAD_Org_ID());
			newBOM.setPP_Order_ID(newOrder.getPP_Order_ID());
			newBOM.saveEx();
			bomCount++;

			// =============================================
			// Step 5: 复制 PP_Order_BOMLine
			// =============================================
			for (MPPOrderBOMLine fromLine : fromBOM.getLines()) {
				MPPOrderBOMLine newLine = new MPPOrderBOMLine(ctx, 0, trxName);
				PO.copyValues(fromLine, newLine);
				newLine.setAD_Org_ID(fromLine.getAD_Org_ID());
				newLine.setPP_Order_BOM_ID(newBOM.getPP_Order_BOM_ID());
				newLine.setPP_Order_ID(newOrder.getPP_Order_ID());
				newLine.saveEx();
				bomLineCount++;
			}
		}

		// =============================================
		// Step 6: 复制 PP_Order_Workflow
		// =============================================
		List<MPPOrderWorkflow> fromWFList = new Query(ctx, MPPOrderWorkflow.Table_Name,
				MPPOrderWorkflow.COLUMNNAME_PP_Order_ID + "=?", trxName).setParameters(fromOrder.getPP_Order_ID())
				.list();

		for (MPPOrderWorkflow fromWF : fromWFList) {

			MPPOrderWorkflow newWF = new MPPOrderWorkflow(ctx, 0, trxName);
			PO.copyValues(fromWF, newWF);
			newWF.setAD_Org_ID(fromWF.getAD_Org_ID());
			newWF.setPP_Order_ID(newOrder.getPP_Order_ID());
			newWF.setPP_Order_Node_ID(0); // 先置 0，避免循环引用，Step 9 再更新
			newWF.saveEx();
			workflowCount++;

			// nodeIdMap: 旧 PP_Order_Node_ID → 新 PP_Order_Node_ID
			Map<Integer, Integer> nodeIdMap = new HashMap<>();

			// =============================================
			// Step 7: 复制 PP_Order_Node（第一遍：保存节点，建立 ID 映射）
			// =============================================
			List<MPPOrderNode> fromNodes = new Query(ctx, MPPOrderNode.Table_Name,
					MPPOrderNode.COLUMNNAME_PP_Order_Workflow_ID + "=?", trxName)
					.setParameters(fromWF.getPP_Order_Workflow_ID()).list();

			for (MPPOrderNode fromNode : fromNodes) {
				MPPOrderNode newNode = new MPPOrderNode(ctx, 0, trxName);
				PO.copyValues(fromNode, newNode);
				newNode.setAD_Org_ID(fromNode.getAD_Org_ID());
				newNode.setPP_Order_ID(newOrder.getPP_Order_ID());
				newNode.setPP_Order_Workflow_ID(newWF.getPP_Order_Workflow_ID());
				newNode.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
				newNode.saveEx();
				nodeCount++;

				nodeIdMap.put(fromNode.getPP_Order_Node_ID(), newNode.getPP_Order_Node_ID());

				// =============================================
				// Step 7a: 复制 PP_Order_Node_Product（工序物料）
				// =============================================
				List<MPPOrderNodeProduct> fromProducts = new Query(ctx, MPPOrderNodeProduct.Table_Name,
						X_PP_Order_Node_Product.COLUMNNAME_PP_Order_Node_ID + "=?", trxName)
						.setParameters(fromNode.getPP_Order_Node_ID()).list();
				for (MPPOrderNodeProduct fromNP : fromProducts) {
					MPPOrderNodeProduct newNP = new MPPOrderNodeProduct(ctx, 0, trxName);
					PO.copyValues(fromNP, newNP);
					newNP.setAD_Org_ID(fromNP.getAD_Org_ID());
					newNP.setPP_Order_ID(newOrder.getPP_Order_ID());
					newNP.setPP_Order_Workflow_ID(newWF.getPP_Order_Workflow_ID());
					newNP.setPP_Order_Node_ID(newNode.getPP_Order_Node_ID());
					newNP.saveEx();
					nodeProductCount++;
				}

				// =============================================
				// Step 7b: 复制 PP_Order_Node_Asset（工序资产）
				// =============================================
				List<MPPOrderNodeAsset> fromAssets = new Query(ctx, MPPOrderNodeAsset.Table_Name,
						X_PP_Order_Node_Asset.COLUMNNAME_PP_Order_Node_ID + "=?", trxName)
						.setParameters(fromNode.getPP_Order_Node_ID()).list();
				for (MPPOrderNodeAsset fromNA : fromAssets) {
					MPPOrderNodeAsset newNA = new MPPOrderNodeAsset(ctx, 0, trxName);
					PO.copyValues(fromNA, newNA);
					newNA.setAD_Org_ID(fromNA.getAD_Org_ID());
					newNA.setPP_Order_ID(newOrder.getPP_Order_ID());
					newNA.setPP_Order_Workflow_ID(newWF.getPP_Order_Workflow_ID());
					newNA.setPP_Order_Node_ID(newNode.getPP_Order_Node_ID());
					newNA.saveEx();
					nodeAssetCount++;
				}
			}

			// =============================================
			// Step 8: 复制 PP_Order_NodeNext（流转，第二遍：所有节点已保存，ID 映射完整）
			// =============================================
			for (MPPOrderNode fromNode : fromNodes) {
				Integer newNodeId = nodeIdMap.get(fromNode.getPP_Order_Node_ID());
				if (newNodeId == null)
					continue;

				List<MPPOrderNodeNext> fromNexts = new Query(ctx, MPPOrderNodeNext.Table_Name,
						MPPOrderNodeNext.COLUMNNAME_PP_Order_Node_ID + "=?", trxName)
						.setParameters(fromNode.getPP_Order_Node_ID()).list();

				for (MPPOrderNodeNext fromNext : fromNexts) {
					MPPOrderNodeNext newNext = new MPPOrderNodeNext(ctx, 0, trxName);
					PO.copyValues(fromNext, newNext);
					newNext.setAD_Org_ID(fromNext.getAD_Org_ID());
					newNext.setPP_Order_ID(newOrder.getPP_Order_ID());
					newNext.setPP_Order_Node_ID(newNodeId); // 源节点：用映射替换

					// 目标节点：优先通过 AD_WF_Next_ID 自动解析
					newNext.setPP_Order_Next_ID();
					if (newNext.getPP_Order_Next_ID() <= 0) {
						// 降级：用 nodeIdMap 手动映射
						Integer newNextId = nodeIdMap.get(fromNext.getPP_Order_Next_ID());
						if (newNextId != null) {
							newNext.setPP_Order_Next_ID(newNextId);
						}
					}
					newNext.saveEx();
					nodeNextCount++;
				}
			}

			// =============================================
			// Step 9: 更新 PP_Order_Workflow 的起始节点
			// =============================================
			Integer newFirstNodeId = nodeIdMap.get(fromWF.getPP_Order_Node_ID());
			if (newFirstNodeId != null && newFirstNodeId > 0) {
				newWF.setPP_Order_Node_ID(newFirstNodeId);
				newWF.saveEx();
			}
		}

		return "已复制工单: " + fromOrder.getDocumentNo() + " → " + newOrder.getDocumentNo() + "，BOM " + bomCount + " 条"
				+ "，BOM子件 " + bomLineCount + " 条" + "，工艺路线 " + workflowCount + " 条" + "，工序 " + nodeCount + " 条"
				+ "，工序物料 " + nodeProductCount + " 条" + "，工序资产 " + nodeAssetCount + " 条" + "，流转 " + nodeNextCount + " 条";
	}

	@Override
	protected void postProcess(boolean success) {
		this.addLog("@Copied@ BOM=" + bomCount + ", BOMLine=" + bomLineCount + ", Workflow=" + workflowCount + ", Node="
				+ nodeCount + ", NodeProduct=" + nodeProductCount + ", NodeAsset=" + nodeAssetCount + ", NodeNext="
				+ nodeNextCount);
	}
}