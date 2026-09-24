package com.hoifu.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrderLine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;

/**
 * 生产补数申请单 Model 类
 * 实现 DocAction 接口，支持标准审批工作流
 */
public class MPPOrderRepairRequest extends X_PP_Order_Repair_Request implements DocAction {

    private static final long serialVersionUID = 1L;

    private static final String DOC_PREFIX = "RMA";

    /** Process Message */
    private String m_processMsg = null;
    /** Just Prepared Flag */
    private boolean m_justPrepared = false;

    public MPPOrderRepairRequest(Properties ctx, int PP_Order_Repair_Request_ID, String trxName) {
        super(ctx, PP_Order_Repair_Request_ID, trxName);
    }

    public MPPOrderRepairRequest(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {
        if (newRecord) {
            // 优先使用用户在页签上已经选择的单据类型
            int docTypeId = get_ValueAsInt("C_DocType_ID");

			// 兜底：页签未选择或查不到时，按名称查询"补数申请单"
			if (docTypeId <= 0) {
				docTypeId = DB.getSQLValueEx(get_TrxName(),
						"SELECT C_DocType_ID FROM C_DocType " + "WHERE Name=? AND IsActive='Y' AND AD_Client_ID=?",
						"补数申请单", getAD_Client_ID());
            }

            set_ValueOfColumn("C_DocType_ID", docTypeId);

            if (getDocumentNo() == null || getDocumentNo().isEmpty()) {
                String docNo = DB.getDocumentNo(docTypeId, get_TrxName(), false, this);
                setDocumentNo(docNo);
            }
        }
        return true;
    }

    // ========== DocAction 接口实现 ==========

    /**
     * 委托给 DocumentEngine，由 Engine 按状态机调用 prepareIt / completeIt / voidIt 等方法
     */
    @Override
    public boolean processIt(String action) throws Exception {
        m_processMsg = null;
        DocumentEngine engine = new DocumentEngine(this, getDocStatus());
        return engine.processIt(action, getDocAction());
    }

    @Override
    public boolean unlockIt() {
		set_ValueOfColumn("Processing", false);
        return true;
    }

    @Override
    public boolean invalidateIt() {
        setDocAction(DocAction.ACTION_Prepare);
        return true;
    }

    /**
     * 验证阶段：检查必填字段是否完整
     */
    @Override
    public String prepareIt() {
        m_processMsg = null;

        // 校验：原工单号必填
        if (getPP_Order_ID() <= 0) {
            m_processMsg = "原工单号不能为空";
            return DocAction.STATUS_Invalid;
        }

        // 校验：补数方式必填
        String repairMethod = get_ValueAsString("repairmethod");
        if (repairMethod == null || repairMethod.isEmpty()) {
            m_processMsg = "补数方式不能为空";
            return DocAction.STATUS_Invalid;
        }

        // 校验：补数数量必填且大于0
		Object val = get_Value("RepairQty");
		BigDecimal repairQty = (val != null) ? (BigDecimal) val : BigDecimal.ZERO;

		if (repairQty.signum() <= 0) {
			m_processMsg = "补数数量必须大于0";
			return DocAction.STATUS_Invalid;
		}

        m_justPrepared = true;
        if (!DocAction.ACTION_Complete.equals(getDocAction()))
            setDocAction(DocAction.ACTION_Complete);
        return DocAction.STATUS_InProgress;
    }

    @Override
    public boolean approveIt() {
        setIsApproved(true);
        return true;
    }

    @Override
    public boolean rejectIt() {
        setIsApproved(false);
        return true;
    }

    /**
     * 完成阶段：标记已处理，设置文档状态为已完成
     */
    @Override
    public String completeIt() {
        // 若未经过 prepareIt，先执行一次
        if (!m_justPrepared) {
            String status = prepareIt();
            m_justPrepared = false;
            if (!DocAction.STATUS_InProgress.equals(status))
                return status;
        }

        // 隐式审批
        if (!isApproved())
            approveIt();

        // 记录完成时间
        set_ValueOfColumn("datecompleted", new Timestamp(System.currentTimeMillis()));

        // 回写补数信息到关联的销售订单明细
        syncToOrderLine();

        // 触发点②：申请单完成后，将原工单补数状态更新为"处理中"(IP)
        int originalOrderId = get_ValueAsInt("PP_Order_ID");
        if (originalOrderId > 0) {
            DB.executeUpdateEx(
                    "UPDATE PP_Order SET RepairStatus='IP', Updated=now(), UpdatedBy=? "
                            + "WHERE PP_Order_ID=?",
                    new Object[] { getUpdatedBy(), originalOrderId }, get_TrxName());
        }

        // 标记已处理
        setProcessed(true);
        setDocAction(DocAction.ACTION_None);
        return DocAction.STATUS_Completed;
    }

    /**
     * 回写补数信息到关联的销售订单明细
     * 当申请单完成后，把补数方式、补数数量同步到 C_OrderLine，
     * 并计算交货总数 = 订单数量 + 随单补数数量
     */
    private void syncToOrderLine() {

		String repairMethod = get_ValueAsString("repairmethod");

        // 只有"随销单补数"(SO)才需要回写订单明细，"工单补数"(WO)通过新建工单满足，不涉及订单明细
        if (!"SO".equals(repairMethod))
            return;

        int orderLineId = get_ValueAsInt("C_OrderLine_New_ID");

        if (orderLineId <= 0) {
            // 随销单补数必须有新销售单号
            log.warning("补单方式为随销单补数但新销售订单单号为空，无法回写订单明细");
            return;
        }

        MOrderLine orderLine = new MOrderLine(getCtx(), orderLineId, get_TrxName());
        if (orderLine.get_ID() <= 0)
            return;


		Object val = get_Value("RepairQty");
		BigDecimal repairQty = (val instanceof BigDecimal) ? (BigDecimal) val : BigDecimal.ZERO;

        orderLine.set_ValueOfColumn("RepairMethod", repairMethod);
        orderLine.set_ValueOfColumn("RepairQtyAlong", repairQty);

        BigDecimal qtyOrdered = orderLine.getQtyOrdered();
        if (qtyOrdered == null) qtyOrdered = BigDecimal.ZERO;
        if (repairQty == null) repairQty = BigDecimal.ZERO;
        orderLine.set_ValueOfColumn("QtyDeliverAllTotal", qtyOrdered.add(repairQty));

        orderLine.saveEx();
    }

    @Override
    protected boolean beforeDelete() {
        // 已完成的申请单不允许删除
        String docStatus = get_ValueAsString("DocStatus");
        if ("CO".equals(docStatus)) {
            throw new AdempiereException("已完成的补数申请单不能删除");
        }

        // 回滚关联工单的 RepairStatus：IP → DP
        int orderId = get_ValueAsInt("PP_Order_ID");
        if (orderId > 0) {
            DB.executeUpdateEx(
                    "UPDATE PP_Order SET RepairStatus='DP', Updated=now(), UpdatedBy=? "
                            + "WHERE PP_Order_ID=? AND RepairStatus='IP'",
                    new Object[] { getUpdatedBy(), orderId },
                    get_TrxName());
        }

        return true;
    }

    @Override
    public boolean reActivateIt() {
        setDocStatus(DocAction.STATUS_InProgress);
        setDocAction(DocAction.ACTION_Complete);
        setProcessed(false);
        setIsApproved(false);
        set_ValueOfColumn("datecompleted", null);
        saveEx();
        return true;
    }

    @Override
    public boolean voidIt() {
        setDocStatus(DocAction.STATUS_Voided);
        setDocAction(DocAction.ACTION_None);
        setProcessed(true);
        return true;
    }

    @Override
    public boolean closeIt() {
        setDocStatus(DocAction.STATUS_Closed);
        setDocAction(DocAction.ACTION_None);
        setProcessed(true);
        return true;
    }

    @Override
    public boolean reverseCorrectIt() {
        return false;
    }

    @Override
    public boolean reverseAccrualIt() {
        return false;
    }

    @Override
    public File createPDF() {
        return null;
    }

    @Override
    public String getSummary() {
        return getDocumentNo();
    }

    @Override
    public String getDocumentInfo() {
        return getDocumentNo();
    }

    @Override
    public String getProcessMsg() {
        return m_processMsg;
    }

    @Override
    public int getDoc_User_ID() {
        return getUpdatedBy();
    }

    @Override
    public BigDecimal getApprovalAmt() {
        return BigDecimal.ZERO;
    }

    @Override
    public int getC_Currency_ID() {
        return 0;
    }

    // ========== 未使用的辅助方法（保留旧的单据号生成逻辑备用） ==========

    /**
     * 生成规则：RMA + YYMMDD + 三位流水（每天从 001 开始）
     * 已改用 DB.getDocumentNo()，此方法保留备用
     */
    private String generateDocumentNo() {
        String dateStr = new java.text.SimpleDateFormat("yyMMdd").format(new java.util.Date());
        String prefix = DOC_PREFIX + dateStr;

        String sql = "SELECT DocumentNo FROM PP_Order_Repair_Request " + "WHERE DocumentNo LIKE ? "
                + "AND AD_Client_ID=? " + "ORDER BY DocumentNo DESC " + "LIMIT 1 FOR UPDATE";

        String lastDocNo = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            pstmt.setString(1, prefix + "%");
            pstmt.setInt(2, getAD_Client_ID());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                lastDocNo = rs.getString(1);
            }
        } catch (java.sql.SQLException e) {
            throw new AdempiereException("生成补数单号失败: " + e.getMessage(), e);
        } finally {
            DB.close(rs, pstmt);
        }

        int nextSeq = 1;
        if (lastDocNo != null && lastDocNo.length() >= prefix.length() + 3) {
            String seqPart = lastDocNo.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }

        return prefix + String.format("%03d", nextSeq);
    }
}
