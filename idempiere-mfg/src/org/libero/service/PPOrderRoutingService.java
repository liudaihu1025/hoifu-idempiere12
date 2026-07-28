// idempiere-mfg/src/org/libero/service/PPOrderRoutingService.java  
package org.libero.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWorkflow;
import org.libero.form.vo.RoutingNodeVO;
import org.libero.model.MPPOrderNode;

/**
 * 工艺路线模板（AD_Workflow + MWFNode）及工单工序操作服务
 */
public class PPOrderRoutingService {

	private static final CLogger log = CLogger.getCLogger(PPOrderRoutingService.class);

	/**
	 * 创建或更新工艺路线模板。
	 * 
	 * @param existingWorkflowId 已关联的 AD_Workflow_ID，0 表示新建
	 * @return 保存后的 AD_Workflow_ID
	 */
	public int createOrUpdateRoutingTemplate(Properties ctx, String trxName, int orgId, int productId,
			Timestamp dateStart, String createdByName, int existingWorkflowId, List<RoutingNodeVO> routingNodes) {

		MProduct product = MProduct.get(ctx, productId);
		String dateStr = new java.text.SimpleDateFormat("yyMMddHHmmssSSS").format(new java.util.Date());

		MWorkflow workflow;
		if (existingWorkflowId > 0) {
			workflow = new MWorkflow(ctx, existingWorkflowId, trxName);
			// 删除旧节点
			for (MWFNode oldNode : workflow.getNodes(true, workflow.getAD_Client_ID())) {
				oldNode.deleteEx(false, trxName);
			}
		} else {
			workflow = new MWorkflow(ctx, 0, trxName);
			workflow.setAD_Org_ID(orgId);
			// 编码 = 系统字段编码（自动生成）
			workflow.setValue(product.getValue() + "_" + dateStr);
			workflow.setWorkflowType(MWorkflow.WORKFLOWTYPE_Manufacturing);
			workflow.setDurationUnit(MWorkflow.DURATIONUNIT_Minute);
			workflow.setAuthor(createdByName);
			workflow.setDuration(1);
			workflow.setQtyBatchSize(BigDecimal.ONE);
		}

		// 名称 = 产品名称 + 日期(yyMMddHHmmssSSS)
		workflow.setName(product.getName() + dateStr);
		// 描述 = 产品编码 + 产品名称 + 日期 + 创建人
		workflow.setDescription(product.getValue() + " " + product.getName() + " " + dateStr + " " + createdByName);
		workflow.setValidFrom(dateStart);
		workflow.setPublishStatus(MWorkflow.PUBLISHSTATUS_UnderRevision); // "U"
		workflow.set_ValueOfColumn("VersionStatus", "C"); // Current
		workflow.saveEx(trxName);

		// 写入工序节点
		int firstNodeId = 0;
		for (RoutingNodeVO vo : routingNodes) {
			if (vo.isDeleted)
				continue;

			// 从 AD_Routing_Node 取名称和编码
			String nodeName = DB.getSQLValueString(trxName,
					"SELECT Name FROM AD_Routing_Node WHERE AD_Routing_Node_ID=?", vo.adRoutingNodeId);

			String nodeValue = vo.routingNodeValue;
			MWFNode node = new MWFNode(workflow, nodeValue, nodeName);
			node.setAD_Org_ID(orgId);
			node.setValue(nodeValue);
			node.setName(nodeName != null ? nodeName : vo.routingNodeName);
			node.setDescription(vo.description);
			node.setAction(MWFNode.ACTION_UserWorkbench);
			node.setDuration(1);
//			node.setDurationUnit(MWorkflow.DURATIONUNIT_Minute);
			node.set_ValueOfColumn("AD_Routing_Node_ID", vo.adRoutingNodeId);
			node.saveEx(trxName);

			vo.adWFNodeId = node.getAD_WF_Node_ID();
			if (firstNodeId == 0)
				firstNodeId = node.getAD_WF_Node_ID();
		}
		// 设置起始节点
		if (firstNodeId > 0) {
			workflow.setAD_WF_Node_ID(firstNodeId);
		}
		workflow.setIsValid(true);
		workflow.setPublishStatus(MWorkflow.PUBLISHSTATUS_Released); // "R"
		workflow.set_ValueOfColumn("VersionStatus", "C"); // Current
		workflow.saveEx(trxName);

		return workflow.getAD_Workflow_ID();
	}

