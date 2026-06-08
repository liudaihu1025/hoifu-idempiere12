package com.hoifu.model.qc;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.DB;

public class MQC_Template extends X_QC_Template {
	private static final long serialVersionUID = 1L;

	public MQC_Template(Properties ctx, int QC_Template_ID, String trxName) {
		super(ctx, QC_Template_ID, trxName);
	}

	public MQC_Template(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord && (getTemplateCode() == null || getTemplateCode().isEmpty())) {
			// 可自动生成编码
		}
		return true;
	}

	public List<MQC_TemplateIndex> getIndexLines() {
		return new Query(getCtx(), MQC_TemplateIndex.Table_Name, "QC_Template_ID=? AND IsActive='Y'", get_TrxName())
				.setParameters(getQC_Template_ID()).list();
	}

	public static MQC_Template getForProduct(Properties ctx, int productId, String qcType, String trxName) {
		String sql = "SELECT tp.QC_Template_ID FROM QC_TemplateProduct tp "
				+ "INNER JOIN QC_Template t ON tp.QC_Template_ID = t.QC_Template_ID "
				+ "WHERE tp.M_Product_ID = ? AND t.QCTypes LIKE ? AND t.IsActive = 'Y' AND tp.IsActive = 'Y' "
				+ "ORDER BY tp.Created DESC LIMIT 1";
		int id = DB.getSQLValueEx(trxName, sql, productId, "%" + qcType + "%");
		if (id <= 0)
			return null;
		return new MQC_Template(ctx, id, trxName);
	}
}