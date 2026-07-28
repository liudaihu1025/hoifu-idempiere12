package com.hoifu.process;  
  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;

import org.compiere.model.MInOutLine;
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
  
@org.adempiere.base.annotation.Process  
public class InOutLineBatchUpdateLocator extends SvrProcess {  
  
    @Override  
    protected void prepare() {  
        // 无需额外参数  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int updated = 0;  
      
        String sql =  
            "SELECT tw.T_SELECTION_ID, tw.VALUE_NUMBER " +  
            "FROM T_Selection_InfoWindow tw " +  
            "WHERE tw.AD_PINSTANCE_ID = ? " +  
            "  AND tw.COLUMNNAME = 'IntendedLocation_ID' " +  
            "  AND tw.VALUE_NUMBER IS NOT NULL " +  
            "  AND tw.T_SELECTION_ID IS NOT NULL";  
      
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
      
            while (rs.next()) {  
                int inOutLineId = rs.getInt(1);  
                java.math.BigDecimal intendedLocId = rs.getBigDecimal(2);  // 保持 BigDecimal  
              
                if (intendedLocId == null || intendedLocId.intValue() <= 0)  
                    continue;  
              
                MInOutLine line = new MInOutLine(getCtx(), inOutLineId, get_TrxName());  
                if (line.get_ID() == 0)  
                    continue;  
              
                line.setM_Locator_ID(intendedLocId.intValue());  
                line.set_ValueOfColumn("IntendedLocation_ID", intendedLocId);  // 传 BigDecimal  
                line.saveEx();  
                updated++;  
            }  
        } catch (SQLException e) {  
            throw new IllegalArgumentException("查询失败: " + e.getMessage(), e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
      
        if (updated == 0)  
            return "@NoRecordSelected@";  
      
        return "@Updated@ = " + updated;  
    }
}