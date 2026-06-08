package com.hoifu.factory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.base.IDocFactory;
import org.compiere.acct.Doc;
import org.compiere.model.MAcctSchema;
import org.compiere.util.DB;

import com.hoifu.acct.Doc_BillTransaction;
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
		return null;
	}

	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, ResultSet rs, String trxName) {
		if (AD_Table_ID == MBillTransaction.Table_ID) {
			// 通过 ResultSet 加载文档
			return new Doc_BillTransaction(as, rs, trxName);
		}
		return null;
	}
}
