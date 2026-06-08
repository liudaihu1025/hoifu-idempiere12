package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
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
@org.adempiere.base.annotation.Process
public class BillDiscountProcess extends SvrProcess {

    // 参数定义
    private Timestamp p_BusinessDate = null;
    private boolean p_IsRecourse = true; // 是否保留追索权
    private int p_C_BPartner_ID = 0; // 放款业务伙伴
    private BigDecimal p_DiscountNetAmt = Env.ZERO; // 贴现净额
    private BigDecimal p_DiscountFeeAmt = Env.ZERO; // 贴现费用
    private int p_C_Charge_ID = 0; // 费用
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
                p_BusinessDate = (Timestamp) para[i].getParameter();
            else if (name.equals("IsRecourse"))
                p_IsRecourse = "Y".equals(para[i].getParameter());
            else if (name.equals("C_BPartner_ID"))
                p_C_BPartner_ID = para[i].getParameterAsInt();
            else if (name.equals("DiscountNetAmt"))
                p_DiscountNetAmt = (BigDecimal) para[i].getParameter();
            else if (name.equals("DiscountFeeAmt"))
                p_DiscountFeeAmt = (BigDecimal) para[i].getParameter();
            else if (name.equals("C_Charge_ID"))
                p_C_Charge_ID = para[i].getParameterAsInt();
            else if (name.equals("C_BankAccount_ID")) // 【新增】银行账户参数处理
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
        // 业务伙伴必选
        if (p_C_BPartner_ID <= 0) {
            throw new AdempiereUserError("请选择业务伙伴");
        }

        // 杂费必选
        if (p_C_Charge_ID <= 0) {
            throw new AdempiereUserError("请选择费用");
        }

        // 贴现净额必须大于0
        if (p_DiscountNetAmt == null || p_DiscountNetAmt.compareTo(Env.ZERO) <= 0) {
            throw new AdempiereUserError("贴现净额必须大于0");
        }

        // 贴现费用不能为负数
        if (p_DiscountFeeAmt == null || p_DiscountFeeAmt.compareTo(Env.ZERO) < 0) {
            throw new AdempiereUserError("贴现费用不能为负数");
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

        // 校验贴现费用计算：贴现费用 = 票据金额 - 贴现净额
        BigDecimal billAmt = billPool.getBillAmt();
        BigDecimal expectedFee = billAmt.subtract(p_DiscountNetAmt);

        if (expectedFee.compareTo(p_DiscountFeeAmt) != 0) {
            throw new AdempiereUserError("票据 " + billPool.getDocumentNo() +
                    " 贴现费用计算错误：票据金额=" + billAmt +
                    "，贴现净额=" + p_DiscountNetAmt +
                    "，预期费用=" + expectedFee +
                    "，实际费用=" + p_DiscountFeeAmt);
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
		billPool.set_ValueOfColumn("DiscountNetAmt", p_DiscountNetAmt);
		billPool.set_ValueOfColumn("DiscountFeeAmt", p_DiscountFeeAmt);

        billPool.saveEx();

        // 创建票据作业记录
        createBillTransaction(billPool);
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
        transaction.setC_BPartner_ID(p_C_BPartner_ID);
        transaction.setC_Charge_ID(p_C_Charge_ID);
        transaction.setDrawer_Id(billPool.getDrawer_Id());
        transaction.setReceiver_Id(billPool.getPayee_Id());
        transaction.setAcceptor_Id(billPool.getAcceptor_Id());
        transaction.setEndorser_Id(billPool.getEndorser_Id());
        transaction.setEndorsee_Id(billPool.getEndorsee_Id());

        // 设置金额信息（支付信息）
        transaction.setC_Currency_ID(billPool.getC_Currency_ID());
        transaction.setSettleAmt(billPool.getBillAmt());
        transaction.setChargeAmt(p_C_Charge_ID > 0 ? billPool.getBillAmt().multiply(new BigDecimal("0.01")) : Env.ZERO);

        BigDecimal maturityAmt = billPool.getMaturityAmt();
        BigDecimal billAmt = billPool.getBillAmt();
        BigDecimal interestAmt = (maturityAmt != null && billAmt != null) ? maturityAmt.subtract(billAmt) : Env.ZERO;
        transaction.setInterestAmt(interestAmt);

        // 设置贴现相关金额（保存到数据库）
		transaction.set_ValueOfColumn("DiscountNetAmt", p_DiscountNetAmt);
		transaction.set_ValueOfColumn("DiscountFeeAmt", p_DiscountFeeAmt);
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