/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

	public class SetProductPackFactor extends SvrProcess {

	private int UnitsPerPack = 0;

	private BigDecimal UnitsPerPallet = Env.ZERO;
	
	private int cnt=0;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("UnitsPerPack")){
					UnitsPerPack = p.getParameterAsInt();
			}
				else if(name.equals("UnitsPerPallet")){
					UnitsPerPallet = (BigDecimal)p.getParameterAsBigDecimal();
			}
		}
	}

	protected String doIt() {
		if (UnitsPerPack==0 && UnitsPerPallet.compareTo(Env.ZERO)>0)
			throw new AdempiereException("No Units Setting. Aborted!");
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE T_Selection.AD_PInstance_ID=? AND T_Selection.T_Selection_ID=M_Product.M_Product_ID)";

		List<MProduct> lines = new Query(Env.getCtx(),MProduct.Table_Name,whereClause,get_TrxName())
		.setParameters(getAD_PInstance_ID()).list();

		for (MProduct line:lines){
			MProduct product = new Query(Env.getCtx(),MProduct.Table_Name,MProduct.COLUMNNAME_M_Product_ID+"=?",get_TrxName())
					.setParameters(line.getM_Product_ID())
					.first();
			if (UnitsPerPack>0)
				product.setUnitsPerPack(UnitsPerPack);
			if (UnitsPerPallet.compareTo(Env.ZERO)>0)
				product.setUnitsPerPallet(UnitsPerPallet);
			
			product.saveEx(get_TrxName());
			cnt++;
	}

	return "Products Set: "+cnt;

	}
}
