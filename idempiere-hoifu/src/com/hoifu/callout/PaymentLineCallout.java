package com.hoifu.callout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;

import com.hoifu.model.MBillPool;

/**
 * C_PaymentLine表的通用Callout 支持金额计算和其他字段处理
 */
@Callout(tableName = "C_PaymentLine", columnName = { "Amount", "C_Currency_ID", "C_Bill_Pool_ID", "EndorsementAmt",
		"PayAmt", "TenderType" })
public class PaymentLineCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		String columnName = mField.getColumnName();

		if ("C_Currency_ID".equals(columnName)) {
			// 币种字段处理 - 控制汇率类型字段显示
			return handleCurrencyChange(ctx, WindowNo, mTab, mField, value, oldValue);
		} else if ("C_Bill_Pool_ID".equals(columnName)) {
			// 票据流水号字段处理 - 从票据池表自动填充数据
			return handleDocumentNoChange(ctx, WindowNo, mTab, mField, value, oldValue);
		} else if ("TenderType".equals(columnName)) {
			// 支付方式字段处理 - 设置业务伙伴上下文变量
			return handleTenderTypeChange(ctx, WindowNo, mTab, mField, value, oldValue);
		} else if ("EndorsementAmt".equals(columnName)) {
			// 背书金额字段处理 - 设置Amount为背书金额的值
			return handleEndorsementAmtChange(ctx, WindowNo, mTab, mField, value, oldValue);
		}
		return "";
	}

	/**
	 * 处理支付方式变化，设置业务伙伴上下文变量
	 */
	private String handleTenderTypeChange(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value,
			Object oldValue) {
		// 通过父Tab获取业务伙伴ID
		GridTab parentTab = mTab.getParentTab();
		if (parentTab != null) {
			Integer C_BPartner_ID = (Integer) parentTab.getValue("C_BPartner_ID");
			if (C_BPartner_ID != null && C_BPartner_ID > 0) {
				// 设置业务伙伴上下文变量
				Env.setContext(ctx, "#PaymentLine_C_BPartner_ID", C_BPartner_ID.toString());
			}
		}
		return "";
	}

	/**
	 * 处理币种变化，控制转换类型字段显示
	 */
	private String handleCurrencyChange(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value,
			Object oldValue) {
		Integer lineCurrency = (Integer) value;
		Integer headerCurrency = null;

		// 通过父Tab获取币种类型
		GridTab parentTab = mTab.getParentTab();
		if (parentTab != null) {
			headerCurrency = (Integer) parentTab.getValue("C_Currency_ID");
		}

		// 设置上下文变量：币种不同时为Y，相同或任一为null时为N
		boolean needConversion = lineCurrency != null && headerCurrency != null && !lineCurrency.equals(headerCurrency);
		Env.setContext(ctx, WindowNo, "IsCurrencyDifferent", needConversion ? "Y" : "N");

		return "";
	}

	/**
	 * 处理票据流水号变化，从票据池表自动填充数据
	 */
	private String handleDocumentNoChange(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value,
			Object oldValue) {
		if (value == null) {
			return "";
		}

		// value 是 C_Bill_Pool_ID (Integer)
		Integer billPoolId = (Integer) value;
		if (billPoolId == 0) {
			return "";
		}

		// 使用ID加载票据池记录
		MBillPool billPool = new MBillPool(ctx, billPoolId, null);

		if (billPool != null && billPool.get_ID() > 0) {

			// 设置票据金额、背书金额、支付金额
			if (billPool.getBillAmt() != null) {
				mTab.setValue("BillAmt", billPool.getBillAmt());
				mTab.setValue("EndorsementAmt", billPool.getBillAmt());
				mTab.setValue("Amount", billPool.getBillAmt());
			}

			// 设置分包金额
			if (billPool.getSubPackageAmt() != null) {
				mTab.setValue("SubPackageAmt", billPool.getSubPackageAmt());
			}

			// 设置票据类型
			if (billPool.getBillType() != null) {
				mTab.setValue("BillType", billPool.getBillType());
			}

			// 设置票据（包）号
			if (billPool.getBillPackageNo() != null) {
				mTab.setValue("BillPackageNo", billPool.getBillPackageNo());
			}

			// 设置到期日期
			if (billPool.getMaturityDate() != null) {
				mTab.setValue("MaturityDate", billPool.getMaturityDate());
			}

			// 设置票面利率
			if (billPool.getBillRate() != null) {
				mTab.setValue("BillRate", billPool.getBillRate());
			}

			// 设置业务状态
			if (billPool.getBusinessStatus() != null) {
				mTab.setValue("BusinessStatus", billPool.getBusinessStatus());
			}

			// 计算并设置费用金额
			BigDecimal chargeAmt = null;
			BigDecimal baseAmount = null;

			if (!billPool.isSplittable()) {
				// 不可拆分：费用金额 = 票据金额 * 2%
				baseAmount = billPool.getBillAmt();
			} else {
				// 可拆分：费用金额 = 分包金额 * 2%
				baseAmount = billPool.getSubPackageAmt();
			}

			if (baseAmount != null && baseAmount.compareTo(BigDecimal.ZERO) > 0) {
				// 2% = 0.02
				BigDecimal rate = new BigDecimal("0.02");
				chargeAmt = baseAmount.multiply(rate);

				// 设置2位小数精度
				chargeAmt = chargeAmt.setScale(2, RoundingMode.HALF_UP);
				mTab.setValue("ChargeAmt", chargeAmt);
			}

			// 判断可拆分字段，控制分包金额字段显示
			GridField subPackageAmtField = mTab.getField("SubPackageAmt");
			if (subPackageAmtField != null) {
				// 使用标准的isSplittable()方法
				if (!billPool.isSplittable()) {
					// 如果不可拆分，隐藏分包金额字段
					subPackageAmtField.setDisplayed(false);
				} else {
					// 如果可拆分，显示分包金额字段
					subPackageAmtField.setDisplayed(true);
				}
			}
		}

		return "";
	}

	/**
	 * 处理背书金额变化，设置Amount为背书金额的值
	 */
	private String handleEndorsementAmtChange(Properties ctx, int WindowNo, GridTab mTab, GridField mField,
			Object value, Object oldValue) {
		if (value != null) {
			BigDecimal endorsementAmt = (BigDecimal) value;
			mTab.setValue("Amount", endorsementAmt);
		}
		return "";
	}
}