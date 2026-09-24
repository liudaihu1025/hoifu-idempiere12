package org.libero.process;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MDocType;  
import org.compiere.model.Query;  
import org.compiere.process.DocAction;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.AdempiereUserError;  
import org.compiere.util.DB;  
import org.libero.model.MPPCostCollector;  
import org.libero.model.MPPMaterialRequisition;
import org.libero.model.MPPOrderNode;

import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;  
  
/**  
 * 出入库申请单（M_InOut_Requisition + M_InOut_RequisitionLine，定义在 idempiere-hoifu 中）→  
 * PP_Material_Requisition + PP_Cost_Collector（领退料单）  
 * 或 PP_Cost_Collector（生产入库单，直接生成，不经过 PP_Material_Requisition）  
 * 的生成流程。  
 *  
 * <p>本流程从 com.hoifu.service.InOutRequisitionToPPMaterialOrderService 迁移而来，  
 * 运行在 idempiere-mfg bundle 内，因此可以直接使用 mfg 模块的  
 * {@link MPPMaterialRequisition} / {@link MPPCostCollector}，拥有完整的单据生命周期  
 * （联动完成/作废/重新激活明细行等），而不再受限于 hoifu 侧的 GenericPO。</p>  
 *  
 * <p>调用方（hoifu 侧）通过 ProcessInfo + ServerProcessCtl.process(...) 按 AD_Process_ID  
 * 触发，参数（DocumentNo/Line/Qty）走标准的 AD_Process_Para/ProcessInfoParameter 机制传入。</p>  
 */  
public class InOutRequisitionToPPMaterialOrder extends SvrProcess {  
  
	private static final String INOUTTYPE_PPMaterial = "PP_MATERIA_OUT"; // 生产领料出库  
	private static final String INOUTTYPE_PPIn = "PP_IN";                // 生产入库  
  
	/** CostCollectorType 字面量，与 X_PP_Cost_Collector 中的常量保持一致 */  
	private static final String CCTYPE_ComponentIssue = MPPCostCollector.COSTCOLLECTORTYPE_ComponentIssue;   // 110 领退料  
	private static final String CCTYPE_MaterialReceipt = MPPCostCollector.COSTCOLLECTORTYPE_MaterialReceipt; // 100 生产入库  
  
	// ---- 流程参数 ----  
	private String p_DocumentNo;  
	private int p_Line;  
	private BigDecimal p_Qty;  
  
	/** doIt() 处理完成后回写的结果 ID，供调用方通过 ProcessInfo/getResultID() 读取 */  
	private int p_Result_ID = 0;  
  
	@Override  
	protected void prepare() {  
		for (ProcessInfoParameter para : getParameter()) {  
			String name = para.getParameterName();  
			if (para.getParameter() == null)  
				continue;  
			if ("DocumentNo".equals(name))  
				p_DocumentNo = para.getParameterAsString();  
			else if ("Line".equals(name))  
				p_Line = para.getParameterAsInt();  
			else if ("Qty".equals(name))  
				p_Qty = (BigDecimal) para.getParameter();  
			else  
				org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);  
		}  
  
