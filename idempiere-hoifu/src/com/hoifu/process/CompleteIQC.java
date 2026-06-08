// CompleteIQC.java
package com.hoifu.process;

import org.compiere.process.DocAction;
import org.compiere.process.SvrProcess;

import com.hoifu.model.qc.MQC_IQC;

public class CompleteIQC extends SvrProcess {
	private int p_QC_IQC_ID;

	protected void prepare() {
		p_QC_IQC_ID = getRecord_ID();
	}

	protected String doIt() throws Exception {
		MQC_IQC iqc = new MQC_IQC(getCtx(), p_QC_IQC_ID, get_TrxName());
		iqc.processIt(DocAction.ACTION_Complete);
		iqc.saveEx();
		return "完成";
	}
}