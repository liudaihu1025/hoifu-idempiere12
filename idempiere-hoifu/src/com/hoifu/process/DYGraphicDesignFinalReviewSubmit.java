package com.hoifu.process;  
  
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;  
  
@org.adempiere.base.annotation.Process  
public class DYGraphicDesignFinalReviewSubmit extends SvrProcess {  
  
    // ==================== 需求状态常量 ====================  
	private static final String DEMAND_STATUS_GRAPHIC_REVIEWING = "GR"; // 平面设计评审中
	private static final String DEMAND_STATUS_PROCESS_WAITING = "PW"; // 待工艺处理（工艺设计待受理）
	private static final String DEMAND_STATUS_COMPLETED = "CO"; // 已完成
  
    // ==================== 平面设计状态常量 ====================  
	private static final String DESIGN_STATUS_COMPLETED = "CO"; // 已完成
  
    // ==================== 设计效果状态常量 ====================  
	private static final String EFFECT_STATUS_WAITING_REVIEW = "WR"; // 待评审
	private static final String EFFECT_STATUS_REVIEWED = "RE"; // 已评审
  
    // ==================== 评审结果常量 ====================  
	private static final String REVIEW_RESULT_ADOPT = "AC"; // 采纳
	private static final String REVIEW_RESULT_REJECT = "NA"; // 不采纳
  
	// ==================== 流程参数 ====================
	private String p_ReviewResult; // 评审结果（AC=采纳，NA=不采纳）
	private String p_Description; // 评审意见/备注
  
