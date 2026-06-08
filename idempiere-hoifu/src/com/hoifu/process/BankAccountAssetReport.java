package com.hoifu.process;  
  
import java.sql.Timestamp;  
import java.util.logging.Level;  
  
import org.compiere.model.MProcessPara;  
import org.compiere.print.MPrintFormat;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.compiere.util.Ini;
import org.compiere.util.TimeUtil;  
  
/**  
 * Bank Account Asset Report  
 * Inserts summarized data into T_BankAccountAsset for reporting.  
 * AssetType: A=银行存款 (Fact_Acct), B=库存票据 (C_Bill_Pool)  
 */  
@org.adempiere.base.annotation.Process  
public class BankAccountAssetReport extends SvrProcess  
{  
    /** Date range (mandatory) */  
    private Timestamp p_DateFrom = null;  
    private Timestamp p_DateTo   = null;  
  
    /** Optional filters */  
    private String p_AssetType  = null;   // A or B; null = both  
    private int    p_AD_Org_ID  = 0;      // 0 = no filter  
    private int    p_C_Bank_ID  = 0;      // 0 = no filter  
    private int    p_C_Currency_ID  = 0;      // 0 = no filter  
  
    private long m_start = System.currentTimeMillis();  
  
    @Override  
    protected void prepare()  
    {  
        ProcessInfoParameter[] para = getParameter();  
        for (ProcessInfoParameter p : para)  
        {  
            String name = p.getParameterName();  
            if (p.getParameter() == null && p.getParameter_To() == null)  
                ;  
            else if (name.equals("DateAcct"))  
            {  
                p_DateFrom = (Timestamp) p.getParameter();  
                p_DateTo   = (Timestamp) p.getParameter_To();  
            }  
            else if (name.equals("AssetType"))  
                p_AssetType = (String) p.getParameter();  
            else if (name.equals("AD_Org_ID"))  
                p_AD_Org_ID = p.getParameterAsInt();  
            else if (name.equals("C_Bank_ID"))  
            	p_C_Bank_ID = p.getParameterAsInt();  
            else if (name.equals("C_Currency_ID"))  
            	p_C_Currency_ID = p.getParameterAsInt();  
            else  
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);  
        }  
      
