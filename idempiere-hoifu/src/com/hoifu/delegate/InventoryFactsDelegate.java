package com.hoifu.delegate;

import java.math.BigDecimal;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.FactsEventData;
import org.adempiere.base.event.annotations.doc.FactsValidateDelegate;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MDocType;
import org.compiere.model.MElementValue;
import org.compiere.model.MInventory;
import org.compiere.model.Query;
import org.compiere.model.X_C_InterOrg_Acct;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

@EventTopicDelegate // 自动注册
@ModelEventTopic(modelClass = MInventory.class) // 指定模型类
public class InventoryFactsDelegate extends FactsValidateDelegate<MInventory> {

	public InventoryFactsDelegate(MInventory po, Event event) {
		super(po, event);
	}

	@Override
	protected void onFactsValidate(FactsEventData data) {
		MInventory inv = getModel();
		MAcctSchema as = data.getAcctSchema();

		MDocType dt = MDocType.get(inv.getC_DocType_ID());
		if (!"IU".equals(dt.getDocSubTypeInv()))
			return;
		if (!"行政物料领用单".equals(dt.getName()))
			return;

		int companyOrgId = inv.getAD_Org_ID();
		int bizUnitOrgId = inv.getAD_OrgTrx_ID();
		if (bizUnitOrgId == 0 || bizUnitOrgId == companyOrgId)
			return;

		for (Fact fact : data.getFacts()) {
			BigDecimal totalCost = BigDecimal.ZERO;
			FactLine chargeLine = null;

			for (FactLine fl : fact.getLines()) {
				if (fl.getAmtSourceDr() != null && fl.getAmtSourceDr().signum() > 0 && fl.getAccount_ID() > 0) {
					chargeLine = fl;
					totalCost = fl.getAmtSourceDr();
					break;
				}
			}
			if (chargeLine == null || totalCost.signum() == 0)
				continue;

			// 1. 费用行改为事业部
			chargeLine.setAD_Org_ID(bizUnitOrgId);

			// 2. 事业部 CR 应付账款-公司（负数 = CR）
			MAccount apAcct = getIntercompanyDueTo(as, companyOrgId, bizUnitOrgId);
			if (apAcct == null)
				continue;
			FactLine apLine = fact.createLine(null, apAcct, as.getC_Currency_ID(), totalCost.negate());
			if (apLine != null)
				apLine.setAD_Org_ID(bizUnitOrgId);

			// 3. 公司 DR 应收账款-事业部（正数 = DR）
			MAccount arAcct = getIntercompanyDueFrom(as, companyOrgId, bizUnitOrgId);
			if (arAcct == null)
				continue;
			FactLine arLine = fact.createLine(null, arAcct, as.getC_Currency_ID(), totalCost);
			if (arLine != null)
				arLine.setAD_Org_ID(companyOrgId);
		}
	}

	/**
	 * 从 C_InterOrg_Acct 查询往来科目, 没有则用会计账套的总账配置的内部应付字段，
	 * 目前用的是会计账套的总账配置的内部应付字段，C_InterOrg_Acct表暂时没用
	 * 
	 * @Title: getIntercompanyDueTo
	 * @param as
	 * @param fromOrg
	 * @param toOrg
	 * @return MAccount
	 */
	private MAccount getIntercompanyDueTo(MAcctSchema as, int fromOrg, int toOrg) {
		X_C_InterOrg_Acct acct = new Query(Env.getCtx(), X_C_InterOrg_Acct.Table_Name,
				"C_AcctSchema_ID=? AND AD_Org_ID=? AND AD_OrgTo_ID=?", null).setParameters(as.get_ID(), fromOrg, toOrg)
				.first();
		if (acct != null)
			return MAccount.get(acct.getIntercompanyDueTo_Acct());
		return as.getDueTo_Acct("Org"); // fallback
	}

	/**
	 * 从 C_InterOrg_Acct 查询往来科目, 没有则用会计账套的总账配置的内部应收字段
	 * 
	 * @Title: getIntercompanyDueTo
	 * @param as
	 * @param fromOrg
	 * @param toOrg
	 * @return MAccount
	 */
	private MAccount getIntercompanyDueFrom(MAcctSchema as, int fromOrg, int toOrg) {
		X_C_InterOrg_Acct acct = new Query(Env.getCtx(), X_C_InterOrg_Acct.Table_Name,
				"C_AcctSchema_ID=? AND AD_Org_ID=? AND AD_OrgTo_ID=?", null).setParameters(as.get_ID(), fromOrg, toOrg)
				.first();
		if (acct != null)
			return MAccount.get(acct.getIntercompanyDueFrom_Acct());
		return as.getDueFrom_Acct("Org"); // fallback
	}

	/**
	 * 根据科目编码查询科目信息
	 * 
	 * @Title: getAccountByValue
	 * @param as
	 * @param accountValue
	 * @param trxName
	 * @return MAccount
	 */
	private MAccount getAccountByValue(MAcctSchema as, String accountValue, String trxName) {
		MElementValue ev = new Query(Env.getCtx(), MElementValue.Table_Name,
				"Value=? AND C_Element_ID IN " + "(SELECT C_Element_ID FROM C_AcctSchema_Element "
						+ " WHERE C_AcctSchema_ID=? AND ElementType='AC')",
				null).setParameters(accountValue, as.get_ID()).first();
		if (ev == null)
			return null;
		return MAccount.get(Env.getCtx(), as.getAD_Client_ID(), 0, as.get_ID(), ev.get_ID(), 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, trxName);
	}

}