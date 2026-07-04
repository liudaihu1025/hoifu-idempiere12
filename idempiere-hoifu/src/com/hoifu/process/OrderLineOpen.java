package com.hoifu.process;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.hoifu.model.MOrderEx;

@org.adempiere.base.annotation.Process
public class OrderLineOpen extends SvrProcess {

	private int p_C_OrderLine_ID = 0;

	@Override
	protected void prepare() {
		p_C_OrderLine_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		if (p_C_OrderLine_ID == 0)
			throw new IllegalArgumentException("No OrderLine");

		MOrderLine line = new MOrderLine(getCtx(), p_C_OrderLine_ID, get_TrxName());
		if (line.get_ID() == 0)
			throw new IllegalArgumentException("Order line not found");

		MOrderEx order = new MOrderEx(getCtx(), line.getC_Order_ID(), get_TrxName());

		// ===== 前置检查 =====

		// 仅允许采购订单（isSOTrx=false）
		if (order.isSOTrx())
			throw new IllegalArgumentException("此功能仅适用于采购订单");

		// 1. 订单不能是已关闭状态
		if (MOrder.DOCSTATUS_Closed.equals(order.getDocStatus()))
			throw new IllegalArgumentException("订单已关闭，无法打开明细");

		// 2. 行必须是已关闭状态
		if (!"CL".equals(line.get_Value("OrderLineStatus")))
			throw new IllegalArgumentException("订单明细未处于已关闭状态");

		// ===== 打开逻辑 =====

		// 1. 清除行状态
		line.set_ValueNoCheck("OrderLineStatus", null);
		line.set_ValueNoCheck("Processed", Boolean.FALSE);

		// 2. 恢复数量（同 MOrder.reopenIt() 逻辑）
		if (Env.ZERO.compareTo(line.getQtyLostSales()) != 0) {
			line.setQtyOrdered(line.getQtyLostSales().add(line.getQtyDelivered()));
			line.setQtyLostSales(Env.ZERO);
			line.setDescription(line.getDescriptionStrippingCloseTag());
		}
		line.saveEx(get_TrxName());

		// 3. 恢复预留/在途数量
		if (!order.reserveStockForLine(line))
			throw new IllegalArgumentException("无法恢复库存预留数量");


		return "订单明细已打开";
	}
}