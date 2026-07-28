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
          

        return null;  
    }  
      

}