package com.hoifu.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.doc.BeforeVoid;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

/**
 * 销售订单作废前校验：检查是否关联了未作废的生产工单
 * 若存在任意一条关联的 PP_Order 且 DocStatus != 'VO'，则阻止作废
 */
@EventTopicDelegate
@ModelEventTopic(modelClass = MOrder.class)
public class SalesOrderVoidPPOrderCheck extends ModelEventDelegate<MOrder> {

    public SalesOrderVoidPPOrderCheck(MOrder po, Event event) {
        super(po, event);
    }

    @BeforeVoid
    public void onBeforeVoid() {
        MOrder order = getModel();

        // 只对销售订单生效，采购订单直接放行
        if (!order.isSOTrx()) {
            return;
        }

        MOrderLine[] lines = order.getLines(false, null);
        if (lines == null || lines.length == 0) {
            return;
        }

        for (MOrderLine line : lines) {
            // 查询该订单行关联的未作废工单数量
			int count = new Query(Env.getCtx(), "PP_Order",
                    "C_OrderLine_ID=? AND DocStatus<>?", order.get_TrxName())
                    .setParameters(line.getC_OrderLine_ID(), "VO")
                    .setOnlyActiveRecords(true)
                    .count();

            if (count > 0) {
                throw new AdempiereException("销售订单已关联工单，作废失败");
            }
        }
    }
}
