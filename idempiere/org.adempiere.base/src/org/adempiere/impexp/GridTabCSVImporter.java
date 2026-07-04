/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2012 Carlos Ruiz                                             *
 * Copyright (C) 2012 Trek Global                                             *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.impexp;

import static org.compiere.model.SystemIDs.REFERENCE_DOCUMENTACTION;
import static org.compiere.model.SystemIDs.REFERENCE_PAYMENTRULE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;

import org.adempiere.base.IGridTabImporter;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.IProcessUI;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.GridWindowVO;
import org.compiere.model.MColumn;
import org.compiere.model.MLocation;
import org.compiere.model.MProcess;
import org.compiere.model.MQuery;
import org.compiere.model.MRefList;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.tools.FileUtil;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.compiere.wf.MWFProcess;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrMinMax;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import com.google.common.base.Charsets;

/**
 * CSV Importer for GridTab
 * @author Carlos Ruiz
 * @author Juan David Arboleda 
 */
public class GridTabCSVImporter implements IGridTabImporter
{
	private static final String ERROR_HEADER = "_ERROR_";
	private static final String LOG_HEADER = "_LOG_";
	private static final String IMPORT_MODE_MERGE = "M";
	private static final String IMPORT_MODE_UPDATE = "U";
	private static final String IMPORT_MODE_INSERT = "I";
	
	private boolean m_isError = false;
	private String m_import_mode = null;
	private List<String> header;  
    private List<CellProcessor> readProcArray;
    private List<GridField> locationFields;
	private boolean isThereKey;
    private boolean isThereDocAction;
    private Map<GridTab,Integer> tabMapIndexes;
    private List<Map<String, Object>> data;
    private ICsvMapReader mapReader;
    private TreeMap<GridTab,Integer> sortedtTabMapIndexes;
    private List<String> rawData;
    private boolean isMasterok = true; 
	private boolean isDetailok = true;
	private boolean error = false;
	private List<String>  rowsTmpResult;
	private PO masterRecord;
    
    //Files management
	private File errFile;
	private File logFile;
	private PrintWriter errFileW;
	private PrintWriter logFileW;

	private String delimiterChar = ",";
	private String quoteChar = "\"";

	//Trx
	private Trx trx;
	private String trxName;
	private boolean isSingleTrx = false;

	/**	Logger			*/
	private static final CLogger log = CLogger.getCLogger(GridTabCSVImporter.class);
	
	@Override
	public File fileImport(GridTab gridTab, List<GridTab> childs, InputStream filestream, Charset charset , String importMode) {		
		return fileImport(gridTab, childs, filestream, charset, importMode, null, null, null);
	}//fileImport

	@Override
	public File fileImport(GridTab gridTab, List<GridTab> childs, InputStream filestream, Charset charset, String importMode, IProcessUI processUI) {
		return fileImport(gridTab, childs, filestream, charset, importMode, null, null, processUI);
	}

