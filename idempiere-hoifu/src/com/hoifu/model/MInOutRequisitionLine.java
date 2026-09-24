package com.hoifu.model;  
  
import java.sql.ResultSet;  
import java.util.Properties;  
  
public class MInOutRequisitionLine extends X_M_InOut_RequisitionLine {  
  
    public MInOutRequisitionLine(Properties ctx, int M_InOut_RequisitionLine_ID, String trxName) {  
        super(ctx, M_InOut_RequisitionLine_ID, trxName);  
    }  
  
    public MInOutRequisitionLine(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
    }  
  
    @Override  
    protected boolean beforeSave(boolean newRecord) {  
    	
        return true;  
    }  
}