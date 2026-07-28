package com.hoifu.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class DYGraphicDesignReviewSubmit extends SvrProcess {

	// ==================== 需求状态常量 ====================
	private static final String DEMAND_STATUS_GRAPHIC_REVIEWING = "GR"; // 平面设计评审中

	// ==================== 平面设计状态常量 ====================
	private static final String DESIGN_STATUS_REVIEWING = "RV"; // 评审中

	// ==================== 设计效果状态常量 ====================
	private static final String EFFECT_STATUS_WAITING_REVIEW = "WR"; // 待评审（不采纳时回退）
	private static final String EFFECT_STATUS_REVIEWED = "RE"; // 已评审（全部采纳时更新）

	// ==================== 评审结果常量 ====================
	private static final String REVIEW_RESULT_ADOPT = "AC"; // 采纳
	private static final String REVIEW_RESULT_REJECT = "NA"; // 不采纳

	// ==================== 参数 ====================
	private int p_CustomerMatchScore = 0; // 客户需求匹配度（1-5分，必填）
	private int p_BrandTonalityScore = 0; // 品牌调性契合度（1-5分，必填）
	private int p_ProcessFeasibilityScore = 0; // 工艺可实现性预判（1-5分，必填）
	private int p_ComplianceScore = 0; // 合理性-环保/法规（1-5分，必填）
	private int p_MarketCompetitiveScore = 0; // 市场竞争力（1-5分，必填）
	private String p_ReviewComment = null; // 评审意见
	private String p_ReviewResult = null; // 评审结果（AC=采纳，NA=不采纳，必填）

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			String name = p.getParameterName();
			if (p.getParameter() == null)
				continue;
			switch (name) {
			case "customermatchscore":
				p_CustomerMatchScore = p.getParameterAsInt();
				break;
			case "brandtonalityscore":
				p_BrandTonalityScore = p.getParameterAsInt();
				break;
			case "processfeasibilityscore":
				p_ProcessFeasibilityScore = p.getParameterAsInt();
				break;
			case "compliancescore":
				p_ComplianceScore = p.getParameterAsInt();
				break;
			case "marketcompetitivescore":
				p_MarketCompetitiveScore = p.getParameterAsInt();
				break;
			case "reviewcomment":
				p_ReviewComment = p.getParameterAsString();
				break;
			case "reviewresult":
				p_ReviewResult = p.getParameterAsString();
				break;
			default:
				break;
			}
		}
	}

	@Override
	protected String doIt() throws Exception {

		// ==================== 1. 从 T_Selection 获取信息窗口选中的设计效果记录 ====================
		// AD_Table_ID = dy_graphicdesigneffect，T_Selection 存的是
		// dy_graphicdesigneffect_id
		int[] ids = DB.getIDsEx(get_TrxName(), "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
				getAD_PInstance_ID());

		if (ids == null || ids.length == 0)
			throw new AdempiereException("请先选择一条设计效果记录");
		if (ids.length > 1)
			throw new AdempiereException("每次只能选择一条设计效果记录进行提交");

		int effectId = ids[0];

		// ==================== 2. 加载设计效果记录 ====================
		PO effect = MTable.get(getCtx(), "dy_graphicdesigneffect").getPO(effectId, get_TrxName());
		if (effect == null || effect.get_ID() == 0)
			throw new AdempiereException("设计效果记录不存在，ID=" + effectId);

		// ==================== 3. 通过 effectId + 当前用户 查找当前用户的评审记录 ====================
		// 只查普通评审记录（isfinalreview='N'），终审记录由终审流程处理
		int currentUserId = getAD_User_ID();
		int reviewId = DB.getSQLValue(get_TrxName(),
				"SELECT dy_graphicdesignreview_id FROM adempiere.dy_graphicdesignreview "
						+ "WHERE dy_graphicdesigneffect_id=? AND reviewer_id=? "
						+ "AND isactive='Y' AND isfinalreview='N'",
				effectId, currentUserId);

		if (reviewId <= 0)
			throw new AdempiereException("当前用户不是该设计效果的评审人，无权提交评审");

		// ==================== 4. 加载评审记录 ====================
		PO review = MTable.get(getCtx(), "dy_graphicdesignreview").getPO(reviewId, get_TrxName());
		if (review == null || review.get_ID() == 0)
			throw new AdempiereException("评审记录不存在，ID=" + reviewId);

		// ==================== 5. 取关联的平面设计任务 ID ====================
		int graphicDesignId = ((Number) effect.get_Value("dy_graphicdesign_id")).intValue();

		// ==================== 6. 加载平面设计任务记录 ====================
		PO graphicDesign = MTable.get(getCtx(), "dy_graphicdesign").getPO(graphicDesignId, get_TrxName());
		if (graphicDesign == null || graphicDesign.get_ID() == 0)
			throw new AdempiereException("平面设计任务记录不存在，ID=" + graphicDesignId);

		// ==================== 7. 加载打样需求记录 ====================
		int samplingDemandId = ((Number) graphicDesign.get_Value("dy_samplingdemand_id")).intValue();
		PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(samplingDemandId, get_TrxName());
		if (demand == null || demand.get_ID() == 0)
			throw new AdempiereException("打样需求记录不存在，ID=" + samplingDemandId);

		// ==================== 8. 校验需求状态、设计状态和效果状态 ====================
		String requestStatus = (String) demand.get_Value("requeststatus");
		if (!DEMAND_STATUS_GRAPHIC_REVIEWING.equals(requestStatus))
			throw new AdempiereException("当前需求状态不是'平面设计评审中'，无法提交评审");

		String designStatus = (String) graphicDesign.get_Value("designstatus");
		if (!DESIGN_STATUS_REVIEWING.equals(designStatus))
			throw new AdempiereException("当前平面设计状态不是'评审中'，无法提交评审");

		// 效果已评审则不允许再次提交
		String effectStatus = (String) effect.get_Value("effectstatus");
		if (EFFECT_STATUS_REVIEWED.equals(effectStatus))
			throw new AdempiereException("该设计效果已完成评审，无法重复提交");

		// ==================== 9. 校验参数 ====================
		// 评审结果必填且合法
		if (p_ReviewResult == null || p_ReviewResult.trim().isEmpty())
			throw new AdempiereException("评审结果不能为空");
		if (!REVIEW_RESULT_ADOPT.equals(p_ReviewResult) && !REVIEW_RESULT_REJECT.equals(p_ReviewResult))
			throw new AdempiereException("评审结果值非法，只允许 AC（采纳）或 NA（不采纳）");

		// 五个维度评分必填，且在 1-5 范围内
		validateScore("客户需求匹配度", p_CustomerMatchScore);
		validateScore("品牌调性契合度", p_BrandTonalityScore);
		validateScore("工艺可实现性预判", p_ProcessFeasibilityScore);
		validateScore("合理性-环保/法规", p_ComplianceScore);
		validateScore("市场竞争力", p_MarketCompetitiveScore);

		// ==================== 10. 拼接评审意见到效果表 Description（无论采纳/不采纳）====================
		if (p_ReviewComment != null && !p_ReviewComment.trim().isEmpty()) {
			MUser user = MUser.get(getCtx(), currentUserId);
			String reviewer = (user != null) ? user.getName() : String.valueOf(currentUserId);
			String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
			String newEntry = dateStr + "_" + reviewer + "\n" + p_ReviewComment;

			String existingDesc = (String) effect.get_Value("description");
			String merged = (existingDesc == null || existingDesc.trim().isEmpty()) ? newEntry
					: newEntry + "\n\n" + existingDesc;
			effect.set_ValueNoCheck("description", merged);
		}

		// ==================== 11. 保存评审记录（赋值各维度分数、总分、评审意见、评审结果）====================
		int totalScore = p_CustomerMatchScore + p_BrandTonalityScore + p_ProcessFeasibilityScore + p_ComplianceScore
				+ p_MarketCompetitiveScore;

		review.set_ValueNoCheck("customermatchscore", p_CustomerMatchScore);
		review.set_ValueNoCheck("brandtonalityscore", p_BrandTonalityScore);
		review.set_ValueNoCheck("processfeasibilityscore", p_ProcessFeasibilityScore);
		review.set_ValueNoCheck("compliancescore", p_ComplianceScore);
		review.set_ValueNoCheck("marketcompetitivescore", p_MarketCompetitiveScore);
		review.set_ValueNoCheck("totalscore", new BigDecimal(totalScore));
		review.set_ValueNoCheck("reviewcomment", p_ReviewComment);
		review.set_ValueNoCheck("reviewresult", p_ReviewResult);
		review.saveEx();

		// ==================== 12. 根据评审结果执行不同逻辑 ====================
		if (REVIEW_RESULT_ADOPT.equals(p_ReviewResult)) {

			// --- 采纳：检查是否所有普通评审人都已完成评审（排除终审记录）---
			int pendingCount = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM adempiere.dy_graphicdesignreview "
							+ "WHERE dy_graphicdesigneffect_id=? AND isactive='Y' "
							+ "AND isfinalreview='N' AND reviewresult IS NULL",
					effectId);

			if (pendingCount == 0) {
				// 所有评审人均已完成 → 更新效果状态=已评审，计算综合平均得分（向上取整）
				// 只统计普通评审人的得分（排除终审记录）
				BigDecimal avgScore = DB.getSQLValueBD(get_TrxName(),
						"SELECT SUM(totalscore) / COUNT(*) FROM adempiere.dy_graphicdesignreview "
								+ "WHERE dy_graphicdesigneffect_id=? AND isactive='Y' "
								+ "AND isfinalreview='N' AND reviewresult IS NOT NULL",
						effectId);
				if (avgScore != null) {
					avgScore = avgScore.setScale(0, RoundingMode.CEILING);
					effect.set_ValueNoCheck("avgscore", avgScore);
				}
			}
			effect.saveEx();

		} else {
			// --- 不采纳：回退相关状态 ---
			// 更新设计效果状态=待评审
			effect.set_ValueNoCheck("effectstatus", EFFECT_STATUS_WAITING_REVIEW);
			effect.saveEx();
			return "评审提交成功，综合得分：" + totalScore + " 分，不采纳，该效果退回修改";
		}

		return "评审提交成功，综合得分：" + totalScore + " 分";
	}

	/**
	 * 校验评分是否在 1-5 范围内
	 * 
	 * @param label 维度名称（用于错误提示）
	 * @param score 评分值
	 * @throws IllegalArgumentException 评分不在 1-5 范围时抛出
	 */
	private void validateScore(String label, int score) {
		if (score < 1 || score > 5)
			throw new IllegalArgumentException(label + " 评分必须在 1-5 之间，当前值：" + score);
	}
}