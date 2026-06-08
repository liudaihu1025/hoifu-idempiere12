package org.libero.model;

import java.util.Properties;

import org.compiere.util.DB;

@org.adempiere.base.Model(table = "C_WorkTeam")
public class MC_WorkTeam extends X_C_WorkTeam {

	private static final long serialVersionUID = 1L;

	public MC_WorkTeam(Properties ctx, int C_WorkTeam_ID, String trxName) {
		super(ctx, C_WorkTeam_ID, trxName);
	}

	public MC_WorkTeam(Properties ctx, String C_WorkTeam_UU, String trxName) {
		super(ctx, C_WorkTeam_UU, trxName);
	}

	public MC_WorkTeam(Properties ctx, java.sql.ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 负责人必须是班组成员之一
		// 新建时动态验证规则使下拉为空（C_WorkTeam_ID=0 无成员），
		// 用户无法选择，AD_User_ID=0，条件不成立，校验自然跳过
		if (getAD_User_ID() > 0) {
			int count = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM C_WorkTeamMember "
							+ "WHERE C_WorkTeam_ID=? AND AD_User_ID=? AND IsActive='Y'",
					getC_WorkTeam_ID(), getAD_User_ID());
			if (count == 0) {
				log.saveError("Error", "负责人必须是班组成员之一");
				return false;
			}
		}
		return true;
	}
}