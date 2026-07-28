package com.hoifu.event.processor;  
  
import org.adempiere.base.event.IEventTopics;  
import org.compiere.model.PO;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;  
  
public class ProductBOMLineEventProcessor implements IEventProcessor {  
  
    @Override  
    public boolean supports(PO po, String topic) {  
        return po instanceof MPPProductBOMLine;  
    }  
  
    @Override  
    public void process(PO po, String topic) {  
    	MPPProductBOMLine line = (MPPProductBOMLine) po;
        // 同步组织到父 BOM 的组织  
        syncOrgID(line, topic);  
    }  
    
    /**  
     * 若 BOMLine 的 AD_Org_ID 与父 PP_Product_BOM 不一致，则自动对齐。  
     * 在 BEFORE_NEW / BEFORE_CHANGE 时执行，框架保存时会写入数据库。  
     */  
    void syncOrgID(MPPProductBOMLine line, String topic) {  
        boolean isBefore = IEventTopics.PO_BEFORE_NEW.equals(topic)  
                || IEventTopics.PO_BEFORE_CHANGE.equals(topic);  
        if (!isBefore) {  
            return;  
        }  
  
        MPPProductBOM parentBOM = line.getParent();  
        if (parentBOM == null) {  
            return;  
        }  
  
        int parentOrgID = parentBOM.getAD_Org_ID();  
        if (line.getAD_Org_ID() != parentOrgID) {  
            line.setAD_Org_ID(parentOrgID);  
        }  
    }  
  

}
