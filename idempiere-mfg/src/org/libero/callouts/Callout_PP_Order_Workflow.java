package org.libero.callouts;  
  
import java.math.BigDecimal;  
import java.util.List;  
import java.util.Properties;  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.CalloutEngine;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MTable;  
import org.compiere.model.PO;  
import org.compiere.model.Query;  
import org.libero.model.MPPOrderBOMLine;  
  
public class Callout_PP_Order_Workflow extends CalloutEngine implements IColumnCallout {  
      
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  
          
        if (mField.getColumnName().equals("QtyBatchSize")) {  
            return updateNodeQtyRequired(ctx, WindowNo, mTab, mField, value);  
        }  
        return null;  
    }  
      
    private String updateNodeQtyRequired(Properties ctx, int WindowNo, GridTab mTab,  
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
          
        // 获取当前QtyBatchSize值  
        BigDecimal qtyBatchSize = (BigDecimal) value;  
        if (qtyBatchSize == null) {  
            qtyBatchSize = BigDecimal.ZERO;  
        }  
          
        // 查询所有PP_Order_Node记录  
        String nodeWhereClause = "PP_Order_ID=?";  
        List<PO> orderNodes = new Query(ctx, "PP_Order_Node", nodeWhereClause, null)  
                .setParameters(mTab.getValue("PP_Order_ID"))  
                .list();  
          
        // 更新每个节点的QtyRequired  
        for (PO node : orderNodes) {  
            BigDecimal calculatedQty = keyMatQtyRequiered;  
              
            // 获取AD_Routing_Node_ID  
            Integer AD_Routing_Node_ID = (Integer) node.get_Value("AD_Routing_Node_ID");  
              
            if (AD_Routing_Node_ID != null && AD_Routing_Node_ID > 0) {  
                // 查询AD_Routing_Node表  
                String routingWhereClause = "AD_Routing_Node_ID=? AND IsBatchCalculation='Y' and IsActive ='Y'";  
                MTable routingTable = MTable.get(ctx, "AD_Routing_Node");  
                PO routingNode = new Query(ctx, routingTable, routingWhereClause, null)  
                        .setParameters(AD_Routing_Node_ID)  
                        .firstOnly();  
                  
                if (routingNode != null) {  
                    // 需要批量计算，乘以QtyBatchSize  
                    calculatedQty = calculatedQty.multiply(qtyBatchSize);  
                }  
            }  
              
            // 更新QtyRequiered字段  
            node.set_ValueOfColumn("QtyRequiered", calculatedQty);  
            node.saveEx();  
        }  
          
        return "";  
    }  
}