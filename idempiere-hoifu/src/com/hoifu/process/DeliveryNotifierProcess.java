package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.util.concurrent.atomic.AtomicBoolean;  
import java.util.concurrent.atomic.AtomicReference;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MOrder;  
import org.compiere.model.MStorageOnHand;  
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;  
import org.compiere.util.Env;
import org.compiere.wf.MWorkflow;

import com.hoifu.model.MInOutNotice;  
import com.hoifu.model.MInOutNoticeLine;  
  
@org.adempiere.base.annotation.Process  
public class DeliveryNotifierProcess extends SvrProcess {  
  
    @Override  
    protected void prepare() {  
        // 无需额外参数  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int recordId = getRecord_ID();  
        if (recordId <= 0)  
            throw new AdempiereException("@NoRecord@");  
      
        MInOutNotice notice = new MInOutNotice(getCtx(), recordId, get_TrxName());  
      
        // 只允许草稿/无效状态执行  
        String docStatus = notice.getDocStatus();  
        if (!DocAction.STATUS_Drafted.equals(docStatus)  
                && !DocAction.STATUS_Invalid.equals(docStatus)  
                && !DocAction.STATUS_NotApproved.equals(docStatus)) {  
            throw new AdempiereException("单据状态不允许执行通知发货，当前状态：" + docStatus);  
        }  
      
        // 获取仓库 ID（从关联订单取）  
        MOrder order = new MOrder(getCtx(), notice.getC_Order_ID(), get_TrxName());  
        int warehouseId = order.getM_Warehouse_ID();  
      
        // 检查每条明细的库存  
        StringBuilder shortageMsg = new StringBuilder();  
        for (MInOutNoticeLine line : notice.getLines()) {  
            int productId = line.getM_Product_ID();  
            BigDecimal qtyNeeded = line.getQtyEntered();  
      
            BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHandForShipping(  
                    productId, warehouseId, 0, get_TrxName());  
      
            if (qtyOnHand.compareTo(qtyNeeded) < 0) {  
                String productName = line.getM_Product().getName();  
                shortageMsg.append(productName)  
                        .append("：可用库存 ").append(qtyOnHand.toPlainString())  
                        .append("，通知数量 ").append(qtyNeeded.toPlainString())  
                        .append("\n");  
            }  
        }  
      
        // 如果有库存不足，询问用户是否继续  
        if (shortageMsg.length() > 0) {  
            if (processUI == null) {  
                throw new AdempiereException("库存不足，无法在后台自动执行通知发货：\n" + shortageMsg);  
            }  
      
            String confirmMsg = "库存不足，是否继续通知？\n" + shortageMsg.toString();  
      
            AtomicBoolean confirmed = new AtomicBoolean(false);  
            AtomicBoolean answered = new AtomicBoolean(false);  
      
            processUI.ask(confirmMsg, result -> {  
                confirmed.set(result != null && result);  
                answered.set(true);  
            });  
      
            while (!answered.get()) {  
                try {  
                    Thread.sleep(200);  
                } catch (InterruptedException e) {  
                    Thread.currentThread().interrupt();  
                }  
            }  
      
            if (!confirmed.get()) {  
                return "用户取消，保持原有状态";  
            }  
        }  
      
        // 执行完成（走 AD 工作流）  
        ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(notice, DocAction.ACTION_Complete);  
        if (pi != null && pi.isError())  
            throw new AdempiereException("完成单据失败: " + pi.getSummary());  
      
        notice.load(get_TrxName());  
        return "发货通知单已提交：" + notice.getDocumentNo();  
    }
}