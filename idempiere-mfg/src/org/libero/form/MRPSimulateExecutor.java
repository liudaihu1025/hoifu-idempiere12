package org.libero.form;

import org.adempiere.webui.panel.ADForm;
import org.compiere.util.CLogger;

/**  
 * 模拟执行-计算物料计划MRP
 */  
@org.idempiere.ui.zk.annotation.Form  
public class MRPSimulateExecutor extends ADForm {
  
    private static final CLogger log = CLogger.getCLogger(MRPSimulateExecutor.class);  
  
    /**  计算物料计划MRP 流程的 AD_Process_UU */  
    private static final String AD_PROCESS_UU = "8e8e7eae-0dab-4913-8d88-4e506eb61e1b";  

    @Override  
    protected void initForm() {  

    }  


}