package com.hoifu.window;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.action.IAction;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MRMA;
import org.compiere.model.MTable;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.osgi.service.component.annotations.Component;
import org.zkoss.zul.Toolbarbutton;

@Component(name = "com.hoifu.window.CreateRMAAction", service = { IAction.class })
public class CreateRMAAction implements IAction {

	/** 供应商退货授权单 C_DocType UU */
	private static final String VENDOR_RETURN_RMA_UU = "57d0f118-7517-4545-8a4d-ea29afcfe513";
	/** 客户退货授权单 C_DocType UU */
	private static final String CUSTOMER_RETURN_RMA_UU = "3a45acb0-d62d-4195-874e-6cd5e190a1c8";

	@Override
	public String getIconSclass() {
		return "z-icon-Request";
	}

	@Override
	public void decorate(Toolbarbutton toolbarButton) {
		toolbarButton.setLabel("退货");
	}

	@Override
	public void execute(Object context) {
		if (!(context instanceof ADWindow))
			return;
		ADWindow adWindow = (ADWindow) context;
		IADTabpanel tabPanel = adWindow.getADWindowContent().getADTab().getSelectedTabpanel();
		if (tabPanel == null)
			return;

		GridTab gridTab = tabPanel.getGridTab();
		int inoutId = gridTab.getRecord_ID();
		if (inoutId <= 0)
			return;

		MInOut inout = new MInOut(Env.getCtx(), inoutId, null);
		if (!DocAction.STATUS_Completed.equals(inout.getDocStatus())) {
			Dialog.warn(gridTab.getWindowNo(), "单据未完成，无法创建退货单");
			return;
		}

		boolean isSO = inout.isSOTrx();
		String bpName = MBPartner.get(Env.getCtx(), inout.getC_BPartner_ID()).getName();
		String label = isSO ? "确认创建客户退货授权单？" : "确认创建供应商退货授权单？";

		int windowNo = gridTab.getWindowNo();
		Dialog.ask(windowNo, label, null, new Callback<Boolean>() {
			@Override
			public void onCallback(Boolean confirmed) {
				if (Boolean.TRUE.equals(confirmed))
					createRMAAndZoom(inout, isSO, bpName, windowNo);
			}
		});
	}

	private void createRMAAndZoom(MInOut inout, boolean isSO, String bpName, int windowNo) {
		Trx trx = Trx.get(Trx.createTrxName("CreateRMA"), true);
		try {
			// 提前计算每行可退数量，全部为 0 则直接拒绝
			MInOutLine[] lines = inout.getLines(false);
			Map<MInOutLine, BigDecimal> availableQtyMap = new LinkedHashMap<>();
			for (MInOutLine line : lines) {
				BigDecimal alreadyReturned = DB.getSQLValueBD(trx.getTrxName(),
						"SELECT COALESCE(SUM(rl.Qty), 0) " + "FROM M_RMALine rl "
								+ "JOIN M_RMA r ON (r.M_RMA_ID = rl.M_RMA_ID) " + "WHERE rl.M_InOutLine_ID = ? "
								+ "  AND r.Processed = 'Y' " + "  AND r.DocStatus IN ('CO','CL')",
						line.getM_InOutLine_ID());
				if (alreadyReturned == null)
					alreadyReturned = Env.ZERO;
				BigDecimal availableQty = line.getMovementQty().subtract(alreadyReturned);
				availableQtyMap.put(line, availableQty);
			}

			boolean hasAnyAvailable = availableQtyMap.values().stream().anyMatch(qty -> qty.signum() > 0);
			if (!hasAnyAvailable) {
				trx.close();
				Dialog.warn(windowNo, "该单所有明细物料已全部授权退货，无法创建退货授权单");
				return;
			}

			// 查询单据类型
			String docTypeUU = isSO ? CUSTOMER_RETURN_RMA_UU : VENDOR_RETURN_RMA_UU;
			int docTypeId = DB.getSQLValueEx(trx.getTrxName(),
					"SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU=?", docTypeUU);
			if (docTypeId <= 0)
				throw new AdempiereException("未找到退货授权单据类型，UU=" + docTypeUU);

			// 创建 RMA 头
			MRMA rma = new MRMA(Env.getCtx(), 0, trx.getTrxName());
			rma.setM_InOut_ID(inout.getM_InOut_ID());
			rma.setIsSOTrx(isSO);
			rma.setName(bpName + "退货单");
			rma.setC_DocType_ID(docTypeId);
			rma.setAD_Org_ID(inout.getAD_Org_ID());
			rma.saveEx();

			// 只为有可退数量的行创建 RMA 行
			for (Map.Entry<MInOutLine, BigDecimal> entry : availableQtyMap.entrySet()) {
				if (entry.getValue().signum() <= 0)
					continue;
				MInOutLine line = entry.getKey();
				rma.createLineFrom(line.getM_InOutLine_ID(), entry.getValue(), line.getDescription());
			}

			trx.commit();
			AEnv.zoom(MTable.getTable_ID(MRMA.Table_Name), rma.getM_RMA_ID());

		} catch (Exception e) {
			trx.rollback();
			Dialog.warn(windowNo, "创建退货授权单失败：" + e.getMessage());
		} finally {
			trx.close();
		}
	}
}