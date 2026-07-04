package com.hoifu.info;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.model.MStorageOnHand;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 行政/办公物料领用单信息窗口 基于 C_OfficeRequisitionLine 表
 */
public class COfficeRequisitionInfoWindow extends InfoWindow {

	public COfficeRequisitionInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public COfficeRequisitionInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public COfficeRequisitionInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public COfficeRequisitionInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override
	protected void enableButtons() {
		super.enableButtons();

		int selectedCount = contentPanel.getSelectedCount();

		for (Button btProcess : btProcessList) {
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
			if (processId == null)
				continue;

			MProcess process = MProcess.get(Env.getCtx(), processId);
			String className = process.getClassname();
			if (className == null)
				continue;

			switch (className) {
			case "com.hoifu.process.COfficeRequisitionClaimProcess":
				// 只有选中1条、状态可领用(WL/CG/PR)、且有库存时才启用
				btProcess.setEnabled(selectedCount == 1 && areAllSelectedItemsCanClaimed() && hasAnyStock());
				btProcess.setTooltiptext("仅支持未领用状态的物料领用");
				break;

			case "com.hoifu.process.COfficeRequisitionProductProcess":
				// 只有选中1条且状态为未领用(WL)时才启用
				btProcess.setEnabled(selectedCount == 1 && areAllSelectedItemsUnclaimed());
				btProcess.setTooltiptext("物料调整");
				break;

			case "com.hoifu.process.COfficeRequisitionDetailPRCreate":
				// 选中项都是WL或PR、有库存不足、且没有已有申购单时才启用
				btProcess.setEnabled(selectedCount > 0 && areAllSelectedItemsUnclaimedOrPartiallyClaimed()
						&& hasInsufficientStock() && !hasExistingRequisitions());
				btProcess.setTooltiptext("仅支持未领用或部分领用状态的物料创建申购单");
				break;

			case "com.hoifu.process.COfficeRequisitionEndClaimProcess":
				// 只有选中项都是部分领用(PR)状态时才启用
				btProcess.setEnabled(selectedCount > 0 && areAllSelectedItemsPartiallyClaimed());
				btProcess.setTooltiptext("结束领用 - 将部分领用状态更新为已领取");
				break;

			case "com.hoifu.process.COfficeRequisitionCancelProcess":
				// 只有选中项有草稿状态申购单时才启用
				btProcess.setEnabled(selectedCount > 0 && hasDraftRequisitions());
				btProcess.setTooltiptext("取消申购 - 仅支持采购中状态的物料取消申购");
				break;

			default:
				break;
			}
		}
	}

	/**
	 * 检查所有选中项是否都是可领用状态(WL、CG、PR)
	 */
	private boolean areAllSelectedItemsCanClaimed() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM C_OfficeRequisitionLine ");
		sql.append("WHERE C_OfficeRequisitionLine_ID IN (");
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND ClaimStatus NOT IN (?, ?, ?)");

		Object[] params = new Object[selectedKeys.size() + 3];
		for (int i = 0; i < selectedKeys.size(); i++)
			params[i] = selectedKeys.get(i);
		params[selectedKeys.size()] = "WL";
		params[selectedKeys.size() + 1] = "CG";
		params[selectedKeys.size() + 2] = "PR";

