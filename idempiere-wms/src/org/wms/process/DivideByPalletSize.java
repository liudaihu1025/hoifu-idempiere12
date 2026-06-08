/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import org.compiere.process.ProcessInfoParameter;

import java.util.List;
import org.compiere.model.Query;
import org.compiere.util.Env;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import org.compiere.util.DB;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MSequence;
import org.compiere.model.PO;
import org.wms.model.MWM_DeliveryScheduleLine;
import org.compiere.process.SvrProcess;

	public class DivideByPalletSize extends SvrProcess {

	private int UnitsPerPallet = 0;
	private int productPalletSize = 0;
	private int count = 0;
	
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("UnitsPerPallet")){
					UnitsPerPallet = p.getParameterAsInt();
			}
		}
	}

	protected String doIt() {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=WM_DeliveryScheduleLine.WM_DeliveryScheduleLine_ID)";

		List<MWM_DeliveryScheduleLine> lines = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();
		if (UnitsPerPallet>0)
			productPalletSize=UnitsPerPallet;
		
		for (MWM_DeliveryScheduleLine line:lines){
			if (UnitsPerPallet==0) {
				//get from Product info
				MProduct product = (MProduct) line.getM_Product();
				productPalletSize = product.getUnitsPerPallet().intValue();
			}
			if (productPalletSize==0)
				continue;
			
			BigDecimal qty = line.getQtyDelivered();

			if (qty.intValue()>productPalletSize) {
				line.setQtyDelivered(new BigDecimal(productPalletSize));
				line.setQtyOrdered(new BigDecimal(productPalletSize));
				line.saveEx(get_TrxName());			
				
				while (qty.intValue()>productPalletSize) 
					qty = iteratePalletSize(line, qty);
			}
	}
	return "New Lines Created: "+count;

	}

	private BigDecimal iteratePalletSize(MWM_DeliveryScheduleLine line, BigDecimal qty) {
		MWM_DeliveryScheduleLine newdsline = new MWM_DeliveryScheduleLine(getCtx(), 0, get_TrxName());
		PO.copyValues(line, newdsline);
		qty = qty.subtract(new BigDecimal(productPalletSize));
		if (qty.compareTo(new BigDecimal(productPalletSize))>0) {
			newdsline.setQtyOrdered(new BigDecimal(productPalletSize));
			newdsline.setQtyDelivered(new BigDecimal(productPalletSize));
		} else {
			newdsline.setQtyOrdered(qty);
			newdsline.setQtyDelivered(qty);
		}
		newdsline.saveEx(get_TrxName());
		count++;
		return qty;
	}
}
