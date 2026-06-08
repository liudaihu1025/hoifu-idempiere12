package org.libero.model;

import java.math.BigDecimal;
import java.util.Properties;

import org.compiere.util.DB;

@org.adempiere.base.Model(table = "C_WorkTeamMember")
public class MC_WorkTeamMember extends X_C_WorkTeamMember {

	private static final long serialVersionUID = 1L;

	public MC_WorkTeamMember(Properties ctx, int C_WorkTeamMember_ID, String trxName) {
		super(ctx, C_WorkTeamMember_ID, trxName);
	}

	public MC_WorkTeamMember(Properties ctx, String C_WorkTeamMember_UU, String trxName) {
		super(ctx, C_WorkTeamMember_UU, trxName);
	}

	public MC_WorkTeamMember(Properties ctx, java.sql.ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 获取所属班组的核算方式（AccountingType 在 C_WorkTeam 表上）
		// P=计件，T=计时
		String accountingType = DB.getSQLValueString(get_TrxName(),
				"SELECT AccountingType FROM C_WorkTeam WHERE C_WorkTeam_ID=?", getC_WorkTeam_ID());
		boolean isPieceWork = "P".equals(accountingType);

		BigDecimal ratio = getpieceratio();

		if (isPieceWork) {
			// 1. 计件比例范围校验：0.00 ~ 1.00（仅计件模式）
			if (ratio != null) {
				if (ratio.compareTo(BigDecimal.ZERO) <= 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
					log.saveError("Error", "计件比例必须大于0并且小于等于1");
					return false;
				}
			}

			// 2. 班组计件比例合计不能超过 1.00（仅计件模式）
			BigDecimal total = DB.getSQLValueBD(get_TrxName(),
					"SELECT COALESCE(SUM(PieceRatio), 0) FROM C_WorkTeamMember "
							+ "WHERE C_WorkTeam_ID=? AND IsActive='Y' AND C_WorkTeamMember_ID<>?",
					getC_WorkTeam_ID(), getC_WorkTeamMember_ID());
			BigDecimal newTotal = (total != null ? total : BigDecimal.ZERO)
					.add(ratio != null ? ratio : BigDecimal.ZERO);
			if (newTotal.compareTo(BigDecimal.ONE) > 0) {
				log.saveError("Error", "所有成员的计件比例合计总数不能超过1，当前已有合计：" + total.toPlainString());
				return false;
			}
		}
		
		// 同一班组内用户不能重复
		if (newRecord || is_ValueChanged("AD_User_ID")) {
			int cnt = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM C_WorkTeamMember "
							+ "WHERE C_WorkTeam_ID=? AND AD_User_ID=? AND IsActive='Y' " + "AND C_WorkTeamMember_ID!=?",
					getC_WorkTeam_ID(), getAD_User_ID(), getC_WorkTeamMember_ID());
			if (cnt > 0) {
				log.saveError("Error", "该用户已是班组成员，不能重复添加");
				return false;
			}
		}
		
		// 一个班组只允许一个机长
		if ("CL".equals(getPosition()) && (newRecord || is_ValueChanged("Position"))) {
			int cnt = DB.getSQLValue(get_TrxName(), "SELECT COUNT(*) FROM C_WorkTeamMember "
					+ "WHERE C_WorkTeam_ID=? AND Position='CL' AND IsActive='Y' " + "AND C_WorkTeamMember_ID!=?",
					getC_WorkTeam_ID(), getC_WorkTeamMember_ID());
			if (cnt > 0) {
				log.saveError("Error", "班组内已存在机长，每个班组只允许一个机长");
				return false;
			}
		}

		// 3. 同一用户只能有一个缺省班组
		if (isDefault()) {
			int count = DB.getSQLValue(
					get_TrxName(), "SELECT COUNT(*) FROM C_WorkTeamMember "
							+ "WHERE AD_User_ID=? AND IsDefault='Y' AND IsActive='Y' " + "AND C_WorkTeamMember_ID<>?",
					getAD_User_ID(), getC_WorkTeamMember_ID());
			if (count > 0) {
				log.saveError("Error", "该用户已有缺省班组，一个用户只能有一个缺省班组");
				return false;
			}
		}

		return true;
	}
}