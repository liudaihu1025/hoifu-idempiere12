package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

/**
 * 收货物料检验表 Model 类
 * 继承 X_QC_IQCInspect（自动生成基类），添加业务逻辑
 */
public class MQC_IQCInspect extends X_QC_IQCInspect {

	private static final long serialVersionUID = 1L;

	public MQC_IQCInspect(Properties ctx, int QC_IQCInspect_ID, String trxName) {
		super(ctx, QC_IQCInspect_ID, trxName);
	}

	public MQC_IQCInspect(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

    @Override
    protected boolean beforeSave(boolean newRecord) {
        if (newRecord) {
            int docTypeId = get_ValueAsInt("C_DocType_ID");
            if (docTypeId <= 0) {
                docTypeId = getDocTypeID(getCtx(), getAD_Client_ID(), getAD_Org_ID(), get_TrxName());
            }
            if (docTypeId > 0) {
                setC_DocType_ID(docTypeId);
            }
            if (getDocumentNo() == null || getDocumentNo().isEmpty()) {
                String docNo = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), false, this);
                setDocumentNo(docNo);
            }
        }
        return true;
    }

	/**
	 * 按 C_DocType.Name='物料检验单' 查询单据类型 优先匹配当前组织，其次匹配组织 0（全局），取 IsDefault 最优先的
	 */
	public static int getDocTypeID(Properties ctx, int AD_Client_ID, int AD_Org_ID, String trxName) {
		String sql = "SELECT C_DocType_ID FROM C_DocType " + "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,?) AND Name=? "
				+ "AND IsActive='Y' ORDER BY AD_Org_ID DESC, IsDefault DESC";
		return DB.getSQLValueEx(trxName, sql, AD_Client_ID, AD_Org_ID, "物料检验单");
	}
}
