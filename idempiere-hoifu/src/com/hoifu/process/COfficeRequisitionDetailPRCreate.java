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

import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

import com.hoifu.model.X_C_OfficeRequisition;
import com.hoifu.model.X_C_OfficeRequisitionLine;

/**
 * 创建申购单 根据 C_OfficeRequisitionLine 。 申购数量 = 需求数量 - 当前库存数量（若 <= 0 则跳过）。
 * M_RequisitionLine 上通过自定义列 C_OfficeRequisitionLine_ID 关联回来。
 */
@org.adempiere.base.annotation.Process
public class COfficeRequisitionDetailPRCreate extends SvrProcess {

	private List<Integer> selectedLineIds = new ArrayList<>();
	private String p_Purpose = null;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			if ("Purpose".equals(p.getParameterName()))
				p_Purpose = p.getParameterAsString();
		}
	}

	@Override
	protected String doIt() throws Exception {
		loadSelectedLines();
		if (selectedLineIds.isEmpty())
			throw new AdempiereUserError("请先勾选明细记录");

		// 按 org+warehouse 分组
		Map<String, List<X_C_OfficeRequisitionLine>> warehouseGroups = new HashMap<>();

		for (Integer lineId : selectedLineIds) {
			X_C_OfficeRequisitionLine line = new X_C_OfficeRequisitionLine(getCtx(), lineId, get_TrxName());
			if (line.get_ID() == 0)
				throw new AdempiereUserError("领用单明细行不存在: " + lineId);
			if (line.getM_Product_ID() == 0)
				throw new AdempiereUserError("明细行无产品信息: " + lineId);

			String status = (String) line.get_Value("ClaimStatus");
			if ("YL".equals(status)) {
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());
				throw new AdempiereUserError("物料[" + product.getName() + "]已领用，无需申购");
			}

			X_C_OfficeRequisition header = new X_C_OfficeRequisition(getCtx(), line.getC_OfficeRequisition_ID(),
					get_TrxName());
			String key = header.getAD_Org_ID() + "_" + header.getM_Warehouse_ID();
			warehouseGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
		}

		List<String> createdNos = new ArrayList<>();
		int reqDocTypeId = getRequisitionDocTypeId();

		for (Map.Entry<String, List<X_C_OfficeRequisitionLine>> entry : warehouseGroups.entrySet()) {
			String[] keys = entry.getKey().split("_");
			int orgId = Integer.parseInt(keys[0]);
			int warehouseId = Integer.parseInt(keys[1]);
			List<X_C_OfficeRequisitionLine> lines = entry.getValue();

			MRequisition requisition = new MRequisition(getCtx(), 0, get_TrxName());
			requisition.setAD_Org_ID(orgId);
			requisition.setAD_User_ID(getAD_User_ID());
			requisition.setM_Warehouse_ID(warehouseId);
			requisition.setDateDoc(new Timestamp(System.currentTimeMillis()));
			requisition.setDateRequired(new Timestamp(System.currentTimeMillis()));
			requisition.setPriorityRule("5");
			requisition.set_CustomColumn("Purpose", (p_Purpose != null && !p_Purpose.isEmpty()) ? p_Purpose : "COP02");
			requisition.setC_DocType_ID(reqDocTypeId);
			requisition.saveEx();

			int lineNo = 10;
			int createdLines = 0;

			for (X_C_OfficeRequisitionLine offLine : lines) {
				BigDecimal demand = (BigDecimal) offLine.get_Value("QtyDemand");
				if (demand == null)
					demand = Env.ZERO;

				BigDecimal stock = MStorageOnHand.getQtyOnHandForLocator(offLine.getM_Product_ID(),
						offLine.getM_Locator_ID(), 0, get_TrxName());

				BigDecimal reqQty = demand.subtract(stock);
				if (reqQty.signum() <= 0)
					continue; // 库存充足，无需申购

				MProduct product = MProduct.get(getCtx(), offLine.getM_Product_ID());

				MRequisitionLine reqLine = new MRequisitionLine(requisition);
				reqLine.setLine(lineNo);
				reqLine.setM_Product_ID(offLine.getM_Product_ID());
				reqLine.setC_UOM_ID(product.getC_UOM_ID());
				reqLine.setM_AttributeSetInstance_ID(offLine.getM_AttributeSetInstance_ID());
				reqLine.setQty(reqQty);
				reqLine.setPriceActual(getProductPrice(offLine.getM_Product_ID()));
				// 关联回 C_OfficeRequisitionLine（需在AD注册此自定义列）
				reqLine.set_CustomColumn("C_OfficeRequisitionLine_ID", offLine.getC_OfficeRequisitionLine_ID());
				reqLine.saveEx();

				// 更新领用状态为"采购中"
				offLine.set_CustomColumn("ClaimStatus", "CG");
				offLine.saveEx();

				lineNo += 10;
				createdLines++;
			}

			if (createdLines == 0) {
				// 所有明细库存充足，删除空申购单
				requisition.deleteEx(true);
				continue;
			}

			addBufferLog(0, null, requisition.getTotalLines(),
					Msg.parseTranslation(getCtx(), "@Created@ @M_Requisition_ID@ ") + requisition.getDocumentNo(),
					MRequisition.Table_ID, requisition.getM_Requisition_ID());

			createdNos.add(requisition.getDocumentNo());
		}

		if (createdNos.isEmpty())
			return "所有选中物料库存充足，无需创建申购单";

		return "@Created@ " + String.join(", ", createdNos);
	}

	private int getRequisitionDocTypeId() {
		String sql = "SELECT C_DocType_ID FROM C_DocType "
				+ "WHERE AD_Client_ID = ? AND Name = '申购单' AND IsActive = 'Y'";
		int id = DB.getSQLValueEx(get_TrxName(), sql, getAD_Client_ID());
		if (id <= 0) {
			sql = "SELECT C_DocType_ID FROM C_DocType "
					+ "WHERE AD_Client_ID = ? AND DocBaseType = 'POR' AND IsActive = 'Y' "
					+ "ORDER BY IsDefault DESC, C_DocType_ID FETCH FIRST 1 ROWS ONLY";
			id = DB.getSQLValueEx(get_TrxName(), sql, getAD_Client_ID());
		}
		if (id <= 0)
			throw new AdempiereUserError("未找到申购单文档类型，请配置DocBaseType='POR'或名称为'申购单'的文档类型");
		return id;
	}

	private BigDecimal getProductPrice(int productId) {
		String sql = "SELECT pp.PriceStd FROM M_ProductPrice pp "
				+ "JOIN M_PriceList_Version plv ON pp.M_PriceList_Version_ID = plv.M_PriceList_Version_ID "
				+ "JOIN M_PriceList pl ON plv.M_PriceList_ID = pl.M_PriceList_ID "
				+ "WHERE pl.AD_Client_ID = ? AND pl.IsDefault = 'Y' AND pl.IsSOPriceList = 'N' "
				+ "AND pl.IsActive = 'Y' AND plv.IsActive = 'Y' AND pp.IsActive = 'Y' "
				+ "AND pp.M_Product_ID = ? AND plv.ValidFrom <= ? "
				+ "ORDER BY plv.ValidFrom DESC FETCH FIRST 1 ROWS ONLY";
		BigDecimal price = DB.getSQLValueBD(get_TrxName(), sql, getAD_Client_ID(), productId,
				new Timestamp(System.currentTimeMillis()));
		return price != null ? price : Env.ZERO;
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