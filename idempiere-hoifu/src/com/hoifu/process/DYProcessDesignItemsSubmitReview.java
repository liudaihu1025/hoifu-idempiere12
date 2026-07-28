package com.hoifu.process;

import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class DYProcessDesignItemsSubmitReview extends SvrProcess {

	// ==================== 工艺设计状态常量 ====================
	private static final String PROCESS_STATUS_REVIEWING = "RV"; // 评审中

	// ==================== 需求状态常量 ====================
	private static final String DEMAND_STATUS_PROCESS_REVIEWING = "PR"; // 工艺设计评审中

	// ==================== 参数 ====================
	private int[] p_ReviewerIDs;
	private int p_FinalReviewer_ID;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("reviewers".equals(name)) {
				p_ReviewerIDs = para.getParameterAsIntArray();
			} else if ("finalreviewer_ID".equals(name)) {
				p_FinalReviewer_ID = para.getParameterAsInt();
			}
		}
	}

	@Override
    protected String doIt() throws Exception {  
  
		// ==================== 1. 获取工艺设计任务ID ====================
		int processDesignId = getRecord_ID();
		if (processDesignId <= 0)
			throw new IllegalArgumentException("未获取到工艺设计任务ID，请从工艺设计任务窗口发起流程");
  
        // ==================== 2. 校验参数 ====================  
        if (p_ReviewerIDs == null || p_ReviewerIDs.length == 0)  
            throw new IllegalArgumentException("请至少选择一位评审人");  
        if (p_FinalReviewer_ID <= 0)  
            throw new IllegalArgumentException("请选择终审人");  
  
		// ==================== 3. 加载工艺设计任务 ====================
		PO processDesign = MTable.get(getCtx(), "dy_processdesign").getPO(processDesignId, get_TrxName());
		if (processDesign == null || processDesign.get_ID() == 0)
			throw new IllegalArgumentException("工艺设计任务记录不存在");
  
		int orgId = ((Number) processDesign.get_Value("ad_org_id")).intValue();
		int samplingDemandId = ((Number) processDesign.get_Value("dy_samplingdemand_id")).intValue();
  
        // ==================== 4. 加载需求记录 ====================  
        PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(samplingDemandId, get_TrxName());  
        if (demand == null || demand.get_ID() == 0)  
            throw new IllegalArgumentException("打样需求记录不存在");  
  
		// ==================== 5. 更新工艺设计任务状态=评审中，记录终审人和完成时间 ====================
		processDesign.set_ValueNoCheck("processstatus", PROCESS_STATUS_REVIEWING);
		processDesign.set_ValueNoCheck("finalreviewer_id", p_FinalReviewer_ID);
		processDesign.set_ValueNoCheck("enddate", new Timestamp(System.currentTimeMillis()));
		processDesign.saveEx();
  
		// ==================== 6. 更新需求状态=工艺设计评审中 ====================
		demand.set_ValueNoCheck("requeststatus", DEMAND_STATUS_PROCESS_REVIEWING);
        demand.saveEx();  
  
		// ==================== 7. 查询该工艺设计任务下所有有效事项 ====================
		int[] itemIds = DB.getIDsEx(get_TrxName(), "SELECT dy_processdesignitem_id FROM adempiere.dy_processdesignitem "
				+ "WHERE dy_processdesign_id=? AND isactive='Y'", new Object[] { processDesignId });
		if (itemIds == null || itemIds.length == 0)
			throw new IllegalArgumentException("该工艺设计任务下没有有效的工艺事项记录");
  
		// ==================== 8. 遍历每个事项，为每位评审人生成评审记录 ====================
		// 评审人数 × 事项数 + 终审人数 × 事项数 = (p_ReviewerIDs.length + 1) × itemIds.length 条记录
        int totalReviewCount = 0;  
  
		for (int itemId : itemIds) {

			// 8.0 更新事项状态=评审中
			PO item = MTable.get(getCtx(), "dy_processdesignitem").getPO(itemId, get_TrxName());
			item.set_ValueNoCheck("itemstatus", "RV");
			item.saveEx();
  
			// 8.1 为每位普通评审人生成评审记录（isfinalreview='N'）
            for (int reviewerId : p_ReviewerIDs) {  
				PO review = MTable.get(getCtx(), "dy_processdesignreview").getPO(0, get_TrxName());
				review.set_ValueNoCheck("ad_client_id", getAD_Client_ID());
				review.set_ValueNoCheck("ad_org_id", orgId);
				review.set_ValueNoCheck("dy_processdesign_id", processDesignId);
				review.set_ValueNoCheck("dy_processdesignitem_id", itemId);
				review.set_ValueNoCheck("reviewer_id", reviewerId);
				review.set_ValueNoCheck("isfinalreview", "N");
                review.saveEx();  
                totalReviewCount++;  
            }  
  
			// 8.2 为终审人生成终审记录（isfinalreview='Y'）
			PO finalReview = MTable.get(getCtx(), "dy_processdesignreview").getPO(0, get_TrxName());
			finalReview.set_ValueNoCheck("ad_client_id", getAD_Client_ID());
			finalReview.set_ValueNoCheck("ad_org_id", orgId);
			finalReview.set_ValueNoCheck("dy_processdesign_id", processDesignId);
			finalReview.set_ValueNoCheck("dy_processdesignitem_id", itemId);
			finalReview.set_ValueNoCheck("reviewer_id", p_FinalReviewer_ID);
			finalReview.set_ValueNoCheck("isfinalreview", "Y");
            finalReview.saveEx();  
            totalReviewCount++;  
        }  
  
		return "提交成功，共 " + itemIds.length + " 个工艺事项，生成 " + totalReviewCount + " 条评审记录（" + p_ReviewerIDs.length
				+ " 位评审人 + 1 位终审人），工艺设计进入评审中";
	}
}