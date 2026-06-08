package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MRefList;
import org.compiere.model.POInfo;
import org.compiere.util.DB;

/**
 * Model for C_Bill_Transaction
 * 
 * @author iDempiere (generated)
 * @version Release 12 - $Id$
 */
public class MBillTransaction extends X_C_Bill_Transaction implements I_C_Bill_Transaction, I_Persistent {
	/**  
	 *  
	 */
	private static final long serialVersionUID = 20250310L;

	/** Standard Constructor */
	public MBillTransaction(Properties ctx, int C_Bill_Transaction_ID, String trxName) {
		super(ctx, C_Bill_Transaction_ID, trxName);
		/**
		 * if (C_Bill_Transaction_ID == 0) { setC_Bill_Transaction_ID(0);
		 * setDocumentNo(null); setTransactionType(null); setBusinessDate(null);
		 * setC_Currency_ID(0); setSettleAmt(Env.ZERO); setChargeAmt(Env.ZERO);
		 * setInterestAmt(Env.ZERO); setC_Bill_Pool_DocumentNo(null); setBillType(null);
		 * setBillAmt(Env.ZERO); setSubPackageAmt(Env.ZERO); setBillPackageNo(null);
		 * setBusinessStatus(null); setAffairType(null); }
		 */
	}

	/** Standard Constructor */
	public MBillTransaction(Properties ctx, int C_Bill_Transaction_ID, String trxName, String... virtualColumns) {
		super(ctx, C_Bill_Transaction_ID, trxName, virtualColumns);
	}

	/** Standard Constructor */
	public MBillTransaction(Properties ctx, String C_Bill_Transaction_UU, String trxName) {
		super(ctx, C_Bill_Transaction_UU, trxName);
	}

	/** Load Constructor */
	public MBillTransaction(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * AccessLevel
	 * 
	 * @return 3 - Client - Org
	 */
	protected int get_AccessLevel() {
		return accessLevel.intValue();
	}

	/** Load Meta Data */
	protected POInfo initPO(Properties ctx) {
		POInfo poi = POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
		return poi;
	}

	/** String representation */
	public String toString() {
		StringBuffer sb = new StringBuffer("MBillTransaction[").append(get_ID()).append("]");
		return sb.toString();
	}

	// 获取业务状态描述
	public String getTransactionTypeName() {
		String transactionType = getTransactionType();
		if (transactionType == null)
			return "空";

		// 通过名称查找 AD_Reference_ID
		String sql = "SELECT AD_Reference_ID FROM AD_Reference WHERE Name = ?";
		int AD_Reference_ID = DB.getSQLValue(null, sql, "C_Bill_Transaction TransactionType");

		if (AD_Reference_ID <= 0) {
			return transactionType; // 找不到引用时返回原值
		}

		return MRefList.getListName(getCtx(), AD_Reference_ID, transactionType);
	}
}