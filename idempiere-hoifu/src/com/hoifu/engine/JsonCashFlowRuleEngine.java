package com.hoifu.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MFactAcct;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONObject;

import com.hoifu.model.MCashFlowRule;

/**
 * 
 * @Description: 现金流量规则处理引擎
 * @author ldh
 * @date 2025年12月9日
 */
public class JsonCashFlowRuleEngine {

	private final static CLogger log = CLogger.getCLogger(JsonCashFlowRuleEngine.class);

	/**
	 * 根据JSONB规则确定现金流量项目
	 */
	public static Optional<Integer> determineCashFlowItem(MFactAcct factAcct) {
		List<MCashFlowRule> rules = getActiveRules();

		return rules.stream().filter(rule -> matchesJsonRule(rule, factAcct)).findFirst()
				.map(MCashFlowRule::getC_CashFlow_Item_ID);
	}
	
	/**
	 * 获取活跃规则
	 */
	private static List<MCashFlowRule> getActiveRules() {
		String whereClause = "AD_Client_ID=? AND IsActive='Y'";
		return new Query(Env.getCtx(), MCashFlowRule.Table_Name, whereClause, null)
				.setParameters(Env.getAD_Client_ID(Env.getCtx())).setOrderBy("Sequence").list();
	}

	/**
	 * 检查JSONB规则是否匹配
	 */
	private static boolean matchesJsonRule(MCashFlowRule rule, MFactAcct factAcct) {
		try {
			String conditionsJson = rule.getConditions();
			if (conditionsJson == null)
				return false;

			JSONObject conditions = new JSONObject(conditionsJson);
			return evaluateJsonConditions(conditions, factAcct);

		} catch (Exception e) {
			log.warning("检查JSONB规则是否匹配异常:" + e.getMessage());
			return false;
		}
	}

	/**
	 * 评估JSON条件
	 */
	private static boolean evaluateJsonConditions(JSONObject conditions, MFactAcct factAcct) {
		// 账户条件
		if (conditions.has("account_id")) {
			JSONObject accountCond = conditions.getJSONObject("account_id");
			if (!evaluateCondition(accountCond, factAcct.getAccount_ID())) {
				return false;
			}
		}

		// 单据类型条件
		if (conditions.has("doc_base_type")) {
			JSONObject docCond = conditions.getJSONObject("doc_base_type");
			String docBaseType = getDocBaseType(factAcct);
			if (!evaluateCondition(docCond, docBaseType)) {
				return false;
			}
		}

		// 收付款方向条件
		if (conditions.has("is_receipt")) {
			JSONObject receiptCond = conditions.getJSONObject("is_receipt");
			boolean isReceipt = factAcct.getAmtAcctDr().compareTo(BigDecimal.ZERO) > 0;
			if (!evaluateCondition(receiptCond, isReceipt)) {
				return false;
			}
		}

		// 金额条件
		if (conditions.has("amount_range")) {
			JSONObject amountCond = conditions.getJSONObject("amount_range");
			BigDecimal amount = factAcct.getAmtAcctDr().compareTo(BigDecimal.ZERO) > 0 ? factAcct.getAmtAcctDr()
					: factAcct.getAmtAcctCr();
			if (!evaluateCondition(amountCond, amount)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 评估单个条件
	 */
	private static boolean evaluateCondition(JSONObject condition, Object actualValue) {
		try {
			String operator = condition.getString("operator");
			Object expectedValue = condition.get("value");

			switch (operator) {
			case "=":
				return actualValue.toString().equals(expectedValue.toString());
			case "!=":
				return !actualValue.toString().equals(expectedValue.toString());
			case ">":
				if (actualValue instanceof Number && expectedValue instanceof Number) {
					return ((Number) actualValue).doubleValue() > ((Number) expectedValue).doubleValue();
				}
				break;
			case "<":
				if (actualValue instanceof Number && expectedValue instanceof Number) {
					return ((Number) actualValue).doubleValue() < ((Number) expectedValue).doubleValue();
				}
				break;
			case "LIKE":
				return actualValue.toString().matches(expectedValue.toString().replace("%", ".*"));
			case "IN":
				String[] values = expectedValue.toString().split(",");
				for (String value : values) {
					if (actualValue.toString().equals(value.trim())) {
						return true;
					}
				}
				return false;
			}
		} catch (Exception e) {
			log.warning("检查JSONB现金流量规则评估单个条件异常:" + e.getMessage());
			return false;
		}
		return false;
	}

	/**
	 * 获取单据类型
	 */
	private static String getDocBaseType(MFactAcct factAcct) {
		String sql = "SELECT dt.DocBaseType FROM C_DocType dt " + "JOIN AD_Table t ON dt.C_DocType_ID = t.AD_Table_ID "
				+ "WHERE t.AD_Table_ID = ?";
		return DB.getSQLValueString(null, sql, factAcct.getAD_Table_ID());
	}
}