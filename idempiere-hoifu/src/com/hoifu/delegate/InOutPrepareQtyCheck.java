package com.hoifu.delegate;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.BeforePrepare;  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MDocType;  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
  
import org.osgi.service.event.Event;  
  
@EventTopicDelegate  
@ModelEventTopic(modelClass = MInOut.class)  
public class InOutPrepareQtyCheck extends ModelEventDelegate<MInOut> {  
  
    public InOutPrepareQtyCheck(MInOut po, Event event) {  
        super(po, event);  
    }  
  
    @BeforePrepare  
    public void onBeforePrepare() {  
        MInOut inout = getModel();  
  
        // 冲销单的 QtyEntered 为负，跳过校验  
        if (inout.isReversal())  
            return;  
  
        // 读取单据类型上的自定义字段 DocMovementType  
        MDocType dt = MDocType.get(inout.getCtx(), inout.getC_DocType_ID());  
        String docMovementType = dt.get_ValueAsString("DocMovementType");  
  
        // 仅对其他出库单(O-)和其他入库单(O+)执行校验  
        if (!"O-".equals(docMovementType) && !"O+".equals(docMovementType))  
            return;  
  
        // 逐行检查：非描述行的 QtyEntered 必须 > 0  
        for (MInOutLine line : inout.getLines(false)) {  
            if (line.isDescription())  
                continue;  
            if (line.getQtyEntered().signum() <= 0) {  
                throw new AdempiereException(  
                    "@Line@ " + line.getLine() + ":数量必须大于等于0" );  
            }  
        }  
    }  
}