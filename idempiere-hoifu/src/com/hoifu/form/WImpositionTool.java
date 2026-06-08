package com.hoifu.form;

import org.adempiere.webui.panel.ADForm;
import org.zkoss.zul.Iframe;

@org.idempiere.ui.zk.annotation.Form
public class WImpositionTool extends ADForm {

	@Override
	protected void initForm() {
		Iframe iframe = new Iframe();
		iframe.setSrc("/html/imposition.html");
//		iframe.setSrc("~./imposition.html");
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		this.appendChild(iframe);
	}
}