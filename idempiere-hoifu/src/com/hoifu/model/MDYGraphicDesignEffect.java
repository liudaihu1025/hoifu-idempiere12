package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * 平面设计效果 Model 类
 * 
 * 删除校验：只有状态为"待评审（WR）"的记录才允许删除
 */
@org.adempiere.base.Model(table = "dy_graphicdesigneffect")
public class MDYGraphicDesignEffect extends X_dy_graphicdesigneffect {

	private static final long serialVersionUID = 20250716L;

	// ==================== 效果状态常量 ====================
	private static final String EFFECT_STATUS_WAITING_REVIEW = "WR"; // 待评审

	/**
	 * 标准构造函数
	 */
	public MDYGraphicDesignEffect(Properties ctx, int dy_graphicdesigneffect_id, String trxName) {
		super(ctx, dy_graphicdesigneffect_id, trxName);
	}

	/**
	 * UUID 构造函数
	 */
	public MDYGraphicDesignEffect(Properties ctx, String dy_graphicdesigneffect_uu, String trxName) {
		super(ctx, dy_graphicdesigneffect_uu, trxName);
	}

	/**
	 * ResultSet 构造函数
	 */
	public MDYGraphicDesignEffect(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 删除前校验：只有状态为"待评审（WR）"的效果记录才允许删除
	 * 
	 * @return true 允许删除；false 阻止删除
	 */
	@Override
	protected boolean beforeDelete() {
		String status = geteffectstatus();
		if (!EFFECT_STATUS_WAITING_REVIEW.equals(status)) {
			log.saveError("Error", "只有状态为'待评审'的设计效果记录才能删除");
			return false;
		}
		return true;
	}
}