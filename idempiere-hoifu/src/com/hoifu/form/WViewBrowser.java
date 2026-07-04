package com.hoifu.form;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.adempiere.impexp.ArrayExcelExporter;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.WListItemRenderer;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MLookup;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.idempiere.ui.zk.media.Medias;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Frozen;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.South;
import org.zkoss.zul.event.ZulEvents;

/**
 * 视图报表快速浏览窗体
 * 
 * @ClassName: WViewBrowser
 * @author ldh
 * @date 2026年6月22日
 */
@org.idempiere.ui.zk.annotation.Form
public class WViewBrowser extends ADForm implements EventListener<Event> {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(WViewBrowser.class);

	private static final String TABLE_SQL = "SELECT TableName FROM AD_Table WHERE IsActive='Y' AND IsView='Y' ORDER BY TableName";

	// ---- UI 组件 ----
	private Borderlayout layout = new Borderlayout();
	private Combobox m_cmbTable = new Combobox();
	private org.adempiere.webui.component.Button m_btnQuery = new org.adempiere.webui.component.Button("查询");
	private org.adempiere.webui.component.Button m_btnExport = new org.adempiere.webui.component.Button("导出Excel");
	private Label m_lblStatus = new Label();
	private WListbox listbox = new WListbox();
	private Paging paging = new Paging();

	// ---- 分页状态 ----
	private String currentTableName = null;
	private String currentWhere = null;
	private int totalCount = 0;
	private int pageSize = 100;

	@Override
	protected void initForm() {
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		layout.setStyle("background-color: transparent; position: relative;");

		// ---- North: 参数栏 ----
		North north = new North();
		Div div = new Div();
		div.setStyle("display:flex; align-items:center; gap:8px; padding:4px;");

		m_cmbTable.setAutocomplete(true);
		m_cmbTable.setAutodrop(true);
		ZKUpdateUtil.setWidth(m_cmbTable, "300px");
		m_cmbTable.setPlaceholder("选择视图...");
		fillTableCombo();

		m_btnQuery.addEventListener(Events.ON_CLICK, this);

		m_btnExport.addEventListener(Events.ON_CLICK, this);
		m_btnExport.setEnabled(false); // 查询后才启用

		div.appendChild(new Label("视图："));
		div.appendChild(m_cmbTable);
		div.appendChild(m_btnQuery);
		div.appendChild(m_btnExport);
		north.appendChild(div);
		layout.appendChild(north);

		// ---- Center: 数据列表 ----
		Center center = new Center();
		center.appendChild(listbox);
		ZKUpdateUtil.setVflex(listbox, "1");
		ZKUpdateUtil.setHflex(listbox, "1");
		layout.appendChild(center);

		// ---- South: 分页 + 状态 ----
		South south = new South();
		Div southDiv = new Div();
		southDiv.setStyle("display:flex; flex-direction:column; gap:2px;");
		paging.setVisible(false);
		paging.setDetailed(true);
		paging.addEventListener(ZulEvents.ON_PAGING, this);
		southDiv.appendChild(paging);
		southDiv.appendChild(m_lblStatus);
		south.appendChild(southDiv);
		layout.appendChild(south);

		this.appendChild(layout);
	}

	private void fillTableCombo() {
		boolean trl = !Env.isBaseLanguage(Env.getCtx(), "AD_Table");
		String lang = Env.getAD_Language(Env.getCtx());

		String sql = "SELECT t.TableName, "
				+ (trl ? "COALESCE(trl.Name, t.Name)" : "t.Name")
				+ " FROM AD_Table t"
				+ (trl ? " LEFT JOIN AD_Table_Trl trl ON (t.AD_Table_ID=trl.AD_Table_ID"
				+ " AND trl.AD_Language=" + DB.TO_STRING(lang) + ")" : "")
				+ " WHERE t.IsActive='Y' AND t.IsView='Y'"
				+ " AND t.AccessLevel IN ('1','2','3','7')" // 过滤掉系统级视图
				+ " ORDER BY 2";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String tableName = rs.getString(1);
				String displayName = rs.getString(2);
				// 有翻译名且与表名不同时，显示"翻译名 (TableName)"，否则只显示表名
				String display = (displayName != null && !displayName.isEmpty()
						&& !displayName.equalsIgnoreCase(tableName)) ? displayName + " (" + tableName + ")" : tableName;
				Comboitem item = m_cmbTable.appendItem(display);
				item.setValue(tableName);
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, "fillTableCombo", e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**  
	 * 构建 WHERE 子句：仅当视图/表实际存在对应列时才加过滤  
	 */  
	private String buildLoginWhere(String tableName) {
		Properties ctx = Env.getCtx();
		MTable mTable = MTable.get(ctx, tableName);

		StringBuilder sb = new StringBuilder();
		if (mTable != null && mTable.columnExistsInDB("AD_Client_ID")) {
			int clientID = Env.getAD_Client_ID(ctx);
			sb.append(" WHERE AD_Client_ID = ").append(clientID);

			if (mTable.columnExistsInDB("AD_Org_ID")) {
				int orgID = Env.getAD_Org_ID(ctx);
				if (orgID > 0)
					sb.append(" AND AD_Org_ID = ").append(orgID);
			}
		}
		return sb.toString();
	}

	/** 点击查询：校验 → COUNT → 加载第一页 → 设置分页 */
	private void startQuery() {
		Comboitem sel = m_cmbTable.getSelectedItem();
		String tableName = sel != null ? (String) sel.getValue() : m_cmbTable.getValue();

		if (tableName == null || tableName.trim().isEmpty()) {
			m_lblStatus.setValue("请先选择视图");
			return;
		}
		// 安全校验：表名必须在 AD_Table 中
		if (DB.getSQLValue(null, "SELECT COUNT(*) FROM AD_Table WHERE IsActive='Y' AND TableName=?", tableName) <= 0) {
			m_lblStatus.setValue("ERROR: 无效的表名 " + tableName);
			return;
		}

		currentTableName = tableName;
		currentWhere = buildLoginWhere(currentTableName);

		totalCount = DB.getSQLValue(null, "SELECT COUNT(*) FROM " + currentTableName + currentWhere);
		pageSize = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_MAX_RECORDS, 100);

		paging.setPageSize(pageSize);
		paging.setTotalSize(totalCount);
		paging.setActivePage(0);
		paging.setVisible(totalCount > pageSize);

		m_lblStatus.setValue(loadPage(0));
		m_btnExport.setEnabled(totalCount > 0);
	}

