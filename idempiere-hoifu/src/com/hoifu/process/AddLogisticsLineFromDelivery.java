package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.util.LinkedHashSet;  
import java.util.List;  
import java.util.Set;  
  
import org.adempiere.base.annotation.Parameter;  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.Query;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Msg;  
  
import com.hoifu.model.MLogistics;  
import com.hoifu.model.MLogisticsLine;  
  
@org.adempiere.base.annotation.Process  
public class AddLogisticsLineFromDelivery extends SvrProcess {  
  
    /** 流程参数：当前物流单 ID，AD 中默认值设为 @M_Logistics_ID@ */  
    @Parameter(name = MLogistics.COLUMNNAME_M_Logistics_ID)  
    private int p_M_Logistics_ID;  
  
    @Override  
    protected void prepare() {  
        // @Parameter 自动注入，无需手动解析  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
  
        // 1. 校验参数  
        if (p_M_Logistics_ID <= 0)  
            throw new IllegalArgumentException("请选择物流单 M_Logistics_ID");  
  
        // 2. 加载已有物流单头（不创建新的）  
        MLogistics logistics = new MLogistics(getCtx(), p_M_Logistics_ID, get_TrxName());  
        if (logistics.get_ID() <= 0)  
            throw new IllegalArgumentException("找不到物流单，ID=" + p_M_Logistics_ID);  
  
        // 3. 从 T_Selection 取选中的 M_InOutLine_ID，  
        //    JOIN 找出这些行所属的所有不重复 M_InOut_ID  
        Set<Integer> inOutIds = new LinkedHashSet<>();  
        String sql = "SELECT DISTINCT iol.M_InOut_ID "  
                + "FROM M_InOutLine iol "  
                + "INNER JOIN T_Selection ts ON ts.T_Selection_ID = iol.M_InOutLine_ID "  
                + "WHERE ts.AD_PInstance_ID = ?";  
  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, getAD_PInstance_ID());  
            rs = pstmt.executeQuery();  
            while (rs.next())  
                inOutIds.add(rs.getInt(1));  
        } catch (SQLException e) {  
            throw new IllegalArgumentException("查询发货单失败：" + e.getLocalizedMessage(), e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
  
        if (inOutIds.isEmpty())  
            return "未选择任何发货明细记录";  
  
        // 4. 查出该物流单已存在的 M_InOut_ID（用于去重过滤）  
        Set<Integer> existingInOutIds = new LinkedHashSet<>();  
        String existSql = "SELECT DISTINCT M_InOut_ID FROM M_LogisticsLine "  
                + "WHERE M_Logistics_ID = ? AND IsActive = 'Y'";  
        try {  
            pstmt = DB.prepareStatement(existSql, get_TrxName());  
            pstmt.setInt(1, p_M_Logistics_ID);  
            rs = pstmt.executeQuery();  
            while (rs.next())  
                existingInOutIds.add(rs.getInt(1));  
        } catch (SQLException e) {  
            throw new IllegalArgumentException("查询已有物流明细失败：" + e.getLocalizedMessage(), e);  
        } finally {  
            DB.close(rs, pstmt);  
        }  
  
     // 6. 遍历每张发货单，跳过已存在的，只保存第一行（createSiblingLines 自动处理其余行）  
        int skipped = 0;  
        int addedInOuts = 0;  
          
        for (int M_InOut_ID : inOutIds) {  
            if (existingInOutIds.contains(M_InOut_ID)) {  
                skipped++;  
                continue;  
            }  
          
            MInOut inout = new MInOut(getCtx(), M_InOut_ID, get_TrxName());  
          
            // 只取第一行，afterSave 中 createSiblingLines() 会自动添加其余行  
            MInOutLine firstLine = new Query(getCtx(), MInOutLine.Table_Name,  
                    "M_InOut_ID=? AND IsActive='Y'", get_TrxName())  
                    .setParameters(M_InOut_ID)  
                    .setOrderBy(MInOutLine.COLUMNNAME_Line)  
                    .first();  
          
            if (firstLine == null) continue;  
          
            MLogisticsLine logLine = new MLogisticsLine(getCtx(), 0, get_TrxName());  
            logLine.setM_Logistics_ID(p_M_Logistics_ID);  
            // Line 由 beforeSave.autoSetLine() 自动生成，无需手动设置  
            logLine.setM_InOut_ID(M_InOut_ID);  
            logLine.setM_InOutLine_ID(firstLine.getM_InOutLine_ID());  
            logLine.setBoxCount(1);  
            logLine.setWeight(BigDecimal.ZERO);  
            logLine.setC_BPartner_ID(inout.getC_BPartner_ID());  
            if (inout.getC_BPartner_Location_ID() > 0)  
                logLine.setC_BPartner_Location_ID(inout.getC_BPartner_Location_ID());  
            if (firstLine.getM_Product_ID() > 0)  
                logLine.setM_Product_ID(firstLine.getM_Product_ID());  
            if (firstLine.getC_UOM_ID() > 0)  
                logLine.setC_UOM_ID(firstLine.getC_UOM_ID());  
            logLine.setQtyDelivered(firstLine.getMovementQty());  
            int C_OrderLine_ID = firstLine.getC_OrderLine_ID();  
            if (C_OrderLine_ID > 0) {  
                logLine.setC_OrderLine_ID(C_OrderLine_ID);  
                MOrderLine ol = new MOrderLine(getCtx(), C_OrderLine_ID, get_TrxName());  
                if (ol.getC_Order_ID() > 0)  
                    logLine.setC_Order_ID(ol.getC_Order_ID());  
            }  
          
            logLine.saveEx(); // afterSave → createSiblingLines() 自动添加其余行  
            addedInOuts++;  
        }  
          
        StringBuilder result = new StringBuilder("@Created@ ").append(addedInOuts).append(" 张发货单");  
        if (skipped > 0)  
            result.append("，跳过 ").append(skipped).append(" 张已存在的发货单");  
        return result.toString();
 
    }  
}