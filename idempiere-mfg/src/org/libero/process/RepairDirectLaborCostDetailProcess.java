package org.libero.process;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.engines.CostDimension;
import org.adempiere.model.engines.CostEngine;
import org.compiere.acct.DocManager;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCostDetail;
import org.compiere.model.MCostElement;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.libero.model.MPPCostCollector;
import org.libero.tables.X_C_WorkTeamMember;  
  
/**
 * 一次性历史数据维护流程：批量补建/更新指定月份内所有历史报工单（PP_Cost_Collector, CostCollectorType='160'， 即
 * isActivityControl()）的班组直接人工 M_CostDetail，并对有变化的单据重新过账。
 * 
 * 容错策略：单张单据的过账失败（或整体处理异常）只记入失败清单，不中断整个流程， 继续处理该月份下的其它单据（"单张失败不影响其它单据"）。
 */  
@org.adempiere.base.annotation.Process  
public class RepairDirectLaborCostDetailProcess extends SvrProcess {  
  
	/** 参数：待处理的月份（前端只需传当月任意一天，通常是月初，如 2025-07-01） */
	private Timestamp p_Month = null;
  
	/** 员工当月工时上限（小时），对应 HF_MONTHLY_WORK_HOURS 配置，默认 260 */  
	private BigDecimal p_MonthlyLimit = new BigDecimal("260");  
  
	/** 复用 CostEngine 的公共方法（isActivityControlElement / getResourceActualCostRate） */
	private final CostEngine costEngine = new CostEngine();

	@Override  
	protected void prepare() {  
		for (ProcessInfoParameter para : getParameter()) {  
			String name = para.getParameterName();  
			if (para.getParameter() == null)  
				continue;  
			if ("Month".equals(name)) {
				p_Month = (Timestamp) para.getParameter();
			} else if ("MonthlyLimit".equals(name)) {  
				p_MonthlyLimit = (BigDecimal) para.getParameter();  
			}  
		}  
	}  
  
	@Override  
	protected String doIt() throws Exception {  
		if (p_Month == null) {
			throw new AdempiereException("请指定要处理的月份（Month 参数）");
		}  
  
		// 把前端传入的"月份内任意一天"归一化为：月初(00:00:00) 和 次月月初(00:00:00)，
		// 用半开区间 [monthStart, nextMonthStart) 查询，完整覆盖整月数据。
		Calendar cal = Calendar.getInstance();
		cal.setTime(p_Month);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Timestamp monthStart = new Timestamp(cal.getTimeInMillis());

		cal.add(Calendar.MONTH, 1);
		Timestamp nextMonthStart = new Timestamp(cal.getTimeInMillis());

		// 按月份区间批量查出符合条件的报工单：生产报工单(ActivityControl) + 已完成(CO)
		String whereClause = "CostCollectorType=? AND DocStatus='CO' AND MovementDate>=? AND MovementDate<?";
		List<MPPCostCollector> collectors = new Query(getCtx(), MPPCostCollector.Table_Name, whereClause, get_TrxName())
				.setParameters(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl, monthStart, nextMonthStart)
				.setOrderBy("MovementDate, PP_Cost_Collector_ID").list();

		int totalCollectors = collectors.size();
		int processedCollectors = 0; // 成功进入直接人工补建/更新逻辑的报工单数
		int skippedCollectors = 0; // 因为没有绑定班组等原因被跳过的报工单数
		int[] laborStatTotal = new int[3]; // 汇总：新建/更新/跳过的 M_CostDetail 条数（跨所有单据累计）

		// 过账相关计数：成功 / 跳过(无变化) / 失败
		int repostSuccess = 0;
		int repostSkippedNoChange = 0;
		int repostFailed = 0;
		List<Integer> repostFailedIds = new ArrayList<Integer>();
		// 单据处理本身（M_CostDetail 生成阶段）异常的清单，同样不中断整体流程
		List<Integer> processFailedIds = new ArrayList<Integer>();

		for (MPPCostCollector cc : collectors) {
			// 双重保险：再次确认类型和状态（理论上 SQL 已经过滤过，这里防御性判断，避免脏数据）
			if (!cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl)
					|| !"CO".equals(cc.getDocStatus())) {
				skippedCollectors++;
				continue;
			}

			int workTeamId = cc.get_ValueAsInt("C_WorkTeam_ID");
			if (workTeamId <= 0) {
				log.info("PP_Cost_Collector_ID=" + cc.get_ID() + " 没有绑定班组，跳过");
				skippedCollectors++;
				continue;
			}

			int[] laborStat = new int[3]; // 本张单据自己的新建/更新/跳过统计

			// 单张单据处理异常（包括其内部触发的过账异常）不再向外抛出中断整个流程，
			// 只记录失败清单，继续处理下一张单据。
			try {
				boolean changed = repairDirectLaborCostDetail(cc, workTeamId, laborStat);

				laborStatTotal[0] += laborStat[0];
				laborStatTotal[1] += laborStat[1];
				laborStatTotal[2] += laborStat[2];
				processedCollectors++;
  
				if (!changed) {
					// 本张单据没有任何 M_CostDetail 新建/更新，不需要重新过账
					repostSkippedNoChange++;
					continue;
				}
  
				String postResult = repost(cc);
				if (postResult != null && postResult.trim().length() > 0) {
					repostFailed++;
					repostFailedIds.add(cc.get_ID());
					log.warning("PP_Cost_Collector_ID=" + cc.get_ID() + " 重新过账失败：" + postResult);
				} else {
					repostSuccess++;
				}
			} catch (Exception e) {
				// 无论是 M_CostDetail 生成阶段还是过账阶段抛出的异常，都只记录、不中断，继续下一张
				processFailedIds.add(cc.get_ID());
				log.warning("PP_Cost_Collector_ID=" + cc.get_ID() + " 处理异常：" + e.getMessage());
			}
		}  
  
