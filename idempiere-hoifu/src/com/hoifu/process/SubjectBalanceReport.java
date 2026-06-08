package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Language;

/**
 * 财务报表 - 科目余额表（按科目+组织维度） 功能：根据会计凭证（Fact_Acct）计算指定期间内各科目的期初余额、本期发生额、期末余额、本年累计，
 * 支持按组织过滤，并自动汇总下级科目到上级科目。
 * 
 * @ClassName: SubjectBalanceReport
 * @author ldh
 * @date 2026年4月17日
 */
@org.adempiere.base.annotation.Process
public class SubjectBalanceReport extends SvrProcess {

	private int p_C_AcctSchema_ID = 0; // 会计账套（必填）
	private int p_C_Period_ID = 0; // 会计期间（可选，与日期二选一）
	private Timestamp p_DateAcct_From = null; // 开始日期
	private Timestamp p_DateAcct_To = null; // 结束日期
	private String p_AccountValue = null; // 科目编码模糊匹配
	private String p_PostingType = "A"; // 过账类型，默认实际
	private boolean p_ShowDetailAccounts = false; // 是否显示明细科目
	private int p_AD_Org_ID = 0; // 组织过滤，0表示所有组织

	private long m_start = System.currentTimeMillis();

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				continue;
			switch (name) {
			case "C_AcctSchema_ID":
				p_C_AcctSchema_ID = para.getParameterAsInt();
				break;
			case "C_Period_ID":
				p_C_Period_ID = para.getParameterAsInt();
				break;
			case "DateAcct":
				p_DateAcct_From = (Timestamp) para.getParameter();
				p_DateAcct_To = (Timestamp) para.getParameter_To();
				break;
			case "AccountValue":
				p_AccountValue = (String) para.getParameter();
				break;
			case "PostingType":
				p_PostingType = (String) para.getParameter();
				break;
			case "ShowDetailAccounts":
				p_ShowDetailAccounts = "Y".equals(para.getParameter());
				break;
			case "AD_Org_ID":
				p_AD_Org_ID = para.getParameterAsInt();
				break;
			default:
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
			}
		}
		if (p_C_AcctSchema_ID == 0)
			throw new IllegalArgumentException("请选择会计账套，会计账套不能为空！");
		setDateAcct();
	}

	/**
	 * 设置报表日期范围（优先级：指定日期 > 期间 > 当前月）
	 * 
	 * @Title: setDateAcct
	 * @return void
	 */
	private void setDateAcct() {
		validateDate();
		if (p_DateAcct_From != null) {
			if (p_DateAcct_To == null)
				p_DateAcct_To = new Timestamp(System.currentTimeMillis());
			return;
		}
		if (p_C_Period_ID != 0) {
			String sql = "SELECT StartDate, EndDate FROM C_Period WHERE C_Period_ID=?";
			try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
				pstmt.setInt(1, p_C_Period_ID);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						p_DateAcct_From = rs.getTimestamp(1);
						p_DateAcct_To = rs.getTimestamp(2);
					}
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "获取期间日期失败", e);
			}
		} else {
			GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
			cal.setTimeInMillis(System.currentTimeMillis());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			p_DateAcct_From = new Timestamp(cal.getTimeInMillis());
			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DAY_OF_YEAR, -1);
			p_DateAcct_To = new Timestamp(cal.getTimeInMillis());
		}
	}

	/**
	 * 校验日期
	 * 
	 * @Title: validateDate
	 * @return void
	 */
	private void validateDate() {
		if ((p_DateAcct_From == null && p_DateAcct_To != null) || (p_DateAcct_From != null && p_DateAcct_To == null))
			throw new IllegalArgumentException("请填写完整的日期范围！");
		if (p_DateAcct_From != null && p_DateAcct_To != null && p_DateAcct_From.after(p_DateAcct_To))
			throw new IllegalArgumentException("开始日期不能晚于结束日期！");
	}

	@Override
	protected String doIt() throws Exception {
		log.info("========== 开始执行报表，AD_PInstance_ID = " + getAD_PInstance_ID() + " ==========");
		deleteTemporaryData();
		insertAccountDataWithBalances(); // 一次扫描完成：插入基础行 + 期初/本期/本年累计
		ensureParentAccounts(); // 补充缺失的父科目（一级及以上）
		initializeBalanceFields(); // 初始化期初/期末默认值（确保无数据行有正确的 NULL/0）
		buildAccountHierarchy(); // 科目层级汇总（等值连接，高性能）
		calculateEndingBalance(); // 计算期末余额（依赖期初和本期）
		deleteZeroBalanceAccounts(); // 删除全零记录
		updateDisplaySequence(); // 更新显示顺序
		log.info("报表完成，耗时 " + (System.currentTimeMillis() - m_start) + " ms");
		return "";
	}

	/**
	 * 向 StringBuilder 追加 Fact_Acct 表的公共过滤条件，并同时将参数加入列表。
	 * 
	 * @param sql    SQL 构建器
	 * @param params 参数列表
	 * @param alias  表别名（如 "fa"）
	 */
	private void appendFactConditions(StringBuilder sql, List<Object> params, String alias) {
		sql.append("AND ").append(alias).append(".AD_Client_ID = ? ");
		params.add(getAD_Client_ID());
		sql.append("AND ").append(alias).append(".C_AcctSchema_ID = ? ");
		params.add(p_C_AcctSchema_ID);
		sql.append("AND ").append(alias).append(".PostingType = ? ");
		params.add(p_PostingType);
		if (p_AD_Org_ID != 0) {
			sql.append("AND ").append(alias).append(".AD_Org_ID = ? ");
			params.add(p_AD_Org_ID);
		}
	}

	// ==================== 临时表数据操作 ====================
	private void deleteTemporaryData() {
		String sql = "DELETE FROM T_SubjectBalance WHERE AD_PInstance_ID=?";
		int cnt = DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		log.fine("删除临时表数据: " + cnt + " 行");
	}

	/**
	 * 一次扫描 Fact_Acct，同时获取科目基础信息、期初余额、本期发生额、本年累计， 并插入临时表。避免多次扫描，大幅提升性能。
	 * 
	 * @Title: insertAccountDataWithBalances
	 * @return void
	 */
	private void insertAccountDataWithBalances() {
		int elementId = getElementIdFromAcctSchema();
		if (elementId == 0)
			throw new IllegalStateException("无法获取会计科目表的元素ID");

		// 计算本年度的起止日期
		Calendar cal = Calendar.getInstance();
		cal.setTime(p_DateAcct_From);
		int year = cal.get(Calendar.YEAR);
		cal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Timestamp yearStart = new Timestamp(cal.getTimeInMillis());
		cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
		Timestamp yearEnd = new Timestamp(cal.getTimeInMillis());

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO T_SubjectBalance (").append("AD_PInstance_ID, AD_Client_ID, AD_Org_ID, ")
				// ★ 新增 AccountSign 字段
				.append("C_ElementValue_ID, AccountValue, AccountName, AccountType, AccountSign, ParentAccountValue, ")
				.append("LevelNo, IsSummary, ").append("BeginningBalanceDr, BeginningBalanceCr, ")
				.append("PeriodBalanceDr, PeriodBalanceCr, ").append("YearToDateDr, YearToDateCr, ")
				.append("EndingBalanceDr, EndingBalanceCr, T_SubjectBalance_UU) ").append("SELECT ")
				.append("? AS AD_PInstance_ID, ").append("fa.AD_Client_ID, ").append("fa.AD_Org_ID, ")
				.append("ev.C_ElementValue_ID, ").append("MIN(ev.Value) AS AccountValue, ")
				.append("MIN(ev.Name) AS AccountName, ").append("MIN(ev.AccountType) AS AccountType, ")
				// ★ 读取 AccountSign
				.append("MIN(ev.AccountSign) AS AccountSign, ")
				.append("MIN(CASE WHEN LENGTH(ev.Value) > 4 THEN SUBSTR(ev.Value, 1, LENGTH(ev.Value)-2) ELSE NULL END) AS ParentAccountValue, ")
				.append("MIN(CASE WHEN LENGTH(ev.Value) <= 4 THEN 1 ELSE (LENGTH(ev.Value)-4)/2+1 END) AS LevelNo, ")
				.append("MIN(ev.IsSummary) AS IsSummary, ")

				/*
				 * ★ 期初余额借方： AccountSign='D' → 净额(Dr-Cr)放借方 AccountSign='N' → 净额>0时放借方
				 * AccountSign='C' → NULL
				 */
				.append("CASE ").append("  WHEN MIN(ev.AccountSign) = 'D' THEN ")
				.append("    COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0) ")
				.append("  WHEN MIN(ev.AccountSign) = 'N' THEN ")
				.append("    CASE WHEN COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0) > 0 ")
				.append("         THEN COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0) ")
				.append("         ELSE NULL END ").append("  ELSE NULL ").append("END AS BeginningBalanceDr, ")

				/*
				 * ★ 期初余额贷方： AccountSign='C' → 净额(Cr-Dr)放贷方 AccountSign='N' → 净额<=0时放贷方（绝对值）
				 * AccountSign='D' → NULL
				 */
				.append("CASE ").append("  WHEN MIN(ev.AccountSign) = 'C' THEN ")
				.append("    COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctCr - fa.AmtAcctDr ELSE 0 END), 0) ")
				.append("  WHEN MIN(ev.AccountSign) = 'N' THEN ")
				.append("    CASE WHEN COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0) <= 0 ")
				.append("         THEN ABS(COALESCE(SUM(CASE WHEN fa.DateAcct < ? THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0)) ")
				.append("         ELSE NULL END ").append("  ELSE NULL ").append("END AS BeginningBalanceCr, ")

				/* 本期发生额（不变） */
				.append("COALESCE(SUM(CASE WHEN fa.DateAcct >= ? AND fa.DateAcct <= ? THEN fa.AmtAcctDr ELSE 0 END), 0) AS PeriodBalanceDr, ")
				.append("COALESCE(SUM(CASE WHEN fa.DateAcct >= ? AND fa.DateAcct <= ? THEN fa.AmtAcctCr ELSE 0 END), 0) AS PeriodBalanceCr, ")
				/* 本年累计（借方、贷方） */
				.append("COALESCE(SUM(CASE WHEN fa.DateAcct >= ? AND fa.DateAcct <= ? THEN fa.AmtAcctDr ELSE 0 END), 0) AS YearToDateDr, ")
				.append("COALESCE(SUM(CASE WHEN fa.DateAcct >= ? AND fa.DateAcct <= ? THEN fa.AmtAcctCr ELSE 0 END), 0) AS YearToDateCr, ")
				/* 期末余额暂为0，后续由 calculateEndingBalance 计算 */
				.append("0 AS EndingBalanceDr, 0 AS EndingBalanceCr, ")
				.append("generate_uuid() AS T_SubjectBalance_UU ").append("FROM Fact_Acct fa ")
				.append("INNER JOIN C_ElementValue ev ON fa.Account_ID = ev.C_ElementValue_ID ")
				.append("WHERE ev.C_Element_ID = ? ").append("  AND ev.AccountType != 'M' ")
				.append("  AND LENGTH(ev.Value) > 3 ").append("  AND ev.IsActive = 'Y' ");

		// 科目编码模糊过滤
		if (p_AccountValue != null && !p_AccountValue.isEmpty()) {
			sql.append("AND ev.Value LIKE '%").append(p_AccountValue).append("%' ");
		}

		// 日期范围：包括期初之前、本期、本年累计的所有凭证（OR 条件）
		sql.append("AND fa.DateAcct <= ? ");

		List<Object> params = new ArrayList<>();
		params.add(getAD_PInstance_ID()); // 1. AD_PInstance_ID
		params.add(p_DateAcct_From); // 2. 期初借方 D: DateAcct < From
		params.add(p_DateAcct_From); // 3. 期初借方 N: CASE WHEN DateAcct < From
		params.add(p_DateAcct_From); // 4. 期初借方 N: THEN DateAcct < From
		params.add(p_DateAcct_From); // 5. 期初贷方 C: DateAcct < From
		params.add(p_DateAcct_From); // 6. 期初贷方 N: CASE WHEN DateAcct < From
		params.add(p_DateAcct_From); // 7. 期初贷方 N: ABS DateAcct < From
		params.add(p_DateAcct_From); // 8. 本期Dr起
		params.add(p_DateAcct_To); // 9. 本期Dr止
		params.add(p_DateAcct_From); // 10. 本期Cr起
		params.add(p_DateAcct_To); // 11. 本期Cr止
		params.add(yearStart); // 12. 本年Dr起
		params.add(yearEnd); // 13. 本年Dr止
		params.add(yearStart); // 14. 本年Cr起
		params.add(yearEnd); // 15. 本年Cr止
		params.add(elementId); // 16. C_Element_ID
		params.add(yearEnd); // 17. DateAcct <= 年末

		appendFactConditions(sql, params, "fa");

		sql.append("GROUP BY fa.AD_Client_ID, fa.AD_Org_ID, ev.C_ElementValue_ID");

		int inserted = DB.executeUpdateEx(sql.toString(), params.toArray(), get_TrxName());
		log.info("一次扫描插入科目+余额数据，行数: " + inserted);
		if (inserted == 0) {
			log.warning("警告：未插入任何科目数据，请检查 Fact_Acct 中是否存在符合条件的凭证！");
		}
	}

	/**
	 * 根据账套查询会计科目元素ID
	 * 
	 * @Title: getElementIdFromAcctSchema
	 * @return
	 * @return int
	 */
	private int getElementIdFromAcctSchema() {
		String sql = "SELECT C_Element_ID FROM C_AcctSchema_Element WHERE ElementType='AC' AND C_AcctSchema_ID=?";
		return DB.getSQLValueEx(get_TrxName(), sql, p_C_AcctSchema_ID);
	}

	/**
	 * 获取父科目编码（每级增加2位，如 100101 -> 1001）
	 * 
	 * @Title: getParentAccountValue
	 * @param accountValue
	 * @return
	 * @return String
	 */
	private String getParentAccountValue(String accountValue) {
		if (accountValue == null || accountValue.length() <= 4)
			return null;
		return accountValue.substring(0, accountValue.length() - 2);
	}

	/**
	 * 补充缺失的父科目（一级及以上），以便层级汇总
	 * 
	 * @Title: ensureParentAccounts
	 * @return void
	 */
	private void ensureParentAccounts() {
		// 1. 获取当前临时表中所有 (组织, 科目) 组合
		String sql = "SELECT DISTINCT AD_Org_ID, AccountValue FROM T_SubjectBalance WHERE AD_PInstance_ID = ?";
		List<Object[]> orgAccountPairs = new ArrayList<>();
		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, getAD_PInstance_ID());
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					orgAccountPairs.add(new Object[] { rs.getInt(1), rs.getString(2) });
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "获取组织科目对失败", e);
			return;
		}

		// 2. 收集需要补充的 (组织, 父科目) 组合
		Set<String> needParentKeys = new HashSet<>(); // 格式 "组织ID|科目编码"
		for (Object[] pair : orgAccountPairs) {
			int orgId = (int) pair[0];
			String accValue = (String) pair[1];
			String parent = getParentAccountValue(accValue);
			while (parent != null && parent.length() >= 4) {
				needParentKeys.add(orgId + "|" + parent);
				parent = getParentAccountValue(parent);
			}
		}

		// 3. 移除已存在的组合
		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, getAD_PInstance_ID());
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String key = rs.getInt(1) + "|" + rs.getString(2);
					needParentKeys.remove(key);
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "获取已有组合失败", e);
			return;
		}

		if (needParentKeys.isEmpty())
			return;

		int elementId = getElementIdFromAcctSchema();
		if (elementId == 0)
			return;

		// 4. 批量查询父科目的基础信息
		Set<String> parentValues = new HashSet<>();
		for (String key : needParentKeys)
			parentValues.add(key.split("\\|")[1]);

		Map<String, Object[]> accountInfo = new HashMap<>();
		StringBuilder infoSql = new StringBuilder(
				"SELECT Value, Name, AccountType, IsSummary FROM C_ElementValue WHERE C_Element_ID = ? AND Value IN (");
		int idx = 0;
		for (String val : parentValues) {
			if (idx++ > 0)
				infoSql.append(",");
			infoSql.append("?");
		}
		infoSql.append(")");
		List<Object> infoParams = new ArrayList<>();
		infoParams.add(elementId);
		infoParams.addAll(parentValues);
		try (PreparedStatement pstmt = DB.prepareStatement(infoSql.toString(), get_TrxName())) {
			for (int i = 0; i < infoParams.size(); i++)
				pstmt.setObject(i + 1, infoParams.get(i));
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					accountInfo.put(rs.getString(1),
							new Object[] { rs.getString(2), rs.getString(3), rs.getString(4) });
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "查询父科目信息失败", e);
			return;
		}

		// 5. 插入缺失的父科目行（逐条插入，数量通常很少）
		String insertSql = "INSERT INTO T_SubjectBalance (AD_PInstance_ID, AD_Client_ID, AD_Org_ID, "
				+ "C_ElementValue_ID, AccountValue, AccountName, AccountType, AccountSign, ParentAccountValue, "
				+ "LevelNo, IsSummary, BeginningBalanceDr, BeginningBalanceCr, "
				+ "PeriodBalanceDr, PeriodBalanceCr, EndingBalanceDr, EndingBalanceCr, "
				+ "YearToDateDr, YearToDateCr, T_SubjectBalance_UU) "
				+ "SELECT ?, ?, ?, ev.C_ElementValue_ID, ev.Value, ev.Name, ev.AccountType, ev.AccountSign, "
				+ "CASE WHEN LENGTH(ev.Value) > 4 THEN SUBSTR(ev.Value, 1, LENGTH(ev.Value)-2) ELSE NULL END, "
				+ "CASE WHEN LENGTH(ev.Value) <= 4 THEN 1 ELSE (LENGTH(ev.Value)-4)/2+1 END, "
				+ "ev.IsSummary, 0, 0, 0, 0, 0, 0, 0, 0, generate_uuid() "
				+ "FROM C_ElementValue ev WHERE ev.C_Element_ID = ? AND ev.Value = ?";

		int insertedCount = 0;
		for (String key : needParentKeys) {
			String[] parts = key.split("\\|");
			int orgId = Integer.parseInt(parts[0]);
			String accVal = parts[1];
			Object[] info = accountInfo.get(accVal);
			if (info == null)
				continue;
			List<Object> params = new ArrayList<>();
			params.add(getAD_PInstance_ID());
			params.add(getAD_Client_ID());
			params.add(orgId);
			params.add(elementId);
			params.add(accVal);
			insertedCount += DB.executeUpdateEx(insertSql, params.toArray(), get_TrxName());
		}
		log.info("补充父科目数量: " + insertedCount + " 条");
	}

	/**
	 * 初始化期初和期末余额字段的默认值（根据科目类型设置正确的借贷方向）
	 * 
	 * @Title: initializeBalanceFields
	 * @return void
	 */
	private void initializeBalanceFields() {
		// AccountSign='D'：借方保留，贷方置NULL
		// AccountSign='C'：贷方保留，借方置NULL
		// AccountSign='N'：两边都保留（由buildAccountHierarchy汇总后再决定方向）
		String sql = "UPDATE T_SubjectBalance SET "
				+ "BeginningBalanceDr = CASE WHEN AccountSign IN ('D','N') THEN BeginningBalanceDr ELSE NULL END, "
				+ "BeginningBalanceCr = CASE WHEN AccountSign IN ('C','N') THEN BeginningBalanceCr ELSE NULL END, "
				+ "EndingBalanceDr    = CASE WHEN AccountSign IN ('D','N') THEN EndingBalanceDr    ELSE NULL END, "
				+ "EndingBalanceCr    = CASE WHEN AccountSign IN ('C','N') THEN EndingBalanceCr    ELSE NULL END "
				+ "WHERE AD_PInstance_ID = ?";
		int cnt = DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		log.fine("初始化期初/期末默认值，影响行数: " + cnt);
	}

	/**
	 * 计算期末余额
	 * 
	 * @Title: calculateEndingBalance
	 * @return void
	 */
	private void calculateEndingBalance() {
		// AccountSign='D'：期末借方 = 期初借方 + 本期借方 - 本期贷方，贷方置NULL
		String debitSql = "UPDATE T_SubjectBalance SET "
				+ "EndingBalanceDr = COALESCE(BeginningBalanceDr, 0) + COALESCE(PeriodBalanceDr, 0) - COALESCE(PeriodBalanceCr, 0), "
				+ "EndingBalanceCr = NULL " + "WHERE AD_PInstance_ID = ? AND AccountSign = 'D'";

		// AccountSign='C'：期末贷方 = 期初贷方 + 本期贷方 - 本期借方，借方置NULL
		String creditSql = "UPDATE T_SubjectBalance SET "
				+ "EndingBalanceCr = COALESCE(BeginningBalanceCr, 0) + COALESCE(PeriodBalanceCr, 0) - COALESCE(PeriodBalanceDr, 0), "
				+ "EndingBalanceDr = NULL " + "WHERE AD_PInstance_ID = ? AND AccountSign = 'C'";

		// AccountSign='N'：计算净额，净额>0放借方，净额<=0放贷方（绝对值）
		// 净额 = (期初借方 - 期初贷方) + 本期借方 - 本期贷方
		String neutralSql = "UPDATE T_SubjectBalance SET " + "EndingBalanceDr = CASE "
				+ "  WHEN (COALESCE(BeginningBalanceDr,0) - COALESCE(BeginningBalanceCr,0)) "
				+ "       + COALESCE(PeriodBalanceDr,0) - COALESCE(PeriodBalanceCr,0) > 0 "
				+ "  THEN (COALESCE(BeginningBalanceDr,0) - COALESCE(BeginningBalanceCr,0)) "
				+ "       + COALESCE(PeriodBalanceDr,0) - COALESCE(PeriodBalanceCr,0) " + "  ELSE NULL END, "
				+ "EndingBalanceCr = CASE "
				+ "  WHEN (COALESCE(BeginningBalanceDr,0) - COALESCE(BeginningBalanceCr,0)) "
				+ "       + COALESCE(PeriodBalanceDr,0) - COALESCE(PeriodBalanceCr,0) <= 0 "
				+ "  THEN ABS((COALESCE(BeginningBalanceDr,0) - COALESCE(BeginningBalanceCr,0)) "
				+ "       + COALESCE(PeriodBalanceDr,0) - COALESCE(PeriodBalanceCr,0)) " + "  ELSE NULL END "
				+ "WHERE AD_PInstance_ID = ? AND AccountSign = 'N'";

		int cnt1 = DB.executeUpdateEx(debitSql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		int cnt2 = DB.executeUpdateEx(creditSql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		int cnt3 = DB.executeUpdateEx(neutralSql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		log.fine("期末余额更新: 借方科目 " + cnt1 + " 行，贷方科目 " + cnt2 + " 行，中性科目 " + cnt3 + " 行");
	}

	/**
	 * 构建科目层级汇总（使用 ParentAccountValue 等值连接） 将下级科目的余额累加到直接上级科目，支持多级汇总。
	 * @Title: buildAccountHierarchy
	 * @return void
	 */
	private void buildAccountHierarchy() {
		String childSql = "SELECT parent.AccountValue, parent.AD_Org_ID, "
				+ "  SUM(COALESCE(child.BeginningBalanceDr, 0)) AS sum_beginning_dr, "
				+ "  SUM(COALESCE(child.BeginningBalanceCr, 0)) AS sum_beginning_cr, "
				+ "  SUM(child.PeriodBalanceDr) AS sum_period_dr, " + "  SUM(child.PeriodBalanceCr) AS sum_period_cr, "
				+ "  SUM(COALESCE(child.EndingBalanceDr, 0)) AS sum_ending_dr, "
				+ "  SUM(COALESCE(child.EndingBalanceCr, 0)) AS sum_ending_cr, "
				+ "  SUM(child.YearToDateDr) AS sum_yeartodate_dr, " + "  SUM(child.YearToDateCr) AS sum_yeartodate_cr "
				+ "FROM T_SubjectBalance parent "
				+ "JOIN T_SubjectBalance child ON child.AccountValue LIKE parent.AccountValue || '%' "
				+ "  AND child.AccountValue != parent.AccountValue " + "  AND child.AD_Org_ID = parent.AD_Org_ID "
				+ "  AND parent.AD_PInstance_ID = ? AND child.AD_PInstance_ID = ? "
				+ "GROUP BY parent.AccountValue, parent.AD_Org_ID";

		// 更新父科目：期初和期末使用净额（根据父科目类型），本期和本年累计直接加总
		String updateSql = "UPDATE T_SubjectBalance a SET "

				// ★ 期初借方
				+ "BeginningBalanceDr = CASE " + "  WHEN a.AccountSign = 'D' THEN "
				+ "    COALESCE(a.BeginningBalanceDr, 0) + (COALESCE(child.sum_beginning_dr,0) - COALESCE(child.sum_beginning_cr,0)) "
				+ "  WHEN a.AccountSign = 'N' THEN "
				+ "    CASE WHEN (COALESCE(a.BeginningBalanceDr,0) - COALESCE(a.BeginningBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_beginning_dr,0) - COALESCE(child.sum_beginning_cr,0)) > 0 "
				+ "         THEN (COALESCE(a.BeginningBalanceDr,0) - COALESCE(a.BeginningBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_beginning_dr,0) - COALESCE(child.sum_beginning_cr,0)) "
				+ "         ELSE NULL END " + "  ELSE NULL END, "

				// ★ 期初贷方
				+ "BeginningBalanceCr = CASE " + "  WHEN a.AccountSign = 'C' THEN "
				+ "    COALESCE(a.BeginningBalanceCr, 0) + (COALESCE(child.sum_beginning_cr,0) - COALESCE(child.sum_beginning_dr,0)) "
				+ "  WHEN a.AccountSign = 'N' THEN "
				+ "    CASE WHEN (COALESCE(a.BeginningBalanceDr,0) - COALESCE(a.BeginningBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_beginning_dr,0) - COALESCE(child.sum_beginning_cr,0)) <= 0 "
				+ "         THEN ABS((COALESCE(a.BeginningBalanceDr,0) - COALESCE(a.BeginningBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_beginning_dr,0) - COALESCE(child.sum_beginning_cr,0))) "
				+ "         ELSE NULL END " + "  ELSE NULL END, "

				// 本期发生额：直接加总（不变）
				+ "PeriodBalanceDr = COALESCE(a.PeriodBalanceDr, 0) + COALESCE(child.sum_period_dr, 0), "
				+ "PeriodBalanceCr = COALESCE(a.PeriodBalanceCr, 0) + COALESCE(child.sum_period_cr, 0), "

				// ★ 期末借方
				+ "EndingBalanceDr = CASE " + "  WHEN a.AccountSign = 'D' THEN "
				+ "    COALESCE(a.EndingBalanceDr, 0) + (COALESCE(child.sum_ending_dr,0) - COALESCE(child.sum_ending_cr,0)) "
				+ "  WHEN a.AccountSign = 'N' THEN "
				+ "    CASE WHEN (COALESCE(a.EndingBalanceDr,0) - COALESCE(a.EndingBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_ending_dr,0) - COALESCE(child.sum_ending_cr,0)) > 0 "
				+ "         THEN (COALESCE(a.EndingBalanceDr,0) - COALESCE(a.EndingBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_ending_dr,0) - COALESCE(child.sum_ending_cr,0)) "
				+ "         ELSE NULL END " + "  ELSE NULL END, "

				// ★ 期末贷方
				+ "EndingBalanceCr = CASE " + "  WHEN a.AccountSign = 'C' THEN "
				+ "    COALESCE(a.EndingBalanceCr, 0) + (COALESCE(child.sum_ending_cr,0) - COALESCE(child.sum_ending_dr,0)) "
				+ "  WHEN a.AccountSign = 'N' THEN "
				+ "    CASE WHEN (COALESCE(a.EndingBalanceDr,0) - COALESCE(a.EndingBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_ending_dr,0) - COALESCE(child.sum_ending_cr,0)) <= 0 "
				+ "         THEN ABS((COALESCE(a.EndingBalanceDr,0) - COALESCE(a.EndingBalanceCr,0)) "
				+ "              + (COALESCE(child.sum_ending_dr,0) - COALESCE(child.sum_ending_cr,0))) "
				+ "         ELSE NULL END " + "  ELSE NULL END, "

				// 本年累计：直接加总（不变）
				+ "YearToDateDr = COALESCE(a.YearToDateDr, 0) + COALESCE(child.sum_yeartodate_dr, 0), "
				+ "YearToDateCr = COALESCE(a.YearToDateCr, 0) + COALESCE(child.sum_yeartodate_cr, 0) " + "FROM ("
				+ childSql + ") child " + "WHERE a.AccountValue = child.AccountValue AND a.AD_Org_ID = child.AD_Org_ID "
				+ "AND a.AD_PInstance_ID = ?";

		Object[] params = new Object[] { getAD_PInstance_ID(), getAD_PInstance_ID(), getAD_PInstance_ID() };
		int cnt = DB.executeUpdateEx(updateSql, params, get_TrxName());
		log.fine("层级汇总更新行数: " + cnt);

		if (!p_ShowDetailAccounts) {
			String delDetailSql = "DELETE FROM T_SubjectBalance WHERE AD_PInstance_ID = ? AND LENGTH(accountvalue) != 4";
			int del = DB.executeUpdateEx(delDetailSql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
			log.info("删除明细科目数: " + del);
		}
	}

	/**
	 * 删除0余额科目数据
	 * 
	 * @Title: deleteZeroBalanceAccounts
	 * @return void
	 */
	private void deleteZeroBalanceAccounts() {
		String sql = "DELETE FROM T_SubjectBalance WHERE AD_PInstance_ID = ? "
				+ "AND COALESCE(BeginningBalanceDr, 0) = 0 AND COALESCE(BeginningBalanceCr, 0) = 0 "
				+ "AND PeriodBalanceDr = 0 AND PeriodBalanceCr = 0 "
				+ "AND COALESCE(EndingBalanceDr, 0) = 0 AND COALESCE(EndingBalanceCr, 0) = 0 "
				+ "AND COALESCE(YearToDateDr, 0) = 0 AND COALESCE(YearToDateCr, 0) = 0";
		int no = DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		log.info("删除零余额记录数: " + no);
	}

	/**
	 * 修改排序序号
	 * 
	 * @Title: updateDisplaySequence
	 * @return void
	 */
	private void updateDisplaySequence() {
		String sql = "WITH numbered AS (" + "  SELECT AD_PInstance_ID, C_ElementValue_ID, AD_Org_ID, "
				+ "    ROW_NUMBER() OVER (ORDER BY AD_Org_ID, AccountValue, LevelNo) AS rn "
				+ "  FROM T_SubjectBalance WHERE AD_PInstance_ID = ?" + ") "
				+ "UPDATE T_SubjectBalance SET SeqNo = numbered.rn " + "FROM numbered "
				+ "WHERE T_SubjectBalance.AD_PInstance_ID = numbered.AD_PInstance_ID "
				+ "  AND T_SubjectBalance.C_ElementValue_ID = numbered.C_ElementValue_ID "
				+ "  AND T_SubjectBalance.AD_Org_ID = numbered.AD_Org_ID";
		int cnt = DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		log.fine("更新显示顺序行数: " + cnt);
	}
}