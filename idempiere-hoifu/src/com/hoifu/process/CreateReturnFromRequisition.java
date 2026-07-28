package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MDocType;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.Query;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Process to create a return-from-requisition inventory document from a
 * completed internal use inventory.
 *
 * @author Sapiens AI
 */
@org.adempiere.base.annotation.Process
public class CreateReturnFromRequisition extends org.compiere.process.SvrProcess {
	/** Return document type name */
	private static final String RETURN_DOC_TYPE_NAME = "退库单";

	/**
	 * Get Return DocType ID
	 *
	 * @return C_DocType_ID or 0 if not found
	 */
	private int getReturnDocTypeID() {
		// 修复：'MMI' 而不是 'M_MaterialPhysicalInventory'
		final String whereClause = "AD_Client_ID=? AND DocBaseType='MMI' AND IsActive='Y' AND Name LIKE ?";
		Query query = new Query(getCtx(), MDocType.Table_Name, whereClause, get_TrxName())
				.setParameters(Env.getAD_Client_ID(getCtx()), "%" + RETURN_DOC_TYPE_NAME + "%");
		MDocType docType = query.first();
		return docType != null ? docType.getC_DocType_ID() : 0;
	}

	@Override
	protected void prepare() {
		// No parameters — uses the record ID from the context
	}

	/**
	 * Process
	 *
	 * @return message
	 * @throws Exception
	 */
	@Override
	protected String doIt() throws Exception {
		int p_M_Inventory_ID = getRecord_ID();

		MInventory requisition = new MInventory(getCtx(), p_M_Inventory_ID, get_TrxName());

		// 验证是否已完成

		if (!MInventory.DOCSTATUS_Completed.equals(requisition.getDocStatus())) {
			throw new AdempiereUserError("@DocStatus@ = " + requisition.getDocStatus());
		}

		// 验证是否为领用单（DocSubTypeInv = IU）
		MDocType dt = MDocType.get(requisition.getC_DocType_ID());
		String docSubTypeInv = dt.getDocSubTypeInv();
		if (!MDocType.DOCSUBTYPEINV_InternalUseInventory.equals(docSubTypeInv)) {
			throw new AdempiereUserError(Msg.parseTranslation(getCtx(), "Only @C_DocType_ID@ @InternalUseInventory@"));
		}

		// 检查是否已全部退库
		boolean allReturned = true;
		BigDecimal totalReturnable = Env.ZERO;
		MInventoryLine[] reqLines = requisition.getLines(true);
		for (MInventoryLine reqLine : reqLines) {
			BigDecimal returnable = reqLine.getQtyInternalUse().subtract(reqLine.getQtyReturned());
			if (returnable.compareTo(Env.ZERO) > 0) {
				allReturned = false;
				totalReturnable = totalReturnable.add(returnable);
			}
		}

		if (allReturned) {
			// 构建友好提示信息，列出每行产品的领用/已退数量
			StringBuilder sb = new StringBuilder();
			sb.append("领用单 ").append(requisition.getDocumentNo()).append(" 已全部退库，无需再次操作。");
			sb.append("\n\n明细如下：\n");
			for (MInventoryLine reqLine : reqLines) {
				String productName = reqLine.getM_Product_ID() > 0
						? reqLine.getM_Product().getName()
						: "费用";
				sb.append("  · ").append(productName)
						.append("：领用 ").append(reqLine.getQtyInternalUse().stripTrailingZeros().toPlainString())
						.append(" / 已退 ").append(reqLine.getQtyReturned().stripTrailingZeros().toPlainString())
						.append("\n");
			}
			throw new AdempiereUserError(sb.toString());
		}

		// 获取退库单 DocType
		int returnDocTypeID = getReturnDocTypeID();
		if (returnDocTypeID == 0) {
			throw new AdempiereUserError("@C_DocType_ID@ " + RETURN_DOC_TYPE_NAME + " 未定义");
		}

		// 创建退库单
		MInventory returnDoc = new MInventory(getCtx(), 0, get_TrxName());
		returnDoc.setC_DocType_ID(returnDocTypeID);
		returnDoc.setM_Warehouse_ID(requisition.getM_Warehouse_ID());
		returnDoc.setMovementDate(new Timestamp(System.currentTimeMillis()));
		returnDoc.setDescription("退库单来源: " + requisition.getDocumentNo());
		returnDoc.setRef_Inventory_ID(requisition.get_ID());

		// 从领用单复制自定义字段到退库单
		// PP_Order_Work_Process
		Object ppOrderWorkProcess = requisition.get_Value("PP_Order_Work_Process");
		if (ppOrderWorkProcess != null)
			returnDoc.set_ValueOfColumn("PP_Order_Work_Process", ppOrderWorkProcess);

		// C_WorkTeam_ID
		Integer workTeamId = (Integer) requisition.get_Value("C_WorkTeam_ID");
		if (workTeamId != null && workTeamId > 0)
			returnDoc.set_ValueOfColumn("C_WorkTeam_ID", workTeamId);

		// C_Activity_ID
		int activityId = requisition.getC_Activity_ID();
		if (activityId > 0)
			returnDoc.setC_Activity_ID(activityId);

		// Handler_ID
		Integer handlerId = (Integer) requisition.get_Value("Handler_ID");
		if (handlerId != null && handlerId > 0)
			returnDoc.set_ValueOfColumn("Handler_ID", handlerId);

		returnDoc.saveEx(get_TrxName());

		// 创建退库明细
		int lineNo = 10;
		for (MInventoryLine reqLine : reqLines) {
			BigDecimal returnableQty = reqLine.getQtyInternalUse().subtract(reqLine.getQtyReturned());
			if (returnableQty.compareTo(Env.ZERO) > 0) {
				MInventoryLine returnLine = new MInventoryLine(getCtx(), 0, get_TrxName());
				returnLine.setM_Inventory_ID(returnDoc.get_ID());

				returnLine.setLine(lineNo);
				returnLine.setM_Product_ID(reqLine.getM_Product_ID());
				returnLine.setM_Locator_ID(reqLine.getM_Locator_ID());
				returnLine.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());
				returnLine.setC_Charge_ID(reqLine.getC_Charge_ID());
				returnLine.setQtyInternalUse(returnableQty);
				returnLine.set_ValueOfColumn("Ref_InventoryLine_ID", reqLine.get_ID());
				returnLine.saveEx(get_TrxName());
				lineNo += 10;
			}
		}

		addLog(returnDoc.get_ID(), null, null, "退库单: " + returnDoc.getDocumentNo(), returnDoc.get_Table_ID(),
				returnDoc.get_ID());

		return "退库单创建成功: " + returnDoc.getDocumentNo();
	}
}
