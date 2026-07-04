package org.libero.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MDocType;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFProcess;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderNodeAsset;
import org.libero.model.MPPOrderNodeNext;
import org.libero.model.MPPOrderNodeProduct;
import org.libero.model.MPPOrderWorkflow;

/**
 * 重工申请流程 复制当前生产工单（表头 + 生产BOM + 工艺路线），创建重工工单
 */
@org.adempiere.base.annotation.Process
public class PPOrderReworkApplyProcess extends SvrProcess {

	private String p_ReworkReason = null;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("ReworkReason"))
				p_ReworkReason = para.getParameterAsString();
		}
	}

	@Override
	protected String doIt() throws Exception {
		// 1. 校验必填
		if (p_ReworkReason == null || p_ReworkReason.trim().isEmpty())
			throw new FillMandatoryException("ReworkReason");

		// 2. 获取原工单
		MPPOrder srcOrder = new MPPOrder(getCtx(), getRecord_ID(), get_TrxName());
		if (srcOrder.get_ID() <= 0)
			throw new IllegalArgumentException("工单不存在");

		// 3. 校验单据状态
		if (!MPPOrder.DOCSTATUS_Completed.equals(srcOrder.getDocStatus()))
			throw new IllegalStateException("只有已完成(CO)状态的工单才能发起重工申请");

//		// 4. 数量校验：重工工单数量 ≤ 订单数量（不与原工单累计）
//		validateQty(srcOrder);

		// 5. 生成重工工单号：原工单号 + -RW + 两位流水
		String newDocumentNo = generateReworkDocumentNo(srcOrder.getDocumentNo());

		// 6. 查找重工工单单据类型
		int reworkDocTypeId = getReworkDocTypeId();

		// 7. 复制工单表头
		MPPOrder newOrder = new MPPOrder(getCtx(), 0, get_TrxName());
		MPPOrder.copyValues(srcOrder, newOrder);

		// copyValues 会跳过 PP_Product_BOM_ID 和 AD_Workflow_ID，需手动设置
		newOrder.setPP_Product_BOM_ID(srcOrder.getPP_Product_BOM_ID());
		newOrder.setAD_Workflow_ID(srcOrder.getAD_Workflow_ID());
		newOrder.setDocumentNo(newDocumentNo);
		newOrder.setDocStatus("IP"); // 处理中状态
		newOrder.setC_OrderLine_ID(srcOrder.getC_OrderLine_ID()); // ← 新增这一行
		newOrder.set_ValueOfColumn("Orderstatus", "UnderReview"); // 工单状态：审批中
		newOrder.set_ValueOfColumn("ReworkReason", p_ReworkReason);
		// newOrder.set_ValueOfColumn("IsReworkOrder", true);
		newOrder.setC_DocTypeTarget_ID(reworkDocTypeId);
		newOrder.setC_DocType_ID(reworkDocTypeId);
		newOrder.setProcessed(false);
		newOrder.setProcessing(false);
		newOrder.setIsApproved(false);
		newOrder.setDocAction(MPPOrder.DOCACTION_Prepare);
		newOrder.setAD_Org_ID(srcOrder.getAD_Org_ID());
		// 重置运行时数量字段
		newOrder.setQtyDelivered(Env.ZERO);
		newOrder.setQtyReject(Env.ZERO);
		newOrder.setQtyScrap(Env.ZERO);
		newOrder.setQtyReserved(Env.ZERO);
		newOrder.setDateStart(null);
		newOrder.setDateFinish(null);
		newOrder.saveEx(); // 触发 afterSave → explosion()，自动从模板展开 BOM/工艺路线

		// 8. 删除 explosion() 自动生成的 BOM 和工艺路线
		deleteWorkflowAndBOM(newOrder);

		// 9. 从原工单复制生产 BOM（PP_Order_BOM + PP_Order_BOMLine）
		copyOrderBOM(srcOrder, newOrder);

		// 10. 从原工单复制工艺路线（PP_Order_Workflow + PP_Order_Node + 子记录）
		copyOrderWorkflow(srcOrder, newOrder);
		// 通过 Value 查找审批工作流
		MWorkflow wf = new Query(getCtx(), MWorkflow.Table_Name, "Value=? AND IsActive='Y'", get_TrxName())
				.setParameters("rework").first();
		if (wf != null && wf.getAD_Workflow_ID() > 0) {
			ProcessInfo pi = new ProcessInfo(wf.getName(), 0, MPPOrder.Table_ID, newOrder.get_ID());
			pi.setTransactionName(get_TrxName());
			pi.setPO(newOrder);
			MWFProcess wfProcess = new MWFProcess(wf, pi, get_TrxName());
			wfProcess.startWork();
			newOrder.saveEx();
		} else {
			throw new IllegalStateException("未找到审批工作流，请确认 Value='ReworkOrderApproval' 的工作流已配置");
		}
		return "重工工单已创建: " + newDocumentNo;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// 工单号生成
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * 生成重工工单号：原工单号 + -RW + 两位流水（如 MO260527045-RW01） 若已有 -RW
	 * 后缀，则继续递增（MO260527045-RW01 → MO260527045-RW02）
	 */
	private String generateReworkDocumentNo(String originalDocNo) {
		if (originalDocNo == null || originalDocNo.isEmpty())
			return originalDocNo;

		// 取基础单号（去掉已有的 -RW 后缀）
		String baseDocNo = originalDocNo.replaceAll("-RW\\d+$", "");

		// 查询已有的重工工单数量
		String sql = "SELECT COUNT(*) FROM PP_Order WHERE DocumentNo LIKE ? AND AD_Client_ID=?";
		int count = DB.getSQLValueEx(get_TrxName(), sql, baseDocNo + "-RW%", Env.getAD_Client_ID(getCtx()));

		return baseDocNo + "-RW" + String.format("%02d", count + 1);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// 数量校验
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * 重工工单数量 ≤ 订单数量（不与原工单数量累计）
	 */
	private void validateQty(MPPOrder srcOrder) {
		int orderLineId = srcOrder.getC_OrderLine_ID();
		if (orderLineId <= 0)
			return; // 无关联销售订单行，不限制

		java.math.BigDecimal qtyOrdered = DB.getSQLValueBDEx(get_TrxName(),
				"SELECT QtyOrdered FROM C_OrderLine WHERE C_OrderLine_ID=?", orderLineId);
		if (qtyOrdered == null)
			return;

		if (srcOrder.getQtyOrdered().compareTo(qtyOrdered) > 0) {
			throw new IllegalArgumentException(
					"重工工单数量 (" + srcOrder.getQtyOrdered() + ") 不能超过订单数量 (" + qtyOrdered + ")");
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// 单据类型
	// ─────────────────────────────────────────────────────────────────────────

	private int getReworkDocTypeId() {
		int id = DB.getSQLValueEx(get_TrxName(),
				"SELECT C_DocType_ID FROM C_DocType WHERE DocBaseType=? AND Name=? AND AD_Client_ID=?",
				MDocType.DOCBASETYPE_ManufacturingOrder, "重工工单", Env.getAD_Client_ID(getCtx()));
		if (id <= 0)
			throw new IllegalStateException("未找到重工工单单据类型，请先配置 Name='重工工单' 的 C_DocType 记录");
		return id;
	}
	// ─────────────────────────────────────────────────────────────────────────
	// 删除 explosion() 自动生成的 BOM 和工艺路线
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * 参考 MPPOrder.deleteWorkflowAndBOM()（private），用 SQL 实现相同逻辑
	 */
	private void deleteWorkflowAndBOM(MPPOrder order) {
		int orderId = order.get_ID();
		int clientId = order.getAD_Client_ID();
		Object[] params = new Object[] { orderId, clientId };
		String trxName = get_TrxName();

		// 先删除所有翻译记录（必须在删除主表之前）
		DB.executeUpdateEx(
				"DELETE FROM PP_Order_BOM_Trl WHERE PP_Order_BOM_ID IN "
						+ "(SELECT PP_Order_BOM_ID FROM PP_Order_BOM WHERE PP_Order_ID=? AND AD_Client_ID=?)",
				params, trxName);
		// ↓ 新增这一行
		DB.executeUpdateEx(
				"DELETE FROM PP_Order_BOMLine_Trl WHERE PP_Order_BOMLine_ID IN "
						+ "(SELECT PP_Order_BOMLine_ID FROM PP_Order_BOMLine WHERE PP_Order_ID=? AND AD_Client_ID=?)",
				params, trxName);
		DB.executeUpdateEx(
				"DELETE FROM PP_Order_Workflow_Trl WHERE PP_Order_Workflow_ID IN "
						+ "(SELECT PP_Order_Workflow_ID FROM PP_Order_Workflow WHERE PP_Order_ID=? AND AD_Client_ID=?)",
				params, trxName);
		DB.executeUpdateEx(
				"DELETE FROM PP_Order_Node_Trl WHERE PP_Order_Node_ID IN "
						+ "(SELECT PP_Order_Node_ID FROM PP_Order_Node WHERE PP_Order_ID=? AND AD_Client_ID=?)",
				params, trxName);

		// 重置工艺路线起始节点
		DB.executeUpdateEx("UPDATE PP_Order_Workflow SET PP_Order_Node_ID=NULL WHERE PP_Order_ID=? AND AD_Client_ID=?",
				params, trxName);

		// 删除工艺路线
		DB.executeUpdateEx("DELETE FROM PP_Order_Node_Asset WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_Node_Product WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_NodeNext WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_Node WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_Workflow WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);

		// 删除 BOM 明细和 BOM
		DB.executeUpdateEx("DELETE FROM PP_Order_BOMLine WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);
		DB.executeUpdateEx("DELETE FROM PP_Order_BOM WHERE PP_Order_ID=? AND AD_Client_ID=?", params, trxName);
	}
	// ─────────────────────────────────────────────────────────────────────────
	// 复制生产 BOM
	// ─────────────────────────────────────────────────────────────────────────

	private void copyOrderBOM(MPPOrder srcOrder, MPPOrder newOrder) {
		List<MPPOrderBOM> srcBOMs = new Query(getCtx(), MPPOrderBOM.Table_Name,
				MPPOrderBOM.COLUMNNAME_PP_Order_ID + "=?", get_TrxName()).setParameters(srcOrder.get_ID()).list();

		for (MPPOrderBOM srcBOM : srcBOMs) {
			// 复制 BOM 表头
			MPPOrderBOM newBOM = new MPPOrderBOM(getCtx(), 0, get_TrxName());
			MPPOrderBOM.copyValues(srcBOM, newBOM);
			newBOM.setPP_Order_ID(newOrder.get_ID());
			newBOM.setAD_Org_ID(newOrder.getAD_Org_ID());
			newBOM.saveEx();

			// 复制 BOM 明细
			for (MPPOrderBOMLine srcLine : srcBOM.getLines()) {
				MPPOrderBOMLine newLine = new MPPOrderBOMLine(getCtx(), 0, get_TrxName());
				MPPOrderBOMLine.copyValues(srcLine, newLine);
				newLine.setPP_Order_ID(newOrder.get_ID());
				newLine.setPP_Order_BOM_ID(newBOM.get_ID());
				newLine.setAD_Org_ID(newOrder.getAD_Org_ID());
				// 重置运行时字段
				newLine.setQtyDelivered(Env.ZERO);
				newLine.setQtyReserved(Env.ZERO);
				newLine.setQtyPost(Env.ZERO);
				newLine.setQtyReject(Env.ZERO);
				newLine.setQtyScrap(Env.ZERO);
				newLine.saveEx();
			}
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// 复制工艺路线
	// ─────────────────────────────────────────────────────────────────────────

	private void copyOrderWorkflow(MPPOrder srcOrder, MPPOrder newOrder) {
		List<MPPOrderWorkflow> srcWorkflows = new Query(getCtx(), MPPOrderWorkflow.Table_Name,
				MPPOrderWorkflow.COLUMNNAME_PP_Order_ID + "=?", get_TrxName()).setParameters(srcOrder.get_ID()).list();

		for (MPPOrderWorkflow srcWF : srcWorkflows) {
			// 复制工艺路线表头（先不设置起始节点，避免外键问题）
			MPPOrderWorkflow newWF = new MPPOrderWorkflow(getCtx(), 0, get_TrxName());
			MPPOrderWorkflow.copyValues(srcWF, newWF);
			newWF.setPP_Order_ID(newOrder.get_ID());
			newWF.setAD_Org_ID(newOrder.getAD_Org_ID());
			newWF.setPP_Order_Node_ID(0); // 先置空，最后再设置
			newWF.saveEx();

			// 复制节点，记录 旧PP_Order_Node_ID → 新PP_Order_Node_ID 的映射
			Map<Integer, Integer> nodeMapping = new HashMap<>();
			MPPOrderNode[] srcNodes = srcWF.getNodes(false, srcWF.getAD_Client_ID());

			for (MPPOrderNode srcNode : srcNodes) {
				MPPOrderNode newNode = new MPPOrderNode(getCtx(), 0, get_TrxName());
				MPPOrderNode.copyValues(srcNode, newNode);
				newNode.setPP_Order_ID(newOrder.get_ID());
				newNode.setPP_Order_Workflow_ID(newWF.get_ID());
				newNode.setAD_Org_ID(newOrder.getAD_Org_ID());
				// 重置运行时字段
				newNode.setDocStatus(MPPOrderNode.DOCSTATUS_Drafted);
				newNode.setDocAction(MPPOrderNode.DOCACTION_Complete);
				newNode.setQtyDelivered(Env.ZERO);
				newNode.setDurationReal(Env.ZERO);
				newNode.setSetupTimeReal(Env.ZERO);
				newNode.saveEx();

				nodeMapping.put(srcNode.get_ID(), newNode.get_ID());

				// 复制节点资产（PP_Order_Node_Asset）
				copyNodeAssets(srcNode, newNode, newOrder, newWF);
				// 复制节点产品（PP_Order_Node_Product）
				copyNodeProducts(srcNode, newNode, newOrder, newWF);
			}

			// 复制节点转换关系（PP_Order_NodeNext），使用映射更新 Next ID
			for (MPPOrderNode srcNode : srcNodes) {
				Integer newNodeId = nodeMapping.get(srcNode.get_ID());
				if (newNodeId == null)
					continue;
				MPPOrderNode newNode = new MPPOrderNode(getCtx(), newNodeId, get_TrxName());

				for (MPPOrderNodeNext srcNext : srcNode.getTransitions(srcWF.getAD_Client_ID())) {
					Integer newNextId = nodeMapping.get(srcNext.getPP_Order_Next_ID());
					if (newNextId == null)
						continue;

					MPPOrderNodeNext newNext = new MPPOrderNodeNext(getCtx(), 0, get_TrxName());
					MPPOrderNodeNext.copyValues(srcNext, newNext);
					newNext.setPP_Order_ID(newOrder.get_ID());
					newNext.setPP_Order_Node_ID(newNode.get_ID());
					newNext.setPP_Order_Next_ID(newNextId);
					newNext.setAD_Org_ID(newOrder.getAD_Org_ID());
					newNext.saveEx();
				}
			}

			// 最后设置工艺路线起始节点
			Integer newStartNodeId = nodeMapping.get(srcWF.getPP_Order_Node_ID());
			if (newStartNodeId != null) {
				newWF.setPP_Order_Node_ID(newStartNodeId);
				newWF.saveEx();
			}
		}
	}

	private void copyNodeAssets(MPPOrderNode srcNode, MPPOrderNode newNode, MPPOrder newOrder, MPPOrderWorkflow newWF) {
		List<MPPOrderNodeAsset> assets = new Query(getCtx(), MPPOrderNodeAsset.Table_Name, "PP_Order_Node_ID=?",
				get_TrxName()).setParameters(srcNode.get_ID()).list();
		for (MPPOrderNodeAsset src : assets) {
			MPPOrderNodeAsset dst = new MPPOrderNodeAsset(getCtx(), 0, get_TrxName());
			MPPOrderNodeAsset.copyValues(src, dst);
			dst.setPP_Order_ID(newOrder.get_ID());
			dst.setPP_Order_Workflow_ID(newWF.get_ID());
			dst.setPP_Order_Node_ID(newNode.get_ID());
			dst.setAD_Org_ID(newOrder.getAD_Org_ID());
			dst.saveEx();
		}
	}

	private void copyNodeProducts(MPPOrderNode srcNode, MPPOrderNode newNode, MPPOrder newOrder,
			MPPOrderWorkflow newWF) {
		List<MPPOrderNodeProduct> products = new Query(getCtx(), MPPOrderNodeProduct.Table_Name, "PP_Order_Node_ID=?",
				get_TrxName()).setParameters(srcNode.get_ID()).list();
		for (MPPOrderNodeProduct src : products) {
			MPPOrderNodeProduct dst = new MPPOrderNodeProduct(getCtx(), 0, get_TrxName());
			MPPOrderNodeProduct.copyValues(src, dst);
			dst.setPP_Order_ID(newOrder.get_ID());
			dst.setPP_Order_Workflow_ID(newWF.get_ID());
			dst.setPP_Order_Node_ID(newNode.get_ID());
			dst.setAD_Org_ID(newOrder.getAD_Org_ID());
			dst.saveEx();
		}
	}
}