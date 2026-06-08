package com.hoifu.factory;

import org.adempiere.base.AnnotationBasedModelFactory;
import org.adempiere.base.IModelFactory;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IModelFactory.class, property = { "service.ranking:Integer=0" })
public class BaseModelFactory extends AnnotationBasedModelFactory {

	@Override
	protected String[] getPackages() {
		// 扫描所有实体类所在的包，可以列多个
		return new String[] { "com.hoifu.model", "com.hoifu.model.qc" };
	}
}