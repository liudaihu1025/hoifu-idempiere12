package org.libero.callouts;  
  
import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB; // ← 新增 import  
import org.libero.model.MPPOrderBOMLine;
  
public class Callout_PP_Order_Node extends CalloutEngine implements IColumnCallout {  

    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  

        if (mField.getColumnName().equals("AD_Routing_Node_ID")) {  
            return updateQtyRequired(ctx, WindowNo, mTab, mField, value);  
        }  
        return null;  
    }  

    private String updateQtyRequired(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value) {  

        // 获取主物料  
        String whereClause = "PP_Order_ID=? AND KeyMat='Y'";  
        MPPOrderBOMLine keyMatLine = new Query(ctx, MPPOrderBOMLine.Table_Name, whereClause, null)  
                .setParameters(mTab.getValue("PP_Order_ID"))  
                .firstOnly();  
  
        if (keyMatLine == null) {  
            throw new AdempiereException("工单BOM未维护主物料");  
        }  

        BigDecimal keyMatQtyRequiered = keyMatLine.getQtyRequiered();  
        BigDecimal calculatedQty = keyMatQtyRequiered;  

        // 获取新的AD_Routing_Node_ID值  
        Integer AD_Routing_Node_ID = (Integer) value;  
  
        if (AD_Routing_Node_ID != null && AD_Routing_Node_ID > 0) {  
  
            // ↓↓↓ 新增：带出 Name ↓↓↓  
            String name = DB.getSQLValueString(null,  
                    "SELECT Name FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'",  
                    AD_Routing_Node_ID);  
            if (name != null) {  
                mTab.setValue("Name", name);  
            }  
			// ↑↑↑ 新增结束 ↑↑↑

			// 查询AD_Routing_Node表（仅批量计算节点）
			String routingWhereClause = "AD_Routing_Node_ID=? AND IsBatchCalculation='Y' and IsActive='Y'";
            MTable routingTable = MTable.get(ctx, "AD_Routing_Node");  
            PO routingNode = new Query(ctx, routingTable, routingWhereClause, null)  
                    .setParameters(AD_Routing_Node_ID)  
                    .firstOnly();  

            if (routingNode != null) {  
                // 需要批量计算，获取QtyBatchSize  
                Integer PP_Order_Workflow_ID = (Integer) mTab.getValue("PP_Order_Workflow_ID");  
                if (PP_Order_Workflow_ID != null && PP_Order_Workflow_ID > 0) {  
                    String workflowWhereClause = "PP_Order_Workflow_ID=?";  
                    PO workflow = new Query(ctx, "PP_Order_Workflow", workflowWhereClause, null)  
                            .setParameters(PP_Order_Workflow_ID)  
                            .firstOnly();  

                    if (workflow != null) {  
                        BigDecimal qtyBatchSize = (BigDecimal) workflow.get_Value("QtyBatchSize");  
                        if (qtyBatchSize != null && qtyBatchSize.compareTo(BigDecimal.ZERO) > 0) {  
                            calculatedQty = calculatedQty.multiply(qtyBatchSize);  
                        }  
                    }  
                }  
            }  
        }  

        // 更新当前节点的QtyRequiered字段  
        mTab.setValue("QtyRequiered", calculatedQty);  

        return "";  
    }  
}