package com.hoifu.callout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 《生产补数申请单》选择原工单号后自动回填相关信息
 * 参照 InOutNoticeLineCallout 选完 C_OrderLine_ID 回填产品/数量的模式
 *
 * 注意：idempiere-hoifu 不依赖 org.idempiere.mfg，不能直接使用 MPPOrder，
 * 改用 SQL 查询 PP_Order 数据
 */
@Callout(tableName = "PP_Order_Repair_Request", columnName = "PP_Order_ID")
public class PPOrderRepairRequestCallout implements IColumnCallout {

    @Override
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField,
            Object value, Object oldValue) {

        // 信息窗口回填的值可能是 BigDecimal（PostgreSQL numeric → JDBC BigDecimal），不能直接强转 Integer
        Integer ppOrderId = null;
        if (value instanceof Number) {
            ppOrderId = ((Number) value).intValue();
        }
        if (ppOrderId == null || ppOrderId == 0) {
            // 清空工单时，同步清空回填字段
            mTab.setValue("C_OrderLine_ID", null);
            mTab.setValue("M_Product_ID", null);
            mTab.setValue("QtyShortage", null);
            mTab.setValue("QtyPlanned", null);
            mTab.setValue("QtyDeliveredSnap", null);
            mTab.setValue("RepairQty", null);
            return "";
        }

        // 查询 PP_Order 数据（不依赖 MPPOrder 类）
        String sql = "SELECT C_OrderLine_ID, M_Product_ID, QtyEntered, QtyDelivered "
                + "FROM PP_Order WHERE PP_Order_ID=?";
        List<Object> row = DB.getSQLValueObjectsEx(null, sql, ppOrderId);
        if (row == null || row.isEmpty())
            return "";

        // PostgreSQL numeric 类型经 JDBC 映射为 BigDecimal，需通过 Number.intValue() 转换
        Integer orderLineId = row.get(0) instanceof Number ? ((Number) row.get(0)).intValue() : null;
        Integer productId = row.get(1) instanceof Number ? ((Number) row.get(1)).intValue() : null;
        BigDecimal qtyEntered = (BigDecimal) row.get(2);
        BigDecimal qtyDelivered = (BigDecimal) row.get(3);

        if (qtyEntered == null) qtyEntered = Env.ZERO;
        if (qtyDelivered == null) qtyDelivered = Env.ZERO;

        // 回填关联订单行
        mTab.setValue("C_OrderLine_ID", orderLineId != null && orderLineId > 0 ? orderLineId : null);

        // 回填产品
        mTab.setValue("M_Product_ID", productId != null && productId > 0 ? productId : null);

        // 回填计划数量、入库数量快照
        mTab.setValue("QtyPlanned", qtyEntered);
        mTab.setValue("QtyDeliveredSnap", qtyDelivered);

        // 欠数数量 = 计划数量 - 入库数量（直接现算，不依赖虚拟列）
        BigDecimal qtyShortage = qtyEntered.subtract(qtyDelivered);
        mTab.setValue("QtyShortage", qtyShortage);

        // 补数数量默认 = 欠数数量，支持用户后续修改
        mTab.setValue("RepairQty", qtyShortage);

        return "";
    }
}
