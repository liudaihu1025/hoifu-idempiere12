package org.libero.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.libero.form.vo.BOMLineVO;
import org.libero.form.vo.RoutingNodeVO;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderWorkflow;

public class PPOrderService {

	private static final CLogger log = CLogger.getCLogger(PPOrderService.class);

	private final PPOrderBOMService bomService = new PPOrderBOMService();
	private final PPOrderRoutingService routingService = new PPOrderRoutingService();

	/**
	 * 新建工单并发布（提交按钮）
	 * 
	 * @param ctx           上下文
	 * @param orgId         组织ID
	 * @param productId     产品ID
	 * @param docTypeId     工单类型（C_DocTypeTarget_ID）
	 * @param dateStart     计划开工日期
	 * @param datePromised  计划交货日期
	 * @param qtyOrdered    生产数量
	 * @param uomId         单位ID
	 * @param resourceId    生产线/车间（S_Resource_ID）
	 * @param warehouseId   仓库ID
	 * @param priorityRule  优先级（PriorityRule）
	 * @param orderLineId   关联销售订单行（C_OrderLine_ID，0=无）
	 * @param qtyBatchSize  拼版联数（QtyBatchSize）
	 * @param nRate         BOM比例（PP_Product_BOM.nRate）
	 * @param description   备注
	 * @param existingBOMId 已关联BOM模板ID（0=新建）
	 * @param existingWFId  已关联工艺路线ID（0=新建）
	 * @param bomLines      BOM物料列表
	 * @param routingNodes  工序列表
	 * @param createdByName 创建人姓名（用于描述字段）
	 * @return 已发布的工单
	 */
	public MPPOrder submitNewOrder(Properties ctx, int orgId, int productId, int docTypeId, Timestamp dateStart,
			Timestamp datePromised, BigDecimal qtyOrdered, int uomId, int resourceId, int warehouseId,
			String priorityRule, int orderLineId, BigDecimal qtyBatchSize, BigDecimal nRate, String description,
			int existingBOMId, int existingWFId, List<BOMLineVO> bomLines, List<RoutingNodeVO> routingNodes,
			String createdByName) {

		// ── 阶段一：创建/更新 BOM 模板和工艺路线模板，独立事务提交 ──────────────
		// MPPProductBOM.get() 使用 ImmutableIntPOCache，不读未提交记录，
		// 所以 BOM/Workflow 必须先 commit，explosion() 才能找到它们。
		int bomId;
		int workflowId;
		String phase1Trx = Trx.createTrxName("WO_PHASE1");
		Trx trx1 = Trx.get(phase1Trx, true);
		try {
			MProduct product = MProduct.get(ctx, productId);

			// 1a. 创建/更新 BOM 模板
			bomId = bomService.createOrUpdateBOMTemplate(ctx, phase1Trx, orgId, productId, uomId, dateStart, nRate,
					createdByName, existingBOMId, bomLines);

			// 1b. 创建/更新 工艺路线模板
			workflowId = routingService.createOrUpdateRoutingTemplate(ctx, phase1Trx, orgId, productId, dateStart,
					createdByName, existingWFId, routingNodes);

			// 1c. 确保产品已验证（explosion() 需要，ECN_DERIVED 可绕过，但设上更安全）
			product = new MProduct(ctx, productId, phase1Trx); // 用事务内版本
			if (!product.isVerified()) {
				product.setIsVerified(true);
				product.saveEx(phase1Trx);
			}

			trx1.commit();
			log.info("Phase1 committed: bomId=" + bomId + ", workflowId=" + workflowId);
		} catch (Exception e) {
			trx1.rollback();
			throw new AdempiereException("BOM/工艺路线创建失败: " + e.getMessage(), e);
		} finally {
			trx1.close();
		}

		// ── 阶段二：创建工单并发布 ────────────────────────────────────────────
		String phase2Trx = Trx.createTrxName("WO_PHASE2");
		Trx trx2 = Trx.get(phase2Trx, true);
		try {
			MPPOrder order = new MPPOrder(ctx, 0, phase2Trx);
			order.setAD_Org_ID(orgId);
			order.setM_Product_ID(productId);
			order.setC_DocTypeTarget_ID(docTypeId);
			order.setC_DocType_ID(docTypeId);
			order.setDateOrdered(new Timestamp(System.currentTimeMillis()));
			order.setDateStartSchedule(dateStart);
			order.setDateFinishSchedule(datePromised);
			order.setDatePromised(datePromised);
			order.setQtyOrdered(qtyOrdered);
			order.setQtyEntered(qtyOrdered);
			order.setC_UOM_ID(uomId);
			order.setS_Resource_ID(resourceId);
			if (warehouseId > 0)
				order.setM_Warehouse_ID(warehouseId);
			if (priorityRule != null && !priorityRule.isEmpty())
				order.setPriorityRule(priorityRule);
			if (orderLineId > 0)
				order.setC_OrderLine_ID(orderLineId);
			if (description != null && !description.isEmpty())
				order.setDescription(description);
			order.setYield(Env.ZERO);
			order.setM_AttributeSetInstance_ID(0);
			order.setLine(10);

			// BOM 和工艺路线（已提交，explosion() 可以找到）
			order.setPP_Product_BOM_ID(bomId);
			order.setAD_Workflow_ID(workflowId);

			// 标记为 ECN_DERIVED，跳过产品验证检查
			order.set_ValueOfColumn("CreationSource", "ECN_DERIVED");
			BigDecimal batchSize = (qtyBatchSize != null && qtyBatchSize.compareTo(BigDecimal.ZERO) > 0) ? qtyBatchSize
					: BigDecimal.ONE;
			order.setQtyBatchSize(batchSize);
			order.set_ValueOfColumn("Orderstatus", "Released");
			order.saveEx(phase2Trx);

			// 发布工单（改 DocStatus DR → IP）
			order.setDocAction(DocAction.ACTION_Prepare);
			if (!order.processIt(DocAction.ACTION_Prepare)) {
				throw new AdempiereException("工单发布失败: " + order.getProcessMsg());
			}
			order.saveEx(phase2Trx);

			// explosion() 已执行，PP_Order_Node 已存在，现在同步 BOMLine的工序关联，并且AD_Routing_Node.IsBatchCalculation='Y'（按批计算），则 × QtyBatchSize（拼版联数）
//			syncBOMLineRoutingNode(ctx, order.getPP_Order_ID(), bomLines, phase2Trx);

			// 计算并保存损耗字段（标准加工数取用户输入值，不重新计算）
			applyScrapCalculation(ctx, order.getPP_Order_ID(), bomLines, routingNodes, qtyOrdered, batchSize, phase2Trx,
					false);

			trx2.commit();
			log.info("工单已发布: " + order.getDocumentNo());
			return order;
		} catch (Exception e) {
			trx2.rollback();
			log.log(Level.SEVERE, "submitNewOrder phase2 error", e);
			throw new AdempiereException("工单提交失败: " + e.getMessage(), e);
		} finally {
			trx2.close();
		}
	}

