package com.hoifu.window;

import java.math.BigDecimal;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.action.IAction;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.window.Dialog;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTable;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.osgi.service.component.annotations.Component;
import org.zkoss.zul.Toolbarbutton;

import com.hoifu.model.MInOutNotice;
import com.hoifu.model.MInOutNoticeLine;

@Component(name = "com.hoifu.window.NotifyShipmentAction", service = { IAction.class })
public class NotifyShipmentAction implements IAction {

	/**
	 * 发货通知单UUID
	 */
	final String OUT_NOTICE_UUID = "69cefa28-a09d-4d5c-b37e-ef0928c67400";

	@Override  
	public String getIconSclass() {  
	    return "z-icon-Request";  // 或其他图标名  
	}
	@Override  
	public void decorate(Toolbarbutton toolbarButton) {  
	    toolbarButton.setLabel("通知发货");  
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
		int orderId = gridTab.getRecord_ID();
		if (orderId <= 0)
			return;

		MOrder order = new MOrder(Env.getCtx(), orderId, null);
		if (!DocAction.ACTION_Complete.equals(order.getDocStatus())) {
			Dialog.warn(gridTab.getWindowNo(), null, "订单未完成，无法创建发货通知单");
			return;
		}

		// 采购订单不处理
		if (!order.isSOTrx())
			return;
		
		// 检查是否有超发
		boolean hasOverDelivery = checkOverDelivery(order);

		if (hasOverDelivery) {
			handleOverDelivery(gridTab, order);
		} else {
			handleNormalDelivery(gridTab, order);
		}
	}

	/** 检查是否存在超发（QtyDelivered > QtyOrdered）的订单行 */
	private boolean checkOverDelivery(MOrder order) {
		for (MOrderLine ol : order.getLines(true, null)) {
			if (ol.getM_Product_ID() == 0)
				continue;
			if (ol.getQtyDelivered().compareTo(ol.getQtyOrdered()) >= 0)
				return true;
		}
		return false;
	}

	/** 无超发：弹出简单确认，确认后直接生成 */
	private void handleNormalDelivery(GridTab gridTab, MOrder order) {
		FDialog.ask(gridTab.getWindowNo(), null, "确认通知发货？", null, new Callback<Boolean>() {
			@Override
			public void onCallback(Boolean confirmed) {
				if (Boolean.TRUE.equals(confirmed)) {
					createNoticeAndZoom(order, null);
				}
			}
		});
	}

	/** 有超发：弹出带输入框的确认，要求填写超发原因 */
	private void handleOverDelivery(GridTab gridTab, MOrder order) {  
	    OverDeliveryReasonDialog dialog = new OverDeliveryReasonDialog(reason -> {  
	        if (reason != null) {  
	            createNoticeAndZoom(order, reason);  
	        }  
	        // reason == null 表示用户点了取消，什么都不做  
	    });  
	    AEnv.showCenterScreen(dialog);  
	}

	/** 创建 MInOutNotice 并跳转 */
	private void createNoticeAndZoom(MOrder order, String overDeliveryReason) {
		Trx trx = Trx.get(Trx.createTrxName("NotifyShipment"), true);
		try {
			MInOutNotice notice = new MInOutNotice(Env.getCtx(), 0, trx.getTrxName());
			notice.setAD_Org_ID(order.getAD_Org_ID());
			notice.setC_Order_ID(order.getC_Order_ID());
			notice.setC_BPartner_ID(order.getC_BPartner_ID());
			notice.setDateTrx(new java.sql.Timestamp(System.currentTimeMillis()));
			notice.setDatePromised(order.getDatePromised());
			notice.setSalesRep_ID(Env.getAD_User_ID(Env.getCtx()));

			// 通过 UU 查询 C_DocType_ID
			int docTypeId = DB.getSQLValueEx(trx.getTrxName(),
					"SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU=?", OUT_NOTICE_UUID);
			if (docTypeId <= 0)
				throw new AdempiereException("未找到对应的单据类型");
			notice.setC_DocType_ID(docTypeId);
			notice.saveEx();

			// afterSave 会在非超发时自动生成明细
			// 只有超发情况才由 Action 自己生成（通知数量=0，带超发原因）
			if (overDeliveryReason != null) {
				createNoticeLines(notice, order, true, overDeliveryReason, trx.getTrxName());
			}

			trx.commit();

			// 跳转到新建的发货通知单
			int tableId = MTable.getTable_ID(MInOutNotice.Table_Name);
			AEnv.zoom(tableId, notice.getM_InOutNotice_ID());

		} catch (Exception e) {
			trx.rollback();
			Dialog.warn(0, null, "创建发货通知单失败：" + e.getMessage());
		} finally {
			trx.close();
		}
	}

	private void createNoticeLines(MInOutNotice notice, MOrder order, boolean isOverDelivery, String overDeliveryReason,
			String trxName) {
		if (!isOverDelivery) {
			return;
		}
		int lineNo = 10;
		for (MOrderLine ol : order.getLines(true, null)) {
			if (ol.getM_Product_ID() == 0)
				continue;

			BigDecimal qtyToNotice;
			// 超发情况：通知数量默认为 0，用户手动编辑
			qtyToNotice = BigDecimal.ZERO;
			MInOutNoticeLine line = new MInOutNoticeLine(Env.getCtx(), 0, trxName);
			line.setAD_Org_ID(notice.getAD_Org_ID());
			line.setM_InOutNotice_ID(notice.getM_InOutNotice_ID());
			line.setLine(lineNo);
			line.setC_OrderLine_ID(ol.getC_OrderLine_ID());
			line.setM_Product_ID(ol.getM_Product_ID());
			line.setC_UOM_ID(ol.getC_UOM_ID());
			line.setQtyEntered(qtyToNotice);
			line.setQtyDelivered(BigDecimal.ZERO);
			line.setDescription(overDeliveryReason); // 超发原因写入备注
			line.saveEx();
			lineNo += 10;
		}
	}
}