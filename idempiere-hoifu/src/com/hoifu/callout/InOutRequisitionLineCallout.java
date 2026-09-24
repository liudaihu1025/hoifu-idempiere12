package com.hoifu.callout;  
  
import java.math.BigDecimal;  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MProduct;  
import org.compiere.model.MRMALine;  
import org.compiere.model.MTable;  
import org.compiere.model.PO;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
import com.hoifu.model.MInOutRequisition;  
  
/**  
 * M_InOut_RequisitionLine（出入库申请单明细）Callout。  
 *  
 * <p>所有分支都要先从父表头 GridTab 读 InOutType 做守卫判断，  
 * 避免在不对应的场景下误填字段。</p>  
 *  
 * <p>注意：PP_Order / PP_Order_BOMLine / PP_Cost_Collector 属于 idempiere-mfg 模块  
 * （org.libero.model 包），hoifu 这个 bundle 的 MANIFEST.MF 未声明对  
 * org.libero.model 的 Import-Package 依赖，因此这里一律用通用 PO  
 * （MTable.get(ctx, "PP_Order").getPO(...)）+ get_Value(...) 访问，  
 * 不直接 import 这些强类型 Model 类，避免引入跨 bundle 的编译期依赖。</p>  
 */  
@Callout(tableName = "M_InOut_RequisitionLine", columnName = {  
        "M_Product_ID", "C_OrderLine_ID", "M_RMALine_ID",  
        "C_OfficeRequisitionLine_ID", "PP_Order_ID", "PP_Order_BOMLine_ID" })  
public class InOutRequisitionLineCallout implements IColumnCallout {  
  
    /** CostCollectorType 字面量，对应 idempiere-mfg 的 X_PP_Cost_Collector 常量：  
     *  100=MaterialReceipt（生产/委外入库），110=ComponentIssue（领料），115=ProductionReturn（退料） */  
    private static final String CCTYPE_MATERIAL_RECEIPT = "100";  
    private static final String CCTYPE_COMPONENT_ISSUE = "110";  
    private static final String CCTYPE_PRODUCTION_RETURN = "115";  
  
    @Override  
    public String start(Properties ctx, int windowNo, GridTab mTab, GridField mField,  
            Object value, Object oldValue) {  
  
        String columnName = mField.getColumnName();  
  
        if ("M_Product_ID".equals(columnName)) {  
            return onProductChanged(ctx, windowNo, mTab, value);  
        } else if ("C_OrderLine_ID".equals(columnName)) {  
            return onOrderLineChanged(ctx, windowNo, mTab, value);  
        } else if ("M_RMALine_ID".equals(columnName)) {  
            return onRMALineChanged(ctx, windowNo, mTab, value);  
        } else if ("C_OfficeRequisitionLine_ID".equals(columnName)) {  
            return onOfficeRequisitionLineChanged(ctx, windowNo, mTab, value);  
        } else if ("PP_Order_ID".equals(columnName)) {  
            return onPPOrderChanged(ctx, windowNo, mTab, value);  
        } else if ("PP_Order_BOMLine_ID".equals(columnName)) {  
            return onPPOrderBOMLineChanged(ctx, windowNo, mTab, value);  
        }  
        return "";  
    }  
  
    private String getParentInOutType(GridTab mTab) {  
        GridTab parentTab = mTab.getParentTab();  
        if (parentTab == null)  
            return null;  
        Object v = parentTab.getValue("InOutType");  
        return v == null ? null : v.toString();  
    }  
  
    private boolean isOutStock(String inOutType) {  
        return MInOutRequisition.OUT_INV_IU_OUT.equals(inOutType)  
                || MInOutRequisition.OUT_PP_MATERIA_OUT.equals(inOutType)  
                || MInOutRequisition.OUT_ORDER_OUT.equals(inOutType)  
                || MInOutRequisition.OUT_RMA_OUT.equals(inOutType)  
                || MInOutRequisition.OUT_OTHER_OUT.equals(inOutType);  
    }  
  
