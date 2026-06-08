package com.hoifu.factory;

import org.adempiere.base.AnnotationBasedProcessFactory;
import org.adempiere.base.IProcessFactory;
import org.osgi.service.component.annotations.Component;

@Component(name = "com.hoifu.factory.BaseProcessFactory", immediate = true, service = IProcessFactory.class, property = {"service.ranking:Integer=10" })
public class BaseProcessFactory extends AnnotationBasedProcessFactory {

    @Override
    protected String[] getPackages() {
        return new String[] { "com.hoifu.process" };
    }
}