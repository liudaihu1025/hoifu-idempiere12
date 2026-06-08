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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.compiere.process.SvrProcess;

	public class SetGatePromisedDate extends SvrProcess {

	private int WM_Gate_ID = 0;

	private Timestamp DatePromised = null;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_Gate_ID")){
					WM_Gate_ID = p.getParameterAsInt();
			}
				else if(name.equals("DatePromised")){
					DatePromised = p.getParameterAsTimestamp();
			}
		}
	}

	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";

		List<MWM_DeliveryScheduleLine> lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		//Must select ALL, only can split during Putaway
		MWM_DeliverySchedule ds = (MWM_DeliverySchedule) lines.get(0).getWM_DeliverySchedule();
		int count = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliverySchedule_ID+"=?",get_TrxName())
				.setParameters(ds.get_ID()).count();
		
		if (count!=lines.size())
			throw new AdempiereException("Select ALL. Split during Putaway");
		
		 ds.setDatePromised(DatePromised);
		 ds.setWM_Gate_ID(WM_Gate_ID);
		 ds.saveEx(get_TrxName());

	return "Gate is set. DatePromised set to: "+DatePromised;

	}
}
