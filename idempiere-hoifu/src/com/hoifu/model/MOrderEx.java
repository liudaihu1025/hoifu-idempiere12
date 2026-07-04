package com.hoifu.model;

import java.util.Properties;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;

public class MOrderEx extends MOrder {

	public MOrderEx(Properties ctx, int C_Order_ID, String trxName) {
		super(ctx, C_Order_ID, trxName);
	}

	public boolean reserveStockForLine(MOrderLine line) {
		return reserveStock(null, new MOrderLine[] { line });
	}
}