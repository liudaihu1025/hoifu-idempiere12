package com.hoifu.delegate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.doc.AfterComplete;
import org.adempiere.base.event.annotations.doc.AfterReverseAccrual;
import org.adempiere.base.event.annotations.doc.AfterReverseCorrect;
import org.adempiere.base.event.annotations.doc.AfterVoid;
import org.adempiere.base.event.annotations.doc.BeforeComplete;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClientInfo;
import org.compiere.model.MDocType;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MProduct;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

/**
 * 库存单（MInventory）表头单据事件委托。
 *
 * <p>由原 com.hoifu.event.processor.InventoryEventProcessor 迁移而来，改走注解式注册通道
 * （{@code @EventTopicDelegate} + {@code @ModelEventTopic}），从根本上避开与
 * {@code BaseEnventHandler} 重复注册的问题。</p>
 *
 * <ul>
 *   <li>完成前（DOC_BEFORE_COMPLETE）→ 库存盘点入库校验物料成本 + 退库单数量校验</li>
 *   <li>完成（DOC_AFTER_COMPLETE）→ 累加领用单的 QtyReturned</li>
 *   <li>作废（DOC_AFTER_VOID）→ 回滚领用单的 QtyReturned</li>
 *   <li>反冲（DOC_AFTER_REVERSECORRECT / DOC_AFTER_REVERSEACCRUAL）→ 回滚领用单的 QtyReturned</li>
 * </ul>
 *
 * @author ldh
 * @date 2026年8月14日
 */
@EventTopicDelegate
@ModelEventTopic(modelClass = MInventory.class)
public class InventoryReturnDelegate extends ModelEventDelegate<MInventory> {

	private static final CLogger log = CLogger.getCLogger(InventoryReturnDelegate.class);

	/** 退库单 DocType 名称 */
	private static final String RETURN_DOC_TYPE_NAME = "退库单";

	public InventoryReturnDelegate(MInventory po, Event event) {
		super(po, event);
	}

	@BeforeComplete
	public void onBeforeComplete() {
		MInventory inventory = getModel();
		// 库存盘点入库校验物料成本（仅 PI 类型）
		validateProductCost(inventory);
		// 退库单完成前校验退库数量
		if (isReturnDocument(inventory))
			validateReturnDocument(inventory);
	}

	@AfterComplete
	public void onAfterComplete() {
		MInventory inventory = getModel();
		if (isReturnDocument(inventory))
			onReturnDocumentCompleted(inventory);
	}

	@AfterVoid
	@AfterReverseCorrect
	@AfterReverseAccrual
	public void onAfterVoidOrReverse() {
		MInventory inventory = getModel();
		if (isReturnDocument(inventory))
			onReturnDocumentVoided(inventory);
	}

	// ──────────────────────────────────────────────
	//  核心业务方法
	// ──────────────────────────────────────────────

