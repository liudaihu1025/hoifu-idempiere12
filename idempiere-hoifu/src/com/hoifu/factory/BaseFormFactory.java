package com.hoifu.factory;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;

import com.hoifu.form.GenerateTreeBOM;
import com.hoifu.form.HFOneCodeSystemForm;
import com.hoifu.form.HFPricingSheetSystemForm;
import com.hoifu.form.UReportForm;
import com.hoifu.form.WAttachmentViewerForm;
import com.hoifu.form.WImpositionTool;
import com.hoifu.form.WPPOrderTrackingDetail;
import com.hoifu.form.WPackagingDiagramTool;
import com.hoifu.form.WViewBrowser;

public class BaseFormFactory implements IFormFactory {

	@Override
	public ADForm newFormInstance(String formName) {

		if ("com.hoifu.form.GenerateTreeBOM".equals(formName)) {
			IFormController controller = new GenerateTreeBOM();

			return controller.getForm();
		}
		if ("com.hoifu.form.WImpositionTool".equals(formName)) {
			return new WImpositionTool();
		}
		if ("com.hoifu.form.WPackagingDiagramTool".equals(formName)) {
			return new WPackagingDiagramTool();
		}
		// 附件查看器
		if ("com.hoifu.form.WAttachmentViewerForm".equals(formName)) {
			return new WAttachmentViewerForm();
		}
		if ("com.hoifu.form.WViewBrowser".equals(formName)) {
			return new WViewBrowser();
		}
		// 海富计价单系统
		if ("com.hoifu.form.HFPricingSheetSystemForm".equals(formName)) {
			return new HFPricingSheetSystemForm();
		}
		// 海富一物一码系统
		if ("com.hoifu.form.HFOneCodeSystemForm".equals(formName)) {
			return new HFOneCodeSystemForm();
		}
		// 打样追踪详情
		if ("com.hoifu.form.WPPOrderTrackingDetail".equals(formName)) {
			return new WPPOrderTrackingDetail();
		}
		// ureport2报表
		if ("com.hoifu.form.UReportForm".equals(formName)) {
			return new UReportForm();
		}
		return null;
	}
}