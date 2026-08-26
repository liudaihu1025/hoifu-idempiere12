package com.hoifu.model;

import java.util.Properties;

/**
 * HF_PP_OrderNode_Tracking 表对应的 Model 类，继承 AD 代码生成的 X_HF_PP_OrderNode_Tracking
 * 基类，具备所有列的标准 getter/setter。
 */
public class MHFPPOrderNodeTracking extends X_HF_PP_OrderNode_Tracking {

	private static final long serialVersionUID = 1L;
	public static final String Table_Name = "HF_PP_OrderNode_Tracking";

	public MHFPPOrderNodeTracking(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public MHFPPOrderNodeTracking(Properties ctx, String uu, String trxName) {
		super(ctx, uu, trxName);
	}

	/**
	 * 保存前校验：工单、工序实例、工序组三个关联外键不能为空。
	 */
	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getPP_Order_ID() <= 0) {
			log.saveError("Error", "PP_Order_ID 不能为空");
			return false;
		}
		if (getPP_Order_Node_ID() <= 0) {
			log.saveError("Error", "PP_Order_Node_ID 不能为空");
			return false;
		}
		if (getoperationclass_ID() <= 0) {
			log.saveError("Error", "OperationClass_ID 不能为空");
			return false;
		}
		return true;
	}
}