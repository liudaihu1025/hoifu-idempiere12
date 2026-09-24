package com.hoifu.form;

import org.adempiere.webui.panel.ADForm;
import org.zkoss.zul.Iframe;

import com.hoifu.enums.HFSysConfigEnum;

@org.idempiere.ui.zk.annotation.Form
public class UReportForm extends ADForm {

	private static final long serialVersionUID = 1L;

	@Override
	protected void initForm() {
		String previewUrl = HFSysConfigEnum.HF_UREPORT_URL.getValue();

		Iframe iframe = new Iframe();
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		iframe.setSrc(previewUrl);
		this.appendChild(iframe);
	}
}