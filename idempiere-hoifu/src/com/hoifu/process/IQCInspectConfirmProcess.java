package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MNote;
import org.compiere.model.MRMA;
import org.compiere.model.MUser;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.wf.MWorkflow;

import com.hoifu.model.MQC_IQCInspect;

/**
 * 来料检验确认处理
 *
 * 用途：根据检验结果执行后续操作
 *   - 合格 / 放行 → 生成移库单（待检库位 → 目标库位）
 *   - 不合格 + 退货 → 生成供应商退货授权单（MRMA）
 *
 * 参数说明：
 *   InspectResult  - 检验结果（Y=合格, N=不合格）
 *   HandleMethod   - 处理方式（G=放行, R=退货）
 *   Description    - 备注说明
 *
 * 前置条件：
 *   InspectStatus = 'N'（未确认的检验单才能执行）
 *
 * 后续操作：
 *   1. 更新检验单状态（InspectStatus=Y, Inspector, InspectDate 等）
 *   2. 根据结果分支：
 *      - 合格或放行 → 创建移库单，从待检库位移至目标库位，自动完成
 *      - 不合格且退货 → 创建供应商退货授权单，触发审批流程
 *   3. 发送通知给收货单创建人（工作台通知，可点击跳转到收货单）
 *
 * @author hoifu
 */
@org.adempiere.base.annotation.Process(name="IQCInspectConfirm")
public class IQCInspectConfirmProcess extends SvrProcess {

    /** 检验结果：Y=合格, N=不合格 */
    private String p_inspectResult = "N";
    /** 处理方式：G=放行, R=退货 */
    private String p_handleMethod = null;
    /** 备注说明 */
    private String p_description = null;

	private int p_AD_Image_ID = 0;

    /** 供应商退货授权单据类型名称（C_DocType.Name） */
    private static final String VENDOR_RETURN_RMA_DOC_TYPE_NAME = "供应商退货授权";
    /** 待检库位类型名称（M_LocatorType.Name） */
    private static final String LOCATOR_TYPE_INSPECTION = "待检库位";

	private static final String VENDOR_RETURN_RMA_TYPE_NAME = "01_供应商退货";

    /**
     * 准备阶段：读取流程参数
     */
    @Override
    protected void prepare() {
        ProcessInfoParameter[] paras = getParameter();
        for (ProcessInfoParameter para : paras) {
            String name = para.getParameterName();
            if ("InspectResult".equals(name)) {
				p_inspectResult = para.getParameterAsString();
            } else if ("HandleMethod".equals(name)) {
                p_handleMethod = para.getParameterAsString();
            } else if ("Description".equals(name)) {
                p_description = para.getParameterAsString();
			} else if ("AD_Image_ID".equals(name)) {
				p_AD_Image_ID = para.getParameterAsInt();
            }
        }
    }

    /**
     * 执行阶段：校验 → 更新检验单 → 分支处理
     */
    @Override
    protected String doIt() throws Exception {

        // 1. 加载检验单
		int qcInspectId = resolveQCInspectId();
		MQC_IQCInspect inspect = new MQC_IQCInspect(getCtx(), qcInspectId, get_TrxName());
		if (inspect.get_ID() <= 0) {
			throw new Exception("@QC_IQCInspect_ID@ @NotFound@");
        }

        // 2. 校验：已确认的检验单不允许重复处理
		String currentStatus = (String) inspect.get_Value("InspectStatus");
		if ("Y".equals(currentStatus)) {
			throw new Exception("@IQCInspectConfirm@ - @AlreadyProcessed@");
		}

		if ("N".equals(p_inspectResult) && !"G".equals(p_handleMethod) && !"R".equals(p_handleMethod)) {
			throw new Exception("@HandleMethod@ @Mandatory@");
		}

        // 3. 更新检验单字段（InspectStatus/InspectResult 为 CHAR(1)，必须传 "Y"/"N" 字符串）
        inspect.set_ValueOfColumn("InspectStatus", "Y");
        inspect.set_ValueOfColumn("InspectResult", p_inspectResult);
        inspect.set_ValueOfColumn("HandleMethod", p_handleMethod);
        inspect.set_ValueOfColumn("Description", p_description);
        inspect.set_ValueOfColumn("InspectDate", new Timestamp(System.currentTimeMillis()));
        // Inspector 字段语义为检验人员姓名（VARCHAR），取当前登录用户名称
        int userId = Env.getContextAsInt(getCtx(), "#AD_User_ID");
        String userName = MUser.get(getCtx(), userId).getName();
        inspect.set_ValueOfColumn("Inspector", userName);

		if (p_AD_Image_ID > 0) {
			inspect.set_ValueOfColumn("AD_Image_ID", p_AD_Image_ID);
		}

        inspect.saveEx();

        // 4. 根据检验结果和处理方式分支
        if ("Y".equals(p_inspectResult) || "G".equals(p_handleMethod)) {
            // 合格 或 放行 → 移库（待检库位 → 目标库位）
            doTransferMovement(inspect);
        } else {
            // 不合格 + 退货 → 创建供应商退货授权单
            doCreateRMA(inspect);
        }

		return "@SUCCESS@";
    }

