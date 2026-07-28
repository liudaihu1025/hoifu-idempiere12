package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MDocType;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.PO;
import org.compiere.util.CLogger;

/**
 * 退库单明细（MInventoryLine）事件处理器
 *
 * <ul>
 *   <li>退库明细保存前（PO_BEFORE_NEW / PO_BEFORE_CHANGE）→ 校验 QtyInternalUse ≤ 可退库数量</li>
 * </ul>
 *
 * @author ldh
 * @date 2026年7月21日
 */
public class InventoryLineEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(InventoryLineEventProcessor.class);

	/** 退库单 DocType 名称 */
	private static final String RETURN_DOC_TYPE_NAME = "退库单";

	/** 支持的 PO 事件 */
	private static final List<String> SUPPORTED_TOPICS = List.of(
			IEventTopics.PO_BEFORE_NEW,
			IEventTopics.PO_BEFORE_CHANGE);

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInventoryLine && SUPPORTED_TOPICS.contains(topic);
	}

	@Override
	public void process(PO po, String topic) {
		MInventoryLine line = (MInventoryLine) po;
		
		MInventory parent = line.getParent();
		if (parent == null || parent.get_ID() <= 0)
			return;

		// 领用单（IU）：QtyInternalUse 不能为负数（冲销单除外）
		if (isInternalUseDocument(parent)) {
			validateIUQty(line, parent);
		}
		
		// 仅当 Ref_InventoryLine_ID 有值时，说明是退库单明细
		int refLineId = line.getRef_InventoryLine_ID();
		if (refLineId <= 0)
			return;

		// 确认父单是退库单
		if (!isReturnDocument(parent))
			return;

		// 跳过反冲单明细
		if (isReversalDocument(parent))
			return;

		// 校验退库数量不超过可退数量
		BigDecimal qtyInternalUse = line.getQtyInternalUse();
		if (qtyInternalUse == null || qtyInternalUse.signum() <= 0)
			return;

		MInventoryLine refLine = new MInventoryLine(line.getCtx(), refLineId, line.get_TrxName());
		BigDecimal returnableQty = refLine.getReturnableQty();
		if (qtyInternalUse.compareTo(returnableQty) > 0) {
			String msg = "退库数量(" + qtyInternalUse + ") 超过可退数量(" + returnableQty + ")";
			log.saveError("ValidationError", msg);
			throw new IllegalArgumentException(msg);
		}
	}
	
    // ──────────────────────────────────────────────  
    //  校验方法  
    // ──────────────────────────────────────────────  
	/**
	 * 领用单（IU）明细：QtyInternalUse 不能为负数。 冲销单由系统自动生成，其 QtyInternalUse =
	 * -原值，属于合法负数，跳过校验。
	 */
	private void validateIUQty(MInventoryLine line, MInventory parent) {
		if (isReversalDocument(parent))
			return;

		BigDecimal qty = line.getQtyInternalUse();
		if (qty != null && qty.compareTo(BigDecimal.ZERO) <= 0) {
			String msg = "领用单明细数量必须大于0（当前值：" + qty.toPlainString() + "），如需撤销请对原单使用冲销功能";
			log.saveError("ValidationError", msg);
			throw new IllegalArgumentException(msg);
		}
	}

	// ──────────────────────────────────────────────
	//  工具方法
	// ──────────────────────────────────────────────

	/** 判断是否为领用单（DocSubTypeInv = IU） */
	private boolean isInternalUseDocument(MInventory inventory) {
		MDocType dt = MDocType.get(inventory.getC_DocType_ID());
		return dt != null && MDocType.DOCSUBTYPEINV_InternalUseInventory.equals(dt.getDocSubTypeInv());
	}

	/**
	 * 判断是否为退库单（DocType Name = "退库单"）
	 */
	private boolean isReturnDocument(MInventory inventory) {
		MDocType dt = MDocType.get(inventory.getC_DocType_ID());
		return dt != null && RETURN_DOC_TYPE_NAME.equals(dt.getName());
	}

	/**
	 * 判断是否为反冲单（counter-document），使用持久化字段 Reversal_ID 跨 PO 实例可靠。
	 *
	 * <p>反冲单的 Reversal_ID 指向原单（较小 ID）→ {@code Reversal_ID > 0 && Reversal_ID < ID}
	 */
	private boolean isReversalDocument(MInventory doc) {
		if (doc.getReversal_ID() <= 0)
			return false;
		return doc.getReversal_ID() < doc.get_ID();
	}

}