	/** 加载指定页：数据库级分页（OFFSET / FETCH FIRST） */
	private String loadPage(int pageNo) {
		listbox.clear();
		String lang = Env.getAD_Language(Env.getCtx());
		int timeout = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_TIMEOUT_IN_SECONDS, 120);

		String baseSql = "SELECT * FROM " + currentTableName + currentWhere;
		int start = pageNo * pageSize + 1;
		int end = start + pageSize - 1;
		String sql = DB.getDatabase().isPagingSupported() ? DB.getDatabase().addPagingSQL(baseSql, start, end)
				: baseSql;

		List<String> header = new ArrayList<>();
		header.add("#序号");
		ListModelTable newModel = new ListModelTable();

		Frozen frozen = new Frozen();
		frozen.setColumns(1);
		listbox.appendChild(frozen);

		StringBuilder result = new StringBuilder();
		Trx trx = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String trxName = Trx.createTrxName("WViewBrowser");
			trx = Trx.get(trxName, false);
			trx.setDisplayName(getClass().getName() + "_loadPage");
			trx.getConnection().setReadOnly(true);

			pstmt = DB.prepareNormalReadReplicaStatement(sql, trxName);
			pstmt.setQueryTimeout(timeout);
			rs = pstmt.executeQuery();

			ResultSetMetaData meta = rs.getMetaData();
			int colCount = meta.getColumnCount();

			// 列名翻译：AD_Element_Trl，找不到则用原始列名
			for (int col = 1; col <= colCount; col++) {
				String raw = meta.getColumnLabel(col);
				String trl = Msg.getElement(lang, raw, true);
				header.add((trl != null && !trl.isEmpty()) ? trl : raw);
			}

			// 建立 FK 列 → 被引用表ID 的映射
			Map<Integer, Integer> fkColToTableID = new HashMap<>();
			for (int col = 1; col <= colCount; col++) {
				String rawName = meta.getColumnLabel(col);
				if (rawName.endsWith("_id")) {
					String refTableName = rawName.substring(0, rawName.length() - 3);
					MTable refTable = MTable.get(Env.getCtx(), refTableName);
					if (refTable != null && refTable.get_ID() > 0) {
						fkColToTableID.put(col, refTable.get_ID());
					}
				}
			}

			int rowNum = pageNo * pageSize;
			while (rs.next()) {
				List<Object> row = new ArrayList<>();
				row.add(++rowNum);
				for (int col = 1; col <= colCount; col++) {
					Object cellValue = rs.getObject(col);
					if (fkColToTableID.containsKey(col) && cellValue instanceof Number) {
						int recordID = ((Number) cellValue).intValue();
						if (recordID > 0) {
							String display = MLookup.getIdentifier(fkColToTableID.get(col), recordID);
							row.add((display != null && !display.isEmpty()) ? display : recordID);
						} else {
							row.add(cellValue);
						}
					} else {
						// 非FK列：BigDecimal等浮点数转字符串，保留原始精度
						if (cellValue instanceof BigDecimal 
								|| cellValue instanceof Double
								|| cellValue instanceof Float) {
							row.add(cellValue.toString());
						} else {
							row.add(cellValue);
						}
					}
				}
				newModel.add(row);
			}

			WListItemRenderer renderer = new WListItemRenderer(header);
			newModel.setNoColumns(header.size());
			listbox.setModel(newModel);
			listbox.setItemRenderer(renderer);
			listbox.initialiseHeader();
			listbox.setSizedByContent(true);

