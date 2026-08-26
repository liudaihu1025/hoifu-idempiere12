package org.libero.process;

import java.math.BigDecimal;

import org.compiere.model.MDocType;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;

/**
 * 生产补数申请单 → 生成补数生产工单
 * 参照 PPOrderReworkApplyProcess 的"复制原工单生成新工单"模式
 *
 * 前置条件：
 * - 申请单 DocStatus='CO'（已完成）
 * - 申请单 RepairMethod='WO'（工单补数）
 * - 申请单 PP_Order_New_ID 为空（未生成过工单）
 */
@org.adempiere.base.annotation.Process
public class PPOrderRepairCreateOrderProcess extends SvrProcess {

    @Override
    protected void prepare() {
        // 无参数
    }

    @Override
    protected String doIt() throws Exception {

        // 1. 加载申请单（用通用 PO 方式，因 idempiere-mfg 不依赖 idempiere-hoifu）
		PO request = MTable.get(getCtx(), "PP_Order_Repair_Request").getPO(getRecord_ID(), get_TrxName());
		if (request == null || request.get_ID() <= 0)
			throw new IllegalArgumentException("未找到生产补数申请单");

        if (request.get_ID() <= 0)
            throw new IllegalArgumentException("未找到生产补数申请单");

        // 2. 校验：单据状态必须为已完成(CO)
        String docStatus = request.get_ValueAsString("DocStatus");
        if (!"CO".equals(docStatus))
            throw new IllegalStateException("仅【已完成】状态的申请单才能生成工单");

        // 3. 校验：补数方式必须为工单补数(WO)
		String repairMethod = request.get_ValueAsString("RepairMethod");
        if (!"WO".equals(repairMethod))
            throw new IllegalStateException("随销单补数不支持生成工单");

        // 4. 校验：不能重复生成
        int existingNewOrderId = request.get_ValueAsInt("PP_Order_New_ID");
        if (existingNewOrderId > 0)
            throw new IllegalStateException("该申请单已生成过补数工单，不能重复生成");

        // 5. 加载原工单
        int srcOrderId = request.get_ValueAsInt("PP_Order_ID");
        if (srcOrderId <= 0)
            throw new IllegalStateException("申请单未关联原工单");

        MPPOrder srcOrder = new MPPOrder(getCtx(), srcOrderId, get_TrxName());
        if (srcOrder.get_ID() <= 0)
            throw new IllegalStateException("未找到原生产工单");

        // 6. 查找"生产补数"单据类型
        int repairDocTypeId = getRepairDocTypeId();

        // 7. 复制生成新工单（参照 PPOrderReworkApplyProcess 模式）
        MPPOrder newOrder = new MPPOrder(getCtx(), 0, get_TrxName());
        MPPOrder.copyValues(srcOrder, newOrder);

        // copyValues 会跳过 PP_Product_BOM_ID 和 AD_Workflow_ID，需手动设置
        newOrder.setPP_Product_BOM_ID(srcOrder.getPP_Product_BOM_ID());
        newOrder.setAD_Workflow_ID(srcOrder.getAD_Workflow_ID());

        // 单据编号：使用补数单据类型的编号规则
        String docNo = DB.getDocumentNo(repairDocTypeId, get_TrxName(), false, newOrder);
        newOrder.setDocumentNo(docNo);

        // 单据类型
        newOrder.setC_DocTypeTarget_ID(repairDocTypeId);
        newOrder.setC_DocType_ID(repairDocTypeId);

        // 关键：重置文档状态为草稿（copyValues 会复制原工单的 DocStatus='CO'，必须覆盖）
        newOrder.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
        newOrder.setDocAction(MPPOrder.DOCACTION_Prepare);

        // 状态：待发布
        newOrder.set_ValueOfColumn("Orderstatus", "Ready");

        // 关联字段
        newOrder.setC_OrderLine_ID(srcOrder.getC_OrderLine_ID());
        newOrder.setAD_Org_ID(srcOrder.getAD_Org_ID());

        // 日期
        java.sql.Timestamp dateCompleted = (java.sql.Timestamp) request.get_Value("datecompleted");
        if (dateCompleted != null) {
            newOrder.setDateStartSchedule(dateCompleted);
            newOrder.setDateOrdered(dateCompleted);
        }
        java.sql.Timestamp datePromised = (java.sql.Timestamp) request.get_Value("DatePromised");
        if (datePromised != null) {
            newOrder.setDateFinishSchedule(datePromised);
        }

        // 数量：取补数数量
        BigDecimal repairQty = (BigDecimal) request.get_Value("repairqty");
        if (repairQty == null)
            repairQty = Env.ZERO;
        newOrder.setQtyOrdered(repairQty);
        newOrder.setQtyEntered(repairQty);

        // 补数相关字段
        newOrder.set_ValueOfColumn("RepairQty", repairQty);
        newOrder.set_ValueOfColumn("RepairMethod", repairMethod);
        newOrder.set_ValueOfColumn("Shortage_PP_Order_ID", srcOrder.get_ID());

        // 继承自定义字段（印刷咬口等，copyValues 已自动复制）

        // 重置运行时数量字段
        newOrder.setQtyDelivered(Env.ZERO);
        newOrder.setQtyReject(Env.ZERO);
        newOrder.setQtyScrap(Env.ZERO);
        newOrder.setQtyReserved(Env.ZERO);
        newOrder.setDateStart(null);
        newOrder.setDateFinish(null);

        // 重置运行时状态
        newOrder.setProcessed(false);
        newOrder.setProcessing(false);
        newOrder.setIsApproved(false);

        // 保存，触发 explosion() 自动展开 BOM/工艺路线
        newOrder.saveEx();

		// 8. 更新原工单（DB 层）
		DB.executeUpdateEx(
				"UPDATE PP_Order SET Repair_PP_Order_ID=?, IsRepair='Y', RepairMethod=?, "
						+ "Updated=now(), UpdatedBy=? WHERE PP_Order_ID=?",
				new Object[] { newOrder.get_ID(), repairMethod, getAD_User_ID(), srcOrder.get_ID() }, get_TrxName());

		// 同步内存对象，确保 calculateRepairStatus 读到的是最新值
		srcOrder.set_ValueOfColumn("Repair_PP_Order_ID", newOrder.get_ID());
		srcOrder.set_ValueOfColumn("IsRepair", true);
		srcOrder.set_ValueOfColumn("RepairMethod", repairMethod);

		// 计算原工单补数状态
		String newRepairStatus = MPPOrder.calculateRepairStatus(srcOrder);
		DB.executeUpdateEx("UPDATE PP_Order SET RepairStatus=?, Updated=now(), UpdatedBy=? WHERE PP_Order_ID=?",
				new Object[] { newRepairStatus, getAD_User_ID(), srcOrder.get_ID() }, 
                get_TrxName());

        // 9. 更新申请单：记录新工单ID
        DB.executeUpdateEx(
                "UPDATE PP_Order_Repair_Request SET PP_Order_New_ID=?, Updated=now(), UpdatedBy=? "
                        + "WHERE PP_Order_Repair_Request_ID=?",
                new Object[] { newOrder.get_ID(), getAD_User_ID(), request.get_ID() },
                get_TrxName());

		addLog(0, null, null, "已创建工单 " + newOrder.getDocumentNo(), MPPOrder.Table_ID, newOrder.get_ID());

		return "已创建工单 " + newOrder.getDocumentNo();
    }

    /**
     * 查找"生产补数"单据类型的 C_DocType_ID
     */
    private int getRepairDocTypeId() {
        int id = DB.getSQLValueEx(get_TrxName(),
                "SELECT C_DocType_ID FROM C_DocType "
                        + "WHERE DocBaseType=? AND Name=? AND AD_Client_ID=? AND IsActive='Y'",
                MDocType.DOCBASETYPE_ManufacturingOrder, "补数工单", Env.getAD_Client_ID(getCtx()));
        if (id <= 0)
            throw new IllegalStateException("未找到「补数工单」单据类型，请先在字典《Document Type》中配置");
        return id;
    }
}
