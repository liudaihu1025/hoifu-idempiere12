package com.hoifu.delegate;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.BeforePrepare;  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.osgi.service.event.Event;  
  	
@EventTopicDelegate  
@ModelEventTopic(modelClass = MOrder.class)  
public class PurchaseOrderPrepareQtyPriceCheck extends ModelEventDelegate<MOrder> {  
  
    public PurchaseOrderPrepareQtyPriceCheck(MOrder po, Event event) {  
        super(po, event);  
    }  
  
    @BeforePrepare  
    public void onBeforePrepare() {  
        MOrder order = getModel();  
  
        // 只对采购单生效，跳过销售单  
        if (order.isSOTrx()) {  
            return;  
        }  
  
        MOrderLine[] lines = order.getLines(false, null);  
        for (MOrderLine line : lines) {  
            // 跳过描述行（无产品/价格/数量）  
            if (line.isDescription()) {  
                continue;  
            }  
  
            // 检查采购数量 <= 0  
            if (line.getQtyOrdered().signum() <= 0) {  
                throw new AdempiereException("@Line@ " + line.getLine()  
                        + ": @QtyOrdered@ " + line.getQtyOrdered() + " （需要大于0）");  
            }  
  
            // 检查价格 <= 0  
            if (line.getPriceActual().signum() <= 0) {  
                throw new AdempiereException("@Line@ " + line.getLine()  
                        + ": @PriceActual@ " + line.getPriceActual() + " （需要大于0）");  
            }  
        }  
    }  
}