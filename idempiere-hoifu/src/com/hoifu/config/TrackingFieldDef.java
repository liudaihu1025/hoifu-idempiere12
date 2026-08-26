package com.hoifu.config;

import com.hoifu.enums.FieldControlType;

/**
 * 打样追踪单个字段的静态元数据定义（不可变对象）。 描述某个工序组追踪弹窗/详情页里的一列/一个输入项：
 * 对应哪个数据库列、界面显示什么中文标签、是否必填、用什么控件渲染。
 */
public final class TrackingFieldDef {

	/** 对应 HF_PP_OrderNode_Tracking 表的列名，落库/回显时按此列名读写 PO */
	public final String columnName;
	/** 中文表头/标签，展示给用户看 */
	public final String label;
	/** 是否必填，控制界面必填校验（红星标注、保存前判空） */
	public final boolean mandatory;
	/** 渲染控件类型：单行文本框 or 多行文本域 */
	public final FieldControlType controlType;

	public TrackingFieldDef(String columnName, String label, boolean mandatory, FieldControlType controlType) {
		this.columnName = columnName;
		this.label = label;
		this.mandatory = mandatory;
		this.controlType = controlType;
	}

	/** 单行文本框字段的便捷工厂方法 */
	public static TrackingFieldDef text(String columnName, String label, boolean mandatory) {
		return new TrackingFieldDef(columnName, label, mandatory, FieldControlType.TEXT);
	}

	/** 多行文本域字段的便捷工厂方法 */
	public static TrackingFieldDef textarea(String columnName, String label, boolean mandatory) {
		return new TrackingFieldDef(columnName, label, mandatory, FieldControlType.TEXTAREA);
	}

	/** 是否用多行控件渲染 */
	public boolean isMultiLine() {
		return controlType == FieldControlType.TEXTAREA;
	}
}