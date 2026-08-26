package com.hoifu.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.Msg;

import com.hoifu.service.RequisitionLinePOCreateService;

/**
 * 1.用户勾选需要创建的申购单明细； 2.点击“创建采购订单”按钮，弹出一个对话框，让用户选择供应商、用途，然后提交，创建采购单
 * 3.相同商品合并，采购单按仓库分组
 */
@org.adempiere.base.annotation.Process
public class RequisitionDetailPOCreate extends SvrProcess {

	// 采购订单单据类型
	private static final String ORDER_DOCTYPE_UU = "1b5b0262-ed61-4f5d-a844-5a88da658491";

	private int p_C_BPartner_ID = 0;
	private String p_Purpose = null;
	private List<Integer> selectedLineIds = new ArrayList<>();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("Purpose"))
				p_Purpose = para[i].getParameterAsString();
		}
	}

	@Override
	protected String doIt() throws Exception {
		// 验证参数
		if (p_C_BPartner_ID == 0) {
			throw new AdempiereUserError("@FillMandatory@ @C_BPartner_ID@");
		}

		// 从T_Selection表获取选中的申购单明细
		selectedLineIds = RequisitionLinePOCreateService.loadSelectedRequisitionLines(getAD_PInstance_ID(),
				get_TrxName());
		if (selectedLineIds.isEmpty()) {
			throw new AdempiereUserError("@NoSelection@");
		}

		int docTypeId = RequisitionLinePOCreateService.resolveOrderDocType(getCtx(), ORDER_DOCTYPE_UU, get_TrxName());

		// 获取供应商信息
		MBPartner bpartner = MBPartner.get(getCtx(), p_C_BPartner_ID);

		// 过滤：只处理未绑定过采购订单行的明细（RequisitionDetailPOCreate 特有逻辑）
		List<MRequisitionLine> filteredLines = new ArrayList<>();
		for (Integer lineId : selectedLineIds) {
			MRequisitionLine reqLine = new MRequisitionLine(getCtx(), lineId, get_TrxName());
			if (reqLine.getC_OrderLine_ID() != 0) {
				log.warning("跳过已处理的明细行: " + lineId);
				continue;
			}
			filteredLines.add(reqLine);
		}

		// 按仓库分组申购单明细
		Map<Integer, List<MRequisitionLine>> warehouseGroups = RequisitionLinePOCreateService
				.groupByWarehouse(filteredLines);

		// 为每个仓库创建一个采购订单
		List<String> createdOrders = new ArrayList<>();

		for (Map.Entry<Integer, List<MRequisitionLine>> entry : warehouseGroups.entrySet()) {
			int warehouseId = entry.getKey();
			List<MRequisitionLine> lines = entry.getValue();

			RequisitionLinePOCreateService.CreatedOrderResult result = RequisitionLinePOCreateService
					.createDraftOrderForWarehouse(getCtx(), get_TrxName(), docTypeId, bpartner, warehouseId,
							getAD_User_ID(), p_Purpose, lines);

			// 记录日志 - 保持草稿状态，不完成订单
			addBufferLog(0, null, result.grandTotal,
					Msg.parseTranslation(getCtx(), "@Created@ @C_Order_ID@ ") + result.documentNo + " (草稿)",
					MOrder.Table_ID, result.orderId);

			createdOrders.add(result.documentNo);
		}

		return "@Created@ " + String.join(", ", createdOrders) + " (草稿)";
	}
}