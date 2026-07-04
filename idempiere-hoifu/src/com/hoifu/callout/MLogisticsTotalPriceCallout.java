package com.hoifu.callout;  
  
import java.math.BigDecimal;  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.util.Env;

import com.hoifu.model.MLogistics;  
  
@Callout(tableName = MLogistics.Table_Name, columnName = {MLogistics.COLUMNNAME_FreightCharges, MLogistics.COLUMNNAME_Surcharges})  
public class MLogisticsTotalPriceCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab,  
            GridField mField, Object value, Object oldValue) {  
  
        // 从 mTab 取当前值（包含用户刚输入的新值）  
        BigDecimal freight   = (BigDecimal) mTab.getValue(MLogistics.COLUMNNAME_FreightCharges);  
        BigDecimal surcharge = (BigDecimal) mTab.getValue(MLogistics.COLUMNNAME_Surcharges);  
  
        if (freight   == null) freight   = Env.ZERO;  
        if (surcharge == null) surcharge = Env.ZERO;  
  
        mTab.setValue(MLogistics.COLUMNNAME_TotalPrice, freight.add(surcharge));  
  
        return null;  
    }  
}