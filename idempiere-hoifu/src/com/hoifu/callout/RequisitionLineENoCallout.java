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
 * 从 C_Order（外协单，Subcontract_IDs）或 PP_Order（工单，PP_Order_IDs）  
 * 自动带出对应单号到 ENo 字段。  
 *  
 * IsSubcontracting = Y  -> 查 C_Order，用 Subcontract_IDs  
 * IsSubcontracting = N  -> 查 PP_Order，用 PP_Order_IDs  
 */  
@Callout(tableName = "M_RequisitionLine",  
		columnName = { "IsSubcontracting", "Subcontract_IDs", "PP_Order_IDs" })  
public class RequisitionLineENoCallout implements IColumnCallout {  
  
	private static final String COLUMN_ENO = "ENo";  
	private static final String COLUMN_IS_SUBCONTRACTING = "IsSubcontracting";  
	private static final String COLUMN_SUBCONTRACT_IDS = "Subcontract_IDs";  
	private static final String COLUMN_PP_ORDER_IDS = "PP_Order_IDs";  
  
	@Override  
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
  
		boolean isSubcontracting = resolveIsSubcontracting(mTab, mField, value);  
		String idsColumnName = isSubcontracting ? COLUMN_SUBCONTRACT_IDS : COLUMN_PP_ORDER_IDS;  
		String tableName = isSubcontracting ? "C_Order" : "PP_Order";  
  
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
  
		Set<String> documentNoSet = RequisitionLinePOCreateService.queryDocumentNos(tableName, idSet, null);  
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