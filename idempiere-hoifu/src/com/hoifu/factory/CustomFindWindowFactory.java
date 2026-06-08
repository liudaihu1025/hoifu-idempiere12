package com.hoifu.factory;

import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.factory.IFindWindowFactory;
import org.adempiere.webui.window.FindWindow;
import org.compiere.model.GridField;
import org.osgi.service.component.annotations.Component;

import com.hoifu.window.CustomFindWindow;


@Component(service = IFindWindowFactory.class, property = {"service.ranking:Integer=100"})  
public class CustomFindWindowFactory implements IFindWindowFactory {  
    
    @Override  
    public FindWindow getInstance(int targetWindowNo, int targetTabNo, String title,  
            int AD_Table_ID, String tableName, String whereExtended,  
            GridField[] findFields, int minRecords, int adTabId,   
            AbstractADWindowContent windowPanel) {  
          
        // 返回自定义的FindWindow实例  
        return new CustomFindWindow(targetWindowNo, targetTabNo, title,  
                AD_Table_ID, tableName, whereExtended, findFields,   
                minRecords, adTabId, windowPanel);  
    }  
}