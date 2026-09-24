package com.hoifu.service;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.Properties;

import com.hoifu.service.extsync.wms.WmsInOutRequisitionService;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;  
import org.compiere.model.MInOut;  
import org.compiere.model.MRMA;  
import org.compiere.model.MRMALine;  
import org.compiere.process.DocAction;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;  
  
/**  
 * 出入库申请单（M_InOut_Requisition）通用生成服务 —— M_RMA（退货授权）来源场景。  
 *  
 * <p>本类无实例状态，全部方法为 static。</p>  
 */  
public final class RMAInOutRequisitionService {  
  
    private static final CLogger log = CLogger.getCLogger(RMAInOutRequisitionService.class);  
  
    /** 出入库申请单（M_InOut_Requisition）自身的 C_DocType_ID（固定，与 C_Order 场景共用同一个申请单单据类型） */  
    private static final String REQUISITION_DOCTYPE_UU = "97619cfe-e96f-4e65-b66d-2a73bec5893f";  
  
    /** IsSOTrx='N'：供应商退货授权 -> 供应商退货单 */  
    private static final String VENDOR_RETURN_INOUT_UU = "aaefbaad-f188-4809-aa9c-ec7b9b2e00b8";  
  
    /** IsSOTrx='Y'：客户退货授权 -> 客户退货单 */  
    private static final String CUSTOMER_RETURN_INOUT_UU = "64be6ab3-dfb8-4dc6-8224-1cd22b4d1522";  
  
    /** 申请单来源表：固定为 M_InOut 场景 */  
    private static final String TABLE_NAME = MInOut.Table_Name;  
  
    private RMAInOutRequisitionService() {  
        // 静态工具类，禁止实例化  
    }  
  
    /**  
     * RMA 完成后生成出入库申请单（幂等）。  
     *  
     * @param rma     已完成的退货授权单  
     * @param trxName 事务名（通常沿用 RMA 所在事务）  
     * @return 新建的申请单；若同一 RMA 已存在有效（非作废）申请单，则返回该已存在的申请单  
     */  
    public static MInOutRequisition createFromRMA(MRMA rma, String trxName) {  
        Properties ctx = rma.getCtx();  
  
        MInOutRequisition existing = findActiveByRMA(ctx, rma.getM_RMA_ID(), trxName);  
        if (existing != null) {  
            log.info("M_RMA_ID=" + rma.getM_RMA_ID() + " 已存在关联的 M_InOut_Requisition #"  
                    + existing.getDocumentNo() + "，跳过生成");  
            return existing;  
        }  
  
        MRMALine[] rmaLines = rma.getLines(true);  
        if (rmaLines == null || rmaLines.length == 0) {  
            log.warning("RMA " + rma.getDocumentNo() + " 无有效明细行，不生成 M_InOut_Requisition");  
            return null;  
        }  
  
        MInOut originalInOut = rma.getShipment();  
        if (originalInOut == null)  
            throw new AdempiereException("RMA[" + rma.getDocumentNo() + "] 未关联原始收发单，无法确定仓库/往来单位");  
  
        boolean isSOTrx = rma.isSOTrx();  
        int sourceDocTypeId = rma.getC_DocType_ID();  
        int targetDocTypeId = resolveTargetDocTypeId(ctx, trxName, isSOTrx);  
  
        MInOutRequisition requisition = buildHeader(rma, originalInOut, ctx, trxName,  
                sourceDocTypeId, targetDocTypeId, isSOTrx);  
        requisition.saveEx(trxName);  
  
        int lineCount = buildLines(requisition, rmaLines, ctx, trxName);  
        if (lineCount == 0) {  
            throw new AdempiereException("RMA " + rma.getDocumentNo() + " 未生成任何有效的申请单行");  
        }  
  
        log.info("RMA " + rma.getDocumentNo() + " 完成，已生成 M_InOut_Requisition #"  
                + requisition.getDocumentNo() + "，共 " + lineCount + " 行");  
        return requisition;  
    }  
  
