/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLocator;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;

	public class GenerateEmptyStorage extends SvrProcess {

	private int M_Warehouse_ID = 0;

	private String X = "";

	private String Y = "";

	private String Z = "";

	private BigDecimal VacantCapacity;
	private String LocatorValueName;

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
			}else if(name.equals("VacantCapacity")){
				VacantCapacity = (BigDecimal)p.getParameter();
			}
		}
	}

	protected String doIt() {
		int cnt=0;
	//	if (X==null || Y==null || Z==null || (X+Y+Z).isEmpty()){
	//		throw new AdempiereException("X/Y/Z must be set. If above, only Locators will be assigned Empty Storage");
	//	} 
		if (M_Warehouse_ID<1){
			List<MWarehouse> whses = new Query(Env.getCtx(),MWarehouse.Table_Name,"",get_TrxName())
					.setClient_ID().setOnlyActiveRecords(true).list();
			for (MWarehouse whse:whses){
				cnt = createEmptyStorages(cnt, whse);
			}
		}else{
			MWarehouse wh = new Query(Env.getCtx(),MWarehouse.Table_Name,MWarehouse.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())
					.setParameters(M_Warehouse_ID)
					.setClient_ID()
					.first();
			cnt = createEmptyStorages(cnt,wh);
		}
		return "Empty Created "+cnt+", Last Empty Storage at Locator: "+LocatorValueName;
	}

	private int createEmptyStorages(int cnt, MWarehouse whse) { 
		//delete old locators first?
		MWM_EmptyStorage emptyStorage = null; 
		List<MLocator> locs= new Query(Env.getCtx(),MLocator.Table_Name,MLocator.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())
							.setParameters(whse.get_ID()).list();
		for (MLocator loc:locs){
			if (loc==null)
				break;
			MWM_EmptyStorage oldstore = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(loc.get_ID())
					.first();
			if (oldstore!=null){
				int oldempties = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=?",get_TrxName())
						.setParameters(oldstore.get_ID()).count();
				if (oldempties>0)
					throw new AdempiereException("EmptyStorage has lines! DELETE THEM FIRST!");
				log.info("Deleting EmptyStorage:"+loc.getValue());
				oldstore.delete(false);
			} 
			emptyStorage = new MWM_EmptyStorage(Env.getCtx(),0,get_TrxName());
			emptyStorage.setM_Locator_ID(loc.get_ID());
			emptyStorage.setIsFull(false);
			emptyStorage.setIsBlocked(false);
			emptyStorage.setVacantCapacity(VacantCapacity);
			emptyStorage.setAvailableCapacity(VacantCapacity);
			emptyStorage.setPercentage(Env.ONEHUNDRED);
			emptyStorage.saveEx(get_TrxName());
			LocatorValueName = loc.getValue();
			statusUpdate("Generated EmptyStorage for:" + LocatorValueName);
			log.info("EmptyStorage:"+LocatorValueName);
			cnt++;
		}
					
		  
		return cnt;
	}

}
