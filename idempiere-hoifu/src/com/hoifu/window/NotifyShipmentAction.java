package com.hoifu.window;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
import org.compiere.model.MProduct;
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
		return "z-icon-Request";
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

		// 第一步：按行校验"已通知发货数量"，过滤超限行
		List<String> overLimitProducts = new ArrayList<>();
		List<MOrderLine> notifiableLines = filterNotifiableLines(order, overLimitProducts);

		if (notifiableLines.isEmpty()) {
			// 全部行超限，中断整体操作
			Dialog.warn(gridTab.getWindowNo(), null,
					"产品发货通知数量已大于订单数量！(" + String.join("、", overLimitProducts) + ")");
			return;
		}

		if (!overLimitProducts.isEmpty()) {
			// 部分行超限：汇总提示，但不中断，继续处理剩余行
			Dialog.warn(gridTab.getWindowNo(), null,
					"以下产品发货通知数量已大于订单数量，已跳过：" + String.join("、", overLimitProducts));
		}

		// 第二步：仅对未超限的行做超发判断
		boolean hasOverDelivery = checkOverDelivery(notifiableLines);

		if (hasOverDelivery) {
			handleOverDelivery(gridTab, order, notifiableLines);
		} else {
			handleNormalDelivery(gridTab, order, notifiableLines);
		}
	}

	/**
	 * 统计某订单行已通知发货数量：IP/CO 状态的 M_InOutNotice 关联的 M_InOutNoticeLine.QtyEntered 之和
	 */
	private BigDecimal getNotifiedQty(int orderLineId) {
		BigDecimal sum = DB.getSQLValueBD(null,
				"SELECT COALESCE(SUM(l.QtyEntered),0) "
						+ "FROM M_InOutNoticeLine l "
						+ "JOIN M_InOutNotice n ON n.M_InOutNotice_ID = l.M_InOutNotice_ID "
						+ "WHERE l.C_OrderLine_ID = ? "
						+ "AND n.DocStatus IN ('IP','CO')",
				orderLineId);
		return sum == null ? BigDecimal.ZERO : sum;
	}

	/**
	 * 按行校验：过滤出未超限的可通知行，同时收集超限行的产品名用于汇总提示
	 */
	private List<MOrderLine> filterNotifiableLines(MOrder order, List<String> overLimitProductNames) {
		List<MOrderLine> notifiable = new ArrayList<>();

		for (MOrderLine ol : order.getLines(true, null)) {
			if (ol.getM_Product_ID() == 0)
				continue;
			BigDecimal notifiedQty = getNotifiedQty(ol.getC_OrderLine_ID());
			if (notifiedQty.compareTo(ol.getQtyOrdered()) >= 0) {
				MProduct product = MProduct.get(ol.getCtx(), ol.getM_Product_ID());
				overLimitProductNames.add(product != null ? product.getName() : String.valueOf(ol.getM_Product_ID()));
			} else {
				notifiable.add(ol);
			}
		}

		return notifiable;
	}

	/** 检查过滤后的行集合中是否存在超发（QtyDelivered > QtyOrdered）的订单行 */
	private boolean checkOverDelivery(List<MOrderLine> lines) {
		for (MOrderLine ol : lines) {
			if (ol.getQtyDelivered().compareTo(ol.getQtyOrdered()) >= 0)
				return true;
		}
		return false;
	}

	/** 无超发：弹出简单确认，确认后直接生成 */
	private void handleNormalDelivery(GridTab gridTab, MOrder order, List<MOrderLine> lines) {
		FDialog.ask(gridTab.getWindowNo(), null, "确认通知发货？", null, new Callback<Boolean>() {
			@Override
			public void onCallback(Boolean confirmed) {
				if (Boolean.TRUE.equals(confirmed)) {
					createNoticeAndZoom(order, lines, null);
				}
			}
		});
	}

	/** 有超发：弹出带输入框的确认，要求填写超发原因 */
	private void handleOverDelivery(GridTab gridTab, MOrder order, List<MOrderLine> lines) {
		OverDeliveryReasonDialog dialog = new OverDeliveryReasonDialog(reason -> {
			if (reason != null) {
				createNoticeAndZoom(order, lines, reason);
			}
		});
		AEnv.showCenterScreen(dialog);
	}

	/** 创建 MInOutNotice 并跳转 */
	private void createNoticeAndZoom(MOrder order, List<MOrderLine> lines, String overDeliveryReason) {
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

			// 始终抑制 afterSave 自动生成明细，统一由本方法根据过滤后的行集合生成
			notice.setSuppressAutoLines(true);
			notice.saveEx();

			// 根据过滤后的行集合生成明细
			createNoticeLines(notice, lines, overDeliveryReason, trx.getTrxName());

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

	/**
	 * 根据过滤后的行集合创建通知单明细。
	 * 超发情况：通知数量默认为 0，用户手动编辑，超发原因写入备注。
	 * 非超发情况：通知数量 = QtyOrdered - QtyDelivered。
	 */
	private void createNoticeLines(MInOutNotice notice, List<MOrderLine> lines, String overDeliveryReason,
			String trxName) {
		int lineNo = 10;
		for (MOrderLine ol : lines) {
			BigDecimal qtyToNotice;
			if (overDeliveryReason != null) {
				// 超发情况：通知数量默认为 0，用户手动编辑
				qtyToNotice = BigDecimal.ZERO;
			} else {
				// 非超发情况：可通知数量 = 订购数量 - 已发货数量
				qtyToNotice = ol.getQtyOrdered().subtract(ol.getQtyDelivered());
				if (qtyToNotice.compareTo(BigDecimal.ZERO) <= 0)
					continue;
			}

			MInOutNoticeLine line = new MInOutNoticeLine(Env.getCtx(), 0, trxName);
			line.setAD_Org_ID(notice.getAD_Org_ID());
			line.setM_InOutNotice_ID(notice.getM_InOutNotice_ID());
			line.setLine(lineNo);
			line.setC_OrderLine_ID(ol.getC_OrderLine_ID());
			line.setM_Product_ID(ol.getM_Product_ID());
			line.setC_UOM_ID(ol.getC_UOM_ID());
			line.setQtyEntered(qtyToNotice);
			line.setQtyDelivered(BigDecimal.ZERO);
			if (overDeliveryReason != null) {
				line.setDescription(overDeliveryReason);
			}
			line.saveEx();
			lineNo += 10;
		}
	}
}
