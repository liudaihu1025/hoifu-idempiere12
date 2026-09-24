package com.hoifu.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.model.DocActionDelegate;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.process.DocAction;
import org.compiere.util.DB;

public class MLogistics extends X_M_Logistics implements DocAction {

	private static final long serialVersionUID = 1L;

	private DocActionDelegate<MLogistics> docActionDelegate;
	private String m_processMsg = null;

	public MLogistics(Properties ctx, int M_Logistics_ID, String trxName) {
		super(ctx, M_Logistics_ID, trxName);
		if (M_Logistics_ID == 0)
			setInitialDefaults();
		init();
	}

	public MLogistics(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		init();
	}

	private void setInitialDefaults() {
		setProcessed(false);
	}

	private void init() {
		docActionDelegate = new DocActionDelegate<>(this);
		docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		return true;
	}

	private String doComplete() {
		setLogisticsStatus("Shipped");
		markInOutsAsDelivered();
		return null;
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		calculateTotalPrice();
		return true;
	}

	// 私有辅助方法 -------------------------------------------------------------------------

	/** TotalPrice = FreightCharges + Surcharges */
	private void calculateTotalPrice() {
		BigDecimal freight = getFreightCharges();
		BigDecimal surcharge = getSurcharges();
		if (freight == null) freight = BigDecimal.ZERO;
		if (surcharge == null) surcharge = BigDecimal.ZERO;
		setTotalPrice(freight.add(surcharge));
	}

	/**
	 * 单据完成时，将此物流单明细关联的所有发货单标记为已发货（IsDelivered='Y'）。
	 */
	private void markInOutsAsDelivered() {
		int updated = DB.executeUpdateEx(
				"UPDATE M_InOut SET IsDelivered='Y' "
						+ "WHERE M_InOut_ID IN ("
						+ "  SELECT DISTINCT M_InOut_ID FROM M_LogisticsLine "
						+ "  WHERE M_Logistics_ID=? AND IsActive='Y'"
						+ ")",
				new Object[]{ getM_Logistics_ID() },
				get_TrxName());

		if (log.isLoggable(Level.INFO))
			log.info("IsDelivered updated: " + updated + " M_InOut(s)");
	}

	/**
	 * 撤销完成时的副作用：将关联发货单的 IsDelivered 恢复为 'N'。
	 */
	private void undoMarkInOutsAsDelivered() {
		int updated = DB.executeUpdateEx(
				"UPDATE M_InOut SET IsDelivered='N' "
						+ "WHERE M_InOut_ID IN ("
						+ "  SELECT DISTINCT M_InOut_ID FROM M_LogisticsLine "
						+ "  WHERE M_Logistics_ID=? AND IsActive='Y'"
						+ ")",
				new Object[]{ getM_Logistics_ID() },
				get_TrxName());

		if (log.isLoggable(Level.INFO))
			log.info("IsDelivered reverted: " + updated + " M_InOut(s)");
	}

	// ── DocAction 接口方法 ────────────────────────────

	@Override
	public boolean processIt(String action) throws Exception {
		return docActionDelegate.processIt(action);
	}

	@Override
	public boolean unlockIt() {
		return docActionDelegate.unlockIt();
	}

	@Override
	public boolean invalidateIt() {
		return docActionDelegate.invalidateIt();
	}

	@Override
	public String prepareIt() {
		return docActionDelegate.prepareIt();
	}

	@Override
	public boolean approveIt() {
		return docActionDelegate.approveIt();
	}

	@Override
	public boolean rejectIt() {
		return docActionDelegate.rejectIt();
	}

	@Override
	public String completeIt() {
		return docActionDelegate.completeIt();
	}

	/**
	 * 作废：已完成的物流单需要撤销发货单标记；未完成的走通用委托。
	 */
	@Override
	public boolean voidIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		// 已作废 → 不可重复作废
		if (DocAction.STATUS_Voided.equals(getDocStatus())) {
			m_processMsg = "Document already voided";
			return false;
		}

		// 已完成 → 撤销副作用后作废
		if (DocAction.STATUS_Completed.equals(getDocStatus())) {
			// Before Void
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);
			if (m_processMsg != null)
				return false;

			// 撤销 doComplete() 造成的副作用
			undoMarkInOutsAsDelivered();
			setLogisticsStatus(X_M_Logistics.LOGISTICSSTATUS_已创建);

			// After Void
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
			if (m_processMsg != null)
				return false;

			setDocStatus(DocAction.STATUS_Voided);
			setDocAction(DocAction.ACTION_None);
			setProcessed(true);

			return true;
		}

		// 未完成（草稿/处理中等）→ 走通用委托
		return docActionDelegate.voidIt();
	}

	@Override
	public boolean closeIt() {
		return docActionDelegate.closeIt();
	}

	@Override
	public boolean reverseCorrectIt() {
		return docActionDelegate.reverseCorrectIt();
	}

	@Override
	public boolean reverseAccrualIt() {
		return docActionDelegate.reverseAccrualIt();
	}

	/**
	 * 重新激活：将物流单从已完成退回处理中，恢复发货单标记。
	 */
	@Override
	public boolean reActivateIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());

		// 仅已完成状态可重新激活
		if (!DocAction.STATUS_Completed.equals(getDocStatus())) {
			m_processMsg = "Cannot reActivate document with status: " + getDocStatus();
			return false;
		}

		// Before ReActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;

		// 撤销完成时的副作用
		undoMarkInOutsAsDelivered();
		setLogisticsStatus(X_M_Logistics.LOGISTICSSTATUS_已创建);

		// After ReActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;

		setDocStatus(DocAction.STATUS_InProgress);
		setDocAction(DocAction.ACTION_Complete);
		setProcessed(false);

		return true;
	}

	// ── 其它委托方法 ────────────────────────────

	@Override
	public File createPDF() {
		return docActionDelegate.createPDF();
	}

	@Override
	public String getProcessMsg() {
		return m_processMsg;
	}

	@Override
	public int getC_Currency_ID() {
		return docActionDelegate.getC_Currency_ID();
	}

	@Override
	public String getDocAction() {
		return docActionDelegate.getDocAction();
	}

	@Override
	public void setDocStatus(String s) {
		docActionDelegate.setDocStatus(s);
	}

	@Override
	public String getDocStatus() {
		return docActionDelegate.getDocStatus();
	}

	@Override
	public String getSummary() {
		return getDocumentNo();
	}

	@Override
	public String getDocumentNo() {
		return get_ValueAsString(COLUMNNAME_DocumentNo);
	}

	@Override
	public String getDocumentInfo() {
		return getDocumentNo();
	}

	@Override
	public int getDoc_User_ID() {
		return getCreatedBy();
	}

	@Override
	public BigDecimal getApprovalAmt() {
		return null;
	}
}
