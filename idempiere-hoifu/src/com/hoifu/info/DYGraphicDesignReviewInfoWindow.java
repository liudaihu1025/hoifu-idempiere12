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
 * 平面设计评审信息窗口（自定义）
 * 
 * 重写 enableButtons(boolean enable)，根据选中行的 isfinalreview 字段
 * 动态控制"设计评审"和"最终审核"按钮的可用性： - isfinalreview = 'N'（普通评审人）→ 设计评审按钮可点，最终审核按钮置灰 -
 * isfinalreview = 'Y'（终审人） → 最终审核按钮可点，设计评审按钮置灰
 * 
 * 同时强制单选模式，并修复 p_multipleSelection=false 时 getSaveKeys/saveResultSelection 返回
 * null 导致的 NPE。
 * 
 * 信息窗口配置说明： AD_Table_ID = dy_graphicdesigneffect WhereClause = gdr.reviewer_id
 * = @#AD_User_ID@ ClassName = com.hoifu.info.DYGraphicDesignReviewInfoWindow
 */  
public class DYGraphicDesignReviewInfoWindow extends InfoWindow {  

	private static final long serialVersionUID = 20260715L;

	private static final CLogger log = CLogger.getCLogger(DYGraphicDesignReviewInfoWindow.class);

	/** 普通评审流程类名，用于识别"设计评审"按钮 */
	private static final String REVIEW_PROCESS_CLASSNAME = "com.hoifu.process.DYGraphicDesignReviewSubmit";

	/** 终审流程类名，用于识别"最终审核"按钮 */
	private static final String FINAL_REVIEW_PROCESS_CLASSNAME = "com.hoifu.process.DYGraphicDesignFinalReviewSubmit";

	// ==================== 构造器（与父类保持一致）====================

	public DYGraphicDesignReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public DYGraphicDesignReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public DYGraphicDesignReviewInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

    public DYGraphicDesignReviewInfoWindow(int WindowNo, String tableName, String keyColumn,  
            String queryValue, boolean multipleSelection, String whereClause,  
			int AD_InfoWindow_ID, boolean lookup, GridField field, String predefinedContextVariables) {
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);  

		// 强制单选
		// 注意：p_multipleSelection 仍需在 getSaveKeys/saveResultSelection 中临时恢复为 true，
		// 否则父类会返回 null，导致流程提交时 NPE。
		setMultipleSelection(false);

		// 窗口打开时自动查询数据（仅当 queryValue 为空时，避免重复查询）
		// super() 中 haveProcess=true 会强制 p_multipleSelection=true，
		// renderItems() 会把 model 设为多选模式。
		// 在 renderItems() 之后手动将 model 改回单选，确保 listbox 真正单选。
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
	 * 
	 * 父类注释："should never reach here, when have process, p_multipleSelection is
	 * always true"，但我们强制了单选，所以需要临时恢复多选让父类正常处理，再恢复单选。
	 */
    @Override  
	public Collection<NamePair> getSaveKeys(int infoColumnId) {
		// 临时恢复多选，让父类走 if (p_multipleSelection) 分支，返回 m_viewIDMap（非 null）
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
	 * 
	 * 父类在 p_multipleSelection=false 时，m_values 保持为空 map，后续
	 * createT_Selection_InfoWindow 在 m_values.entrySet() 上会 NPE。
	 */
	@Override
	protected void saveResultSelection(int infoColumnId) {
		// 临时恢复多选，让父类正常填充 m_values
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

		// 强制单选：只保留第一条记录（正常情况下 listbox 单选模式已保证只有一条，此处为防御性代码）
		if (m_values.size() > 1) {
			NamePair firstKey = m_values.keySet().iterator().next();
			LinkedHashMap<NamePair, LinkedHashMap<String, Object>> single = new LinkedHashMap<>();
			single.put(firstKey, m_values.get(firstKey));
			m_values = single;
        }  
    }  

	// ==================== 按钮启用逻辑 ====================

	/**
	 * 重写 enableButtons(boolean enable)，在父类统一 enable/disable 所有按钮之后， 根据选中行的
	 * isfinalreview 字段精细控制两个流程按钮的可用性： - isfinalreview = 'N' → 设计评审按钮可点，最终审核按钮置灰 -
	 * isfinalreview = 'Y' → 最终审核按钮可点，设计评审按钮置灰
	 * - 同一用户同时有 N 和 Y 记录 → 两个按钮都可点
	 */
	@Override
	protected void enableButtons(boolean enable) {
		// 先让父类统一处理（有选中行则 enable，无选中行则 disable）
		super.enableButtons(enable);

		// 无选中行时，所有按钮已被父类 disable，无需额外处理
		if (!enable)
			return;

		// 获取选中行的主键（dy_graphicdesigneffect_id）
		Object selectedKey = getSelectedRowKey();
		if (selectedKey == null)
			return;

		int effectId;
		try {
			effectId = Integer.parseInt(selectedKey.toString());
		} catch (NumberFormatException e) {
			return; // UUID 主键，无法精细控制，保持父类状态
		}

		// 查询当前用户在该效果上的所有评审记录，分别记录是否存在普通评审和终审
		boolean hasNormalReview = false;
		boolean hasFinalReview = false;
		int currentUserId = Env.getAD_User_ID(Env.getCtx());
		String sql = "SELECT gdr.isfinalreview " + "FROM adempiere.dy_graphicdesignreview gdr "
				+ "INNER JOIN adempiere.dy_graphicdesigneffect gde "
				+ "    ON gdr.dy_graphicdesigneffect_id = gde.dy_graphicdesigneffect_id "
				+ "WHERE gdr.dy_graphicdesigneffect_id = ? " + "  AND gdr.reviewer_id = ? "
				+ "  AND gdr.isactive = 'Y' " + "  AND gde.effectstatus = 'RV'";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setInt(1, effectId);
			pstmt.setInt(2, currentUserId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String val = rs.getString("isfinalreview");
					if ("N".equals(val)) hasNormalReview = true;
					if ("Y".equals(val)) hasFinalReview = true;
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "DYGraphicDesignReviewInfoWindow.enableButtons 查询失败", e);
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