	/**
	 * 修改打样/研发工单的BOM和工序（已发布工单修改入口）
	 * 
	 * @param ctx          上下文
	 * @param ppOrderId    工单ID
	 * @param bomLines     最新BOM物料列表
	 * @param routingNodes 最新工序列表
	 */
	public void updateOrderBOMAndRouting(Properties ctx, int ppOrderId, List<BOMLineVO> bomLines,
			List<RoutingNodeVO> routingNodes) {

		MPPOrder order = new MPPOrder(ctx, ppOrderId, null);
		if (order.getPP_Order_ID() <= 0)
			throw new AdempiereException("工单不存在: " + ppOrderId);

		int orgId = order.getAD_Org_ID();
		int productId = order.getM_Product_ID();
		int uomId = order.getC_UOM_ID();
		Timestamp dateStart = order.getDateStartSchedule();
		int existingBOMId = order.getPP_Product_BOM_ID();
		int existingWFId = order.getAD_Workflow_ID();
		String createdByName = org.compiere.model.MUser.get(ctx, Env.getAD_User_ID(ctx)).getName();

		// ── 阶段一：更新 BOM 模板和工艺路线模板（独立事务，先提交）──────────────
		String phase1Trx = Trx.createTrxName("WO_UPD_P1");
		Trx trx1 = Trx.get(phase1Trx, true);
		int newBOMId, newWFId;
		try {
			newBOMId = bomService.createOrUpdateBOMTemplate(ctx, phase1Trx, orgId, productId, uomId, dateStart,
					BigDecimal.ZERO, createdByName, existingBOMId, bomLines);
			newWFId = routingService.createOrUpdateRoutingTemplate(ctx, phase1Trx, orgId, productId, dateStart,
					createdByName, existingWFId, routingNodes);
			trx1.commit();
		} catch (Exception e) {
			trx1.rollback();
			throw new AdempiereException("BOM/工艺路线更新失败: " + e.getMessage(), e);
		} finally {
			trx1.close();
		}

		// ── 阶段二：更新工单关联并重新展开 ──────────────────────────────────────
		String phase2Trx = Trx.createTrxName("WO_UPD_P2");
		Trx trx2 = Trx.get(phase2Trx, true);
		try {
			MPPOrder o = new MPPOrder(ctx, ppOrderId, phase2Trx);
			o.setPP_Product_BOM_ID(newBOMId);
			o.setAD_Workflow_ID(newWFId);
			o.set_ValueOfColumn("CreationSource", "ECN_DERIVED");
			// 确保 QtyBatchSize 非零，避免 beforeSave 中 MWorkflow.get() NPE
			if (o.getQtyBatchSize() == null || o.getQtyBatchSize().compareTo(BigDecimal.ZERO) == 0)
				o.setQtyBatchSize(BigDecimal.ONE);
			o.saveEx(phase2Trx);

			// 重新同步 BOM 和工序到工单
			// 修复：直接增删 BOMLine 和 Node，不动 PP_Order_Workflow

			// 1. 获取 PP_Order_BOM 头（工单BOM头，不是模板）
			MPPOrderBOM orderBOM = new Query(ctx, MPPOrderBOM.Table_Name, "PP_Order_ID=?", phase2Trx)
					.setParameters(ppOrderId).first();
			if (orderBOM == null)
				throw new AdempiereException("找不到工单BOM头: PP_Order_ID=" + ppOrderId);

			// 2. 处理 BOM 物料行
			for (BOMLineVO vo : bomLines) {
				if (vo.isDeleted) {
					if (vo.ppOrderBOMLineId > 0) {
						MPPOrderBOMLine obl = new MPPOrderBOMLine(ctx, vo.ppOrderBOMLineId, phase2Trx);
						obl.deleteEx(true);
					}
				} else if (vo.ppOrderBOMLineId <= 0 && vo.productId > 0) {
					// 2. 新建 BOM 行时补全所有非空字段
					MPPOrderBOMLine obl = new MPPOrderBOMLine(ctx, 0, phase2Trx);
					obl.setPP_Order_ID(ppOrderId);
					obl.setPP_Order_BOM_ID(orderBOM.getPP_Order_BOM_ID());
					obl.setM_Product_ID(vo.productId);
					obl.setC_UOM_ID(vo.uomId);
					obl.setM_Warehouse_ID(order.getM_Warehouse_ID());
					obl.setAD_Org_ID(order.getAD_Org_ID());
					obl.setComponentType(MPPOrderBOMLine.COMPONENTTYPE_Component); // "CO"
					obl.setQtyBOM(vo.qtyBOM != null ? vo.qtyBOM : BigDecimal.ONE);
					obl.setQtyBatch(BigDecimal.ZERO); // ← 修复 qtybatch NOT NULL
					obl.setQtyRequiered(vo.qtyRequiered != null ? vo.qtyRequiered : BigDecimal.ZERO);
					obl.setQtyEntered(vo.qtyBOM != null ? vo.qtyBOM : BigDecimal.ONE);
					obl.setValidFrom(new java.sql.Timestamp(System.currentTimeMillis())); // ← 建议设置
					obl.setIsActive(true);
					obl.saveEx(phase2Trx);
					vo.ppOrderBOMLineId = obl.getPP_Order_BOMLine_ID(); // 回填ID
				}
			}

			// 3. 获取 PP_Order_Workflow（工单工艺路线头）
			MPPOrderWorkflow orderWF = new Query(ctx, MPPOrderWorkflow.Table_Name, "PP_Order_ID=?", phase2Trx)
					.setParameters(ppOrderId).first();

			// 4. 处理工序行
			if (orderWF != null) {
				for (RoutingNodeVO vo : routingNodes) {
					if (vo.isDeleted) {
						if (vo.ppOrderNodeId > 0) {
							MPPOrderNode node = new MPPOrderNode(ctx, vo.ppOrderNodeId, phase2Trx);
							node.deleteEx(true);
						}
					} else if (vo.ppOrderNodeId <= 0 && vo.adRoutingNodeId > 0) {
						// 新增行
						MPPOrderNode node = new MPPOrderNode(ctx, 0, phase2Trx);
						node.setPP_Order_Workflow_ID(orderWF.getPP_Order_Workflow_ID());
						node.setPP_Order_ID(ppOrderId);
						node.setValue(vo.routingNodeValue);
						node.setName(vo.routingNodeName);
						node.set_ValueOfColumn("AD_Routing_Node_ID", vo.adRoutingNodeId);
						node.setAD_Org_ID(o.getAD_Org_ID());
						node.setIsActive(true);
						node.saveEx(phase2Trx);
						vo.ppOrderNodeId = node.getPP_Order_Node_ID();
					}
				}
			}

			// 5. 同步 BOMLine 的所属工序
//			syncBOMLineRoutingNode(ctx, ppOrderId, bomLines, phase2Trx);
			// 计算并保存损耗字段
			applyScrapCalculation(ctx, ppOrderId, bomLines, routingNodes, order.getQtyOrdered(),
					order.getQtyBatchSize(), phase2Trx, false);
			trx2.commit();
		} catch (Exception e) {
			trx2.rollback();
			throw new AdempiereException("工单修改失败: " + e.getMessage(), e);
		} finally {
			trx2.close();
		}
	}

