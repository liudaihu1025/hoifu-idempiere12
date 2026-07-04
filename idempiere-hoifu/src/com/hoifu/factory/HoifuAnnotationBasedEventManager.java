package com.hoifu.factory;

import org.adempiere.base.AnnotationBasedEventManager;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = { AnnotationBasedEventManager.class })
public class HoifuAnnotationBasedEventManager extends AnnotationBasedEventManager {

	@Override
	public String[] getPackages() {
		return new String[] { "com.hoifu.delegate" };
	}
}