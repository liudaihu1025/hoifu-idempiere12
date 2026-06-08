package org.libero.process;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.logging.Level;

import org.adempiere.model.engines.CostDimension;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCost;
import org.compiere.model.MCostElement;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;


/**
 *	CopyPriceToStandardCurrent 
 *	
 *  @author Victor Perez, e-Evolution, S.C.
 *  @version $Id: CopyPriceToStandardCurrent.java,v 1.1 2004/06/22 05:24:03 vpj-cd Exp $
 */
public class CopyPriceToStandardCurrent extends SvrProcess
{
	private int p_AD_Org_ID = 0;
	private int p_C_AcctSchema_ID = 0;
	private int p_M_CostType_ID = 0;
	private int p_M_CostElement_ID = 0;
	private int p_M_PriceList_Version_ID =0;

	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();

			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_CostType_ID"))
			{    
				p_M_CostType_ID = ((BigDecimal)para[i].getParameter()).intValue();
			}
			else if (name.equals("AD_Org_ID"))
			{    
				p_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			}
			else if (name.equals("C_AcctSchema_ID"))
			{    
				p_C_AcctSchema_ID = ((BigDecimal)para[i].getParameter()).intValue();
			}           
			else if (name.equals("M_CostElement_ID"))
			{    
				p_M_CostElement_ID = ((BigDecimal)para[i].getParameter()).intValue();
			}
			else if (name.equals("M_PriceList_Version_ID"))
			{    
				p_M_PriceList_Version_ID = ((BigDecimal)para[i].getParameter()).intValue();
			}
			else
			{
				log.log(Level.SEVERE,"prepare - Unknown Parameter: " + name);
			}
		}
	}
	
	protected String doIt() throws Exception                
	{
		MAcctSchema as = MAcctSchema.get(getCtx(), p_C_AcctSchema_ID);
		MCostElement element = MCostElement.get(getCtx(), p_M_CostElement_ID);
//		if (!MCostElement.COSTELEMENTTYPE_Material.equals(element.getCostElementType()))
//		{
//			throw new AdempiereException("Only Material Cost Elements are allowed");
//		}
		
		int count_updated = 0;
		
		MPriceListVersion plv = new MPriceListVersion(getCtx(), p_M_PriceList_Version_ID, get_TrxName());
		for (final MProductPrice pprice : plv.getProductPrice(" AND "+MProductPrice.COLUMNNAME_PriceStd+"<>0"))
		{
			BigDecimal price = pprice.getPriceStd();
			int C_Currency_ID = plv.getPriceList().getC_Currency_ID();
			if (C_Currency_ID != as.getC_Currency_ID())
			{                     	
				price = MConversionRate.convert(getCtx(), pprice.getPriceStd(),
								C_Currency_ID, as.getC_Currency_ID(),
								getAD_Client_ID(), p_AD_Org_ID);                     	
			}
			MProduct product = MProduct.get(getCtx(), pprice.getM_Product_ID());
			
			CostDimension d = new CostDimension(product, as, p_M_CostType_ID, p_AD_Org_ID, 0, p_M_CostElement_ID);
			Collection<MCost> costs = d.toQuery(MCost.class, get_TrxName()).list(); 
			for (MCost cost : costs)
			{
				if (cost.getM_CostElement_ID() == element.get_ID())
				{
					// 假设当前成本价格>0  则跳过
					if (BigDecimal.ZERO.compareTo(cost.getCurrentCostPrice()) < 0) 
						break;
					
					cost.setCurrentCostPrice(price);
					cost.saveEx();
					count_updated++;
					break;
				}
			}                                                                      
		}
		return "@Updated@ #"+count_updated;
	}
}	