	/**
	 * 按工单号查询
	 */
	public MPPOrder findByDocumentNo(Properties ctx, String documentNo) {
		return new Query(ctx, MPPOrder.Table_Name, "DocumentNo=?", null).setParameters(documentNo).setClient_ID()
				.first();
	}

	/**
	 * 同步 PP_Order_BOMLine.PP_Order_Node_ID 根据 BOMLineVO.routingNodeId
	 * (AD_Routing_Node_ID) 匹配 PP_Order_Node， 将对应的 PP_Order_Node_ID 写入
	 * PP_Order_BOMLine。 新增工单（explosion 后）和修改工单后均需调用。
	 */
	private void syncBOMLineRoutingNode(Properties ctx, int ppOrderId, List<BOMLineVO> bomLines, String trxName) {
		for (BOMLineVO vo : bomLines) {
			if (vo.isDeleted || vo.productId <= 0 || vo.routingNodeId <= 0)
				continue;

			// 1. 通过 AD_Routing_Node_ID 找到对应的 PP_Order_Node_ID
			int ppOrderNodeId = DB.getSQLValue(trxName,
					"SELECT PP_Order_Node_ID FROM PP_Order_Node "
							+ "WHERE PP_Order_ID=? AND AD_Routing_Node_ID=? AND IsActive='Y'",
					ppOrderId, vo.routingNodeId);
			if (ppOrderNodeId <= 0)
				continue;

			// 2. 找到对应的 PP_Order_BOMLine（按产品匹配）
			MPPOrderBOMLine obl = MPPOrderBOMLine.forM_Product_ID(ctx, ppOrderId, vo.productId, trxName);
			if (obl == null)
				continue;

			// 3. 写入 PP_Order_Node_ID（自定义列）
			obl.set_ValueOfColumn("PP_Order_Node_ID", ppOrderNodeId);
			obl.saveEx(trxName);

			// 4. 回填 VO，供后续 loadOrder 使用
			vo.ppOrderNodeId = ppOrderNodeId;
		}
	}

