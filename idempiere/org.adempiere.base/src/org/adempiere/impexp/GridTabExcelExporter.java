/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2008 SC ARHIPAC SERVICE SRL. All Rights Reserved.            *
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.adempiere.base.IGridTabExporter;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.Lookup;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;

/**
 * Excel (XLS) Exporter Adapter for GridTab
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>FR [ 1943731 ] Window data export functionality
 */
public class GridTabExcelExporter extends AbstractExcelExporter implements IGridTabExporter
{
	private GridTab m_tab = null;
	
	private List<VirtualColumn> virtualColumns = new ArrayList<>();

	/**
	 * Default constructor
	 */
	public GridTabExcelExporter()
	{
		setFreezePane(0, 1);
	}

	@Override
	public int getColumnCount()
	{
		return virtualColumns.isEmpty() ? m_tab.getFieldCount() : virtualColumns.size();
	}

	@Override
	public int getDisplayType(int row, int col)
	{
		VirtualColumn vc = virtualColumns.get(col);
		if (vc.idColName != null)
			return DisplayType.String; // 已解析为字符串
		return m_tab.getField(vc.fieldIndex).getDisplayType();
	}

	@Override
	public String getHeaderName(int col)
	{
		VirtualColumn vc = virtualColumns.get(col);
		GridField f = m_tab.getField(vc.fieldIndex);
		if (vc.idColName == null)
			return f.getHeader();
		return f.getHeader() + "[" + vc.idColName + "]";
	}

	@Override
	public int getRowCount()
	{
		return m_tab.getRowCount();
	}

	@Override
	public Object getValueAt(int row, int col) {
		VirtualColumn vc = virtualColumns.get(col);
		GridField f = m_tab.getField(vc.fieldIndex);
		Object key = m_tab.getValue(row, f.getColumnName());

		if (vc.idColName == null) {
			// 原有逻辑
			Lookup lookup = f.getLookup();
			if (lookup == null && f.getDisplayType() == DisplayType.Button)
				lookup = getButtonLookup(f);
			if (lookup != null)
				return lookup.getDisplay(key);
			return key;
		}

		// 多标识符展开列：直接 SQL 查询对应标识符列
		if (key == null)
			return null;
		Lookup lookup = f.getLookup();
		if (!(lookup instanceof MLookup))
			return null;
		MLookupInfo info = ((MLookup) lookup).getLookupInfo();
		if (info == null)
			return null;
		String foreignTable = info.TableName;
		String foreignKeyCol = info.KeyColumn;
		MTable fTable = MTable.get(Env.getCtx(), foreignTable);
		if (fTable == null)
			return null;
		MColumn idColumn = fTable.getColumn(vc.idColName);
		if (idColumn == null)
			return null;

		int displayType = idColumn.getAD_Reference_ID();
		String selectExpr;
		Language lang = getLanguage();
		if ((displayType == DisplayType.TableDir || displayType == DisplayType.Search) && vc.idColName.endsWith("_ID")
				&& idColumn.getAD_Reference_Value_ID() == 0) {
			String embedded = MLookupFactory.getLookup_TableDirEmbed(lang, vc.idColName, foreignTable);
			selectExpr = (embedded != null && !embedded.isEmpty()) ? "(" + embedded + ")" : vc.idColName;
		} else if ((displayType == DisplayType.TableDirUU || displayType == DisplayType.SearchUU)
				&& vc.idColName.endsWith("_UU")) {
			String embedded = MLookupFactory.getLookup_TableDirEmbed(lang, vc.idColName, foreignTable);
			selectExpr = (embedded != null && !embedded.isEmpty()) ? "(" + embedded + ")" : vc.idColName;
		} else if ((displayType == DisplayType.Table || displayType == DisplayType.Search)
				&& idColumn.getAD_Reference_Value_ID() != 0) {
			String embedded = MLookupFactory.getLookup_TableEmbed(lang, vc.idColName, foreignTable,
					idColumn.getAD_Reference_Value_ID());
			selectExpr = (embedded != null && !embedded.isEmpty()) ? "(" + embedded + ")" : vc.idColName;
		} else {
			selectExpr = vc.idColName;
		}

		String sql = "SELECT " + selectExpr + " FROM " + foreignTable + " WHERE " + foreignKeyCol + "=?";
		Object keyParam;
		try {
			keyParam = Integer.parseInt(key.toString());
		} catch (NumberFormatException e) {
			keyParam = key.toString();
		}
		return DB.getSQLValueStringEx(null, sql, keyParam);
	}

