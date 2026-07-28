package com.hoifu.model;

import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
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

		// 新增或修改替代产品/主产品时，校验单位必须一致
		if (newRecord || is_ValueChanged(COLUMNNAME_Substitute_ID) || is_ValueChanged(COLUMNNAME_M_Product_ID)) {

			// 主料不能等于替代料
			if (getM_Product_ID() == getSubstitute_ID()) {
				throw new AdempiereException("主物料与替代料不能相同");
			}

			if (getM_Product_ID() > 0 && getSubstitute_ID() > 0) {
				MProduct mainProduct = MProduct.get(getCtx(), getM_Product_ID());
				MProduct subProduct = MProduct.get(getCtx(), getSubstitute_ID());

				if (mainProduct != null && subProduct != null
						&& mainProduct.getC_UOM_ID() != subProduct.getC_UOM_ID()) {

					MUOM mainUOM = MUOM.get(getCtx(), mainProduct.getC_UOM_ID());
					MUOM subUOM = MUOM.get(getCtx(), subProduct.getC_UOM_ID());

					throw new AdempiereException("替代料[" + subProduct.getName() + "]单位("
							+ (subUOM != null ? subUOM.getUOMSymbol() : subProduct.getC_UOM_ID()) + ")" + " 与主产品["
							+ mainProduct.getName() + "]单位("
							+ (mainUOM != null ? mainUOM.getUOMSymbol() : mainProduct.getC_UOM_ID()) + ")"
							+ " 不一致，不允许保存替代料");
				}
			}
		}
		return true;
	}
}