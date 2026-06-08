package com.hoifu.editor;  
  
import org.adempiere.webui.LayoutUtils;  
import org.adempiere.webui.component.Button;  
import org.adempiere.webui.component.Textbox;  
import org.adempiere.webui.util.ZKUpdateUtil;  
import org.zkoss.zk.ui.event.Event;  
import org.zkoss.zk.ui.event.EventListener;  
import org.zkoss.zk.ui.event.Events;  
import org.zkoss.zul.Div;  
import org.zkoss.zul.Hbox;  
import org.zkoss.zul.Popup;  
import org.zkoss.zul.Vbox;  
  
public class BoxFormulaDiv extends Div {  
  
    private static final long serialVersionUID = 1L;  
  
    private Textbox textbox;  
    private Button  btn;  
    private Popup   popup;  
    private Textbox txtFormula;  
  
    public BoxFormulaDiv() { this(false); }  
  
    public BoxFormulaDiv(boolean tableEditor) {  
        super();  
        init(tableEditor);  
    }  
  
    private void init(boolean tableEditor) {  
        textbox = new Textbox();  
        textbox.setStyle("display: inline-block;");  
        textbox.setSclass("editor-input");  
        textbox.setId(textbox.getUuid());  
        textbox.setReadonly(true);  
        ZKUpdateUtil.setHflex(textbox, "0");  
        appendChild(textbox);  
  
        btn = new Button("f(x)");  
        btn.setTabindex(-1);  
        ZKUpdateUtil.setHflex(btn, "0");  
        btn.setStyle("background-color: black; color: white; border: none;");  
  
        btn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {  
            @Override  
            public void onEvent(Event event) throws Exception {  
                if (popup != null) {  
                    txtFormula.setValue(textbox.getValue());  
                    popup.open(BoxFormulaDiv.this, "after_start");  
                }  
            }  
        });  
        LayoutUtils.addSclass("editor-button", btn);  
        appendChild(btn);  
  
        popup = buildFormulaPopup();  
        appendChild(popup);  
  
