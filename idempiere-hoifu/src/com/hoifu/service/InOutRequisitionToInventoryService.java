package com.hoifu.service;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MInventory;  
import org.compiere.model.MInventoryLine;  
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;  
  
/**  
 * 出入库申请单（M_InOut_Requisition + M_InOut_RequisitionLine）→ M_Inventory  
 * （领用单 / 盘点单）的生成服务。  
 *  
 * <p>本 Service 仅处理 M_InOut_Requisition.InOutType = INV_IU（领用单）或  
 * INV_PI（盘点单）的申请行；其它 InOutType（RMA/ORDER 走  
 * {@code InOutRequisitionToInOutService}，PP_MATERIA/PP_IN 走对应的  
 * PP_Cost_Collector 生成服务）请勿调用本类。</p>  
 *  
 * <p>使用方式：调用方传入申请单 DocumentNo、申请单明细行号 Line、以及本次要生成的数量 Qty，  
 * 本服务负责：</p>  
 * <ol>  
 *   <li>按 DocumentNo 定位 M_InOut_Requisition 表头；</li>  
 *   <li>按表头 ID + Line 定位 M_InOut_RequisitionLine 明细行；</li>  
 *   <li>校验 InOutType 是否为 INV_IU / INV_PI；</li>  
 *   <li>校验剩余可生成数量（QtyRequested - QtyGenerated）是否足够；</li>  
 *   <li>创建草稿状态的 M_Inventory + 对应 M_InventoryLine，  
 *       按 InOutType 分支填充 QtyInternalUse（领用）或 QtyBook/QtyCount（盘点）；</li>  
 *   <li>累加申请单明细行的 QtyGenerated，回写防止超发。</li>  
 * </ol>  
 *  
 * <p>注意：本方法只负责"生成草稿单据"，不负责自动 Complete，是否自动完成由调用方（流程/按钮）决定。</p>  
 */  
