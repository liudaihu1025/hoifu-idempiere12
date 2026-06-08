package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.MPeriod;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.hoifu.config.BalanceSheetConfig;

/**
 * 财务报表 - 资产负债表
 * 
 * 功能说明： 1. 根据选定的账套、组织、期间、过账类型，生成资产负债表。 2.
 * 支持公式解析：科目编码（如1001）、模糊匹配（如1122%）、项目名称引用（如“现金”）、加减运算。 3.
 * 支持单独取借方或贷方发生额：在科目编码后加#D（借方）或#C（贷方），如112201#D。 4. 资产类年初/期末 = 借方-贷方；负债/权益类年初/期末
 * = 贷方-借方（自动取反）。 5. 一次性加载所有科目余额（含无发生额的科目信息），高性能计算。 6. 结果批量插入T_BalanceSheet表。
 * 
 * @ClassName: BalanceSheet
 * @author ldh
 * @date 2026年4月20日
 */
@org.adempiere.base.annotation.Process
public class BalanceSheet extends SvrProcess {

	// ==================== 报表参数 ====================
	private int p_C_AcctSchema_ID = 0; // 会计模式ID
	private int p_C_Period_ID = 0; // 期间ID
	private Timestamp p_DateAcct_From = null; // 期间开始日期（实际从期间获取）
	private Timestamp p_DateAcct_To = null; // 期间结束日期
	private int p_AD_Org_ID = 0; // 组织ID（0表示所有组织）
	private String p_PostingType = "A"; // 过账类型（A=实际，B=预算等）

	// ==================== 日期边界 ====================
	private Timestamp yearStart; // 所选年份的1月1日 00:00:00（用于期初余额）
	private Timestamp periodEnd; // 所选期间的最后一天 23:59:59（用于期末余额）

	// ==================== 数据缓存 ====================
	// 科目余额缓存：key=科目编码，value=期初/期末的借方、贷方累计值
	private Map<String, Balance> accountCache = new HashMap<>();
	// 科目信息缓存：key=科目编码，value=科目ID、编码、名称
	private Map<String, AccountInfo> accountInfoCache = new HashMap<>();
	// ★ 新增：科目方向缓存（D=借方，C=贷方，N=中性）
	private Map<String, String> accountSignCache = new HashMap<>();

	// 报表项计算结果缓存：左侧资产类、右侧负债权益类
	private Map<String, RowResult> leftResultCache = new LinkedHashMap<>();
	private Map<String, RowResult> rightResultCache = new LinkedHashMap<>();

	// 公式映射：项目名称 -> 计算公式（从配置类加载）
	private Map<String, String> leftFormulaMap = new LinkedHashMap<>(); // 左侧资产类公式
	private Map<String, String> rightFormulaMap = new LinkedHashMap<>(); // 右侧负债权益类公式

	// ==================== 生命周期方法 ====================
	@Override
	protected void prepare() {
		// 1. 获取用户传入的参数
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			String name = p.getParameterName();
			if (p.getParameter() == null)
				continue;
			else if (name.equals("C_AcctSchema_ID"))
				p_C_AcctSchema_ID = p.getParameterAsInt();
			else if (name.equals("C_Period_ID"))
				p_C_Period_ID = p.getParameterAsInt();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = p.getParameterAsInt();
			else if (name.equals("PostingType"))
				p_PostingType = (String) p.getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
		}

		// 2. 校验必要参数
		if (p_C_AcctSchema_ID <= 0)
			throw new RuntimeException("会计模式不能为空");
		if (p_PostingType == null || p_PostingType.trim().isEmpty())
			p_PostingType = "A";
		if (p_C_Period_ID <= 0)
			throw new RuntimeException("期间不能为空");

		// 3. 根据期间ID获取起止日期
		try {
			MPeriod period = new MPeriod(getCtx(), p_C_Period_ID, get_TrxName());
			p_DateAcct_From = period.getStartDate();
			p_DateAcct_To = period.getEndDate();
		} catch (Exception e) {
			throw new RuntimeException("获取期间失败", e);
		}

		// 4. 计算年初日期（所选年份1月1日 00:00:00）
		Calendar cal = Calendar.getInstance();
		cal.setTime(p_DateAcct_From);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		yearStart = new Timestamp(cal.getTimeInMillis());