        LayoutUtils.addSclass("editor-box", this);  
        setTableEditorMode(tableEditor);  
    }  
  
    private Popup buildFormulaPopup() {  
        Popup p = new Popup();  
        Vbox vbox = new Vbox();  
  
        txtFormula = new Textbox();  
        txtFormula.setId(txtFormula.getUuid());  
        txtFormula.setCols(30);  
        txtFormula.setMaxlength(250);  
        txtFormula.setStyle("width:100%; margin-bottom:4px;");  
        vbox.appendChild(txtFormula);  
  
        String fId = txtFormula.getId();  
  
        // 第 1 行：长度、宽度、高度、钉口、接插口  
        Hbox row1 = new Hbox();  
        row1.appendChild(makeInsertBtn("长度", "长度", "40px", fId));  
        row1.appendChild(makeInsertBtn("宽度", "宽度", "40px", fId));  
        row1.appendChild(makeInsertBtn("高度", "高度", "40px", fId));  
        row1.appendChild(makeInsertBtn("钉口", "钉口", "40px", fId));  
        row1.appendChild(makeInsertBtn("接插口", "接插口", "40px", fId));  
  
        // 第 2 行：系数、净重、厚度、AC、C  
        Hbox row2 = new Hbox();  
        row2.appendChild(makeInsertBtn("系数", "系数", "40px", fId));  
        row2.appendChild(makeInsertBtn("净重", "净重", "40px", fId));  
        row2.appendChild(makeInsertBtn("厚度", "厚度", "40px", fId));  
        row2.appendChild(makeAcBtn(fId));  
        row2.appendChild(makeCBtn(fId));  
  
        // 第 3 行：1、2、3、+、-  
        Hbox row3 = new Hbox();  
        row3.appendChild(makeInsertBtn("1", "1", "40px", fId));  
        row3.appendChild(makeInsertBtn("2", "2", "40px", fId));  
        row3.appendChild(makeInsertBtn("3", "3", "40px", fId));  
        row3.appendChild(makeInsertBtn("+", "+", "40px", fId));  
        row3.appendChild(makeInsertBtn("-", "-", "40px", fId));  
  
        // 第 4 行：4、5、6、*、/  
        Hbox row4 = new Hbox();  
        row4.appendChild(makeInsertBtn("4", "4", "40px", fId));  
        row4.appendChild(makeInsertBtn("5", "5", "40px", fId));  
        row4.appendChild(makeInsertBtn("6", "6", "40px", fId));  
        row4.appendChild(makeInsertBtn("*", "*", "40px", fId));  
        row4.appendChild(makeInsertBtn("/", "/", "40px", fId));  
  
        // 第 5 行：7、8、9  
        Hbox row5 = new Hbox();  
        row5.appendChild(makeInsertBtn("7", "7", "40px", fId));  
        row5.appendChild(makeInsertBtn("8", "8", "40px", fId));  
        row5.appendChild(makeInsertBtn("9", "9", "40px", fId));  
        row5.appendChild(makeInsertBtn(".", ".", "40px", fId));  
  
        // 第 6 行：(、0、)、确认  
        Hbox row6 = new Hbox();  
        row6.appendChild(makeInsertBtn("(", "(", "40px", fId));  
        row6.appendChild(makeInsertBtn("0", "0", "40px", fId));  
        row6.appendChild(makeInsertBtn(")", ")", "40px", fId));  
  
        Button btnOk = new Button("确认");  
        ZKUpdateUtil.setWidth(btnOk, "40px");  
        btnOk.setStyle("background-color: #1a73e8; color: white;");  
        btnOk.addEventListener(Events.ON_CLICK, e -> {  
            textbox.setValue(txtFormula.getValue());  
            Events.postEvent(Events.ON_CHANGE, textbox, null);  
            p.close();  
        });  
        row6.appendChild(btnOk);  
  
        vbox.appendChild(row1);  
        vbox.appendChild(row2);  
        vbox.appendChild(row3);  
        vbox.appendChild(row4);  
        vbox.appendChild(row5);  
        vbox.appendChild(row6);  
  
        p.appendChild(vbox);  
  
        p.addEventListener(Events.ON_CANCEL, e -> p.close());  
  
        return p;  
    }  
  
    private Button makeInsertBtn(String label, String insertText, String width, String tbId) {  
        Button b = new Button(label);  
        ZKUpdateUtil.setWidth(b, width);  
        String escaped = insertText.replace("\\", "\\\\").replace("'", "\\'");  
        b.setWidgetListener("onClick", buildInsertJs(tbId, escaped, insertText.length()));  
        return b;  
    }  
  
    private Button makeAcBtn(String tbId) {  
        Button b = new Button("AC");  
        ZKUpdateUtil.setWidth(b, "40px");  
        b.setWidgetListener("onClick",  
            "var tb=jq('$" + tbId + "')[0];" +  
            "tb.value='';" +  
            "tb.focus();"  
        );  
        return b;  
    }  
  
    private Button makeCBtn(String tbId) {  
        Button b = new Button("C");  
        ZKUpdateUtil.setWidth(b, "40px");  
        b.setWidgetListener("onClick",  
            "var tb=jq('$" + tbId + "')[0];" +  
            "var pos=tb.selectionStart;" +  
            "if(pos>0){" +  
            "  var val=tb.value;" +  
            "  tb.value=val.substring(0,pos-1)+val.substring(pos);" +  
            "  tb.setSelectionRange(pos-1,pos-1);" +  
            "}" +  
            "tb.focus();"  
        );  
        return b;  
    }  
  
    private String buildInsertJs(String tbId, String escapedCh, int chLen) {  
        return "var tb=jq('$" + tbId + "')[0];" +  
               "var pos=tb.selectionStart;" +  
               "var val=tb.value;" +  
               "tb.value=val.substring(0,pos)+'" + escapedCh + "'+val.substring(pos);" +  
               "tb.setSelectionRange(pos+" + chLen + ",pos+" + chLen + ");" +  
               "tb.focus();";  
    }  
  
    public String getValue()              { return textbox.getValue(); }  
    public void   setValue(String value)  { textbox.setValue(value == null ? "" : value); }  
    public Textbox getTextbox()           { return textbox; }  
    public Button  getButton()            { return btn; }  
  
    public void setEnabled(boolean enabled) {  
        textbox.setDisabled(!enabled);  
        btn.setEnabled(enabled);  
        if (enabled) {  
            if (btn.getParent() != textbox.getParent())  
                btn.setParent(textbox.getParent());  
            LayoutUtils.removeSclass("editor-input-disd", textbox);  
        } else {  
            if (btn.getParent() != null)  
                btn.detach();  
            LayoutUtils.addSclass("editor-input-disd", textbox);  
        }  
        textbox.setReadonly(true);  
    }  
  
    public boolean isEnabled() { return btn.isEnabled(); }  
  
    public void setTableEditorMode(boolean flag) {  
        if (flag) {  
            ZKUpdateUtil.setHflex(this, "0");  
            LayoutUtils.addSclass("grid-editor-input", textbox);  
            LayoutUtils.addSclass("grid-editor-button", btn);  
        } else {  
            ZKUpdateUtil.setHflex(this, "1");  
            LayoutUtils.removeSclass("grid-editor-input", textbox);  
            LayoutUtils.removeSclass("grid-editor-button", btn);  
        }  
    }  
  
    @Override  
    public boolean addEventListener(String evtnm, EventListener<?> listener) {  
        if (Events.ON_CLICK.equals(evtnm))  
            return btn.addEventListener(evtnm, listener);  
        else  
            return textbox.addEventListener(evtnm, listener);  
    }  
  
    @Override  
    public void focus() { textbox.focus(); }  
}