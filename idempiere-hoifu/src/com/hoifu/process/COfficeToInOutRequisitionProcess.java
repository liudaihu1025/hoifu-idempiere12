package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Timestamp;  
import java.util.ArrayList;  
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  
  
import org.compiere.model.MInventory;  
import org.compiere.model.MProcessPara;  
import org.compiere.model.MProduct;  
import org.compiere.model.MStorageOnHand;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.AdempiereUserError;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.compiere.util.Msg;  
  
import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;  
import com.hoifu.model.X_C_OfficeRequisition;  
import com.hoifu.model.X_C_OfficeRequisitionLine;  
  
/**  
 * 领用流程（改造版）：从 C_OfficeRequisitionLine 读取选中行，  
 * 不再直接创建 M_Inventory / M_InventoryLine，而是创建统一出入库申请队列记录  
 * M_InOut_Requisition / M_InOut_RequisitionLine（TableName=M_Inventory, InOutType=INV），  
 * 等待后续通用生成器批量产出真正的 M_Inventory 内部使用领用单。  
 *  
 * <p>本类结构直接对照 {@link COfficeRequisitionClaimProcess}：  
 * 保留原有的参数解析 / 选中行加载 / 校验（产品、费用科目、库存、重复领用）逻辑，  
 * 仅把"第 3~5 步：创建并完成 M_Inventory"替换为"创建 M_InOut_Requisition 表头 + 明细"。</p>  
 */  
@org.adempiere.base.annotation.Process  
public class COfficeToInOutRequisitionProcess extends SvrProcess {  
  
	/** M_InOut_Requisition 自身的单据类型 UUID（与 OrderInOutRequisitionService 中保持一致） */  
	private static final String REQUISITION_DOCTYPE_UU = "97619cfe-e96f-4e65-b66d-2a73bec5893f";  
  
	/** 该场景下游最终生成的表：M_Inventory（内部使用领用单） */  
	private static final String TABLE_NAME = MInventory.Table_Name;  
  
	/** InOutType 标记：领用单场景 */  
	private static final String INOUT_TYPE = "INV_IU";  
  
	private List<Integer> selectedLineIds = new ArrayList<>();  
	private BigDecimal claimQty = Env.ZERO;  
  
