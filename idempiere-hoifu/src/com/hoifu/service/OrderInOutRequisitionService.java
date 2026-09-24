package com.hoifu.service;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.Properties;

import com.hoifu.service.extsync.wms.WmsInOutRequisitionService;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;  
import org.compiere.model.MInOut;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MWarehouse;  
import org.compiere.process.DocAction;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;  
  
/**  
 * 出入库申请单（M_InOut_Requisition）通用生成服务 —— C_Order（订单）来源场景。  
 *  
 * <p>本类无实例状态，全部方法为 static。</p>  
 */  
public final class OrderInOutRequisitionService {  
  
    private static final CLogger log = CLogger.getCLogger(OrderInOutRequisitionService.class);  
  
    /** 出入库申请单（M_InOut_Requisition）自身的 C_DocType_ID（固定） */  
    private static final String REQUISITION_DOCTYPE_UU = "97619cfe-e96f-4e65-b66d-2a73bec5893f";  
  
    /** 采购订单（普通） -> 收货单 */  
    private static final String RECEIPT_DOCTYPE_UU = "4a2110a7-04b6-4369-b9db-dc32c0efbe01";  
  
    /** 委外订单（来源单据类型标识） */  
    private static final String SUBCONTRACT_ORDER_DOCTYPE_UU = "5ae935d6-5237-4d93-ac5b-4967cf9be287";  
    /** 委外订单 -> 委外收货单 */  
    private static final String SUBCONTRACT_RECEIPT_DOCTYPE_UU = "503a5e26-3cb6-4525-94ae-ada837bc13ce";  
  
    /** 销售订单/打印订单/内部销单等（IsSOTrx=Y） -> 发货单 */  
    private static final String SHIPMENT_DOCTYPE_UU = "d7a2250b-ffed-4695-8ad4-d78698283ea1";  
  
    /** 申请单来源表：固定为 M_InOut 场景 */  
    private static final String TABLE_NAME = MInOut.Table_Name;  
  
    private OrderInOutRequisitionService() {  
        // 静态工具类，禁止实例化  
    }  
  
    /**  
     * 订单完成后生成出入库申请单（幂等）。  
     *  
     * @param order   已完成的订单  
     * @param trxName 事务名（通常沿用订单所在事务）  
     * @return 新建的申请单；若同一订单已存在有效（非作废）申请单，则返回该已存在的申请单  
     */  
    public static MInOutRequisition createFromOrder(MOrder order, String trxName) {  
        Properties ctx = order.getCtx();  
  
        MInOutRequisition existing = findActiveByOrder(ctx, order.getC_Order_ID(), trxName);  
        if (existing != null) {  
            log.info("C_Order_ID=" + order.getC_Order_ID() + " 已存在关联的 M_InOut_Requisition #"  
                    + existing.getDocumentNo() + "，跳过生成");  
            return existing;  
        }  

        MOrderLine[] orderLines = order.getLines(false, "Line");  
        if (orderLines == null || orderLines.length == 0) {  
            log.warning("订单 " + order.getDocumentNo() + " 无有效明细行，不生成 M_InOut_Requisition");  
            return null;  
        }  
  
        int sourceDocTypeId = order.getC_DocTypeTarget_ID();  
        int targetDocTypeId = resolveTargetDocTypeId(ctx, trxName, sourceDocTypeId, order.isSOTrx());  
  
        MInOutRequisition requisition = buildHeader(order, ctx, trxName, sourceDocTypeId, targetDocTypeId);  
        requisition.saveEx(trxName);  
  
        int lineCount = buildLines(requisition, orderLines, order, ctx, trxName);  
        if (lineCount == 0) {  
            throw new AdempiereException("订单 " + order.getDocumentNo() + " 未生成任何有效的申请单行");  
        }  
  
        log.info("订单 " + order.getDocumentNo() + " 完成，已生成 M_InOut_Requisition #"  
                + requisition.getDocumentNo() + "，共 " + lineCount + " 行");  
        return requisition;  
    }  
  
