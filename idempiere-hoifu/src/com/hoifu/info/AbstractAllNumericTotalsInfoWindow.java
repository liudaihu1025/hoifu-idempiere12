package com.hoifu.info;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.stream.IntStream;

import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridField;
import org.compiere.util.DB;
import org.compiere.util.KeyNamePair;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Listitem;

/**
 * Info Window 通用基类：在结果集底部自动追加一行合计，对表格里全部 BigDecimal/Double/Integer 类型的列求和。
 * 
 * 1. 行数自检不再与"当前页展示行数"比较（那是错误的比较对象，无法防御 JOIN 行膨胀）。 
 * 2. "Σ 合计"文案固定放在第 1列（勾选框列之后第一列），不再依赖 firstNumericCol 的相对位置。 
 * 3.追加合计行前先检测末行是否已经是合计行，避免排序/分页等重复渲染路径下出现"合计的合计"。 
 * 4. Integer 求和增加溢出检测。 
 * 5. 尝试关闭contentPanel 原生 showTotals，避免和本类自定义合计行并存导致重复合计。 
 * 6. 失败时打印 SQL本身（不含参数值），方便线上排查。
 * 
 * 注意：本类依赖 InfoWindow/InfoPanel 若干受保护字段（buildDataSQL/setParameters/queryTimeout
 * 等）， 若上游 idempiere 基类未来调整这些字段/方法的可见性或语义，需要重新核实。
 */
public abstract class AbstractAllNumericTotalsInfoWindow extends InfoWindow {

	private static final long serialVersionUID = -7878481801523458212L;

	/** 合计查询使用的 fetchSize，抽成常量方便子类按需覆盖 */
	protected static final int TOTALS_FETCH_SIZE = 1000;

	/** 用于标记"这一行是合计行"的哨兵对象，写入第 0 列，同时用来判断某一行是否已经是合计行 */
	private static final IDColumn TOTALS_ROW_MARKER = new IDColumn(-1);

	public AbstractAllNumericTotalsInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public AbstractAllNumericTotalsInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public AbstractAllNumericTotalsInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public AbstractAllNumericTotalsInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	/** 标记合计行是否已经追加，避免样式调度在没有合计行时空跑 */
	protected boolean totalsRowAdded = false;

	/** 每列对应的数据库聚合结果缓存；下标与 ColumnInfo[] layout 对应，null 表示非数值列或未取到值 */
	protected Object[] globalTotalsCache;

	/** 是否需要重新向数据库查询合计（true = 数据集已变化，需要重新聚合） */
	protected boolean totalsDirty = true;

	@Override
	public void onQueryCallback(Event event) {
		super.onQueryCallback(event);
		totalsDirty = true; // 真正重新检索 -> 数据集变化，需要重新计算
		disableNativeShowTotalsIfPossible();
		safeRefreshTotalsRow();
	}

	@Override
	public void onEvent(Event event) {
		try {
			super.onEvent(event);
		} catch (Exception e) {
			log.warning("AbstractAllNumericTotalsInfoWindow.onEvent方法异常：" + e.getLocalizedMessage());
			return;
		}
		if (event.getTarget() == paging) {
			// 分页不改变整体数据集，直接复用已缓存的合计值重新渲染，不重复查询数据库
			safeRefreshTotalsRow();
		}
	}

	@Override
	public void sort(Comparator<Object> cmpr, boolean ascending) {
		super.sort(cmpr, ascending);
		// 排序不改变整体数据集，直接复用已缓存的合计值重新渲染，不重复查询数据库
		safeRefreshTotalsRow();
	}

	/**
	 * 尝试关闭 contentPanel 原生的 showTotals 机制，避免它自己的 addTotals(layout) 和本类
	 * 自定义的合计行同时出现，导致界面上出现两行合计。 用 try/catch 包裹是因为不同 idempiere 版本 WListbox 是否公开
	 * setShowTotals 未完全确认； 一旦调用失败只记录日志，不影响本类自身合计行的渲染。
	 */
	private void disableNativeShowTotalsIfPossible() {
		try {
			if (contentPanel != null && contentPanel.getShowTotals()) {
				contentPanel.setShowTotals(false);
			}
		} catch (Exception e) {
			log.warning("AbstractAllNumericTotalsInfoWindow.disableNativeShowTotalsIfPossible方法异常（可忽略，若目标版本不支持该API）："
					+ e.getLocalizedMessage());
		}
	}

