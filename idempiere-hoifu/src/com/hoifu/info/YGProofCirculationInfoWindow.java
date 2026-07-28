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
import org.compiere.util.NamePair;

/**
 * 样稿流转信息窗口（自定义）
 * 
 * 重写 enableButtons(boolean enable)，根据选中明细行的样稿状态 动态控制"归还"按钮的可用性： - 样稿流转状态 =
 * 被领用（OU）且 样稿状态 <> 待报废（C）且 领用状态 = 领用中（BO）→ 归还按钮可点 - 否则 → 归还按钮置灰
 * 
 * 同时强制单选模式，并修复 p_multipleSelection=false 时 getSaveKeys/saveResultSelection 返回
 * null 导致的 NPE。
 */
public class YGProofCirculationInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 20260702L;

	private static final CLogger log = CLogger.getCLogger(YGProofCirculationInfoWindow.class);

	/** 归还流程的完整类名，用于识别归还按钮 */
	private static final String RETURN_PROCESS_CLASSNAME = "com.hoifu.process.YGProofReturn";

	public YGProofCirculationInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);

		// 强制单选
		// 注意：p_multipleSelection 仍需在 getSaveKeys/saveResultSelection 中临时恢复为 true，
		// 否则父类会返回 null，导致流程提交时 NPE。
		setMultipleSelection(false);

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
			// 防御性兜底：理论上不会为 null，但以防万一
			return result != null ? result : Collections.emptyList();
		} finally {
			// 无论是否抛异常，都恢复单选状态
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
			// 无论是否抛异常，都恢复单选状态
			setMultipleSelection(false);
		}

		// 修复 NPE：m_keyColumnIndex == -1 时父类直接 return，m_values 保持 null，
		// createT_Selection_InfoWindow 会在 m_values.entrySet() 上 NPE
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

	/**
	 * 重写 enableButtons(boolean enable)，在父类统一 enable/disable 所有按钮之后，
	 * 根据选中明细行的样稿状态精细控制"归还"按钮的可用性。
	 */
	@Override
	protected void enableButtons(boolean enable) {
		// 先让父类统一处理（有选中行则 enable，无选中行则 disable）
		super.enableButtons(enable);

		// 无选中行时，归还按钮已被父类 disable，无需额外处理
		if (!enable)
			return;

		// 获取选中行的主键（yg_proofborr_id）
		Object selectedKey = getSelectedRowKey();
		if (selectedKey == null)
			return;

		int borrId;
		try {
			borrId = Integer.parseInt(selectedKey.toString());
		} catch (NumberFormatException e) {
			return; // UUID 主键，无法精细控制，保持父类状态
		}

		// 查询选中明细对应的样稿状态（JOIN 主表）
		boolean canReturn = false;
		String sql = "SELECT pi.flowstatus, pi.samplestatus, pb.borrowstatus " + "FROM yg_proofborr pb "
				+ "JOIN yg_proofinventory pi ON pb.yg_proofinventory_id = pi.yg_proofinventory_id "
				+ "WHERE pb.yg_proofborr_id = ?";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setInt(1, borrId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String flowStatus = rs.getString("flowstatus");
					String sampleStatus = rs.getString("samplestatus");
					String borrowStatus = rs.getString("borrowstatus");
					// 被领用（OU）且非待报废（C）且领用中（BO）才允许归还
					canReturn = "OU".equals(flowStatus) && !"C".equals(sampleStatus) && "BO".equals(borrowStatus);
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "YGProofCirculationInfoWindow.enableButtons 查询失败", e);
		}

		// 精细控制归还按钮
		final boolean finalCanReturn = canReturn;
		for (Button bt : btProcessList) {
			MProcess process = (MProcess) bt.getAttribute(ATT_INFO_PROCESS_KEY);
			if (process != null && RETURN_PROCESS_CLASSNAME.equals(process.getClassname())) {
				bt.setDisabled(!finalCanReturn);
			}
		}
	}
}