package com.hoifu.process;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
  
/**
 * 普通入库单 -> 集团内出库单（非反向单据）
 */
@org.adempiere.base.annotation.Process  
public class CreateIntraGroupOutboundOrder extends SvrProcess {  
  
    /** 集团内出库单 DocType UUID */  
    private static final String OUTBOUND_DOCTYPE_UU = "8e07456e-132c-4fdb-9996-55d540c530c7";  
  
    /** 参数：入库方（B组织）的关联业务伙伴ID */  
    private int p_C_BPartner_ID = 0;  
  
    @Override  
    protected void prepare() {  
        for (ProcessInfoParameter p : getParameter()) {  
            String name = p.getParameterName();  
            if (p.getParameter() == null)  
                continue;  
            if ("C_BPartner_ID".equals(name))  
                p_C_BPartner_ID = p.getParameterAsInt();  
            else  
                log.warning("Unknown Parameter: " + name);  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
  
        // ── 1. 加载当前采购入库单 ──────────────────────────────────────────  
        MInOut receipt = new MInOut(getCtx(), getRecord_ID(), get_TrxName());  
        if (receipt.get_ID() == 0)  
            throw new Exception("采购入库单不存在: " + getRecord_ID());  
  
        // ── 2. 校验参数 ───────────────────────────────────────────────────  
        if (p_C_BPartner_ID == 0)  
            throw new Exception("请指定入库方业务伙伴（B组织的关联BP）");  
  
        // ── 3. 查找集团内出库单 DocType ───────────────────────────────────  
        int outboundDocTypeID = DB.getSQLValueEx(get_TrxName(),  
            "SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU=?",  
            OUTBOUND_DOCTYPE_UU);  
        if (outboundDocTypeID <= 0)  
            throw new Exception("找不到集团内出库单单据类型 (UU=" + OUTBOUND_DOCTYPE_UU + ")");  
  
        // ── 4. 获取入库方BP的默认收货地址 ────────────────────────────────  
        int bpLocationID = DB.getSQLValueEx(get_TrxName(),  
            "SELECT C_BPartner_Location_ID FROM C_BPartner_Location " +  
            "WHERE C_BPartner_ID=? AND IsActive='Y' " +  
            "ORDER BY IsShipTo DESC, C_BPartner_Location_ID FETCH FIRST 1 ROWS ONLY",  
            p_C_BPartner_ID);  
  
        // ── 5. 创建集团内出库单头 ─────────────────────────────────────────  
        // 出库方就是 A 组织，仓库就是采购入库单对应的仓库  
        Timestamp movementDate = receipt.getMovementDate() != null  
            ? receipt.getMovementDate()  
            : new Timestamp(System.currentTimeMillis());  
  
        MInOut outbound = new MInOut(getCtx(), 0, get_TrxName());  
        //outbound.setAD_Client_ID(receipt.getAD_Client_ID());  
        outbound.setAD_Org_ID(receipt.getAD_Org_ID());           // A 组织  
        outbound.setC_DocType_ID(outboundDocTypeID);  
        outbound.setIsSOTrx(true);  
        outbound.setMovementType();
        outbound.setM_Warehouse_ID(receipt.getM_Warehouse_ID()); // A 组织的仓库  
        outbound.setC_BPartner_ID(p_C_BPartner_ID);              // B 组织的关联 BP  
        if (bpLocationID > 0)  
            outbound.setC_BPartner_Location_ID(bpLocationID);  
        outbound.setMovementDate(movementDate);  
        outbound.setDateAcct(receipt.getDateAcct() != null ? receipt.getDateAcct() : movementDate);  
        outbound.setDateOrdered(receipt.getDateOrdered() != null ? receipt.getDateOrdered() : movementDate);  
        outbound.setDescription("集团内出库 ← （收货单）" + receipt.getDocumentNo());  
        outbound.setDocStatus(MInOut.DOCSTATUS_Drafted);  
        outbound.setDocAction(MInOut.DOCACTION_Complete);  
        outbound.saveEx(get_TrxName());  
  
        // ── 6. 复制明细行，获取出库库位 ──────────────────────────────────  
        MInOutLine[] receiptLines = receipt.getLines(false);  
        if (receiptLines.length == 0)  
            throw new Exception("采购入库单没有明细行");  
  
        for (MInOutLine receiptLine : receiptLines) {  
            int productID = receiptLine.getM_Product_ID();  
            if (productID == 0)  
                continue; // 跳过费用/Charge行  
  
            // 优先调用自定义函数获取推荐库位  
            int locatorID = DB.getSQLValueEx(get_TrxName(),  
                "SELECT adempiere.get_recommended_locator(?, ?, 'Y')",  
                new Object[]{  
                    new BigDecimal(productID),  
                    new BigDecimal(receipt.getM_Warehouse_ID())  
                });  
  
            if (locatorID <= 0)
            	locatorID = receiptLine.getM_Locator_ID();
 
  
            MInOutLine outboundLine = new MInOutLine(outbound);  
            outboundLine.setM_Product_ID(productID, receiptLine.getC_UOM_ID());  
            outboundLine.setM_Locator_ID(locatorID);  
            outboundLine.setQtyEntered(receiptLine.getQtyEntered());  
            outboundLine.setMovementQty(receiptLine.getMovementQty());  
            outboundLine.setC_UOM_ID(receiptLine.getC_UOM_ID());  
            outboundLine.setDescription(receiptLine.getDescription());  
            outboundLine.saveEx(get_TrxName());  
        }  
  
        addBufferLog(0, null, null,  
            "集团内出库单已创建: " + outbound.getDocumentNo(),  
            MInOut.Table_ID, outbound.getM_InOut_ID());  
  
        return "集团内出库单已创建: " + outbound.getDocumentNo();  
    }  
}