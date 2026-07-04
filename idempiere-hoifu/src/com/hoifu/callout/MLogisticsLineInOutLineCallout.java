package com.hoifu.callout;  
   
import java.util.Properties;  

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;  
import org.compiere.model.MOrderLine;  
import com.hoifu.model.MLogisticsLine;

  
@Callout(tableName = MLogisticsLine.Table_Name, columnName = { MLogisticsLine.COLUMNNAME_M_InOutLine_ID})  
public class MLogisticsLineInOutLineCallout implements IColumnCallout {  
	  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  
  
        if (value == null) return null;  
        int M_InOutLine_ID = (Integer) value;  
        if (M_InOutLine_ID == 0) return null;  
  
        MInOutLine iol = new MInOutLine(ctx, M_InOutLine_ID, null);  
        if (iol.get_ID() == 0) return null;  
  
        // 从发货单行直接取  
        mTab.setValue(MLogisticsLine.COLUMNNAME_M_Product_ID,  iol.getM_Product_ID()  > 0 ? iol.getM_Product_ID()  : null);  
        mTab.setValue(MLogisticsLine.COLUMNNAME_C_UOM_ID,      iol.getC_UOM_ID()      > 0 ? iol.getC_UOM_ID()      : null);  
        mTab.setValue(MLogisticsLine.COLUMNNAME_QtyDelivered,  iol.getMovementQty());  
  
        // 订单行 & 订单头  
        int C_OrderLine_ID = iol.getC_OrderLine_ID();  
        if (C_OrderLine_ID > 0) {  
            mTab.setValue(MLogisticsLine.COLUMNNAME_C_OrderLine_ID, C_OrderLine_ID);  
            MOrderLine ol = new MOrderLine(ctx, C_OrderLine_ID, null);  
            mTab.setValue(MLogisticsLine.COLUMNNAME_C_Order_ID, ol.getC_Order_ID() > 0 ? ol.getC_Order_ID() : null);  
        } else {  
            mTab.setValue(MLogisticsLine.COLUMNNAME_C_OrderLine_ID, null);  
            mTab.setValue(MLogisticsLine.COLUMNNAME_C_Order_ID,     null);  
        }  
  
        // 从父单据（发货单头）取业务伙伴 & 地址  
        MInOut inout = iol.getParent();  
        mTab.setValue(MLogisticsLine.COLUMNNAME_C_BPartner_ID,  
                inout.getC_BPartner_ID() > 0 ? inout.getC_BPartner_ID() : null);  
        mTab.setValue(MLogisticsLine.COLUMNNAME_C_BPartner_Location_ID,  
                inout.getC_BPartner_Location_ID() > 0 ? inout.getC_BPartner_Location_ID() : null);  
  
        return null;  
    }  
}