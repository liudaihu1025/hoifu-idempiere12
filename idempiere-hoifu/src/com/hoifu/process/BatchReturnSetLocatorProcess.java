package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MLocator;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 批量设置退库单明细的库位 用户勾选多条领用明细行后，一键将所有行的库位设置为同一个值
 */
@org.adempiere.base.annotation.Process
public class BatchReturnSetLocatorProcess extends SvrProcess {

	/** 统一设置的库位 ID（通过进程参数传入） */
	private int p_M_Locator_ID = 0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_Locator_ID"))
				p_M_Locator_ID = para[i].getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_M_Locator_ID == 0)
			throw new AdempiereUserError("@FillMandatory@ @M_Locator_ID@");

		// 校验库位
		MLocator locator = new MLocator(getCtx(), p_M_Locator_ID, get_TrxName());
		if (locator.get_ID() == 0 || locator.getAD_Org_ID() != Env.getAD_Org_ID(getCtx())) {
			throw new AdempiereUserError("选择的库位不属于当前组织");
		}

		// 从 T_Selection 读取用户勾选的行 ID（T_Selection_ID 就是 M_InventoryLine_ID）
		List<Integer> inventoryLineIds = new ArrayList<>();
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				inventoryLineIds.add(rs.getInt(1));
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取领用单明细失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}

		if (log.isLoggable(Level.INFO))
			log.info("BatchSetLocator: pInstanceID=" + getAD_PInstance_ID() + ", inventoryLineCount="
					+ inventoryLineIds.size() + ", M_Locator_ID=" + p_M_Locator_ID);

		if (inventoryLineIds.isEmpty())
			return "@NoRecordSelected@";

		// 批量更新 M_InventoryLine 的 M_Locator_ID
		int updated = 0;
		for (int lineId : inventoryLineIds) {
			int no = DB.executeUpdateEx(
					"UPDATE M_InventoryLine SET M_Locator_ID=?, Updated=getDate(), UpdatedBy=? WHERE M_InventoryLine_ID=?",
					new Object[] { p_M_Locator_ID, Env.getAD_User_ID(getCtx()), lineId }, get_TrxName());
			if (log.isLoggable(Level.FINE))
				log.fine("Updated M_InventoryLine_ID=" + lineId + ", rows=" + no);
			updated += no;
		}

		return "@Updated@ #" + updated;
	}
}