        // 开始时间 00:00:00，结束时间 23:59:59.999  
        // 参数是 Date 类型（无时分秒），需要在代码中补全时间  
        if (p_DateFrom != null)  
            p_DateFrom = TimeUtil.getDayBorder(p_DateFrom, null, false);  // 00:00:00  
        if (p_DateTo != null)  
            p_DateTo   = TimeUtil.getDayBorder(p_DateTo,   null, true);   // 23:59:59.999  
    }
  
    @Override  
    protected String doIt()  
    {  
        // Insert based on AssetType filter (null = both)  
        if (p_AssetType == null || "A".equals(p_AssetType))  
            insertBankDeposit();  
        if (p_AssetType == null || "B".equals(p_AssetType))  
            insertBillInStock();  
  
        // Load print format  
        int AD_PrintFormat_ID = DB.getSQLValueEx(get_TrxName(),  
            "SELECT AD_PrintFormat_ID FROM AD_PrintFormat"  
            + " WHERE Name = 'Bank Account Asset Report' AND AD_Client_ID=?",  
            getAD_Client_ID());  
        if (AD_PrintFormat_ID > 0)  
        {  
            if (Ini.isClient())  
                getProcessInfo().setTransientObject(MPrintFormat.get(getCtx(), AD_PrintFormat_ID, false));  
            else  
                getProcessInfo().setSerializableObject(MPrintFormat.get(getCtx(), AD_PrintFormat_ID, false));  
        }  
  
        if (log.isLoggable(Level.FINE))  
            log.fine((System.currentTimeMillis() - m_start) + " ms");  
        return "";  
    }   //  doIt  
  
    /**  
     * Insert bank deposit summary rows (AssetType = 'A').  
     * Source: Fact_Acct -> C_Payment -> C_BankAccount -> C_Bank  
     * BeginBalance : DateAcct < DateFrom  
     * PeriodBalance: DateFrom <= DateAcct < DateTo  
     */  
    private void insertBankDeposit()  
    {  
        int clientId = Env.getAD_Client_ID(getCtx());  
  
        StringBuilder sb = new StringBuilder(  
              "INSERT INTO T_BankAccountAsset "  
            + "(AD_PInstance_ID, AD_Client_ID, AD_Org_ID,"  
            + " AssetType, C_Bank_ID, RoutingNo, BankName,"  
            + " C_BankAccount_ID, AccountName, AccountNo, AccountValue, C_Currency_ID,"  
            + " BeginBalance, PeriodBalance) ");  
  
        sb.append("SELECT ")  
          .append(getAD_PInstance_ID()).append(", ")  
          .append(clientId).append(", ")  
          .append(Env.getAD_Org_ID(getCtx())).append(", ")  
          .append("'A', ")                                   // AssetType = A (银行存款)  
          .append("b.C_Bank_ID, b.RoutingNo, b.Name, ")  
          .append("ba.C_BankAccount_ID, ba.Name, ba.AccountNo, ba.Value, ba.C_Currency_ID, ")  
          // BeginBalance: DateAcct < DateFrom  
          .append("COALESCE(SUM(CASE WHEN TRUNC(fa.DateAcct) < ").append(DB.TO_DATE(p_DateFrom))  
          .append("              THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0), ")  
          // PeriodBalance: DateFrom <= DateAcct < DateTo  
          .append("COALESCE(SUM(CASE WHEN TRUNC(fa.DateAcct) >= ").append(DB.TO_DATE(p_DateFrom))  
          .append("              AND TRUNC(fa.DateAcct) <  ").append(DB.TO_DATE(p_DateTo))  
          .append("              THEN fa.AmtAcctDr - fa.AmtAcctCr ELSE 0 END), 0) ")  
          .append("FROM Fact_Acct fa ")  
          .append("INNER JOIN C_Payment p   ON p.C_Payment_ID = fa.Record_ID AND p.DocStatus IN ('CO','CL') ")  
          .append("INNER JOIN C_BankAccount ba ON ba.C_BankAccount_ID = p.C_BankAccount_ID ")  
          .append("INNER JOIN C_Bank b          ON b.C_Bank_ID = ba.C_Bank_ID ")  
          .append("INNER JOIN C_BankAccount_Acct baa ON p.C_BankAccount_ID = baa.C_BankAccount_ID ")  
          .append("INNER JOIN C_ValidCombination vc  ON (vc.C_ValidCombination_ID = baa.B_InTransit_Acct ")  
          .append("                                   OR vc.C_ValidCombination_ID = baa.B_Asset_Acct) ")  
          .append("WHERE fa.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'C_Payment') ")  
          .append("  AND fa.Account_ID = vc.Account_ID ")  
          .append("  AND fa.AD_Client_ID = ").append(clientId).append(" ")   // 当前租户  
          .append("  AND TRUNC(fa.DateAcct) < ").append(DB.TO_DATE(p_DateTo));  // covers both balances  
  
        // Optional org filter  
        if (p_AD_Org_ID > 0)  
            sb.append("  AND fa.AD_Org_ID = ").append(p_AD_Org_ID);  
        if (p_C_Bank_ID > 0)  
            sb.append("  AND b.C_Bank_ID = ").append(p_C_Bank_ID);  
        if (p_C_Currency_ID > 0)  
            sb.append("  AND ba.C_Currency_ID = ").append(p_C_Currency_ID);  
        
        sb.append(" GROUP BY b.C_Bank_ID, b.RoutingNo, b.Name, ")  
          .append("          ba.C_BankAccount_ID, ba.Name, ba.AccountNo, ba.Value, ba.C_Currency_ID");  
  
        int no = DB.executeUpdate(sb.toString(), get_TrxName());  
        if (log.isLoggable(Level.FINE))   log.fine("Bank Deposit rows: #" + no);  
        if (log.isLoggable(Level.FINEST)) log.finest(sb.toString());  
    }   //  insertBankDeposit  
  
    /**  
     * Insert bill-in-stock summary rows (AssetType = 'B').  
     * Source: C_Bill_Pool -> C_BankAccount -> C_Bank  
     * Filter: DocStatus='AP', IsReceipt='Y', MaturityDate > DateFrom,  
     *         BusinessStatus IN ('H','E','G','S')  
     * BeginBalance : BillDate < DateFrom AND MaturityDate >= DateFrom  
     * PeriodBalance: BillDate >= DateFrom AND BillDate < DateTo  
     */  
    private void insertBillInStock()  
    {  
        int clientId = Env.getAD_Client_ID(getCtx());  
  
        StringBuilder sb = new StringBuilder(  
              "INSERT INTO T_BankAccountAsset "  
            + "(AD_PInstance_ID, AD_Client_ID, AD_Org_ID,"  
            + " AssetType, C_Bank_ID, RoutingNo, BankName,"  
            + " C_BankAccount_ID, AccountName, AccountNo, AccountValue, C_Currency_ID,"  
            + " BeginBalance, PeriodBalance) ");  
  
        sb.append("SELECT ")  
          .append(getAD_PInstance_ID()).append(", ")  
          .append(clientId).append(", ")  
          .append(Env.getAD_Org_ID(getCtx())).append(", ")  
          .append("'B', ")                                   // AssetType = B (库存票据)  
          .append("b.C_Bank_ID, b.RoutingNo, b.Name, ")  
          .append("ba.C_BankAccount_ID, ba.Name, ba.AccountNo, ba.Value, ba.C_Currency_ID, ")  
          // BeginBalance: BillDate < DateFrom AND MaturityDate >= DateFrom  
          .append("COALESCE(SUM(CASE WHEN TRUNC(bp.BillDate) <  ").append(DB.TO_DATE(p_DateFrom))  
          .append("              AND TRUNC(bp.MaturityDate) >= ").append(DB.TO_DATE(p_DateFrom))  
          .append("              THEN bp.MaturityAmt ELSE 0 END), 0), ")  
          // PeriodBalance: BillDate >= DateFrom AND BillDate < DateTo  
          .append("COALESCE(SUM(CASE WHEN TRUNC(bp.BillDate) >= ").append(DB.TO_DATE(p_DateFrom))  
          .append("              AND TRUNC(bp.BillDate) <  ").append(DB.TO_DATE(p_DateTo))  
          .append("              THEN bp.MaturityAmt ELSE 0 END), 0) ")  
          .append("FROM C_Bill_Pool bp ")  
          .append("INNER JOIN C_BankAccount ba ON ba.C_BankAccount_ID = bp.C_BankAccount_ID ")  
          .append("INNER JOIN C_Bank b          ON b.C_Bank_ID = ba.C_Bank_ID ")  
          .append("WHERE bp.DocStatus = 'AP' ")  
          .append("  AND bp.IsReceipt = 'Y' ")  
          .append("  AND TRUNC(bp.MaturityDate) > ").append(DB.TO_DATE(p_DateFrom)).append(" ")  
          .append("  AND bp.BusinessStatus IN ('H','E','G','S') ")  
          .append("  AND bp.AD_Client_ID = ").append(clientId);   // 当前租户  
  
        // Optional org filter  
        if (p_AD_Org_ID > 0)  
            sb.append("  AND bp.AD_Org_ID = ").append(p_AD_Org_ID);  
        if (p_C_Bank_ID > 0)  
            sb.append("  AND b.C_Bank_ID = ").append(p_C_Bank_ID);  
        if (p_C_Currency_ID > 0)  
            sb.append("  AND ba.C_Currency_ID = ").append(p_C_Currency_ID);  
  
        sb.append(" GROUP BY b.C_Bank_ID, b.RoutingNo, b.Name, ")  
          .append("          ba.C_BankAccount_ID, ba.Name, ba.AccountNo, ba.Value, ba.C_Currency_ID");  
  
        int no = DB.executeUpdate(sb.toString(), get_TrxName());  
        if (log.isLoggable(Level.FINE))   log.fine("Bill In Stock rows: #" + no);  
        if (log.isLoggable(Level.FINEST)) log.finest(sb.toString());  
    }   //  insertBillInStock  
  
}   //  BankAccountAssetReport