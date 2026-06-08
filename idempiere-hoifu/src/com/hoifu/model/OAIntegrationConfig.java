package com.hoifu.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: OA集成配置模型
 * @author ldh
 * @date 2025年11月5日
 */
public class OAIntegrationConfig {

	// 配置主键ID
	private int oAIntegrationConfigID;

	// 配置名称 - 用于标识此配置的用途,如"采购订单OA审批"
	private String name;

	// ERP表ID - 关联到AD_Table,指定此配置处理哪个业务表(如C_Order)
	private int tableID;

	// OA流程ID - OA系统中的流程标识符,用于指定调用OA的哪个审批流程
	private String oaProcessID;

	// 字段映射列表 - 包含所有ERP字段到OA字段的映射关系
	private List<FieldMapping> fieldMappings;

	// 明细表ID-关联AD_Table,指定明细表(如C_OrderLine)
	private int detailTableID;

	// 明细关联列-明细表中关联主表的列名(如C_Order_ID)
	private String detailLinkColumn;

	// OA主表JSON键名-在data对象中主表数据的键名,如main_form
	private String mainFormKey;

	// OA明细JSON键名-在data对象中明细数组的键名,如material_items
	private String detailArrayKey;

	// 是否有data包装层-Y表示主表和明细在data对象内,N表示直接在根级别
	private boolean hasDataWrapper;

	// 使用Stream API创建字段映射的快速查找Map
	public Map<String, FieldMapping> getMappingsByColumn() {
		return Optional.ofNullable(fieldMappings).orElse(Collections.emptyList()).stream().collect(Collectors
				.toMap(FieldMapping::getColumnName, Function.identity(), (existing, replacement) -> existing));
	}

	// Getters and Setters
	public int getOAIntegrationConfigID() {
		return oAIntegrationConfigID;
	}

	public void setOAIntegrationConfigID(int oAIntegrationConfigID) {
		this.oAIntegrationConfigID = oAIntegrationConfigID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTableID() {
		return tableID;
	}

	public void setTableID(int tableID) {
		this.tableID = tableID;
	}

	public String getOAProcessID() {
		return oaProcessID;
	}

	public void setOAProcessID(String oaProcessID) {
		this.oaProcessID = oaProcessID;
	}

	public List<FieldMapping> getFieldMappings() {
		return fieldMappings;
	}

	public void setFieldMappings(List<FieldMapping> fieldMappings) {
		this.fieldMappings = fieldMappings;
	}

	public int getDetailTableID() {
		return detailTableID;
	}

	public void setDetailTableID(int detailTableID) {
		this.detailTableID = detailTableID;
	}

	public String getDetailLinkColumn() {
		return detailLinkColumn;
	}

	public void setDetailLinkColumn(String detailLinkColumn) {
		this.detailLinkColumn = detailLinkColumn;
	}

	public String getMainFormKey() {
		return mainFormKey;
	}

	public void setMainFormKey(String mainFormKey) {
		this.mainFormKey = mainFormKey;
	}

	public String getDetailArrayKey() {
		return detailArrayKey;
	}

	public void setDetailArrayKey(String detailArrayKey) {
		this.detailArrayKey = detailArrayKey;
	}

	public boolean isHasDataWrapper() {
		return hasDataWrapper;
	}

	public void setHasDataWrapper(boolean hasDataWrapper) {
		this.hasDataWrapper = hasDataWrapper;
	}
}