public class InOutRequisitionToInventoryService {  
	private static final CLogger log = CLogger.getCLogger(InOutRequisitionToInventoryService.class);
	/**  
	 * 通过申请单号 + 明细行号 + 数量，生成（或追加到）一张 M_Inventory 草稿单据。  
	 *  
	 * <p>当前实现为"每次调用生成一张独立的 M_Inventory"（表头+一行明细）。如果你希望同一批调用  
	 * 合并到同一张 M_Inventory 里，请在调用方按 DocumentNo 分组后循环调用  
	 * {@link #appendLineToInventory(MInventory, MInOutRequisitionLine, String, BigDecimal, String)}。</p>  
	 *  
	 * @param documentNo M_InOut_Requisition.DocumentNo  
	 * @param line       M_InOut_RequisitionLine.Line（行号）  
	 * @param qty        本次要生成的数量，必须 > 0，且不能超过该行剩余可生成数量  
	 * @param ctx        上下文  
	 * @param trxName    事务名（建议由调用方的 SvrProcess 统一开启事务后传入）  
	 */  
	public static MInventory createFromRequisition(String documentNo, int line,  
			BigDecimal qty, Properties ctx, String trxName) {  
  
		if (documentNo == null || documentNo.trim().isEmpty())  
			throw new AdempiereException("申请单号不能为空");  
		if (line <= 0)  
			throw new AdempiereException("行号必须大于0");  
		if (qty == null || qty.signum() <= 0)  
			throw new AdempiereException("生成数量必须大于0");  
  
		// 1. 定位申请单表头  
		MInOutRequisition requisition = new Query(ctx, MInOutRequisition.Table_Name,  
				"DocumentNo=? AND IsActive='Y'", trxName)  
				.setParameters(documentNo)  
				.setOnlyActiveRecords(true)  
				.first();  
		if (requisition == null || requisition.get_ID() == 0)  
			throw new AdempiereException("找不到出入库申请单，单号：" + documentNo);  
  
		// 2. 校验 InOutType：本 Service 仅处理领用单 / 盘点单  
		String inOutType = requisition.getInOutType();  
//		boolean isInternalUse = INOUTTYPE_InternalUse.equals(inOutType);  
//		boolean isPhysicalInventory = INOUTTYPE_PhysicalInventory.equals(inOutType);  
//		if (!isInternalUse && !isPhysicalInventory)  
//			throw new AdempiereException("申请单 " + documentNo + " 的 InOutType(" + inOutType  
//					+ ") 不属于领用单/盘点单场景，请使用对应的生成服务");  
//  
		// 3. 定位申请单明细行  
		MInOutRequisitionLine reqLine = new Query(ctx, MInOutRequisitionLine.Table_Name,  
				"M_InOut_Requisition_ID=? AND Line=? AND IsActive='Y'", trxName)  
				.setParameters(requisition.get_ID(), line)  
				.setOnlyActiveRecords(true)  
				.first();  
		if (reqLine == null || reqLine.get_ID() == 0)  
			throw new AdempiereException("找不到申请单明细行，单号：" + documentNo + "，行号：" + line);  
  
		// 4. 校验目标单据类型：M_Inventory.C_DocType_ID 取自 M_InOut_Requisition.TargetDocType_ID  
		int targetDocTypeId = requisition.getTargetDocType_ID();  
		if (targetDocTypeId <= 0)  
			throw new AdempiereException("申请单 " + documentNo + " 未配置目标单据类型（TargetDocType_ID），无法生成领用单/盘点单");  
  
		// 5. 校验剩余可生成数量  
		BigDecimal qtyRequested = reqLine.getQtyRequested();  
		BigDecimal qtyGenerated = reqLine.getQtyGenerated();  
		BigDecimal qtyRemaining = qtyRequested.subtract(qtyGenerated);  
		if (qty.compareTo(qtyRemaining) > 0)  
			throw new AdempiereException("生成数量(" + qty + ")超过该行剩余可生成数量(" + qtyRemaining  
					+ ")，单号：" + documentNo + "，行号：" + line);  
  
		// 6. 创建 M_Inventory 表头  
		MInventory inventory = new MInventory(ctx, 0, trxName);  
		inventory.setAD_Org_ID(reqLine.getAD_Org_ID() > 0 ? reqLine.getAD_Org_ID() : requisition.getAD_Org_ID());  
		if (requisition.getAD_OrgTrx_ID() > 0)  
			inventory.setAD_OrgTrx_ID(requisition.getAD_OrgTrx_ID());  
		inventory.setC_DocType_ID(targetDocTypeId);  
		inventory.setM_Warehouse_ID(requisition.getM_Warehouse_ID());  
		inventory.setMovementDate(requisition.getMovementDate() != null  
				? requisition.getMovementDate()  
				: new Timestamp(System.currentTimeMillis()));  
		if (requisition.getC_Activity_ID() > 0)  
			inventory.setC_Activity_ID(requisition.getC_Activity_ID());  
		if (requisition.getApprovalAmt() != null)  
			inventory.setApprovalAmt(requisition.getApprovalAmt());  
		inventory.setDescription("来源申请单：" + documentNo);  
  
		inventory.setDocStatus(MInventory.DOCSTATUS_Drafted);  
		inventory.setDocAction(MInventory.DOCACTION_Complete);  
		inventory.saveEx(trxName);  
  
		// 7. 创建 M_InventoryLine 明细  
		appendLineToInventory(inventory, requisition,reqLine, inOutType, qty, trxName);  
		// 7. 尝试走标准完成工作流，失败不影响单据创建  
		try {  
		    if (!inventory.processIt(DocAction.ACTION_Complete)) {  
		        // 完成失败：记录错误信息，但不抛异常，让单据保持在草稿/失败状态  
		        log.warning("通用领退单 " + inventory.getDocumentNo()   + " 自动完成失败：" + inventory.getProcessMsg());  
		    }  
		    inventory.saveEx(trxName);  
		} catch (Exception e) {  
		    // 任何异常（包括 processIt 内部抛出的 AdempiereException）都吞掉  
		    log.log(Level.WARNING,   "通用领退单 " + inventory.getDocumentNo() + " 自动完成异常: " + e.getMessage(), e);  
		    // 注意：这里不要再调用 inOut.saveEx()，避免把异常时的中间状态强行落库；  
		}
		return inventory;
	}  
  
