package com.hoifu.info;  
  
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MProcess;
import org.compiere.model.MStorageOnHand;
import org.compiere.util.DB;
import org.compiere.util.Env;  
 
/**
 * 自定义PPOrder表的信息窗口
 */
public class MInventoryInfoWindow extends InfoWindow {  

	 // 基础构造函数 - 7个参数  
    public MInventoryInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID);  
    }  
      
    // 带 lookup 参数的构造函数 - 8个参数  
    public MInventoryInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID, boolean lookup) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup);  
    }  
      
    // 带 GridField 参数的构造函数 - 9个参数  
    public MInventoryInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID, boolean lookup, GridField field) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup, field);  
    }  
      
    // 完整构造函数 - 10个参数  
    public MInventoryInfoWindow(int WindowNo, String tableName, String keyColumn,   
            String queryValue, boolean multipleSelection, String whereClause,   
            int AD_InfoWindow_ID, boolean lookup, GridField field, String predefinedContextVariables) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);  
    }  


    @Override  
    protected void enableButtons() {  
        super.enableButtons();  
          
        int selectedCount = contentPanel.getSelectedCount();  
          
        for (Button btProcess : btProcessList) {  
            Integer processId = (Integer)btProcess.getAttribute(PROCESS_ID_KEY);  
            if (processId != null) {  
                MProcess process = MProcess.get(Env.getCtx(), processId);  
                if (process.getClassname() != null && process.getClassname().equals("com.hoifu.process.OfficeRequisitionClaimStatusProcess")) {  
                      
                    // 只有选中项都是可领用(WL、CG)状态时，才开启领用按钮  
					boolean enabled = selectedCount == 1 && areAllSelectedItemsCanClaimed() && hasAnyStock();
                    btProcess.setEnabled(enabled);  
                      
                    // 修改按钮鼠标悬停提示  
                    btProcess.setTooltiptext("仅支持未领用状态的物料领用");  
                }
                if (process.getClassname() != null && process.getClassname().equals("com.hoifu.process.OfficeRequisitionProductProcess")) {  
                          
                        // 选中的记录等于1且是未领用时，才允许更新物料
                        boolean enabled = selectedCount == 1 && areAllSelectedItemsUnclaimed(); 
                        btProcess.setEnabled(enabled);  
                          
                        // 修改按钮鼠标悬停提示  
                        btProcess.setTooltiptext("物料调整");  
                    }
                if (process.getClassname() != null && process.getClassname().equals("com.hoifu.process.OfficeRequisitionDetailPRCreate")) {  
                          
					// 只有选中项都是未领用(WL)状态和部分领用状态时，才开启创建申购单按钮
					boolean enabled = selectedCount > 0 && areAllSelectedItemsUnclaimedOrPartiallyClaimed()
							&& hasInsufficientStock() && !hasExistingRequisitions();
                        btProcess.setEnabled(enabled);  
                          
                        // 修改按钮鼠标悬停提示  
                        btProcess.setTooltiptext("仅支持未领用状态的物料领用");  
                    }

					// 新增：结束领用按钮
					if (process.getClassname() != null
							&& process.getClassname().equals("com.hoifu.process.OfficeRequisitionEndClaimProcess")) {

						// 只有选中"部分领用"状态的记录时才启用
						boolean enabled = selectedCount > 0 && areAllSelectedItemsPartiallyClaimed();
						btProcess.setEnabled(enabled);
						btProcess.setTooltiptext("结束领用 - 将部分领用状态更新为已领取");
					}
					// 新增：取消申购按钮
					if (process.getClassname() != null
							&& process.getClassname().equals("com.hoifu.process.OfficeRequisitionCancelProcess")) {

						// 只有选中"采购中"状态的记录时才启用
						boolean enabled = selectedCount > 0 && hasDraftRequisitions();
						btProcess.setEnabled(enabled);
						btProcess.setTooltiptext("取消申购 - 仅支持采购中状态的物料取消申购");
					}
            }  
        }  
    }  
      
	/**
	 * 检查所有选中项是否都是可领用状态(WL、CG、PR)
	 */
	private boolean areAllSelectedItemsCanClaimed() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty()) {
			return false;
		}

		// 使用参数化查询，检查是否都是可领用状态(WL、CG、PR)
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_InventoryLine ");
		sql.append("WHERE M_InventoryLine_ID IN (");

		// 构建参数占位符
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND ClaimStatus NOT IN (?, ?, ?)");
            
		// 准备参数数组
		Object[] params = new Object[selectedKeys.size() + 3];
		for (int i = 0; i < selectedKeys.size(); i++) {
			params[i] = selectedKeys.get(i);
		}
		params[selectedKeys.size()] = "WL";
		params[selectedKeys.size() + 1] = "CG";
		params[selectedKeys.size() + 2] = "PR"; // 添加部分领用状态
            
		int nonClaimableCount = DB.getSQLValueEx(null, sql.toString(), params);
		return nonClaimableCount == 0;
    }
    
    /**  
     * 检查所有选中项是否都是未领用状态为领用(WL)  
     */  
    private boolean areAllSelectedItemsUnclaimed() {  
        if (contentPanel.getSelectedCount() == 0) {  
            return false;  
        }  
          
        List<Serializable> selectedKeys = getSelectedRowKeys();  
        if (selectedKeys == null || selectedKeys.isEmpty()) {  
            return false;  
        }  
          
        // 构建SQL检查是否都是未领用状态  
        StringBuilder sql = new StringBuilder();  
        sql.append("SELECT COUNT(*) FROM M_InventoryLine ");  
        sql.append("WHERE M_InventoryLine_ID IN (");  
        for (int i = 0; i < selectedKeys.size(); i++) {  
            if (i > 0) sql.append(",");  
            sql.append(selectedKeys.get(i));  
        }  
        sql.append(") AND ClaimStatus != ?");  
          
        int nonWLCount = DB.getSQLValue(null, sql.toString(), "WL");  
        return nonWLCount == 0;  
    }
    
	/**
	 * 检查所有选中项是否都是未领用(WL)或部分领用(PR)状态
	 */
	private boolean areAllSelectedItemsUnclaimedOrPartiallyClaimed() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty()) {
			return false;
		}

		// 使用参数化查询检查是否都是未领用或部分领用状态
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_InventoryLine ");
		sql.append("WHERE M_InventoryLine_ID IN (");

		// 构建参数占位符
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND ClaimStatus NOT IN (?, ?)");

		// 准备参数数组
		Object[] params = new Object[selectedKeys.size() + 2];
		for (int i = 0; i < selectedKeys.size(); i++) {
			params[i] = selectedKeys.get(i);
		}
		params[selectedKeys.size()] = "WL"; // 未领用状态
		params[selectedKeys.size() + 1] = "PR"; // 部分领用状态

		int invalidCount = DB.getSQLValueEx(null, sql.toString(), params);
		return invalidCount == 0;
	}

    /**    
     * 检查选中物料的库存是否充足    
     */    
    private boolean isStockSufficient() {    
        if (contentPanel.getSelectedCount() == 0) {    
            return false;    
        }    
            
        List<Serializable> selectedKeys = getSelectedRowKeys();    
        if (selectedKeys == null || selectedKeys.isEmpty()) {    
            return false;    
        }    
  
        // 获取所有选中的库存明细行    
        List<MInventoryLine> selectedLines = new ArrayList<>();    
        for (Serializable key : selectedKeys) {    
            MInventoryLine line = new MInventoryLine(Env.getCtx(), (Integer)key, null);    
            if (line.get_ID() != 0 && line.getM_Product_ID() > 0) {    
                selectedLines.add(line);    
            }    
        }    
  
        if (selectedLines.isEmpty()) {    
            return false;    
        }    
  
        // 按产品分组汇总领用数量和收集库位    
        Map<Integer, BigDecimal> productTotalQty = new HashMap<>();    
        Map<Integer, List<Integer>> productLocators = new HashMap<>();    
  
        for (MInventoryLine line : selectedLines) {    
            int productId = line.getM_Product_ID();    
            BigDecimal qtyInternalUse = line.getQtyInternalUse();    
              
            if (qtyInternalUse == null || qtyInternalUse.signum() <= 0) {    
                continue;    
            }    
              
            // 汇总领用数量    
            if (!productTotalQty.containsKey(productId)) {    
                productTotalQty.put(productId, Env.ZERO);    
                productLocators.put(productId, new ArrayList<>());    
            }    
              
            productTotalQty.put(productId, productTotalQty.get(productId).add(qtyInternalUse));    
              
            // 收集库位    
            Integer locatorId = line.getM_Locator_ID();    
            if (!productLocators.get(productId).contains(locatorId)) {    
                productLocators.get(productId).add(locatorId);    
            }    
        }    
  
        // 检查每个产品的库存是否充足    
        for (Map.Entry<Integer, BigDecimal> entry : productTotalQty.entrySet()) {    
            int productId = entry.getKey();    
            BigDecimal totalReqQty = entry.getValue();    
              
            // 获取该产品在所有相关库位的总库存    
            BigDecimal totalStockQty = getTotalStockOnHand(productId, productLocators.get(productId));    
              
            // 库存必须大于等于总领用数量    
            if (totalStockQty.compareTo(totalReqQty) < 0) {    
                return false;    
            }    
        }    
  
        return true;    
    }    
  
    /**    
     * 获取物料在指定库位的库存数量    
     */    
    private BigDecimal getStockOnHand(int M_Product_ID, int M_Locator_ID) {    
        return MStorageOnHand.getQtyOnHandForLocator(M_Product_ID, M_Locator_ID, 0, null);    
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
     * 获取ClaimStatus字段在结果集中的索引位置  
     */  
    private int getClaimStatusColumnIndex() {  
        // 这里需要根据你的实际SQL查询来确定ClaimStatus字段的索引  
        // 例如，如果SQL是: SELECT i.M_InventoryLine_ID, i.ClaimStatus, ...   
        // 那么ClaimStatus的索引就是1  
        return 1; // 请根据实际情况调整这个值  
    }

	private boolean hasAnyStock() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		for (Serializable key : selectedKeys) {
			MInventoryLine line = new MInventoryLine(Env.getCtx(), (Integer) key, null);
			BigDecimal stockQty = getStockOnHand(line.getM_Product_ID(), line.getM_Locator_ID());
			if (stockQty.signum() > 0) {
				return true; // 只要有一个物料有库存就返回true
			}
		}
		return false;
	}

	/**
	 * 检查所有选中项是否都是部分领用状态(PR)
	 */
	private boolean areAllSelectedItemsPartiallyClaimed() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty()) {
			return false;
		}

		// 使用参数化查询检查是否都是部分领用状态
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_InventoryLine ");
		sql.append("WHERE M_InventoryLine_ID IN (");

		// 构建参数占位符
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND ClaimStatus != ?");

		// 准备参数数组
		Object[] params = new Object[selectedKeys.size() + 1];
		for (int i = 0; i < selectedKeys.size(); i++) {
			params[i] = selectedKeys.get(i);
		}
		params[selectedKeys.size()] = "PR"; // 部分领用状态

		int nonPartialCount = DB.getSQLValueEx(null, sql.toString(), params);
		return nonPartialCount == 0;
	}



	/**
	 * 检查选中物料是否有库存不足（需要申购）
	 */
	private boolean hasInsufficientStock() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		for (Serializable key : selectedKeys) {
			MInventoryLine line = new MInventoryLine(Env.getCtx(), (Integer) key, null);
			BigDecimal stockQty = getStockOnHand(line.getM_Product_ID(), line.getM_Locator_ID());

			// 获取需求数量
			BigDecimal demandQty = (BigDecimal) line.get_Value("QtyDemand");
			if (demandQty == null) {
				demandQty = Env.ZERO;
			}

			// 如果库存 < 需求数量，说明需要申购
			if (stockQty.compareTo(demandQty) < 0) {
				return true;
			}
		}
		return false; // 所有物料库存都充足
	}

	/**
	 * 检查选中项是否已有有效的申购单
	 */
	private boolean hasExistingRequisitions() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty()) {
			return false;
		}

		// 使用参数化查询检查是否有有效的申购单
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_RequisitionLine rl ");
		sql.append("INNER JOIN M_Requisition r ON rl.M_Requisition_ID = r.M_Requisition_ID ");
		sql.append("WHERE rl.M_InventoryLine_ID IN (");

		// 构建参数占位符
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND r.DocStatus NOT IN (?, ?)"); // 排除已取消和已冲销的申购单

		// 准备参数数组
		Object[] params = new Object[selectedKeys.size() + 2];
		for (int i = 0; i < selectedKeys.size(); i++) {
			params[i] = selectedKeys.get(i);
		}
		params[selectedKeys.size()] = "VO"; // 已取消
		params[selectedKeys.size() + 1] = "RE"; // 已冲销

		int existingCount = DB.getSQLValueEx(null, sql.toString(), params);
		return existingCount > 0;
	}

	/**
	 * 检查选中项是否有草稿状态的申购单
	 */
	private boolean hasDraftRequisitions() {
		if (contentPanel.getSelectedCount() == 0) {
			return false;
		}

		List<Serializable> selectedKeys = getSelectedRowKeys();
		if (selectedKeys == null || selectedKeys.isEmpty()) {
			return false;
		}

		// 使用参数化查询检查是否有草稿状态的申购单
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM M_RequisitionLine rl ");
		sql.append("INNER JOIN M_Requisition r ON rl.M_Requisition_ID = r.M_Requisition_ID ");
		sql.append("WHERE rl.M_InventoryLine_ID IN (");

		// 构建参数占位符
		for (int i = 0; i < selectedKeys.size(); i++) {
			if (i > 0)
				sql.append(",");
			sql.append("?");
		}
		sql.append(") AND r.DocStatus = ?"); // 只要草稿状态

		// 准备参数数组
		Object[] params = new Object[selectedKeys.size() + 1];
		for (int i = 0; i < selectedKeys.size(); i++) {
			params[i] = selectedKeys.get(i);
		}
		params[selectedKeys.size()] = "DR"; // 草稿状态

		int draftCount = DB.getSQLValueEx(null, sql.toString(), params);
		return draftCount > 0;
	}
    
}