    /**  
     * 订单作废 / 重新激活后，联动作废对应的 M_InOut_Requisition。  
     *  
     * <p>无论订单是被作废（DOC_AFTER_VOID）还是被重新打开（DOC_AFTER_REACTIVATE），  
     * 已生成的申请单都视为失效，需要重新走一遍 {@link #createFromOrder} 才能产生新的有效申请单。</p>  
     *  
     * @param order   触发事件的订单  
     * @param trxName 事务名  
     */  
    public static void voidByOrder(MOrder order, String trxName) {  
        Properties ctx = order.getCtx();  
        MInOutRequisition requisition = findLatestByOrder(ctx, order.getC_Order_ID(), trxName);  
        if (requisition == null) {  
            log.fine("C_Order_ID=" + order.getC_Order_ID() + " 无关联的 M_InOut_Requisition，无需作废");  
            return;  
        }  
        if (DocAction.STATUS_Voided.equals(requisition.getDocStatus())) {  
            log.fine("M_InOut_Requisition #" + requisition.getDocumentNo() + " 已是作废状态，跳过");  
            return;  
        }  
        if (!requisition.processIt(DocAction.ACTION_Void)) {  
            throw new AdempiereException("M_InOut_Requisition #" + requisition.getDocumentNo()  
                    + " 作废失败: " + requisition.getProcessMsg());  
        }  
        requisition.saveEx(trxName); 
  
        log.info("M_InOut_Requisition #" + requisition.getDocumentNo() + " 已随来源订单联动作废");  
    }  
  
    /**  
     * 按业务规则解析目标单据类型：  
     * <ul>  
     *   <li>IsSOTrx='N' 且来源单据类型为委外订单（{@link #SUBCONTRACT_ORDER_DOCTYPE_UU}） -> 委外收货单</li>  
     *   <li>IsSOTrx='N' 且其他采购订单 -> 普通收货单</li>  
     *   <li>IsSOTrx='Y'（销售订单/打印订单/内部销单等） -> 发货单</li>  
     * </ul>  
     */  
    private static int resolveTargetDocTypeId(Properties ctx, String trxName, int sourceDocTypeId, boolean isSOTrx) {  
        String targetUU;  
        if (isSOTrx) {  
            targetUU = SHIPMENT_DOCTYPE_UU;  
        } else {  
            MDocType sourceDT = MDocType.get(ctx, sourceDocTypeId);  
            if (sourceDT == null || sourceDT.getC_DocType_ID() == 0)  
                throw new AdempiereException("无法解析订单来源单据类型：C_DocTypeTarget_ID=" + sourceDocTypeId);  
  
            targetUU = SUBCONTRACT_ORDER_DOCTYPE_UU.equals(sourceDT.getC_DocType_UU())  
                    ? SUBCONTRACT_RECEIPT_DOCTYPE_UU  
                    : RECEIPT_DOCTYPE_UU;  
        }  
  
        MDocType targetDT = new MDocType(ctx, targetUU, trxName);  
        if (targetDT.getC_DocType_ID() <= 0)  
            throw new AdempiereException("目标单据类型不存在，请检查 C_DocType_UU 配置：" + targetUU);  
  
        return targetDT.getC_DocType_ID();  
    }  
  
    /**  
     * 查找"有效"的申请单，用于生成前的幂等检查。  
     * 作废（DocStatus='VO'）的申请单不计入，因此订单被重新完成后可以再次生成新的申请单。  
     */  
    private static MInOutRequisition findActiveByOrder(Properties ctx, int orderId, String trxName) {  
        int id = DB.getSQLValue(trxName,  
                "SELECT M_InOut_Requisition_ID FROM M_InOut_Requisition "  
                        + "WHERE C_Order_ID=? AND IsActive='Y' AND DocStatus NOT IN ('VO') "  
                        + "ORDER BY Created DESC",  
                orderId);  
        if (id > 0)  
            return new MInOutRequisition(ctx, id, trxName);  
        return null;  
    }  
  
    /**  
     * 查找该订单最近一条申请单，不论其当前 DocStatus，用于作废操作定位目标记录。  
     */  
    private static MInOutRequisition findLatestByOrder(Properties ctx, int orderId, String trxName) {  
        int id = DB.getSQLValue(trxName,  
                "SELECT M_InOut_Requisition_ID FROM M_InOut_Requisition "  
                        + "WHERE C_Order_ID=? AND IsActive='Y' "  
                        + "ORDER BY Created DESC",  
                orderId);  
        if (id > 0)  
            return new MInOutRequisition(ctx, id, trxName);  
        return null;  
    }  
  
