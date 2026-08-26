package org.libero.process;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MQuery;
import org.compiere.model.PrintInfo;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportCtl;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;
import org.libero.model.MPPProcessCard;  
  
/**  
 * 根据选中的生产工单：  
 *   - 若该工单尚未生成流程卡（PP_Process_Card），则先按流程参数 Qty（卡板数量）生成  
 *   - 生成完成后（或已存在流程卡时），直接打开打印格式 'HF_生产流程卡' 的打印查看器  
 *  
 * 必须实现 ClientProcess，因为 ReportCtl.preview() 需要在客户端 UI 线程弹出查看器窗口。  
 */  
@org.adempiere.base.annotation.Process  
public class ProcessCardPrintByPPOrder extends SvrProcess {  
  
	/** 打印格式名称，固定 */  
	private static final String PRINT_FORMAT_NAME = "HF_生产流程卡";  
	/** 流程卡表名 */  
	private static final String TABLE_NAME = "RV_PP_Process_Card"; // "PP_Process_Card"  
  
	private BigDecimal p_Qty = null; // 卡板数量（流程参数），仅在首次生成流程卡时需要  
  
	@Override  
	protected void prepare() {  
		ProcessInfoParameter[] para = getParameter();  
		for (int i = 0; i < para.length; i++) {  
			String name = para[i].getParameterName();  
			if (para[i].getParameter() == null)  
				;  
			else if (name.equals("Qty"))  
				p_Qty = (BigDecimal) para[i].getParameter();  
			else  
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);  
		}  
	}  
  
	@Override  
	protected String doIt() throws Exception {  
		List<Integer> orderIds = getSelectedOrderIds();  
		if (orderIds.isEmpty())  
			throw new AdempiereUserError("请选择要生成/打印流程卡的工单");  
  
		int userId = Env.getAD_User_ID(getCtx());  
		int printedCount = 0;  
  
		for (Integer orderId : orderIds) {  
			MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
			if (order.get_ID() <= 0) {  
				log.warning("工单不存在: " + orderId);  
				continue;  
			}  
  
			// 该工单是否已生成过流程卡  
			boolean alreadyGenerated = DB.getSQLValue(get_TrxName(),  
					"SELECT COUNT(*) FROM PP_Process_Card WHERE PP_Order_ID=?", orderId) > 0;  
  
			if (!alreadyGenerated) {  
				if (p_Qty == null || p_Qty.signum() <= 0)  
					throw new AdempiereUserError("工单【" + order.getDocumentNo() + "】尚未生成流程卡，请输入有效的卡板数量(Qty)");  
				generateCards(order, p_Qty, userId);  
			}  
  
			// 打开打印查看器  
			ReportEngine re = getReportEngine(orderId);  
			if (re == null) {  
				addLog("@NotFound@ @AD_PrintFormat_ID@ " + PRINT_FORMAT_NAME);  
				continue;  
			}  
			ReportCtl.preview(re);  
			printedCount++;
		}  
  
		if (printedCount == 0)  
			return "未打开任何流程卡打印";  
  
		return "已生成/打开 " + printedCount + " 张工单的流程卡打印";  
	}  
  
	/**  
	 * 按卡板数量参数生成该工单的所有流程卡（PP_Process_Card）。  
	 * CardNo/Qty(末张吸收余数) 由 MPPProcessCard.beforeSave() 自动计算。  
	 */  
	private void generateCards(MPPOrder order, BigDecimal batchQty, int userId) {  
		BigDecimal qtyOrdered = order.getQtyOrdered();  
		if (qtyOrdered == null || qtyOrdered.signum() <= 0)  
			throw new AdempiereUserError("工单 " + order.getDocumentNo() + " 数量无效");  
  
		int total = qtyOrdered.divide(batchQty, 0, RoundingMode.CEILING).intValue();  
		for (int i = 0; i < total; i++) {  
			MPPProcessCard card = new MPPProcessCard(order, batchQty, userId, get_TrxName());  
			card.saveEx(get_TrxName());  
		}  
	}  
  
	/**  
	 * 构造打印查看器需要的 ReportEngine：打印格式='HF_生产流程卡'，按 PP_Order_ID 过滤。  
	 */  
	private ReportEngine getReportEngine(int orderId) {  
	    int formatId = DB.getSQLValueEx(get_TrxName(),  
	            "SELECT AD_PrintFormat_ID FROM AD_PrintFormat WHERE Name=? AND AD_Client_ID IN (0,?) ORDER BY AD_Client_ID DESC",  
	            PRINT_FORMAT_NAME, getAD_Client_ID());  
	    if (formatId <= 0)  
	        throw new AdempiereUserError("找不到打印格式: " + PRINT_FORMAT_NAME);  
	  
	    MPrintFormat format = MPrintFormat.get(getCtx(), formatId, true);  
	    if (format == null)  
	        throw new AdempiereUserError("找不到打印格式: " + PRINT_FORMAT_NAME);  
	  
	    MQuery query = new MQuery(TABLE_NAME);  
	    query.addRestriction("PP_Order_ID", MQuery.EQUAL, orderId);  
	  
	    PrintInfo info = new PrintInfo(getProcessInfo());  
//	    return new ReportEngine(getCtx(), format, query, info); // 只 return，不 preview  
		return new ReportEngine(getCtx(), format, query, info, get_TrxName(), 0);
	}
	
  
	/**  
	 * 三段式获取选中的 PP_Order_ID：多选 → 单选 → T_Selection（Info Window）  
	 */  
	private List<Integer> getSelectedOrderIds() {  
		List<Integer> orderIds = getRecord_IDs();  
		if (orderIds == null || orderIds.isEmpty()) {  
			int recordId = getRecord_ID();  
			if (recordId > 0) {  
				orderIds = List.of(recordId);  
			} else {  
				int[] selIds = DB.getIDsEx(get_TrxName(),  
						"SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?", getAD_PInstance_ID());  
				orderIds = new ArrayList<>();  
				if (selIds != null) {  
					for (int id : selIds)  
						orderIds.add(id);  
				}  
			}  
		}  
		return orderIds == null ? new ArrayList<>() : orderIds;  
	}  
}