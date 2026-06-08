package com.hoifu.process;  
  
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MInventoryLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;  

@org.adempiere.base.annotation.Process  
public class OfficeRequisitionEndClaimProcess extends SvrProcess {  
    private List<Integer> selectedLineIds = new ArrayList<>();  

    protected void prepare() {  
		// 加载选中的记录
		loadSelectedLines();
	}

	protected String doIt() throws Exception {
		if (selectedLineIds.isEmpty()) {
			return "请先选择要结束领用的记录";
		}

		int updatedCount = 0;
		for (Integer lineId : selectedLineIds) {
			MInventoryLine line = new MInventoryLine(getCtx(), lineId, get_TrxName());

			// 直接更新状态为"已领取"
			line.set_CustomColumn("ClaimStatus", "YL");
			if (line.save()) {
				updatedCount++;
			}
		}

		return "成功结束 " + updatedCount + " 条记录的领用";
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
            throw new IllegalArgumentException("获取领用单明细失败");  
        } finally {  
            DB.close(rs, pstmt);  
        }  
    }  
}