			result.append("第 ").append(pageNo + 1).append(" 页 / 共 ").append(totalCount).append(" 条记录");

		} catch (Exception e) {
			if (trx != null)
				trx.rollback();
			if (DBException.isTimeout(e))
				result.append("查询超时（超过 ").append(timeout).append(" 秒）");
			else {
				log.log(Level.SEVERE, "loadPage: " + sql, e);
				result.append("查询异常：").append(e.getMessage());
			}
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
			if (trx != null) {
				trx.close();
				trx = null;
			}
		}
		return result.toString();
	}

	/**
	 * 导出全量数据到 Excel（不分页，重新查询全部记录） 参考 InfoWindow.XlsxExporter 实现
	 */
	private void exportExcel() {
		if (currentTableName == null)
			return;

		String lang = Env.getAD_Language(Env.getCtx());
		int timeout = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_TIMEOUT_IN_SECONDS, 120);
		String sql = "SELECT * FROM " + currentTableName + currentWhere;

		// 构建 data：第0行=表头，后续行=数据
		ArrayList<ArrayList<Object>> data = new ArrayList<>();

		Trx trx = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String trxName = Trx.createTrxName("WViewBrowserExport");
			trx = Trx.get(trxName, false);
			trx.setDisplayName(getClass().getName() + "_exportExcel");
			trx.getConnection().setReadOnly(true);

			pstmt = DB.prepareNormalReadReplicaStatement(sql, trxName);
			pstmt.setQueryTimeout(timeout);
			pstmt.setFetchSize(100);
			rs = pstmt.executeQuery();

			ResultSetMetaData meta = rs.getMetaData();
			int colCount = meta.getColumnCount();

			// 表头行
			ArrayList<Object> headerRow = new ArrayList<>();
			for (int col = 1; col <= colCount; col++) {
				String raw = meta.getColumnLabel(col);
				String trl = Msg.getElement(lang, raw, true);
				headerRow.add((trl != null && !trl.isEmpty()) ? trl : raw);
			}
			data.add(headerRow);

			// 建立 FK 列 → 被引用表ID 的映射（与 loadPage 保持一致）
			Map<Integer, Integer> fkColToTableID = new HashMap<>();
			for (int col = 1; col <= colCount; col++) {
				String rawName = meta.getColumnLabel(col);
				if (rawName.endsWith("_id")) {
					String refTableName = rawName.substring(0, rawName.length() - 3);
					MTable refTable = MTable.get(Env.getCtx(), refTableName);
					if (refTable != null && refTable.get_ID() > 0) {
						fkColToTableID.put(col, refTable.get_ID());
					}
				}
			}

			// 数据行（全量，不分页）
			while (rs.next()) {
				ArrayList<Object> row = new ArrayList<>();
				for (int col = 1; col <= colCount; col++) {
					Object cellValue = rs.getObject(col);
					if (fkColToTableID.containsKey(col) && cellValue instanceof Number) {
						// FK列解析为标识符字符串
						int recordID = ((Number) cellValue).intValue();
						if (recordID > 0) {
							String display = MLookup.getIdentifier(fkColToTableID.get(col), recordID);
							row.add((display != null && !display.isEmpty()) ? display : String.valueOf(recordID));
						} else {
							row.add(cellValue != null ? cellValue.toString() : null);
						}
					} else if (cellValue instanceof Number) {
						// 数字转字符串，避免红色标注和精度丢失
						row.add(cellValue.toString());
					} else {
						row.add(cellValue);
					}
				}
				data.add(row);
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "exportExcel: " + sql, e);
			m_lblStatus.setValue("导出异常：" + e.getMessage());
			return;
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
			if (trx != null) {
				trx.close();
				trx = null;
			}
		}

		if (data.size() <= 1) {
			m_lblStatus.setValue("无数据可导出");
			return;
		}

		try {
			ArrayExcelExporter exporter = new ArrayExcelExporter(Env.getCtx(), data);
			File file = File.createTempFile(currentTableName + "_", ".xlsx");
			exporter.export(file, null);

			AMedia media = new AMedia(currentTableName + ".xlsx", null, Medias.EXCEL_XML_MIME_TYPE, file, true);
			Filedownload.save(media);
			m_lblStatus.setValue("导出完成，共 " + (data.size() - 1) + " 条记录");
		} catch (Exception e) {
			log.log(Level.SEVERE, "exportExcel write", e);
			m_lblStatus.setValue("导出文件异常：" + e.getMessage());
		}
	}

	@Override
	public void onEvent(Event event) throws Exception {
		if (event.getTarget() == m_btnQuery) {
			startQuery();
		} else if (event.getTarget() == m_btnExport) {
			exportExcel();
		} else if (event.getTarget() == paging) {
			if (currentTableName != null)
				m_lblStatus.setValue(loadPage(paging.getActivePage()));
		}
		super.onEvent(event);
	}
}