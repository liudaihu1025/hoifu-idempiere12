package com.hoifu.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MFactAcct;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.engine.JsonCashFlowRuleEngine;
import com.hoifu.model.MCashFlowConfig;
import com.hoifu.model.MCashFlowItem;
import com.hoifu.model.MFactCashFlow;

/**
 * 
 * @Description: 现金流量数据同步Service
 * @author ldh
 * @date 2025年12月9日
 */
public class CashFlowSyncService {

	private final static CLogger log = CLogger.getCLogger(JsonCashFlowRuleEngine.class);
	
	/**
	 * 同步现金流量数据
	 */
	public void syncCashFlowData() {
		Properties ctx = Env.getCtx();
		MCashFlowConfig config = new Query(ctx, MCashFlowConfig.Table_Name, "AD_Client_ID=? AND IsActive='Y'", null)
				.setParameters(Env.getAD_Client_ID(ctx)).first();

		if (config == null || !config.isEnabled()) {
			return;
		}

		try {
			// 获取需要同步的会计分录
			List<MFactAcct> factAccts = getUnsyncedFactAccts(config);

			// 生成现金流量分录
			List<MFactCashFlow> cashFlowEntries = factAccts.stream().filter(this::isCashAccount)
					.map(this::createCashFlowEntry).filter(java.util.Objects::nonNull).collect(Collectors.toList());

			// 保存现金流量分录
			cashFlowEntries.forEach(entry -> {
				if (entry.save()) {
					updateFactAcctProcessed(entry.getFact_Acct_ID());
				}
			});

			// 更新最后同步时间
			config.setLastSyncTime(new Timestamp(System.currentTimeMillis()));
			config.save();

		} catch (Exception e) {
			log.warning("同步现金流量数据异常:" + e.getMessage());
			throw new AdempiereException("同步现金流量数据异常", e);
		}
	}

	/**
	 * 获取未同步的会计分录
	 */
	private List<MFactAcct> getUnsyncedFactAccts(MCashFlowConfig config) {
		Properties ctx = Env.getCtx();
		String whereClause = " DateAcct > ? AND AD_Client_ID=? "
				+ "AND Fact_Acct_ID NOT IN (SELECT Fact_Acct_ID FROM Fact_CashFlow)";

		Timestamp lastSyncTime = config.getLastSyncTime();
		if (lastSyncTime == null) {
			lastSyncTime = Timestamp.valueOf("2000-01-01 00:00:00");
		}

		return new Query(Env.getCtx(), MFactAcct.Table_Name, whereClause, null).setParameters(lastSyncTime, Env.getAD_Client_ID(ctx))
				.setOrderBy("DateAcct, Fact_Acct_ID").list();
	}

	/**
	 * 判断是否为现金科目
	 */
	private boolean isCashAccount(MFactAcct factAcct) {
		String sql = "SELECT COUNT(*) FROM C_ElementValue ev " + "WHERE ev.C_ElementValue_ID = ? "
				+ "AND ev.AccountType IN ('A', 'B') " + "AND ev.IsSummary = 'N' "
				+ "AND ( ev.Value LIKE '1001%' OR ev.Value LIKE '1002%')";

		int accountID = factAcct.getAccount_ID();
		int count = DB.getSQLValueEx(null, sql, accountID);
		return count > 0;
	}

