/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Recurring Model
 *
 *	@author Jorg Janke
 *	@version $Id: MRecurring.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 */
public class MRecurring extends X_C_Recurring
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 6053691462050896981L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_Recurring_UU  UUID key
     * @param trxName Transaction
     */
    public MRecurring(Properties ctx, String C_Recurring_UU, String trxName) {
        super(ctx, C_Recurring_UU, trxName);
		if (Util.isEmpty(C_Recurring_UU))
			setInitialDefaults();
    }

    /**
     * @param ctx
     * @param C_Recurring_ID
     * @param trxName
     */
	public MRecurring (Properties ctx, int C_Recurring_ID, String trxName)
	{
		super (ctx, C_Recurring_ID, trxName);
		if (C_Recurring_ID == 0)
			setInitialDefaults();
	}	//	MRecurring

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDateNextRun (new Timestamp(System.currentTimeMillis()));
		setFrequencyType (FREQUENCYTYPE_Monthly);
		setFrequency(1);
		setRunsMax (1);
		setRunsRemaining (0);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MRecurring (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRecurring

	/**
	 *	String representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder ("MRecurring[")
			.append(get_ID()).append("-").append(getName());
		if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Order))
			sb.append(",C_Order_ID=").append(getC_Order_ID());
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Invoice))
			sb.append(",C_Invoice_ID=").append(getC_Invoice_ID());
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Project))
			sb.append(",C_Project_ID=").append(getC_Project_ID());
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_GLJournal))
			sb.append(",GL_JournalBatch_ID=").append(getGL_JournalBatch_ID());
		sb.append(",Frequency=").append(getFrequencyType()).append("*").append(getFrequency());
		sb.append("]");
		return sb.toString();
	}	//	toString
	
	/**
	 * 	Execute Recurring Run.
	 *	@return clear text execution message
	 */
	public String executeRun()
	{
		Timestamp dateDoc = getDateNextRun();

		// 如果当前时间还未到 DateNextRun，直接返回，不创建单据
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (now.before(dateDoc)) {
			return "尚未到达运行时间: " + dateDoc;
		}

		if (!calculateRuns())
			throw new IllegalStateException ("无剩余运行次数");

		//	log
		MRecurringRun run = new MRecurringRun (getCtx(), this);
		String msg = "@Created@ ";

		//	Copy
		if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Order))
		{
			MOrder from = new MOrder (getCtx(), getC_Order_ID(), get_TrxName());
			MOrder order = MOrder.copyFrom (from, dateDoc, 
				from.getC_DocType_ID(), from.isSOTrx(), false, false, get_TrxName());
			run.setC_Order_ID(order.getC_Order_ID());
			msg += order.getDocumentNo();
			setLastPO(order);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Invoice))
		{
			MInvoice from = new MInvoice (getCtx(), getC_Invoice_ID(), get_TrxName());
			MInvoice invoice = MInvoice.copyFrom (from, dateDoc, dateDoc,
				from.getC_DocType_ID(), from.isSOTrx(), false, get_TrxName(), false);
			run.setC_Invoice_ID(invoice.getC_Invoice_ID());
			msg += invoice.getDocumentNo();
			setLastPO(invoice);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Project))
		{
			MProject project = MProject.copyFrom (getCtx(), getC_Project_ID(), dateDoc, get_TrxName());
			run.setC_Project_ID(project.getC_Project_ID());
			msg += project.getValue();
			setLastPO(project);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_GLJournal))
		{
			MJournalBatch journal = MJournalBatch.copyFrom (getCtx(), getGL_JournalBatch_ID(), dateDoc, get_TrxName());
			run.setGL_JournalBatch_ID(journal.getGL_JournalBatch_ID());
			msg += journal.getDocumentNo();
			setLastPO(journal);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Payment))
		{
			MPayment from = new MPayment(getCtx(), getC_Payment_ID(), get_TrxName());
			MPayment to = new MPayment(getCtx(), 0, get_TrxName());
			copyValues(from, to);
			to.setAD_Org_ID(from.getAD_Org_ID());
			to.setIsReconciled(false); // can't be already on a bank statement
			to.setDateAcct(dateDoc);
			to.setDateTrx(dateDoc);
			to.setDocumentNo("");
			to.setProcessed(false);
			to.setPosted(false);
			to.setDocStatus(MPayment.DOCSTATUS_Drafted);
			to.setDocAction(MPayment.DOCACTION_Complete);
			to.saveEx();

			run.setC_Payment_ID(to.getC_Payment_ID());
			msg += to.getDocumentNo();	
			setLastPO(to);
		} else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_GLJournalAmortization))
		{
			// ── 前置校验 ──────────────────────────────────────────────────
			if (getGL_Journal_ID() == 0)
				throw new IllegalStateException("未配置原始凭证记账（GL_Journal_ID）");
			if (getAccount_ID() == 0)
				throw new IllegalStateException("未配置分摊科目（Account_ID）");
			if (getRunsMax() <= 0)
				throw new IllegalStateException("最大执行次数（RunsMax）必须大于0");

			// ── 1. 加载原始 Journal ────────────────────────────────────────
			MJournal sourceJournal = new MJournal(getCtx(), getGL_Journal_ID(), get_TrxName());
			if (sourceJournal.get_ID() == 0)
				throw new IllegalStateException("原始凭证记录不存在，GL_Journal_ID=" + getGL_Journal_ID());

			// ── 2. 查找原始 Journal 中 Account_ID 对应的明细行 ──────────────
			// GL_JournalLine 的科目通过 C_ValidCombination_ID 存储，需要 JOIN C_ValidCombination
			MJournalLine srcLine = new Query(getCtx(), MJournalLine.Table_Name,
					"GL_Journal_ID=? AND EXISTS ("
							+ "SELECT 1 FROM C_ValidCombination vc "
							+ "WHERE vc.C_ValidCombination_ID=GL_JournalLine.C_ValidCombination_ID "
							+ "AND vc.Account_ID=?)",
					get_TrxName())
					.setParameters(getGL_Journal_ID(), getAccount_ID())
					.setOrderBy(MJournalLine.COLUMNNAME_Line)
					.first();

			if (srcLine == null)
				throw new IllegalStateException("在原始凭证中未找到科目（Account_ID="
						+ getAccount_ID() + "）对应的明细行，请检查科目C_Recurring.Account_ID配置");

			int srcCombinationID = srcLine.getC_ValidCombination_ID();
			BigDecimal srcDr = srcLine.getAmtSourceDr();
			BigDecimal srcCr = srcLine.getAmtSourceCr();

			// ── 3. 计算净额和每次分摊金额 ──────────────────────────────────
			// srcBalance 正数=借方，负数=贷方，支持负数金额场景
			BigDecimal srcBalance = srcDr.subtract(srcCr);
			if (srcBalance.signum() == 0)
				throw new IllegalStateException("原始明细行净额为零，无法分摊");

			int precision = MCurrency.getStdPrecision(getCtx(), sourceJournal.getC_Currency_ID());
			BigDecimal amtPerRun = srcBalance.divide(
					BigDecimal.valueOf(getRunsMax()), precision, RoundingMode.HALF_UP);
			if (amtPerRun.signum() == 0)
				throw new IllegalStateException("每次分摊金额为零（原始净额="
						+ srcBalance + "，RunsMax=" + getRunsMax() + "），请检查配置");

			// ── 4. 查找绑定的 GL_Distribution ─────────────────────────────
			MDistribution distribution = new Query(getCtx(), MDistribution.Table_Name,
					"C_Recurring_ID=? AND IsActive='Y'", get_TrxName())
					.setParameters(getC_Recurring_ID())
					.first();

			if (distribution == null)
				throw new IllegalStateException("未找到绑定的分配规则，"
						+ "请在分配子页签中配置分配规则" );
			if (!distribution.isValid())
				throw new IllegalStateException("分配规则未通过验证（IsValid=N），"
						+ "请执行验证操作，GL_Distribution_ID=" + distribution.getGL_Distribution_ID());

			// ── 5. 计算各明细行分摊金额 ────────────────────────────────────
			// distribute() 内部会调用 dl.setAccount(baseAccount)，必须传入真实对象，不能传 null
			MAccount baseAccount = new MAccount(getCtx(), srcCombinationID, get_TrxName());
			distribution.distribute(baseAccount, amtPerRun, Env.ZERO, sourceJournal.getC_Currency_ID());
			MDistributionLine[] lines = distribution.getLines(false);

			List<MDistributionLine> activeLines = new ArrayList<>();
			for (MDistributionLine dl : lines) {
				if (dl.isActive() && dl.getAmt() != null && dl.getAmt().signum() != 0)
					activeLines.add(dl);
			}
			if (activeLines.isEmpty())
				throw new IllegalStateException("GL分配规则没有有效的明细行，"
						+ "请检查分配明细配置（IsActive=Y 且 Percent>0）");

			// ── 6. 创建新 GL_Journal ───────────────────────────────────────
			MJournal newJournal = new MJournal(getCtx(), 0, get_TrxName());
			PO.copyValues(sourceJournal, newJournal, getAD_Client_ID(), getAD_Org_ID());
			newJournal.setGL_JournalBatch_ID(0);
			newJournal.set_ValueNoCheck("DocumentNo", null);   // 系统自动生成新单号
			newJournal.set_ValueNoCheck("C_Period_ID", null);  // 由 setDateAcct 自动推算
			newJournal.setDateDoc(dateDoc);
			newJournal.setDateAcct(dateDoc);                   // 自动推算 C_Period_ID
			newJournal.setDocStatus(MJournal.DOCSTATUS_Drafted);
			newJournal.setDocAction(MJournal.DOCACTION_Complete);
			newJournal.setTotalCr(Env.ZERO);
			newJournal.setTotalDr(Env.ZERO);
			newJournal.setIsApproved(false);
			newJournal.setIsPrinted(false);
			newJournal.setPosted(false);
			newJournal.setProcessed(false);
			newJournal.set_Value("C_Recurring_ID", getC_Recurring_ID());
			newJournal.saveEx(get_TrxName());

			// ── 7. 生成目标行（A/B/C车间）────────────────────────────────────
			// 目标行方向与 srcBalance 相同（正=借方，负=贷方）
			int lineNo = 10;
			for (MDistributionLine dl : activeLines) {
				MAccount targetAccount = dl.getAccount();
				if (targetAccount == null) {
					log.warning("GL分配明细行无法解析目标科目组合，跳过，GL_DistributionLine_ID="
							+ dl.getGL_DistributionLine_ID());
					continue;
				}
				// MAccount.get() 可能返回未持久化的新组合，需要确保已保存
				if (targetAccount.get_ID() == 0)
					targetAccount.saveEx(get_TrxName());

				MJournalLine targetLine = new MJournalLine(newJournal);
				targetLine.setLine(lineNo);
				targetLine.setC_ValidCombination_ID(targetAccount.get_ID());

				BigDecimal lineAmt = dl.getAmt();  // 符号与 amtPerRun 一致
				if (lineAmt.signum() >= 0) {
					targetLine.setAmtSourceDr(lineAmt);
					targetLine.setAmtSourceCr(Env.ZERO);
					targetLine.setAmtAcctDr(lineAmt);
					targetLine.setAmtAcctCr(Env.ZERO);
				} else {
					targetLine.setAmtSourceDr(Env.ZERO);
					targetLine.setAmtSourceCr(lineAmt.negate());
					targetLine.setAmtAcctDr(Env.ZERO);
					targetLine.setAmtAcctCr(lineAmt.negate());
				}
				targetLine.setIsGenerated(true);
				targetLine.setProcessed(false);
				targetLine.saveEx(get_TrxName());
				lineNo += 10;
			}

			// ── 8. 生成分摊行（长期分摊费，方向与原始行相反）──────────────────
			MJournalLine amortLine = new MJournalLine(newJournal);
			amortLine.setLine(lineNo);
			amortLine.setC_ValidCombination_ID(srcCombinationID);

			// 分摊行方向与 srcBalance 相反（取反）
			BigDecimal amortAmt = amtPerRun.negate();
			if (amortAmt.signum() >= 0) {
				amortLine.setAmtSourceDr(amortAmt);
				amortLine.setAmtSourceCr(Env.ZERO);
				amortLine.setAmtAcctDr(amortAmt);
				amortLine.setAmtAcctCr(Env.ZERO);
			} else {
				amortLine.setAmtSourceDr(Env.ZERO);
				amortLine.setAmtSourceCr(amortAmt.negate());
				amortLine.setAmtAcctDr(Env.ZERO);
				amortLine.setAmtAcctCr(amortAmt.negate());
			}
			amortLine.setIsGenerated(true);
			amortLine.setProcessed(false);
			amortLine.saveEx(get_TrxName());

			// ── 9. 记录执行结果 ────────────────────────────────────────────
			run.set_Value("GL_Journal_ID", newJournal.getGL_Journal_ID());
			msg += newJournal.getDocumentNo();
			setLastPO(newJournal);
		} else
			return "Invalid @RecurringType@ = " + getRecurringType();
		run.saveEx(get_TrxName());

		//
		setDateLastRun (run.getUpdated());
		setRunsRemaining (getRunsRemaining()-1);
		setDateNextRun();
		saveEx(get_TrxName());
		return msg;
	}	//	executeRun

	/**
	 *	Calculate & set remaining Runs
	 *	@return true if there are remaining runs left
	 */
	private boolean calculateRuns()
	{
		String sql = "SELECT COUNT(*) FROM C_Recurring_Run WHERE C_Recurring_ID=?";
		int current = DB.getSQLValue(get_TrxName(), sql, getC_Recurring_ID());
		int remaining = getRunsMax() - current;
		setRunsRemaining(remaining);
		saveEx();
		return remaining > 0;
	}	//	calculateRuns

	/**
	 *	Calculate and set next run date
	 */
	private void setDateNextRun()
	{
		if (getFrequency() < 1)
			setFrequency(1);
		int frequency = getFrequency();
		Calendar cal = Calendar.getInstance();
		cal.setTime(getDateNextRun());
		//
		if (getFrequencyType().equals(FREQUENCYTYPE_Daily))
			cal.add(Calendar.DAY_OF_YEAR, frequency);
		else if (getFrequencyType().equals(FREQUENCYTYPE_Weekly))
			cal.add(Calendar.WEEK_OF_YEAR, frequency);
		else if (getFrequencyType().equals(FREQUENCYTYPE_Monthly))
			cal.add(Calendar.MONTH, frequency);
		else if (getFrequencyType().equals(FREQUENCYTYPE_Quarterly))
			cal.add(Calendar.MONTH, 3*frequency);
		Timestamp next = new Timestamp (cal.getTimeInMillis());
		setDateNextRun(next);
	}	//	setDateNextRun

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Validate mandatory for RecurringType and corresponding field
		String rt = getRecurringType();
		if (rt == null)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "RecurringType"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_Order)
			&& getC_Order_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Order_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_Invoice)
			&& getC_Invoice_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Invoice_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_GLJournal)
			&& getGL_JournalBatch_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "GL_JournalBatch_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_Project)
			&& getC_Project_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Project_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_GLJournalAmortization)
				&& getGL_Journal_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "GL_Journal_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_GLJournalAmortization)
				&& getAccount_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "Account_ID"));
			return false;
		}
		return true;
	}	//	beforeSave

	/* The last PO generated by a recent execute Run */
	private volatile PO lastPO;

	/**
	 * @return last PO generated by recent {@link #executeRun()}
	 */
	public PO getLastPO() {
		return lastPO;
	}

	/**
	 * @param lastPO
	 */
	public void setLastPO(PO lastPO) {
		this.lastPO = lastPO;
	}

}	//	MRecurring
