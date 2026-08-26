package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
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

/**
 * 贴现流程
 */
@org.adempiere.base.annotation.Process
public class BillDiscountProcess extends SvrProcess {

    // 参数定义
	private Timestamp p_BusinessDate = null;// 业务日期
	private int p_C_BPartner_ID = 0; // 出票人/背书人
	private int p_owner_bp_ID = 0;// 收款单位/被背书人
	private int p_settle_bp_ID = 0;// 结算单位
	private boolean p_IsRecourse = false; // 是否保留追索权
	private int p_C_Charge_ID1 = 0; // 费用项目1
	private BigDecimal p_DiscountRate = Env.ZERO;// 贴现率
	private BigDecimal p_DiscountFeeAmt = Env.ZERO;// 贴现息
	private int p_C_Charge_ID2 = 0; // //费用项目2
	private BigDecimal p_ChargeAmt = Env.ZERO; // 贴现费用
	private BigDecimal p_SettleAmt = Env.ZERO; // 结算金额
	private String p_DiscountBank = null;// 贴现银行
    private int p_C_BankAccount_ID = 0; // 银行账户ID
    private boolean p_Selection = false;
    private int processedCount = 0;

    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (para[i].getParameter() == null)
                ;
            else if (name.equals("BusinessDate"))
				p_BusinessDate = (Timestamp) para[i].getParameter();// 业务日期
            else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();// 出票人/背书人
			else if (name.equals("owner_bp_ID"))
				p_owner_bp_ID = para[i].getParameterAsInt();// 收款单位/被背书人
			else if (name.equals("settle_bp_ID"))
				p_settle_bp_ID = para[i].getParameterAsInt();// 结算单位
			else if (name.equals("IsRecourse"))
				p_IsRecourse = "Y".equals(para[i].getParameter());// 是否保留追索权
			else if (name.equals("C_Charge_ID1"))
				p_C_Charge_ID1 = para[i].getParameterAsInt();// 费用项目1
			else if (name.equals("DiscountRate"))
				p_DiscountRate = (BigDecimal) para[i].getParameter();// 贴现率
			else if (name.equals("DiscountFeeAmt"))
				p_DiscountFeeAmt = (BigDecimal) para[i].getParameter();// 贴现息
			else if (name.equals("C_Charge_ID2"))
				p_C_Charge_ID2 = para[i].getParameterAsInt();// 费用项目2
			else if (name.equals("ChargeAmt"))
				p_ChargeAmt = (BigDecimal) para[i].getParameter();// 贴现费用
			else if (name.equals("SettleAmt"))
				p_SettleAmt = (BigDecimal) para[i].getParameter();// 结算金额
			else if (name.equals("DiscountBank"))
				p_DiscountBank = (String) para[i].getParameter();// 贴现银行
			else if (name.equals("C_BankAccount_ID")) // 银行账户
                p_C_BankAccount_ID = para[i].getParameterAsInt();
            else
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
        }

        // 从信息窗口调用时自动设置选择标记
        if (getProcessInfo().getAD_InfoWindow_ID() > 0)
            p_Selection = true;

        // 基本参数校验
        validateParameters();
    }

    private void validateParameters() throws AdempiereUserError {
		// 业务日期
		if (p_BusinessDate == null) {
			throw new AdempiereUserError("请选择业务日期");
		}
		// 出票人/背书人必选
        if (p_C_BPartner_ID <= 0) {
			throw new AdempiereUserError("请选择出票人/背书人");
		}
		// 收款单位/被背书人
		if (p_owner_bp_ID <= 0) {
			throw new AdempiereUserError("请选择收款单位/被背书人");
		}
		// 结算单位
		if (p_settle_bp_ID <= 0) {
			throw new AdempiereUserError("请选择结算单位");
        }

		// 费用项目必选
		if (p_C_Charge_ID1 <= 0) {
			throw new AdempiereUserError("请选择费用项目");
        }

		// 结算金额必须大于0
		if (p_SettleAmt == null || p_SettleAmt.compareTo(Env.ZERO) <= 0) {
			throw new AdempiereUserError("结算金额必须大于0");
        }

		// 贴现息不能为负数
        if (p_DiscountFeeAmt == null || p_DiscountFeeAmt.compareTo(Env.ZERO) < 0) {
			throw new AdempiereUserError("贴现息不能为负数");
        }

		// 贴现率不能为负数（也可加上限校验，如 <=100）
		if (p_DiscountRate == null || p_DiscountRate.compareTo(Env.ZERO) < 0) {
			throw new AdempiereUserError("贴现率不能为负数");
		}

		// 贴现费用不能为负数
		if (p_ChargeAmt == null || p_ChargeAmt.compareTo(Env.ZERO) < 0) {
			throw new AdempiereUserError("贴现费用不能为负数");
		}

		// 银行账户必填
		if (p_C_BankAccount_ID <= 0) {
			throw new AdempiereUserError("请选择银行账户");
		}
    }

    protected String doIt() throws Exception {
        if (!p_Selection) {
            throw new AdempiereUserError("请从信息窗口选择票据记录");
        }

        // 获取选中的票据ID
        List<Integer> billIds = getSelectedBillIds();

        if (billIds.isEmpty()) {
            return "没有找到可处理的票据";
        }

        // 处理所有票据并进行业务校验
        processedCount = 0;
        for (Integer billId : billIds) {
            MBillPool billPool = new MBillPool(getCtx(), billId, get_TrxName());

            // 业务校验
            validateBillForDiscount(billPool);

            processBillDiscount(billPool);
            processedCount++;
        }

        return "成功处理 " + processedCount + " 张票据";
    }

    private void validateBillForDiscount(MBillPool billPool) throws AdempiereUserError {
        // 检查票据状态是否为"已签收"
        if (!"H".equals(billPool.getBusinessStatus())) {
            throw new AdempiereUserError("票据 " + billPool.getDocumentNo() + " 状态不是已签收，无法贴现");
        }

        // 检查票据是否为应收票据（只能贴现应收票据）
		if (!billPool.isReceipt()) {
			throw new AdempiereUserError("票据 " + billPool.getDocumentNo() + " 不是应收票据，无法贴现");
        }
    }

    private List<Integer> getSelectedBillIds() throws Exception {
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

        return billIds;
    }

    private void processBillDiscount(MBillPool billPool) throws Exception {
        // 更新票据状态
        billPool.setBusinessStatus("C"); // 已贴现
        billPool.setDocStatus("AP"); // 已审核
        billPool.setDeliveryDate(new Timestamp(System.currentTimeMillis()));
        billPool.saveEx();

        // 新增：同步贴现字段到票据池
		billPool.set_ValueNoCheck("settle_bp_ID", p_settle_bp_ID); // 结算单位

		billPool.set_ValueNoCheck("IsDiscounted", Boolean.TRUE); // 是否贴现
		billPool.set_ValueNoCheck("BillDiscountDate", p_BusinessDate);// 贴现日期 = 业务日期
		billPool.set_ValueNoCheck("C_BankAccount_ID", p_C_BankAccount_ID); // 收款银行账号 = 银行账户
		billPool.set_ValueNoCheck("DiscountBank", p_DiscountBank); // 贴现银行 = 参数9贴现银行



		MBPartner drawer = MBPartner.get(Env.getCtx(), p_C_BPartner_ID, get_TrxName());
		MBPartner payee = MBPartner.get(Env.getCtx(), p_owner_bp_ID, get_TrxName());

		// 获取背书类型
		String endorserType = billPool.getEndorserType();
		// 根据背书类型设置相关字段
		if ("N".equals(endorserType)) {
			// 背书类型为无：设置出票人和收票人
			billPool.setDrawer_Id(drawer.getName()); // 出票人名称
			billPool.setPayee_Id(payee.getName());// 收票人名称
		} else if ("T".equals(endorserType)) {
			// 背书类型为转让：设置背书人和被背书人
			billPool.setEndorser_Id(drawer.getName());// 背书人名称
			billPool.setEndorsee_Id(payee.getName());// 被背书人名称
		}

		billPool.set_ValueOfColumn("IsRecourse", p_IsRecourse ? "Y" : "N");// 是否追索
		billPool.set_ValueNoCheck("SettleDate", p_BusinessDate); // 结算日期
		billPool.setProcessed(true);

		billPool.set_ValueNoCheck("DiscountRate", p_DiscountRate);// 贴现率
		billPool.set_ValueNoCheck("DiscountFeeAmt", p_DiscountFeeAmt);// 贴现息
		// 结算费用=贴现息+费用金额（贴现费用）
		billPool.set_ValueNoCheck("SettleFeeAmt", p_DiscountFeeAmt.add(p_ChargeAmt));// 结算费用
		billPool.set_ValueNoCheck("SettleAmt", p_SettleAmt);// 结算金额

        billPool.saveEx();

        // 创建票据作业记录
        createBillTransaction(billPool);
        
        //同步签收作业单的可追索字段
        syncRecourseToAcceptTransaction(billPool);
    }
    
	// 同步更新该票据之前的签收作业单的 IsRecourse
    private void syncRecourseToAcceptTransaction(MBillPool billPool) throws Exception {  
        String sql = "SELECT C_Bill_Transaction_ID FROM C_Bill_Transaction "  
                + "WHERE C_Bill_Pool_ID=? AND TransactionType='A' "  
                + "ORDER BY C_Bill_Transaction_ID DESC";  
        int acceptTransId = DB.getSQLValue(get_TrxName(), sql, billPool.getC_Bill_Pool_ID());  
        if (acceptTransId > 0) {  
            MBillTransaction acceptTrans = new MBillTransaction(getCtx(), acceptTransId, get_TrxName());  
            acceptTrans.set_ValueOfColumn("IsRecourse", p_IsRecourse ? "Y" : "N");  
            acceptTrans.saveEx();  
        }  
    }

    private void createBillTransaction(MBillPool billPool) throws Exception {
        MBillTransaction transaction = new MBillTransaction(getCtx(), 0, get_TrxName());

        // 设置基本信息
        transaction.setAD_Org_ID(billPool.getAD_Org_ID());
        transaction.setAffairType("R"); // 事务类型：应收票据
        transaction.setTransactionType("C"); // 作业类型：贴现
        transaction.setBusinessDate(p_BusinessDate != null ? p_BusinessDate : new Timestamp(System.currentTimeMillis()));

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

        // 设置相关方信息（BP信息）
		transaction.setC_BPartner_ID(p_C_BPartner_ID);// 往来单位
		transaction.set_ValueNoCheck("owner_bp_ID", p_owner_bp_ID); // 收款单位
		transaction.set_ValueNoCheck("settle_bp_ID", p_settle_bp_ID); // 结算单位
		transaction.setC_Charge_ID(p_C_Charge_ID1);// 费用项目1
		transaction.set_ValueNoCheck("C_Charge2_ID", p_C_Charge_ID2); // 费用项目2
        transaction.setDrawer_Id(billPool.getDrawer_Id());
        transaction.setReceiver_Id(billPool.getPayee_Id());
        transaction.setAcceptor_Id(billPool.getAcceptor_Id());
        transaction.setEndorser_Id(billPool.getEndorser_Id());
        transaction.setEndorsee_Id(billPool.getEndorsee_Id());

        // 设置金额信息（支付信息）
        transaction.setC_Currency_ID(billPool.getC_Currency_ID());
		transaction.setSettleAmt(p_SettleAmt);// 结算金额
		transaction.setChargeAmt(p_ChargeAmt);// 费用金额

        BigDecimal maturityAmt = billPool.getMaturityAmt();
        BigDecimal billAmt = billPool.getBillAmt();
        BigDecimal interestAmt = (maturityAmt != null && billAmt != null) ? maturityAmt.subtract(billAmt) : Env.ZERO;
        transaction.setInterestAmt(interestAmt);

        // 设置贴现相关金额（保存到数据库）
		transaction.set_ValueOfColumn("DiscountFeeAmt", p_DiscountFeeAmt);// 贴现息
		transaction.set_ValueOfColumn("IsRecourse", p_IsRecourse ? "Y" : "N");

        // 设置银行账户ID
        transaction.setC_BankAccount_ID(p_C_BankAccount_ID);

        // 设置为已完成状态以触发过账（引用信息）
        transaction.setDocStatus(DocAction.STATUS_Completed);
        transaction.setDocAction(DocAction.STATUS_Closed);
        transaction.setProcessed(true);

        // 设置文档类型
        transaction.setC_DocType_ID(getDocTypeId("BTR", "应收票据作业单"));

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