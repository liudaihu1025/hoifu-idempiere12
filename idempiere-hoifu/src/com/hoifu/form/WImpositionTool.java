package com.hoifu.form;

import org.adempiere.webui.panel.ADForm;
import org.zkoss.zul.Iframe;

@org.idempiere.ui.zk.annotation.Form
public class WImpositionTool extends ADForm {

	private static final long serialVersionUID = 1670669565228047657L;

	@Override
	protected void initForm() {
		Iframe iframe = new Iframe();
		iframe.setSrc("/html/imposition.html");
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		this.appendChild(iframe);
	}
}