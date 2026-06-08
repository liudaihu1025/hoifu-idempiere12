package com.hoifu.factory;

import org.compiere.util.Env;
import org.idempiere.model.MappedModelFactory;
import org.osgi.service.component.annotations.Component;

@Component(name = "com.hoifu.factory.CashFlowModelFactory", service = org.adempiere.base.IModelFactory.class, property = {
		"service.ranking:Integer=2" })
public class CashFlowModelFactory extends MappedModelFactory {

	public CashFlowModelFactory() {
		// 注册MCashFlowConfig
		addMapping("C_CashFlow_Config", () -> com.hoifu.model.MCashFlowConfig.class,
				(id, trxName) -> new com.hoifu.model.MCashFlowConfig(Env.getCtx(), id, trxName),
				(rs, trxName) -> new com.hoifu.model.MCashFlowConfig(Env.getCtx(), rs, trxName));

		// 注册MCashFlowItem
		addMapping("C_CashFlow_Item", () -> com.hoifu.model.MCashFlowItem.class,
				(id, trxName) -> new com.hoifu.model.MCashFlowItem(Env.getCtx(), id, trxName),
				(rs, trxName) -> new com.hoifu.model.MCashFlowItem(Env.getCtx(), rs, trxName));

		// 注册MCashFlowRule
		addMapping("C_CashFlow_Rule", () -> com.hoifu.model.MCashFlowRule.class,
				(id, trxName) -> new com.hoifu.model.MCashFlowRule(Env.getCtx(), id, trxName),
				(rs, trxName) -> new com.hoifu.model.MCashFlowRule(Env.getCtx(), rs, trxName));

		// 注册MFactCashFlow
		addMapping("Fact_CashFlow", () -> com.hoifu.model.MFactCashFlow.class,
				(id, trxName) -> new com.hoifu.model.MFactCashFlow(Env.getCtx(), id, trxName),
				(rs, trxName) -> new com.hoifu.model.MFactCashFlow(Env.getCtx(), rs, trxName));
	}
}