	/**
	 * 退库单完成前：按物料汇总验证退库数量
	 */
	private void validateReturnDocument(MInventory returnDoc) {
		if (isReversalDocument(returnDoc))
			return;

		int requisitionId = returnDoc.getRef_Inventory_ID();
		if (requisitionId <= 0)
			return;

		MInventory requisition = new MInventory(returnDoc.getCtx(), requisitionId, returnDoc.get_TrxName());
		MInventoryLine[] reqLines = requisition.getLines(false);

		Map<Integer, BigDecimal> productReturnQtyMap = new HashMap<>();
		for (MInventoryLine returnLine : returnDoc.getLines(false)) {
			if (!returnLine.isActive())
				continue;

			int refLineId = returnLine.getRef_InventoryLine_ID();
			if (refLineId <= 0)
				continue;

			BigDecimal qtyInternalUse = returnLine.getQtyInternalUse();
			if (qtyInternalUse == null || qtyInternalUse.signum() <= 0)
				continue;

			for (MInventoryLine reqLine : reqLines) {
				if (reqLine.get_ID() == refLineId) {
					int productId = reqLine.getM_Product_ID();
					productReturnQtyMap.merge(productId, qtyInternalUse, BigDecimal::add);
					break;
				}
			}
		}

		for (Map.Entry<Integer, BigDecimal> entry : productReturnQtyMap.entrySet()) {
			int productId = entry.getKey();
			BigDecimal totalReturnQty = entry.getValue();

			BigDecimal totalReturnableQty = Env.ZERO;
			for (MInventoryLine reqLine : reqLines) {
				if (reqLine.getM_Product_ID() == productId) {
					totalReturnableQty = totalReturnableQty.add(reqLine.getReturnableQty());
				}
			}

			if (totalReturnQty.compareTo(totalReturnableQty) > 0) {
				MProduct product = MProduct.get(returnDoc.getCtx(), productId);
				String msg = "物料 [" + product.getName() + "] 退库数量(" + totalReturnQty
						+ ") 超过可退数量(" + totalReturnableQty + ")";
				log.saveError("ValidationError", msg);
				throw new IllegalArgumentException(msg);
			}
		}
	}

	/**
	 * 退库单完成：累加更新领用单的 QtyReturned<br/>
	 * 反冲单（冲销单）跳过：反冲单的 QtyReturned 回滚由 DOC_AFTER_REVERSECORRECT /
	 * DOC_AFTER_REVERSEACCRUAL 触发 {@link #onReturnDocumentVoided} 完成，避免重复计算。
	 */
	private void onReturnDocumentCompleted(MInventory returnDoc) {
		if (isReversalDocument(returnDoc))
			return;

		int requisitionId = returnDoc.getRef_Inventory_ID();
		if (requisitionId <= 0)
			return;

		MInventory requisition = new MInventory(returnDoc.getCtx(), requisitionId, returnDoc.get_TrxName());
		MInventoryLine[] reqLines = requisition.getLines(false);

		for (MInventoryLine returnLine : returnDoc.getLines(false)) {
			if (!returnLine.isActive())
				continue;

			int refLineId = returnLine.getRef_InventoryLine_ID();
			if (refLineId <= 0)
				continue;

			for (MInventoryLine reqLine : reqLines) {
				if (reqLine.get_ID() == refLineId) {
					BigDecimal currentReturned = reqLine.getQtyReturned();
					BigDecimal qtyInternalUse = returnLine.getQtyInternalUse();
					if (qtyInternalUse == null || qtyInternalUse.signum() == 0)
						continue;
					BigDecimal newReturned = currentReturned.add(qtyInternalUse);
					reqLine.setQtyReturned(newReturned);
					reqLine.saveEx(returnDoc.get_TrxName());
					log.fine("退库单完成: 领用单 #" + requisition.getDocumentNo()
							+ " 行 " + reqLine.getLine() + " QtyReturned = " + newReturned);
					break;
				}
			}
		}
	}

	/**
	 * 退库单作废：回滚 QtyReturned（减去 QtyInternalUse）
	 */
	private void onReturnDocumentVoided(MInventory returnDoc) {
		int requisitionId = returnDoc.getRef_Inventory_ID();
		if (requisitionId <= 0)
			return;

		MInventory requisition = new MInventory(returnDoc.getCtx(), requisitionId, returnDoc.get_TrxName());
		MInventoryLine[] reqLines = requisition.getLines(false);

		for (MInventoryLine returnLine : returnDoc.getLines(false)) {
			if (!returnLine.isActive())
				continue;

			int refLineId = returnLine.getRef_InventoryLine_ID();
			if (refLineId <= 0)
				continue;

			for (MInventoryLine reqLine : reqLines) {
				if (reqLine.get_ID() == refLineId) {
					BigDecimal currentReturned = reqLine.getQtyReturned();
					BigDecimal qtyInternalUse = returnLine.getQtyInternalUse();
					if (qtyInternalUse == null || qtyInternalUse.signum() == 0)
						continue;
					BigDecimal newReturned = currentReturned.subtract(qtyInternalUse);
					reqLine.setQtyReturned(newReturned.signum() >= 0 ? newReturned : Env.ZERO);
					reqLine.saveEx(returnDoc.get_TrxName());
					log.fine("退库单作废: 领用单 #" + requisition.getDocumentNo()
							+ " 行 " + reqLine.getLine() + " QtyReturned = " + newReturned);
					break;
				}
			}
		}
	}

