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
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_PreferredProduct;

	public class SetLocatorPreferred extends SvrProcess {

	private int M_Warehouse_ID = 0;

	private String X = "Z"; 
	private String Y = "Z";
	private String Z = "Z";
	
	private int cnt = 0;

	private boolean DeleteOld = false;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
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
				else if(name.equals("DeleteOld")){
				DeleteOld = (boolean)p.getParameter();
			}
		}
	}

	protected String doIt() {
		if (X=="Z" || Y=="Z" || Z=="Z" ){
			;
		}else {
			if (M_Warehouse_ID < 1)
				throw  new AdempiereException("Select Warehouse if X,Y,Z has values");
 		}
		
		MWarehouse wh = new Query(Env.getCtx(),MWarehouse.Table_Name,MWarehouse.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())
				.setParameters(M_Warehouse_ID)
				.first();			
		List<MLocator> locs = new Query(Env.getCtx(),MLocator.Table_Name,MLocator.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())
				.setParameters(M_Warehouse_ID)
				.list();
		
		if (wh==null)
			throw new AdempiereException("Warehouse fatal error!");
		
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=M_Product.M_Product_ID)";

		List<MProduct> lines = new Query(Env.getCtx(),MProduct.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MProduct line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);

			for (MLocator loc:locs){
				if (DeleteOld){
					List<MWM_PreferredProduct> prefs = new Query(Env.getCtx(),MWM_PreferredProduct.Table_Name,MWM_PreferredProduct.COLUMNNAME_M_Locator_ID,get_TrxName())
							.setParameters(loc.get_ID())
							.list();
					for (MWM_PreferredProduct pref:prefs){
						log.info(pref.getName()+" PREFERRED RECORD DELETED");
						pref.delete(false, get_TrxName());
					} 
				}
				if (loc.getX().compareTo(X)>=0)
					break;
				if (loc.getY().compareTo(Y)>=0)
					break;
				if (loc.getZ().compareTo(Z)>=0)
					break;
				
				MWM_PreferredProduct pref = new MWM_PreferredProduct(getCtx(), 0, get_TrxName());
				pref.setM_Product_ID(line.get_ID());
				pref.setM_Locator_ID(loc.get_ID());
				pref.saveEx(get_TrxName());
				cnt++;
			} 
		}

		return "Number of Preferred Locators created "+cnt;

	}
}
