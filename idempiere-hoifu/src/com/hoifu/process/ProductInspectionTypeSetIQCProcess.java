package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

/**
 * 批量设置物料检验类型：勾选多个 M_Product，
 * 校验物料是否有效（IsActive），若检验类型（InspectionType）为 无(NONE)，
 * 则修改为 来料检验(IQC)。
 *
 * 支持两种获取物料ID的方式（合并去重）：
 * 1. T_Selection 表（信息窗口多选行勾选）
 * 2. M_Product_ID 参数（ChosenMultipleSelectionSearch 多选参数，CSV格式）
 */
@org.adempiere.base.annotation.Process
public class ProductInspectionTypeSetIQCProcess extends SvrProcess {

	/** 检验类型编码：无 */
	private static final String INSPECTIONTYPE_NONE = "NONE";
	/** 检验类型编码：来料检验 */
	private static final String INSPECTIONTYPE_IQC = "IQC";

	private List<Integer> selectedProductIds = new ArrayList<>();

	@Override
	protected void prepare() {
		// 方式1：从 T_Selection 表读取（信息窗口勾选行）
		loadSelectedProducts();

		// 方式2：从 M_Product_ID 参数读取（ChosenMultipleSelectionSearch 多选参数，CSV格式）
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			if ("M_Product_ID".equals(p.getParameterName())) {
				int[] ids = p.getParameterAsIntArray();
				if (ids != null) {
					for (int id : ids) {
						selectedProductIds.add(id);
					}
				}
			}
		}

		// 去重（保持顺序）
		Set<Integer> unique = new LinkedHashSet<>(selectedProductIds);
		selectedProductIds = new ArrayList<>(unique);
	}

	@Override
	protected String doIt() throws Exception {
		if (selectedProductIds.isEmpty())
			throw new AdempiereUserError("请先勾选物料记录或通过参数选择物料");

		int updatedCount = 0;
		int skippedInactive = 0;
		int skippedNotNone = 0;

		for (Integer productId : selectedProductIds) {

			MProduct product = new MProduct(getCtx(), productId, get_TrxName());
			if (product == null || product.get_ID() == 0) {
				log.warning("物料不存在: " + productId);
				continue;
			}

			// 校验物料是否有效
			if (!product.isActive()) {
				log.warning("物料[" + product.getName() + "]已停用，跳过");
				skippedInactive++;
				continue;
			}

			// 读取自定义字段 InspectionType
			String inspectionType = (String) product.get_Value("InspectionType");

			// 当前为 无(NONE) 或空值 时才修改为 来料检验(IQC)
			if (INSPECTIONTYPE_NONE.equals(inspectionType) || inspectionType == null || inspectionType.trim().isEmpty()) {
				product.set_ValueOfColumn("InspectionType", INSPECTIONTYPE_IQC);
				if (product.save()) {
					updatedCount++;
				} else {
					log.warning("更新物料[" + product.getName() + "]检验类型失败");
				}
			} else {
				skippedNotNone++;
				if (log.isLoggable(Level.FINE))
					log.fine("物料[" + product.getName() + "]检验类型非[无/空]，当前值=" + inspectionType + "，跳过");
			}
		}

		return "已更新 " + updatedCount + " 条，跳过停用 " + skippedInactive
				+ " 条，跳过非[无/空]检验类型 " + skippedNotNone + " 条";
	}

	/**
	 * 从 T_Selection 表加载用户勾选的 M_Product_ID（信息窗口多选行方式）
	 */
	private void loadSelectedProducts() {
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				selectedProductIds.add(rs.getInt(1));
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取勾选的物料记录失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}
}
