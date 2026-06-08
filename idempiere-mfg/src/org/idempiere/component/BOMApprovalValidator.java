package org.idempiere.component;  
  
import java.util.List;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.eevolution.model.MPPProductBOM;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.osgi.service.event.Event;  
  
public class BOMApprovalValidator extends AbstractEventHandler {  
    private static CLogger log = CLogger.getCLogger(BOMApprovalValidator.class);  
      
    @Override  
    protected void initialize() {  
        // 监听产品BOM完成后事件  
        registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MPPProductBOM.Table_Name);  
        log.info("BOM Approval Validator initialized");  
    }  
      
    @Override  
    protected void doHandleEvent(Event event) {  
        PO po = getPO(event);  
        if (po instanceof MPPProductBOM) {  
            MPPProductBOM productBOM = (MPPProductBOM) po;  
              
            // 检查是否为已完成状态  
            if (DocAction.STATUS_Completed.equals(productBOM.getDocStatus())) {  
                handleProductBOMCompleted(productBOM);  
            }  
        }  
    }  
      
    private void handleProductBOMCompleted(MPPProductBOM productBOM) {  
        try {  
			log.info("ProductBOM completed - ID: " + productBOM.getPP_Product_BOM_ID() + ", Revision: "
					+ productBOM.getRevision() + ", BOMStatus: " + productBOM.getBOMStatus());

			// 重新加载产品BOM以确保获取最新数据
			productBOM.load(productBOM.get_TrxName());
			log.info("After reload - Revision: " + productBOM.getRevision());

			updateRelatedOrderBOM(productBOM);
        } catch (Exception e) {  
            log.severe("Failed to update order BOM: " + e.getMessage());  
        }  
	}
      
	private void updateRelatedOrderBOM(MPPProductBOM productBOM) {
		// 查询PP_Order_BOM表中非活跃且对应工单为草稿状态的记录
		String whereClause = "PP_Order_BOM.M_Product_ID=? AND PP_Order_BOM.IsActive=? AND PP_Order.DocStatus=?";
		List<MPPOrderBOM> orderBOMs = new Query(productBOM.getCtx(), MPPOrderBOM.Table_Name, whereClause,
				productBOM.get_TrxName())
				.addJoinClause("INNER JOIN PP_Order ON PP_Order_BOM.PP_Order_ID = PP_Order.PP_Order_ID")
				.setParameters(productBOM.getM_Product_ID(), false, MPPOrder.DOCSTATUS_Drafted).list();
          
		for (MPPOrderBOM orderBOM : orderBOMs) {
			// 更新工单BOM的状态和版本
			orderBOM.setBOMStatus(productBOM.getBOMStatus());
			orderBOM.setRevision(productBOM.getRevision());
              
			// 重新激活
			orderBOM.setIsActive(true);
			orderBOM.saveEx();
              
			log.info("Updated Order BOM ID: " + orderBOM.getPP_Order_BOM_ID() + " with Revision: "
					+ productBOM.getRevision());
		}
    }

}