package com.hoifu.model;

import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.X_M_Substitute;

@org.adempiere.base.Model(table = "M_Substitute")
public class MSubstitute extends X_M_Substitute {

	private static final long serialVersionUID = 1L;

	public MSubstitute(Properties ctx, int substitute_ID, String trxName) {
		super(ctx, substitute_ID, trxName);
	}

	public MSubstitute(Properties ctx, java.sql.ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 主料不能等于替代料
		if (getM_Product_ID() == getSubstitute_ID()) {
			throw new AdempiereException("主物料与替代料不能相同");
		}
		return true;
	}
}