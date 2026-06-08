/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.process;
import java.sql.Timestamp;import java.util.Calendar;import java.util.List;import org.compiere.model.Query;import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_EmptyStorageLine;

	public class CheckUp extends SvrProcess {
	Timestamp earliest = new Timestamp (System.currentTimeMillis()); 	private boolean IsActive = false;
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)					;
				else if(name.equals("IsActive")){
					IsActive = "Y".equals(p.getParameter());
			}
		}
	}
	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_EmptyStorageLine.WM_EmptyStorageLine_ID)";

		List<MWM_EmptyStorageLine> lines = new Query(Env.getCtx(),MWM_EmptyStorageLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MWM_EmptyStorageLine line:lines){			setFIFO(line);
		}

	return "Earliest FIFO DateStart:"+earliest;
	}		private void setFIFO(MWM_EmptyStorageLine line) {		 MWM_EmptyStorageLine fifoist = new Query(getCtx(),MWM_EmptyStorageLine.Table_Name,MWM_EmptyStorageLine.COLUMNNAME_M_Product_ID+"=? AND "				 +MWM_EmptyStorageLine.COLUMNNAME_QtyMovement+">=?",				 get_TrxName())				 .setParameters(line.getM_Product_ID(),line.getQtyMovement())				 .setOnlyActiveRecords(true)				 .setOrderBy(MWM_EmptyStorageLine.COLUMNNAME_DateStart)				 .first();		 Calendar cal = Calendar.getInstance();		 cal.setTimeInMillis(fifoist.getDateStart().getTime());		 cal.add(Calendar.DAY_OF_MONTH, -1);		 line.setDateStart(new Timestamp(cal.getTime().getTime()));		 line.saveEx(get_TrxName());		 if (earliest.after(line.getDateStart())) 			 earliest=line.getDateStart();  	}
}
