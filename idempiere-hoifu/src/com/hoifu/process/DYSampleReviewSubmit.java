package com.hoifu.process;  
  
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
  
@org.adempiere.base.annotation.Process  
public class DYSampleReviewSubmit extends SvrProcess {  
  
	// ==================== 参数 ====================
	private int p_DesignRestorationScore = 0; // 设计还原度（1-5分）
	private int p_ProcessEffectScore = 0; // 工艺效果（1-5分）
	private int p_MassProductionScore = 0; // 批量生产可行性（1-5分）
	private int p_CostControlScore = 0; // 成本控制（1-5分）
	private int p_AppearanceScore = 0; // 外观效果（1-5分）
	private int p_DraftRestorationScore = 0; // 样稿还原度（1-5分）
	private int p_PhysicochemicalScore = 0; // 理化数据（1-5分）
	private int p_MassStabilityScore = 0; // 批量稳定性（1-5分）
	private String p_ReviewComment = null; // 评审意见

    @Override  
    protected void prepare() {  
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			String name = p.getParameterName();
			if (p.getParameter() == null)
				continue;
			switch (name) {
			case "designrestorationscore":
				p_DesignRestorationScore = p.getParameterAsInt();
				break;
			case "processeffectscore":
				p_ProcessEffectScore = p.getParameterAsInt();
				break;
			case "massproductionscore":
				p_MassProductionScore = p.getParameterAsInt();
				break;
			case "costcontrolscore":
				p_CostControlScore = p.getParameterAsInt();
				break;
			case "appearancescore":
				p_AppearanceScore = p.getParameterAsInt();
				break;
			case "draftrestorationscore":
				p_DraftRestorationScore = p.getParameterAsInt();
				break;
			case "physicochemicalscore":
				p_PhysicochemicalScore = p.getParameterAsInt();
				break;
			case "massstabilityscore":
				p_MassStabilityScore = p.getParameterAsInt();
				break;
			case "reviewcomment":
				p_ReviewComment = p.getParameterAsString();
				break;
			default:
				break;
			}
		}
    }  
  
    @Override  
    protected String doIt() throws Exception {  
  
		// ==================== 1. 从 T_Selection 获取信息窗口选中的需求记录 ====================
		// AD_Table_ID=1000171 对应 dy_samplingdemand，T_Selection_ID 存的是
		// dy_samplingdemand_id
		int[] ids = DB.getIDsEx(get_TrxName(), "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
				getAD_PInstance_ID());

		if (ids == null || ids.length == 0)
			throw new AdempiereException("请先选择一条需求记录");
		if (ids.length > 1)
			throw new AdempiereException("每次只能选择一条需求记录进行评审");

		int demandId = ids[0];

		// ==================== 2. 加载打样需求记录 ====================
		PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(demandId, get_TrxName());
        if (demand == null || demand.get_ID() == 0)  
			throw new AdempiereException("打样需求记录不存在，ID=" + demandId);

		// ==================== 3. 查找该需求下的样品评审单 ====================
		int reviewId = DB.getSQLValue(get_TrxName(), "SELECT dy_samplereview_id FROM adempiere.dy_samplereview "
				+ "WHERE dy_samplingdemand_id=? AND isactive='Y'", demandId);

		if (reviewId <= 0)
			throw new AdempiereException("该需求下不存在样品评审单，无法提交评审");

		// ==================== 3.5 校验样品评审单状态必须为"评审中" ====================
		PO review = MTable.get(getCtx(), "dy_samplereview").getPO(reviewId, get_TrxName());
		String reviewStatus = (String) review.get_Value("reviewstatus");

		// 样品评审单当前状态不是'评审中'，无法提交评审
		if (!"RV".equals(reviewStatus))
			throw new AdempiereException("样品已终审，无法再次提交评审");

		// ==================== 4. 校验当前用户是否为该评审单的审核人 ====================
		int currentUserId = getAD_User_ID();
		int reviewLineId = DB.getSQLValue(get_TrxName(),
				"SELECT dy_samplereviewline_id FROM adempiere.dy_samplereviewline "
						+ "WHERE dy_samplereview_id=? AND reviewer_id=? " + "AND isactive='Y' AND isfinalreview='N'",
				reviewId, currentUserId);

		if (reviewLineId <= 0)
			throw new AdempiereException("当前用户不是该样品评审单的审核人，无权提交评审");

		// ==================== 5. 加载评审行记录 ====================
		PO reviewLine = MTable.get(getCtx(), "dy_samplereviewline").getPO(reviewLineId, get_TrxName());
		if (reviewLine == null || reviewLine.get_ID() == 0)
			throw new AdempiereException("评审行记录不存在，ID=" + reviewLineId);
  
		// ==================== 6. 校验八个维度评分（1-5分）====================
		validateScore("设计还原度", p_DesignRestorationScore);
		validateScore("工艺效果", p_ProcessEffectScore);
		validateScore("批量生产可行性", p_MassProductionScore);
		validateScore("成本控制", p_CostControlScore);
		validateScore("外观效果", p_AppearanceScore);
		validateScore("样稿还原度", p_DraftRestorationScore);
		validateScore("理化数据", p_PhysicochemicalScore);
		validateScore("批量稳定性", p_MassStabilityScore);
  
		// ==================== 7. 计算总分 ====================
		int totalScore = p_DesignRestorationScore + p_ProcessEffectScore + p_MassProductionScore + p_CostControlScore
				+ p_AppearanceScore + p_DraftRestorationScore + p_PhysicochemicalScore + p_MassStabilityScore;
  
		// ==================== 8. 赋值并保存评审行记录 ====================
		reviewLine.set_ValueNoCheck("designrestorationscore", p_DesignRestorationScore);
		reviewLine.set_ValueNoCheck("processeffectscore", p_ProcessEffectScore);
		reviewLine.set_ValueNoCheck("massproductionscore", p_MassProductionScore);
		reviewLine.set_ValueNoCheck("costcontrolscore", p_CostControlScore);
		reviewLine.set_ValueNoCheck("appearancescore", p_AppearanceScore);
		reviewLine.set_ValueNoCheck("draftrestorationscore", p_DraftRestorationScore);
		reviewLine.set_ValueNoCheck("physicochemicalscore", p_PhysicochemicalScore);
		reviewLine.set_ValueNoCheck("massstabilityscore", p_MassStabilityScore);
		reviewLine.set_ValueNoCheck("totalscore", new BigDecimal(totalScore));
		reviewLine.set_ValueNoCheck("reviewcomment", p_ReviewComment);
		reviewLine.set_ValueNoCheck("reviewdate", new Timestamp(System.currentTimeMillis()));
		reviewLine.saveEx();
  
		return "评审提交成功，综合得分：" + totalScore + " 分";
	}
  
	/**
	 * 校验评分是否在 1-5 范围内
	 */
	private void validateScore(String label, int score) {
		if (score < 1 || score > 5)
			throw new IllegalArgumentException(label + " 评分必须在 1-5 之间，当前值：" + score);
    }  
}