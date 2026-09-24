package org.libero.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;

/**
 * 补数申请流程
 * 从信息窗口勾选的欠数工单创建《生产补数申请单》
 * 参照 BillReceiptEndorseProcess 从 T_Selection 读取信息窗口选中行的模式
 */
@org.adempiere.base.annotation.Process
public class PPOrderShortageApplyProcess extends SvrProcess {

    private String p_ShortageReason;
    private String p_ReasonDesc;
	private String p_RepairMethod;

	private int p_OrderLineNewID = 0;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            String name = para.getParameterName();
            if (para.getParameter() == null)
                continue;
            if ("ShortageReason".equals(name))
                p_ShortageReason = (String) para.getParameter();
            else if ("ReasonDesc".equals(name))
                p_ReasonDesc = (String) para.getParameter();
            else if ("RepairMethod".equals(name))
                p_RepairMethod = (String) para.getParameter();
			else if ("C_OrderLine_New_ID".equals(name))
				p_OrderLineNewID = para.getParameterAsInt();
        }

    }

    @Override
    protected String doIt() throws Exception {
        // 从 T_Selection 读取信息窗口勾选的唯一一条 PP_Order_ID
        int ppOrderId = 0;
        String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            pstmt.setInt(1, getAD_PInstance_ID());
            rs = pstmt.executeQuery();
            if (rs.next())
                ppOrderId = rs.getInt(1);
            if (rs.next()) // 校验只能选一条
                throw new AdempiereUserError("只能选择一条欠数工单");
        } finally {
            DB.close(rs, pstmt);
        }

        if (ppOrderId <= 0)
            throw new AdempiereUserError("请先选择一条欠数工单");

        MPPOrder order = new MPPOrder(getCtx(), ppOrderId, get_TrxName());
        if (order.get_ID() <= 0)
            throw new AdempiereUserError("未找到工单");

        // 校验补数状态必须为"待处理"(PD)
        String repairStatus = order.get_ValueAsString("RepairStatus");
		if (!"DP".equals(repairStatus))
			throw new AdempiereUserError("仅【补数状态】为待处理的工单可发起补数申请");

        // 创建 生产补数申请单
        int tableId = DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_Table_ID FROM AD_Table WHERE TableName='PP_Order_Repair_Request' AND AD_Client_ID IN (0,?)",
                Env.getAD_Client_ID(getCtx()));
        if (tableId <= 0)
            throw new AdempiereUserError("未找到 PP_Order_Repair_Request 表定义，请先在应用字典中注册");

        // 使用通用 PO 方式创建记录（待 MPPOrderRepairRequest Model 类建好后可改为强类型）
        org.compiere.model.PO request = org.compiere.model.MTable.get(getCtx(), tableId).getPO(0, get_TrxName());
        request.set_ValueOfColumn("PP_Order_ID", order.getPP_Order_ID());
        request.set_ValueOfColumn("C_OrderLine_ID", order.getC_OrderLine_ID());
        request.set_ValueOfColumn("M_Product_ID", order.getM_Product_ID());
        request.set_ValueOfColumn("QtyPlanned", order.getQtyEntered());
        request.set_ValueOfColumn("QtyDeliveredSnap", order.getQtyDelivered());

        if (p_RepairMethod == null || p_RepairMethod.isEmpty())
            throw new AdempiereUserError("请选择补数方式（工单补数/随销单补数）");

        request.set_ValueOfColumn("RepairMethod", p_RepairMethod);


		BigDecimal qtyRepair = order.getQtyEntered().subtract(order.getQtyDelivered());
		if (qtyRepair.signum() < 0) {
			qtyRepair = BigDecimal.ZERO; // 防止已超交货时出现负数
		}
		// 工单数量(QtyEntered) - 入库数量(QtyDelivered)
		request.set_ValueOfColumn("QtyShortage", qtyRepair);
		request.set_ValueOfColumn("RepairQty", qtyRepair);

        request.set_ValueOfColumn("ShortageReason", p_ShortageReason);
        request.set_ValueOfColumn("ReasonDesc", p_ReasonDesc);

		if (p_OrderLineNewID > 0)
			request.set_ValueOfColumn("C_OrderLine_New_ID", p_OrderLineNewID);
		else
			request.set_ValueOfColumn("C_OrderLine_New_ID", null);

        request.set_ValueOfColumn("DateDoc", new java.sql.Timestamp(System.currentTimeMillis()));
        request.set_ValueOfColumn("AD_Org_ID", order.getAD_Org_ID());
        request.set_ValueOfColumn("AD_Client_ID", order.getAD_Client_ID());
        request.set_ValueOfColumn("DocStatus", "DR"); // 草稿状态

		Timestamp now = new Timestamp(System.currentTimeMillis());
		request.set_ValueOfColumn("DateDoc", now);
		request.set_ValueOfColumn("DatePromised", now); // 补充：非空约束，取当前日期时间
        request.saveEx();

		if ("SO".equals(p_RepairMethod) && p_OrderLineNewID <= 0)
			throw new AdempiereUserError("补数方式为随销单补数时，必须选择新销售单号");

        // 将工单补数状态更新为"补数申请中"(IP)
        order.set_ValueOfColumn("RepairStatus", "IP");
        order.saveEx();

        String docNo = request.get_ValueAsString("DocumentNo");
        getProcessInfo().setRecord_ID(request.get_ID());

		// 生成可点击穿透到《生产补数申请单》记录的日志
		addLog(request.get_ID(), null, null, "补数申请单：" + (docNo != null ? docNo : String.valueOf(request.get_ID())),
				tableId, request.get_ID());

		return "@Success@";
    }
}
