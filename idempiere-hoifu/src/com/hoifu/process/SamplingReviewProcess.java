package com.hoifu.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.MDYSamplingReviewLine;
import com.hoifu.model.MDYSamplingReviewResult;
import com.hoifu.utils.DYSamplingUtil;

/**
 * 打样评审流程 - 参与人评审 / 总监终审（复用同一流程类）
 * 
 * <p>
 * 支持两种阶段类型：
 * <ul>
 * <li>设计评审（非 EN）：参与人录入评分（0-100），全员评分后计算平均分，状态置为 RD</li>
 * <li>工艺设计评审（EN）：参与人直接录入 AC/NA；有任意 NA 则明细判定为 NA； 所有最新结果均为 AC 时，明细判定为
 * AC；全员评完后状态置为 RD</li>
 * </ul>
 * 
 * <p>
 * 总监终审逻辑两种类型相同：录入 AC/NA，明细状态置为 FA。
 * 
 * <p>
 * 通用逻辑（评审意见追加留痕、旧记录设为 isactive='N'）由 {@link MDYSamplingReviewResult#afterSave}
 * 负责。
 */
@Process
public class SamplingReviewProcess extends SvrProcess {

	private String p_ReviewComment;
	private BigDecimal p_ReviewScore;
	/** 总监或工艺评审参与人录入的采纳结果：AC=采纳，NA=不采纳 */
	private String p_ReviewResult;

	/** 设计评审得分采纳阈值：>= 80 → 采纳（AC），否则 → 不采纳（NA） */
	private static final BigDecimal ADOPT_THRESHOLD = new BigDecimal("80");

	/** 工艺设计阶段类型标识 */
	private static final String PHASE_TYPE_EN = "EN";

	/** 生产打样阶段类型标识 */
	private static final String PHASE_TYPE_PR = "PR";

	// ==================== 参数解析 ====================

	@Override
	protected void prepare() {
		for (ProcessInfoParameter p : getParameter()) {
			String name = p.getParameterName();
			if (p.getParameter() == null)
				continue;
			if ("reviewcomment".equals(name))
				p_ReviewComment = (String) p.getParameter();
			else if ("reviewscore".equals(name))
				p_ReviewScore = p.getParameterAsBigDecimal();
			else if ("reviewresult".equals(name))
				p_ReviewResult = (String) p.getParameter();
		}
	}

	// ==================== 主流程 ====================

	@Override
	protected String doIt() throws Exception {

		// 1. 获取选中的评审明细 ID
		int[] ids = DB.getIDsEx(get_TrxName(), "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
				getAD_PInstance_ID());

		if (ids == null || ids.length != 1)
			throw new AdempiereException("请只选择一条评审明细记录");

		int reviewLineId = ids[0];

		// 2. 加载评审明细
		MDYSamplingReviewLine line = new MDYSamplingReviewLine(getCtx(), reviewLineId, get_TrxName());
		if (line.get_ID() <= 0)
			throw new AdempiereException("评审明细记录不存在");

		// 3. 判断角色，校验状态
		boolean isDirector = DYSamplingUtil.hasJobPosition(Env.getAD_User_ID(getCtx()), "J", "6");
		validateStatus(line, isDirector);

		// 4. 获取主表 ID 及阶段类型
		int reviewId = line.get_ValueAsInt("dy_samplingreview_id");
		String phasetype = DB.getSQLValueString(get_TrxName(),
				"SELECT phasetype FROM dy_samplingreview WHERE dy_samplingreview_id=?", reviewId);

		// 5. 校验角色相关参数
		validateParams(isDirector, phasetype);

		// 6. 计算本次评审结果
		String reviewResult = calcReviewResult(isDirector, phasetype);

		// 7. 新建打样评审结果记录
		createReviewResult(reviewLineId, reviewId, reviewResult, isDirector, phasetype);

		// 8. 重新加载评审明细（afterSave 已更新 reviewcomment）
		line = new MDYSamplingReviewLine(getCtx(), reviewLineId, get_TrxName());

		// 9. 角色分支：更新明细字段
		if (isDirector) {
			doDirectorReview(line, phasetype);
		} else {
			doParticipantReview(line, reviewLineId, reviewId, phasetype);
		}

		// 10. 保存评审明细
		line.saveEx();

		return "";
	}

	// ==================== 状态校验 ====================

