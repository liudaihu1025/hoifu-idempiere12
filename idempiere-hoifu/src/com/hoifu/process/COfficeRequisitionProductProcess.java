package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

import com.hoifu.model.X_C_OfficeRequisitionLine;

/**
 * 物料调整  更新OA行政申领明细的物料
 * @ClassName: COfficeRequisitionProductProcess
 * @author ldh
 * @date 2026年6月6日
 */
@org.adempiere.base.annotation.Process
public class COfficeRequisitionProductProcess extends SvrProcess {

	private int p_M_Product_ID = 0;
	private List<Integer> selectedLineIds = new ArrayList<>();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			if ("M_Product_ID".equals(p.getParameterName()))
				p_M_Product_ID = p.getParameterAsInt();
		}
	}

	@Override
	protected String doIt() throws Exception {
		loadSelectedLines();

		if (p_M_Product_ID == 0)
			throw new AdempiereUserError("请先选择物料");
		if (selectedLineIds.isEmpty())
			throw new AdempiereUserError("请先勾选明细记录");

		MProduct product = MProduct.get(getCtx(), p_M_Product_ID);
		if (product == null || product.get_ID() == 0)
			throw new AdempiereUserError("该物料不存在");

		int updatedCount = 0;
		for (Integer lineId : selectedLineIds) {
			X_C_OfficeRequisitionLine line = new X_C_OfficeRequisitionLine(getCtx(), lineId, get_TrxName());
			if (line.get_ID() == 0) {
				log.warning("领用单明细行不存在: " + lineId);
				continue;
			}
			line.setM_Product_ID(p_M_Product_ID);
			if (line.save())
				updatedCount++;
			else
				log.warning("更新领用单明细行失败: " + lineId);
		}

		return "已更新 " + updatedCount + " 条明细记录";
	}

	private void loadSelectedLines() {
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				selectedLineIds.add(rs.getInt(1));
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取领用单明细失败");
		} finally {
			DB.close(rs, pstmt);
		}
	}
}