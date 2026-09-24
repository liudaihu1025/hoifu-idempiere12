package com.hoifu.info;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.NamePair;

/**
 * 来料检验任务列表信息窗口
 *
 * 功能：
 *   1. 强制单选（父类在 haveProcess=true 时会强制多选，通过 getSaveKeys/saveResultSelection 绕过）
 *   2. "检验"按钮按检验状态控制：已检验（InspectResult 非空）则灰色禁用
 *
 * 配置要求：
 *   AD_InfoWindow.ClassName = com.hoifu.info.IQCInspectListInfoWindow
 */
public class IQCInspectListInfoWindow extends InfoWindow {

    private static final long serialVersionUID = 1L;

    /** IQCInspectConfirmProcess 完整类名，用于识别"检验"按钮 */
    private static final String CONFIRM_PROCESS_CLASSNAME = "com.hoifu.process.IQCInspectConfirmProcess";

    public IQCInspectListInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
            boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
            String predefinedContextVariables) {
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
                field, predefinedContextVariables);

        // 强制单选（覆盖父类因 haveProcess 而设置的 true）
        setMultipleSelection(false);

        if (queryValue == null || queryValue.trim().isEmpty()) {
            executeQuery();
            renderItems();
            ((ListModelTable) contentPanel.getModel()).setMultiple(false);
            bindInfoProcessBt();
        }
    }

    /**
     * 修复 p_multipleSelection=false 时父类 getSaveKeys 直接返回 null 的问题
     * 临时切多选 → 调父类 → 切回单选
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
     * 修复 p_multipleSelection=false 时父类不填充 m_values 的问题
     * 临时切多选 → 调父类 → 切回单选 → 截取第一条（单选）
     */
    @Override
    protected void saveResultSelection(int infoColumnId) {
        setMultipleSelection(true);
        try {
            super.saveResultSelection(infoColumnId);
        } finally {
            setMultipleSelection(false);
        }
        if (m_values == null) {
            m_values = new LinkedHashMap<>();
            return;
        }
        // 单选：只保留第一条
        if (m_values.size() > 1) {
            NamePair firstKey = m_values.keySet().iterator().next();
            LinkedHashMap<NamePair, LinkedHashMap<String, Object>> single = new LinkedHashMap<>();
            single.put(firstKey, m_values.get(firstKey));
            m_values = single;
        }
    }

    /**
     * 按检验状态控制"检验"按钮的启用/禁用
     *
     * 逻辑：
     *   - 选中行数 != 1 → 禁用
     *   - 选中行的 InspectResult 非空（已检验）→ 禁用，tooltip 提示
     *   - 其他情况 → 启用
     */
    @Override
    protected void enableButtons() {
        super.enableButtons();

        List<Serializable> keys = getSelectedRowKeys();
        int selectedCount = keys == null ? 0 : keys.size();

        for (Button btProcess : btProcessList) {
            Integer processId = (Integer) btProcess.getAttribute(PROCESS_ID_KEY);
            if (processId == null)
                continue;

            MProcess process = MProcess.get(Env.getCtx(), processId);
            if (process == null || process.getClassname() == null)
                continue;

            if (CONFIRM_PROCESS_CLASSNAME.equals(process.getClassname())) {
                boolean enabled = selectedCount == 1 && !isAlreadyInspected(keys.get(0));
                btProcess.setEnabled(enabled);
            }
        }
    }

    /**
     * 判断选中的检验单是否已经检验（InspectResult 不为空）
     *
     * @param key 选中行主键（QC_IQCInspect_ID）
     * @return true=已检验，false=未检验
     */
    private boolean isAlreadyInspected(Serializable key) {
        int qcInspectId;
        try {
            qcInspectId = Integer.parseInt(key.toString());
        } catch (NumberFormatException e) {
            return false;
        }
        String inspectResult = DB.getSQLValueStringEx(null,
                "SELECT InspectResult FROM QC_IQCInspect WHERE QC_IQCInspect_ID=?", qcInspectId);
        return inspectResult != null && !inspectResult.trim().isEmpty();
    }
}
