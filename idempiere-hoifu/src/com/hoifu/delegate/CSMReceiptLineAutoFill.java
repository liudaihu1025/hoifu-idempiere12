package com.hoifu.delegate;  
  
import java.math.BigDecimal;  
import java.util.LinkedHashSet;  
import java.util.List;  
import java.util.Properties;  
import java.util.Set;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.po.AfterChange;  
import org.adempiere.base.event.annotations.po.AfterNew;  
import org.compiere.model.MDocType;  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MProduct;  
import org.compiere.model.MStorageOnHand;  
import org.compiere.model.MWarehouse;  
import org.compiere.model.Query;  
import org.compiere.util.DB;  
import org.eevolution.model.MPPProductBOM;  
import org.eevolution.model.MPPProductBOMLine;  
import org.osgi.service.event.Event;  
  
/**  
 * 监听 M_InOut 的新建和 C_Order_ID 变更事件，自动为客供料单据创建明细行。  
 *  
 * <ul>  
 *   <li>客供料入库单（CSM_RECEIPT_DOCTYPE_UU）：为所有 IsCSM='Y' 的叶子物料创建行，数量=0。</li>  
 *   <li>客供料出库单（CSM_SHIPMENT_DOCTYPE_UU）：仅为仓库中 QtyOnHand &gt; 0 的物料创建行，数量=0。</li>  
 * </ul>  
 */  
@EventTopicDelegate  
@ModelEventTopic(modelClass = MInOut.class)  
public class CSMReceiptLineAutoFill extends ModelEventDelegate<MInOut> {  
  
    /** 客供料入库单的单据类型 UUID */  
    private static final String CSM_RECEIPT_DOCTYPE_UU  = "98590873-11f6-4564-895c-081e19e0e688";  
    /** 客供料出库单的单据类型 UUID */  
    private static final String CSM_SHIPMENT_DOCTYPE_UU = "d913466f-f2af-48a7-9e33-eb728efb8aa0";  
  
    public CSMReceiptLineAutoFill(MInOut po, Event event) {  
        super(po, event);  
    }  
  
    /**  
     * 新建 M_InOut 记录保存后触发。  
     * 新记录所有字段都是"新的"，直接检查 C_Order_ID 是否有效。  
     */  
    @AfterNew  
    public void onAfterNew() {  
        MInOut receipt = getModel();  
        if (receipt.getC_Order_ID() <= 0)  
            return;  
        tryCreateCSMLines(receipt);  
    }  
  
    /**  
     * 更新 M_InOut 记录保存后触发。  
     * 仅当 C_Order_ID 字段本次发生变化时才处理。  
     */  
    @AfterChange  
    public void onAfterChange() {  
        MInOut receipt = getModel();  
        if (!receipt.is_ValueChanged("C_Order_ID"))  
            return;  
        if (receipt.getC_Order_ID() <= 0)  
            return;  
        tryCreateCSMLines(receipt);  
    }  
  
    // -------------------------------------------------------------------------  
    // 核心逻辑  
    // -------------------------------------------------------------------------  
  
