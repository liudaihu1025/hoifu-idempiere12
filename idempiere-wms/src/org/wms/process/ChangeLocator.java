/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_InOutLine;

	public class ChangeLocator extends SvrProcess {
		
		private int packFactor = 0;
		private int M_Locator_ID = 0;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID = p.getParameterAsInt();
			}
		}
	}

	protected String doIt() {
		Utils util = new Utils(get_TrxName());
		if (M_Locator_ID==0)
			throw new AdempiereException("Must set Locator");
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_InOutLine.WM_InOutLine_ID)";

		List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();
		
		for (MWM_InOutLine line:lines){ 
			//if not received don't do it
			if (!line.getWM_DeliveryScheduleLine().isReceived()) {
				log.warning("Item not received: "+line.getM_Product().getName()+" at "+line.getM_Locator().getValue());
				continue;
			}
			line.setM_Locator_ID(M_Locator_ID);
			line.saveEx(get_TrxName());
			
			//update related records	
			//Handling Unit
			MWM_HandlingUnit hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())
					.setParameters(line.getWM_HandlingUnit_ID())
					.first();
			if (hu!=null){
				hu.setM_Locator_ID(line.getM_Locator_ID());
				hu.saveEx(get_TrxName());
			}
			
			
			//EmptyStorage Line
			MWM_EmptyStorageLine eline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
					.setParameters(line.get_ID()).first();
			if (eline==null)
				continue;			
			//Target Storage
			MWM_EmptyStorage target = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(M_Locator_ID)
					.first();
			if (target==null)
				throw new AdempiereException("No Target Storage at Locator:"+line.getM_Locator().getValue());
			
			//Calculate UOM Box or Pack Factor
			MUOMConversion highestUOMConversion = new Query(getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=?",null)
					.setParameters(line.getM_Product_ID())
					.setOrderBy(MUOMConversion.COLUMNNAME_DivideRate+" DESC")
					.first(); 
			if (highestUOMConversion!=null)  
				packFactor = highestUOMConversion.getDivideRate().intValue();
			else
				packFactor = line.getM_Product().getUnitsPerPack();
			
			//check AvailableCapacity
			BigDecimal balance = target.getAvailableCapacity().subtract(line.getQtyPicked().divide(new BigDecimal(packFactor),2,RoundingMode.HALF_EVEN));
			if (balance.compareTo(Env.ZERO)<0)
				throw new AdempiereException("Insufficient space at target storage: "+line.getM_Locator().getValue());
			
			target.setAvailableCapacity(balance);
			target.setPercentage((target.getAvailableCapacity().divide(target.getVacantCapacity(),2,RoundingMode.HALF_EVEN)).multiply(Env.ONEHUNDRED));
			//set is Full if 0% vacant
			if (target.getPercentage().compareTo(Env.ZERO)>=0)
				target.setIsFull(true);
			else
				target.setIsFull(false);
			target.saveEx(get_TrxName());
			
			//Source Storage
			MWM_EmptyStorage source = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_WM_EmptyStorage_ID+"=?",get_TrxName())
					.setParameters(eline.getWM_EmptyStorage_ID()).first();
			if (source==null)
				throw new AdempiereException("No Storage parent for:"+line.getM_Locator().getValue());
			source.setAvailableCapacity(source.getAvailableCapacity().add(line.getQtyPicked().divide(new BigDecimal(packFactor),2,RoundingMode.HALF_EVEN)));
			source.setPercentage((source.getAvailableCapacity().divide(source.getVacantCapacity(),2,RoundingMode.HALF_EVEN)).multiply(Env.ONEHUNDRED));
			//set is Full if 0% vacant
			if (source.getPercentage().compareTo(Env.ZERO)==0)
				source.setIsFull(true);
			else
				source.setIsFull(false);
			source.saveEx(get_TrxName());
			
			eline.setWM_EmptyStorage_ID(target.get_ID());
			eline.saveEx(get_TrxName());
	}

	return "RESULT: "+lines.toString();

	}
}
