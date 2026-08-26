package com.hoifu.window;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.window.FindWindow;
import org.compiere.model.GridField;
import org.compiere.model.MLookup;
import org.zkoss.zul.Comboitem;

import com.hoifu.enums.HFSysConfigEnum;

public class HFFindWindow extends FindWindow {
	private static final long serialVersionUID = 8604702693005016947L;

	/** 级联规则：父列 -> 规则（子列 + 子表关联列 + 子级过滤条件） */
	private final Map<String, CascadeRuleFull> rulesByParentColumn;

	public HFFindWindow(int targetWindowNo, int targetTabNo, String title, int AD_Table_ID, String tableName,
			String whereExtended, GridField[] findFields, int minRecords, int adTabId,
			AbstractADWindowContent windowPanel) {
		super(targetWindowNo, targetTabNo, title, AD_Table_ID, tableName, whereExtended, findFields, minRecords,
				adTabId, windowPanel);
		this.rulesByParentColumn = loadCascadeRules(tableName);
	}

	@Override
	protected void addHistoryRestriction(Comboitem selectedHistoryItem) {
		String selectedHistoryValue = historyCombo.getSelectedItem().getValue();

		if (null != selectedHistoryItem && selectedHistoryItem.toString().length() > 0
				&& getHistoryDays(selectedHistoryValue) > 0) {

			StringBuilder where = new StringBuilder(m_tableName);

			// 统一使用Created字段进行时间过滤
			String dateCondition = getExactDateCondition("Created", selectedHistoryValue);
			where.append(dateCondition);

			m_query.addRestriction(where.toString());
		}
	}

	private String getExactDateCondition(String dateColumn, String selectedHistoryValue) {
		StringBuilder condition = new StringBuilder();

		if (selectedHistoryValue.equals(HISTORY_DAY_MONTH)) {
			condition.append(".").append(dateColumn).append(" >= TRUNC(CURRENT_DATE, 'MM')").append(" AND ")
					.append(dateColumn).append(" < TRUNC(CURRENT_DATE, 'MM') + INTERVAL '1 month'");
		} else if (selectedHistoryValue.equals(HISTORY_DAY_YEAR)) {
			condition.append(".").append(dateColumn).append(" >= TRUNC(CURRENT_DATE, 'Y')").append(" AND ")
					.append(dateColumn).append(" < TRUNC(CURRENT_DATE, 'Y') + INTERVAL '1 year'");
		} else if (selectedHistoryValue.equals(HISTORY_DAY_WEEK)) {
			condition.append(".").append(dateColumn).append(" >= TRUNC(CURRENT_DATE, 'W')").append(" AND ")
					.append(dateColumn).append(" < TRUNC(CURRENT_DATE, 'W') + INTERVAL '1 week'");
		} else if (selectedHistoryValue.equals(HISTORY_DAY_DAY)) {
			condition.append(".").append(dateColumn).append(" >= TRUNC(CURRENT_DATE, 'D')").append(" AND ")
					.append(dateColumn).append(" < TRUNC(CURRENT_DATE, 'D') + INTERVAL '1 day'");
		} else {
			// 其他选项保持原有逻辑
			condition.append(".").append(dateColumn).append(" >= CURRENT_DATE - ")
					.append(getHistoryDays(selectedHistoryValue));
		}

		return condition.toString();
	}

	@Override
	protected void initFind() {
		super.initFind();

		// 父列有值就按当前值重建合法的 validation，无值才 reset。
		if (rulesByParentColumn != null && !rulesByParentColumn.isEmpty()) {
			rulesByParentColumn.values().forEach(this::refreshCascadeForRule);
		}
	}

	@Override
	public void valueChange(ValueChangeEvent evt) {
		super.valueChange(evt);
		try {
			String changedColumn = evt.getPropertyName();
			CascadeRuleFull rule = rulesByParentColumn.get(changedColumn);
			if (rule == null)
				return;

			refreshCascadeForRule(rule);

			// 递归清空更下一级（如果子列本身又是别的规则的父列）
			CascadeRuleFull grandRule = rulesByParentColumn.get(rule.childColumn);
			if (grandRule != null) {
				resetLookup(grandRule.childColumn, grandRule.childCondition);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "cascade refresh failed", e);
		}
	}

