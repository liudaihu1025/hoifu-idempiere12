package org.libero.process;  
  
import java.util.ArrayList;  
import java.util.List;  
  
import org.compiere.model.MQuery;  
import org.compiere.model.PrintInfo;  
import org.compiere.print.MPrintFormat;  
import org.compiere.print.ReportCtl;  
import org.compiere.print.ReportEngine;  
import org.compiere.process.ClientProcess;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.AdempiereUserError;  
import org.compiere.util.DB;  
  
/**  
 * 根据选中的 PP_Process_Card_ID（多选/单选/T_Selection），  
 * 直接打开打印格式 'HF_生产流程卡' 的打印查看器。  
 *  
 * 必须实现 ClientProcess，因为 ReportCtl.preview() 需要在客户端 UI 线程弹出查看器窗口。  
 */  
@org.adempiere.base.annotation.Process  
public class ProcessCardPrintById extends SvrProcess implements ClientProcess {  
  
	/** 打印格式名称，固定 */  
	private static final String PRINT_FORMAT_NAME = "HF_生产流程卡";  
	/** 流程卡打印视图名 */  
	private static final String TABLE_NAME = "RV_PP_Process_Card"; // "PP_Process_Card"  
  
	@Override  
	protected void prepare() {  
		// 该流程无需额外参数  
	}  
  
	@Override  
	protected String doIt() throws Exception {  
		List<Integer> cardIds = getSelectedCardIds();  
		if (cardIds.isEmpty())  
			throw new AdempiereUserError("请选择要打印的流程卡");  
  
		ReportEngine re = getReportEngine(cardIds);  
		if (re == null) {  
			addLog("@NotFound@ @AD_PrintFormat_ID@ " + PRINT_FORMAT_NAME);  
			return "未打开流程卡打印";  
		}  
  
		ReportCtl.preview(re);  
  
		return "已打开 " + cardIds.size() + " 张流程卡打印";  
	}  
  
	/**  
	 * 构造打印查看器需要的 ReportEngine：打印格式='HF_生产流程卡'，按 RV_PP_Process_Card_ID IN (...) 过滤。  
	 */  
	private ReportEngine getReportEngine(List<Integer> cardIds) {  
	    int formatId = DB.getSQLValueEx(get_TrxName(),  
	            "SELECT AD_PrintFormat_ID FROM AD_PrintFormat WHERE Name=? AND AD_Client_ID IN (0,?) ORDER BY AD_Client_ID DESC",  
	            PRINT_FORMAT_NAME, getAD_Client_ID());  
	    if (formatId <= 0)  
	        throw new AdempiereUserError("找不到打印格式: " + PRINT_FORMAT_NAME);  
	  
	    MPrintFormat format = MPrintFormat.get(getCtx(), formatId, true);  
	    if (format == null)  
	        throw new AdempiereUserError("找不到打印格式: " + PRINT_FORMAT_NAME);  
	  
	    MQuery query = new MQuery(TABLE_NAME);  
	    // 拼接 IN (...) 字符串，注意视图里主键别名为 RV_PP_Process_Card_ID  
	    StringBuilder ids = new StringBuilder();  
	    for (int i = 0; i < cardIds.size(); i++) {  
	        if (i > 0)  
	            ids.append(",");  
	        ids.append(cardIds.get(i));  
	    }  
	    query.addRestriction("RV_PP_Process_Card_ID IN (" + ids + ")");  
	  
	    PrintInfo info = new PrintInfo(getProcessInfo());  
	    return new ReportEngine(getCtx(), format, query, info);  
	} 
  
	/**  
	 * 三段式获取选中的 PP_Process_Card_ID：多选 → 单选 → T_Selection（Info Window）  
	 */  
	private List<Integer> getSelectedCardIds() {  
		List<Integer> cardIds = getRecord_IDs();  
		if (cardIds == null || cardIds.isEmpty()) {  
			int recordId = getRecord_ID();  
			if (recordId > 0) {  
				cardIds = List.of(recordId);  
			} else {  
				int[] selIds = DB.getIDsEx(get_TrxName(),  
						"SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?", getAD_PInstance_ID());  
				cardIds = new ArrayList<>();  
				if (selIds != null) {  
					for (int id : selIds)  
						cardIds.add(id);  
				}  
			}  
		}  
		return cardIds == null ? new ArrayList<>() : cardIds;  
	}  
}