    /**  
     * RMA 作废 / 重新激活后，联动作废对应的 M_InOut_Requisition。  
     *  
     * @param rma     触发事件的 RMA  
     * @param trxName 事务名  
     */  
    public static void voidByRMA(MRMA rma, String trxName) {  
        Properties ctx = rma.getCtx();  
        MInOutRequisition requisition = findLatestByRMA(ctx, rma.getM_RMA_ID(), trxName);  
        if (requisition == null) {  
            log.fine("M_RMA_ID=" + rma.getM_RMA_ID() + " 无关联的 M_InOut_Requisition，无需作废");  
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
    }  
  
    /**  
     * 按业务规则解析目标单据类型：  
     * <ul>  
     *   <li>IsSOTrx='N'（供应商退货授权） -> 供应商退货单</li>  
     *   <li>IsSOTrx='Y'（客户退货授权） -> 客户退货单</li>  
     * </ul>  
     */  
    private static int resolveTargetDocTypeId(Properties ctx, String trxName, boolean isSOTrx) {  
        String targetUU = isSOTrx ? CUSTOMER_RETURN_INOUT_UU : VENDOR_RETURN_INOUT_UU;  
  
        MDocType targetDT = new MDocType(ctx, targetUU, trxName);  
        if (targetDT.getC_DocType_ID() <= 0)  
            throw new AdempiereException("目标退货单单据类型不存在，请检查 C_DocType_UU 配置：" + targetUU);  
  
        return targetDT.getC_DocType_ID();  
    }  
  
    /**  
     * 查找"有效"的申请单，用于生成前的幂等检查。  
     * 作废（DocStatus='VO'）的申请单不计入，因此 RMA 被重新完成后可以再次生成新的申请单。  
     */  
    private static MInOutRequisition findActiveByRMA(Properties ctx, int rmaId, String trxName) {  
        int id = DB.getSQLValue(trxName,  
                "SELECT M_InOut_Requisition_ID FROM M_InOut_Requisition "  
                        + "WHERE M_RMA_ID=? AND IsActive='Y' AND DocStatus NOT IN ('VO') "  
                        + "ORDER BY Created DESC",  
                rmaId);  
        if (id > 0)  
            return new MInOutRequisition(ctx, id, trxName);  
        return null;  
    }  
  
    /**  
     * 查找该 RMA 最近一条申请单，不论其当前 DocStatus，用于作废操作定位目标记录。  
     */  
    private static MInOutRequisition findLatestByRMA(Properties ctx, int rmaId, String trxName) {  
        int id = DB.getSQLValue(trxName,  
                "SELECT M_InOut_Requisition_ID FROM M_InOut_Requisition "  
                        + "WHERE M_RMA_ID=? AND IsActive='Y' "  
                        + "ORDER BY Created DESC",  
                rmaId);  
        if (id > 0)  
            return new MInOutRequisition(ctx, id, trxName);  
        return null;  
    }  
  
    /** 构建申请单表头（不落库，由调用方 saveEx） */  
    private static MInOutRequisition buildHeader(MRMA rma, MInOut originalInOut, Properties ctx, String trxName,  
            int sourceDocTypeId, int targetDocTypeId, boolean isSOTrx) {  
  
        MInOutRequisition requisition = new MInOutRequisition(ctx, 0, trxName);  
        requisition.setAD_Org_ID(rma.getAD_Org_ID());  
        requisition.setAD_OrgTrx_ID(originalInOut.getAD_OrgTrx_ID());  
        // DocumentNo 留空，由框架按 AD_Sequence 自动生成  
        requisition.setC_DocType_ID(resolveRequisitionDocTypeId(ctx, trxName));  
        requisition.setSourceDocType_ID(sourceDocTypeId);  
        requisition.setTargetDocType_ID(targetDocTypeId);
        requisition.setInOutType(isSOTrx? MInOutRequisition.IN_RMA_IN : MInOutRequisition.OUT_RMA_OUT);
        requisition.set_ValueOfColumn("TableName", TABLE_NAME);  
        requisition.setDocStatus(DocAction.STATUS_Drafted);  
        requisition.setDocAction(DocAction.ACTION_Complete);  
        requisition.setM_Warehouse_ID(originalInOut.getM_Warehouse_ID());  
        requisition.setMovementDate(new Timestamp(System.currentTimeMillis()));  
        requisition.setM_RMA_ID(rma.getM_RMA_ID());  
        requisition.setC_BPartner_ID(rma.getC_BPartner_ID());  
        requisition.setC_BPartner_Location_ID(originalInOut.getC_BPartner_Location_ID());  
        requisition.setIsSOTrx(isSOTrx);  
//        requisition.setMovementType(isSOTrx
//                ? MInOut.MOVEMENTTYPE_CustomerReturns   // C+ 客户退货
//                : MInOut.MOVEMENTTYPE_VendorReturns);   // V- 供应商退货
        requisition.setC_Activity_ID(originalInOut.getC_Activity_ID());  
        requisition.setC_Campaign_ID(originalInOut.getC_Campaign_ID());  
        requisition.setC_Project_ID(originalInOut.getC_Project_ID());  
        requisition.setAD_User_ID(rma.getSalesRep_ID());  
        requisition.setDescription("来源RMA: " + rma.getDocumentNo());  
        return requisition;  
    }  
  
    /** 按 RMA 明细逐行构建申请单明细并落库，返回实际生成的行数 */  
    private static int buildLines(MInOutRequisition requisition, MRMALine[] rmaLines,  
            Properties ctx, String trxName) {  
  
        int lineNo = 10;  
        int count = 0;  
        for (MRMALine rl : rmaLines) {  
            if (rl.getM_Product_ID() <= 0 && rl.getC_Charge_ID() <= 0)  
                continue;  
  
            BigDecimal qty = rl.getQty();  
            if (qty == null || qty.signum() == 0)  
                continue;  
  
            MInOutRequisitionLine line = new MInOutRequisitionLine(ctx, 0, trxName);  
            line.setM_InOut_Requisition_ID(requisition.get_ID());  
            line.setAD_Org_ID(rl.getAD_Org_ID());  
            line.setLine(lineNo);  
            line.setDescription(rl.getDescription());  
            line.set_ValueOfColumn("TableName", MRMALine.Table_Name); // 来源表：M_RMALine  
            line.setM_RMALine_ID(rl.getM_RMALine_ID());  
  
            line.setM_Product_ID(rl.getM_Product_ID());  
            line.setM_Locator_ID(rl.getM_Locator_ID());  
            line.setM_AttributeSetInstance_ID(rl.getM_AttributeSetInstance_ID());  
            line.setC_UOM_ID(rl.getC_UOM_ID());  
  
            line.setQtyRequested(qty);  
            line.setQtyGenerated(Env.ZERO);  
  
            line.setC_Charge_ID(rl.getC_Charge_ID());  
            line.setC_Activity_ID(rl.getC_Activity_ID());  
            line.setC_Campaign_ID(rl.getC_Campaign_ID());  
            line.setC_Project_ID(rl.getC_Project_ID());  
  
            line.saveEx(trxName);  
            lineNo += 10;  
            count++;  
        }  
        return count;  
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