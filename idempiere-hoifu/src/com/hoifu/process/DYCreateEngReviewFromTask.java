package com.hoifu.process;

import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.X_dy_samplingphase;
import com.hoifu.model.X_dy_samplingrequest;
import com.hoifu.model.X_dy_samplingreview;
import com.hoifu.model.X_dy_samplingreviewline;
import com.hoifu.model.X_dy_samplingtask;
import com.hoifu.model.X_dy_samplingtaskline;

/**
 * 从工艺任务生成工艺评审单
 * 
 * 
 * 流程逻辑：
 * 
 * 从流程参数 {@code dy_samplingtask_ID} 获取工艺任务 ID
 * 通过任务找到所属阶段（dy_samplingphase）和需求单（dy_samplingrequest）， 用于填充评审单主表的必填字段 解析任务的
 * selectedvalue 字段，查询对应评审明细的效果编码， 拼接为"红塔山1/红塔山2"格式，作为评审明细的统一效果编码
 * 查询该任务下所有有效事项（dy_samplingtaskline），若无事项则终止
 * 新建评审单主表（dy_samplingreview），关联工艺任务、阶段、需求单
 * 按事项逐条生成评审明细（dy_samplingreviewline），关联对应事项， 效果编码统一使用任务的 selectedvalue 拼接结果
 * 
 * 
 * 
 * 触发方式：在工艺任务窗口的按钮上绑定此流程，流程通过参数 {@code dy_samplingtask_ID} 接收当前任务 ID。
 * 
 * 
 * AD_Process.Classname = com.hoifu.process.DYCreateEngReviewFromTask
 */
@Process
public class DYCreateEngReviewFromTask extends SvrProcess {

	/**
	 * 参数：工艺任务 ID。 ， 框架在触发流程时自动将当前任务 ID 传入。
	 */
	private int p_TaskID = 0;

	/**
	 * 参数：评审参与人。 dy_samplingreview.reviewers 字段。
	 */
	private String p_Reviewers;

	/**
	 * 参数：提交备注。 dy_samplingreview.submitremark 字段。
	 */
	private String p_SubmitRemark;

	// ── 结果字段，用于 postProcess 跳转 ──────────────────────────────
	/** 新建评审单的主键 ID，用于 postProcess 中生成跳转链接 */
	private int m_reviewID = 0;
	/** 新建评审单的单据编号，用于 doIt 返回消息 */
	private String m_finalDocNo = "";
	/** 实际生成的评审明细行数，用于 doIt 返回消息 */
	private int m_lineCount = 0;

