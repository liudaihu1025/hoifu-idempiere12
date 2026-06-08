package org.libero.process;

import java.util.logging.Level;

import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.libero.model.MPPOrder;
import org.libero.model.MPP_Engineering_Change_Notice;

@org.adempiere.base.annotation.Process
public class ECN_ExecuteChange extends SvrProcess {

	@Override
	protected void prepare() {
		// 标准准备逻辑 - 无参数处理
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		MPP_Engineering_Change_Notice ecn = new MPP_Engineering_Change_Notice(getCtx(), recordId, get_TrxName());

		if (!MPP_Engineering_Change_Notice.ECNSTATUS_Approved.equals(ecn.getECNStatus())) {
			return "只有已批准状态的ECN才能执行变更";
		}
		
		
		// 更新工单状态为ECN变更中
		MPPOrder order = new MPPOrder(getCtx(), ecn.getPP_Order_ID(), get_TrxName());
		
		String orderStatus = (String) order.get_Value("Orderstatus");
		if ("ChangeExecuted".equals(orderStatus)) {
		    return "执行变更失败，该工单已变更";
		}
		

		ecn.setECNStatus(MPP_Engineering_Change_Notice.ECNSTATUS_InExecution);
		ecn.setDocStatus(DocAction.STATUS_InProgress);
		ecn.saveEx();

		return "开始执行变更";
	}
}