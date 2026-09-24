package com.hoifu.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.doc.BeforeVoid;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.Query;
import org.osgi.service.event.Event;

/**
 * 采购订单作废前校验：检查是否关联了未返冲的收货单 若存在任意一条关联的 M_InOut 且 DocStatus != 'RE'（已返冲），则阻止作废
 */
@EventTopicDelegate
@ModelEventTopic(modelClass = MOrder.class)
public class PurchaseOrderVoidInOutCheck extends ModelEventDelegate<MOrder> {

	public PurchaseOrderVoidInOutCheck(MOrder po, Event event) {
		super(po, event);
	}

	@BeforeVoid
	public void onBeforeVoid() {
		MOrder order = getModel();

		// 只对采购订单生效，销售订单直接放行
		if (order.isSOTrx()) {
			return;
		}

		if (order.getC_Order_ID() <= 0) {
			return;
		}

		// 查询该采购订单下关联的、未返冲的收货单数量
		int count = new Query(order.getCtx(), "M_InOut", "C_Order_ID=? AND DocStatus<>?", order.get_TrxName())
				.setParameters(order.getC_Order_ID(), "RE").setOnlyActiveRecords(true).count();

		if (count > 0) {
			throw new AdempiereException("采购订单已关联收货单，作废失败");
		}
	}
}