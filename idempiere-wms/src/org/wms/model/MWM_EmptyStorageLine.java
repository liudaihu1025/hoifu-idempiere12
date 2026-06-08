/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;

public class MWM_EmptyStorageLine extends X_WM_EmptyStorageLine{

	private static final long serialVersionUID = -1L;

	public MWM_EmptyStorageLine(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_EmptyStorageLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public boolean isWMInOutLineProcessed() {
		MWM_InOutLine ioline = (MWM_InOutLine) getWM_InOutLine();
		if (ioline==null)
			return true;
		MWM_InOut inout = (MWM_InOut) ioline.getWM_InOut();
		
		if (!inout.isSOTrx())
			return true;
		else if (inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Completed)
				||inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Closed)
				||inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Voided)
				||inout.getDocStatus().equals(MWM_InOut.DOCSTATUS_Reversed)) {
 			return true;
		} 
		log.warning(ioline.get_ID()+" Picking "+get_ID()+" "+ioline.getQtyPicked()+" "
		+ioline.getM_Product().getValue()+" held up by Picking "+inout.getDocumentNo()+" "
		+inout.getName());
			return false;
	}
}
