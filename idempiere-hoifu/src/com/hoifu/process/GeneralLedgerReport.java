package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Level;

import org.compiere.model.MPeriod;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * 财务报表 - 总分类账报表
 * 
 * @ClassName: GeneralLedgerReport
 * @author ldh
 * @date 2026年3月21日
 */
@org.adempiere.base.annotation.Process
public class GeneralLedgerReport extends SvrProcess {

	/** 会计准则参数 */
	private int p_C_AcctSchema_ID = 0;

	/** 期间参数 */
	private int p_C_Period_ID = 0;

	/** 组织参数 */
	private int p_AD_Org_ID = 0;

	/** 过账类型参数 */
	private String p_Posting_Type;

	/** 参数WHERE子句 */
	private StringBuffer m_parameterWhere = new StringBuffer();

	/**
	 * 准备参数
	 */
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("C_AcctSchema_ID"))
				p_C_AcctSchema_ID = ((BigDecimal) para[i].getParameter()).intValue();
			else if (name.equals("C_Period_ID"))
				p_C_Period_ID = ((BigDecimal) para[i].getParameter()).intValue();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = ((BigDecimal) para[i].getParameter()).intValue();
			else if (name.equals("PostingType"))
				p_Posting_Type = para[i].getParameter().toString();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}

		buildParameterWhere();
	} // prepare

	/**
	 * 构建参数WHERE子句 - 固定过账类型为实际过账
	 */
	private void buildParameterWhere() {
		if (p_C_AcctSchema_ID == 0)
			throw new IllegalArgumentException("C_AcctSchema_ID required");

		m_parameterWhere = new StringBuffer(" AND fa.C_AcctSchema_ID=").append(p_C_AcctSchema_ID);

		if (p_AD_Org_ID != 0)
			m_parameterWhere.append(" AND fa.AD_Org_ID=").append(p_AD_Org_ID);

		if (p_Posting_Type != null && p_Posting_Type.length() > 0)
			m_parameterWhere.append(" AND fa.PostingType='").append(p_Posting_Type).append("'");
		
		// 固定为实际过账类型
//		m_parameterWhere.append(" AND PostingType='A'");
	} // buildParameterWhere

	@Override
	protected String doIt() {
		// 清理临时表
		DB.executeUpdate("DELETE FROM T_GeneralLedger WHERE AD_PInstance_ID=" + getAD_PInstance_ID(), get_TrxName());

		if (p_C_Period_ID == 0) {
			// 查询所有期间的数据
			processAllPeriods();
		} else {
			// 查询指定期间的数据
			processSpecificPeriod();
		}

		return "";
	} // doIt

	/**
	 * 处理指定期间的数据
	 * 
	 * @Title: processSpecificPeriod
	 * @return void
	 */
	private void processSpecificPeriod() {
		// 获取一级科目主数据
		getMasterDataForPeriod(p_C_Period_ID);
	}

	/**
	 * 处理所有期间的数据
	 * 
	 * @Title: processAllPeriods
	 * @return void
	 */
	private void processAllPeriods() {
		String sql = "SELECT DISTINCT C_Period_ID FROM Fact_Acct " + "WHERE AD_Client_ID=? AND C_AcctSchema_ID=? ";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_Client_ID());
			pstmt.setInt(2, p_C_AcctSchema_ID);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				int periodId = rs.getInt(1);
				getMasterDataForPeriod(periodId);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * 获取指定期间的一级科目主数据
	 * 
	 * @Title: getMasterDataForPeriod
	 * @param periodId
	 * @return void
	 */
	private void getMasterDataForPeriod(int periodId) {
		// 获取期间日期范围
		MPeriod period = MPeriod.get(getCtx(), periodId);
		Timestamp periodStart = period.getStartDate();
		Timestamp periodEnd = period.getEndDate();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT fa.AD_Org_ID, ev.C_ElementValue_ID, ev.Value, ev.Name ");
		sql.append("FROM Fact_Acct fa ");
		sql.append("INNER JOIN C_ElementValue ev ON fa.Account_ID = ev.C_ElementValue_ID ");
		sql.append("WHERE fa.AD_Client_ID=").append(getAD_Client_ID());
		sql.append(" AND fa.C_Period_ID=").append(periodId);
		sql.append(" AND ev.Value ~ '^[0-9]{4}$'"); // 只选择一级科目
		sql.append(m_parameterWhere);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				int orgId = rs.getInt(1);
				int accountId = rs.getInt(2);
				String accountValue = rs.getString(3);
				String accountName = rs.getString(4);

				// 为每个科目生成三种类型的数据
				generateOpeningBalance(orgId, accountId, accountValue, accountName, periodStart, periodId);
				generatePeriodBalance(orgId, accountId, accountValue, accountName, periodStart, periodEnd, periodId);
				generateYearTotalBalance(orgId, accountId, accountValue, accountName, periodStart, periodEnd, periodId);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql.toString(), e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * 生成期初余额数据
	 * 
	 * @Title: generateOpeningBalance
	 * @param orgId
	 * @param accountId
	 * @param accountValue
	 * @param accountName
	 * @param periodStart
	 * @param periodId
	 * @return void
	 */
	private void generateOpeningBalance(int orgId, int accountId, String accountValue, String accountName,
			Timestamp periodStart, int periodId) {
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO T_GeneralLedger ");
		sql.append("(AD_Client_ID, AD_PInstance_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy, IsActive, ");
		sql.append("C_AcctSchema_ID, C_Period_ID, Account_ID, AccountValue, AccountName, ");
		sql.append(
				"FiscalYear, Period, Description, AmtAcctDr, AmtAcctCr, Balance, Direction, LevelNo, T_GeneralLedger_UU) ");

		sql.append("SELECT ").append(getAD_Client_ID()).append(", ").append(getAD_PInstance_ID()).append(", ");
		sql.append(orgId).append(", getDate(), ").append(getAD_User_ID()).append(", ");
		sql.append("getDate(), ").append(getAD_User_ID()).append(", 'Y', ");
		sql.append(p_C_AcctSchema_ID).append(", ").append(periodId).append(", ");
		sql.append(accountId).append(", '").append(accountValue).append("', '").append(accountName).append("', ");
		sql.append("EXTRACT(YEAR FROM ").append(DB.TO_DATE(periodStart)).append("), ");
		sql.append("(SELECT Name FROM C_Period WHERE C_Period_ID=").append(periodId).append("), ");
		sql.append("'期初余额', ");
		sql.append("COALESCE(SUM(fa.AmtAcctDr),0), COALESCE(SUM(fa.AmtAcctCr),0), ");
		sql.append("COALESCE(SUM(fa.AmtAcctDr),0)-COALESCE(SUM(fa.AmtAcctCr),0), ");
		sql.append(
				"CASE WHEN COALESCE(SUM(fa.AmtAcctDr),0)-COALESCE(SUM(fa.AmtAcctCr),0) >= 0 THEN '借' ELSE '贷' END, ");
		sql.append("1, generate_uuid() ");

		sql.append("FROM Fact_Acct fa ");
		sql.append("WHERE fa.AD_Client_ID=").append(getAD_Client_ID());
		sql.append(" AND fa.DateAcct < ").append(DB.TO_DATE(periodStart, true));

		sql.append(m_parameterWhere);
		// 包含子科目
		sql.append(" AND Account_ID IN (");
		sql.append("SELECT C_ElementValue_ID FROM C_ElementValue ");
		sql.append("WHERE Value LIKE '").append(accountValue).append("%'");
		sql.append(")");

		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("#" + no + " Opening Balance records for account " + accountValue);
	}

	/**
	 * 生成本期合计数据
	 * 
	 * @Title: generatePeriodBalance
	 * @param orgId
	 * @param accountId
	 * @param accountValue
	 * @param accountName
	 * @param periodStart
	 * @param periodEnd
	 * @param periodId
	 * @return void
	 */
	private void generatePeriodBalance(int orgId, int accountId, String accountValue, String accountName,
			Timestamp periodStart, Timestamp periodEnd, int periodId) {
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO T_GeneralLedger ");
		sql.append("(AD_Client_ID, AD_PInstance_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy, IsActive, ");
		sql.append("C_AcctSchema_ID, C_Period_ID, Account_ID, AccountValue, AccountName, ");
		sql.append(
				"FiscalYear, Period, Description, AmtAcctDr, AmtAcctCr, Balance, Direction, LevelNo, T_GeneralLedger_UU) ");

		sql.append("SELECT ").append(getAD_Client_ID()).append(", ").append(getAD_PInstance_ID()).append(", ");
		sql.append(orgId).append(", getDate(), ").append(getAD_User_ID()).append(", ");
		sql.append("getDate(), ").append(getAD_User_ID()).append(", 'Y', ");
		sql.append(p_C_AcctSchema_ID).append(", ").append(periodId).append(", ");
		sql.append(accountId).append(", '").append(accountValue).append("', '").append(accountName).append("', ");
		sql.append("EXTRACT(YEAR FROM ").append(DB.TO_DATE(periodStart)).append("), ");
		sql.append("(SELECT Name FROM C_Period WHERE C_Period_ID=").append(periodId).append("), ");
		sql.append("'本期合计', ");
		sql.append("COALESCE(SUM(fa.AmtAcctDr),0), COALESCE(SUM(fa.AmtAcctCr),0), ");
		sql.append("COALESCE(SUM(fa.AmtAcctDr),0)-COALESCE(SUM(fa.AmtAcctCr),0), ");
		sql.append(
				"CASE WHEN COALESCE(SUM(fa.AmtAcctDr),0)-COALESCE(SUM(fa.AmtAcctCr),0) >= 0 THEN '借' ELSE '贷' END, ");
		sql.append("2, generate_uuid() ");

		sql.append("FROM Fact_Acct fa ");
		sql.append("WHERE fa.AD_Client_ID=").append(getAD_Client_ID());
		sql.append(" AND fa.DateAcct >= ").append(DB.TO_DATE(periodStart, true));
		sql.append(" AND fa.DateAcct <= ").append(DB.TO_DATE(periodEnd, true));
//		sql.append(" AND fa.PostingType='A'");
		sql.append(m_parameterWhere);

		// 包含子科目
		sql.append(" AND Account_ID IN (");
		sql.append("SELECT C_ElementValue_ID FROM C_ElementValue ");
		sql.append("WHERE Value LIKE '").append(accountValue).append("%'");
		sql.append(")");

		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("#" + no + " Period Balance records for account " + accountValue);
	}

	/**
	 * 生成本年合计数据
	 * 
	 * @Title: generateYearTotalBalance
	 * @param orgId
	 * @param accountId
	 * @param accountValue
	 * @param accountName
	 * @param periodStart
	 * @param periodEnd
	 * @param periodId
	 * @return void
	 */
	private void generateYearTotalBalance(int orgId, int accountId, String accountValue, String accountName,
			Timestamp periodStart, Timestamp periodEnd, int periodId) {
		// 获取本年开始日期
		Timestamp yearStart = getYearStartDate(periodStart);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO T_GeneralLedger ");
		sql.append("(AD_Client_ID, AD_PInstance_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy, IsActive, ");
		sql.append("C_AcctSchema_ID, C_Period_ID, Account_ID, AccountValue, AccountName, ");
		sql.append(
				"FiscalYear, Period, Description, AmtAcctDr, AmtAcctCr, Balance, Direction, LevelNo, T_GeneralLedger_UU) ");

		sql.append("SELECT ").append(getAD_Client_ID()).append(", ").append(getAD_PInstance_ID()).append(", ");
		sql.append(orgId).append(", getDate(), ").append(getAD_User_ID()).append(", ");
		sql.append("getDate(), ").append(getAD_User_ID()).append(", 'Y', ");
		sql.append(p_C_AcctSchema_ID).append(", ").append(periodId).append(", ");
		sql.append(accountId).append(", '").append(accountValue).append("', '").append(accountName).append("', ");
		sql.append("EXTRACT(YEAR FROM ").append(DB.TO_DATE(yearStart)).append("), ");
		sql.append("(SELECT Name FROM C_Period WHERE C_Period_ID=").append(periodId).append("), ");
		sql.append("'本年合计', ");
		sql.append("COALESCE(SUM(fa.AmtAcctDr),0), COALESCE(SUM(fa.AmtAcctCr),0), ");
		sql.append("COALESCE(SUM(fa.AmtAcctDr),0)-COALESCE(SUM(fa.AmtAcctCr),0), ");
		sql.append(
				"CASE WHEN COALESCE(SUM(fa.AmtAcctDr),0)-COALESCE(SUM(fa.AmtAcctCr),0) >= 0 THEN '借' ELSE '贷' END, ");
		sql.append("3, generate_uuid() ");

		sql.append("FROM Fact_Acct fa ");
		sql.append("WHERE fa.AD_Client_ID=").append(getAD_Client_ID());
		sql.append(" AND fa.DateAcct >= ").append(DB.TO_DATE(yearStart, true));
		sql.append(" AND fa.DateAcct <= ").append(DB.TO_DATE(periodEnd, true));
//		sql.append(" AND fa.PostingType='A'");
		sql.append(m_parameterWhere);

		// 包含子科目
		sql.append(" AND Account_ID IN (");
		sql.append("SELECT C_ElementValue_ID FROM C_ElementValue ");
		sql.append("WHERE Value LIKE '").append(accountValue).append("%'");
		sql.append(")");

		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("#" + no + " Year Total Balance records for account " + accountValue);
	}

	/**
	 * 获取本年开始日期
	 */
	private Timestamp getYearStartDate(Timestamp date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.MONTH, 0); // 设置
		cal.set(Calendar.DAY_OF_MONTH, 1); // 设置为本年第一天
		return new Timestamp(cal.getTimeInMillis());
	}

}