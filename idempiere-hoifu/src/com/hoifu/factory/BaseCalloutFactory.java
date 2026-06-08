package com.hoifu.factory;

import org.adempiere.base.AnnotationBasedColumnCalloutFactory;
import org.osgi.service.component.annotations.Component;

@Component(name = "com.hoifu.factory.BaseCalloutFactory", immediate = true, service = org.adempiere.base.IColumnCalloutFactory.class)
public class BaseCalloutFactory extends AnnotationBasedColumnCalloutFactory {

    @Override
    protected String[] getPackages() {
        return new String[] { "com.hoifu.callout" };
    }
}