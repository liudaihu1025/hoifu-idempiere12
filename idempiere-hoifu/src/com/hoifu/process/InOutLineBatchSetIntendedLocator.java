package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class InOutLineBatchSetIntendedLocator extends SvrProcess {

	private List<Integer> selectedLineIds = new ArrayList<>();  
	private int p_M_Locator_ID = 0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_Locator_ID"))
				p_M_Locator_ID = para[i].getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_M_Locator_ID == 0)
			throw new AdempiereUserError("@FillMandatory@ @M_Locator_ID@");

		loadSelectedLines();
		if (selectedLineIds.size() == 0)
			return "@NoRecordSelected@";

		int updated = 0;
		for (int lineId : selectedLineIds) {
			int no = DB.executeUpdateEx(
					"UPDATE M_InOutLine SET IntendedLocation_ID=?, Updated=getDate(), UpdatedBy=?"
							+ " WHERE M_InOutLine_ID=?",
					new Object[] { p_M_Locator_ID, getAD_User_ID(), lineId }, get_TrxName());
			if (log.isLoggable(Level.FINE))
				log.fine("Updated M_InOutLine_ID=" + lineId + ", rows=" + no);
			updated += no;
		}

		return "@Updated@ #" + updated;
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
	        throw new IllegalArgumentException("获取明细记录失败");  
	    } finally {  
	        DB.close(rs, pstmt);  
	    }  
	} 
}
