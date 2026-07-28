package com.hoifu.event.processor;  
  
import org.adempiere.base.event.IEventTopics;  
import org.compiere.model.MBPartner;  
import org.compiere.model.PO;  
import org.compiere.util.DB;  
  
public class BPartnerEventProcessor implements IEventProcessor {  
  
    @Override  
    public boolean supports(PO po, String topic) {  
        return po instanceof MBPartner;  
    }  
  
    @Override  
    public void process(PO po, String topic) {  
        MBPartner bp = (MBPartner) po;  
        syncOrgToChildTables(bp, topic);  
    }  
  
    /**  
     * 监听 C_BPartner.AD_Org_ID 变更，同步更新子表中与原组织相同的记录。  
     * 触发时机：PO_AFTER_CHANGE，且 AD_Org_ID 字段发生变化。  
     */  
    void syncOrgToChildTables(MBPartner bp, String topic) {  
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic)) {  
            return;  
        }  
        if (!bp.is_ValueChanged("AD_Org_ID")) {  
            return;  
        }  
  
        int oldOrgId  = bp.get_ValueOldAsInt("AD_Org_ID");  
        int newOrgId  = bp.getAD_Org_ID();  
        int bPartnerId = bp.getC_BPartner_ID();  
        String trxName = bp.get_TrxName();  
  
        String setClause   = " SET AD_Org_ID=" + newOrgId;  
        String whereClause = " WHERE C_BPartner_ID=" + bPartnerId  
                           + " AND AD_Org_ID=" + oldOrgId;  
  
        // 地址  
        DB.executeUpdate("UPDATE C_BPartner_Location" + setClause + whereClause, trxName);  
  
        // 联系人/用户  
        DB.executeUpdate("UPDATE AD_User" + setClause + whereClause, trxName);  
  
        // 银行账户  
        DB.executeUpdate("UPDATE C_BP_BankAccount" + setClause + whereClause, trxName);  
  
        // 客户会计科目  
        DB.executeUpdate("UPDATE C_BP_Customer_Acct" + setClause + whereClause, trxName);  
  
        // 供应商会计科目  
        DB.executeUpdate("UPDATE C_BP_Vendor_Acct" + setClause + whereClause, trxName);  
  
        // 员工会计科目  
        DB.executeUpdate("UPDATE C_BP_Employee_Acct" + setClause + whereClause, trxName);  
  
        // 业务伙伴产品信息（OrgOwnership 在 generalOwnership 中全局置 0，  
        // 此处改为按 BPartner 精确同步）  
        DB.executeUpdate("UPDATE C_BPartner_Product" + setClause + whereClause, trxName);  
  
    }  
}