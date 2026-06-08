/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/
package org.wms.model;

import java.io.File;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import java.util.logging.Level;

import org.wms.component.WM_EmptyStorageLineDocEvent;
import org.wms.model.X_WM_InOut;
import org.wms.process.Utils;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;

import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.ReportEngine;

import org.compiere.process.DocumentEngine;

import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

/**
 * During Completion, DeliverySchedule must be Received to proceed. 
 * WM_EmptyStorage Vacant and PercentageAvailable are updated
 * WM_EmptyStorageLines are affected
 * M_InOut Shipment/Receipt OR M_Movement is created
 * @author red1
 *
 */
public class MWM_InOut extends X_WM_InOut implements DocAction {
	public MWM_InOut(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);

		if (id==0){
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
			setProcessed(false);
		}
	}

	public MWM_InOut(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	private static final long serialVersionUID = 1L;
	
	/**	Process Message 			*/
	private String			m_processMsg = null;

	private boolean			m_justPrepared = false;


	private BigDecimal packFactor=Env.ONE;
	private BigDecimal boxConversion=Env.ONE;
	private BigDecimal currentUOM=Env.ONE;
	
	protected boolean beforeSave (boolean newRecord)
	{
		return super.beforeSave(newRecord);
	}

	protected boolean beforeDelete() {	 
		return super.beforeDelete();
	}

	protected boolean afterSave (boolean newRecord, boolean success)
	{
		return super.afterSave(newRecord, success);
	}
 
	protected boolean afterDelete(boolean success) {
		return super.afterDelete(success);
	}
  
 
	public boolean processIt(String processAction) throws Exception {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}
 
	public boolean unlockIt() {
		if (log.isLoggable(Level.INFO)) 
			log.info("unlockIt - " + toString());
		return true;
	}
 
	public boolean invalidateIt() {
		if (log.isLoggable(Level.INFO)) log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}
	/**
	 * Integrated to Mobile Scanner on Warehouse Floor
	 * Any update of different Handling Unit or Qty or Locator to be reflected for
	 * end processing onto EmptyStorageLine at CompleteIt() reoutine
	 * 1. Remove linked EmptyStorageLine.WM_InOutLine_ID and update it to scanned
	 */
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
	
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);

		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//go thru each WIOLine and check ESLine
		List<MWM_InOutLine>wiolines=new Query(Env.getCtx(), MWM_InOutLine.Table_Name, MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?", get_TrxName())
				.setParameters(get_ID())
				.list();
		for (MWM_InOutLine wioline:wiolines) {
			MWM_EmptyStorageLine esline = new Query(Env.getCtx(), MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
					.setParameters(wioline.get_ID())
					.first();
			if (esline==null) { 
				if (isSOTrx()) {
					MWM_EmptyStorage empty = null;
					MWM_EmptyStorageLine eline = new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_HandlingUnit_ID+"=?",get_TrxName())
							.setParameters(wioline.getWM_HandlingUnitOld_ID()>0?wioline.getWM_HandlingUnitOld_ID():wioline.getWM_HandlingUnit_ID())
							.first();
					if (eline!=null) {
						empty = new Query(getCtx(), MWM_EmptyStorage.Table_Name, MWM_EmptyStorage.COLUMNNAME_WM_EmptyStorage_ID+"=?",get_TrxName())
								.setParameters(eline.getWM_EmptyStorage_ID())
								.first();
					}
					throw new AdempiereException("No EmptyStorageLine in "+wioline.getSequence()+". "+wioline.getQtyPicked()
					+" "+wioline.getM_Product().getValue()+" at WioLine:"+wioline.get_ID()+" "+wioline.getM_Locator().getValue()
					+" HU:"+(wioline.getWM_HandlingUnitOld_ID()>0?wioline.getWM_HandlingUnitOld().getName():wioline.getWM_HandlingUnit().getName())
					+" at Storage "+(empty==null?"NULL":empty.getM_Locator().getValue()));
				}
				else { 
					; //OK. Note that putaway is still unassigned, as create new ESLine happens during CompleteIt()
				}
			}
			if ((esline!=null && wioline.getWM_HandlingUnitOld_ID()>0) &&
					(esline.getWM_HandlingUnit_ID()!=wioline.getWM_HandlingUnitOld_ID())) {
				changeEmptyStorageLine(wioline,esline);
			}
			if (wioline.getM_LocatorOld_ID()>0)
				changeLocator(wioline);
			
			if (wioline.getWM_HandlingUnit_ID()<1 && wioline.getWM_HandlingUnitOld_ID()<1)
				throw new AdempiereException("Pick/Putaway Line NOT assigned Handling Unit.");
		}
		
 		m_justPrepared = true;

		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);

		return DocAction.STATUS_InProgress;

	}
	/**
	 * During Putaway only, allow force over capacity.
	 * Update originating Movement document
	 * No ESLine to handle (as it is handled during WMInOut.CompleteIt()
	 * @param wioline
	 * @param esline
	 */
	private void changeLocator(MWM_InOutLine wioline) {
		 if (wioline.getM_Locator_ID()==wioline.getM_LocatorOld_ID()) {
			 log.warning("Locator changed to same!");
			 return;
		 }
		 MMovementLine movedline = new MMovementLine(getCtx(), wioline.getM_MovementLine_ID(), get_TrxName());
		 if (movedline==null)
			 throw new AdempiereException("Change Locator - Putaway Line lost its orignating MovementLine :"+wioline.getQtyPicked()+" "+wioline.getM_Product().getValue());
		 
		 //9 Item Putaway Locator changed from [old locator] to [new locator]
		 System.out.println(wioline.getQtyPicked()+" "+wioline.getM_Product().getValue()+" Putaway Locator changed from "+movedline.getM_LocatorTo().getValue()+" to "+wioline.getM_Locator().getValue());
		 
		 movedline.setM_LocatorTo_ID(wioline.getM_Locator_ID());
		 movedline.saveEx(get_TrxName());
	}

	/**
	 * Change to EmptyStorageLine picked by WMInOutLine as per HandlingUnit
	 * Can change Qty (breakup) but not Locator
	 * @param wioline
	 * @return
	 */
	private boolean changeEmptyStorageLine(MWM_InOutLine wioline, MWM_EmptyStorageLine eline) {
		//if still left empty by floor, then user has to manually key in first.
		if (wioline.getWM_HandlingUnit_ID()==0)
			throw new AdempiereException("Key in new Handling Unit for "+wioline.getWM_HandlingUnitOld().getName());				
		// find Eline with changed HU ID
		MWM_EmptyStorageLine cline = new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_HandlingUnit_ID+"=? ", get_TrxName())
				.setParameters(wioline.getWM_HandlingUnit_ID())
				.setOnlyActiveRecords(true)
				.first();
		if (cline==null) {
			if (wioline.getWM_InOut().isSOTrx())
				throw new AdempiereException("Changed Item HandlingUnit is not found in EmptyStorage");
			else
				return true;
		}
		//not allow change locator during picking but allow during putaway
		if (wioline.getWM_InOut().isSOTrx() && wioline.getM_Locator_ID()!=cline.getWM_EmptyStorage().getM_Locator_ID())
			throw new AdempiereException("Not same Locator in changed HandlingUnit");
		if (eline.isWMInOutLineProcessed()) 
			throw new AdempiereException("This StorageLine has pending Pick/Put record NOT CLOSED NOR COMPLETE");
		
		if (wioline.getQtyPicked().compareTo(cline.getQtyMovement())>0)  
			throw new AdempiereException("Picking line is more than Storage box/line");
		cline.setWM_InOutLine_ID(wioline.get_ID());
		cline.saveEx(get_TrxName());
		System.out.println("Picking Changed HandlingUnit "+eline.getWM_HandlingUnit().getName()+" to "+wioline.getWM_HandlingUnit().getName());
		return true;
	}
	
	private void saveM_InOut(MInOut inout,List<MWM_InOutLine> lines) {
		if (inout.getC_Order_ID()>0)
			return;
		MOrder order = (MOrder) lines.get(0).getC_OrderLine().getC_Order();
		inout.setIsSOTrx(order.isSOTrx());
		inout.setC_Order_ID(order.getC_Order_ID());
		inout.setC_BPartner_ID(order.getC_BPartner_ID());
		inout.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());
		inout.setM_Warehouse_ID(lines.get(0).getM_Locator().getM_Warehouse_ID());
		inout.setC_Project_ID(order.getC_Project_ID());
		inout.setMovementDate(lines.get(0).getUpdated());
		if (inout.isSOTrx())
			inout.setMovementType(MInOut.MOVEMENTTYPE_CustomerShipment);
		else
			inout.setMovementType(MInOut.MOVEMENTTYPE_VendorReceipts);
		inout.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));
		inout.setDocAction(DOCACTION_Prepare);
		inout.setC_DocType_ID();
		inout.setDateOrdered(order.getDateOrdered());
		inout.setDateReceived(lines.get(0).getWM_DeliveryScheduleLine().getWM_DeliverySchedule().getDateDelivered());
		inout.setPOReference(order.getPOReference());
		inout.saveEx(get_TrxName()); //save previous one before new one
	}

 	public boolean approveIt() {
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());

		setIsApproved(true);

		return true;

	}

 	public boolean rejectIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		setIsApproved(false);

		return true;

	}

 	public String completeIt() {
		//	Just prepare
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		MBPartner partner = (MBPartner) getC_BPartner();
		//Handling Stock Movements
		if (getName().startsWith("Stock Movement")
				||getName().startsWith("Inventory Replenishment")
				||getName().startsWith("Consignment")) {
			//check if Picking is done first before Putaway
			 if (!isSOTrx()) {
				 //get Picking
				 MWM_InOut pick = new Query(getCtx(), MWM_InOut.Table_Name, MWM_InOut.COLUMNNAME_Name+"=? AND "
						 +MWM_InOut.COLUMNNAME_IsSOTrx+"=?",get_TrxName())
						 .setParameters(getName(),true)
						 .first();
				 if (pick==null)  
					 log.warning("Picking not found during Putaway of "+getName());
				 else {
					 if (!pick.getDocStatus().equals(MWM_InOut.DOCSTATUS_Completed)) {
						 throw new AdempiereException("Complete Picking first :"+getName());
					 }
				 }  
			 }
			Utils util= new Utils(get_TrxName());
			
			 List<MWM_InOutLine>wiolines=new Query(getCtx(), MWM_InOutLine.Table_Name, MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",get_TrxName())
					 .setParameters(get_ID())
					 .list();
			 if (wiolines.isEmpty())
				 throw new AdempiereException("No WM InOut Lines for "+getName());
			 for (MWM_InOutLine wioline:wiolines) {
				 processWMSStorage(wioline, null, util);
			 }
			 //complete associated Movement AND Picking if Putaway is done first
			 if (!isSOTrx()) {
				 MMovement move = (MMovement) getM_Movement();	
				 if (move!=null && !move.getDocStatus().equals(MMovement.STATUS_Completed)) {
					MDocType dt = MDocType.get(getCtx(), move.getC_DocType_ID());
					if (!MPeriod.isOpen(getCtx(), move.getMovementDate(), dt.getDocBaseType(), getAD_Org_ID()))
						{
							m_processMsg = "@PeriodClosed@";
							return DocAction.STATUS_Invalid;
						}
					 move.setDocStatus(DOCSTATUS_InProgress);
					 move.setDocAction(DocAction.ACTION_Complete);
					 move.processIt(DocAction.ACTION_Complete);
					 move.saveEx(get_TrxName());
					 log.info("Movement also completed: "+move.getDescription());	 
				 }else
					 log.warning("Movement not found to auto complete :"+getName());
			}
		} else {
			Utils util = new Utils(get_TrxName());
			//Create Material Receipt process    
			MInOut inout = null;
			List<MWM_InOutLine> lines = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",get_TrxName())
					.setParameters(this.get_ID()).list();
			//holder for separate M_InOut according to different C_Order
			int c_Order_Holder = 0;
			for (MWM_InOutLine wioline:lines){
				
				MWM_DeliveryScheduleLine del = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_WM_DeliveryScheduleLine_ID+"=?",get_TrxName())
						.setParameters(wioline.getWM_DeliveryScheduleLine_ID())
						.first();
				if (del!=null && !del.isReceived())
					throw new AdempiereException("DeliverySchedule Line still not Received"); //still not processed at DeliverySchedule level, so no Shipment/Receipt possible
				if (wioline.getM_InOutLine_ID()>0)
					throw new AdempiereException("Already has Shipment/Receipt record!");//already done before
				if (del!=null && del.getC_OrderLine().getC_Order_ID()!=c_Order_Holder){
					if (inout!=null){
						saveM_InOut(inout,lines);
						inout.setDescription(isSOTrx()?"Picking":"Putaway");
						inout.setDocStatus(this.DOCSTATUS_InProgress);
						inout.setDocAction(this.DOCACTION_Complete);
						inout.processIt(DocAction.ACTION_Complete);
						inout.saveEx(get_TrxName());
					}
					//create new MInOut  as C_Order_ID has changed
					inout = new MInOut(Env.getCtx(),0,get_TrxName());
					saveM_InOut(inout,lines);
					c_Order_Holder = del.getC_OrderLine().getC_Order_ID();
				}
				if (inout==null)
					inout = new MInOut(Env.getCtx(),0,get_TrxName());
				processWMSStorage(wioline,del,util);
				
				MInOutLine ioline = new MInOutLine(inout);
				ioline.setC_OrderLine_ID(wioline.getC_OrderLine_ID());
				ioline.setM_Product_ID(wioline.getM_Product_ID());
				ioline.setM_AttributeSetInstance_ID(wioline.getM_AttributeSetInstance_ID());
				ioline.setC_UOM_ID(wioline.getC_UOM_ID());
				ioline.setM_Warehouse_ID(wioline.getM_Locator().getM_Warehouse_ID());
				ioline.setM_Locator_ID(wioline.getM_Locator_ID());
				ioline.setQtyEntered(wioline.getQtyPicked());
				ioline.setMovementQty(wioline.getQtyPicked());
				ioline.saveEx(get_TrxName());		
				//populate back WM_InOutLine with M_InOutLine_ID
				wioline.setM_InOutLine_ID(ioline.get_ID());ioline.getM_Locator();ioline.getM_Warehouse_ID();
				wioline.saveEx(get_TrxName());
				//if Sales' Shipment, then release the Handling Unit <--deprecated
				MWM_HandlingUnit hu = MWM_HandlingUnit.get(getCtx(), wioline.getWM_HandlingUnit_ID(), get_TrxName());
				if (hu!=null) {
					//deactivate HandlingUnit history
					MWM_HandlingUnitHistory huh = new Query(Env.getCtx(),MWM_HandlingUnitHistory.Table_Name,MWM_HandlingUnitHistory.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
							+MWM_HandlingUnitHistory.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
							.setParameters(hu.get_ID(),wioline.get_ID())
							.first();
					if (huh==null){
							log.severe("HandlingUnit has no history: "+wioline.getWM_HandlingUnit().getName());
							continue;
					}
					if (huh.getDateEnd()==null){
						log.warning("HandlingUnit history has no DateEnd during Receive of DeliverySchedule: "+wioline.getWM_HandlingUnit().getName());
						huh.setDateEnd(hu.getUpdated());
					}
					huh.setIsActive(false);
					huh.saveEx(get_TrxName());
				}
				//check if has previous BackOrder that is not complete (no QtyDelivered value) so disallow any new BackOrders 
				//check if has previous WM_InOut (backorder case) and if QtyDelivered then error of premature process
				MWM_DeliveryScheduleLine prevDsLine = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_C_OrderLine_ID+"=?"
						+ " AND "+MWM_DeliveryScheduleLine.COLUMNNAME_IsBackOrder+"=? "
								+ "AND "+MWM_DeliveryScheduleLine.COLUMNNAME_Received+"=?"
										+ " AND "+MWM_DeliveryScheduleLine.COLUMNNAME_Created+"<?",get_TrxName())
						.setParameters(del.getC_OrderLine_ID(),"Y","Y",del.getCreated())
						.setOrderBy(COLUMNNAME_Created+ " DESC")
						.first(); 
	 			
				//check if old backorder needs to reset
				//get C_Orderline, check if Delivered=Ordered
				MOrderLine orderline = new Query(Env.getCtx(),MOrderLine.Table_Name,MOrderLine.COLUMNNAME_C_OrderLine_ID+"=?",get_TrxName())
							.setParameters(del.getC_OrderLine_ID())
							.first();
				if (orderline!=null){
					//the prev DS Line backorder has to be updated by this new DS Line
					if (prevDsLine!=null){
						prevDsLine.setQtyDelivered(prevDsLine.getQtyDelivered().add(del.getQtyOrdered()));
						if (prevDsLine.getQtyDelivered().compareTo(prevDsLine.getQtyOrdered())==0) 
							prevDsLine.saveEx(get_TrxName());
					} 
				} 
			}
			if (inout!=null){
				saveM_InOut(inout,lines);		
				inout.setDescription(isSOTrx()?"Picking":"Putaway");
				inout.setDocStatus(this.DOCSTATUS_InProgress);
				inout.setDocAction(this.DOCACTION_Complete);
				inout.processIt(DocAction.ACTION_Complete);
				inout.saveEx(get_TrxName());
			}
		}
		
		//	Implicit Approval
		if (!isApproved())
			approveIt(); 
		
		if (log.isLoggable(Level.INFO)) log.info(toString());

		StringBuilder info = new StringBuilder();
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);

		if (valid != null)
		{
			if (info.length() > 0)
				info.append(" - ");
			info.append(valid);
			m_processMsg = info.toString();
			return DocAction.STATUS_Invalid;
		}
		setProcessed(true);
		m_processMsg = info.toString();
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	 {
	 	String ds = getDocStatus();
	 	return DOCSTATUS_Completed.equals(ds) 
	 		|| DOCSTATUS_Closed.equals(ds)
	 		|| DOCSTATUS_Reversed.equals(ds);
	 }
	//	isComplete
	
	public boolean voidIt() {
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
		voidingIt();
		setDocStatus(DOCSTATUS_Voided);
 		setProcessed(true);
		setDocAction(DOCACTION_None);
		saveEx(get_TrxName());
		return true;
	}

 	private void voidingIt() {
		if (!getDocStatus().equals(DocAction.STATUS_Completed) && !getDocStatus().equals(DocAction.STATUS_InProgress)
				&& !getDocStatus().equals(DocAction.STATUS_Drafted))
			return;
		boolean reversedCore = false;
 		List<MWM_InOutLine>wiolines = new Query(getCtx(), MWM_InOutLine.Table_Name, MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?", get_TrxName())
				.setParameters(get_ID())
				.list();
		for (MWM_InOutLine wioline:wiolines) {
			if (getM_Movement_ID()<1 && !reversedCore) {
				MInOut inout = (MInOut) wioline.getM_InOutLine().getM_InOut();
				if (inout.getLines().length>0) {
					if (inout.getDocStatus().equals(DocAction.STATUS_Completed)) {
						inout.setDocAction(MInOut.DOCACTION_Reverse_Correct);
						inout.processIt(MInOut.DOCACTION_Reverse_Correct);
						inout.saveEx(get_TrxName());
					}
					else
						log.severe((isSOTrx()?"Shipment ":"Material Receipt ")+inout.getDescription()+" Not Complete Status. Cannot Reverse.");
				}
				reversedCore=true;
			}	
			MWM_EmptyStorageLine eline = new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
					.setParameters(wioline.get_ID())
					.first();
			MWM_DeliveryScheduleLine dsline = new Query(getCtx(), MWM_DeliveryScheduleLine.Table_Name, MWM_DeliveryScheduleLine.COLUMNNAME_WM_InOutLine_ID+"=?",get_TrxName())
					.setParameters(wioline.get_ID())
					.first();
			if (dsline!=null) {
				dsline.setWM_InOutLine_ID(0);
				dsline.saveEx(get_TrxName());
				log.warning("DeliverySchedule Line Cleared:"+dsline.getQtyOrdered()+" "+dsline.getM_Product().getValue());
			}
			if(getDocStatus().equals(DOCSTATUS_InProgress) || 
					getDocStatus().equals(DOCSTATUS_Drafted)) {
				if (eline!=null) {
					eline.setWM_InOutLine_ID(0);
					eline.saveEx(get_TrxName());
					log.info((isSOTrx()?"Picking ":"Putaway ")+" removed from StorageLine "+wioline.getQtyPicked()+" "+wioline.getM_Product().getValue());
				}else
					log.warning((isSOTrx()?"Picking ":"Putaway ")+wioline.getQtyPicked()+" InProgress ** "+wioline.getM_Product().getValue()+" ** No StorageLine");
				continue;
				// ************* RETURN FOR IN PROGRESS PICKING/PUTAWAY ** NO STORAGE CHANGE NEEDED
			}
			//Completed Picking/Putaway				
			MWM_EmptyStorage empty = new Query(Env.getCtx(), MWM_EmptyStorage.Table_Name, MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
			.setParameters(wioline.getM_Locator_ID())
			.first();
			if (eline==null) {
				log.warning("StorageLine not found by Picking Line:"+wioline.getQtyPicked()+" "+wioline.getM_Product().getValue());

				//try old handling unit
				if (wioline.getWM_HandlingUnitOld_ID()>0) {
					eline = new Query(Env.getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
							+MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=?", get_TrxName())
							.setParameters(wioline.getWM_HandlingUnitOld_ID(),empty.get_ID())
							.first();
					log.info("Origin Handling Unit:"+wioline.getWM_HandlingUnitOld().getName());
				}else {
					eline = new Query(Env.getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_HandlingUnit_ID+"=? AND "
							+MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=?", get_TrxName())
							.setParameters(wioline.getWM_HandlingUnit_ID(),empty.get_ID())
							.first();
					log.info("Intact Handling Unit "+wioline.getWM_HandlingUnit().getName());
				}
			}
			if (eline==null) {
				log.warning("No StorageLine via Handling Unit:"+wioline.getWM_HandlingUnit().getName());
				//find another Eline from same locator to restore.
				eline=new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "
						+MWM_EmptyStorageLine.COLUMNNAME_WM_EmptyStorage_ID+"=? AND "
						+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">=?",get_TrxName())
						.setParameters(wioline.getM_Product_ID(),empty.get_ID(),wioline.getQtyPicked())
						.first();
				if (eline==null) {
					log.severe("FATAL !! can't restore:"+wioline.getQtyPicked()+" "+wioline.getM_Product().getValue()+" at "+empty.getM_Locator().getValue());
					continue;
				}
				log.info("Found finally at HandlingUnit "+eline.getWM_HandlingUnit().getName()+" "+eline.getQtyMovement()+" "+eline.getM_Product().getValue());
			}
			if (isSOTrx()) {
				log.info("Picking Storage at "+eline.getWM_EmptyStorage().getM_Locator().getValue()+" restored by "+wioline.getQtyPicked()+" "+eline.getM_Product().getValue());
				eline.setQtyMovement(eline.getQtyMovement().add(wioline.getQtyPicked()));
				eline.setIsActive(true); 
				eline.saveEx(get_TrxName());
			}else {
				log.info("Putaway Storage deleted "+eline.getQtyMovement()+" "+eline.getM_Product().getValue());
				eline.delete(true);
			}
	 		setDocStatus(DOCSTATUS_Reversed);
		}
	}

	public boolean closeIt() {
		if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;
 		setProcessed(true);
		setDocAction(DOCACTION_None);
		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;
		return true;
	}

  	public boolean reverseCorrectIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
 		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		boolean voided = voidIt();
		setDocStatus(DOCSTATUS_Reversed);
 		return voided;
	}

 	public boolean reverseAccrualIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
 		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
 		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed);
		//	 may come from void
		setDocAction(DOCACTION_None);
		return true;
	}

 	public boolean reActivateIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;
 		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
 		setDocAction(DOCACTION_Complete);
		setProcessed(false);
		return true;
	}

 	public String getSummary() {
		return null;
	}

 	public String getDocumentInfo() {
		return null;
	}

 	public File createPDF() {
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}

 	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getWM_InOut_ID());
		if (re == null)
			return null;
		return re.getPDF(file);
	}
	//	createPDF
 	public String getProcessMsg() {
		return m_processMsg;
	}

 	public int getDoc_User_ID() {
		return 0;
	}

 	public int getC_Currency_ID() {
		return 0;
	}

 	public BigDecimal getApprovalAmt() {
		return null;
	}
 	/**
 	 * WM_EmptyStorage is processed here to affect Vacant Storage Qty and Percentage Available
 	 * @param iolineID
 	 * @param dsline
 	 */
 	private void processWMSStorage(MWM_InOutLine wioline,MWM_DeliveryScheduleLine dsline,Utils util) {
		if (dsline!=null && !dsline.isReceived())
			throw new AdempiereException("DeliveryLine not Received. Complete its DeliverySchedule first.");
		else {
			if (wioline==null)
				throw new AdempiereException("WMS InOutLine lost!");
			BigDecimal eachQty=uomFactors(wioline, Env.ZERO);
			MWM_EmptyStorage storage = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(wioline.getM_Locator_ID())
					.first();
			if (storage==null && getName().startsWith("Consignment")) {
				//create EmptyStorage for Consignee 
				MWM_EmptyStorage empty = new MWM_EmptyStorage(getCtx(), 0, get_TrxName());
				empty.setM_Locator_ID(wioline.getM_Locator_ID());
				empty.setVacantCapacity(Env.ONEHUNDRED);
				empty.saveEx(get_TrxName());
				storage=empty;
				log.warning("Creating EmptyStorage for Consignee at Locator: "+wioline.getM_Locator().getValue());
			}
			if (wioline.getWM_InOut().isSOTrx()) { //Picking OutBound Sales 
				BigDecimal picked = wioline.getQtyPicked().divide(boxConversion,2,RoundingMode.HALF_EVEN); 			
				BigDecimal vacancy = storage.getAvailableCapacity().add(picked); 
				storage.setAvailableCapacity(vacancy);
				MWM_EmptyStorageLine esline = new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_InOutLine_ID+"=?", get_TrxName())
						.setParameters(wioline.get_ID())
						.first();
				if (esline.getWM_EmptyStorage_ID()!=storage.get_ID()||esline.getM_Product_ID()!=wioline.getM_Product_ID())
					throw new AdempiereException("EmptyStorageLine not same Product and Locator as Pick/Put Line ESLINE:"+esline.get_ID()
					+" WIOLINE:"+wioline.get_ID()+" "+wioline.getQtyPicked()+" "+wioline.getM_Product().getValue());
				util.pickedEmptyStorageLine(eachQty, esline);
			}
			else { 	//Putaway InBound Purchases
				MProduct product = MProduct.get(getCtx(), wioline.getM_Product_ID());
				MWM_EmptyStorageLine newESLine = util.newEmptyStorageLine(dsline, wioline.getQtyPicked(), storage, wioline);
				if (product.getGuaranteeDays()>0)
					newESLine.setDateEnd(TimeUtil.addDays(wioline.getUpdated(), product.getGuaranteeDays()));	
				storage.setAvailableCapacity(storage.getAvailableCapacity().subtract(wioline.getQtyPicked().divide(boxConversion,2,RoundingMode.HALF_EVEN)));
				if (wioline.getWM_HandlingUnit_ID()<1)
					throw new AdempiereException("Putaway has no HandlingUnit");
				newESLine.setWM_HandlingUnit_ID(wioline.getWM_HandlingUnit_ID());
				newESLine.saveEx(get_TrxName());
			}
			if (dsline==null)
				util.calculatePercentageVacant(true,storage);
			else
				util.calculatePercentageVacant(dsline.isReceived(),storage);
		//TODO IsActive = N when DeliverySchedule.DocStatus='CO' and IsSOTrx 
			log.info("Processed InoutLine:"+wioline.toString());
		}
 	}
 	
	private BigDecimal uomFactors(MWM_InOutLine line, BigDecimal balance) {
		boxConversion=Env.ONE;
		BigDecimal picked = line.getQtyPicked();//.multiply(new BigDecimal(product.getUnitsPerPack()));

		//Current = current UOM Conversion Qty	
		MUOMConversion currentuomConversion = new Query(Env.getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=? AND "
				+MUOMConversion.COLUMNNAME_C_UOM_To_ID+"=?",null)
				.setParameters(line.getM_Product_ID(),line.getC_UOM_ID())
				.first();
		if (currentuomConversion!=null)
			currentUOM = currentuomConversion.getDivideRate();
		BigDecimal eachQty=picked.multiply(currentUOM);
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
			boxConversion=picked;//avoid non existent of box type, making each line a box by default
		return eachQty;
		} 
}

