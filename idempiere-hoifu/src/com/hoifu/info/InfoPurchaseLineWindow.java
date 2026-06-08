package com.hoifu.info;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridField;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

/**
 * Info window for Purchase Order Line (C_OrderLine) with selective totals (UI
 * only).
 */
public class InfoPurchaseLineWindow extends InfoWindow {
	private static final long serialVersionUID = 1L;

	// 需要求和的列名（与 AD_InfoColumn 的显示标题一致）
	private static final String[] TOTAL_COLUMNS = { "明细净额", "订购数量", "收货数量" };

	public InfoPurchaseLineWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public InfoPurchaseLineWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public InfoPurchaseLineWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public InfoPurchaseLineWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	@Override
	public void onQueryCallback(Event event) {
		super.onQueryCallback(event);
		if (contentPanel.getRowCount() > 0 && contentPanel.getLayout() != null) {
			addSelectiveTotalsRow();
		}
	}

	@Override
	public void onEvent(Event event) {
		try {
			super.onEvent(event);
			if (event.getTarget() == paging) {
				if (contentPanel.getRowCount() > 0 && contentPanel.getLayout() != null) {
					addSelectiveTotalsRow();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addSelectiveTotalsRow() {
		WListbox table = contentPanel;
		ColumnInfo[] layout = table.getLayout();
		if (layout == null)
			return;

		Map<String, Integer> nameToIndex = new HashMap<>();
		for (int i = 0; i < layout.length; i++) {
			nameToIndex.put(layout[i].getColHeader(), i);
		}

		// 找出需要求和的列中最左边的列索引
		int leftmostTotalColumn = Integer.MAX_VALUE;
		for (String name : TOTAL_COLUMNS) {
			Integer idx = nameToIndex.get(name);
			if (idx != null && idx < leftmostTotalColumn) {
				leftmostTotalColumn = idx;
			}
		}

		Object[] totals = new Object[layout.length];
		for (int row = 0; row < table.getRowCount(); row++) {
			for (String name : TOTAL_COLUMNS) {
				Integer idx = nameToIndex.get(name);
				if (idx != null) {
					Object val = table.getValueAt(row, idx);
					if (val instanceof BigDecimal) {
						BigDecimal cur = (BigDecimal) val;
						BigDecimal sum = (BigDecimal) totals[idx];
						totals[idx] = (sum == null ? cur : sum.add(cur));
					} else if (val instanceof Double) {
						Double cur = (Double) val;
						Double sum = (Double) totals[idx];
						totals[idx] = (sum == null ? cur : sum + cur);
					} else if (val instanceof Integer) {
						Integer cur = (Integer) val;
						Integer sum = (Integer) totals[idx];
						totals[idx] = (sum == null ? cur : sum + cur);
					}
				}
			}
		}

		int totalRow = table.getRowCount();
		table.setRowCount(totalRow + 1);
		for (int col = 0; col < layout.length; col++) {
			if (totals[col] != null) {
				table.setValueAt(totals[col], totalRow, col);
			} else {
				if (col == 0) {
					table.setValueAt(new IDColumn(-1), totalRow, col);
				} else if (col == leftmostTotalColumn - 1) {
					// 在最左边需要求和的列的前一列添加求和符号
					table.setValueAt("Σ", totalRow, col);
				} else {
					table.setValueAt(null, totalRow, col);
				}
			}
		}

		// 对汇总行应用样式
		applyStyleToTotalsRow(table, totalRow, nameToIndex, leftmostTotalColumn);
	}

	private void applyStyleToTotalsRow(WListbox table, int totalRow, Map<String, Integer> nameToIndex,
			int leftmostTotalColumn) {
		Executions.schedule(table.getDesktop(), new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Listitem totalItem = table.getItemAtIndex(totalRow);
					if (totalItem != null) {
						// 使用更强的CSS选择器和!important
						ZkCssHelper.appendStyle(totalItem,
								"background-color: #E6F3FF !important; color: #000000 !important;");

						// 设置行样式类
						totalItem.setSclass("summary-row");

						// 对求和符号应用加粗样式
						if (leftmostTotalColumn > 0) {
							Listcell sigmaCell = totalItem.getChildren().size() > leftmostTotalColumn - 1
									? (Listcell) totalItem.getChildren().get(leftmostTotalColumn - 1)
									: null;
							if (sigmaCell != null) {
								ZkCssHelper.appendStyle(sigmaCell, "font-weight: bold !important;");
							}
						}

						// 对所有汇总列应用加粗样式
						for (String columnName : TOTAL_COLUMNS) {
							Integer colIndex = nameToIndex.get(columnName);
							if (colIndex != null) {
								Listcell cell = totalItem.getChildren().size() > colIndex
										? (Listcell) totalItem.getChildren().get(colIndex)
										: null;
								if (cell != null) {
									ZkCssHelper.appendStyle(cell, "font-weight: bold !important;");
								}
							}
						}
					}
				} catch (Exception e) {
					// 忽略样式应用异常
				}
			}
		}, new Event("onApplyStyle"));
	}

	@Override  
	public void sort(Comparator<Object> cmpr, boolean ascending) {  
	    super.sort(cmpr, ascending);  
	    if (contentPanel.getRowCount() > 0 && contentPanel.getLayout() != null) {  
	        addSelectiveTotalsRow();  
	    }  
	}
}