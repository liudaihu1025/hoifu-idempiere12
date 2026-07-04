package com.hoifu.callout;  
  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MOrder;  
  
import com.hoifu.model.MInOutNotice;  
  
@Callout(tableName = MInOutNotice.Table_Name, columnName = { MInOutNotice.COLUMNNAME_C_Order_ID })  
public class InOutNoticeCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField,  
                        Object value, Object oldValue) {  
  
        Integer C_Order_ID = (Integer) value;  
        if (C_Order_ID == null || C_Order_ID.intValue() == 0) {  
            return "";  
        }  
  
        MOrder order = new MOrder(ctx, C_Order_ID.intValue(), null);  
        if (order.get_ID() != 0) {  
            mTab.setValue(MInOutNotice.COLUMNNAME_C_BPartner_ID,  
                          Integer.valueOf(order.getC_BPartner_ID()));  
        }  
  
        return "";  
    }  
}