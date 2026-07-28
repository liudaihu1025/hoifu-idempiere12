package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * 工艺设计事项 Model 类
 * 
 * 删除校验：只有状态为"待评审（WR）"的记录才允许删除
 */
@org.adempiere.base.Model(table = "dy_processdesignitem")
public class MDYProcessDesignItem extends X_dy_processdesignitem {

	private static final long serialVersionUID = 20250716L;

	// ==================== 事项状态常量 ====================
	private static final String ITEM_STATUS_WAITING_REVIEW = "WR"; // 待评审

	/**
	 * 标准构造函数
	 */
	public MDYProcessDesignItem(Properties ctx, int dy_processdesignitem_id, String trxName) {
		super(ctx, dy_processdesignitem_id, trxName);
	}

	/**
	 * UUID 构造函数
	 */
	public MDYProcessDesignItem(Properties ctx, String dy_processdesignitem_uu, String trxName) {
		super(ctx, dy_processdesignitem_uu, trxName);
	}

	/**
	 * ResultSet 构造函数
	 */
	public MDYProcessDesignItem(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 删除前校验：只有状态为"待评审（WR）"的事项记录才允许删除
	 * 
	 * @return true 允许删除；false 阻止删除
	 */
	@Override
	protected boolean beforeDelete() {
		String status = getitemstatus();
		if (status == null || !ITEM_STATUS_WAITING_REVIEW.equals(status)) {
			log.saveError("Error", "只有状态为'待评审'的工艺设计事项记录才能删除");
			return false;
		}
		return true;
	}
}