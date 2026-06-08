package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Language;

/**
 * 存货数量金额帐报表 - 使用 INSERT SELECT 从 M_Transaction 获取数据
 * 
 * @ClassName: InventoryValueReport
 * @author ldh
 * @date 2026年4月8日
 */
@org.adempiere.base.annotation.Process
public class InventoryValueReport extends SvrProcess {

	// 参数定义
	private int p_AD_Org_ID = 0;
	private int p_C_AcctSchema_ID = 0;
	private int p_C_Period_ID = 0;
	private Timestamp p_DateAcct_From = null;
	private Timestamp p_DateAcct_To = null;
	private int p_M_Product_Category_ID = 0;
	private int p_M_Product_ID = 0;

	private StringBuffer m_parameterWhere = new StringBuffer();
	private long m_start = System.currentTimeMillis();

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para.getParameterAsInt();
			else if (name.equals("C_AcctSchema_ID"))
				p_C_AcctSchema_ID = para.getParameterAsInt();
			else if (name.equals("C_Period_ID"))
				p_C_Period_ID = para.getParameterAsInt();
			else if (name.equals("DateAcct")) {
				p_DateAcct_From = (Timestamp) para.getParameter();
				p_DateAcct_To = (Timestamp) para.getParameter_To();
			} else if (name.equals("M_Product_Category_ID"))
				p_M_Product_Category_ID = para.getParameterAsInt();
			else if (name.equals("M_Product_ID"))
				p_M_Product_ID = para.getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
		}

		if (p_C_AcctSchema_ID == 0)
			throw new IllegalArgumentException("请选择会计账套！");

		buildParameterWhere();
		setDateAcct();
	}

	private void buildParameterWhere() {
		m_parameterWhere.append(" t.AD_Client_ID=").append(getAD_Client_ID());

		if (p_AD_Org_ID != 0)
			m_parameterWhere.append(" AND t.AD_Org_ID=").append(p_AD_Org_ID);

		if (p_M_Product_Category_ID != 0)
			m_parameterWhere.append(" AND p.M_Product_Category_ID=").append(p_M_Product_Category_ID);

		if (p_M_Product_ID != 0)
			m_parameterWhere.append(" AND t.M_Product_ID=").append(p_M_Product_ID);
	}

	private void setDateAcct() {
		if (p_DateAcct_From != null) {
			if (p_DateAcct_To == null)
				p_DateAcct_To = new Timestamp(System.currentTimeMillis());
			return;
		}

		if (p_C_Period_ID != 0) {
			String sql = "SELECT StartDate, EndDate FROM C_Period WHERE C_Period_ID=?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql, get_TrxName());
				pstmt.setInt(1, p_C_Period_ID);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					p_DateAcct_From = rs.getTimestamp(1);
					p_DateAcct_To = rs.getTimestamp(2);
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, sql, e);
			} finally {
				DB.close(rs, pstmt);
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

	@Override
	protected String doIt() throws Exception {
		deleteTemporaryData();
		insertDataFromTransaction();
		updateDisplaySequence();
		deleteZeroValueRecords();

		log.info("Inventory Value Report completed in " + (System.currentTimeMillis() - m_start) + " ms");
		return "";
	}

	/**
	 * 根据流程id删除数据
	 * 
	 * @Title: deleteTemporaryData
	 * @return void
	 */
	private void deleteTemporaryData() {
		String sql = "DELETE FROM T_InventoryValueReport WHERE AD_PInstance_ID=?";
		DB.executeUpdateEx(sql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
	}

	/**
	 * 使用 INSERT SELECT 直接从 M_Transaction 获取所有数据 修复： 1. 期初库存按库位分别取最新余额后，按物料+组织汇总
	 * 2.入库/出库按物料+组织汇总 3. AD_Org_ID 取自事务表，而非产品表
	 * 
	 * @Title: insertDataFromTransaction
	 * @return void
	 */
	private void insertDataFromTransaction() {
		Timestamp priorDate = getPriorMonthEndDate();

		// 使用 CTE 提高可读性
		String sql = "WITH "
				// ========= 期初数据：按物料+库位取最新余额，再按物料+组织汇总 =========
				+ "beginning_base AS ( " + "  SELECT " + "    t.M_Product_ID, " + "    t.AD_Client_ID, "
				+ "    t.AD_Org_ID, " + "    t.M_Locator_ID, " + "    t.currentqtyonhand, " + "    t.currentcostprice, "
				+ "    ROW_NUMBER() OVER ( "
				+ "      PARTITION BY t.M_Product_ID, t.AD_Client_ID, t.AD_Org_ID, t.M_Locator_ID "
				+ "      ORDER BY t.M_Transaction_ID DESC " + "    ) AS rn " + "  FROM M_Transaction t "
				+ "  INNER JOIN M_Product p ON (t.M_Product_ID = p.M_Product_ID) " + "  WHERE t.MovementDate <= ? AND "
				+ m_parameterWhere.toString() + "), " + "beginning AS ( " + "  SELECT " + "    M_Product_ID, "
				+ "    AD_Client_ID, " + "    AD_Org_ID, " + "    SUM(currentqtyonhand) AS qty, "
				+ "    SUM(currentqtyonhand * currentcostprice) AS amt, " + "    CASE WHEN SUM(currentqtyonhand) > 0 "
				+ "      THEN SUM(currentqtyonhand * currentcostprice) / SUM(currentqtyonhand) " + "      ELSE 0 "
				+ "    END AS price " + "  FROM beginning_base " + "  WHERE rn = 1 "
				+ "  GROUP BY M_Product_ID, AD_Client_ID, AD_Org_ID " + "), "

				// ========= 本期入库数据：按物料+组织汇总 =========
				+ "inbound AS ( " + "  SELECT " + "    t.M_Product_ID, " + "    t.AD_Client_ID, " + "    t.AD_Org_ID, "
				+ "    SUM(CASE WHEN t.MovementQty > 0 THEN t.MovementQty ELSE 0 END) AS qty, "
				+ "    CASE WHEN SUM(CASE WHEN t.MovementQty > 0 THEN t.MovementQty ELSE 0 END) > 0 "
				+ "      THEN SUM(CASE WHEN t.MovementQty > 0 THEN t.MovementQty * t.currentcostprice ELSE 0 END) "
				+ "           / SUM(CASE WHEN t.MovementQty > 0 THEN t.MovementQty ELSE 0 END) " + "      ELSE 0 "
				+ "    END AS price, "
				+ "    SUM(CASE WHEN t.MovementQty > 0 THEN t.MovementQty * t.currentcostprice ELSE 0 END) AS amt "
				+ "  FROM M_Transaction t " + "  INNER JOIN M_Product p ON (t.M_Product_ID = p.M_Product_ID) "
				+ "  WHERE t.MovementDate >= ? AND t.MovementDate <= ? AND " + m_parameterWhere.toString()
				+ "  GROUP BY t.M_Product_ID, t.AD_Client_ID, t.AD_Org_ID " + "), "

				// ========= 本期出库数据：按物料+组织汇总 =========
				+ "outbound AS ( " + "  SELECT " + "    t.M_Product_ID, " + "    t.AD_Client_ID, " + "    t.AD_Org_ID, "
				+ "    SUM(CASE WHEN t.MovementQty < 0 THEN ABS(t.MovementQty) ELSE 0 END) AS qty, "
				+ "    CASE WHEN SUM(CASE WHEN t.MovementQty < 0 THEN ABS(t.MovementQty) ELSE 0 END) > 0 "
				+ "      THEN SUM(CASE WHEN t.MovementQty < 0 THEN ABS(t.MovementQty) * t.currentcostprice ELSE 0 END) "
				+ "           / SUM(CASE WHEN t.MovementQty < 0 THEN ABS(t.MovementQty) ELSE 0 END) " + "      ELSE 0 "
				+ "    END AS price, "
				+ "    SUM(CASE WHEN t.MovementQty < 0 THEN ABS(t.MovementQty) * t.currentcostprice ELSE 0 END) AS amt "
				+ "  FROM M_Transaction t " + "  INNER JOIN M_Product p ON (t.M_Product_ID = p.M_Product_ID) "
				+ "  WHERE t.MovementDate >= ? AND t.MovementDate <= ? AND " + m_parameterWhere.toString()
				+ "  GROUP BY t.M_Product_ID, t.AD_Client_ID, t.AD_Org_ID " + "), "

				// ========= 所有涉及的物料+组织组合（基础表） =========
				+ "all_combos AS ( " + "  SELECT M_Product_ID, AD_Client_ID, AD_Org_ID FROM beginning " + "  UNION "
				+ "  SELECT M_Product_ID, AD_Client_ID, AD_Org_ID FROM inbound " + "  UNION "
				+ "  SELECT M_Product_ID, AD_Client_ID, AD_Org_ID FROM outbound " + ") "

				// ========= 主查询：关联产品信息和 UOM =========
				+ "INSERT INTO T_InventoryValueReport ( " + "  AD_PInstance_ID, AD_Client_ID, AD_Org_ID, "
				+ "  M_Product_ID, ProductValue, ProductName, C_UOM_ID, UOMName, "
				+ "  BeginningQty, BeginningPrice, BeginningAmt, " + "  InboundQty, InboundPrice, InboundAmt, "
				+ "  OutboundQty, OutboundPrice, OutboundAmt, " + "  EndingQty, EndingPrice, EndingAmt, "
				+ "  T_InventoryValueReport_UU " + ") " + "SELECT " + "  " + getAD_PInstance_ID()
				+ " AS AD_PInstance_ID, " + "  c.AD_Client_ID, " + "  c.AD_Org_ID, " + "  p.M_Product_ID, "
				+ "  p.Value AS ProductValue, " + "  p.Name AS ProductName, " + "  p.C_UOM_ID, "
				+ "  u.Name AS UOMName, " + "  COALESCE(b.qty, 0) AS BeginningQty, "
				+ "  COALESCE(b.price, 0) AS BeginningPrice, " + "  COALESCE(b.amt, 0) AS BeginningAmt, "
				+ "  COALESCE(i.qty, 0) AS InboundQty, " + "  COALESCE(i.price, 0) AS InboundPrice, "
				+ "  COALESCE(i.amt, 0) AS InboundAmt, " + "  COALESCE(o.qty, 0) AS OutboundQty, "
				+ "  COALESCE(o.price, 0) AS OutboundPrice, " + "  COALESCE(o.amt, 0) AS OutboundAmt, "
				+ "  0 AS EndingQty, " + "  0 AS EndingPrice, " + "  0 AS EndingAmt, " + "  generate_uuid() "
				+ "FROM all_combos c "
				+ "INNER JOIN M_Product p ON (p.M_Product_ID = c.M_Product_ID AND p.AD_Client_ID = c.AD_Client_ID) "
				+ "LEFT JOIN C_UOM u ON (p.C_UOM_ID = u.C_UOM_ID) "
				+ "LEFT JOIN beginning b ON (b.M_Product_ID = c.M_Product_ID AND b.AD_Client_ID = c.AD_Client_ID AND b.AD_Org_ID = c.AD_Org_ID) "
				+ "LEFT JOIN inbound i ON (i.M_Product_ID = c.M_Product_ID AND i.AD_Client_ID = c.AD_Client_ID AND i.AD_Org_ID = c.AD_Org_ID) "
				+ "LEFT JOIN outbound o ON (o.M_Product_ID = c.M_Product_ID AND o.AD_Client_ID = c.AD_Client_ID AND o.AD_Org_ID = c.AD_Org_ID) "
				+ "WHERE p.IsActive='Y' AND p.IsStocked='Y'";

		// 参数：期初日期、入库起止、出库起止（共5个日期参数）
		ArrayList<Object> params = new ArrayList<>();
		params.add(priorDate); // beginning_base 中的日期
		params.add(p_DateAcct_From);
		params.add(p_DateAcct_To);
		params.add(p_DateAcct_From);
		params.add(p_DateAcct_To);

		DB.executeUpdateEx(sql, params.toArray(), get_TrxName());

		// 更新期末数据（不变）
		String updateEndingSql = "UPDATE T_InventoryValueReport SET "
				+ "EndingQty = COALESCE(BeginningQty, 0) + COALESCE(InboundQty, 0) - COALESCE(OutboundQty, 0), "
				+ "EndingAmt = COALESCE(BeginningAmt, 0) + COALESCE(InboundAmt, 0) - COALESCE(OutboundAmt, 0), "
				+ "EndingPrice = CASE WHEN (COALESCE(BeginningQty, 0) + COALESCE(InboundQty, 0) - COALESCE(OutboundQty, 0)) > 0 "
				+ "THEN (COALESCE(BeginningAmt, 0) + COALESCE(InboundAmt, 0) - COALESCE(OutboundAmt, 0)) / "
				+ "(COALESCE(BeginningQty, 0) + COALESCE(InboundQty, 0) - COALESCE(OutboundQty, 0)) " + "ELSE 0 END "
				+ "WHERE AD_PInstance_ID = ?";
		DB.executeUpdateEx(updateEndingSql, new Object[] { getAD_PInstance_ID() }, get_TrxName());
	}

	/**
	 * 更新顺序号
	 * 
	 * @Title: updateDisplaySequence
	 * @return void
	 */
	private void updateDisplaySequence() {
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE T_InventoryValueReport SET SeqNo = ")
				.append("(SELECT ROW_NUMBER() OVER (ORDER BY ProductValue)) ").append("WHERE AD_PInstance_ID = ?");

		DB.executeUpdateEx(sql.toString(), new Object[] { getAD_PInstance_ID() }, get_TrxName());
	}

	/**
	 * 获取期间月最后一天
	 * 
	 * @Title: getPriorMonthEndDate
	 * @return
	 * @return Timestamp
	 */
	private Timestamp getPriorMonthEndDate() {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(p_DateAcct_From);
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * 删除全部数量单价金额数据都为0的记录
	 * 
	 * @Title: deleteZeroValueRecords
	 * @return void
	 */
	private void deleteZeroValueRecords() {
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM T_InventoryValueReport ").append("WHERE AD_PInstance_ID = ? ")
				.append("AND BeginningQty = 0 AND BeginningPrice = 0 AND BeginningAmt = 0 ")
				.append("AND InboundQty = 0 AND InboundPrice = 0 AND InboundAmt = 0 ")
				.append("AND OutboundQty = 0 AND OutboundPrice = 0 AND OutboundAmt = 0 ")
				.append("AND EndingQty = 0 AND EndingPrice = 0 AND EndingAmt = 0");

		int no = DB.executeUpdateEx(sql.toString(), new Object[] { getAD_PInstance_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))
			log.fine("Deleted zero value records #" + no);
	}
}