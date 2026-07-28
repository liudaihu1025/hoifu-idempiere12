package com.hoifu.event.processor;  
  
import org.adempiere.base.event.IEventTopics;  
import org.compiere.model.PO;  
import org.compiere.util.DB;  
import org.compiere.wf.MWorkflow;  
  
public class WorkflowEventProcessor implements IEventProcessor {  
  
    @Override  
    public boolean supports(PO po, String topic) {  
        return po instanceof MWorkflow;  
    }  
  
    @Override  
    public void process(PO po, String topic) {  
        MWorkflow wf = (MWorkflow) po;  
        syncOrgToChildTables(wf, topic);  
    }  
  
    /**  
     * 监听 AD_Workflow.AD_Org_ID 变更，同步更新子表及二级子表中与原组织相同的记录。  
     * 触发时机：PO_AFTER_CHANGE，且 AD_Org_ID 字段发生变化。  
     */  
    void syncOrgToChildTables(MWorkflow wf, String topic) {  
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic)) {  
            return;  
        }  
        if (!wf.is_ValueChanged("AD_Org_ID")) {  
            return;  
        }  
  
        int oldOrgId    = wf.get_ValueOldAsInt("AD_Org_ID");  
        int newOrgId    = wf.getAD_Org_ID();  
        int workflowId  = wf.getAD_Workflow_ID();  
        String trxName  = wf.get_TrxName();  
  
        String setClause = " SET AD_Org_ID=" + newOrgId;  
  
        // ── 直接子表（AD_Workflow_ID） ──────────────────────────────────  
  
        // AD_WF_Node（节点）  
        String whereByWF = " WHERE AD_Workflow_ID=" + workflowId  
                         + " AND AD_Org_ID=" + oldOrgId;  
        DB.executeUpdate("UPDATE AD_WF_Node" + setClause + whereByWF, trxName);  
  
        // AD_WF_Block（事务块，直接挂在 Workflow 下）  
        DB.executeUpdate("UPDATE AD_WF_Block" + setClause + whereByWF, trxName);  
  
        // ── 二级子表（AD_WF_Node_ID，需通过 AD_WF_Node 关联） ──────────  
        // 子查询：找到属于该 Workflow 的所有 Node ID  
        String nodeSubQuery = "(SELECT AD_WF_Node_ID FROM AD_WF_Node"  
                            + " WHERE AD_Workflow_ID=" + workflowId + ")";  
  
        String whereByNode = " WHERE AD_WF_Node_ID IN " + nodeSubQuery  
                           + " AND AD_Org_ID=" + oldOrgId;  
  
        // AD_WF_NodeNext（节点跳转，AccessLevel=6 System-Client，通常已为0，按需同步）  
        DB.executeUpdate("UPDATE AD_WF_NodeNext" + setClause + whereByNode, trxName);  
  
        // AD_WF_Node_Para（节点参数）  
        DB.executeUpdate("UPDATE AD_WF_Node_Para" + setClause + whereByNode, trxName);  
  
        // PP_WF_Node_Asset（节点资产，AccessLevel=3）  
        DB.executeUpdate("UPDATE PP_WF_Node_Asset" + setClause + whereByNode, trxName);  
  
        // PP_WF_Node_Product（节点产品，AccessLevel=3）  
        DB.executeUpdate("UPDATE PP_WF_Node_Product" + setClause + whereByNode, trxName);  
    }  
}