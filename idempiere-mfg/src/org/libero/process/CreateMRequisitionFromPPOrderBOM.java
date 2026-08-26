package org.libero.process;  
  
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOMLine;  
  
/**  
 * 《申购物料信息》信息窗口  "生成申购单" 流程  
 *  
 *   
 */  
public class CreateMRequisitionFromPPOrderBOM extends SvrProcess {  

	// ==================== 流程参数 ====================  
	private Timestamp p_DateRequired = null;  
  
	// ==================== 从信息窗口读取的勾选数据 ====================  
	/** 勾选的 T_Selection_ID 列表，本信息窗口的 keyColumn 就是 PP_Order_BOMLine_ID */  
	private List<Integer> selectedBOMLineIds = new ArrayList<Integer>();  
  
	/**申购数量*/  
	private Map<String, Object> selectionValueMap = new HashMap<String, Object>();  
  
	@Override  
	protected void prepare() {  
		// 读取流程参数 需求日期  
		for (ProcessInfoParameter para : getParameter()) {  
			String name = para.getParameterName();  
			if (para.getParameter() == null)  
				continue;  
			if ("DateRequired".equals(name))  
				p_DateRequired = (Timestamp) para.getParameter();  
			else  
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);  
		}  
		if (p_DateRequired == null)  
			throw new AdempiereUserError("请先填写需求日期");  
  
		// 从 T_Selection / T_Selection_InfoWindow 读取信息窗口中勾选的行及用户填写的申购数量  
		loadSelectedLines();  
  
