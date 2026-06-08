package org.libero.helper;

import java.math.BigDecimal;
import java.util.List;

import org.compiere.model.I_M_Locator;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocator;
import org.compiere.model.MLocatorType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;

/**
 * 委外工单相关业务方法封装类
 */
public class SubcontractPPOrderHelper {

	/**
	 * 更新委外订单明细关联的工单，根据本次收货数量，按工单创建时间先后顺序为工单分配入库数量，超收则将数量分配给最后一个工单。
	 * 
	 * @param ol          委外订单明细
	 * @param get_TrxName 当前事务
	 */
	public void updateSubcontractPPOrder(MOrderLine ol, String get_TrxName) {
		MOrder order = ol.getParent();

		if (!isSubcontractOrder(order)) {
			return; // 不是委外订单，不处理
		}

		// 获取QtyDelivered的变化量
		BigDecimal qtyDifference = (BigDecimal) ol.get_ValueDifference("QtyDelivered");
		if (qtyDifference == null || qtyDifference.compareTo(Env.ZERO) == 0) {
			return; // 没有变化，直接返回
		}

		// 查找所有关联的工单，按创建时间排序
		String whereClause = "C_OrderLine_ID=? AND DocStatus='CO'";
		List<MPPOrder> orders = new Query(ol.getCtx(), MPPOrder.Table_Name, whereClause, get_TrxName)
				.setParameters(ol.getC_OrderLine_ID()).setOnlyActiveRecords(true) // 替代 IsActive='Y'
				.setOrderBy("Created ASC").list();

		if (orders.isEmpty()) {
			return; // 没有关联工单
		}

		if (qtyDifference.compareTo(Env.ZERO) > 0) {
			// 正数：按时间正序累加
			distributePositiveQuantity(orders, qtyDifference, get_TrxName);
		} else {
			// 负数：按时间倒序扣减
			distributeNegativeQuantity(orders, qtyDifference.negate(), get_TrxName);
		}
	}

	private void distributePositiveQuantity(List<MPPOrder> orders, BigDecimal qtyToDistribute, String trxName) {
		for (int i = 0; i < orders.size() && qtyToDistribute.compareTo(Env.ZERO) > 0; i++) {
			MPPOrder order = orders.get(i);
			BigDecimal qtyEntered = order.getQtyEntered();
			BigDecimal currentDelivered = order.getQtyDelivered();

			// 计算当前工单还能接收多少数量
			BigDecimal remainingCapacity = qtyEntered.subtract(currentDelivered);

			// 如果是最后一个工单，可以超过QtyEntered
			boolean isLastOrder = (i == orders.size() - 1);
			BigDecimal qtyToAdd = isLastOrder ? qtyToDistribute : qtyToDistribute.min(remainingCapacity);

			if (qtyToAdd.compareTo(Env.ZERO) > 0) {
				order.setQtyDelivered(currentDelivered.add(qtyToAdd));
				// 更新工单状态
				updatePPOrderStatusByQtyDelivered(order);
				order.saveEx(trxName);
				qtyToDistribute = qtyToDistribute.subtract(qtyToAdd);
			}
		}
	}

	private void distributeNegativeQuantity(List<MPPOrder> orders, BigDecimal qtyToDeduct, String trxName) {
		// 按时间倒序处理
		for (int i = orders.size() - 1; i >= 0 && qtyToDeduct.compareTo(Env.ZERO) > 0; i--) {
			MPPOrder order = orders.get(i);
			BigDecimal currentDelivered = order.getQtyDelivered();

			if (currentDelivered.compareTo(Env.ZERO) > 0) {
				// 计算可以从当前工单扣减的数量（不能小于0）
				BigDecimal qtyToDeductFromOrder = qtyToDeduct.min(currentDelivered);
				order.setQtyDelivered(currentDelivered.subtract(qtyToDeductFromOrder));
				// 更新工单状态
				updatePPOrderStatusByQtyDelivered(order);
				order.saveEx(trxName);
				qtyToDeduct = qtyToDeduct.subtract(qtyToDeductFromOrder);
			}
		}
	}

	/**
	 * 校验订单的目标单据类型名称是否为"委外订单"
	 * 
	 * @param order 要校验的订单
	 * @return true 表示是委外订单
	 */
	public boolean isSubcontractOrder(MOrder order) {
		int docTypeTargetId = order.getC_DocTypeTarget_ID();
		if (docTypeTargetId <= 0)
			return false;

		MDocType docType = MDocType.get(order.getCtx(), docTypeTargetId);
		if (docType == null)
			return false;

		return "委外订单".equals(docType.getName());
	}

	/**
	 * 检查订单所有明细的 QtyDelivered >= QtyEntered， 若全部满足则将 C_Order.IsDelivered 设为
	 * true，否则设为 false。 只在值发生变化时才执行 saveEx，避免不必要的数据库写入。
	 */
	public void updateOrderDeliveredStatus(MOrder order, String trxName) {
		// 强制重新查询，确保拿到当前事务内最新的 QtyDelivered 值
		MOrderLine[] lines = order.getLines(true, null);
		if (lines == null || lines.length == 0)
			return;

		boolean allDelivered = true;
		for (MOrderLine line : lines) {
			if (line.getQtyDelivered().compareTo(line.getQtyEntered()) < 0) {
				allDelivered = false;
				break;
			}
		}

		// 只在状态发生变化时才保存，减少不必要的 DB 写入
		if (order.isDelivered() != allDelivered) {
			order.setIsDelivered(allDelivered);
			order.saveEx(trxName);
		}
	}

