package org.adempiere.impexp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.util.ReferenceFieldHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

/**
 * Excel后处理器 - 添加关联表字段 增强版：自动识别参数行（包含“=”的行）并跳过，定位真正的表头行。
 * 
 * @ClassName: ExcelPostProcessor
 * @author ldh
 * @date 2026年4月11日
 */
public class ExcelPostProcessor {

	private static final CLogger log = CLogger.getCLogger(ExcelPostProcessor.class);

	/**
	 * 处理Excel文件，添加参考字段列
	 * 
	 * @Title: processExcel
	 * @param originalExcel
	 * @return
	 * @throws Exception
	 * @return File
	 */
	public File processExcel(File originalExcel) throws Exception {
		// 检查系统配置
		if (!MSysConfig.getBooleanValue(MSysConfig.EXPORT_INCLUDE_REFERENCE_FIELDS, false,
				Env.getAD_Client_ID(Env.getCtx()))) {
			return originalExcel;
		}

		File processedExcel = null;
		Workbook workbook = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;

		try {
			// 读取原始Excel
			fis = new FileInputStream(originalExcel);
			workbook = WorkbookFactory.create(fis);

			// 处理第一个工作表
			Sheet sheet = workbook.getSheetAt(0);

			// 1. 查找真正的表头行（第一个不包含 "=" 的行）
			int headerRowIndex = findHeaderRow(sheet);
			if (headerRowIndex < 0) {
				log.warning("Cannot find header row, skip processing");
				return originalExcel;
			}
			Row headerRow = sheet.getRow(headerRowIndex);
			if (headerRow == null) {
				return originalExcel;
			}

			// 2. 识别需要处理的列
			Map<Integer, String[]> targetColumns = identifyTargetColumns(headerRow);
			if (targetColumns.isEmpty()) {
				return originalExcel;
			}

			// 3. 添加新的表头列（在真正的表头行末尾添加）
			addNewHeaderColumns(headerRow, targetColumns);

			// 4. 处理数据行（从表头下一行开始）
			int firstDataRowIndex = headerRowIndex + 1;
			processDataRows(sheet, headerRow, targetColumns, firstDataRowIndex);

			// 5. 自动调整列宽（基于整个工作表，但列宽会自动适应内容）
			autoSizeColumns(sheet);

			// 写入处理后的Excel
			processedExcel = File.createTempFile("processed_excel_", ".xlsx");
			fos = new FileOutputStream(processedExcel);
			workbook.write(fos);
			fos.flush();

			return processedExcel;

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to process Excel", e);
			if (processedExcel != null && processedExcel.exists()) {
				processedExcel.delete();
			}
			return originalExcel;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
			if (workbook != null) {
				try {
					workbook.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * 查找表头行：第一个完全不包含 "=" 字符的行（即非参数行）
	 * 
	 * @param sheet Excel工作表
	 * @return 表头行索引，如果没找到则返回 -1
	 */
	private int findHeaderRow(Sheet sheet) {
		int lastRowNum = sheet.getLastRowNum();
		// 最多搜索前30行（参数行通常不会超过20行）
		int maxSearchRows = Math.min(lastRowNum, 30);

		for (int rowIndex = 0; rowIndex <= maxSearchRows; rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null)
				continue;

			boolean hasEqualSign = false;
			for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
				Cell cell = row.getCell(colIndex);
				if (cell != null) {
					String cellValue = getCellStringValue(cell);
					if (cellValue != null && cellValue.contains("=")) {
						hasEqualSign = true;
						break;
					}
				}
			}

			// 如果当前行没有任何单元格包含 "="，则认为是表头行
			if (!hasEqualSign) {
				log.info("Found header row at index: " + rowIndex);
				return rowIndex;
			}
		}

		// 降级：如果所有行都包含等号（极少见），返回 -1，放弃处理
		log.warning("No row without '=' found, cannot locate header.");
		return -1;
	}

	/**
	 * 识别需要处理的列
	 * 
	 * @Title: identifyTargetColumns
	 * @param headerRow
	 * @return
	 * @return Map<Integer,String[]>
	 */
	private Map<Integer, String[]> identifyTargetColumns(Row headerRow) {
		Map<Integer, String[]> targetColumns = new LinkedHashMap<>();
		Map<String, String[]> refMap = ReferenceFieldHelper.getReferenceFieldsMap();

		for (int i = 0; i < headerRow.getLastCellNum(); i++) {
			Cell cell = headerRow.getCell(i);
			if (cell != null) {
				String headerValue = getCellStringValue(cell);
				if (refMap.containsKey(headerValue)) {
					targetColumns.put(i, refMap.get(headerValue));
				}
			}
		}
		return targetColumns;
	}

	/**
	 * 添加新的表头列（在表头行末尾添加）
	 * 
	 * @Title: addNewHeaderColumns
	 * @param headerRow
	 * @param targetColumns
	 * @return void
	 */
	private void addNewHeaderColumns(Row headerRow, Map<Integer, String[]> targetColumns) {
		int lastCellNum = headerRow.getLastCellNum();
		int newColumnIndex = lastCellNum;

		for (Map.Entry<Integer, String[]> entry : targetColumns.entrySet()) {
			int originalColumnIndex = entry.getKey();
			Cell originalCell = headerRow.getCell(originalColumnIndex);
			String originalHeader = getCellStringValue(originalCell);
			String tableName = ReferenceFieldHelper.getTableName(originalHeader);
			String[] refFields = entry.getValue();

			CellStyle originalStyle = originalCell != null ? originalCell.getCellStyle() : null;

			for (String refField : refFields) {
				Cell newHeaderCell = headerRow.createCell(newColumnIndex++);
				String columnKey = tableName + "_" + refField;
				String headerText = ReferenceFieldHelper.getChineseHeader(columnKey);
				newHeaderCell.setCellValue(headerText);

				// 复制样式
				if (originalStyle != null) {
					newHeaderCell.setCellStyle(originalStyle);
				}
			}
		}
	}

	/**
	 * 处理数据行，添加关联字段值
	 * 
	 * @param sheet         工作表
	 * @param headerRow     表头行（用于获取列名映射）
	 * @param targetColumns 需要处理的列信息
	 * @param startRowIndex 数据开始行索引（表头下一行）
	 */
	private void processDataRows(Sheet sheet, Row headerRow, Map<Integer, String[]> targetColumns, int startRowIndex) {
		int lastRowNum = sheet.getLastRowNum();

		for (int rowIndex = startRowIndex; rowIndex <= lastRowNum; rowIndex++) {
			Row dataRow = sheet.getRow(rowIndex);
			if (dataRow == null)
				continue;

			int newColumnIndex = dataRow.getLastCellNum();

			for (Map.Entry<Integer, String[]> entry : targetColumns.entrySet()) {
				int originalColumnIndex = entry.getKey();
				Cell idCell = dataRow.getCell(originalColumnIndex);
				String idValue = idCell != null ? getCellStringValue(idCell) : null;

				Cell originalCell = headerRow.getCell(originalColumnIndex);
				String originalHeader = originalCell != null ? getCellStringValue(originalCell) : null;
				String tableName = originalHeader != null ? ReferenceFieldHelper.getTableName(originalHeader) : null;
				String[] refFields = entry.getValue();

				CellStyle originalStyle = idCell != null ? idCell.getCellStyle() : null;

				Map<String, String> refValues = new HashMap<>();

				if (idValue != null && !idValue.trim().isEmpty() && tableName != null) {
					refValues = ReferenceFieldHelper.getReferenceValues(tableName, idValue, refFields);
				}

				// 添加参考字段值
				for (String refField : refFields) {
					Cell newCell = dataRow.createCell(newColumnIndex++);
					String value = refValues.getOrDefault(refField, "");
					newCell.setCellValue(value);

					if (originalStyle != null) {
						newCell.setCellStyle(originalStyle);
					}
				}
			}
		}
	}

	/**
	 * 获取单元格字符串值
	 * 
	 * @Title: getCellStringValue
	 * @param cell
	 * @return
	 * @return String
	 */
	private String getCellStringValue(Cell cell) {
		if (cell == null)
			return "";

		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue();
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue().toString();
			}
			double numValue = cell.getNumericCellValue();
			if (numValue == (long) numValue) {
				return String.valueOf((long) numValue);
			}
			return String.valueOf(numValue);
		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case FORMULA:
			try {
				return cell.getStringCellValue();
			} catch (Exception e) {
				return cell.getCellFormula();
			}
		case BLANK:
			return "";
		default:
			return "";
		}
	}

	/**
	 * 自动调整列宽（基于整个工作表的第一行，但列宽调整会应用到所有列）
	 * 
	 * @Title: autoSizeColumns
	 * @param sheet
	 * @return void
	 */
	private void autoSizeColumns(Sheet sheet) {
		Row headerRow = sheet.getRow(0);
		if (headerRow == null)
			return;

		for (int i = 0; i < headerRow.getLastCellNum(); i++) {
			sheet.autoSizeColumn(i);
			// 设置合理列宽范围
			int width = sheet.getColumnWidth(i);
			if (width > 20000) {
				sheet.setColumnWidth(i, 20000);
			}
			if (width < 2500) {
				sheet.setColumnWidth(i, 2500);
			}
		}
	}
}