/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.util.List;

import org.compiere.model.MLocator;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_EmptyStorage;

	public class BlockUnblockLocator extends SvrProcess {
		int cnt = 0;
	private boolean IsBlocked = false;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("IsBlocked")){
					IsBlocked = "Y".equals(p.getParameter());
			}
		}
	}

	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=M_Locator.M_Locator_ID)";

		List<MLocator> lines = new Query(Env.getCtx(),MLocator.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MLocator line:lines){ 
			MWM_EmptyStorage es = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(line.get_ID())
					.first();
			
			es.setIsBlocked(IsBlocked);
			es.saveEx(get_TrxName());
			cnt++;
	}

	return (IsBlocked?"Blocked: ":"Unblocked:")+cnt;

	}
}
