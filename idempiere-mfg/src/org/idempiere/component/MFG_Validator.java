/**    
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.    
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.    
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.    
 * In compliance with previous GPLv2.0 works of ComPiere USA, eEvolution MEXICO, iDempiere contributors and Mutlimage SLOVAKIA    
 */
package org.idempiere.component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_Forecast;
import org.compiere.model.I_M_ForecastLine;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_Movement;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Requisition;
import org.compiere.model.I_M_RequisitionLine;
import org.compiere.model.MDocType;
import org.compiere.model.MForecastLine;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMALine;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_M_Forecast;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.model.I_PP_Order_Node;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.libero.model.MPPCostCollector;
import org.libero.model.MPPMRP;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;
import org.libero.tables.I_DD_Order;
import org.libero.tables.I_DD_OrderLine;
import org.libero.tables.I_PP_Order;
import org.libero.tables.I_PP_Order_BOMLine;
import org.osgi.service.event.Event;

public class MFG_Validator extends AbstractEventHandler {
	private static CLogger log = CLogger.getCLogger(MFG_Validator.class);
	private String trxName = "";
	private PO po = null;

	@Override
	protected void initialize() {
		registerEvent(IEventTopics.AFTER_LOGIN);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, I_M_Movement.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_C_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_Requisition.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_RequisitionLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_DD_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_DD_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_PP_Order_BOMLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, I_M_Product.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_C_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_Requisition.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_RequisitionLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_DD_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_DD_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_PP_Order_BOMLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_C_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_Requisition.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_RequisitionLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_DD_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_DD_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_PP_Order_BOMLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, I_M_Movement.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, I_M_InOut.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_PP_Order_Node.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_PP_Order_Node.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_PP_Order_Node.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, "C_PaperScrapStd");
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, "C_PaperScrapStd");
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, "C_PaperScrapStd");
		log.info("MFG MODEL VALIDATOR IS NOW INITIALIZED");
	}

	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		DocAction doc = null;
		boolean isDelete = false;
		boolean isReleased = false;
		boolean isVoided = false;
		boolean isChange = false;

		if (type.equals(IEventTopics.AFTER_LOGIN)) {
			LoginEventData eventData = getEventData(event);
			log.fine(" topic=" + event.getTopic() + " AD_Client_ID=" + eventData.getAD_Client_ID() + " AD_Org_ID="
					+ eventData.getAD_Org_ID() + " AD_Role_ID=" + eventData.getAD_Role_ID() + " AD_User_ID="
					+ eventData.getAD_User_ID());
		} else {
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			log.info(" topic=" + event.getTopic() + " po=" + po);
			isChange = (IEventTopics.PO_AFTER_NEW == type
					|| (IEventTopics.PO_AFTER_CHANGE == type && MPPMRP.isChanged(po)));
			isDelete = (IEventTopics.PO_BEFORE_DELETE == type);
			isReleased = false;
			isVoided = false;

			// Can we change M_Product.C_UOM_ID ?
			if (po instanceof MProduct && IEventTopics.PO_BEFORE_CHANGE == type
					&& po.is_ValueChanged(MProduct.COLUMNNAME_C_UOM_ID) && MPPMRP.hasProductRecords((MProduct) po)) {
				throw new AdempiereException("@SaveUomError@");
			}

			// ★ 先拦截 PP_Order_Node，不走 MPPMRP.deleteMRP
			if ("PP_Order_Node".equals(po.get_TableName())) {
				Integer ppOrderId = (Integer) po.get_Value("PP_Order_ID");
				if (ppOrderId != null && ppOrderId > 0) {
					Integer excludeNodeId = isDelete ? po.get_ID() : null;
					recalculatePaperScrapChain(ppOrderId, excludeNodeId, trxName);
				}
				return;
			}

			// ★ C_PaperScrapStd：唯一性校验 + 数据变化后同步工单工序
			if ("C_PaperScrapStd".equals(po.get_TableName())) {
				// BEFORE_NEW / BEFORE_CHANGE：唯一性校验
				if (IEventTopics.PO_BEFORE_NEW.equals(type) || IEventTopics.PO_BEFORE_CHANGE.equals(type)) {
					Integer routingNodeId = (Integer) po.get_Value("AD_Routing_Node_ID");
					String processDifficulty = (String) po.get_Value("ProcessDifficulty");
					int currentId = po.get_ID(); // 新建时为 0
					String checkSql = "SELECT COUNT(1) FROM C_PaperScrapStd " + "WHERE AD_Client_ID=? AND AD_Org_ID=? "
							+ "AND AD_Routing_Node_ID=? AND ProcessDifficulty=? "
							+ "AND C_PaperScrapStd_ID <> ? AND IsActive='Y'";
					int count = org.compiere.util.DB.getSQLValue(trxName, checkSql, po.getAD_Client_ID(),
							po.getAD_Org_ID(), routingNodeId, processDifficulty, currentId);
					if (count > 0) {
						throw new AdempiereException("该工序和工艺难度的组合已存在，不能重复");
					}
				}
				// AFTER_CHANGE：同步更新所有使用该数据的工单工序
				if (IEventTopics.PO_AFTER_CHANGE.equals(type)) {
					syncPaperScrapStdToNodes(po, trxName);
				}
				return; // ← 所有事件类型统一 return，不走后面的 DocStatus 相关代码
			}

			// 以下代码只对有 DocStatus 字段的表执行
			if (po instanceof DocAction) {
				doc = (DocAction) po;
			} else if (po instanceof MOrderLine) {
				doc = ((MOrderLine) po).getParent();
			}

			if (doc != null) {
				String docStatus = doc.getDocStatus();
				isReleased = DocAction.STATUS_InProgress.equals(docStatus)
						|| DocAction.STATUS_Completed.equals(docStatus);
				isVoided = DocAction.STATUS_Voided.equals(docStatus);
			}

			if (isDelete || isVoided || !po.isActive()) {
				logEvent(event, po, type);
				MPPMRP.deleteMRP(po);
			} else if (po instanceof MOrder) {
				MOrder order = (MOrder) po;
				if (isChange && !order.isSOTrx()) {
					logEvent(event, po, type);
					MPPMRP.C_Order(order);
				} else if (type == IEventTopics.PO_AFTER_CHANGE && order.isSOTrx()) {
					if (isReleased || MPPMRP.isChanged(order)) {
						logEvent(event, po, type);
						MPPMRP.C_Order(order);
					}
				}
			} else if (po instanceof MOrderLine && isChange) {
				MOrderLine ol = (MOrderLine) po;
				MOrder order = ol.getParent();
				if (!order.isSOTrx()) {
					logEvent(event, po, type);
					MPPMRP.C_OrderLine(ol);
				} else if (order.isSOTrx() && isReleased) {
					logEvent(event, po, type);
					MPPMRP.C_OrderLine(ol);
				}
			} else if (po instanceof MRequisition && isChange) {
				MRequisition r = (MRequisition) po;
				logEvent(event, po, type);
				MPPMRP.M_Requisition(r);
			} else if (po instanceof MRequisitionLine && isChange) {
				MRequisitionLine rl = (MRequisitionLine) po;
				logEvent(event, po, type);
				MPPMRP.M_RequisitionLine(rl);
			} else if (po instanceof X_M_Forecast && isChange) {
				X_M_Forecast fl = (X_M_Forecast) po;
				logEvent(event, po, type);
				MPPMRP.M_Forecast(fl);
			} else if (po instanceof MForecastLine && isChange) {
				MForecastLine fl = (MForecastLine) po;
				logEvent(event, po, type);
				MPPMRP.M_ForecastLine(fl);
			} else if (po instanceof MDDOrder && isChange) {
				MDDOrder order = (MDDOrder) po;
				logEvent(event, po, type);
				MPPMRP.DD_Order(order);
			} else if (po instanceof MDDOrderLine && isChange) {
				MDDOrderLine ol = (MDDOrderLine) po;
				logEvent(event, po, type);
				MPPMRP.DD_OrderLine(ol);
			} else if (po instanceof MPPOrder) {
				MPPOrder order = (MPPOrder) po;
				if (isChange) {
					logEvent(event, po, type);
					MPPMRP.PP_Order(order);
				}
				if (IEventTopics.PO_AFTER_CHANGE == type && order.is_ValueChanged(MPPOrder.COLUMNNAME_DocStatus)
						&& MPPOrder.DOCSTATUS_Approved.equals(order.getDocStatus())) {
					MDocType docType = MDocType.get(order.getCtx(), order.getC_DocType_ID());
					if ("重工工单".equals(docType.getName())) {
						order.setDocStatus(MPPOrder.DOCSTATUS_Drafted);
						order.setDocAction(MPPOrder.DOCACTION_Complete);
						order.saveEx(order.get_TrxName());
					}
				}
			} else if (po instanceof MPPOrderBOMLine && isChange) {
				MPPOrderBOMLine obl = (MPPOrderBOMLine) po;
				logEvent(event, po, type);
				MPPMRP.PP_Order_BOMLine(obl);
			}

			// PO: TYPE_AFTER_NEW
			if (event.getTopic().equals(IEventTopics.PO_AFTER_NEW)) {
				po = getPO(event);
				log.info(" topic=" + event.getTopic() + " po=" + po);
			} else if (event.getTopic().equals(IEventTopics.PO_BEFORE_CHANGE)) {
				po = getPO(event);
				log.info(" topic=" + event.getTopic() + " po=" + po);
				if (po.get_TableName().equals(I_M_Product.Table_Name)) {
					String msg = "TODO";
					logEvent(event, po, type);
				}
			}

			if (po instanceof MInOut && type == IEventTopics.DOC_AFTER_COMPLETE) {
				logEvent(event, po, type);
				MInOut inout = (MInOut) po;
				if (inout.isSOTrx()) {
					for (MInOutLine outline : inout.getLines()) {
						updateMPPOrder(outline);
					}
				} else {
					for (MInOutLine line : inout.getLines()) {
						final String whereClause = "C_OrderLine_ID=? AND PP_Cost_Collector_ID IS NOT NULL";
						Collection<MOrderLine> olines = new Query(po.getCtx(), MOrderLine.Table_Name, whereClause,
								trxName).setParameters(new Object[] { line.getC_OrderLine_ID() }).list();
						for (MOrderLine oline : olines) {
							if (oline.getQtyOrdered().compareTo(oline.getQtyDelivered()) >= 0) {
								MPPCostCollector cc = new MPPCostCollector(po.getCtx(), oline.getPP_Cost_Collector_ID(),
										trxName);
								String docStatus = cc.completeIt();
								cc.setDocStatus(docStatus);
								cc.setDocAction(MPPCostCollector.DOCACTION_Close);
								cc.saveEx(trxName);
								return;
							}
						}
					}
				}
			} else if (po instanceof MMovement && type == IEventTopics.DOC_AFTER_COMPLETE) {
				logEvent(event, po, type);
				MMovement move = (MMovement) po;
				for (MMovementLine line : move.getLines(false)) {
					if (line.getDD_OrderLine_ID() > 0) {
						MDDOrderLine oline = new MDDOrderLine(line.getCtx(), line.getDD_OrderLine_ID(),
								po.get_TrxName());
						MLocator locator_to = MLocator.get(line.getCtx(), line.getM_LocatorTo_ID());
						MWarehouse warehouse = MWarehouse.get(line.getCtx(), locator_to.getM_Warehouse_ID());
						if (warehouse.isInTransit()) {
							oline.setQtyInTransit(oline.getQtyInTransit().add(line.getMovementQty()));
							oline.setConfirmedQty(Env.ZERO);
						} else {
							oline.setQtyInTransit(oline.getQtyInTransit().subtract(line.getMovementQty()));
							oline.setQtyDelivered(oline.getQtyDelivered().add(line.getMovementQty()));
						}
						oline.saveEx(trxName);
					}
				}
				if (move.getDD_Order_ID() > 0) {
					MDDOrder order = new MDDOrder(move.getCtx(), move.getDD_Order_ID(), move.get_TrxName());
					order.setIsInTransit(isInTransit(order));
					order.reserveStock(order.getLines(true, null));
					order.saveEx(trxName);
				}
			}
		}
	}

	private boolean isInTransit(MDDOrder order) {
		for (MDDOrderLine line : order.getLines(true, null)) {
			if (line.getQtyInTransit().signum() != 0) {
				return true;
			}
		}
		return false;
	}

	private void updateMPPOrder(MInOutLine outline) {
		MPPOrder order = null;
		BigDecimal qtyShipment = Env.ZERO;
		MInOut inout = outline.getParent();
		String movementType = inout.getMovementType();
		int C_OrderLine_ID = 0;
		if (MInOut.MOVEMENTTYPE_CustomerShipment.equals(movementType)) {
			C_OrderLine_ID = outline.getC_OrderLine_ID();
			qtyShipment = outline.getMovementQty();
		} else if (MInOut.MOVEMENTTYPE_CustomerReturns.equals(movementType)) {
			MRMALine rmaline = new MRMALine(outline.getCtx(), outline.getM_RMALine_ID(), null);
			MInOutLine line = (MInOutLine) rmaline.getM_InOutLine();
			C_OrderLine_ID = line.getC_OrderLine_ID();
			qtyShipment = outline.getMovementQty().negate();
		}

		final String whereClause = " C_OrderLine_ID = ? " + " AND DocStatus IN  (?,?)"
				+ " AND EXISTS (SELECT 1 FROM  PP_Order_BOM "
				+ " WHERE PP_Order_BOM.PP_Order_ID=PP_Order.PP_Order_ID AND PP_Order_BOM.BOMType =? )";

		order = new Query(outline.getCtx(), I_PP_Order.Table_Name, whereClause, outline.get_TrxName())
				.setParameters(new Object[] { C_OrderLine_ID, MPPOrder.DOCSTATUS_InProgress,
						MPPOrder.DOCSTATUS_Completed, MPPOrderBOM.BOMTYPE_Make_To_Kit })
				.firstOnly();
		if (order == null) {
			return;
		}

		if (MPPOrder.DOCSTATUS_InProgress.equals(order.getDocStatus())) {
			order.completeIt();
			order.setDocStatus(MPPOrder.ACTION_Complete);
			order.setDocAction(MPPOrder.DOCACTION_Close);
			order.saveEx(trxName);
		}
		if (MPPOrder.DOCSTATUS_Completed.equals(order.getDocStatus())) {
			String description = order.getDescription() != null ? order.getDescription()
					: "" + Msg.translate(inout.getCtx(), MInOut.COLUMNNAME_M_InOut_ID) + " : "
							+ Msg.translate(inout.getCtx(), MInOut.COLUMNNAME_DocumentNo);
			order.setDescription(description);
			order.updateMakeToKit(qtyShipment);
			order.saveEx(trxName);
		}

		if (order.getQtyToDeliver().compareTo(Env.ZERO) == 0) {
			order.closeIt();
			order.setDocStatus(MPPOrder.DOCACTION_Close);
			order.setDocAction(MPPOrder.DOCACTION_None);
			order.saveEx(trxName);
		}
		return;
	}

	private void logEvent(Event event, PO po, String msg) {
		log.info("LiberoMFG >> ModelValidator // " + event.getTopic() + " po=" + po + " MESSAGE =" + msg);
	}

	private void setPo(PO eventPO) {
		po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;
	}

	private void recalculatePaperScrapChain(Integer ppOrderId, Integer excludeNodeId, String trxName) {
		BigDecimal qtyPerBatch = getPaperQtyPerBatch(ppOrderId, trxName);

		String excludeClause = (excludeNodeId != null) ? " AND PP_Order_Node_ID <> " + excludeNodeId : "";
		String sql = "SELECT PP_Order_Node_ID, QtyPaperScrap " + "FROM PP_Order_Node "
				+ "WHERE PP_Order_ID=? AND IsActive='Y'" + excludeClause + " ORDER BY Value ASC";

		java.util.List<Object[]> nodes = new java.util.ArrayList<>();
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = org.compiere.util.DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, ppOrderId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				nodes.add(new Object[] { rs.getInt(1), rs.getBigDecimal(2) });
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
			return;
		} finally {
			org.compiere.util.DB.close(rs, pstmt);
		}

		BigDecimal prevTotal = BigDecimal.ZERO;
		for (Object[] node : nodes) {
			int nodeId = (int) node[0];
			BigDecimal qtyPaperScrap = (BigDecimal) node[1];

			if (qtyPaperScrap == null)
				qtyPaperScrap = BigDecimal.ZERO;

			BigDecimal qtyPaperTotalScrap = qtyPaperScrap.add(prevTotal);

			BigDecimal ratePaperTotalScrap = null;
			if (qtyPerBatch != null && qtyPerBatch.signum() != 0) {
				BigDecimal denominator = qtyPaperTotalScrap.add(qtyPerBatch);
				if (denominator.signum() != 0) {
					ratePaperTotalScrap = qtyPaperTotalScrap.divide(denominator, 8, java.math.RoundingMode.HALF_UP)
							.multiply(new BigDecimal("100")).setScale(4, java.math.RoundingMode.HALF_UP);
				}
			}

			updatePaperNodeCumulative(nodeId, qtyPaperTotalScrap, ratePaperTotalScrap, trxName);
			prevTotal = qtyPaperTotalScrap;
		}
	}

	private BigDecimal getPaperQtyPerBatch(Integer ppOrderId, String trxName) {
		String sql = "SELECT QtyEntered, QtyBatchSize FROM PP_Order WHERE PP_Order_ID=?";
		java.sql.PreparedStatement pstmt = null;
		java.sql.ResultSet rs = null;
		try {
			pstmt = org.compiere.util.DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, ppOrderId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal qtyEntered = rs.getBigDecimal("QtyEntered");
				BigDecimal qtyBatchSize = rs.getBigDecimal("QtyBatchSize");
				if (qtyEntered == null)
					return BigDecimal.ONE;
				if (qtyBatchSize == null || qtyBatchSize.signum() == 0)
					return qtyEntered;
				return qtyEntered.divide(qtyBatchSize, 6, java.math.RoundingMode.HALF_UP);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "getPaperQtyPerBatch", e);
		} finally {
			org.compiere.util.DB.close(rs, pstmt);
		}
		return BigDecimal.ONE;
	}

	private void updatePaperNodeCumulative(int nodeId, BigDecimal qtyPaperTotalScrap, BigDecimal ratePaperTotalScrap,
			String trxName) {
		String sql = "UPDATE PP_Order_Node SET "
				+ "QtyPaperTotalScrap=?, RatePaperTotalScrap=?, Updated=now(), UpdatedBy=0 "
				+ "WHERE PP_Order_Node_ID=?";
		org.compiere.util.DB.executeUpdate(sql, new Object[] { qtyPaperTotalScrap, ratePaperTotalScrap, nodeId }, false,
				trxName);
		String bomSql = "UPDATE PP_Order_BOMLine SET "
				+ "QtyPaperTotalScrap=?, RatePaperTotalScrap=?, Updated=now(), UpdatedBy=0 "
				+ "WHERE PP_Order_Node_ID=? AND IsActive='Y'";
		org.compiere.util.DB.executeUpdate(bomSql, new Object[] { qtyPaperTotalScrap, ratePaperTotalScrap, nodeId },
				false, trxName);
	}

	private void syncPaperScrapStdToNodes(PO scrapStd, String trxName) {
		Integer routingNodeId = (Integer) scrapStd.get_Value("AD_Routing_Node_ID");
		String processDifficulty = (String) scrapStd.get_Value("ProcessDifficulty");
		BigDecimal difficultyFactor = getBDFromPO(po, "DifficultyFactor");
		BigDecimal stdBaseQty = getBDFromPO(po, "StdBaseQty");
		BigDecimal stdScrapRate = getBDFromPO(po, "StdScrapRate");
		if (routingNodeId == null || processDifficulty == null)
			return;

		String querySql = "SELECT ppon.PP_Order_Node_ID, ppon.PP_Order_ID, ppon.QtyColor, "
				+ "ppo.QtyEntered, NULLIF(owf.QtyBatchSize, 0) AS QtyBatchSize " + "FROM PP_Order_Node ppon "
				+ "JOIN PP_Order ppo ON ppo.PP_Order_ID = ppon.PP_Order_ID "
				+ "LEFT JOIN PP_Order_Workflow owf ON owf.PP_Order_Workflow_ID = ppon.PP_Order_Workflow_ID "
				+ "WHERE ppon.AD_Routing_Node_ID=? AND ppon.ScrapType=? AND ppon.IsActive='Y'";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Set<Integer> affectedOrderIds = new java.util.LinkedHashSet<>();
		try {
			pstmt = org.compiere.util.DB.prepareStatement(querySql, trxName);
			pstmt.setInt(1, routingNodeId);
			pstmt.setString(2, processDifficulty);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int nodeId = rs.getInt("PP_Order_Node_ID");
				int ppOrderId = rs.getInt("PP_Order_ID");
				BigDecimal qtyColor = rs.getBigDecimal("QtyColor");
				BigDecimal qtyEntered = rs.getBigDecimal("QtyEntered");
				BigDecimal qtyBatchSize = rs.getBigDecimal("QtyBatchSize");

				BigDecimal qtyPaperScrap = null;
				if (qtyColor != null && qtyEntered != null && qtyBatchSize != null && difficultyFactor != null
						&& stdBaseQty != null && stdScrapRate != null) {
					BigDecimal qtyPerBatch = qtyEntered.divide(qtyBatchSize, 6, java.math.RoundingMode.HALF_UP);
					BigDecimal scrapRateActual = stdScrapRate.divide(new BigDecimal("1000"), 10,
							java.math.RoundingMode.HALF_UP);
					qtyPaperScrap = stdBaseQty.add(scrapRateActual.multiply(qtyPerBatch)).multiply(qtyColor)
							.multiply(difficultyFactor).setScale(0, java.math.RoundingMode.HALF_UP);
				}

				String updateSql = "UPDATE PP_Order_Node SET "
						+ "ScrapFactor=?, QtyPaperNodeScrap=?, RatePaperNodeScrap=?, QtyPaperScrap=?, "
						+ "Updated=now(), UpdatedBy=0 " + "WHERE PP_Order_Node_ID=?";
				org.compiere.util.DB.executeUpdate(updateSql,
						new Object[] { difficultyFactor != null ? difficultyFactor.toPlainString() : null, stdBaseQty,
								stdScrapRate, qtyPaperScrap, nodeId },
						false, trxName);

				affectedOrderIds.add(ppOrderId);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "syncPaperScrapStdToNodes", e);
		} finally {
			org.compiere.util.DB.close(rs, pstmt);
		}

		for (int ppOrderId : affectedOrderIds) {
			recalculatePaperScrapChain(ppOrderId, null, trxName);
		}
	}

	private BigDecimal getBDFromPO(PO po, String columnName) {
		Object val = po.get_Value(columnName);
		if (val == null)
			return null;
		if (val instanceof BigDecimal)
			return (BigDecimal) val;
		if (val instanceof Integer)
			return new BigDecimal((Integer) val);
		if (val instanceof String) {
			String s = ((String) val).trim();
			if (s.isEmpty())
				return null;
			try {
				return new BigDecimal(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}