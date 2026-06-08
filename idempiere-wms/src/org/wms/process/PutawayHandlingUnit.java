/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_InOutLine;

	public class PutawayHandlingUnit extends SvrProcess {

	private int WM_HandlingUnit_ID = 0;

	private boolean IsSameDistribution = false;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
				else if(name.equals("IsSameDistribution")){
					IsSameDistribution = "Y".equals(p.getParameter());
			}
		}
	}

	protected String doIt() {
		if (WM_HandlingUnit_ID<1)
			throw new AdempiereException("Must Select Starting Handling Unit");
			
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		int handunit = WM_HandlingUnit_ID;

		//check handling unit not assigned.
		MWM_HandlingUnit hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "+
		MWM_HandlingUnit.COLUMNNAME_M_Locator_ID+"<1",get_TrxName())
				.setParameters(WM_HandlingUnit_ID)
				.first();
		if (hu==null)
			throw new AdempiereException("Handling Unit Already Assigned");
		
		//check if same handling unit or running number
		List<MWM_HandlingUnit> handunits = null;
		if (!IsSameDistribution){//running
				handunits = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
					+MWM_HandlingUnit.COLUMNNAME_M_Locator_ID+"<1",get_TrxName())
					.setParameters(WM_HandlingUnit_ID)
					.setOrderBy(MWM_HandlingUnit.COLUMNNAME_Name)
					.list();
				if (handunits!=null)
					handunit=0;
		}
		int unitserial = 0;
		for (MWM_InOutLine line:lines){
			unitserial++; 
			if (line.getM_Locator_ID()<1)
				continue;
			
			if (IsSameDistribution && handunit>0){
				line.setWM_HandlingUnit_ID(handunit);
				line.saveEx(get_TrxName());
			}
			else {
				handunit = handunits.get(unitserial).getWM_HandlingUnit_ID();
				line.setWM_HandlingUnit_ID(handunit);
				line.saveEx(get_TrxName());
			} 
	}

	return "RESULT: "+unitserial;

	}
}
