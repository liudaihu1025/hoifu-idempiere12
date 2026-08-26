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

import com.hoifu.model.MBillPool;
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
	 * 创建票据到期收款会计分录 根据 是否贴现、附追索权、票面利率、业务状态组合判断，生成不同的会计分录
	 */
	private void createBillReceiptFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 1. 根据作业单的 C_Bill_Pool_ID 反查票据池记录，获取"是否贴现"、"是否追索权"、"票面利率"
		MBillPool billPool = new MBillPool(getCtx(), billTrx.getC_Bill_Pool_ID(), getTrxName());

		// 2. 是否贴现：票据池新增字段 IsDiscounted='Y' 表示已贴现
		boolean isDiscounted = "true".equals(billPool.get_ValueAsString("IsDiscounted"));

		// 3. 是否附追索权（如商业承兑汇票）
		boolean isRecourse = billPool.isRecourse();

		// 4. 票面利率是否为0或空 -> 无息票据
		BigDecimal billRate = billPool.getBillRate();
		boolean isInterestFree = (billRate == null || billRate.compareTo(Env.ZERO) == 0);

		// 5. 是否拒付
		boolean isDishonored = "V".equals(billTrx.getBusinessStatus());

		// 6.结算单位
		int settleBPartnerID = billTrx.get_ValueAsInt("settle_bp_id");
		// 7.往来单位
		int bpartnerID = billTrx.getC_BPartner_ID();

		if (!isDishonored) {
			// ============ 银行不拒付（BusinessStatus != "V"） ============
			if (!isDiscounted) {
				// ---- 未贴现 ----
				if (!isInterestFree) {
					// 未贴现（有息票据）—— 下次迭代，暂不处理
					log.info("未贴现有息票据的到期收款分录，暂未实现（下次迭代）");
					return;
				}
				// 未贴现（无息票据）：借：银行存款(结算单位)，贷：应收票据(往来单位)

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
				// 借：银行存款，BP维度 = 结算单位
				FactLine drLine = fact.createLine(null, bankAcct, billTrx.getC_Currency_ID(), billAmt, null);
				if (drLine != null) {
					drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					drLine.setC_BPartner_ID(settleBPartnerID);
					drLine.setDescription(
							billTrx.getTransactionTypeName() + " - 银行存款 - 票据作业单号:" + billTrx.getDocumentNo());
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
				// 贷：应收票据，BP维度 = 往来单位
				FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, billAmt);
				if (crLine != null) {
					crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					crLine.setC_BPartner_ID(bpartnerID);
					crLine.setDescription(
							billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
				}
			} else {
				// ---- 已贴现 ----
				if (!isRecourse) {
					// 已贴现（不附追索权，如银行承兑汇票）—— 无分录
					return;
				}
				// 已贴现（附追索权，如商业承兑汇票）：借：短期借款(结算单位)，贷：应收票据(往来单位)

				// 获取短期借款科目
				int shortTermBorrowingAcctId = acctDefault.get_ValueAsInt("C_ShortTermBorrowing_Acct");
				if (shortTermBorrowingAcctId <= 0) {
					log.severe("未配置短期借款科目 (C_ShortTermBorrowing_Acct)");
					return;
				}
				MAccount shortTermAcct = MAccount.get(getCtx(), shortTermBorrowingAcctId);
				if (shortTermAcct == null) {
					log.severe("短期借款科目不存在: " + shortTermBorrowingAcctId);
					return;
				}
				// 借：短期借款，BP维度 = 结算单位
				FactLine drLine = fact.createLine(null, shortTermAcct, billTrx.getC_Currency_ID(), billAmt, null);
				if (drLine != null) {
					drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					drLine.setC_BPartner_ID(settleBPartnerID);
					drLine.setDescription(
							billTrx.getTransactionTypeName() + " - 短期借款 - 票据作业单号:" + billTrx.getDocumentNo());
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
				// 贷：应收票据，BP维度 = 往来单位
				FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, billAmt);
				if (crLine != null) {
					crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					crLine.setC_BPartner_ID(bpartnerID);
					crLine.setDescription(
							billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
				}
			}
		} else {
			// ============ 银行拒付（BusinessStatus == "V"） ============
			if (!isDiscounted) {
				// ---- 未贴现 ----
				if (!isInterestFree) {
					// 未贴现（有息票据）—— 下次迭代，暂不处理
					log.info("未贴现有息票据拒付的到期收款分录，暂未实现（下次迭代）");
					return;
				}
				// 未贴现（无息票据）拒付：借：应收账款(出票人/背书人)，贷：应收票据(出票人/背书人)

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
				// 借：应收账款，BP维度 = 出票人/背书人
				FactLine drLine = fact.createLine(null, receivableTradeAcct, billTrx.getC_Currency_ID(), billAmt, null);
				if (drLine != null) {
					drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					drLine.setC_BPartner_ID(bpartnerID);
					drLine.setDescription(
							billTrx.getTransactionTypeName() + " - 应收账款 - 票据作业单号:" + billTrx.getDocumentNo());
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
				// 贷：应收票据，BP维度 = 出票人/背书人
				FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, billAmt);
				if (crLine != null) {
					crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					crLine.setC_BPartner_ID(bpartnerID);
					crLine.setDescription(
							billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
				}
			} else {
				// ---- 已贴现 ----
				if (!isRecourse) {
					// 已贴现（不附追索权，如银行承兑汇票）—— 无分录
					return;
				}
				// 已贴现（附追索权，如商业承兑汇票）拒付：两笔分录

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

					// 第一笔分录：继续挂应收账款（对方拒付，票款未能收到，转应收账款）
					// 借：应收账款，BP维度 = 出票人/背书人（复用 C_BPartner_ID 字段）
					FactLine drLine1 = fact.createLine(null, receivableTradeAcct, billTrx.getC_Currency_ID(), billAmt,
							null);
					if (drLine1 != null) {
						drLine1.setAD_Org_ID(billTrx.getAD_Org_ID());
						drLine1.setC_BPartner_ID(bpartnerID);
						drLine1.setDescription(
								billTrx.getTransactionTypeName() + " - 应收账款 - 票据作业单号:" + billTrx.getDocumentNo());
					}

					// 获取应收票据科目
					int receivableAcctID2 = acctDefault.get_ValueAsInt("N_Receivable_Acct");
					if (receivableAcctID2 <= 0) {
						log.severe("未配置应收票据科目 (N_Receivable_Acct)");
						return;
					}
					MAccount receivableAcct2 = MAccount.get(getCtx(), receivableAcctID2);
					if (receivableAcct2 == null) {
						log.severe("应收票据科目不存在: " + receivableAcctID2);
						return;
					}

					// 贷：应收票据，BP维度 = 出票人/背书人
					FactLine crLine1 = fact.createLine(null, receivableAcct2, billTrx.getC_Currency_ID(), null,
							billAmt);
					if (crLine1 != null) {
						crLine1.setAD_Org_ID(billTrx.getAD_Org_ID());
						crLine1.setC_BPartner_ID(bpartnerID);
						crLine1.setDescription(
								billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
					}

					// 第二笔分录：海富归还银行借款（票据既已贴现拿到银行的钱，现对方拒付，需偿还此前的短期借款）
					// 获取短期借款科目
					int shortTermBorrowingAcctId = acctDefault.get_ValueAsInt("C_ShortTermBorrowing_Acct");
					if (shortTermBorrowingAcctId <= 0) {
						log.severe("未配置短期借款科目 (C_ShortTermBorrowing_Acct)");
						return;
					}
					MAccount shortTermBorrowingAcct = MAccount.get(getCtx(), shortTermBorrowingAcctId);
					if (shortTermBorrowingAcct == null) {
						log.severe("短期借款科目不存在: " + shortTermBorrowingAcctId);
						return;
					}

					// 借：短期借款，BP维度 = 结算单位（settle_bp_id）
					FactLine drLine2 = fact.createLine(null, shortTermBorrowingAcct, billTrx.getC_Currency_ID(),
							billAmt, null);
					if (drLine2 != null) {
						drLine2.setAD_Org_ID(billTrx.getAD_Org_ID());
						drLine2.setC_BPartner_ID(settleBPartnerID);
						drLine2.setDescription(
								billTrx.getTransactionTypeName() + " - 短期借款 - 票据作业单号:" + billTrx.getDocumentNo());
					}

					// 获取银行存款科目
					int bankAcctID2 = acctDefault.getB_Asset_Acct();
					if (bankAcctID2 <= 0) {
						log.severe("未配置银行存款科目 (B_Asset_Acct)");
						return;
					}
					MAccount bankAcct2 = MAccount.get(getCtx(), bankAcctID2);
					if (bankAcct2 == null) {
						log.severe("银行存款科目不存在: " + bankAcctID2);
						return;
					}

					// 贷：银行存款，BP维度 = 结算单位（settle_bp_id）
					FactLine crLine2 = fact.createLine(null, bankAcct2, billTrx.getC_Currency_ID(), null, billAmt);
					if (crLine2 != null) {
						crLine2.setAD_Org_ID(billTrx.getAD_Org_ID());
						crLine2.setC_BPartner_ID(settleBPartnerID);
						crLine2.setDescription(
								billTrx.getTransactionTypeName() + " - 银行存款 - 票据作业单号:" + billTrx.getDocumentNo());
					}
				}
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
	 * 创建票据到期付款会计分录 根据 作业单.业务状态(BusinessStatus='M'表示我方拒付，其余视为不拒付)、
	 * 票据.票据类型(BillType='B'银票/'G'金票/'C'商票)、票据.票面利率(BillRate) 组合判断，生成不同的会计分录
	 */
	private void createBillPaymentFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
			MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt();

		// 1. 根据作业单的 C_Bill_Pool_ID 反查票据池记录，获取"票据类型"、"票面利率"
		MBillPool billPool = new MBillPool(getCtx(), billTrx.getC_Bill_Pool_ID(), getTrxName());
		String billType = billPool.getBillType();

		// 2. 票面利率是否为0或空 -> 无息票据（有息票据下次迭代，本次不生成分录，直接返回）
		BigDecimal billRate = billPool.getBillRate();
		boolean isInterestFree = billRate == null || billRate.compareTo(Env.ZERO) == 0;

		// 3. 是否拒付： M(我方拒付) 和 L(已结清)

		boolean isDishonored = "M".equals(billTrx.getBusinessStatus());

		int bpartnerID = billTrx.getC_BPartner_ID(); // 往来单位
		int settleBPartnerID = billTrx.get_ValueAsInt("settle_bp_id"); // 结算单位

		// 应付票据科目
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

		if (!isDishonored) {
			// ---- 不拒付（有息票据） ----
			if (!isInterestFree) {
				// 有息票据【下次迭代】，暂不生成分录
				return;
			}
			// ---- 不拒付（无息票据） ----
			// 借：应付票据，BP维度 = 往来单位
			FactLine drLine = fact.createLine(null, billPayableAcct, billTrx.getC_Currency_ID(), billAmt, null);
			if (drLine != null) {
				drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				drLine.setC_BPartner_ID(bpartnerID);
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

			// 贷：银行存款，BP维度 = 结算单位
			FactLine crLine = fact.createLine(null, bankAcct, billTrx.getC_Currency_ID(), null, billAmt);
			if (crLine != null) {
				crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				crLine.setC_BPartner_ID(settleBPartnerID);
				crLine.setDescription(billTrx.getTransactionTypeName() + " - 银行存款 - 票据作业单号:" + billTrx.getDocumentNo());
			}
		} else {
			// ---- 拒付 ----
			// 借：应付票据，BP维度 = 往来单位（两个分支均相同，先创建）
			FactLine drLine = fact.createLine(null, billPayableAcct, billTrx.getC_Currency_ID(), billAmt, null);
			if (drLine != null) {
				drLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				drLine.setC_BPartner_ID(bpartnerID);
				drLine.setDescription(billTrx.getTransactionTypeName() + " - 应付票据 - 票据作业单号:" + billTrx.getDocumentNo());
			}

			if ("B".equals(billType)) {
				// 拒付 + 银票：贷：短期借款（承兑银行的短期借款），BP维度 = 结算单位
				int shortTermBorrowingAcctId = acctDefault.get_ValueAsInt("C_ShortTermBorrowing_Acct");
				if (shortTermBorrowingAcctId <= 0) {
					log.severe("未配置短期借款科目 (C_ShortTermBorrowing_Acct)");
					return;
				}
				MAccount shortTermAcct = MAccount.get(getCtx(), shortTermBorrowingAcctId);
				if (shortTermAcct == null) {
					log.severe("短期借款科目不存在: " + shortTermBorrowingAcctId);
					return;
				}

				FactLine crLine = fact.createLine(null, shortTermAcct, billTrx.getC_Currency_ID(), null, billAmt);
				if (crLine != null) {
					crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					crLine.setC_BPartner_ID(settleBPartnerID);
					crLine.setDescription(
							billTrx.getTransactionTypeName() + " - 短期借款 - 票据作业单号:" + billTrx.getDocumentNo());
				}
			} else if ("G".equals(billType) || "C".equals(billType)) {
				// 拒付 + 金票/商票：贷：应付账款（重新转回应付账款），BP维度 = 往来单位
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

				FactLine crLine = fact.createLine(null, payableAcct, billTrx.getC_Currency_ID(), null, billAmt);
				if (crLine != null) {
					crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
					crLine.setC_BPartner_ID(bpartnerID);
					crLine.setDescription(
							billTrx.getTransactionTypeName() + " - 应付账款 - 票据作业单号:" + billTrx.getDocumentNo());
				}
			} else {
				log.severe("到期付款拒付场景票据类型异常，期望为B/G/C，实际为: " + billType + "，票据作业单号:" + billTrx.getDocumentNo());
				return;
			}
		}
	}

	private void createBillDiscountFact(Fact fact, MBillTransaction billTrx, MAcctSchemaDefault acctDefault,
									   MAcctSchema as) {
		BigDecimal billAmt = billTrx.getBillAmt(); // 票面金额
		BigDecimal settleAmt = billTrx.getSettleAmt(); // 结算金额
		BigDecimal chargeAmt = billTrx.getChargeAmt(); // 贴现费用（费用项目1）
		BigDecimal discountFeeAmt = (BigDecimal) billTrx.get_Value("DiscountFeeAmt"); // 贴现息（费用项目2）
		BigDecimal interestAmt = billTrx.getInterestAmt(); // 到期利息
		if (interestAmt == null)
			interestAmt = Env.ZERO;
		BigDecimal maturityAmt = billAmt.add(interestAmt); // 到期金额 = 票面金额 + 到期利息
		boolean isRecourse = "true".equals(billTrx.get_ValueAsString("IsRecourse"));

		if (settleAmt == null)
			settleAmt = Env.ZERO;
		if (chargeAmt == null)
			chargeAmt = Env.ZERO;
		if (discountFeeAmt == null)
			discountFeeAmt = Env.ZERO;

		// 结算单位（settle_bp_ID）—用于借方三条分录的BP维度
		int settleBPartnerID = billTrx.get_ValueAsInt("settle_bp_ID");

		// 设置银行账户ID
		setC_BankAccount_ID(billTrx.getC_BankAccount_ID());

		// 借：银行存款（结算金额）BP维度 = 结算单位
		FactLine bankLine = fact.createLine(null, getAccount(Doc.ACCTTYPE_BankAsset, as), billTrx.getC_Currency_ID(),
				settleAmt, null);
		if (bankLine != null) {
			bankLine.setAD_Org_ID(billTrx.getAD_Org_ID());
			bankLine.setC_BPartner_ID(settleBPartnerID);
			bankLine.setDescription(billTrx.getTransactionTypeName() + " - 银行存款 - 票据作业单号:" + billTrx.getDocumentNo());
		}

		// 借：费用项目1（财务费用，贴现费用 ChargeAmt）- 使用 C_Charge_ID - BP维度 = 结算单位
		if (chargeAmt.compareTo(Env.ZERO) > 0) {
			MAccount chargeAcct = MCharge.getAccount(billTrx.getC_Charge_ID(), as);
			FactLine chargeLine = fact.createLine(null, chargeAcct, billTrx.getC_Currency_ID(), chargeAmt, null);
			if (chargeLine != null) {
				chargeLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				chargeLine.setC_BPartner_ID(settleBPartnerID);
				chargeLine.setDescription(
						billTrx.getTransactionTypeName() + " - 财务费用(贴现费用) - 票据作业单号:" + billTrx.getDocumentNo());
			}
		}

		// 借：费用项目2（财务费用，贴现息 DiscountFeeAmt）- 使用 C_Charge2_ID - BP维度 = 结算单位【0816】
		if (discountFeeAmt.compareTo(Env.ZERO) > 0) {
			int charge2Id = billTrx.get_ValueAsInt("C_Charge2_ID");
			MAccount discountFeeAcct = MCharge.getAccount(charge2Id, as);
			FactLine discountFeeLine = fact.createLine(null, discountFeeAcct, billTrx.getC_Currency_ID(),
					discountFeeAmt, null);
			if (discountFeeLine != null) {
				discountFeeLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				discountFeeLine.setC_BPartner_ID(settleBPartnerID);
				discountFeeLine.setDescription(
						billTrx.getTransactionTypeName() + " - 财务费用(贴现息) - 票据作业单号:" + billTrx.getDocumentNo());
			}
		}

		// 贷方科目：根据是否保留追索权确定（此部分逻辑不变）
		MAccount creditAcct;
		if (isRecourse) {
			// 保留追索权 - 贷：短期借款 - BP维度 = 结算单位
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
			FactLine crLine = fact.createLine(null, creditAcct, billTrx.getC_Currency_ID(), null, maturityAmt);
			if (crLine != null) {
				crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				crLine.setC_BPartner_ID(settleBPartnerID);
				crLine.setDescription(billTrx.getTransactionTypeName() + " - 短期借款 - 票据作业单号:" + billTrx.getDocumentNo());
			}
		} else {
			// 不保留追索权 - 贷：应收票据（票面金额）- BP维度 = 往来单位
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
			FactLine crLine = fact.createLine(null, receivableAcct, billTrx.getC_Currency_ID(), null, maturityAmt);
			if (crLine != null) {
				crLine.setAD_Org_ID(billTrx.getAD_Org_ID());
				crLine.setC_BPartner_ID(billTrx.getC_BPartner_ID());
				crLine.setDescription(billTrx.getTransactionTypeName() + " - 应收票据 - 票据作业单号:" + billTrx.getDocumentNo());
			}
		}
	}

}
