package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.print.MPrintFormat;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;

/**
 * 资产折旧明细表报表 将计算结果插入 T_Asset_Depreciation_Detail 临时表
 */
@org.adempiere.base.annotation.Process
public class AssetDepreciationDetail extends SvrProcess {

	/** 会计期间参数 */
	private int p_C_Period_ID = 0;
	/** 所有组织参数 */
	private int p_AD_Org_ID = 0;
	/** 使用组织参数 */
	private int p_C_BPartner_ID = 0;
	/** 账套参数 */
	private int p_C_AcctSchema_ID = 0;

	/** 期间开始日期 */
	private Timestamp p_DateFrom = null;
	/** 期间结束日期 */
	private Timestamp p_DateTo = null;

	/** 开始时间 */
	private long m_start = System.currentTimeMillis();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("C_Period_ID")) {
				p_C_Period_ID = para[i].getParameterAsInt();
			} else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_AcctSchema_ID"))
				p_C_AcctSchema_ID = para[i].getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}

		// 从环境变量获取默认值
		if (p_AD_Org_ID == 0)
			p_AD_Org_ID = Env.getContextAsInt(getCtx(), Env.AD_ORG_ID);
		if (p_C_AcctSchema_ID == 0)
			p_C_AcctSchema_ID = Env.getContextAsInt(getCtx(), Env.C_ACCTSCHEMA_ID);
	}

	@Override
	protected String doIt() throws Exception {
		// 根据期间ID获取日期范围
		setPeriodDates();

		// 清空临时表
		clearTemporaryTable();

		// 插入折旧明细数据
		insertDepreciationDetail(p_DateFrom, p_DateTo);

		// 更新计算字段
		updateCalculatedFields();

		// 加载打印格式
		int AD_PrintFormat_ID = DB.getSQLValueEx(get_TrxName(),
				"SELECT AD_PrintFormat_ID FROM AD_PrintFormat WHERE name = 'Asset Depreciation Detail Report' AND AD_Client_ID=?",
				getAD_Client_ID());
		if (AD_PrintFormat_ID > 0) {
			if (Ini.isClient())
				getProcessInfo().setTransientObject(MPrintFormat.get(getCtx(), AD_PrintFormat_ID, false));
			else
				getProcessInfo().setSerializableObject(MPrintFormat.get(getCtx(), AD_PrintFormat_ID, false));
		}

		if (log.isLoggable(Level.FINE))
			log.fine((System.currentTimeMillis() - m_start) + " ms");
		return "";
	}

	/**
	 * 根据期间ID设置日期范围
	 */
	private void setPeriodDates() {
		String sql = "SELECT StartDate, EndDate FROM C_Period WHERE C_Period_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, p_C_Period_ID);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				p_DateFrom = rs.getTimestamp(1);
				p_DateTo = rs.getTimestamp(2);
			} else {
				throw new Exception("期间ID " + p_C_Period_ID + " 不存在");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
			throw new RuntimeException("获取期间日期失败", e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	/**
	 * 清空临时表
	 */
	private void clearTemporaryTable() {
		String sql = "DELETE FROM T_Asset_Depreciation_Detail WHERE AD_PInstance_ID = ?";
		int no = DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Cleared " + no + " rows from temporary table");
	}

	/**
	 * 插入折旧明细数据
	 */
	private void insertDepreciationDetail(Timestamp dateFrom, Timestamp dateTo) {
		StringBuilder sb = new StringBuilder();

		// 插入明细数据
		sb.append("INSERT INTO T_Asset_Depreciation_Detail ")
				.append("(AD_PInstance_ID, AD_Client_ID, AD_Org_ID, C_AcctSchema_ID, ")
				.append("A_Asset_ID, A_Asset_Group_ID, Asset_Org_ID, C_BPartner_ID, ")
				.append("Asset_Value, Asset_Name, Asset_Qty, Asset_ServiceDate, ")
				.append("DateAcct, UseLifeMonths, Depreciated_Periods, ")
				.append("Monthly_Depreciation_Rate, Salvage_Value, ")
				.append("Beginning_Cost, Beginning_Accumulated_Depr, Beginning_Net_Value, Beginning_Recoverable_Amount, ")
				.append("Current_Addition, Current_Disposal, Current_Depreciation, Year_Accumulated_Depr, ")
				.append("Ending_Cost, Ending_Accumulated_Depr, Ending_Net_Value, Ending_Recoverable_Amount) ")
				.append("SELECT ").append(getAD_PInstance_ID()).append(", ").append(getAD_Client_ID()).append(", ")
				.append(p_AD_Org_ID).append(", ").append(p_C_AcctSchema_ID).append(", ")

				// 资产基本信息
				.append("a.A_Asset_ID, a.A_Asset_Group_ID, a.AD_Org_ID, a.C_BPartner_ID, ")
				.append("a.InventoryNo, a.Name, a.Qty, a.AssetServiceDate, ")

				// 折旧相关信息
				.append("wk.DateAcct, wk.UseLifeMonths, wk.A_Current_Period - 1, ")
				.append("CASE WHEN wk.A_Asset_Cost > 0 THEN ROUND(((wk.A_Asset_Cost - wk.A_Salvage_Value) / wk.A_Asset_Cost / 120) * 100, 2) ELSE 0 END, ")
				.append("aa.A_Salvage_Value, ")

				// 期初原值 - 使用期间开始日期作为分界点
				.append("(SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition aa1 ")
				.append("WHERE aa1.a_asset_id = a.A_Asset_ID AND aa1.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND aa1.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND aa1.postingtype = 'A' AND aa1.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep1 ")
				.append("WHERE dep1.a_asset_id = aa1.a_asset_id AND dep1.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")) - ")

				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ad1 ")
				.append("WHERE ad1.a_asset_id = a.A_Asset_ID AND ad1.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND ad1.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND ad1.postingtype = 'A' AND ad1.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep2 ")
				.append("WHERE dep2.a_asset_id = ad1.a_asset_id AND dep2.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")), ")

				// 期初累计折旧
				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp exp1 ")
				.append("WHERE exp1.a_asset_id = a.A_Asset_ID AND exp1.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND exp1.ad_org_id = ").append(p_AD_Org_ID).append(" AND exp1.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(" AND exp1.postingtype = 'A' AND exp1.processed = 'Y'), ")

				// 期初净值
				.append("((SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition aa2 ")
				.append("WHERE aa2.a_asset_id = a.A_Asset_ID AND aa2.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND aa2.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND aa2.postingtype = 'A' AND aa2.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep3 ")
				.append("WHERE dep3.a_asset_id = aa2.a_asset_id AND dep3.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")) - ")

				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ad2 ")
				.append("WHERE ad2.a_asset_id = a.A_Asset_ID AND ad2.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND ad2.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND ad2.postingtype = 'A' AND ad2.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep4 ")
				.append("WHERE dep4.a_asset_id = ad2.a_asset_id AND dep4.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")) - ")

				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp exp2 ")
				.append("WHERE exp2.a_asset_id = a.A_Asset_ID AND exp2.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND exp2.ad_org_id = ").append(p_AD_Org_ID).append(" AND exp2.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(" AND exp2.postingtype = 'A' AND exp2.processed = 'Y')), ")

				// 期初可回收净额
				.append("((SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition aa3 ")
				.append("WHERE aa3.a_asset_id = a.A_Asset_ID AND aa3.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND aa3.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND aa3.postingtype = 'A' AND aa3.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep5 ")
				.append("WHERE dep5.a_asset_id = aa3.a_asset_id AND dep5.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")) - ")

				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ad3 ")
				.append("WHERE ad3.a_asset_id = a.A_Asset_ID AND ad3.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND ad3.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND ad3.postingtype = 'A' AND ad3.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep6 ")
				.append("WHERE dep6.a_asset_id = ad3.a_asset_id AND dep6.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")) - ")

				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp exp3 ")
				.append("WHERE exp3.a_asset_id = a.A_Asset_ID AND exp3.dateacct < ").append(DB.TO_DATE(dateFrom))
				.append(" AND exp3.ad_org_id = ").append(p_AD_Org_ID).append(" AND exp3.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(" AND exp3.postingtype = 'A' AND exp3.processed = 'Y')), ")

				// 本期原值增加
				.append("(SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition aa4 ")
				.append("WHERE aa4.a_asset_id = a.A_Asset_ID AND aa4.dateacct BETWEEN ").append(DB.TO_DATE(dateFrom))
				.append(" AND ").append(DB.TO_DATE(dateTo)).append(" AND aa4.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND aa4.postingtype = 'A' AND aa4.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep7 ")
				.append("WHERE dep7.a_asset_id = aa4.a_asset_id AND dep7.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")), ")

				// 本期原值减少
				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ad4 ")
				.append("WHERE ad4.a_asset_id = a.A_Asset_ID AND ad4.dateacct BETWEEN ").append(DB.TO_DATE(dateFrom))
				.append(" AND ").append(DB.TO_DATE(dateTo)).append(" AND ad4.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND ad4.postingtype = 'A' AND ad4.processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep8 ")
				.append("WHERE dep8.a_asset_id = ad4.a_asset_id AND dep8.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(")), ")

				// 本期折旧额
				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp exp4 ")
				.append("WHERE exp4.a_asset_id = a.A_Asset_ID AND exp4.dateacct BETWEEN ").append(DB.TO_DATE(dateFrom))
				.append(" AND ").append(DB.TO_DATE(dateTo)).append(" AND exp4.ad_org_id = ").append(p_AD_Org_ID)
				.append(" AND exp4.c_acctschema_id = ").append(p_C_AcctSchema_ID)
				.append(" AND exp4.postingtype = 'A' AND exp4.processed = 'Y'), ")

				// 本年累计折旧额
				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp exp5 ")
				.append("WHERE exp5.a_asset_id = a.A_Asset_ID AND exp5.dateacct BETWEEN ")
				.append(DB.TO_DATE(getYearStart(dateFrom))).append(" AND ").append(DB.TO_DATE(dateTo))
				.append(" AND exp5.ad_org_id = ").append(p_AD_Org_ID).append(" AND exp5.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(" AND exp5.postingtype = 'A' AND exp5.processed = 'Y'), ")

				// 期末数据（将在updateCalculatedFields中计算）
				.append("0, 0, 0, 0 ")

				// FROM 和 WHERE 子句
				.append("FROM A_Asset a ")
				.append("INNER JOIN A_Depreciation_Workfile wk ON a.A_Asset_ID = wk.A_Asset_ID ")
				.append("INNER JOIN A_Asset_Acct aa ON a.A_Asset_ID = aa.A_Asset_ID AND wk.C_AcctSchema_ID = aa.C_AcctSchema_ID ")
				.append("WHERE a.AD_Org_ID = ").append(p_AD_Org_ID).append(" ").append("AND wk.C_AcctSchema_ID = ")
				.append(p_C_AcctSchema_ID).append(" ");

		// 添加使用组织过滤条件
		if (p_C_BPartner_ID != 0) {
			sb.append("AND a.C_BPartner_ID = ").append(p_C_BPartner_ID).append(" ");
		}

		int no = DB.executeUpdate(sb.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Asset Depreciation Detail rows: #" + no);
	}

	/**
	 * 更新计算字段 分三次UPDATE避免字段引用问题
	 */
	private void updateCalculatedFields() {
		// 第一次更新：计算期末原值和期末累计折旧
		String sql1 = "UPDATE T_Asset_Depreciation_Detail SET "
				+ "Ending_Cost = Beginning_Cost + Current_Addition - Current_Disposal, "
				+ "Ending_Accumulated_Depr = Beginning_Accumulated_Depr + Current_Depreciation "
				+ "WHERE AD_PInstance_ID = ?";

		int no1 = DB.executeUpdateEx(sql1, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Updated ending cost and accumulated depreciation: #" + no1);

		// 第二次更新：计算净值
		String sql2 = "UPDATE T_Asset_Depreciation_Detail SET "
				+ "Ending_Net_Value = Ending_Cost - Ending_Accumulated_Depr " + "WHERE AD_PInstance_ID = ?";

		int no2 = DB.executeUpdateEx(sql2, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Updated ending net value: #" + no2);

		// 第三次更新：计算可回收金额
		String sql3 = "UPDATE T_Asset_Depreciation_Detail SET " + "Ending_Recoverable_Amount = Ending_Net_Value "
				+ "WHERE AD_PInstance_ID = ?";

		int no3 = DB.executeUpdateEx(sql3, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Updated ending recoverable amount: #" + no3);
	}

	/**
	 * 获取年度开始日期
	 */
	private Timestamp getYearStart(Timestamp date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.MONTH, 0);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp(cal.getTimeInMillis());
	}
}