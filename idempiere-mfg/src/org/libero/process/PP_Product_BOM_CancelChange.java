package org.libero.process;

import org.compiere.process.SvrProcess;
import org.eevolution.model.MPPProductBOM;

@org.adempiere.base.annotation.Process
public class PP_Product_BOM_CancelChange extends SvrProcess {

	@Override
	protected void prepare() {
		int recordId = getRecord_ID();
		if (recordId <= 0) {
			throw new IllegalArgumentException("No record ID");
		}
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();

		// 获取BOM对象
		MPPProductBOM bom = new MPPProductBOM(getCtx(), recordId, get_TrxName());
		if (bom.get_ID() == 0) {
			throw new IllegalArgumentException("BOM not found");
		}

		// 验证BOM状态
		if (!"InChange".equals(bom.getBOMStatus())) {
			return "只有变更中的BOM才能取消变更";
		}

		// 执行恢复操作
		return bom.restoreFromHistory();
	}
}