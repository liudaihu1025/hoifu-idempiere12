package org.libero.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MSequence;
import org.compiere.util.DB;
import org.libero.tables.X_HX_BoxType;


public class MHX_BoxType extends X_HX_BoxType {

	private static final long serialVersionUID = 20260424L;

	public MHX_BoxType(Properties ctx, int HX_BoxType_ID, String trxName) {
		super(ctx, HX_BoxType_ID, trxName);
	}

	public MHX_BoxType(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord) {
			// 通过名称获取指定的序列
			String sql = "SELECT AD_Sequence_ID FROM AD_Sequence WHERE Name = ? AND IsActive = 'Y'";
			int sequenceId = DB.getSQLValue(get_TrxName(), sql, "HX_BoxType_Value");
			if (sequenceId > 0) {
				MSequence seq = new MSequence(getCtx(), sequenceId, get_TrxName());
				if (seq != null && seq.get_ID() > 0) {
					String documentNo = MSequence.getDocumentNoFromSeq(seq, get_TrxName(), this);
					if (documentNo != null) {
						setValue(documentNo);
					}
				}
			}

			// 校验名称重复
			if (getName() != null && isNameDuplicate(getName())) {
				log.saveError("Error", "箱型名称已存在");
				return false;
			}
		} else {
			// 更新记录时检查名称重复
			if (getName() != null && isNameDuplicate(getName())) {
				log.saveError("Error", "箱型名称已存在");
				return false;
			}
		}

		return true;
	}

	private boolean isNameDuplicate(String name) {
		String sql = "SELECT COUNT(*) FROM HX_BoxType WHERE Name = ? AND HX_BoxType_ID != ?";
		int count = DB.getSQLValue(get_TrxName(), sql, name, getHX_BoxType_ID());
		return count > 0;
	}
}