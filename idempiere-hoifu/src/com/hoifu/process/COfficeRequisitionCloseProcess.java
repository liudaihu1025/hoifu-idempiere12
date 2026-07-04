package com.hoifu.process;

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.hoifu.model.I_C_OfficeRequisition;
import com.hoifu.model.X_C_OfficeRequisition;
import com.hoifu.model.X_C_OfficeRequisitionLine;

@org.adempiere.base.annotation.Process
public class COfficeRequisitionCloseProcess extends SvrProcess {

	private static final CLogger log = CLogger.getCLogger(COfficeRequisitionCloseProcess.class);

	private int p_C_OfficeRequisition_ID;

	@Override
	protected void prepare() {
		p_C_OfficeRequisition_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		if (p_C_OfficeRequisition_ID <= 0)
			throw new AdempiereException("@FillMandatory@ @C_OfficeRequisition_ID@");

		X_C_OfficeRequisition requisition = new X_C_OfficeRequisition(getCtx(), p_C_OfficeRequisition_ID,
				get_TrxName());
		if (requisition.get_ID() <= 0)
			throw new AdempiereException("@NotFound@ @C_OfficeRequisition_ID@");
		
		if (I_C_OfficeRequisition.REQSTATUS_Closed.equals(requisition.getReqStatus())) {
			return "当前申领单状态是已完成！";
		}

		// 1. 更新所有明细为已领用 YL(已领用)
		String sql = "UPDATE C_OfficeRequisitionLine SET ClaimStatus='YL', Updated=getDate(), UpdatedBy=? "
				+ "WHERE C_OfficeRequisition_ID=? AND ClaimStatus<>'YL' AND IsActive='Y'";
		int updated = DB.executeUpdateEx(sql, new Object[] { getAD_User_ID(), p_C_OfficeRequisition_ID },
				get_TrxName());

		log.info("已更新 " + updated + " 条明细为已领用(YL)");

		// 2. 更新申领单状态为已完成
		requisition.setReqStatus(I_C_OfficeRequisition.REQSTATUS_Closed);
		requisition.saveEx();

		return "@Success@ - " + updated + " 条明细已更新为已领用，申领状态已更新为已完成";
	}
}