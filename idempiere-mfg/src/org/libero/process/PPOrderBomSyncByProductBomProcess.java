package org.libero.process;  
import java.util.List;  
import org.libero.model.MPPOrder;  
import org.eevolution.model.MPPProductBOM;  
import org.compiere.process.DocAction;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
  
/**  
 * 从产品BOM窗口同步相关工单的BOM信息  
 */  
@org.adempiere.base.annotation.Process  
public class PPOrderBomSyncByProductBomProcess extends SvrProcess {  
      
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
		// 获取当前窗口选中的产品BOM记录ID
		List<Integer> productBomIds = getRecord_IDs();
		if (productBomIds == null || productBomIds.isEmpty()) {
			int recordId = getRecord_ID();
			if (recordId > 0) {
				productBomIds = List.of(recordId);
			}
		}

		if (productBomIds.isEmpty()) {
			throw new IllegalArgumentException("请选择要同步的产品BOM");
		}

		if (p_PP_Order_ID <= 0) {
			throw new IllegalArgumentException("请指定要同步BOM的工单");
		}

		// 验证指定工单是否使用了选中的产品BOM
		validateOrderUsesProductBOM(p_PP_Order_ID, productBomIds);
		validateOrderAndBOMStatus(p_PP_Order_ID);
		int updatedCount = updateOrders(List.of(p_PP_Order_ID));
        MPPOrder order = new MPPOrder(getCtx(), p_PP_Order_ID, get_TrxName());  
		return "成功更新工单【" + order.getDocumentNo() + " 】的BOM信息";

	}

    /**  
     * 验证指定工单是否使用了选中的产品BOM  
     */  
    private void validateOrderUsesProductBOM(int orderId, List<Integer> productBomIds) throws Exception {  
        MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
        if (order.get_ID() <= 0) {  
            throw new IllegalArgumentException("工单不存在: " + orderId);  
        }  
          
        if (!productBomIds.contains(order.getPP_Product_BOM_ID())) {  
            throw new IllegalArgumentException(  
                "工单 " + order.getDocumentNo() + " 没有使用选中的产品BOM");  
        }  
    }  
       
      
    /**  
     * 验证工单状态和BOM状态  
     */  
    private void validateOrderAndBOMStatus(int orderId) throws Exception {  
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
          
        // 验证BOM状态  
        int productBomId = order.getPP_Product_BOM_ID();  
        if (productBomId <= 0) {  
            throw new IllegalArgumentException("工单 " + order.getDocumentNo() + " 没有关联的BOM");  
        }  
          
        MPPProductBOM productBOM = MPPProductBOM.get(getCtx(), productBomId);  
        if (productBOM == null) {  
            throw new IllegalArgumentException("工单 " + order.getDocumentNo() + " 关联的BOM不存在");  
        }  
          
        String bomStatus = (String)productBOM.get_Value("bomstatus");  
        if (!"Released".equals(bomStatus)) {  
            throw new IllegalArgumentException(  
                "只有【已发布】状态的BOM才能同步");  
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
                order.resyncBOM();  
                order.saveEx(get_TrxName());  
                count++;  
                  
                log.info("Successfully resynced BOM for Order " + orderId);  
                  
            } catch (Exception e) {  
                log.severe("Failed to resync BOM for Order " + orderId + ": " + e.getMessage());  
                // 继续处理下一个工单  
            }  
        }  
          
        return count;  
    }  
}