		// 5. 计算期末日期（期间最后一天 23:59:59）
		cal.setTime(p_DateAcct_To);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		periodEnd = new Timestamp(cal.getTimeInMillis());

		// 6. 从配置类加载公式映射（左侧资产类、右侧负债权益类）
		leftFormulaMap = BalanceSheetConfig.getLeftFormulaMap();
		rightFormulaMap = BalanceSheetConfig.getRightFormulaMap();
		log.fine("加载左侧公式 " + leftFormulaMap.size() + " 条，右侧公式 " + rightFormulaMap.size() + " 条");
	}

	@Override
	protected String doIt() throws Exception {
		long start = System.currentTimeMillis();

		loadAccountBalances();
		computeAll(leftFormulaMap, leftResultCache);
		computeAll(rightFormulaMap, rightResultCache);
		deleteOldRecords();
		insertResults();

		long end = System.currentTimeMillis();
		log.info("资产负债表生成完成，耗时: " + (end - start) + " ms");
		return "资产负债表生成完成";
	}

	// ==================== 科目余额加载 ====================

	/**
	 * 递归收集所有公式中用到的科目前缀（用于加载科目信息）
	 * 
	 * @param formulaMap 公式映射
	 * @param prefixes   用于存储科目前缀的集合
	 */
	private void collectAllPrefixes(Map<String, String> formulaMap, Set<String> prefixes) {
		Set<String> visited = new HashSet<>();
		for (String item : formulaMap.keySet()) {
			collectPrefixesFromItem(item, formulaMap, visited, prefixes);
		}
	}

	/**
	 * 递归处理单个项目，提取其公式中的科目前缀
	 * @Title: collectPrefixesFromItem
	 * @param item
	 * @param formulaMap
	 * @param visited
	 * @param prefixes
	 * @return void
	 */
	private void collectPrefixesFromItem(String item, Map<String, String> formulaMap, Set<String> visited,
			Set<String> prefixes) {
		if (visited.contains(item))
			return;
		visited.add(item);
		String formula = formulaMap.get(item);
		if (formula == null)
			return;
		// 按 + 和 - 分割公式
		for (String token : formula.split("[+\\-]")) {
			token = token.trim();
			// 去除方向后缀（#D, #C）后再判断
			String pureToken = token;
			if (pureToken.contains("#")) {
				pureToken = pureToken.split("#")[0].trim();
			}
			if (pureToken.matches("\\d+%?")) {
				String prefix = pureToken.endsWith("%") ? pureToken.substring(0, pureToken.length() - 1) : pureToken;
				prefixes.add(prefix);
			} else if (formulaMap.containsKey(pureToken)) {
				collectPrefixesFromItem(pureToken, formulaMap, visited, prefixes);
			}
		}
	}

	/**
	 * 加载所有需要的科目余额及科目信息，同时缓存 AccountSign。
	 */
	private void loadAccountBalances() {
		// 1. 收集所有科目前缀（用于查询科目信息）
		Set<String> prefixes = new HashSet<>();
		collectAllPrefixes(leftFormulaMap, prefixes);
		collectAllPrefixes(rightFormulaMap, prefixes);
		log.info("收集到科目前缀数量: " + prefixes.size());

		// 2. 加载科目信息（从 C_ElementValue，不依赖发生额）
		if (!prefixes.isEmpty()) {
			StringBuilder like = new StringBuilder();
			for (String p : prefixes) {
				if (like.length() > 0)
					like.append(" OR ");
				like.append("Value LIKE '").append(p).append("%'");
			}
			// ★ 增加 AccountSign 字段
			String infoSql = "SELECT C_ElementValue_ID, Value, Name, AccountSign FROM C_ElementValue "
					+ "WHERE AD_Client_ID = ? AND (" + like.toString() + ") AND IsActive='Y' "
					+ "and c_element_id = (SELECT C_Element_ID FROM C_AcctSchema_Element WHERE ElementType='AC' AND C_AcctSchema_ID= "
					+ p_C_AcctSchema_ID + " )";
			try (java.sql.PreparedStatement ps = DB.prepareStatement(infoSql, get_TrxName())) {
				ps.setInt(1, getAD_Client_ID());
				try (java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						AccountInfo info = new AccountInfo(rs.getInt(1), rs.getString(2), rs.getString(3));
						accountInfoCache.put(info.value, info);
						accountSignCache.put(info.value, rs.getString(4)); // ★ 存储科目方向
					}
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "加载科目信息失败", e);
				throw new RuntimeException("加载科目信息失败: " + e.getMessage(), e);
			}
			log.info("加载科目信息完成，共 " + accountInfoCache.size() + " 个科目");
		} else {
			log.warning("未收集到任何科目前缀，请检查公式配置");
		}

		// 3. 加载余额（分别查询借方和贷方累计）
		String where = "C_AcctSchema_ID = " + p_C_AcctSchema_ID + " AND PostingType = '" + p_PostingType + "'";
		if (p_AD_Org_ID != 0)
			where += " AND fa.AD_Org_ID = " + p_AD_Org_ID;

		// ★ 增加 ev.AccountSign，GROUP BY 也加上
		String sql = "SELECT ev.Value, ev.AccountSign, "
				+ "  COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctDr ELSE 0 END), 0) AS BeginDr, "
				+ "  COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctCr ELSE 0 END), 0) AS BeginCr, "
				+ "  COALESCE(SUM(CASE WHEN fa.DateAcct <= ? THEN fa.AmtAcctDr ELSE 0 END), 0) AS EndDr, "
				+ "  COALESCE(SUM(CASE WHEN fa.DateAcct <= ? THEN fa.AmtAcctCr ELSE 0 END), 0) AS EndCr "
				+ "FROM Fact_Acct fa " + "INNER JOIN C_ElementValue ev ON ev.C_ElementValue_ID = fa.Account_ID "
				+ "WHERE fa.AD_Client_ID = ? " + "  AND " + where + " " + "  AND ev.IsActive = 'Y' "
				+ "  AND ev.C_Element_ID = (SELECT C_Element_ID FROM C_AcctSchema_Element "
				+ "WHERE ElementType = 'AC' AND C_AcctSchema_ID =" + p_C_AcctSchema_ID + ") "
				+ "GROUP BY ev.Value, ev.AccountSign"; // ★

		try (java.sql.PreparedStatement ps = DB.prepareStatement(sql, get_TrxName())) {
			ps.setTimestamp(1, yearStart);
			ps.setTimestamp(2, yearStart);
			ps.setTimestamp(3, periodEnd);
			ps.setTimestamp(4, periodEnd);
			ps.setInt(5, getAD_Client_ID());
			try (java.sql.ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String value = rs.getString(1);
					String accountSign = rs.getString(2); // ★
					BigDecimal beginDr = rs.getBigDecimal(3);
					BigDecimal beginCr = rs.getBigDecimal(4);
					BigDecimal endDr = rs.getBigDecimal(5);
					BigDecimal endCr = rs.getBigDecimal(6);
					accountCache.put(value, new Balance(beginDr, beginCr, endDr, endCr));
					accountSignCache.put(value, accountSign); // ★ 存储科目方向
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "加载科目余额失败", e);
			throw new RuntimeException("加载科目余额失败: " + e.getMessage(), e);
		}
		log.info("加载科目余额完成，共 " + accountCache.size() + " 个科目");
	}

	// ==================== 公式计算引擎 ====================

	private void computeAll(Map<String, String> formulaMap, Map<String, RowResult> resultCache) {
		Set<String> computing = new HashSet<>();
		for (String item : formulaMap.keySet()) {
			if (!resultCache.containsKey(item)) {
				computeItem(item, formulaMap, resultCache, computing);
			}
		}
	}

	/**
	 * 递归计算单个项目。 ★ 移除原来基于 isLeft 的取反逻辑，方向已由 AccountSign 在 getTokenValue() 中决定。
	 */
	private void computeItem(String item, Map<String, String> formulaMap, Map<String, RowResult> resultCache,
			Set<String> computing) {
		if (resultCache.containsKey(item))
			return;
		if (computing.contains(item)) {
			String errMsg = "检测到循环依赖: " + item;
			log.severe(errMsg);
			throw new RuntimeException(errMsg);
		}
		computing.add(item);
		String formula = formulaMap.get(item);
		log.fine("计算项目: " + item + " 公式=" + formula);

		// ★ 直接使用公式计算结果，方向已由 AccountSign 在 getTokenValue() 中决定
		BigDecimal begin = evaluateFormula(formula, true, resultCache, formulaMap, computing);
		BigDecimal end = evaluateFormula(formula, false, resultCache, formulaMap, computing);

		// ★ 移除原来基于 isLeft 的取反逻辑

		AccountInfo accInfo = getAccountInfoFromFormula(formula, formulaMap);
		RowResult rowRes = new RowResult(begin, end, accInfo);
		resultCache.put(item, rowRes);
		computing.remove(item);
		log.fine("项目 " + item + " 期初=" + begin + " 期末=" + end);
	}

	/**
	 * 从公式中提取科目信息（仅当公式代表单一科目时返回）
	 * 
	 * @param formula    公式字符串
	 * @param formulaMap 公式映射（用于解析项目名称引用）
	 * @return 科目信息，否则返回null
	 */
	private AccountInfo getAccountInfoFromFormula(String formula, Map<String, String> formulaMap) {
		if (formula == null || formula.trim().isEmpty())
			return null;
		formula = formula.trim();
        // 只有无运算符的简单公式才能代表单个科目
		if (!formula.contains("+") && !formula.contains("-") && formula.length() > 3) {
			return getAccountInfoFromToken(formula, formulaMap);
		}
		// 复合公式，不记录科目信息
		return null;
	}

	/**
	 * 从单个token中提取科目信息（token可以是科目编码、模糊匹配、项目名称）
	 * 
	 * @param token      单个表达式单元
	 * @param formulaMap 公式映射
	 * @return 科目信息，否则返回null
	 */
	private AccountInfo getAccountInfoFromToken(String token, Map<String, String> formulaMap) {
		if (token == null || token.isEmpty())
			return null;

		String pureToken = token.contains("#") ? token.split("#")[0].trim() : token;

		if (pureToken.matches("\\d+")) {
			return accountInfoCache.computeIfAbsent(pureToken, k -> {
				AccountInfo info = loadAccountInfoDirect(k);
				if (info != null)
					log.fine("兜底加载科目信息: " + k + " -> " + info.name);
				else
					log.fine("未找到科目信息: " + k);
				return info;
			});
		}

		if (pureToken.endsWith("%")) {
			String prefix = pureToken.substring(0, pureToken.length() - 1);
			return accountInfoCache.values().stream().filter(acc -> acc.value.equals(prefix)).findFirst()
					.orElseGet(() -> {
						AccountInfo info = loadAccountInfoDirect(prefix);
						if (info != null) {
							accountInfoCache.put(prefix, info);
							log.fine("模糊匹配兜底加载科目信息: " + prefix + " -> " + info.name);
						}
						return info;
					});
		}

		if (formulaMap.containsKey(pureToken)) {
			return getAccountInfoFromFormula(formulaMap.get(pureToken), formulaMap);
		}

		log.fine("无法识别的token: " + token);
		return null;
	}

	/**
	 * 直接从数据库查询单个科目的信息（兜底方法），同时存入 accountSignCache。
	 */
	private AccountInfo loadAccountInfoDirect(String accountValue) {
		// ★ 增加 AccountSign 字段
		String sql = "SELECT C_ElementValue_ID, Value, Name, AccountSign FROM C_ElementValue "
				+ "WHERE Value = ? AND AD_Client_ID = ? AND IsActive='Y'";
		try (java.sql.PreparedStatement ps = DB.prepareStatement(sql, get_TrxName())) {
			ps.setString(1, accountValue);
			ps.setInt(2, getAD_Client_ID());
			try (java.sql.ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					AccountInfo info = new AccountInfo(rs.getInt(1), rs.getString(2), rs.getString(3));
					accountSignCache.put(info.value, rs.getString(4)); // ★
					return info;
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "直接查询科目信息失败: " + accountValue, e);
		}
		return null;
	}

    /**
     * 计算公式的值（返回借方-贷方原始余额，或根据方向返回借方/贷方累计）
     *
     * @param formula     公式字符串
     * @param isBeginning true=期初，false=期末
     * @param resultCache 当前侧的结果缓存
     * @param formulaMap  公式映射
     * @param computing   循环检测集合
     * @return 计算得到的金额
     */
	private BigDecimal evaluateFormula(String formula, boolean isBeginning, Map<String, RowResult> resultCache,
			Map<String, String> formulaMap, Set<String> computing) {
		if (formula == null || formula.trim().isEmpty())
			return BigDecimal.ZERO;
		formula = formula.trim();
		if (!formula.contains("+") && !formula.contains("-")) {
			return getTokenValue(formula, isBeginning, resultCache, formulaMap, computing);
		}
		String[] tokens = formula.split("(?=[+-])");
		BigDecimal result = BigDecimal.ZERO;
		for (String token : tokens) {
			token = token.trim();
			boolean positive = true;
			if (token.startsWith("-")) {
				positive = false;
				token = token.substring(1).trim();
			} else if (token.startsWith("+")) {
				token = token.substring(1).trim();
			}
			BigDecimal val = getTokenValue(token, isBeginning, resultCache, formulaMap, computing);
			result = positive ? result.add(val) : result.subtract(val);
		}
		return result;
	}

	/**
	 * 获取单个token的值（支持方向后缀）。 ★ 当 direction==null 时，根据 AccountSign 决定净额方向，替代原来固定的
	 * Dr-Cr。
	 */
	private BigDecimal getTokenValue(String token, boolean isBeginning, Map<String, RowResult> resultCache,
			Map<String, String> formulaMap, Set<String> computing) {
		if (token.isEmpty())
			return BigDecimal.ZERO;

		// 解析方向后缀（#D, #C）
		String direction = null;
		String pureToken = token;
		if (token.contains("#")) {
			String[] parts = token.split("#");
			if (parts.length == 2) {
				pureToken = parts[0].trim();
				direction = parts[1].trim().toUpperCase();
				if (!direction.equals("D") && !direction.equals("C")) {
					log.warning("无效的方向后缀: " + token);
					direction = null;
				}
			}
		}

		// 模糊匹配（如 1122% 或 1122%#D）
		if (pureToken.endsWith("%")) {
			String prefix = pureToken.substring(0, pureToken.length() - 1);
			BigDecimal total = BigDecimal.ZERO;
			for (Map.Entry<String, Balance> e : accountCache.entrySet()) {
				if (e.getKey().startsWith(prefix)) {
					Balance bal = e.getValue();
					if (direction == null) {
						// ★ 根据每个科目自身的 AccountSign 决定净额方向
						total = total.add(calcNetBySign(e.getKey(), bal, isBeginning));
					} else if (direction.equals("D")) {
						total = total.add(isBeginning ? bal.beginDr : bal.endDr);
					} else {
						total = total.add(isBeginning ? bal.beginCr : bal.endCr);
					}
				}
			}
			return total;
		}

		// 精确科目编码
		if (pureToken.matches("\\d+")) {
			Balance bal = accountCache.get(pureToken);
			if (bal == null)
				return BigDecimal.ZERO;
			if (direction == null) {
				// ★ 根据科目 AccountSign 决定净额方向
				return calcNetBySign(pureToken, bal, isBeginning);
			} else if (direction.equals("D")) {
				return isBeginning ? bal.beginDr : bal.endDr;
			} else {
				return isBeginning ? bal.beginCr : bal.endCr;
			}
		}

		// 项目名称引用（直接取已计算好的结果，方向已在子项中处理）
		if (resultCache.containsKey(pureToken)) {
			RowResult ref = resultCache.get(pureToken);
			return isBeginning ? ref.begin : ref.end;
		}
		if (formulaMap.containsKey(pureToken)) {
			computeItem(pureToken, formulaMap, resultCache, computing);
			RowResult ref = resultCache.get(pureToken);
			return isBeginning ? ref.begin : ref.end;
		}

		log.warning("无法解析token: " + token);
		return BigDecimal.ZERO;
	}

	/**
	 * ★ 新增：根据科目方向（AccountSign）计算净额 D → 借方 - 贷方（借方科目，余额在借方） C → 贷方 - 借方（贷方科目，余额在贷方）
	 * N → 借方 - 贷方（中性，正数=借方余额，负数=贷方余额） 未知 → 默认按 D 处理
	 */
	private BigDecimal calcNetBySign(String accountValue, Balance bal, boolean isBeginning) {
		String sign = accountSignCache.getOrDefault(accountValue, "D");
		if ("C".equals(sign)) {
			// 贷方科目：贷方 - 借方
			return isBeginning ? bal.beginCr.subtract(bal.beginDr) : bal.endCr.subtract(bal.endDr);
		} else if ("N".equals(sign)) {
			// 中性科目：用金额大的一方 - 另一方，结果始终为正数
			BigDecimal net = isBeginning ? bal.beginDr.subtract(bal.beginCr) : bal.endDr.subtract(bal.endCr);
			return net.abs();
		} else {
			// 借方科目（D）：借方 - 贷方
			return isBeginning ? bal.beginDr.subtract(bal.beginCr) : bal.endDr.subtract(bal.endCr);
		}
	}

	// ==================== 数据持久化 ====================

	/**
	 * 删除本次处理实例的旧数据
	 */
	private void deleteOldRecords() {
		String sql = "DELETE FROM T_BalanceSheet WHERE AD_PInstance_ID = ? AND AD_Client_ID = ?";
		DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID(), getAD_Client_ID() }, get_TrxName());
	}

	/**
	 * 将计算结果按配置顺序插入数据库
	 */
	private void insertResults() {
		String[][] configItems = BalanceSheetConfig.getBalanceSheetItems();
		List<Object[]> batch = new ArrayList<>();

		for (String[] item : configItems) {
			if (item.length < 3)
				continue;
			int seq = Integer.parseInt(item[0]);
			String leftNameRaw = item[1];
			String rightNameRaw = item[2];

			// 去除首尾全角空格和半角空格，用于匹配缓存中的key（缓存中的key没有空格）
			String leftKey = leftNameRaw.replaceAll("^[\\s\\u3000]+|[\\s\\u3000]+$", "");
			String rightKey = rightNameRaw.replaceAll("^[\\s\\u3000]+|[\\s\\u3000]+$", "");

			RowResult leftRes = leftResultCache.get(leftKey);
			RowResult rightRes = rightResultCache.get(rightKey);

			boolean isLeftTitle = leftNameRaw.endsWith(":") || leftNameRaw.isEmpty();
			boolean isRightTitle = rightNameRaw.endsWith(":") || rightNameRaw.isEmpty();

			// 处理左侧（金额和科目信息）
			BigDecimal leftBegin = null, leftEnd = null;
			Integer leftAccountId = null;
			String leftAccountValue = null, leftAccountName = null;
			if (isLeftTitle) {
				// 标题行：金额为 null
				leftBegin = null;
				leftEnd = null;
			} else {
				if (leftRes != null) {
					leftBegin = leftRes.begin;
					leftEnd = leftRes.end;
					if (leftRes.accountInfo != null) {
						leftAccountId = leftRes.accountInfo.id;
						leftAccountValue = leftRes.accountInfo.value;
						leftAccountName = leftRes.accountInfo.name;
					}
				} else {
					// 非标题行无计算结果时默认为0
					leftBegin = BigDecimal.ZERO;
					leftEnd = BigDecimal.ZERO;
				}
			}

			// 处理右侧（金额和科目信息）
			BigDecimal rightBegin = null, rightEnd = null;
			Integer rightAccountId = null;
			String rightAccountValue = null, rightAccountName = null;
			if (isRightTitle) {
				rightBegin = null;
				rightEnd = null;
			} else {
				if (rightRes != null) {
					rightBegin = rightRes.begin;
					rightEnd = rightRes.end;
					if (rightRes.accountInfo != null) {
						rightAccountId = rightRes.accountInfo.id;
						rightAccountValue = rightRes.accountInfo.value;
						rightAccountName = rightRes.accountInfo.name;
					}
				} else {
					rightBegin = BigDecimal.ZERO;
					rightEnd = BigDecimal.ZERO;
				}
			}

			boolean isSummary = leftNameRaw.trim().endsWith("合计") || leftNameRaw.trim().endsWith(":");
			batch.add(new Object[] { getAD_PInstance_ID(), // 1
					seq, // 2
					1, // 3 LevelNo 固定为1
					leftNameRaw, // 4
					rightNameRaw, // 5
					isSummary ? "Y" : "N", // 6
					leftBegin, // 7
					leftEnd, // 8
					rightBegin, // 9
					rightEnd, // 10
					leftAccountValue, // 11
					leftAccountName, // 12
					leftAccountId, // 13
					rightAccountValue, // 14
					rightAccountName, // 15
					rightAccountId, // 16
					getAD_Client_ID(), // 17
					p_AD_Org_ID, // 18
					getAD_User_ID(), // 19
					getAD_User_ID() // 20
			});
		}

		if (batch.isEmpty()) {
			log.warning("无数据可插入");
			return;
		}

		// 批量插入（每批500条）
		int batchSize = 500;
		int total = 0;
		for (int i = 0; i < batch.size(); i += batchSize) {
			int end = Math.min(i + batchSize, batch.size());
			List<Object[]> subBatch = batch.subList(i, end);
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO T_BalanceSheet (")
					.append("AD_PInstance_ID, SeqNo, LevelNo, ItemName_Left, ItemName_Right, IsSummary, ")
					.append("Amt_Begin_Left, Amt_End_Left, Amt_Begin_Right, Amt_End_Right, ")
					.append("Account_Value_Left, Account_Name_Left, Left_Account_ID, ")
					.append("Account_Value_Right, Account_Name_Right, Right_Account_ID, ")
					.append("AD_Client_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy, IsActive")
					.append(") VALUES ");

			List<Object> params = new ArrayList<>();
			for (int j = 0; j < subBatch.size(); j++) {
				if (j > 0)
					sql.append(",");
				sql.append("(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?,'Y')");
				Object[] row = subBatch.get(j);
				for (Object o : row)
					params.add(o);
			}

			try {
				int inserted = DB.executeUpdateEx(sql.toString(), params.toArray(), get_TrxName());
				total += inserted;
			} catch (Exception e) {
				log.log(Level.SEVERE, "批量插入失败，尝试单条插入", e);
				for (Object[] row : subBatch) {
					try {
						insertSingleRow(row);
						total++;
					} catch (Exception ex) {
						log.log(Level.SEVERE, "单条插入失败", ex);
					}
				}
			}
		}
		log.info("成功插入 " + total + " 条记录");
	}

	/**
	 * 单条插入（降级方案）
	 * 
	 * @param row 数据行
	 */
	private void insertSingleRow(Object[] row) {
		String sql = "INSERT INTO T_BalanceSheet ("
				+ "AD_PInstance_ID, SeqNo, LevelNo, ItemName_Left, ItemName_Right, IsSummary, "
				+ "Amt_Begin_Left, Amt_End_Left, Amt_Begin_Right, Amt_End_Right, "
				+ "Account_Value_Left, Account_Name_Left, Left_Account_ID, "
				+ "Account_Value_Right, Account_Name_Right, Right_Account_ID, "
				+ "AD_Client_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy, IsActive"
				+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?,'Y')";
		DB.executeUpdateEx(sql, row, get_TrxName());
	}

	// ==================== 内部数据类 ====================
	/**
	 * 科目余额（期初/期末的借方、贷方累计）
	 */
	private static class Balance {
		BigDecimal beginDr, beginCr;
		BigDecimal endDr, endCr;

		Balance(BigDecimal beginDr, BigDecimal beginCr, BigDecimal endDr, BigDecimal endCr) {
			this.beginDr = beginDr;
			this.beginCr = beginCr;
			this.endDr = endDr;
			this.endCr = endCr;
		}
	}

	/**
	 * 科目信息
	 */
	private static class AccountInfo {
		int id; // C_ElementValue_ID
		String value; // 科目编码
		String name; // 科目名称

		AccountInfo(int id, String value, String name) {
			this.id = id;
			this.value = value;
			this.name = name;
		}
	}

	/**
	 * 报表项计算结果（已按类型调整符号）
	 */
	private static class RowResult {
		BigDecimal begin, end; // 期初、期末金额（已调整符号）
		AccountInfo accountInfo; // 关联的科目信息（仅单一科目时非空）

		RowResult(BigDecimal b, BigDecimal e, AccountInfo acc) {
			begin = b;
			end = e;
			accountInfo = acc;
		}
	}
}