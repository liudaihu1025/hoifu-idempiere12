package com.hoifu.process;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MBPartner;  
import org.compiere.model.MDocType;  
import org.compiere.model.MDocTypeCounter;  
import org.compiere.model.MOrg;  
import org.compiere.model.MOrgInfo;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.process.SvrProcess;  
  
/**
 * 反向单据创建: 委外订单 -> 销售订单
 */
@org.adempiere.base.annotation.Process  
public class CreateCounterSubcontractOrderProcess extends SvrProcess {  
  
    // 委外订单 UUID → 对应销售订单 UUID  
    private static final String SUBCONTRACT_DOCTYPE_UU = "5ae935d6-5237-4d93-ac5b-4967cf9be287";  
    //销售订单 UUID  
    private static final String SALES_DOCTYPE_UU       = "4276ac3a-467b-4821-bf44-43d0490af1dc";  
  
    private int p_C_Order_ID = 0;  
  
    @Override  
    protected void prepare() {  
        p_C_Order_ID = getRecord_ID();  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        if (p_C_Order_ID == 0)  
            throw new AdempiereException("未选择订单");  
  
        MOrder order = new MOrder(getCtx(), p_C_Order_ID, get_TrxName());  
        if (order.get_ID() == 0)  
            throw new AdempiereException("找不到订单记录");  
  
        // 1. 已存在内部委外销单 → 提示单号  
        if (order.getRef_Order_ID() != 0) {  
            MOrder existing = new MOrder(getCtx(), order.getRef_Order_ID(), get_TrxName());  
            return "内部委外销单已存在，单号：" + existing.getDocumentNo();  
        }  
  
        // 2. 确定目标单据类型 ID 和 DocAction  
        int C_DocTypeTarget_ID = 0;  
        String docAction = null;  
  
        MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(), order.getC_DocType_ID());  
        if (counterDT != null && counterDT.isCreateCounter() && counterDT.isValid()) {  
            // 有有效的 C_DocTypeCounter 配置 → 直接使用  
            C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();  
            docAction = counterDT.getDocAction();  
        } else {  
            // 无有效配置 → 判断是否委外订单  
            MDocType sourceDT = MDocType.get(getCtx(), order.getC_DocType_ID());  
            if (!SUBCONTRACT_DOCTYPE_UU.equals(sourceDT.getC_DocType_UU()))  
                return "当前单据类型未配置反向单据规则，不创建";  
  
            MDocType targetDT = new MDocType(getCtx(), SALES_DOCTYPE_UU, get_TrxName());  
            if (targetDT.getC_DocType_ID() == 0)  
                throw new AdempiereException("目标销售订单单据类型不存在，请检查 UUID：" + SALES_DOCTYPE_UU);  
  
            C_DocTypeTarget_ID = targetDT.getC_DocType_ID();  
            docAction = MDocTypeCounter.DOCACTION_Complete; // 'CO'，自动完成  
        }  
  
        // 3. 检查组织与业务伙伴关联  
        MOrg org = MOrg.get(getCtx(), order.getAD_Org_ID());  
        int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName());  
        if (counterC_BPartner_ID == 0)  
            throw new AdempiereException("当前组织未关联业务伙伴，无法创建内部委外销单");  
  
        MBPartner bp = new MBPartner(getCtx(), order.getC_BPartner_ID(), get_TrxName());  
        int counterAD_Org_ID = bp.getAD_OrgBP_ID();  
        if (counterAD_Org_ID == 0)  
            throw new AdempiereException("业务伙伴未关联组织，无法创建内部委外销单");  
  
        MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());  
  
        // 4. 深拷贝创建内部委外销单（同时设置双向 Ref_Order_ID）  
        MOrder counter = MOrder.copyFrom(order, order.getDateOrdered(),  
                C_DocTypeTarget_ID, !order.isSOTrx(), true, false, get_TrxName());  
        if (counter == null)  
            throw new AdempiereException("复制订单失败，请检查组织与业务伙伴配置");  
  
        counter.setAD_Org_ID(counterAD_Org_ID);  
        counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());  
        counter.setDatePromised(order.getDatePromised());  
        counter.setSalesRep_ID(order.getSalesRep_ID());  
        counter.saveEx(get_TrxName());  
  
        // 5. 更新行（重新计算税等）  
        MOrderLine[] counterLines = counter.getLines(true, null);  
        for (MOrderLine counterLine : counterLines) {  
            counterLine.setOrder(counter);  
            counterLine.setTax();  
            counterLine.saveEx(get_TrxName());  
        }  
  
        // 6. 执行 DocAction（如果有配置）  
        if (docAction != null && !MDocTypeCounter.DOCACTION_None.equals(docAction)) {  
            counter.setDocAction(docAction);  
            if (!counter.processIt(docAction))  
                throw new AdempiereException("内部委外销单处理失败：" + counter.getProcessMsg());  
            counter.saveEx(get_TrxName());  
        }  
  
        return "内部委外销单创建成功，单号：" + counter.getDocumentNo();  
    }  
}