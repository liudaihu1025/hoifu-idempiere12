package com.hoifu.callout;  
  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.util.DB;  
  
@Callout(tableName = "C_PaymentRequest", columnName = {"C_BPartner_ID"})  
public class PaymentRequestBPartnerCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
        Integer C_BPartner_ID = (Integer) value;  
        if (C_BPartner_ID == null || C_BPartner_ID == 0) {  
            mTab.setValue("C_BPartner_Location_ID", null);  
            return null;  
        }  
  
        // 取 IsBillTo='Y' 的账单地址  
        int locID = DB.getSQLValue(null,  
            "SELECT MAX(C_BPartner_Location_ID) FROM C_BPartner_Location "  
            + "WHERE C_BPartner_ID=? AND IsBillTo='Y' AND IsActive='Y'",  
            C_BPartner_ID);  
  
        if (locID > 0)  
            mTab.setValue("C_BPartner_Location_ID", locID);  
        else  
            mTab.setValue("C_BPartner_Location_ID", null);  
  
        return null;  
    }  
}