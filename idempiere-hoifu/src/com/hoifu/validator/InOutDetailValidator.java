package com.hoifu.validator;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * M_InOutLine对账月字段初始化验证器
 * 
 * @ClassName: InOutDetailValidator
 * @author ldh
 * @date 2025年12月19日
 */
public class InOutDetailValidator implements ModelValidator {

	private static final CLogger log = CLogger.getCLogger(InOutDetailValidator.class);
	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

	private int m_AD_Client_ID = -1;

	@Override
	public void initialize(org.compiere.model.ModelValidationEngine engine, org.compiere.model.MClient client) {
		Optional.ofNullable(client).ifPresent(c -> m_AD_Client_ID = c.getAD_Client_ID());
		engine.addModelChange("M_InOutLine", this);
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		if (po instanceof MInOutLine && type == ModelValidator.TYPE_NEW) {
			MInOutLine line = (MInOutLine) po;
			return initializeReconciliationMonth(line);
		}
		return null;
	}

	/**
	 * 初始化对账月份字段
	 * 
	 * @param line
	 * @return
	 */
	private String initializeReconciliationMonth(MInOutLine line) {
		try {
			MInOut inOut = new MInOut(line.getCtx(), line.getM_InOut_ID(), line.get_TrxName());

//			int netDays = getNetDaysFromPaymentTerm(inOut);
			String reconciliationMonth = calculateReconciliationMonth(inOut);

			line.set_ValueOfColumn("ReconciliationMonth", reconciliationMonth);

		} catch (Exception e) {
			return "初始化对账月字段失败: " + e.getMessage();
		}
		return null;
	}

	/**
	 * 获取支付条款的净天数字段 - 优先从订单获取，其次从业务伙伴获取
	 * 
	 * @Title: getNetDaysFromPaymentTerm
	 * @param inOut
	 * @return int
	 */
	private int getNetDaysFromPaymentTerm(MInOut inOut) {
		try {
			// 首先尝试从关联的订单获取支付条款
			Optional<Integer> orderPaymentTermId = getOrderPaymentTermId(inOut);

			if (orderPaymentTermId.isPresent()) {
				return getNetDaysFromPaymentTermId(inOut, orderPaymentTermId.get());
			}

			// 如果没有关联订单，则从业务伙伴获取支付条款
			return getBusinessPartnerPaymentTermNetDays(inOut);

		} catch (Exception e) {
			log.warning("获取支付条款净天数失败: " + e.getMessage());
			return 0;
		}
	}

	/**
	 * 从收发单获取关联订单的支付条款ID
	 * 
	 * @Title: getOrderPaymentTermId
	 * @param inOut
	 * @return Optional<Integer>
	 */
	private Optional<Integer> getOrderPaymentTermId(MInOut inOut) {
		// 直接从M_InOut表获取C_Order_ID
		if (inOut.getC_Order_ID() <= 0) {
			return Optional.empty();
		}

		String sql = "SELECT C_PaymentTerm_ID FROM C_Order WHERE C_Order_ID = ? AND C_PaymentTerm_ID IS NOT NULL";
		int paymentTermId = DB.getSQLValue(inOut.get_TrxName(), sql, inOut.getC_Order_ID());
		return paymentTermId > 0 ? Optional.of(paymentTermId) : Optional.empty();
	}

	/**
	 * 从业务伙伴获取支付条款净天数
	 * 
	 * @Title: getBusinessPartnerPaymentTermNetDays
	 * @param inOut
	 * @return
	 * @return int
	 */
	private int getBusinessPartnerPaymentTermNetDays(MInOut inOut) {
		if (inOut.getC_BPartner_ID() <= 0) {
			return 0;
		}

		String sql = "SELECT C_PaymentTerm_ID FROM C_BPartner WHERE C_BPartner_ID = ? AND C_PaymentTerm_ID IS NOT NULL";
		int paymentTermId = DB.getSQLValue(inOut.get_TrxName(), sql, inOut.getC_BPartner_ID());

		return paymentTermId > 0 ? getNetDaysFromPaymentTermId(inOut, paymentTermId) : 0;
	}

	/**
	 * 根据支付条款ID获取净天数
	 * 
	 * @Title: getNetDaysFromPaymentTermId
	 * @param inOut
	 * @param paymentTermId
	 * @return
	 * @return int
	 */
	private int getNetDaysFromPaymentTermId(MInOut inOut, int paymentTermId) {
		String sql = "SELECT NetDays FROM C_PaymentTerm WHERE C_PaymentTerm_ID = ?";
		int netDays = DB.getSQLValue(inOut.get_TrxName(), sql, paymentTermId);
		return Math.max(netDays, 0);
	}

	/**
	 * 根据净天数计算对账月份
	 * 
	 * @Title: calculateReconciliationMonth
	 * @param netDays
	 * @param created
	 * @return
	 * @return String
	 */
	private String calculateReconciliationMonth(MInOut inOut) {

		String sql = "SELECT cutoffday FROM C_BPartner WHERE C_BPartner_ID = ?";
		int cutoffday = DB.getSQLValue(inOut.get_TrxName(), sql, inOut.getC_BPartner_ID());

		if (cutoffday <= 0) cutoffday = 25;

		LocalDate createdDate = inOut.getCreated().toLocalDateTime().toLocalDate();

		int dayOfMonth = createdDate.getDayOfMonth();

		return dayOfMonth > cutoffday ? createdDate.plusMonths(1).format(MONTH_FORMATTER)
				: createdDate.format(MONTH_FORMATTER);
	}

	@Override
	public int getAD_Client_ID() {
		return m_AD_Client_ID;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		return null;
	}
}