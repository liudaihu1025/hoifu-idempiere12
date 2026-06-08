package com.hoifu.model.qc;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.engine.QualityEngine;

public class MQC_IQC extends X_QC_IQC implements DocAction, IQCDocument {
	private static final long serialVersionUID = 1L;

	/** Process Message */
	private String m_processMsg = null;
	/** Just Prepared Flag */
	private boolean m_justPrepared = false;

	public MQC_IQC(Properties ctx, int QC_IQC_ID, String trxName) {
		super(ctx, QC_IQC_ID, trxName);
	}

	public MQC_IQC(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord) {
			// 设置单据类型（草稿时 C_DocType_ID=0，Target 设为实际类型）
			if (getC_DocTypeTarget_ID() == 0) {
				int docTypeId = getDocTypeID(getCtx(), getAD_Client_ID(), getAD_Org_ID(), get_TrxName());
				if (docTypeId > 0) {
					setC_DocTypeTarget_ID(docTypeId);
				}
			}
			// 生成单据编号
			if (getDocumentNo() == null || getDocumentNo().isEmpty()) {
				String docNo = DB.getDocumentNo(getC_DocTypeTarget_ID(), get_TrxName(), false, this);
				setDocumentNo(docNo);
			}

			setC_DocType_ID(0); // 草稿阶段始终为 0
		}
		return true;
	}

	public static int getDocTypeID(Properties ctx, int AD_Client_ID, int AD_Org_ID, String trxName) {
		String sql = "SELECT C_DocType_ID FROM C_DocType "
				+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,?) AND DocBaseType='IQC' "
				+ "AND IsActive='Y' ORDER BY AD_Org_ID DESC, IsDefault DESC";
		return DB.getSQLValueEx(trxName, sql, AD_Client_ID, AD_Org_ID);
	}

	public List<MQC_IQCLine> getLines() {
		return new Query(getCtx(), MQC_IQCLine.Table_Name, "QC_IQC_ID=?", get_TrxName()).setParameters(getQC_IQC_ID())
				.list();
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
		setProcessing(false); // 清除处理中标志
		return true;
	}

	@Override
	public boolean invalidateIt() {
		setDocAction(DocAction.ACTION_Prepare);
		return true;
	}

	/**
	 * 验证阶段：检查明细行是否完整 返回 STATUS_InProgress 表示验证通过，STATUS_Invalid 表示失败
	 */
	@Override
	public String prepareIt() {
		m_processMsg = null;

		List<MQC_IQCLine> lines = getLines();
		if (lines.isEmpty()) {
			m_processMsg = "来料检验单无明细行，无法完成";
			return DocAction.STATUS_Invalid;
		}
//		for (MQC_IQCLine line : lines) {
//			if (line.getMeasuredValMin() == null || line.getMeasuredValMax() == null) {
//				m_processMsg = "检测项 " + line.getQC_Index_ID() + " 未录入实测值";
//				return DocAction.STATUS_Invalid;
//			}
//		}

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
	 * 完成阶段：执行检验结果计算、回写，设置 Processed
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

		// 将 C_DocType_ID 从 Target 复制过来（草稿→完成）
		if (getC_DocTypeTarget_ID() > 0)
			setC_DocType_ID(getC_DocTypeTarget_ID());

		// 隐式审批
		if (!isApproved())
			approveIt();

		// 核心业务：计算检验结果
		new QualityEngine().determineResult(this);

		// 记录检验时间和检验人
		setInspectDate(new Timestamp(System.currentTimeMillis()));
		String inspector = Env.getContext(getCtx(), "#AD_User_Name");
		if (inspector != null && !inspector.isEmpty())
			setInspector(inspector);

		// 回写到来源单据
//		WriteBackHandler.writeBackIQCResult(this);

		// 标记已处理
		setProcessed(true);
		setDocAction(DocAction.ACTION_Close);
		return DocAction.STATUS_Completed;
	}

	@Override
	public boolean reActivateIt() {
		setDocStatus(DocAction.STATUS_InProgress);
		setDocAction(DocAction.ACTION_Complete);
		setProcessed(false); // 重置 Processed
		setIsApproved(false); // 重置审批状态
		set_ValueOfColumn("CheckResult", null);
		set_ValueOfColumn("InspectDate", null);
		set_ValueOfColumn("Inspector", null);
		set_ValueOfColumn("CR_Rate", null);
		set_ValueOfColumn("MAJ_Rate", null);
		set_ValueOfColumn("MIN_Rate", null);
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
	} // 修复：返回实际消息

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

	@Override
	public BigDecimal getLotSize() {
		return getQuantityReceived();
	}

	// DocStatus / DocAction 的 getter/setter 直接继承自 X_QC_IQC，无需重写
}