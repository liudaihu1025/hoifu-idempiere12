package com.hoifu.event.processor;  
  
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.PO;  
  
public class COrderEventProcessor implements IEventProcessor {  
  
    private static final String COLUMNNAME_C_Tax_ID = "C_Tax_ID";  
    // 委外订单 UUID
    private static final String SUBCONTRACT_DOCTYPE_UU = "5ae935d6-5237-4d93-ac5b-4967cf9be287";  
    
    @Override  
    public boolean supports(PO po, String topic) {  
        return po instanceof MOrder;  
    }  
  
    @Override  
    public void process(PO po, String topic) {  
        MOrder order = (MOrder) po;  
        syncTaxToLines(order, topic);
        inheritDocumentNoFromSubcontractSource(order, topic);
    }  
  
    /**  
     * 若 C_Order.C_Tax_ID 发生变化，则将新值同步到所有 C_OrderLine  
     */  
    void syncTaxToLines(MOrder order, String topic) {  
        // 仅在 AFTER_CHANGE 且 C_Tax_ID 确实变化时触发  
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic)) {  
            return;  
        }  
        if (!order.is_ValueChanged(COLUMNNAME_C_Tax_ID)) {  
            return;  
        }  
  
        int newTaxId = order.get_ValueAsInt(COLUMNNAME_C_Tax_ID);  
        if (newTaxId <= 0) {  
            return;  
        }  
  
        MOrderLine[] lines = order.getLines();  
        for (MOrderLine line : lines) {  
            line.setC_Tax_ID(newTaxId);  
            line.saveEx();  
        }  
    }
    
    /**  
     * 若订单是通过反向单据（counter document）生成的，  
     * 且源订单的单据类型是委外订单，则直接使用源订单的单号。  
     *  
     * 判断依据：Ref_Order_ID > 0 表示该订单是由源订单生成的对应单据。  
     */  
    void inheritDocumentNoFromSubcontractSource(MOrder order, String topic) {  
        // 仅在新建前触发（此时 Ref_Order_ID 已在内存中设置，DocumentNo 可被覆盖）  
        if (!IEventTopics.PO_BEFORE_NEW.equals(topic)) {  
            return;  
        }  
        // Ref_Order_ID > 0 才是反向单据  
        int refOrderId = order.getRef_Order_ID();  
        if (refOrderId <= 0) {  
            return;  
        }  
        // 加载源订单  
        MOrder sourceOrder = new MOrder(order.getCtx(), refOrderId, order.get_TrxName());  
        if (sourceOrder.getC_Order_ID() <= 0) {  
            return;  
        }  
        // 判断源订单的单据类型是否是委外订单  
        MDocType sourceDocType = MDocType.get(order.getCtx(), sourceOrder.getC_DocType_ID());  
        if (sourceDocType == null) {  
            return;  
        }  
        if (!SUBCONTRACT_DOCTYPE_UU.equals(sourceDocType.getC_DocType_UU())) {  
            return;  
        }  
        // 直接使用源订单的单号  
        order.setDocumentNo(sourceOrder.getDocumentNo());  
    }  
  
}