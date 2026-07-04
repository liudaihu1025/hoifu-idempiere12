package com.hoifu.validator;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.adempiere.webui.apps.IProcessParameterListener;
import org.adempiere.webui.apps.ProcessParameterPanel;
import org.adempiere.webui.editor.WEditor;
import org.compiere.model.MStorageOnHand;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

@Component(service = IProcessParameterListener.class, property = {
		"ProcessClass=com.hoifu.process.COfficeRequisitionClaimProcess" })
public class ClaimQtyParameterListener implements IProcessParameterListener {

	@Override
	public void onChange(ProcessParameterPanel parameterPanel, String columnName, WEditor editor) {
		// 对于动态默认值的需求，这个方法可以为空
		// 如果需要处理参数值变更逻辑，可以在这里添加代码
	}

	@Override
	public void onInit(ProcessParameterPanel parameterPanel) {
		// 获取ClaimQty参数的编辑器
		WEditor claimQtyEditor = parameterPanel.getEditor("ClaimQty");
		if (claimQtyEditor == null)
			return;

		List<Integer> selectedIds = parameterPanel.getProcessInfo().getRecord_IDs();
		if (selectedIds == null || selectedIds.isEmpty())
			return;

		BigDecimal defaultValue = calculateDefaultClaimQty(selectedIds.get(0));
		if (defaultValue != null && defaultValue.signum() > 0)
			claimQtyEditor.setValue(defaultValue);
	}

	private BigDecimal calculateDefaultClaimQty(int lineId) {
		String sql = "SELECT QtyDemand, QuantityPicked, M_Product_ID, M_Locator_ID "
				+ "FROM C_OfficeRequisitionLine WHERE C_OfficeRequisitionLine_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, lineId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal demand = rs.getBigDecimal(1);
				BigDecimal picked = rs.getBigDecimal(2);
				int productId = rs.getInt(3);
				int locatorId = rs.getInt(4);

				if (demand == null)
					demand = Env.ZERO;
				if (picked == null)
					picked = Env.ZERO;

				// 建议领用数量 = 需求数量 - 已领数量
				BigDecimal suggested = demand.subtract(picked);
				if (suggested.signum() <= 0)
					return Env.ZERO;

				// 不超过当前库存
				BigDecimal stock = MStorageOnHand.getQtyOnHandForLocator(productId, locatorId, 0, null);
				return suggested.compareTo(stock) > 0 ? stock : suggested;
			}
		} catch (Exception e) {
			// ignore
		} finally {
			DB.close(rs, pstmt);
		}
		return null;
	}
}