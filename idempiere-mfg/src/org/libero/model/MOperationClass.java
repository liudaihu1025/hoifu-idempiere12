package org.libero.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;
import org.libero.tables.X_operationclass;


public class MOperationClass extends X_operationclass {

	private static final long serialVersionUID = 20260724L;

	public MOperationClass(Properties ctx, int operationclass_ID, String trxName) {
		super(ctx, operationclass_ID, trxName);
	}

	public MOperationClass(Properties ctx, String operationclass_UU, String trxName) {
		super(ctx, operationclass_UU, trxName);
	}

	public MOperationClass(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 新增或名称被修改时才校验
		if (newRecord || is_ValueChanged(COLUMNNAME_Name)) {
			if (isNameDuplicate(getName())) {
				log.saveError("Error", "工序组名称已存在: " + getName());
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查 name 是否与其他记录重复（同一 Client 下）
	 */
	private boolean isNameDuplicate(String name) {
		if (name == null)
			return false;
		String sql = "SELECT COUNT(*) FROM operationclass "
				+ "WHERE Name = ? AND AD_Client_ID = ? AND operationclass_ID != ?";
		int count = DB.getSQLValue(get_TrxName(), sql, name, getAD_Client_ID(), getoperationclass_ID());
		return count > 0;
	}
}