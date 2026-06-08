//package org.adempiere.model.engines;
//
//import java.math.BigDecimal;
//
//import org.compiere.model.MAcctSchema;
//import org.compiere.model.MCost;
//import org.compiere.model.MCostElement;
//import org.compiere.model.MProduct;
//import org.compiere.util.Env;
//import org.libero.model.MPPCostCollector;
//
///**
// * 
// * @Description: 平均发票成本引擎
// * @author Administrator
// * @date 2025年11月22日
// */
//public class AverageInvoiceCostEngine extends CostEngine {
//
//	@Override
//	public String getCostingMethod() {
//		return MCostElement.COSTINGMETHOD_AverageInvoice; // "I"
//	}
//
//	@Override
//	public BigDecimal getProductActualCostPrice(MPPCostCollector cc, MProduct product, MAcctSchema as,
//			MCostElement element, String trxName) {
//		if (product == null) {
//			log.warning("Product is null");
//			return Env.ZERO;
//		}
//
//		CostDimension d = new CostDimension(product, as, as.getM_CostType_ID(), cc.getAD_Org_ID(),
//				product.getM_AttributeSetInstance_ID(), element.getM_CostElement_ID());
//		MCost cost = d.toQuery(MCost.class, trxName).firstOnly();
//
//		if (cost == null)
//			return Env.ZERO;
//
//		// 使用加权平均成本
//		BigDecimal price = cost.getCurrentCostPrice().add(cost.getCurrentCostPriceLL());
//		return roundCost(price, as.getC_AcctSchema_ID());
//	}
//
//	@Override
//	public void createRateVariances(MPPCostCollector cc) {
//		// 平均发票成本法不进行差异分析
//		log.info("Rate variance not applicable for Average Invoice costing");
//	}
//
//	@Override
//	public void createMethodVariances(MPPCostCollector cc) {
//		// 平均发票成本法不进行方法差异分析
//		log.info("Method variance not applicable for Average Invoice costing");
//	}
//
//	@Override
//	public void createUsageVariances(MPPCostCollector cc) {
//		// 平均发票成本法不进行用量差异分析
//		log.info("Usage variance not applicable for Average Invoice costing");
//	}
//}