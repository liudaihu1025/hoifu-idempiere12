package com.hoifu.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;

import com.hoifu.service.ReconciliationGenerateService;

@org.adempiere.base.annotation.Process 
public class BatchGenerateVenderReconciliation extends SvrProcess {
	
	private int p_C_BPartner_ID = 0;  
    private String p_ReconPeroid = null;  
    private int p_AD_Org_ID = 0;  
    private static CLogger log = CLogger.getCLogger(BatchGenerateCustomerReconciliation.class);  
  
    @Override  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (int i = 0; i < para.length; i++) {  
            String name = para[i].getParameterName();  
            if (name.equals("C_BPartner_ID"))  
                p_C_BPartner_ID = para[i].getParameterAsInt();  
            else if (name.equals("ReconPeroid"))  
                p_ReconPeroid = (String) para[i].getParameter();  
            else if (name.equals("AD_Org_ID"))  
                p_AD_Org_ID = para[i].getParameterAsInt();  
            else  
                org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);  
        }  
          
        // 验证必填参数  
//        if (p_AD_Org_ID < 0) {  
//            throw new AdempiereException("@AD_Org_ID@ @FillMandatory@");  
//        }  
        if (p_ReconPeroid == null || p_ReconPeroid.length() != 6) {  
            throw new AdempiereException("@ReconPeroid@ @FillMandatory@");  
        }  
          
        log.info("客户批量生成对账单 - 组织ID=" + p_AD_Org_ID +   
            ", 期间=" + p_ReconPeroid +   
            ", 业务伙伴ID=" + (p_C_BPartner_ID > 0 ? String.valueOf(p_C_BPartner_ID) : "全部"));  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        // 创建服务实例  
        ReconciliationGenerateService service = new ReconciliationGenerateService(  
            getCtx(),   
            get_TrxName(),   
            getAD_Client_ID(),   
            getAD_User_ID()  
        );  
          
        ReconciliationGenerateService.GenerateResult result = service.generate(p_C_BPartner_ID, p_ReconPeroid, p_AD_Org_ID, false);  
        
        // 将Service返回的日志写入ProcessInfo，用户可见  
        for (String logMsg : result.getLogs()) {  
            addLog(0, null, null, logMsg);  
        }  
                 
        return result.getSummary(); 
    }  
}
