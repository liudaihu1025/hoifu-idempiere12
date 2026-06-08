package org.libero.process;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.compiere.process.DocAction;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.libero.model.MPPOrder;

/**
 * 工单开始工作流程 支持三种场景： 1. 生产工单窗口（列表视图多选）：通过 getRecord_IDs() 获取 2. 普通 AD
 * 窗口（详情视图单条）：通过 getRecord_ID() 获取 3. 工单信息窗口（Info Window）：通过 T_Selection 表获取
 */
@org.adempiere.base.annotation.Process
public class PPOrderStartWorkProcess extends SvrProcess {

	protected void prepare() {
		// 此流程没有参数
	}

	protected String doIt() throws Exception {
		List<Integer> orderIds = getRecord_IDs();

		if (orderIds == null || orderIds.isEmpty()) {
			int recordId = getRecord_ID();
			if (recordId > 0) {
				// 场景2：普通 AD 窗口详情视图
				orderIds = List.of(recordId);
			} else {
				// 场景3：Info Window，从 T_Selection 表读取选中记录
				int[] ids = DB.getIDsEx(get_TrxName(), "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
						getAD_PInstance_ID());
				if (ids != null && ids.length > 0) {
					orderIds = new ArrayList<>();
					for (int id : ids) {
						orderIds.add(id);
					}
				}
			}
		}

		if (orderIds == null || orderIds.isEmpty()) {
			throw new IllegalArgumentException("请选择要开工的工单");
		}

		validateOrderStatus(orderIds);
		int updatedCount = updateOrders(orderIds);

		return "成功开工 " + updatedCount + " 个工单";
	}

	private void validateOrderStatus(List<Integer> orderIds) throws Exception {
		List<String> invalidOrders = orderIds.stream().map(orderId -> {
			MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());
			if (order.get_ID() > 0) {
				String orderStatus = (String) order.get_Value("Orderstatus");
				if (!"Released".equals(orderStatus) && !"Paused".equals(orderStatus)) {
					return order.getDocumentNo();
				}
			}
			return null;
		}).filter(docNo -> docNo != null).collect(Collectors.toList());

		if (!invalidOrders.isEmpty()) {
			throw new IllegalArgumentException(
					"您选择的工单中包含以下\"非已发布或已暂停\"状态的条目：" + String.join(", ", invalidOrders) + "。只有状态为【已发布】或【已暂停】的工单才允许开工。");
		}
	}

	private int updateOrders(List<Integer> orderIds) {
		int count = 0;
		for (Integer orderId : orderIds) {
			MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());
			if (order.get_ID() > 0) {
				String orderStatus = (String) order.get_Value("Orderstatus");

				if ("Released".equals(orderStatus)) {
					if (DocAction.STATUS_InProgress.equals(order.getDocStatus())) {
						order.setDocAction(DocAction.ACTION_Complete);
						if (order.processIt(DocAction.ACTION_Complete)) {
							order.set_ValueOfColumn("Orderstatus", "Started");
							order.setDateStart(new Timestamp(System.currentTimeMillis()));
							order.saveEx();
							count++;
						} else {
							log.warning("完成工单 " + order.getDocumentNo() + " 失败: " + order.getProcessMsg());
							throw new IllegalArgumentException("完成工单失败: " + order.getProcessMsg());
						}
					}
				} else if ("Paused".equals(orderStatus)) {
					order.set_ValueOfColumn("Orderstatus", "InProgress");
					order.saveEx();
					count++;
				}
			}
		}
		return count;
	}
}