    /**  
     * 通用：任何 InOutType 下选产品都要回填 C_UOM_ID / M_AttributeSetInstance_ID，  
     * 并尝试推荐库位（出库/入库标志按父表头 InOutType 分组决定）。  
     */  
    private String onProductChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        Integer productId = (Integer) value;  
        if (productId == null || productId <= 0)  
            return "";  
  
        MProduct product = MProduct.get(ctx, productId);  
        mTab.setValue("C_UOM_ID", product.getC_UOM_ID());  
        mTab.setValue("M_AttributeSetInstance_ID", product.getM_AttributeSetInstance_ID());  
  
        GridTab parentTab = mTab.getParentTab();  
        if (parentTab == null)  
            return "";  
        Object whIdObj = parentTab.getValue("M_Warehouse_ID");  
        if (whIdObj == null)  
            return "";  
        int warehouseId = (Integer) whIdObj;  
        String inOutType = getParentInOutType(mTab);  
        boolean outStock = isOutStock(inOutType);  
  
        // 注意：第三个参数必须是 "Y"/"N" 字符串，不能直接传 boolean  
        int locatorId = DB.getSQLValue(null, "SELECT get_recommended_locator(?, ?, ?)",  
                productId, warehouseId, outStock ? "Y" : "N");  
        if (locatorId <= 0 && outStock) {  
            locatorId = DB.getSQLValue(null,  
                    "SELECT l.M_Locator_ID FROM M_Locator l "  
                            + "JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID) "  
                            + "WHERE l.M_Warehouse_ID = ? AND lt.Name = '成品库位' AND l.IsActive = 'Y' "  
                            + "ORDER BY CASE WHEN l.IsDefault='Y' THEN 0 ELSE 1 END, l.PriorityNo LIMIT 1",  
                    warehouseId);  
        }  
        if (locatorId > 0)  
            mTab.setValue("M_Locator_ID", locatorId);  
  
