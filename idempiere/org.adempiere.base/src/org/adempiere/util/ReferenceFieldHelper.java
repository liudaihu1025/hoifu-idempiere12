package org.adempiere.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * 参考字段处理工具类 - 提供标识符转换、参考字段查询等公共方法
 * 
 * @ClassName: ReferenceFieldHelper
 * @author ldh
 * @date 2026年4月11日
 */
public class ReferenceFieldHelper {

	private static final CLogger log = CLogger.getCLogger(ReferenceFieldHelper.class);

	// 参考字段映射配置
	private static final Map<String, String[]> REFERENCE_FIELDS_MAP = new HashMap<>();
	private static final Map<String, String> REFERENCE_TABLES_MAP = new HashMap<>();
	private static final Map<String, String> CHINESE_HEADER_MAP = new HashMap<>();

	static {
		Properties ctx = Env.getCtx();

		// 对应导出字段
		REFERENCE_FIELDS_MAP.put(Msg.translate(ctx, "M_Product_ID"), new String[] { "Value", "Name", "Description" });
		REFERENCE_FIELDS_MAP.put(Msg.translate(ctx, "C_BPartner_ID"), new String[] { "Value", "Name", "Description" });
		REFERENCE_FIELDS_MAP.put(Msg.translate(ctx, "C_Order_ID"), new String[] { "DocumentNo", "Description" });
		REFERENCE_FIELDS_MAP.put(Msg.translate(ctx, "PP_Order_ID"), new String[] { "DocumentNo", "Description" });

		// 对应表名
		REFERENCE_TABLES_MAP.put(Msg.translate(ctx, "M_Product_ID"), "M_Product");
		REFERENCE_TABLES_MAP.put(Msg.translate(ctx, "C_BPartner_ID"), "C_BPartner");
		REFERENCE_TABLES_MAP.put(Msg.translate(ctx, "C_Order_ID"), "C_Order");
		REFERENCE_TABLES_MAP.put(Msg.translate(ctx, "PP_Order_ID"), "PP_Order");

		// 中文字段头映射
		CHINESE_HEADER_MAP.put("M_Product_Value", "物料编码");
		CHINESE_HEADER_MAP.put("M_Product_Name", "物料名称");
		CHINESE_HEADER_MAP.put("M_Product_Description", "物料描述");
		CHINESE_HEADER_MAP.put("C_BPartner_Value", "业务伙伴编码");
		CHINESE_HEADER_MAP.put("C_BPartner_Name", "业务伙伴名称");
		CHINESE_HEADER_MAP.put("C_BPartner_Description", "业务伙伴描述");
		CHINESE_HEADER_MAP.put("C_Order_DocumentNo", "订单编号");
		CHINESE_HEADER_MAP.put("C_Order_Description", "订单描述");
		CHINESE_HEADER_MAP.put("PP_Order_DocumentNo", "工单编号");
		CHINESE_HEADER_MAP.put("PP_Order_Description", "工单描述");
	}

	/**
	 * 获取需要处理的列映射
	 * 
	 * @Title: getReferenceFieldsMap
	 * @return
	 * @return Map<String,String[]>
	 */
	public static Map<String, String[]> getReferenceFieldsMap() {
		return REFERENCE_FIELDS_MAP;
	}

	/**
	 * 获取列名对应的表名
	 * 
	 * @Title: getTableName
	 * @param columnHeader
	 * @return
	 * @return String
	 */
	public static String getTableName(String columnHeader) {
		return REFERENCE_TABLES_MAP.get(columnHeader);
	}

	/**
	 * 获取中文表头文字
	 * 
	 * @Title: getChineseHeader
	 * @param columnKey
	 * @return
	 * @return String
	 */
	public static String getChineseHeader(String columnKey) {
		return CHINESE_HEADER_MAP.getOrDefault(columnKey, columnKey);
	}

	/**
	 * 获取参考字段值
	 * 
	 * @param tableName  表名
	 * @param identifier 标识符
	 * @param refFields  需要查询的字段数组
	 * @return Map<字段名, 字段值>
	 */
	public static Map<String, String> getReferenceValues(String tableName, String identifier, String[] refFields) {
		Map<String, String> values = new HashMap<>();

		if (identifier == null || identifier.trim().isEmpty() || tableName == null) {
			return values;
		}

		// 将标识符转换为真正的数据库ID
		Integer actualId = getIdByIdentifier(tableName, identifier);
		if (actualId == null) {
			log.fine("Cannot convert identifier to ID for " + tableName + ": " + identifier);
			return values;
		}

		// 构建SQL
		StringBuilder sql = new StringBuilder("SELECT ");
		for (int i = 0; i < refFields.length; i++) {
			if (i > 0)
				sql.append(", ");
			sql.append(refFields[i]);
		}
		sql.append(" FROM ").append(tableName).append(" WHERE ").append(tableName).append("_ID = ?");

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, actualId);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				for (String refField : refFields) {
					String value = rs.getString(refField);
					values.put(refField, value != null ? value : "");
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to get reference values for " + tableName, e);
		} finally {
			DB.close(rs, pstmt);
		}

