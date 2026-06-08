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

public class MQC_RQC extends X_QC_RQC implements DocAction, IQCDocument {
	private static final long serialVersionUID = 1L;

	private String m_processMsg = null;
	private boolean m_justPrepared = false;

	public MQC_RQC(Properties ctx, int QC_RQC_ID, String trxName) {
		super(ctx, QC_RQC_ID, trxName);
	}

	public MQC_RQC(Properties ctx, ResultSet rs, String trxName) {
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
				+ "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,?) AND DocBaseType='RQC' "
				+ "AND IsActive='Y' ORDER BY AD_Org_ID DESC, IsDefault DESC";
		return DB.getSQLValueEx(trxName, sql, AD_Client_ID, AD_Org_ID);
	}

	public List<MQC_RQCLine> getLines() {
		return new Query(getCtx(), MQC_RQCLine.Table_Name, "QC_RQC_ID=?", get_TrxName()).setParameters(getQC_RQC_ID())
				.list();
	}

	// ========== DocAction 接口实现 ==========

	@Override
	public boolean processIt(String action) throws Exception {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(action, getDocAction());
	}

	@Override
	public boolean unlockIt() {
		setProcessing(false);
		return true;
	}

	@Override
	public boolean invalidateIt() {
		setDocAction(DocAction.ACTION_Prepare);
		return true;
	}

	@Override
	public String prepareIt() {
		m_processMsg = null;

		List<MQC_RQCLine> lines = getLines();
		if (lines.isEmpty()) {
			m_processMsg = "退料检验单无明细行，无法完成";
			return DocAction.STATUS_Invalid;
		}
//		for (MQC_RQCLine line : lines) {
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

	@Override
	public String completeIt() {
		if (!m_justPrepared) {
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		if (getC_DocTypeTarget_ID() > 0)
			setC_DocType_ID(getC_DocTypeTarget_ID());

		if (!isApproved())
			approveIt();

		new QualityEngine().determineResult(this);

		// 3. FAIL 时 Disposition 必填
		if ("FAIL".equals(getCheckResult())) {
			if (getDisposition() == null || getDisposition().isEmpty()) {
				m_processMsg = "检验结果为不合格，请选择处置方式后再完成";
				return DocAction.STATUS_Invalid;
			}
		}

		setInspectDate(new Timestamp(System.currentTimeMillis()));
		String inspector = Env.getContext(getCtx(), "#AD_User_Name");
		if (inspector != null && !inspector.isEmpty())
			setInspector(inspector);

//		WriteBackHandler.writeBackRQCResult(this);

		setProcessed(true);
		setDocAction(DocAction.ACTION_Close);
		return DocAction.STATUS_Completed;
	}

	@Override
	public boolean reActivateIt() {
		setDocStatus(DocAction.STATUS_InProgress);
		setDocAction(DocAction.ACTION_Complete);
		setProcessed(false);
		setIsApproved(false);
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

	@Override
	public BigDecimal getLotSize() {
		return getQuantityApply();
	}

	@Override
	public void onPass() {
		if (getDisposition() == null || getDisposition().isEmpty())
			setDisposition("ACCEPT");
	}
}