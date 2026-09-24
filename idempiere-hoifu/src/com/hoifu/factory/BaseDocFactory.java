package com.hoifu.factory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.base.IDocFactory;
import org.compiere.acct.Doc;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.util.DB;

import com.hoifu.acct.Doc_BillTransaction;
import com.hoifu.acct.Doc_InOut_Accrual;
import com.hoifu.acct.Doc_Invoice_Accrual;
import com.hoifu.model.MBillTransaction;

public class BaseDocFactory implements IDocFactory {
	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, int Record_ID, String trxName) {
		if (AD_Table_ID == MBillTransaction.Table_ID) {
			// 先加载数据到 ResultSet
			String sql = "SELECT * FROM " + MBillTransaction.Table_Name + " WHERE " + MBillTransaction.Table_Name
					+ "_ID = ?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql, trxName);
				pstmt.setInt(1, Record_ID);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					return new Doc_BillTransaction(as, rs, trxName);
				}
			} catch (Exception e) {
				// 处理异常
			} finally {
				DB.close(rs, pstmt);
			}
		}

		// 新增：M_InOut（发货/收货单）——用于追加暂估收入确认分录
		if (AD_Table_ID == MInOut.Table_ID) {
			String sql = "SELECT * FROM " + MInOut.Table_Name + " WHERE " + MInOut.Table_Name + "_ID = ?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql, trxName);
				pstmt.setInt(1, Record_ID);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					return new Doc_InOut_Accrual(as, rs, trxName);
				}
			} catch (Exception e) {
				// 处理异常
			} finally {
				DB.close(rs, pstmt);
			}
		}
		// 新增：C_Invoice（应收/应付单）——用于追加暂估冲减分录
		if (AD_Table_ID == MInvoice.Table_ID) {
			String sql = "SELECT * FROM " + MInvoice.Table_Name + " WHERE " + MInvoice.Table_Name + "_ID = ?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql, trxName);
				pstmt.setInt(1, Record_ID);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					return new Doc_Invoice_Accrual(as, rs, trxName);
				}
			} catch (Exception e) {
				// 处理异常
			} finally {
				DB.close(rs, pstmt);
			}
		}
		return null;
	}

	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, ResultSet rs, String trxName) {
		if (AD_Table_ID == MBillTransaction.Table_ID) {
			// 通过 ResultSet 加载文档
			return new Doc_BillTransaction(as, rs, trxName);
		}

		if (AD_Table_ID == MInOut.Table_ID) {
			return new Doc_InOut_Accrual(as, rs, trxName);
		}

		if (AD_Table_ID == MInvoice.Table_ID) {
			return new Doc_Invoice_Accrual(as, rs, trxName);
		}
		return null;
	}
}