	@Override
	public File fileImport(GridTab gridTab, List<GridTab> childs, InputStream filestream, Charset charset, String importMode, String p_delimiterChar, String p_quoteChar, IProcessUI processUI) {
		// BUG修复：确保所有输入流都支持mark/reset  
	    if (!filestream.markSupported()) {  
	        filestream = new BufferedInputStream(filestream);  
	    }  
	    
		if(!gridTab.isInsertRecord() && isInsertMode())
			throwAdempiereException("当前页签不允许新增记录，请检查权限或页签配置");

		try {
			String errFileName = FileUtil.getTempMailName("Import_" + gridTab.getTableName(), "_err.csv");
			initValues();
			m_import_mode = importMode;
			errFile = new File(errFileName);
			errFileW = new PrintWriter(errFile, charset.name());

			if (p_delimiterChar != null)
				delimiterChar = p_delimiterChar;
			if (p_quoteChar != null)
				quoteChar = p_quoteChar;
			CsvPreference csvpref = new CsvPreference.Builder(quoteChar.charAt(0), delimiterChar.charAt(0), "\r\n" /* ignored */).build();

			if (Env.getAD_Language(Env.getCtx()).compareToIgnoreCase("zh_CN") == 0
					&& charset.compareTo(Charsets.UTF_8) == 0) {
				byte[] bHeader = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
				filestream.mark(0);
				filestream.read(bHeader, 0, 3);
				if (!(bHeader[0] == (byte) 0xEF && bHeader[1] == (byte) 0xBB && bHeader[2] == (byte) 0xBF))
					filestream.reset();
			}

			mapReader = new CsvMapReader(new InputStreamReader(filestream, charset), csvpref);
			header =  Arrays.asList(mapReader.getHeader(true));  

			//Mapping header
			int indxDetail = mapCSVHeader(gridTab);

			if(isUpdateOrMergeMode() && !isThereKey)
				throwAdempiereException(gridTab.getTableName()+": "+Msg.getMsg(Env.getCtx(), "NoKeyFound"));

			tabMapIndexes.put(gridTab,indxDetail-1);
			isThereKey = false;
			locationFields = null;

			//Mapping details 
			mapCSVDetail(indxDetail, childs);

			sortedtTabMapIndexes = null;
			if (childs.size()>0 && !tabMapIndexes.isEmpty()){
				ValueComparator bvc =  new ValueComparator(tabMapIndexes);
				sortedtTabMapIndexes = new TreeMap<GridTab,Integer>(bvc);
				sortedtTabMapIndexes.putAll(tabMapIndexes);
			}else{
				Map<GridTab,Integer> localMapIndexes = new HashMap<GridTab,Integer>();
				localMapIndexes.put(gridTab, header.size()-1);
				ValueComparator bvc =  new ValueComparator(localMapIndexes);
				sortedtTabMapIndexes = new TreeMap<GridTab,Integer>(bvc);
				sortedtTabMapIndexes.putAll(localMapIndexes);
			}

			m_isError = false;
			// write the header
			String rawHeader = mapReader.getUntokenizedRow();
//			errFileW.write(rawHeader + delimiterChar + ERROR_HEADER + "\n");
			if ("M_Product".equals(gridTab.getTableName())) {
				errFileW.write(rawHeader + delimiterChar + "_VALUE_" + delimiterChar + ERROR_HEADER + "\n");
			} else {
				errFileW.write(rawHeader + delimiterChar + ERROR_HEADER + "\n");
			}
			
			data = new ArrayList<Map<String, Object>>();
			rawData = new ArrayList<String>();

			// pre-process to check for errors
			preProcess(processUI, gridTab, indxDetail);

			if ( !m_isError ) {

				String logFileName = FileUtil.getTempMailName("Import_" + gridTab.getTableName(), "_log.csv");
				logFile = new File(logFileName);
				logFileW = new PrintWriter(logFile, charset.name());
				// write the header
//				logFileW.write(rawHeader + delimiterChar + LOG_HEADER + "\n");
				if ("M_Product".equals(gridTab.getTableName())) {  
				    logFileW.write(rawHeader + delimiterChar + "_VALUE_" + delimiterChar + LOG_HEADER + "\n");  
				} else {  
				    logFileW.write(rawHeader + delimiterChar + LOG_HEADER + "\n");  
				}
				// no errors found - process header and then details 
				isMasterok = true; 
				isDetailok = true;
				error = false;
				trx = null;
				trxName = null;
				rowsTmpResult = new ArrayList<String>();

				long lastOutput = new Date().getTime();

				for (int idx = 0; idx < data.size(); idx++) {

					if( processUI != null && new Date().getTime()-lastOutput > 1000 /* one second */){
						processUI.statusUpdate(refreshImportStatus(idx + 1, data.size() + 1));
						lastOutput = new Date().getTime();
					}

					String rawLine = rawData.get(idx);
					StringBuilder rowResult = new StringBuilder();
					boolean isDetail = false;

					if (rawLine.charAt(0)==','){
						isDetail=true;
						//check out if master row comes empty  
						Map<String, Object> rowMap = data.get(idx);
						for(int i=0; i < indxDetail-1; i++){	
							if(rowMap.get(header.get(i))!=null){
								isDetail=false;
								break;
							}
						}
					}

					if (!isMasterok && isDetail){
						rawLine = rawLine + delimiterChar + quoteChar + Msg.getMsg(Env.getCtx(),"NotProcessed") + quoteChar + "\n";
						rowsTmpResult.add(rawLine);
						continue;		 
					}else if(isMasterok && isDetail && !isDetailok){
						rawLine = rawLine + delimiterChar + quoteChar + " 因明细行保存失败，本行数据未被导入" + quoteChar + "\n";
						rowsTmpResult.add(rawLine);
						continue;	 
					}
					
					if( isSingleTrx() && trx == null )
						createTrx(gridTab);

					if( !isDetail && !isSingleTrx() ){
						manageMasterTrx(gridTab, null);
						createTrx(gridTab);
					}
					if (trx != null)
						trx.setDisplayName(GridTabCSVImporter.class.getName()+"_fileImport_" + gridTab.getTableName());

					String recordResult = processRecord(importMode, gridTab, indxDetail, isDetail, idx, rowResult, childs);
					rowResult.append(recordResult);

					// write
//					rawLine = rawLine + delimiterChar + quoteChar + rowResult.toString().replaceAll(delimiterChar, "") + quoteChar + "\n";
					if ("M_Product".equals(gridTab.getTableName())) {
						String productValue = getProductValue(gridTab, idx);
						rawLine = rawLine + delimiterChar + quoteChar + (productValue != null ? productValue : "")
								+ quoteChar + delimiterChar + quoteChar
								+ rowResult.toString().replaceAll(delimiterChar, "") + quoteChar + "\n";
					} else {
						rawLine = rawLine + delimiterChar + quoteChar
								+ rowResult.toString().replaceAll(delimiterChar, "") + quoteChar + "\n";
					}
					
					rowsTmpResult.add(rawLine);
					
					if( isSingleTrx() && isError() )
						break;

				}

				manageMasterTrx(gridTab,childs);

			}
		} catch (IOException e) {
			throw new AdempiereException(e);
		} catch (Exception ex) {
			throw new AdempiereException(ex);
		} finally {
			try {
				if (mapReader != null)
					mapReader.close();
				if (errFileW != null) {
					errFileW.flush();
					errFileW.close();
				}
				if (logFileW != null) {
					logFileW.flush();
					logFileW.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			gridTab.getTableModel().setImportingMode(false,null);
			for (GridTab detail : childs) {
				detail.getTableModel().setImportingMode(false,null);
			}
			gridTab.dataRefreshAll();

		}		
		if (logFile != null)
			return logFile;
		else
			return errFile;
	}//fileImport

	private String getProductValue(GridTab gridTab, int idx) {
		if (!"M_Product".equals(gridTab.getTableName()) || data == null || idx >= data.size())
			return null;

		Map<String, Object> rowMap = data.get(idx);
		// 1. 先从导入数据中获取
		Object value = rowMap.get("Value");
		if (value != null)
			return value.toString();

		// 2. 从已保存的 PO 对象读取（PO_BEFORE_NEW 生成的编码在这里）
		PO po = gridTab.getTableModel().getPO(gridTab.getCurrentRow());
		if (po != null) {
			Object poValue = po.get_Value("Value");
			if (poValue != null)
				return poValue.toString();
		}

		return "";
	}
	
	private void initValues(){
		mapReader = null;
		errFile = null;
		logFile = null;
		errFileW = null;
		logFileW = null;
		masterRecord = null;
		readProcArray = new ArrayList<CellProcessor>();
		tabMapIndexes = new HashMap<GridTab,Integer>();
		locationFields = null;
		isThereKey = false;
		isThereDocAction = false;
	}
	
	/**
	 * Rollback trx and update the text in log file
	 */
	private void rollbackTrx(){
		trx.rollback();
		for( String row : rowsTmpResult ){
			row = row.replace("Updated","RolledBack");
			row = row.replace("Inserted","RolledBack");
			logFileW.write(row);
		}
	}
	
	/**
	 * Commit trx and writes in log file
	 * @return error message or null
	 */
	private String commitTrx(){
		try {
			trx.commit(true);
		} catch (SQLException e) {
			setError(true);
			return e.getLocalizedMessage();
		}
		for( String row : rowsTmpResult )
			logFileW.write(row);
		return null;
	}

	// ldh新增方法：将中文列名翻译为英文列名
	private String translateChineseHeaderToEnglish(String chineseHeader, String tableName, Integer windowId) {
		String adLanguage = Env.getAD_Language(Env.getCtx());
		if (Env.isBaseLanguage(adLanguage, "AD_Element")) {
			return chineseHeader; // 基础语言不需要翻译
		}

		// 检查是否包含外键标识符
		if (chineseHeader.indexOf("[") >= 0 && chineseHeader.endsWith("]")) {
			// 分离主字段名和标识符部分
			String mainFieldName = chineseHeader.substring(0, chineseHeader.indexOf("["));
			String identifierPart = chineseHeader.substring(chineseHeader.indexOf("["));

			// 只翻译主字段名
			String translatedMainField = translateMainField(mainFieldName, tableName, adLanguage, windowId);

			// 重新组合：翻译后的主字段名 + 原始标识符
			return translatedMainField + identifierPart;
		} else {
			// 没有标识符，直接翻译整个字段名
			return translateMainField(chineseHeader, tableName, adLanguage, windowId);
		}
	}

	// 辅助方法：翻译主字段名
	private String translateMainField(String fieldName, String tableName, String adLanguage, Integer windowId) {
		String sql = "SELECT e.ColumnName FROM AD_Element e "
				+ "JOIN AD_Element_Trl t ON e.AD_Element_ID = t.AD_Element_ID "
				+ "JOIN AD_Column c ON c.AD_Element_ID = e.AD_Element_ID "
				+ "JOIN AD_Table tab ON tab.AD_Table_ID = c.AD_Table_ID "
				+ "JOIN AD_Field f ON f.AD_Column_ID = c.AD_Column_ID "
				+ "JOIN AD_Tab tb ON tb.AD_Tab_ID = f.AD_Tab_ID "
				+ "JOIN AD_Window w ON w.AD_Window_ID = tb.AD_Window_ID "
				+ "WHERE t.Name = ? AND t.AD_Language = ? AND tab.TableName = ? AND (f.IsDisplayed = 'Y' OR f.IsDisplayedGrid = 'Y') ";
		
		String englishHeadName = null;
		if (Objects.nonNull(windowId) && windowId > 0) {
			sql += "AND w.AD_Window_ID = ? group by e.ColumnName";
			englishHeadName = DB.getSQLValueString(null, sql, fieldName, adLanguage, tableName, windowId);
		} else {
			sql += "group by e.ColumnName";
			englishHeadName = DB.getSQLValueString(null, sql, fieldName, adLanguage, tableName);
		}
		
		return Util.isEmpty(englishHeadName) ? fieldName : englishHeadName;
	}
	
	/**
	 * Map the header and returns the index where the detail starts
	 * @param gridTab
	 * @return index where detail start
	 */
	private int mapCSVHeader(GridTab gridTab){
		
		int indxDetail = 0;
		
		for(int idx = 0; idx < header.size(); idx++) {
			String headName = header.get(idx);

			if (headName==null) {
				throwAdempiereException("CSV 文件第 " + (idx + 1) + " 列的表头为空，请补全列名后重新导入");
			}

			// 新增：尝试将中文列名转换为英文列名
			String englishColumnName = translateChineseHeaderToEnglish(headName, gridTab.getTableName(), gridTab.getAD_Window_ID());
			if (englishColumnName != null && !englishColumnName.equals(headName)) {
				headName = englishColumnName;
				header.set(idx, headName); // 更新header列表中的列名
			}

			if (headName.equals(ERROR_HEADER) || headName.equals(LOG_HEADER)){
				header.set(idx, null);
				readProcArray.add(null);
				continue;
			}
			if (headName.indexOf(">") > 0) {
				if(idx==0){
					throwAdempiereException(Msg.getMsg(Env.getCtx(),"WrongHeader", new Object[] {headName}));
				}else if (headName.contains(MTable.getTableName(Env.getCtx(), MLocation.Table_ID)) && locationFields==null){ 
					locationFields = getSpecialMColumn(header,MTable.getTableName(Env.getCtx(), MLocation.Table_ID),idx);
					for(GridField sField:locationFields){
						readProcArray.add(getProccesorFromColumn(sField)); 
						indxDetail++;
					}
					idx=indxDetail;
				}else
					break;

			}else{
				boolean isKeyColumn = headName.indexOf("/") > 0;
				boolean isForeing 	= headName.indexOf("[") > 0 && headName.indexOf("]")>0;
				String  columnName  = getColumnName (isKeyColumn,isForeing,false,headName);
				GridField field 	= gridTab.getField(columnName);

				if (field == null)
					throwAdempiereException(Msg.getMsg(Env.getCtx(), "FieldNotFoundInTab" , new Object[] {columnName, gridTab.getName()}) );
				else if(isKeyColumn && !isThereKey)
					isThereKey =true;
				else if (!isThereDocAction &&
						MColumn.get(Env.getCtx(),field.getAD_Column_ID()).getAD_Reference_Value_ID() == REFERENCE_DOCUMENTACTION )
					isThereDocAction= true;

				readProcArray.add(getProccesorFromColumn(field)); 
				indxDetail++;
			}
		}
		
		return indxDetail;
	}//mapCSVHeader
	
	/**
	 * Map details fields in the csv file
	 * @param indxDetail
	 * @param childs
	 */
	private void mapCSVDetail(int indxDetail, List<GridTab> childs){

		String  childTableName = null;
		GridTab currentDetailTab = null;

		for(int idx = indxDetail; idx < header.size(); idx++) {	
			String detailName = header.get(idx);
			if(detailName!=null && detailName.indexOf(">") > 0){
				childTableName = detailName.substring(0,detailName.indexOf(">"));  
				// 翻译表名
				String englishTableName = translateChineseTableNameToEnglish(childTableName);
				if (englishTableName != null && !englishTableName.equals(childTableName)) {
					childTableName = englishTableName;
					// 翻译列名并更新header
					String columnNamePart = detailName.substring(detailName.indexOf(">") + 1);
					String englishColumnName = translateChineseHeaderToEnglish(columnNamePart, englishTableName, null);
					header.set(idx, englishTableName + ">" + englishColumnName);
					detailName = header.get(idx); // 使用更新后的header
				}
				if (currentDetailTab==null || 
						(currentDetailTab!=null && !childTableName.equals(currentDetailTab.getTableName()))){

					if(currentDetailTab!=null){ 
						//check out key per Tab   
						if(isUpdateOrMergeMode() && !isThereKey){
							throwAdempiereException(currentDetailTab.getTableName()+": "+Msg.getMsg(Env.getCtx(), "NoKeyFound"));
						}else{
							tabMapIndexes.put(currentDetailTab,idx-1); 	
							isThereKey =false; 
						} 
					}

					for(GridTab detail: childs){
						if(detail.getTableName().equals(childTableName)){
							currentDetailTab = detail;
							break;
						}
					} 
				}

				if(currentDetailTab == null) 
					throwAdempiereException(Msg.getMsg(Env.getCtx(),"NoChildTab",new Object[] {childTableName}));

				String columnName = detailName;
				if (columnName.contains(MTable.getTableName(Env.getCtx(), MLocation.Table_ID)) && locationFields==null){
					locationFields = getSpecialMColumn(header,MTable.getTableName(Env.getCtx(), MLocation.Table_ID),idx);
					for(GridField sField:locationFields){
						readProcArray.add(getProccesorFromColumn(sField)); 
						idx++;
					}
					idx--;
				}else{
					boolean isKeyColumn= columnName.indexOf("/") > 0;
					boolean isForeing  = columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
					columnName = getColumnName(isKeyColumn,isForeing,true,columnName);
					GridField field = currentDetailTab.getField(columnName);

					if(field == null)
						throwAdempiereException(Msg.getMsg(Env.getCtx(), "FieldNotFound",new Object[] {detailName}));
					else if(isKeyColumn && !isThereKey)
						isThereKey =true;

					readProcArray.add(getProccesorFromColumn(field));  
				}				   
			}else
				throwAdempiereException(Msg.getMsg(Env.getCtx(),"WrongDetailName",new Object[] {" col("+idx+") ",detailName}));

		}
		if(currentDetailTab!=null){
			if(isUpdateOrMergeMode() && !isThereKey)
				throwAdempiereException(currentDetailTab.getTableName()+": "+Msg.getMsg(Env.getCtx(), "NoKeyFound"));

			tabMapIndexes.put(currentDetailTab,header.size()-1); 	   
		}

	}//mapCSVDetail
	
	/**
	 * Pre-process the input csv file and look for errors
	 * @param processUI
	 * @param gridTab
	 * @param indxDetail
	 */
	private void preProcess(IProcessUI processUI, GridTab gridTab, int indxDetail){

		CellProcessor[] processors = readProcArray.toArray(new CellProcessor[readProcArray.size()]);	
		long lastOutput = new Date().getTime();

		while (true) {
			if( processUI != null && new Date().getTime()-lastOutput > 1000 /* one second */){
				processUI.statusUpdate(refreshImportStatus(data.size(), 0));
				lastOutput = new Date().getTime();
			}

			Map<String, Object> map = null;
			boolean isLineError = false; 
			StringBuilder errMsg = new StringBuilder();

			try {			
				// devCoffee #6141 - IDEMPIERE-3832
				String[] hdrs = new String[header.size()];
				header.toArray(hdrs);
				map = mapReader.read( hdrs, processors);
			} catch (SuperCsvCellProcessorException e) {
				int idx = e.getCsvContext().getColumnNumber() - 1;
				errMsg.append(header.get(idx)).append(": ").append(e.getMessage());
				isLineError = true;
			} catch (IOException e) {
				throw new AdempiereException(e);
			}

			String rawLine = mapReader.getUntokenizedRow();
			if (! isLineError) {
				if(map == null)
					break;

				//Re-order information coming from map
				List<Object> tmpRow = getOrderedRowFromMap(header,map);	  					
				//read master and detail
				int initIndx= 0;
				for(Map.Entry<GridTab, Integer> tabIndex : sortedtTabMapIndexes.entrySet()) {
					GridTab tmpGrid = tabIndex.getKey(); 						
					if(gridTab.equals(tmpGrid) && tmpRow.get(0)==null){
						initIndx = indxDetail;
						continue;	
					}						
					int endindx = tabIndex.getValue();
					StringBuilder lineError = preprocessRow (tmpGrid,header,tmpRow,initIndx,endindx);
					if( lineError!= null && lineError.length() > 0 ){
						isLineError = true;
						if (errMsg.length() > 0)
							errMsg.append(" / ");
						errMsg.append(lineError);
					}
					initIndx = endindx + 1;
				}
			}
			if (isLineError && ! m_isError)
				m_isError = true;
			if (!m_isError) {
				data.add(map);
				rawData.add(rawLine);
			}
			// write
			rawLine = rawLine + delimiterChar + quoteChar + errMsg.toString().replaceAll(quoteChar, "") + quoteChar + "\n";
			errFileW.write(rawLine);
		}
	}//preProcess
	
	/**
	 * Manage the trx
	 * if the trx exists - commits when no errors, rollback when errors.
	 * @param gridTab
	 * @param childs
	 */
	private void manageMasterTrx(GridTab gridTab, List<GridTab> childs){
		if (trx != null) {
			try {
				if (isError()) {
					gridTab.dataDelete();
					rollbackTrx();
					setError(false);
				} else {
					boolean commit = false;
					if (isThereDocAction) {
	
						boolean isError = false;
						int AD_Process_ID = MColumn.get(Env.getCtx(), gridTab.getField("DocAction").getAD_Column_ID()).getAD_Process_ID(); 
	
						if (AD_Process_ID > 0){
							String docResult = processDocAction(masterRecord, AD_Process_ID); 
	
							if (docResult.contains("error")) 
								isError = true; 
	
							rowsTmpResult.set(0,rowsTmpResult.get(0).replace(quoteChar + "\n",docResult + quoteChar + "\n")); 
						} else {
							throwAdempiereException("单据动作未配置对应的流程，请联系管理员检查配置");	  
						}
	
						if (isError){
							gridTab.dataDelete();
							rollbackTrx();
						} else {
							commit = true;
						}
					} else {
						commit = true;
					}
					if (commit) {
						String commitResult = commitTrx();
						if (isError()) {
							rowsTmpResult.set(0,rowsTmpResult.get(0).replace(quoteChar + "\n",commitResult + quoteChar + "\n")); 
							gridTab.dataDelete();
							rollbackTrx();
						}
					}
				}
			} finally {
				trx.close();
				trx=null;
			}
		}

	}//manageMasterTrx
	
	/**
	 * Create a new Trx with a random Name
	 * @param gridTab
	 */
	private void createTrx(GridTab gridTab){
		trxName = Trx.createTrxName("CSVImport");
		gridTab.getTableModel().setImportingMode(true,trxName);	
		trx = Trx.get(trxName,true);
		masterRecord = null;
		rowsTmpResult.clear();
		isMasterok = true;
		isDetailok = true;
		
	} //createTrx
	
	/**
	 * Process the record for each row.<br/>
	 * First insert the master tab - if no errors found proceeds with the details tabs when existing.<br/>
	 * Stops at the first error found in the row.
	 * @param importMode
	 * @param gridTab
	 * @param indxDetail
	 * @param isDetail
	 * @param idx
	 * @param rowResult
	 * @return message or empty string
	 */
	private String processRecord(String importMode, GridTab gridTab, int indxDetail, boolean isDetail, int idx, StringBuilder rowResult, List<GridTab> childs){
		
		String logMsg = null;
		GridTab currentGridTab = null;
		int currentColumn = 0;

		try {

			for( Map.Entry<GridTab, Integer> tabIndex : sortedtTabMapIndexes.entrySet() ) {

				currentGridTab = tabIndex.getKey(); 			

				if( isDetail && gridTab.equals(currentGridTab) ){
					currentColumn = indxDetail;
					continue;			
				}

				//Assign master trx to its children
				if( !gridTab.equals(currentGridTab) ){
					currentGridTab.getTableModel().setImportingMode(true,trxName);	
					isDetail=true;
				}

				int j = tabIndex.getValue();	
				logMsg = areValidKeysAndColumns(currentGridTab,data.get(idx),header,currentColumn,j,masterRecord,trx);

				if (logMsg == null){

					if ( isInsertMode() ){
						if( !currentGridTab.getTableModel().isOpen() )
							currentGridTab.getTableModel().open(0);
						//how to read from status since the warning is coming empty ?
						if ( !currentGridTab.dataNew(false) ){
							logMsg = "["+currentGridTab.getName()+"]"+"- Was not able to create a new record!";
							logMsg = "页签[" + currentGridTab.getName() + "]无法创建新记录，可能是必填字段缺失或权限不足!";
						}else{
							currentGridTab.navigateCurrent();
							if (! isDetail) {
								for (GridTab child : childs) {
									child.query(false);
								}
							}
						}
					} 

					if( logMsg==null )
						logMsg = proccessRow(currentGridTab,header,data.get(idx),currentColumn,j,masterRecord,trx);

					currentColumn = j + 1;
					if( !(logMsg == null) ){
						m_import_mode = importMode;   
						//Ignore row since there is no data 
						if("NO_DATA_TO_IMPORT".equals(logMsg)){
							logMsg = "";
							continue;
						}else 
							setError(true);
					}

				}else {
					setError(true);
					currentColumn = j + 1;
				}

				if ( !isError() ) {
					if ( currentGridTab.dataSave(false) ){						
						PO po = currentGridTab.getTableModel().getPO(currentGridTab.getCurrentRow());		
						//Keep master record for details validation 
						if(currentGridTab.equals(gridTab))
							masterRecord = po;

						if( isInsertMode() ) {
							logMsg = Msg.getMsg(Env.getCtx(), "Inserted") + " " + po.toString();
							if (!Util.isEmpty(currentGridTab.getKeyColumnName()) && currentGridTab.getKeyColumnName().endsWith("_ID")) {
								int recordId = currentGridTab.getRecord_ID();
								if (recordId > 0) {
									if (currentGridTab.getTabNo() == 0)
										Env.setContext(Env.getCtx(), currentGridTab.getWindowNo(), currentGridTab.getKeyColumnName(), recordId);
									Env.setContext(Env.getCtx(), currentGridTab.getWindowNo(), currentGridTab.getTabNo(), currentGridTab.getKeyColumnName(), Integer.toString(recordId));
								}
							}
						} else {
							logMsg = Msg.getMsg(Env.getCtx(), "Updated") + " " + po.toString(); 
							if( currentGridTab.equals(gridTab) && sortedtTabMapIndexes.size()>1 )
								currentGridTab.dataRefresh(true); 
						}
					} else {
						ValueNamePair ppE = CLogger.retrieveWarning();
						if (ppE==null)   
							ppE = CLogger.retrieveError();

						String info = null;

						if ( ppE != null )
							info = ppE.getName();
						if ( info == null )
							info = "";

						logMsg = Msg.getMsg(Env.getCtx(), "Error") + " " + Msg.getMsg(Env.getCtx(), "SaveError") + " (" + info + ")";
						currentGridTab.dataIgnore();

						//Problem in the master record
						if( currentGridTab.equals(gridTab) && masterRecord == null ){
							isMasterok = false;
							rowResult.append( "<" + currentGridTab.getTableName() + ">: " );
							rowResult.append(logMsg);
							rowResult.append(" / ");
							break;
						}

						//Problem in the detail record
						if( !currentGridTab.equals(gridTab) && masterRecord != null ){
							isDetailok = false;
							rowResult.append( "<" + currentGridTab.getTableName() + ">: " );
							rowResult.append(logMsg);
							rowResult.append(" / ");
							break;
						}
					}
					
					rowResult.append( "<" + currentGridTab.getTableName() + ">: " );
					rowResult.append(logMsg);
					rowResult.append(" / ");

				} else { //if error true
					currentGridTab.dataIgnore();

					rowResult.append( "<" + currentGridTab.getTableName() + ">: " );
					rowResult.append(logMsg);
					rowResult.append(" / ");

					//Master Failed, thus details cannot be imported 
					if( currentGridTab.equals(gridTab) && masterRecord == null ){
						isMasterok = false;
						break;
					}
					
					//Detail failed
					if( !currentGridTab.equals(gridTab) && masterRecord != null ){
						isDetailok = false;
						break;
					}
				}	
				m_import_mode = importMode;	
			}
		} catch (Exception e) {

			rowResult.append( "<" + currentGridTab.getTableName() + ">: " );
			rowResult.append(Msg.getMsg(Env.getCtx(), "Error") + " " + e);
			rowResult.append(" / ");
			currentGridTab.dataIgnore();

			setError(true);

			//Master Failed, thus details cannot be imported 
			if( currentGridTab.equals(gridTab) && masterRecord == null )
				isMasterok = false;

			if( !currentGridTab.equals(gridTab) && masterRecord != null )
				isDetailok = false;

		} finally {
			m_import_mode = importMode;
		}
		
		return rowResult.toString();

	}//processRecord
	
	private void throwAdempiereException(String msg){
	    throw new AdempiereException(msg);
	}
	
	/**
	 * @param currentRecord
	 * @param total
	 * @return status text
	 */
	private String refreshImportStatus(int currentRecord, int total){
		int percent = currentRecord * 100;
		if (total > 0)
			percent = percent / total;
		else
			percent = 0;
		
		if( percent == 0 ){
			Object[] args = new Object[] {currentRecord};
			return Msg.getMsg(Env.getCtx(), "PreProcessingCVSProgress", args);
		}else{
			Object[] args = new Object[] {currentRecord, total, percent};
			return Msg.getMsg(Env.getCtx(), "PercentProcessingProgress", args);
		}
	}

	/**
	 * 
	 * @param document
	 * @param AD_Process_ID
	 * @return message
	 */
	private String processDocAction(PO document, int AD_Process_ID){
		int AD_Workflow_ID = MProcess.get(Env.getCtx(),AD_Process_ID).getAD_Workflow_ID();  
		
		if (AD_Workflow_ID > 0){
			ProcessInfo wfProcess = new ProcessInfo (document.get_TrxName(),AD_Process_ID,document.get_Table_ID(),document.get_ID());
			wfProcess.setTransactionName(document.get_TrxName());  
			MWFProcess wdPro = ProcessUtil.startWorkFlow(Env.getCtx(),wfProcess, AD_Workflow_ID);
			if(wdPro == null) 
			   return "单据动作执行失败，未找到对应的工作流"; 
			else if (wfProcess.isError())
				return "单据动作执行出错: " + wfProcess.getSummary();
			else
				return "单据动作执行成功: " + wfProcess.getSummary();
		}else {
		   return "未找到对应的工作流，请检查单据动作配置";	
		}
	}
	
	private boolean isInsertMode() {
		return IMPORT_MODE_INSERT.equals(m_import_mode);
	}
	
	private boolean isUpdateMode() {
		return IMPORT_MODE_UPDATE.equals(m_import_mode);
	}

	private boolean isMergeMode() {
		return IMPORT_MODE_MERGE.equals(m_import_mode);
	}

	private boolean isUpdateOrMergeMode() {
		return isUpdateMode() || isMergeMode();
	}

	/**
	 * @param isKey
	 * @param isForeing
	 * @param isDetail
	 * @param headName
	 * @return column name
	 */
	private String getColumnName(boolean isKey ,boolean isForeing ,boolean isDetail , String headName){		
		
		if(isKey){
		   if(headName.indexOf("/") > 0){
			  if(headName.endsWith("K"))
				  headName = headName.substring(0,headName.length()-2);
			  else if (headName.endsWith("KT")){
				  setSingleTrx(true);
				  headName = headName.substring(0,headName.length()-3);
			  }
			  else
				 throw new AdempiereException(Msg.getMsg(Env.getCtx(), "ColumnKey")+" "+headName);
		   } 
		}
		
		if(isForeing)
		   headName = headName.substring(0, headName.indexOf("["));		
		
        if(isDetail){
           headName = headName.substring(headName.indexOf(">")+ 1,headName.length());
           if (headName.indexOf(">")>0)
        	   headName = headName.substring(headName.indexOf(">")+ 1,headName.length());
        }
        return headName;
	}

	// ldh 新增表名翻译方法
	private String translateChineseTableNameToEnglish(String chineseTableName) {
		String adLanguage = Env.getAD_Language(Env.getCtx());
		if (Env.isBaseLanguage(adLanguage, "AD_Table")) {
			return chineseTableName; // 基础语言不需要翻译
		}

		String sql = "SELECT e.TableName FROM AD_Table e " + "JOIN AD_Table_Trl t ON e.AD_Table_ID = t.AD_Table_ID "
				+ "WHERE t.Name = ? AND t.AD_Language = ?";

		String englishTableName = DB.getSQLValueString(null, sql, chineseTableName, adLanguage);
		return Util.isEmpty(englishTableName) ? chineseTableName : englishTableName;
	}
	
	/**
	 * Get extra fields for Location display type
	 * @param header
	 * @param tableName
	 * @param idx
	 * @return Extra fields for Location display type
	 */
	private List<GridField> getSpecialMColumn(List<String> header, String tableName, int idx) {
		
		List<GridField> lsField = new ArrayList<GridField>();		
		if (tableName.equals(MTable.getTableName(Env.getCtx(), MLocation.Table_ID))){
			GridWindowVO gWindowVO = Env.getMWindowVO(0,121,0); 
			GridWindow m_mWindow = new GridWindow (gWindowVO);
			GridTab m_mTab = m_mWindow.getTab(0);
			m_mWindow.initTab(0);
			for(int i = idx;i< header.size();i++){
				if (header.get(i).contains(MTable.getTableName(Env.getCtx(), MLocation.Table_ID))) {
					boolean isKeyColumn = header.get(i).indexOf("/") > 0;
					boolean isForeing 	= header.get(i).indexOf("[") > 0 && header.get(i).indexOf("]")>0;
					String  columnName  = getColumnName (isKeyColumn,isForeing,true,header.get(i)); 
					GridField field  = m_mTab.getField(columnName);
					if (field == null)
						throw new AdempiereException(Msg.getMsg(Env.getCtx(), "FieldNotFound", new Object[] {header.get(i)}));
					
					lsField.add(field);
				}else
					break;
			}
		}
		return lsField;
	}

	/**
	 * Get column value from map, order by header 
	 * @param header
	 * @param map
	 * @return Value list for row
	 */
	private List<Object> getOrderedRowFromMap (List<String> header,Map<String, Object> map){
		List<Object> tmpRow= new ArrayList<Object>();  
		for (int i = 0; i < header.size(); i++)
			tmpRow.add(null);
		
		for(Map.Entry<String, Object> record : map.entrySet()) {
			String Column =record.getKey();
			Object value  = record.getValue();
		    int toIndx= header.indexOf(Column);
		    tmpRow.set(toIndx, value);
        }	
		return tmpRow;	
	}
	
	/**
	 * Pre-process tmpRow and looks for error
	 * @param gridTab
	 * @param header
	 * @param tmpRow
	 * @param startindx
	 * @param endindx
	 * @return error message or null
	 */
	private StringBuilder preprocessRow (GridTab gridTab,List<String> header,List<Object> tmpRow,int startindx,int endindx){
		
	    boolean isEmptyRow = true;
	    boolean isAddressValidated = false ;
	    StringBuilder  mandatoryColumns = new StringBuilder();
	    for(int i = startindx;  i < endindx +1; i++){
			String columnName = header.get(i);	
			Object value = tmpRow.get(i); 	
			//Validate Address
			if (!"C_Location".equals(gridTab.getTableName()))
			{
				//Validate Address
				if(header.get(i).contains(MTable.getTableName(Env.getCtx(),MLocation.Table_ID)) && !isAddressValidated){
				   StringBuilder specialColumns = new StringBuilder();
				   specialColumns = validateSpecialFields(gridTab,header,tmpRow,i,"C_Location_ID");
				   isAddressValidated = true;
				   if(specialColumns==null)
					  continue;   
				   else
					  return specialColumns;     
				}else if (header.get(i).contains(MTable.getTableName(Env.getCtx(),MLocation.Table_ID)) && isAddressValidated){
					continue;
				}
			}
			
			if(value!=null)
			   isEmptyRow=false;
			
			if (log.isLoggable(Level.FINE)) log.fine("Setting " + columnName + " to " + value);

			boolean isKeyColumn = columnName.indexOf("/") > 0;
			boolean isForeing 	= columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
			boolean isDetail    = columnName.indexOf(">") > 0;
			columnName = getColumnName(isKeyColumn,isForeing,isDetail,columnName);
			String foreignColumn=null; 
			if(isForeing) 
			   foreignColumn = header.get(i).substring(header.get(i).indexOf("[")+1, header.get(i).indexOf("]"));
		
			GridField field=gridTab.getField(columnName);					
			if (field == null) 
				return new StringBuilder(Msg.getMsg(Env.getCtx(), "NotAWindowField" , new Object[] {header.get(i)}));

			if (field.isParentValue())
				continue;
			
			if (!(field.isDisplayed() || field.isDisplayedGrid())) 
				return new StringBuilder(Msg.getMsg(Env.getCtx(), "FieldNotDisplayed",new Object[] {header.get(i)}));
			
			MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			boolean isWrongValueForMandatory = false;
			if (field.isMandatory(false) || column.isMandatory()){
				if (isInsertMode() && value == null && field.getDefault()==null){
					isWrongValueForMandatory = true;
				}else if (!isInsertMode() && "(null)".equals(value)){
					isWrongValueForMandatory = true;
				}
			}
			
			if(isWrongValueForMandatory){ 
				mandatoryColumns.append(" / ");
				mandatoryColumns.append(header.get(i));
			} 
			
			if (isForeing && value != null && !"(null)".equals(value)){
				String foreignTable = column.getReferenceTableName();
				Object idS = null;
				if("AD_Ref_List".equals(foreignTable))
				   idS = resolveForeignList(column,foreignColumn,value,null);
				else 
				   idS = resolveForeign(foreignTable,foreignColumn,value,field,null);
				
				if(idS == null){	
				   //it could be that record still doesn't exist if import mode is inserting or merging   	
				   if(isUpdateMode())
				     return new StringBuilder(Msg.getMsg(Env.getCtx(),(idS instanceof Integer && (int)idS==-2)?"ForeignMultipleResolved":"ForeignNotResolved",new Object[]{header.get(i),value}));
				}
			} else {
				// TODO: we could validate length of string or min/max
			}
		}
	    
		if(mandatoryColumns.length()>1 && !isEmptyRow) 
		   return new StringBuilder(Msg.getMsg(Env.getCtx(), "FillMandatory")+" "+mandatoryColumns);
		else
		   return null;		
	}
	
	/**
	 * Validate extra fields for DisplayType.Location.
	 * @param gridTab
	 * @param header
	 * @param tmpRow
	 * @param i
	 * @param sField
	 * @return error message or null
	 */
	private StringBuilder validateSpecialFields(GridTab gridTab,List<String> header,List<Object> tmpRow,int i,String sField){

	   GridField field = gridTab.getField(sField);
	   if(field == null) 
		  return new StringBuilder(Msg.getMsg(Env.getCtx(), "NotAWindowField",new Object[] {sField}));
	    
	   if(!(field.isDisplayed() || field.isDisplayedGrid())) 
		  return new StringBuilder(Msg.getMsg(Env.getCtx(), "FieldNotDisplayed",new Object[] {field.getColumnName()}));
	   
	   if (header.get(i).contains(MTable.getTableName(Env.getCtx(),MLocation.Table_ID)))
	   {
		   //without Country any address would be invalid 
		   boolean thereIsCountry = false ;
		   boolean isEmptyRow = true;
		   Object countryId = null;
		   int regionIndex = -1;
		   for(int j= i;j< header.size();j++){
			   if(!header.get(j).contains(MTable.getTableName(Env.getCtx(),MLocation.Table_ID)))
			       break;
			    //validate if location contains its parent table 
				if(!header.get(j).contains(gridTab.getTableName()))
				    return new StringBuilder().append("地址列名格式错误，列名中必须包含其父表名：")
				    		   .append(gridTab.getTableName().toString())
				    		   .append("[").append(header.get(j)).append("]");
				
			   String columnName = header.get(j);	
			   Object value = tmpRow.get(j);   
			   if(value!=null){ 
				  if(columnName.contains("C_Country_ID")) {
					 thereIsCountry= true;
					 countryId = value;
				  }
			   }else
				  continue;
			   			    
			   boolean isKeyColumn = columnName.indexOf("/") > 0;
			   boolean isForeign   = columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
			   boolean isDetail    = columnName.indexOf(">") > 0;
			   String  foreignColumn = null; 
			   columnName = getColumnName(isKeyColumn,isForeign,isDetail,columnName);
			   if(isForeign) 
				  foreignColumn = header.get(j).substring(header.get(j).indexOf("[")+1, header.get(j).indexOf("]"));
			   
			   if(isForeign && !"(null)".equals(value)){ 
			      String foreignTable = columnName.substring(0,columnName.length()-3);
			      if ("C_Region".equals(foreignTable)) {
			    	  regionIndex = j;
			      } else {
			    	  Object id = resolveForeign(foreignTable,foreignColumn,value,field,null);
					  if (id == null || (id instanceof Integer && (int)id < 0))
						  return new StringBuilder(Msg.getMsg(Env.getCtx(),(id instanceof Integer && (int)id==-2)?"ForeignMultipleResolved":"ForeignNotResolved" ,new Object[]{header.get(j),value}));   
					  if (columnName.contains("C_Country_ID")) {
						  countryId = id;
					  }
			      }
			   }	   
			   isEmptyRow=false;
	      }	   
		  MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());		
		  if((field.isMandatory(true) || column.isMandatory()) && !isEmptyRow && !thereIsCountry) 
			  return new StringBuilder(Msg.getMsg(Env.getCtx(), "FillMandatory")+" "+field.getColumnName()+"["+"C_Country_ID]");
		  
		  if (countryId != null && regionIndex != -1) {
			  String columnName = header.get(regionIndex);	
			  Object value = tmpRow.get(regionIndex);
			  
			  boolean isKeyColumn = columnName.indexOf("/") > 0;
			  boolean isForeign = columnName.indexOf("[") > 0 && columnName.indexOf("]") > 0;
			  boolean isDetail = columnName.indexOf(">") > 0;
			  String foreignColumn = null;
			  columnName = getColumnName(isKeyColumn, isForeign, isDetail, columnName);
			  if (isForeign)
				foreignColumn = header.get(regionIndex).substring(header.get(regionIndex).indexOf("[") + 1, header.get(regionIndex).indexOf("]"));

			  if (isForeign && !"(null)".equals(value)) {
				int id = resolveForeignRegionByCountry(foreignColumn, value, field, null, (Integer) countryId);
				if (id < 0)
					return new StringBuilder(Msg.getMsg(Env.getCtx(), id == -2 ? "ForeignMultipleResolved" : "ForeignNotResolved",new Object[] { header.get(regionIndex), value }));
			  }
		  }
			
	   }
	   return null;
	}	
	
	/**
	 * Process row for import
	 * @param gridTab
	 * @param header
	 * @param map
	 * @param startindx
	 * @param endindx
	 * @param masterRecord
	 * @param trx
	 * @return error message or null
	 */
	private String proccessRow(GridTab gridTab,List<String> header, Map<String, Object> map,int startindx,int endindx,PO masterRecord,Trx trx){
		
		String logMsg = null;	
		boolean isThereRow = false;
		MLocation address = null;
		List<String> parentColumns = new ArrayList<String>(); 
		int regionIndex = -1;
		for(int i = startindx ; i < endindx + 1 ; i++){
			String columnName = header.get(i);
			Object value = map.get(header.get(i));
			boolean isDetail = false;
			if(value == null)
			   continue;
				
			if (columnName.endsWith("_ID") && "0".equals(value) && ! MTable.isZeroIDTable(gridTab.getTableName()))
			   continue;
				
			boolean isKeyColumn= columnName.indexOf("/") > 0;
			boolean isForeign  = columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
			isDetail   = columnName.indexOf(">") > 0;
			columnName = getColumnName(isKeyColumn,isForeign,isDetail,columnName);
			String foreignColumn = null;
			Object setValue = null;
			
			if(isForeign) 
			   foreignColumn = header.get(i).substring(header.get(i).indexOf("[")+1,header.get(i).indexOf("]"));
			if(!isForeign && !isKeyColumn && ("AD_Language".equals(columnName) || "EntityType".equals(columnName))) {
				setValue = value;
				GridField field = gridTab.getField(columnName);
			    logMsg = gridTab.setValue(field,setValue);
			    if(logMsg!=null && logMsg.equals(""))
			    	logMsg= null;
			}else if(!"C_Location".equals(gridTab.getTableName()) && header.get(i).contains(MTable.getTableName(Env.getCtx(),MLocation.Table_ID))){
		    
				if(address == null){
				    if(isInsertMode()){
					   address = new MLocation (Env.getCtx(),0,trx.getTrxName());	   
				    }else{
				       Object location = gridTab.getValue("C_Location_ID")==null?0:gridTab.getValue("C_Location_ID").toString();
					   int C_Location_ID = Integer.parseInt(location.toString());  
					   address =  new MLocation (Env.getCtx(),C_Location_ID,trx.getTrxName());	
				    }
				}
				GridField field = gridTab.getField(columnName);
				if(!"(null)".equals(value.toString().trim())){
				   if(isForeign) {
					  String foreignTable = columnName.substring(0,columnName.length()-3);
					  if("C_Region".equals(foreignTable))
						  regionIndex = i;
					  else
						  setValue = resolveForeign(foreignTable,foreignColumn,value,field,trx);
					  if("C_City".equals(foreignTable))
						 address.setCity(value.toString());  
					}else
					  setValue = value;			
				}
				address.set_ValueOfColumn(columnName,setValue);
			}else{
				if(isKeyColumn && isUpdateMode())
				   continue;
				
				GridField field = gridTab.getField(columnName);
				if (field.isParentValue()){
					
					if("(null)".equals(value.toString())){
					   logMsg = Msg.getMsg(Env.getCtx(),"NoParentDelete", new Object[] {header.get(i)}); 
					   break;
					}
					
					if(isForeign && masterRecord!=null){
					   if (masterRecord.get_Value(foreignColumn).toString().equals(value)){
						   logMsg = gridTab.setValue(field,masterRecord.get_ID());
						   if(logMsg.equals(""))
							  logMsg= null;
						   else break;
					   }else{
						   if(value!=null){					      
						      logMsg = header.get(i)+" - "+Msg.getMsg(Env.getCtx(),"DiffParentValue", new Object[] {masterRecord.get_Value(foreignColumn).toString(),value});
						      break;
						   }   
					   }
					}else if(isForeign && masterRecord==null && gridTab.getTabLevel()>0){
						Object master =gridTab.getParentTab().getValue(foreignColumn);
						if (master!=null && value!=null && !master.toString().equals(value)){
							logMsg = header.get(i)+" - "+Msg.getMsg(Env.getCtx(),"DiffParentValue", new Object[] {master.toString(),value});
							break;
						}			
					}else if (masterRecord==null && isDetail){
						MColumn column = MColumn.get(Env.getCtx(),field.getAD_Column_ID());
						String foreignTable = column.getReferenceTableName();
						Object idS = null;
						
						if ("AD_Ref_List".equals(foreignTable)) 
							idS = resolveForeignList(column, foreignColumn, value,trx);
						else 
							idS = resolveForeign(foreignTable,foreignColumn,value, field, trx);
						
					    if (idS == null || (idS instanceof Integer && (int)idS < 0))
						   return Msg.getMsg(Env.getCtx(),(idS instanceof Integer && (int)idS==-2)?"ForeignMultipleResolved":"ForeignNotResolved",new Object[]{header.get(i),value});
						
						if (idS instanceof Integer && (int)idS >= 0)
						   logMsg = gridTab.setValue(field,idS);
						else if (idS != null)
						   logMsg = gridTab.setValue(field,idS);
						
						if(logMsg !=null && logMsg.equals(""))
						   logMsg = null;
						else break;
					}
					parentColumns.add(columnName);	
					continue;
				}
				//this field should not be inserted or updated 
				if(!field.isDisplayed(true)) 
					continue;
					
				if("(null)".equals(value.toString().trim())){
				   logMsg = gridTab.setValue(field,null);	
				   if(logMsg.equals(""))
					  logMsg= null;
				   else break;
				}else{
				   
				   MColumn column = MColumn.get(Env.getCtx(),field.getAD_Column_ID());
				   if (isForeign){
						String foreignTable = column.getReferenceTableName();
						if ("AD_Ref_List".equals(foreignTable)) {
							String idS = resolveForeignList(column, foreignColumn, value,trx);
							if(idS == null)	
							   return Msg.getMsg(Env.getCtx(),"ForeignNotResolved",new Object[]{header.get(i),value});
							
							setValue = idS;
							isThereRow =true;
						} else {
							
							Object id = resolveForeign(foreignTable, foreignColumn, value,field,trx);
						    if (id == null || (id instanceof Integer && (int)id < 0))
								return Msg.getMsg(Env.getCtx(),(id instanceof Integer && (int)id==-2)?"ForeignMultipleResolved":"ForeignNotResolved",new Object[]{header.get(i),value});

							setValue = id;
							if (field.isParentValue()) {
								Object actualId = field.getValue();
								if (actualId != null && ! actualId.equals(id)) {
									logMsg = Msg.getMsg(Env.getCtx(), "ParentCannotChange",new Object[]{header.get(i)});
									break;
								}
							}
							isThereRow =true;
						}
				   }else{
					   if(value != null) {
						  if(value instanceof java.util.Date)
							 value = new Timestamp(((java.util.Date)value).getTime());
							
						  if(DisplayType.Payment == field.getDisplayType()){
   							 String oldValue = value.toString(); 
							 for(ValueNamePair pList: MRefList.getList(Env.getCtx(),REFERENCE_PAYMENTRULE,false)){
								 if(pList.getName().equals(oldValue.toString())){
									oldValue = pList.getValue(); 
									break;
								 }
							 }
							 if(!value.toString().equals(oldValue)) 
							     value = oldValue;
							 else
								 return Msg.getMsg(Env.getCtx(),"ForeignNotResolved",new Object[]{header.get(i),value}); 
						  }else if(DisplayType.Button == field.getDisplayType()){
							 if(column.getAD_Reference_Value_ID()== REFERENCE_DOCUMENTACTION){
								String oldValue = value.toString(); 
							    for(ValueNamePair pList: MRefList.getList(Env.getCtx(),REFERENCE_DOCUMENTACTION,false)){
								    if(pList.getName().equals(oldValue.toString())){
									   oldValue = pList.getValue(); 
									   break;
									}
								}
								if(!value.toString().equals(oldValue)) 
									value = oldValue;
							    else
								    return Msg.getMsg(Env.getCtx(),"ForeignNotResolved",new Object[]{header.get(i),value});  
							 }else{
									return "字段 [" + column.getColumnName() + "] 是按钮类型，不支持通过 CSV 导入";
							 } 
						  }  else if (DisplayType.isNumeric(field.getDisplayType()) || DisplayType.isID(field.getDisplayType())) {
							  if (columnName.endsWith("_ID")) {
								  if (!(value instanceof Integer)) {
									  Integer idValue = Integer.valueOf(value.toString());
									  value = idValue;
								  }
							  } else if (!(value instanceof BigDecimal)) {
								  BigDecimal decValue = new BigDecimal(value.toString());
								  value = decValue;
							  }
						  }
						  setValue = value;
						  isThereRow =true;
					   }
				   }
									   
				   if(setValue != null) {
					  Object oldValue = gridTab.getValue(field);
					  if (isValueChanged(oldValue, setValue)) {
						  if (!field.isEditable(true)) {
							  return Msg.getMsg(Env.getCtx(), "FieldIsReadOnly",new Object[] {header.get(i)});
						  }
						  logMsg = gridTab.setValue(field,setValue);
					  } else {
						  logMsg = "";
					  }
				   }
				   
				   if(logMsg!=null && logMsg.equals(""))
					  logMsg= null;
				   else break;
			   }
			}	
		}
			 
		if(address!=null){  
			if (regionIndex != -1 && address.getC_Country_ID() > 0) {
				String columnName = header.get(regionIndex);
				Object value = map.get(header.get(regionIndex));
				boolean isDetail = false;
				boolean isKeyColumn= columnName.indexOf("/") > 0;
				boolean isForeign  = columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
				isDetail   = columnName.indexOf(">") > 0;
				columnName = getColumnName(isKeyColumn,isForeign,isDetail,columnName);
				String foreignColumn = null;
				Object setValue = null;
				
				if(isForeign) 
				   foreignColumn = header.get(regionIndex).substring(header.get(regionIndex).indexOf("[")+1,header.get(regionIndex).indexOf("]"));
				
					GridField field = gridTab.getField(columnName);
					if(!"(null)".equals(value.toString().trim())){
					   if(isForeign) {
						  setValue = resolveForeignRegionByCountry(foreignColumn, value, field, trx, address.getC_Country_ID());
						}else
						  setValue = value;			
					}
					address.set_ValueOfColumn(columnName,setValue);
				}
			if (!address.save()){
				logMsg = "地址保存失败：" + address + "，原因：" + CLogger.retrieveError();
			}else {
				logMsg = gridTab.setValue("C_Location_ID",address.getC_Location_ID());
				if(logMsg.equals(""))
				   logMsg= null;
				else 
				   return logMsg;
				
				isThereRow =true;	
			}
		}	
	    
		boolean checkParentKey = parentColumns.size()!=gridTab.getParentColumnNames().size();
		if(isThereRow && logMsg==null && (masterRecord!=null||gridTab.getParentTab()!=null) && checkParentKey){
			for(String linkColumn : gridTab.getParentColumnNames()){
				String columnName = linkColumn;
				Object setValue   = masterRecord != null ? masterRecord.get_Value(linkColumn) : gridTab.getParentTab().getValue(linkColumn);
		        //resolve missing key 
				if(setValue==null){
			       columnName = null;
		           for(int j = startindx;j < endindx + 1;j++){
		        	   if(header.get(j).contains(linkColumn)){
		        		   columnName = header.get(j);
		        		   setValue   = map.get(columnName);
		        		   break;
		        	   }
		           }
		           if( columnName!=null ){
					   String foreignColumn = null;						
					   boolean isForeing = columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
					   if(isForeing) 
						  foreignColumn  = columnName.substring(columnName.indexOf("[")+1,columnName.indexOf("]"));   
			           
					   columnName = getColumnName(false,isForeing,true,columnName);
					   GridField field = gridTab.getField(columnName);
					   MColumn column = MColumn.get(Env.getCtx(),field.getAD_Column_ID());
					   if (isForeing){
							String foreignTable = column.getReferenceTableName();
							if ("AD_Ref_List".equals(foreignTable)) {
								String idS = resolveForeignList(column,foreignColumn,setValue,trx);
								if(idS == null)	
								   return Msg.getMsg(Env.getCtx(),"ForeignNotResolved",new Object[]{columnName,setValue});
								
								setValue = idS;
							} else {
								Object id = resolveForeign(foreignTable, foreignColumn, setValue, field, trx);
							    if (id == null || (id instanceof Integer && (int)id < 0))
								   return Msg.getMsg(Env.getCtx(),(id instanceof Integer && (int)id==-2)?"ForeignMultipleResolved":"ForeignNotResolved",new Object[]{columnName,setValue});
								
								setValue = id;
							}
					   }	   
		           }else{ 
						logMsg = "找不到父关联字段的关键值：" + linkColumn;
		    	       break; 
		           }
			    }
				logMsg = gridTab.setValue(linkColumn,setValue);		   
			    if(logMsg.equals(""))
			       logMsg= null;
			    else continue;
		   }
		}
		
		if(logMsg == null && !isThereRow)
		   logMsg ="NO_DATA_TO_IMPORT";
		
		return logMsg;
	}
	
	/**
	 * @param field
	 * @return CellProcessor
	 */
	private CellProcessor getProccesorFromColumn(GridField field) {
		// TODO: List columns can use RequireSubStr constraint
		if (DisplayType.Date == field.getDisplayType()) {
			return (new Optional(new ParseDate(DisplayType.DEFAULT_DATE_FORMAT)));
		} else if (DisplayType.DateTime == field.getDisplayType()) {
			return (new Optional(new ParseDate(DisplayType.DEFAULT_TIMESTAMP_FORMAT)));
		} else if (DisplayType.Time == field.getDisplayType()) {
			return (new Optional(new ParseDate(DisplayType.DEFAULT_TIME_FORMAT)));
		} else if (DisplayType.Integer == field.getDisplayType()) {
			return (new Optional(new ParseInt()));
		} else if (DisplayType.isNumeric(field.getDisplayType())) {
			return (new Optional(new ParseBigDecimal(new DecimalFormatSymbols(Language.getLoginLanguage().getLocale()))));
		} else if (DisplayType.YesNo == field.getDisplayType()) {
			return (new Optional(new ParseBool("y", "n")));
		} else if (DisplayType.TextLong == field.getDisplayType() || DisplayType.JSON == field.getDisplayType()) {
			return (new Optional(new StrMinMax(1, Long.MAX_VALUE)));
		} else if (DisplayType.isText(field.getDisplayType())) {
			return (new Optional(new StrMinMax(1, field.getFieldLength())));
		} else {  // optional lookups and text
			return null;
		}
	}
	
	/**
	 * @param gridTab
	 * @param map
	 * @param header
	 * @param startindx
	 * @param endindx
	 * @param masterRecord
	 * @param trx
	 * @return error message or null
	 */
	private String areValidKeysAndColumns(GridTab gridTab, Map<String, Object> map,List<String> header,int startindx,int endindx,PO masterRecord,Trx trx){
		MQuery pquery = new MQuery(gridTab.getAD_Table_ID());
		String logMsg= null;
		Object tmpValue=null;
		String columnwithKey=null;
		Object setValue = null;
		List<String> parentColumns = new ArrayList<String>(); 
		//Process columnKeys + Foreign to add restrictions.
		for (int i = startindx ; i < endindx + 1 ; i++){					  
		    boolean isKeyColumn = header.get(i).indexOf("/") > 0 && ( header.get(i).endsWith("K") || header.get(i).endsWith("KT"));	
		    if(isKeyColumn && ("C_Location".equals(gridTab.getTableName()) || !header.get(i).contains(MTable.getTableName(Env.getCtx(),MLocation.Table_ID)))){  
			   boolean isForeing = header.get(i).indexOf("[") > 0 && header.get(i).indexOf("]")>0;
			   boolean isDetail  = header.get(i).indexOf(">") > 0;
			   columnwithKey = getColumnName(isKeyColumn,isForeing,isDetail,header.get(i));
			   
			   if(map.get(header.get(i)) instanceof java.util.Date)
				  tmpValue = new Timestamp(((java.util.Date)map.get(header.get(i))).getTime());
			   else 
				  tmpValue = map.get(header.get(i));
				
			   if (tmpValue==null)
				   continue;
			   
			   GridField field = gridTab.getField(columnwithKey);
			   MColumn column  = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			   if(field.isParentValue()){
				  parentColumns.add(column.getColumnName());
			   }
			   String foreignColumn = null;		   
			   if(isForeing){
				  foreignColumn  = header.get(i).substring(header.get(i).indexOf("[")+1,header.get(i).indexOf("]"));
				  String foreignTable = column.getReferenceTableName();
				  if ("AD_Ref_List".equals(foreignTable)) {
					  String idS = resolveForeignList(column, foreignColumn, tmpValue,trx);
					  setValue = idS;
				  }else {
					  Object id = resolveForeign(foreignTable, foreignColumn, tmpValue,field,trx);
					  setValue = id;
	             }
			   }else{
				   setValue = tmpValue ;
			   }
			   pquery.addRestriction(columnwithKey,MQuery.EQUAL,setValue);
		   }
		}
		
		if (pquery.getRestrictionCount() > 0){
			//check out if parent keys were completed properly 
			if (gridTab.isDetail()){
				for(String linkColumn : gridTab.getParentColumnNames()){
					if(!pquery.getWhereClause().contains(linkColumn)){
						Object value = masterRecord!=null
								    ? masterRecord.get_Value(linkColumn)
									: gridTab.getParentTab() != null ? gridTab.getParentTab().getValue(linkColumn) : null;
						//resolve key
						if(value==null){
						   String columnName = null;
				           for(int j = startindx;j<endindx + 1;j++){
				        	   if(header.get(j).contains(linkColumn)){
				        		   columnName = header.get(j);
				        		   value = map.get(header.get(j));
				        		   break;
				        	   }
				           }
				           if(columnName!=null){
				        	   boolean isForeing = columnName.indexOf("[") > 0 && columnName.indexOf("]")>0;
							   columnwithKey     = getColumnName(false,isForeing,true,columnName);
							   GridField field = gridTab.getField(columnwithKey);
							   MColumn column    = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
							   String foreignColumn = null;		   
							   if(isForeing){
								  foreignColumn       = columnName.substring(columnName.indexOf("[")+1,columnName.indexOf("]"));
								  String foreignTable = column.getReferenceTableName();
								  if ("AD_Ref_List".equals(foreignTable)) {
									  String idS = resolveForeignList(column,foreignColumn,value,trx);
									  value = idS;
								  }else {
									  value = resolveForeign(foreignTable,foreignColumn,value,field,trx);
					             }
							   }
				           }else{ //mandatory key not found 
				    	       return Msg.getMsg(Env.getCtx(),"FillMandatory")+" "+linkColumn;   
				           }
					    }
						if(value!=null)
						   pquery.addRestriction(linkColumn,MQuery.EQUAL,value);  	
					}
				}	
			}
			gridTab.getTableModel().dataRequery(pquery.getWhereClause(), false, 0, false);
	    	if (isInsertMode()){
				if(gridTab.getTableModel().getRowCount()>=1)
				   logMsg = Msg.getMsg(Env.getCtx(), "AlreadyExists")+" "+pquery;
				else  
				  return null;	
			}
			if (isUpdateMode()){
				if(gridTab.getTableModel().getRowCount()==1){
			       gridTab.navigateCurrent();
				   return null;
				}
				else if(gridTab.getTableModel().getRowCount()<=0)
				   logMsg = Msg.getMsg(Env.getCtx(), "not.found")+" "+pquery; 
				else if(gridTab.getTableModel().getRowCount()>1)
			       logMsg = Msg.getMsg(Env.getCtx(),"TooManyRows")+" "+pquery; 
			}
		    if (isMergeMode()){
			   if(gridTab.getTableModel().getRowCount()==1){
			      gridTab.navigateCurrent();
				  m_import_mode = IMPORT_MODE_UPDATE;
			   }else if(gridTab.getTableModel().getRowCount()<=0)
				  m_import_mode = IMPORT_MODE_INSERT;
			   else if(gridTab.getTableModel().getRowCount()>1)
				  logMsg = Msg.getMsg(Env.getCtx(),"TooManyRows")+" "+pquery; 	
		   }
	   }
		
	   return logMsg;
	}
	
	/**
	 * @param column
	 * @param foreignColumn
	 * @param value
	 * @param trx
	 * @return value from matching ad_ref_list record
	 */
	private String resolveForeignList(MColumn column, String foreignColumn, Object value ,Trx trx) {
		String idS = null;
		String trxName = (trx!=null?trx.getTrxName():null); 
		StringBuilder select = new StringBuilder("SELECT Value FROM AD_Ref_List WHERE ")
			.append(foreignColumn).append("=? AND AD_Reference_ID=? AND IsActive='Y'");
		idS = DB.getSQLValueStringEx(trxName, select.toString(), value, column.getAD_Reference_Value_ID());
		return idS;
	}

	/**
	 * @param foreignTable
	 * @param foreignColumn
	 * @param value
	 * @param field
	 * @param trx
	 * @return -3 for not found, -2 for more than 1 match and > 0 for foreign id
	 */
	private Object resolveForeign(String foreignTable, String foreignColumn, Object value, GridField field, Trx trx) {
		boolean systemAccess = false;
		if (!"AD_Client".equals(foreignTable)) {
			MTable ft = MTable.get(Env.getCtx(), foreignTable);
			String accessLevel = ft.getAccessLevel();
			if (   MTable.ACCESSLEVEL_All.equals(accessLevel)
				|| MTable.ACCESSLEVEL_SystemOnly.equals(accessLevel)
				|| MTable.ACCESSLEVEL_SystemPlusClient.equals(accessLevel)) {
				systemAccess = true;
			}
		}
		int thisClientId = Env.getAD_Client_ID(Env.getCtx());

		String trxName = (trx!=null?trx.getTrxName():null); 

		StringBuilder postSelect = new StringBuilder(" FROM ")
			.append(foreignTable).append(" WHERE ")
			.append(foreignColumn).append("=? AND IsActive='Y' AND AD_Client_ID=?");
	  if (field != null ) {
		if (!Util.isEmpty(field.getVO().ValidationCode)) {
			String dynamicValid = Env.parseContext(Env.getCtx(), field.getWindowNo(), field.getGridTab().getTabNo(), field.getVO().ValidationCode, false);
			if (Util.isEmpty(dynamicValid)) {
				return 0;// it's parse error but simple consider like ForeignNotResolved
			}else {
				postSelect.append(" AND (").append(dynamicValid).append(")");
			}
		}
		int ref = field.getVO().displayType;
		int refval = field.getVO().AD_Reference_Value_ID;
		if (refval > 0 && (ref == DisplayType.Table || ref == DisplayType.Search)) {
			final MRefTable refTable = new Query(Env.getCtx(), MRefTable.Table_Name, "AD_Reference_ID=?", trxName)
					.setParameters(refval)
					.firstOnly();
			String whereClause = refTable.getWhereClause();
			if (!Util.isEmpty(whereClause)) {
				String dynamicValid = Env.parseContext(Env.getCtx(), field.getWindowNo(), field.getGridTab().getTabNo(), whereClause, false);
				if (Util.isEmpty(dynamicValid)) {
					return 0;// it's parse error but simple consider like ForeignNotResolved
				}else {
					postSelect.append(" AND (").append(dynamicValid).append(")");
				}
			}
		}
	  }
		StringBuilder selectCount = new StringBuilder("SELECT COUNT(*)").append(postSelect);
		MTable forTab = MTable.get(Env.getCtx(), foreignTable);
		StringBuilder selectId = new StringBuilder("SELECT ").append(forTab.getKeyColumns()[0]).append(postSelect);
		int count = DB.getSQLValueEx(trxName, selectCount.toString(), value, thisClientId);
		if (count == 1) { // single value found, OK
			if (forTab.isUUIDKeyTable())
				return DB.getSQLValueStringEx(trxName, selectId.toString(), value, thisClientId);
			else
				return DB.getSQLValueEx(trxName, selectId.toString(), value, thisClientId);
		} else if (count > 1) { // multiple values found, error ForeignMultipleResolved
			return -2;
		} else if (count == 0) { // no values found, error ForeignNotResolved
			if (systemAccess && thisClientId != 0) {
				// not found in client, try with System
				count = DB.getSQLValueEx(trxName, selectCount.toString(), value, 0 /* System */);
				if (count == 1) { // single value found, OK
					if (forTab.isUUIDKeyTable())
						return DB.getSQLValueStringEx(trxName, selectId.toString(), value,  0 /* System */);
					else
						return DB.getSQLValueEx(trxName, selectId.toString(), value,  0 /* System */);
				} else if (count > 1) { // multiple values found, error ForeignMultipleResolved
					return -2;
				}
			}
		}
		return -3;   // no values found, error ForeignNotResolved
	}

	private int resolveForeignRegionByCountry(String foreignColumn, Object regionValue, GridField field, Trx trx, int countryId) {
		String foreignTable = "C_Region";
		boolean systemAccess = true;
		int thisClientId = Env.getAD_Client_ID(Env.getCtx());
		
		StringBuilder postSelect = new StringBuilder(" FROM ")
				.append(foreignTable).append(" WHERE ")
				.append(foreignColumn).append("=? AND C_Country_ID=? AND IsActive='Y' AND AD_Client_ID=?");
		
		StringBuilder selectCount = new StringBuilder("SELECT COUNT(*)").append(postSelect);
		StringBuilder selectId = new StringBuilder("SELECT ").append(foreignTable).append("_ID").append(postSelect);
		int count = DB.getSQLValueEx(trxName, selectCount.toString(), regionValue, countryId, thisClientId);
		if (count == 1) { // single value found, OK
			return DB.getSQLValueEx(trxName, selectId.toString(), regionValue, countryId, thisClientId);
		} else if (count > 1) { // multiple values found, error ForeignMultipleResolved
			return -2;
		} else if (count == 0) { // no values found, error ForeignNotResolved
			if (systemAccess && thisClientId != 0) {
				// not found in client, try with System
				count = DB.getSQLValueEx(trxName, selectCount.toString(), regionValue, countryId, 0 /* System */);
				if (count == 1) { // single value found, OK
					return DB.getSQLValueEx(trxName, selectId.toString(), regionValue, countryId, 0 /* System */);
				} else if (count > 1) { // multiple values found, error ForeignMultipleResolved
					return -2;
				}
			}
		}
		return -3;   // no values found, error ForeignNotResolved
	}
	
	//Copy from GridTable
	@SuppressWarnings("unchecked")
	private boolean	isValueChanged(Object oldValue, Object value)
	{
		if ( isNotNullAndIsEmpty(oldValue) ) {
			oldValue = null;
		}

		if ( isNotNullAndIsEmpty(value) ) {
			value = null;
		}

		boolean bChanged = (oldValue == null && value != null) 
							|| (oldValue != null && value == null);

		if (!bChanged && oldValue != null)
		{
			if (oldValue.getClass().equals(value.getClass()))
			{
				if (oldValue instanceof Comparable<?>)
				{
					bChanged = (((Comparable<Object>)oldValue).compareTo(value) != 0);
				}
				else
				{
					bChanged = !oldValue.equals(value);
				}
			}
			else if(value != null)
			{
				bChanged = !(oldValue.toString().equals(value.toString()));
			}
		}
		return bChanged;	
	}
	
	/**
	 * @param value
	 * @return true if value is not null and not an empty string
	 */
	private boolean isNotNullAndIsEmpty (Object value) {
		if (value != null 
				&& (value instanceof String) 
				&& value.toString().equals("")
			) 
		{
			return true;
		} else {
			return false;
		}

	}
	
	@Override
	public String getFileExtension() {
		return "csv";
	}

	@Override
	public String getFileExtensionLabel() {
		return Msg.getMsg(Env.getCtx(), "FileCSV");
	}

	@Override
	public String getContentType() {
		return "application/csv";
	}

	@Override
	public String getSuggestedFileName(GridTab gridTab) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String dt = sdf.format(cal.getTime());
		String localFile = "Import_" + gridTab.getTableName() + "_" + dt
				+ (m_isError ? "_err" : "_log")
				+ "." + getFileExtension();
		return localFile;
	}
	
    public boolean isError() {
		return error;
	}

	public boolean isSingleTrx() {
		return isSingleTrx;
	}

	public void setSingleTrx(boolean isSingleTrx) {
		this.isSingleTrx = isSingleTrx;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	static class ValueComparator implements Comparator<GridTab> {
    	Map<GridTab,Integer> base;
		public ValueComparator(Map<GridTab,Integer> base) {
		    this.base = base;
		}
		// Note: this comparator imposes orderings that are inconsistent with equals.    
		public int compare(GridTab a, GridTab b) {
		    if(base.get(a) >= base.get(b))
		       return 1;
		    else
		       return -1;
		}
    }

}