		return values;
	}

	/**
	 * 通过完整标识符字符串获取记录ID（使用数据库表达式递归拼接，支持外键）
	 * 
	 * @param tableName  表名
	 * @param identifier 完整标识符，例如 "MO260106104_CB01018699_MRP2产品半成品-B"
	 * @return 记录ID，失败返回 null
	 */
	public static Integer getIdByIdentifier(String tableName, String identifier) {
		if (identifier == null || identifier.trim().isEmpty()) {
			return null;
		}

		// 快速数字路径：纯数字直接按 ID 查询
		try {
			return Integer.parseInt(identifier.trim());
		} catch (NumberFormatException e) {
			// 不是纯数字，继续
		}

		MTable table = MTable.get(Env.getCtx(), tableName);
		String[] idCols = table.getIdentifierColumns();
		if (idCols.length == 0) {
			return null;
		}

		String separator = MSysConfig.getValue(MSysConfig.IDENTIFIER_SEPARATOR, "_", Env.getAD_Client_ID(Env.getCtx()));

		// 构建标识符表达式（带子查询的外键处理）
		String identifierExpr = buildIdentifierExpression(tableName, idCols, separator);

		String sql = "SELECT " + tableName + "_ID FROM " + tableName + " WHERE " + identifierExpr + " = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, identifier);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "getIdByIdentifier failed for " + tableName + " identifier=" + identifier, e);
		} finally {
			DB.close(rs, pstmt);
		}
		return null;
	}

	/**
	 * 递归构建标识符表达式，外键列使用子查询获取外键表的标识符字符串
	 * 
	 * @param tableName 当前表名
	 * @param idCols    标识符列数组
	 * @param separator 分隔符
	 * @return SQL表达式，例如：CASE WHEN DocumentNo IS NULL THEN '-1' ELSE DocumentNo END
	 *         || '_' || (SELECT (CASE WHEN Value IS NULL THEN '-1' ELSE Value END
	 *         || '_' || ...) FROM M_Product WHERE M_Product_ID =
	 *         PP_Order.M_Product_ID)
	 */
	private static String buildIdentifierExpression(String tableName, String[] idCols, String separator) {
		List<String> exprParts = new ArrayList<>();
		for (String col : idCols) {
			if (col.endsWith("_ID")) {
				// 外键列：生成子查询
				String fkTableName = getForeignKeyTableName(col);
				MTable fkTable = MTable.get(Env.getCtx(), fkTableName);
				String[] fkIdCols = fkTable.getIdentifierColumns();
				// 递归构建外键表的标识符表达式（子查询内部，使用表别名避免冲突）
				String fkExpr = buildSubqueryIdentifierExpression(fkTableName, fkIdCols, separator);
				// 子查询中，外键表的主键列名 = fkTableName + "_ID"，例如 M_Product_ID
				String fkIdColumn = fkTableName + "_ID";
				// 注意：外层表名直接用 tableName
				String subquery = "(SELECT " + fkExpr + " FROM " + fkTableName + " WHERE " + fkIdColumn + " = "
						+ tableName + "." + col + ")";
				exprParts.add(subquery);
			} else {
				// 普通列：空值或空字符串转为 '-1'
				String caseExpr = "CASE WHEN " + col + " IS NULL OR " + col + " = '' THEN '-1' ELSE " + col + " END";
				exprParts.add(caseExpr);
			}
		}
		return String.join(" || '" + separator + "' || ", exprParts);
	}

	/**
	 * 构建子查询内部的标识符表达式（用于外键表，避免重复生成子查询）此方法假设传入的 idCols 中可能还包含外键列，继续递归处理。
	 * 
	 * @Title: buildSubqueryIdentifierExpression
	 * @param tableName
	 * @param idCols
	 * @param separator
	 * @return
	 * @return String
	 */
	private static String buildSubqueryIdentifierExpression(String tableName, String[] idCols, String separator) {
		List<String> exprParts = new ArrayList<>();
		for (String col : idCols) {
			if (col.endsWith("_ID")) {
				// 外键列：继续生成子查询（支持多层嵌套）
				String fkTableName = getForeignKeyTableName(col);
				MTable fkTable = MTable.get(Env.getCtx(), fkTableName);
				String[] fkIdCols = fkTable.getIdentifierColumns();
				// 递归生成更深层的子查询表达式
				String nestedExpr = buildSubqueryIdentifierExpression(fkTableName, fkIdCols, separator);
				String fkIdColumn = fkTableName + "_ID";
				String subquery = "(SELECT " + nestedExpr + " FROM " + fkTableName + " WHERE " + fkIdColumn + " = "
						+ col + ")";
				exprParts.add(subquery);
			} else {
				// 普通列
				String caseExpr = "CASE WHEN " + col + " IS NULL OR " + col + " = '' THEN '-1' ELSE " + col + " END";
				exprParts.add(caseExpr);
			}
		}
		return String.join(" || '" + separator + "' || ", exprParts);
	}

	/**
	 * 根据外键列名获取关联的表名
	 * 
	 * @Title: getForeignKeyTableName
	 * @param columnName
	 * @return
	 * @return String
	 */
	private static String getForeignKeyTableName(String columnName) {
		return columnName.replace("_ID", "");
	}
}