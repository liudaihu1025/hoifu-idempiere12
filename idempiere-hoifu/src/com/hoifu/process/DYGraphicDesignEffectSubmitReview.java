package com.hoifu.process;

import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class DYGraphicDesignEffectSubmitReview extends SvrProcess {

	// ==================== 平面设计状态常量 ====================
	private static final String DESIGN_STATUS_REVIEWING = "RV"; // 评审中

	// ==================== 需求状态常量 ====================
	private static final String DEMAND_STATUS_GRAPHIC_REVIEWING = "GR"; // 平面设计评审中

	// ==================== 设计效果状态常量 ====================
	private static final String EFFECT_STATUS_REVIEWING = "RV"; // 评审中
	private static final String EFFECT_STATUS_WAITING_REVIEW = "WR"; // 待评审

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
  
        // ==================== 1. 获取平面设计任务ID ====================  
        int graphicDesignId = getRecord_ID();  
        if (graphicDesignId <= 0)  
            throw new IllegalArgumentException("未获取到平面设计任务ID，请从平面设计任务窗口发起流程");  
  
        // ==================== 2. 校验参数 ====================  
        if (p_ReviewerIDs == null || p_ReviewerIDs.length == 0)  
            throw new IllegalArgumentException("请至少选择一位评审人");  
        if (p_FinalReviewer_ID <= 0)  
            throw new IllegalArgumentException("请选择终审人");  
  
        // ==================== 3. 加载平面设计任务 ====================  
        PO graphicDesign = MTable.get(getCtx(), "dy_graphicdesign").getPO(graphicDesignId, get_TrxName());  
        if (graphicDesign == null || graphicDesign.get_ID() == 0)  
            throw new IllegalArgumentException("平面设计任务记录不存在");  
  
        int orgId = ((Number) graphicDesign.get_Value("ad_org_id")).intValue();  
        int samplingDemandId = ((Number) graphicDesign.get_Value("dy_samplingdemand_id")).intValue();  
  
        // ==================== 4. 加载需求记录 ====================  
        PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(samplingDemandId, get_TrxName());  
        if (demand == null || demand.get_ID() == 0)  
            throw new IllegalArgumentException("打样需求记录不存在");  
  
        // ==================== 5. 更新平面设计任务状态=评审中，记录终审人和完成时间 ====================  
        graphicDesign.set_ValueNoCheck("designstatus", DESIGN_STATUS_REVIEWING);  
        graphicDesign.set_ValueNoCheck("finalreviewer_id", p_FinalReviewer_ID);  
        graphicDesign.set_ValueNoCheck("enddate", new Timestamp(System.currentTimeMillis()));  
        graphicDesign.saveEx();  
  
        // ==================== 6. 更新需求状态=平面设计评审中 ====================  
        demand.set_ValueNoCheck("requeststatus", DEMAND_STATUS_GRAPHIC_REVIEWING);  
        demand.saveEx();  
  
        // ==================== 7. 查询该任务下状态为"待评审"的有效效果 ====================  
        // 只对 effectstatus='WR'（待评审）的效果生成评审记录，已评审/评审中的效果不重复处理  
        int[] effectIds = DB.getIDsEx(get_TrxName(),  
                "SELECT dy_graphicdesigneffect_id FROM adempiere.dy_graphicdesigneffect "  
                        + "WHERE dy_graphicdesign_id=? AND isactive='Y' AND effectstatus=?",  
                new Object[] { graphicDesignId, EFFECT_STATUS_WAITING_REVIEW });  
        if (effectIds == null || effectIds.length == 0)  
			throw new IllegalArgumentException("该平面设计任务下没有状态为'待评审'的设计效果记录");
  
        // ==================== 8. 遍历每个待评审效果，生成评审记录 ====================  
        int totalReviewCount = 0;  
  
        for (int effectId : effectIds) {  
  
            // 8.1 加载效果记录  
            PO effect = MTable.get(getCtx(), "dy_graphicdesigneffect").getPO(effectId, get_TrxName());  
            if (effect == null || effect.get_ID() == 0)  
                continue;  
  
            // 8.2 更新效果状态=评审中  
            effect.set_ValueNoCheck("effectstatus", EFFECT_STATUS_REVIEWING);  
            effect.saveEx();  
  
			// 8.3 将该效果下所有旧评审记录置为无效（isactive='N'）
			// 重新提交评审时，旧记录全部失效，确保每轮只有一套有效评审记录
			DB.executeUpdateEx(
					"UPDATE adempiere.dy_graphicdesignreview " + "SET isactive='N', updated=now(), updatedby=? "
							+ "WHERE dy_graphicdesigneffect_id=? AND isactive='Y'",
					new Object[] { getAD_User_ID(), effectId }, get_TrxName());

			// 8.4 为每位评审人生成评审记录（isfinalreview='N'）
            for (int reviewerId : p_ReviewerIDs) {  
                PO review = MTable.get(getCtx(), "dy_graphicdesignreview").getPO(0, get_TrxName());  
                review.set_ValueNoCheck("ad_client_id", getAD_Client_ID());  
                review.set_ValueNoCheck("ad_org_id", orgId);  
                review.set_ValueNoCheck("dy_graphicdesigneffect_id", effectId);  
                review.set_ValueNoCheck("reviewer_id", reviewerId);  
                review.set_ValueNoCheck("isfinalreview", "N");  
                review.saveEx();  
                totalReviewCount++;  
            }  
  
            // 8.5 为终审人生成终审记录（isfinalreview='Y'）  
            PO finalReview = MTable.get(getCtx(), "dy_graphicdesignreview").getPO(0, get_TrxName());  
            finalReview.set_ValueNoCheck("ad_client_id", getAD_Client_ID());  
            finalReview.set_ValueNoCheck("ad_org_id", orgId);  
            finalReview.set_ValueNoCheck("dy_graphicdesigneffect_id", effectId);  
            finalReview.set_ValueNoCheck("reviewer_id", p_FinalReviewer_ID);  
            finalReview.set_ValueNoCheck("isfinalreview", "Y");  
            finalReview.saveEx();  
            totalReviewCount++;  
        }  
  
        return "提交成功，共 " + effectIds.length + " 个待评审效果，生成 " + totalReviewCount  
                + " 条评审记录（" + p_ReviewerIDs.length + " 位评审人 + 1 位终审人），平面设计进入评审中";  
    }
}