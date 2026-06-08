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
 * 资产折旧汇总表报表 将计算结果插入 T_Asset_Depreciation_Summary 临时表
 */
@org.adempiere.base.annotation.Process
public class AssetDepreciationSummary extends SvrProcess {

	/** 会计期间参数 */
	private int p_C_Period_ID = 0;
	/** 组织参数 */
	private int p_AD_Org_ID = 0;
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

		// 插入折旧汇总数据
		insertDepreciationSummary(p_DateFrom, p_DateTo);

		// 加载打印格式
		int AD_PrintFormat_ID = DB.getSQLValueEx(get_TrxName(),
				"SELECT AD_PrintFormat_ID FROM AD_PrintFormat WHERE name = 'Asset Depreciation Summary Report' AND AD_Client_ID=?",
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
		String sql = "DELETE FROM T_Asset_Depreciation_Summary WHERE AD_PInstance_ID = ?";
		int no = DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Cleared " + no + " rows from temporary table");
	}

	/**
	 * 插入折旧汇总数据
	 */
	private void insertDepreciationSummary(Timestamp dateFrom, Timestamp dateTo) {
		StringBuilder sb = new StringBuilder();

		// 插入汇总数据
		sb.append("INSERT INTO T_Asset_Depreciation_Summary ")
				.append("(AD_PInstance_ID, AD_Client_ID, AD_Org_ID, C_AcctSchema_ID, ")
				.append("Beginning_Cost, Beginning_Accumulated_Depr, Beginning_Net_Value, Beginning_Recoverable_Amount, ")
				.append("Current_Addition, Current_Disposal, Current_Depreciation, Year_Accumulated_Depr, ")
				.append("Ending_Cost, Ending_Accumulated_Depr, Ending_Net_Value, Ending_Recoverable_Amount) ")
				.append("SELECT ").append(getAD_PInstance_ID()).append(", ").append(getAD_Client_ID()).append(", ")
				.append(p_AD_Org_ID).append(", ").append(p_C_AcctSchema_ID).append(", ")

				// 期初原值 - 使用期间开始日期作为分界点
				.append("(SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_addition.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")) - ")

				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_disposed.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")), ")

				// 期初累计折旧
				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND c_acctschema_id = ").append(p_C_AcctSchema_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y'), ")

				// 期初净值
				.append("((SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_addition.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")) - ")

				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_disposed.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")) - ")

				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND c_acctschema_id = ").append(p_C_AcctSchema_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y')), ")

				// 期初可回收净额
				.append("((SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_addition.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")) - ")

				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_disposed.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")) - ")

				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp ").append("WHERE dateacct < ")
				.append(DB.TO_DATE(dateFrom)).append(" ").append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND c_acctschema_id = ").append(p_C_AcctSchema_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y')), ")

				// 本期原值增加
				.append("(SELECT COALESCE(SUM(assetvalueamt), 0) FROM a_asset_addition ")
				.append("WHERE dateacct BETWEEN ").append(DB.TO_DATE(dateFrom)).append(" AND ")
				.append(DB.TO_DATE(dateTo)).append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_addition.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")), ")

				// 本期原值减少
				.append("(SELECT COALESCE(SUM(a_asset_cost), 0) FROM a_asset_disposed ")
				.append("WHERE dateacct BETWEEN ").append(DB.TO_DATE(dateFrom)).append(" AND ")
				.append(DB.TO_DATE(dateTo)).append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y' ")
				.append("AND EXISTS (SELECT 1 FROM a_depreciation_exp dep ")
				.append("WHERE dep.a_asset_id = a_asset_disposed.a_asset_id ").append("AND dep.c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(")), ")

				// 本期折旧额
				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp ").append("WHERE dateacct BETWEEN ")
				.append(DB.TO_DATE(dateFrom)).append(" AND ").append(DB.TO_DATE(dateTo)).append("AND ad_org_id = ")
				.append(p_AD_Org_ID).append(" ").append("AND c_acctschema_id = ").append(p_C_AcctSchema_ID).append(" ")
				.append("AND postingtype = 'A' AND processed = 'Y'), ")

				// 本年累计折旧额
				.append("(SELECT COALESCE(SUM(expense), 0) FROM a_depreciation_exp ").append("WHERE dateacct BETWEEN ")
				.append(DB.TO_DATE(getYearStart(dateFrom))).append(" AND ").append(DB.TO_DATE(dateTo))
				.append("AND ad_org_id = ").append(p_AD_Org_ID).append(" ").append("AND c_acctschema_id = ")
				.append(p_C_AcctSchema_ID).append(" ").append("AND postingtype = 'A' AND processed = 'Y'), ")

				// 期末数据（将在updateCalculatedFields中计算）
				.append("0, 0, 0, 0");

		int no = DB.executeUpdate(sb.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Asset Depreciation Summary rows: #" + no);

		// 更新计算字段
		updateCalculatedFields();
	}

	/**
	 * 更新计算字段 分三次UPDATE避免字段引用问题
	 */
	private void updateCalculatedFields() {
		// 第一次更新：计算期末原值和期末累计折旧
		String sql1 = "UPDATE T_Asset_Depreciation_Summary SET "
				+ "Ending_Cost = Beginning_Cost + Current_Addition - Current_Disposal, "
				+ "Ending_Accumulated_Depr = Beginning_Accumulated_Depr + Current_Depreciation "
				+ "WHERE AD_PInstance_ID = ?";

		int no1 = DB.executeUpdateEx(sql1, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Updated ending cost and accumulated depreciation: #" + no1);

		// 第二次更新：计算净值
		String sql2 = "UPDATE T_Asset_Depreciation_Summary SET "
				+ "Ending_Net_Value = Ending_Cost - Ending_Accumulated_Depr " + "WHERE AD_PInstance_ID = ?";

		int no2 = DB.executeUpdateEx(sql2, new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Updated ending net value: #" + no2);

		// 第三次更新：计算可回收金额
		String sql3 = "UPDATE T_Asset_Depreciation_Summary SET " + "Ending_Recoverable_Amount = Ending_Net_Value "
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