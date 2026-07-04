package com.hoifu.process;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MTable;  
import org.compiere.model.PO;  
import org.compiere.process.DocAction;  
import org.compiere.process.ProcessInfo;  
import org.compiere.process.SvrProcess;  
import org.compiere.wf.MWorkflow;  
  
/**  
 * 直接完成单据  
 */  
@org.adempiere.base.annotation.Process  
public class DocCompleteProcess extends SvrProcess {  
  
    @Override  
    protected void prepare() {  
        // 无需额外参数，table_ID 和 record_ID 由框架自动注入  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int tableId  = getTable_ID();  
        int recordId = getRecord_ID();  
  
        if (tableId <= 0 || recordId <= 0)  
            throw new AdempiereException("Table ID 或 Record ID 无效");  
  
        // 1. 根据 tableId 获取 MTable，再加载对应的 PO  
        MTable table = MTable.get(getCtx(), tableId);  
        PO po = table.getPO(recordId, get_TrxName());  
  
        if (po == null)  
            throw new AdempiereException("找不到记录: Table=" + table.getTableName() + ", ID=" + recordId);  
  
        // 2. 确认该 PO 实现了 DocAction 接口（即是可处理的单据）  
        if (!(po instanceof DocAction))  
            throw new AdempiereException("该记录不是单据类型: " + table.getTableName());  
  
        // 3. 调用工作流引擎执行"完成"动作  
        ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(po, DocAction.ACTION_Complete);  
  
        if (pi != null && pi.isError())  
            throw new AdempiereException("完成单据失败: " + pi.getSummary());  
  
        // 4. 重新加载 PO 以获取最新状态  
        po.load(get_TrxName());  
        String docStatus = (String) po.get_Value(DocAction.DOC_COLUMNNAME_DocStatus);  
  
        return "操作成功";  
    }  
}