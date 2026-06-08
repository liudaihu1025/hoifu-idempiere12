package org.libero.process;

import java.util.logging.Level;

import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.wf.MWorkflow;
import org.libero.model.MPP_Engineering_Change_Notice;

@org.adempiere.base.annotation.Process
public class ECN_CompleteChange extends SvrProcess {

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

		if (!MPP_Engineering_Change_Notice.ECNSTATUS_InExecution.equals(ecn.getECNStatus())) {
			return "只有执行中状态的ECN才能提交完成";
		}

		ecn.setECNStatus(MPP_Engineering_Change_Notice.ECNSTATUS_Confirming);
		ecn.setDocStatus(DocAction.STATUS_InProgress);

		// 启动第二次工作流
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(ecn, DocAction.ACTION_Complete);
		if (pi.isError()) {
			ecn.setECNStatus(MPP_Engineering_Change_Notice.ECNSTATUS_Confirming);
			ecn.saveEx();
			return pi.getSummary();
		}


		ecn.saveEx();

		return "提交完成审批成功";
	}
}