package com.hoifu.delegate;  

import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;  
import java.util.Set;  

import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.AfterComplete;  
import org.compiere.model.MDocType;  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MProduct;  
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_C_Order;  
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;  
import org.eevolution.model.MPPProductBOM;  
import org.eevolution.model.MPPProductBOMLine;  
import org.osgi.service.event.Event;  
 
@EventTopicDelegate  
@ModelEventTopic(modelClass = X_C_Order.class)  
public class SalesOrderWithCustomerMaterials extends ModelEventDelegate<X_C_Order> {  
  
    /** 客供料收货单的单据类型 UUID（已在 Application Dictionary 中配置） */  
    private static final String CSM_DOCTYPE_UU = "98590873-11f6-4564-895c-081e19e0e688";  
  
    public SalesOrderWithCustomerMaterials(X_C_Order po, Event event) {  
        super(po, event);  
    }  
  
    /**  
     * 销售订单完成后触发：  
     * 若订单标记为客供料订单（IsCSM='Y'），则自动创建客供料收货单（草稿状态）。  
     */  
    @AfterComplete  
    public void onAfterComplete() {  
        X_C_Order xOrder = getModel();  
  
        // 1. 仅处理标记了"客供料"的销售订单  
        if (!xOrder.get_ValueAsBoolean("IsCSM"))  
            return;  
  
        // 运行时实际对象为 MOrder，可安全强转  
        MOrder order = (MOrder) xOrder;  
        String trxName = order.get_TrxName();  
        Properties ctx = order.getCtx();  
  
        // 2. 通过固定 UUID 解析客供料收货单的 C_DocType_ID  
        MDocType csmDocType = new MDocType(ctx, CSM_DOCTYPE_UU, trxName);  
        int csmDocTypeId = csmDocType.getC_DocType_ID();  
        if (csmDocTypeId <= 0)  
           return;
  
        // 3. 若该销售订单已存在未作废/未冲销的客供料收货单，则跳过，避免重复创建  
        int existing = DB.getSQLValue(trxName,  
                "SELECT COUNT(*) FROM M_InOut"  
                + " WHERE C_Order_ID=? AND C_DocType_ID=? AND DocStatus NOT IN ('VO','RE')",  
                order.getC_Order_ID(), csmDocTypeId);  
        if (existing > 0)  
            return;  
  
        // 4. 遍历订单行 → 递归展开 BOM → 收集所有叶子节点中 IsCSM='Y' 的物料  
//        Set<Integer> csmProductIds = new LinkedHashSet<>();  
//        MOrderLine[] orderLines = order.getLines(false, null);  
//        for (MOrderLine ol : orderLines) {  
//            int productId = ol.getM_Product_ID();  
//            if (productId <= 0)  
//                continue;  
//            MProduct product = MProduct.get(ctx, productId, trxName);  
//            if (product == null)  
//                continue;  
//  
//            // 只处理配置了 BOM 的产品；未配置 BOM 的产品跳过（由仓库手动创建入库单）  
//            MPPProductBOM bom = getDefaultBOM(product);  
//            if (bom == null)  
//                continue;  
//  
//            // 用 visited 集合防止循环 BOM 导致无限递归  
//            Set<Integer> visited = new LinkedHashSet<>();  
//            visited.add(productId);  
//            traverseBOMForCSM(bom, ctx, trxName, visited, csmProductIds);  
//        }  
//  
//        // 若未找到任何客供料物料，则不创建收货单  
//        if (csmProductIds.isEmpty())  
//            return;  
  
        // 5. 创建客供料收货单表头（草稿状态）  
        //    MInOut(MOrder, docTypeId, movementDate) 会自动从销售订单复制：  
        //    客户（C_BPartner_ID）、客户地址（C_BPartner_Location_ID）、  
        //    仓库（M_Warehouse_ID）、IsSOTrx、源单号（C_Order_ID）等字段  
        Timestamp movementDate = TimeUtil.getDay(System.currentTimeMillis());  
        MInOut receipt = new MInOut(order, csmDocTypeId, movementDate);  
        receipt.saveEx(trxName);  
  
        // 6. 获取收货仓库的默认库位，用于明细行  
        MWarehouse wh = MWarehouse.get(ctx, receipt.getM_Warehouse_ID());  
        //int defaultLocatorId = wh.getDefaultLocator().getM_Locator_ID();  
  
        // 7. 为每个客供料物料创建一条明细行，数量默认为 0（由仓库人员填写实收数量后确认入库）  
//        int lineNo = 10;  
//        for (int productId : csmProductIds) {  
//            MInOutLine line = new MInOutLine(receipt);  
//            line.setLine(lineNo);  
//            line.setM_Product_ID(productId, true); // true：同时从物料主数据带出计量单位  
//            line.setQty(BigDecimal.ZERO);           // 同时设置 MovementQty 和 QtyEntered 为 0  
//	        int locatorId = DB.getSQLValue(trxName,  
//	                "SELECT get_recommended_locator(?, ?, ?)",  
//	                productId, wh.getM_Warehouse_ID(), "N");  
//	        
//	        if (locatorId > 0) {  
//	        	line.setM_Locator_ID(locatorId);  
//	        } else {  
//	            // 函数未返回有效库位，fallback：按库存查找，找不到则用仓库默认库位  
//	        	line.setM_Locator_ID(Env.ZERO);  
//	        } 
//            line.saveEx(trxName);  
//            lineNo += 10;  
//        }
    }
 
