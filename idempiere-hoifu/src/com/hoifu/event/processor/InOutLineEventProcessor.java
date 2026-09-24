package com.hoifu.event.processor;

import java.math.BigDecimal;

import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;

public class InOutLineEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(InOutLineEventProcessor.class);

	// 入库单的UUID
	private static final String RECEIPT_DOCTYPE_UU = "4a2110a7-04b6-4369-b9db-dc32c0efbe01";

	private final IIQCService iqcService;
	private final IOQCService oqcService;
	private final IRQCService rqcService;

	public InOutLineEventProcessor(IIQCService iqcService, IOQCService oqcService, IRQCService rqcService) {
		this.iqcService = iqcService;
		this.oqcService = oqcService;
		this.rqcService = rqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOutLine;

	}

	@Override
	public void process(PO po, String topic) {
		MInOutLine line = (MInOutLine) po;
		// 保存前校验：收货数量不超过采购订单可收货数量
		if (IEventTopics.PO_BEFORE_NEW.equals(topic) || IEventTopics.PO_BEFORE_CHANGE.equals(topic)) {
			if (isReceiptDocType(line)) {
				checkOrderedQtyLimit(line);
			}

		}
		changeValueByProduct(line, topic);
		updateHeaderWeight(line , topic);
		if (IEventTopics.PO_AFTER_NEW.equals(topic)) {
			handleQC(line); // 保存后逻辑
		}
	}

	// ── QC 逻辑 ──────────────────────────────────────────────────────────────
	private void handleQC(MInOutLine line) {
		if (!MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false))
			return;
		MInOut parent = (MInOut) line.getParent();
		if (rqcService.isReturnDocument(parent))
			rqcService.createFromLine(parent, line);
		else if (!parent.isSOTrx())
			iqcService.createFromReceiptLine(line);
		else
			oqcService.createFromShipmentLine(line);
	}

	// ── 收货数量不超过采购订单数量校验 ──────────────────────────────────────
	/**
	 * 校验收货数量是否超过关联采购订单行的可收货数量（QtyOrdered - QtyDelivered - 其他未过账草稿累计）。
	 * 仅对采购方向、关联了 C_OrderLine_ID、MovementQty > 0 的行生效。
	 * 退货（MovementQty <= 0）、冲销单跳过校验（系统会自动释放/扣减额度）。
	 */
	private void checkOrderedQtyLimit(MInOutLine line) {
		MInOut inout = (MInOut) line.getParent();

		// 冲销单跳过（冲销行本身是系统生成的负数行，不需要再校验）
		if (inout.isReversal())
			return;

		String docStatus = inout.getDocStatus();
		if (MInOut.DOCSTATUS_Completed.equals(docStatus) || MInOut.DOCSTATUS_Closed.equals(docStatus))
			return;

		// 仅对采购方向（非销售）执行校验
		if (inout.isSOTrx())
			return;

		// 未关联采购订单行则跳过（非采购订单来源的收货，如其他入库单）
		int c_OrderLine_ID = line.getC_OrderLine_ID();
		if (c_OrderLine_ID <= 0)
			return;

		// 退货/负数行跳过：负数只会释放额度，不需要拦截
		BigDecimal movementQty = line.getMovementQty();
		if (movementQty == null || movementQty.signum() <= 0)
			return;

		MOrderLine orderLine = new MOrderLine(line.getCtx(), c_OrderLine_ID, line.get_TrxName());
		BigDecimal qtyOrdered = orderLine.getQtyOrdered();
		if (qtyOrdered == null || qtyOrdered.signum() <= 0)
			return;

		// 已过账部分：C_OrderLine.QtyDelivered 由 MMatchPO 在过账/冲销时自动维护，
		// 冲销单完成后会自动扣减回去，这里直接读取即可，不需要额外处理冲销逻辑
		BigDecimal qtyDelivered = orderLine.getQtyDelivered();
		if (qtyDelivered == null)
			qtyDelivered = Env.ZERO;

		// 草稿部分：同一 C_OrderLine_ID 下，其他"尚未过账完成"的收货单明细累计数量
		// （已完成的单据数量已经体现在 QtyDelivered 里，不能重复统计；
		//  已冲销(RE)/作废(VO)的单据不应计入占用）
		int currentLine_ID = line.getM_InOutLine_ID();
		StringBuilder sql = new StringBuilder(
				"SELECT COALESCE(SUM(iol.MovementQty), 0) "
						+ "FROM M_InOutLine iol "
						+ "INNER JOIN M_InOut io ON iol.M_InOut_ID = io.M_InOut_ID "
						+ "WHERE iol.C_OrderLine_ID = ? "
						+ "AND iol.IsActive = 'Y' "
						+ "AND io.DocStatus NOT IN ('CO','CL','RE','VO') ");
		if (currentLine_ID > 0)
			sql.append(" AND iol.M_InOutLine_ID != ?");

		BigDecimal draftQty;
		if (currentLine_ID > 0)
			draftQty = DB.getSQLValueBD(line.get_TrxName(), sql.toString(), c_OrderLine_ID, currentLine_ID);
		else
			draftQty = DB.getSQLValueBD(line.get_TrxName(), sql.toString(), c_OrderLine_ID);
		if (draftQty == null)
			draftQty = Env.ZERO;

		// 剩余可收数量 = 订购数量 - 已过账收货数量 - 其他草稿占用数量
		BigDecimal remainingQty = qtyOrdered.subtract(qtyDelivered).subtract(draftQty);

		if (movementQty.compareTo(remainingQty) > 0) {
			MOrder order = (MOrder) orderLine.getC_Order();
			String orderLineInfo = order.getDocumentNo() + " - " + orderLine.getLine();
			String productName = "";
			if (orderLine.getM_Product_ID() > 0) {
				MProduct product = MProduct.get(line.getCtx(), orderLine.getM_Product_ID());
				if (product != null)
					productName = product.getName();
			}

			throw new AdempiereException(
					"收货数量超出采购订单可收货数量！"
							+ " 采购订单行: " + orderLineInfo
							+ (productName.isEmpty() ? "" : ", 产品: " + productName)
							+ ", 本次收货数量: " + movementQty.toPlainString()
							+ ", 剩余可收数量: " + remainingQty.toPlainString()
							+ "（订购: " + qtyOrdered.toPlainString()
							+ ", 已收货: " + qtyDelivered.toPlainString()
							+ ", 其他在途草稿: " + draftQty.toPlainString() + "）");
		}
	}


	private void updateHeaderWeight(MInOutLine line, String topic) {  
	    boolean isNew = IEventTopics.PO_AFTER_NEW.equals(topic);  
	    boolean isChange = IEventTopics.PO_AFTER_CHANGE.equals(topic)  
	            && (line.is_ValueChanged("Weight") || line.is_ValueChanged(MInOutLine.COLUMNNAME_QtyEntered));
	    if (!isNew && !isChange) {
	    	return;
	    }
	    
	    int M_InOut_ID = line.getM_InOut_ID();  
	    int M_InOutLine_ID = line.get_ID();  
	    if (M_InOut_ID <= 0)  
	        return;  
	  
	    BigDecimal total = DB.getSQLValueBDEx(line.get_TrxName(),  
	        "SELECT COALESCE(SUM(Weight * QtyEntered), 0) FROM M_InOutLine " +  
	        "WHERE M_InOut_ID=? AND IsActive='Y'",  
	        M_InOut_ID);  
	    if (total == null)  
	        total = BigDecimal.ZERO;  
	  
	    MInOut parent = new MInOut(line.getCtx(), M_InOut_ID, line.get_TrxName());  
	    parent.set_ValueOfColumn("Weight", total);  
	    parent.saveEx();  
	}
	
	private void changeValueByProduct(MInOutLine line, String topic) {

		// 仅在 PO_BEFORE_CHANGE 且 M_Product_ID 确实变化时，或 PO_BEFORE_NEW 时触发
		boolean isChange = IEventTopics.PO_BEFORE_CHANGE.equals(topic)
				&& line.is_ValueChanged(MInOutLine.COLUMNNAME_M_Product_ID);
		boolean isNew = IEventTopics.PO_BEFORE_NEW.equals(topic);

		if (!isChange && !isNew) {
			return;
		}

		Object productIdObj = line.get_Value(MInOutLine.COLUMNNAME_M_Product_ID);
		if (productIdObj == null)
			return;
		int productId = ((Number) productIdObj).intValue();
		if (productId <= 0)
			return;

		MProduct product = MProduct.get(line.getCtx(), productId);
		if (product == null)
			return;

		// 面积
		Object boxArea = product.get_Value("BoxArea");
		if (boxArea != null)
			line.set_ValueOfColumn("Area", boxArea);

		// 规格：CardLength * CardWidth
		BigDecimal cardLength = toBD(product.get_Value("CardLength"));
		BigDecimal cardWidth = toBD(product.get_Value("CardWidth"));
		if (cardLength.compareTo(BigDecimal.ZERO) != 0 && cardWidth.compareTo(BigDecimal.ZERO) != 0) {
			line.set_ValueOfColumn("Specification", cardLength.stripTrailingZeros().toPlainString() + "*"
					+ cardWidth.stripTrailingZeros().toPlainString());
		}

		// 重量
		Object weightNet = product.get_Value("WeightNet");
		if (weightNet != null)
			line.set_ValueOfColumn("Weight", weightNet);

		// 压线
		Object creaseLine = product.get_Value("CreaseLine");
		if (creaseLine != null)
			line.set_ValueOfColumn("CreaseLine", creaseLine);
	}

	private boolean isReceiptDocType(MInOutLine line) {
		MInOut inout = (MInOut) line.getParent();
		int docTypeId = inout.getC_DocType_ID();
		if (docTypeId <= 0)
			return false;
		MDocType dt = MDocType.get(line.getCtx(), docTypeId);
		return dt != null && RECEIPT_DOCTYPE_UU.equals(dt.get_UUID());
	}

	// ── 工具方法 ──────────────────────────────────────────────────────────────
	private BigDecimal toBD(Object val) {
		if (val instanceof BigDecimal)
			return (BigDecimal) val;
		if (val == null)
			return BigDecimal.ZERO;
		try {
			return new BigDecimal(val.toString());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}
}