	/**  
	 * 在已存在的草稿 M_Inventory 上，按申请单明细行追加一条 M_InventoryLine。  
	 * 供"合并生成"场景复用（同一张申请单多行 → 同一张 M_Inventory）。  
	 *  
	 * @param inOutType 申请单头上的 InOutType（INV_IU / INV_PI），决定填充哪个数量字段  
	 */  
	public static MInventoryLine appendLineToInventory(MInventory inventory, MInOutRequisition requisition, MInOutRequisitionLine reqLine,  
			String inOutType, BigDecimal qty, String trxName) {  
  
		MInventoryLine invLine = new MInventoryLine(inventory.getCtx(), 0, trxName);  
		invLine.setM_Inventory_ID(inventory.get_ID());  
		invLine.setM_Product_ID(reqLine.getM_Product_ID());  
		invLine.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());  
  
		// 第一优先：自定义推荐库位函数
		int locatorId = DB.getSQLValue(trxName, "SELECT get_recommended_locator(?, ?, ?)",
				reqLine.getM_Product_ID(), inventory.getM_Warehouse_ID(), requisition.isOutStock());
		// 第二优先：回退到"成品库位"类型下的库位，IsDefault='Y' 优先，再按 PriorityNo 排
		if (locatorId <= 0 && requisition.isOutStock()) {
			locatorId = DB.getSQLValue(trxName,
					"SELECT l.M_Locator_ID " + "FROM M_Locator l "
							+ "JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID) "
							+ "WHERE l.M_Warehouse_ID = ? " + "  AND lt.Name = '原材料库位' " + "  AND l.IsActive = 'Y' "
							+ "ORDER BY CASE WHEN l.IsDefault='Y' THEN 0 ELSE 1 END, l.PriorityNo " + "LIMIT 1",
							inventory.getM_Warehouse_ID());
		}
		if (locatorId <= 0)
			throw new AdempiereException("物料 " + reqLine.getM_Product_ID() + " 找不到推荐库位");
		if (locatorId > 0)  
			invLine.setM_Locator_ID(locatorId);  
  
		if (reqLine.getC_Charge_ID() > 0)  
			invLine.setC_Charge_ID(reqLine.getC_Charge_ID());  
		if (reqLine.getC_Project_ID() > 0)  
			invLine.set_ValueOfColumn("C_Project_ID", reqLine.getC_Project_ID());  
  
//		if (INOUTTYPE_InternalUse.equals(inOutType)) {  
			// 领用单场景：填 QtyInternalUse，InventoryType 按是否挂 C_Charge_ID 区分  
			invLine.setQtyInternalUse(qty);  
			invLine.setInventoryType(reqLine.getC_Charge_ID() > 0  
					? MInventoryLine.INVENTORYTYPE_ChargeAccount  
					: MInventoryLine.INVENTORYTYPE_InventoryDifference);  
//		} else if (INOUTTYPE_PhysicalInventory.equals(inOutType)) {  
//			// 盘点单场景：QtyBook 取当前账面在库数量，QtyCount 为本次盘点数量（即传入 qty）  
//			BigDecimal qtyBook = getQtyOnHand(reqLine.getM_Product_ID(), locatorId,  
//					reqLine.getM_AttributeSetInstance_ID(), inventory.getMovementDate(), trxName);  
//			invLine.setQtyBook(qtyBook);  
//			invLine.setQtyCount(qty);  
//			invLine.setInventoryType(MInventoryLine.INVENTORYTYPE_InventoryDifference);  
//		} else {  
//			throw new AdempiereException("不支持的 InOutType: " + inOutType);  
//		}  
//  
		invLine.setDescription(reqLine.getDescription());  
		invLine.saveEx(trxName);  
  
		// 回写申请单明细行累计已生成数量，防止超发  
		reqLine.setQtyGenerated(reqLine.getQtyGenerated().add(qty));  
		reqLine.saveEx(trxName);  
  
		return invLine;  
	}  
  
	/**  
	 * 获取指定库位/产品/ASI 在指定日期的账面在库数量，用于盘点单 QtyBook 回填。  
	 */  
	private static BigDecimal getQtyOnHand(int productId, int locatorId, int asiId,  
			Timestamp movementDate, String trxName) {  
		if (locatorId <= 0)  
			return Env.ZERO;  
		BigDecimal qty = org.compiere.model.MStorageOnHand.getQtyOnHandForLocatorWithASIMovementDate(  
				productId, locatorId, asiId, movementDate, trxName);  
		return qty != null ? qty : Env.ZERO;  
	}  
}