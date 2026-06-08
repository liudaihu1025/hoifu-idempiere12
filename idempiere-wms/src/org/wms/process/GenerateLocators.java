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
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

	public class GenerateLocators extends SvrProcess {

	private int M_Warehouse_ID = 0; 
	private String X = ""; 
	private String Y = ""; 
	private String Z = "";
	private String prefix = "";
	
	
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
			}  else if (name.equals("Prefix")) {
				prefix = (String)p.getParameter();
			}
		}
	}
	
	private StringBuilder LocatorValueName = new StringBuilder();
	char x = 0;
	char y = 0;
	char z = 0;

	protected String doIt() {
		int cnt = 0;
		if (X==null || Y==null || Z==null || (X+Y+Z).isEmpty()){
			throw new AdempiereException("X/Y/Z must be set to maximum dimensions");
		} 
		x = X.charAt(0);
		y = Y.charAt(0);
		z = Z.charAt(0);
		if (M_Warehouse_ID<1){
			List<MWarehouse> whses = new Query(Env.getCtx(),MWarehouse.Table_Name,"",get_TrxName())
					.setClient_ID().setOnlyActiveRecords(true).list();
			for (MWarehouse whse:whses){
				cnt = createLocators(cnt, whse);
			}
		}else{
			MWarehouse wh = new Query(Env.getCtx(),MWarehouse.Table_Name,MWarehouse.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())
					.setParameters(M_Warehouse_ID)
					.setClient_ID()
					.first();
			cnt = createLocators(cnt,wh);
		}
		return "Locators Created "+cnt+", Last Locator Value: "+LocatorValueName.toString();
	}

	private int createLocators(int cnt, MWarehouse whse) { 
		//delete old locators first?
		List<MLocator> oldlocs = new Query(Env.getCtx(),MLocator.Table_Name,MLocator.COLUMNNAME_M_Warehouse_ID+"=? AND M_Locator_ID>999999",get_TrxName())
				.setParameters(whse.get_ID())
				.setClient_ID()
				.list();
		for (MLocator loc:oldlocs){
			MStorageOnHand soh = new Query(Env.getCtx(),MStorageOnHand.Table_Name,MStorageOnHand.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(loc.get_ID())
					.first();
			if (soh==null){
				log.info("Old locator deleted:"+loc.getValue());
				loc.delete(false);
			}
		}
		MLocator locator = null; 
		char a = "A".charAt(0);
		char b = "A".charAt(0);
		char c= "A".charAt(0);

		int posx = x - 'A' + 1;
		int posy = y - 'A' + 1;
		int posz = z - 'A' + 1;
		
		if (Character.isDigit(x)){ 
			posx=new Integer(X);
		}
		if (Character.isDigit(y)){ 
			posy=new Integer(Y);
		}
		if (Character.isDigit(z)){ 
			posz=new Integer(Z); 
		} 
		for (int i=0;i<posx;i++){
			for (int j=0;j<posy;j++){
				for (int k=0;k<posz;k++){
					String aisle = x>8?String.valueOf(i+1):prefix+String.valueOf(i+1);
					if (Character.isLetter(x)){
						aisle = String.valueOf((char)(a+i));
					}
					String bin = j>8?String.valueOf(j+1):prefix+String.valueOf(j+1);
					if (Character.isLetter(y)){
						bin =String.valueOf((char)(b+j));
					}
					String level = k>8?String.valueOf(k+1):prefix+String.valueOf(k+1);
					if (Character.isLetter(z)){
						level =String.valueOf((char)(c+k));
					}
					LocatorValueName = new StringBuilder(whse.getValue()+"-"+aisle+"-"+bin+"-"+level);
					locator = new MLocator(whse,LocatorValueName.toString());
					locator.setXYZ(aisle,bin,level);
					locator.saveEx(get_TrxName());
					cnt++;
					log.info("Locator generated: "+locator.getValue());
					statusUpdate("Generate Locator " + locator.getValue());
				}
			}
		}
		if (locator!=null){
			whse.setM_ReserveLocator_ID(locator.get_ID());
			whse.saveEx(get_TrxName());
			locator.setIsDefault(true);
			locator.saveEx(get_TrxName());
		}
		return cnt;
	}
}
