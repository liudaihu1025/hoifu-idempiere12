package com.hoifu.info;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Messagebox;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 欠数工单信息窗口
 * 1. 控制"补数申请"和"无需补数"按钮的启用/禁用状态
 * 2. 穿透到关联的《生产补数申请单》（有申请单可穿透，无申请单不支持）
 */
public class PPOrderShortageInfoWindow extends InfoWindow {

    private static final long serialVersionUID = 1L;

    public PPOrderShortageInfoWindow(int WindowNo, String tableName, String keyColumn,
            String value, boolean multiSelection, String whereClause,
            int AD_InfoWindow_ID, boolean lookup, org.compiere.model.GridField field) {
        super(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
                AD_InfoWindow_ID, lookup, field);
    }

    public PPOrderShortageInfoWindow(int WindowNo, String tableName, String keyColumn,
            String value, boolean multiSelection, String whereClause,
            int AD_InfoWindow_ID, boolean lookup, org.compiere.model.GridField field,
            String predefinedContextVariables) {
        super(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
                AD_InfoWindow_ID, lookup, field, predefinedContextVariables);
    }

	@Override
	protected void preRunProcess(Integer processId) {
		if (processId != null) {
			MProcess process = MProcess.get(Env.getCtx(), processId);
			if (process != null && "org.libero.process.PPOrderShortageApplyProcess".equals(process.getClassname())) {
				List<Serializable> keys = getSelectedRowKeys();
				if (keys != null && keys.size() == 1) {
					Integer orderId = getIntSelectedRowKey(MTable.getTable_ID("PP_Order"));
					int[] values = getOrderProductAndLineId(orderId);
					Env.setContext(Env.getCtx(), p_WindowNo, "M_Product_ID",
							values[0] > 0 ? String.valueOf(values[0]) : "");
					Env.setContext(Env.getCtx(), p_WindowNo, "C_OrderLine_ID",
							values[1] > 0 ? String.valueOf(values[1]) : "");
				} else {
					// 非单选时清空，避免读到旧值
					Env.setContext(Env.getCtx(), p_WindowNo, "M_Product_ID", "");
					Env.setContext(Env.getCtx(), p_WindowNo, "C_OrderLine_ID", "");
				}
			}
		}
		super.preRunProcess(processId);
	}

	private int[] getOrderProductAndLineId(Integer orderId) {
		if (orderId == null || orderId <= 0)
			return new int[] { 0, 0 };

		int[] result = new int[] { 0, 0 };
		String sql = "SELECT M_Product_ID, C_OrderLine_ID FROM PP_Order WHERE PP_Order_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, orderId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				result[0] = rs.getInt(1);
				result[1] = rs.getInt(2);
			}
		} catch (Exception e) {
			// log
		} finally {
			DB.close(rs, pstmt);
		}
		return result;
	}

    // ==================== 按钮启用/禁用控制 ====================

    @Override
    protected void enableButtons() {
        super.enableButtons();

        List<Serializable> keys = getSelectedRowKeys();
        int selectedCount = keys == null ? 0 : keys.size();

        // 控制穿透按钮：有申请单才可穿透
        updateZoomButtonState(selectedCount);

        for (Button btProcess : btProcessList) {
            Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
            if (processId == null)
                continue;

            MProcess process = MProcess.get(Env.getCtx(), processId);
            if (process == null || process.getClassname() == null)
                continue;

            String className = process.getClassname();

            if ("org.libero.process.PPOrderShortageApplyProcess".equals(className)) {
                // 补数申请：仅支持单选，且补数状态=待处理
                boolean enabled = selectedCount == 1 && isAllPending(keys);
                btProcess.setEnabled(enabled);
                btProcess.setTooltiptext(enabled ? "" : "仅支持勾选一条「待处理」状态的欠数工单");
            } else if ("org.libero.process.PPOrderNoRepairProcess".equals(className)) {
                // 无需补数：支持多选，且全部为待处理
                boolean enabled = selectedCount > 0 && isAllPending(keys);
                btProcess.setEnabled(enabled);
                btProcess.setTooltiptext(enabled ? "" : "仅支持「待处理」状态的欠数工单");
            }
        }
    }

    /**
     * 检查所有选中的工单是否都是"待处理"状态
     */
    private boolean isAllPending(List<Serializable> keys) {
        if (keys == null || keys.isEmpty())
            return false;

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM PP_Order WHERE PP_Order_ID IN (");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0)
                sql.append(",");
            sql.append("?");
        }
        sql.append(") AND (RepairStatus IS NULL OR RepairStatus <> 'DP')");

        int nonPendingCount = DB.getSQLValueEx(null, sql.toString(), keys.toArray(new Serializable[0]));
        return nonPendingCount == 0;
    }


    // ==================== 穿透到申请单 ====================

    @Override
    protected boolean hasZoom() {
        return true;
    }

	@Override
	public void zoom() {
		Integer orderId = getIntSelectedRowKey(MTable.getTable_ID("PP_Order"));
		if (orderId == null || orderId <= 0) {
			Dialog.error(getWindowNo(), "请选择记录");
			return;
		}

		// 查找该工单对应的补数申请单，穿透目标是申请单本身
		int requestId = getRepairRequestId(orderId);
		if (requestId <= 0) {
			Messagebox.showDialog("该工单未生成补数申请单，不支持穿透", "提示", Messagebox.OK, Messagebox.INFORMATION, null);
			return;
		}

		int requestTableId = MTable.getTable_ID("PP_Order_Repair_Request");
		if (requestTableId <= 0)
			requestTableId = MTable.getTable_ID("pp_order_repair_request");

		if (requestTableId <= 0) {
			Dialog.error(getWindowNo(), "未找到《生产补数申请单》表定义");
			return;
		}

		// 穿透到《生产补数申请单》记录
		AEnv.zoom(requestTableId, requestId);
	}

	/**
	 * 控制穿透按钮状态：有申请单才可穿透
	 */
	private void updateZoomButtonState(int selectedCount) {
		Button zoomButton = confirmPanel.getButton(ConfirmPanel.A_ZOOM);
		if (zoomButton == null)
			return;

		if (selectedCount != 1) {
			zoomButton.setDisabled(true);
			zoomButton.setTooltiptext("请选中一条工单记录");
			return;
		}

		Integer orderId = getIntSelectedRowKey(MTable.getTable_ID("PP_Order"));
		if (orderId == null || orderId <= 0) {
			zoomButton.setDisabled(true);
			zoomButton.setTooltiptext("请选中一条工单记录");
			return;
		}

		if (getRepairRequestId(orderId) > 0) {
			zoomButton.setDisabled(false);
			zoomButton.setTooltiptext("穿透到生产补数申请单");
		} else {
			zoomButton.setDisabled(true);
			zoomButton.setTooltiptext("该工单未生成补数申请单，不支持穿透");
		}
	}

	/**
	 * 查找工单对应的（最新一条）生产补数申请单ID，不存在返回0
	 */
	private int getRepairRequestId(int orderId) {
		return DB.getSQLValue(null,
				"SELECT PP_Order_Repair_Request_ID FROM PP_Order_Repair_Request "
						+ "WHERE PP_Order_ID=? AND AD_Client_ID=? AND IsActive='Y' "
						+ "ORDER BY PP_Order_Repair_Request_ID DESC LIMIT 1",
				orderId, Env.getAD_Client_ID(Env.getCtx()));
	}
}
