package org.wms.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_M_Locator;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.TimeUtil;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_ESLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_HandlingUnitHistory;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.X_WM_HandlingUnit;

public class Utils {
	public Utils(String processTrxName) {
		trxName = processTrxName; 
	}
	private String trxName="";
	private int WM_HandlingUnit_ID = 0;
	private boolean same = false;
	private Timestamp Today = new Timestamp (System.currentTimeMillis()); 
	MWM_HandlingUnit hu = null;
	static int cnt=0;
	CLogger			log = CLogger.getCLogger (getClass());
	
	public void setHandlingUnit(int unit){
		WM_HandlingUnit_ID = unit;
	}
	
	/**
	 * SameDistribution = use same HandlingUnit
	 * @param inoutline
	 * @param empty
	 * @param qty
	 * @return WM_InOutLine
	 */
	public MWM_InOutLine assignHandlingUnit(boolean sameDistribution, MWM_InOutLine inoutline, BigDecimal qty) { 
		 if (sameDistribution && same){			 
		 }else  
			 getAvailableHandlingUnit();
		 //SameDistribution = use same HandlingUnit for all selected items
		if (same){
			hu.setQtyMovement(hu.getQtyMovement().add(qty));
		}else {
			if (inoutline.getWM_InOut().isSOTrx()) //so no other picking can touch this
				hu.setDocStatus(X_WM_HandlingUnit.DOCSTATUS_InProgress);
			else 
				hu.setDocStatus(X_WM_HandlingUnit.DOCSTATUS_Completed);
			hu.setQtyMovement(qty); 
			hu.setM_Product_ID(inoutline.getM_Product_ID());
			WM_HandlingUnit_ID = hu.get_ID();
			if (sameDistribution)
				same = true;
		}
		hu.saveEx(trxName);
		
		//create new history
		MWM_HandlingUnitHistory huh = new MWM_HandlingUnitHistory(Env.getCtx(),0,trxName);
		huh.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
		huh.setWM_InOutLine_ID(inoutline.get_ID());
		huh.setC_Order_ID(inoutline.getC_OrderLine().getC_Order_ID());
		huh.setQtyMovement(qty);
		huh.setC_UOM_ID(inoutline.getC_UOM_ID());
		huh.setM_Product_ID(inoutline.getM_Product_ID());
		huh.setDateStart(hu.getUpdated());
		huh.saveEx(trxName);
		MWM_EmptyStorageLine eline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_HandlingUnit_ID+"=?", trxName)
				.setParameters(inoutline.getWM_HandlingUnit_ID())
				.first();
		if (eline==null)
			log.fine("StorageLine not found Handling Unit: "+inoutline.getWM_HandlingUnit().getName());
		else{
			eline.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
			eline.saveEx(trxName);
		}
		inoutline.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
		inoutline.saveEx(trxName);
		log.info(hu.getName()+" assigned to "+inoutline.getQtyPicked()+" "+inoutline.getM_Product().getValue());
		return inoutline;
	}

	private void getAvailableHandlingUnit() {
		String huname = "";
		if (WM_HandlingUnit_ID==0) {//auto set to last Org HU - create more if needed
			int AD_Org_ID = Env.getAD_Org_ID(Env.getCtx());
			//get last name of Org HandlingUnit
			hu = new Query(Env.getCtx(), MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_AD_Org_ID+"=? AND "
					+MWM_HandlingUnit.COLUMNNAME_DocStatus+"=?",trxName)
					.setParameters(AD_Org_ID,MWM_HandlingUnit.DOCSTATUS_Drafted) 
					.setOrderBy(MWM_HandlingUnit.COLUMNNAME_Name)
					.first();
			if (hu!=null)
				huname=hu.getName();
			else if (AD_Org_ID==0)
				throw new AdempiereException("Check your Organization - "+AD_Org_ID);
			else {
				log.warning("RUN Generate HandlingUnit process."); 
				 generateHU();
				 nextDraftHU(hu.getName());
				 return;
			}
		} else {
			huname = huname();
		} 
		if (nextDraftHU(huname)==null){
			generateHU();
			nextDraftHU(huname);
		}
	}

	private String huname() {
		String huname;
		hu = new MWM_HandlingUnit(Env.getCtx(), WM_HandlingUnit_ID, trxName);
		huname = hu.getName();
		return huname;
	}

	private MWM_HandlingUnit nextDraftHU(String huname) {
		hu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_Name+">? AND "
			+MWM_HandlingUnit.COLUMNNAME_DocStatus+"=?",trxName)
			.setParameters(huname.substring(0,huname.length()-3),MWM_HandlingUnit.DOCSTATUS_Drafted) 
			.setOrderBy(MWM_HandlingUnit.COLUMNNAME_Name)
			.first();
		if (hu!=null) {
			WM_HandlingUnit_ID=hu.get_ID();
		}
		return hu;
	}

	private void generateHU() {
		int AD_Org_ID=Env.getAD_Org_ID(Env.getCtx());
		int leading = 3;
		int Counter=200;
		StringBuilder zeros = new StringBuilder();
		for (int i=0;i<leading;i++) {
			zeros.append("0");
		} 
 		//find last number
		int last = 0;
		MWM_HandlingUnit lasthu = new Query(Env.getCtx(),MWM_HandlingUnit.Table_Name,"AD_Org_ID=?",trxName)
				.setParameters(AD_Org_ID)
				.setOrderBy("Created DESC").first();
		if (lasthu!=null) {
			AD_Org_ID = lasthu.getAD_Org_ID();
			int x = lasthu.getName().length()-leading;
			String lastnumber = lasthu.getName().substring(lasthu.getName().length()-x);
			last = Integer.valueOf(lastnumber)+1;
		} else 
			throw new AdempiereException("Your Org has no Handling Unit. Create one first manually");
		for (int i=0;i<Counter;i++) {
			 createHandlingUnit(lasthu,leading+3,last++); 
		} 
	}

	private void createHandlingUnit(MWM_HandlingUnit lasthu,int zeros,int serial) { 
		String Prefix = lasthu.getName().substring(0,2);
		String name = Prefix+String.format("%0"+zeros+"d", serial);
		hu = new Query(Env.getCtx(), MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_Name+"=?",trxName)
				.setParameters(name)
				.first();
		if (hu!= null)
			return;
		hu = new MWM_HandlingUnit(Env.getCtx(), 0, trxName);
		hu.setAD_Org_ID(lasthu.getAD_Org_ID());
		hu.setCapacity(Env.ONEHUNDRED);
		hu.setDocStatus(MWM_HandlingUnit.DOCSTATUS_Drafted); 
		hu.setName(name);
		hu.setQtyMovement(Env.ZERO);
		hu.saveEx(trxName);   
		log.info("Handling Unit generated: "+hu.getName());
	}
	/**
	 * DocStatus = Drafted
	 * QtyMovement = ZERO
	 */
	public void releaseHandlingUnit(MWM_EmptyStorageLine line) { 
		MWM_HandlingUnit hu =  MWM_HandlingUnit.get(Env.getCtx(), line.getWM_HandlingUnit_ID(), trxName);
		hu.setQtyMovement(Env.ZERO);
		hu.setDocStatus(X_WM_HandlingUnit.DOCSTATUS_Drafted);
		hu.saveEx(trxName);
	}
	
	/**
	 * Handling Unit will be fully released during CompleteIt() of WM_InOut > M_InOut (IsSOTrx='Y')
	 * @param oline
	 * @param empty
	 */
	private void releaseHandlingUnitHistory(MWM_InOutLine oline, MWM_EmptyStorageLine empty) {
		MWM_HandlingUnit hu = (MWM_HandlingUnit) empty.getWM_HandlingUnit();
		if (hu==null){
			log.severe("HandlingUnit not found for EmptyLine at this Locator - "+empty.getWM_EmptyStorage().getM_Locator().getValue());
			return;
		} 
		MWM_HandlingUnitHistory huh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
		+MWM_HandlingUnitHistory.COLUMNNAME_DateEnd+" IS NULL",trxName)
				.setParameters(hu.get_ID())
				.first();
		if (huh==null) {
			log.severe("No Handling Unit to release (DateEnd Not null)");
			return;
		}
		huh.setDateEnd(empty.getUpdated());	
		huh.saveEx(trxName); 
	}
	/**
	 * New way to calculate Available Capacity NOT during EmptyStorageLine creation
	 * but prior, during MWM_InOutLine creation. 
	 * Has to take into account open MWM_InOutLine's picks and puts that points to this Locator.EmptyStorage
	 * Movements will result in InOuts thus open Movements are excluded.
	 * @param empty
	 * @return availableCapacity
	 */
	public BigDecimal getAvailableCapacity(MWM_EmptyStorage empty) {
		BigDecimal availableCapacity = empty.getAvailableCapacity();
		List<MWM_InOutLine> wiolines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_M_Locator_ID+"=?",trxName)
				.setParameters(empty.getM_Locator_ID())
				.setOnlyActiveRecords(true)
				.setOrderBy(MWM_InOutLine.COLUMNNAME_WM_InOut_ID)
				.list();
		if (wiolines==null || wiolines.isEmpty() )
			return availableCapacity;
		MWM_InOut wio = (MWM_InOut) wiolines.get(0).getWM_InOut();
		int wioID = wio.get_ID();// keeping track of changing headers
		for (MWM_InOutLine wioline:wiolines) {
			if (wioID!=wioline.getWM_InOut_ID()) {
				wio = (MWM_InOut) wioline.getWM_InOut();
				wioID = wio.get_ID();
			}
			if (wio.getDocStatus().equals(MWM_InOut.DOCSTATUS_Completed))
				continue;
			BigDecimal convertedQty = convertUOM(wioline.getM_Product_ID(),wioline.getQtyPicked());
			if (wio.isSOTrx())//if outgoing Picking, then available shall increase
				availableCapacity = availableCapacity.add(convertedQty);
			else //if incoming Putaway, then available shall decrease
				availableCapacity = availableCapacity.subtract(convertedQty);
		} 
		return availableCapacity;
	}
	
	private BigDecimal convertUOM(int M_Product_ID,BigDecimal qty) { 
		MUOMConversion highestUOMConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=?",null)
				.setParameters(M_Product_ID)
				.setOrderBy(MUOMConversion.COLUMNNAME_DivideRate+" DESC")
				.first();
		BigDecimal converted = qty.divide(highestUOMConversion==null?qty:highestUOMConversion.getDivideRate(),2,RoundingMode.HALF_EVEN);
		return converted;
	}
	
	/**
	 * 
	 * @param dsline
	 * @param alloted
	 * @param empty
	 * @param inoutline
	 * @return WM_EmptyStorageLine
	 */
	public MWM_EmptyStorageLine newEmptyStorageLine(MWM_DeliveryScheduleLine dsline, BigDecimal alloted, MWM_EmptyStorage empty, MWM_InOutLine inoutline) {
		MWM_EmptyStorageLine storline = new MWM_EmptyStorageLine(Env.getCtx(),0,trxName);
		storline.setWM_EmptyStorage_ID(empty.get_ID());
		storline.setWM_InOutLine_ID(inoutline.get_ID());
		storline.setQtyMovement(alloted);
		storline.setDateStart(inoutline.getUpdated());
		storline.setIsSOTrx(inoutline.getWM_InOut().isSOTrx());
		if (dsline==null) {
			
		}else {
			if (dsline.isReceived())
				storline.setDateStart(dsline.getWM_DeliverySchedule().getDateDelivered());
			else { 
				if (dsline.getWM_DeliverySchedule().getDatePromised()==null)
					throw new AdempiereException("Set Gate/PromisedDate first");
				if (dsline.getWM_DeliverySchedule().getDatePromised().after(Today))
					storline.setDateStart(dsline.getWM_DeliverySchedule().getDatePromised());
				else if (dsline.getC_OrderLine().getDatePromised().after(Today))
					storline.setDateStart(dsline.getC_OrderLine().getDatePromised());
				else
					throw new AdempiereException("DSLine.NotReceived OR NOT FUTURE: NoDatePromised OR Order NoDatePromised");
			//9Mac19 -  Future Forecast is when No DateStart if Not Received Delivery Schedule and No future Promise Date.
			}
		} 
		MProduct product = MProduct.get(Env.getCtx(),inoutline.getM_Product_ID());
		if (product.getGuaranteeDays()>0)
			storline.setDateEnd(TimeUtil.addDays(storline.getCreated(), product.getGuaranteeDays()));
		
		storline.setC_UOM_ID(inoutline.getC_UOM_ID());
		storline.setM_Product_ID(inoutline.getM_Product_ID());
		if (inoutline.getWM_HandlingUnit_ID()>0)
			storline.setWM_HandlingUnit_ID(inoutline.getWM_HandlingUnit_ID());
		storline.saveEx(trxName);
		cnt++;
		System.out.println(cnt+". Created new EmptyStorageLine :"+storline.getQtyMovement()+ " "+storline.getM_Product().getValue()
				+" at Locator "+empty.getM_Locator().getValue());
		return storline;
	}

	/**
	 * 
	 * @param eline
	 * @param newline
	 * @return WM_ESLine
	 */
	public void createESLinePicking(MWM_EmptyStorageLine eline, MWM_EmptyStorageLine newline) {
		//link source SEL and new SEL with WM_ESLine
		MWM_ESLine link = new MWM_ESLine(Env.getCtx(),0,trxName);
		link.setWM_EmptyStorageLine_ID(eline.get_ID());
		link.setValue(Integer.toString(newline.get_ID()));
		link.setProcessed(false);
		link.saveEx(trxName);
	} 
	
	/**
	 * Here it is finally picked 
	 * Check if QtyMovement=0, then Set DateEnd and IsActive=false  
	 * @param oldine
	 * @param picking 
	 */
	public void pickedEmptyStorageLine(BigDecimal picking,MWM_EmptyStorageLine  oldline) {  
		oldline.setQtyMovement(oldline.getQtyMovement().subtract(picking));
		//oldline.setWM_InOutLine_ID(0);//keep as record as its parent WMInOut is Complete = free 
		if (oldline.getQtyMovement().compareTo(Env.ZERO)==0) {
			oldline.setIsActive(false);
			oldline.setDateEnd(Today);
		}
		oldline.saveEx(trxName); 
	}
	
	/**
	 * 
	 * @param empty
	 * @param dline
	 * @return future forecast
	 */
	public BigDecimal getFutureStorage(MWM_EmptyStorage empty, MWM_DeliveryScheduleLine dline){
		BigDecimal forecastStorage =Env.ZERO;
		if (dline.isReceived())
			return Env.ZERO;
		List<MWM_EmptyStorageLine> slines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_DateStart+"<=? AND "
				+MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=?",trxName)
				.setParameters(dline.getWM_DeliverySchedule().getDatePromised(),empty.get_ID())
				.setOnlyActiveRecords(true)
				.list();
		for (MWM_EmptyStorageLine sline:slines){
			if (sline.getDateEnd()!=null && sline.getDateEnd().before(dline.getWM_DeliverySchedule().getDatePromised()))
				continue;
			if (sline.isSOTrx())
				forecastStorage=forecastStorage.subtract(sline.getQtyMovement());
			else 
				forecastStorage=forecastStorage.add(sline.getQtyMovement());
		}
		return forecastStorage;
	}
	
	/**
	 * 
	 * @param dsline
	 * @param empty
	 */
	public void calculatePercentageVacant(boolean received,MWM_EmptyStorage empty) {
		if (!received)
			return;//future, do not want to affect statistics of EmptyStorage
		empty.setPercentage((empty.getAvailableCapacity().divide(empty.getVacantCapacity(),2,RoundingMode.HALF_EVEN)).multiply(Env.ONEHUNDRED));
		//set is Full if 0% vacant
		if (empty.getPercentage().compareTo(Env.ZERO)<=0)
			empty.setIsFull(true);
		else
			empty.setIsFull(false);
		empty.saveEx(trxName);
	}
	
	/**
	 * 
	 * @param inout
	 * @param dsline
	 * @param alloted
	 * @return WM_InOutLine
	 */
	public MWM_InOutLine newInOutLine(MWM_InOut inout, MWM_DeliveryScheduleLine dsline, BigDecimal alloted) {
		MWM_InOutLine inoutline = new MWM_InOutLine(Env.getCtx(),0,trxName);
		inoutline.setWM_InOut_ID(inout.get_ID());
		inoutline.setC_UOM_ID(dsline.getM_Product().getC_UOM_ID());
		inoutline.setC_OrderLine_ID(dsline.getC_OrderLine_ID());
		inoutline.setM_Product_ID(dsline.getM_Product_ID());
		inoutline.setQtyPicked(alloted);
		inoutline.setWM_DeliveryScheduleLine_ID(dsline.get_ID());
		inoutline.setM_AttributeSetInstance_ID(dsline.getM_AttributeSetInstance_ID());
		inoutline.setC_DocType_ID(dsline.getWM_DeliverySchedule().getC_DocType_ID());
		inoutline.saveEx(trxName);
		dsline.setWM_InOutLine_ID(inoutline.get_ID());
		dsline.saveEx(trxName);
		return inoutline;
	}
	
	/**
	 * 
	 * @param inout
	 */
	public void sortFinalList(MWM_InOut inout) {
		//sort inout list according to WH,XYZ
 		String sql = "SELECT wl.WM_InOutLine_ID,l.Value FROM  WM_InOutLine wl,WM_InOut w,M_Locator l "
 				+ "WHERE w.WM_InOut_ID=? AND wl.WM_InOut_ID=w.WM_InOut_ID AND wl.M_Locator_ID=l.M_Locator_ID ORDER BY l.M_Warehouse_ID,l.X,l.Y,l.Z";
		int seq = 1;
		 KeyNamePair[] list = DB.getKeyNamePairs(trxName, sql, false,inout.get_ID());
		for (KeyNamePair line:list){
			int id = line.getKey();
			MWM_InOutLine ioline = new MWM_InOutLine(Env.getCtx(),id,trxName);
			ioline.setSequence(new BigDecimal(seq));
			ioline.saveEx(trxName);
			seq++;			
		} 
	}

	/**
	 * From WM_InOut Complete after creation of Shipment
	 * @param lines
	 */
	public void closeOutbound(List<MWM_InOutLine> lines) {
		if (lines==null)
			throw new AdempiereException("Suddenly WM_InOutLine(s) disappear!");
		for (MWM_InOutLine line:lines){
			MWM_EmptyStorageLine esline = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",trxName)
					.setParameters(line.getWM_InOutLine_ID())
					.first(); 
			MWM_HandlingUnit hu = (MWM_HandlingUnit) line.getWM_HandlingUnit();
			WM_HandlingUnit_ID = hu.get_ID(); 
			releaseHandlingUnitHistory(line, esline);
			releaseHandlingUnit(esline);
		}
	} 
	/**
	 * Remove if not thisWH, if not this org, if not > 0, if not blocked
	 * @param org
	 * @param thisWH
	 * @param elines
	 * @return
	 */
	public List<MWM_EmptyStorageLine> removeOtherWarehouse(boolean org, int WH, List<MWM_EmptyStorageLine> elines) {
		 for (int i=0; i<elines.size();i++) {
			if (!org && elines.get(i).getWM_EmptyStorage().getM_Locator().getM_Warehouse_ID()!=WH) {
				elines.remove(i);
				i--;
			}
			else if (elines.get(i).getQtyMovement().compareTo(Env.ZERO)==0) {
				elines.remove(i); 
				i--;
			}
			else if (elines.get(i).getQtyMovement().compareTo(Env.ZERO)<0) {
				log.warning("Storage Line below ZERO at:"+elines.get(i).getWM_EmptyStorage().getM_Locator().getValue()+" for "+elines.get(i).getM_Product().getValue()+" "
			+elines.get(i).getQtyMovement());
				elines.remove(i); 
				i--;
			}
			else if (elines.get(i).getWM_EmptyStorage().isBlocked()) {
				elines.remove(i);
				i--;
			}else if (org) {
				int t = elines.get(i).getWM_EmptyStorage().getM_Locator().getM_Warehouse().getAD_Org_ID();
				if (t!=Env.getAD_Org_ID(Env.getCtx())) {
					elines.remove(i);
					i--;
				}
			}
		 }
		return elines;
	}
	public List<MWM_EmptyStorageLine> removeOtherOrgBlockedAndZero(boolean org, I_M_Locator locator, List<MWM_EmptyStorageLine> elines) {
		 for (int i=0; i<elines.size();i++) { 
			if (!org && locator!=null && elines.get(i).getWM_EmptyStorage().getM_Locator().getM_Locator_ID()!=locator.getM_Locator_ID()) {
				elines.remove(i);
				i--;
			}else if (elines.get(i).getWM_HandlingUnit_ID()==0) {
					log.warning("Missing Handling Unit for :"+elines.get(i).getQtyMovement()+" "
							+elines.get(i).getM_Product().getValue()+" at "+elines.get(i).getWM_EmptyStorage().getM_Locator().getValue());
					elines.remove(i); 
					i--;
			} else if (elines.get(i).getQtyMovement().compareTo(Env.ZERO)<0) {
				log.warning("Storage Line ZERO or below at:"+elines.get(i).getWM_EmptyStorage().getM_Locator().getValue()+" for "+elines.get(i).getM_Product().getValue()+" "
			+elines.get(i).getQtyMovement());
				elines.remove(i); 
				i--;
			} else if (elines.get(i).getWM_EmptyStorage().isBlocked()) {
				elines.remove(i);
				i--;
			} else if (elines.get(i).getWM_InOutLine_ID()>0) {
				MWM_InOut inout = (MWM_InOut) elines.get(i).getWM_InOutLine().getWM_InOut();
				if (!inout.isSOTrx()) 
					continue;
				else if (inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Completed)
						||inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Closed)
						||inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Voided)
						||inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Reversed))  
					continue;
				log.warning(elines.get(i).getQtyMovement()+" "+elines.get(i).getM_Product().getValue()
						+" "+elines.get(i).getWM_HandlingUnit().getName()+" HandlingUnit Storage Line held up by"+inout.getName());
				elines.remove(i);
				i--;
			}else if (org) {
				int t = elines.get(i).getWM_EmptyStorage().getM_Locator().getM_Warehouse().getAD_Org_ID();
				if (t!=Env.getAD_Org_ID(Env.getCtx())) {
					elines.remove(i);
					i--;
				}
			}
		 }
		return elines;
	}
	 
	public List<MWM_EmptyStorageLine> removeBiggerBoxSize(List<MWM_EmptyStorageLine> elines,BigDecimal boxConversion) {
		for (int i=0; i<elines.size();i++) { 
			if (elines.get(i).getQtyMovement().compareTo(boxConversion)>0) {
				elines.remove(i);
				i--;
			}
		}
		return elines;
	}
}
