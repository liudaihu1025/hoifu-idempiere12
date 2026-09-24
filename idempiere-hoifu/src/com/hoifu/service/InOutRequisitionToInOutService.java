package com.hoifu.service;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.model.Query;  
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;  
  
import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;
  
/**  
 * 出入库申请单（M_InOut_Requisition + M_InOut_RequisitionLine）→ M_InOut（收发货单或退货单）  
 * 的生成服务。  
 *  
 * <p>使用方式：调用方传入申请单 DocumentNo、申请单明细行号 Line、以及本次要生成的数量 Qty，  
 * 本服务负责：</p>  
 * <ol>  
 *   <li>按 DocumentNo 定位 M_InOut_Requisition 表头；</li>  
 *   <li>按表头 ID + Line 定位 M_InOut_RequisitionLine 明细行；</li>  
 *   <li>校验剩余可生成数量（QtyRequested - QtyGenerated）是否足够；</li>  
 *   <li>根据 M_InOut_Requisition.TargetDocType_ID 对应的 C_DocType.DocSubTypeSO  
 *       是否为 'RM'（退货），判断生成收发货单还是退货单，并按对应场景填充  
 *       C_Order_ID/C_OrderLine_ID 或 M_RMA_ID/M_RMALine_ID、MovementType 等字段；</li>  
 *   <li>创建草稿状态的 M_InOut + 对应 M_InOutLine；</li>  
 *   <li>累加申请单明细行的 QtyGenerated，回写防止超发。</li>  
 * </ol>  
 *  
 * <p>注意：本方法只负责"生成草稿单据"，不负责自动 Complete，是否自动完成由调用方（流程/按钮）决定。</p>  
 */  
public class InOutRequisitionToInOutService {  
  
	private static final CLogger log = CLogger.getCLogger(InOutRequisitionToInOutService.class);  
	/**  
	 * 通过申请单号 + 明细行号 + 数量，生成（或追加到）一张 M_InOut 草稿单据。  
	 *  
	 * <p>当前实现为"每次调用生成一张独立的 M_InOut"（表头+一行明细）。如果你希望同一批调用  
	 * 合并到同一张 M_InOut 里，请在调用方按 DocumentNo 分组后循环调用  
	 * {@link #appendLineToInOut(MInOut, MInOutRequisitionLine, boolean, BigDecimal, String)}。</p>  
	 *  
	 * @param documentNo M_InOut_Requisition.DocumentNo  
	 * @param line       M_InOut_RequisitionLine.Line（行号）  
	 * @param qty        本次要生成的数量，必须 > 0，且不能超过该行剩余可生成数量  
	 * @param ctx        上下文  
	 * @param trxName    事务名（建议由调用方的 SvrProcess 统一开启事务后传入）  
	 */  
	public static MInOut createFromRequisition(String documentNo, int line,  
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
  
		// 2. 定位申请单明细行  
		MInOutRequisitionLine reqLine = new Query(ctx, MInOutRequisitionLine.Table_Name,  
				"M_InOut_Requisition_ID=? AND Line=? AND IsActive='Y'", trxName)  
				.setParameters(requisition.get_ID(), line)  
				.setOnlyActiveRecords(true)  
				.first();  
		if (reqLine == null || reqLine.get_ID() == 0)  
			throw new AdempiereException("找不到申请单明细行，单号：" + documentNo + "，行号：" + line);  
  
		// 3. 校验目标单据类型：M_InOut.C_DocType_ID 取自 M_InOut_Requisition.TargetDocType_ID  
		int targetDocTypeId = requisition.getTargetDocType_ID();  
		if (targetDocTypeId <= 0)  
			throw new AdempiereException("申请单 " + documentNo + " 未配置目标单据类型（TargetDocType_ID），无法生成收发货单");  
  
		// 3.1 判断是否为"退货类"单据（DocSubTypeSO='RM'），决定后续字段映射走哪条分支  
		boolean isReturn = MInOutRequisition.IN_RMA_IN.equals(requisition.getInOutType()) || MInOutRequisition.OUT_RMA_OUT.equals(requisition.getInOutType());  
  
		// 4. 校验剩余可生成数量  
		BigDecimal qtyRequested = reqLine.getQtyRequested();  
		BigDecimal qtyGenerated = reqLine.getQtyGenerated();  
		BigDecimal qtyRemaining = qtyRequested.subtract(qtyGenerated);  
		if (qty.compareTo(qtyRemaining) > 0)  
			throw new AdempiereException("生成数量(" + qty + ")超过该行剩余可生成数量(" + qtyRemaining  
					+ ")，单号：" + documentNo + "，行号：" + line);  
  
		// 5. 创建 M_InOut 表头  
		MInOut inOut = new MInOut(ctx, 0, trxName);  
		inOut.setAD_Org_ID(reqLine.getAD_Org_ID() > 0 ? reqLine.getAD_Org_ID() : requisition.getAD_Org_ID());  
		inOut.setC_DocType_ID(targetDocTypeId);  
		inOut.setM_Warehouse_ID(requisition.getM_Warehouse_ID());  
		inOut.setMovementDate(new Timestamp(System.currentTimeMillis()));  
		inOut.setDateReceived(new Timestamp(System.currentTimeMillis()));  
		inOut.setDateAcct(new Timestamp(System.currentTimeMillis()));  
		inOut.setIsSOTrx(requisition.isSOTrx());  
		inOut.setMovementType();
		if (requisition.getC_BPartner_ID() > 0)  
			inOut.setC_BPartner_ID(requisition.getC_BPartner_ID());  
		if (requisition.getC_BPartner_Location_ID() > 0)  
			inOut.setC_BPartner_Location_ID(requisition.getC_BPartner_Location_ID());  
  
		if (isReturn) {  
			// 退货场景：挂 M_RMA_ID，MovementType 走客户/供应商退货  
			if (requisition.getM_RMA_ID() > 0)  
				inOut.setM_RMA_ID(requisition.getM_RMA_ID());  
		} else {  
			// 收发货场景：挂 C_Order_ID，MovementType 沿用申请单自带取值（发货/收货）  
			if (requisition.getC_Order_ID() > 0)  
				inOut.setC_Order_ID(requisition.getC_Order_ID());  
		}  
  
		inOut.setDocStatus(DocAction.STATUS_Drafted);  
		inOut.setDocAction(DocAction.ACTION_Complete);  
		inOut.set_ValueOfColumn("Ref_InOut_Requisition_No", documentNo); // 冗余记录来源申请单号，便于追溯  
		inOut.saveEx(trxName);  
  
		// 6. 创建 M_InOutLine 明细  
		appendLineToInOut(requisition, inOut, reqLine, isReturn, qty, trxName);  
		
		// 7. 尝试走标准完成工作流，失败不影响单据创建  
		try {  
		    if (!inOut.processIt(DocAction.ACTION_Complete)) {  
		        // 完成失败：记录错误信息，但不抛异常，让单据保持在草稿/失败状态  
		        log.warning("收发货单 " + inOut.getDocumentNo()   + " 自动完成失败：" + inOut.getProcessMsg());  
		    }  
		    inOut.saveEx(trxName);  
		} catch (Exception e) {  
		    // 任何异常（包括 processIt 内部抛出的 AdempiereException）都吞掉  
		    log.log(Level.WARNING,   "收发货单 " + inOut.getDocumentNo() + " 自动完成异常: " + e.getMessage(), e);  
		    // 注意：这里不要再调用 inOut.saveEx()，避免把异常时的中间状态强行落库；  
		}
		return inOut;
	}  
  
