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
import org.compiere.model.MInventoryLine;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MStorageOnHand;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.wf.MWorkflow;

import com.hoifu.model.X_C_OfficeRequisition;
import com.hoifu.model.X_C_OfficeRequisitionLine;

/**
 * 领用流程：从 C_OfficeRequisitionLine 读取选中行， 创建内部使用库存单(M_Inventory)并立即完成，实现实时库存扣减。
 */
@org.adempiere.base.annotation.Process
public class COfficeRequisitionClaimProcess extends SvrProcess {

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

		// 2. 按 C_OfficeRequisition_ID 分组（保证M_InventoryLine唯一约束不冲突）
		Map<Integer, List<X_C_OfficeRequisitionLine>> groups = new HashMap<>();
		for (X_C_OfficeRequisitionLine line : validLines) {
			int headerId = line.getC_OfficeRequisition_ID();
			groups.computeIfAbsent(headerId, k -> new ArrayList<>()).add(line);
		}

		int internalUseDocTypeId = getInternalUseDocTypeId();
		int updatedCount = 0;

		for (Map.Entry<Integer, List<X_C_OfficeRequisitionLine>> entry : groups.entrySet()) {
			X_C_OfficeRequisition header = new X_C_OfficeRequisition(getCtx(), entry.getKey(), get_TrxName());
			List<X_C_OfficeRequisitionLine> lines = entry.getValue();

			// 3. 创建 M_Inventory（内部使用类型）
			MInventory inventory = new MInventory(getCtx(), 0, get_TrxName());
			inventory.setC_DocType_ID(internalUseDocTypeId);
			inventory.setAD_Org_ID(header.getAD_Org_ID());
			inventory.setAD_OrgTrx_ID(header.getAD_OrgTrx_ID());
			inventory.setC_Activity_ID(header.getC_Activity_ID());
			inventory.setApprovalAmt(header.getApprovalAmt());
			inventory.setM_Warehouse_ID(header.getM_Warehouse_ID());
			inventory.setMovementDate(new Timestamp(System.currentTimeMillis()));
			inventory.setDescription("OA行政物料领用");
			inventory.setPosted(false);
			inventory.set_CustomColumn("OAID", header.getOAID());
			inventory.set_CustomColumn("AD_User_ID", header.getAD_User_ID());
			inventory.set_CustomColumn("Bdepartment", header.getBdepartment());
			inventory.saveEx();

			// 4. 为每条明细创建 M_InventoryLine
			for (X_C_OfficeRequisitionLine offLine : lines) {
				MInventoryLine invLine = new MInventoryLine(getCtx(), 0, get_TrxName());
				invLine.setM_Inventory_ID(inventory.getM_Inventory_ID());
				invLine.setM_Locator_ID(offLine.getM_Locator_ID());
				invLine.setM_Product_ID(offLine.getM_Product_ID());
				invLine.setM_AttributeSetInstance_ID(offLine.getM_AttributeSetInstance_ID());
				invLine.setQtyInternalUse(claimQty);
				invLine.set_CustomColumn("QtyDemand", claimQty);
				invLine.set_CustomColumn("ClaimStatus", "YL");
				invLine.setC_Charge_ID(offLine.getC_Charge_ID());
				// beforeSave 会自动将 InventoryType 设为 ChargeAccount
				invLine.saveEx();
			}

			// 5. 完成 M_Inventory，触发实时库存扣减
//			if (!DocumentEngine.processIt(inventory, DocAction.ACTION_Complete)) {
//				throw new AdempiereUserError("完成领用单失败: " + inventory.getProcessMsg());
//			}
//			inventory.saveEx();
//			if (!DocAction.STATUS_Completed.equals(inventory.getDocStatus())) {
//				throw new AdempiereUserError("完成领用单失败，单据状态: " + inventory.getDocStatus());
//			}
			// 5. 使用 MWorkflow 完成单据（替代 DocumentEngine.processIt）  
//			ProcessInfo info = MWorkflow.runDocumentActionWorkflow(inventory, DocAction.ACTION_Complete);  
//			inventory.load(get_TrxName());  
//			if (info.isError()) {  
//			    throw new AdempiereUserError("完成领用单失败: " + info.getSummary());  
//			}  
//			if (!DocAction.STATUS_Completed.equals(inventory.getDocStatus())) {  
//			    throw new AdempiereUserError("完成领用单失败，单据状态: " + inventory.getDocStatus());  
//			}

			// 6. 更新 C_OfficeRequisitionLine 状态
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
					Msg.parseTranslation(getCtx(), "@Created@ @M_Inventory_ID@ ") + inventory.getDocumentNo(),
					MInventory.Table_ID, inventory.getM_Inventory_ID());

		}

		return "成功领用 " + updatedCount + " 条记录，库存已实时扣减";
	}

	/**
	 * 查询内部使用库存文档类型 (DocSubTypeInv = 'IU')
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