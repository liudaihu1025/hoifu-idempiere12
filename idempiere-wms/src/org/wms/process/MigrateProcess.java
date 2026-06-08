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
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MTable;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_Gate;
import org.wms.model.MWM_HandlingUnit;
import org.wms.model.MWM_HandlingUnitHistory;
import org.wms.model.MWM_InOut;
import org.wms.model.MWM_InOutLine;
import org.wms.model.MWM_Migration;
/**
 * Warehouse Migration Process - directly from Excel sheet with just Product, Qty, Price, Locator
 * Auto create phantom standard PO, Receipts, Putaway to WMS Empty Storage
 * @author red1
 */
	public class MigrateProcess extends SvrProcess {

	private int M_PriceList_Version_ID = 0;

	private int M_Warehouse_ID = 0;

	private int M_Locator_ID = 0; 
	private int M_Product_ID = 0;
	int cnt = 0;
	private int WM_Gate_ID = 0;
	private int WM_HandlingUnit_ID=0;
	BigDecimal currentUOM = Env.ONE;
	BigDecimal boxConversion = Env.ONE;
	BigDecimal packFactor = Env.ONE;

	private BigDecimal eachQty;
	
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("M_PriceList_Version_ID")){
					M_PriceList_Version_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Warehouse_ID")){
					M_Warehouse_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Locator_ID")){
					M_Locator_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Product_ID")){
					M_Product_ID = p.getParameterAsInt();
			}
		}
	}

	protected String doIt() {
		List<MWM_Migration> migration = new Query(getCtx(),MWM_Migration.Table_Name,"",get_TrxName())
				.setOnlyActiveRecords(true)
				.setClient_ID(true)
				.list();
		if (migration==null || migration.isEmpty())
			return "Migration table empty or inactive records (already imported - activate back if needed)";
		//check parameters
		if (M_PriceList_Version_ID<1)
			throw new AdempiereException("PriceList Version not set!");
		MPriceListVersion pricelistversion = new MPriceListVersion(getCtx(), M_PriceList_Version_ID, null);
		MPriceList pricelist = new MPriceList(getCtx(),pricelistversion.getM_PriceList_ID(),null);
		
		//set phantom gate at warehouse
		MWM_Gate gate = new Query(getCtx(),MWM_Gate.Table_Name,MWM_Gate.COLUMNNAME_Name+"=?",get_TrxName())
				.setParameters("Phantom").first();
		if (gate==null)
			gate = new MWM_Gate(getCtx(), 0, get_TrxName());
		gate.setM_Warehouse_ID(M_Warehouse_ID);
		gate.setName("Phantom");
		gate.saveEx(get_TrxName());
		WM_Gate_ID = gate.get_ID();
		
		//set phantom Handling Unit
		MWM_HandlingUnit handlingunit = new Query(getCtx(),MWM_HandlingUnit.Table_Name,MWM_HandlingUnit.COLUMNNAME_Name+"=?",get_TrxName())
				.setParameters("Phantom").first();
		if (handlingunit==null)
			handlingunit = new MWM_HandlingUnit(getCtx(), 0, get_TrxName()); 
		handlingunit.setName("Phantom");
		handlingunit.saveEx(get_TrxName());
		WM_HandlingUnit_ID = handlingunit.get_ID();
		
		//set Standard Business Partner
		MBPartner partner = new Query(getCtx(),MBPartner.Table_Name,MBPartner.COLUMNNAME_Name+"=?",null)
				.setParameters("Standard")
				.first();
		if (partner==null)
			throw new AdempiereException("No Standard Business Partner - check first");
		int BPartnerID = partner.get_ID();
		
		MOrder purchase = createPurchaseOrder(pricelist, BPartnerID);
		Boolean processStatus = false;
		Utils utils = new Utils(get_TrxName());
		Timestamp movementdate = purchase.getCreated();
		
		//WMS DS, Putaway and Material Receipt -- assign handling unit as Draft for every locator
		//Post accounts and resubmit postings
		//continue WMS to EmptyStorage
		MWM_DeliverySchedule deliveryschedule = createDeliverySchedule(purchase);
		
		MWM_InOut wminout = createWMInOut(deliveryschedule);
		
		MInvoice invoice = createInvoice(purchase);
		
		//Material Receipt record ID holder
		int InOut_ID = 0;
				
		for (MWM_Migration record:migration) {
			if (M_Product_ID>0)
				if (record.getM_Product_ID()!=M_Product_ID)
					continue;
			if (M_Locator_ID>0) {
				if (record.getM_Locator_ID()!=M_Locator_ID)
					continue;
			}			
			if (M_Warehouse_ID>1) {
				if (M_Warehouse_ID!=record.getM_Locator().getM_Warehouse_ID())
					continue;
			}  
			
			if (record.getWM_HandlingUnit_ID()>0) {
				MWM_HandlingUnit hu = (MWM_HandlingUnit) record.getWM_HandlingUnit();
				String huname = hu.getName();
				List<MWM_HandlingUnit>hus = new Query(getCtx(), MWM_HandlingUnit.Table_Name, MWM_HandlingUnit.COLUMNNAME_Name+"=?",get_TrxName())
						.setParameters(huname)
						.list();
				if (hus.size()>1)
					log.warning("Duplicate Handling Unit :"+huname);
				WM_HandlingUnit_ID = record.getWM_HandlingUnit_ID();
				MWM_EmptyStorageLine check = new Query(getCtx(), MWM_EmptyStorageLine.Table_Name, MWM_EmptyStorageLine.COLUMNNAME_WM_HandlingUnit_ID+"=?", get_TrxName())
						.setParameters(WM_HandlingUnit_ID)
						.first();
				if (check!=null)
					log.warning("Handling Unit:"+hus.get(0).getName()+" in in use by "+check.getM_Product().getValue()
							+" at "+check.getWM_EmptyStorage().getM_Locator().getValue());
				handlingunit = (MWM_HandlingUnit) record.getWM_HandlingUnit();
				handlingunit.setDocStatus(MWM_HandlingUnit.DOCSTATUS_Completed);
				handlingunit.saveEx(get_TrxName());
			}
			
			statusUpdate(cnt+" "+record.getM_Locator().toString());
			
			//start Purchase OrderLine	
			MOrderLine purchaseline = new MOrderLine(purchase);
			purchaseline.setM_Product_ID(record.getM_Product_ID());
			purchaseline.setC_UOM_ID(record.getM_Product().getC_UOM_ID());
			//implement the un-matching process
			//if not exist, create new Product/Price
			
			purchaseline.setQty(record.getMovementQty());
			if (record.getMovementDate()!=null)
				movementdate = record.getMovementDate();
			purchaseline.setDatePromised(movementdate);
			purchaseline.setDateDelivered(movementdate);
			
			if (record.getPrice().compareTo(Env.ZERO)>0)
				purchaseline.setPrice(record.getPrice());
			else {
				//get from PriceListVersion
				MProductPrice productprice = new MProductPrice(getCtx(), M_PriceList_Version_ID, M_Product_ID, null);
				purchaseline.setPrice(productprice.getPriceStd());
			}
			purchaseline.saveEx(get_TrxName());
			System.out.println(record.getM_Product().getName()+" "+record.getWM_HandlingUnit().getName()+" "+record.getM_Locator().getValue());
			MWM_DeliveryScheduleLine dline = new MWM_DeliveryScheduleLine(Env.getCtx(), 0, get_TrxName());
			dline.setWM_DeliverySchedule_ID(deliveryschedule.get_ID());
			dline.setC_OrderLine_ID(purchaseline.getC_OrderLine_ID());
			dline.setM_Product_ID(purchaseline.getM_Product_ID());
			dline.setM_AttributeSetInstance_ID(purchaseline.getM_AttributeSetInstance_ID());
			dline.setC_UOM_ID(purchaseline.getC_UOM_ID());
			dline.setReceived(true);
			dline.setQtyOrdered(purchaseline.getQtyOrdered());
			dline.setQtyDelivered(purchaseline.getQtyOrdered());
			dline.saveEx(get_TrxName());
			
			//invoice creation and posting
			MInvoiceLine invoiceline = new MInvoiceLine(invoice);
			invoiceline.setOrderLine(purchaseline);
			invoiceline.setQty(purchaseline.getQtyOrdered());
			invoiceline.saveEx(get_TrxName());
			
			//Locator EmptyStorage
			MWM_EmptyStorage empty = new Query(getCtx(),MWM_EmptyStorage.Table_Name,MWM_EmptyStorage.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
					.setParameters(record.getM_Locator_ID())
					.first();
			if (empty==null)
				throw new AdempiereException("No EmptyStorage at Locator ID - "+record.getM_Locator_ID());
			
			//create put-away list
			MWM_InOutLine inoutline = utils.newInOutLine(wminout, dline, purchaseline.getQtyOrdered());
			inoutline.setM_Locator_ID(record.getM_Locator_ID());
			inoutline.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
			inoutline.saveEx(get_TrxName());
			
			createHandlingUnitHistory(handlingunit, purchaseline, inoutline); 
			
			//deduct Empty Available Storage -- calculate Percentage
			eachQty = uomFactors(dline,dline.getQtyOrdered());
			
			//BigDecimal available = empty.getAvailableCapacity().subtract(sline.getQtyMovement().divide(boxConversion,2,RoundingMode.HALF_EVEN));
			//empty.setAvailableCapacity(available);
			//utils.calculatePercentageVacant(dline.isReceived(), empty); //TODO Available Capacity
			cnt++;
			record.setIsActive(false);
			record.saveEx(get_TrxName());
			if (cnt>1000)
				break;
		}
		//Complete Purchase
		purchase.setDocStatus(MOrder.DOCSTATUS_InProgress);
		purchase.setDocAction(MOrder.DOCACTION_Complete);
		processStatus = purchase.processIt(DocAction.ACTION_Complete);
		purchase.saveEx(get_TrxName());
		statusUpdate("Complete Purchase Order");
		invoice.setDocStatus(invoice.DOCSTATUS_InProgress);
		invoice.setDocAction(invoice.DOCACTION_Complete);
		processStatus = invoice.processIt(DocAction.ACTION_Complete);
		invoice.saveEx(get_TrxName());
		statusUpdate("completing material receipts");
		wminout.setDocStatus(wminout.DOCSTATUS_InProgress);
		wminout.setDocAction(wminout.DOCACTION_Complete); 
		try {
		wminout.processIt(DocAction.ACTION_Complete);wminout.getDocStatus();
		} catch (Exception e) {
			e.printStackTrace();
		}
		wminout.saveEx(get_TrxName());
		List<MWM_InOutLine> ioline = new Query(getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_InOut_ID+"=?",get_TrxName())
				.setParameters(wminout.get_ID())
				.list();
		return "RUN AGAIN UNTIL LESS THAN A THOUSAND - Records processed "+cnt; 
	}

	private void createHandlingUnitHistory(MWM_HandlingUnit handlingunit, MOrderLine purchaseline,
			MWM_InOutLine inoutline) {
		//create new history
		MWM_HandlingUnitHistory huh = new MWM_HandlingUnitHistory(Env.getCtx(),0,get_TrxName());
		huh.setWM_HandlingUnit_ID(WM_HandlingUnit_ID);
		huh.setWM_InOutLine_ID(inoutline.get_ID());
		huh.setC_Order_ID(inoutline.getC_OrderLine().getC_Order_ID());
		huh.setQtyMovement(purchaseline.getQtyOrdered());
		huh.setC_UOM_ID(inoutline.getC_UOM_ID());
		huh.setM_Product_ID(inoutline.getM_Product_ID());
		huh.setDateStart(handlingunit.getUpdated());
		huh.saveEx(get_TrxName()); 
	}

	private MInvoice createInvoice(MOrder purchase) {
		//Invoice creation and completion and accts posted
		MInvoice invoice = new MInvoice(purchase,MDocType.getDocType(MDocType.DOCBASETYPE_APInvoice),purchase.getDateOrdered());
		invoice.saveEx(get_TrxName());
		return invoice;
	}

	private MWM_InOut createWMInOut(MWM_DeliverySchedule deliveryschedule) {
		MWM_InOut wminout = new MWM_InOut(getCtx(), 0, get_TrxName());
		wminout.setC_BPartner_ID(deliveryschedule.getC_BPartner_ID());
		wminout.setIsSOTrx(false);
		wminout.setName(deliveryschedule.getName());
		wminout.setWM_DeliverySchedule_ID(deliveryschedule.get_ID());
		wminout.saveEx(get_TrxName());
		return wminout;
	}

	private MWM_DeliverySchedule createDeliverySchedule(MOrder purchase) {
		MWM_DeliverySchedule deliveryschedule = new MWM_DeliverySchedule(getCtx(), 0, get_TrxName());
		deliveryschedule.setWM_Gate_ID(WM_Gate_ID);
		deliveryschedule.setC_Order_ID(purchase.get_ID());
		deliveryschedule.setDatePromised(purchase.getDatePromised());
		deliveryschedule.setDateDelivered(purchase.getDatePromised());
		deliveryschedule.setName(purchase.getCreated().toString()+":"+deliveryschedule.getWM_Gate().getName());
		deliveryschedule.setC_BPartner_ID(purchase.getC_BPartner_ID());
		deliveryschedule.saveEx(get_TrxName());
		return deliveryschedule;
	}

	private MOrder createPurchaseOrder(MPriceList pricelist, int BPartnerID) {
		//create one purchase order for all purchaselines
		MOrder purchase = new MOrder(getCtx(), 0, get_TrxName());
		purchase.setC_BPartner_ID(BPartnerID);
		purchase.setDescription("PHANTOM PURCHASE TO BRING IN ALL OPENING STOCK");
		purchase.setM_Warehouse_ID(M_Warehouse_ID);//Env.getContextAsInt(getCtx(), "#M_Warehouse_ID"
		purchase.setM_PriceList_ID(pricelist.get_ID());
		purchase.setIsSOTrx(false);
		purchase.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
		purchase.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_PurchaseOrder));
		purchase.setC_DocTypeTarget_ID();
		purchase.saveEx(get_TrxName());
		return purchase;
	}
	private BigDecimal uomFactors(MWM_DeliveryScheduleLine line) {
		BigDecimal qtyEntered = line.getQtyOrdered();//.multiply(new BigDecimal(product.getUnitsPerPack()));

		//Current = current UOM Conversion Qty
		MUOMConversion currentuomConversion = new Query(getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=? AND "
		+MUOMConversion.COLUMNNAME_C_UOM_To_ID+"=?",null)
				.setParameters(line.getM_Product_ID(),line.getC_UOM_ID())
				.first();
		if (currentuomConversion!=null)
			currentUOM = currentuomConversion.getDivideRate();
		BigDecimal eachQty=qtyEntered.multiply(currentUOM);
		
		//Pack Factor calculation
		MUOMConversion highestUOMConversion = new Query(getCtx(),MUOMConversion.Table_Name,MUOMConversion.COLUMNNAME_M_Product_ID+"=?",null)
				.setParameters(line.getM_Product_ID())
				.setOrderBy(MUOMConversion.COLUMNNAME_DivideRate+" DESC")
				.first(); 
		if (highestUOMConversion!=null) {
			boxConversion = highestUOMConversion.getDivideRate();
			packFactor = boxConversion.multiply(highestUOMConversion.getDivideRate().divide(currentUOM,2,RoundingMode.HALF_EVEN));
		}
		return eachQty;
	}
	private BigDecimal uomFactors(MWM_DeliveryScheduleLine line, BigDecimal balance) {
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
