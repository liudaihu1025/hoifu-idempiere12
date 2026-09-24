package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCost;
import org.compiere.model.MCostElement;
import org.compiere.model.MOrg;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

/**
 * 产品价格(M_ProductPrice)事件处理器。
 * 
 * 触发条件： 1. 只处理物料类型(M_Product.ProductType)=资源(R) 的产品价格记录，其它类型的价格变动不处理；
 * 2.新增价格记录，或已有记录的 PriceStd 字段发生变化时触发；
 * 
 * 覆盖策略：财务每次改时薪都要覆盖更新 CurrentCostPrice（不管之前是否>0）。
 */
public class ProductPriceEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(ProductPriceEventProcessor.class);

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MProductPrice
				&& (IEventTopics.PO_AFTER_NEW.equals(topic) || IEventTopics.PO_AFTER_CHANGE.equals(topic));
	}

	@Override
	public void process(PO po, String topic) {
		MProductPrice productPrice = (MProductPrice) po;
		syncPriceToCost(productPrice, topic);
	}

	/**
	 * 产品价格 -> 产品成本 同步入口。
	 */
	private void syncPriceToCost(MProductPrice productPrice, String topic) {
		boolean isNew = IEventTopics.PO_AFTER_NEW.equals(topic);
		// 非新增时，只有 PriceStd 真正变化才需要重新同步，避免其它字段变化触发无意义的同步
		if (!isNew && !productPrice.is_ValueChanged(MProductPrice.COLUMNNAME_PriceStd)) {
			return;
		}

		MProduct product = new MProduct(productPrice.getCtx(), productPrice.getM_Product_ID(),
				productPrice.get_TrxName());

		// 产品数据检查：物料类型必须是"资源"，否则不需要同步成本
		if (!MProduct.PRODUCTTYPE_Resource.equals(product.getProductType())) {
			return;
		}

		BigDecimal priceStd = productPrice.getPriceStd();
		if (priceStd == null) {
			log.warning("M_ProductPrice.PriceStd 为空，跳过同步。M_ProductPrice_ID=" + productPrice.getM_ProductPrice_ID()
					+ "，M_Product_ID=" + product.getM_Product_ID() + "，ProductValue=" + product.getValue());
			return;
		}

		try {
			// 会计科目表：租户下可能有多个会计科目表(不同币种)，需要分别同步，保证每个科目表下都对得上
			MAcctSchema[] acctSchemas = MAcctSchema.getClientAcctSchema(productPrice.getCtx(),
					productPrice.getAD_Client_ID());
			if (acctSchemas == null || acctSchemas.length == 0) {
				log.warning("未找到会计科目表(C_AcctSchema)，跳过成本同步。AD_Client_ID=" + productPrice.getAD_Client_ID());
				return;
			}

			// 资源对应的成本要素：CostElementType=资源(R)
			MCostElement resourceCostElement = getResourceCostElement(productPrice.getAD_Client_ID());
			if (resourceCostElement == null) {
				log.warning("未找到 CostElementType=资源(R) 的成本要素(M_CostElement)，请先在成本要素窗口维护好该记录。" + "AD_Client_ID="
						+ productPrice.getAD_Client_ID() + "，M_Product_ID=" + product.getM_Product_ID());
				return;
			}

			// 组织范围：AD_Org_ID=0 表示所有组织，需要按该产品所属租户下所有组织各建/更新一条；否则只处理这一个组织
			int[] orgIds = resolveOrgIds(productPrice);

			for (MAcctSchema as : acctSchemas) {
				// 币种换算：价格表币种与会计科目表币种不一致时，按当前汇率换算
				BigDecimal price = convertToAcctSchemaCurrency(productPrice, as, priceStd);

				for (int orgId : orgIds) {
					// 按产品成本核算级别(CostingLevel)归一化组织维度，逻辑等价于 CostDimension.updateForProduct
					int normalizedOrgId = normalizeOrgId(product, as, orgId);
					upsertCost(product, as, resourceCostElement, normalizedOrgId, price, productPrice);
				}
			}
		} catch (Exception e) {
			// 同步失败不应影响财务维护价格这个主流程，只记录日志
			log.log(Level.WARNING, "同步产品价格到产品成本异常。M_Product_ID=" + product.getM_Product_ID() + "，ProductValue="
					+ product.getValue() + "，M_ProductPrice_ID=" + productPrice.getM_ProductPrice_ID(), e);
		}
	}

	/**
	 * 获取 CostElementType=资源(R) 的成本要素。 按约定：同一租户下资源类型的成本要素只应该有一条(实施阶段提前建好)，取第一条即可。
	 */
	private MCostElement getResourceCostElement(int adClientId) {
		return new Query(Env.getCtx(), MCostElement.Table_Name,
				MCostElement.COLUMNNAME_AD_Client_ID + "=? AND " + MCostElement.COLUMNNAME_CostElementType + "=?", null)
				.setParameters(adClientId, MCostElement.COSTELEMENTTYPE_Resource).setOnlyActiveRecords(true).first();
	}

	/**
	 * 解析这条价格记录需要覆盖的组织范围： - AD_Org_ID=0(所有组织)：取该租户下所有激活组织，并按 MCost.create(MProduct)
	 * 的既有规则， 跳过汇总组织(IsSummary=Y)——汇总组织本身不承载实际业务数据，不应该建成本记录，
	 * 这样才能和框架自带的成本记录初始化逻辑保持一致，避免产生脏数据。 - AD_Org_ID<>0：只处理这一个组织
	 */
	private int[] resolveOrgIds(MProductPrice productPrice) {
		if (productPrice.getAD_Org_ID() == 0) {
			MOrg[] orgs = MOrg.getOfClient(productPrice.getAD_Client_ID());
			java.util.List<Integer> orgIds = new java.util.ArrayList<>();
			for (MOrg org : orgs) {
				if (org.isSummary()) {
					continue; // 跳过汇总组织，和 MCost.create(MProduct) 的过滤规则保持一致
				}
				orgIds.add(org.getAD_Org_ID());
			}
			int[] result = new int[orgIds.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = orgIds.get(i);
			}
			return result;
		}
		return new int[] { productPrice.getAD_Org_ID() };
	}


	/**
	 * 复制自 CostDimension.updateForProduct 的组织维度归一化逻辑： 根据产品的成本核算级别(CostingLevel)，决定这条
	 * M_Cost 记录实际应该落在哪个组织维度上
	 */
	private int normalizeOrgId(MProduct product, MAcctSchema as, int orgId) {
		String costingLevel = product.getCostingLevel(as);
		if (MAcctSchema.COSTINGLEVEL_Client.equals(costingLevel)
				|| MAcctSchema.COSTINGLEVEL_BatchLot.equals(costingLevel)) {
			return 0;
		}
		return orgId;
	}

	/**
	 * 价格表币种与会计科目表币种不一致时，按当前汇率换算成会计科目表币种的金额；一致则原样返回。
	 */
	private BigDecimal convertToAcctSchemaCurrency(MProductPrice productPrice, MAcctSchema as, BigDecimal priceStd) {
		MPriceListVersion plv = new MPriceListVersion(productPrice.getCtx(), productPrice.getM_PriceList_Version_ID(),
				productPrice.get_TrxName());
		MPriceList priceList = plv.getPriceList();
		int priceListCurrencyId = priceList != null ? priceList.getC_Currency_ID() : as.getC_Currency_ID();

		if (priceListCurrencyId == as.getC_Currency_ID()) {
			return priceStd;
		}
		return MConversionRate.convert(productPrice.getCtx(), priceStd, priceListCurrencyId, as.getC_Currency_ID(),
				productPrice.getAD_Client_ID(), productPrice.getAD_Org_ID());
	}

	/**
	 * 成本记录不存在就新建，存在就覆盖更新 CurrentCostPrice （财务每次改时薪都要覆盖，不管之前是否>0，因为工资本来就会变）。
	 */
	private void upsertCost(MProduct product, MAcctSchema as, MCostElement element, int orgId, BigDecimal price,
			MProductPrice productPrice) {
		MCost cost = new Query(Env.getCtx(), MCost.Table_Name,
				MCost.COLUMNNAME_AD_Client_ID + "=? AND " + MCost.COLUMNNAME_AD_Org_ID + "=? AND "
						+ MCost.COLUMNNAME_M_Product_ID + "=? AND " + MCost.COLUMNNAME_M_AttributeSetInstance_ID
						+ "=? AND " + MCost.COLUMNNAME_M_CostType_ID + "=? AND " + MCost.COLUMNNAME_C_AcctSchema_ID
						+ "=? AND " + MCost.COLUMNNAME_M_CostElement_ID + "=?",
				productPrice.get_TrxName())
				.setParameters(product.getAD_Client_ID(), orgId, product.getM_Product_ID(), 0, as.getM_CostType_ID(),
						as.getC_AcctSchema_ID(), element.getM_CostElement_ID())
				.first();

		if (cost == null) {
			// 不存在则新建：M_AttributeSetInstance_ID 固定传0（资源类型产品不涉及批次/属性维度）
			cost = new MCost(product, 0, as, orgId, element.getM_CostElement_ID());
			cost.setM_CostType_ID(as.getM_CostType_ID());
		}
		cost.setCurrentCostPrice(price);
		cost.saveEx(productPrice.get_TrxName());

		log.info("同步产品价格到产品成本，M_Product_ID=" + product.getM_Product_ID() + "，ProductValue=" + product.getValue()
				+ "，AD_Org_ID=" + orgId + "，C_AcctSchema_ID=" + as.getC_AcctSchema_ID() + "，CurrentCostPrice=" + price);
	}
}