	private void validateProductCost(MInventory inventory) {
		// 冲销单跳过：Reversal_ID > 0 且 < 自身ID 表示本单是冲销单
		if (inventory.getReversal_ID() > 0 && inventory.getReversal_ID() < inventory.get_ID())
			return;

		MDocType dt = MDocType.get(inventory.getC_DocType_ID());
		if (dt == null)
			return;

		String docSubTypeInv = dt.getDocSubTypeInv();
		boolean isPI = MDocType.DOCSUBTYPEINV_PhysicalInventory.equals(docSubTypeInv);
		if (!isPI)
			return; // 只校验库存盘点，其他类型不校验

		MClientInfo clientInfo = MClientInfo.get(inventory.getCtx(), inventory.getAD_Client_ID(),
				inventory.get_TrxName());
		if (clientInfo == null)
			return;
		MAcctSchema as = clientInfo.getMAcctSchema1();
		if (as == null)
			return;

		MInventoryLine[] lines = inventory.getLines(false);
		List<String> errors = new ArrayList<>();

		for (MInventoryLine line : lines) {
			if (!line.isActive())
				continue;

			// 计算本行库存变动方向
			BigDecimal qtyDiff = line.getQtyCount().subtract(line.getQtyBook()); // PI: MovementQty = QtyCount - QtyBook

			if (qtyDiff.signum() <= 0)
				continue; // 只校验入库行

			String sql = "SELECT COALESCE(SUM(mc.CurrentCostPrice), 0) " + "FROM M_Cost mc "
					+ "JOIN M_CostElement ce ON mc.M_CostElement_ID = ce.M_CostElement_ID "
					+ "WHERE mc.AD_Client_ID = ? AND mc.AD_Org_ID = ? "
					+ "  AND mc.M_Product_ID = ? AND mc.C_AcctSchema_ID = ? " + "  AND ce.CostElementType = 'M'";

			BigDecimal cost = DB.getSQLValueBDEx(inventory.get_TrxName(), sql, inventory.getAD_Client_ID(),
					inventory.getAD_Org_ID(), line.getM_Product_ID(), as.getC_AcctSchema_ID());

			if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
				String productDesc = Optional.ofNullable(MProduct.get(inventory.getCtx(), line.getM_Product_ID()))
						.map(p -> String.format("%s - %s", p.getValue(), p.getName()))
						.orElse("ID=" + line.getM_Product_ID());
				errors.add(String.format("第%d行 物料【%s】成本不存在或为0，请先通过【采购收货】或【成本调整】建立物料成本", line.getLine(), productDesc));
			}
		}

		if (!errors.isEmpty()) {
			String msg = String.join("\n", errors);
			log.saveError("ValidationError", msg);
			throw new IllegalArgumentException(msg);
		}
	}

	// ──────────────────────────────────────────────
	//  工具方法
	// ──────────────────────────────────────────────

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
	 * <p>iDempiere 的 reverse() 逻辑：
	 * <ul>
	 *   <li>反冲单的 Reversal_ID 指向原单（较小 ID）→ {@code Reversal_ID > 0 && Reversal_ID < ID}</li>
	 *   <li>原单的 Reversal_ID 指向反冲单（较大 ID）→ {@code Reversal_ID > ID}</li>
	 * </ul>
	 */
	private boolean isReversalDocument(MInventory doc) {
		if (doc.getReversal_ID() <= 0)
			return false;
		return doc.getReversal_ID() < doc.get_ID();
	}

}
