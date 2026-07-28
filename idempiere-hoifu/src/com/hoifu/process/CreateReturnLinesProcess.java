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
import org.compiere.model.MLocator;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

/**
 * 从信息窗口选中的领用明细行，在退库单中创建对应的明细
 * @see CreateFromInOut — T_Selection + T_Selection_InfoWindow 读取模式
 */
@org.adempiere.base.annotation.Process
public class CreateReturnLinesProcess extends SvrProcess {

	/** 退库单 ID（通过进程参数传入，默认值 @M_Inventory_ID@） */
	private int p_M_Inventory_ID = 0;

	/** 从信息窗口选中的领用明细行 ID 列表 */
	private final List<Integer> selectedLineIds = new ArrayList<>();

	/** 信息窗口中用户修改的字段值，key = "ColumnName_T_Selection_ID" */
	private final Map<String, Object> selectionValueMap = new HashMap<>();

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("M_Inventory_ID"))
				p_M_Inventory_ID = para.getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
		}
		loadSelectedLines();
	}

	/** 从 T_Selection 和 T_Selection_InfoWindow 读取信息窗口中勾选的行及用户修改的值 */
	private void loadSelectedLines() {
		// ① 读取选中的行 ID
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
			throw new IllegalArgumentException("获取领用单明细失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}

		// ② 读取用户修改的字段值（退库数量、库位等）
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
				String ColumnName = rs.getString("ColumnName");
				String Value_String = rs.getString("Value_String");

				Object Value_Number = null;
				if (ColumnName.toUpperCase().endsWith("_ID"))
					Value_Number = rs.getInt("Value_Number");
				else
					Value_Number = rs.getBigDecimal("Value_Number");

				Timestamp Value_Date = rs.getTimestamp("Value_Date");

				String key = ColumnName + "_" + T_Selection_ID;
				Object value = null;
				if (Value_String != null)
					value = Value_String;
				else if (Value_Number != null)
					value = Value_Number;
				else if (Value_Date != null)
					value = Value_Date;
				selectionValueMap.put(key, value);
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取信息窗口修改值失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/** 从 selectionValueMap 中获取指定列的值 */
	@SuppressWarnings("unchecked")
	private <T> T getSelectionValue(String columnName, int T_Selection_ID) {
		String key = columnName + "_" + T_Selection_ID;
		return (T) selectionValueMap.get(key);
	}

	@Override
	protected String doIt() throws Exception {
		if (p_M_Inventory_ID == 0)
			throw new AdempiereUserError("@FillMandatory@ @M_Inventory_ID@");
		if (selectedLineIds.isEmpty())
			throw new AdempiereUserError("请先勾选要退库的明细行");

		// 获取退库单
		MInventory returnDoc = new MInventory(getCtx(), p_M_Inventory_ID, get_TrxName());
		if (returnDoc.get_ID() == 0)
			throw new AdempiereUserError("退库单不存在");
		if (returnDoc.isProcessed())
			throw new AdempiereUserError("退库单已处理，不能添加明细");

		// 获取当前最大行号，避免与已有明细冲突
		int lineNo = DB.getSQLValue(get_TrxName(),
				"SELECT COALESCE(MAX(Line), 0) + 10 FROM M_InventoryLine WHERE M_Inventory_ID = ?",
				p_M_Inventory_ID);

		int created = 0;
		int skipped = 0;
		for (Integer T_Selection_ID : selectedLineIds) {
			// 从信息窗口修改值中读取用户填写的退库数量
			BigDecimal returnQty = getSelectionValue("QtyInternalUse", T_Selection_ID);
			if (returnQty == null || returnQty.signum() <= 0) {
				log.warning("退库数量为空或为0，跳过: " + T_Selection_ID);
				continue;
			}

			// 从信息窗口修改值中读取用户填写的库位
			int locatorId = 0;
			Integer locatorValue = getSelectionValue("M_Locator_ID", T_Selection_ID);
			if (locatorValue != null && locatorValue > 0) {
				locatorId = locatorValue;
				// 校验库位是否属于退库单所在组织
				MLocator locator = new MLocator(getCtx(), locatorId, get_TrxName());
				if (locator.get_ID() == 0 || locator.getAD_Org_ID() != returnDoc.getAD_Org_ID()) {
					throw new AdempiereUserError("选中的领用明细行（T_Selection_ID=" + T_Selection_ID + "）修改的库位不属于当前组织");
				}
			}

			// 从 T_Selection 行 ID 读取领用明细的基础信息（产品、属性实例、费用等）
			MInventoryLine reqLine = new MInventoryLine(getCtx(), T_Selection_ID, get_TrxName());
			if (reqLine.get_ID() == 0) {
				log.warning("领用单明细不存在: " + T_Selection_ID);
				continue;
			}

			// 检查该源领用明细行是否已存在于当前退库单中，防止重复添加
			int exists = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM M_InventoryLine WHERE M_Inventory_ID = ? AND Ref_InventoryLine_ID = ?",
					p_M_Inventory_ID, reqLine.get_ID());
			if (exists > 0) {
				String productInfo = reqLine.getM_Product_ID() > 0
						? reqLine.getM_Product().getName()
						: "费用";
				log.warning("领用明细行（" + productInfo + "，ID=" + reqLine.get_ID() + "）已存在于退库单中，跳过重复添加");
				skipped++;
				continue;
			}

			// 创建退库明细行
			MInventoryLine returnLine = new MInventoryLine(getCtx(), 0, get_TrxName());
			returnLine.setM_Inventory_ID(p_M_Inventory_ID);
			returnLine.setLine(lineNo);
			returnLine.setM_Product_ID(reqLine.getM_Product_ID());
			// 使用用户修改的库位，如果没有修改则使用原值
			returnLine.setM_Locator_ID(locatorId > 0 ? locatorId : reqLine.getM_Locator_ID());
			returnLine.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());
			returnLine.setC_Charge_ID(reqLine.getC_Charge_ID());
			returnLine.setQtyInternalUse(returnQty);
			returnLine.set_ValueOfColumn("Ref_InventoryLine_ID", reqLine.get_ID());
			returnLine.saveEx(get_TrxName());

			lineNo += 10;
			created++;
		}

		String msg = "成功创建 " + created + " 条退库明细";
		if (skipped > 0) {
			msg += "，跳过 " + skipped + " 条重复明细";
		}
		return msg;
	}
}