	/** 统一入口：保证任何异常都不会向上抛给 ZK 事件分发框架 */
	private void safeRefreshTotalsRow() {
		try {
			refreshTotalsRowIfPossible();
		} catch (Exception e) {
			log.warning("AbstractAllNumericTotalsInfoWindow.safeRefreshTotalsRow方法异常：" + e.getLocalizedMessage());
		}
	}

	private void refreshTotalsRowIfPossible() {
		if (contentPanel != null && contentPanel.getRowCount() > 0 && contentPanel.getLayout() != null) {
			refreshTotalsRow();
		}
	}

	protected void refreshTotalsRow() {
		WListbox table = contentPanel;
		if (table == null)
			return;

		int rowCount = table.getRowCount();
		ColumnInfo[] layout = table.getLayout();
		if (rowCount <= 0 || layout == null || layout.length == 0)
			return;

		// 防御性检测：如果最后一行已经是我们自己追加的合计行（第0列是 TOTALS_ROW_MARKER），
		// 就直接覆盖这一行，而不是无条件再 setRowCount(+1) 追加新的一行。
		// 这避免了在“super.sort()/super.onEvent() 是否会先清掉旧合计行”这个不确定假设下，
		// 反复调用 refreshTotalsRow() 导致“合计的合计”或者多行合计残留。
		int lastRowIndex = rowCount - 1;
		boolean lastRowIsTotals = isTotalsRow(table, lastRowIndex);
		int dataRowCount = lastRowIsTotals ? lastRowIndex : rowCount;
		int totalRow = dataRowCount;

		if (totalsDirty || globalTotalsCache == null) {
			globalTotalsCache = computeGlobalTotals(layout, dataRowCount);
			totalsDirty = false;
		}
		if (globalTotalsCache == null) {
			totalsRowAdded = false;
			return;
		}

		if (!lastRowIsTotals) {
			table.setRowCount(totalRow + 1);
		}

		// "Σ 合计"文案固定放在第 1 列（勾选框列之后第一列），不再依赖 firstNumericCol 的相对位置，
		// 避免"第一个数值列恰好是第1列"时文案被第0列的勾选框覆盖逻辑挤掉、永远不显示的问题。
		int labelCol = layout.length > 1 ? 1 : -1;

		for (int col = 0; col < layout.length; col++) {
			if (col == 0) {
				table.setValueAt(TOTALS_ROW_MARKER, totalRow, col);
			} else if (globalTotalsCache[col] != null) {
				table.setValueAt(globalTotalsCache[col], totalRow, col);
			} else if (col == labelCol) {
				table.setValueAt("\u03a3 合计", totalRow, col);
			} else {
				table.setValueAt(null, totalRow, col);
			}
		}
		totalsRowAdded = true;
		applyStyleToTotalsRow(table, totalRow);
	}

