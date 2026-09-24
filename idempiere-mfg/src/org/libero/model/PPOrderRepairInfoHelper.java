package org.libero.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 补数信息分摊工具类
 * 供 Callout（PP_Order.C_OrderLine_ID）和 PPOrderService（ECN 创建工单）共用
 *
 * 逻辑：根据 C_OrderLine_ID 查询随销单补数(SO)申请单，计算剩余可分配补数数量，
 * 按工单数量分摊补数数量到当前工单
 */
public class PPOrderRepairInfoHelper {

    /**
     * 补数分摊结果
     */
    public static class RepairAllocation {
        /** 原工单ID（欠数工单） */
        public int shortageOrderId = 0;
        /** 分摊到的补数数量 */
        public BigDecimal repairQty = BigDecimal.ZERO;
        /** 补数方式 */
        public String repairMethod = null;
        /** 原工单欠数数量 */
        public BigDecimal qtyShortage = BigDecimal.ZERO;
        /** 是否成功分配到补数（false 表示已分配完或无匹配申请单） */
        public boolean allocated = false;
    }

    /**
     * 根据销售订单行查询补数申请单，计算当前工单可分摊的补数数量
     *
     * @param ctx           上下文
     * @param C_OrderLine_ID 销售订单行ID
     * @param currentOrderQty 当前工单数量
     * @param currentPP_Order_ID 当前工单ID（排除自身，0表示新建）
     * @param trxName       事务
     * @return 分摊结果
     */
    public static RepairAllocation allocateRepairQty(Properties ctx, int C_OrderLine_ID,
            BigDecimal currentOrderQty, int currentPP_Order_ID, String trxName) {


        RepairAllocation result = new RepairAllocation();

        if (C_OrderLine_ID <= 0 || currentOrderQty == null || currentOrderQty.signum() <= 0) {
            return result;
        }

        int clientId = Env.getAD_Client_ID(ctx);

        // 1. 查询该订单行下已完成的"随销单补数"申请单（取最新的一个）
        String requestSql = "SELECT PP_Order_Repair_Request_ID, PP_Order_ID, repairqty "
                + "FROM PP_Order_Repair_Request "
				+ "WHERE C_OrderLine_New_ID=? AND repairmethod='SO' AND DocStatus='CO' "
                + "AND AD_Client_ID=? AND IsActive='Y' "
                + "ORDER BY PP_Order_Repair_Request_ID DESC "
                + "LIMIT 1";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(requestSql, trxName);
            pstmt.setInt(1, C_OrderLine_ID);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                return result; // 无匹配申请单
            }

            int requestId = rs.getInt(1);
            int shortageOrderId = rs.getInt(2);
            BigDecimal totalRepairQty = rs.getBigDecimal(3);
            if (totalRepairQty == null) totalRepairQty = BigDecimal.ZERO;
            if (totalRepairQty.signum() <= 0) {
                return result; // 补数数量为0
            }

            // 2. 计算该订单行下已分配的补数数量（排除当前工单）
            String allocatedSql = "SELECT COALESCE(SUM(RepairQty), 0) FROM PP_Order "
                    + "WHERE C_OrderLine_ID=? AND RepairQty IS NOT NULL AND RepairQty > 0 "
                    + "AND PP_Order_ID != ? AND AD_Client_ID=? AND IsActive='Y' "
                    + "AND DocStatus NOT IN ('VO', 'CL')";
            BigDecimal alreadyAllocated = DB.getSQLValueBD(trxName, allocatedSql,
                    C_OrderLine_ID, currentPP_Order_ID, clientId);
            if (alreadyAllocated == null) alreadyAllocated = BigDecimal.ZERO;

            // 3. 剩余可分配 = 总补数数量 - 已分配
            BigDecimal remaining = totalRepairQty.subtract(alreadyAllocated);
            if (remaining.signum() <= 0) {
                return result; // 已分配完
            }

            // 4. 分摊：取工单数量和剩余补数数量的较小值
            BigDecimal allocatedQty = currentOrderQty.min(remaining);

            result.shortageOrderId = shortageOrderId;
            result.repairQty = allocatedQty;
            result.repairMethod = "SO";
            result.allocated = true;

            // 5. 查询原工单的欠数数量
//            BigDecimal qtyShortage = DB.getSQLValueBD(trxName,
//                    "SELECT QtyShortage FROM PP_Order WHERE PP_Order_ID=?",
//                    shortageOrderId);
//            result.qtyShortage = qtyShortage != null ? qtyShortage : BigDecimal.ZERO;

        } catch (SQLException e) {
            // 查询失败，不阻断主流程
            return result;
        } finally {
            DB.close(rs, pstmt);
        }

        return result;
    }
}
