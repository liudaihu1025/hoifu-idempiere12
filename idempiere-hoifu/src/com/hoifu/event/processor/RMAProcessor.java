package com.hoifu.event.processor;

import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.DB;

public class RMAProcessor implements IEventProcessor {

	/** 供应商退货收发单 C_DocType UU */
	private static final String VENDOR_RETURN_INOUT_UU = "aaefbaad-f188-4809-aa9c-ec7b9b2e00b8";
	/** 客户退货收发单 C_DocType UU */
	private static final String CUSTOMER_RETURN_INOUT_UU = "64be6ab3-dfb8-4dc6-8224-1cd22b4d1522";

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MRMA;
	}

	@Override
	public void process(PO po, String topic) {
		MRMA rma = (MRMA) po;

		// 完成时创建退货单
		createReturnInOut(rma, topic);
	}

	private void createReturnInOut(MRMA rma, String topic) {

		// 仅在 DocStatus 变更为"已完成"时触发
		if (!rma.is_ValueChanged(MRMA.COLUMNNAME_DocStatus))
			return;
		if (!DocAction.STATUS_Completed.equals(rma.getDocStatus()))
			return;

		// 防止重复创建（幂等保护）
		int existingReturnOrderId = DB.getSQLValueEx(rma.get_TrxName(),
				"SELECT ReturnOrder_ID FROM M_RMA WHERE M_RMA_ID=?", rma.getM_RMA_ID());
		if (existingReturnOrderId > 0)
			return;

		String trxName = rma.get_TrxName();
		boolean isSO = rma.isSOTrx();

		String docTypeUU = isSO ? CUSTOMER_RETURN_INOUT_UU : VENDOR_RETURN_INOUT_UU;
		int docTypeId = DB.getSQLValueEx(trxName, "SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU=?", docTypeUU);
		if (docTypeId <= 0)
			throw new AdempiereException("未找到退货收发单单据类型，UU=" + docTypeUU);

		MInOut originalInOut = rma.getShipment();
		if (originalInOut == null)
			throw new AdempiereException("RMA[" + rma.getDocumentNo() + "] 未关联原始收发单");

		Timestamp now = new Timestamp(System.currentTimeMillis());

		// 创建退货收发单头
		MInOut returnInOut = new MInOut(rma.getCtx(), 0, trxName);
		returnInOut.setM_RMA_ID(rma.getM_RMA_ID());
		returnInOut.setAD_Org_ID(rma.getAD_Org_ID());
		returnInOut.setAD_OrgTrx_ID(originalInOut.getAD_OrgTrx_ID());
		returnInOut.setDescription(rma.getDescription());
		returnInOut.setC_BPartner_ID(rma.getC_BPartner_ID());
		returnInOut.setC_BPartner_Location_ID(originalInOut.getC_BPartner_Location_ID());
		returnInOut.setIsSOTrx(isSO);
		returnInOut.setC_DocType_ID(docTypeId);
		returnInOut.setM_Warehouse_ID(originalInOut.getM_Warehouse_ID());
		returnInOut.setMovementType(isSO ? MInOut.MOVEMENTTYPE_CustomerReturns : MInOut.MOVEMENTTYPE_VendorReturns);
		returnInOut.setMovementDate(now);
		returnInOut.setDateAcct(now);
		returnInOut.setDocStatus(DocAction.STATUS_Drafted);
		returnInOut.setDocAction(DocAction.ACTION_Complete);
		returnInOut.setC_Project_ID(originalInOut.getC_Project_ID());
		returnInOut.setC_Campaign_ID(originalInOut.getC_Campaign_ID());
		returnInOut.setC_Activity_ID(originalInOut.getC_Activity_ID());
		returnInOut.saveEx();

		// 回写 M_RMA.ReturnOrder_ID
		DB.executeUpdateEx("UPDATE M_RMA SET ReturnOrder_ID=? WHERE M_RMA_ID=?",
				new Object[] { returnInOut.getM_InOut_ID(), rma.getM_RMA_ID() }, trxName);

		// 创建退货收发单行
		MRMALine[] rmaLines = rma.getLines(true);
		for (MRMALine rmaLine : rmaLines) {
			if (rmaLine.getM_InOutLine_ID() == 0 && rmaLine.getC_Charge_ID() == 0 && rmaLine.getM_Product_ID() == 0)
				continue;

			MInOutLine returnLine = new MInOutLine(returnInOut);
			returnLine.setM_RMALine_ID(rmaLine.get_ID());
			returnLine.setLine(rmaLine.getLine());
			returnLine.setDescription(rmaLine.getDescription());

			if (rmaLine.getC_Charge_ID() != 0) {
				returnLine.setC_Charge_ID(rmaLine.getC_Charge_ID());
				returnLine.set_ValueNoCheck(MInOutLine.COLUMNNAME_M_Product_ID, null);
				returnLine.set_ValueNoCheck(MInOutLine.COLUMNNAME_M_AttributeSetInstance_ID, null);
				returnLine.set_ValueNoCheck(MInOutLine.COLUMNNAME_M_Locator_ID, null);
			} else {
				returnLine.setM_Product_ID(rmaLine.getM_Product_ID());
				returnLine.setM_AttributeSetInstance_ID(rmaLine.getM_AttributeSetInstance_ID());
				returnLine.setM_Locator_ID(rmaLine.getM_Locator_ID());
			}

			returnLine.setC_UOM_ID(rmaLine.getC_UOM_ID());
			returnLine.setQty(rmaLine.getQty());
			returnLine.setC_Project_ID(rmaLine.getC_Project_ID());
			returnLine.setC_Campaign_ID(rmaLine.getC_Campaign_ID());
			returnLine.setC_Activity_ID(rmaLine.getC_Activity_ID());
			returnLine.saveEx();

			// 回写 M_RMALine.ReturnOrderLine_ID
			DB.executeUpdateEx("UPDATE M_RMALine SET ReturnOrderLine_ID=? WHERE M_RMALine_ID=?",
					new Object[] { returnLine.getM_InOutLine_ID(), rmaLine.getM_RMALine_ID() }, trxName);
		}
	}
}