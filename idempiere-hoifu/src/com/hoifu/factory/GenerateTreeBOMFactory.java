package com.hoifu.factory;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;

import com.hoifu.form.GenerateTreeBOM;
import com.hoifu.form.WAttachmentViewerForm;
import com.hoifu.form.WImpositionTool;
import com.hoifu.form.WPackagingDiagramTool;
import com.hoifu.form.WViewBrowser;

public class GenerateTreeBOMFactory implements IFormFactory {

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
		// 新增：附件查看器
		if ("com.hoifu.form.WAttachmentViewerForm".equals(formName)) {
			return new WAttachmentViewerForm();
		}
		if ("com.hoifu.form.WViewBrowser".equals(formName)) {
			return new WViewBrowser();
		}
		return null;
	}
}