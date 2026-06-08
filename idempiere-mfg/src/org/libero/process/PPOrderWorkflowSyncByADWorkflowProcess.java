package org.libero.process;  
  
import java.util.List;  
import org.libero.model.MPPOrder;  
import org.compiere.wf.MWorkflow; 
import org.compiere.process.DocAction;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
  
/**  
 * 从AD工艺路线窗口同步相关工单的工艺路线信息  
 */  
@org.adempiere.base.annotation.Process  
public class PPOrderWorkflowSyncByADWorkflowProcess extends SvrProcess {  
      
    // 流程参数  
    private int p_PP_Order_ID = 0;  
      
    protected void prepare() {  
        for (ProcessInfoParameter para : getParameter()) {  
            String name = para.getParameterName();  
            if (para.getParameter() == null)  
                ;  
            else if (name.equals("PP_Order_ID"))  
                p_PP_Order_ID = para.getParameterAsInt();  
            else  
                log.log(java.util.logging.Level.SEVERE, "Unknown Parameter: " + name);  
        }  
    }  
      
    protected String doIt() throws Exception {  
        // 获取当前窗口选中的AD工艺路线记录ID  
        List<Integer> workflowIds = getRecord_IDs();  
        if (workflowIds == null || workflowIds.isEmpty()) {  
            int recordId = getRecord_ID();  
            if (recordId > 0) {  
                workflowIds = List.of(recordId);  
            }  
        }  
  
        if (workflowIds.isEmpty()) {  
            throw new IllegalArgumentException("请选择要同步的AD工艺路线");  
        }  
  
        if (p_PP_Order_ID <= 0) {  
            throw new IllegalArgumentException("请指定要同步工艺路线的工单");  
        }  
  
        // 验证指定工单是否使用了选中的AD工艺路线  
        validateOrderUsesWorkflow(p_PP_Order_ID, workflowIds);  
        validateOrderAndWorkflowStatus(p_PP_Order_ID);  
        updateOrders(List.of(p_PP_Order_ID));  
        MPPOrder order = new MPPOrder(getCtx(), p_PP_Order_ID, get_TrxName());  
        return "成功更新工单【" + order.getDocumentNo() + " 】的工艺路线信息";  
    }  
  
    /**  
     * 验证指定工单是否使用了选中的AD工艺路线  
     */  
    private void validateOrderUsesWorkflow(int orderId, List<Integer> workflowIds) throws Exception {  
        MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
        if (order.get_ID() <= 0) {  
            throw new IllegalArgumentException("工单不存在: " + orderId);  
        }  
          
        if (!workflowIds.contains(order.getAD_Workflow_ID())) {  
            throw new IllegalArgumentException(  
                "工单 " + order.getDocumentNo() + " 没有使用选中的AD工艺路线");  
        }  
    }  
      
    /**  
     * 验证工单状态和工艺路线状态  
     */  
    private void validateOrderAndWorkflowStatus(int orderId) throws Exception {  
        MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
        if (order.get_ID() <= 0) {  
            throw new IllegalArgumentException("工单不存在: " + orderId);  
        }  
          
        // 验证工单状态  
        String orderStatus = (String) order.get_Value("Orderstatus");  
        String docStatus = order.getDocStatus();  
        if (!"Ready".equals(orderStatus) || !DocAction.STATUS_Drafted.equals(docStatus)) {  
            throw new IllegalArgumentException(  
                "工单 " + order.getDocumentNo() + " 状态不符合要求。需要工单状态【待发布】");  
        }  
          
        // 验证工艺路线状态  
        int workflowId = order.getAD_Workflow_ID();  
        if (workflowId <= 0) {  
            throw new IllegalArgumentException("工单 " + order.getDocumentNo() + " 没有关联的工艺路线");  
        }  
          
        MWorkflow workflow = MWorkflow.get(getCtx(), workflowId);  
        if (workflow == null) {  
            throw new IllegalArgumentException("工单 " + order.getDocumentNo() + " 关联的工艺路线不存在");  
        }  
          
        String workflowStatus = workflow.getPublishStatus();  
        if (!"R".equals(workflowStatus)) {  
            throw new IllegalArgumentException(  
                "只有【已发布】状态的工艺路线才能同步");  
        }  
    }  
      
    private int updateOrders(List<Integer> orderIds) {  
        int count = 0;  
          
        for (Integer orderId : orderIds) {  
            if (orderId == null || orderId <= 0) {  
                continue;  
            }  
              
            MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
              
            try {  
                order.resyncWorkflow();  
                order.saveEx(get_TrxName());  
                count++;  
                  
                log.info("Successfully resynced Workflow for Order " + orderId);  
                  
            } catch (Exception e) {  
                log.severe("Failed to resync Workflow for Order " + orderId + ": " + e.getMessage());  
                // 继续处理下一个工单  
            }  
        }  
          
        return count;  
    }  
}