package com.hoifu.process;

import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class DYProcessDesignFinalReviewSubmit extends SvrProcess {

	// ==================== 工艺设计状态常量 ====================
	private static final String PROCESS_STATUS_COMPLETED = "CO"; // 已完成

	// ==================== 需求状态常量 ====================
	private static final String DEMAND_STATUS_PROCESS_REVIEWING = "PR"; // 工艺设计评审中
	private static final String DEMAND_STATUS_COMPLETED = "CO"; // 已完成

	// ==================== 事项状态常量 ====================
	private static final String ITEM_STATUS_REVIEWED = "RE"; // 已评审

	// ==================== 评审结果常量 ====================
	private static final String REVIEW_RESULT_ADOPT = "AC"; // 采纳

	// ==================== 参数 ====================
	private String p_ReviewResult;
	private String p_ReviewComment;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("reviewresult".equals(name)) {
				p_ReviewResult = para.getParameterAsString();
			} else if ("reviewcomment".equals(name)) {
				p_ReviewComment = para.getParameterAsString();
			}
		}
		// 默认采纳
		if (p_ReviewResult == null || p_ReviewResult.trim().isEmpty()) {
			p_ReviewResult = REVIEW_RESULT_ADOPT;
		}
	}

	@Override
	protected String doIt() throws Exception {

		int processDesignId = getRecord_ID();
		int currentUserId = getAD_User_ID();

		// ==================== 1. 加载工艺设计记录 ====================
		PO processDesign = MTable.get(getCtx(), "dy_processdesign").getPO(processDesignId, get_TrxName());
		if (processDesign == null || processDesign.get_ID() <= 0)
			throw new IllegalArgumentException("未找到工艺设计记录，ID=" + processDesignId);

		// ==================== 2. 校验：当前用户是否为终审人 ====================
		Object finalReviewerObj = processDesign.get_Value("finalreviewer_id");
		int finalReviewerId = finalReviewerObj != null ? ((Number) finalReviewerObj).intValue() : 0;
		if (finalReviewerId != currentUserId)
			throw new IllegalArgumentException("当前用户不是该工艺设计的终审人，无权提交终审");

		// ==================== 3. 加载需求记录 ====================
		Object demandIdObj = processDesign.get_Value("dy_samplingdemand_id");
		int samplingDemandId = demandIdObj != null ? ((Number) demandIdObj).intValue() : 0;
		PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(samplingDemandId, get_TrxName());
		if (demand == null || demand.get_ID() <= 0)
			throw new IllegalArgumentException("未找到关联的打样需求记录");

		// ==================== 4. 校验：需求状态=工艺设计评审中PR ====================
		String requestStatus = (String) demand.get_Value("requeststatus");
		if (!DEMAND_STATUS_PROCESS_REVIEWING.equals(requestStatus))
			throw new IllegalArgumentException("需求状态不是'工艺设计评审中'，当前状态：" + requestStatus);

		// ==================== 5. 查找当前用户的终审记录（可能有多条，每个事项一条）====================
		int[] reviewIds = DB.getIDsEx(get_TrxName(),
				"SELECT dy_processdesignreview_id FROM adempiere.dy_processdesignreview "
						+ "WHERE dy_processdesign_id=? AND reviewer_id=? AND isfinalreview='Y' AND isactive='Y'",
				processDesignId, currentUserId);
		if (reviewIds == null || reviewIds.length == 0)
			throw new IllegalArgumentException("未找到当前用户的终审记录");

		// ==================== 6. 更新终审记录：评审结果、评审时间、评审意见 ====================
		Timestamp now = new Timestamp(System.currentTimeMillis());
		for (int reviewId : reviewIds) {
			PO review = MTable.get(getCtx(), "dy_processdesignreview").getPO(reviewId, get_TrxName());
			review.set_ValueNoCheck("reviewresult", p_ReviewResult);
			review.set_ValueNoCheck("reviewdate", now);
			review.set_ValueNoCheck("description", p_ReviewComment);
			review.saveEx();
		}

		// ==================== 7. 更新所有事项状态=已评审RE ====================
		DB.executeUpdateEx(
				"UPDATE adempiere.dy_processdesignitem " + "SET itemstatus='" + ITEM_STATUS_REVIEWED
						+ "', updated=now(), updatedby=? " + "WHERE dy_processdesign_id=? AND isactive='Y'",
				new Object[] { currentUserId, processDesignId }, get_TrxName());

		// ==================== 8. 更新工艺设计状态=已完成CO ====================
		processDesign.set_ValueNoCheck("processstatus", PROCESS_STATUS_COMPLETED);
		processDesign.saveEx();

		// ==================== 9. 更新需求状态=已完成CO ====================
		demand.set_ValueNoCheck("requeststatus", DEMAND_STATUS_COMPLETED);
		demand.saveEx();

		return "终审完成，结果：采纳，共更新 " + reviewIds.length + " 条终审记录，工艺设计已完成";
	}
}