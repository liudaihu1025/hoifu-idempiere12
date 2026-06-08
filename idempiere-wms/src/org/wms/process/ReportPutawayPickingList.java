/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import org.compiere.process.ProcessInfoParameter;

import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.wms.model.MWM_InOutLine;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.process.SvrProcess;

	public class ReportPutawayPickingList extends SvrProcess {

	private boolean IsActive = false;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("IsActive")){
					IsActive = "Y".equals(p.getParameter());
			}
		}
	}

	protected String doIt() {
		MSequence seq = MSequence.get(getCtx(), "ReportPutawayPickingList");
		if (seq == null)
			throw new AdempiereException("No sequence for ReportPutawayPickingList table");
 		if (IsActive) { 
			String delete = "DELETE FROM ReportPutawayPickingList";
 			DB.executeUpdate(delete, get_TrxName());
 		}

 		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_InOutLine line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);

		String insert="INSERT INTO ReportPutawayPickingList (WM_HandlingUnitOld_ID,WM_InOutLine_ID,WM_InOut_ID,Sequence,WM_DeliverySchedule_ID,WM_Gate_ID,QtyPicked,Value,WM_HandlingUnit_ID,IsSOTrx,X,Y,Z,M_Warehouse_ID,M_Product_ID,C_UOM_ID,M_InOutLine_ID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,IsActive,Updated,UpdatedBy, ReportPutawayPickingList_ID) SELECT a.WM_HandlingUnitOld_ID,a.WM_InOutLine_ID,a.WM_InOut_ID,a.Sequence,b.WM_DeliverySchedule_ID,b.WM_Gate_ID,a.QtyPicked,m.Value,a.WM_HandlingUnit_ID,b.IsSOTrx,m.X,m.Y,m.Z,m.M_Warehouse_ID,a.M_Product_ID,a.C_UOM_ID,a.M_InOutLine_ID,a.AD_Client_ID,a.AD_Org_ID,a.Created,a.CreatedBy,a.IsActive,a.Updated,a.UpdatedBy, nextIDFunc(?, 'N') FROM WM_InOutLine a " 
+"INNER JOIN WM_InOut b ON (b.WM_InOut_ID=a.WM_InOut_ID) INNER JOIN M_Locator m ON (m.M_Locator_ID=a.M_Locator_ID) LEFT JOIN M_InOutLine o ON (o.M_InOutLine_ID=a.M_InOutLine_ID)"
		+"  "
		+"  WHERE WM_InOutLine_ID ="+line.get_ID();

		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement(insert, get_TrxName());
			pstmt.setInt(1, seq.getAD_Sequence_ID());
			pstmt.executeUpdate();
		}
		catch (SQLException e)		{
			throw new AdempiereException(e);
		}
		finally {
			DB.close(pstmt);
			pstmt = null;
		}
	}

	return "RESULT: "+lines.toString();

	}
}