		StringBuilder result = new StringBuilder();
		result.append("月份[").append(monthStart).append(" ~ ").append(nextMonthStart).append(")：").append("共找到报工单=")
				.append(totalCollectors).append("张，").append("处理成功=").append(processedCollectors).append("张，")
				.append("跳过=").append(skippedCollectors).append("张；").append("直接人工M_CostDetail汇总[新建=")
				.append(laborStatTotal[0]).append(",更新=").append(laborStatTotal[1]).append(",跳过=")
				.append(laborStatTotal[2]).append("]；").append("重新过账[成功=").append(repostSuccess).append(",跳过(无变化)=")
				.append(repostSkippedNoChange).append(",失败=").append(repostFailed).append("]");
		if (!repostFailedIds.isEmpty()) {
			result.append("；过账失败单据ID=").append(repostFailedIds);
		}
		if (!processFailedIds.isEmpty()) {
			result.append("；处理异常单据ID=").append(processFailedIds);
		}
		return result.toString();
	}
  
	// =========================================================================
	// 班组直接人工分录 —— 对照你新增的 createDirectLaborCostDetails(cc)
	// 返回值：本张单据是否产生了任何 M_CostDetail 新建/更新（true=有变化，需要重新过账）
	// =========================================================================
	private boolean repairDirectLaborCostDetail(MPPCostCollector cc, int workTeamId, int[] stat) {
		Timestamp movementDate = cc.getMovementDate();
		if (movementDate == null) {
			log.warning("PP_Cost_Collector_ID=" + cc.get_ID() + " 缺少 MovementDate，跳过直接人工部分");
			return false;
		}  
  
		// 只取"直接人工=Y"的班组成员
		List<X_C_WorkTeamMember> members = new Query(cc.getCtx(), X_C_WorkTeamMember.Table_Name,
				"C_WorkTeam_ID=? AND IsDirectLabor='Y' AND IsActive='Y'", get_TrxName())  
				.setParameters(workTeamId)  
				.setOrderBy("AD_User_ID")
				.list();  
		if (members.isEmpty())
			return false;

		BigDecimal currentDuration = cc.getDurationReal();
		if (currentDuration == null)
			currentDuration = BigDecimal.ZERO;
  
		// 本张单据是否产生了任何实际的新建/更新（不含"跳过"计数）
		boolean anyChange = false;
  
		for (X_C_WorkTeamMember member : members) {  
			int userId = member.getAD_User_ID();  
			if (userId <= 0) {  
				stat[2]++;
				continue;  
			}  
  
			// 人员 -> 资源
			int resourceId = DB.getSQLValueEx(get_TrxName(),  
					"SELECT S_Resource_ID FROM S_Resource WHERE AD_User_ID=? AND IsActive='Y' LIMIT 1", userId);
			if (resourceId <= 0) {  
				log.warning("PP_Cost_Collector_ID=" + cc.get_ID() + " AD_User_ID=" + userId + " 未绑定人力资源，跳过直接人工分录");
				stat[2]++;
				continue;  
			}  

			// 资源 -> 产品
			final MProduct product = MProduct.forS_Resource_ID(cc.getCtx(), resourceId, get_TrxName());
			if (product == null) {  
				log.warning("PP_Cost_Collector_ID=" + cc.get_ID() + " AD_User_ID=" + userId + " 资源没有关联产品，跳过直接人工分录");
				stat[2]++;
				continue;  
			}  
  
			// ===== 截止当前报工单报工那一刻，统计该员工当月已报工时长 =====
			BigDecimal reportedHours = getReportedHoursBeforeThisCollector(cc, userId, movementDate);
			BigDecimal remaining = p_MonthlyLimit.subtract(reportedHours);  
			if (remaining.compareTo(BigDecimal.ZERO) < 0)  
				remaining = BigDecimal.ZERO;  
  
			BigDecimal creditableQty = currentDuration.compareTo(remaining) > 0 ? remaining : currentDuration;  
  
			if (creditableQty.compareTo(currentDuration) < 0) {  
				log.warning("PP_Cost_Collector_ID=" + cc.get_ID() + " @" + MUser.getNameOfUser(userId)
						+ "(AD_User_ID=" + userId + ") 已报工" + reportedHours + "小时，当前报工" + currentDuration  
						+ "小时，当月累计不可超过" + p_MonthlyLimit + "小时，多余"  
						+ currentDuration.subtract(creditableQty) + "小时不计入");  
			}  
  
			if (creditableQty.signum() <= 0) {  
				stat[2]++;
				continue; // 本月额度已用完，不生成分录
			}  
  
			// ===== 账套×成本要素动态遍历，不再硬编码要素ID =====
			for (MAcctSchema as : getAcctSchema(cc)) {
				for (MCostElement element : getCostElements(cc.getCtx(), product, as)) {
					if (!CostEngine.isActivityControlElement(element))
						continue;
  
					final CostDimension d = new CostDimension(product, as, as.getM_CostType_ID(), cc.getAD_Org_ID(),
							product.getM_AttributeSetInstance_ID(), element.getM_CostElement_ID());
					final BigDecimal price = costEngine.getResourceActualCostRate(cc, resourceId, d, cc.get_TrxName());
					BigDecimal costs = price.multiply(creditableQty);
					if (costs.scale() > as.getCostingPrecision())
						costs = costs.setScale(as.getCostingPrecision(), RoundingMode.HALF_UP);
  
					BigDecimal negCosts = costs.negate();
					BigDecimal negQty = creditableQty.negate();
  
					int before0 = stat[0], before1 = stat[1];
					upsertCostDetail(cc, as, element.getM_CostElement_ID(), d.getM_Product_ID(),
							product.getM_AttributeSetInstance_ID(), negCosts, negQty,
							"直接人工-" + MUser.getNameOfUser(userId) + "（历史数据补建）", stat);
					// 只要这次调用产生了新建或更新（stat[0]/stat[1] 增加了），就标记本单据"有变化"
					if (stat[0] > before0 || stat[1] > before1)
						anyChange = true;
				}  
			}  
		}
		return anyChange;
	}

	/** 对该报工单重新过账：先删除旧 Fact_Acct，再按最新 M_CostDetail 重新生成分录 */
	private String repost(MPPCostCollector cc) {
		String sql = "SELECT * FROM PP_Cost_Collector WHERE PP_Cost_Collector_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, cc.get_ID());
			rs = pstmt.executeQuery();
			if (!rs.next())
				return "找不到单据 PP_Cost_Collector_ID=" + cc.get_ID();

			MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(getCtx(), cc.getAD_Client_ID());
			// force=true, repost=true：强制重新过账，先删除旧 Fact_Acct 再生成新的
			return DocManager.postDocument(ass, MPPCostCollector.Table_ID, rs, true, true, get_TrxName());
		} catch (Exception e) {
			return "重新过账异常：" + e.getMessage();
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * 幂等新建/更新一条 M_CostDetail：按
	 * PP_Cost_Collector_ID+C_AcctSchema_ID+M_CostElement_ID+M_Product_ID
	 * 精确查询是否已存在，存在则 Delta 更新，不存在才新建。
	 */
	private void upsertCostDetail(MPPCostCollector cc, MAcctSchema as, int costElementId, int productId,
			int attributeSetInstanceId, BigDecimal negCosts, BigDecimal negQty, String description, int[] stat) {

		MCostDetail cd = findExistingCostDetail(cc.get_ID(), as.getC_AcctSchema_ID(), costElementId, productId);
  
		if (cd == null) {
			cd = new MCostDetail(as, cc.getAD_Org_ID(), productId, attributeSetInstanceId, costElementId, negCosts,
					negQty, description, new Timestamp(System.currentTimeMillis()), 0, get_TrxName());
			cd.setPP_Cost_Collector_ID(cc.getPP_Cost_Collector_ID());
			cd.saveEx();
			stat[0]++; // created
			log.info("PP_Cost_Collector_ID=" + cc.get_ID() + " M_CostElement_ID=" + costElementId + " M_Product_ID="
					+ productId + " 新建 M_CostDetail_ID=" + cd.get_ID() + " Amt=" + negCosts + " Qty=" + negQty);
			if (!cd.isProcessed()) {  
				cd.process();  
				cd.saveEx();  
			}  
			return;
		}

		// 已存在，走 Delta 更新逻辑
		if (cd.isProcessed()) {
			cd.setDeltaAmt(negCosts.subtract(cd.getAmt()));
			cd.setDeltaQty(negQty.subtract(cd.getQty()));
		} else {
			cd.setDeltaAmt(BigDecimal.ZERO);
			cd.setDeltaQty(BigDecimal.ZERO);
			cd.setAmt(negCosts);
			cd.setQty(negQty);
		}  
  
		if (cd.isDelta()) {
			cd.setProcessed(false);
			cd.setAmt(negCosts);
			cd.setQty(negQty);
			cd.saveEx();
			stat[1]++; // updated
			log.info("PP_Cost_Collector_ID=" + cc.get_ID() + " M_CostElement_ID=" + costElementId + " M_Product_ID="
					+ productId + " 更新 M_CostDetail_ID=" + cd.get_ID() + " 新Amt=" + negCosts + " 新Qty=" + negQty);
			if (!cd.isProcessed()) {
				cd.process();
				cd.saveEx();
			}
		} else {
			stat[2]++; // 跳过计数（金额/数量都没变化）
		}
	}  
  
	/**  
	 * 按 PP_Cost_Collector_ID + C_AcctSchema_ID + M_CostElement_ID + M_Product_ID  
	 * 精确查询是否已存在对应的 M_CostDetail 记录（幂等检查的核心）。  
	 */  
	private MCostDetail findExistingCostDetail(int ppCostCollectorId, int acctSchemaId, int costElementId,  
			int productId) {  
		String whereClause = MCostDetail.COLUMNNAME_PP_Cost_Collector_ID + "=? AND "  
				+ MCostDetail.COLUMNNAME_C_AcctSchema_ID + "=? AND " + MCostDetail.COLUMNNAME_M_CostElement_ID
				+ "=? AND " + MCostDetail.COLUMNNAME_M_Product_ID + "=?";
		return new Query(getCtx(), MCostDetail.Table_Name, whereClause, get_TrxName())  
				.setParameters(ppCostCollectorId, acctSchemaId, costElementId, productId)  
				.setOrderBy(MCostDetail.COLUMNNAME_M_CostDetail_ID)  
				.first();  
	}  
  
	/**
	 * 截止当前报工单报工那一刻，统计该员工当月已报工（其它已完成报工单）的时长。 排除当前单据本身，按
	 * MovementDate/Created/主键三级排序，保证顺序稳定。
	 */
	private BigDecimal getReportedHoursBeforeThisCollector(MPPCostCollector cc, int userId, Timestamp movementDate) {
		String sql = "SELECT COALESCE(SUM(cc2.DurationReal), 0) " + "FROM PP_Cost_Collector cc2 "
				+ "JOIN C_WorkTeamMember wtm ON wtm.C_WorkTeam_ID = cc2.C_WorkTeam_ID " + "WHERE wtm.AD_User_ID = ? "
				+ "  AND wtm.IsActive = 'Y' " + "  AND cc2.CostCollectorType = ? " + "  AND cc2.DocStatus = 'CO' "
				+ "  AND cc2.PP_Cost_Collector_ID <> ? "
				+ "  AND cc2.MovementDate >= date_trunc('month', ?::timestamp) " + "  AND ( cc2.MovementDate < ? "
				+ "        OR (cc2.MovementDate = ? AND cc2.Created < ?) "
				+ "        OR (cc2.MovementDate = ? AND cc2.Created = ? AND cc2.PP_Cost_Collector_ID < ?) )";

		BigDecimal reportedHours = DB.getSQLValueBDEx(get_TrxName(), sql, userId,
				MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl, cc.get_ID(), movementDate, movementDate,
				movementDate, cc.getCreated(), movementDate, cc.getCreated(), cc.get_ID());
		return reportedHours == null ? BigDecimal.ZERO : reportedHours;
	}

	// =========================================================================
	// 以下两个方法是 CostEngine 中 private 方法的原样复制（无法直接调用，只能复制同样的逻辑）
	// =========================================================================

	/** 复制自 CostEngine.getAcctSchema(PO)：取该单据客户下所有未跳过该组织的科目表 */
	private Collection<MAcctSchema> getAcctSchema(MPPCostCollector cc) {
		int AD_Org_ID = cc.getAD_Org_ID();
		MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(cc.getCtx(), cc.getAD_Client_ID());
		ArrayList<MAcctSchema> list = new ArrayList<MAcctSchema>(ass.length);
		for (MAcctSchema as : ass) {
			if (!as.isSkipOrg(AD_Org_ID))
				list.add(as);
		}
		return list;
	}

	/** 复制自 CostEngine.getCostElements(ctx, product, schema)：按科目表的成本计价方法取成本要素 */
	private Collection<MCostElement> getCostElements(java.util.Properties ctx, MProduct product, MAcctSchema schema) {
		return MCostElement.getByCostingMethod(ctx, getCostingMethod(product, schema.getC_AcctSchema_ID()));
	}

	/** 复制自 CostEngine.getCostingMethod(product, acctSchemaId) */
	private String getCostingMethod(MProduct product, int acctSchemaId) {
		MAcctSchema mAcctSchema = MAcctSchema.get(getCtx(), acctSchemaId, null);
		if (product == null) {
			return mAcctSchema.getCostingMethod();
		}
		MProductCategoryAcct acct = MProductCategoryAcct.get(product.getCtx(), product.getM_Product_Category_ID(),
				acctSchemaId, null);
		String costingMethod = Objects.nonNull(acct) ? acct.getCostingMethod() : "";
		if (costingMethod == null || costingMethod.isEmpty()) {
			costingMethod = mAcctSchema.getCostingMethod();
		}  
		return costingMethod;
	}  
}