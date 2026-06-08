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
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MWarehouse;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;  

/**  
 * 根据领用单明细创建申购单  
 * 按仓库分组，按产品合并数量  
 */  
@org.adempiere.base.annotation.Process  
public class OfficeRequisitionDetailPRCreate extends SvrProcess {  
      
    private List<Integer> selectedLineIds = new ArrayList<>();  
	private String p_Purpose = null;  
      
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (name.equals("Purpose"))
				p_Purpose = para[i].getParameterAsString();
		}
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
     * @param M_Product_ID 物料ID  
     * @param locatorIds 库位ID列表  
     * @return 总库存数量  
     */  
    private BigDecimal getTotalStockOnHand(int M_Product_ID, List<Integer> locatorIds) {  
        BigDecimal totalStock = Env.ZERO;  
        for (Integer locatorId : locatorIds) {  
            BigDecimal stock = getStockOnHand(M_Product_ID, locatorId);  
            totalStock = totalStock.add(stock);  
        }  
        return totalStock;  
    } 
    
    @Override  
    protected String doIt() throws Exception {  
        // 加载选中的记录  
        loadSelectedLines();  
        if (selectedLineIds.isEmpty()) {  
            throw new AdempiereUserError("请选勾选明细记录");  
        }  
          
        // 按组织+仓库分组  
        Map<String, List<MInventoryLine>> warehouseGroups = new HashMap<>();  
          
        for (Integer lineId : selectedLineIds) {  
            MInventoryLine line = new MInventoryLine(getCtx(), lineId, get_TrxName());  
              
            if (line.get_ID() == 0) {  
            	 throw new AdempiereUserError("领用单明细行不存在: " + lineId);  
            }  
              
            // 验证明细行是否可以处理  
            if (line.getParent().isProcessed()) {  
            	throw new AdempiereUserError("单据已处理，不能调整明细行: " + lineId);  
            }  
              
            // 验证必须有产品  
            if (line.getM_Product_ID() == 0) {  
                throw new AdempiereUserError("明细行无产品信息: " + lineId);  
            }  
            
            String currentStatus = (String) line.get_Value("ClaimStatus");  
            if ("YL".equals(currentStatus)) {  
                MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());  
                throw new AdempiereUserError("物料[" + product.getName() + "]当前状态为[" +   
                    (currentStatus != null ? currentStatus : "空") + "]，已领用。");  
            }  
            
            MInventory inventory = line.getParent();  
            String groupKey = inventory.getAD_Org_ID() + "_" + inventory.getM_Warehouse_ID();  
              
            if (!warehouseGroups.containsKey(groupKey)) {  
                warehouseGroups.put(groupKey, new ArrayList<>());  
            }  
            warehouseGroups.get(groupKey).add(line);  
        }  
          
        if (warehouseGroups.isEmpty()) {  
            throw new AdempiereUserError("没有可处理的明细记录");  
        }  
          
        // 为每个仓库组创建申购单  
        List<String> createdReqs = new ArrayList<>();  
          
        for (Map.Entry<String, List<MInventoryLine>> entry : warehouseGroups.entrySet()) {  
            String groupKey = entry.getKey();  
            List<MInventoryLine> lines = entry.getValue();  
              
            String[] keys = groupKey.split("_");  
            int orgId = Integer.parseInt(keys[0]);  
            int warehouseId = Integer.parseInt(keys[1]);  
              
            // 获取仓库信息  
            MWarehouse warehouse = MWarehouse.get(getCtx(), warehouseId);  
              
            // 创建申购单  
            MRequisition requisition = new MRequisition(getCtx(), 0, get_TrxName());  
            requisition.setAD_Org_ID(orgId);  
            requisition.setAD_User_ID(getAD_User_ID());  
            requisition.setM_Warehouse_ID(warehouseId);  
            requisition.setDateDoc(new Timestamp(System.currentTimeMillis()));  
            requisition.setDateRequired(new Timestamp(System.currentTimeMillis()));  
            requisition.setPriorityRule("5"); // 中等优先级  
            requisition.set_CustomColumn("Purpose", p_Purpose != null && !p_Purpose.isEmpty() ? p_Purpose: "COP02");  
            requisition.setC_DocType_ID(getRequisitionDocTypeId());  
            requisition.saveEx();  
              
            // 按产品+库位分组合并（用于库存检查）  
            Map<String, List<MInventoryLine>> productLocatorGroups = new HashMap<>();  
              
            for (MInventoryLine line : lines) {  
                String productKey = line.getM_Product_ID() + "_" + line.getM_Locator_ID();  
                if (!productLocatorGroups.containsKey(productKey)) {  
                    productLocatorGroups.put(productKey, new ArrayList<>());  
                }  
                productLocatorGroups.get(productKey).add(line);  
            }  
              
//            // 按产品汇总（用于创建申购单行）  
//            Map<Integer, List<MInventoryLine>> productGroups = new HashMap<>();  
//            Map<Integer, List<Integer>> productLocators = new HashMap<>();  
//              
//            for (MInventoryLine line : lines) {  
//            	line.set_CustomColumn("ClaimStatus", "CG"); 
//                if (!line.save()) {  
//                    MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());  
//                    throw new AdempiereUserError("更新物料[" + product.getName() + "]的领用状态失败");  
//                }  
//                int productId = line.getM_Product_ID();  
//                if (!productGroups.containsKey(productId)) {  
//                    productGroups.put(productId, new ArrayList<>());  
//                    productLocators.put(productId, new ArrayList<>());  
//                }  
//                productGroups.get(productId).add(line);  
//                  
//                // 收集该产品的所有库位  
//                Integer locatorId = line.getM_Locator_ID();  
//                if (!productLocators.get(productId).contains(locatorId)) {  
//                    productLocators.get(productId).add(locatorId);  
//                }  
//            }  
              
            int lineNo = 10;  
			// 为每条领用明细创建申购单行
			for (MInventoryLine invLine : lines) {
				// 更新领用状态
				invLine.set_CustomColumn("ClaimStatus", "CG");
				if (!invLine.save()) {
					MProduct product = MProduct.get(getCtx(), invLine.getM_Product_ID());
					throw new AdempiereUserError("更新物料[" + product.getName() + "]的领用状态失败");
				}

				// 获取需求数量 - 确保在使用前声明变量
				BigDecimal demandQty = (BigDecimal) invLine.get_Value("QtyDemand");
				if (demandQty == null) {
					demandQty = Env.ZERO;
				}
				// 获取该物料的库存数量
				BigDecimal stockQty = getStockOnHand(invLine.getM_Product_ID(), invLine.getM_Locator_ID());



				// ：计算需要申购的数量 = 需求数量 - 库存数量
				BigDecimal requisitionQty = demandQty.subtract(stockQty);

				// 创建申购单行
				MRequisitionLine reqLine = new MRequisitionLine(requisition);
				reqLine.setLine(lineNo);
				reqLine.setM_Product_ID(invLine.getM_Product_ID());
				reqLine.set_CustomColumn("M_InventoryLine_ID", invLine.getM_InventoryLine_ID());

				// 获取产品以获取UOM
				MProduct product = MProduct.get(getCtx(), invLine.getM_Product_ID());
				reqLine.setC_UOM_ID(product.getC_UOM_ID());
				reqLine.setM_AttributeSetInstance_ID(invLine.getM_AttributeSetInstance_ID());

				reqLine.setQty(requisitionQty);

				// 从价格表获取价格
				BigDecimal priceStd = getProductPrice(invLine.getM_Product_ID());
				reqLine.setPriceActual(priceStd);

				reqLine.saveEx();

				lineNo += 10;
			}
              
            // 记录日志  
            addBufferLog(0, null, requisition.getTotalLines(),   
                        Msg.parseTranslation(getCtx(), "@Created@ @M_Requisition_ID@ ") + requisition.getDocumentNo(),  
                        MRequisition.Table_ID, requisition.getM_Requisition_ID());  
              
            createdReqs.add(requisition.getDocumentNo());  
        }  
          
        return "@Created@ " + String.join(", ", createdReqs);  
    }
      
    /**  
     * 获取申购单文档类型ID  
     * 需要根据实际系统配置返回正确的文档类型ID  
     */  
    private int getRequisitionDocTypeId() {  
        // 首先按 DocBaseType 查询  
    	String sql = "SELECT C_DocType_ID FROM C_DocType " +  
                "WHERE AD_Client_ID = ? AND Name = '申购单' " +  
                "AND IsActive = 'Y'";  
          
        int docTypeId = DB.getSQLValueEx(get_TrxName(), sql, getAD_Client_ID());  
          
        // 如果没找到，按名称查找备用方案  
        if (docTypeId <= 0) {  
             sql = "SELECT C_DocType_ID FROM C_DocType " +  
                    "WHERE AD_Client_ID = ? AND DocBaseType = 'POO' " +  
                    "AND IsActive = 'Y' " +  
                    "ORDER BY IsDefault DESC, C_DocType_ID";  
              
            docTypeId = DB.getSQLValueEx(get_TrxName(), sql, getAD_Client_ID());  
        }  
          
        if (docTypeId <= 0) {  
            throw new AdempiereUserError("未找到申购单文档类型，请配置DocBaseType='POO'或名称为'申购单'的文档类型");  
        }  
          
        return docTypeId;  
    }
    
    /**  
     * 获取物料的标准价格  
     * @param M_Product_ID 物料ID  
     * @return 标准价格，如果找不到返回0  
     */  
    private BigDecimal getProductPrice(int M_Product_ID) {  
        // 查找默认的采购价格表  
        String priceListSql = "SELECT M_PriceList_ID FROM M_PriceList " +  
                             "WHERE AD_Client_ID = ? AND IsDefault = 'Y' " +  
                             "AND IsSOPriceList = 'N' AND IsActive = 'Y'";  
          
        int priceListId = DB.getSQLValueEx(get_TrxName(), priceListSql, getAD_Client_ID());  
          
        if (priceListId <= 0) {  
            log.warning("未找到默认的采购价格表");  
            return Env.ZERO;  
        }  
          
        // 查找有效的价格表版本  
        String versionSql = "SELECT M_PriceList_Version_ID FROM M_PriceList_Version " +  
                           "WHERE M_PriceList_ID = ? AND ValidFrom <= ? " +  
                           "AND IsActive = 'Y' ORDER BY ValidFrom DESC";  
          
        Timestamp currentDate = new Timestamp(System.currentTimeMillis());  
        int versionId = DB.getSQLValueEx(get_TrxName(), versionSql, priceListId, currentDate);  
          
        if (versionId <= 0) {  
            log.warning("未找到有效的价格表版本");  
            return Env.ZERO;  
        }  
          
        // 获取物料价格  
        String priceSql = "SELECT PriceStd FROM M_ProductPrice " +  
                         "WHERE M_PriceList_Version_ID = ? AND M_Product_ID = ? " +  
                         "AND IsActive = 'Y'";  
          
        BigDecimal price = DB.getSQLValueBD(get_TrxName(), priceSql, versionId, M_Product_ID);  
          
        return price != null ? price : Env.ZERO;  
    }
}