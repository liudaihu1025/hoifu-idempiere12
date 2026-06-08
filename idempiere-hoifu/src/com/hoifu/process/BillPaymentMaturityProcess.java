package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPayment;
import org.compiere.model.MProcessPara;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.MBillPool;
import com.hoifu.model.MBillTransaction;
import com.hoifu.model.MPaymentLine;

@org.adempiere.base.annotation.Process
public class BillPaymentMaturityProcess extends SvrProcess {
	// 参数
	private int p_C_BPartner_ID = 0;
	private int p_C_Charge_ID = 0;
	private boolean p_IsDishonored = false;
	private BigDecimal p_AccruedInterestAmt = null;
	private boolean p_IsGeneratePayment = false;
	private int p_C_BankAccount_ID = 0;
	private Timestamp p_BusinessDate = null;

	// 选择标记
	private boolean p_Selection = false;

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Charge_ID"))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if (name.equals("IsDishonored"))
				p_IsDishonored = "Y".equals(para[i].getParameter());
			else if (name.equals("C_Charge_ID"))
				p_AccruedInterestAmt = (BigDecimal) para[i].getParameter();
			else if (name.equals("IsGeneratePayment"))
				p_IsGeneratePayment = "Y".equals(para[i].getParameter());
			else if (name.equals("C_BankAccount_ID"))
				p_C_BankAccount_ID = para[i].getParameterAsInt();
			else if (name.equals("BusinessDate"))
				p_BusinessDate = (Timestamp) para[i].getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}

		// 从信息窗口调用时自动设置选择标记
		if (getProcessInfo().getAD_InfoWindow_ID() > 0)
			p_Selection = true;
	}

	protected String doIt() throws Exception {
		if (!p_Selection) {
			throw new AdempiereUserError("请从信息窗口选择票据记录");
		}

		// 直接获取所有选中的票据ID
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?";
		List<Integer> billIds = new ArrayList<>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				billIds.add(rs.getInt(1));
			}
		} finally {
			DB.close(rs, pstmt);
		}

		if (billIds.isEmpty()) {
			return "没有找到可处理的票据";
		}

		// 处理所有有效的票据
		int processedCount = 0;

		for (Integer billId : billIds) {
			MBillPool billPool = new MBillPool(getCtx(), billId, get_TrxName());
			processBillReceipt(billPool);
			processedCount++;
		}

		StringBuilder resultMsg = new StringBuilder("成功处理 ").append(processedCount).append(" 张票据，生成的单据：");

		return resultMsg.toString();
	}

	private void processBillReceipt(MBillPool billPool) throws Exception {
		// 设置状态
		if (p_IsDishonored) {
			billPool.setBusinessStatus("M"); // 我方拒付
		} else {
			billPool.setBusinessStatus("L"); // 已结清
		}
		billPool.setDocStatus("AP"); // 已审核
		billPool.setDeliveryDate(new Timestamp(System.currentTimeMillis()));

		// 保存
		billPool.saveEx();

		// 创建票据作业记录
		createBillTransaction(billPool);

		// 如果需要生成付款单
		if (p_IsGeneratePayment) {
			createReceipt(billPool);
		}
	}

	private void createBillTransaction(MBillPool billPool) throws Exception {
		MBillTransaction transaction = new MBillTransaction(getCtx(), 0, get_TrxName());

		// 设置基本信息
		transaction.setAD_Org_ID(billPool.getAD_Org_ID());
		transaction.setTransactionType("P"); // 付款
		transaction.setAffairType("P"); // 应付票据

		transaction.setBusinessDate(new Timestamp(System.currentTimeMillis()));

		// 使用BusinessDate参数设置日期字段
		if (p_BusinessDate != null) {
			transaction.setDateTrx(p_BusinessDate);
		}

		// 设置票据相关信息
		transaction.setC_Bill_Pool_DocumentNo(billPool.getDocumentNo());
		transaction.setC_Bill_Pool_ID(billPool.getC_Bill_Pool_ID());
		transaction.setBillType(billPool.getBillType());
		transaction.setBillAmt(billPool.getBillAmt());
		transaction.setSubPackageAmt(billPool.getSubPackageAmt());
		transaction.setBillPackageNo(billPool.getBillPackageNo());
		transaction.setMaturityDate(billPool.getMaturityDate());
		transaction.setBillRate(billPool.getBillRate());
		transaction.setBusinessStatus(billPool.getBusinessStatus());

		// 设置相关方信息
		transaction.setC_BPartner_ID(p_C_BPartner_ID);
		transaction.setC_Charge_ID(p_C_Charge_ID);
		transaction.setDrawer_Id(billPool.getDrawer_Id());
		transaction.setReceiver_Id(billPool.getPayee_Id());
		transaction.setAcceptor_Id(billPool.getAcceptor_Id());
		transaction.setEndorser_Id(billPool.getEndorser_Id());
		transaction.setEndorsee_Id(billPool.getEndorsee_Id());

		// 设置金额信息
		transaction.setC_Currency_ID(billPool.getC_Currency_ID());
		transaction.setSettleAmt(billPool.getBillAmt());
		transaction.setChargeAmt(p_C_Charge_ID > 0 ? billPool.getBillAmt().multiply(new BigDecimal("0.01")) : Env.ZERO);

		BigDecimal maturityAmt = billPool.getMaturityAmt();
		BigDecimal billAmt = billPool.getBillAmt();
		BigDecimal interestAmt = (maturityAmt != null && billAmt != null) ? maturityAmt.subtract(billAmt) : Env.ZERO;
		transaction.setInterestAmt(interestAmt);

		// 设置为已完成状态以触发过账
		transaction.setDocStatus(DocAction.STATUS_Completed);
		transaction.setDocAction(DocAction.STATUS_Closed);
		transaction.setProcessed(true);

		// 设置文档类型
		transaction.setC_DocType_ID(getDocTypeId("BTP", "应付票据作业单"));

		transaction.saveEx();

		addBufferLog(transaction.get_ID(), transaction.getBusinessDate(), transaction.getBillAmt(),
				"票据作业单: " + transaction.getDocumentNo(), MBillTransaction.Table_ID, transaction.get_ID());

		// 添加自动过账
		if (!transaction.isPosted()) {
			String error = DocumentEngine.postImmediate(Env.getCtx(), transaction.getAD_Client_ID(),
					MBillTransaction.Table_ID, transaction.get_ID(), false, get_TrxName());
			if (error != null) {
				throw new AdempiereException("票据作业单过账失败: " + error);
			}
		}
	}

	private void createReceipt(MBillPool billPool) throws Exception {
		MPayment payment = new MPayment(getCtx(), 0, get_TrxName());

		// 设置为付款单
		payment.setIsReceipt(false);
		payment.setC_DocType_ID(false); // 付款单类型

		// 设置基本信息
		payment.setAD_Org_ID(billPool.getAD_Org_ID());
		payment.setDateTrx(new Timestamp(System.currentTimeMillis()));
		payment.setDateAcct(new Timestamp(System.currentTimeMillis()));
		payment.setC_BPartner_ID(p_C_BPartner_ID);
		payment.setTenderType("M"); // 支付方式
		payment.setC_Charge_ID(p_C_Charge_ID); // 费用

		// 设置金额
		payment.setC_Currency_ID(billPool.getC_Currency_ID());

		// 设置银行账户
		payment.setC_BankAccount_ID(p_C_BankAccount_ID);

		// 设置状态为草稿
		payment.setDocStatus(DocAction.STATUS_Drafted);
		payment.setDocAction(DocAction.ACTION_Complete);

		// 设置付款事项
		payment.setR_Info("票据到期付款 - " + billPool.getDocumentNo());

		payment.saveEx();

		// 创建付款明细
		createPaymentLine(payment, billPool);

		addBufferLog(payment.getC_Payment_ID(), payment.getDateTrx(), payment.getPayAmt(),
				"付款单: " + payment.getDocumentNo(), MPayment.Table_ID, payment.getC_Payment_ID());

	}

	/**
	 * 创建收款明细
	 */
	private void createPaymentLine(MPayment payment, MBillPool billPool) throws Exception {
		// 创建支付明细行
		MPaymentLine paymentLine = new MPaymentLine(getCtx(), 0, get_TrxName());

		// 设置基本信息
		paymentLine.setAD_Org_ID(payment.getAD_Org_ID());
		paymentLine.setC_Payment_ID(payment.getC_Payment_ID());
		paymentLine.setLineNo(10); // 行号

		// 设置支付方式
		paymentLine.setTenderType(payment.getTenderType());

		// 设置金额信息
		paymentLine.setC_Currency_ID(payment.getC_Currency_ID());
		paymentLine.setEndorsementAmt(billPool.getBillAmt());

		if (billPool.getSubPackageAmt().compareTo(BigDecimal.ZERO) > 0) {
			paymentLine.setAmount(billPool.getSubPackageAmt());
		} else {
			paymentLine.setAmount(billPool.getBillAmt());
		}

		// 设置票据相关信息
		paymentLine.setBillAmt(billPool.getBillAmt());
		paymentLine.setSubPackageAmt(billPool.getSubPackageAmt());
		paymentLine.setBillType(billPool.getBillType());
		paymentLine.setBillPackageNo(billPool.getBillPackageNo());
		paymentLine.setMaturityDate(billPool.getMaturityDate());
		paymentLine.setBillRate(billPool.getBillRate());
		paymentLine.setBusinessStatus(billPool.getBusinessStatus());
		paymentLine.setC_Bill_Pool_ID(billPool.getC_Bill_Pool_ID());
		paymentLine.setEndorsee_Id(billPool.getEndorsee_Id());
		paymentLine.setEndorser_Id(billPool.getEndorser_Id());

		paymentLine.saveEx();
	}

	/**
	 * 获取文档类型ID
	 */
	private int getDocTypeId(String docBaseType, String name) {
		String sql = "SELECT C_DocType_ID FROM C_DocType "
				+ "WHERE AD_Client_ID=? AND DocBaseType=? AND Name=? AND IsActive='Y'";
		int docTypeId = DB.getSQLValue(get_TrxName(), sql, Env.getAD_Client_ID(getCtx()), docBaseType, name);

		if (docTypeId <= 0) {
			throw new AdempiereException("未找到文档类型: " + name + " (DocBaseType=" + docBaseType + ")");
		}

		return docTypeId;
	}
}
