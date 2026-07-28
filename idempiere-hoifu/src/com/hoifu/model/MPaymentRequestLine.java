package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;

public class MPaymentRequestLine extends X_C_PaymentRequestLine {

	private static final long serialVersionUID = 1L;

	public MPaymentRequestLine(Properties ctx, int C_PaymentRequestLine_ID, String trxName) {
		super(ctx, C_PaymentRequestLine_ID, trxName);
	}

	public MPaymentRequestLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	// -------------------------------------------------------------------------
	// 生命周期方法
	// -------------------------------------------------------------------------

	@Override
	protected boolean beforeSave(boolean newRecord) {
		autoSetLine();
		return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		updateHeader();
		return true;
	}

	@Override
	protected boolean afterDelete(boolean success) {
		if (!success)
			return false;
		updateHeader();
		return true;
	}

	// -------------------------------------------------------------------------
	// 私有辅助方法
	// -------------------------------------------------------------------------

	/**
	 * 汇总更新主表 C_PaymentRequest 的金额字段
	 */
	private boolean updateHeader() {  
	    // 主单已完成时不允许更新金额（防止覆盖已审批数据）  
	    String processed = DB.getSQLValueString(get_TrxName(),  
	        "SELECT Processed FROM C_PaymentRequest WHERE C_PaymentRequest_ID=?",  
	        getC_PaymentRequest_ID());  
	    if ("Y".equals(processed))  
	        return true;  
	  
	    String sql = "UPDATE C_PaymentRequest pr "  
	            + "SET TotalAmt      = (SELECT COALESCE(SUM(prl.GrandTotal),    0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y'), "  
	            + "    PayAmt        = (SELECT COALESCE(SUM(prl.PayAmt),        0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y'), "  
	            + "    RebateAmt     = (SELECT COALESCE(SUM(prl.RebateAmt),     0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y'), "  
	            + "    DiscountAmt   = (SELECT COALESCE(SUM(prl.DiscountAmt),   0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y'), "  
	            + "    DifferenceAmt = (SELECT COALESCE(SUM(prl.DifferenceAmt), 0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y'), "  
	            + "    RequestAmt    = (SELECT COALESCE(SUM(prl.RequestAmt),    0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y'), "  
	            + "    ApprovedAmt   = (SELECT COALESCE(SUM(prl.ApprovedAmt),   0) FROM C_PaymentRequestLine prl WHERE prl.C_PaymentRequest_ID = pr.C_PaymentRequest_ID AND prl.IsActive='Y') "  
	            + "WHERE C_PaymentRequest_ID = ?";  
	    int no = DB.executeUpdateEx(sql, new Object[] { getC_PaymentRequest_ID() }, get_TrxName());  
	    if (no != 1)  
	        log.log(Level.SEVERE, "updateHeader #" + no);  
	    return no == 1;  
	}

	/** 若行号未赋值，自动取当前物流单最大行号 +10 */
	private void autoSetLine() {
		// 自动计算行号
		if (getLine() == 0) {
			String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM C_PaymentRequestLine " + "WHERE C_PaymentRequest_ID=?";
			int lineNo = DB.getSQLValue(get_TrxName(), sql, getC_PaymentRequest_ID());
			setLine(lineNo);
		}
	}

}