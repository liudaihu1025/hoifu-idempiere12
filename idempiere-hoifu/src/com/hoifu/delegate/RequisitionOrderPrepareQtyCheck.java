package com.hoifu.delegate;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.BeforePrepare;  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MRequisition;  
import org.compiere.model.MRequisitionLine;  
import org.osgi.service.event.Event;  
  
@EventTopicDelegate  
@ModelEventTopic(modelClass = MRequisition.class)  
public class RequisitionOrderPrepareQtyCheck extends ModelEventDelegate<MRequisition> {  
  
    public RequisitionOrderPrepareQtyCheck(MRequisition po, Event event) {  
        super(po, event);  
    }  
  
    @BeforePrepare  
    public void onBeforePrepare() {  
        MRequisition requisition = getModel();  
  
        MRequisitionLine[] lines = requisition.getLines();  
        for (MRequisitionLine line : lines) {  
            // 检查采购数量 <= 0  
            if (line.getQty().signum() <= 0) {  
                throw new AdempiereException("@Line@ " + line.getLine()  
                        + ": @Qty@ " + line.getQty() + " （需要大于0）");  
            }  
        }  
    }  
}