	/**
	 * 创建现金流量分录
	 */
	private MFactCashFlow createCashFlowEntry(MFactAcct factAcct) {
		// 使用JSONB规则引擎确定现金流量项目
		Integer cashFlowItemId = JsonCashFlowRuleEngine.determineCashFlowItem(factAcct)
				.orElseGet(() -> getDefaultCashFlowItem(factAcct));

		if (cashFlowItemId == null) {
			return null;
		}

		MFactCashFlow cashFlow = new MFactCashFlow(Env.getCtx(), 0, null);

		// 复制基础信息
//		cashFlow.setAD_Client_ID(factAcct.getAD_Client_ID());
		cashFlow.setAD_Org_ID(factAcct.getAD_Org_ID());
		cashFlow.setFact_Acct_ID(factAcct.getFact_Acct_ID());
		cashFlow.setC_CashFlow_Item_ID(cashFlowItemId);

		// 获取现金流量项目信息
		MCashFlowItem item = new MCashFlowItem(Env.getCtx(), cashFlowItemId, null);
		cashFlow.setCashFlow_Type(item.getCashFlow_Type());
		cashFlow.setIsCashIn(item.is_CashIn());

		// 确定金额和方向
		BigDecimal amount = factAcct.getAmtAcctDr().compareTo(BigDecimal.ZERO) > 0 
				? factAcct.getAmtAcctDr()
				: factAcct.getAmtAcctCr();
		cashFlow.setAmount(amount);

		// 复制业务信息
		cashFlow.setDateAcct(factAcct.getDateAcct());
		cashFlow.setDateDoc(factAcct.getDateTrx());
		cashFlow.setC_Period_ID(factAcct.getC_Period_ID());
		cashFlow.setAD_Table_ID(factAcct.getAD_Table_ID());
		cashFlow.setRecord_ID(factAcct.getRecord_ID());
		cashFlow.setLine_ID(factAcct.getLine_ID());
		cashFlow.setC_Currency_ID(factAcct.getC_Currency_ID());
		cashFlow.setDescription(factAcct.getDescription());

		return cashFlow;
	}

	/**
	 * 获取默认现金流量项目
	 */
	private Integer getDefaultCashFlowItem(MFactAcct factAcct) {
		// 获取现金科目的对方科目
		int counterAccountID = getCounterAccountID(factAcct);

		// 获取C_ValidCombination_ID
		String sql = "SELECT C_ValidCombination_ID FROM C_ValidCombination "
				+ "WHERE Account_ID = ? AND AD_Client_ID = ? AND IsActive = 'Y' ORDER BY C_ValidCombination_ID DESC LIMIT 1";
		Integer validCombinationId = DB.getSQLValue(null, sql, counterAccountID, Env.getAD_Client_ID(Env.getCtx()));

		if (validCombinationId == null || validCombinationId <= 0) {
			return null;
		}
		// 然后获取MAccount对象
		MAccount counterAccount = MAccount.get(Env.getCtx(), validCombinationId);

		// 简单的默认逻辑：根据描述推断
		String description = counterAccount.getDescription();
		if (description == null)
			return null;

		if (description.contains("销售") || description.contains("收入") || description.contains("应收") || description.contains("收款")) {
			return 1; // 销售商品收到的现金
		} else if (description.contains("采购") || description.contains("购买") || description.contains("付款") || description.contains("应付")) {
			return 2; // 购买商品支付的现金
		} else if (description.contains("工资") || description.contains("薪酬")) {
			return 3; // 支付职工薪酬
		} else if (description.contains("税")) {
			return 4; // 支付各项税费
		} else if (description.contains("固定资产")) {
			return 5; // 购建固定资产支付的现金
		} else if (description.contains("借款")) {
			return 6; // 取得借款收到的现金
		} else if (description.contains("偿还") || description.contains("债务")) {
			return 7; // 偿还债务支付的现金
		}

		return null;
	}

	/**
	 * 获取对方科目ID
	 */
	private static int getCounterAccountID(MFactAcct factAcct) {
		String sql = "SELECT Account_ID FROM Fact_Acct " + "WHERE AD_Table_ID = ? AND Record_ID = ? "
				+ "AND Fact_Acct_ID != ? " + "AND (AmtAcctDr > 0 OR AmtAcctCr > 0)";

		return DB.getSQLValue(null, sql, factAcct.getAD_Table_ID(), factAcct.getRecord_ID(),
				factAcct.getFact_Acct_ID());
	}

	/**
	 * 更新会计分录处理状态
	 */
	private void updateFactAcctProcessed(int factAcctId) {
		// 可以通过添加处理标记或更新状态来实现
		// 这里简化处理，实际可以添加字段或日志表
	}
}