	/**
	 * 根据角色校验评审明细当前状态是否允许操作。
	 */
	private void validateStatus(MDYSamplingReviewLine line, boolean isDirector) {
		String status = line.getreviewstatus();
		if (isDirector) {
			if ("CL".equals(status))
				throw new AdempiereException("评审明细已关闭，无法进行终审操作");
		} else {
			if ("FA".equals(status) || "CL".equals(status))
				throw new AdempiereException("评审明细已终审或已关闭，无法提交评审结果");
		}
	}

	// ==================== 参数校验 ====================

	/**
	 * 根据角色和阶段类型校验流程参数合法性。
	 * 
	 * <ul>
	 * <li>总监：必须选择 AC/NA</li>
	 * <li>工艺评审（EN）参与人：必须选择 AC/NA，不需要评分</li>
	 * <li>设计评审参与人：必须录入 0-100 的评分</li>
	 * </ul>
	 */
	private void validateParams(boolean isDirector, String phasetype) {
		if (isDirector || PHASE_TYPE_EN.equals(phasetype)) {
			if (!"AC".equals(p_ReviewResult) && !"NA".equals(p_ReviewResult))
				throw new AdempiereException("请选择采纳结果（采纳/不采纳）");
		} else {
			if (p_ReviewScore == null || p_ReviewScore.compareTo(BigDecimal.ZERO) < 0
					|| p_ReviewScore.compareTo(new BigDecimal("100")) > 0)
				throw new AdempiereException("评审得分必须在 0 到 100 之间");
		}
		// EN 参与人评审意见必填
		if (!isDirector && PHASE_TYPE_EN.equals(phasetype)) {
			if (p_ReviewComment == null || p_ReviewComment.trim().isEmpty())
				throw new AdempiereException("请录入评审意见");
		}
	}

	// ==================== 评审结果计算 ====================

	/**
	 * 根据角色和阶段类型计算本次评审结果值。
	 * 
	 * <ul>
	 * <li>总监 / 工艺评审参与人：直接取 p_ReviewResult</li>
	 * <li>设计评审参与人：按得分与阈值比较</li>
	 * </ul>
	 */
	private String calcReviewResult(boolean isDirector, String phasetype) {
		if (isDirector || PHASE_TYPE_EN.equals(phasetype))
			return p_ReviewResult;
		return p_ReviewScore.compareTo(ADOPT_THRESHOLD) >= 0 ? "AC" : "NA";
	}

	// ==================== 新建评审结果记录 ====================

	/**
	 * 新建一条 {@link MDYSamplingReviewResult} 并保存。 工艺评审（EN）参与人和总监不写 reviewscore。
	 */
	private void createReviewResult(int reviewLineId, int reviewId, String reviewResult, boolean isDirector,
			String phasetype) {
		MDYSamplingReviewResult result = new MDYSamplingReviewResult(getCtx(), 0, get_TrxName());
		result.setdy_samplingreviewline_ID(reviewLineId);
		result.setdy_samplingreview_ID(reviewId);
		result.setreviewcomment(p_ReviewComment);
		result.setreviewresult(reviewResult);
		result.setAD_User_ID(getAD_User_ID());
		result.setreviewdate(new Timestamp(System.currentTimeMillis()));
		if (!isDirector && !PHASE_TYPE_EN.equals(phasetype)) {
			result.setreviewscore(p_ReviewScore); // 设计评审参与人录入评分
		}
		result.saveEx();
	}

	// ==================== 总监终审 ====================

	/**
	 * 总监终审：将明细状态置为 FA，记录终审结果和终审日期。 两种阶段类型逻辑相同。
	 * 
	 * @param line 已重新加载的评审明细
	 */
	private void doDirectorReview(MDYSamplingReviewLine line, String phasetype) {
		line.setreviewstatus("FA");
		line.setreviewresult(p_ReviewResult);
		line.setfinalreviewdate(new Timestamp(System.currentTimeMillis()));

		// 工艺设计评审（EN）和样品评审（PR）需要同步事项的评审结果和评审状态
		if (PHASE_TYPE_EN.equals(phasetype) || PHASE_TYPE_PR.equals(phasetype)) {
			int taskLineId = line.get_ValueAsInt("dy_samplingtaskline_id");
			if (taskLineId > 0) {
				DB.executeUpdateEx(
						"UPDATE dy_samplingtaskline "
								+ "SET reviewresult=?, itemstatus='FA', updated=now(), updatedby=? "
								+ "WHERE dy_samplingtaskline_id=?",
						new Object[] { p_ReviewResult, getAD_User_ID(), taskLineId }, get_TrxName());
			}
		}
	}

	// ==================== 参与人评审（分发） ====================