    /**  
     * 校验单据类型 → 幂等保护 → 展开 BOM → 按单据类型创建明细行。  
     */  
    private void tryCreateCSMLines(MInOut receipt) {  
        String trxName = receipt.get_TrxName();  
        Properties ctx  = receipt.getCtx();  
      
        // 1. 判断单据类型  
        int docTypeId  = receipt.getC_DocType_ID();  
        boolean isReceipt  = matchDocTypeUU(ctx, docTypeId, CSM_RECEIPT_DOCTYPE_UU,  trxName);  
        boolean isShipment = !isReceipt  
                          && matchDocTypeUU(ctx, docTypeId, CSM_SHIPMENT_DOCTYPE_UU, trxName);  
        if (!isReceipt && !isShipment)  
            return;  
      
        // 2. 幂等保护  
        int existingLines = DB.getSQLValue(trxName,  
                "SELECT COUNT(*) FROM M_InOutLine WHERE M_InOut_ID=?",  
                receipt.getM_InOut_ID());  
        if (existingLines > 0)  
            return;  
      
        // 3. 加载关联的销售订单  
        MOrder order = new MOrder(ctx, receipt.getC_Order_ID(), trxName);  
        if (order.getC_Order_ID() <= 0)  
            return;  
      
        // 4. 遍历订单行 → 递归展开 BOM → 收集叶子节点中 IsCSM='Y' 的物料  
        Set<Integer> csmProductIds = new LinkedHashSet<>();  
        Set<Integer> visited       = new LinkedHashSet<>();  
        for (MOrderLine ol : order.getLines(false, null)) {  
            int productId = ol.getM_Product_ID();  
            if (productId <= 0) continue;  
            MProduct product = MProduct.get(ctx, productId, trxName);  
            if (product == null) continue;  
            MPPProductBOM bom = getDefaultBOM(product);  
            if (bom == null) continue;  
            visited.add(productId);  
            traverseBOMForCSM(bom, ctx, trxName, visited, csmProductIds);  
        }  
        if (csmProductIds.isEmpty())  
            return;  
      
        // 5. 获取仓库  
        MWarehouse wh = MWarehouse.get(ctx, receipt.getM_Warehouse_ID());  
      
        // 6. 为每个 CSM 物料创建明细行（数量=0，由仓库人员填写后确认）  
        String locatorFlag = isShipment ? "Y" : "N"; // 出库=Y，入库=N  
        int lineNo = 10;  
        for (int productId : csmProductIds) {  
      
            // 出库单专属过滤：只为有库存的物料创建行  
            if (isShipment) {  
                BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHand(  
                        productId, wh.getM_Warehouse_ID(), 0, trxName);  
                if (qtyOnHand.compareTo(BigDecimal.ZERO) <= 0)  
                    continue;  
            }  
      
            MInOutLine line = new MInOutLine(receipt);  
            line.setLine(lineNo);  
            line.setM_Product_ID(productId, true);  
            line.setQty(BigDecimal.ZERO);  
      
            int locatorId = DB.getSQLValue(trxName,  
                    "SELECT get_recommended_locator(?, ?, ?)",  
                    productId, wh.getM_Warehouse_ID(), locatorFlag);  
      
            if (locatorId > 0) {  
                line.setM_Locator_ID(locatorId);  
            } else {  
                int defaultLocatorId = wh.getDefaultLocator().getM_Locator_ID();  
                if (defaultLocatorId > 0)  
                    line.setM_Locator_ID(defaultLocatorId);  
            }  
      
            line.saveEx(trxName);  
            lineNo += 10;  
        }  
    } 
  
    // -------------------------------------------------------------------------  
    // 工具方法  
    // -------------------------------------------------------------------------  
  
    /**  
     * 判断给定的 C_DocType_ID 是否对应指定 UUID 的单据类型。  
     * 通过 UUID 加载 MDocType 后比对 ID，避免硬编码数字 ID。  
     */  
    private boolean matchDocTypeUU(Properties ctx, int docTypeId, String uuid, String trxName) {  
        MDocType dt = new MDocType(ctx, uuid, trxName);  
        return dt.getC_DocType_ID() > 0 && dt.getC_DocType_ID() == docTypeId;  
    }  
  
    // -------------------------------------------------------------------------  
    // BOM 递归遍历  
    // -------------------------------------------------------------------------  
  
    /**  
     * 递归遍历 BOM，收集所有叶子节点中 IsCSM='Y' 的物料 ID。  
     *  
     * <ul>  
     *   <li>组件有子 BOM → 非叶子节点，继续向下递归。</li>  
     *   <li>组件无子 BOM → 叶子节点，检查 IsCSM 是否为 'Y'。</li>  
     * </ul>  
     *  
     * @param bom      当前层级的 BOM  
     * @param ctx      上下文  
     * @param trxName  事务名称  
     * @param visited  已访问的物料 ID 集合（防止循环 BOM 导致无限递归）  
     * @param result   收集结果：满足条件的客供料物料 ID  
     */  
    private void traverseBOMForCSM(MPPProductBOM bom, Properties ctx,  
            String trxName, Set<Integer> visited, Set<Integer> result) {  
  
        for (MPPProductBOMLine bomLine : bom.getLines()) {  
            int componentId = bomLine.getM_Product_ID();  
  
            if (visited.contains(componentId))  
                continue;  
            visited.add(componentId);  
  
            MProduct component = MProduct.get(ctx, componentId, trxName);  
            if (component == null)  
                continue;  
  
            MPPProductBOM childBom = getDefaultBOM(component);  
            if (component.isBOM() && childBom != null) {  
                // 非叶子节点：继续向下展开  
                traverseBOMForCSM(childBom, ctx, trxName, visited, result);  
            } else {  
                // 叶子节点：若标记为客供料则加入结果集  
                if (component.get_ValueAsBoolean("IsCSM"))  
                    result.add(componentId);  
            }  
        }  
    }  
  
    // -------------------------------------------------------------------------  
    // BOM 查找  
    // -------------------------------------------------------------------------  
  
    /**  
     * 按产品自身组织查找默认 BOM。  
     * 使用产品的 AD_Org_ID 而非登录组织，避免跨组织场景下找不到 BOM。  
     */  
    private MPPProductBOM getDefaultBOM(MProduct product) {  
        int AD_Org_ID = product.getAD_Org_ID();  
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