	/**
	 * 计算工序数量（与 Callout_PP_Order_Node 逻辑一致） 若工序节点标记了 IsBatchCalculation='Y'，则 qty =
	 * keyMatQty * qtyBatchSize
	 */
	public BigDecimal calculateNodeQty(Properties ctx, String trxName, int adRoutingNodeId, BigDecimal keyMatQty,
			BigDecimal qtyBatchSize) {
		if (adRoutingNodeId <= 0)
			return keyMatQty;
		String where = "AD_Routing_Node_ID=? AND IsBatchCalculation='Y' AND IsActive='Y'";
		PO routingNode = new Query(ctx, "AD_Routing_Node", where, trxName).setParameters(adRoutingNodeId).firstOnly();
		if (routingNode != null && qtyBatchSize != null && qtyBatchSize.compareTo(BigDecimal.ZERO) > 0) {
			return keyMatQty.multiply(qtyBatchSize);
		}
		return keyMatQty;
	}

	/**
	 * 从已有工单加载工序行到VO列表
	 */
	public void loadRoutingNodes(Properties ctx, int ppOrderId, List<RoutingNodeVO> target) {
		target.clear();
		// 查询工单工序（PP_Order_Node）
		String where = "PP_Order_ID=? AND IsActive='Y'";
		List<MPPOrderNode> nodes = new Query(ctx, MPPOrderNode.Table_Name, where, null).setParameters(ppOrderId)
				.setOrderBy("Value").list();
		int seq = 1;
		
		for (MPPOrderNode n : nodes) {
			RoutingNodeVO vo = new RoutingNodeVO();
			vo.lineNo = seq++;
			vo.ppOrderNodeId = n.getPP_Order_Node_ID();
			vo.adWFNodeId = n.getAD_WF_Node_ID();
			vo.routingNodeValue = n.getValue();
			vo.routingNodeName = n.getName();
			vo.description = n.getDescription();
			vo.qtyRequiered = n.getQtyRequiered();
			vo.qtyDelivered = n.getQtyDelivered();
			vo.qtyScrap = n.getQtyScrap();
			if (n.get_ValueAsString("EffectName") != null)
				vo.effectName = n.get_ValueAsString("EffectName");
			Integer rn = (Integer) n.get_Value("AD_Routing_Node_ID");
			if (rn != null)
				vo.adRoutingNodeId = rn;

			// ── 新增：加载自定义字段 ──────────────────────────────────────────────
			String pd = n.get_ValueAsString("ScrapType");
			vo.processdifficulty = pd != null ? pd : "";

			BigDecimal colorCount = (BigDecimal) n.get_Value("QtyColor");
			vo.colorCount = colorCount != null && colorCount.compareTo(BigDecimal.ZERO) > 0 ? colorCount
					: BigDecimal.ONE;

			vo.qtyPaperScrap = new BigDecimal(n.get_ValueAsInt("QtyPaperScrap"));
			vo.qtyPaperTotalScrap = new BigDecimal(n.get_ValueAsInt("QtyPaperTotalScrap"));

			BigDecimal ratePaper = (BigDecimal) n.get_Value("RatePaperTotalScrap");
			vo.ratePaperTotalScrap = ratePaper != null ? ratePaper : BigDecimal.ZERO;

			if (vo.adRoutingNodeId > 0) {
				int classId = DB.getSQLValue(null, "SELECT operationclass_ID FROM AD_Routing_Node "
						+ "WHERE AD_Routing_Node_ID=? AND IsActive='Y'", vo.adRoutingNodeId);
				vo.operationClassId = classId > 0 ? classId : 0;
				if (vo.operationClassId > 0) {
					String className = DB.getSQLValueString(null,
							"SELECT name FROM operationclass WHERE operationclass_id=? AND isactive='Y'",
							vo.operationClassId);
					vo.operationClassName = className != null ? className : "";
				}
			}
			// ─────────────────────────────────────────────────────────────────────
			target.add(vo);
		}

		// 设置当前报工工序为当前工序，高亮展示
		int currentNodeId = DB.getSQLValue(null,
				"SELECT PP_Order_Node_ID FROM PP_Cost_Collector " + "WHERE PP_Order_ID=? AND CostCollectorType='160' "
						+ "  AND IsActive='Y' ORDER BY Created DESC FETCH FIRST 1 ROWS ONLY", ppOrderId);
		if (currentNodeId > 0) {
			target.stream().filter(vo -> vo.ppOrderNodeId == currentNodeId).findFirst()
					.ifPresent(vo -> vo.isCurrentNode = true);
		}
	}
}