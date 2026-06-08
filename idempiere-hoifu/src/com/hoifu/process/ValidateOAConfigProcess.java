package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 * @Description: 配置验证Process - 验证OA集成配置的正确性 检查字段映射、表名、列名等配置,防止SQL注入和运行时错误
 * @author ldh
 * @date 2025年11月4日
 */
@org.adempiere.base.annotation.Process
public class ValidateOAConfigProcess extends SvrProcess {

	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

	private int p_OA_Integration_Config_ID = 0;

	@Override
	protected void prepare() {
		Arrays.stream(getParameter()).filter(para -> para.getParameter() != null).forEach(para -> {
			if ("OA_Integration_Config_ID".equals(para.getParameterName())) {
				p_OA_Integration_Config_ID = para.getParameterAsInt();
			}
		});

		if (p_OA_Integration_Config_ID == 0) {
			p_OA_Integration_Config_ID = getRecord_ID();
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_OA_Integration_Config_ID == 0) {
			throw new AdempiereException("未指定配置ID");
		}

		// 加载配置
		MTable configTable = MTable.get(getCtx(), "OA_Integration_Config");
		PO config = configTable.getPO(p_OA_Integration_Config_ID, get_TrxName());

		if (config == null) {
			throw new AdempiereException("配置不存在");
		}

		int tableId = (Integer) config.get_Value("AD_Table_ID");
		MTable table = MTable.get(getCtx(), tableId);

		if (table == null) {
			throw new AdempiereException("无效的表ID: " + tableId);
		}

		// 验证字段映射
		List<String> errors = validateFieldMappings(p_OA_Integration_Config_ID, table);

		if (!errors.isEmpty()) {
			throw new AdempiereException(buildErrorMessage(errors));
		}

		return "配置验证通过";
	}

	/**
	 * 
	 * @Description: 验证所有字段映射配置
	 * @param configId
	 * @param table
	 * @param @throws Exception
	 * @return List<String>
	 */
	private List<String> validateFieldMappings(int configId, MTable table) throws Exception {
		String sql = "SELECT ColumnName, OA_Field_Name, LookupTable, " + "LookupKeyColumn, LookupDisplayColumns "
				+ "FROM OA_Field_Mapping " + "WHERE OA_Integration_Config_ID=? AND IsActive='Y'";

		List<String> errors = new ArrayList<>();

		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, configId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					validateMapping(rs, table, errors);
				}
			}
		}

		return errors;
	}

	/**
	 * 
	 * @Description: 验证单个字段映射
	 * @param rs
	 * @param table
	 * @param errors
	 * @throws SQLException
	 */
	private void validateMapping(ResultSet rs, MTable table, List<String> errors) throws SQLException {
		String columnName = rs.getString("ColumnName");
		String oaFieldName = rs.getString("OA_Field_Name");
		String lookupTable = rs.getString("LookupTable");
		String lookupKeyColumn = rs.getString("LookupKeyColumn");
		String lookupDisplayColumns = rs.getString("LookupDisplayColumns");

		// 验证ERP列是否存在
		MColumn column = MColumn.get(getCtx(), table.get_TableName(), columnName);
		if (column == null || !column.isActive()) {
			errors.add("无效的列名: " + columnName);
			return;
		}

		// 验证OA字段名格式
		if (!isValidIdentifier(oaFieldName)) {
			errors.add("无效的OA字段名: " + oaFieldName);
		}

		// 验证关联表配置
		if (!Util.isEmpty(lookupTable, true)) {
			validateLookupConfiguration(lookupTable, lookupKeyColumn, lookupDisplayColumns, errors);
		}
	}

	/**
	 * 
	 * @Description: 验证关联表配置
	 * @param lookupTable
	 * @param lookupKeyColumn
	 * @param lookupDisplayColumns
	 * @param errors
	 */
	private void validateLookupConfiguration(String lookupTable, String lookupKeyColumn, String lookupDisplayColumns,
			List<String> errors) {
		if (!isValidIdentifier(lookupTable)) {
			errors.add("无效的关联表名: " + lookupTable);
			return;
		}

		MTable lookupTableObj = MTable.get(getCtx(), lookupTable);
		if (lookupTableObj == null) {
			errors.add("关联表不存在: " + lookupTable);
			return;
		}

		// 验证关联键列
		if (!Util.isEmpty(lookupKeyColumn, true)) {
			if (!isValidIdentifier(lookupKeyColumn)) {
				errors.add("无效的关联键列名: " + lookupKeyColumn);
			} else {
				MColumn lookupCol = lookupTableObj.getColumn(lookupKeyColumn);
				if (lookupCol == null) {
					errors.add("关联表中不存在键列: " + lookupKeyColumn);
				}
			}
		}

		// 验证显示列
		if (!Util.isEmpty(lookupDisplayColumns, true)) {
			Arrays.stream(lookupDisplayColumns.split(",")).map(String::trim).filter(col -> !col.isEmpty())
					.forEach(col -> {
						if (!isValidIdentifier(col)) {
							errors.add("无效的显示列名: " + col);
						} else {
							MColumn displayCol = lookupTableObj.getColumn(col);
							if (displayCol == null) {
								errors.add("关联表中不存在显示列: " + col);
							}
						}
					});
		}
	}

	/**
	 * 
	 * @Description: 验证标识符是否合法(防止SQL注入)
	 * @param identifier
	 * @return
	 */
	private boolean isValidIdentifier(String identifier) {
		if (Util.isEmpty(identifier, true)) {
			return false;
		}
		return IDENTIFIER_PATTERN.matcher(identifier).matches();
	}

	/**
	 * 
	 * @Description: 构建错误消息
	 * @param errors
	 * @return
	 */
	private String buildErrorMessage(List<String> errors) {
		return errors.stream().collect(Collectors.joining("\n- ", "配置验证失败:\n- ", ""));
	}
}
