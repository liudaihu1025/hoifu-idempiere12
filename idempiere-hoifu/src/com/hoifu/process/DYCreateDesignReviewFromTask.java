package com.hoifu.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSequence;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.hoifu.model.X_dy_samplingphase;
import com.hoifu.model.X_dy_samplingrequest;
import com.hoifu.model.X_dy_samplingreview;
import com.hoifu.model.X_dy_samplingreviewline;
import com.hoifu.model.X_dy_samplingtask;

/**
 * 从设计任务新建设计评审单
 *
 * <p>
 * 流程逻辑：
 * <ol>
 * <li>从参数或 Tab 多选中获取任务 ID 列表</li>
 * <li>校验所有任务属于同一阶段，且效果数量 > 0</li>
 * <li>新建一条《打样评审》主表记录</li>
 * <li>按每个任务的效果数量，逐条新建《打样评审明细》记录</li>
 * </ol>
 *
 * <p>
 * AD_Process.Classname = com.hoifu.process.DYCreateDesignReviewFromTask
 */
@Process
public class DYCreateDesignReviewFromTask extends SvrProcess {

	// ==================== 流程参数 ====================
	/** 任务 ID 列表（逗号分隔字符串，来自 ChosenMultipleSelectionTable 参数 DY_SamplingTask_ID） */
	private String p_TaskIDsStr = null;
	/** 评审人 ID 列表（逗号分隔字符串，来自多选参数 Reviewers），必填 */
	private String p_Reviewers = null;
	/** 提交备注（来自文本参数 SubmitRemark），可选 */
	private String p_SubmitRemark = null;

