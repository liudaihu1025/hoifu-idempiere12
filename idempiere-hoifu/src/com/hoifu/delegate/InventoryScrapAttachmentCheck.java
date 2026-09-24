package com.hoifu.delegate;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.BeforePrepare;  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MAttachment;  
import org.compiere.model.MDocType;  
import org.compiere.model.MInventory;  
import org.osgi.service.event.Event;  
  
/**  
 * 报废单（M_Inventory）完成前（DocStatus=DR -> IP 之前）校验：  
 * 必须已上传附件，否则不允许提交。冲销单跳过校验。  
 */  
@EventTopicDelegate  
@ModelEventTopic(modelClass = MInventory.class)  
public class InventoryScrapAttachmentCheck extends ModelEventDelegate<MInventory> {  
  
    // 报废单单据类型 UUID  
    private static final String SCRAP_DOCTYPE_UU = "3c69e54b-296d-4799-b3c5-27137da3ff1c";  
  
    public InventoryScrapAttachmentCheck(MInventory po, Event event) {  
        super(po, event);  
    }  
  
    @BeforePrepare  
    public void onBeforePrepare() {  
        MInventory inventory = getModel();  
  
        // 仅对报废单单据类型生效  
        MDocType docType = MDocType.get(inventory.getCtx(), inventory.getC_DocType_ID());  
        if (docType == null || !SCRAP_DOCTYPE_UU.equals(docType.getC_DocType_UU())) {  
            return;  
        }  
  
        // 冲销单（系统自动生成）跳过附件校验  
        if (isReversalDocument(inventory)) {  
            return;  
        }  
  
        // 检查是否已有附件  
        MAttachment attachment = inventory.getAttachment(false);  
        if (attachment == null || attachment.getEntryCount() == 0) {  
            throw new AdempiereException("请先上传附件后再提交报废单");  
        }  
    }  
  
    /**  
     * 判断是否为反冲单（counter-document），使用持久化字段 Reversal_ID 跨 PO 实例可靠。  
     *  
     * <p>iDempiere 的 reverse() 逻辑：  
     * <ul>  
     *   <li>反冲单的 Reversal_ID 指向原单（较小 ID）→ {@code Reversal_ID > 0 && Reversal_ID < ID}</li>  
     *   <li>原单的 Reversal_ID 指向反冲单（较大 ID）→ {@code Reversal_ID > ID}</li>  
     * </ul>  
     */  
    private boolean isReversalDocument(MInventory doc) {  
        if (doc.getReversal_ID() <= 0)  
            return false;  
        return doc.getReversal_ID() < doc.get_ID();  
    }  
}