    /**  
     * 递归遍历 BOM，收集所有叶子节点中 IsCSM='Y' 的物料 ID。  
     *  
     * <p>判断规则：  
     * <ul>  
     *   <li>若某 BOM 行的组件物料本身还有默认 BOM → 非叶子节点，继续向下递归。</li>  
     *   <li>若某 BOM 行的组件物料没有 BOM → 叶子节点，检查 IsCSM 是否为 'Y'。</li>  
     * </ul>  
     *  
     * @param bom      当前层级的 BOM  
     * @param ctx      上下文  
     * @param trxName  事务名称  
     * @param visited  已访问的物料 ID 集合（防止循环 BOM）  
     * @param result   收集结果：满足条件的客供料物料 ID  
     */  
    private void traverseBOMForCSM(MPPProductBOM bom, Properties ctx,  
            String trxName, Set<Integer> visited, Set<Integer> result) {  
  
        for (MPPProductBOMLine bomLine : bom.getLines()) {  
            int componentId = bomLine.getM_Product_ID();  
            
            // 已访问过的物料跳过，防止循环 BOM 导致无限递归  
            if (visited.contains(componentId))  
                continue;  
            visited.add(componentId);  
  
            
            MProduct component = MProduct.get(ctx, componentId, trxName);  
            if (component == null)  
                continue;  
  
            MPPProductBOM childBom = getDefaultBOM(component);  
            if (component.isBOM() && childBom != null) {  
                // 该组件还有子 BOM → 非叶子节点，继续向下展开  
                traverseBOMForCSM(childBom, ctx, trxName, visited, result);  
            } else {  
                // 叶子节点：若标记为客供料则加入结果集  
                if (component.get_ValueAsBoolean("IsCSM"))  
                    result.add(componentId);  
            }  
        }  
    }  
    
    /**  
	 * 按产品自身组织查找默认BOM，作为 MPPProductBOM.getDefault 的保底方案。  
	 * MPPProductBOM.getDefault 使用登录组织过滤，可能导致跨组织场景下找不到BOM。  
	 */  
	private MPPProductBOM getDefaultBOM(MProduct product) {  
		int AD_Org_ID = product.getAD_Org_ID(); // 用产品组织，不用登录组织  
		String filter = "M_Product_ID=? AND BOMUse=? AND BOMType=? ";  
		if (AD_Org_ID > 0) {  
			filter += "AND AD_Org_ID IN (0, " + AD_Org_ID + ") ";  
		}  
		Query query = new Query(product.getCtx(), MPPProductBOM.Table_Name, filter, null)  
				.setParameters(product.getM_Product_ID(),  
						MPPProductBOM.BOMUSE_Master,  
						MPPProductBOM.BOMTYPE_CurrentActive)  
				.setOnlyActiveRecords(true)  
				.setClient_ID();  
		if (AD_Org_ID > 0)  
			query.setOrderBy("AD_Org_ID Desc");  
  
		List<MPPProductBOM> list = query.list();  
		if (!list.isEmpty()) {  
			if (AD_Org_ID > 0 || list.size() == 1) {  
				return list.get(0);  
			}  
		}  
		return null;  
	}
}