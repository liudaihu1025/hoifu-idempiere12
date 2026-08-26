package com.hoifu.enums;

/**
 * 字段的渲染控件类型。 在编辑弹窗/详情页里用单行还是多行控件展示,后续可添加组件类型。
 */
public enum FieldControlType {
	/** 单行文本框，适用于短文本字段 */
	TEXT,
	/** 多行文本域，适用于内容较长、需要换行输入的字段 */
	TEXTAREA
}