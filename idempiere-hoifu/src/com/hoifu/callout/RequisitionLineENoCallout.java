package com.hoifu.callout;

import java.util.Set;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Util;

import com.hoifu.service.RequisitionLinePOCreateService;

import java.util.Properties;

/**
 * 申购明细(M_RequisitionLine) - 根据 IsSubcontracting 标志，
 * 从 C_OrderLine（委外采购订单行，SubcontractLine_IDs，优先经 Ref_OrderLine_ID
 * 关联的 PP_Order.DocumentNo，兜底 C_Order.DocumentNo）或
 * PP_Order（工单，PP_Order_IDs）自动带出对应单号到 ENo 字段。
 *
 * IsSubcontracting = Y  -> 用 SubcontractLine_IDs，优先取 PP_Order 单号，兜底取 C_Order 单号
 * IsSubcontracting = N  -> 查 PP_Order，用 PP_Order_IDs
 */
@Callout(tableName = "M_RequisitionLine",
		columnName = { "IsSubcontracting", "SubcontractLine_IDs", "PP_Order_IDs" })
public class RequisitionLineENoCallout implements IColumnCallout {

	private static final String COLUMN_ENO = "ENo";
	private static final String COLUMN_IS_SUBCONTRACTING = "IsSubcontracting";
	private static final String COLUMN_SUBCONTRACTLINE_IDS = "SubcontractLine_IDs";
	private static final String COLUMN_PP_ORDER_IDS = "PP_Order_IDs";

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		boolean isSubcontracting = resolveIsSubcontracting(mTab, mField, value);
		String idsColumnName = isSubcontracting ? COLUMN_SUBCONTRACTLINE_IDS : COLUMN_PP_ORDER_IDS;

		String idsStr = resolveIdsString(mTab, mField, value, idsColumnName);
		if (Util.isEmpty(idsStr, true)) {
			mTab.setValue(COLUMN_ENO, null);
			return "";
		}

		// 复用 RequisitionLinePOCreateService 的公共工具方法
		Set<Integer> idSet = RequisitionLinePOCreateService.parseIdList(idsStr);
		if (idSet.isEmpty()) {
			mTab.setValue(COLUMN_ENO, null);
			return "";
		}

		Set<String> documentNoSet = isSubcontracting
				? RequisitionLinePOCreateService.resolveSubcontractDocumentNos(idSet, null)
				: RequisitionLinePOCreateService.queryDocumentNos("PP_Order", idSet, null);

		mTab.setValue(COLUMN_ENO, documentNoSet.isEmpty() ? null : String.join(" | ", documentNoSet));

		return "";
	}

	private boolean resolveIsSubcontracting(GridTab mTab, GridField mField, Object value) {
		Object raw = COLUMN_IS_SUBCONTRACTING.equals(mField.getColumnName())
				? value
				: mTab.getValue(COLUMN_IS_SUBCONTRACTING);
		return "Y".equals(raw) || Boolean.TRUE.equals(raw);
	}

	private String resolveIdsString(GridTab mTab, GridField mField, Object value, String idsColumnName) {
		Object raw = idsColumnName.equals(mField.getColumnName()) ? value : mTab.getValue(idsColumnName);
		return raw == null ? null : raw.toString();
	}
}