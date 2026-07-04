package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Msg;

public class MInOutNoticeLine extends X_M_InOutNoticeLine {

	public MInOutNoticeLine(Properties ctx, int X_M_InOutNoticeLine_ID, String trxName) {
		super(ctx, X_M_InOutNoticeLine_ID, trxName);
	}

	public MInOutNoticeLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
    @Override  
    protected boolean beforeSave(boolean newRecord) {  
        if (!validateUniqueOrderLine(newRecord)) {  
            return false;  
        }  
        return true;  
    }  
  
    /**  
     * 校验同一 M_InOutNotice 下 C_OrderLine_ID 不能重复  
     */  
    private boolean validateUniqueOrderLine(boolean newRecord) {  
        if (getC_OrderLine_ID() <= 0) {  
            return true;  
        }  
        if (!newRecord && !is_ValueChanged(COLUMNNAME_C_OrderLine_ID)) {  
            return true;  
        }  
        int cnt = DB.getSQLValue(  
                get_TrxName(),  
                "SELECT COUNT(*) FROM M_InOutNoticeLine"  
                + " WHERE M_InOutNotice_ID=?"  
                + " AND C_OrderLine_ID=?"  
                + " AND M_InOutNoticeLine_ID!=?",  
                getM_InOutNotice_ID(),  
                getC_OrderLine_ID(),  
                getM_InOutNoticeLine_ID());  
        if (cnt > 0) {  
            log.saveError("SaveError",  
                    Msg.parseTranslation(getCtx(), "当前订单明细行已存在。"));  
            return false;  
        }  
        return true;  
    }  
}
