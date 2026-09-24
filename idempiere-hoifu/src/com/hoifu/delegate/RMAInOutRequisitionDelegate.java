package com.hoifu.delegate;  
  

import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.AfterComplete;
import org.adempiere.base.event.annotations.doc.AfterReactivate;
import org.adempiere.base.event.annotations.doc.AfterVoid;
import org.compiere.model.MRMA;  
import org.compiere.model.X_M_RMA;  
import org.compiere.util.CLogger;  
import org.osgi.service.event.Event;

import com.hoifu.service.RMAInOutRequisitionService;  
  
/**  
 * C_Order（销售/采购订单）表头事件委托。  
 *  
 * <p>订单完成（DOC_AFTER_COMPLETE）后，按订单本身及其明细行，  
 * 自动生成一张 M_InOut_Requisition（出入库申请单，草稿状态）
 */  
@EventTopicDelegate  
@ModelEventTopic(modelClass = X_M_RMA.class)  
public class RMAInOutRequisitionDelegate extends ModelEventDelegate<X_M_RMA> {  
  
    private static final CLogger log = CLogger.getCLogger(RMAInOutRequisitionDelegate.class);  
  
    public RMAInOutRequisitionDelegate(X_M_RMA po, Event event) {  
        super(po, event);  
    }  
  
    @AfterComplete  
    public void onAfterComplete() {  
        MRMA rma = (MRMA) getModel();  
        RMAInOutRequisitionService.createFromRMA(rma, rma.get_TrxName());  
    }
  
    @AfterVoid  
    @AfterReactivate  
    public void onAfterVoidOrReactivate() {  
        MRMA rma = (MRMA) getModel();  
        RMAInOutRequisitionService.voidByRMA(rma, rma.get_TrxName());  
    }
}