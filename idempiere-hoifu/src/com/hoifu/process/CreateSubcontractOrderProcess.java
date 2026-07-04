package com.hoifu.process;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Msg;

@org.adempiere.base.annotation.Process
public class CreateSubcontractOrderProcess extends SvrProcess {

	// ── 单据类型:委外订单 ──────────────────────────────────────────────────────────────
	private static final String SUBCONTRACT_DOCTYPE_UU = "5ae935d6-5237-4d93-ac5b-4967cf9be287";

	// ── 进程参数名 ────────────────────────────────────────────────────────────
	private static final String PARAM_VENDOR_ID = "C_BPartner_ID";

	// ── 自定义列名 ────────────────────────────────────────────────────────────
	/** C_Order 上的委外加工勾选框列名 */
	private static final String COL_IS_SUBCONTRACTING = "IsSubcontracting";
	private static final String COL_IS_CSM = "IsCSM";
	/** C_OrderLine 上存储关联销售订单号的列名 */
	private static final String COL_RELATED_ORDER_NO = "RelatedOrderNo";
	/** C_OrderLine 上存储客户的自定义列名 */
	private static final String COL_CUSTOMER_ID = "Customer_ID";

	// ── 进程参数 ──────────────────────────────────────────────────────────────
	private int p_Vendor_ID = 0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (ProcessInfoParameter p : para) {
			String name = p.getParameterName();
            if (p.getParameter() == null);  
			else if (PARAM_VENDOR_ID.equals(name))
				p_Vendor_ID = ((BigDecimal) p.getParameter()).intValue();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
		}
	}

	@Override
	protected String doIt() throws Exception {

		// 1. 获取当前销售订单（从窗口上下文）
		int soID = getRecord_ID();
		if (soID <= 0)
			throw new AdempiereUserError("未找到销售订单");

		MOrder so = new MOrder(getCtx(), soID, get_TrxName());
		if (so.get_ID() == 0)
			throw new AdempiereUserError("销售订单不存在: " + soID);

		// 2. 前置校验：单据状态必须为"已完成"
		if (!MOrder.DOCSTATUS_Completed.equals(so.getDocStatus()))
			throw new AdempiereUserError("销售订单状态必须为【已完成】，当前状态: " + so.getDocStatus());

		// 2.5 校验：是否已存在委外订单
		Object existingSubcontractID = so.get_Value("Subcontract_ID");
		if (existingSubcontractID instanceof Integer && (Integer) existingSubcontractID > 0) {
			// 查出已有委外订单的单号，提示更友好
			MOrder existingSubOrder = new MOrder(getCtx(), (Integer) existingSubcontractID, get_TrxName());
			throw new AdempiereUserError("该单已生成委外订单【" + existingSubOrder.getDocumentNo() + "】");
		}

		// 3. 前置校验：必须勾选"委外加工"
		if (!(Boolean) so.get_Value(COL_IS_SUBCONTRACTING))
			throw new AdempiereUserError("销售订单未勾选【委外加工】，无法生成委外订单");

		// 4. 校验供应商参数
		if (p_Vendor_ID <= 0)
			throw new AdempiereUserError("请选择供应商");

		MBPartner vendor = new MBPartner(getCtx(), p_Vendor_ID, get_TrxName());
		if (vendor.get_ID() == 0)
			throw new AdempiereUserError("供应商不存在: " + p_Vendor_ID);

		// 5. 获取委外订单单据类型（通过 UUID 查找）
		MDocType docType = new MDocType(getCtx(), SUBCONTRACT_DOCTYPE_UU, get_TrxName());
		if (docType.get_ID() == 0)
            throw new AdempiereUserError(  
                "未找到委外订单单据类型 (UUID=" + SUBCONTRACT_DOCTYPE_UU + ")");  

		// 6. 创建委外订单表头
		MOrder subOrder = new MOrder(getCtx(), 0, get_TrxName());
		subOrder.setClientOrg(so.getAD_Client_ID(), so.getAD_Org_ID());
		subOrder.setIsSOTrx(false);
		subOrder.setC_DocTypeTarget_ID(docType.getC_DocType_ID());
		subOrder.set_ValueOfColumn(COL_IS_CSM, true);
		// 设置供应商（自动带入付款条件、价格表、地址等默认值）
		subOrder.setBPartner(vendor);

		// 继承销售订单字段
		subOrder.setDateOrdered(so.getDateOrdered()); // 订单日期
		subOrder.setDateAcct(so.getDateOrdered());
		subOrder.setDatePromised(so.getDatePromised()); // 承诺日期
		subOrder.setSalesRep_ID(so.getSalesRep_ID()); // 业务人员 → 制单人
		subOrder.setM_Warehouse_ID(so.getM_Warehouse_ID());
		subOrder.setLink_Order_ID(so.getC_Order_ID());
		subOrder.setPOReference(so.getDocumentNo());

		subOrder.setDocStatus(MOrder.DOCSTATUS_Drafted);
		subOrder.setDocAction(MOrder.DOCACTION_Complete);
		subOrder.saveEx();

		// 回写委外订单 ID 到销售订单表头
		DB.executeUpdateEx("UPDATE C_Order SET Subcontract_ID=? WHERE C_Order_ID=?",
				new Object[] { subOrder.getC_Order_ID(), so.getC_Order_ID() }, get_TrxName());

		// 7. 创建委外加工明细
		MOrderLine[] soLines = so.getLines(true, null);
		if (soLines == null || soLines.length == 0)
			throw new AdempiereUserError("销售订单没有明细行，无法生成委外订单");

		int lineNo = 10;
		for (MOrderLine soLine : soLines) {
			if (soLine.getM_Product_ID() == 0)
				continue;

			MOrderLine subLine = new MOrderLine(subOrder);
			subLine.setLine(lineNo);
			subLine.setM_Product_ID(soLine.getM_Product_ID()); // 物料
			subLine.setC_UOM_ID(soLine.getC_UOM_ID()); // 单位
			subLine.setQtyEntered(soLine.getQtyEntered()); // 数量
			subLine.setQtyOrdered(soLine.getQtyOrdered());
			subLine.setPrice(soLine.getPriceActual()); // 单价

			// 关联单号：存入自定义列 RelatedOrderNo
			subLine.set_ValueOfColumn(COL_RELATED_ORDER_NO, so.getDocumentNo());
			// 客户：取销售订单表头客户，存入自定义列 Customer_ID
			subLine.set_ValueOfColumn(COL_CUSTOMER_ID, so.getC_BPartner_ID());

			subLine.saveEx();

			// 回写委外明细行 ID 到销售订单明细行
			DB.executeUpdateEx("UPDATE C_OrderLine SET SubcontractLine_ID=? WHERE C_OrderLine_ID=?",
					new Object[] { subLine.getC_OrderLine_ID(), soLine.getC_OrderLine_ID() }, get_TrxName());

			lineNo += 10;
		}

		if (log.isLoggable(Level.INFO))
			log.info("Created Subcontract Order: " + subOrder.getDocumentNo() + " from SO: " + so.getDocumentNo());

		// 8. 记录日志，生成穿透链接
		String msg = Msg.parseTranslation(getCtx(), "@C_Order_ID@: " + subOrder.getDocumentNo());
		addLog(0, null, null, msg, subOrder.get_Table_ID(), subOrder.getC_Order_ID());

		return "@C_Order_ID@ @Created@: " + subOrder.getDocumentNo();
	}
}