package com.hoifu.service;  
  
import java.math.BigDecimal;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.adempiere.model.GenericPO;  
import org.compiere.model.MProcess;  
import org.compiere.process.ProcessInfo;  
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.Trx;  
import org.compiere.util.Util;  
  
import java.util.Properties;  
  
/**  
 * 出入库申请单 → PP_Material_Requisition / PP_Cost_Collector 生成服务。  
 *  
 * <p>本类不再直接用 GenericPO 拼装 PP_Material_Requisition / PP_Cost_Collector 记录，  
 * 而是通过标准的 iDempiere 流程调用机制，触发运行在 idempiere-mfg bundle 内的  
 * {@code org.libero.process.InOutRequisitionToPPMaterialOrder}（AD_Process.Value =  
 * "InOutRequisitionToPPMaterialOrder"），由该流程内部使用 MPPMaterialRequisition /  
 * MPPCostCollector 完成完整的单据业务逻辑。hoifu 侧因此不需要 import mfg 的  
 * org.libero.model 包，只依赖 org.compiere.process 的通用调用接口。</p>  
 */  
public class InOutRequisitionToPPMaterialOrderService {  
  
	private static final CLogger log = CLogger.getCLogger(InOutRequisitionToPPMaterialOrderService.class);
	/** 与 org.libero.process.InOutRequisitionToPPMaterialOrder 对应的 AD_Process.Value */  
	public static final String AD_PROCESS_VALUE = "InOutRequisitionToPPMaterialOrder";  
  
	public static final String TABLE_PP_Cost_Collector = "PP_Cost_Collector";  
  
	/**  
	 * 通过申请单号 + 明细行号 + 数量，触发生成流程，返回新建的 PP_Cost_Collector（只读包装，  
	 * 供调用方读取字段用，不再用于写业务逻辑——业务逻辑已在流程内部由 MPPCostCollector 完成）。  
	 *  
	 * @param documentNo M_InOut_Requisition.DocumentNo  
	 * @param line       M_InOut_RequisitionLine.Line  
	 * @param qty        本次生成数量  
	 * @param ctx        上下文  
	 * @param trxName    调用方事务名（流程会在同一事务内执行，不额外开/提交事务）  
	 * @return 新建的 PP_Cost_Collector（GenericPO 包装，仅用于读取字段）  
	 */  
	public static GenericPO createFromRequisition(String documentNo, int line,  
			BigDecimal qty, Properties ctx, String trxName) {  
  
		if (documentNo == null || documentNo.trim().isEmpty())  
			throw new AdempiereException("申请单号不能为空");  
		if (line <= 0)  
			throw new AdempiereException("行号必须大于0");  
		if (qty == null || qty.signum() <= 0)  
			throw new AdempiereException("生成数量必须大于0");  
  
		// 1. 按 AD_Process.Value 解析流程 ID（不用硬编码 AD_Process_ID，避免环境间 ID 不一致）  
		int processId = MProcess.getProcess_ID(AD_PROCESS_VALUE, trxName);  
		if (processId <= 0)  
			throw new AdempiereException("未找到流程定义，AD_Process.Value=" + AD_PROCESS_VALUE  
					+ "，请先在 Application Dictionary 中创建该流程记录");  
  
		MProcess process = MProcess.get(ctx, processId);  
		if (process == null)  
			throw new AdempiereException("加载流程失败，AD_Process_ID=" + processId);  
  
		// 2. 组装 ProcessInfo + 参数（内存态，不落 AD_PInstance_Para 表）  
		ProcessInfo pi = new ProcessInfo(process.getName(), processId);  
		pi.setAD_Client_ID(org.compiere.util.Env.getAD_Client_ID(ctx));  
		pi.setAD_User_ID(org.compiere.util.Env.getAD_User_ID(ctx));  
		pi.setTransactionName(trxName);  
  
		ProcessInfoParameter[] parameters = new ProcessInfoParameter[] {  
				new ProcessInfoParameter("DocumentNo", documentNo, null, null, null),  
				new ProcessInfoParameter("Line", line, null, null, null),  
				new ProcessInfoParameter("Qty", qty, null, null, null)  
		};  
		pi.setParameter(parameters);  
  
		// 3. 复用调用方事务执行流程（managedTrx=false，避免流程内部提交/回滚调用方事务）  
		Trx trx = trxName == null ? null : Trx.get(trxName, false);  
		boolean ok = process.processIt(pi, trx, false);  
  
		if (!ok || pi.isError()) {  
			String msg = Util.isEmpty(pi.getSummary()) ? "生成失败，无详细信息" : pi.getSummary();  
			throw new AdempiereException("调用流程 " + AD_PROCESS_VALUE + " 失败：" + msg);  
		}  
  
		// 4. 解析流程摘要中的 PP_Cost_Collector_ID（流程 doIt() 返回  
		//    "@Created@ PP_Cost_Collector_ID=xxx"），并加载该记录只读返回  
		int ccId = parseResultId(pi.getSummary());  
		if (ccId <= 0)  
			throw new AdempiereException("流程执行成功但未能解析生成的 PP_Cost_Collector_ID，摘要：" + pi.getSummary());  
  
		GenericPO cc = new GenericPO(TABLE_PP_Cost_Collector, ctx, ccId, trxName);  
		if (cc.get_ID() != ccId)  
			throw new AdempiereException("流程返回的 PP_Cost_Collector_ID=" + ccId + " 无法加载到记录");  
  
		return cc;  
	}  
  
	/**  
	 * 从流程摘要文本中解析出 "PP_Cost_Collector_ID=123" 里的数字 ID。  
	 */  
	private static int parseResultId(String summary) {  
		if (summary == null)  
			return 0;  
		int idx = summary.indexOf("PP_Cost_Collector_ID=");  
		if (idx < 0)  
			return 0;  
		String tail = summary.substring(idx + "PP_Cost_Collector_ID=".length()).trim();  
		StringBuilder digits = new StringBuilder();  
		for (int i = 0; i < tail.length(); i++) {  
			char c = tail.charAt(i);  
			if (Character.isDigit(c))  
				digits.append(c);  
			else  
				break;  
		}  
		if (digits.length() == 0)  
			return 0;  
		try {  
			return Integer.parseInt(digits.toString());  
		} catch (NumberFormatException e) {  
			return 0;  
		}  
	}  
}