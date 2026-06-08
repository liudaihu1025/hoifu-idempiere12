package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.Env;

@org.adempiere.base.Model(table = "C_PaymentLine")
public class MPaymentLine extends X_C_PaymentLine {

	private static final long serialVersionUID = 20260305L;

	public MPaymentLine(Properties ctx, int C_PaymentLine_ID, String trxName) {
		super(ctx, C_PaymentLine_ID, trxName);
	}

	public MPaymentLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 计算实付金额 Amount = DueAmt - DiscountAmt - WriteOffAmt - OverUnderAmt
	 */
	public void calculateAmount() {
		BigDecimal amount = getDueAmt().subtract(getDiscountAmt()).subtract(getWriteOffAmt())
				.subtract(getOverUnderAmt());
		setAmount(amount);
	}

	/**
	 * 计算剩余金额 RemainingAmt = DueAmt - Amount
	 */
	public void calculateRemainingAmt() {
		BigDecimal remainingAmt = getDueAmt().subtract(getAmount());
		setRemainingAmt(remainingAmt);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 验证必填字段
		if (getC_Currency_ID() == 0) {
			log.saveError("Error", "币种不能为空");
			return false;
		}

		if (getTenderType() == null || getTenderType().length() != 1) {
			log.saveError("Error", "支付方式不能为空且必须为1位字符");
			return false;
		}

		// 验证金额不能为负数
		if (getDueAmt().compareTo(Env.ZERO) < 0) {
			log.saveError("Error", "应收金额不能为负数");
			return false;
		}

		return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		// 如果是新建记录，设置默认值
		if (newRecord) {
			// 自动计算金额
			calculateAmount();
			calculateRemainingAmt();
		}

		return true;
	}
}
