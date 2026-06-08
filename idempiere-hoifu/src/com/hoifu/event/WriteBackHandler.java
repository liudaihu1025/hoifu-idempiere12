package com.hoifu.event;

import java.util.logging.Level;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.util.CLogger;
import org.eevolution.model.X_PP_Order;

import com.hoifu.model.qc.MQC_IPQC;
import com.hoifu.model.qc.MQC_IQC;
import com.hoifu.model.qc.MQC_OQC;
import com.hoifu.model.qc.MQC_RQC;

public class WriteBackHandler {
	private static final CLogger log = CLogger.getCLogger(WriteBackHandler.class);

	// ===== IQC 回写：收货单行 =====
	public static void writeBackIQCResult(MQC_IQC iqc) {
		if (iqc == null)
			return;
		int sourceId = iqc.getSourceLineID();
		if (sourceId <= 0)
			return;

		MInOutLine line = new MInOutLine(iqc.getCtx(), sourceId, iqc.get_TrxName());
		if (line.get_ID() == 0) {
			log.warning("IQC 回写：找不到收货单行 M_InOutLine_ID=" + sourceId);
			return;
		}
		// 安全检查：租户一致
		if (line.getAD_Client_ID() != iqc.getAD_Client_ID()) {
			log.warning("IQC 回写：租户不一致，跳过");
			return;
		}
		try {
			// 若列在 AD 中注册，用 set_ValueOfColumn；否则用 set_CustomColumn
			setColumnSafe(line, "QC_Result", iqc.getCheckResult());
			setColumnSafe(line, "QC_IQC_ID", iqc.getQC_IQC_ID());
			setColumnSafe(line, "QC_InspectDate", iqc.getInspectDate());
			line.saveEx();
			log.info("IQC 结果回写成功 -> M_InOutLine_ID=" + sourceId);
		} catch (Exception e) {
			log.log(Level.SEVERE, "IQC 回写失败 M_InOutLine_ID=" + sourceId, e);
			throw e; // 让上层事务感知并回滚
		}
	}

	// ===== IPQC 回写：制造工单 =====
	public static void writeBackIPQCResult(MQC_IPQC ipqc) {
		if (ipqc == null)
			return;
		int ppOrderId = ipqc.getPP_Order_ID();
		if (ppOrderId <= 0)
			return;

		// 使用 PO 模型，而非原生 SQL，保证安全检查和变更日志
		X_PP_Order ppOrder = new X_PP_Order(ipqc.getCtx(), ppOrderId, ipqc.get_TrxName());
		if (ppOrder.get_ID() == 0) {
			log.warning("IPQC 回写：找不到工单 PP_Order_ID=" + ppOrderId);
			return;
		}
		if (ppOrder.getAD_Client_ID() != ipqc.getAD_Client_ID()) {
			log.warning("IPQC 回写：租户不一致，跳过");
			return;
		}
		try {
			setColumnSafe(ppOrder, "QC_Result", ipqc.getCheckResult());
			setColumnSafe(ppOrder, "QC_IPQC_ID", ipqc.getQC_IPQC_ID());
			setColumnSafe(ppOrder, "QC_InspectDate", ipqc.getInspectDate());
			ppOrder.saveEx();
			log.info("IPQC 结果回写工单成功 -> PP_Order_ID=" + ppOrderId);
		} catch (Exception e) {
			log.log(Level.SEVERE, "IPQC 回写失败 PP_Order_ID=" + ppOrderId, e);
			throw e;
		}
	}

	// ===== OQC 回写：发货单行 =====
	public static void writeBackOQCResult(MQC_OQC oqc) {
		if (oqc == null)
			return;
		
		int sourceLineId = oqc.getSourceLineID();
		if (sourceLineId <= 0)
			return;

		MInOutLine line = new MInOutLine(oqc.getCtx(), sourceLineId, oqc.get_TrxName());
		if (line.get_ID() == 0) {
			log.warning("OQC 回写：找不到发货单行 M_InOutLine_ID=" + sourceLineId);
			return;
		}
		if (line.getAD_Client_ID() != oqc.getAD_Client_ID()) {
			log.warning("OQC 回写：租户不一致，跳过");
			return;
		}
		try {
			setColumnSafe(line, "QC_Result", oqc.getCheckResult());
			setColumnSafe(line, "QC_OQC_ID", oqc.getQC_OQC_ID());
			setColumnSafe(line, "QC_InspectDate", oqc.getInspectDate());
			line.saveEx();
			log.info("OQC 结果回写成功 -> M_InOutLine_ID=" + sourceLineId);
		} catch (Exception e) {
			log.log(Level.SEVERE, "OQC 回写失败 M_InOutLine_ID=" + sourceLineId, e);
			throw e;
		}
	}

	// ===== RQC 回写：退货单主表 =====
	public static void writeBackRQCResult(MQC_RQC rqc) {
		if (rqc == null)
			return;
		int sourceId = rqc.getSourceLineID();
		if (sourceId <= 0)
			return;

		MInOutLine io = new MInOutLine(rqc.getCtx(), sourceId, rqc.get_TrxName());
		if (io.get_ID() == 0) {
			log.warning("RQC 回写：找不到退货单行 M_InOutLine_ID=" + sourceId);
			return;
		}
		if (io.getAD_Client_ID() != rqc.getAD_Client_ID()) {
			log.warning("RQC 回写：租户不一致，跳过");
			return;
		}
		try {
			setColumnSafe(io, "RQC_Result", rqc.getCheckResult());
			setColumnSafe(io, "RQC_QC_ID", rqc.getQC_RQC_ID());
			setColumnSafe(io, "RQC_Disposition", rqc.getDisposition());
			setColumnSafe(io, "RQC_InspectDate", rqc.getInspectDate());
			io.saveEx();
			log.info("RQC 结果回写成功 -> M_InOut_ID=" + sourceId);

			// 根据处置建议触发后续动作
			handleDisposition(rqc);
		} catch (Exception e) {
			log.log(Level.SEVERE, "RQC 回写失败 M_InOut_ID=" + sourceId, e);
			throw e;
		}
	}

	// ===== 工具方法 =====

	/**
	 * 安全写列： - 若列在 AD 中注册（AD_Column 存在），用 set_ValueOfColumn（走 PO 框架校验） - 若列未在 AD
	 * 中注册（直接 ALTER TABLE 添加），用 set_CustomColumn（直接写 SQL） 两者都不会抛出异常，但
	 * set_ValueOfColumn 在列不存在时静默失败， 所以这里统一用 set_CustomColumn 保证写入，同时兼容两种情况。
	 */
	private static void setColumnSafe(org.compiere.model.PO po, String columnName, Object value) {
		if (value == null)
			return;
		// 优先走 PO 框架（列在 AD 中注册时有变更日志）
		boolean set = po.set_ValueOfColumnReturningBoolean(columnName, value);
		if (!set) {
			// 列未在 AD 中注册，回退到 CustomColumn（直接写 SQL）
			po.set_CustomColumn(columnName, value);
		}
	}

	private static void handleDisposition(MQC_RQC rqc) {
		String disposition = rqc.getDisposition();
		if (disposition == null)
			return;
		switch (disposition) {
		case "REPAIR":
			log.info("RQC 建议返工，待扩展创建返工工单");
			// TODO: 调用工单创建逻辑
			break;
		case "RETURN":
			log.info("RQC 建议退货，待扩展创建退货单");
			// TODO: 调用退货单创建逻辑
			break;
		case "SCRAP":
			log.info("RQC 建议报废，待扩展");
			break;
		default:
			break;
		}
	}
}