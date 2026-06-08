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
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_M_Locator;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_HandlingUnitHistory;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.MWM_StorageType;
	/**
	 * THIS ROUTINE IS DEPRECATED BY REPLENISH MOVEMENT WHICH HAS INTELLIGENT ZONING 
	 * This Stock Movement merely takes the selection and move lock stock barrel to a specified locator
	 * without consideration of capacity (resolves thru manual decision) 
	 * No Delivery Schedule. No use of fresh Handling Units.
	 * Only Movement header done in draft mode and linked for user to open,
	 * Prepare shall create automatically Picking/Putaway lists for mobile scanner to pick up
	 * Completing the Pick/Putaway lists shall effect the EmptyStorage details.
	 * @author red1
	 * @version 1.0
	 */
	public class StockMovement extends SvrProcess {

	private int WM_HandlingUnit_ID = 0; 
	private BigDecimal Percent = Env.ZERO; 
	private BigDecimal QtyMovement = Env.ZERO;  
	private int M_Locator_ID = 0; 
	private int WM_Type_ID = 0;  
	private int done=0;
	private boolean IsSameDistribution=false;
	private boolean IsSameLine=false;
	private boolean movement=false; 
	private String trxName = "";
	private MBPartner partner = null;
	private int M_Warehouse_ID = 0;
	private boolean IsPutaway;
	private boolean IsPicking;
	MMovement move = null;
	
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_HandlingUnit_ID")){
					WM_HandlingUnit_ID = p.getParameterAsInt();
			}
				else if(name.equals("Percent")){
					Percent = p.getParameterAsBigDecimal();
			}
				else if(name.equals("QtyMovement")){
					QtyMovement = p.getParameterAsBigDecimal();
			} 
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID = p.getParameterAsInt();
			}	
				else if(name.equals("WM_Type_ID")){
				WM_Type_ID = p.getParameterAsInt();
			}	
				else if(name.equals("IsSameDistribution")){
				IsSameDistribution = p.getParameterAsBoolean();
			}
				else if(name.equals("IsPicking")){
				IsPicking = p.getParameterAsBoolean();
			}
				else if(name.equals("IsPutaway")){
				IsPutaway = p.getParameterAsBoolean();
			}
				else if(name.equals("IsSameLine")){
				IsSameLine = p.getParameterAsBoolean();
			} else if (name.equals("M_Warehouse_ID")) {
				M_Warehouse_ID =p.getParameterAsInt();
			}
		}
	}
	MWM_HandlingUnit hu = null;
	int storTypeCounter = 0;
	List<MWM_StorageType> stortypes = null;
	MWM_DeliverySchedule delivery = null; //init for Material Movement at end 
	String whereClause = "";
	List<MWM_EmptyStorageLine> selection = null;
	MWM_InOutLine ioline = null;
	MWM_DeliveryScheduleLine dline = null;
	MWM_EmptyStorage source = null;
	MProduct product = null;
	MWM_EmptyStorage target = null;
	BigDecimal balance = Env.ZERO;
	Utils util = null;
	private boolean checked=false;
	
	private void setTrxName() {
		trxName = get_TrxName();
	}

	protected String doIt() {
		if (M_Locator_ID+M_Warehouse_ID==0)
			throw new AdempiereException("Come on lah beb. Make life easy. Send to where? Choose Warehouse (that has default) or Locator");
		setTrxName();
		util = new Utils(trxName);
		util.setHandlingUnit(WM_HandlingUnit_ID);
		checkParams();
		setTargetToLocator();
		//HandlingUnit to split the storage contents 
		selection = selectionFromInfoWindow();
		if (partner==null) {
			partner = new Query(getCtx(), MBPartner.Table_Name, MBPartner.COLUMNNAME_Name+"=?", get_TrxName())
					.setParameters("Standard")
					.first();
			if (partner==null)
				throw new AdempiereException("Create Standard BPartner first");
		} 
		for (MWM_EmptyStorageLine line:selection){ 
			if (!line.isActive())
				continue;
			//Product
			product = MProduct.get(getCtx(), line.getM_Product_ID());
			//source Storage
			source = (MWM_EmptyStorage)line.getWM_EmptyStorage(); 
			//goto target to fit available pack Qty
			mainRoutine(line);
			done++;
		}
		if (movement)
			return "Lines done: "+done;
		else 
			return "Nothing Moved";
	} 

	private void mainRoutine(MWM_EmptyStorageLine line) {  
		//Force movement - no need to check Capacity. Solve elsewhere by user. 
 		createMovementSet(line);	 
	}

	private void setTargetToLocator() {		
		//search by Warehouse
		if (M_Locator_ID>0) {
			target = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
					.setParameters(M_Locator_ID).first();
		} else if (M_Warehouse_ID>0) {
			MWarehouse warehouse = MWarehouse.get(getCtx(), M_Warehouse_ID);
			partner = new Query(Env.getCtx(),MBPartner.Table_Name,MBPartner.COLUMNNAME_Name+"=?",trxName)
					.setParameters(warehouse.getName())
					.first();
			if (partner==null)
				partner=new Query(Env.getCtx(),MBPartner.Table_Name,MBPartner.COLUMNNAME_Name+"=?",trxName)
						.setParameters("Standard")
						.first();
			X_M_Locator locator = new Query(Env.getCtx(),X_M_Locator.Table_Name,X_M_Locator.COLUMNNAME_M_Warehouse_ID+"=? AND IsDefault='Y'",trxName)
					.setParameters(M_Warehouse_ID)
					.first();
			if (locator==null && M_Locator_ID==0)
				locator=warehouse.getDefaultLocator();
			target = new Query(Env.getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",trxName)
					.setParameters(locator.get_ID()).first();
			M_Locator_ID = target.getM_Locator_ID();
		} 
		if (target==null ) {
			//create EmptyStorage for Consignee 
			MWM_EmptyStorage empty = new MWM_EmptyStorage(getCtx(), 0, get_TrxName());
			empty.setM_Locator_ID(M_Locator_ID);
			empty.setVacantCapacity(Env.ONEHUNDRED);
			empty.saveEx(get_TrxName());
			target=empty;
			System.out.println("Creating EmptyStorage for Consignee at Locator: "+empty.getM_Locator().getValue());
		}
		if (target.isBlocked())
			throw new  AdempiereException("Locator is Blocked. Unblock first.");
		if (target.isFull())
			throw new AdempiereException("Locator is Full. Select another Locator.");
	}

	private void checkParams() {
		if (WM_Type_ID>0)
			throw new AdempiereException("Type use is temporariy deprecated by advanced Replenishment Movement");
	}

	private List<MWM_EmptyStorageLine> selectionFromInfoWindow() {
		whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_EmptyStorageLine.WM_EmptyStorageLine_ID)";
		List<MWM_EmptyStorageLine> lines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,whereClause,trxName)
		.setParameters(getAD_PInstance_ID())
		.list();
		return lines;
	}

	/*	This should be done from Cockpit BackOrder Management
	 * 	Create DS, WIO, Move docs
	 * 	WS marked as Received as WM InOut already generated to pick from locators
	 * 	
	 * 	related WM InOut set without DeliverySchedule
	 *	moveline.setM_Locator_ID(ioline.getM_Locator_ID());
	 *	moveline.setM_LocatorTo_ID(M_Locator_ID);
	 */
	private void createMovementSet(MWM_EmptyStorageLine line) {
		//check if core M_InOut exist, then create a Material Movement record. 
		String name = "Stock Movement To "+target.getM_Locator().getValue();
		if (movement==false){
			move = new MMovement(getCtx(),0,trxName);
			move.setC_BPartner_ID(partner.get_ID());
			move.setDescription(name); 
			//About MMovement ID to WM InOut .. workaround by matching Move.Description to WIO.Name.
			move.saveEx(trxName);
			movement=true;
			addBufferLog(move.get_ID(), move.getUpdated(), null,
					Msg.parseTranslation(getCtx(), "@M_Movement_ID@ @Created@"),
					MMovement.Table_ID, move.get_ID());
		} 
		//create each Movement Line
		MMovementLine moveline = new MMovementLine(move);
		moveline.setM_Product_ID(line.getM_Product_ID());
		if (QtyMovement.compareTo(Env.ZERO)>0)
			moveline.setMovementQty(QtyMovement.compareTo(line.getQtyMovement())<0?QtyMovement:line.getQtyMovement());
		else 
			moveline.setMovementQty(line.getQtyMovement());
		moveline.setM_Locator_ID(line.getWM_EmptyStorage().getM_Locator_ID());
		moveline.setM_LocatorTo_ID(M_Locator_ID);
		moveline.saveEx(trxName);
		System.out.println("MovementLine created "+line.getQtyMovement()+" "+line.getM_Product().getValue()+" at "+target.getM_Locator().getValue());
	}
  
}
