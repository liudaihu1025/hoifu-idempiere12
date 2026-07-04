package com.hoifu.event.processor;  
  
import java.math.BigDecimal;  
  
import org.adempiere.base.event.IEventTopics;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.PO;  
  
public class COrderLineEventProcessor implements IEventProcessor {  
  
    private static final String COLUMNNAME_DeliveryStatus = "DeliveryStatus";  
  
    public enum DeliveryStatus {  
        NotShipped,  
        PartiallyShipped,  
        FullyShipped,  
        OverShipped  
    }  
  
    @Override  
    public boolean supports(PO po, String topic) {  
        return po instanceof MOrderLine;  
    }  
  
    @Override  
    public void process(PO po, String topic) {  
        MOrderLine line = (MOrderLine) po;
        
        //更新订单行发货状态
        syncDeliveryStatus(line, topic);  
    }  
  
    /**  
     * 根据发货数量与订单数量的关系，自动维护 DeliveryStatus 字段  
     */  
    void syncDeliveryStatus(MOrderLine line, String topic) {
        // 仅在 AFTER_CHANGE 且 QtyDelivered 确实变化时，或 AFTER_NEW 时触发  
        boolean isAfterChange = IEventTopics.PO_BEFORE_CHANGE.equals(topic)  
                && line.is_ValueChanged(MOrderLine.COLUMNNAME_QtyDelivered);  
        boolean isAfterNew = IEventTopics.PO_BEFORE_NEW.equals(topic);  
  
        if (!isAfterChange && !isAfterNew) {  
            return; 
        }  
  
        BigDecimal qtyDelivered = line.getQtyDelivered();  
        BigDecimal qtyOrdered   = line.getQtyOrdered();  
  
        if (qtyDelivered == null) {  
            qtyDelivered = BigDecimal.ZERO;  
        }  
        if (qtyOrdered == null) {  
            qtyOrdered = BigDecimal.ZERO;  
        }  
  
        DeliveryStatus status;  
        int cmp = qtyDelivered.compareTo(BigDecimal.ZERO);  
  
        if (cmp == 0) {  
            // 发货数量 = 0  
            status = DeliveryStatus.NotShipped;  
        } else {  
            int cmpToOrdered = qtyDelivered.compareTo(qtyOrdered);  
            if (cmpToOrdered < 0) {  
                // 0 < 发货数量 < 订单数量  
                status = DeliveryStatus.PartiallyShipped;  
            } else if (cmpToOrdered == 0) {  
                // 发货数量 = 订单数量  
                status = DeliveryStatus.FullyShipped;  
            } else {  
                // 发货数量 > 订单数量  
                status = DeliveryStatus.OverShipped;  
            }  
        }  
  
        line.set_CustomColumn(COLUMNNAME_DeliveryStatus, status.name());  
    }  
}