		return DB.getSQLValueEx(null, sql.toString(), params) == 0;
	}

	/**
	 * 检查所有选中项是否都是未领用状态(WL)
	 */
	private boolean areAllSelectedItemsUnclaimed() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM C_OfficeRequisitionLine ");
		sql.append("WHERE C_OfficeRequisitionLine_ID IN (");
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND ClaimStatus != ?");

		Object[] params = new Object[selectedKeys.size() + 1];
		for (int i = 0; i < selectedKeys.size(); i++)
			params[i] = selectedKeys.get(i);
		params[selectedKeys.size()] = "WL";

		return DB.getSQLValueEx(null, sql.toString(), params) == 0;
	}

	/**
	 * 检查所有选中项是否都是未领用(WL)或部分领用(PR)状态
	 */
	private boolean areAllSelectedItemsUnclaimedOrPartiallyClaimed() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM C_OfficeRequisitionLine ");
		sql.append("WHERE C_OfficeRequisitionLine_ID IN (");
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND ClaimStatus NOT IN (?, ?)");

		Object[] params = new Object[selectedKeys.size() + 2];
		for (int i = 0; i < selectedKeys.size(); i++)
			params[i] = selectedKeys.get(i);
		params[selectedKeys.size()] = "WL";
		params[selectedKeys.size() + 1] = "PR";

		return DB.getSQLValueEx(null, sql.toString(), params) == 0;
	}

	/**
	 * 检查所有选中项是否都是部分领用/未领用状态(PR)
	 */
	private boolean areAllSelectedItemsPartiallyClaimed() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM C_OfficeRequisitionLine ");
		sql.append("WHERE C_OfficeRequisitionLine_ID IN (");
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
			params.add(selectedKeys.get(i));
		}
		// 每个值单独一个占位符
		sql.append(") AND ClaimStatus NOT IN (?, ?, ?)");
		params.add("PR");
		params.add("WL");
		params.add("CG");

		int count = DB.getSQLValueEx(null, sql.toString(), params);
		return count == 0;
	}

	/**
	 * 检查选中的物料是否至少有一个有库存（用于领用按钮）
	 */
	private boolean hasAnyStock() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		for (Serializable key : selectedKeys) {
			String sql = "SELECT M_Product_ID, M_Locator_ID FROM C_OfficeRequisitionLine "
					+ "WHERE C_OfficeRequisitionLine_ID = ?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, (Integer) key);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					int productId = rs.getInt(1);
					int locatorId = rs.getInt(2);
					if (productId > 0) {
						BigDecimal stock = MStorageOnHand.getQtyOnHandForLocator(productId, locatorId, 0, null);
						if (stock.signum() > 0)
							return true;
					}
				}
			} catch (Exception e) {
				// ignore
			} finally {
				DB.close(rs, pstmt);
			}
		}
		return false;
	}

	/**
	 * 检查选中物料是否有库存不足（需要申购）
	 */
	private boolean hasInsufficientStock() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		for (Serializable key : selectedKeys) {
			String sql = "SELECT M_Product_ID, M_Locator_ID, QtyDemand " + "FROM C_OfficeRequisitionLine "
					+ "WHERE C_OfficeRequisitionLine_ID = ?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, (Integer) key);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					int productId = rs.getInt(1);
					int locatorId = rs.getInt(2);
					BigDecimal demand = rs.getBigDecimal(3);
					if (demand == null)
						demand = Env.ZERO;
					BigDecimal stock = MStorageOnHand.getQtyOnHandForLocator(productId, locatorId, 0, null);
					if (stock.compareTo(demand) < 0)
						return true;
				}
			} catch (Exception e) {
				// ignore
			} finally {
				DB.close(rs, pstmt);
			}
		}
		return false;
	}

	/**
	 * 检查选中项是否已有有效的申购单（排除已取消/已冲销） 关联字段：M_RequisitionLine.C_OfficeRequisitionLine_ID
	 */
	private boolean hasExistingRequisitions() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_RequisitionLine rl ");
		sql.append("INNER JOIN M_Requisition r ON rl.M_Requisition_ID = r.M_Requisition_ID ");
		sql.append("WHERE rl.C_OfficeRequisitionLine_ID IN (");
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND r.DocStatus NOT IN (?, ?)");

		Object[] params = new Object[selectedKeys.size() + 2];
		for (int i = 0; i < selectedKeys.size(); i++)
			params[i] = selectedKeys.get(i);
		params[selectedKeys.size()] = "VO"; // 已取消
		params[selectedKeys.size() + 1] = "RE"; // 已冲销

		return DB.getSQLValueEx(null, sql.toString(), params) > 0;
	}

	/**
	 * 检查选中项是否有草稿状态的申购单（用于取消申购按钮） 关联字段：M_RequisitionLine.C_OfficeRequisitionLine_ID
	 */
	private boolean hasDraftRequisitions() {
		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty())
			return false;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_RequisitionLine rl ");
		sql.append("INNER JOIN M_Requisition r ON rl.M_Requisition_ID = r.M_Requisition_ID ");
		sql.append("WHERE rl.C_OfficeRequisitionLine_ID IN (");
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND r.DocStatus = ?");

		Object[] params = new Object[selectedKeys.size() + 1];
		for (int i = 0; i < selectedKeys.size(); i++)
			params[i] = selectedKeys.get(i);
		params[selectedKeys.size()] = "DR";

		return DB.getSQLValueEx(null, sql.toString(), params) > 0;
	}
}