		if (p_DocumentNo == null || p_DocumentNo.trim().isEmpty())  
			throw new AdempiereUserError("申请单号不能为空");  
		if (p_Line <= 0)  
			throw new AdempiereUserError("行号必须大于0");  
		if (p_Qty == null || p_Qty.signum() <= 0)  
			throw new AdempiereUserError("生成数量必须大于0");  
	}  
  
	@Override  
	protected String doIt() throws Exception {  
  
		// 1. 定位申请单表头  
		MInOutRequisition requisition = new Query(getCtx(), MInOutRequisition.Table_Name,  
				"DocumentNo=? AND IsActive='Y'", get_TrxName())  
				.setParameters(p_DocumentNo).setOnlyActiveRecords(true).first();  
		if (requisition == null || requisition.get_ID() == 0)  
			throw new AdempiereException("找不到出入库申请单，单号：" + p_DocumentNo);  
  
		// 2. 校验 InOutType  
		String inOutType = requisition.getInOutType();  
		boolean isPPMaterial = INOUTTYPE_PPMaterial.equals(inOutType);  
		boolean isPPIn = INOUTTYPE_PPIn.equals(inOutType);  
		if (!isPPMaterial && !isPPIn)  
			throw new AdempiereException("申请单 " + p_DocumentNo + " 的 InOutType(" + inOutType  
					+ ") 不属于领退料单/生产入库单场景，请使用对应的生成服务");  
  
		// 3. 定位申请单明细行  
		MInOutRequisitionLine reqLine = new Query(getCtx(), MInOutRequisitionLine.Table_Name,  
				"M_InOut_Requisition_ID=? AND Line=? AND IsActive='Y'", get_TrxName())  
				.setParameters(requisition.get_ID(), p_Line).setOnlyActiveRecords(true).first();  
		if (reqLine == null || reqLine.get_ID() == 0)  
			throw new AdempiereException("找不到申请单明细行，单号：" + p_DocumentNo + "，行号：" + p_Line);  
  
		// 4. 校验生产工单必填  
		int ppOrderId = reqLine.getPP_Order_ID();  
		if (ppOrderId <= 0)  
			throw new AdempiereException("申请单 " + p_DocumentNo + " 行 " + p_Line + " 未关联生产工单（PP_Order_ID）");  
  
		// 5. 校验剩余可生成数量  
		BigDecimal qtyRequested = reqLine.getQtyRequested() != null ? reqLine.getQtyRequested() : BigDecimal.ZERO;  
		BigDecimal qtyGenerated = reqLine.getQtyGenerated() != null ? reqLine.getQtyGenerated() : BigDecimal.ZERO;  
		BigDecimal qtyRemaining = qtyRequested.subtract(qtyGenerated);  
		if (p_Qty.compareTo(qtyRemaining) > 0)  
			throw new AdempiereException("生成数量(" + p_Qty + ")超过该行剩余可生成数量(" + qtyRemaining + ")");  
  
		MPPCostCollector cc;  
		if (isPPIn) {  
			// ---- 生产入库：直接生成 PP_Cost_Collector，不创建 PP_Material_Requisition ----  
			cc = createCostCollectorDirect(requisition, reqLine, ppOrderId, p_Qty);  
			tryAutoComplete(cc);  
		} else {  
			// ---- 领退料：先建 PP_Material_Requisition 头，再挂一条 PP_Cost_Collector 行 ----  
			int targetDocTypeId = requisition.getTargetDocType_ID();  
			if (targetDocTypeId <= 0)  
				throw new AdempiereException("申请单 " + p_DocumentNo + " 未配置目标单据类型（TargetDocType_ID）");  
  
			MPPMaterialRequisition requisitionOrder = new MPPMaterialRequisition(getCtx(), 0, get_TrxName());  
			requisitionOrder.setAD_Org_ID(reqLine.getAD_Org_ID() > 0 ? reqLine.getAD_Org_ID() : requisition.getAD_Org_ID());  
			requisitionOrder.setC_DocType_ID(targetDocTypeId);  
			requisitionOrder.setM_Warehouse_ID(requisition.getM_Warehouse_ID());  
			requisitionOrder.setM_Product_ID(reqLine.getM_Product_ID());  
			requisitionOrder.setPP_Order_ID(ppOrderId);  
			if (reqLine.getPP_Order_Node_ID() > 0)  
				requisitionOrder.setPP_Order_Node_ID(reqLine.getPP_Order_Node_ID());  
			Timestamp movementDate = requisition.getMovementDate() != null  
					? requisition.getMovementDate() : new Timestamp(System.currentTimeMillis());  
			requisitionOrder.setMovementDate(movementDate);  
			if (requisition.getAD_User_ID() > 0)  
				requisitionOrder.setAD_User_ID(requisition.getAD_User_ID());  
			requisitionOrder.setDocStatus(DocAction.STATUS_Drafted);  
			requisitionOrder.setDocAction(DocAction.ACTION_Complete);  
			requisitionOrder.setProcessed(false);  
			requisitionOrder.saveEx(get_TrxName());  
  
			cc = appendCostCollectorLine(requisitionOrder, requisition, reqLine, ppOrderId, CCTYPE_ComponentIssue, p_Qty);  
			 // 完成的是单据头 requisitionOrder，联动完成挂在下面的 cc 行  
		    tryAutoComplete(requisitionOrder);  
		}  
  
		// 6. 回写申请单明细行累计已生成数量，防止超发  
		reqLine.setQtyGenerated(qtyGenerated.add(p_Qty));  
		reqLine.saveEx(get_TrxName());  
  
		p_Result_ID = cc.get_ID();  
		addLog(0, null, null, "已生成 PP_Cost_Collector " + cc.getDocumentNo(),  
				MPPCostCollector.Table_ID, cc.get_ID());  
  
		return "@Created@ PP_Cost_Collector_ID=" + p_Result_ID;  
	}  
  
	public int getResultID() {  
		return p_Result_ID;  
	}  
  
	/**  
	 * 尝试将单据走标准工作流自动完成，失败（返回 false 或抛异常）都不向外抛，  
	 * 只记录日志，避免影响单据本身已经创建成功这一结果。  
	 */  
	private void tryAutoComplete(DocAction doc) {  
	    try {  
	        boolean ok = doc.processIt(DocAction.ACTION_Complete);  
	        doc.saveEx();  
	        if (!ok) {  
	            log.warning("单据 " + doc.getDocumentNo() + " 自动完成失败：" + doc.getProcessMsg());  
	        }  
	    } catch (Exception e) {  
	        log.warning("单据自动完成异常: " + e.getMessage());  
	    }  
	}
	
	/**  
	 * 生产入库场景：直接生成一条 PP_Cost_Collector（CostCollectorType=100/MaterialReceipt），  
	 * 不创建、不关联 PP_Material_Requisition，与 mfg 模块现有的完工入库/报工创建方式保持一致  
	 * （参见 MPPProcessCardRecord.createAndCompleteCostCollector()）。  
	 */  
	private MPPCostCollector createCostCollectorDirect(MInOutRequisition requisition,  
			MInOutRequisitionLine reqLine, int ppOrderId, BigDecimal qty) {  
  
		MPPCostCollector cc = new MPPCostCollector(getCtx(), 0, get_TrxName());  
  
		cc.setAD_Org_ID(reqLine.getAD_Org_ID() > 0 ? reqLine.getAD_Org_ID() : requisition.getAD_Org_ID());  
		cc.setPP_Order_ID(ppOrderId);  
		if (reqLine.getPP_Order_Node_ID() > 0)  
			cc.setPP_Order_Node_ID(reqLine.getPP_Order_Node_ID());  
  
		cc.setM_Product_ID(reqLine.getM_Product_ID());  
		cc.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());  
		cc.setC_UOM_ID(reqLine.getC_UOM_ID());  
		int warehouseId = requisition.getM_Warehouse_ID();  
		cc.setM_Warehouse_ID(warehouseId);  
  
		// 第一优先：自定义推荐库位函数
		int locatorId = DB.getSQLValue(get_TrxName(), "SELECT get_recommended_locator(?, ?, ?)",
				reqLine.getM_Product_ID(), cc.getM_Warehouse_ID(), requisition.isOutStock());
		// 第二优先：回退到"成品库位"类型下的库位，IsDefault='Y' 优先，再按 PriorityNo 排
		if (locatorId <= 0 && requisition.isOutStock()) {
			locatorId = DB.getSQLValue(get_TrxName(),
					"SELECT l.M_Locator_ID " + "FROM M_Locator l "
							+ "JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID) "
							+ "WHERE l.M_Warehouse_ID = ? " + "  AND lt.Name = '成品库位' " + "  AND l.IsActive = 'Y' "
							+ "ORDER BY CASE WHEN l.IsDefault='Y' THEN 0 ELSE 1 END, l.PriorityNo " + "LIMIT 1",
							cc.getM_Warehouse_ID());
		}
		if (locatorId <= 0)
			throw new AdempiereException("物料 " + reqLine.getM_Product_ID() + " 找不到推荐库位");
		if (locatorId > 0)  
			cc.setM_Locator_ID(locatorId);  
  
		if (reqLine.getC_Activity_ID() > 0)  
			cc.setC_Activity_ID(reqLine.getC_Activity_ID());  
		if (reqLine.getC_Campaign_ID() > 0)  
			cc.setC_Campaign_ID(reqLine.getC_Campaign_ID());  
		if (reqLine.getC_Project_ID() > 0)  
			cc.setC_Project_ID(reqLine.getC_Project_ID());  
  
		Timestamp movementDate = requisition.getMovementDate() != null  
				? requisition.getMovementDate() : new Timestamp(System.currentTimeMillis());  
		cc.setMovementDate(movementDate);  
		cc.setDateAcct(movementDate);  
		cc.setMovementQty(qty);  
		cc.setAD_User_ID(reqLine.getAD_User_ID()); 
		cc.setS_Resource_ID(reqLine.getS_Resource_ID()); 
		if (reqLine.getScrappedQty() != null)  
			cc.setScrappedQty(reqLine.getScrappedQty());  
		if (reqLine.getQtyReject() != null)  
			cc.setQtyReject(reqLine.getQtyReject());  
  
		// CostCollectorType：优先取申请单明细行自带的值，未填则固定为 100（MaterialReceipt）  
		String costCollectorType = reqLine.getCostCollectorType();  
		if (costCollectorType == null || costCollectorType.trim().isEmpty())  
			costCollectorType = CCTYPE_MaterialReceipt;  
		cc.setCostCollectorType(costCollectorType);  
  
		int docTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_ManufacturingCostCollector);  
		cc.setC_DocType_ID(docTypeId);  
		cc.setC_DocTypeTarget_ID(docTypeId);  
  
		cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);  
		cc.setDocAction(MPPCostCollector.DOCACTION_Complete);  
		cc.setProcessed(false);  
		cc.setProcessing(false);  
		cc.setPosted(false);  
  
		cc.setDescription(reqLine.getDescription());  
		// 注意：不设置 PP_Material_Requisition_ID —— 生产入库不挂申请单头  
		cc.saveEx(get_TrxName());  
  
		return cc;  
	}  
  
	/**  
	 * 领退料场景：为一张已存在的草稿 PP_Material_Requisition 追加一条 PP_Cost_Collector 明细行  
	 * （挂 PP_Material_Requisition_ID）。  
	 */  
	private MPPCostCollector appendCostCollectorLine(MPPMaterialRequisition requisitionOrder,  
			MInOutRequisition requisition, MInOutRequisitionLine reqLine, int ppOrderId, String costCollectorType, BigDecimal qty) {  
  
		MPPCostCollector cc = new MPPCostCollector(getCtx(), 0, get_TrxName());  
  
		cc.setAD_Org_ID(requisitionOrder.getAD_Org_ID());  
		cc.setPP_Material_Requisition_ID(requisitionOrder.get_ID());  
		cc.setPP_Order_ID(ppOrderId);  
		if (reqLine.getPP_Order_Node_ID() > 0) {  
		    cc.setPP_Order_Node_ID(reqLine.getPP_Order_Node_ID());  
		    MPPOrderNode node = new MPPOrderNode(getCtx(), reqLine.getPP_Order_Node_ID(), get_TrxName());  
		    if (node.getPP_Order_Workflow_ID() > 0)  
		        cc.setPP_Order_Workflow_ID(node.getPP_Order_Workflow_ID());  
		}
		if (reqLine.getPP_Order_Node_ID() > 0)  
			cc.setPP_Order_Node_ID(reqLine.getPP_Order_Node_ID());  
		if (reqLine.getPP_Order_BOMLine_ID() > 0)  
			cc.setPP_Order_BOMLine_ID(reqLine.getPP_Order_BOMLine_ID());  
		if (reqLine.getS_Resource_ID() > 0)  
			cc.setS_Resource_ID(reqLine.getS_Resource_ID());  
  
		cc.setM_Product_ID(reqLine.getM_Product_ID());  
		cc.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());  
		cc.setC_UOM_ID(reqLine.getC_UOM_ID());  
		int warehouseId = requisitionOrder.getM_Warehouse_ID();  
		cc.setM_Warehouse_ID(warehouseId);  
  
		// 第一优先：自定义推荐库位函数
		int locatorId = DB.getSQLValue(get_TrxName(), "SELECT get_recommended_locator(?, ?, ?)",
				reqLine.getM_Product_ID(), cc.getM_Warehouse_ID(), requisition.isOutStock());
		// 第二优先：回退到"成品库位"类型下的库位，IsDefault='Y' 优先，再按 PriorityNo 排
		if (locatorId <= 0 && requisition.isOutStock()) {
			locatorId = DB.getSQLValue(get_TrxName(),
					"SELECT l.M_Locator_ID " + "FROM M_Locator l "
							+ "JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID) "
							+ "WHERE l.M_Warehouse_ID = ? " + "  AND lt.Name = '原材料库位' " + "  AND l.IsActive = 'Y' "
							+ "ORDER BY CASE WHEN l.IsDefault='Y' THEN 0 ELSE 1 END, l.PriorityNo " + "LIMIT 1",
							cc.getM_Warehouse_ID());
		}
		if (locatorId <= 0)
			throw new AdempiereException("物料 " + reqLine.getM_Product_ID() + " 找不到推荐库位");
		if (locatorId > 0)  
			cc.setM_Locator_ID(locatorId);  
  
		if (reqLine.getC_Activity_ID() > 0)  
			cc.setC_Activity_ID(reqLine.getC_Activity_ID());  
		if (reqLine.getC_Campaign_ID() > 0)  
			cc.setC_Campaign_ID(reqLine.getC_Campaign_ID());  
		if (reqLine.getC_Project_ID() > 0)  
			cc.setC_Project_ID(reqLine.getC_Project_ID());  
  
		Timestamp movementDate = requisitionOrder.getMovementDate();  
		cc.setMovementDate(movementDate);  
		cc.setDateAcct(movementDate);  
		cc.setMovementQty(qty);  
		cc.setAD_User_ID(reqLine.getAD_User_ID()); 
		cc.setS_Resource_ID(reqLine.getS_Resource_ID()); 
		if (reqLine.getScrappedQty() != null)  
			cc.setScrappedQty(reqLine.getScrappedQty());  
		if (reqLine.getQtyReject() != null)  
			cc.setQtyReject(reqLine.getQtyReject());  
  
		String type = reqLine.getCostCollectorType();  
		if (type == null || type.trim().isEmpty())  
			type = costCollectorType;  
		cc.setCostCollectorType(type);  
  
		int docTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_ManufacturingCostCollector);  
		cc.setC_DocType_ID(docTypeId);  
		cc.setC_DocTypeTarget_ID(docTypeId);  
  
		cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);  
		cc.setDocAction(MPPCostCollector.DOCACTION_Complete);  
		cc.setProcessed(false);  
		cc.setProcessing(false);  
		cc.setPosted(false);  
  
		cc.setDescription(reqLine.getDescription());  
		cc.saveEx(get_TrxName());  
  
		return cc;  
	}  
}