	private int resolveQCInspectId() throws Exception {

		// 场景1：普通窗口多选
		List<Integer> recordIds = getRecord_IDs();
		if (recordIds != null && !recordIds.isEmpty()) {
			if (recordIds.size() > 1)
				throw new AdempiereException("只允许选择一条检验单，当前选中了 " + recordIds.size() + " 条");
			return recordIds.get(0);
		}

		// 场景2：普通窗口单条表单
		int recordId = getRecord_ID();
		if (recordId > 0)
			return recordId;

		// 场景3：信息窗口 T_Selection
		List<Integer> selectedIds = new ArrayList<>();
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";

		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, getAD_PInstance_ID());
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next())
					selectedIds.add(rs.getInt(1));
			}
		}

		if (selectedIds.isEmpty())
			throw new AdempiereException("未找到选中的检验单，请先选择一条记录");
		if (selectedIds.size() > 1)
			throw new AdempiereException("只允许选择一条检验单，当前选中了 " + selectedIds.size() + " 条");
		return selectedIds.get(0);
	}

    /**
     * 移库处理：从待检库位移至目标库位
     *
     * 流程：
     *   1. 根据收货单仓库查找待检库位类型的库位（M_LocatorType.Name='待检库位'）
     *   2. 读取出库行的目标库位（IntendedLocation_ID）
     *   3. 创建移库单 + 移库明细
     *   4. 触发完成流程
     *   5. 发送通知给收货单创建人
     */
    private void doTransferMovement(MQC_IQCInspect inspect) throws Exception {
        int inOutLineId = inspect.get_ValueAsInt("M_InOutLine_ID");
        MInOutLine line = new MInOutLine(getCtx(), inOutLineId, get_TrxName());
        MInOut receipt = new MInOut(getCtx(), line.getM_InOut_ID(), get_TrxName());

        // 查找待检库位类型的库位（按仓库 + 库位类型）
        int warehouseId = receipt.getM_Warehouse_ID();
        int reservedLocatorId = getReservedLocatorId(warehouseId);

        // 读取目标库位（IntendedLocation_ID 为自定义列，通过 get_Value 获取）
        Object intendedLocObj = line.get_Value("IntendedLocation_ID");
		int intendedLocId = intendedLocObj != null ? ((Number) intendedLocObj).intValue() : 0;

        if (intendedLocId <= 0) {
            throw new Exception("@M_InOut_ID@ [" + receipt.getDocumentNo() + "] @IntendedLocation_ID@ @NotFound@/@Mandatory@");
        }

		MLocator sourceLocator = MLocator.get(getCtx(), reservedLocatorId); // 待检库位
		MLocator targetLocator = MLocator.get(getCtx(), intendedLocId); // 目标库位（IntendedLocation_ID）

		// 创建调拨单头（C_DocType 由 MMovement.beforeSave() 自动按 DocBaseType=MMM 查找默认类型）
        MMovement move = new MMovement(getCtx(), 0, get_TrxName());
		move.setM_Warehouse_ID(sourceLocator.getM_Warehouse_ID());
		move.setM_WarehouseTo_ID(targetLocator.getM_Warehouse_ID());
		move.setAD_Org_ID(receipt.getAD_Org_ID());
        move.setMovementDate(new Timestamp(System.currentTimeMillis()));
		move.setDescription("由来料检验单 " + inspect.getDocumentNo() + "（收货单 " + receipt.getDocumentNo() + "）自动生成");

        move.setDocAction(DocAction.ACTION_Complete);
        move.saveEx();

		// 创建调拨单明细：待检库位 → 目标库位
        MMovementLine moveLine = new MMovementLine(getCtx(), 0, get_TrxName());
		moveLine.setAD_Org_ID(move.getAD_Org_ID());
        moveLine.setM_Movement_ID(move.get_ID());
        moveLine.setM_Locator_ID(reservedLocatorId);       // 来源：待检库位
        moveLine.setM_LocatorTo_ID(intendedLocId);          // 目标：入库行指定的目标库位
        moveLine.setM_Product_ID(line.getM_Product_ID());
        moveLine.setMovementQty(line.getMovementQty());
        moveLine.setC_UOM_ID(line.getC_UOM_ID());
        moveLine.saveEx();

        // 触发完成流程
        MWorkflow.runDocumentActionWorkflow(move, DocAction.ACTION_Complete);
        move.saveEx();

        // 发送通知给收货单创建人
		sendNotification(receipt, inspect, move.getDocumentNo(), "来料检验放行，调拨单 {0} 已完成");
    }

    /**
     * 创建供应商退货授权单（MRMA）
     *
     * 流程：
     *   1. 查找供应商退货授权单据类型（通过 C_DocType_UU）
     *   2. 创建 MRMA 头（关联原收货单）
     *   3. 从收货单明细创建 RMA 明细
     *   4. 触发完成流程（会走审批节点，预期停在处理中状态）
     *   5. 发送通知给收货单创建人
     */
    private void doCreateRMA(MQC_IQCInspect inspect) throws Exception {
        int inOutLineId = inspect.get_ValueAsInt("M_InOutLine_ID");
        MInOutLine line = new MInOutLine(getCtx(), inOutLineId, get_TrxName());
        MInOut receipt = new MInOut(getCtx(), line.getM_InOut_ID(), get_TrxName());

        // 通过单据类型名称查找供应商退货授权单据类型
        int rmaDocTypeId = DB.getSQLValueEx(get_TrxName(),
				"SELECT C_DocType_ID FROM C_DocType WHERE Name=?  AND AD_Client_ID=? AND IsActive='Y'",
				VENDOR_RETURN_RMA_DOC_TYPE_NAME, getAD_Client_ID());
        if (rmaDocTypeId <= 0) {
            throw new Exception("@C_DocType_ID@ @NotFound@ (Name=" + VENDOR_RETURN_RMA_DOC_TYPE_NAME + ")");
        }

		int rmaTypeId = DB.getSQLValueEx(get_TrxName(),
				"SELECT M_RMAType_ID FROM M_RMAType WHERE Name=? AND AD_Client_ID=? AND IsActive='Y'",
				VENDOR_RETURN_RMA_TYPE_NAME,
				getAD_Client_ID());
		if (rmaTypeId <= 0) {
			throw new Exception("@M_RMAType_ID@ @NotFound@ (Name=01_供应商退货)");
		}

        // 创建 RMA 头（Name 为必填字段，格式：供应商名称 + "退货单"）
        String bpName = receipt.getC_BPartner().getName();
        MRMA rma = new MRMA(getCtx(), 0, get_TrxName());
        rma.setC_BPartner_ID(receipt.getC_BPartner_ID());
        rma.setAD_Org_ID(receipt.getAD_Org_ID());
        rma.setM_InOut_ID(receipt.getM_InOut_ID());
        rma.setIsSOTrx(false);
        rma.setName(bpName + "退货单");
        rma.setC_DocType_ID(rmaDocTypeId);
		rma.setM_RMAType_ID(rmaTypeId);

		rma.setHelp("由来料检验单 " + inspect.getDocumentNo() + "（收货单 " + receipt.getDocumentNo() + "）自动生成，判定不合格并退货");

        rma.setDocAction(DocAction.ACTION_Complete);
        rma.saveEx();

        // 从收货单明细创建 RMA 明细
		rma.createLineFrom(inOutLineId, line.getMovementQty(), null);

        // 触发完成流程（供应商退货单会走审批节点，完成后状态可能为 IP（处理中））
        MWorkflow.runDocumentActionWorkflow(rma, DocAction.ACTION_Complete);
        rma.saveEx();

        // 根据最终状态发送不同通知
        String docStatus = rma.getDocStatus();
        if (DocAction.STATUS_Completed.equals(docStatus)) {
			sendNotification(receipt, inspect, rma.getDocumentNo(), "来料检验退货，供应商退货授权 {0} 已创建并完成");
        } else {
            // 预期停在 IP（处理中），需手动审批完成
			sendNotification(receipt, inspect, rma.getDocumentNo(),
					"来料检验退货，供应商退货授权 {0} 已创建（状态：" + docStatus + "），需手动完成");
        }
    }

    /**
     * 查找待检库位类型的库位
     *
     * @param warehouseId 仓库 ID
     * @return 待检库位 ID
     * @throws Exception 找不到待检库位时抛出异常
     */
    private int getReservedLocatorId(int warehouseId) throws Exception {
        String sql = "SELECT l.M_Locator_ID FROM M_Locator l"
                + " JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID)"
                + " WHERE l.M_Warehouse_ID=? AND lt.Name=? AND l.IsActive='Y' AND lt.IsActive='Y'";
        int locatorId = DB.getSQLValueEx(get_TrxName(), sql, warehouseId, LOCATOR_TYPE_INSPECTION);
        if (locatorId <= 0) {
            throw new Exception("@M_Locator_ID@ (" + LOCATOR_TYPE_INSPECTION + ") @NotFound@ (M_Warehouse_ID=" + warehouseId + ")");
        }
        return locatorId;
    }

    /**
     * 发送通知给收货单创建人
     *
     * 通知关联到收货单（M_InOut），用户点击通知时可跳转到收货单详情。
     *
     * @param receipt    收货单
     * @param inspect    检验单
     * @param refDocNo   引用单据号（移库单号或退货单号）
     * @param msgPattern 通知消息模板（支持 {0} 占位符替换为引用单据号）
     */
    private void sendNotification(MInOut receipt, MQC_IQCInspect inspect, String refDocNo, String msgPattern) throws Exception {
        // 获取收货单创建人 ID
        int userId = receipt.getCreatedBy();
        if (userId <= 0) return;

        // 格式化通知消息
        String textMsg = Msg.getMsg(getCtx(), msgPattern, new Object[]{refDocNo});

        // 创建通知记录（关联到收货单，用户点击可跳转）
		MNote note = new MNote(getCtx(), 0, userId, MInOut.Table_ID, receipt.getM_InOut_ID(), receipt.getDocumentNo(), // Reference
				textMsg, // TextMsg
				get_TrxName());
        note.saveEx();
    }
}
