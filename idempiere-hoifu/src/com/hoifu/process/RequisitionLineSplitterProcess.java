package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class RequisitionLineSplitterProcess extends SvrProcess {

	/** 拆分后 新行 使用的物料 */
	private int p_M_Product_ID = 0;
	/** 拆分出去的数量 */
	private BigDecimal p_Qty = null;

	private List<Integer> selectedLineIds = new ArrayList<>();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null);
			else if (name.equals("M_Product_ID"))
				p_M_Product_ID = para[i].getParameterAsInt();
			else if (name.equals("Qty"))
				p_Qty = (BigDecimal) para[i].getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_M_Product_ID <= 0)
			throw new AdempiereUserError("请选择拆分【物料】");
		if (p_Qty == null || p_Qty.signum() <= 0)
			throw new AdempiereUserError("拆分【数量】必须大于0");

		loadSelectedLines();
		if (selectedLineIds.isEmpty())
			throw new AdempiereUserError("请先勾选需要拆分的申购行");

		int count = 0;
		for (Integer lineId : selectedLineIds) {
			MRequisitionLine oldLine = new MRequisitionLine(getCtx(), lineId, get_TrxName());
			if (oldLine.get_ID() <= 0) {
				if (log.isLoggable(Level.WARNING))
					log.warning("RequisitionLine not found: " + lineId);
				continue;
			}

			// 拆分数量必须小于原行数量（否则原行数量会变成0或负数，没有意义）
			if (p_Qty.compareTo(oldLine.getQty()) >= 0)
				throw new AdempiereUserError(
						"拆分数量(" + p_Qty + ")必须小于申购行的原数量(" + oldLine.getQty() + ")");

			// 1. 扣减原行数量
			BigDecimal remainingQty = oldLine.getQty().subtract(p_Qty);
			oldLine.setQty(remainingQty);
			oldLine.setLineNetAmt();
			oldLine.saveEx(get_TrxName());

			// 2. 创建新行：继承原行的父申购单/客户/组织等信息
			MRequisitionLine newLine = new MRequisitionLine(oldLine.getParent());

			// 3. 其他字段继承原行
			newLine.setM_AttributeSetInstance_ID(oldLine.getM_AttributeSetInstance_ID());
			newLine.setC_BPartner_ID(oldLine.getC_BPartner_ID());
			newLine.setC_Charge_ID(oldLine.getC_Charge_ID());
			newLine.setPriceActual(oldLine.getPriceActual());
			newLine.setDescription(oldLine.getDescription());
			newLine.setC_OrderLine_ID(0);

			// 自定义字段复制（若模型尚未生成对应 getter/setter，用 get_Value/set_CustomColumn）
			newLine.set_CustomColumn("RSupplier", oldLine.get_Value("RSupplier"));
			newLine.set_CustomColumn("ENo", oldLine.get_Value("ENo"));
			newLine.set_CustomColumn("PP_Order_ID", oldLine.get_Value("PP_Order_ID"));

			// 4. 覆盖【物料】和【数量】参数（不再手动设置 C_UOM_ID，交给 beforeSave 按新物料取默认单位）
			newLine.setM_Product_ID(p_M_Product_ID);
			newLine.setQty(p_Qty);
			newLine.setLineNetAmt();

			newLine.saveEx(get_TrxName());

			count++;
		}

		return "@Processed@ #" + count;
	}

	private void loadSelectedLines() {
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				selectedLineIds.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取明细记录失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}
}