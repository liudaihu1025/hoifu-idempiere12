package com.hoifu.process;  
  
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.hoifu.model.MDYSamplingTask;
import com.hoifu.model.X_dy_samplingphase;
import com.hoifu.model.X_dy_samplingtask;
import com.hoifu.model.X_dy_samplingtaskline;
  
/**
 * 从工艺任务创建生产打样任务（含任务明细）
 * 
 * <p>
 * 流程逻辑：
 * <ol>
 * <li>从参数获取工艺任务 ID 列表（多选，默认当前记录，动态验证只能选已终审任务）</li>
 * <li>找到项目下的生产打样阶段（phasetype='PR'）</li>
 * <li>防重复创建：同一工艺任务不能重复生成打样任务</li>
 * <li>创建打样任务，字段值从工艺任务复制，ref_task_id 指向工艺任务</li>
 * <li>解析工艺任务的 selectedvalue（逗号分隔的评审明细 ID），
 * 查询每个评审明细的效果编码（value），为打样任务逐条创建任务明细</li>
 * <li>根据实际创建的明细数量更新打样任务的效果数量（qtydesign）</li>
 * </ol>
 * 
 * <p>
 * AD_Process.Classname = com.hoifu.process.DYCreateSampleTaskFromEng
 */  
@Process  
public class DYCreateSampleTaskFromEng extends SvrProcess {  
  
	/** 工艺任务 ID 列表（逗号分隔，来自多选参数 dy_samplingtask_ID） */
	private String p_TaskIDs;
  
    @Override  
    protected void prepare() {  
		for (ProcessInfoParameter para : getParameter()) {
			if ("dy_samplingtask_ID".equals(para.getParameterName()))
				p_TaskIDs = para.getParameterAsString();
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
		// ── 1. 解析任务 ID 列表 ──────────────────────────────────────
		List<Integer> taskIds = parseIDs(p_TaskIDs);
		if (taskIds.isEmpty())
			throw new AdempiereException("请选择工艺任务");

		int created = 0;

		for (int engTaskId : taskIds) {
			// ── 2. 加载工艺任务 ──────────────────────────────────────
			X_dy_samplingtask engTask = new X_dy_samplingtask(getCtx(), engTaskId, get_TrxName());
			if (engTask.get_ID() == 0)
				throw new AdempiereException("工艺任务不存在: " + engTaskId);

			// 校验：工艺任务状态必须为已终审（FA）
			if (!"FA".equals(engTask.getTaskStatus()))
				throw new AdempiereException("工艺任务 " + engTask.getValue() + " 状态不是已完成，无法生成打样任务");

			// 校验：该任务下所有事项必须已采纳（reviewresult='AC'），不允许存在未采纳或未评审的事项
			int naCount = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM dy_samplingtaskline " + "WHERE dy_samplingtask_id=? AND isactive='Y' "
							+ "AND (reviewresult IS NULL OR reviewresult != 'AC')",
					engTaskId);
			if (naCount > 0)
				throw new AdempiereException("工艺任务 " + engTask.getValue() + " 存在未采纳或未评审的事项，无法生成打样任务");

			// ── 3. 找到项目下的生产打样阶段（phasetype='PR'） ──────────
			int engPhaseId = engTask.getdy_samplingphase_ID();
			X_dy_samplingphase engPhase = new X_dy_samplingphase(getCtx(), engPhaseId, get_TrxName());
			int requestId = engPhase.getdy_samplingrequest_ID();

			X_dy_samplingphase prPhase = new Query(getCtx(), X_dy_samplingphase.Table_Name,
					"dy_samplingrequest_id=? AND phasetype='PR' AND isactive='Y'", get_TrxName())
					.setParameters(requestId).first();
			if (prPhase == null)
				throw new AdempiereException("未找到生产打样阶段（PR），项目 ID=" + requestId);

			// ── 4. 防重复创建 ──────────────────────────────────────
			// 同一工艺任务在同一 PR 阶段下只允许生成一条打样任务
			int existing = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM dy_samplingtask "
							+ "WHERE ref_task_id=? AND dy_samplingphase_id=? AND isactive='Y'",
					engTaskId, prPhase.get_ID());
			if (existing > 0) {
				throw new AdempiereException(
						"工艺任务 " + engTask.getValue() + " - " + engTask.getName() + " 已有对应的打样任务，请勿重复创建");
			}

			// ── 5. 创建打样任务 ──────────────────────────────────────
			MDYSamplingTask sampleTask = new MDYSamplingTask(getCtx(), 0, get_TrxName());
			sampleTask.setAD_Org_ID(engTask.getAD_Org_ID());
			sampleTask.setdy_samplingphase_ID(prPhase.get_ID());
			sampleTask.setValue(engTask.getValue()); // 效果编码
			sampleTask.setName(engTask.getName()); // 任务名称
			sampleTask.set_ValueOfColumn("ref_task_id", engTaskId); // 来源工艺任务
			sampleTask.set_ValueOfColumn("selectedvalue", engTask.get_Value("selectedvalue")); // 效果多选框
			// 计划时间继承自打样阶段（PR）
			sampleTask.setplannedstartdate(prPhase.getplannedstartdate());
			sampleTask.setplannedenddate(prPhase.getplannedenddate());
			sampleTask.saveEx();

			int sampleTaskId = sampleTask.get_ID();
  
			// ── 6. 创建任务明细 ──────────────────────────────────────
			// 将 selectedvalue（如 "1000094,1000095"）拆开，
			// 查询每个评审明细的效果编码，逐条创建 dy_samplingtaskline 明细
			// 返回实际创建的明细数量，用于更新效果数量
			String selectedvalue = (String) engTask.get_Value("selectedvalue");
			int lineCount = createTaskLines(sampleTaskId, selectedvalue, engTask.getAD_Org_ID(),
					sampleTask.getplannedstartdate(), sampleTask.getplannedenddate());
  
			// 根据实际创建的明细数量更新打样任务的效果数量（qtydesign）
			sampleTask.setqtydesign(new BigDecimal(lineCount));
			sampleTask.saveEx();
  
			// 在结果面板中为每条打样任务输出一条可点击的跳转链接
			addLog(0, null, null, "打样任务：" + sampleTask.getValue() + " - " + sampleTask.getName(),
					new MDYSamplingTask(getCtx(), 0, null).get_Table_ID(), sampleTaskId);
  
			created++;
        }  
  
