package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

public class MQC_TemplateProduct extends X_QC_TemplateProduct {
	private static final long serialVersionUID = 1L;

	public MQC_TemplateProduct(Properties ctx, int QC_TemplateProduct_ID, String trxName) {
		super(ctx, QC_TemplateProduct_ID, trxName);
	}

	public MQC_TemplateProduct(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 可校验允收标准范围
		return true;
	}

	public static MQC_TemplateProduct get(Properties ctx, int productId, int templateId, String trxName) {
		String where = "M_Product_ID=? AND QC_Template_ID=?  AND IsActive = 'Y'";
		int id = DB.getSQLValueEx(trxName, "SELECT QC_TemplateProduct_ID FROM QC_TemplateProduct WHERE " + where,
				productId, templateId);
		if (id <= 0)
			return null;
		return new MQC_TemplateProduct(ctx, id, trxName);
	}
}