		if (selectedBOMLineIds.isEmpty())  
			throw new AdempiereUserError("请至少勾选一条需要申购的物料");  
	}  
  
	/** 读取信息窗口中勾选的行 ID，以及用户在 申购数量 列填写的值 */
	private void loadSelectedLines() {  
		// 读取勾选的 T_Selection_ID（即 PP_Order_BOMLine_ID）  
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		try {  
			pstmt = DB.prepareStatement(sql, get_TrxName());  
			pstmt.setInt(1, getAD_PInstance_ID());  
			rs = pstmt.executeQuery();  
			while (rs.next())  
				selectedBOMLineIds.add(rs.getInt(1));  
		} catch (SQLException e) {  
			throw new AdempiereException("获取信息窗口记录行失败: " + e.getMessage());
		} finally {  
			DB.close(rs, pstmt);  
		}  
  
		// 读取用户在信息窗口里填写的 申购数量 (QtyToRequisition)
		sql = "SELECT T_Selection_ID, ColumnName, Value_String, Value_Number, Value_Date "  
				+ "FROM T_Selection_InfoWindow "  
				+ "WHERE AD_PInstance_ID = ? "  
				+ "ORDER BY T_Selection_ID, ColumnName";  
		pstmt = null;  
		rs = null;  
		try {  
			pstmt = DB.prepareStatement(sql, get_TrxName());  
			pstmt.setInt(1, getAD_PInstance_ID());  
			rs = pstmt.executeQuery();  
			while (rs.next()) {  
				int T_Selection_ID = rs.getInt("T_Selection_ID");  
				String columnName = rs.getString("ColumnName");  
				String valueString = rs.getString("Value_String");  
				BigDecimal valueNumber = rs.getBigDecimal("Value_Number");  
				Timestamp valueDate = rs.getTimestamp("Value_Date");  
  
				Object value = null;  
				if (valueString != null)  
					value = valueString;  
				else if (valueNumber != null)  
					value = valueNumber;  
				else if (valueDate != null)  
					value = valueDate;  
  
				String key = columnName + "_" + T_Selection_ID;  
				selectionValueMap.put(key, value);  
			}  
		} catch (SQLException e) {  
			throw new AdempiereException("获取信息窗口值失败: " + e.getMessage());
		} finally {  
			DB.close(rs, pstmt);  
		}  
	}  
  
	/** 从 T_Selection_InfoWindow 读取指定行的【申购数量】 */  
	private BigDecimal getQtyToRequisition(int T_Selection_ID) {  
		Object value = selectionValueMap.get("QtyToRequisition_" + T_Selection_ID);  
		if (value instanceof BigDecimal)  
			return (BigDecimal) value;  
		return null;  
	}  
  
	@Override  
	protected String doIt() throws Exception {  
  
		// ================= 第一步：逐行计算，判断是否有申购数量超过剩余需求量的行 =================  
		StringBuilder overLimitMsg = new StringBuilder();  
		Map<Integer, BigDecimal> qtyMap = new HashMap<Integer, BigDecimal>();  
  
		for (Integer bomLineId : selectedBOMLineIds) {  
			MPPOrderBOMLine bomLine = new MPPOrderBOMLine(getCtx(), bomLineId, get_TrxName());  
			if (bomLine.get_ID() != bomLineId)  
				throw new AdempiereUserError("未找到工单BOM物料行: " + bomLineId);  
  
			BigDecimal qtyToRequisition = getQtyToRequisition(bomLineId);  
			if (qtyToRequisition == null || qtyToRequisition.signum() <= 0)  
				throw new AdempiereUserError("物料[" + bomLine.getM_Product_ID() + "]的申购数量必须大于0");  
  
			qtyMap.put(bomLineId, qtyToRequisition);  
  
			BigDecimal qtyRequiered = bomLine.getQtyRequiered() == null ? BigDecimal.ZERO : bomLine.getQtyRequiered(); // 需求数量
			BigDecimal qtyDelivered = bomLine.getQtyDelivered() == null ? BigDecimal.ZERO : bomLine.getQtyDelivered(); // 领用数量
			BigDecimal remainQty = qtyRequiered.subtract(qtyDelivered);  
  
			if (qtyToRequisition.compareTo(remainQty) > 0) {  
				MProduct product = MProduct.get(getCtx(), bomLine.getM_Product_ID());  
				overLimitMsg.append("物料[").append(product.getValue()).append(" ").append(product.getName())  
						.append("]：申购数量(").append(qtyToRequisition).append(") 大于 剩余需求量(")  
						.append(remainQty).append(")\n");  
			}  
		}  
  
		// ================= 第二步：如有超发，弹出二次确认；取消则整个流程终止，不生成任何数据 =================  
		if (overLimitMsg.length() > 0) {  
			if (processUI == null) {  
				// 无 UI 上下文（例如后台调度触发），无法弹窗确认，直接中止，不允许静默继续  
				throw new AdempiereException("存在物料申购数量大于需求数量，无法在无界面环境下确认，流程终止：\n" + overLimitMsg);  
			}  
  
			String confirmMsg = "存在物料申购数量已超过需求数量，是否继续申购？\n" + overLimitMsg.toString();
  
			final AtomicBoolean confirmed = new AtomicBoolean(false);  
			final AtomicBoolean answered = new AtomicBoolean(false);  
  
			processUI.ask(confirmMsg, result -> {  
				confirmed.set(result != null && result);  
				answered.set(true);  
			});  
  
			while (!answered.get()) {  
				try {  
					Thread.sleep(200);  
				} catch (InterruptedException e) {  
					Thread.currentThread().interrupt();  
					return "用户取消，未生成申购单";  
				}  
			}  
  
			if (!confirmed.get()) {  
				// 用户点取消：整个流程终止，一条申购单/申购明细都不生成  
				return "用户取消，未生成申购单";  
			}  
		}  
  
		// ================= 第三步：生成申购单表头（只生成一张，组织/仓库取工单） =================  
		// 取当前工单信息（所有勾选行均属于同一个工单，取第一行即可）  
		MPPOrderBOMLine firstLine = new MPPOrderBOMLine(getCtx(), selectedBOMLineIds.get(0), get_TrxName());  
		int ppOrderId = firstLine.getPP_Order_ID();  
		MPPOrder ppOrder = new MPPOrder(getCtx(), ppOrderId, get_TrxName());  
  
		MRequisition requisition = new MRequisition(getCtx(), 0, get_TrxName());  
		requisition.setAD_Org_ID(ppOrder.getAD_Org_ID());          // 组织取工单组织  
		requisition.setM_Warehouse_ID(ppOrder.getM_Warehouse_ID()); // 仓库取工单仓库  
		requisition.setAD_User_ID(Env.getAD_User_ID(getCtx())); // 申购人为当前登录用户
		requisition.set_CustomColumn("Purpose", "COP01");          // 生产物料申购  
		requisition.setDateRequired(p_DateRequired); // 流程参数 需求日期
		requisition.setDateDoc(new Timestamp(System.currentTimeMillis())); // 单据日期
		requisition.setDocStatus(MRequisition.DOCSTATUS_Drafted); // 单据状态 草稿
  
		// 单据类型：查找"采购申购单"标准文档类型（DocBaseType=POR），供 saveEx 落地必填字段使用  
		int reqDocTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_PurchaseRequisition);  
		if (reqDocTypeId <= 0)  
			throw new AdempiereException("未找到申购单(POR)的单据类型配置，请检查 AD_DocType");  
		requisition.setC_DocType_ID(reqDocTypeId);  
  
		requisition.setM_PriceList_ID(1000012);// 价格表：采购价格表

		requisition.saveEx();  

		// ================= 第四步：生成申购明细行 =================  
		String ppOrderIdStr = String.valueOf(ppOrderId); // 本次生成的所有明细行工单都相同  
		int lineNo = 10;  
  
		for (Integer bomLineId : selectedBOMLineIds) {  
			MPPOrderBOMLine bomLine = new MPPOrderBOMLine(getCtx(), bomLineId, get_TrxName());  
			MProduct product = MProduct.get(getCtx(), bomLine.getM_Product_ID()); // 物料
			BigDecimal qtyToRequisition = qtyMap.get(bomLineId); // 申购数量
  
			MRequisitionLine reqLine = new MRequisitionLine(requisition);  
			reqLine.setLine(lineNo); // 行号
			reqLine.setM_Product_ID(bomLine.getM_Product_ID()); // 物料
			reqLine.setC_UOM_ID(product.getC_UOM_ID()); // 单位
			reqLine.setQty(qtyToRequisition); // 申购数量
			reqLine.set_CustomColumn("PP_Order_IDs", ppOrderIdStr); // 自定义列：所属工单ID（逗号分隔字符串）  
			reqLine.saveEx();  
  
			// PriceActual：复用框架标准定价逻辑（按申购单表头的价格表取标准价）  
			reqLine.setPrice();  
			reqLine.saveEx();  
  
			lineNo += 10;  
		}  
  
		// ================= 第五步：记录日志，返回单号 =================  
		addLog(requisition.getM_Requisition_ID(), null, null,
				"生成申购单: " + requisition.getDocumentNo(),  
				MRequisition.Table_ID, requisition.getM_Requisition_ID());  
  
		return "";
	}  
}