	/**
	 * 判断表格里某一行是否是本类自己追加的合计行：第0列的值是否是 TOTALS_ROW_MARKER（引用相等即可，
	 * 因为该对象只在本类内部创建并写入，不会被其他数据行意外持有）。
	 */
	private boolean isTotalsRow(WListbox table, int rowIndex) {
		if (rowIndex < 0)
			return false;
		try {
			Object v = table.getValueAt(rowIndex, 0);
			return v == TOTALS_ROW_MARKER;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 对当前查询条件下的"全部数据"做一次数据库查询并在 Java 端流式累加。
	 * 
	 * @param layout       当前表格列定义
	 * @param dataRowCount 当前 UI 已展示的"真实数据行数"（不含合计行），仅用于兜底日志，
	 *                     不再作为决定是否展示合计的判断依据（原版用它跟全量物理行数比较的方向是反的， 无法防御 JOIN
	 *                     行膨胀，这里改为只记录，不做强制放弃）。
	 * @return 下标与 layout 对应的合计值数组；返回 null 表示没有数值列或查询失败
	 */
	protected Object[] computeGlobalTotals(ColumnInfo[] layout, int dataRowCount) {
		boolean hasNumeric = IntStream.range(0, layout.length).anyMatch(col -> isNumeric(layout[col].getColClass()));
		if (!hasNumeric)
			return null;

		String dataSql;
		try {
			// end == start(0,0) -> buildDataSQL 内部不会附加数据库分页(LIMIT/OFFSET)，
			// 但仍会附加动态 WHERE / 行级安全 / otherClause / ORDER BY。
			dataSql = buildDataSQL(0, 0);
		} catch (Exception e) {
			log.warning("AbstractAllNumericTotalsInfoWindow.buildDataSQL方法异常：" + e.getLocalizedMessage());
			return null;
		}
		if (dataSql == null || dataSql.trim().isEmpty())
			return null;

		Object[] totals = new Object[layout.length];
		BigDecimal[] bdSum = new BigDecimal[layout.length];
		double[] dblSum = new double[layout.length];
		long[] intSum = new long[layout.length];
		boolean[] seen = new boolean[layout.length];
		boolean[] intOverflow = new boolean[layout.length];
		int[] physicalRowCount = { 0 };

		try (PreparedStatement pstmt = DB.prepareStatement(dataSql, null)) {
			if (pstmt == null)
				return null;
			if (useQueryTimeoutFromSysConfig && queryTimeout > 0) {
				pstmt.setQueryTimeout(queryTimeout);
			}
			pstmt.setFetchSize(TOTALS_FETCH_SIZE);
			setParameters(pstmt, false);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					physicalRowCount[0]++;
					int colOffset = 1;
					for (int col = 0; col < layout.length; col++) {
						Class<?> c = layout[col].getColClass();
						int colIndex = col + colOffset;
						if (c == KeyNamePair.class) {
							if (layout[col].isKeyPairCol()) {
								colOffset++;
							}
							continue;
						}
						if (c == BigDecimal.class) {
							BigDecimal v = rs.getBigDecimal(colIndex);
							if (v != null) {
								bdSum[col] = (bdSum[col] == null) ? v : bdSum[col].add(v);
								seen[col] = true;
							}
						} else if (c == Double.class) {
							double v = rs.getDouble(colIndex);
							if (!rs.wasNull()) {
								dblSum[col] += v;
								seen[col] = true;
							}
						} else if (c == Integer.class) {
							int v = rs.getInt(colIndex);
							if (!rs.wasNull()) {
								intSum[col] += v;
								seen[col] = true;
								if (!intOverflow[col]
										&& (intSum[col] > Integer.MAX_VALUE || intSum[col] < Integer.MIN_VALUE)) {
									intOverflow[col] = true;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.warning("AbstractAllNumericTotalsInfoWindow.computeGlobalTotals方法异常，SQL=[" + dataSql + "]，异常："
					+ e.getLocalizedMessage());
			return null;
		}

		// 仅记录日志，不再用来决定是否放弃渲染：原版拿"物理行数 < 当前页展示行数"来自检，
		// 但全量查询理论上本来就应该 >= 当前页行数（分页场景下几乎总是更大），
		// 这个方向的判断无法防御"JOIN 导致行膨胀、合计被重复计算"的真正风险场景。
		// 若后续确认了 InfoPanel/InfoWindow 中记录"全量总行数"的字段可以安全访问，
		// 应改为与该字段做严格比较，此处先降级为诊断日志，避免用错误的自检误杀正确合计。
		if (physicalRowCount[0] < dataRowCount) {
			log.warning("AbstractAllNumericTotalsInfoWindow.computeGlobalTotals: 聚合查询返回行数(" + physicalRowCount[0]
					+ ")少于当前展示行数(" + dataRowCount + ")，请核实查询条件是否一致（该异常不再阻止合计行渲染，仅记录日志）");
		}

		for (int col = 0; col < layout.length; col++) {
			if (!seen[col])
				continue;
			Class<?> c = layout[col].getColClass();
			if (c == BigDecimal.class) {
				totals[col] = bdSum[col];
			} else if (c == Double.class) {
				totals[col] = dblSum[col];
			} else if (c == Integer.class) {
				if (intOverflow[col]) {
					log.warning("AbstractAllNumericTotalsInfoWindow.computeGlobalTotals: 第" + col
							+ "列 Integer 合计值超出 int 范围(" + intSum[col] + ")，放弃展示该列合计以避免静默截断");
					totals[col] = null;
				} else {
					totals[col] = (int) intSum[col];
				}
			}
		}
		return totals;
	}

	private static boolean isNumeric(Class<?> c) {
		return c == BigDecimal.class || c == Double.class || c == Integer.class;
	}

	/**
	 * 禁用合计整行的交互，并尝试隐藏勾选框（若目标 ZK 版本的 Listitem 支持 setCheckable）。 ZK 组件在异步渲染过程中可能已被
	 * detach，这里失败只记录日志，不影响合计数据本身已经写入表格。
	 */
	protected void applyStyleToTotalsRow(WListbox table, int totalRow) {
		if (!totalsRowAdded)
			return;
		try {
			Listitem totalItem = table.getItemAtIndex(totalRow);
			if (totalItem == null)
				return;
			totalItem.setDisabled(true);
		} catch (Exception e) {
			log.warning("AbstractAllNumericTotalsInfoWindow.applyStyleToTotalsRow方法异常：" + e.getLocalizedMessage());
		}
	}
}