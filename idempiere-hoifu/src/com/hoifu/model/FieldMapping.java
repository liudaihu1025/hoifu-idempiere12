package com.hoifu.model;
import java.util.Optional;

/**
 * @Description: OA配置字段映射模型
 * @author ldh
 * @date 2025年11月5日
 */
public class FieldMapping {
	// ERP列名 - 对应AD_Column表中的列名,如"DocumentNo", "C_BPartner_ID"
	private String columnName;

	// OA字段名 - OA系统中的字段名,如"file1", "file2", "file3"
	private String oaFieldName;

	// 转换类型 - 指定如何转换字段值,可选值: "STRING", "NUMBER", "DATE", "BOOLEAN"
	private String conversionType;

	// 日期格式 - 当conversionType为DATE时使用,如"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"
	private String dateFormat;// 日期格式

	// 最大长度 - 字符串类型的最大长度限制,0表示不限制
	private Integer maxLength;

	// 小数位数 - 数字类型的小数位数,如2表示保留两位小数
	private Integer decimalScale;

	// 默认值 - 当ERP字段值为null时使用的默认值
	private String defaultValue;

	// 超长截断标志 - 当字符串超过maxLength时是否截断(Y/N),N表示抛出异常
	private boolean truncateIfTooLong;//

	// 是否关联查询 - 标识是否需要通过ID查询关联表获取显示值(Y/N)
	private boolean isLookup;

	// 关联表名 - 需要查询的关联表名,如"M_Warehouse", "C_BPartner", "M_Product"
	private String lookupTable;

	// 关联键列 - 关联表的主键列名,用于WHERE条件,如"M_Warehouse_ID"
	private String lookupKeyColumn;

	// 显示列 - 需要从关联表获取的列名,多列用逗号分隔,如"Value,Name"
	private String lookupDisplayColumns;

	// 显示列分隔符 - 多个显示列之间的分隔符,默认为空格,如"-", " - "
	private String lookupSeparator;

	// 字段层级-ROOT(根级别)/MAIN(主表字段)/DETAIL(明细字段)
	private String fieldLevel; // ROOT/MAIN/DETAIL

	// 是否静态值-Y表示使用Static_Value,N表示从ERP字段取值
	private boolean staticValue;

	// 静态值-当Is_Static_Value=Y时使用,如templateCode的固定值
	private String staticValueContent;
	
	// 关联查询-拼接AND条件参数,如:AND client_id = 1000024
	private String lookupAndParam;
	
	// 关联查询-拼接Order By/limit等参数,如:order by created desc
	private String lookupOrderBy;
	

	// 使用Optional处理可能为null的值
	public Optional<String> getDefaultValueOptional() {
		return Optional.ofNullable(defaultValue);
	}

	public Optional<String> getDateFormatOptional() {
		return Optional.ofNullable(dateFormat);
	}

	// Getters and Setters
	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getOAFieldName() {
		return oaFieldName;
	}

	public void setOAFieldName(String oaFieldName) {
		this.oaFieldName = oaFieldName;
	}

	public String getConversionType() {
		return conversionType;
	}

	public void setConversionType(String conversionType) {
		this.conversionType = conversionType;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public Integer getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	public Integer getDecimalScale() {
		return decimalScale;
	}

	public void setDecimalScale(Integer decimalScale) {
		this.decimalScale = decimalScale;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isTruncateIfTooLong() {
		return truncateIfTooLong;
	}

	public void setTruncateIfTooLong(boolean truncateIfTooLong) {
		this.truncateIfTooLong = truncateIfTooLong;
	}

	public boolean isLookup() {
		return isLookup;
	}

	public void setLookup(boolean lookup) {
		isLookup = lookup;
	}

	public String getLookupTable() {
		return lookupTable;
	}

	public void setLookupTable(String lookupTable) {
		this.lookupTable = lookupTable;
	}

	public String getLookupKeyColumn() {
		return lookupKeyColumn;
	}

	public void setLookupKeyColumn(String lookupKeyColumn) {
		this.lookupKeyColumn = lookupKeyColumn;
	}

	public String getLookupDisplayColumns() {
		return lookupDisplayColumns;
	}

	public void setLookupDisplayColumns(String lookupDisplayColumns) {
		this.lookupDisplayColumns = lookupDisplayColumns;
	}

	public String getLookupSeparator() {
		return lookupSeparator;
	}

	public void setLookupSeparator(String lookupSeparator) {
		this.lookupSeparator = lookupSeparator;
	}

	public String getFieldLevel() {
		return fieldLevel;
	}

	public void setFieldLevel(String fieldLevel) {
		this.fieldLevel = fieldLevel;
	}

	public boolean isStaticValue() {
		return staticValue;
	}

	public void setStaticValue(boolean staticValue) {
		this.staticValue = staticValue;
	}

	public String getStaticValueContent() {
		return staticValueContent;
	}

	public void setStaticValueContent(String staticValueContent) {
		this.staticValueContent = staticValueContent;
	}

	public String getLookupAndParam() {
		return lookupAndParam;
	}

	public void setLookupAndParam(String lookupAndParam) {
		this.lookupAndParam = lookupAndParam;
	}

	public String getLookupOrderBy() {
		return lookupOrderBy;
	}

	public void setLookupOrderBy(String lookupOrderBy) {
		this.lookupOrderBy = lookupOrderBy;
	}
}