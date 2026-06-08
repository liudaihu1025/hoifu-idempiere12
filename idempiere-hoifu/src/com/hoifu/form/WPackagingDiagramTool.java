package com.hoifu.form;

import org.adempiere.webui.panel.ADForm;
import org.zkoss.zul.Iframe;

@org.idempiere.ui.zk.annotation.Form
public class WPackagingDiagramTool extends ADForm {

	@Override
	protected void initForm() {
		Iframe iframe = new Iframe();
		iframe.setSrc("/html/PackagingDiagram_v2.5.html");
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		this.appendChild(iframe);
	}
}