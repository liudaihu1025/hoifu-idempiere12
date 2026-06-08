/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.component;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.osgi.service.event.Event;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.wms.model.MWM_EmptyStorage;
import org.wms.model.MWM_EmptyStorageLine;
import org.wms.model.MWM_InOutLine;
import org.wms.process.Utils;

public class WM_DeliveryScheduleLineDocEvent extends AbstractEventHandler {
 	private static CLogger log = CLogger.getCLogger(WM_DeliveryScheduleLineDocEvent.class);
		private String trxName = "";
		private PO po = null;
		private Utils util = null;
		private BigDecimal currentUOM=Env.ONE;
		private BigDecimal packFactor=Env.ONE;
		private BigDecimal eachQty = Env.ONE;
		private BigDecimal boxConversion=Env.ONE;
		
	@Override 
	protected void initialize() { 
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MWM_DeliveryScheduleLine.Table_Name);
		log.info("WM_DeliveryScheduleLine<PLUGIN> .. IS NOW INITIALIZED");
		}

	@Override 
	protected void doHandleEvent(Event event){
		String type = event.getTopic();
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
		}
 		else {
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			
			if (po instanceof MWM_DeliveryScheduleLine){
				if (IEventTopics.PO_AFTER_CHANGE == type){
					;
				}
			}
		}
	}
	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;
		}
}
