package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMatchPO;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRMALine;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.enums.HFSysConfigEnum;
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

		// 采购退货收发单完成后，反冲 M_MatchPO 以恢复采购订单行可创
		restoreOrderLineQtyOnVendorReturn(inout, topic);

		// 客户退货完成后，校验退货数量不超过已发货数量
		validateCustomerReturnQty(inout, topic);

		// 【新增】客户退货完成前，按暂估应收冲减策略（A/R）校验数量是否合法，不合法直接阻止完成
		validateReturnAccrualQtyBeforeComplete(inout, topic);

		// 客户退货完成后，更新销售订单发货数量
		updateOrderLineQtyDeliveredOnCustomerReturn(inout, topic);

		// 采购收货/退货完成后，重新计算订单 IsDelivered
		updateOrderIsDelivered(inout, topic); 

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

    /**
     * 采购退货（供应商退货）完成后，创建负数 M_MatchPO 记录，恢复采购订单行可创建数量。
     * M_MatchPO 是采购专属机制，不能用于销售侧。
     */
    void restoreOrderLineQtyOnVendorReturn(MInOut inOut, String topic) {
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic))
            return;
        if (!inOut.is_ValueChanged(MInOut.COLUMNNAME_DocStatus))
            return;
        if (!DocAction.STATUS_Completed.equals(inOut.getDocStatus()))
            return;
        // 仅处理采购退货（供应商退货）—— M_MatchPO 是采购专属机制
        if (inOut.isSOTrx())
            return;
        if (!MInOut.MOVEMENTTYPE_VendorReturns.equals(inOut.getMovementType()))
            return;

        for (MInOutLine iol : inOut.getLines()) {
            if (iol.getM_RMALine_ID() <= 0)
                continue;

            MRMALine rmaLine = new MRMALine(inOut.getCtx(), iol.getM_RMALine_ID(), inOut.get_TrxName());
            if (rmaLine.getM_InOutLine_ID() <= 0)
                continue;

            MInOutLine originalLine = new MInOutLine(inOut.getCtx(), rmaLine.getM_InOutLine_ID(), inOut.get_TrxName());
            if (originalLine.getC_OrderLine_ID() <= 0)
                continue;

            // 计算该收货行当前的净匹配数量（含历史退货）
            BigDecimal netMatchedQty = DB.getSQLValueBD(inOut.get_TrxName(),
                    "SELECT COALESCE(SUM(Qty),0) FROM M_MatchPO WHERE C_OrderLine_ID=? AND M_InOutLine_ID=?",
                    originalLine.getC_OrderLine_ID(), originalLine.getM_InOutLine_ID());

            if (netMatchedQty.compareTo(iol.getMovementQty()) < 0) {
                throw new AdempiereException("退货数量超过该收货行当前净匹配数量，无法处理");
            }

            // 创建一条负数 MatchPO 记录，扣减退货数量
            MMatchPO reversal = new MMatchPO(inOut.getCtx(), 0, inOut.get_TrxName());
            reversal.setC_OrderLine_ID(originalLine.getC_OrderLine_ID());
            reversal.setM_InOutLine_ID(originalLine.getM_InOutLine_ID());
            reversal.setM_Product_ID(iol.getM_Product_ID());
            reversal.setM_AttributeSetInstance_ID(iol.getM_AttributeSetInstance_ID());
            reversal.setAD_Org_ID(inOut.getAD_Org_ID());
            reversal.setQty(iol.getMovementQty().negate());
            reversal.setDateAcct(inOut.getMovementDate());
            reversal.setDateTrx(inOut.getMovementDate());
            reversal.setPosted(false);
            reversal.setProcessed(true);
            reversal.saveEx(inOut.get_TrxName());
        }
    }

    /**
	 * 客户退货完成后，校验退货数量不超过原发货行已发货数量（累计历史退货）。
	 * 
	 */
    void validateCustomerReturnQty(MInOut inOut, String topic) {
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic))
            return;
        if (!inOut.is_ValueChanged(MInOut.COLUMNNAME_DocStatus))
            return;
        if (!DocAction.STATUS_Completed.equals(inOut.getDocStatus()))
            return;
        // 仅处理客户退货
        if (!inOut.isSOTrx())
            return;
        if (!MInOut.MOVEMENTTYPE_CustomerReturns.equals(inOut.getMovementType()))
            return;

        for (MInOutLine iol : inOut.getLines()) {
            if (iol.getM_RMALine_ID() <= 0)
                continue;

            MRMALine rmaLine = new MRMALine(inOut.getCtx(), iol.getM_RMALine_ID(), inOut.get_TrxName());
            if (rmaLine.getM_InOutLine_ID() <= 0)
                continue;

            MInOutLine originalLine = new MInOutLine(inOut.getCtx(), rmaLine.getM_InOutLine_ID(), inOut.get_TrxName());

            // 统计该原发货行累计已完成的退货数量（含本次）
            BigDecimal netReturnedQty = DB.getSQLValueBD(inOut.get_TrxName(),
                    "SELECT COALESCE(SUM(iol2.MovementQty),0) "
                  + "FROM M_InOutLine iol2 "
                  + "JOIN M_InOut io2 ON io2.M_InOut_ID = iol2.M_InOut_ID "
                  + "JOIN M_RMALine rma2 ON rma2.M_RMALine_ID = iol2.M_RMALine_ID "
                  + "WHERE rma2.M_InOutLine_ID = ? AND io2.DocStatus = 'CO'",
                    originalLine.getM_InOutLine_ID());

            if (netReturnedQty.compareTo(originalLine.getMovementQty()) > 0) {
                throw new AdempiereException("退货数量超过该发货行已发货数量，无法处理");
            }
        }
    }

    /**
     * 采购收货单完成/反冲、供应商退货单完成后，重新计算关联采购订单的 IsDelivered 状态。
     *
     * 触发条件：PO_AFTER_CHANGE，DocStatus 变为 Completed 或 Reversed，
     *          仅处理采购方向（!isSOTrx），MovementType 为 VendorReceipts 或 VendorReturns。
     */
    void updateOrderIsDelivered(MInOut inOut, String topic) {
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic))
            return;
        if (!inOut.is_ValueChanged(MInOut.COLUMNNAME_DocStatus))
            return;
        String docStatus = inOut.getDocStatus();
        if (!DocAction.STATUS_Completed.equals(docStatus) && !DocAction.STATUS_Reversed.equals(docStatus))
            return;
        if (inOut.isSOTrx())
            return;
        String movementType = inOut.getMovementType();
        if (!MInOut.MOVEMENTTYPE_VendorReceipts.equals(movementType)
                && !MInOut.MOVEMENTTYPE_VendorReturns.equals(movementType))
            return;

        // 收集本次收发单涉及的所有采购订单 ID（去重）
        Set<Integer> orderIds = new HashSet<>();
        for (MInOutLine iol : inOut.getLines()) {
            int orderLineId = iol.getC_OrderLine_ID();

			// 退货行没有 C_OrderLine_ID，需要通过 M_RMALine_ID 反查原始收货行
            if (orderLineId <= 0 && iol.getM_RMALine_ID() > 0) {
                MRMALine rmaLine = new MRMALine(inOut.getCtx(), iol.getM_RMALine_ID(), inOut.get_TrxName());
                if (rmaLine.getM_InOutLine_ID() > 0) {
                    MInOutLine originalLine = new MInOutLine(inOut.getCtx(), rmaLine.getM_InOutLine_ID(), inOut.get_TrxName());
                    orderLineId = originalLine.getC_OrderLine_ID();
                }
            }

            if (orderLineId <= 0)
                continue;

            MOrderLine ol = new MOrderLine(inOut.getCtx(), orderLineId, inOut.get_TrxName());
            if (ol.getC_Order_ID() > 0)
                orderIds.add(ol.getC_Order_ID());
        }

        for (Integer orderId : orderIds) {
            MOrder order = new MOrder(inOut.getCtx(), orderId, inOut.get_TrxName());
            updateOrderDeliveredStatus(order, inOut.get_TrxName());
        }
    }

    /**
     * 检查订单所有明细的 QtyDelivered >= QtyEntered，
     * 若全部满足则将 C_Order.IsDelivered 设为 true，否则设为 false。
     * 只在值发生变化时才执行 saveEx，避免不必要的数据库写入。
     */
    private void updateOrderDeliveredStatus(MOrder order, String trxName) {
        MOrderLine[] lines = order.getLines(true, null);
        if (lines == null || lines.length == 0)
            return;

        boolean allDelivered = true;
        for (MOrderLine line : lines) {
            if (line.getQtyDelivered().compareTo(line.getQtyEntered()) < 0) {
                allDelivered = false;
                break;
            }
        }

        if (order.isDelivered() != allDelivered) {
            order.setIsDelivered(allDelivered);
            order.saveEx(trxName);
        }
    }

	/**
	 * 客户退货完成后，扣减原发货订单行的 QtyDelivered 反冲（Reversed）时把扣减的数量加回去。
	 */
	void updateOrderLineQtyDeliveredOnCustomerReturn(MInOut inOut, String topic) {
		if (!IEventTopics.PO_AFTER_CHANGE.equals(topic))
			return;
		if (!inOut.is_ValueChanged(MInOut.COLUMNNAME_DocStatus))
			return;

		String docStatus = inOut.getDocStatus();
		boolean isCompleted = DocAction.STATUS_Completed.equals(docStatus);
		boolean isReversed = DocAction.STATUS_Reversed.equals(docStatus);
		if (!isCompleted && !isReversed)
			return;

		// 仅处理客户退货
		if (!inOut.isSOTrx())
			return;
		if (!MInOut.MOVEMENTTYPE_CustomerReturns.equals(inOut.getMovementType()))
			return;

		for (MInOutLine iol : inOut.getLines()) {
			if (iol.getM_RMALine_ID() <= 0)
				continue;

			MRMALine rmaLine = new MRMALine(inOut.getCtx(), iol.getM_RMALine_ID(), inOut.get_TrxName());
			if (rmaLine.getM_InOutLine_ID() <= 0)
				continue;

			MInOutLine originalLine = new MInOutLine(inOut.getCtx(), rmaLine.getM_InOutLine_ID(), inOut.get_TrxName());
			if (originalLine.getC_OrderLine_ID() <= 0)
				continue;

			MOrderLine orderLine = new MOrderLine(inOut.getCtx(), originalLine.getC_OrderLine_ID(),
					inOut.get_TrxName());

			BigDecimal delta = iol.getMovementQty();
			BigDecimal newQtyDelivered = isCompleted ? orderLine.getQtyDelivered().subtract(delta) // 退货完成：扣减
					: orderLine.getQtyDelivered().add(delta); // 退货反冲：加回

			orderLine.setQtyDelivered(newQtyDelivered.signum() >= 0 ? newQtyDelivered : Env.ZERO);
			orderLine.saveEx(inOut.get_TrxName());
		}
	}

	/**
	 * 客户退货完成前（DOC_BEFORE_COMPLETE）校验：按"暂估应收冲减方式"策略 （组织级配置
	 * HF_ACCRUED_RECEIVABLE_REVERSAL_TYPE，A=优先冲暂估应收/未开票优先，
	 * R=优先冲正式应收/已开票优先），校验退货明细行的冲暂估/冲正式数量是否合法， 不合法则抛出异常，阻止退货单完成。
	 *
	 */
	private void validateReturnAccrualQtyBeforeComplete(MInOut inOut, String topic) {
		if (!IEventTopics.DOC_BEFORE_COMPLETE.equals(topic))
			return;
		// 仅处理客户退货
		if (!inOut.isSOTrx())
			return;
		if (!MInOut.MOVEMENTTYPE_CustomerReturns.equals(inOut.getMovementType()))
			return;

		// 本单据内的两个"预占用"累加器，循环内逐行累加，模拟过账时的顺序消耗
		// 按发货行 ID，累计本单据内前面行已占用的暂估池数量
		Map<Integer, BigDecimal> consumedAccrualQtyByShipLine = new HashMap<>();

		// 按订单行 ID，累计本单据内前面行已占用的正式应收（已开票）额度
		Map<Integer, BigDecimal> consumedFormalQtyByOrderLine = new HashMap<>();

		for (MInOutLine iol : inOut.getLines()) {
			if (iol.getM_RMALine_ID() <= 0)
				continue;
			// 只校验正常退货行，红冲行（ReversalLine_ID>0）不重新校验
			if (iol.getReversalLine_ID() > 0)
				continue;

			MRMALine rmaLine = new MRMALine(inOut.getCtx(), iol.getM_RMALine_ID(), inOut.get_TrxName());
			if (rmaLine.getM_InOutLine_ID() <= 0)
				continue;

			MInOutLine shipLine = new MInOutLine(inOut.getCtx(), rmaLine.getM_InOutLine_ID(), inOut.get_TrxName());
			if (shipLine.getC_OrderLine_ID() <= 0)
				continue;

			MOrderLine orderLine = new MOrderLine(inOut.getCtx(), shipLine.getC_OrderLine_ID(), inOut.get_TrxName());

			BigDecimal returnQty = iol.getMovementQty().abs(); // R：退货数量
			BigDecimal shipQty = shipLine.getMovementQty().abs(); // 发货数量

			// 通用校验1：退货数量 > 发货数量
			if (returnQty.compareTo(shipQty) > 0) {
				throw new AdempiereException("退货数量（" + returnQty + "）超过发货数量（" + shipQty + "），无法处理。");
			}

			BigDecimal shipAccrualQty = (BigDecimal) shipLine.get_Value("AccrualQty");
			if (shipAccrualQty == null) {
				shipAccrualQty = BigDecimal.ZERO;
			}
			BigDecimal shipReversedQty = (BigDecimal) shipLine.get_Value("AccrualReversedQty");
			if (shipReversedQty == null) {
				shipReversedQty = BigDecimal.ZERO;
			}
			BigDecimal remainingQty = shipAccrualQty.subtract(shipReversedQty); // 未冲暂估数量（数据库真实值）

			// 通用校验2：未冲暂估数量为负数（用数据库真实值判断，不受本单内预占用影响，判断的是历史数据本身是否异常）
			if (remainingQty.signum() < 0) {
				throw new AdempiereException("发货明细的未冲暂估数量异常（" + remainingQty + "），请检查历史单据。");
			}

			// 扣除本单据内前面行已经预占用的暂估池数量，得到"对本行而言"真正可用的剩余量
			int shipLineKey = shipLine.getM_InOutLine_ID();
			BigDecimal usedAccrualQty = consumedAccrualQtyByShipLine.getOrDefault(shipLineKey, BigDecimal.ZERO);
			BigDecimal effectiveRemainingQty = remainingQty.subtract(usedAccrualQty);
			if (effectiveRemainingQty.signum() < 0) {
				effectiveRemainingQty = BigDecimal.ZERO;
			}

			BigDecimal qtyInvoiced = orderLine.getQtyInvoiced(); // I：已开票数量（数据库真实值）
			// 扣除本单据内前面行已经预占用的正式应收额度，得到"对本行而言"真正可用的已开票额度
			int orderLineKey = orderLine.getC_OrderLine_ID();
			BigDecimal usedFormalQty = consumedFormalQtyByOrderLine.getOrDefault(orderLineKey, BigDecimal.ZERO);
			BigDecimal effectiveQtyInvoiced = qtyInvoiced.subtract(usedFormalQty);
			if (effectiveQtyInvoiced.signum() < 0) {
				effectiveQtyInvoiced = BigDecimal.ZERO;
			}

			BigDecimal unInvoicedQty = shipQty.subtract(effectiveQtyInvoiced); // U：未开票数量（基于扣除后的有效已开票额度）
			if (unInvoicedQty.signum() < 0) {
				unInvoicedQty = BigDecimal.ZERO;
			}

			String reversalType = HFSysConfigEnum.HF_ACCRUED_RECEIVABLE_REVERSAL_TYPE.getValue(inOut.getAD_Client_ID(),
					iol.getAD_Org_ID());

			BigDecimal thisLineAccrualQty; // 本行实际占用暂估池的数量，用于事后累加进 usedAccrualQty
			BigDecimal thisLineFormalQty; // 本行实际占用正式应收额度的数量，用于事后累加进 usedFormalQty

			if ("R".equals(reversalType)) {
				// 策略R：优先冲正式应收（已开票优先）
				BigDecimal formalQty = returnQty.min(effectiveQtyInvoiced); // F = min(R, 有效I)
				BigDecimal overQty = returnQty.subtract(formalQty); // O = R - F
				if (overQty.signum() > 0 && overQty.compareTo(effectiveRemainingQty) > 0) {
					throw new AdempiereException(
							"超出已开票部分的数量（" + overQty + "）大于未冲暂估数量（" + effectiveRemainingQty + "），无法处理。");
				}
				thisLineFormalQty = formalQty;
				thisLineAccrualQty = overQty.signum() > 0 ? overQty : BigDecimal.ZERO;
			} else {
				// 策略A（默认）：优先冲暂估应收（未开票优先）
				BigDecimal candidateQty = returnQty.min(unInvoicedQty); // 候选冲暂估数量 = min(R, 有效U)
				BigDecimal accrualQty = candidateQty.min(effectiveRemainingQty); // A：本次可冲暂估数量
				BigDecimal formalQty = returnQty.subtract(accrualQty); // F = R - A

				if (formalQty.compareTo(effectiveQtyInvoiced) > 0) {
					throw new AdempiereException(
							"冲正式应收数量（" + formalQty + "）超过已开票数量（" + effectiveQtyInvoiced + "），请检查历史单据。");
				}
				if (accrualQty.compareTo(candidateQty) < 0) {
					throw new AdempiereException("实际可冲暂估数量（" + accrualQty + "）小于应冲暂估数量（" + candidateQty + "），请检查历史单据。");
				}
				thisLineAccrualQty = accrualQty;
				thisLineFormalQty = formalQty;
			}

			// 本行校验通过后，把本行占用的量累加进两个"预占用"累加器，供本单据内后续行使用
			consumedAccrualQtyByShipLine.put(shipLineKey, usedAccrualQty.add(thisLineAccrualQty));
			consumedFormalQtyByOrderLine.put(orderLineKey, usedFormalQty.add(thisLineFormalQty));
		}
	}


}