	/**
	 * 根据父列当前值，重建或重置子列的 lookup 校验条件。 供 {@link #initFind()} 初始化和
	 * {@link #valueChange(ValueChangeEvent)} 共用，避免逻辑重复。
	 */
	private void refreshCascadeForRule(CascadeRuleFull rule) {
		WEditor parentEditor = findEditorByColumnName(rule.parentColumn);
		Object parentValue = (parentEditor != null) ? parentEditor.getValue() : null;

		if (parentValue == null || "".equals(parentValue.toString())) {
			resetLookup(rule.childColumn, rule.childCondition);
		} else {
			String validation = buildChildValidation(rule.childCondition, rule.childLinkColumn, parentValue);
			applyValidation(rule.childColumn, validation);
		}
	}

	/**
	 * 构造子级 Lookup 的 ValidationCode。 注意：childLinkColumn 在配置里直接写全限定名（如
	 * M_Product_Category.M_Product_Category_Parent_ID）， 避免在存在多表 JOIN 的 Lookup SQL
	 * 里出现列名歧义。
	 */
	private String buildChildValidation(String childCondition, String childLinkColumn, Object parentValue) {
		String valueLiteral = (parentValue instanceof Number) ? parentValue.toString()
				: "'" + parentValue.toString().replace("'", "''") + "'"; // 简单转义单引号，避免破坏 SQL

		return childCondition + " AND " + childLinkColumn + "=" + valueLiteral;
	}

	private void applyValidation(String columnName, String validation) {
		WEditor editor = findEditorByColumnName(columnName);
		if (editor == null)
			return;
		GridField field = editor.getGridField();
		if (field == null || !(field.getLookup() instanceof MLookup mLookup))
			return;

		// 必须先清空，否则 WTableDirEditor.refreshList() 会保留旧值
		editor.setValue(null);

		mLookup.getLookupInfo().ValidationCode = validation;
		mLookup.getLookupInfo().IsValidated = false;
		mLookup.refresh();
	}

	private void resetLookup(String columnName, String baseCondition) {
		applyValidation(columnName, baseCondition + " AND 1=2");
	}

	private WEditor findEditorByColumnName(String columnName) {
		if (columnName == null || m_sEditors == null)
			return null;
		WEditor editor = m_sEditors.stream().filter(ed -> ed != null && columnName.equals(ed.getColumnName()))
				.findFirst().orElse(null);
		if (editor == null && log.isLoggable(Level.WARNING)) {
			log.warning("HF cascade rule references unknown column: " + columnName + " — check "
					+ HFSysConfigEnum.HF_FIND_WINDOW_CASCADE_RULES.getKey() + " config or window search fields");
		}
		return editor;
	}

	/**
	 * 一条级联规则：父列 -> 子列 -> 子表关联父级的列 -> 子级本身固定条件（不含 Parent_ID 关联部分）
	 */
	private static class CascadeRuleFull {
		final String parentColumn;
		final String childColumn;
		final String childLinkColumn;
		final String childCondition;

		CascadeRuleFull(String parentColumn, String childColumn, String childLinkColumn, String childCondition) {
			this.parentColumn = parentColumn;
			this.childColumn = childColumn;
			this.childLinkColumn = childLinkColumn;
			this.childCondition = childCondition;
		}
	}

	/**
	 * 从 AD_SysConfig 里加载当前表相关的级联规则。
	 * 配置格式：TableName|ParentColumn|ChildColumn|ChildLinkColumn|ChildCondition;...
	 * 例如：M_Product|M_Product_Category_ID_L1|M_Product_Category_ID_L2|M_Product_Category.M_Product_Category_Parent_ID|M_Product_Category.Category_Type='B'
	 * AND M_Product_Category.IsActive='Y'
	 */
	private Map<String, CascadeRuleFull> loadCascadeRules(String tableName) {
		String raw = HFSysConfigEnum.HF_FIND_WINDOW_CASCADE_RULES.getValue();
		if (raw == null || raw.isBlank() || tableName == null)
			return Collections.emptyMap();

		return Arrays.stream(raw.split(";")).map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.split("\\|", -1))
				.filter(parts -> parts.length >= 5 && parts[0].equals(tableName)) // parts[0].equals 避免 tableName 为 null
																					// 时 NPE
				.collect(Collectors.toMap(parts -> parts[1], // key: parentColumn
						parts -> new CascadeRuleFull(parts[1], parts[2], parts[3], parts[4]), (a, b) -> a));
	}

}