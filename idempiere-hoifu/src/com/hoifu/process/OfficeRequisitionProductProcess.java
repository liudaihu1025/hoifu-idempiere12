package com.hoifu.process;  
  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.util.ArrayList;  
import java.util.List;  
  
import org.compiere.model.MInventoryLine;  
import org.compiere.model.MProduct;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.AdempiereUserError;  
import org.compiere.util.DB;  
import org.compiere.util.Msg;  
  
/**  
 * 行政/办公物料领用单明细更新物料ID字段  
 */  
@org.adempiere.base.annotation.Process  
public class OfficeRequisitionProductProcess extends SvrProcess {  
      
    private int p_M_Product_ID = 0;  
    private List<Integer> selectedLineIds = new ArrayList<>();  
      
    /**  	
     * 准备方法 - 获取参数  
     */  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (int i = 0; i < para.length; i++) {  
            String name = para[i].getParameterName();  
            if (name.equals("M_Product_ID"))  
                p_M_Product_ID = para[i].getParameterAsInt();  
        }  
    }  
      

    /**  
     * 执行流程  
     */  
    protected String doIt() throws Exception {  
        // 从T_Selection表获取选中的记录
        loadSelectedLines();  
        // 验证参数  
        if (p_M_Product_ID == 0) {  
            throw new AdempiereUserError("请先选择物料");  
        }  
          
        if (selectedLineIds.isEmpty()) {  
            throw new AdempiereUserError("请选勾选明细记录");  
        }  
          
        // 验证产品是否存在  
        MProduct product = MProduct.get(getCtx(), p_M_Product_ID);  
        if (product == null || product.get_ID() == 0) {  
            throw new AdempiereUserError("该物料不存在");  
        }  
        
        int updatedCount = 0;  
          
        // 更新选中的领用单明细行  
        for (Integer lineId : selectedLineIds) {  
            MInventoryLine line = new MInventoryLine(getCtx(), lineId, get_TrxName());  
              
            if (line.get_ID() == 0) {  
                log.warning("领用单明细行不存在: " + lineId);  
                continue;  
            }  
              
            // 验证明细行是否可以更新（检查单据状态）  
            if (line.getParent().isProcessed()) {  
                log.warning("跳过已处理的单据明细行: " + lineId);  
                continue;  
            }  
              
            // 更新产品ID  
            line.setM_Product_ID(p_M_Product_ID);  
            if (line.save()) {  
                updatedCount++;  
            } else {  
                log.warning("更新领用单明细行失败: " + lineId);  
            }  
        }  
          
		String message = "已更新 " + updatedCount + " 条明细记录";
		return message; 
    }  
    
    /**  
     * 从T_Selection表加载选中的明细行  
     */  
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