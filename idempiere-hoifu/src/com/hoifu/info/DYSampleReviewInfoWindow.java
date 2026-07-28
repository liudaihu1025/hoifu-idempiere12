package com.hoifu.info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.logging.Level;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.NamePair;

/**
 * 样品评审信息窗口（自定义）
 * 
 * 重写 enableButtons(boolean enable)，根据选中行的 isfinalreview 字段
 * 动态控制"样品评审"和"终审"按钮的可用性： - isfinalreview = 'N'（普通评审人）→ 样品评审按钮可点，终审按钮置灰 -
 * isfinalreview = 'Y'（终审人） → 终审按钮可点，样品评审按钮置灰 - 同一用户同时有 N 和 Y 记录 → 两个按钮都可点
 * 
 * 同时强制单选模式，并修复 p_multipleSelection=false 时 getSaveKeys/saveResultSelection 返回
 * null 导致的 NPE。
 * 
 */
public class DYSampleReviewInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 20260722L;

	private static final CLogger log = CLogger.getCLogger(DYSampleReviewInfoWindow.class);

	/** 普通评审流程类名，用于识别"样品评审"按钮 */
	private static final String REVIEW_PROCESS_CLASSNAME = "com.hoifu.process.DYSampleReviewSubmit";

	/** 终审流程类名，用于识别"终审"按钮 */
	private static final String FINAL_REVIEW_PROCESS_CLASSNAME = "com.hoifu.process.DYSampleFinalReviewSubmit";

	// ==================== 构造器（与父类保持一致）====================

	public DYSampleReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public DYSampleReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public DYSampleReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public DYSampleReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);

		// 强制单选
		setMultipleSelection(false);

		// 窗口打开时自动查询数据（仅当 queryValue 为空时，避免重复查询）
		if (queryValue == null || queryValue.trim().isEmpty()) {
			executeQuery();
			renderItems();
			((ListModelTable) contentPanel.getModel()).setMultiple(false);
			bindInfoProcessBt();
		}
	}

	// ==================== 单选修复 ====================

	/**
	 * 重写 getSaveKeys，修复 p_multipleSelection=false 时父类直接返回 null 的问题。
	 */
	@Override
	public Collection<NamePair> getSaveKeys(int infoColumnId) {
		setMultipleSelection(true);
		try {
			Collection<NamePair> result = super.getSaveKeys(infoColumnId);
			return result != null ? result : Collections.emptyList();
		} finally {
			setMultipleSelection(false);
		}
	}

	/**
	 * 重写 saveResultSelection，修复 p_multipleSelection=false 时父类不填充 m_values 的问题。
	 */
	@Override
	protected void saveResultSelection(int infoColumnId) {
		setMultipleSelection(true);
		try {
			super.saveResultSelection(infoColumnId);
		} finally {
			setMultipleSelection(false);
		}

		// 修复 NPE：m_keyColumnIndex == -1 时父类直接 return，m_values 保持 null
		if (m_values == null) {
			m_values = new LinkedHashMap<>();
			return;
		}

		// 强制单选：只保留第一条记录（防御性代码）
		if (m_values.size() > 1) {
			NamePair firstKey = m_values.keySet().iterator().next();
			LinkedHashMap<NamePair, LinkedHashMap<String, Object>> single = new LinkedHashMap<>();
			single.put(firstKey, m_values.get(firstKey));
			m_values = single;
		}
	}

	// ==================== 按钮启用逻辑 ====================

	/**
	 * 重写 enableButtons(boolean enable)，根据选中行的 isfinalreview 字段 精细控制两个流程按钮的可用性。
	 * 
	 * 选中行的主键是 dy_samplingdemand_id，需要通过 dy_samplereview 关联到 dy_samplereviewline
	 * 来判断当前用户的角色。
	 */
	@Override
	protected void enableButtons(boolean enable) {
		// 先让父类统一处理（有选中行则 enable，无选中行则 disable）
		super.enableButtons(enable);

		// 无选中行时，所有按钮已被父类 disable，无需额外处理
		if (!enable)
			return;

		// 获取选中行的主键（dy_samplingdemand_id）
		Object selectedKey = getSelectedRowKey();
		if (selectedKey == null)
			return;

		int demandId;
		try {
			demandId = Integer.parseInt(selectedKey.toString());
		} catch (NumberFormatException e) {
			return; // UUID 主键，无法精细控制，保持父类状态
		}

		// 查询当前用户在该需求的评审单下的所有评审行记录
		boolean hasNormalReview = false;
		boolean hasFinalReview = false;
		int currentUserId = Env.getAD_User_ID(Env.getCtx());

		// 通过 dy_samplereview 关联，用 dy_samplingdemand_id 定位
		String sql = "SELECT srl.isfinalreview " + "FROM adempiere.dy_samplereviewline srl "
				+ "INNER JOIN adempiere.dy_samplereview sr " + "    ON srl.dy_samplereview_id = sr.dy_samplereview_id "
				+ "WHERE sr.dy_samplingdemand_id = ? " + "  AND srl.reviewer_id = ? "
				+ "  AND srl.isactive = 'Y' "
				+ "  AND sr.reviewstatus = 'RV'";

		try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setInt(1, demandId);
			pstmt.setInt(2, currentUserId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String val = rs.getString("isfinalreview");
					if ("N".equals(val))
						hasNormalReview = true;
					if ("Y".equals(val))
						hasFinalReview = true;
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "DYSampleReviewInfoWindow.enableButtons 查询失败", e);
			return;
		}

		// 未找到任何评审记录，两个按钮都置灰
		if (!hasNormalReview && !hasFinalReview) {
			disableAllProcessButtons();
			return;
		}

		// 根据记录精细控制按钮
		for (Button bt : btProcessList) {
			MProcess process = (MProcess) bt.getAttribute(ATT_INFO_PROCESS_KEY);
			if (process == null || process.getClassname() == null)
				continue;

			String className = process.getClassname();

			if (REVIEW_PROCESS_CLASSNAME.equals(className)) {
				bt.setDisabled(!hasNormalReview);
			}
			if (FINAL_REVIEW_PROCESS_CLASSNAME.equals(className)) {
				bt.setDisabled(!hasFinalReview);
			}
		}
	}

	/**
	 * 将所有流程按钮置灰（未找到评审记录时调用）
	 */
	private void disableAllProcessButtons() {
		for (Button bt : btProcessList) {
			MProcess process = (MProcess) bt.getAttribute(ATT_INFO_PROCESS_KEY);
			if (process == null)
				continue;
			String className = process.getClassname();
			if (REVIEW_PROCESS_CLASSNAME.equals(className) || FINAL_REVIEW_PROCESS_CLASSNAME.equals(className)) {
				bt.setDisabled(true);
			}
		}
	}
}