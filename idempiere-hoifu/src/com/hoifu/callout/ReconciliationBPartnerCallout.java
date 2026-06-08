package com.hoifu.callout;

import java.sql.Timestamp;  
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MBPartner;


/**
 * 对账单-根据业务伙伴自动计算出对账开始、结束日期和对账月份、根据对账月份计算对账开始、结束日期
 */
@Callout(tableName = "C_Reconciliation", columnName = {"C_BPartner_ID", "ReconPeroid"})  
public class ReconciliationBPartnerCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
        if (value == null || value.equals(oldValue)) {  
            return "";  
        }  
  
        String columnName = mField.getColumnName();  
        if ("C_BPartner_ID".equals(columnName)) {  
            return handleBPartnerChange(ctx, WindowNo, mTab, value);  
        } else if ("ReconPeroid".equals(columnName)) {  
            return handlePeriodChange(ctx, WindowNo, mTab, value);  
        }  
        return "";  
    }  
  
    /**  
     * 处理业务伙伴变更：设置地址/联系人，计算当前月对账日期并设置对账月份  
     */  
    private String handleBPartnerChange(Properties ctx, int WindowNo, GridTab mTab, Object value) {  
        Integer C_BPartner_ID = (Integer) value;  
        if (C_BPartner_ID <= 0) {  
            return "";  
        }  
  
        // 设置发票地址和联系人  
        setBPartnerInfo(ctx, WindowNo, mTab, C_BPartner_ID);  
  
        // 获取业务伙伴的对账日  
        MBPartner bpartner = new MBPartner(ctx, C_BPartner_ID, null);  
        Integer reconciliationDay = bpartner.get_ValueAsInt("cutoffday");  
        if (reconciliationDay <= 0 || reconciliationDay > 31) {  
            return "";  
        }  
  
        // 计算当前月的对账结束日期  
        LocalDate currentDate = LocalDate.now();  
        LocalDate currentMonthEnd = currentDate.withDayOfMonth(Math.min(reconciliationDay, currentDate.lengthOfMonth()));  
        // 计算上个月的对账开始日期  
        LocalDate lastMonth = currentDate.minusMonths(1);  
        LocalDate lastMonthReconciliation = lastMonth.withDayOfMonth(Math.min(reconciliationDay, lastMonth.lengthOfMonth()));  
        // 计算对账月份  
        String reconPeriod = currentMonthEnd.format(DateTimeFormatter.ofPattern("yyyyMM"));  
  
        // 设置日期字段  
        mTab.setValue("ReconciliationDay", Timestamp.valueOf(lastMonthReconciliation.atStartOfDay()));  
        mTab.setValue("Reconciliationcutoff", Timestamp.valueOf(currentMonthEnd.atStartOfDay()));  
        mTab.setValue("ReconPeroid", reconPeriod);  
  
        return "";  
    }  
  
    /**  
     * 处理对账月份变更：根据对账月份和业务伙伴的对账日重新计算开始/结束日期  
     */  
    private String handlePeriodChange(Properties ctx, int WindowNo, GridTab mTab, Object value) {  
        String reconPeriod = (String) value;  
        if (reconPeriod == null || reconPeriod.isEmpty()) {  
            return "";  
        } 
        
        if (reconPeriod.length() != 6) {  
            return "对账月份格式错误，必须为6位（yyyyMM）";  
        } 
        
        int year, month;  
        try {  
            year = Integer.parseInt(reconPeriod.substring(0, 4));  
            month = Integer.parseInt(reconPeriod.substring(4, 6));  
        } catch (NumberFormatException e) {  
            return "对账月份必须为数字";  
        }  
        if (month < 1 || month > 12) {  
            return "对账月份无效，月份必须在 01-12 之间";  
        } 
  
        // 获取当前记录的业务伙伴ID  
        Integer C_BPartner_ID = (Integer) mTab.getValue("C_BPartner_ID");  
        if (C_BPartner_ID == null || C_BPartner_ID <= 0) {  
            return "";  
        }  
  
        // 获取业务伙伴的对账日  
        MBPartner bpartner = new MBPartner(ctx, C_BPartner_ID, null);  
        Integer reconciliationDay = bpartner.get_ValueAsInt("cutoffday");  
        if (reconciliationDay <= 0 || reconciliationDay > 31) {  
            return "";  
        }  
    
        LocalDate targetMonth = LocalDate.of(year, month, 1);  
  
        // 使用公共方法设置日期  
        setReconciliationDates(mTab, reconciliationDay, targetMonth);  
  
        return "";  
    }  
  
    /**  
     * 设置业务伙伴的发票地址和联系人信息  
     */  
    private void setBPartnerInfo(Properties ctx, int WindowNo, GridTab mTab, Integer C_BPartner_ID) {  
        MBPartner bp = new MBPartner(ctx, C_BPartner_ID, null);  
  
        // 获取主地址（优先BillTo地址）  
        int locationID = bp.getPrimaryC_BPartner_Location_ID();  
        if (locationID > 0) {  
            mTab.setValue("C_BPartner_Location_ID", Integer.valueOf(locationID));  
        }  
  
        // 获取主联系人  
        int userID = bp.getPrimaryAD_User_ID();  
        if (userID > 0) {  
            mTab.setValue("AD_User_ID", Integer.valueOf(userID));  
        }  
    }  
  
    /**  
     * 公共日期计算逻辑：根据对账日和目标月份设置开始/结束日期  
     */  
    private static void setReconciliationDates(GridTab mTab, Integer reconciliationDay, LocalDate targetMonth) {  
        LocalDate currentMonthEnd = targetMonth.withDayOfMonth(Math.min(reconciliationDay, targetMonth.lengthOfMonth()));  
        LocalDate lastMonth = targetMonth.minusMonths(1);  
        LocalDate lastMonthReconciliation = lastMonth.withDayOfMonth(Math.min(reconciliationDay, lastMonth.lengthOfMonth()));  
  
        mTab.setValue("ReconciliationDay", Timestamp.valueOf(lastMonthReconciliation.atStartOfDay()));  
        mTab.setValue("Reconciliationcutoff", Timestamp.valueOf(currentMonthEnd.atStartOfDay()));  
    }  
}
