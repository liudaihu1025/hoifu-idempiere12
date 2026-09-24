package com.hoifu.delegate;  
  

import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.AfterComplete;
import org.adempiere.base.event.annotations.doc.AfterReactivate;
import org.adempiere.base.event.annotations.doc.AfterVoid;
import org.compiere.model.MOrder;  
import org.compiere.model.X_C_Order;  
import org.compiere.util.CLogger;  
import org.osgi.service.event.Event;  
import com.hoifu.service.OrderInOutRequisitionService;  
  
/**  
 * C_Order（销售/采购订单）表头事件委托。  
 *  
 * <p>订单完成（DOC_AFTER_COMPLETE）后，按订单本身及其明细行，  
 * 自动生成一张 M_InOut_Requisition（出入库申请单，草稿状态）
 */  
@EventTopicDelegate  
@ModelEventTopic(modelClass = X_C_Order.class)  
public class OrderInOutRequisitionDelegate extends ModelEventDelegate<X_C_Order> {  
  
    private static final CLogger log = CLogger.getCLogger(OrderInOutRequisitionDelegate.class);  
  
    public OrderInOutRequisitionDelegate(X_C_Order po, Event event) {  
        super(po, event);  
    }  
  
    @AfterComplete  
    public void onAfterComplete() {  
        MOrder order = (MOrder) getModel();  
        OrderInOutRequisitionService.createFromOrder(order, order.get_TrxName());  
    }
  
    @AfterVoid  
    @AfterReactivate  
    public void onAfterVoidOrReactivate() {  
        MOrder order = (MOrder) getModel();  
        OrderInOutRequisitionService.voidByOrder(order, order.get_TrxName());  
    }
}