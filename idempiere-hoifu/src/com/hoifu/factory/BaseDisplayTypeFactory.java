package com.hoifu.factory;  
  
import java.text.DecimalFormat;  
import java.text.SimpleDateFormat;  
  
import org.adempiere.base.IDisplayTypeFactory;
import org.compiere.model.MReference;
import org.compiere.util.Language;  
  
public class BaseDisplayTypeFactory implements IDisplayTypeFactory {  
  
    /**  
     * AD_Reference_ID = '纸箱计算公式'.  
     * AD_Reference_UU = 'f6aa0fdf-ed31-4dfb-b941-80e6f9470d90';  
     */  
    public static final String BOX_FORMULA_UUID = "f6aa0fdf-ed31-4dfb-b941-80e6f9470d90";   
  
    @Override public boolean isID(int displayType)       { return false; }  
    @Override public boolean isNumeric(int displayType)  { return false; }  
    @Override public Integer getDefaultPrecision(int dt) { return null; }  
    @Override public boolean isDate(int displayType)     { return false; }  
    @Override public boolean isLookup(int displayType)   { return false; }  
    @Override public boolean isLOB(int displayType)      { return false; }  
  
    @Override  
    public boolean isText(int displayType) {  
    	String uuid = MReference.get(displayType).get_UUID();
    	if (BOX_FORMULA_UUID.equals(uuid)) {
    		return true;
    	}
        return false;  
    }  
  
    @Override  
    public Class<?> getClass(int displayType, boolean yesNoAsBoolean) {
    	String uuid = MReference.get(displayType).get_UUID();
    	if (BOX_FORMULA_UUID.equals(uuid)) {
    		return String.class;
    	}
        return null;  
    }  
  
    @Override  
    public String getSQLDataType(int displayType, String columnName, int fieldLength) {
    	String uuid = MReference.get(displayType).get_UUID();
    	if (BOX_FORMULA_UUID.equals(uuid)) {
    		return "VARCHAR(" + fieldLength + ")";
    	}
        return null;  
    }  
  
    @Override  
    public String getDescription(int displayType) {
    	String uuid = MReference.get(displayType).get_UUID();
    	if (BOX_FORMULA_UUID.equals(uuid)) {
    		return "纸箱计算公式";
    	}
        return null;  
    }  
  
    @Override  
    public DecimalFormat getNumberFormat(int displayType, Language language, String pattern) {  
        return null;  
    }  
  
    @Override  
    public SimpleDateFormat getDateFormat(int displayType, Language language, String pattern) {  
        return null;  
    }  
}