/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.process;
import org.compiere.process.ProcessInfoParameter;import java.util.Calendar;import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;import org.compiere.util.Msg;import java.sql.SQLException;import java.sql.Timestamp;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;import org.compiere.model.MLocator;
import org.compiere.model.MSequence;import org.compiere.model.MStorageOnHand;import org.compiere.model.MWarehouse;
import org.wms.model.MWM_DeliveryScheduleLine;import org.wms.model.MWM_EmptyStorageLine;import org.wms.model.MWM_InOut;
import org.compiere.process.SvrProcess;

	public class CheckAvailability extends SvrProcess {
	Timestamp earliest = new Timestamp (System.currentTimeMillis()); 	private boolean IsActive = false;
	int M_Warehouse_ID = 0; 	private int M_Locator_ID = 0;	MWarehouse wh = null;	MLocator loc = null;		protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("IsActive")){
					IsActive = "Y".equals(p.getParameter());
			}				else if(name.equals("M_Warehouse_ID")){					M_Warehouse_ID = p.getParameterAsInt();			}				else if(name.equals("M_Locator_ID")){					M_Locator_ID = p.getParameterAsInt();			}
		}
	}
	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";				List<MWM_DeliveryScheduleLine> lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();
		if (lines.isEmpty())			throw new AdempiereException("Empty Selection");		if (M_Warehouse_ID==0) 			M_Warehouse_ID = Env.getContextAsInt(getCtx(), "#M_Warehouse_ID");		wh = new Query(getCtx(), MWarehouse.Table_Name, MWarehouse.COLUMNNAME_M_Warehouse_ID+"=?", get_TrxName())				.setParameters(M_Warehouse_ID)				.first();		if (wh==null)			throw new AdempiereException("No Warehouse found");		if (M_Locator_ID==0) {			loc = MLocator.getDefault(wh);			if (loc==null)				throw new AdempiereException("No Default Locator");			M_Locator_ID=loc.get_ID();		}else {			loc=new MLocator(getCtx(), M_Locator_ID, get_TrxName());		}
		for (MWM_DeliveryScheduleLine line:lines){
			if (isAvailable(line) && IsActive)				setFIFO(line);
	}

	return "All selected are available "+(IsActive?" FIFO Set "+earliest:"");

	}	private void setFIFO(MWM_DeliveryScheduleLine line) {		 MWM_EmptyStorageLine fifoist = new Query(getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "			 +MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">=?",			 get_TrxName())			 .setParameters(line.getM_Product_ID(),line.getQtyOrdered())			 .setOnlyActiveRecords(true)			 .setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart+","+MWM_EmptyStorageLine.COLUMNNAME_QtyMovement)			 .first();		 if (fifoist==null)			 throw new AdempiereException("No StorageLine for "+line.getQtyOrdered()+" "+line.getM_Product().getValue());		 		 Calendar cal = Calendar.getInstance();		 cal.setTimeInMillis(fifoist.getDateStart().getTime());		 cal.add(Calendar.DAY_OF_MONTH, -1);		 fifoist.setDateStart(new Timestamp(cal.getTime().getTime()));		 fifoist.saveEx(get_TrxName());		 if (earliest.after(fifoist.getDateStart())) 			 earliest=fifoist.getDateStart();  	}	private boolean isAvailable(MWM_DeliveryScheduleLine line) {		MStorageOnHand onhand = new Query(getCtx(), MStorageOnHand.Table_Name, MStorageOnHand.COLUMNNAME_M_Product_ID+"=? AND "				+MStorageOnHand.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())				.setParameters(line.getM_Product_ID(),M_Locator_ID)				.first();		if (onhand==null) {			StringBuilder buffer = new StringBuilder();			List<MStorageOnHand> onhands = new Query(getCtx(), MStorageOnHand.Table_Name, MStorageOnHand.COLUMNNAME_M_Product_ID+"=?",get_TrxName())					.setParameters(line.getM_Product_ID())					.list();			if (onhands.size()>0) {				for (int i=0;i<onhands.size();i++) {					if (onhands.get(i).getQtyOnHand().compareTo(line.getQtyOrdered())>=0){						buffer.append("\r\nAvailable "+onhands.get(i).getQtyOnHand()+" at "+onhands.get(i).getM_Locator().getValue());					}  				}			}			throw new AdempiereException("Not available :"+line.getQtyOrdered()+" "+line.getM_Product().getValue()+" at "+loc.getValue()+buffer.toString());		}		return true;	}
}