	/**
	 * 入库数量大于0，就为已入库
	 */
	private void updatePPOrderStatusByQtyDelivered(MPPOrder order) {
		String orderStatus = (String) order.get_Value("Orderstatus");
		if (order.getQtyDelivered().compareTo(Env.ZERO) > 0 && !"Stored".equals(orderStatus)) {
			order.set_ValueOfColumn("Orderstatus", "Stored");
		}
		if (order.getQtyDelivered().compareTo(Env.ZERO) == 0 && "Stored".equals(orderStatus)) {
			order.set_ValueOfColumn("Orderstatus", "Completed");
		}
	}

	/**
	 * 在收货单保存时，自动生成收发明细行。
	 * 
	 * 触发条件： 1. 必须是收货单（非销售事务，DocBaseType=MMR） 2. C_DocType.PrintName 包含"委外" 3.
	 * 已关联采购单（C_Order_ID > 0）
	 * 
	 * 生成逻辑： - 遍历采购单明细，找出  QtyEntered >  QtyDelivered的行 - 默认入库数量 = QtyEntered -
	 * QtyDelivered - 默认库位 = M_LocatorType.Name='委外库位' 下 IsDefault=true 的库位（按 Created
	 * ASC） 若无 IsDefault，则取该类型下按 Created ASC 排序的第一个 若该类型下无库位，则跳过不生成
	 * 
	 */
	public void autoCreateInOutLines(MInOut inout, String trxName) {
		  
	    // 1. 必须已关联订单  
	    if (inout.getC_Order_ID() <= 0)  
	        return;  
	  
	    // 2. 判断是否是委外单据  
	    MDocType docType = MDocType.get(inout.getCtx(), inout.getC_DocType_ID());  
	    if (docType == null)  
	        return;  
	    String docTypeName = docType.getName();  
	    boolean isSubcontract = docTypeName != null && docTypeName.contains("委外");  
	  
	    int warehouseId = inout.getM_Warehouse_ID();  
	  
	    // 3. 委外单据：在循环外查一次"委外库位"，找不到则整个方法退出  
	    MLocator subcontractLocator = null;  
	    if (isSubcontract) {  
	        MLocatorType subcontractLocType = new Query(inout.getCtx(), MLocatorType.Table_Name, "Name=?", trxName)  
	                .setParameters("委外库位").setOnlyActiveRecords(true).firstOnly();  
	        if (subcontractLocType == null)  
	            return;  
	  
	        String locatorWhere = "M_LocatorType_ID=? AND M_Warehouse_ID=? AND IsDefault='Y'";  
	        List<MLocator> defaultLocators = new Query(inout.getCtx(), I_M_Locator.Table_Name, locatorWhere, trxName)  
	                .setParameters(subcontractLocType.getM_LocatorType_ID(), warehouseId)  
	                .setOnlyActiveRecords(true).setOrderBy("Created ASC").list();  
	  
	        if (!defaultLocators.isEmpty()) {  
	            subcontractLocator = defaultLocators.get(0);  
	        } else {  
	            List<MLocator> allLocators = new Query(inout.getCtx(), I_M_Locator.Table_Name,  
	                    "M_LocatorType_ID=? AND M_Warehouse_ID=?", trxName)  
	                    .setParameters(subcontractLocType.getM_LocatorType_ID(), warehouseId)  
	                    .setOnlyActiveRecords(true).setOrderBy("Created ASC").list();  
	            if (allLocators.isEmpty())  
	                return;  
	            subcontractLocator = allLocators.get(0);  
	        }  
	    }  
	  
	    // 4. 遍历订单明细，生成收发货明细行  
	    MOrder order = new MOrder(inout.getCtx(), inout.getC_Order_ID(), trxName);  
	    MOrderLine[] orderLines = order.getLines(true, null);  
	    String isSOTrx = inout.isSOTrx() ? "Y" : "N";  
	  
	    for (MOrderLine ol : orderLines) {  
	        BigDecimal qtyDelivered = ol.getQtyDelivered();  
	        BigDecimal qtyEntered = ol.getQtyEntered();  
	  
	        if (qtyEntered.compareTo(qtyDelivered) <= 0)  
	            continue;  
	  
	        int locatorId;  
	        if (isSubcontract) {  
	            // 委外：所有行共用同一个委外库位  
	            locatorId = subcontractLocator.getM_Locator_ID();  
	        } else {  
	            // 非委外：按产品调用 PG 函数获取推荐库位  
	            locatorId = DB.getSQLValue(trxName,  
	                "SELECT get_recommended_locator(?, ?, ?)",  
	                ol.getM_Product_ID(), warehouseId, isSOTrx);  
	            if (locatorId <= 0)  
	                continue; // 找不到库位则跳过该行  
	        }  
	  
	        BigDecimal qty = qtyEntered.subtract(qtyDelivered);  
	  
	        MInOutLine ioLine = new MInOutLine(inout);  
	        ioLine.setOrderLine(ol, locatorId, qty);  
	        ioLine.setQty(qty);  
	        ioLine.saveEx(trxName);  
	    }  
	}
}
