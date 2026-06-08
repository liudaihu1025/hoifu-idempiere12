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
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_ProductType;
import org.wms.model.MWM_StorageType; 

/**
 * @deprecated
 * @author red1
 *
 */
	public class SetTypeString extends SvrProcess {

	private int WM_Type_ID = 0;

	private int M_Product_Category_ID = 0;

	private int M_Warehouse_ID = 0;

	private String X = "";

	private String Y = "";

	private String Z = "";
	
	private int pcnt = 0;
	private int scnt=0;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_Type_ID")){
					WM_Type_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Product_Category_ID")){
					M_Product_Category_ID = p.getParameterAsInt();
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
		}
	}

	protected String doIt() { 
		List<MProduct>products = new Query(Env.getCtx(),MProduct.Table_Name,M_Product_Category_ID>0?MProduct.COLUMNNAME_M_Product_Category_ID+"="+M_Product_Category_ID:"",null)
				.list();
		if (products==null)
			throw new AdempiereException("No Products!");
		
		for (MProduct product:products){
			if (M_Product_Category_ID>0 && product.getM_Product_Category_ID()!=M_Product_Category_ID)
				continue;
			
			StringBuilder typestring = new StringBuilder();
			List<MWM_ProductType>ptypes = new Query(Env.getCtx(),MWM_ProductType.Table_Name,MWM_ProductType.COLUMNNAME_M_Product_ID+"=?",null)
					.setParameters(product.get_ID())
					.setOrderBy(MWM_ProductType.COLUMNNAME_Priority)
					.list();
			if (ptypes==null)
				continue;
			
			for (MWM_ProductType ptype:ptypes){ 
				typestring.append(ptype.getWM_Type().getName());
			} 
			for (MWM_ProductType ptype:ptypes){ 
				ptype.setTypeString(typestring.toString());
				ptype.saveEx(get_TrxName());
				pcnt++;
			} 
		}
		
		List<MLocator>locators = new Query(Env.getCtx(),MLocator.Table_Name,"",get_TrxName())
				.list();		
		if (locators==null)
			throw new AdempiereException("Warehouse has no locators!");
		
		for (MLocator locator:locators){
			if (M_Warehouse_ID >0 && locator.getM_Warehouse_ID()!=M_Warehouse_ID)
				continue;
			StringBuilder typestring = new StringBuilder();
			List<MWM_StorageType>stypes= new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_M_Locator_ID+"=?",null)
					.setParameters(locator.get_ID()).list();
			if (stypes==null)
				continue;
			for (MWM_StorageType stype:stypes){
				typestring.append(stype.getWM_Type().getName());
			}
			for (MWM_StorageType stype:stypes){
				stype.setTypeString(typestring.toString());
				stype.saveEx(get_TrxName());
				scnt++;
			}
		} 
		return "Product Types set: "+pcnt+", Storage Type set: "+scnt;
	}
}
