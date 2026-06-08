/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import org.compiere.process.ProcessInfoParameter;

import java.util.Date;
import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.wms.model.MWM_EmptyStorageLine;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.process.SvrProcess;

	public class ExpiredStock extends SvrProcess {

	private int WM_HandlingUnit_ID = 0;

	private int Percent = 0;
	private int cnt=0;
	private int QtyMovement = 0;
	String dateset = "";
	private int M_Warehouse_ID = 0;

	private String X = "";

	private String Y = "";

	private String Z = "";

	private int M_Locator_ID = 0;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
				else if(name.equals("Percent")){
					Percent = p.getParameterAsInt();
			}
				else if(name.equals("QtyMovement")){
					QtyMovement = p.getParameterAsInt();
			}
				else if(name.equals("M_Warehouse_ID")){
					M_Warehouse_ID = p.getParameterAsInt();
			}
				else if(name.equals("X")){
					X = (String)p.getParameter();
			}
				else if(name.equals("Y")){
					Y = (String)p.getParameter();
			}
				else if(name.equals("Z")){
					Z = (String)p.getParameter();
			}
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID = p.getParameterAsInt();
			}
		}
	}

	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_EmptyStorageLine.WM_EmptyStorageLine_ID)";
		cnt=0;
		List<MWM_EmptyStorageLine> lines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_EmptyStorageLine line:lines){
			Timestamp time = line.getDateStart(); 
			line.setIsSOTrx(line.isSOTrx()?false:true);
			time.setYear(Percent-1900);
			line.setDateStart(time);
			line.saveEx(get_TrxName());
			dateset=line.getDateStart().toString();
			cnt++;
	}

	return "RESULT: "+cnt+" Set to Date "+dateset;

	}
}