	@Override
	public boolean isColumnPrinted(int col)
	{
		VirtualColumn vc = virtualColumns.get(col);
		GridField f = m_tab.getField(vc.fieldIndex);
		// Hide not displayed fields
		if (!f.isDisplayed())
			return false;
		// Hide encrypted fields
		if (f.isEncrypted())
			return false;
		// Hide simple button fields without a value
		if (f.getDisplayType() == DisplayType.Button && f.getAD_Reference_Value_ID() == 0)
			return false;
		return true;
	}

	@Override
	public boolean isFunctionRow()
	{
		return false;
	}

	@Override
	public boolean isPageBreak(int row, int col)
	{
		return false;
	}

	@Override
	protected void setCurrentRow(int row)
	{
		; // nothing
	}

	@Override
	protected int getCurrentRow()
	{
		return m_tab.getCurrentRow();
	}

	/** Column Name:MLookup */
	private HashMap<String, MLookup> m_buttonLookups = new HashMap<String, MLookup>();

	/**
	 * @param mField
	 * @return lookup for field
	 */
	private MLookup getButtonLookup(GridField mField)
	{
		MLookup lookup = m_buttonLookups.get(mField.getColumnName());
		if (lookup != null)
			return lookup;
		// TODO: refactor with org.compiere.grid.ed.VButton.setField(GridField)
		if (mField.getColumnName().endsWith("_ID") && !mField.getColumnName().equals("Record_ID"))
		{
			lookup = MLookupFactory.get(Env.getCtx(), mField.getWindowNo(), 0,
				mField.getAD_Column_ID(), DisplayType.Search);
		}
		else if (mField.getAD_Reference_Value_ID() != 0)
		{
			//	Assuming List
			lookup = MLookupFactory.get(Env.getCtx(), mField.getWindowNo(), 0,
				mField.getAD_Column_ID(), DisplayType.List);
		}
		//
		m_buttonLookups.put(mField.getColumnName(), lookup);
		return lookup;
	}

	@Override
	public void export(GridTab gridTab, List<GridTab> childs, boolean currentRowOnly, File file,int indxDetailSelected) {
		m_tab = gridTab;
		buildVirtualColumns(); // ← 新增
		setCurrentRowOnly(currentRowOnly);
		try {
			export(file, null);
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}

	@Override
	public String getFileExtension() {
		return "xls";
	}

	@Override
	public String getFileExtensionLabel() {
		return Msg.getMsg(Env.getCtx(), "FileXLS");
	}

	@Override
	public String getContentType() {
		return "application/vnd.ms-excel";
	}

	@Override
	public String getSuggestedFileName(GridTab gridTab) {
		return gridTab.getName() + "." + getFileExtension();
	}

	/**
	 * {@inheritDoc}
	 * no detail tab is support to export with excel
	 */
	@Override
	public boolean isExportableTab(GridTab gridTab) {
		return false;
	}

	@Override
	public boolean isDisplayed(int row, int col)
	{
		return true;
	}
	
	private static class VirtualColumn {
		final int fieldIndex; // m_tab.getField(fieldIndex) 对应的原始字段
		final String idColName; // null = 普通字段；非null = 多标识符展开列的标识符列名

		VirtualColumn(int fieldIndex, String idColName) {
			this.fieldIndex = fieldIndex;
			this.idColName = idColName;
		}
	}
	
	private void buildVirtualColumns() {
		virtualColumns = new ArrayList<>();
		for (int i = 0; i < m_tab.getFieldCount(); i++) {
			GridField f = m_tab.getField(i);
			if (!DisplayType.isLookup(f.getDisplayType()) || DisplayType.isMultiID(f.getDisplayType())) {
				virtualColumns.add(new VirtualColumn(i, null));
				continue;
			}
			Lookup lookup = f.getLookup();
			if (!(lookup instanceof MLookup)) {
				virtualColumns.add(new VirtualColumn(i, null));
				continue;
			}
			MLookupInfo info = ((MLookup) lookup).getLookupInfo();
			if (info == null || info.lookupDisplayColumnNames == null || info.lookupDisplayColumnNames.size() <= 1) {
				virtualColumns.add(new VirtualColumn(i, null));
				continue;
			}
			// 多标识符字段：展开为多个虚拟列
			for (String idCol : info.lookupDisplayColumnNames) {
				virtualColumns.add(new VirtualColumn(i, idCol));
			}
		}
	}
}
