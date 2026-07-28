package com.hoifu.service;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.logging.Level;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MOrg;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;
import org.compiere.wf.MWorkflow;

import com.hoifu.model.MInOutNotice;  
import com.hoifu.model.MInOutNoticeLine;  
  
public class InOutNoticeService {  
  
    private static final String OUT_NOTICE_DOCTYPE_UU = "69cefa28-a09d-4d5c-b37e-ef0928c67400";  
  
    protected static CLogger log = CLogger.getCLogger(InOutNoticeService.class);  
  
    public boolean isBToANotice(MInOutNotice notice, String trxName) {  
        if (notice.getC_Order_ID() <= 0)  
            return false;  
        MOrder bSO = new MOrder(notice.getCtx(), notice.getC_Order_ID(), trxName);  
        if (bSO.getRef_Order_ID() <= 0)  
            return false;  
        int aSOId = DB.getSQLValue(trxName,  
                "SELECT C_Order_ID FROM C_Order WHERE Subcontract_ID = ?",  
                bSO.getRef_Order_ID());  
        return aSOId > 0;  
    }  
  
    public MInOutNotice createNoticeFromBNotice(MInOutNotice bNotice, String trxName) {  
  
        // 取 B 组织真实名称（后续提示使用）  
        String bOrgName = MOrg.get(bNotice.getCtx(), bNotice.getAD_Org_ID()).getName();  
  
        // ── 1. B销售订单 ──────────────────────────────────────────────────────  
        MOrder bSO = new MOrder(bNotice.getCtx(), bNotice.getC_Order_ID(), trxName);  
        if (bSO.get_ID() == 0)  
            throw new AdempiereException(  
                    "找不到【" + bOrgName + "】销售订单，C_Order_ID=" + bNotice.getC_Order_ID());  
  
        // ── 2. A委外采购订单（通过 Ref_Order_ID）────────────────────────────  
        int aPOId = bSO.getRef_Order_ID();  
        if (aPOId <= 0)  
            throw new AdempiereException(  
                    "【" + bOrgName + "】销售订单未关联委外采购订单（Ref_Order_ID 为空），单号：" + bSO.getDocumentNo());  
  
        MOrder aPO = new MOrder(bNotice.getCtx(), aPOId, trxName);  
        if (aPO.get_ID() == 0)  
            throw new AdempiereException(  
                    "找不到委外采购订单，C_Order_ID=" + aPOId);  
  
        // ── 3. A销售订单（通过 Subcontract_ID 反查）─────────────────────────  
        int aSOId = DB.getSQLValue(trxName,  
                "SELECT C_Order_ID FROM C_Order WHERE Subcontract_ID = ?", aPOId);  
        if (aSOId <= 0)  
            throw new AdempiereException(  
                    "找不到对应的销售订单（Subcontract_ID=" + aPOId + "），请检查委外订单关联");  
  
        MOrder aSO = new MOrder(bNotice.getCtx(), aSOId, trxName);  
        if (aSO.get_ID() == 0)  
            throw new AdempiereException(  
                    "找不到销售订单，C_Order_ID=" + aSOId);  
  
        // 取 A 组织真实名称（此时已确定 aSO）  
        String aOrgName = MOrg.get(bNotice.getCtx(), aSO.getAD_Org_ID()).getName();  
  
        if (!MOrder.DOCSTATUS_Completed.equals(aSO.getDocStatus()))  
            throw new AdempiereException(  
                    "【" + aOrgName + "】销售订单状态不是已完成，无法创建发货通知单，单号：" + aSO.getDocumentNo());  
  
        // ── 4. 查找发货通知单单据类型 ─────────────────────────────────────────  
        int docTypeId = DB.getSQLValueEx(trxName,  
                "SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU=?",  
                OUT_NOTICE_DOCTYPE_UU);  
        if (docTypeId <= 0)  
            throw new AdempiereException(  
                    "未找到发货通知单单据类型（UUID=" + OUT_NOTICE_DOCTYPE_UU + "）");  
  
        // ── 5. 创建 A 组织发货通知单表头 ─────────────────────────────────────  
        MInOutNotice aNotice = new MInOutNotice(bNotice.getCtx(), 0, trxName);  
        aNotice.setSuppressAutoLines(true);  
        aNotice.setAD_Org_ID(aSO.getAD_Org_ID());  
        aNotice.setC_DocType_ID(docTypeId);  
        aNotice.setC_Order_ID(aSO.getC_Order_ID());  
        aNotice.setC_BPartner_ID(aSO.getC_BPartner_ID());  
        aNotice.setDateTrx(new Timestamp(System.currentTimeMillis()));  
        aNotice.setDatePromised(bNotice.getDatePromised());  
        aNotice.setSalesRep_ID(Env.getAD_User_ID(bNotice.getCtx()));  
        aNotice.saveEx();  
  
        // ── 6. 创建明细行 ─────────────────────────────────────────────────────  
        int lineNo = 10;  
        for (MInOutNoticeLine bLine : bNotice.getLines()) {  
            if (bLine.getC_OrderLine_ID() <= 0)  
                continue;  
  
            MOrderLine bSOLine = new MOrderLine(bNotice.getCtx(), bLine.getC_OrderLine_ID(), trxName);  
            if (bSOLine.get_ID() == 0)  
                continue;  
  
            int aPOLineId = bSOLine.getRef_OrderLine_ID();  
            if (aPOLineId <= 0) {  
                if (log.isLoggable(Level.WARNING))  
                    log.warning("【" + bOrgName + "】销售订单行 Ref_OrderLine_ID 为空，跳过，C_OrderLine_ID="  
                            + bLine.getC_OrderLine_ID());  
                continue;  
            }  
  
            int aSOLineId = DB.getSQLValue(trxName,  
                    "SELECT C_OrderLine_ID FROM C_OrderLine WHERE SubcontractLine_ID = ?",  
                    aPOLineId);  
            if (aSOLineId <= 0) {  
                if (log.isLoggable(Level.WARNING))  
                    log.warning("找不到【" + aOrgName + "】销售订单行（SubcontractLine_ID=" + aPOLineId + "），跳过");  
                continue;  
            }  
  
            MOrderLine aSOLine = new MOrderLine(bNotice.getCtx(), aSOLineId, trxName);  
            if (aSOLine.get_ID() == 0)  
                continue;  
  
            MInOutNoticeLine aLine = new MInOutNoticeLine(bNotice.getCtx(), 0, trxName);  
            aLine.setAD_Org_ID(aNotice.getAD_Org_ID());  
            aLine.setM_InOutNotice_ID(aNotice.getM_InOutNotice_ID());  
            aLine.setLine(lineNo);  
            aLine.setC_OrderLine_ID(aSOLine.getC_OrderLine_ID());  
            aLine.setM_Product_ID(aSOLine.getM_Product_ID());  
            aLine.setC_UOM_ID(aSOLine.getC_UOM_ID());  
            aLine.setQtyEntered(bLine.getQtyEntered());  
            aLine.setQtyDelivered(BigDecimal.ZERO);  
            aLine.saveEx();  
  
            lineNo += 10;  
        }  
  
        if (lineNo == 10)  
            throw new AdempiereException(  
                    "未能生成任何明细行，请检查【" + bOrgName + "】通知单行的 Ref_OrderLine_ID 及 SubcontractLine_ID 关联");  
  
        if (log.isLoggable(Level.INFO))  
            log.info("已根据【" + bOrgName + "】发货通知单 [" + bNotice.getDocumentNo()  
                    + "] 创建【" + aOrgName + "】发货通知单 [" + aNotice.getDocumentNo() + "]");  
  
        // ── 7. 触发工作流（走审批流程，不强制直接完成）────────────────────────  
        ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(aNotice, DocAction.ACTION_Complete);  
        if (pi == null)  
            throw new AdempiereException(  
                    "【" + aOrgName + "】发货通知单未配置单据处理工作流（DocAction 列或流程未找到）");  
        // 刷新对象状态（工作流可能已改变 DocStatus）  
        aNotice.load(trxName);  
        if (pi.isError())  
            throw new AdempiereException(  
                    "触发【" + aOrgName + "】发货通知单工作流失败：" + pi.getSummary());  
  
        if (log.isLoggable(Level.INFO))  
            log.info("已根据【" + bOrgName + "】发货通知单 [" + bNotice.getDocumentNo()  
                    + "] 创建【" + aOrgName + "】发货通知单 [" + aNotice.getDocumentNo()  
                    + "]，当前状态：" + aNotice.getDocStatus());  
  
        return aNotice;
    }  
}