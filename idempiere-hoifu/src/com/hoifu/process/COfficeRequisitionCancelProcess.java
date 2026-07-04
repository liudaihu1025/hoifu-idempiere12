package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.X_C_OfficeRequisitionLine;

/**
 * 取消申购：删除与 C_OfficeRequisitionLine 关联的申购单行/申购单， 并根据已领数量重新计算领用状态。
 * 
 * 前提：M_RequisitionLine 需有自定义列 C_OfficeRequisitionLine_ID
 */
@org.adempiere.base.annotation.Process
public class COfficeRequisitionCancelProcess extends SvrProcess {

	private List<Integer> selectedLineIds = new ArrayList<>();

	@Override
	protected void prepare() {
		loadSelectedLines();
	}

	@Override
	protected String doIt() throws Exception {
		if (selectedLineIds.isEmpty())
			return "请先勾选明细记录";

		int cancelledCount = 0;

		for (Integer offLineId : selectedLineIds) {
			X_C_OfficeRequisitionLine offLine = new X_C_OfficeRequisitionLine(getCtx(), offLineId, get_TrxName());
			if (offLine.get_ID() == 0)
				continue;

			// 通过自定义列找到关联的申购单行
			int reqLineId = DB.getSQLValueEx(get_TrxName(),
					"SELECT M_RequisitionLine_ID FROM M_RequisitionLine " + "WHERE C_OfficeRequisitionLine_ID = ?",
					offLineId);
			if (reqLineId <= 0)
				continue;

			MRequisitionLine reqLine = new MRequisitionLine(getCtx(), reqLineId, get_TrxName());
			MRequisition requisition = new MRequisition(getCtx(), reqLine.getM_Requisition_ID(), get_TrxName());

			// 只允许取消草稿状态的申购单
			if (!MRequisition.DOCSTATUS_Drafted.equals(requisition.getDocStatus()))
				throw new Exception("物料采购中，不支持取消申购");

			// 根据已领数量重新计算领用状态
			BigDecimal picked = (BigDecimal) offLine.get_Value("QuantityPicked");
			if (picked == null)
				picked = Env.ZERO;
			BigDecimal demand = (BigDecimal) offLine.get_Value("QtyDemand");
			if (demand == null)
				demand = Env.ZERO;

			String newStatus;
			if (picked.compareTo(demand) >= 0)
				newStatus = "YL";
			else if (picked.signum() > 0)
				newStatus = "PR";
			else
				newStatus = "WL";

			offLine.set_CustomColumn("ClaimStatus", newStatus);
			offLine.saveEx();

			// 删除申购单行
			reqLine.deleteEx(true);

			// 若申购单已无明细，则删除申购单
			int remaining = DB.getSQLValueEx(get_TrxName(),
					"SELECT COUNT(*) FROM M_RequisitionLine WHERE M_Requisition_ID = ?",
					requisition.getM_Requisition_ID());
			if (remaining == 0)
				requisition.deleteEx(true);

			cancelledCount++;
		}

		return "成功取消 " + cancelledCount + " 个申购";
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