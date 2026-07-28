package com.hoifu.info;  
  
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.GridField;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
  
/**  
 * 工艺设计任务列表信息窗口（自定义）  
 *  
 * 主表：dy_samplingdemand（需求表）  
 * - 双击 / Zoom 按钮：穿透到需求表记录（默认行为）  
 * - 自定义"查看工艺设计"按钮：穿透到该需求下的 dy_processdesign 记录  
 */  
public class DYProcessDesignInfoWindow extends InfoWindow {  
  
    private static final long serialVersionUID = 1L;  
  
    protected static final CLogger log = CLogger.getCLogger(DYProcessDesignInfoWindow.class);  
  
    /** 自定义按钮 ID */  
    private static final String BTN_ZOOM_PROCESS_DESIGN = "ZoomProcessDesign";  
  
    /** 自定义按钮引用（在 renderWindow 后初始化） */  
    private Button btnZoomProcessDesign;  
  
    // ==================== 构造函数 ====================  
  
    public DYProcessDesignInfoWindow(int WindowNo, String tableName, String keyColumn,  
            String queryValue, boolean multipleSelection, String whereClause,  
            int AD_InfoWindow_ID, boolean lookup, GridField field,  
            String predefinedContextVariables) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause,  
				AD_InfoWindow_ID, lookup, field, predefinedContextVariables);

		// 打开时自动查询一次（queryValue 为空时 super 不会自动查询）
		if (queryValue == null || queryValue.trim().isEmpty()) {
			executeQuery();
			renderItems();
		}
    }  
  
    // ==================== 启用默认 Zoom 按钮（穿透到需求表）====================  
  
    @Override  
    protected boolean hasZoom() {  
        return true;  
    }  
  
    // ==================== 双击 / Zoom 按钮：穿透到需求表 ====================  
  
    @Override  
    public void zoom() {  
        Integer demandId = getIntSelectedRowKey(MTable.getTable_ID("dy_samplingdemand"));  
        if (demandId == null || demandId <= 0) {  
            Dialog.error(getWindowNo(), "PleaseSelectRecord");  
            return;  
        }  
        AEnv.zoom(MTable.getTable_ID("dy_samplingdemand"), demandId);  
    }  
  
    // ==================== 添加自定义按钮 ====================  
  
    @Override  
    protected void renderWindow() {  
        super.renderWindow();  
  
		// 修改默认 Zoom 按钮标签为"查看需求"
		confirmPanel.getButton(ConfirmPanel.A_ZOOM).setLabel("查看需求");

        // 在 confirmPanel 左侧添加"查看工艺设计"按钮  
        btnZoomProcessDesign = confirmPanel.createButton(BTN_ZOOM_PROCESS_DESIGN);  
        btnZoomProcessDesign.setLabel("查看工艺设计");  
        btnZoomProcessDesign.setTooltiptext("穿透到该需求下的工艺设计任务");  
        btnZoomProcessDesign.setDisabled(true); // 初始置灰，选中行后才可点  
        btnZoomProcessDesign.addEventListener(Events.ON_CLICK, this);  
        confirmPanel.addComponentsLeft(btnZoomProcessDesign);  
    }  
  
    // ==================== 处理自定义按钮点击 ====================  
  
    @Override  
	public void onEvent(Event event) {
        if (btnZoomProcessDesign != null && event.getTarget() == btnZoomProcessDesign) {  
			try {
				zoomToProcessDesign();
			} catch (Exception e) {
				log.log(Level.SEVERE, "zoomToProcessDesign error", e);
			}
            return;  
        }  
        super.onEvent(event);  
	}
  
    // ==================== 控制按钮可用状态 ====================  
  
    @Override  
    protected void enableButtons(boolean enable) {  
        super.enableButtons(enable);  
        if (btnZoomProcessDesign != null) {  
            btnZoomProcessDesign.setDisabled(!enable);  
        }  
    }  
  
    // ==================== 穿透到工艺设计任务 ====================  
  
    private void zoomToProcessDesign() {  
        Integer demandId = getIntSelectedRowKey(MTable.getTable_ID("dy_samplingdemand"));  
        if (demandId == null || demandId <= 0) {  
            Dialog.error(getWindowNo(), "PleaseSelectRecord");  
            return;  
        }  
  
        // 查询该需求下最新的工艺设计任务  
        int processDesignId = DB.getSQLValue(null,  
                "SELECT dy_processdesign_id FROM adempiere.dy_processdesign "  
                        + "WHERE dy_samplingdemand_id = ? AND isactive = 'Y' "  
                        + "ORDER BY created DESC LIMIT 1",  
                demandId);  
  
        if (processDesignId <= 0) {  
            Dialog.error(getWindowNo(), "未找到该需求下的工艺设计任务");  
            return;  
        }  
  
        AEnv.zoom(MTable.getTable_ID("dy_processdesign"), processDesignId);  
	}

	// ==================== 三段过滤逻辑 ====================

	@Override
	protected String getSQLWhere() {
		// 缓存逻辑（与 InfoProductWindow 保持一致）
		if (!isQueryByUser && prevWhereClause != null) {
			return prevWhereClause;
		}

		// 没有用户输入查询条件时，直接返回 super（显示所有满足固定条件的记录）
		if (!hasUserQueryInput()) {
			prevWhereClause = super.getSQLWhere();
			return prevWhereClause;
		}

		// 有用户输入时，获取 super 的 WHERE 子句（包含 isActive + 用户查询条件）
		String superWhere = super.getSQLWhere();

		int currentUserId = Env.getAD_User_ID(Env.getCtx());

		// 第二段：当前用户是工艺设计处理人
		String segment2 = "EXISTS (SELECT 1 FROM adempiere.dy_processdesign pd "
				+ "WHERE pd.dy_samplingdemand_id = a.dy_samplingdemand_id " + "AND pd.ad_user_id = " + currentUserId
				+ " AND pd.isactive = 'Y')";

		// 第三段：当前用户是工艺评审人（去重：只要有一条评审记录即可）
		String segment3 = "EXISTS (SELECT 1 FROM adempiere.dy_processdesignreview pdr "
				+ "JOIN adempiere.dy_processdesign pd2 " + "  ON pdr.dy_processdesign_id = pd2.dy_processdesign_id "
				+ "WHERE pd2.dy_samplingdemand_id = a.dy_samplingdemand_id " + "AND pdr.reviewer_id = " + currentUserId
				+ " AND pdr.isactive = 'Y')";

		// 去掉 superWhere 前导 "AND "，得到纯条件字符串
		String userCondition = superWhere.trim();
		if (userCondition.toUpperCase().startsWith("AND ")) {
			userCondition = userCondition.substring(4).trim();
		}

		// 三段 OR 拼接，整体作为一个 AND 条件
		prevWhereClause = " AND (" + userCondition + " OR " + segment2 + " OR " + segment3 + ")";
		return prevWhereClause;
    }  

	/**
	 * 判断用户是否在查询条件框中输入了值
	 */
	private boolean hasUserQueryInput() {
		if (editors == null)
			return false;
		for (WEditor editor : editors) {
			if (editor.isVisible() && editor.getValue() != null && !editor.getValue().toString().trim().isEmpty()) {
				return true;
			}
		}
		return false;
	}

}