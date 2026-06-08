package org.libero.process;   
import java.sql.Timestamp;
import java.util.List;  
import java.util.stream.Collectors;  
import org.libero.model.MPPOrder;  
import org.compiere.process.DocAction;  
import org.compiere.process.SvrProcess;  
  
/**  
 * 工单完工流程  
 * 校验工单状态为已暂停(Paused)或执行中(InProgress)，然后将其变更为已完成(Completed)  
 */  
@org.adempiere.base.annotation.Process  
public class PPOrderFinishWorkProcess extends SvrProcess {  
  
    /**  
     * 准备方法 - 无参数  
     */  
    protected void prepare() {  
        // 此流程没有参数  
    }  
  
    /**  
     * 执行流程  
     */  
    protected String doIt() throws Exception {  
        // 获取当前记录ID或选中的记录ID列表  
        List<Integer> orderIds = getRecord_IDs();  
        if (orderIds == null || orderIds.isEmpty()) {  
            // 如果没有多选记录，尝试获取单个记录  
            int recordId = getRecord_ID();  
            if (recordId > 0) {  
                orderIds = List.of(recordId);  
            }  
        }  
  
        if (orderIds.isEmpty()) {  
            throw new IllegalArgumentException("请选择要完工的工单");  
        }  
  
        // 校验所有选中的工单状态  
        validateOrderStatus(orderIds);  
  
        // 更新工单状态  
        int updatedCount = updateOrders(orderIds);  
  
        return "成功完工 " + updatedCount + " 个工单";  
    }  
  
    private void validateOrderStatus(List<Integer> orderIds) throws Exception {  
        List<String> invalidOrders = orderIds.stream()  
            .map(orderId -> {  
                MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
                if (order.get_ID() > 0) {  
                    String orderStatus = (String) order.get_Value("Orderstatus");  
                    if (!"Paused".equals(orderStatus) && !"InProgress".equals(orderStatus)) {  
                        return order.getDocumentNo();  
                    }  
                }  
                return null;  
            })  
            .filter(docNo -> docNo != null)  
            .collect(Collectors.toList());  
  
        if (!invalidOrders.isEmpty()) {  
            throw new IllegalArgumentException(  
                "您选择的工单中包含以下\"非执行中或已暂停\"状态的条目：" + String.join(", ", invalidOrders) +   
                "。只有状态为【执行中】或【已暂停】的工单才允许完工。");  
        }  
    }  
  
    private int updateOrders(List<Integer> orderIds) {  
        int count = 0;  
        for (Integer orderId : orderIds) {  
            MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
            if (order.get_ID() > 0) {  
                String orderStatus = (String) order.get_Value("Orderstatus");  
                if ("Paused".equals(orderStatus) || "InProgress".equals(orderStatus)) {  
                    // 更新工单状态为已完成  
                    order.set_ValueOfColumn("Orderstatus", "Completed");  
                    // 设置完工时间为当前时间    
                    order.setDateFinish(new Timestamp(System.currentTimeMillis()));  
                    order.saveEx();  
                    count++;  
                }  
            }  
        }  
        return count;  
    }  
}