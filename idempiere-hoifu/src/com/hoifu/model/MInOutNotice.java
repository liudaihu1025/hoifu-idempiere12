package com.hoifu.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.DocActionDelegate;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MInOutNotice extends X_M_InOutNotice implements DocAction , DocOptions {

	private static final long serialVersionUID = 1L;

	private DocActionDelegate<MInOutNotice> docActionDelegate;

	public MInOutNotice(Properties ctx, int M_InOutNotice_ID, String trxName) {
		super(ctx, M_InOutNotice_ID, trxName);
		if (M_InOutNotice_ID == 0)
			setInitialDefaults();
		init();
	}

	public MInOutNotice(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		init();
	}

	private void setInitialDefaults() {
		setProcessed(false);
	}

	private void init() {
		docActionDelegate = new DocActionDelegate<>(this);
		docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		// 新增时根据订单ID（C_Order_ID）创建明细
		if (newRecord && getC_Order_ID() > 0 && !hasOverDelivery())
			createNoticeLines();

		// 更新订单ID（C_Order_ID）时重新生成
		if (!newRecord && is_ValueChanged(COLUMNNAME_C_Order_ID)) {
			deleteNoticeLines(); // 先删旧行
			if (!hasOverDelivery())  
	            createNoticeLines(); 
		}
		return true;
	}

	private String doComplete() {
		MInOut shipment = generateShipment();  
	    setM_InOut_ID(shipment.getM_InOut_ID());  
	    setApproved(new Timestamp(System.currentTimeMillis()));
	    setApprovedBy(Env.getAD_User_ID(getCtx()));    
	    saveEx();  
		return null;
	}

	/**  
	 * 根据发货通知单明细生成发货单（草稿状态，不自动完成）。  
	 * 同时将生成的 M_InOutLine_ID 回写到每条通知单明细。  
	 *  
	 * @return 生成的 MInOut 发货单  
	 */  
	private MInOut generateShipment() {
		MOrder order = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());

		int C_DocTypeShipment_ID = DB.getSQLValue(get_TrxName(),
				"SELECT C_DocTypeShipment_ID FROM C_DocType WHERE C_DocType_ID=?", order.getC_DocType_ID());
		if (C_DocTypeShipment_ID <= 0)
			throw new AdempiereException("找不到对应的发货单单据类型，请检查订单单据类型配置");

		MInOut shipment = new MInOut(order, C_DocTypeShipment_ID, getDatePromised());
		shipment.setDocAction(MInOut.DOCACTION_Complete);
		shipment.saveEx();

		String isSOTrx = order.isSOTrx() ? "Y" : "N";

		for (MInOutNoticeLine noticeLine : getLines()) {
			if (noticeLine.getC_OrderLine_ID() <= 0)
				continue;

			MOrderLine orderLine = new MOrderLine(getCtx(), noticeLine.getC_OrderLine_ID(), get_TrxName());
			BigDecimal qty = noticeLine.getQtyEntered();

			// 第一优先：自定义推荐库位函数
			int locatorId = DB.getSQLValue(get_TrxName(), "SELECT get_recommended_locator(?, ?, ?)",
					noticeLine.getM_Product_ID(), order.getM_Warehouse_ID(), isSOTrx);
			// 第二优先：回退到"成品库位"类型下的库位，IsDefault='Y' 优先，再按 PriorityNo 排
			if (locatorId <= 0) {
				locatorId = DB.getSQLValue(get_TrxName(),
						"SELECT l.M_Locator_ID " + "FROM M_Locator l "
								+ "JOIN M_LocatorType lt ON (l.M_LocatorType_ID = lt.M_LocatorType_ID) "
								+ "WHERE l.M_Warehouse_ID = ? " + "  AND lt.Name = '成品库位' " + "  AND l.IsActive = 'Y' "
								+ "ORDER BY CASE WHEN l.IsDefault='Y' THEN 0 ELSE 1 END, l.PriorityNo " + "LIMIT 1",
						order.getM_Warehouse_ID());
			}
			if (locatorId <= 0)
				throw new AdempiereException("产品 " + noticeLine.getM_Product_ID() + " 找不到推荐库位");

			MInOutLine shipmentLine = new MInOutLine(shipment);
			shipmentLine.setOrderLine(orderLine, locatorId, qty);
			shipmentLine.setQty(qty);
			shipmentLine.saveEx();

			noticeLine.setM_InOutLine_ID(shipmentLine.getM_InOutLine_ID());
			noticeLine.saveEx();
		}

		return shipment;
	}
	/**
	 * 根据订单ID创建明细
	 */
	private void createNoticeLines() {
		MOrder order = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());
		MOrderLine[] orderLines = order.getLines(true, null);
		int lineNo = 10;
		for (MOrderLine ol : orderLines) {
			if (ol.getM_Product_ID() == 0)
				continue;

			BigDecimal qtyToNotice = ol.getQtyOrdered().subtract(ol.getQtyDelivered());
			if (qtyToNotice.compareTo(BigDecimal.ZERO) <= 0)
				continue;

			MInOutNoticeLine noticeLine = new MInOutNoticeLine(getCtx(), 0, get_TrxName());
			noticeLine.setAD_Org_ID(getAD_Org_ID());
			noticeLine.setM_InOutNotice_ID(getM_InOutNotice_ID());
			noticeLine.setLine(lineNo);
			noticeLine.setC_OrderLine_ID(ol.getC_OrderLine_ID());
			noticeLine.setM_Product_ID(ol.getM_Product_ID());
			noticeLine.setC_UOM_ID(ol.getC_UOM_ID());
			noticeLine.setQtyEntered(qtyToNotice);
			noticeLine.setQtyDelivered(BigDecimal.ZERO);
			noticeLine.saveEx();
			lineNo += 10;
		}
	}

	public MInOutNoticeLine[] getLines() {
		List<MInOutNoticeLine> list = new Query(getCtx(), MInOutNoticeLine.Table_Name, "M_InOutNotice_ID = ?",
				get_TrxName()).setParameters(getM_InOutNotice_ID()).setOrderBy("Line").list();
		return list.toArray(new MInOutNoticeLine[0]);
	}

	private void deleteNoticeLines() {
		for (MInOutNoticeLine line : getLines())
			line.deleteEx(true);
	}
	
	/**
	 * 是否超发
	 */
	private boolean hasOverDelivery() {  
	    MOrder order = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());  
	    for (MOrderLine ol : order.getLines(true, null)) {  
	        if (ol.getM_Product_ID() == 0) continue;  
	        if (ol.getQtyDelivered().compareTo(ol.getQtyOrdered()) >= 0)  
	            return true;  
	    }  
	    return false;  
	}
	
	// ── DocAction 接口方法全部委托 ────────────────────────────

	@Override
	public boolean processIt(String action) throws Exception {
		return docActionDelegate.processIt(action);
	}

	@Override
	public boolean unlockIt() {
		return docActionDelegate.unlockIt();
	}

	@Override
	public boolean invalidateIt() {
		return docActionDelegate.invalidateIt();
	}

	@Override
	public String prepareIt() {
		return docActionDelegate.prepareIt();
	}

	@Override
	public boolean approveIt() {
		return docActionDelegate.approveIt();
	}

	@Override
	public boolean rejectIt() {
		return docActionDelegate.rejectIt();
	}

	@Override
	public String completeIt() {
		return docActionDelegate.completeIt();
	}

	@Override
	public boolean voidIt() {
		return docActionDelegate.voidIt();
	}

	@Override
	public boolean closeIt() {
		return docActionDelegate.closeIt();
	}

	@Override
	public boolean reverseCorrectIt() {
		return docActionDelegate.reverseCorrectIt();
	}

	@Override
	public boolean reverseAccrualIt() {
		return docActionDelegate.reverseAccrualIt();
	}

	@Override
	public boolean reActivateIt() {
		return false;
	}

	@Override
	public File createPDF() {
		return docActionDelegate.createPDF();
	}

	@Override
	public String getProcessMsg() {
		return docActionDelegate.getProcessMsg();
	}

	@Override
	public int getC_Currency_ID() {
		return docActionDelegate.getC_Currency_ID();
	}

	@Override
	public String getDocAction() {
		return docActionDelegate.getDocAction();
	}

	@Override
	public void setDocStatus(String s) {
		docActionDelegate.setDocStatus(s);
	}

	@Override
	public String getDocStatus() {
		return docActionDelegate.getDocStatus();
	}

	@Override
	public String getSummary() {
		return getDocumentNo();
	}

	@Override
	public String getDocumentNo() {
		return get_ValueAsString(COLUMNNAME_DocumentNo);
	}

	@Override
	public String getDocumentInfo() {
		return getDocumentNo();
	}

	@Override
	public int getDoc_User_ID() {
		return getSalesRep_ID();
	}

	@Override
	public BigDecimal getApprovalAmt() {
		return null;
	}

	@Override
	public int customizeValidActions(String docStatus, Object processing, String orderType, String isSOTrx,
			int AD_Table_ID, String[] docAction, String[] options, int index) {
		// 从 options[0..index) 中移除 ACTION_Void
		int newIndex = 0;
		for (int i = 0; i < index; i++) {
			if (!DocumentEngine.ACTION_Void.equals(options[i])) {
				options[newIndex++] = options[i];
			}
		}
		return newIndex;
	}
}