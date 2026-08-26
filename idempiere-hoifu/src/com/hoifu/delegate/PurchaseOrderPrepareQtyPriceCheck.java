package com.hoifu.delegate;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.BeforePrepare;  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MDocType;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.osgi.service.event.Event;  
  
@EventTopicDelegate  
@ModelEventTopic(modelClass = MOrder.class)  
public class PurchaseOrderPrepareQtyPriceCheck extends ModelEventDelegate<MOrder> {  
  
    // 只针对采购订单单据类型（UUID）生效  
    private static final String PurchaseOrder_DOCTYPE_UU = "1b5b0262-ed61-4f5d-a844-5a88da658491";  
  
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
  
        // 只对指定的单据类型生效  
        MDocType docType = MDocType.get(order.getCtx(), order.getC_DocType_ID());  
        if (docType == null || !PurchaseOrder_DOCTYPE_UU.equals(docType.getC_DocType_UU())) {  
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