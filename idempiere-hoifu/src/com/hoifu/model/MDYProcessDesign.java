package com.hoifu.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.util.DB;

/**
 * 工艺设计任务 Model 类
 * 
 * 新建记录时，自动将关联需求的状态从"工艺设计待受理"(PW)更新为"工艺设计中"(PD)
 */
@org.adempiere.base.Model(table = "dy_processdesign")
public class MDYProcessDesign extends X_dy_processdesign {

	private static final long serialVersionUID = 20250716L;

	// ==================== 需求状态常量 ====================
	/** 工艺设计待受理 */
	private static final String DEMAND_STATUS_PROCESS_WAITING = "PW";
	/** 工艺设计中 */
	private static final String DEMAND_STATUS_PROCESS_DESIGNING = "PD";

	/**
	 * 标准构造函数
	 * 
	 * @param ctx                 上下文
	 * @param dy_processdesign_id 工艺设计任务ID（0=新建）
	 * @param trxName             事务名称
	 */
	public MDYProcessDesign(Properties ctx, int dy_processdesign_id, String trxName) {
		super(ctx, dy_processdesign_id, trxName);
	}

	/**
	 * UUID 构造函数
	 * 
	 * @param ctx                 上下文
	 * @param dy_processdesign_uu 工艺设计任务UUID
	 * @param trxName             事务名称
	 */
	public MDYProcessDesign(Properties ctx, String dy_processdesign_uu, String trxName) {
		super(ctx, dy_processdesign_uu, trxName);
	}

	/**
	 * ResultSet 构造函数（框架查询时使用）
	 * 
	 * @param ctx     上下文
	 * @param rs      结果集
	 * @param trxName 事务名称
	 */
	public MDYProcessDesign(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 保存前校验：新建工艺设计任务时，关联需求状态必须为"工艺设计待受理"(PW)
	 * 
	 * @param newRecord true=新建记录，false=修改记录
	 * @return false 则阻止保存
	 */
	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord) {
			int demandId = get_ValueAsInt("dy_samplingdemand_id");
			if (demandId > 0) {
				String currentStatus = DB.getSQLValueString(get_TrxName(),
						"SELECT requeststatus FROM adempiere.dy_samplingdemand "
								+ "WHERE dy_samplingdemand_id = ? AND isactive = 'Y'",
						demandId);
				if (!DEMAND_STATUS_PROCESS_WAITING.equals(currentStatus)) {
					log.saveError("Error", "关联需求状态不是'工艺设计待受理'，无法新建工艺设计任务");
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 保存后回调： 1. 新建工艺设计任务时，将关联需求状态更新为"工艺设计中" 2. 每次保存（新建和修改）都将计划开始/结束日期反写到需求的
	 * processplannedstartdate / processplannedenddate 字段
	 * 
	 * @param newRecord true=新建记录，false=修改记录
	 * @param success   本次保存是否成功
	 * @return 是否继续（false 会回滚）
	 */
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		int demandId = get_ValueAsInt("dy_samplingdemand_id");
		if (demandId > 0) {
			// 仅新建时触发状态更新，避免修改记录时重复更新
			if (newRecord) {
				// 将需求状态从"工艺设计待受理"更新为"工艺设计中"
				// WHERE 条件加 requeststatus='PW' 防止重复更新或覆盖其他状态
				DB.executeUpdateEx(
						"UPDATE adempiere.dy_samplingdemand " + "SET requeststatus=?, updated=now(), updatedby=? "
								+ "WHERE dy_samplingdemand_id=? AND requeststatus=?",
						new Object[] { DEMAND_STATUS_PROCESS_DESIGNING, getUpdatedBy(), demandId,
								DEMAND_STATUS_PROCESS_WAITING },
						get_TrxName());
			}

			// 每次保存都将计划日期反写到需求表（新建和修改均执行）
			Timestamp plannedStart = (Timestamp) get_Value("plannedstartdate");
			Timestamp plannedEnd = (Timestamp) get_Value("plannedenddate");
			DB.executeUpdateEx(
					"UPDATE adempiere.dy_samplingdemand " + "SET processplannedstartdate=?, processplannedenddate=?, "
							+ "    updated=now(), updatedby=? " + "WHERE dy_samplingdemand_id=?",
					new Object[] { plannedStart, plannedEnd, getUpdatedBy(), demandId }, get_TrxName());
		}

		return true;
	}
}