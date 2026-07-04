package com.hoifu.process;  
  
import java.util.ArrayList;  
import java.util.List;  
  
import org.compiere.model.MTable;  
import org.compiere.model.PO;  
import org.compiere.process.DocAction;  
import org.compiere.process.StateEngine;  
import org.compiere.process.SvrProcess;  
import org.compiere.wf.MWFActivity;  
  
/**  
 * 单据取消审批工作流活动，单据回退草稿状态  
 */  
@org.adempiere.base.annotation.Process  
public class CancelApprovalProcess extends SvrProcess {  
  
    @Override  
    protected void prepare() {  
        // 无需额外参数，table_ID 和 record_ID 由框架自动注入  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int tableId  = getTable_ID();  
        int recordId = getRecord_ID();  
  
        if (tableId <= 0 || recordId <= 0)  
            throw new IllegalArgumentException("缺少 Table_ID 或 Record_ID");  
  
        // 1. 查找当前单据所有活跃的工作流活动，过滤出挂起状态（WFState='OS'）  
        MWFActivity[] allActive = MWFActivity.get(getCtx(), tableId, recordId, true);  
        List<MWFActivity> suspended = new ArrayList<>();  
        for (MWFActivity a : allActive) {  
            if (StateEngine.STATE_Suspended.equals(a.getWFState()))  
                suspended.add(a);  
        }  
  
        if (suspended.isEmpty())  
            throw new IllegalStateException("未找到该单据的挂起工作流活动");  
  
        // 2. 中止所有挂起的工作流活动  
        //    参考 WFActivityManage.doIt() 的中止逻辑：  
        //    setWFState() 内部会自动 save() 并触发 checkActivities()，  
        //    checkActivities() 会在所有活动关闭后自动关闭 AD_WF_Process  
        for (MWFActivity activity : suspended) {  
            activity.setTextMsgBefore("用户取消审批，操作人ID：" + getAD_User_ID());  
            activity.setProcessed(true);  
            activity.setWFState(StateEngine.STATE_Aborted);  
            activity.saveEx();  
        }  
  
        // 3. 获取单据 PO 对象，执行 Unlock 操作将 DocStatus 改回 DR  
        MTable table = MTable.get(getCtx(), tableId);  
        PO po = table.getPO(recordId, get_TrxName());  
        if (po == null)  
            throw new IllegalStateException("未找到单据 - Table_ID=" + tableId + ", Record_ID=" + recordId);  
        if (!(po instanceof DocAction))  
            throw new IllegalStateException("该单据未实现 DocAction 接口");  
  
        DocAction doc = (DocAction) po;  
        if (!doc.processIt(DocAction.ACTION_Unlock))  
            throw new IllegalStateException("无法解锁单据 - 当前单据状态=" + doc.getDocStatus());  
        po.saveEx();  
  
        return "操作成功";  
    }  
}