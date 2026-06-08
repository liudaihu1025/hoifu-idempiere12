package org.libero.process;  
  
import java.util.List;  
import java.util.stream.Collectors;  
  
import org.libero.model.MPPOrder;  
import org.compiere.process.SvrProcess;  
  
/**  
 * 工单暂停流程  
 * 校验工单状态为执行中(InProgress)，然后将其变更为已暂停(Paused)  
 */  
@org.adempiere.base.annotation.Process  
public class PPOrderPauseProcess extends SvrProcess {  
  
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
            throw new IllegalArgumentException("请选择要暂停的工单");  
        }  
  
        // 校验所有选中的工单状态  
        validateOrderStatus(orderIds);  
  
        // 更新工单状态  
        int updatedCount = updateOrders(orderIds);  
  
        return "成功暂停 " + updatedCount + " 个工单";  
    }  
  
    private void validateOrderStatus(List<Integer> orderIds) throws Exception {  
        List<String> nonInProgressOrders = orderIds.stream()  
            .map(orderId -> {  
                MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
                if (order.get_ID() > 0) {  
                    String orderStatus = (String) order.get_Value("Orderstatus");  
                    if (!"InProgress".equals(orderStatus)) {  
                        return order.getDocumentNo();  
                    }  
                }  
                return null;  
            })  
            .filter(docNo -> docNo != null)  
            .collect(Collectors.toList());  
  
        if (!nonInProgressOrders.isEmpty()) {  
            throw new IllegalArgumentException(  
                "您选择的工单中包含以下\"非执行中\"状态的条目：" + String.join(", ", nonInProgressOrders) +   
                "。只有状态为【执行中】的工单才允许暂停。");  
        }  
    }  
  
    private int updateOrders(List<Integer> orderIds) {  
        int count = 0;  
        for (Integer orderId : orderIds) {  
            MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
            if (order.get_ID() > 0) {  
                String orderStatus = (String) order.get_Value("Orderstatus");  
                if ("InProgress".equals(orderStatus)) {  
                    // 更新工单状态为已暂停  
                    order.set_ValueOfColumn("Orderstatus", "Paused");  
                    order.saveEx();  
                    count++;  
                }  
            }  
        }  
        return count;  
    }  
}