    /** 构建申请单表头（不落库，由调用方 saveEx） */  
    private static MInOutRequisition buildHeader(MOrder order, Properties ctx, String trxName,  
            int sourceDocTypeId, int targetDocTypeId) {  
  
        MInOutRequisition requisition = new MInOutRequisition(ctx, 0, trxName);  
        requisition.setAD_Org_ID(order.getAD_Org_ID());  
        requisition.setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());  
        // DocumentNo 留空，由框架按 AD_Sequence 自动生成  
        requisition.setC_DocType_ID(resolveRequisitionDocTypeId(ctx, trxName));  
        requisition.setSourceDocType_ID(sourceDocTypeId);  
        requisition.setTargetDocType_ID(targetDocTypeId);  
        requisition.setInOutType(order.isSOTrx() ? MInOutRequisition.OUT_ORDER_OUT : MInOutRequisition.IN_ORDER_IN);
        requisition.set_ValueOfColumn("TableName", TABLE_NAME);  
        requisition.setDocStatus(DocAction.STATUS_Drafted);  
        requisition.setDocAction(DocAction.ACTION_Complete);  
        requisition.setM_Warehouse_ID(order.getM_Warehouse_ID());  
        requisition.setMovementDate(order.getDatePromised() != null  
                ? order.getDatePromised()  
                : new Timestamp(System.currentTimeMillis()));  
        requisition.setC_Order_ID(order.getC_Order_ID());  
        requisition.setC_BPartner_ID(order.getC_BPartner_ID());  
        requisition.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());  
        requisition.setIsSOTrx(order.isSOTrx());  
        //requisition.setMovementType(order.isSOTrx() ? "C-" : "V+"); // C- 客户发货 / V+ 供应商收货
        requisition.setC_Activity_ID(order.getC_Activity_ID());  
        requisition.setC_Campaign_ID(order.getC_Campaign_ID());  
        requisition.setC_Project_ID(order.getC_Project_ID());  
        requisition.setAD_User_ID(order.getSalesRep_ID());  
        requisition.setDescription("来源订单: " + order.getDocumentNo());  
        return requisition;  
    }  
  
    /** 按订单明细逐行构建申请单明细并落库，返回实际生成的行数 */  
    private static int buildLines(MInOutRequisition requisition, MOrderLine[] orderLines, MOrder order,  
            Properties ctx, String trxName) {  
  
        MWarehouse warehouse = MWarehouse.get(ctx, order.getM_Warehouse_ID());  
        int lineNo = 10;  
        int count = 0;  
        for (MOrderLine ol : orderLines) {  
            if (ol.isDescription())  
                continue;  
            if (ol.getM_Product_ID() <= 0 && ol.getC_Charge_ID() <= 0)  
                continue;  
  
            BigDecimal qtyOrdered = ol.getQtyOrdered();  
            if (qtyOrdered == null || qtyOrdered.signum() == 0)  
                continue;  
  
            MInOutRequisitionLine line = new MInOutRequisitionLine(ctx, 0, trxName);  
            line.setM_InOut_Requisition_ID(requisition.get_ID());  
            line.setAD_Org_ID(ol.getAD_Org_ID());  
            line.setLine(lineNo);  
            line.setDescription(ol.getDescription());  
            line.set_ValueOfColumn("TableName", MOrderLine.Table_Name); // 来源表：C_OrderLine  
            line.setC_OrderLine_ID(ol.getC_OrderLine_ID());  
  
            line.setM_Product_ID(ol.getM_Product_ID());  
            line.setM_Locator_ID(resolveLocatorId(ol, warehouse));  
            line.setM_AttributeSetInstance_ID(ol.getM_AttributeSetInstance_ID());  
            line.setC_UOM_ID(ol.getC_UOM_ID());  
  
            line.setQtyRequested(qtyOrdered);  
            line.setQtyGenerated(Env.ZERO);  
  
            line.setC_Charge_ID(ol.getC_Charge_ID());  
            line.setC_Activity_ID(ol.getC_Activity_ID());  
            line.setC_Campaign_ID(ol.getC_Campaign_ID());  
            line.setC_Project_ID(ol.getC_Project_ID());  
  
            line.saveEx(trxName);  
            lineNo += 10;  
            count++;  
        }  
        return count;  
    }  
  
    /** 库位推荐：产品行取仓库默认库位，费用行不需要库位 */  
    private static int resolveLocatorId(MOrderLine ol, MWarehouse warehouse) {  
        if (ol.getM_Product_ID() <= 0 || warehouse == null)  
            return 0;  
        return warehouse.getDefaultLocator().getM_Locator_ID();  
    }  
  
    /** 解析申请单自身的 C_DocType_ID（固定 UUID，走 MDocType 缓存） */  
    private static int resolveRequisitionDocTypeId(Properties ctx, String trxName) {  
        MDocType dt = new MDocType(ctx, REQUISITION_DOCTYPE_UU, trxName);  
        if (dt.getC_DocType_ID() <= 0)  
            throw new AdempiereException("未找到出入库申请单的 C_DocType_UU 配置，请检查: "  
                    + REQUISITION_DOCTYPE_UU);  
        return dt.getC_DocType_ID();  
    }  
}