        return "";  
    }  
  
    /** 仅 ORDER_IN / ORDER_OUT 时生效 */  
    private String onOrderLineChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = getParentInOutType(mTab);  
        boolean isOrderGroup = MInOutRequisition.IN_ORDER_IN.equals(inOutType)  
                || MInOutRequisition.OUT_ORDER_OUT.equals(inOutType);  
        if (!isOrderGroup)  
            return "";  
  
        Integer orderLineId = (Integer) value;  
        if (orderLineId == null || orderLineId <= 0)  
            return "";  
  
        MOrderLine ol = new MOrderLine(ctx, orderLineId, null);  
        if (ol.get_ID() == 0)  
            return "";  
  
        mTab.setValue("M_Product_ID", ol.getM_Product_ID());  
        mTab.setValue("C_UOM_ID", ol.getC_UOM_ID());  
        BigDecimal qtyRequested = ol.getQtyOrdered().subtract(ol.getQtyDelivered());  
        mTab.setValue("QtyRequested", qtyRequested);  
        return "";  
    }  
  
    /** 仅 RMA_IN / RMA_OUT 时生效 */  
    private String onRMALineChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = getParentInOutType(mTab);  
        boolean isRmaGroup = MInOutRequisition.IN_RMA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_RMA_OUT.equals(inOutType);  
        if (!isRmaGroup)  
            return "";  
  
        Integer rmaLineId = (Integer) value;  
        if (rmaLineId == null || rmaLineId <= 0)  
            return "";  
  
        MRMALine rl = new MRMALine(ctx, rmaLineId, null);  
        if (rl.get_ID() == 0)  
            return "";  
  
        mTab.setValue("M_Product_ID", rl.getM_Product_ID());  
        mTab.setValue("M_AttributeSetInstance_ID", rl.getM_AttributeSetInstance_ID());  
        mTab.setValue("QtyRequested", rl.getQty());  
        return "";  
    }  
  
    /** 仅 INV_IU_IN/OUT、OTHER_IN/OUT 时生效 */  
    private String onOfficeRequisitionLineChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = getParentInOutType(mTab);  
        boolean isOfficeReqGroup = MInOutRequisition.IN_INV_IU_IN.equals(inOutType)  
                || MInOutRequisition.OUT_INV_IU_OUT.equals(inOutType)  
                || MInOutRequisition.IN_OTHER_IN.equals(inOutType)  
                || MInOutRequisition.OUT_OTHER_OUT.equals(inOutType);  
        if (!isOfficeReqGroup)  
            return "";  
  
        Integer lineId = (Integer) value;  
        if (lineId == null || lineId <= 0)  
            return "";  
  
        PO offLine = MTable.get(ctx, "C_OfficeRequisitionLine").getPO(lineId, null);  
        if (offLine == null)  
            return "";  
  
        Object productIdObj = offLine.get_Value("M_Product_ID");  
        if (productIdObj != null) {  
            int productId = (Integer) productIdObj;  
            mTab.setValue("M_Product_ID", productId);  
            MProduct product = MProduct.get(ctx, productId);  
            mTab.setValue("C_UOM_ID", product.getC_UOM_ID());  
        }  
        Object asiObj = offLine.get_Value("M_AttributeSetInstance_ID");  
        if (asiObj != null)  
            mTab.setValue("M_AttributeSetInstance_ID", asiObj);  
  
        Object demandObj = offLine.get_Value("QtyDemand");  
        BigDecimal demand = demandObj == null ? Env.ZERO : (BigDecimal) demandObj;  
        mTab.setValue("QtyRequested", demand);  
        return "";  
    }  
  
    /**  
     * 仅 PP_MATERIA_IN / PP_MATERIA_OUT / PP_IN / SUBCONTRAC_IN 时生效。  
     * 回填产品/UOM/建议数量，并按 InOutType 给 CostCollectorType 设置默认值。  
     *  
     * <p>PP_Order 属于 idempiere-mfg（org.libero.model.MPPOrder），hoifu 无法静态  
     * import，改用 MTable.get(ctx, "PP_Order").getPO(...) 通用 PO 读字段。</p>  
     */  
    private String onPPOrderChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = getParentInOutType(mTab);  
        boolean isPPGroup = MInOutRequisition.IN_PP_MATERIA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_PP_MATERIA_OUT.equals(inOutType)  
                || MInOutRequisition.IN_PP_IN.equals(inOutType)  
                || MInOutRequisition.IN_SUBCONTRAC_IN.equals(inOutType);  
        if (!isPPGroup)  
            return "";  
  
        Integer ppOrderId = (Integer) value;  
        if (ppOrderId == null || ppOrderId <= 0)  
            return "";  
  
        PO ppOrder = MTable.get(ctx, "PP_Order").getPO(ppOrderId, null);  
        if (ppOrder == null || ppOrder.get_ID() == 0)  
            return "";  
  
        // PP_IN / SUBCONTRAC_IN：生产/委外入库，产品直接取工单成品  
        if (MInOutRequisition.IN_PP_IN.equals(inOutType)  
                || MInOutRequisition.IN_SUBCONTRAC_IN.equals(inOutType)) {  
  
            Object productIdObj = ppOrder.get_Value("M_Product_ID");  
            Object uomIdObj = ppOrder.get_Value("C_UOM_ID");  
            Object qtyOrderedObj = ppOrder.get_Value("QtyOrdered");  
            Object qtyDeliveredObj = ppOrder.get_Value("QtyDelivered");  
  
            if (productIdObj != null)  
                mTab.setValue("M_Product_ID", productIdObj);  
            if (uomIdObj != null)  
                mTab.setValue("C_UOM_ID", uomIdObj);  
  
            BigDecimal qtyOrdered = qtyOrderedObj == null ? Env.ZERO : (BigDecimal) qtyOrderedObj;  
            BigDecimal qtyDelivered = qtyDeliveredObj == null ? Env.ZERO : (BigDecimal) qtyDeliveredObj;  
            BigDecimal qtyOpen = qtyOrdered.subtract(qtyDelivered);  
            mTab.setValue("QtyRequested", qtyOpen);  
            mTab.setValue("CostCollectorType", CCTYPE_MATERIAL_RECEIPT); // 100  
        }  
        // PP_MATERIA_OUT：生产领料出库  
        else if (MInOutRequisition.OUT_PP_MATERIA_OUT.equals(inOutType)) {  
            mTab.setValue("CostCollectorType", CCTYPE_COMPONENT_ISSUE); // 110  
            // 组件/数量交给 PP_Order_BOMLine_ID 回填，这里不预填 M_Product_ID  
        }  
        // PP_MATERIA_IN：生产退料入库  
        else if (MInOutRequisition.IN_PP_MATERIA_IN.equals(inOutType)) {  
            mTab.setValue("CostCollectorType", CCTYPE_PRODUCTION_RETURN); // 115  
        }  
  
        return "";  
    }  
  
    /**  
     * 仅 PP_MATERIA_IN / PP_MATERIA_OUT 时生效：选中工单 BOM 组件行，  
     * 回填产品/UOM/建议数量 = QtyBOM * 工单数量 - 已发（退）数量。  
     *  
     * <p>PP_Order_BOMLine 同样属于 idempiere-mfg（org.libero.model.MPPOrderBOMLine），  
     * 用通用 PO 读字段，避免跨 bundle 编译期依赖。</p>  
     */  
    private String onPPOrderBOMLineChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = getParentInOutType(mTab);  
        boolean isPPMaterialGroup = MInOutRequisition.IN_PP_MATERIA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_PP_MATERIA_OUT.equals(inOutType);  
        if (!isPPMaterialGroup)  
            return "";  
  
        Integer bomLineId = (Integer) value;  
        if (bomLineId == null || bomLineId <= 0)  
            return "";  
  
        PO bomLine = MTable.get(ctx, "PP_Order_BOMLine").getPO(bomLineId, null);  
        if (bomLine == null || bomLine.get_ID() == 0)  
            return "";  
  
        Object bomOrderIdObj = bomLine.get_Value("PP_Order_ID");  
        int bomOrderId = bomOrderIdObj == null ? 0 : (Integer) bomOrderIdObj;  
  
        // 校验 BOM 行归属的工单与本行 PP_Order_ID 是否一致，防止复制行时选错  
        Object ppOrderIdObj = mTab.getValue("PP_Order_ID");  
        if (ppOrderIdObj != null && ((Integer) ppOrderIdObj) > 0  
                && bomOrderId != (Integer) ppOrderIdObj) {  
            return "@Error@ BOM行所属工单与本行PP_Order_ID不一致";  
        }  
  
        Object productIdObj = bomLine.get_Value("M_Product_ID");  
        Object uomIdObj = bomLine.get_Value("C_UOM_ID");  
        if (productIdObj != null)  
            mTab.setValue("M_Product_ID", productIdObj);  
        if (uomIdObj != null)  
            mTab.setValue("C_UOM_ID", uomIdObj);  
  
        // 工单数量：如果本行没有再单独取 PP_Order 表头，直接用 BOMLine 挂的工单 ID 查  
        BigDecimal orderQty = Env.ZERO;  
        if (bomOrderId > 0) {  
            PO ppOrder = MTable.get(ctx, "PP_Order").getPO(bomOrderId, null);  
            if (ppOrder != null && ppOrder.get_ID() != 0) {  
                Object oq = ppOrder.get_Value("QtyOrdered");  
                orderQty = oq == null ? Env.ZERO : (BigDecimal) oq;  
            }  
        }  
  
        Object qtyBOMObj = bomLine.get_Value("QtyBOM");  
        Object qtyDeliveredObj = bomLine.get_Value("QtyDelivered");  
        BigDecimal qtyBOM = qtyBOMObj == null ? Env.ZERO : (BigDecimal) qtyBOMObj;  
        BigDecimal delivered = qtyDeliveredObj == null ? Env.ZERO : (BigDecimal) qtyDeliveredObj;  
  
        BigDecimal qtyRequested = qtyBOM.multiply(orderQty).subtract(delivered);  
        if (qtyRequested.signum() < 0)  
            qtyRequested = Env.ZERO;  
        mTab.setValue("QtyRequested", qtyRequested);  
  
        return "";  
    }  
}