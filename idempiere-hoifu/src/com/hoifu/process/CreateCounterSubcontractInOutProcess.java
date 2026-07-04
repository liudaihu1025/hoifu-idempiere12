package com.hoifu.process;

import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MDocTypeCounter;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 反向单据创建: 发货单 -> 委外收货单 
 */
@org.adempiere.base.annotation.Process
public class CreateCounterSubcontractInOutProcess extends SvrProcess {

	// 发货单 UUID
	private static final String SHIPMENT_DOCTYPE_UU = "d7a2250b-ffed-4695-8ad4-d78698283ea1";
	// 委外收货单 UUID
	private static final String RECEIPT_DOCTYPE_UU = "503a5e26-3cb6-4525-94ae-ada837bc13ce";

    private static final String COLUMNNAME_Ref_InOut_No = "Ref_InOut_No";  
    
	private int p_M_InOut_ID = 0;

	@Override
	protected void prepare() {
		p_M_InOut_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		if (p_M_InOut_ID == 0)
			throw new AdempiereException("未选择发货单");

		MInOut inOut = new MInOut(getCtx(), p_M_InOut_ID, get_TrxName());
		if (inOut.get_ID() == 0)
			throw new AdempiereException("找不到发货单记录");

		// 1. 已存在对向收货单 → 提示单号
		if (inOut.getRef_InOut_ID() != 0) {
			MInOut existing = new MInOut(getCtx(), inOut.getRef_InOut_ID(), get_TrxName());
			return "委外入库单已存在，单号：" + existing.getDocumentNo();
		}

		// 2. 确定目标单据类型 ID 和 DocAction
		int C_DocTypeTarget_ID = 0;
		String docAction = null;

		MDocType sourceDT = MDocType.get(getCtx(), inOut.getC_DocType_ID());
		if (!SHIPMENT_DOCTYPE_UU.equals(sourceDT.getC_DocType_UU()))
			return "当前单据类型未配置反向单据规则，不创建";

		MDocType targetDT = new MDocType(getCtx(), RECEIPT_DOCTYPE_UU, get_TrxName());
		if (targetDT.getC_DocType_ID() == 0)
			throw new AdempiereException("目标委外入库单单据类型不存在，请检查 UUID：" + RECEIPT_DOCTYPE_UU);

		C_DocTypeTarget_ID = targetDT.getC_DocType_ID();
		docAction = MDocTypeCounter.DOCACTION_Complete;

		// 3. 检查组织与业务伙伴关联
		MOrg org = MOrg.get(getCtx(), inOut.getAD_Org_ID());
		int counterC_BPartner_ID = org.getLinkedC_BPartner_ID(get_TrxName());
		if (counterC_BPartner_ID == 0)
			throw new AdempiereException("当前组织未关联业务伙伴，无法创建委外入库单");

		MBPartner bp = new MBPartner(getCtx(), inOut.getC_BPartner_ID(), get_TrxName());
		int counterAD_Org_ID = bp.getAD_OrgBP_ID();
		if (counterAD_Org_ID == 0)
			throw new AdempiereException("业务伙伴未关联组织，无法创建委外入库单");

		MBPartner counterBP = new MBPartner(getCtx(), counterC_BPartner_ID, get_TrxName());
		MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID, get_TrxName());

		// ★ 4. 前置检查：发货单关联的销售订单必须已关联委外订单（Ref_Order_ID != 0）
		// 否则 copyFrom 内部无法跳转 C_Order_ID，委外入库单将没有订单来源，直接拒绝创建
		if (inOut.getC_Order_ID() == 0)
			return "发货单未关联销售订单，无法自动创建委外入库单";
		MOrder salesOrder = new MOrder(getCtx(), inOut.getC_Order_ID(), get_TrxName());
		if (salesOrder.getRef_Order_ID() == 0)
			return "发货单关联的销售订单（" + salesOrder.getDocumentNo() + "）未关联委外订单，无法自动创建委外入库单";

		// 5. 深拷贝创建委外入库单
		// copyFrom(counter=true) 内部会：
		// - 建立双向 Ref_InOut_ID 链接
		// - 通过 MOrder.Ref_Order_ID 跳转 C_Order_ID（步骤 4 已确保非零）
		// - 通过 MOrderLine.Ref_OrderLine_ID 跳转行的 C_OrderLine_ID
		Timestamp movementDate = inOut.getMovementDate();
		Timestamp dateAcct = inOut.getDateAcct();
		MInOut counter = MInOut.copyFrom(inOut, movementDate, dateAcct, C_DocTypeTarget_ID, !inOut.isSOTrx(), true,
				get_TrxName(), true);
		if (counter == null)
			throw new AdempiereException("复制发货单失败，请检查组织与业务伙伴配置");

		counter.setAD_Org_ID(counterAD_Org_ID);
		counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
		counter.setBPartner(counterBP);
		counter.setSalesRep_ID(inOut.getSalesRep_ID());
		counter.setMovementType();
		counter.set_ValueOfColumn(COLUMNNAME_Ref_InOut_No, inOut.getDocumentNo());;
		counter.saveEx(get_TrxName());

		// 6. 更新行（委外入库单固定为 P+，inTrx 恒为 true）  
		int warehouseId = counter.getM_Warehouse_ID();  
		MInOutLine[] counterLines = counter.getLines(true);  
		for (MInOutLine counterLine : counterLines) {  
		    //counterLine.setClientOrg(counter);  
		    counterLine.setM_Warehouse_ID(warehouseId);  
		    counterLine.setM_Locator_ID(0);  // 先清零，否则 setM_Locator_ID(BigDecimal) 会短路返回  
		  
		    int productId = counterLine.getM_Product_ID();  
		    if (productId > 0) {  
		        // 调用数据库函数获取推荐库位（P+ 入库，IsSOTrx='N'）  
		        int locatorId = DB.getSQLValue(get_TrxName(),  
		                "SELECT get_recommended_locator(?, ?, ?)",  
		                productId, warehouseId, "N");  
		        if (locatorId > 0) {  
		            counterLine.setM_Locator_ID(locatorId);  
		        } else {  
		            // 函数未返回有效库位，fallback：按库存查找，找不到则用仓库默认库位  
		            counterLine.setM_Locator_ID(Env.ZERO);  
		        }  
		    }  
		    // productId == 0（费用行等非产品行）不设置库位，saveEx 时 beforeSave 会跳过库位校验  
		  
		    counterLine.saveEx(get_TrxName());  
		}

		// 7. 执行 DocAction（如果有配置）
		if (docAction != null && !MDocTypeCounter.DOCACTION_None.equals(docAction)) {
			counter.setDocAction(docAction);
			if (!counter.processIt(docAction))
				throw new AdempiereException("委外入库单处理失败：" + counter.getProcessMsg());
			counter.saveEx(get_TrxName());
		}

		return "委外入库单创建成功，单号：" + counter.getDocumentNo();
	}
}