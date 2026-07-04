package org.idempiere.print.renderer;

import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MColumn;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.print.MPrintFormatItem;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;

/**
 * 多标识符字段自动拆分实体
 * 
 * @ClassName: MultiIdentifierColumn
 * @author ldh
 * @date 2026年6月21日
 */
public class MultiIdentifierColumn {
	private MPrintFormatItem item; // 原始 PrintFormatItem
	private String idColName; // 标识符列名（如 Value、Name）
	private String foreignTable; // 外键表名
	private String foreignKeyCol; // 外键表主键列名（如 C_BPartner_ID）

	MultiIdentifierColumn(MPrintFormatItem item, String idColName, String foreignTable, String foreignKeyCol) {
		this.item = item;
		this.idColName = idColName;
		this.foreignTable = foreignTable;
		this.foreignKeyCol = foreignKeyCol;
	}

	public MPrintFormatItem getItem() {
		return item;
	}

	public void setItem(MPrintFormatItem item) {
		this.item = item;
	}

	public String getIdColName() {
		return idColName;
	}

	public void setIdColName(String idColName) {
		this.idColName = idColName;
	}

	public String getForeignTable() {
		return foreignTable;
	}

	public void setForeignTable(String foreignTable) {
		this.foreignTable = foreignTable;
	}

	public String getForeignKeyCol() {
		return foreignKeyCol;
	}

	public void setForeignKeyCol(String foreignKeyCol) {
		this.foreignKeyCol = foreignKeyCol;
	}

	/**
	 * 若 item 对应的列是多标识符外键，返回展开后的 MultiIdentifierColumn 列表；否则返回 null。
	 * 
	 * @Title: getMultiIdentifierColumns
	 * @param item
	 * @return
	 * @return List<MultiIdentifierColumn>
	 */
	public static List<MultiIdentifierColumn> getMultiIdentifierColumns(MPrintFormatItem item) {
		if (!item.isTypeField() || item.getAD_Column_ID() <= 0)
			return null;
		MColumn column = MColumn.get(Env.getCtx(), item.getAD_Column_ID());
		if (column == null || column.get_ID() == 0)
			return null;
		if (!DisplayType.isLookup(column.getAD_Reference_ID()) || DisplayType.isMultiID(column.getAD_Reference_ID()))
			return null;
		String foreignTable = column.getReferenceTableName();
		if (foreignTable == null || "AD_Language".equals(foreignTable) || "AD_EntityType".equals(foreignTable)
				|| "AD_Ref_List".equals(foreignTable))
			return null;

		// 用 MLookupFactory 获取准确的标识符列列表
		MLookupInfo info = MLookupFactory.getLookupInfo(Env.getCtx(), 0, item.getAD_Column_ID(),
				column.getAD_Reference_ID());
		if (info == null || info.lookupDisplayColumnNames == null || info.lookupDisplayColumnNames.size() <= 1)
			return null;

		MTable fTable = MTable.get(Env.getCtx(), foreignTable);
		if (fTable == null)
			return null;
		String[] keyCols = fTable.getKeyColumns();
		if (keyCols == null || keyCols.length == 0)
			return null;
		String foreignKeyCol = keyCols[0];

		List<MultiIdentifierColumn> result = new ArrayList<>();
		for (String idCol : info.lookupDisplayColumnNames)
			result.add(new MultiIdentifierColumn(item, idCol, foreignTable, foreignKeyCol));
		return result;
	}

	/**
	 * 解析外键表中某个标识符列的显示值。 1 对于普通列（字符串、数字、日期等），直接执行 SQL 查询返回原始值。 2 对于嵌套外键列（如
	 * C_Order_ID、M_Product_ID 等 TableDir/Search 类型）， 使用
	 * {@link MLookupFactory#getLookup_TableDirEmbed}生成嵌套子查询，
	 * 返回该外键对应的显示值（如订单号、产品名称），而不是原始 ID。
	 * 
	 * @param foreignTable  外键表名，即当前字段所引用的表（如 {@code C_OrderLine}）
	 * @param foreignKeyCol 外键表的主键列名（如 {@code C_OrderLine_ID}）
	 * @param idColName     要查询的标识符列名（如{@code Value}、{@code Name}、{@code C_Order_ID}）
	 * @param keyValue      外键字段的原始值（主键 ID），用于 WHERE 条件
	 * @param language      当前语言，用于翻译表（_Trl）的嵌套查询
	 * @return 标识符列的显示值字符串，若查询结果为 null 则返回 null
	 */
	public static String resolveIdentifierColumnValue(String foreignTable, String foreignKeyCol, String idColName,
			Object keyValue, Language language) {
		MTable fTable = MTable.get(Env.getCtx(), foreignTable);
		if (fTable == null)
			return null;
		MColumn idColumn = fTable.getColumn(idColName);
		if (idColumn == null)
			return null;

		int displayType = idColumn.getAD_Reference_ID();
		String selectExpr;

		if ((displayType == DisplayType.TableDir || displayType == DisplayType.Search) && idColName.endsWith("_ID")
				&& idColumn.getAD_Reference_Value_ID() == 0) {
			// 嵌套 TableDir 外键：用嵌套子查询解析到显示值（如 C_Order_ID → DocumentNo）
			String embeddedSQL = MLookupFactory.getLookup_TableDirEmbed(language, idColName, foreignTable);
			selectExpr = (embeddedSQL != null && !embeddedSQL.isEmpty()) ? "(" + embeddedSQL + ")" : idColName;
		} else if ((displayType == DisplayType.Table || displayType == DisplayType.Search)
				&& idColumn.getAD_Reference_Value_ID() != 0) {
			// 嵌套 Table 外键
			String embeddedSQL = MLookupFactory.getLookup_TableEmbed(language, idColName, foreignTable,
					idColumn.getAD_Reference_Value_ID());
			selectExpr = (embeddedSQL != null && !embeddedSQL.isEmpty()) ? "(" + embeddedSQL + ")" : idColName;
		} else {
			// 普通列（字符串、数字等）直接取值
			selectExpr = idColName;
		}

		String querySql = "SELECT " + selectExpr + " FROM " + foreignTable + " WHERE " + foreignKeyCol + "=?";
		return DB.getSQLValueStringEx(null, querySql, keyValue);
	}

}