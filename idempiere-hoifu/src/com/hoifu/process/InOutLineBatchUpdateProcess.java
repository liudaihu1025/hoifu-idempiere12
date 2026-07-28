package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.util.HashMap;  
import java.util.Map;  
  
import org.compiere.model.MInOutLine;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
  
@org.adempiere.base.annotation.Process  
public class InOutLineBatchUpdateProcess extends SvrProcess {  
  
    @Override  
    protected void prepare() {  
        // 数据来自 T_Selection_InfoWindow，无需额外参数  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        // T_SELECTION_ID = M_InOutLine_ID  
        // IntendedLocation_ID 和 QtyEntered 都存在 VALUE_NUMBER 列  
        String sql = "SELECT T_SELECTION_ID, COLUMNNAME, VALUE_NUMBER "  
                + "FROM T_Selection_InfoWindow "  
                + "WHERE AD_PInstance_ID = ? "  
                + "AND COLUMNNAME IN ('IntendedLocation_ID', 'QtyEntered')";  
  
        // Map<M_InOutLine_ID, Map<ColumnName, Value>>  
        Map<Integer, Map<String, BigDecimal>> lineDataMap = new HashMap<>();  
  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
            while (rs.next()) {  
                int lineId = rs.getInt(1);  
                String columnName = rs.getString(2);  
                BigDecimal value = rs.getBigDecimal(3);  
                lineDataMap.computeIfAbsent(lineId, k -> new HashMap<>())  
                           .put(columnName, value);  
            }  
        } catch (SQLException e) {  
            throw new IllegalArgumentException("读取选中行数据失败: " + e.getMessage());  
        } finally {  
            DB.close(rs, pstmt);  
        }  
  
        int updated = 0;  
        for (Map.Entry<Integer, Map<String, BigDecimal>> entry : lineDataMap.entrySet()) {  
            int mInOutLineId = entry.getKey();  
            Map<String, BigDecimal> fields = entry.getValue();  
  
            BigDecimal intendedLocationId = fields.get("IntendedLocation_ID");  
            BigDecimal qtyEntered = fields.get("QtyEntered");  
  
            if (intendedLocationId == null && qtyEntered == null)  
                continue;  
  
            MInOutLine line = new MInOutLine(getCtx(), mInOutLineId, get_TrxName());  
            if (line.get_ID() == 0)  
                continue;  
  
            if (intendedLocationId != null && intendedLocationId.intValue() > 0)  
                line.setM_Locator_ID(intendedLocationId.intValue());  
  
            if (qtyEntered != null)  
                line.setQty(qtyEntered);  
  
            line.saveEx();  
            updated++;  
        }  
  
        return "@Updated@ = " + updated;  
    }  
}