	/**
	 * 参与人评审入口：根据阶段类型分发到对应逻辑。
	 */
	private void doParticipantReview(MDYSamplingReviewLine line, int reviewLineId, int reviewId, String phasetype) {
		line.setreviewdate(new Timestamp(System.currentTimeMillis()));

		if (PHASE_TYPE_EN.equals(phasetype)) {
			doParticipantReviewEN(line, reviewLineId, reviewId);
		} else {
			doParticipantReviewDesign(line, reviewLineId, reviewId);
		}
	}
  
	// ==================== 工艺设计评审（EN）参与人逻辑 ====================
  
	/**
	 * 工艺设计评审参与人逻辑：
	 * <ul>
	 * <li>有任意参与人录入"不采纳"（NA）→ 明细结果 = NA</li>
	 * <li>所有最新有效结果均为"采纳"（AC）→ 明细结果 = AC</li>
	 * <li>所有参与人均已评审 → 状态置为 RD（已评审）</li>
	 * <li>尚有参与人未评审 → 状态保持 RV（评审中）</li>
	 * </ul>
	 * 依赖 {@link MDYSamplingReviewResult#afterSave} 将同一用户的旧记录置为 isactive='N'，
	 * 保证此处查询到的均为各参与人最新一条结果。
	 * 
	 * @param line         已重新加载的评审明细
	 * @param reviewLineId 评审明细 ID
	 * @param reviewId     评审主表 ID（用于查询 reviewers 列表）
	 */
	private void doParticipantReviewEN(MDYSamplingReviewLine line, int reviewLineId, int reviewId) {
		// 1. 根据当前投票实时更新明细结果（不等全员完成）
		int naCount = DB.getSQLValue(get_TrxName(), "SELECT COUNT(1) FROM dy_samplingreviewresult "
				+ "WHERE dy_samplingreviewline_id=? AND isactive='Y' AND reviewresult='NA'", reviewLineId);
		line.setreviewresult(naCount > 0 ? "NA" : "AC");
  
		// 2. 任意参与人评审后立即置为已评审，无需等全员完成
		line.setreviewstatus("RD");
	}
  
	// ==================== 设计评审参与人逻辑 ====================

	/**
	 * 设计评审参与人逻辑：每次评审后实时计算平均分并更新明细结果；全员评分后状态置为 RD。
	 * 
	 * @param line         已重新加载的评审明细
	 * @param reviewLineId 评审明细 ID
	 * @param reviewId     评审主表 ID
	 */
	private void doParticipantReviewDesign(MDYSamplingReviewLine line, int reviewLineId, int reviewId) {
		// 每次评审后实时更新平均分和结果（不等全员完成）
		updateAverageScore(line, reviewLineId);

		// 只有全员评完，才将状态置为已评审
		if (allParticipantsReviewed(reviewLineId, reviewId)) {
			line.setreviewstatus("RD");
		}
		// 否则状态保持 RV，等待其他参与人
	}
  
	/**
	 * 判断主表 reviewers 字段中所有参与人是否均已提交有效评审结果。
	 */
	private boolean allParticipantsReviewed(int reviewLineId, int reviewId) {
		String reviewers = DB.getSQLValueString(get_TrxName(),
				"SELECT reviewers FROM dy_samplingreview WHERE dy_samplingreview_id=?", reviewId);
  
		if (reviewers == null || reviewers.trim().isEmpty())
			return false;
  
		for (String rid : reviewers.split(",")) {
			rid = rid.trim();
			if (rid.isEmpty())
				continue;
			int count = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(1) FROM dy_samplingreviewresult "
							+ "WHERE dy_samplingreviewline_id=? AND ad_user_id=? AND isactive='Y'",
					reviewLineId, Integer.parseInt(rid));
			if (count == 0)
				return false;
		}
		return true;
	}
  
	/**
	 * 查询所有有效评审结果的平均分，并更新到明细的 reviewscore / reviewresult 字段。
	 */
	private void updateAverageScore(MDYSamplingReviewLine line, int reviewLineId) {
		BigDecimal avgScore = DB.getSQLValueBD(get_TrxName(),
				"SELECT AVG(rr.reviewscore) FROM dy_samplingreviewresult rr "
						+ "WHERE rr.dy_samplingreviewline_id=? AND rr.isactive='Y' AND rr.reviewscore IS NOT NULL",
				reviewLineId);
  
		if (avgScore != null) {
			line.setreviewscore(avgScore.setScale(2, RoundingMode.HALF_UP));
			line.setreviewresult(avgScore.compareTo(ADOPT_THRESHOLD) >= 0 ? "AC" : "NA");
		}
	}
}