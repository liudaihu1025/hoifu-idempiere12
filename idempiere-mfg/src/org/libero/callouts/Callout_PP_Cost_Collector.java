package org.libero.callouts;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.model.GridTabWrapper;
import org.adempiere.model.engines.StorageEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPCostCollector;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderNode;
import org.libero.tables.I_PP_Cost_Collector;
import org.libero.tables.X_PP_Cost_Collector; 

public class Callout_PP_Cost_Collector extends CalloutCostCollector implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("PP_Order_ID"))  
			return order(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("PP_Order_Node_ID"))
			return node(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("AD_User_ID"))
			return user(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("DateStart") || mField.getColumnName().equals("DateFinish"))
			return updateDurationRealFromDates(ctx, WindowNo, mTab, mField,value);
		return null;
	}
	
	/**  
     * 生产工单字段变化时的处理逻辑  
     * 根据CostCollectorType区分生产入库和生产报工  
     */  
    @Override  
    public String order(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {  
        Integer PP_Order_ID = (Integer)value;  
        if (PP_Order_ID == null || PP_Order_ID <= 0)  
            return "";  
          
        I_PP_Cost_Collector cc = GridTabWrapper.create(mTab, I_PP_Cost_Collector.class);  
        MPPOrder pp_order = new MPPOrder(ctx, PP_Order_ID, null);  
        MPPCostCollector.setPP_Order(cc, pp_order);  
          
        // 生产入库(CostCollectorType=100)时自动带出库位  
        String costCollectorType = cc.getCostCollectorType();  
        if (X_PP_Cost_Collector.COSTCOLLECTORTYPE_MaterialReceipt.equals(costCollectorType)) {  
        	//清除自动带出的入库数量
        	cc.setMovementQty(null); 
        	
            int locatorId = DB.getSQLValue(null,  
	                "SELECT get_recommended_locator(?, ?)",  
	                pp_order.getM_Product_ID(), pp_order.getM_Warehouse_ID());  
            if (locatorId > 0) {  
                cc.setM_Locator_ID(locatorId);  
            }
        } 
        // 生产报工(CostCollectorType=160)时清除自动带出的数量（调整为0）  
        else if (X_PP_Cost_Collector.COSTCOLLECTORTYPE_ActivityControl.equals(costCollectorType)) {  
            cc.setMovementQty(Env.ZERO);
        }
          
        return "";  
    }  
  
    /**  
     * 工序字段变化时的处理逻辑  
     * 根据CostCollectorType区分生产入库和生产报工  
     */  
    @Override  
    public String node(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {  
        Integer PP_Order_Node_ID = (Integer)value;  
        if (PP_Order_Node_ID == null || PP_Order_Node_ID <= 0)  
            return "";  
          
        I_PP_Cost_Collector cc = GridTabWrapper.create(mTab, I_PP_Cost_Collector.class);  
        MPPOrderNode node = MPPOrderNode.get(ctx, PP_Order_Node_ID, null);
        cc.setS_Resource_ID(node.getS_Resource_ID());  
        cc.setIsSubcontracting(node.isSubcontracting());  
          
        // 根据CostCollectorType区分处理逻辑  
        String costCollectorType = cc.getCostCollectorType();  
          
        if (X_PP_Cost_Collector.COSTCOLLECTORTYPE_ActivityControl.equals(costCollectorType)) {  
            // 生产报工：不自动带出报工数量，自动带出需求数量   
            mTab.setValue("qtyrequiered", node.getQtyRequiered());
        } 
          
        //duration(ctx, WindowNo, mTab, mField, value);  
        return "";  
    }  
    
    
    
    public String updateDurationRealFromDates(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)  
    {  
        // 获取实际的开始和完成时间字段  
        Timestamp startDate = (Timestamp)mTab.getValue("DateStart");  
        Timestamp finishDate = (Timestamp)mTab.getValue("DateFinish");  
          
        Integer PP_Cost_Collector_ID = (Integer)mTab.getValue("PP_Cost_Collector_ID");  
        if (PP_Cost_Collector_ID == null || PP_Cost_Collector_ID <= 0)  
            return "";  
          
        MPPCostCollector cc = new MPPCostCollector(ctx, PP_Cost_Collector_ID, null);  
        BigDecimal durationReal = cc.updateDurationRealFromDates(startDate, finishDate);  
        // 同步更新界面上的DurationReal字段  
        mTab.setValue("DurationReal", durationReal);
          
        return "";  
    }

}
