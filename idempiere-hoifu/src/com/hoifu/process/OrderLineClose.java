package com.hoifu.process;

import java.math.BigDecimal;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.hoifu.model.MOrderEx;

@org.adempiere.base.annotation.Process
public class OrderLineClose extends SvrProcess {

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
			throw new IllegalArgumentException("订单已关闭，无法单独关闭明细");

		// 2. 行未已关闭
		if ("CL".equals(line.get_Value("OrderLineStatus")))
			throw new IllegalArgumentException("订单明细已关闭");


		// ===== 关闭逻辑 =====

		// 1. 更新行状态
		line.set_ValueNoCheck("OrderLineStatus", "CL");
		line.set_ValueNoCheck("Processed", Boolean.TRUE);

		// 2. 更新数量（同 MOrder.closeIt() 逻辑）
		BigDecimal old = line.getQtyOrdered();
		if (old.compareTo(line.getQtyDelivered()) != 0) {
			if (line.getQtyOrdered().compareTo(line.getQtyDelivered()) > 0) {
				line.setQtyLostSales(line.getQtyOrdered().subtract(line.getQtyDelivered()));
				line.setQtyOrdered(line.getQtyDelivered());
			} else {
				line.setQtyLostSales(Env.ZERO);
			}
			line.addDescription("Close (" + old + ")");
		}
		line.saveEx(get_TrxName());

		// 3. 更新预留/在途数量（传入单行数组）
		if (!order.reserveStockForLine(line))
			throw new IllegalArgumentException("无法更新库存预留数量");


		return "订单明细已关闭";
	}
}