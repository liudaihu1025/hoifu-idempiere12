package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.model.MInventoryLine;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
/**
 * 行政/办公物料领用单明细更新状态字段
 */
@org.adempiere.base.annotation.Process
public class OfficeRequisitionClaimStatusProcess extends SvrProcess {
	private List<Integer> selectedLineIds = new ArrayList<>();

	private BigDecimal claimQty = Env.ZERO; // 添加领用数量参数

	/**
	 * 准备方法 - 接收参数
	 */
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();

		// 处理流程参数
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null) {
				continue;
			} else if (name.equals("ClaimQty"))
				claimQty = (BigDecimal) para[i].getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	/**
	 * 执行流程
	 */
	protected String doIt() throws Exception {
		// 加载选中的记录
		loadSelectedLines();

		if (selectedLineIds.isEmpty()) {
			throw new AdempiereUserError("请先勾选明细记录");
		}

		// 验证领用数量参数
		if (claimQty == null || claimQty.signum() <= 0) {
			throw new AdempiereUserError("请输入有效的领用数量");
		}

		// 首先验证所有记录的基础条件
		List<MInventoryLine> validLines = new ArrayList<>();

		for (Integer lineId : selectedLineIds) {
			MInventoryLine line = new MInventoryLine(getCtx(), lineId, get_TrxName());

			if (line.get_ID() == 0) {
				throw new AdempiereUserError("领用单明细行不存在: " + lineId);
			}

			if (line.getParent().isProcessed()) {
				throw new AdempiereUserError("单据已处理，不能更新明细行: " + lineId);
			}
	  
			if (line.getM_Product_ID() == 0) {
				throw new AdempiereUserError("明细行无产品信息: " + lineId);
			}
	  
			String currentStatus = (String) line.get_Value("ClaimStatus");
			if ("YL".equals(currentStatus)) {
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());
				throw new AdempiereUserError("物料[" + product.getName() + "]当前状态为["
						+ (currentStatus != null ? currentStatus : "空") + "]，已领用。");
			}
	  
			validLines.add(line);
		}
	  
		// 按产品分组收集库位信息
		Map<Integer, List<Integer>> productLocators = new HashMap<>();
		Map<Integer, List<MInventoryLine>> productLines = new HashMap<>();
	  
		for (MInventoryLine line : validLines) {
			int productId = line.getM_Product_ID();

			if (!productLocators.containsKey(productId)) {
				productLocators.put(productId, new ArrayList<>());
				productLines.put(productId, new ArrayList<>());
			}

			Integer locatorId = line.getM_Locator_ID();
			if (!productLocators.get(productId).contains(locatorId)) {
				productLocators.get(productId).add(locatorId);
			}

			productLines.get(productId).add(line);
		}
	  
		// 检查每个产品的库存（只要大于0即可）
		for (Map.Entry<Integer, List<Integer>> entry : productLocators.entrySet()) {
			int productId = entry.getKey();
			BigDecimal totalStockQty = getTotalStockOnHand(productId, entry.getValue());

			if (totalStockQty.signum() <= 0) {
				MProduct product = MProduct.get(getCtx(), productId);
				throw new AdempiereUserError("物料[" + product.getName() + "]无库存！");
	        }  
		}
	  
		// 所有校验通过，更新记录
		int updatedCount = 0;
		for (MInventoryLine line : validLines) {
			String currentStatus = (String) line.get_Value("ClaimStatus");

			// 获取当前已领数量
			BigDecimal currentPickedQty = (BigDecimal) line.get_Value("quantityPicked");
			if (currentPickedQty == null) {
				currentPickedQty = Env.ZERO;
			}

			// 检查当前领取数量是否超过库存
			BigDecimal stockQty = getStockOnHand(line.getM_Product_ID(), line.getM_Locator_ID());
			if (claimQty.compareTo(stockQty) > 0) {
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());
				throw new AdempiereUserError(
						"物料[" + product.getName() + "]领用数量(" + claimQty + ")超过库存数量(" + stockQty + ")");
			}

			// 累加已领数量
			BigDecimal newPickedQty = currentPickedQty.add(claimQty);
			line.set_CustomColumn("quantityPicked", newPickedQty);

			// 覆盖领取数量字段
			line.setQtyInternalUse(newPickedQty);

			// 获取需求数量
			BigDecimal demandQty = (BigDecimal) line.get_Value("QtyDemand");
			if (demandQty == null)
				demandQty = Env.ZERO;

			// 判断领用状态（基于累计已领数量）
			if (newPickedQty.compareTo(demandQty) >= 0) {
				// 累计已领数量 >= 需求数量，设为已领用
				line.set_CustomColumn("ClaimStatus", "YL");
			} else {
				// 累计已领数量 < 需求数量，设为部分领用
				line.set_CustomColumn("ClaimStatus", "PR");
			}

			if (!line.save()) {
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());
				throw new AdempiereUserError("更新物料[" + product.getName() + "]的领用状态失败");
			}
			updatedCount++;
		}
	  
		return "成功更新 " + updatedCount + " 条记录";
	}
	
	 /**  
     * 获取物料在指定库位的库存数量  
     * @param M_Product_ID 物料ID  
     * @param M_Locator_ID 库位ID  
     * @return 库存数量  
     */  
    private BigDecimal getStockOnHand(int M_Product_ID, int M_Locator_ID) {  
        return MStorageOnHand.getQtyOnHandForLocator(M_Product_ID, M_Locator_ID, 0, get_TrxName());  
    } 

    /**  
     * 获取物料在多个库位的总库存数量  
     */  
    private BigDecimal getTotalStockOnHand(int M_Product_ID, List<Integer> locatorIds) {  
        BigDecimal totalStock = Env.ZERO;  
        for (Integer locatorId : locatorIds) {  
            BigDecimal stock = getStockOnHand(M_Product_ID, locatorId);  
            totalStock = totalStock.add(stock);  
        }  
        return totalStock;  
    }  
    
	/**
	 * 从T_Selection表加载选中的明细行
	 */
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
			throw new IllegalArgumentException("获取领用单明细失败");
		} finally {
			DB.close(rs, pstmt);
		}
	}
	

}