	// ==================== 执行结果（供 postProcess 使用）====================
	private int m_reviewID = 0;
	private String m_finalDocNo = null;
	private int m_lineCount = 0;
	/**
	 * 解析流程参数。
	 * 参数名称必须与 AD_Process_Para.ColumnName 完全一致。
	 */
	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				; // 参数值为空，跳过
			else if (name.equals("*RecordIDs*"))
				; // Tab 多选时系统自动注入的特殊参数，此处忽略，通过 getRecord_IDs() 获取
			else if (name.equals("dy_samplingtask_ID"))
				p_TaskIDsStr = para.getParameterAsString(); // 任务多选参数
			else if (name.equals("reviewers"))
				p_Reviewers = para.getParameterAsString();  // 评审人多选参数
			else if (name.equals("submitremark"))
				p_SubmitRemark = para.getParameterAsString(); // 提交备注参数
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para); // 未知参数记录日志
		}
	}

	/**
	 * 流程主逻辑：新建打样评审主表及明细。
	 *
	 * @return 执行结果描述
	 * @throws Exception 业务校验失败或数据库操作异常
	 */
	@Override
	protected String doIt() throws Exception {

		// ==================== 1. 解析任务 ID 列表 ====================
		List<Integer> taskIDs = parseIDs(p_TaskIDsStr);

		if (taskIDs.isEmpty())
			throw new AdempiereException("请至少选择一个任务");

		// ==================== 2. 校验评审人 ====================
		if (p_Reviewers == null || p_Reviewers.trim().isEmpty())
			throw new AdempiereException("评审人不能为空");

		// ==================== 3. 加载任务，校验阶段一致性 ====================
		List<X_dy_samplingtask> tasks = new ArrayList<>();
		int phaseID = -1; // 用于校验所有任务属于同一阶段

		for (int taskID : taskIDs) {
			X_dy_samplingtask task = new X_dy_samplingtask(getCtx(), taskID, get_TrxName());
			if (task.getdy_samplingtask_ID() <= 0)
				throw new AdempiereException("任务 ID " + taskID + " 不存在");

			// 只有已完成（CO）的任务才能发起评审
			if (!"CO".equals(task.getTaskStatus()))
				throw new AdempiereException("任务 " + task.getValue() + " 状态不是已完成，无法发起评审");

			int taskPhaseID = task.getdy_samplingphase_ID();
			if (phaseID == -1) {
				phaseID = taskPhaseID; // 以第一个任务的阶段为基准
			} else if (phaseID != taskPhaseID) {
				throw new AdempiereException("所选任务必须属于同一阶段，请重新选择");
			}

			// 效果数量必须 > 0，否则无法生成明细
			if (task.getqtydesign().compareTo(BigDecimal.ZERO) <= 0)
				throw new AdempiereException("任务 " + task.getValue() + " 的效果数量为 0，无法发起评审");

			tasks.add(task);
		}

		// ==================== 4. 加载阶段和项目信息 ====================
		X_dy_samplingphase phase = new X_dy_samplingphase(getCtx(), phaseID, get_TrxName());
		if (phase.getdy_samplingphase_ID() <= 0)
			throw new AdempiereException("阶段不存在");

		int requestID = phase.getdy_samplingrequest_ID();
		X_dy_samplingrequest request = new X_dy_samplingrequest(getCtx(), requestID, get_TrxName());
		if (request.getdy_samplingrequest_ID() <= 0)
			throw new AdempiereException("项目不存在");

		// ==================== 5. 计算效果数量合计 ====================
		BigDecimal totalQtyDesign = BigDecimal.ZERO;
		for (X_dy_samplingtask task : tasks)
			totalQtyDesign = totalQtyDesign.add(task.getqtydesign()); // 累加所有任务的效果数量

		// ==================== 6. 新建打样评审主表 ====================
		X_dy_samplingreview review = new X_dy_samplingreview(getCtx(), 0, get_TrxName());
		review.setAD_Org_ID(Env.getAD_Org_ID(getCtx())); // 组织（从当前登录上下文获取）

		// 生成评审编号：iDempiere 自动查找或创建名为 DocumentNo_DY_SamplingReview 的序列
		// 若序列不存在，MSequence.getDocumentNo 会自动创建，起始值为 1000000
		String docNo = MSequence.getDocumentNo(getAD_Client_ID(), "DY_SamplingReview", get_TrxName());
		if (docNo != null)
			review.setDocumentNo(docNo);

		review.setdy_samplingphase_ID(phaseID);                      // 阶段
		review.setdy_samplingrequest_ID(requestID);                  // 项目
		review.setphasetype(phase.getphasetype());                   // 阶段类型（DS/EN/PR）
		review.setC_BPartner_ID(request.getC_BPartner_ID());        // 客户（来自项目）

		int bizAssistantID = request.getBizAssistant_ID();           // 跟单员（来自项目）
		if (bizAssistantID > 0)
			review.setBizAssistant_ID(bizAssistantID);

		review.setAD_User_ID(Env.getAD_User_ID(getCtx()));          // 提交人 = 当前登录用户
		review.setreviewers(p_Reviewers);                            // 评审人（逗号分隔的用户 ID）
		review.setqtydesign(totalQtyDesign);                         // 效果数量合计

		// 阶段描述 → 评审单描述
		String phaseDesc = phase.getDescription();
		if (phaseDesc != null)
			review.setDescription(phaseDesc);

		// 提交备注（来自流程参数，可选）
		if (p_SubmitRemark != null && !p_SubmitRemark.trim().isEmpty())
			review.setsubmitremark(p_SubmitRemark);

		review.setreviewstatus("RV"); // 评审状态：评审中
		review.setsubmitdate(new java.sql.Timestamp(System.currentTimeMillis())); // 提交时间 = 当前时间
		review.saveEx();


		// ==================== 7. 新建打样评审明细 ====================
		// 每个任务按其效果数量（qtydesign）循环创建明细，效果编码格式：任务名称 + 序号（如 红塔山1）
		m_reviewID = review.getdy_samplingreview_ID();
		m_finalDocNo = review.getDocumentNo();

		for (X_dy_samplingtask task : tasks) {
			int qty = task.getqtydesign().intValue();       // 该任务的效果数量
			String taskName = task.getName();               // 任务名称，写入明细
			String taskDesc = task.getDescription(); // 任务描述，写入明细描述
			int designerID = task.getAD_User_ID();          // 设计人
			int taskID = task.getdy_samplingtask_ID();

			for (int i = 1; i <= qty; i++) {
				// 效果编码：任务名称 + 一位序号，如 红塔山1、红塔山2
				String effectCode = taskName + i;

				X_dy_samplingreviewline line = new X_dy_samplingreviewline(getCtx(), 0, get_TrxName());
				line.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));      // 组织
				line.setdy_samplingreview_ID(m_reviewID); // 关联评审单
				line.setdy_samplingtask_ID(taskID);                  // 关联阶段任务
				line.setName(taskName + "——" + effectCode); // 任务名称+效果编码
				line.setValue(effectCode);                           // 效果编码

				// 任务描述 → 明细描述 Description（效果描述）
				if (taskDesc != null && !taskDesc.trim().isEmpty())
					line.setDescription(taskDesc);
				if (designerID > 0)
					line.setAD_User_ID(designerID);                  // 设计人（来自任务）
				line.setreviewstatus("RV");                          // 评审状态：评审中
				line.saveEx();
				m_lineCount++;
			}
			// ==================== 明细创建完毕后，更新任务状态为"评审中" ====================
			task.setTaskStatus("RV");
			task.saveEx();
		}


		return "设计评审单 " + m_finalDocNo + " 已创建，共 " + m_lineCount + " 条明细";
	}

	@Override
	protected void postProcess(boolean success) {
		if (success && m_reviewID > 0) {
			addLog(0, null, null, "查看设计评审单", new X_dy_samplingreview(getCtx(), 0, null).get_Table_ID(), m_reviewID);
		}
	}


	/**
	 * 解析逗号分隔的 ID 字符串为整数列表。
	 *
	 * <p>ChosenMultipleSelectionTable/Search 参数值格式为 "101,102,103"，
	 * 此方法将其拆分并转换为 {@code List<Integer>}。
	 * 无法解析的非数字片段会记录警告日志并跳过。
	 *
	 * @param idsStr 逗号分隔的 ID 字符串，允许为 null 或空
	 * @return 解析后的整数 ID 列表，若输入为空则返回空列表
	 */
	private List<Integer> parseIDs(String idsStr) {
		List<Integer> ids = new ArrayList<>();
		if (idsStr == null || idsStr.trim().isEmpty())
			return ids;
		for (String s : idsStr.split(",")) {
			s = s.trim();
			if (!s.isEmpty()) {
				try {
					ids.add(Integer.parseInt(s));
				} catch (NumberFormatException e) {
					log.warning("Invalid task ID: " + s); // 非数字片段，记录警告并跳过
				}
			}
		}
		return ids;
	}
}