		return "共生成 " + created + " 条打样任务";
    }  
  
	/**
	 * 根据工艺任务的 selectedvalue（逗号分隔的评审明细 ID）， 为打样任务创建任务明细，每个效果编码对应一条明细。
	 * 
	 * 
	 * 示例：selectedvalue="1000094,1000095"， 查询得到"红塔山1"和"红塔山2"，分别创建 line=1（显示 01）和
	 * line=2（显示 02）的明细。 行号从 1 开始，步长为 1，前端通过 AD_Column.FormatPattern=00 格式化显示为
	 * 01、02、03…
	 * 
	 * @param sampleTaskId  新建的打样任务 ID
	 * @param selectedvalue 工艺任务的 selectedvalue，逗号分隔的评审明细 ID
	 * @param orgId         组织 ID
	 * @return ★ 实际创建的明细数量
	 */
	private int createTaskLines(int sampleTaskId, String selectedvalue, int orgId, java.sql.Timestamp plannedStartDate,
			java.sql.Timestamp plannedEndDate) {
		if (selectedvalue == null || selectedvalue.trim().isEmpty())
			return 0; // 返回 0

		int lineNo = 1; // 行号从 1 开始，步长 1（前端 FormatPattern=00 显示为 01、02…）
		int lineCount = 0; // 记录实际创建的明细数量
  
		for (String rlId : selectedvalue.split(",")) {
			rlId = rlId.trim();
			if (rlId.isEmpty())
				continue;
  
			// 查询该评审明细的效果编码（value 字段，如"红塔山1"）
			String effectCode = DB.getSQLValueString(get_TrxName(),
					"SELECT value FROM dy_samplingreviewline WHERE dy_samplingreviewline_id=?", Integer.parseInt(rlId));
			if (effectCode == null || effectCode.isEmpty()) {
				log.warning("评审明细 " + rlId + " 未找到效果编码，跳过");
				continue;
			}
  
			// 创建任务明细，事项名称 = 效果编码
			X_dy_samplingtaskline taskLine = new X_dy_samplingtaskline(getCtx(), 0, get_TrxName());
			taskLine.setAD_Org_ID(orgId);
			taskLine.set_ValueOfColumn("dy_samplingtask_id", sampleTaskId); // 关联打样任务
			taskLine.set_ValueOfColumn("Line", lineNo); // 行号（1、2、3…）
			taskLine.setName(effectCode); // 事项名称 = 效果编码
			taskLine.setAD_User_ID(getAD_User_ID()); // 负责人默认为当前操作人
			taskLine.setplannedstartdate(plannedStartDate); // 计划开始时间 = 任务的计划开始时间
			taskLine.setplannedenddate(plannedEndDate); // 计划完成时间 = 任务的计划完成时间
			taskLine.saveEx();
  
			lineNo++;
			lineCount++; // 累计创建数量
        }  

		return lineCount; // 返回实际创建的明细数量
    }  
  
	/**
	 * 解析逗号分隔的 ID 字符串为整数列表。 无法解析的非数字片段会记录警告日志并跳过。
	 * 
	 * @param idsStr 逗号分隔的 ID 字符串，允许为 null 或空
	 * @return 解析后的整数 ID 列表
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
                    log.warning("Invalid task ID: " + s);  
                }  
            }  
        }  
        return ids;  
    }  
}