package com.hoifu.info;

import java.io.Serializable;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CInvoiceInfoWindow extends InfoWindow {

	// 完整构造函数 - 10个参数
	public CInvoiceInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override
	protected void enableButtons() {
		super.enableButtons();

		int selectedCount = contentPanel.getSelectedCount();

		for (Button btProcess : btProcessList) {
			Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
			if (processId != null) {
				MProcess process = MProcess.get(Env.getCtx(), processId);
				if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.CreatePaymentByInvoicesProcess")) {

					boolean enabled = selectedCount > 0 && isSameBPartner();

					btProcess.setEnabled(enabled);
					btProcess.setTooltiptext("仅相同业务伙伴时可合并。");
				}
			}
		}
	}

	private boolean isSameBPartner() {
		List<Serializable> invoiceIds = getSelectedRowKeys();

		if (invoiceIds == null || invoiceIds.isEmpty()) {
			return false;
		}
		// 只有一张发票时，直接通过
		if (invoiceIds.size() == 1) {
			return true;
		}

		// 构建 IN 子句（ID 均为整数，无 SQL 注入风险）
		StringBuilder inClause = new StringBuilder();
		for (Serializable id : invoiceIds) {
			if (inClause.length() > 0)
				inClause.append(",");
			inClause.append(id.toString());
		}

		String sql = "SELECT COUNT(DISTINCT C_BPartner_ID) FROM C_Invoice WHERE C_Invoice_ID IN (" + inClause + ")";

		int distinctCount = DB.getSQLValue(null, sql);
		return distinctCount == 1;
	}

}
