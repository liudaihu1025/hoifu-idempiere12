package org.adempiere.impexp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.util.ReferenceFieldHelper;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * CSV后处理器 - 添加关联表字段
 * 
 * @ClassName: CSVPostProcessor
 * @author ldh
 * @date 2026年4月11日
 */
public class CSVPostProcessor {

	private static final CLogger log = CLogger.getCLogger(CSVPostProcessor.class);

	/**
	 * 处理CSV文件，添加参考字段
	 * 
	 * @Title: processCSV
	 * @param originalCSV
	 * @return
	 * @throws Exception
	 * @return File
	 */
	public File processCSV(File originalCSV) throws Exception {
		if (!MSysConfig.getBooleanValue(MSysConfig.EXPORT_INCLUDE_REFERENCE_FIELDS, false,
				Env.getAD_Client_ID(Env.getCtx()))) {
			return originalCSV;
		}

		File processedCSV = null;

		// Use try-with-resources for automatic stream cleanup
		try (ICsvMapReader reader = new CsvMapReader(new FileReader(originalCSV), CsvPreference.STANDARD_PREFERENCE)) {
			String[] headers = reader.getHeader(true);
			List<Map<String, String>> originalData = new ArrayList<>();

			Map<String, String> row;
			while ((row = reader.read(headers)) != null) {
				originalData.add(row);
			}

			if (originalData.isEmpty()) {
				return originalCSV;
			}

			// 处理CSV数据
			List<Map<String, String>> processedData = processCSVData(headers, originalData);

			// 生成处理后的CSV文件
			processedCSV = File.createTempFile("processed_", ".csv");

			// Use try-with-resources for writer
			try (ICsvMapWriter writer = new CsvMapWriter(new FileWriter(processedCSV),
					CsvPreference.STANDARD_PREFERENCE)) {
				// 生成新的头部
				String[] newHeaders = createNewHeaders(headers);
				writer.writeHeader(newHeaders);

				// 写入数据行
				for (Map<String, String> rowMap : processedData) {
					writer.write(rowMap, newHeaders);
				}
			}

			return processedCSV;
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed handle toprocessCSV: ", e);
			// Clean up temp file on error
			if (processedCSV != null && processedCSV.exists()) {
				processedCSV.delete();
			}
			return originalCSV;
		}
	}

	/**
	 * 处理csv数据，添加参考字段列
	 * 
	 * @Title: processCSVData
	 * @param headers
	 * @param originalData
	 * @return
	 * @return List<Map<String,String>>
	 */
	private List<Map<String, String>> processCSVData(String[] headers, List<Map<String, String>> originalData) {
		List<Map<String, String>> result = new ArrayList<>();

		// 识别需要处理的列
		Map<Integer, String[]> targetColumns = identifyTargetColumns(headers);

		if (targetColumns.isEmpty()) {
			// 如果没有目标列，返回原始数据
			for (Map<String, String> row : originalData) {
				result.add(new HashMap<>(row));
			}
			return result;
		}

		// 处理数据行
		for (Map<String, String> row : originalData) {
			Map<String, String> newRow = new HashMap<>(row);

			// 为每个目标列查询并添加参考字段值
			for (Map.Entry<Integer, String[]> entry : targetColumns.entrySet()) {
				int columnIndex = entry.getKey();
				String[] refFields = entry.getValue();
				String idValue = headers[columnIndex] != null ? row.get(headers[columnIndex]) : null;

				if (idValue != null && !idValue.trim().isEmpty()) {
					String baseColumnName = headers[columnIndex];
					String tableName = ReferenceFieldHelper.getTableName(baseColumnName);
					Map<String, String> refValues = ReferenceFieldHelper.getReferenceValues(tableName, idValue,
							refFields);

					// 添加参考字段值到新行
					for (String refField : refFields) {
						if (!refField.isEmpty()) {
							String chineseColumnName = ReferenceFieldHelper
									.getChineseHeader(tableName + "_" + refField);
							newRow.put(chineseColumnName, refValues.getOrDefault(refField, ""));
						}
					}
				} else {
					// ID值为空，添加空的参考字段
					String baseColumnName = headers[columnIndex];
					String tableName = ReferenceFieldHelper.getTableName(baseColumnName);
					for (String refField : refFields) {
						if (!refField.isEmpty()) {
							String chineseColumnName = ReferenceFieldHelper
									.getChineseHeader(tableName + "_" + refField);
							newRow.put(chineseColumnName, "");
						}
					}
				}
			}
			result.add(newRow);
		}

		return result;
	}

	/**
	 * 识别需要处理的列
	 * 
	 * @Title: identifyTargetColumns
	 * @param headers
	 * @return
	 * @return Map<Integer,String[]>
	 */
	private Map<Integer, String[]> identifyTargetColumns(String[] headers) {
		Map<Integer, String[]> targetColumns = new HashMap<>();
		Map<String, String[]> refMap = ReferenceFieldHelper.getReferenceFieldsMap();

		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];
			if (refMap.containsKey(header)) {
				targetColumns.put(i, refMap.get(header));
			}
		}

		return targetColumns;
	}

	/**
	 * 创建新的CSV头部（添加参考字段列）
	 * 
	 * @param originalHeaders
	 * @return
	 */
	private String[] createNewHeaders(String[] originalHeaders) {
		List<String> newHeaders = new ArrayList<>(Arrays.asList(originalHeaders));

		// 为每个目标列添加参考字段头部
		for (String header : originalHeaders) {
			if (ReferenceFieldHelper.getReferenceFieldsMap().containsKey(header)) {
				String[] refFields = ReferenceFieldHelper.getReferenceFieldsMap().get(header);
				String tableName = ReferenceFieldHelper.getTableName(header);

				for (String refField : refFields) {
					if (!refField.isEmpty()) {
						String chineseHeader = ReferenceFieldHelper.getChineseHeader(tableName + "_" + refField);
						newHeaders.add(chineseHeader);
					}
				}
			}
		}

		return newHeaders.toArray(new String[0]);
	}
}