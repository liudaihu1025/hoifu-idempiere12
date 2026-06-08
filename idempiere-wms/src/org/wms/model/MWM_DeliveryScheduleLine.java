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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class MWM_DeliveryScheduleLine extends X_WM_DeliveryScheduleLine{

	private static final long serialVersionUID = -1L;

	public MWM_DeliveryScheduleLine(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_DeliveryScheduleLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	protected boolean beforeSave (boolean newRecord)
	{
		if (getQtyDelivered().compareTo(getQtyOrdered())<0){
			setIsBackOrder(true); 
		}
		else 
			setIsBackOrder(false);
		return true;
	}
}
