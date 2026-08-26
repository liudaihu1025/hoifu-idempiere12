package org.libero.process;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.libero.model.MPPCostCollector;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 * 生产报工完工Process
 * 手动设置生产报工的完成时间，并完成报工单。
 */
@org.adempiere.base.annotation.Process
public class ProductionFinishWorkReporting extends SvrProcess {

    private Timestamp p_DateFinish = null;

    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (name.equals("DateFinish")) {
                p_DateFinish = (Timestamp) para[i].getParameter();
            }
        }
    }

    protected String doIt() throws Exception {
        List<Integer> collectorIds = getRecord_IDs();
        if (collectorIds == null || collectorIds.isEmpty()) {
            int recordId = getRecord_ID();
            if (recordId > 0) {
                collectorIds = List.of(recordId);
            }
        }

        if (collectorIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要完工的生产报工");
        }

        int updatedCount = updateCollectors(collectorIds);

        return "成功设置完工时间 " + updatedCount + " 个生产报工";
    }

    private int updateCollectors(List<Integer> collectorIds) {
        int count = 0;
        for (Integer collectorId : collectorIds) {
            MPPCostCollector collector = new MPPCostCollector(getCtx(), collectorId, get_TrxName());
            if (collector.get_ID() > 0) {

				// 校验：已完成/已关闭/已作废的报工单不允许重复操作
				String docStatus = collector.getDocStatus();
				if (MPPCostCollector.DOCSTATUS_Completed.equals(docStatus)
						|| MPPCostCollector.DOCSTATUS_Closed.equals(docStatus)
						|| MPPCostCollector.DOCSTATUS_Voided.equals(docStatus)) {
					throw new AdempiereException("报工单 " + collector.getDocumentNo() + " 状态为已完成/已关闭/已作废，不允许重复完工");
				}



				// 使用传入的时间参数，如果没有则使用当前时间
				Timestamp finishTime = p_DateFinish != null ? p_DateFinish
						: new Timestamp(System.currentTimeMillis());
				collector.setDateFinish(finishTime);

                boolean skipQtyCheck = collector.isActivityControl() && !collector.isWorkReportTypeProduce();

                // 非生产报工不校验移动数量，不更新实际报工时间
				if (!collector.isNonProduction()) {
                    if (!skipQtyCheck) {
                        // 校验：完工时移动数量必须 > 0
                        BigDecimal qty = collector.getMovementQty();
                        if (qty == null || qty.signum() <= 0) {
                            throw new AdempiereException("报工单 " + collector.getDocumentNo() + " 移动数量必须大于0，无法完工");
                        }
                    }


					Timestamp startDate = collector.getDateStart();
					if (startDate != null) {
                        BigDecimal roundedHours = MPPCostCollector.updateDurationRealFromDates(startDate, finishTime);
                        collector.setDurationReal(roundedHours);
					}
				}
				collector.saveEx();

                // 完成单据（走完整文档引擎：prepareIt 累加工序数量/工时 + completeIt 创建成本明细）
                if (!collector.processIt(MPPCostCollector.DOCACTION_Complete)) {
                    throw new AdempiereException(collector.getProcessMsg());
                }
                collector.saveEx();
                count++;
            }
        }
        return count;
    }
}