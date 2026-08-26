package com.hoifu.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

import org.adempiere.webui.apps.IProcessParameterListener;
import org.adempiere.webui.apps.ProcessParameterPanel;
import org.adempiere.webui.editor.WEditor;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

import com.hoifu.model.MBillPool;

@Component(service = IProcessParameterListener.class, property = {
		"process.className=com.hoifu.process.BillDiscountProcess" })
public class BillDiscountParameterListener implements IProcessParameterListener {

	// 需与 BillPoolInfoWindow.preRunProcess 中写入的 key 保持一致
	private static final String CTX_KEY_BILLPOOL_ID = "_IWInfo_BillDiscount_M_BillPool_ID";

	@Override
	public void onInit(ProcessParameterPanel parameterPanel) {
		Integer billPoolId = getSelectedBillPoolId(parameterPanel);
		if (billPoolId != null) {
			recalculate(parameterPanel);
		}
	}

	@Override
	public void onChange(ProcessParameterPanel parameterPanel, String columnName, WEditor editor) {
		if ("DiscountRate".equals(columnName) || "ChargeAmt".equals(columnName) || "BusinessDate".equals(columnName)) {
			recalculate(parameterPanel);
		}
	}


	/**
	 * 从 Env context 读取 BillPoolInfoWindow.preRunProcess 提前写入的票据ID， 而不是依赖 onInit
	 * 阶段还未写入的 ProcessInfo.Record_IDs / T_Selection。
	 */
	private Integer getSelectedBillPoolId(ProcessParameterPanel parameterPanel) {
		String idStr = Env.getContext(Env.getCtx(), parameterPanel.getWindowNo(), CTX_KEY_BILLPOOL_ID);
		if (idStr == null || idStr.isEmpty())
			return null;
		try {
			return Integer.parseInt(idStr);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void recalculate(ProcessParameterPanel parameterPanel) {
		WEditor discountRateEditor = parameterPanel.getEditor("DiscountRate");// 贴现率
		WEditor chargeAmtEditor = parameterPanel.getEditor("ChargeAmt");// 贴现费用
		WEditor discountFeeAmtEditor = parameterPanel.getEditor("DiscountFeeAmt");// 贴现息
		WEditor settleAmtEditor = parameterPanel.getEditor("SettleAmt");// 结算金额
		WEditor businessDateEditor = parameterPanel.getEditor("BusinessDate");// 业务日期

		if (discountRateEditor == null || discountFeeAmtEditor == null || settleAmtEditor == null)
			return;

		// 取选中票据ID（改为从 Env context 读取，而非 ProcessInfo.Record_IDs）
		Integer billPoolId = getSelectedBillPoolId(parameterPanel);
		if (billPoolId == null)
			return;

		MBillPool billPool = new MBillPool(Env.getCtx(), billPoolId, null);
		if (billPool.get_ID() <= 0)
			return;

		BigDecimal billAmt = billPool.getBillAmt();
		BigDecimal billRate = billPool.getBillRate();
		Timestamp maturityDate = billPool.getMaturityDate();
		Timestamp billDate = billPool.getBillDate();

		Object discountRateVal = discountRateEditor.getValue();// 贴现率
		Object businessDateVal = businessDateEditor != null ? businessDateEditor.getValue() : null;

		if (billAmt == null || billRate == null || maturityDate == null || discountRateVal == null
				|| businessDateVal == null || billDate == null)
			return;


		BigDecimal discountRate = (BigDecimal) discountRateVal;
		Timestamp businessDate = (Timestamp) businessDateVal;

		// 到期利息的天数 = 到期日期 - 出票日期
		long interestDays = (maturityDate.getTime() - billDate.getTime()) / (1000L * 60 * 60 * 24);
		if (interestDays < 0)
			interestDays = 0;

		// 贴现息的天数 = 到期日期 - 业务日期
		long maturityDays = (maturityDate.getTime() - businessDate.getTime()) / (1000L * 60 * 60 * 24);
		if (maturityDays < 0)
			maturityDays = 0;

		// 到期利息 = 票面金额 * (票面利率 * 到期天数 / 360)
		BigDecimal maturityInterest = billAmt.multiply(billRate).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(interestDays)).divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);

		// 贴现息 = (票据金额 + 到期利息) * 贴现率 * (到期天数 / 360)
		BigDecimal discountFeeAmt = billAmt.add(maturityInterest).multiply(discountRate)
				.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(maturityDays))
				.divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);

		discountFeeAmtEditor.setValue(discountFeeAmt);// 计算后的贴现息

		// 贴现费用
		BigDecimal chargeAmt = chargeAmtEditor != null && chargeAmtEditor.getValue() != null
				? (BigDecimal) chargeAmtEditor.getValue()
				: Env.ZERO;

		// 结算金额 = 票面金额 + 到期利息 - (贴现费用 + 贴现息)
		BigDecimal settleAmt = billAmt.add(maturityInterest).subtract(chargeAmt).subtract(discountFeeAmt);
		settleAmtEditor.setValue(settleAmt);
	}
}