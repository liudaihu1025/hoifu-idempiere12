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
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_HandlingUnitHistory;
import org.wms.model.MWM_InOutLine;

	public class AssignHandlingUnit extends SvrProcess {

	private int WM_HandlingUnit_ID = 0;
	private boolean IsSameDistribution=false;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}	else if(name.equals("IsSameDistribution")){
				IsSameDistribution = "Y".equals(p.getParameter());
			}
		}
	}

	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();
		if (lines.get(0).getWM_InOut().isSOTrx())
			throw new AdempiereException("Change HU is for Putaways (Returns) mostly");
		int old = 0;
		int units = 0;
		Utils util = new Utils(get_TrxName());
		util.setHandlingUnit(WM_HandlingUnit_ID);
		for (MWM_InOutLine line:lines){
			//if previous handling unit, then release/replace it fully 
			if (line.getWM_HandlingUnit_ID()>0){
				MWM_HandlingUnit oldhu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())
						.setParameters(line.getWM_HandlingUnit_ID())
						.first();
				if (oldhu!=null) {
					oldhu.setQtyMovement(Env.ZERO);
					oldhu.setDocStatus(MWM_HandlingUnit.DOCSTATUS_Drafted);
					oldhu.saveEx(get_TrxName());
					old++;
				}

				//history
				MWM_HandlingUnitHistory oldhuh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())
						.setParameters(oldhu.getWM_HandlingUnit_ID())
						.first();
				if (oldhuh!=null){
					oldhuh.setDateEnd(oldhu.getUpdated());
					oldhuh.saveEx(get_TrxName());
				} else
					log.warning("NO HandlingUnit History for: "+oldhu.getName());

			}
			//Replacing with user selected HU
			MWM_EmptyStorageLine esline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
					.setParameters(line.getWM_InOutLine_ID()).first();
			if (esline==null)
				log.severe("NO StorageLine for InOutLine to AssignHandlingUnit to "+line.getM_Product().getName());
			else {
				util.assignHandlingUnit(IsSameDistribution, line, line.getQtyPicked());
				units++;
			}
		}

	return "OLD UNITS RELEASED: "+old+" NEW UNITS ASSIGNED: "+units;

	}
}
