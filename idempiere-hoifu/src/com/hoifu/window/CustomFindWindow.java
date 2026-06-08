package com.hoifu.window;

import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.window.FindWindow;
import org.compiere.model.GridField;
import org.zkoss.zul.Comboitem;

public class CustomFindWindow extends FindWindow {  
    
    public CustomFindWindow(int targetWindowNo, int targetTabNo, String title,  
            int AD_Table_ID, String tableName, String whereExtended,  
            GridField[] findFields, int minRecords, int adTabId, AbstractADWindowContent windowPanel) {  
        super(targetWindowNo, targetTabNo, title, AD_Table_ID, tableName,  
              whereExtended, findFields, minRecords, adTabId, windowPanel);  
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
            condition.append(".").append(dateColumn)  
                     .append(" >= TRUNC(CURRENT_DATE, 'MM')")  
                     .append(" AND ").append(dateColumn)  
                     .append(" < TRUNC(CURRENT_DATE, 'MM') + INTERVAL '1 month'");  
        } else if (selectedHistoryValue.equals(HISTORY_DAY_YEAR)) {  
            condition.append(".").append(dateColumn)  
                     .append(" >= TRUNC(CURRENT_DATE, 'Y')")  
                     .append(" AND ").append(dateColumn)  
                     .append(" < TRUNC(CURRENT_DATE, 'Y') + INTERVAL '1 year'");  
        } else if (selectedHistoryValue.equals(HISTORY_DAY_WEEK)) {  
            condition.append(".").append(dateColumn)  
                     .append(" >= TRUNC(CURRENT_DATE, 'W')")  
                     .append(" AND ").append(dateColumn)  
                     .append(" < TRUNC(CURRENT_DATE, 'W') + INTERVAL '1 week'");  
        } else if (selectedHistoryValue.equals(HISTORY_DAY_DAY)) {  
            condition.append(".").append(dateColumn)  
                     .append(" >= TRUNC(CURRENT_DATE, 'D')")  
                     .append(" AND ").append(dateColumn)  
                     .append(" < TRUNC(CURRENT_DATE, 'D') + INTERVAL '1 day'");  
        } else {  
            // 其他选项保持原有逻辑  
            condition.append(".").append(dateColumn)  
                     .append(" >= CURRENT_DATE - ").append(getHistoryDays(selectedHistoryValue));  
        }  
          
        return condition.toString();  
    }  
}