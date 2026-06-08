package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;

/**
 * 票据池 Model 类
 * 
 * @author iDempiere (generated)
 * @version Release 13
 */
@org.adempiere.base.Model(table = "C_Bill_Pool")
public class MBillPool extends X_C_Bill_Pool {

	/**  
	 *   
	 */
	private static final long serialVersionUID = 20250306L;

	/**
	 * 标准构造函数
	 * 
	 * @param ctx            上下文
	 * @param C_Bill_Pool_ID 票据池ID
	 * @param trxName        事务名称
	 */
	public MBillPool(Properties ctx, int C_Bill_Pool_ID, String trxName) {
		super(ctx, C_Bill_Pool_ID, trxName);
	}

	/**
	 * UUID构造函数
	 * 
	 * @param ctx            上下文
	 * @param C_Bill_Pool_UU 票据池UUID
	 * @param trxName        事务名称
	 */
	public MBillPool(Properties ctx, String C_Bill_Pool_UU, String trxName) {
		super(ctx, C_Bill_Pool_UU, trxName);
	}

	/**
	 * ResultSet构造函数
	 * 
	 * @param ctx     上下文
	 * @param rs      结果集
	 * @param trxName 事务名称
	 */
	public MBillPool(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 根据票据流水号查找票据池记录
	 * 
	 * @param ctx        上下文
	 * @param documentNo 票据流水号
	 * @param trxName    事务名称
	 * @return M_C_Bill_Pool 票据池记录，如果未找到返回null
	 */
	public static MBillPool getByDocumentNo(Properties ctx, String documentNo, String trxName) {
		if (documentNo == null || documentNo.trim().isEmpty()) {
			return null;
		}

		final String whereClause = "DocumentNo = ? AND IsActive = 'Y'";
		int billPoolId = new Query(ctx, Table_Name, whereClause, trxName).setParameters(documentNo).firstId();

		if (billPoolId > 0) {
			// 直接使用构造函数，避免模型工厂
			return new MBillPool(ctx, billPoolId, trxName);
		}

		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("M_C_Bill_Pool[").append(get_ID()).append("-").append(getDocumentNo())
				.append(",Amount=").append(getBillAmt()).append(",Type=").append(getBillType()).append("]");
		return sb.toString();
	}
}