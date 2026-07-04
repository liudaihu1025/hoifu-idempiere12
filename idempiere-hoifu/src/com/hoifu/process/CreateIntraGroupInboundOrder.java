package com.hoifu.process;  
  
import java.sql.Timestamp;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MBPartner;  
import org.compiere.model.MDocType;  
import org.compiere.model.MDocTypeCounter;  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.model.MOrg;  
import org.compiere.model.MOrgInfo;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Env;
import org.compiere.wf.MWorkflow;  
  
/**
 * 反向单据创建: 集团内出库单 ->集团内入库单
 */
@org.adempiere.base.annotation.Process  
public class CreateIntraGroupInboundOrder extends SvrProcess {  
  
    // 集团内出库单 UUID（IsSOTrx=true）  
    private static final String OUTBOUND_DOCTYPE_UU = "8e07456e-132c-4fdb-9996-55d540c530c7";  
    // 集团内入库单 UUID（IsSOTrx=false）  
    private static final String INBOUND_DOCTYPE_UU  = "dad137e1-fcf1-4d66-a250-0a83ea8167bd";  
  
    private static final String COLUMNNAME_Ref_InOut_No = "Ref_InOut_No";  
  
    private int p_M_InOut_ID = 0;  
  
    @Override  
    protected void prepare() {  
        p_M_InOut_ID = getRecord_ID();  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        if (p_M_InOut_ID == 0)  
            throw new AdempiereException("未选择集团内出库单");  
  
        MInOut outbound = new MInOut(getCtx(), p_M_InOut_ID, get_TrxName());  
        if (outbound.get_ID() == 0)  
            throw new AdempiereException("找不到集团内出库单记录");  
  
        // 1. 已存在对向入库单 → 提示单号  
        if (outbound.getRef_InOut_ID() != 0) {  
            MInOut existing = new MInOut(getCtx(), outbound.getRef_InOut_ID(), get_TrxName());  
            return "集团内入库单已存在，单号：" + existing.getDocumentNo();  
        }  
  
        // 2. 校验源单据类型  
        MDocType sourceDT = MDocType.get(getCtx(), outbound.getC_DocType_ID());  
        if (!OUTBOUND_DOCTYPE_UU.equals(sourceDT.getC_DocType_UU()))  
            return "当前单据类型不是集团内出库单，不创建";  
  
        // 3. 确定目标单据类型  
        MDocType targetDT = new MDocType(getCtx(), INBOUND_DOCTYPE_UU, get_TrxName());  
        if (targetDT.getC_DocType_ID() == 0)  
            throw new AdempiereException("目标集团内入库单单据类型不存在，请检查 UUID：" + INBOUND_DOCTYPE_UU);  
  
        int C_DocTypeTarget_ID = targetDT.getC_DocType_ID();  
  
        // 4. 检查组织与业务伙伴关联  
        MOrg org = MOrg.get(getCtx(), outbound.getAD_Org_ID());  
        int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName());  
        if (counterC_BPartner_ID == 0)  
            throw new AdempiereException("当前组织未关联业务伙伴，无法创建集团内入库单");  
  
        MBPartner bp = new MBPartner(getCtx(), outbound.getC_BPartner_ID(), get_TrxName());  
        int counterAD_Org_ID = bp.getAD_OrgBP_ID();  
        if (counterAD_Org_ID == 0)  
            throw new AdempiereException("业务伙伴未关联组织，无法创建集团内入库单");  
  
        MBPartner counterBP = new MBPartner(getCtx(), counterC_BPartner_ID, get_TrxName());  
        MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());  
  
        // 5. 深拷贝创建集团内入库单  
        // C_Order_ID=0 → copyFrom 内部订单跳转逻辑自动跳过，无需前置订单检查  
        // counter=true → setMovementType() 自动按 isSOTrx=false 设为 V+  
        //              → 双向 Ref_InOut_ID 链接自动建立  
        Timestamp movementDate = outbound.getMovementDate();  
        Timestamp dateAcct     = outbound.getDateAcct();  
        MInOut inbound = MInOut.copyFrom(  
                outbound, movementDate, dateAcct,  
                C_DocTypeTarget_ID, !outbound.isSOTrx(), // isSOTrx=false  
                true,              // counter=true  
                get_TrxName(), true);  
        if (inbound == null)  
            throw new AdempiereException("复制集团内出库单失败，请检查组织与业务伙伴配置");  
  
        inbound.setAD_Org_ID(counterAD_Org_ID);  
        inbound.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());  
        inbound.setBPartner(counterBP);  
        inbound.setSalesRep_ID(outbound.getSalesRep_ID());  

        inbound.set_ValueOfColumn(COLUMNNAME_Ref_InOut_No, outbound.getDocumentNo());  
        inbound.saveEx(get_TrxName());  
  
        // 6. 更新行：设置目标仓库库位（G+ 入库，inTrx=true）  
        int warehouseId = inbound.getM_Warehouse_ID();  
        MInOutLine[] inboundLines = inbound.getLines(true);  
        for (MInOutLine inboundLine : inboundLines) {  
            inboundLine.setM_Warehouse_ID(warehouseId);  
            inboundLine.setM_Locator_ID(0);   // 先清零，避免短路  
  
            int productId = inboundLine.getM_Product_ID();  
            if (productId > 0) {  
                // 调用数据库函数获取推荐库位（G+ 入库，IsSOTrx='N'）  
                int locatorId = DB.getSQLValue(get_TrxName(),  
                        "SELECT get_recommended_locator(?, ?, ?)",  
                        productId, warehouseId, "N");  
                if (locatorId > 0) {  
                    inboundLine.setM_Locator_ID(locatorId);  
                } else {  
                    inboundLine.setM_Locator_ID(Env.ZERO); // fallback：仓库默认库位  
                }  
            }  
            inboundLine.saveEx(get_TrxName());  
        }  
  
        // 7. 自动完成集团内入库单  
        ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(inbound, DocAction.ACTION_Complete);  
        if (pi.isError())  
            throw new AdempiereException("自动完成集团内入库单失败：" + pi.getSummary());  
        inbound.load(get_TrxName());  
        if (!DocAction.STATUS_Completed.equals(inbound.getDocStatus()))  
            throw new AdempiereException("集团内入库单未能完成，当前状态：" + inbound.getDocStatus());  
          
        return "集团内入库单创建并完成，单号：" + inbound.getDocumentNo();
    }  
}