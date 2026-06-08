package com.hoifu.validator;

import java.math.BigDecimal;

import org.compiere.model.MClient;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class InvoicePayAmtValidator implements ModelValidator {

	// 应付单
	private static final int TARGET_DOCTYPE_ID = 1000710;
	private int m_AD_Client_ID = -1;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		if (client != null) {
			m_AD_Client_ID = client.getAD_Client_ID();
		}
		// 注册 C_InvoiceLine 表的验证事件
		engine.addModelChange("C_InvoiceLine", this);
	}

	@Override
	public int getAD_Client_ID() {
		return m_AD_Client_ID;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		// TODO 自动生成的方法存根
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		if (po instanceof MInvoiceLine) {
			MInvoiceLine line = (MInvoiceLine) po;

			// 监听行的新增、修改、删除
			if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE || type == TYPE_AFTER_DELETE) {
				updateParentInvoicePayAmt(line);
			}

		}
		return null;
	}

	private void updateParentInvoicePayAmt(MInvoiceLine line) {
		if (line.getC_Invoice_ID() <= 0) {
			return;
		}

		MInvoice invoice = line.getParent();
		if (invoice == null || invoice.get_ID() != line.getC_Invoice_ID()) {
			invoice = new MInvoice(line.getCtx(), line.getC_Invoice_ID(), line.get_TrxName());
		}

		// 只在指定的文档类型时才计算
		if (invoice.getC_DocTypeTarget_ID() != TARGET_DOCTYPE_ID) {
			return;
		}

		// 直接计算，不需要递归保护
		BigDecimal totalLines = invoice.getTotalLines();
		if (totalLines == null)
			totalLines = Env.ZERO;

		BigDecimal rebate = (BigDecimal) invoice.get_Value("Rebate");
		if (rebate == null)
			rebate = Env.ZERO;

		BigDecimal writeOffAmt = (BigDecimal) invoice.get_Value("WriteOffAmt");
		if (writeOffAmt == null)
			writeOffAmt = Env.ZERO;

		BigDecimal discountAmt = (BigDecimal) invoice.get_Value("DiscountAmt");
		if (discountAmt == null)
			discountAmt = Env.ZERO;

		BigDecimal payAmt = totalLines.subtract(rebate).subtract(writeOffAmt).subtract(discountAmt);

		invoice.set_ValueOfColumn("PayAmt", payAmt);
		invoice.saveEx();
	}

	@Override
	public String docValidate(PO po, int timing) {
		// TODO 自动生成的方法存根
		return null;
	}

}