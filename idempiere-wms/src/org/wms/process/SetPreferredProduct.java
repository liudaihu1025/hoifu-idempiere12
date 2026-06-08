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
import org.compiere.model.MLocator;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_PreferredProduct;

	public class SetPreferredProduct extends SvrProcess {

	private int M_Product_ID = 0;

	private boolean DeleteOld =false;

	private int cnt=0;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("M_Product_ID")){
					M_Product_ID = p.getParameterAsInt();
			}
				else if(name.equals("DeleteOld")){
					DeleteOld = (Boolean)p.getParameterAsBoolean();
			}
		}
	}

	protected String doIt() {		
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=M_Locator.M_Locator_ID)";

		List<MLocator> lines = new Query(Env.getCtx(),MLocator.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MLocator line:lines){
			if (DeleteOld) {
				List<MWM_PreferredProduct>oldprefs = new Query(Env.getCtx(),MWM_PreferredProduct.Table_Name,MWM_PreferredProduct.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
						.setParameters(line.getM_Locator_ID())
						.list();
				for (MWM_PreferredProduct old:oldprefs){
					old.delete(false);
				}
			}
			if (M_Product_ID<1)	
				continue;
			MWM_PreferredProduct preferred = new MWM_PreferredProduct(Env.getCtx(),0,get_TrxName());
			preferred.setM_Locator_ID(line.getM_Locator_ID());
			preferred.setM_Product_ID(M_Product_ID);
			preferred.saveEx(get_TrxName());
			cnt++;
		}

		return "Preferred Product Set - No. Of Locators: "+cnt;
	}
}
