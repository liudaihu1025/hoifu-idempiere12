package com.hoifu.delegate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.event.EventHelper;
import org.adempiere.base.event.annotations.EventDelegate;
import org.adempiere.base.event.annotations.doc.BeforeVoid;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

@EventTopicDelegate
public class PPOrderVoidRelatedDocCheck extends EventDelegate {

	public PPOrderVoidRelatedDocCheck(Event event) {
		super(event);
	}

	@BeforeVoid
	public void onBeforeVoid() {
		PO po = EventHelper.getPO(event);
		if (po == null || !"PP_Order".equals(po.get_TableName())) {
			return; // 不是生产工单，不处理
		}

		int ppOrderId = po.get_ID();

		// 领退料单：只要存在非"已作废"的记录就阻止
		int requisitionCount = new Query(Env.getCtx(), "PP_Material_Requisition",
				"PP_Order_ID=? AND DocStatus<>'VO'",
				po.get_TrxName()).setParameters(ppOrderId).setOnlyActiveRecords(true).count();

		// 生产入库（成本收集器）校验：
		// 先收集所有 RE 记录的 Reversal_ID（即"合法反冲结果"的 ID 集合）
		List<PO> reCollectors = new Query(Env.getCtx(), "PP_Cost_Collector",
				"PP_Order_ID=? AND CostCollectorType='100' AND DocStatus='RE' AND Reversal_ID IS NOT NULL",
				po.get_TrxName()).setParameters(ppOrderId).setOnlyActiveRecords(true).list();

		Set<Integer> reversalTargetIds = new HashSet<>();
		for (PO reCC : reCollectors) {
			int reversalId = reCC.get_ValueAsInt("Reversal_ID");
			if (reversalId > 0) {
				reversalTargetIds.add(reversalId);
			}
		}

		// 查询该工单下所有活跃的生产入库记录
		List<PO> allCollectors = new Query(Env.getCtx(), "PP_Cost_Collector",
				"PP_Order_ID=? AND CostCollectorType='100'",
				po.get_TrxName())
				.setParameters(ppOrderId).setOnlyActiveRecords(true).list();

		for (PO cc : allCollectors) {
			int ccId = cc.get_ID();
			String docStatus = cc.get_ValueAsString("DocStatus");

			// RE 本身：被反冲的原始记录，合法状态，继续检查其 Reversal_ID 是否指向已完成的反冲单
			if ("RE".equals(docStatus)) {
				int reversalId = cc.get_ValueAsInt("Reversal_ID");
				if (reversalId <= 0) {
					throw new AdempiereException("生产工单已关联领退料单或生产入库单，作废失败");
				}
				PO reversal = new Query(Env.getCtx(), "PP_Cost_Collector",
						"PP_Cost_Collector_ID=?",
						po.get_TrxName())
						.setParameters(reversalId).first();
				if (reversal == null || !"CO".equals(reversal.get_ValueAsString("DocStatus"))) {
					throw new AdempiereException("生产工单已关联领退料单或生产入库单，作废失败");
				}
				continue; // 合法反冲，不阻止
			}

			// 被某条 RE 记录的 Reversal_ID 指向的记录：是合法的反冲结果（通常为 CO），不阻止
			if (reversalTargetIds.contains(ccId)) {
				continue;
			}

			// 其他所有状态（未反冲的 CO、DR、IP 等）→ 阻止
			throw new AdempiereException("生产工单已关联领退料单或生产入库单，作废失败");
		}

		if (requisitionCount > 0) {
			throw new AdempiereException("生产工单已关联领退料单或生产入库单，作废失败");
		}
	}
}
