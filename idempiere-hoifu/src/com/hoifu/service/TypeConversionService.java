package com.hoifu.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

import com.hoifu.model.FieldMapping;

/**
 * @Description: 类型转换服务 - 处理ERP和OA系统之间的字段类型转换
 * @author ldh
 * @date 2025年11月5日
 */
public class TypeConversionService {

	private static final CLogger logger = CLogger.getCLogger(TypeConversionService.class);

	/**
	 * 
	 * @Description: 转换字段值
	 * @param value   原始值
	 * @param mapping 字段映射配置
	 * @return 转换后的字符串值
	 */
	public String convertValue(Object value, FieldMapping mapping) {
		return Optional.ofNullable(value).map(v -> doConvert(v, mapping))
				.orElseGet(() -> Optional.ofNullable(mapping.getDefaultValue()).orElse(""));
	}

	/**
	 * 
	 * @Description: 执行实际的类型转换
	 * @param value
	 * @param mapping
	 * @return
	 */
	private String doConvert(Object value, FieldMapping mapping) {
		String conversionType = Optional.ofNullable(mapping.getConversionType()).orElse("STRING");

		try {
			switch (conversionType) {
			case "STRING":
				return convertToString(value, mapping);
			case "NUMBER":
				return convertToNumber(value, mapping);
			case "DATE":
				return convertToDate(value, mapping);
			case "BOOLEAN":
				return convertToBoolean(value);
			default:
				logger.warning("未知的转换类型: " + conversionType);
				return value.toString();
			}
		} catch (Exception e) {
			String errorMsg = String.format("字段转换失败: %s, 值: %s, 错误: %s", mapping.getOAFieldName(), value,
					e.getMessage());
			logger.severe(errorMsg);
			throw new AdempiereException(errorMsg, e);
		}
	}

	/**
	 * 
	 * @Description: 转换为字符串,处理长度限制
	 * @param value
	 * @param mapping
	 * @return
	 */
	private String convertToString(Object value, FieldMapping mapping) {
		String strValue = value.toString();

		return Optional.of(mapping.getMaxLength()).filter(maxLen -> maxLen > 0 && strValue.length() > maxLen)
				.map(maxLen -> handleStringOverflow(strValue, maxLen, mapping)).orElse(strValue);
	}

	/**
	 * 
	 * @Description: 处理字符串超长
	 * @param strValue
	 * @param maxLength
	 * @param mapping
	 * @return
	 */
	private String handleStringOverflow(String strValue, int maxLength, FieldMapping mapping) {
		if (mapping.isTruncateIfTooLong()) {
			logger.warning(String.format("字段值被截断: %s, 原长度: %d, 截断后: %d", mapping.getOAFieldName(), strValue.length(),
					maxLength));
			return strValue.substring(0, maxLength);
		} else {
			throw new AdempiereException(String.format("字段值超长: %s, 当前长度: %d, 最大长度: %d", mapping.getOAFieldName(),
					strValue.length(), maxLength));
		}
	}

	/**
	 * 
	 * @Description: 转换为数字,处理小数位数
	 * @param value
	 * @param mapping
	 * @return
	 */
	private String convertToNumber(Object value, FieldMapping mapping) {
		if (value instanceof BigDecimal) {
			BigDecimal bd = (BigDecimal) value;

			return Optional.of(mapping.getDecimalScale()).filter(scale -> scale >= 0)
					.map(scale -> bd.setScale(scale, RoundingMode.HALF_UP)).orElse(bd).toPlainString();
		}

		// 尝试转换为BigDecimal
		try {
			BigDecimal bd = new BigDecimal(value.toString());
			return Optional.of(mapping.getDecimalScale()).filter(scale -> scale >= 0)
					.map(scale -> bd.setScale(scale, RoundingMode.HALF_UP)).orElse(bd).toPlainString();
		} catch (NumberFormatException e) {
			throw new AdempiereException("无法转换为数字: " + value);
		}
	}

	/**
	 * 
	 * @Description: 转换为日期字符串
	 * @param value
	 * @param mapping
	 * @return
	 */
	private String convertToDate(Object value, FieldMapping mapping) {
		if (!(value instanceof Timestamp)) {
			throw new AdempiereException("值不是日期类型: " + value.getClass().getName());
		}

		String dateFormat = Optional.ofNullable(mapping.getDateFormat()).filter(f -> !Util.isEmpty(f, true))
				.orElse("yyyy-MM-dd HH:mm:ss");

		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format((Timestamp) value);
	}

	/**
	 * 
	 * @Description: 转换为布尔值
	 * @param value
	 * @return
	 */
	private String convertToBoolean(Object value) {
		if (value instanceof Boolean) {
			return ((Boolean) value) ? "Y" : "N";
		}

		String strValue = value.toString();
		return Arrays.asList("Y", "true", "1", "yes").stream().anyMatch(s -> s.equalsIgnoreCase(strValue)) ? "Y" : "N";
	}
}