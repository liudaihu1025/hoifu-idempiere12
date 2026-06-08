package org.libero.helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.DBException;
import org.compiere.util.DB;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWorkflow;

public class WFNodeStartNodeHandler {

	  public static void renumberAndUpdateStartNode(MWFNode node) {  
	        int workflowId = node.getAD_Workflow_ID();  
	        if (workflowId <= 0)  
	            return;  
	  
	        // 查询该工艺路线下所有工序，按创建顺序排序  
	        String sql = "SELECT AD_WF_Node_ID FROM AD_WF_Node "  
	                + "WHERE AD_Workflow_ID=? AND IsActive='Y' "  
	                + "ORDER BY AD_WF_Node_ID ASC";  
	  
	        List<Integer> nodeIds = new ArrayList<>();  
	        PreparedStatement pstmt = null;  
	        ResultSet rs = null;  
	        try {  
	            pstmt = DB.prepareStatement(sql, node.get_TrxName());  
	            pstmt.setInt(1, workflowId);  
	            rs = pstmt.executeQuery();  
	            while (rs.next()) {  
	                nodeIds.add(rs.getInt(1));  
	            }  
	        } catch (SQLException e) {  
	            throw new DBException(e);  
	        } finally {  
	            DB.close(rs, pstmt);  
	        }  
	  
	        if (nodeIds.isEmpty())  
	            return;  
	  
	        // 重新编号：01, 02, 03...  
	        for (int i = 0; i < nodeIds.size(); i++) {  
	            String newValue = String.format("%02d", i + 1);  
	            DB.executeUpdateEx(  
	                "UPDATE AD_WF_Node SET Value=? WHERE AD_WF_Node_ID=?",  
	                new Object[]{newValue, nodeIds.get(i)},  
	                node.get_TrxName()  
	            );  
	        }  
	  
	        // 同时更新主表起始节点为第一个工序  
	        int firstNodeId = nodeIds.get(0);  
	        int currentStartNode = DB.getSQLValue(node.get_TrxName(),  
	            "SELECT AD_WF_Node_ID FROM AD_Workflow WHERE AD_Workflow_ID=?", workflowId);  
	        if (currentStartNode != firstNodeId) {  
	            DB.executeUpdateEx(  
	                "UPDATE AD_Workflow SET AD_WF_Node_ID=? WHERE AD_Workflow_ID=?",  
	                new Object[]{firstNodeId, workflowId},  
	                node.get_TrxName()  
	            );  
	        }  
	    }  
}