package com.hoifu.event.processor;

import java.math.BigDecimal;

import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.Env;

import com.hoifu.model.MInOutNoticeLine;
import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;

public class InOutEventProcessor implements IEventProcessor {

	// ── 集团内出库单
	private static final String DOCTYPE_UU_INTRAGROUP_OUTBOUND = "8e07456e-132c-4fdb-9996-55d540c530c7";  
	// ── 集团内入库单
	private static final String DOCTYPE_UU_INTRAGROUP_INBOUND  = "dad137e1-fcf1-4d66-a250-0a83ea8167bd";  
	 
	
	private final IIQCService iqcService;
	private final IOQCService oqcService;
	private final IRQCService rqcService;

	public InOutEventProcessor(IIQCService iqcService, IOQCService oqcService, IRQCService rqcService) {
		this.iqcService = iqcService;
		this.oqcService = oqcService;
		this.rqcService = rqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOut;
	}

	@Override
	public void process(PO po, String topic) {
		MInOut inout = (MInOut) po;
		//更新发货通知单明细发货数量
		syncNoticeLineQtyDelivered(inout, topic);  
		syncRefInOutNo(inout, topic); 
		validateIntragroupInbound(inout, topic); 
		// 收发单完成前进行检验
		if (IEventTopics.DOC_BEFORE_COMPLETE.equals(topic)) {
			if (MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false)) {

				if (rqcService.isReturnDocument(inout))
					rqcService.validateBeforeReturn(inout);
				else if (inout.isSOTrx())
					oqcService.validateBeforeShipment(inout);
				else
					iqcService.validateBeforeReceipt(inout);
			}
		}
		// 收货单完成前：如果采购明细已关闭，更新损失数量
		if (IEventTopics.DOC_BEFORE_COMPLETE.equals(topic)) {
		    // 只处理采购收货（非销售、非退货）
		    if (!inout.isSOTrx() && inout.getMovementType().equals(MInOut.MOVEMENTTYPE_VendorReceipts)) {
		        for (MInOutLine iol : inout.getLines()) {
		            if (iol.getC_OrderLine_ID() == 0)
		                continue;
		            MOrderLine ol = new MOrderLine(inout.getCtx(), iol.getC_OrderLine_ID(), inout.get_TrxName());
		            if (!"CL".equals(ol.get_Value("OrderLineStatus")))
		                continue;
		            // 采购明细已关闭，更新损失数量
		            BigDecimal delta = iol.getMovementQty();
		            BigDecimal newQtyLostSales = ol.getQtyLostSales().subtract(delta);
		            ol.set_ValueNoCheck("QtyLostSales",
		                newQtyLostSales.signum() >= 0 ? newQtyLostSales : Env.ZERO);
		            ol.saveEx(inout.get_TrxName());
		        }
		    }
		}
	}
	
	/**
	 * 校验集团内入库单（G+）必须由对应的集团内出库单（G-）通过 C_DocTypeCounter 自动创建。 触发时机：PO_BEFORE_NEW
	 * 阻止方式：抛出 AdempiereException，由上层事件框架捕获并阻止 save()
	 */
	private void validateIntragroupInbound(MInOut inout, String topic) {
		if (!IEventTopics.PO_BEFORE_NEW.equals(topic))
			return;

		// 仅对集团内入库单生效
		MDocType docType = MDocType.get(inout.getC_DocType_ID());
		if (!DOCTYPE_UU_INTRAGROUP_INBOUND.equals(docType.get_UUID()))
			return;

		// 必须有对应的 G- 出库单（即通过 createCounterDoc 自动创建）
		if (inout.getRef_InOut_ID() == 0) {
			throw new AdempiereException("集团内入库单不允许手动创建，必须由集团内出库单完成时自动生成");
		}

		// 可选：进一步验证关联单据确实是集团内出库单
		MInOut counterInOut = new MInOut(inout.getCtx(), inout.getRef_InOut_ID(), inout.get_TrxName());
		MDocType counterDocType = MDocType.get(counterInOut.getC_DocType_ID());
		if (!DOCTYPE_UU_INTRAGROUP_OUTBOUND.equals(counterDocType.get_UUID())) {
			throw new AdempiereException("集团内入库单的关联单据必须是集团内出库单，实际关联：" + counterDocType.getName());
		}

	}
	
	/**  
	 * 触发时机：PO_BEFORE_NEW / PO_BEFORE_SAVE  
	 * 若 Ref_InOut_ID 有值，自动同步 Ref_InOut_No 为源单的单据编号。  
	 */  
	private void syncRefInOutNo(MInOut inout, String topic) {  
	    if (!IEventTopics.PO_BEFORE_NEW.equals(topic) && !IEventTopics.PO_BEFORE_CHANGE.equals(topic))  
	        return;  
	  
	    if (inout.getRef_InOut_ID() == 0)  
	        return;  
	  
	    // 仅在值发生变化时才重新赋值，避免不必要的 dirty mark  
	    MInOut sourceInOut = new MInOut(inout.getCtx(), inout.getRef_InOut_ID(), inout.get_TrxName());  
	    String refNo = sourceInOut.getDocumentNo();  
	    Object current = inout.get_Value("Ref_InOut_No");  
	    if (!refNo.equals(current)) {  
	        inout.set_ValueOfColumn("Ref_InOut_No", refNo);  
	    }  
	}
	
    
	/**  
     * 当发货单（MInOut）完成（DocStatus 变为 CO）时，  
     * 遍历所有发货单明细，通过 M_InOutLine_ID 找到对应的 M_InOutNoticeLine，  
     * 更新 QtyDelivered = MovementQty。  
     * 发货单撤销（DocStatus 变为 RE）时归零。  
     */  
    void syncNoticeLineQtyDelivered(MInOut inOut, String topic) {  
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic))  
            return;  
        if (!inOut.is_ValueChanged(MInOut.COLUMNNAME_DocStatus))  
            return;  
  
        String docStatus = inOut.getDocStatus();  
        boolean isCompleted = DocAction.STATUS_Completed.equals(docStatus);  
        boolean isReversed  = DocAction.STATUS_Reversed.equals(docStatus);  
        if (!isCompleted && !isReversed)  
            return;  
  
        for (MInOutLine inOutLine : inOut.getLines()) {  
            MInOutNoticeLine noticeLine = new Query(  
                    inOut.getCtx(),  
                    MInOutNoticeLine.Table_Name,  
                    "M_InOutLine_ID = ?",  
                    inOut.get_TrxName())  
                    .setParameters(inOutLine.getM_InOutLine_ID())  
                    .first();  
  
            if (noticeLine == null)  
                continue;  
  
            BigDecimal qtyDelivered = isCompleted  
                    ? inOutLine.getMovementQty()  
                    : BigDecimal.ZERO;  
  
            noticeLine.setQtyDelivered(qtyDelivered);  
            noticeLine.saveEx();  
        }  
    }  
}