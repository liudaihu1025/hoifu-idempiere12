package com.hoifu.process;

import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class DYProcessDesignReviewSubmit extends SvrProcess {

	// ==================== 需求状态常量 ====================
	private static final String DEMAND_STATUS_PROCESS_REVIEWING = "PR"; // 工艺设计评审中

	// ==================== 工艺设计状态常量 ====================
	private static final String DESIGN_STATUS_REVIEWING = "RV"; // 评审中
	private static final String DESIGN_STATUS_REVIEWED = "RE"; // 已评审

	// ==================== 评审结果常量 ====================
	private static final String REVIEW_RESULT_ADOPT = "AC"; // 采纳

	// ==================== 参数 ====================
	private String p_ReviewResult = REVIEW_RESULT_ADOPT; // 评审结果，默认采纳
	private String p_ReviewComment = null; // 评审意见

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("reviewresult".equals(name)) {
				p_ReviewResult = para.getParameterAsString();
				if (p_ReviewResult == null || p_ReviewResult.trim().isEmpty())
					p_ReviewResult = REVIEW_RESULT_ADOPT;
			} else if ("reviewcomment".equals(name)) {
				p_ReviewComment = para.getParameterAsString();
			}
		}
	}

	@Override
	protected String doIt() throws Exception {

		int processDesignId = getRecord_ID();
		if (processDesignId <= 0)
			throw new IllegalArgumentException("未找到工艺设计记录");

		int currentUserId = getAD_User_ID();

		// ==================== 1. 校验：当前用户是否为评审人 ====================
		int reviewCount = DB.getSQLValue(get_TrxName(),
				"SELECT COUNT(*) FROM adempiere.dy_processdesignreview "
						+ "WHERE dy_processdesign_id=? AND reviewer_id=? " + "AND isactive='Y' AND isfinalreview='N'",
				processDesignId, currentUserId);
		if (reviewCount <= 0)
			throw new IllegalArgumentException("当前用户不是该工艺设计的评审人，或评审记录不存在");

		// ==================== 2. 加载工艺设计记录 ====================
		PO processDesign = MTable.get(getCtx(), "dy_processdesign").getPO(processDesignId, get_TrxName());
		if (processDesign == null || processDesign.get_ID() <= 0)
			throw new IllegalArgumentException("工艺设计记录不存在，ID=" + processDesignId);

		// ==================== 3. 校验工艺设计状态=评审中 ====================
		String processStatus = (String) processDesign.get_Value("processstatus");
		if (!DESIGN_STATUS_REVIEWING.equals(processStatus))
			throw new IllegalArgumentException("工艺设计状态不是'评审中'，当前状态：" + processStatus);

		// ==================== 4. 加载需求记录并校验状态 ====================
		int samplingDemandId = ((Number) processDesign.get_Value("dy_samplingdemand_id")).intValue();
		PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(samplingDemandId, get_TrxName());
		if (demand == null || demand.get_ID() <= 0)
			throw new IllegalArgumentException("需求记录不存在，ID=" + samplingDemandId);

		String requestStatus = (String) demand.get_Value("requeststatus");
		if (!DEMAND_STATUS_PROCESS_REVIEWING.equals(requestStatus))
			throw new IllegalArgumentException("需求状态不是'工艺设计评审中'，当前状态：" + requestStatus);

		// ==================== 5. 查询当前用户的所有有效非终审评审记录 ====================
		int[] reviewIds = DB.getIDsEx(get_TrxName(),
				"SELECT dy_processdesignreview_id FROM adempiere.dy_processdesignreview "
						+ "WHERE dy_processdesign_id=? AND reviewer_id=? " + "AND isactive='Y' AND isfinalreview='N'",
				processDesignId, currentUserId);
		if (reviewIds == null || reviewIds.length == 0)
			throw new IllegalArgumentException("未找到当前用户的有效评审记录");

		// ==================== 6. 更新所有评审记录 ====================
		Timestamp now = new Timestamp(System.currentTimeMillis());
		for (int reviewId : reviewIds) {
			PO review = MTable.get(getCtx(), "dy_processdesignreview").getPO(reviewId, get_TrxName());
			review.set_ValueNoCheck("reviewresult", p_ReviewResult);
			review.set_ValueNoCheck("reviewdate", now);
			review.set_ValueNoCheck("description", p_ReviewComment);
			review.saveEx();
		}

		// ==================== 7. 检查是否所有普通评审人都已完成评审 ====================
		int pendingCount = DB.getSQLValue(get_TrxName(), "SELECT COUNT(*) FROM adempiere.dy_processdesignreview "
				+ "WHERE dy_processdesign_id=? AND isactive='Y' " + "AND isfinalreview='N' AND reviewresult IS NULL",
				processDesignId);

		if (pendingCount == 0) {
			// 所有普通评审人均已完成，更新工艺设计状态=已评审
			processDesign.set_ValueNoCheck("processstatus", DESIGN_STATUS_REVIEWED);
			processDesign.saveEx();
		}

		return "评审提交成功，共更新 " + reviewIds.length + " 条评审记录" + (pendingCount == 0 ? "，所有评审人已完成，工艺设计进入已评审状态" : "");
	}
}