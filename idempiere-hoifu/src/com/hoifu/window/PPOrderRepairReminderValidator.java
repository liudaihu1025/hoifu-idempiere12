package com.hoifu.window;

import java.math.BigDecimal;

import org.adempiere.util.Callback;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.validator.WindowValidator;
import org.adempiere.webui.adwindow.validator.WindowValidatorEvent;
import org.adempiere.webui.adwindow.validator.WindowValidatorEventType;
import org.adempiere.webui.component.Messagebox;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 生产工单保存前：随销单补数提醒
 * 当工单关联了随销单补数申请单、但工单数量未超过订单数量时，
 * 弹出只有"确定"按钮的提示框，提醒用户确认是否需要增加补数数量（不阻断保存）
 */
public class PPOrderRepairReminderValidator implements WindowValidator {

    @Override
    public void onWindowEvent(WindowValidatorEvent event, Callback<Boolean> callback) {
        if (!WindowValidatorEventType.BEFORE_SAVE.getName().equals(event.getName())) {
            if (callback != null) callback.onCallback(true);
            return;
        }

        ADWindow adwindow = event.getWindow();
        GridTab gridTab = adwindow.getADWindowContent().getActiveGridTab();

        if (!"PP_Order".equals(gridTab.getTableName())) {
            if (callback != null) callback.onCallback(true);
            return;
        }

        boolean needRemind = checkNeedRemind(gridTab);
        if (!needRemind) {
            if (callback != null) callback.onCallback(true);
            return;
        }

        // 只有确定按钮，点击后关闭继续保存
        Messagebox.showDialog(
                "关联销售订单需要随单补数，请确定当前工单数量是否需要增加补数数量。",
                "提示",
                Messagebox.OK,
                Messagebox.EXCLAMATION,
                result -> {
                    if (callback != null) callback.onCallback(true);
                }
        );
    }

    /**
     * 判断是否需要提醒：
     * 1. 工单有待发布状态（Orderstatus='Ready'）
     * 2. 工单数量 ≤ 关联订单数量（未额外增加补数数量）
     * 3. 该订单行存在符合条件的随销单补数申请单
     */
    private boolean checkNeedRemind(GridTab gridTab) {
        // 获取关联订单行
        int orderLineId = 0;
        Object orderLineObj = gridTab.getValue("C_OrderLine_ID");
        if (orderLineObj instanceof Number) {
            orderLineId = ((Number) orderLineObj).intValue();
        }
        if (orderLineId <= 0) {
            return false;
        }

        // 检查工单状态：待发布(Ready)
        String orderStatus = "";
        Object statusObj = gridTab.getValue("Orderstatus");
        if (statusObj != null) {
            orderStatus = statusObj.toString();
        }
        if (!"Ready".equals(orderStatus)) {
            return false;
        }

        // 获取工单数量和订单行数量
        BigDecimal qtyEntered = BigDecimal.ZERO;
        Object qtyObj = gridTab.getValue("QtyEntered");
        if (qtyObj instanceof BigDecimal) {
            qtyEntered = (BigDecimal) qtyObj;
        }

        BigDecimal orderQty = DB.getSQLValueBD(null,
                "SELECT QtyEntered FROM C_OrderLine WHERE C_OrderLine_ID=?", orderLineId);
        if (orderQty == null) orderQty = BigDecimal.ZERO;

        // 工单数量 > 订单数量，说明已增加补数数量，不需要提醒
        if (qtyEntered.compareTo(orderQty) > 0) {
            return false;
        }

        // 检查是否存在符合条件的随销单补数申请单
        int requestId = DB.getSQLValue(null,
                "SELECT PP_Order_Repair_Request_ID FROM PP_Order_Repair_Request "
                        + "WHERE C_OrderLine_ID=? AND repairmethod='SO' AND DocStatus='CO' "
                        + "AND AD_Client_ID=? AND IsActive='Y'",
                orderLineId, Env.getAD_Client_ID(Env.getCtx()));

        return requestId > 0;
    }
}