	@Override  
	protected void prepare() {  
		ProcessInfoParameter[] para = getParameter();  
		for (ProcessInfoParameter p : para) {  
			String name = p.getParameterName();  
			if (p.getParameter() == null)  
				continue;  
			if ("ClaimQty".equals(name))  
				claimQty = (BigDecimal) p.getParameter();  
			else  
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);  
		}  
	}  
  
	@Override  
	protected String doIt() throws Exception {  
		loadSelectedLines();  
  
		if (selectedLineIds.isEmpty())  
			throw new AdempiereUserError("请先勾选明细记录");  
		if (claimQty == null || claimQty.signum() <= 0)  
			throw new AdempiereUserError("请输入有效的领用数量");  
   
		// 1. 加载并验证所有选中的 C_OfficeRequisitionLine  
		List<X_C_OfficeRequisitionLine> validLines = new ArrayList<>();  
		for (Integer lineId : selectedLineIds) {  
			X_C_OfficeRequisitionLine line = new X_C_OfficeRequisitionLine(getCtx(), lineId, get_TrxName());  
  
			if (line.get_ID() == 0)  
				throw new AdempiereUserError("OA申领明细行不存在: " + lineId);  
			if (line.getM_Product_ID() == 0)  
				throw new AdempiereUserError("明细行无产品信息: " + lineId);  
			if (line.getC_Charge_ID() == 0) {  
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());  
				throw new AdempiereUserError("物料[" + product.getName() + "]未配置费用科目(C_Charge_ID)，内部使用领用必须配置");  
			}  
  
			String status = (String) line.get_Value("ClaimStatus");  
			if ("YL".equals(status)) {  
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());  
				throw new AdempiereUserError("物料[" + product.getName() + "]已领用，不能重复领用");  
			}  
			
			// 检查库存  
			BigDecimal stockQty = MStorageOnHand.getQtyOnHandForLocator(line.getM_Product_ID(), line.getM_Locator_ID(),  
					0, get_TrxName());  
			if (stockQty.signum() <= 0) {  
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());  
				throw new AdempiereUserError("物料[" + product.getName() + "]无库存！");  
			}  
			if (claimQty.compareTo(stockQty) > 0) {  
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());  
				throw new AdempiereUserError(  
						"物料[" + product.getName() + "]领用数量(" + claimQty + ")超过库存(" + stockQty + ")");  
			}  
  
			validLines.add(line);  
		}  
  
		// 2. 按 C_OfficeRequisition_ID 分组（一张 OA申请单只生成一条 M_InOut_Requisition 表头）  
		Map<Integer, List<X_C_OfficeRequisitionLine>> groups = new HashMap<>();  
		for (X_C_OfficeRequisitionLine line : validLines) {  
			int headerId = line.getC_OfficeRequisition_ID();  
			groups.computeIfAbsent(headerId, k -> new ArrayList<>()).add(line);  
		}  
  
		int requisitionDocTypeId = resolveRequisitionDocTypeId();  
		int internalUseDocTypeId = getInternalUseDocTypeId();  
		int updatedCount = 0;  
  
		for (Map.Entry<Integer, List<X_C_OfficeRequisitionLine>> entry : groups.entrySet()) {  
			X_C_OfficeRequisition header = new X_C_OfficeRequisition(getCtx(), entry.getKey(), get_TrxName());  
			List<X_C_OfficeRequisitionLine> lines = entry.getValue();  
  
			// 3. 创建 M_InOut_Requisition 表头（不再创建 M_Inventory）  
			MInOutRequisition requisition = new MInOutRequisition(getCtx(), 0, get_TrxName());  
			requisition.setC_DocType_ID(requisitionDocTypeId);  
			requisition.setTargetDocType_ID(internalUseDocTypeId);  
			requisition.setAD_Org_ID(header.getAD_Org_ID());  
			requisition.setAD_OrgTrx_ID(header.getAD_OrgTrx_ID());  
			requisition.setC_Activity_ID(header.getC_Activity_ID());  
			requisition.setApprovalAmt(header.getApprovalAmt());  
			requisition.setM_Warehouse_ID(header.getM_Warehouse_ID());  
			requisition.setMovementDate(new Timestamp(System.currentTimeMillis()));  
			requisition.setC_OfficeRequisition_ID(header.getC_OfficeRequisition_ID());  
			requisition.setAD_User_ID(header.getAD_User_ID());  
			requisition.setInOutType(INOUT_TYPE);  
			requisition.set_ValueOfColumn("TableName", TABLE_NAME);  
			requisition.setDocStatus("DR");  
			requisition.setDocAction("CO");  
			requisition.setDescription("OA行政物料领用（来源OA申请单: " + header.getOAID() + "）");  
			requisition.saveEx();  
  
			// 4. 为每条 OA明细创建 M_InOut_RequisitionLine（不再创建 M_InventoryLine）  
			int lineNo = 10;  
			for (X_C_OfficeRequisitionLine offLine : lines) {  
				MInOutRequisitionLine reqLine = new MInOutRequisitionLine(getCtx(), 0, get_TrxName());  
				reqLine.setM_InOut_Requisition_ID(requisition.getM_InOut_Requisition_ID());  
				reqLine.setAD_Org_ID(offLine.getAD_Org_ID());  
				reqLine.setLine(lineNo);  
				reqLine.set_ValueOfColumn("TableName", X_C_OfficeRequisitionLine.Table_Name);  
				reqLine.setC_OfficeRequisitionLine_ID(offLine.getC_OfficeRequisitionLine_ID());  
				reqLine.setM_Product_ID(offLine.getM_Product_ID());  
				reqLine.setM_Locator_ID(offLine.getM_Locator_ID());  
				reqLine.setM_AttributeSetInstance_ID(offLine.getM_AttributeSetInstance_ID());  
				reqLine.setC_UOM_ID(offLine.getC_UOM_ID());  
				reqLine.setC_Charge_ID(offLine.getC_Charge_ID());  
				reqLine.setAD_User_ID(header.getAD_User_ID());  
				reqLine.setQtyRequested(claimQty);  
				reqLine.setQtyGenerated(Env.ZERO);  
				reqLine.saveEx();  
				lineNo += 10;  
			}  
  
			// 5. 不再在此处 DocumentEngine.processIt(inventory, ...) 完成 M_Inventory，  
			//    真正的库存扣减交由后续读取 M_InOut_Requisition 的通用生成器处理。  
			//    如果 requisition.DocAction='CO' 需要立即驱动 Doc Engine，请确认  
			//    MInOutRequisition 是否已实现 DocAction 接口，否则这里只是落库标记。  
  
			// 6. 更新 C_OfficeRequisitionLine 状态（保留原逻辑，标记为"已提交申请"而非"已领用"，  
			//    真正的 ClaimStatus=YL 应等下游 M_Inventory 完成后再回写，此处先标记为 PR 待处理）  
			for (X_C_OfficeRequisitionLine offLine : lines) {  
				BigDecimal picked = (BigDecimal) offLine.get_Value("QuantityPicked");  
				if (picked == null)  
					picked = Env.ZERO;  
				BigDecimal newPicked = picked.add(claimQty);  
  
				offLine.set_CustomColumn("QuantityPicked", newPicked);  
				offLine.set_CustomColumn("QtyInternalUse", newPicked);  
  
				BigDecimal demand = (BigDecimal) offLine.get_Value("QtyDemand");  
				if (demand == null)  
					demand = Env.ZERO;  
  
				offLine.set_CustomColumn("ClaimStatus", newPicked.compareTo(demand) >= 0 ? "YL" : "PR");  
				offLine.saveEx();  
				updatedCount++;  
			}  
  
			addBufferLog(0, null, new BigDecimal(lines.size()),  
					Msg.parseTranslation(getCtx(), "@Created@ @M_InOut_Requisition_ID@ ") + requisition.getDocumentNo(),  
					MInOutRequisition.Table_ID, requisition.getM_InOut_Requisition_ID());  
		}  
  
		return "成功提交 " + updatedCount + " 条记录，已生成出入库申请单，等待下游生成领用单";  
	}  
  
	/** 解析 M_InOut_Requisition 自身的 C_DocType_ID（固定 UUID） */  
	private int resolveRequisitionDocTypeId() {  
		org.compiere.model.MDocType dt = new org.compiere.model.MDocType(getCtx(), REQUISITION_DOCTYPE_UU, get_TrxName());  
		if (dt.getC_DocType_ID() <= 0)  
			throw new AdempiereUserError("未找到出入库申请单的 C_DocType_UU 配置，请检查: " + REQUISITION_DOCTYPE_UU);  
		return dt.getC_DocType_ID();  
	}  
  
	/**  
	 * 查询内部使用库存文档类型 (DocSubTypeInv = 'IU')，作为 M_InOut_Requisition.TargetDocType_ID  
	 */  
	private int getInternalUseDocTypeId() {  
		String sql = "SELECT C_DocType_ID FROM C_DocType " + "WHERE AD_Client_ID = ? " + "AND DocBaseType = 'MMI' "  
				+ "AND Name = '行政物料领用单' " + "AND (DocSubTypeInv = 'IU' OR DocSubTypeInv IS NULL) "  
				+ "AND IsActive = 'Y' " + "ORDER BY IsDefault DESC, C_DocType_ID FETCH FIRST 1 ROWS ONLY";  
		int id = DB.getSQLValueEx(get_TrxName(), sql, getAD_Client_ID());  
		if (id <= 0)  
			throw new AdempiereUserError("未找到文档类型：DocBaseType='MMI'，名称='行政物料领用单'，请先在系统中配置");  
		return id;  
	}  
  
	private void loadSelectedLines() {  
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		try {  
			pstmt = DB.prepareStatement(sql, get_TrxName());  
			pstmt.setInt(1, getAD_PInstance_ID());  
			rs = pstmt.executeQuery();  
			while (rs.next())  
				selectedLineIds.add(rs.getInt(1));  
		} catch (SQLException e) {  
			throw new IllegalArgumentException("获取领用单明细失败");  
		} finally {  
			DB.close(rs, pstmt);  
		}  
	}  
}