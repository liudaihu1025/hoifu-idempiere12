package com.hoifu.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.compiere.acct.Doc;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MCharge;
import org.compiere.util.Env;

import com.hoifu.model.MBillTransaction;

/**
 * 票据作业单文档处理器
 */
public class Doc_BillTransaction extends Doc {

	public Doc_BillTransaction(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, MBillTransaction.class, rs, null, trxName);
	}

	@Override
	protected String loadDocumentDetails() {
		MBillTransaction billTrx = (MBillTransaction) getPO();
		setDateDoc(billTrx.getBusinessDate());
		setDateAcct(billTrx.getDateTrx());

		// 设置金额
		setAmount(Doc.AMTTYPE_Gross, billTrx.getBillAmt());
		setAmount(Doc.AMTTYPE_Net, billTrx.getBillAmt());

		return null;
	}

	@Override
	public BigDecimal getBalance() {
		return Env.ZERO; // 票据作业单总是平衡的
	}

	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		ArrayList<Fact> facts = new ArrayList<Fact>();

		// 创建 Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);

		MBillTransaction billTrx = (MBillTransaction) getPO();
		String transactionType = billTrx.getTransactionType();
		BigDecimal billAmt = billTrx.getBillAmt();

		if (billAmt == null || billAmt.signum() == 0) {
			return facts; // 金额为0时不生成分录
		}

		// 获取会计架构默认设置
		MAcctSchemaDefault acctDefault = as.getAcctSchemaDefault();

		// 根据交易类型创建不同的分录
		if ("A".equals(transactionType)) {
			// 签收
			createBillAcceptanceFact(fact, billTrx, acctDefault, as);
		} else if ("E".equals(transactionType)) {
			// 背书
			createBillEndorseFact(fact, billTrx, acctDefault, as);
		} else if ("R".equals(transactionType)) {
			// 到期收款
			createBillReceiptFact(fact, billTrx, acctDefault, as);
		} else if ("I".equals(transactionType)) {
			// 签发
			createBillIssueFact(fact, billTrx, acctDefault, as);
		} else if ("P".equals(transactionType)) {
			// 到期付款
			createBillPaymentFact(fact, billTrx, acctDefault, as);
		} else if("C".equals(transactionType)) {
			// 贴现
			 createBillDiscountFact(fact, billTrx, acctDefault, as);
		}else {
			// 其他事务类型，根据需要扩展
			log.warning("未知的票据交易类型: " + transactionType);
		}

		if (fact.getLines().length > 0) {
			facts.add(fact);
		}

		return facts;
	}

	/**
	 * 创建票据签收会计分录 借：应收票据 贷：应收账款
	 */
	private void createBillAcceptanceFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 获取应收票据科目
		int receivableAcctID = acctDefault.get_ValueAsInt("N_Receivable_Acct");
		if (receivableAcctID <= 0) {
			log.severe("未配置应收票据科目 (N_Receivable_Acct)");
			return;
		}

		MAccount receivableAcct = MAccount.get(getCtx(), receivableAcctID);
		if (receivableAcct == null) {
			log.severe("应收票据科目不存在: " + receivableAcctID);
			return;
		}

		// 借：应收票据
		FactLine drLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), billAmt, null);
		if (drLine != null) {
			drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			drLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			drLine.setDescription(billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
		}

		// 获取应收账款科目
		int receivableTradeAcctID = acctDefault.getC_Receivable_Acct();
		if (receivableTradeAcctID <= 0) {
			log.severe("未配置应收账款科目 (C_Receivable_Acct)");
			return;
		}

		MAccount receivableTradeAcct = MAccount.get(getCtx(), receivableTradeAcctID);
		if (receivableTradeAcct == null) {
			log.severe("应收账款科目不存在: " + receivableTradeAcctID);
			return;
		}

		// 贷：应收账款
		FactLine crLine = fact.createLine(null, receivableTradeAcct, billTrx.getC_Currency_ID(), null, billAmt);
		if (crLine != null) {
			crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			crLine.setDescription(billTrx.getTransactionTypeName() + " - 应收账款 - 票据作业单号:" + billTrx.getDocumentNo());
		}
	}

	/**
	 * 创建票据背书会计分录 借：应付账款 贷：应收票据
	 */
	private void createBillEndorseFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 获取应付账款科目
		int payableAcctID = acctDefault.getV_Liability_Acct();
		if (payableAcctID <= 0) {
			log.severe("未配置应付账款科目 (V_Liability_Acct)");
			return;
		}

		MAccount payableAcct = MAccount.get(getCtx(), payableAcctID);
		if (payableAcct == null) {
			log.severe("应付账款科目不存在: " + payableAcctID);
			return;
		}

		// 借：应付账款
		FactLine drLine = fact.createLine(null, payableAcct, billTrx.getC_Currency_ID(), billAmt, null);
		if (drLine != null) {
			drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			drLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			drLine.setDescription(billTrx.getTransactionTypeName() + " - 应付账款 - 票据作业单号:" + billTrx.getDocumentNo());
		}

		// 获取应收票据科目
		int receivableAcctID = acctDefault.get_ValueAsInt("N_Receivable_Acct");
		if (receivableAcctID <= 0) {
			log.severe("未配置应收票据科目 (N_Receivable_Acct)");
			return;
		}

		MAccount receivableAcct = MAccount.get(getCtx(), receivableAcctID);
		if (receivableAcct == null) {
			log.severe("应收票据科目不存在: " + receivableAcctID);
			return;
		}

		// 贷：应收票据
		FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, billAmt);
		if (crLine != null) {
			crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			crLine.setDescription(billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
		}
	}

	/**
	 * 创建票据到期收款会计分录 借：银行存款 贷：应收票据
	 */
	private void createBillReceiptFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 获取银行存款科目
		int bankAcctID = acctDefault.getB_Asset_Acct();
		if (bankAcctID <= 0) {
			log.severe("未配置银行存款科目 (B_Asset_Acct)");
			return;
		}

		MAccount bankAcct = MAccount.get(getCtx(), bankAcctID);
		if (bankAcct == null) {
			log.severe("银行存款科目不存在: " + bankAcctID);
			return;
		}

		// 借：银行存款
		FactLine drLine = fact.createLine(null, bankAcct, billTrx.getC_Currency_ID(), billAmt, null);
		if (drLine != null) {
			drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			drLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			drLine.setDescription(billTrx.getTransactionTypeName() + " - 银行存款 - 票据作业单号:" + billTrx.getDocumentNo());
		}

		// 获取应收票据科目
		int receivableAcctID = acctDefault.get_ValueAsInt("N_Receivable_Acct");
		if (receivableAcctID <= 0) {
			log.severe("未配置应收票据科目 (N_Receivable_Acct)");
			return;
		}

		MAccount receivableAcct = MAccount.get(getCtx(), receivableAcctID);
		if (receivableAcct == null) {
			log.severe("应收票据科目不存在: " + receivableAcctID);
			return;
		}

		// 贷：应收票据
		FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, billAmt);
		if (crLine != null) {
			crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			crLine.setDescription(billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
		}
	}

	/**
	 * 创建票据签发会计分录 借：应付账款 贷：应付票据
	 */
	private void createBillIssueFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 获取应付账款科目
		int payableAcctID = acctDefault.getV_Liability_Acct();
		if (payableAcctID <= 0) {
			log.severe("未配置应付账款科目 (V_Liability_Acct)");
			return;
		}

		MAccount payableAcct = MAccount.get(getCtx(), payableAcctID);
		if (payableAcct == null) {
			log.severe("应付账款科目不存在: " + payableAcctID);
			return;
		}

		// 借：应付账款
		FactLine drLine = fact.createLine(null, payableAcct, billTrx.getC_Currency_ID(), billAmt, null);
		if (drLine != null) {
			drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			drLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			drLine.setDescription(billTrx.getTransactionTypeName() + " - 应付账款 - 票据作业单号:" + billTrx.getDocumentNo());
		}

		// 获取应付票据科目
		int billPayableAcctID = acctDefault.get_ValueAsInt("N_Payable_Acct");
		if (billPayableAcctID <= 0) {
			log.severe("未配置应付票据科目 (N_Payable_Acct)");
			return;
		}

		MAccount billPayableAcct = MAccount.get(getCtx(), billPayableAcctID);
		if (billPayableAcct == null) {
			log.severe("应付票据科目不存在: " + billPayableAcctID);
			return;
		}

		// 贷：应付票据
		FactLine crLine = fact.createLine(null, billPayableAcct, billTrx.getC_Currency_ID(), null, billAmt);
		if (crLine != null) {
			crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			crLine.setDescription(billTrx.getTransactionTypeName() + " - 应付票据 - 票据作业单号:" + billTrx.getDocumentNo());
		}
	}

	/**
	 * 创建票据到期付款会计分录 借：应付票据 贷：银行存款
	 */
	private void createBillPaymentFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 获取应付票据科目
		int billPayableAcctID = acctDefault.get_ValueAsInt("N_Payable_Acct");
		if (billPayableAcctID <= 0) {
			log.severe("未配置应付票据科目 (N_Payable_Acct)");
			return;
		}

		MAccount billPayableAcct = MAccount.get(getCtx(), billPayableAcctID);
		if (billPayableAcct == null) {
			log.severe("应付票据科目不存在: " + billPayableAcctID);
			return;
		}

		// 借：应付票据
		FactLine drLine = fact.createLine(null, billPayableAcct, billTrx.getC_Currency_ID(), billAmt, null);
		if (drLine != null) {
			drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			drLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			drLine.setDescription(billTrx.getTransactionTypeName() + " - 应付票据 - 票据作业单号:" + billTrx.getDocumentNo());
		}

		// 获取银行存款科目
		int bankAcctID = acctDefault.getB_Asset_Acct();
		if (bankAcctID <= 0) {
			log.severe("未配置银行存款科目 (B_Asset_Acct)");
			return;
		}

		MAccount bankAcct = MAccount.get(getCtx(), bankAcctID);
		if (bankAcct == null) {
			log.severe("银行存款科目不存在: " + bankAcctID);
			return;
		}

		// 贷：银行存款
		FactLine crLine = fact.createLine(null, bankAcct, billTrx.getC_Currency_ID(), null, billAmt);
		if (crLine != null) {
			crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
			crLine.setDescription(billTrx.getTransactionTypeName() + " - 银行存款 - 票据作业单号:" + billTrx.getDocumentNo());
		}
	}

	private void createBillDiscountFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
									   MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();           // 票面金额
		BigDecimal discountFeeAmt = (BigDecimal) billTrx.get_Value("DiscountFeeAmt"); // 贴现费用
		BigDecimal discountNetAmt = (BigDecimal) billTrx.get_Value("DiscountNetAmt"); // 贴现净额
		boolean isRecourse = "true".equals(billTrx.get_ValueAsString("IsRecourse"));

		// 设置银行账户ID
		setC_BankAccount_ID(billTrx.getC_BankAccount_ID());

		// 借：银行存款（贴现净额）
		fact.createLine(null, getAccount(Doc.ACCTTYPE_BankAsset, as), billTrx.getC_Currency_ID(), discountNetAmt, null);

		// 借：杂费科目（财务费用，贴现费用）- 直接使用C_Charge_ID
		if (discountFeeAmt.compareTo(Env.ZERO) > 0) {
			MAccount chargeAcct = MCharge.getAccount(billTrx.getC_Charge_ID(), as);
			fact.createLine(null, chargeAcct, billTrx.getC_Currency_ID(), discountFeeAmt, null);
		}

		// 贷方科目：根据是否保留追索权确定
		MAccount creditAcct;
		if (isRecourse) {
			// 保留追索权 - 贷：短期借款
			// 获取短期借款科目
			int shortTermBorrowingAcctId = acctDefault.get_ValueAsInt("C_ShortTermBorrowing_Acct");
			if (shortTermBorrowingAcctId <= 0) {
				log.severe("未配置短期借款科目 (C_ShortTermBorrowing_Acct)");
				return;
			}
			creditAcct = MAccount.get(getCtx(), shortTermBorrowingAcctId);
			if (creditAcct == null) {
				log.severe("短期借款科目不存在: " + shortTermBorrowingAcctId);
				return;
			}
			// 贷：短期借款
			FactLine crLine = fact.createLine(null, creditAcct, billTrx.getC_Currency_ID(), null, billAmt);
			if (crLine != null) {
				crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
				crLine.setDescription(billTrx.getTransactionTypeName() + " - 短期借款 - 票据作业单号:" + billTrx.getDocumentNo());
			}
		} else {
			// 不保留追索权-贷：应收票据（票面金额）
			// 获取应收票据科目
			int receivableAcctID = acctDefault.get_ValueAsInt("N_Receivable_Acct");
			if (receivableAcctID <= 0) {
				log.severe("未配置应收票据科目 (N_Receivable_Acct)");
				return;
			}

			MAccount receivableAcct = MAccount.get(getCtx(), receivableAcctID);
			if (receivableAcct == null) {
				log.severe("应收票据科目不存在: " + receivableAcctID);
				return;
			}

			// 贷：应收票据
			FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, billAmt);
			if (crLine != null) {
				crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
				crLine.setDescription(billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
			}
		}
	}

}
