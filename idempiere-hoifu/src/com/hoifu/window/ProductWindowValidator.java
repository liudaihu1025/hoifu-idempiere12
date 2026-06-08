package com.hoifu.window;  
  
import org.adempiere.util.Callback;  
import org.adempiere.webui.adwindow.ADWindow;  
import org.adempiere.webui.adwindow.validator.WindowValidator;  
import org.adempiere.webui.adwindow.validator.WindowValidatorEvent;  
import org.adempiere.webui.adwindow.validator.WindowValidatorEventType;  
import org.adempiere.webui.component.Messagebox;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.util.DB;
import org.compiere.util.Env;   

/**
 * 产品资料窗口事件监听
 */
public class ProductWindowValidator implements WindowValidator {  
  
	private static final String COLUMNNAME_CartonMaterial_ID = "CartonMaterial_ID";
    @Override  
    public void onWindowEvent(WindowValidatorEvent event, Callback<Boolean> callback) {  
        if (WindowValidatorEventType.BEFORE_SAVE.getName().equals(event.getName())) {  
        	CartonMateriaCheckBeforeSave(event, callback);  
        } else {  
            if (callback != null)  
                callback.onCallback(true);  
        }  
    }  
  
    private void CartonMateriaCheckBeforeSave(WindowValidatorEvent event, Callback<Boolean> callback) {  
        ADWindow adwindow = event.getWindow();  
        GridTab gridTab = adwindow.getADWindowContent().getActiveGridTab();  
  
        // 只处理主表（M_Product），跳过子 Tab  
        if (!MProduct.Table_Name.equals(gridTab.getTableName())) {  
            if (callback != null)  
                callback.onCallback(true);  
            return;  
        }  
  
        GridField field = gridTab.getField(COLUMNNAME_CartonMaterial_ID);  
  
        // 新记录或字段不存在，直接放行  
        if (field == null || gridTab.isNew()) {  
            if (callback != null)  
                callback.onCallback(true);  
            return;  
        }  
  
        // 校验产品所属组织，只有 Value="0213" 的组织才需要处理  
        int orgId = (Integer) gridTab.getValue("AD_Org_ID");  
        String orgValue = DB.getSQLValueString(null,  
                "SELECT Value FROM AD_Org WHERE AD_Org_ID=?", orgId);  
        if (!"0213".equals(orgValue)) {  
            if (callback != null)  
                callback.onCallback(true);  
            return;  
        }
        
        Object newValue = field.getValue();  
        
     // 从数据库加载，获取真实的旧值（而非 GridField 内存中的 oldValue）  
        int mProductId = gridTab.getRecord_ID();
        MProduct dbProduct = new MProduct(Env.getCtx(), mProductId, null);  
        Object oldValue = dbProduct.get_Value(COLUMNNAME_CartonMaterial_ID);  
  
        // 检测字段是否被修改  
        boolean changed = (newValue != null && !newValue.equals(oldValue))  
                       || (newValue == null && oldValue != null);  
  
        if (!changed) {  
            if (callback != null)  
                callback.onCallback(true);  
            return;  
        }  
  
        // 弹出确认对话框（YES + CANCEL）  
        Messagebox.showDialog(  
            "纸箱材质已修改，是否更新BOM信息",  
            "确认",  
            Messagebox.YES | Messagebox.CANCEL,  
            Messagebox.QUESTION,  
            new Callback<Integer>() {  
                @Override  
                public void onCallback(Integer result) {  
                    if (result != null && result == Messagebox.YES) {  
                        // 用户确认，继续保存；AbstractEventHandler 负责更新 BOMLine  
                        if (callback != null)  
                            callback.onCallback(true);  
                    } else {  
                        // 用户取消：先将字段显示值恢复为旧值，再中断保存  
                        gridTab.setValue(COLUMNNAME_CartonMaterial_ID, oldValue);  
                        if (callback != null)  
                            callback.onCallback(false);  
                    }  
                }  
            }  
        );  
    }  
}