package com.hoifu.process;  
  
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
  
@org.adempiere.base.annotation.Process  
public class DYSampleFinalReviewSubmit extends SvrProcess {  
  
	// ==================== 状态常量 ====================
	private static final String DEMAND_STATUS_CLOSED = "CL"; // 已关闭
	private static final String REVIEW_STATUS_REVIEWED = "RE"; // 已评审

	// ==================== 参数 ====================
	private String p_Description = null; // 终审意见

    @Override  
    protected void prepare() {  
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			String name = p.getParameterName();
			if (p.getParameter() == null)
				continue;
			switch (name) {
			case "Description":
				p_Description = p.getParameterAsString();
				break;
			default:
				break;
			}
		}
    }  
  
    @Override  
    protected String doIt() throws Exception {  
  
		// ==================== 1. 从 T_Selection 获取信息窗口选中的需求记录 ====================
		int[] ids = DB.getIDsEx(get_TrxName(), "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
				getAD_PInstance_ID());

		if (ids == null || ids.length == 0)
			throw new AdempiereException("请先选择一条需求记录");
		if (ids.length > 1)
			throw new AdempiereException("每次只能选择一条需求记录进行终审");

		int demandId = ids[0];

		// ==================== 2. 加载打样需求记录 ====================
		PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(demandId, get_TrxName());
        if (demand == null || demand.get_ID() == 0)  
			throw new AdempiereException("打样需求记录不存在，ID=" + demandId);

		// ==================== 3. 查找该需求下的样品评审单 ====================
		int reviewId = DB.getSQLValue(get_TrxName(), "SELECT dy_samplereview_id FROM adempiere.dy_samplereview "
				+ "WHERE dy_samplingdemand_id=? AND isactive='Y'", demandId);

		if (reviewId <= 0)
			throw new AdempiereException("该需求下不存在样品评审单，无法提交终审");

		// ==================== 4. 加载样品评审单 ====================
		PO review = MTable.get(getCtx(), "dy_samplereview").getPO(reviewId, get_TrxName());
		if (review == null || review.get_ID() == 0)
			throw new AdempiereException("样品评审单不存在，ID=" + reviewId);

		// ==================== 4.5 校验评审单状态必须为"评审中" ====================
		String reviewStatus = (String) review.get_Value("reviewstatus");
		if (!"RV".equals(reviewStatus))
			throw new AdempiereException("样品评审单当前状态不是'评审中'，无法提交终审");

		// ==================== 5. 校验当前用户是否为终审人 ====================
		int currentUserId = getAD_User_ID();
		Object finalReviewerObj = review.get_Value("finalreviewer_id");
		if (finalReviewerObj == null)
			throw new AdempiereException("该样品评审单未指定终审人，无法提交终审");

		int finalReviewerId = ((Number) finalReviewerObj).intValue();
		if (finalReviewerId != currentUserId)
			throw new AdempiereException("当前用户不是该样品评审单的终审人，无权提交终审");

		// ==================== 6. 查找终审人的评审行记录 ====================
		int reviewLineId = DB.getSQLValue(get_TrxName(),
				"SELECT dy_samplereviewline_id FROM adempiere.dy_samplereviewline "
						+ "WHERE dy_samplereview_id=? AND reviewer_id=? " + "AND isfinalreview='Y' AND isactive='Y'",
				reviewId, currentUserId);

		if (reviewLineId <= 0)
			throw new AdempiereException("未找到当前用户的终审行记录，无法提交终审");
  
		// ==================== 7. 加载终审行记录 ====================
		PO reviewLine = MTable.get(getCtx(), "dy_samplereviewline").getPO(reviewLineId, get_TrxName());
		if (reviewLine == null || reviewLine.get_ID() == 0)
			throw new AdempiereException("终审行记录不存在，ID=" + reviewLineId);
  
		// ==================== 8. 赋值终审意见到评审行 ====================
		reviewLine.set_ValueNoCheck("reviewcomment", p_Description);
		reviewLine.set_ValueNoCheck("reviewdate", new Timestamp(System.currentTimeMillis()));
		reviewLine.saveEx();
  
		// ==================== 9. 赋值终审意见到评审单，更新评审状态 ====================
		review.set_ValueNoCheck("finalreviewcomment", p_Description);
		review.set_ValueNoCheck("reviewstatus", REVIEW_STATUS_REVIEWED);
		review.saveEx();
  
		// ==================== 10. 更新打样需求状态为已关闭 ====================
		demand.set_ValueNoCheck("requeststatus", DEMAND_STATUS_CLOSED);
		demand.saveEx();
  
		return "终审提交成功，需求已关闭，样品评审状态已更新为已评审";
    }  
}