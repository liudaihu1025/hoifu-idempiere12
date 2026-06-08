package org.libero.process;

import org.compiere.model.MProduct;
import org.compiere.process.DocAction;
import org.compiere.process.SvrProcess;
import org.eevolution.model.MPPProductBOM;

@org.adempiere.base.annotation.Process
public class PP_Product_BOM_RequestChange extends SvrProcess {

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


		// 验证BOM状态 - 支持已发布和已完成状态
		if (!"Released".equals(bom.getBOMStatus()) && !DocAction.STATUS_Completed.equals(bom.getDocStatus())) {
			return "只有已发布或已完成的BOM才能申请变更";
		}

		// 直接强制解锁到草稿状态
		String result = forceUnlockToDraft(bom);
		if (!result.contains("成功")) {
			return "解锁失败: " + result;
		}
		// 取消产品的BOM验证状态
		unsetProductVerification(bom.getM_Product_ID());

		// 更新BOM状态为"变更中"
		bom.setBOMStatus("InChange");
		bom.saveEx();

		return "变更申请成功";
	}

	/**
	 * 强制解锁已完成文档到草稿状态
	 */
	private String forceUnlockToDraft(MPPProductBOM bom) throws Exception {
		// 强制设置状态为草稿
		bom.setDocStatus(DocAction.STATUS_Drafted);
		bom.setProcessed(false);
		bom.setDocAction(DocAction.ACTION_Complete);
		bom.saveEx();

		return "强制解锁成功，状态: " + bom.getDocStatus();
	}

	/**
	 * 取消产品的BOM验证状态
	 */
	private void unsetProductVerification(int productId) {
		try {
			MProduct product = new MProduct(getCtx(), productId, get_TrxName());
			if (product.get_ID() > 0 && product.isVerified()) {
				product.setIsVerified(false);
				product.saveEx();
				addLog(0, null, null, "已取消产品BOM验证状态: " + product.getName());
			}
		} catch (Exception e) {
			addLog(0, null, null, "取消验证状态失败: " + e.getMessage());
		}
	}
}