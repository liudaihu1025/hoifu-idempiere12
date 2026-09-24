package com.hoifu.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.doc.BeforeVoid;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

/**
 * 委外订单作废前校验：检查是否关联了未反冲的委外收货单（M_InOut，单据类型=委外收货单） 若存在任意一条关联记录且 DocStatus 不是
 * 已反冲(RE)，则阻止作废
 */
@EventTopicDelegate
@ModelEventTopic(modelClass = MOrder.class)
public class SubcontractOrderVoidReceiptCheck extends ModelEventDelegate<MOrder> {

	// 委外收货单 单据类型 UUID
	private static final String RECEIPT_DOCTYPE_UU = "503a5e26-3cb6-4525-94ae-ada837bc13ce";

	// 委外订单 单据类型 UUID
	private static final String SUBCONTRACT_DOCTYPE_UU = "5ae935d6-5237-4d93-ac5b-4967cf9be287";

	public SubcontractOrderVoidReceiptCheck(MOrder po, Event event) {
		super(po, event);
	}

	@BeforeVoid
	public void onBeforeVoid() {
		MOrder order = getModel();

		// 只对委外订单生效，其它采购/销售订单直接放行
		MDocType docType = MDocType.get(order.getCtx(), order.getC_DocType_ID());
		if (docType == null || !SUBCONTRACT_DOCTYPE_UU.equals(docType.getC_DocType_UU())) {
			return;
		}

		// 查找委外收货单的单据类型 ID
		MDocType receiptDocType = new MDocType(Env.getCtx(), RECEIPT_DOCTYPE_UU, order.get_TrxName());
		if (receiptDocType.get_ID() == 0) {
			// 找不到委外收货单单据类型配置，视为无法校验，放行（也可改为直接抛错阻止，视业务需要而定）
			return;
		}

		// 检查关联的委外收货单：DocStatus 必须全部为 已反冲(RE)
		int receiptCount = new Query(Env.getCtx(), MInOut.Table_Name,
				"C_Order_ID=? AND C_DocType_ID=? AND DocStatus<>?",
				order.get_TrxName()).setParameters(order.get_ID(), receiptDocType.get_ID(), "RE")
				.setOnlyActiveRecords(true).count();

		if (receiptCount > 0) {
			throw new AdempiereException("委外订单已关联收货单，作废失败");
		}
	}
}
