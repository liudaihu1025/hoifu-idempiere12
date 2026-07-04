package com.hoifu.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.po.AfterChange;
import org.adempiere.base.event.annotations.po.AfterNew;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

import com.hoifu.model.I_C_OfficeRequisition;
import com.hoifu.model.X_C_OfficeRequisition;
import com.hoifu.model.X_C_OfficeRequisitionLine;

@EventTopicDelegate
@ModelEventTopic(modelClass = X_C_OfficeRequisitionLine.class)
public class OfficeRequisitionLineDelegate extends ModelEventDelegate<X_C_OfficeRequisitionLine> {

	private static final CLogger log = CLogger.getCLogger(OfficeRequisitionLineDelegate.class);

	public OfficeRequisitionLineDelegate(X_C_OfficeRequisitionLine po, Event event) {
		super(po, event);
	}

	@AfterNew
	@AfterChange
	public void onAfterSave() {
		X_C_OfficeRequisitionLine line = getModel();
		String claimStatus = (String) line.getClaimStatus();

		if (!"YL".equals(claimStatus))
			return;

		int requisitionId = line.getC_OfficeRequisition_ID();
		if (requisitionId <= 0)
			return;

		checkAndUpdateReqStatus(requisitionId, line.get_TrxName());
	}

	private void checkAndUpdateReqStatus(int requisitionId, String trxName) {
		String sql = "SELECT COUNT(*) FROM C_OfficeRequisitionLine "
				+ "WHERE C_OfficeRequisition_ID = ? AND ClaimStatus != 'YL'";
		int nonClaimedCount = DB.getSQLValue(trxName, sql, requisitionId);

		log.info("C_OfficeRequisition_ID=" + requisitionId + ", non-claimed lines=" + nonClaimedCount);

		if (nonClaimedCount != 0)
			return;

		X_C_OfficeRequisition requisition = new X_C_OfficeRequisition(Env.getCtx(), requisitionId, trxName);

		String currentStatus = (String) requisition.getReqStatus();
		if (I_C_OfficeRequisition.REQSTATUS_Closed.equals(currentStatus))
			return; // 已经是已完成，跳过

		requisition.setReqStatus(I_C_OfficeRequisition.REQSTATUS_Closed);
		requisition.saveEx();
		log.info("C_OfficeRequisition " + requisition.get_ValueAsString("DocumentNo") + " 已自动更新为已完成");
	}
}