	/**
	 * 1.损耗计算：计算并保存工序的纸张放损数、累加放损数、累计纸张放损率， 以及主物料的累计纸张放损数。 标准加工数（QtyRequiered）取 VO 中用户输入的值，不重新计算。
	 * 2.更新BOM物料中的所属工序 PP_Order_Node_ID
	 * 
	 * @param calcQtyRequiered true=同时计算标准加工数（点击"损耗计算"按钮时）， false=仅计算放损字段（提交时）
	 */
	public void applyScrapCalculation(Properties ctx, int ppOrderId, List<BOMLineVO> bomLines,
			List<RoutingNodeVO> routingNodes, BigDecimal qtyOrdered, BigDecimal qtyBatchSize, String trxName,
			boolean calcQtyRequiered) {

		if (qtyOrdered == null || qtyOrdered.compareTo(BigDecimal.ZERO) <= 0)
			return;
		if (qtyBatchSize == null || qtyBatchSize.compareTo(BigDecimal.ZERO) <= 0)
			qtyBatchSize = BigDecimal.ONE;

		// ── Step1：计算每道工序的纸张放损数 ──────────────────────────────────
		List<RoutingNodeVO> activeNodes = new java.util.ArrayList<>();
		for (RoutingNodeVO vo : routingNodes) {
			if (!vo.isDeleted && vo.adRoutingNodeId > 0)
				activeNodes.add(vo);
		}

		for (RoutingNodeVO vo : activeNodes) {
			BigDecimal paperScrap = BigDecimal.ZERO;
			if (vo.adRoutingNodeId > 0 && vo.processdifficulty != null && !vo.processdifficulty.isEmpty()) {
				// 查询放损配置表
				java.sql.PreparedStatement ps = null;
				java.sql.ResultSet rs = null;
				try {
					ps = DB.prepareStatement("SELECT ps.stdbaseqty, ps.stdscraprate, ps.difficultyfactor "
							+ "FROM c_paperscrapstd ps " + "WHERE ps.operationclass_ID = ("
							+ "    SELECT rn.operationclass_ID FROM AD_Routing_Node rn "
							+ "    WHERE rn.AD_Routing_Node_ID=? AND rn.IsActive='Y') "
							+ "AND ps.processdifficulty=? AND ps.IsActive='Y' AND ps.AD_Client_ID=? "
							+ "ORDER BY ps.c_paperscrapstd_id FETCH FIRST 1 ROWS ONLY", trxName);
					ps.setInt(1, vo.adRoutingNodeId);
					ps.setString(2, vo.processdifficulty);
					ps.setInt(3, Env.getAD_Client_ID(ctx));
					rs = ps.executeQuery();
					if (rs.next()) {
						BigDecimal stdbaseqty = rs.getBigDecimal(1);
						BigDecimal stdscraprate = rs.getBigDecimal(2);
						BigDecimal df = rs.getBigDecimal(3);
						vo.difficultyfactor = df != null ? df : BigDecimal.ONE;
						vo.stdbaseqty = stdbaseqty != null ? stdbaseqty : BigDecimal.ZERO;
						vo.stdscraprate = stdscraprate != null ? stdscraprate : BigDecimal.ZERO;

						BigDecimal colorCount = (vo.colorCount != null && vo.colorCount.compareTo(BigDecimal.ZERO) > 0)
								? vo.colorCount
								: BigDecimal.ONE;

						// 纸张放损数 = (stdbaseqty + stdscraprate * 工单数量/联数) * 色数 * difficultyfactor
						BigDecimal base = stdbaseqty != null ? stdbaseqty : BigDecimal.ZERO;
						BigDecimal rate = stdscraprate != null ? stdscraprate.divide(new BigDecimal("1000"))
								: BigDecimal.ZERO;
						paperScrap = base
								.add(rate.multiply(qtyOrdered).divide(qtyBatchSize, 6, java.math.RoundingMode.HALF_UP))
								.multiply(colorCount).multiply(vo.difficultyfactor);
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "applyScrapCalculation paperScrap error", e);
				} finally {
					DB.close(rs, ps);
				}
			}
			vo.qtyPaperScrap = paperScrap.setScale(0, java.math.RoundingMode.CEILING);
		}

		// ── Step2：总累加纸张放损数 ───────────────────────────────────────────
		BigDecimal totalPaperScrap = BigDecimal.ZERO;
		for (RoutingNodeVO vo : activeNodes)
			totalPaperScrap = totalPaperScrap.add(vo.qtyPaperScrap);

		// ── Step3：计算每道工序的累加放损数、标准加工数（可选）、累计放损率 ──
		BigDecimal cumScrap = BigDecimal.ZERO;
		for (RoutingNodeVO vo : activeNodes) {
			cumScrap = cumScrap.add(vo.qtyPaperScrap);
			vo.qtyPaperTotalScrap = cumScrap;

			if (calcQtyRequiered) {
				int isBatch = DB.getSQLValue(trxName,
						"SELECT CASE WHEN IsBatchCalculation='Y' THEN 1 ELSE 0 END "
								+ "FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'",
						vo.adRoutingNodeId);
				if (isBatch == 1) {
					// 按批计算：工单数量 + (总累计放损 - 工序累计放损) * 联数
					vo.qtyRequiered = qtyOrdered
							.add(totalPaperScrap.subtract(vo.qtyPaperTotalScrap).multiply(qtyBatchSize))
							.setScale(0, java.math.RoundingMode.CEILING);
				} else {
					// 非按批计算：工单数量/联数 + 总累计放损 - 工序累计放损
					vo.qtyRequiered = qtyOrdered.divide(qtyBatchSize, 6, java.math.RoundingMode.CEILING)
							.add(totalPaperScrap).subtract(vo.qtyPaperTotalScrap)
							.setScale(0, java.math.RoundingMode.CEILING);
				}
			}

			// 累计纸张放损率 = 累加放损数 / (累加放损数 + 标准加工数)
			BigDecimal qtyReq = vo.qtyRequiered != null ? vo.qtyRequiered : BigDecimal.ZERO;
			BigDecimal denominator = vo.qtyPaperTotalScrap.add(qtyReq);
			vo.ratePaperTotalScrap = denominator.compareTo(BigDecimal.ZERO) > 0
					? vo.qtyPaperTotalScrap.divide(denominator, 6, java.math.RoundingMode.HALF_UP)
					: BigDecimal.ZERO;
		}

		// ── Step4：计算 BOM 需求数量 ──────────────────────────────────────────
		// 构建 routingNodeId -> RoutingNodeVO 的映射
		java.util.Map<Integer, RoutingNodeVO> nodeMap = new java.util.LinkedHashMap<>();
		for (RoutingNodeVO vo : activeNodes)
			nodeMap.put(vo.adRoutingNodeId, vo);

		boolean isFirstBOM = true;
		for (BOMLineVO bvo : bomLines) {
			if (bvo.isDeleted || bvo.productId <= 0)
				continue;
			RoutingNodeVO rvo = nodeMap.get(bvo.routingNodeId);
			if (rvo == null)
				continue;

			// 根据物料单位的标准精度设置小数位，uomId无效时默认4位
			int precision = bvo.uomId > 0 ? MUOM.getPrecision(ctx, bvo.uomId) : 4;

			if (isFirstBOM) {
				// 主物料：需求数量 = 标准加工数 + 纸张放损数
				bvo.qtyRequiered = rvo.qtyRequiered.add(rvo.qtyPaperScrap).setScale(precision,
						java.math.RoundingMode.CEILING);
				bvo.qtyPaperTotalScrap = totalPaperScrap;
				isFirstBOM = false;
			} else {
				// 其他物料
				int isBatch = DB.getSQLValue(trxName,
						"SELECT CASE WHEN IsBatchCalculation='Y' THEN 1 ELSE 0 END "
								+ "FROM AD_Routing_Node WHERE AD_Routing_Node_ID=? AND IsActive='Y'",
						rvo.adRoutingNodeId);
				BigDecimal qty;
				if (isBatch == 1) {
					// 按批计算：(标准加工数 + 纸张放损数 * 联数) * BOM数量
					qty = rvo.qtyRequiered.add(rvo.qtyPaperScrap.multiply(qtyBatchSize)).multiply(bvo.qtyBOM);
				} else {
					// 非按批计算：(标准加工数 + 纸张放损数) * 联数 * BOM数量
					qty = rvo.qtyRequiered.add(rvo.qtyPaperScrap).multiply(qtyBatchSize).multiply(bvo.qtyBOM);
				}
				bvo.qtyRequiered = qty.setScale(precision, java.math.RoundingMode.CEILING);
			}
		}

		// ── Step5：若有 ppOrderId（已持久化），将结果写入数据库 ───────────────
		if (ppOrderId > 0 && trxName != null) {
			// 保存工序字段
			for (RoutingNodeVO vo : activeNodes) {
				int nodeId = vo.ppOrderNodeId;
				// explosion() 后 ppOrderNodeId 可能为 0，通过 AD_Routing_Node_ID 补查
				if (nodeId <= 0 && vo.adRoutingNodeId > 0) {
					nodeId = DB.getSQLValue(trxName,
							"SELECT PP_Order_Node_ID FROM PP_Order_Node "
									+ "WHERE PP_Order_ID=? AND AD_Routing_Node_ID=? AND IsActive='Y'",
							ppOrderId, vo.adRoutingNodeId);
					if (nodeId > 0)
						vo.ppOrderNodeId = nodeId; // 回填 VO，供后续使用
				}
				if (nodeId <= 0)
					continue;

				MPPOrderNode node = new MPPOrderNode(ctx, nodeId, trxName);
				node.set_ValueOfColumn("QtyPaperScrap", vo.qtyPaperScrap.setScale(0, RoundingMode.CEILING).intValue());
				node.set_ValueOfColumn("QtyPaperTotalScrap",
						vo.qtyPaperTotalScrap.setScale(0, RoundingMode.CEILING).intValue());
				node.set_ValueOfColumn("RatePaperTotalScrap", vo.ratePaperTotalScrap);
				node.set_ValueOfColumn("QtyColor",
						(Objects.nonNull(vo.colorCount) && vo.colorCount.compareTo(BigDecimal.ZERO) > 0) ? vo.colorCount
								: BigDecimal.ONE);
				if (Objects.nonNull(vo.processdifficulty) && vo.processdifficulty.length() > 0) {
					node.set_ValueOfColumn("ScrapType", vo.processdifficulty);
					node.set_ValueOfColumn("ScrapFactor", vo.difficultyfactor != null ? vo.difficultyfactor.toString() : null);
					node.set_ValueOfColumn("QtyPaperNodeScrap", vo.stdbaseqty.setScale(0, RoundingMode.CEILING));
					node.set_ValueOfColumn("RatePaperNodeScrap", vo.stdscraprate);
				}
				if (Objects.nonNull(vo.qtyRequiered) && vo.qtyRequiered.compareTo(BigDecimal.ZERO) > 0) {
					node.setQtyRequiered(vo.qtyRequiered);
				}
				if (vo.effectName != null && !vo.effectName.isEmpty()) {
					node.set_ValueOfColumn("EffectName", vo.effectName);
				}
				node.saveEx(trxName);
			}

			// ── 保存 BOM 行需求数量（按顺序行号匹配）──────────────────────────────
			// 1. 从数据库按 Line 升序取所有 BOM 行
			List<MPPOrderBOMLine> dbLines = new Query(ctx, MPPOrderBOMLine.Table_Name, "PP_Order_ID=? AND IsActive='Y'",
					trxName).setParameters(ppOrderId).setOrderBy(MPPOrderBOMLine.COLUMNNAME_Line).list();

			// 2. 过滤出未删除且有效的 VO 行（顺序与 dbLines 对应）
			List<BOMLineVO> activeBOMLines = bomLines.stream().filter(b -> !b.isDeleted && b.productId > 0)
					.collect(java.util.stream.Collectors.toList());

			// 3. 按顺序逐行匹配并保存
			boolean isFirstBOMLine = true;
			for (int i = 0; i < activeBOMLines.size() && i < dbLines.size(); i++) {
				BOMLineVO bvo = activeBOMLines.get(i);
				MPPOrderBOMLine obl = dbLines.get(i);

				// 同步 PP_Order_Node_ID
				if (bvo.routingNodeId > 0) {
					int ppOrderNodeId = bvo.ppOrderNodeId;
					if (ppOrderNodeId <= 0) {
						ppOrderNodeId = DB.getSQLValue(trxName,
								"SELECT PP_Order_Node_ID FROM PP_Order_Node "
										+ "WHERE PP_Order_ID=? AND AD_Routing_Node_ID=? AND IsActive='Y'",
								ppOrderId, bvo.routingNodeId);
					}
					if (ppOrderNodeId > 0) {
						obl.set_ValueOfColumn("PP_Order_Node_ID", ppOrderNodeId);
						bvo.ppOrderNodeId = ppOrderNodeId; // 回填 VO
					}
				}

				if (bvo.qtyRequiered != null)
					obl.setQtyRequiered(bvo.qtyRequiered);
				if (isFirstBOMLine) {
					// 主物料额外保存 QtyPaperTotalScrap
					obl.set_ValueOfColumn("QtyPaperTotalScrap", totalPaperScrap.intValue());
					isFirstBOMLine = false;
				}
				if (bvo.effectName != null && !bvo.effectName.isEmpty()) {
					obl.set_ValueOfColumn("EffectName", bvo.effectName);
				}
				obl.saveEx(trxName);
			}
		}
	}
}