package com.hoifu.callout;  
  
import java.math.BigDecimal;  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MOrderLine;  
  
import com.hoifu.model.MInOutNoticeLine;  
  
@Callout(tableName = MInOutNoticeLine.Table_Name, columnName = { MInOutNoticeLine.COLUMNNAME_C_OrderLine_ID })  
public class InOutNoticeLineCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField,  
                        Object value, Object oldValue) {  
  
        Integer C_OrderLine_ID = (Integer) value;  
        if (C_OrderLine_ID == null || C_OrderLine_ID.intValue() == 0) {  
            return "";  
        }  
  
        MOrderLine ol = new MOrderLine(ctx, C_OrderLine_ID.intValue(), null);  
        if (ol.get_ID() != 0) {  
            // 产品
            mTab.setValue(MInOutNoticeLine.COLUMNNAME_M_Product_ID, Integer.valueOf(ol.getM_Product_ID()));  

            // 计量单位  
            mTab.setValue(MInOutNoticeLine.COLUMNNAME_C_UOM_ID, Integer.valueOf(ol.getC_UOM_ID()));  
            // 数量：已订购 - 已发货  
            BigDecimal qty = ol.getQtyOrdered().subtract(ol.getQtyDelivered());  
            mTab.setValue(MInOutNoticeLine.COLUMNNAME_QtyEntered, qty);
        }  
  
        return "";  
    }  
}