    @Override  
    protected void prepare() {  
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("reviewresult".equals(name)) {
				p_ReviewResult = para.getParameterAsString();
			} else if ("description".equals(name)) {
				p_Description = para.getParameterAsString();
            }  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
  
		// ==================== 1. 从 T_Selection 获取 effectId ====================
        int[] ids = DB.getIDsEx(get_TrxName(),  
				"SELECT T_Selection_ID FROM adempiere.T_Selection WHERE AD_PInstance_ID=?", getAD_PInstance_ID());
		if (ids == null || ids.length != 1)
			throw new AdempiereException("请只选择一条记录");
		int effectId = ids[0];
  
		// ==================== 2. 加载设计效果记录 ====================
		PO effect = MTable.get(getCtx(), "dy_graphicdesigneffect").getPO(effectId, get_TrxName());
		if (effect == null || effect.get_ID() == 0)
			throw new AdempiereException("设计效果记录不存在");
  
		// ==================== 3. 查找当前用户的终审记录 ====================
		// 通过 effectId + currentUserId + isfinalreview='Y' 查找，隐含了终审人身份校验
		int currentUserId = getAD_User_ID();
		int reviewId = DB.getSQLValue(get_TrxName(),
				"SELECT dy_graphicdesignreview_id FROM adempiere.dy_graphicdesignreview "
						+ "WHERE dy_graphicdesigneffect_id=? AND reviewer_id=? AND isfinalreview='Y' AND isactive='Y'",
				effectId, currentUserId);
		if (reviewId <= 0)
			throw new AdempiereException("当前用户不是该设计效果的最终审核人，无权提交终审");
  
		// ==================== 4. 加载终审评审记录 ====================
        PO review = MTable.get(getCtx(), "dy_graphicdesignreview").getPO(reviewId, get_TrxName());  
        if (review == null || review.get_ID() == 0)  
			throw new AdempiereException("终审评审记录不存在");
  
		// ==================== 5. 校验参数 ====================
        if (p_ReviewResult == null || p_ReviewResult.trim().isEmpty())  
			throw new AdempiereException("请选择终审结果（采纳/不采纳）");
  
		// ==================== 6. 加载关联记录 ====================
        int graphicDesignId = ((Number) effect.get_Value("dy_graphicdesign_id")).intValue();  
        PO graphicDesign = MTable.get(getCtx(), "dy_graphicdesign").getPO(graphicDesignId, get_TrxName());  
        if (graphicDesign == null || graphicDesign.get_ID() == 0)  
            throw new AdempiereException("平面设计记录不存在");  
  
        int demandId = ((Number) graphicDesign.get_Value("dy_samplingdemand_id")).intValue();  
        PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(demandId, get_TrxName());  
        if (demand == null || demand.get_ID() == 0)  
            throw new AdempiereException("打样需求记录不存在");  
  
		// ==================== 7. 校验需求状态 ====================
        String requestStatus = (String) demand.get_Value("requeststatus");  
        if (!DEMAND_STATUS_GRAPHIC_REVIEWING.equals(requestStatus))  
			throw new AdempiereException("当前需求状态不是'平面设计评审中'，无法提交终审");

		// ==================== 8. 共同逻辑（采纳/不采纳均执行）====================
  
		// 8.1 评审结果和评审意见写入评审记录
        review.set_ValueNoCheck("reviewresult", p_ReviewResult);  
		review.set_ValueNoCheck("reviewcomment", p_Description);
		review.saveEx();

		// 8.2 备注写入效果表（直接覆盖）
        if (p_Description != null && !p_Description.trim().isEmpty())  
			effect.set_ValueNoCheck("description", p_Description);
  
		// ==================== 9. 分支逻辑 ====================
        if (REVIEW_RESULT_ADOPT.equals(p_ReviewResult)) {  
  
            // ---- 采纳 ----  
  
			// 9.1 更新效果评审结果=采纳，效果状态=已评审
			effect.set_ValueNoCheck("reviewresult", REVIEW_RESULT_ADOPT);
			effect.set_ValueNoCheck("effectstatus", EFFECT_STATUS_REVIEWED);
            effect.saveEx();  
  
			// 9.2 检查是否所有效果的有效终审记录均已采纳
			// 当前效果已在 9.1 saveEx()，此查询能反映最新状态
			int notAdoptedCount = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM adempiere.dy_graphicdesignreview gdr "
							+ "JOIN adempiere.dy_graphicdesigneffect gde "
							+ "  ON gdr.dy_graphicdesigneffect_id = gde.dy_graphicdesigneffect_id "
							+ "WHERE gde.dy_graphicdesign_id = ? "
							+ "  AND gdr.isfinalreview = 'Y' AND gdr.isactive = 'Y' "
							+ "  AND (gdr.reviewresult IS NULL OR gdr.reviewresult <> 'AC')",
					graphicDesignId);

			if (notAdoptedCount == 0) {
				// 所有效果终审通过，推进表头状态

				// 9.3 更新平面设计状态="已完成"
				graphicDesign.set_ValueNoCheck("designstatus", DESIGN_STATUS_COMPLETED);
				graphicDesign.saveEx();

				// 9.4 根据是否需要工艺设计，更新需求状态
				boolean isNeedProcessDesign = demand.get_ValueAsBoolean("isneedprocessdesign");
				if (isNeedProcessDesign) {
					demand.set_ValueNoCheck("requeststatus", DEMAND_STATUS_PROCESS_WAITING);
				} else {
					demand.set_ValueNoCheck("requeststatus", DEMAND_STATUS_COMPLETED);
				}
				demand.saveEx();
  
				return "终审完成，采纳，所有效果终审通过，平面设计已完成";
            }  
  
			return "终审完成，采纳，仍有效果待终审";
  
        } else {  
  
            // ---- 不采纳 ----  
  
			// 9.1 更新效果状态="待评审"，评审结果=不采纳
			effect.set_ValueNoCheck("effectstatus", EFFECT_STATUS_WAITING_REVIEW);
			effect.set_ValueNoCheck("reviewresult", REVIEW_RESULT_REJECT);
			effect.saveEx();
  
			// 不采纳时不更新平面设计状态和需求状态，其他效果仍可继续评审
  
			return "终审完成，不采纳，该效果退回修改";
        }  
    }  
}