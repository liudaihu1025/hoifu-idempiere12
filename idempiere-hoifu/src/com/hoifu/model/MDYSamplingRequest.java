package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * 打样需求单 Model 类
 * 
 * <p>
 * 重写 {@link #afterSave} 实现以下联动逻辑：
 * <ul>
 * <li>新建需求单时，自动创建三个打样阶段：DS（平面设计）/ EN（工艺设计）/ PR（生产打样）</li>
 * </ul>
 * 
 * <p>
 * 配置：在 iDempiere 中将 {@code dy_samplingrequest} 表的 {@code AD_Table.ClassName}
 * 设置为 {@code com.hoifu.model.MDYSamplingRequest}
 */
public class MDYSamplingRequest extends X_dy_samplingrequest {

	private static final long serialVersionUID = 1L;

	public MDYSamplingRequest(Properties ctx, int dy_samplingrequest_id, String trxName) {
		super(ctx, dy_samplingrequest_id, trxName);
	}

	public MDYSamplingRequest(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/** 三个默认阶段：{阶段编码, 阶段名称} */
	private static final String[][] DEFAULT_PHASES = { { "DS", "平面设计" }, { "EN", "工艺设计" }, { "PR", "生产打样" } };

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		// 只在新建成功时执行一次，修改保存时跳过
		if (!success || !newRecord)
			return success;

		int requestID = get_ID(); // 此时主键 ID 已分配

		for (String[] p : DEFAULT_PHASES) {
			String phaseType = p[0]; // DS / EN / PR
			String phaseName = p[1]; // 平面设计 / 工艺设计 / 生产打样

			X_dy_samplingphase phase = new X_dy_samplingphase(getCtx(), 0, get_TrxName());
			phase.setAD_Org_ID(getAD_Org_ID()); // 组织，继承需求单
			phase.setdy_samplingrequest_ID(requestID); // 关联需求单
			phase.setName(phaseName); // 阶段名称
			phase.setphasetype(phaseType); // 阶段类型
			phase.setAD_User_ID(getAD_User_ID()); // 负责人，默认继承需求单负责人
			phase.setplannedstartdate(getCreated()); // 计划开始时间 = 需求单创建时间
			phase.setplannedenddate(getDatePromised()); // 计划完成时间 = 需求单承诺日期
			phase.setphasestatus("UR"); // 阶段状态 = 审批中
			phase.saveEx(); // 失败时抛异常，整体回滚
		}

		return success;
	}
}