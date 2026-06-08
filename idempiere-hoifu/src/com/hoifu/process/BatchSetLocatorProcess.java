package com.hoifu.process;

import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class BatchSetLocatorProcess extends SvrProcess {

	private int p_M_Locator_ID = 0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_Locator_ID"))
				p_M_Locator_ID = para[i].getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_M_Locator_ID == 0)
			throw new AdempiereUserError("@FillMandatory@ @M_Locator_ID@");

		// 从 T_Selection 中过滤出订单行（ViewID='260' 即 C_OrderLine）
		// 前提：AD_InfoProcess.AD_InfoColumn_ID 需指向信息窗口中 ColumnName='AD_Table_ID' 的
		// AD_InfoColumn 记录
		int[] orderLineIds = DB.getIDsEx(get_TrxName(),
				"SELECT T_SELECTION_ID FROM T_Selection " + "WHERE AD_PInstance_ID = ? AND ViewID = '260'",
				getAD_PInstance_ID());

		if (log.isLoggable(Level.INFO))
			log.info("BatchSetLocator: pInstanceID=" + getAD_PInstance_ID() + ", orderLineCount=" + orderLineIds.length
					+ ", M_Locator_ID=" + p_M_Locator_ID);

		if (orderLineIds.length == 0)
			return "@NoRecordSelected@";

		int updated = 0;
		for (int orderLineId : orderLineIds) {
			int no = DB.executeUpdateEx(
					"UPDATE C_OrderLine SET M_PlannedLocator_ID=?, Updated=getDate(), UpdatedBy=?"
							+ " WHERE C_OrderLine_ID=?",
					new Object[] { p_M_Locator_ID, getAD_User_ID(), orderLineId }, get_TrxName());
			if (log.isLoggable(Level.FINE))
				log.fine("Updated C_OrderLine_ID=" + orderLineId + ", rows=" + no);
			updated += no;
		}

		return "@Updated@ #" + updated;
	}
}