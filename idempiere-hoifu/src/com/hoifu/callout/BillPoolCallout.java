package com.hoifu.callout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

/**
 * 票据池Callout
 */
@Callout(tableName = "C_Bill_Pool", columnName = { "BillDate", "MaturityDate", "BillAmt", "BillRate",
		"PaymentTermDays", "SubBillStartNo", "SubBillEndNo", "IsSplittable", "BillPackageNo" })
public class BillPoolCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (value == null) {
			return "";
		}

		// 获取所有相关字段值
		Timestamp billDate = (Timestamp) mTab.getValue("BillDate");
		Timestamp maturityDate = (Timestamp) mTab.getValue("MaturityDate");

		BigDecimal billAmt = (BigDecimal) mTab.getValue("BillAmt");
		BigDecimal billRate = (BigDecimal) mTab.getValue("BillRate");
		BigDecimal paymentTermDays = (BigDecimal) mTab.getValue("PaymentTermDays");

		// 计算付款期限
		if (billDate != null && maturityDate != null) {
			long diffInMillies = maturityDate.getTime() - billDate.getTime();
			long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
			BigDecimal calculatedPaymentTermDays = BigDecimal.valueOf(diffInDays);
			mTab.setValue("PaymentTermDays", calculatedPaymentTermDays);
			paymentTermDays = calculatedPaymentTermDays; // 更新本地变量
		}

		// 计算子票序号
		String columnName = mField.getColumnName();
		boolean splittable = mTab.getValueAsBoolean("IsSplittable");

		if ("IsSplittable".equals(columnName) || "BillAmt".equals(columnName)) {
			if (splittable && billAmt != null) {
				// 可拆分：设置默认子票号
				String subBillStartNoStr = (String) mTab.getValue("SubBillStartNo");
				if (subBillStartNoStr == null || subBillStartNoStr.trim().isEmpty()) {
					subBillStartNoStr = "1";
					mTab.setValue("SubBillStartNo", subBillStartNoStr);
				}
				try {
					BigDecimal startNo = new BigDecimal(subBillStartNoStr.trim());
					BigDecimal endNo = startNo.add(billAmt.multiply(BigDecimal.valueOf(100))).subtract(BigDecimal.ONE);
					mTab.setValue("SubBillEndNo", endNo.setScale(0, RoundingMode.HALF_UP).toPlainString());
				} catch (NumberFormatException e) {
					// ignore
				}
			} else if (!splittable && "IsSplittable".equals(columnName)) {
				// 不可拆分：清空子票序号
				mTab.setValue("SubBillStartNo", null);
				mTab.setValue("SubBillEndNo", null);
			}
		}

		// 计算分包金额 SubPackageAmt
		BigDecimal subPackageAmt = calculateSubPackageAmt(mTab, billAmt);
		if (subPackageAmt != null) {
			subPackageAmt = subPackageAmt.setScale(2, RoundingMode.HALF_UP);
			mTab.setValue("SubPackageAmt", subPackageAmt);
		}

		// 计算到期金额 MaturityAmt = SubPackageAmt × (1 + (BillRate/100 × PaymentTermDays / 360))  
		if (subPackageAmt != null && billRate != null && paymentTermDays != null) {
			BigDecimal rateDecimal = billRate.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP);
			BigDecimal rateFactor = rateDecimal
					.multiply(paymentTermDays.divide(BigDecimal.valueOf(360), 12, RoundingMode.HALF_UP));
			BigDecimal onePlusRateFactor = BigDecimal.ONE.add(rateFactor);
			BigDecimal maturityAmt = subPackageAmt.multiply(onePlusRateFactor);
			maturityAmt = maturityAmt.setScale(2, RoundingMode.HALF_UP);
			mTab.setValue("MaturityAmt", maturityAmt);
		}

		// 生成票据号码
		generateBillNumber(mTab);

		return "";
	}

	/**
	 * 计算分包金额 - 若不可拆分（IsSplittable = N），SubPackageAmt = BillAmt - 若可拆分（IsSplittable
	 * = Y），SubPackageAmt = BillAmt * (SubBillEndNo - SubBillStartNo + 1) / 100
	 * SubBillEndNo 和 SubBillStartNo 都是字符串类型，需要解析为数字
	 */
	private BigDecimal calculateSubPackageAmt(GridTab mTab, BigDecimal billAmt) {
		if (billAmt == null) {
			return null;
		}

		boolean splittable = mTab.getValueAsBoolean("IsSplittable");

		if (!splittable) {
			// 不可拆分：分包金额 = 票据金额
			return billAmt;
		}

		// 可拆分：分包金额 = BillAmt * (SubBillEndNo - SubBillStartNo + 1) / 100
		String subBillStartNoStr = (String) mTab.getValue("SubBillStartNo");
		String subBillEndNoStr = (String) mTab.getValue("SubBillEndNo");

		if (subBillStartNoStr == null || subBillStartNoStr.trim().isEmpty() || subBillEndNoStr == null
				|| subBillEndNoStr.trim().isEmpty()) {
			return null; // 子票号不完整，无法计算
		}

		try {
			BigDecimal startNo = new BigDecimal(subBillStartNoStr.trim());
			BigDecimal endNo = new BigDecimal(subBillEndNoStr.trim());
			// 子票数量 = EndNo - StartNo + 1
			BigDecimal subBillCount = endNo.subtract(startNo).add(BigDecimal.ONE);

			if (subBillCount.compareTo(BigDecimal.ZERO) <= 0) {
				return null; // 子票数量无效
			}

			// SubPackageAmt = BillAmt * subBillCount / 100
			BigDecimal subPackageAmt = billAmt.multiply(subBillCount).divide(BigDecimal.valueOf(100), 12,
					RoundingMode.HALF_UP);
			return subPackageAmt;
		} catch (NumberFormatException e) {
			// SubBillStartNo 或 SubBillEndNo 无法解析为数字
			return null;
		}
	}

	/**
	 * 生成票据号码 格式：BillPackageNo-SubBillStartNo-SubBillEndNo
	 * 如果SubBillStartNo或SubBillEndNo为空，则不拼接
	 */
	private void generateBillNumber(GridTab mTab) {
		String billPackageNo = (String) mTab.getValue("BillPackageNo");
		Object subBillStartNoObj = mTab.getValue("SubBillStartNo");
		Object subBillEndNoObj = mTab.getValue("SubBillEndNo");

		if (billPackageNo == null || billPackageNo.trim().isEmpty()) {
			return; // 如果BillPackageNo为空，不生成BillNumber
		}

		StringBuilder billNumber = new StringBuilder(billPackageNo.trim());

		// 检查SubBillStartNo是否为空
		boolean hasStartNo = subBillStartNoObj != null
				&& !(subBillStartNoObj instanceof String && ((String) subBillStartNoObj).trim().isEmpty())
				&& !(subBillStartNoObj instanceof BigDecimal
						&& ((BigDecimal) subBillStartNoObj).compareTo(BigDecimal.ZERO) == 0);

		// 检查SubBillEndNo是否为空
		boolean hasEndNo = subBillEndNoObj != null
				&& !(subBillEndNoObj instanceof String && ((String) subBillEndNoObj).trim().isEmpty())
				&& !(subBillEndNoObj instanceof BigDecimal
						&& ((BigDecimal) subBillEndNoObj).compareTo(BigDecimal.ZERO) == 0);

		if (hasStartNo && hasEndNo) {
			// 两个都有值，格式：BillPackageNo-SubBillStartNo-SubBillEndNo
			String startNo = subBillStartNoObj.toString().trim();
			String endNo = subBillEndNoObj.toString().trim();
			billNumber.append("-").append(startNo).append("-").append(endNo);
		} else if (hasStartNo) {
			// 只有StartNo，格式：BillPackageNo-SubBillStartNo
			String startNo = subBillStartNoObj.toString().trim();
			billNumber.append("-").append(startNo);
		}
		// 如果都没有值，只使用BillPackageNo

		mTab.setValue("BillNumber", billNumber.toString());
	}
}
