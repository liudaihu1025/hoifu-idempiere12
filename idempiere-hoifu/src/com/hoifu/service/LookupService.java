package com.hoifu.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Util;

import com.hoifu.model.FieldMapping;

/**
 * @Description: 关联查询服务
 * @author ldh
 * @date 2025年11月5日
 */
public class LookupService {

	private static final CLogger logger = CLogger.getCLogger(LookupService.class);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

	/**
	 * 执行关联查询
	 */
	public Optional<String> performLookup(FieldMapping mapping, Object keyValue, String trxName) {
		if (!mapping.isLookup() || keyValue == null) {
			return Optional.empty();
		}

		validateLookupConfig(mapping);

		String sql = buildLookupSQL(mapping);

		try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
			pstmt.setObject(1, keyValue);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(extractDisplayValue(rs, mapping));
				}
			}
		} catch (SQLException e) {
			throw new DBException(e);
		}

		return Optional.empty();
	}

	private void validateLookupConfig(FieldMapping mapping) {
		Stream.of(mapping.getLookupTable(), mapping.getLookupKeyColumn()).filter(s -> !isValidIdentifier(s)).findFirst()
				.ifPresent(invalid -> {
					throw new AdempiereException("无效的标识符: " + invalid);
				});
	}

	private String buildLookupSQL(FieldMapping mapping) {
		List<String> columns = parseDisplayColumns(mapping.getLookupDisplayColumns());

		// 验证所有列名
//		columns.stream().filter(col -> !isValidIdentifier(col)).findFirst().ifPresent(invalid -> {
//			throw new AdempiereException("无效的列名: " + invalid);
//		});

		String selectClause = columns.stream().collect(Collectors.joining(", "));

		String buildSql = String.format("SELECT %s FROM %s WHERE %s = ?", selectClause, mapping.getLookupTable(),
				mapping.getLookupKeyColumn());
		
		if (Objects.nonNull(mapping.getLookupAndParam()) 
				&& !mapping.getLookupAndParam().isEmpty() 
				&& mapping.getLookupAndParam().toUpperCase().startsWith("AND")) {
			buildSql = buildSql + " " + mapping.getLookupAndParam();
		}
		
		if (Objects.nonNull(mapping.getLookupOrderBy()) && !mapping.getLookupOrderBy().isEmpty()) {
			buildSql = buildSql + " " + mapping.getLookupOrderBy();
		}
		
		return buildSql;
	}

	private String extractDisplayValue(ResultSet rs, FieldMapping mapping) throws SQLException {
		List<String> columns = parseDisplayColumns(mapping.getLookupDisplayColumns());
		String separator = Optional.ofNullable(mapping.getLookupSeparator()).orElse(" ");
		
		final int[] columnIndex = {1};   
		return columns.stream().map(col -> {
			try {
				String value;
				if (col.contains("(")) {
					value = rs.getString(columnIndex[0]);
	            } else {
	            	value = rs.getString(col);
	            }
				columnIndex[0]++;
				return value;
			} catch (SQLException e) {
				logger.warning("无法获取列值: " + col);
				columnIndex[0]++;
				return "";
			}
		}).filter(s -> !Util.isEmpty(s, true)).collect(Collectors.joining(separator));
	}

	private List<String> parseDisplayColumns(String displayColumns) {
		if (Util.isEmpty(displayColumns, true)) {
			return Collections.emptyList();
		}

		return Arrays.stream(displayColumns.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}

	private boolean isValidIdentifier(String identifier) {
		return !Util.isEmpty(identifier, true) && identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
	}
}