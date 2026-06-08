package com.hoifu.editor;  
  
import org.adempiere.webui.ValuePreference;  
import org.adempiere.webui.editor.IEditorConfiguration;  
import org.adempiere.webui.editor.WEditor;  
import org.adempiere.webui.editor.WEditorPopupMenu;  
import org.adempiere.webui.event.ContextMenuEvent;  
import org.adempiere.webui.event.ContextMenuListener;  
import org.adempiere.webui.event.ValueChangeEvent;  
import org.adempiere.webui.window.WFieldRecordInfo;  
import org.compiere.model.GridField;  
import org.zkoss.zk.ui.event.Event;  
import org.zkoss.zk.ui.event.Events;  
  
/**  
 * Field editor for '纸箱计算公式' reference type.  
 * Stores and displays a formula string like (a-b)*(a+b)-(x+y).  
 */  
public class WBoxFormulaEditor extends WEditor implements ContextMenuListener {  
  
    public static final String[] LISTENER_EVENTS = { Events.ON_CHANGE, Events.ON_OK };  
  
    private String oldValue;  
  
    public WBoxFormulaEditor(GridField gridField, boolean tableEditor,  
            IEditorConfiguration editorConfiguration) {  
        super(new BoxFormulaDiv(tableEditor), gridField, tableEditor, editorConfiguration);  
        init();  
    }  
  
    private void init() {  
        setChangeEventWhenEditing(true);  
        if (gridField != null) {  
            getComponent().getTextbox().setTooltiptext(gridField.getDescription());  
            getComponent().getTextbox().setMaxlength(gridField.getFieldLength());  
            if (!tableEditor) {  
                int displayLength = gridField.getDisplayLength();  
                if (displayLength <= 0 || displayLength > MAX_DISPLAY_LENGTH)  
                    displayLength = MAX_DISPLAY_LENGTH;  
                getComponent().getTextbox().setCols(displayLength);  
            }  
            if (gridField.getPlaceholder() != null)  
                getComponent().getTextbox().setPlaceholder(gridField.getPlaceholder());  
        }  
        popupMenu = new WEditorPopupMenu(false, false, isShowPreference());  
        addChangeLogMenu(popupMenu);  
    }  
  
    @Override  
    public BoxFormulaDiv getComponent() {  
        return (BoxFormulaDiv) component;  
    }  
  
    @Override  
    public boolean isReadWrite() {  
        return getComponent().isEnabled();  
    }  
  
    @Override  
    public void setReadWrite(boolean readWrite) {  
        getComponent().setEnabled(readWrite);  
    }  
  
    @Override  
    public void onEvent(Event event) {  
        // INIT_EDIT_EVENT：用户开始编辑时触发（由 setChangeEventWhenEditing(true) 启用）  
        boolean isStartEdit = INIT_EDIT_EVENT.equalsIgnoreCase(event.getName());  
        if (Events.ON_CHANGE.equals(event.getName())  
                || Events.ON_OK.equals(event.getName())  
                || isStartEdit) {  
            String newValue = getComponent().getValue();  
            if (!isStartEdit && oldValue != null && oldValue.equals(newValue))  
                return;  
            if (!isStartEdit && oldValue == null && (newValue == null || newValue.isEmpty()))  
                return;  
            ValueChangeEvent changeEvent = new ValueChangeEvent(this, getColumnName(), oldValue, newValue);  
            changeEvent.setIsInitEdit(isStartEdit);  
            super.fireValueChange(changeEvent);  
            if (!isStartEdit)  
                oldValue = getComponent().getValue(); // callout 可能修改值，重新读取  
        }  
    }  
  
    @Override  
    public String getDisplay() {  
        return getComponent().getValue();  
    }  
  
    @Override  
    public Object getValue() {  
        String v = getComponent().getValue();  
        return (v == null || v.isEmpty()) ? null : v;  
    }  
  
    @Override  
    public void setValue(Object value) {  
        oldValue = value == null ? null : value.toString();  
        getComponent().setValue(oldValue == null ? "" : oldValue);  
    }  
  
    @Override  
    public String[] getEvents() {  
        return LISTENER_EVENTS;  
    }  
  
    @Override  
    public void setTableEditor(boolean b) {  
        super.setTableEditor(b);  
        getComponent().setTableEditorMode(b);  
    }  
  
    @Override  
    protected void setFieldStyle(String style) {  
        if (style != null)  
            getComponent().getTextbox().setStyle(style);  
    }  
  
    @Override  
    public void onMenu(ContextMenuEvent evt) {  
        if (WEditorPopupMenu.PREFERENCE_EVENT.equals(evt.getContextEvent())) {  
            if (isShowPreference())  
                ValuePreference.start(getComponent(), getGridField(), getValue());  
        } else if (WEditorPopupMenu.CHANGE_LOG_EVENT.equals(evt.getContextEvent())) {  
            WFieldRecordInfo.start(gridField);  
        }  
    }  
}