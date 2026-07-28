package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

/**
 * 样稿台账 Model 类 在 X_yg_proofinventory 基础上增加业务校验逻辑
 */
public class MYGProofInventory extends X_yg_proofinventory {

	private static final long serialVersionUID = 1L;

	public MYGProofInventory(Properties ctx, int yg_proofinventory_ID, String trxName) {
		super(ctx, yg_proofinventory_ID, trxName);
	}

	public MYGProofInventory(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {

		// ── 校验"存放位置"唯一性 ─────────────────────────────────────
		// 仅当存放位置不为空，且（新建记录 或 存放位置字段发生了变更）时才校验
		String location = getlocation(); // 替换为实际 getter 方法名
		if (location != null && !location.trim().isEmpty() && (newRecord || is_ValueChanged("location"))) {

			// 查询是否已有其他有效样稿占用了相同的存放位置
			int cnt = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM yg_proofinventory " + "WHERE location = ? "
							+ "  AND yg_proofinventory_id != ? " // 排除当前记录自身
							+ "  AND ad_client_id = ? " // 同一客户内唯一
							+ "  AND isactive = 'Y'", // 只检查有效记录
					location, getyg_proofinventory_ID(), getAD_Client_ID());

			if (cnt > 0) {
				// 返回 false 阻止保存，错误信息显示在界面上
				log.saveError("SaveError", "位置'" + location + "'已存放样稿");
				return false;
			}
		}

		return true;
	}
}