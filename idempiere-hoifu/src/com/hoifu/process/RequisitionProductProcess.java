package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MProduct;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

/**
 * 申购信息明细更新物料ID和单位字段
 */
@org.adempiere.base.annotation.Process
public class RequisitionProductProcess extends SvrProcess {

	private int p_M_Product_ID = 0;
	private List<Integer> selectedLineIds = new ArrayList<>();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (name.equals("M_Product_ID"))
				p_M_Product_ID = para[i].getParameterAsInt();
		}
	}

	@Override
	protected String doIt() throws Exception {
		// 从T_Selection表获取选中的记录
		loadSelectedLines();

		// 验证参数
		if (p_M_Product_ID == 0)
			throw new AdempiereUserError("请先选择物料");
		if (selectedLineIds.isEmpty())
			throw new AdempiereUserError("请勾选明细记录");

		// 验证产品是否存在，同时取出单位
		MProduct product = MProduct.get(getCtx(), p_M_Product_ID);
		if (product == null || product.get_ID() == 0)
			throw new AdempiereUserError("该物料不存在");

		// 从物料上取默认单位
		int uomId = product.getC_UOM_ID();

		int updatedCount = 0;

		for (Integer lineId : selectedLineIds) {
			MRequisitionLine line = new MRequisitionLine(getCtx(), lineId, get_TrxName());
			if (line.get_ID() == 0) {
				log.warning("申购明细行不存在: " + lineId);
				continue;
			}

			// 先设产品，再设单位，避免 beforeSave 用产品默认单位覆盖
			line.setM_Product_ID(p_M_Product_ID);
			if (uomId > 0)
				line.setC_UOM_ID(uomId);

			if (line.save()) {
				updatedCount++;
			} else {
				log.warning("更新申购明细行失败: " + lineId);
			}
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
			while (rs.next()) {
				selectedLineIds.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取申购明细失败");
		} finally {
			DB.close(rs, pstmt);
		}
	}
}