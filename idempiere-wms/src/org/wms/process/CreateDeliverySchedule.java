/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.sql.Timestamp;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_DeliverySchedule;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_InOutLine;

	public class CreateDeliverySchedule extends SvrProcess {

	public CreateDeliverySchedule(){
		
	}

	int cnt = 0;
	private int WM_Gate_ID = 0;
	private Timestamp DatePromised = null;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_Gate_ID")){
					WM_Gate_ID = p.getParameterAsInt();
			}
				else if(name.equals("DatePromised")){
					DatePromised = p.getParameterAsTimestamp();
			}
		}
		setTrxName(get_TrxName());	
	}
	public String executeDoIt(){
		return doIt();
	}
	
	protected String doIt() {
		if (WM_Gate_ID<1)
			throw new AdempiereException("Set Gate Number");
		if (DatePromised==null)
			DatePromised=new Timestamp (System.currentTimeMillis());
		String whereClause = "EXISTS (SELECT ViewID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND CAST(T_Selection.ViewID AS INTEGER)=C_OrderLine.C_OrderLine_ID)";

		List<MOrderLine> lines = null;

		lines = new Query(Env.getCtx(),MOrderLine.Table_Name,whereClause,trxName)
				.setParameters(getAD_PInstance_ID())
				.list();
		MOrder order = (MOrder) lines.get(0).getC_Order();
		MWM_DeliverySchedule schedule = new Query(Env.getCtx(),MWM_DeliverySchedule.Table_Name,MWM_DeliverySchedule.COLUMNNAME_DatePromised+"=? AND "
		+MWM_DeliverySchedule.COLUMNNAME_WM_Gate_ID+"=? AND "+MWM_DeliverySchedule.COLUMNNAME_C_Order_ID+"=?",trxName)
				.setParameters(DatePromised,WM_Gate_ID,lines.get(0).getC_Order_ID())
				.first();
		if (schedule!=null)
			throw new AdempiereException("Already done same DateTime and Gate.");
		
		schedule = new MWM_DeliverySchedule(Env.getCtx(), 0, trxName);
		schedule.setWM_Gate_ID(WM_Gate_ID);
		schedule.setDatePromised(DatePromised);
		schedule.setDateDelivered(DatePromised);
		schedule.setC_Order_ID(lines.get(0).getC_Order_ID());//TODO break at each different OrderID
		schedule.setIsSOTrx(lines.get(0).getC_Order().isSOTrx());
		schedule.setC_DocType_ID(order.getDocTypeID());
		schedule.setC_BPartner_ID(lines.get(0).getC_Order().getC_BPartner_ID());
		schedule.setName(DatePromised.toString()+":"+schedule.getWM_Gate().getName());
		schedule.saveEx(trxName);
		int checkBP = schedule.getC_BPartner_ID();
		for (MOrderLine line:lines){
			int a = line.get_ID();

			log.info("Selected line ID = "+a);
			/** TODO break up DS if Business Partner of Order changes.
			if (checkBP != line.getC_Order().getC_BPartner_ID()) {
				//Order's BP changed
				schedule = new MWM_DeliverySchedule(Env.getCtx(), 0, trxName);
				schedule.setWM_Gate_ID(WM_Gate_ID);
				schedule.setDatePromised(DatePromised);
				schedule.setDateDelivered(DatePromised);
				schedule.setC_Order_ID(lines.get(0).getC_Order_ID());//TODO break at each different OrderID
				schedule.setIsSOTrx(lines.get(0).getC_Order().isSOTrx());
				schedule.setC_BPartner_ID(line.getC_Order().getC_BPartner_ID());
				schedule.setName(DatePromised.toString()+":"+schedule.getWM_Gate().getName());
				schedule.saveEx(trxName);
				checkBP = schedule.getC_BPartner_ID();
			}*/
			MWM_DeliveryScheduleLine dline = new MWM_DeliveryScheduleLine(Env.getCtx(), 0, trxName);
			dline.setWM_DeliverySchedule_ID(schedule.get_ID());
			dline.setC_OrderLine_ID(line.getC_OrderLine_ID());
			dline.setM_Product_ID(line.getM_Product_ID());
			dline.setM_AttributeSetInstance_ID(line.getM_AttributeSetInstance_ID());
			dline.setC_UOM_ID(line.getC_UOM_ID());
			dline.setReceived(lines.get(0).getC_Order().isSOTrx());
			if (dline.getC_OrderLine().getQtyDelivered().compareTo(Env.ZERO)>0){
				dline.setQtyOrdered(line.getQtyOrdered().subtract(line.getQtyDelivered()));
				dline.setQtyDelivered(line.getQtyOrdered().subtract(line.getQtyDelivered()));
			} else {
				dline.setQtyOrdered(line.getQtyOrdered());
				dline.setQtyDelivered(line.getQtyOrdered());
			}
			//check if has previous BackOrder that is not complete WM_InOut  
			MWM_DeliveryScheduleLine prevDsLine = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_C_OrderLine_ID+"=?"
					+ " AND "+MWM_DeliveryScheduleLine.COLUMNNAME_IsBackOrder+"=? ",get_TrxName())
					.setParameters(dline.getC_OrderLine_ID(),"Y")
					.setOrderBy(MWM_DeliveryScheduleLine.COLUMNNAME_Created+ " DESC")
					.first(); 
			if (prevDsLine!=null) {			
				MWM_InOutLine ioline = new Query(Env.getCtx(),MWM_InOutLine.Table_Name,MWM_InOutLine.COLUMNNAME_WM_DeliveryScheduleLine_ID+"=?",trxName)
					.setParameters(prevDsLine.get_ID())
					.first();
				if (ioline==null || !ioline.getM_InOutLine().getM_InOut().getDocStatus().equals(MInOut.STATUS_Completed))
					throw new AdempiereException("Back Order premature. Complete Material Receipt first");		
				prevDsLine.setQtyDelivered(prevDsLine.getQtyDelivered().add(dline.getQtyOrdered()));
				prevDsLine.saveEx(trxName);
			}
			cnt++;
			dline.saveEx(trxName); 
		}

	return "Delivery Lines: "+cnt;

	}
	
	private String trxName = "";
	
	private void setTrxName(String t){
		trxName = t;
	}
}
