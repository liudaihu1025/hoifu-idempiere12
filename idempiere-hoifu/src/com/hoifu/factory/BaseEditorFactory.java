package com.hoifu.factory;  
  
import org.adempiere.webui.editor.IEditorConfiguration;  
import org.adempiere.webui.editor.WEditor;  
import org.adempiere.webui.factory.IEditorFactory;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;
import org.compiere.model.MReference;

import com.hoifu.editor.WBoxFormulaEditor;  
  
/**  
 * Editor factory for '纸箱计算公式' reference type.  
 */  
public class BaseEditorFactory implements IEditorFactory {  
  
    @Override  
    public WEditor getEditor(GridTab gridTab, GridField gridField, boolean tableEditor) {  
        return getEditor(gridTab, gridField, tableEditor, null);  
    }  
  
    @Override  
    public WEditor getEditor(GridTab gridTab, GridField gridField, boolean tableEditor,  
            IEditorConfiguration editorConfiguration) { 
    	String uuid = MReference.get(gridField.getDisplayType()).get_UUID();
        if (gridField != null && BaseDisplayTypeFactory.BOX_FORMULA_UUID.equals(uuid)) {  
            return new WBoxFormulaEditor(gridField, tableEditor, editorConfiguration);  
        }  
        return null; // 不是本类型，让系统继续查找其他工厂  
    }  
}