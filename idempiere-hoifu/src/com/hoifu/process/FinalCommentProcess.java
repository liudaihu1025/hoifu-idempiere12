package com.hoifu.process;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.hoifu.model.MDYSamplingReviewLine;
import com.hoifu.model.MDYSamplingReviewResult;
import com.hoifu.model.X_dy_samplingreview;
import com.hoifu.utils.DYSamplingUtil;

/**
 * 终审意见写入流程
 * 
 * <p>
 * 从信息窗口《打样评审》中选中一条评审明细后触发，录入终审意见，新建一条【打样评审结果】记录，
 * 并将终审意见拼接到对应表头（dy_samplingreview）的 finalcomment 字段。
 * 
 * <p>
 * 仅总监（临时方案：AD_User_ID=1000377）可操作。 校验条件：
 * <ul>
 * <li>《打样评审》状态不能为"已关闭"（CL）</li>
 * <li>该评审单下所有明细须均已终审（FA）方可录入终审意见</li>
 * </ul>
 * 
 * <p>
 * 配置：在 iDempiere 中将此流程挂载到信息窗口《打样评审明细》， 并配置参数 finalcomment（Text，必填）。
 */
@Process
public class FinalCommentProcess extends SvrProcess {

	/** 终审意见 */
	private String p_FinalComment;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter p : getParameter()) {
			String name = p.getParameterName();
			if ("finalcomment".equals(name))
				p_FinalComment = (String) p.getParameter();
		}
	}

	@Override
	protected String doIt() throws Exception {

		// ==================== 1. 权限校验：只有总监可操作 ====================
		if (!DYSamplingUtil.hasJobPosition(getAD_User_ID(), "J", "6"))
			throw new AdempiereException("只有总监才能录入终审意见");
  
		// ==================== 2. 获取选中的评审明细 ID（仅允许单选）====================
		int[] ids = DB.getIDsEx(get_TrxName(), "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
				getAD_PInstance_ID());
		if (ids == null || ids.length != 1)
			throw new AdempiereException("请只选择一条评审明细记录");
		int reviewLineId = ids[0];
  
		// ==================== 3. 加载评审明细 ====================
		MDYSamplingReviewLine line = new MDYSamplingReviewLine(getCtx(), reviewLineId, get_TrxName());
		if (line.get_ID() <= 0)
			throw new AdempiereException("评审明细不存在，ID=" + reviewLineId);
  
		// ==================== 4. 校验终审意见非空 ====================
		if (p_FinalComment == null || p_FinalComment.trim().isEmpty())
			throw new AdempiereException("终审意见不能为空");
  
		// ==================== 5. 获取主表 ID ====================
		int reviewId = line.getdy_samplingreview_ID();
		if (reviewId <= 0)
			throw new AdempiereException("评审明细未关联评审主表");
  
		// ==================== 5.1 校验：主表状态不能是已关闭（CL）====================
		String reviewStatus = DB.getSQLValueString(get_TrxName(),
				"SELECT reviewstatus FROM dy_samplingreview WHERE dy_samplingreview_id=?", reviewId);
		if ("CL".equals(reviewStatus))
			throw new AdempiereException("该评审单已关闭，无法录入终审意见");
  
		// ==================== 5.5 校验：表头下所有明细必须已终审（FA）====================
		int notFACount = DB.getSQLValue(get_TrxName(), "SELECT COUNT(1) FROM dy_samplingreviewline "
				+ "WHERE dy_samplingreview_id=? AND reviewstatus <> 'FA' AND isactive='Y'", reviewId);
		if (notFACount > 0)
			throw new AdempiereException("该评审单下还有 " + notFACount + " 条明细未完成终审，无法录入终审意见");
  
		// ==================== 6. 新建打样评审结果记录 ====================
		MDYSamplingReviewResult result = new MDYSamplingReviewResult(getCtx(), 0, get_TrxName());
		result.setdy_samplingreviewline_ID(reviewLineId); // 关联评审明细
		result.setdy_samplingreview_ID(reviewId); // 关联评审主表
		result.setAD_User_ID(getAD_User_ID()); // 评审人 = 当前登录用户（总监）
		result.setreviewdate(new Timestamp(System.currentTimeMillis())); // 评审时间 = 当前时间
		result.setfinalcomment(p_FinalComment); // 终审意见存入 result.finalcomment
		// reviewcomment 不赋值（null），afterSave 会跳过 line.reviewcomment 的拼接
		result.saveEx();
  
		// ==================== 7. 拼接终审意见到主表 finalcomment，更新审核人和审核时间
		// ====================
		X_dy_samplingreview review = new X_dy_samplingreview(getCtx(), reviewId, get_TrxName());
		String reviewer = "";
		MUser user = MUser.get(getCtx(), getAD_User_ID());
		if (user != null)
			reviewer = user.getName();
		String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
		String newEntry = dateStr + "_" + reviewer + "\n" + p_FinalComment;
		String existing = review.getfinalcomment();
		String merged = (existing == null || existing.trim().isEmpty()) ? newEntry : newEntry + "\n\n" + existing;
		review.setfinalcomment(merged);
		review.setApprover_ID(getAD_User_ID()); // 审核人 = 当前登录用户（总监）
		review.setapprovedate(new Timestamp(System.currentTimeMillis())); // 审核时间 = 当前时间
		review.saveEx();
  
		return "";
	}
  
}