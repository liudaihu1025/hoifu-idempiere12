package com.hoifu.process;  
  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.logging.Level;  
  
import org.compiere.model.MRequisitionLine;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
  
@org.adempiere.base.annotation.Process  
public class RequisitionLineCloseProcess extends SvrProcess {  
  
    private List<Integer> selectedLineIds = new ArrayList<>();  
  
    @Override  
    protected void prepare() {  
        // 本流程无需额外参数  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        loadSelectedLines();  
  
        int count = 0;  
        for (Integer lineId : selectedLineIds) {  
            MRequisitionLine line = new MRequisitionLine(getCtx(), lineId, get_TrxName());  
            if (line.get_ID() <= 0) {  
                if (log.isLoggable(Level.WARNING))  
                    log.warning("RequisitionLine not found: " + lineId);  
                continue;  
            }  
            if (!line.isActive()) {  
                continue; // 已经是失效状态，跳过  
            }  
            line.setIsActive(false);  
            line.saveEx();
            count++;  
        }  
  
        return "@Processed@ #" + count;  
    }  
  
    private void loadSelectedLines() {  
        String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
  
            while (rs.next()) {  
                selectedLineIds.add(rs.getInt(1));  
            }  
        } catch (SQLException e) {  
            throw new IllegalArgumentException("获取明细记录失败: " + e.getMessage());  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }  
}