	/**
	 * 读取流程参数。 iDempiere 框架在 {@link #doIt()} 执行前自动调用此方法。
	 */
	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("dy_samplingtask_ID".equals(name)) {
				// 读取工艺任务 ID 参数（整数类型）
				p_TaskID = para.getParameterAsInt();
			} else if ("reviewers".equals(name)) {
				// 读取评审参与人参数（字符串类型）
				p_Reviewers = para.getParameterAsString();
			} else if ("submitremark".equals(name)) {
				// 读取提交备注参数（字符串类型）
				p_SubmitRemark = para.getParameterAsString();
			}
		}
	}

	/**
	 * 流程主逻辑。
	 * 
	 * @return 执行结果描述，显示在流程结果面板的第一行
	 * @throws Exception 任何业务校验失败或数据库操作异常时抛出，框架会回滚事务
	 */
	@Override
	protected String doIt() throws Exception {

		// ── 1. 获取当前工艺任务 ──────────────────────────────────────
		// p_TaskID 来自流程参数 dy_samplingtask_ID，由框架在触发流程时传入。
		// 若参数未传入（值为 0），则报错提示。
		int taskId = p_TaskID;
		if (taskId <= 0)
			throw new AdempiereException("未找到工艺任务");

		X_dy_samplingtask task = new X_dy_samplingtask(getCtx(), taskId, get_TrxName());
		if (task.get_ID() <= 0)
			throw new AdempiereException("任务不存在：" + taskId);

		// 校验任务状态：只有已完成（CO）的任务才能发起评审
		if (!"CO".equals(task.getTaskStatus()))
			throw new AdempiereException("任务【" + task.getName() + "】状态不是已完成，无法发起评审");

		// 校验评审人
		if (p_Reviewers == null || p_Reviewers.trim().isEmpty())
			throw new AdempiereException("评审人不能为空");

		// ── 2. 获取阶段和需求单（评审单主表的必填字段） ──────────────
		// dy_samplingreview 主表需要填写阶段 ID、需求单 ID、客户 ID 等字段，
		int phaseId = task.getdy_samplingphase_ID();
		X_dy_samplingphase phase = new X_dy_samplingphase(getCtx(), phaseId, get_TrxName());

		int requestId = phase.getdy_samplingrequest_ID();
		X_dy_samplingrequest request = new X_dy_samplingrequest(getCtx(), requestId, get_TrxName());

		// ── 3. 解析效果编码 ───────────────────────────────────────────
		String selectedvalue = (String) task.get_Value("selectedvalue");
		String effectCode = buildEffectCode(selectedvalue);

		// ── 4. 查询任务下所有有效事项 ────────────────────────────────
		// 只查询 IsActive='Y' 的事项，按行号（Line）升序排列，
		// 保证评审明细的顺序与事项列表一致。
		List<X_dy_samplingtaskline> lines = new Query(getCtx(), X_dy_samplingtaskline.Table_Name,
				"DY_SamplingTask_ID=? AND IsActive='Y'", get_TrxName()).setParameters(taskId).setOrderBy("Line").list();

		// 若任务下没有任何事项，无法生成评审明细，直接终止流程
		if (lines.isEmpty())
			throw new AdempiereException("任务【" + task.getName() + "】下没有事项，无法生成评审单");

		// ── 5. 新建评审单主表 ─────────────────────────────────────────
		// 创建 dy_samplingreview 记录，填充所有必填字段：
		X_dy_samplingreview review = new X_dy_samplingreview(getCtx(), 0, get_TrxName());
		review.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
		review.setdy_samplingphase_ID(phaseId);
		review.setdy_samplingrequest_ID(requestId);
		review.setphasetype(phase.getphasetype()); // 阶段类型
		review.setDescription(task.getDescription());// 任务描述--->评审单描述
		review.setC_BPartner_ID(request.getC_BPartner_ID()); // 客户
		review.setAD_User_ID(getAD_User_ID()); // 提交人（当前用户）
		review.setreviewers(p_Reviewers); // 评审参与人
		review.setsubmitremark(p_SubmitRemark); // 提交备注
		review.setreviewstatus("RV"); // 初始状态：评审中
		review.set_ValueOfColumn("dy_samplingtask_id", taskId); // 关联工艺任务

		// 通过单据序列 DY_SamplingReview 自动生成单据编号
		String docNo = MSequence.getDocumentNo(getAD_Client_ID(), "DY_SamplingReview", get_TrxName(), review);
		review.setDocumentNo(docNo);
		review.saveEx();

		// 保存主键和单据编号，供后续步骤和 postProcess 使用
		m_reviewID = review.getdy_samplingreview_ID();
		m_finalDocNo = docNo;

		// ── 6. 逐条生成评审明细 ───────────────────────────────────────
		for (X_dy_samplingtaskline line : lines) {
			X_dy_samplingreviewline rl = new X_dy_samplingreviewline(getCtx(), 0, get_TrxName());
			rl.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
			rl.setdy_samplingreview_ID(m_reviewID); // 关联评审单主表
			rl.setdy_samplingtask_ID(taskId); // 关联工艺任务
			rl.setName(line.getName() + "——" + effectCode); // 名称：事项+效果编码
			rl.setValue(effectCode); // 效果编码：selectedvalue 拼接结果
			rl.setDescription(line.getDescription()); // 事项描述
			if (line.getAD_User_ID() > 0)
				rl.setAD_User_ID(line.getAD_User_ID()); // 负责人继承自事项（若有）
			rl.set_ValueOfColumn("dy_samplingtaskline_id", line.getdy_samplingtaskline_ID()); // 关联来源事项
			rl.saveEx();
			m_lineCount++;
		}
		// 7.明细创建完毕后，更新任务状态为"评审中"
		task.setTaskStatus("RV");
		task.saveEx();

		return "工艺评审单 " + m_finalDocNo + " 已创建，共 " + m_lineCount + " 条明细";
	}

	/**
	 * 流程执行成功后的后处理。 在结果面板中添加一条可点击的跳转链接，用户点击后直接跳转到新建的评审单记录。
	 * 
	 * @param success 流程是否执行成功
	 */
	@Override
	protected void postProcess(boolean success) {
		if (success && m_reviewID > 0) {
			addLog(0, null, null, "查看工艺评审单", new X_dy_samplingreview(getCtx(), 0, null).get_Table_ID(), m_reviewID);
		}
	}

	/**
	 * 根据工艺任务的 selectedvalue 字段，拼接效果编码字符串。
	 * 
	 * 
	 * selectedvalue 存储的是逗号分隔的评审明细 ID（如 {@code "1000094,1000095"}）， 本方法依次查询每个 ID 对应的
	 * dy_samplingreviewline.value（效果编码）， 用"/"拼接后返回（如 {@code "红塔山1/红塔山2"}）。
	 * 
	 * @param selectedvalue 工艺任务的 selectedvalue 字段值，允许为 null 或空
	 * @return 拼接后的效果编码字符串；若 selectedvalue 为空则返回空字符串
	 */
	private String buildEffectCode(String selectedvalue) {
		if (selectedvalue == null || selectedvalue.trim().isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (String rlId : selectedvalue.split(",")) {
			rlId = rlId.trim();
			if (rlId.isEmpty())
				continue;
			// 查询该评审明细的效果编码（value 字段，如"红塔山1"）
			String rlValue = DB.getSQLValueString(get_TrxName(),
					"SELECT value FROM dy_samplingreviewline WHERE dy_samplingreviewline_id=?", Integer.parseInt(rlId));
			if (rlValue != null && !rlValue.isEmpty()) {
				if (sb.length() > 0)
					sb.append("/");
				sb.append(rlValue);
			}
		}
		return sb.toString();
	}
}