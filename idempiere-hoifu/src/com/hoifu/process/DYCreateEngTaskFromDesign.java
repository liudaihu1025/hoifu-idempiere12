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
import com.hoifu.model.X_dy_samplingreviewline;
import com.hoifu.model.X_dy_samplingtask;  
  
/**  
 * 从平面设计任务创建工艺设计任务  
 *  
 * <p>  
 * 流程逻辑：  
 * <ol>  
 * <li>从参数中获取来源平面设计任务 ID 列表（多选）</li>  
 * <li>校验来源任务所属阶段类型必须为 DS（平面设计）</li>  
 * <li>在同一需求单下找到工艺设计阶段（phasetype=EN）</li>  
 * <li>查询来源任务下已终审且已采纳的评审明细（reviewresult=AC, reviewstatus=FA）</li>  
 * <li>每条评审明细生成一条工艺设计任务，并在结果面板中输出跳转链接</li>  
 * </ol>  
 *  
 * <p>  
 * AD_Process.Classname = com.hoifu.process.DYCreateEngTaskFromDesign  
 */  
@Process  
public class DYCreateEngTaskFromDesign extends SvrProcess {  
  
    /** 参数：来源平面设计任务 ID（多选，ChosenMultipleSelectionTable） */  
    private String p_TaskIDsStr;  
  
    /** 统计本次生成的工艺设计任务数 */  
    private int m_taskCount = 0;  

  
    @Override  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (ProcessInfoParameter p : para) {  
            if ("dy_samplingtask_ID".equals(p.getParameterName())) {  
                if (p.getParameter() != null)  
                    p_TaskIDsStr = p.getParameter().toString();  
            } else {  
                log.warning("Unknown Parameter: " + p.getParameterName());  
            }  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        List<Integer> taskIDs = parseIDs(p_TaskIDsStr);  
        if (taskIDs.isEmpty())  
            throw new AdempiereException("请至少选择一个任务");  
  
        for (int taskID : taskIDs) {  
            MDYSamplingTask srcTask = new MDYSamplingTask(getCtx(), taskID, get_TrxName());  
            if (srcTask.get_ID() == 0)  
                throw new AdempiereException("任务不存在: ID=" + taskID);  
  
            // 校验来源任务所属阶段类型必须为 DS（平面设计）  
            X_dy_samplingphase srcPhase = new X_dy_samplingphase(getCtx(),  
                    srcTask.getdy_samplingphase_ID(), get_TrxName());  
            String phasetype = srcPhase.getphasetype();  
  
            if (!X_dy_samplingphase.PHASETYPE_平面设计.equals(phasetype)) {  
                throw new AdempiereException(  
                        "任务 [" + srcTask.getValue() + "] 所属阶段类型 [" + phasetype + "] 不是平面设计阶段，无法生成工艺设计任务");  
            }  
  
            createEngTask(srcTask, srcPhase);  
        }  
  
        return "共生成 " + m_taskCount + " 条工艺设计任务";  
    }  
  
    // =========================================================  
    // DS → EN：从平面设计任务创建工艺设计任务  
    // =========================================================  
  
    /**  
     * 根据一条平面设计任务，在同一需求单的工艺设计阶段下生成工艺设计任务。  
     *  
     * <p>  
     * 每条已终审且已采纳的评审明细（reviewresult=AC, reviewstatus=FA）对应生成一条工艺设计任务，  
     * 并将评审明细 ID 写入 selectedvalue 字段，供后续效果合并使用。  
     *  
     * @param dsTask   来源平面设计任务  
     * @param srcPhase 来源平面设计阶段  
     */  
    private void createEngTask(MDYSamplingTask dsTask, X_dy_samplingphase srcPhase) {  
        int requestID = srcPhase.getdy_samplingrequest_ID();  
  
        // 在同一需求单下找到工艺设计阶段（phasetype=EN）  
        int engPhaseID = new Query(getCtx(), X_dy_samplingphase.Table_Name,  
                "dy_samplingrequest_id=? AND phasetype=?", get_TrxName())  
                .setParameters(requestID, X_dy_samplingphase.PHASETYPE_工艺设计)  
                .setOnlyActiveRecords(true)  
                .firstId();  
  
        if (engPhaseID <= 0)  
            throw new AdempiereException("需求单 [" + requestID + "] 下未找到工艺设计阶段，请先创建需求单");  

		// 加载工艺阶段，用于获取计划时间
		X_dy_samplingphase engPhase = new X_dy_samplingphase(getCtx(), engPhaseID, get_TrxName());
  
        // 查询来源任务下已终审且已采纳的评审明细  
        List<X_dy_samplingreviewline> lines = new Query(getCtx(), X_dy_samplingreviewline.Table_Name,  
                "dy_samplingtask_id=? AND reviewresult='AC' AND reviewstatus='FA'", get_TrxName())  
                .setParameters(dsTask.getdy_samplingtask_ID())  
                .setOnlyActiveRecords(true)  
                .list();  
  
        if (lines.isEmpty()) {  
			throw new AdempiereException(
					"任务 [" + dsTask.getValue() + " - " + dsTask.getName() + "] 没有已终审且已采纳的评审明细，无法创建工艺任务");
		}

		// 防重复创建，检查该设计任务在 EN 阶段是否已有工艺任务
		int existing = DB.getSQLValue(get_TrxName(),
				"SELECT COUNT(1) FROM dy_samplingtask "
						+ "WHERE ref_task_id=? AND dy_samplingphase_id=? AND isactive='Y'",
				dsTask.getdy_samplingtask_ID(), engPhaseID);
		if (existing > 0) {
			throw new AdempiereException(
					"设计任务 [" + dsTask.getValue() + " - " + dsTask.getName() + "] 已生成过工艺设计任务，请勿重复创建");
		}
  
        // 每条评审明细生成一条工艺设计任务  
		for (X_dy_samplingreviewline rl : lines) {

            MDYSamplingTask engTask = new MDYSamplingTask(getCtx(), 0, get_TrxName());  
            engTask.setAD_Org_ID(dsTask.getAD_Org_ID());  
            engTask.setdy_samplingphase_ID(engPhaseID);  
            engTask.setName(dsTask.getName());  
            engTask.setAD_User_ID(dsTask.getAD_User_ID());  
            engTask.setqtydesign(BigDecimal.ONE);  
            engTask.setTaskStatus(X_dy_samplingtask.TASKSTATUS_进行中);  
            // 记录来源平面设计任务 ID，用于后续追溯  
            engTask.set_ValueOfColumn("ref_task_ID", dsTask.getdy_samplingtask_ID());  
            // 将评审明细 ID 写入 selectedvalue，初始只包含本条评审明细  
			engTask.set_ValueOfColumn("selectedvalue", String.valueOf(rl.getdy_samplingreviewline_ID()));
			// 计划时间继承自工艺阶段
			engTask.setplannedstartdate(engPhase.getplannedstartdate());
			engTask.setplannedenddate(engPhase.getplannedenddate());
            engTask.saveEx();  
  
            // 在结果面板中为每条工艺任务输出一条可点击的跳转链接  
            addLog(0, null, null,  
					"工艺任务：" + engTask.getValue() + " - " + rl.getName(),
                    new MDYSamplingTask(getCtx(), 0, null).get_Table_ID(),  
                    engTask.getdy_samplingtask_ID());  

			m_taskCount++;

        }  
    }  
  
    // =========================================================  
    // 工具方法  
    // =========================================================  
  
    /**  
     * 将逗号分隔的 ID 字符串解析为整数列表。  
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
                    log.warning("Invalid task ID: " + s);  
                }  
            }  
        }  
        return ids;  
    }  
}