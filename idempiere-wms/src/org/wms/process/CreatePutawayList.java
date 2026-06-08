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
import java.sql.Timestamp;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.MWM_PreferredProduct;
import org.wms.model.MWM_ProductType;
import org.wms.model.MWM_StorageType;
import org.wms.model.MWM_WarehousePick;

/**
 * Create PutAway and Picking List for Warehouse Locators
 * InBound and OutBound routines to locate according to best practice WMS rules
 * Please refer to http://red1.org/adempiere/ forum
 * @author red1
 * @version 2.0 beta
 */
	public class CreatePutawayList extends SvrProcess {

	private int M_Warehouse_ID = 0; 
	private int WM_HandlingUnit_ID = 0; 
	private int WM_InOut_ID = 0;
	private boolean IsSameDistribution = false;
	private boolean IsSameLine = true;
	private String RouteOrder = ""; //normal
	private String X = "Z"; 
	private String Y = "Z";
	private String Z = "Z";
	private int putaways;
	private int pickings;
	private int notReceived=0;
	private boolean isReceived=false;
	private boolean isSOTrx;
	Timestamp now = new Timestamp (System.currentTimeMillis()); 
	Utils util = null;
	private String trxName = "";
	private boolean external = false;
	MWM_DeliverySchedule externalDeliverySchedule  = null;
	private BigDecimal packFactor=Env.ONE;
	private BigDecimal boxConversion=Env.ONE;
	private BigDecimal currentUOM=Env.ONE;
	private MWarehouse wh = null;
	int productholder = 0;
	List<MWM_EmptyStorageLine>elines = null;
	private int M_Locator_ID = 0; 
	
	public CreatePutawayList(){
		
	}
	
	public CreatePutawayList(MWM_DeliverySchedule schedule, int wM_HandlingUnit_ID2, boolean sameline, boolean samedistriution) { 
		WM_HandlingUnit_ID=wM_HandlingUnit_ID2;
		setTrxName(schedule.get_TrxName());
		externalDeliverySchedule = schedule;
		IsSameLine=sameline;
		IsSameDistribution=samedistriution;
		external = true;
	}
	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;		
	}
	public String executeDoIt(){
		return doIt();
	}
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
				}		
				else if(name.equals("IsSameLine")){
					IsSameLine = (Boolean)p.getParameterAsBoolean();
				}
				else if(name.equals("M_Warehouse_ID")){
				M_Warehouse_ID = p.getParameterAsInt();
				}
				else if(name.equals("IsSameDistribution")){
					IsSameDistribution = "Y".equals(p.getParameter());
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
				else if(name.equals("RouteOrder")){
					RouteOrder = p.getParameterAsString();
				}	
				else if(name.equals("WM_InOut_ID")){
					WM_InOut_ID = p.getParameterAsInt();
				}
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID  = p.getParameterAsInt();
				}
			}
			setTrxName(get_TrxName());
		}
	/**
	 * Main routine 
	 * Determine Lines exists
	 * Create new WM_InOut header.
	 * Segregate between InBound and OutBound process
	 */
	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";
		List<MWM_DeliveryScheduleLine> lines = null;
		if (M_Warehouse_ID==0) {
			M_Warehouse_ID = Env.getContextAsInt(getCtx(), "#M_Warehouse_ID");
		}
		wh = new MWarehouse(getCtx(), M_Warehouse_ID, trxName);
		if (external){
			lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliverySchedule_ID+"=?",trxName)
					.setParameters(externalDeliverySchedule.get_ID())
					.setOrderBy(MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliverySchedule_ID+","+MWM_DeliveryScheduleLine.COLUMNNAME_M_Product_ID)
					.list();			
		}else {
			lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,whereClause,trxName)
					.setParameters(getAD_PInstance_ID())
					.setOnlyActiveRecords(true)
					.setOrderBy(MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliverySchedule_ID+","+MWM_DeliveryScheduleLine.COLUMNNAME_M_Product_ID)
					.list();	
			log.fine(lines.size()+" no of lines for Putaway/Picking creation.");
		}
	
		if (lines==null || lines.isEmpty())
			return "No Lines";
		
		util = new Utils(trxName);
		util.setHandlingUnit(WM_HandlingUnit_ID);
		productholder = 0;
		MWM_InOut wio = null;
		if (WM_InOut_ID>0) {
			wio = new Query(Env.getCtx(), MWM_InOut.Table_Name, MWM_InOut.COLUMNNAME_WM_InOut_ID+"=?", trxName)
					.setParameters(WM_InOut_ID)
					.first();
		} else {
			wio = new MWM_InOut(Env.getCtx(),0,trxName);
			wio.setC_BPartner_ID(lines.get(0).getWM_DeliverySchedule().getC_BPartner_ID());
			wio.setWM_DeliverySchedule_ID(lines.get(0).getWM_DeliverySchedule_ID());
			wio.setName(lines.get(0).getWM_DeliverySchedule().getName());
			wio.setIsSOTrx(lines.get(0).getWM_DeliverySchedule().isSOTrx());
			wio.setWM_Gate_ID(lines.get(0).getWM_DeliverySchedule().getWM_Gate_ID());
		}
		wio.setC_Order_ID(lines.get(0).getWM_DeliverySchedule().getC_Order_ID());
		wio.saveEx(trxName);
		putaways = 0;
		pickings = 0; 
		isSOTrx = wio.isSOTrx();
		//  					 FINISH DIALOG WITH PICK/PUTAWAY LIST LINK
		addBufferLog(wio.get_ID(), wio.getUpdated(), null,
				Msg.parseTranslation(getCtx(), "@WM_InOut_ID@ @Updated@"),
				MWM_InOut.Table_ID, wio.get_ID());
		
		if (isSOTrx){
			pickingProcess(wio,lines);
			util.sortFinalList(wio);
			return "Successful Pickings: "+pickings+" (Future: "+notReceived+")";
		} else{
			putawayProcess(wio,lines);
			util.sortFinalList(wio);
			return "Successful Putaways: "+putaways+" (Future: "+notReceived+")";
		}
	}

	private void putawayProcess(MWM_InOut inout, List<MWM_DeliveryScheduleLine> lines) {
		for (MWM_DeliveryScheduleLine dline:lines){
			if (dline.getWM_InOutLine_ID()>0) {
				log.info("DSLine has WM Line:"+dline.getWM_InOutLine().getWM_InOut().getName());
				continue;//already done
			}
			if (!dline.isReceived()){
				notReceived++;
				isReceived=false;
			} else
				isReceived=true;			
			//running balance in use thru-out here
			BigDecimal balance =dline.getQtyDelivered();				
			
			//get Product from InOut Bound line
			MProduct product = MProduct.get(getCtx(), dline.getM_Product_ID());
			statusUpdate(putaways+". Putaway :"+dline.getQtyDelivered()+" "+dline.getM_Product().getValue());

			//check if defined in PreferredProduct...
			List<MWM_PreferredProduct> preferreds = new Query(Env.getCtx(),MWM_PreferredProduct.Table_Name,MWM_PreferredProduct.COLUMNNAME_M_Product_ID+"=?" ,trxName)
					.setParameters(product.get_ID())
					.setOrderBy(MWM_PreferredProduct.COLUMNNAME_M_Locator_ID)
					.list();
			boolean done=false;
			if (M_Locator_ID>0) {
				balance=startPutAwayProcess(inout, dline, balance, M_Locator_ID);
				if (balance.compareTo(Env.ZERO)>0)
					throw new AdempiereException("Locator insufficient space for "+dline.getQtyOrdered()+" "+dline.getM_Product().getValue());
				continue;
			}
			if (preferreds!=null){
				for (MWM_PreferredProduct preferred:preferreds){
					 
					if (M_Warehouse_ID>0){
						if (preferred.getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)
							continue; 
					}
					if (preferred.getM_Locator().getX().compareTo(X)>=0 || preferred.getM_Locator().getY().compareTo(Y)>=0  || preferred.getM_Locator().getZ().compareTo(Z)>=0 )
						continue;
					//get next EmptyStorage, if fit, then break, otherwise if balance, then continue
					int locator_id = preferred.getM_Locator_ID();
					balance = startPutAwayProcess(inout,dline,balance,locator_id);
					if (balance.compareTo(Env.ZERO)>0)
						continue;
					else {
						done=true;
						break;
					}
				}
			} 
			if (done)
				continue; //done so go to next DeliveryScheduleLine. 
			//get ProductType = StorageType
			MWM_ProductType prodtype = new Query(Env.getCtx(),MWM_ProductType.Table_Name,MWM_ProductType.COLUMNNAME_M_Product_ID+"=?",trxName)
					.setParameters(product.get_ID())
					.first();
			 	if (prodtype!=null){
					List<MWM_StorageType> stortypes= new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_WM_Type_ID+"=?",trxName)
							.setParameters(prodtype.getWM_Type_ID())
							.setOrderBy(MWM_StorageType.COLUMNNAME_WM_Type_ID)
							.list();				
					for (MWM_StorageType stortype:stortypes){
						if (stortype!=null){
							if (stortype.getM_Locator().getX().compareTo(X)>=0 || stortype.getM_Locator().getY().compareTo(Y)>=0  || stortype.getM_Locator().getZ().compareTo(Z)>=0 )
								continue;
							if (M_Warehouse_ID>0)
								if (stortype.getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)
									continue;
							//get next EmptyStorage, if fit, then break, otherwise if balance, then continue
							int locator_id = stortype.getM_Locator_ID(); 
							balance = startPutAwayProcess(inout,dline,balance,locator_id);
							if (balance.compareTo(Env.ZERO)>0)
								continue;
							else {
								done=true;
								break;
							}
						}	
					}
			 	}
			 if (done)
					continue; //enough, i already putaway all.
			
			//get Default Locator since nothing fits in Zone / Preferred Product
			int locator_id = wh.getDefaultLocator().get_ID();
			if (locator_id==0)
				throw new AdempiereException("There is no Default Locator at "+wh.getName());
			balance = startPutAwayProcess(inout,dline,balance,locator_id);
				if (balance.compareTo(Env.ZERO)>0)
					log.saveError("ERROR","Default Storage insufficient for "+balance+" "+dline.getM_Product().getValue()+" - increase capacity to a million");
		}
	}

	/**
	 * Putaway in boxes (Highest UOM Factor), while still vacant 
	 * @param balance returned in original UOM
	 * @param locator_id
	 * @return balance of unallocated qty to empty storage
	 */
	private BigDecimal startPutAwayProcess(MWM_InOut winout, MWM_DeliveryScheduleLine dsline, BigDecimal balance, int locator_id) {
		MWM_EmptyStorage empty = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
				.setParameters(locator_id)
				.first();
		if (empty==null)
			throw new AdempiereException("No Empty Storage set for locator id: "+locator_id);
		//if its full go back and look for next EmptyStorage
		if (empty.isFull()) {
			log.warning("Storage Full at "+empty.getM_Locator().getValue()+" for "+balance+" "+dsline.getM_Product().getValue());
			return balance;  
		}
		BigDecimal alloting = uomFactors(dsline,balance);
		BigDecimal vacancy = util.getAvailableCapacity(empty).multiply(boxConversion);	  
		log.info("Locator "+empty.getM_Locator().getValue()+" has "+vacancy+" for "+alloting+" "+dsline.getM_Product().getValue());
		BigDecimal holder = Env.ZERO;
		boolean fullyfilllocator=false;
		if (alloting.compareTo(vacancy)>=0 && IsSameLine==false){
			alloting = vacancy;
			fullyfilllocator=true;
		} 
		//PutawayLoop until Locator is full - Alloted limited to not exceed Box (HighestUOMSize)
 		while (alloting.compareTo(Env.ZERO)>0) {
 			BigDecimal bal = Env.ZERO;
 			if (alloting.compareTo(boxConversion)>=0)
 				bal=boxConversion;
 			else {
 				if (IsSameLine) {
  					log.fine("SameLine Break. Not Putaway:"+bal+" "+dsline.getM_Product().getName());
 					break;
 				}
 	 			bal=alloting;
 			}			
 			MWM_InOutLine inoutline = util.newInOutLine(winout,dsline,bal); 
 			setLocator(inoutline,locator_id); 
 	 		inoutline = util.assignHandlingUnit(IsSameDistribution,inoutline,bal); 
 	 		alloting=alloting.subtract(bal);
 	 		holder=holder.add(bal);
 	 		if (alloting.compareTo(Env.ZERO)==0)
 	 			log.fine("Locator fully took "+holder+" "+dsline.getM_Product().getName());
 	 		else
 	 		log.fine("Same Locator "+empty.getM_Locator().getValue()+" to take remaining "+alloting+" "+dsline.getM_Product().getName());
 		}
 		if (fullyfilllocator) {
 			balance=balance.subtract(holder.divide(packFactor,2,RoundingMode.HALF_EVEN));
 			log.fine("Locator "+empty.getM_Locator().getValue()+" fully filled by "+dsline.getM_Product().getName());
 		}
 		else
 			balance=Env.ZERO;
 		return balance;
	}

	private void setLocator(MWM_InOutLine line, int put_pick) { 
		line.setM_Locator_ID(put_pick);
		line.saveEx(trxName);
		putaways++;
	}

	private void pickingProcess(MWM_InOut inout, List<MWM_DeliveryScheduleLine> lines) {
		for (MWM_DeliveryScheduleLine line:lines){
			if (line.getWM_InOutLine_ID()>0)
				continue;//already done
			if (!line.isReceived()){
				notReceived++;
				isReceived=false;
			} else
				isReceived=true;
			//running balance in use thru-out here
			BigDecimal balance =line.getQtyDelivered();			
			statusUpdate("Picking :"+line.getQtyDelivered()+" "+line.getM_Product().getValue());
			//if Handling Unit is set, then assign while creating WM_InOuts. EmptyLocators also assigned. Can be cleared and reassigned in next Info-Window
			if (!getPickingLocators(inout,line)) {
				line.setWM_InOutLine_ID(0);
				line.saveEx(trxName);
				log.warning("# "+pickings+". "+line.getQtyOrdered()+" of "+line.getM_Product().getValue());
			}else
				log.info("# "+pickings+". "+line.getQtyOrdered()+" of "+line.getM_Product().getValue());
		}	
	}

	private boolean getPickingLocators(MWM_InOut inout,MWM_DeliveryScheduleLine dline) {  
		MProduct product = MProduct.get(getCtx(), dline.getM_Product_ID());
		if (product==null) {
			log.severe("Fatal: Suddenly Delivery Line has no Product!");
			return false;
		}
		//check if there was WM_WarehousePick (for preselected pick during Sales Order)
		if (dline.getC_OrderLine_ID()>0){dline.getM_Product().getName();
			if  (orderLineWarehousePick(inout, dline))
				return true; 
		}
		//NOrmal (shortest), FIfo, or LIfo based on previous putaway date start order
		if (RouteOrder.equals("NO") && productholder!=dline.getM_Product_ID()) {
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">?",trxName)
					.setParameters(product.get_ID(),0)
					.setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart+","+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement)
					.list();
		}else if (RouteOrder.equals("FI")&&productholder!=dline.getM_Product_ID()) {
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">?",trxName)
					.setParameters(product.get_ID(),0)
					.setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart+","+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+" DESC")
					.list();
		}else if (RouteOrder.equals("LI")&&productholder!=dline.getM_Product_ID()) {
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">?",trxName)
						.setParameters(product.get_ID(),0)
						.setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart+" DESC"+","+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+" DESC")
						.list();
		}else if (productholder!=dline.getM_Product_ID()){
			elines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">?",trxName)
					.setParameters(product.get_ID(),0)
					.setOrderBy(product.getGuaranteeDays()>0?MWM_EmptyStorageLine.COLUMNNAME_DateStart:MWM_EmptyStorageLine.COLUMNNAME_DateStart+" DESC")
					.list();
		}
		if (elines.isEmpty()){
			throw new AdempiereException("Product has no Storage available to pick: "+product.getValue());
 		}
		BigDecimal eachQty=uomFactors(dline,Env.ZERO);
		if (productholder!=dline.getM_Product_ID()) {
			elines=util.removeOtherOrgBlockedAndZero(false,null, elines);
			productholder=dline.getM_Product_ID(); 
		}  
		for (int i=0;i<elines.size();i++){
			MWM_EmptyStorageLine eline = elines.get(i);
			//if (eline.getWM_InOutLine().getM_InOutLine_ID()<1 && line.isReceived())
			//	throw new AdempiereException("This Product Has No Shipment/Receipt record. Complete its WM Inout first before picking - "+product.getName()+" -> "+eline.getWM_InOutLine());
			if (M_Locator_ID>0 && eline.getWM_EmptyStorage().getM_Locator_ID()!=M_Locator_ID)
				continue;
			if (M_Warehouse_ID>0 && eline.getWM_EmptyStorage().getM_Locator().getM_Warehouse_ID()!=M_Warehouse_ID)
				continue; 
			//take those that are Complete DocStatus (Putaway) or no HandlingUnit 
			MWM_HandlingUnit hu = MWM_HandlingUnit.get(getCtx(), eline.getWM_HandlingUnit_ID(),trxName);

			if (hu==null && dline.isReceived())
				continue; //next EmptyLine until not InProgress
			
			//Locator EmptyLine Quantity has more than what you picking
			if (eline.getQtyMovement().compareTo(eachQty)>=0){ 
				eachQty = eachQty.subtract(startPickingProcess(eachQty,inout,dline, eline));
				if (eachQty.compareTo(Env.ZERO)==0)
					return true;
			//Locator EmptyLine Quantity has less than what you picking	
			}else if(!IsSameLine) { //if not SameLine 
				eachQty = eachQty.subtract(startPickingProcess(eline.getQtyMovement(),inout,dline, eline));
				if (eachQty.compareTo(Env.ZERO)==0)
					return true;
				}  
		}
		if (eachQty.compareTo(Env.ZERO)>0)
			throw new AdempiereException("NOT ENOUGH STOCK BY "+eachQty+" for "+dline.getQtyOrdered()+" "+dline.getM_Product().getValue()+" with storage lines:"+elines.size());
		return false;
	}

	private BigDecimal startPickingProcess(BigDecimal picked, MWM_InOut inout, MWM_DeliveryScheduleLine line,MWM_EmptyStorageLine eline) {
		MWM_EmptyStorage empty = MWM_EmptyStorage.get(getCtx(), eline.getWM_EmptyStorage_ID(),trxName);
		//Locator EmptyLine Quantity has more than what you picking
		if (eline.getQtyMovement().compareTo(picked)>0){
			//breakup - handling unit assigned automatically in util.assignhandlingUnit
				MWM_InOutLine inoutline = util.newInOutLine(inout,line,picked); 
				setLocator(inoutline, eline.getWM_EmptyStorage().getM_Locator_ID());				
				util.setHandlingUnit(WM_HandlingUnit_ID); 
				//still need to know the present HU ID for opening box and break out
				inoutline = util.assignHandlingUnit(IsSameDistribution,inoutline, picked);
				inoutline.setWM_HandlingUnitOld_ID(eline.getWM_HandlingUnit_ID());
				inoutline.setC_DocType_ID(inout.isSOTrx()?MDocType.getDocType(MDocType.DOCBASETYPE_SalesOrder):MDocType.getDocType(MDocType.DOCBASETYPE_PurchaseOrder));
				inoutline.saveEx(trxName);
				eline.setWM_InOutLine_ID(inoutline.get_ID());
				eline.saveEx(trxName);
				elines.remove(eline);
				pickings++;
				statusUpdate("Picked "+picked+" from "+eline.getQtyMovement()+" "+eline.getM_Product().getValue()+" at "+empty.getM_Locator().getValue());
				return picked;
			 
		//Locator EmptyLine Quantity has exactly same size what you picking	
		} else {
			MWM_InOutLine inoutline = util.newInOutLine(inout,line,picked); 
			setLocator(inoutline, eline.getWM_EmptyStorage().getM_Locator_ID());
			inoutline.setWM_HandlingUnit_ID(eline.getWM_HandlingUnit_ID());
			inoutline.setC_DocType_ID(inout.isSOTrx()?MDocType.getDocType(MDocType.DOCBASETYPE_SalesOrder):MDocType.getDocType(MDocType.DOCBASETYPE_PurchaseOrder));
			inoutline.saveEx(trxName);
			eline.setWM_InOutLine_ID(inoutline.get_ID());
			eline.saveEx(trxName);
			elines.remove(eline); 
			statusUpdate("Picked "+eline.getQtyMovement()+" "+eline.getM_Product().getValue()+" at "+empty.getM_Locator().getValue());
			pickings++;
			if (isReceived){
				if (WM_HandlingUnit_ID>0){ //Not logical as we do not know which box to pick from
					//DO NOTHING, only when breakup above
				}
			}
			return picked;
		}
	}

	private boolean orderLineWarehousePick(MWM_InOut inout, MWM_DeliveryScheduleLine line) {
		MWM_WarehousePick wp = new Query(Env.getCtx(),MWM_WarehousePick.Table_Name,MWM_WarehousePick.COLUMNNAME_C_OrderLine_ID+"=?",trxName)
				.setParameters(line.getC_OrderLine_ID())
				.first();
		if (wp!=null){
			MWM_EmptyStorageLine sel = new Query(Env.getCtx(), MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorageLine_ID+"=? AND "
					+MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=?", trxName)
					.setParameters(wp.getWM_EmptyStorageLine_ID(),wp.getM_Product_ID())
					.first();
			if (sel==null){
				log.severe("WarehousePick by Sales OrderLine is lost!:"+wp.toString());
			}else {
				BigDecimal picked = sel.getQtyMovement();
				if (picked.compareTo(wp.getQtyOrdered())==0) { 
					picked = startPickingProcess(picked,inout, line, sel);
				}
				if (picked.compareTo(Env.ZERO)==0){
					wp.setDescription(wp.getDescription()+" SUCCESS DURING PICKING!"); 
					wp.saveEx(trxName);
					return true;
				}else {
					log.severe("Cannot pickingEmptyStorage - "+sel.toString());
					return false;
			}}}
		return false;
	}
	private BigDecimal uomFactors(MWM_DeliveryScheduleLine line, BigDecimal balance) {
		currentUOM=Env.ONE;
		BigDecimal qtyEntered = line.getQtyOrdered();//.multiply(new BigDecimal(product.getUnitsPerPack()));
		//Current = current UOM Conversion Qty	
		MUOMConversion currentuomConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=? AND "
				+MUOMConversion.COLUMNNAME_C_UOM_To_ID+"=?",null)
				.setParameters(line.getM_Product_ID(),line.getC_UOM_ID())
				.first();
		if (currentuomConversion!=null)
			currentUOM = currentuomConversion.getDivideRate();
		BigDecimal eachQty=qtyEntered.multiply(currentUOM);
		if (balance.compareTo(Env.ZERO)>0)
			eachQty=balance.multiply(currentUOM);
		//Pack Factor calculation
		MUOMConversion highestUOMConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=?",null)
				.setParameters(line.getM_Product_ID())
				.setOrderBy(MUOMConversion.COLUMNNAME_DivideRate+" DESC")
				.first(); 
		if (highestUOMConversion!=null) {
			boxConversion = highestUOMConversion.getDivideRate();
			if (currentUOM==Env.ONE)
				return eachQty;
			if (currentuomConversion.getDivideRate().compareTo(highestUOMConversion.getDivideRate())!=0)//Plastic5 scenario
				packFactor = boxConversion.divide(currentUOM,2,RoundingMode.HALF_EVEN);
			else
				packFactor = boxConversion;
		}else
			boxConversion=qtyEntered;//avoid non existent of box type, making each line a box by default
		return eachQty;
		} 
}