	/**  
	 * 在已存在的草稿 M_InOut 上，按申请单明细行追加一条 M_InOutLine。  
	 * 供"合并生成"场景复用（同一张申请单多行 → 同一张 M_InOut）。  
	 *  
	 * @param isReturn 是否退货场景，决定行上挂 C_OrderLine_ID 还是 M_RMALine_ID  
	 */  
	public static MInOutLine appendLineToInOut(MInOutRequisition requisition, MInOut inOut, MInOutRequisitionLine reqLine,  
			boolean isReturn, BigDecimal qty, String trxName) {  
  
		MInOutLine ioLine = new MInOutLine(inOut);  
		ioLine.setM_Product_ID(reqLine.getM_Product_ID());  
		ioLine.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());  
		ioLine.setC_UOM_ID(reqLine.getC_UOM_ID());  
		ioLine.setQtyEntered(qty);  
		ioLine.setMovementQty(qty);  
  
		// 第一优先：自定义推荐库位函数
		int locatorId = DB.getSQLValue(trxName, "SELECT get_recommended_locator(?, ?, ?)",
				reqLine.getM_Product_ID(), inOut.getM_Warehouse_ID(), requisition.isOutStock());
		// 第二优先：回退到"成品库位"类型下的库位，IsDefault='Y' 优先，再按 PriorityNo 排
		if (locatorId <= 0 && requisition.isOutStock()) {
			locatorId = DB.getSQLValue(trxName,
					"SELECT l.M_Locator_ID " + "FROM M_Locator l "
							+ "JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID) "
							+ "WHERE l.M_Warehouse_ID = ? " + "  AND lt.Name = '成品库位' " + "  AND l.IsActive = 'Y' "
							+ "ORDER BY CASE WHEN l.IsDefault='Y' THEN 0 ELSE 1 END, l.PriorityNo " + "LIMIT 1",
							inOut.getM_Warehouse_ID());
		}
		if (locatorId <= 0)
			throw new AdempiereException("物料 " + reqLine.getM_Product_ID() + " 找不到推荐库位");
		if (locatorId > 0)  
			ioLine.setM_Locator_ID(locatorId);  
  
		if (reqLine.getC_Charge_ID() > 0)  
			ioLine.setC_Charge_ID(reqLine.getC_Charge_ID());  
		if (reqLine.getC_Activity_ID() > 0)  
			ioLine.setC_Activity_ID(reqLine.getC_Activity_ID());  
		if (reqLine.getC_Campaign_ID() > 0)  
			ioLine.setC_Campaign_ID(reqLine.getC_Campaign_ID());  
		if (reqLine.getC_Project_ID() > 0)  
			ioLine.set_ValueOfColumn("C_Project_ID", reqLine.getC_Project_ID());  
  
		// 来源行回链：退货场景挂 M_RMALine_ID，收发货场景挂 C_OrderLine_ID  
		if (isReturn) {  
			if (reqLine.getM_RMALine_ID() > 0)  
				ioLine.setM_RMALine_ID(reqLine.getM_RMALine_ID());  
		} else {  
			if (reqLine.getC_OrderLine_ID() > 0)  
				ioLine.setC_OrderLine_ID(reqLine.getC_OrderLine_ID());  
		}  
  
		ioLine.setDescription(reqLine.getDescription());  
		ioLine.saveEx(trxName);  
  
		// 回写申请单明细行累计已生成数量，防止超发  
		reqLine.setQtyGenerated(reqLine.getQtyGenerated().add(qty));  
		reqLine.saveEx(trxName);  
  
		return ioLine;  
	}  
}