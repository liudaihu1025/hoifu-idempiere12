package com.hoifu.form;

import org.adempiere.webui.panel.ADForm;
import org.zkoss.zul.Iframe;

@org.idempiere.ui.zk.annotation.Form
public class HFPricingSheetSystemForm extends ADForm {

	private static final long serialVersionUID = -4403654494391825424L;

	@Override
	protected void initForm() {
		Iframe iframe = new Iframe();
		iframe.setSrc("https://app.hoifu.com.cn:8010/login.html");
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		this.appendChild(iframe);
	}
}