package com.hoifu.process;  
  
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;  
  
@org.adempiere.base.annotation.Process  
public class DYSamplingDemandSubmit extends SvrProcess {  
  
	// ==================== 需求状态常量 ====================
	private static final String STATUS_DRAFT = "DR"; // 草稿
	private static final String STATUS_GRAPHIC_WAITING = "GW"; // 平面设计待受理
	private static final String STATUS_PROCESS_WAITING = "PW"; // 工艺设计待受理

    @Override  
    protected void prepare() {  
        // 无额外参数  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int recordId = getRecord_ID();  
        if (recordId <= 0)  
            throw new IllegalArgumentException("未找到记录ID");  
  
        PO demand = MTable.get(getCtx(), "dy_samplingdemand")  
                          .getPO(recordId, get_TrxName());  
        if (demand == null || demand.get_ID() == 0)  
            throw new IllegalArgumentException("打样需求记录不存在");  
  
        // 1. 校验当前状态必须是草稿  
        String currentStatus = (String) demand.get_Value("requeststatus");  
		if (!STATUS_DRAFT.equals(currentStatus))
            return "只有【草稿】状态的需求才能提交，当前状态：" + currentStatus;  
  
        // 2. 校验至少勾选一项需求任务  
		boolean needGraphic = Boolean.TRUE.equals(demand.get_Value("isneedgraphicdesign"));
		boolean needProcess = Boolean.TRUE.equals(demand.get_Value("isneedprocessdesign"));
  
        if (!needGraphic && !needProcess)  
            throw new IllegalArgumentException("请至少勾选一项需求任务（平面设计 或 工艺设计）");  
  
        // 3. 根据勾选情况设置目标状态  
        // 勾选了平面设计（含两者都勾）→ 平面设计待受理  
        // 仅勾选工艺设计            → 工艺设计待受理  
		String targetStatus = needGraphic ? STATUS_GRAPHIC_WAITING : STATUS_PROCESS_WAITING;
  
        demand.set_ValueOfColumn("requeststatus", targetStatus);  
        demand.saveEx();  

		return "提交成功";
    }  
}