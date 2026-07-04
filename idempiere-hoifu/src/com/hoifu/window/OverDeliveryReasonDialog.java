package com.hoifu.window;

import org.adempiere.util.Callback;  
import org.adempiere.webui.apps.AEnv;  
import org.adempiere.webui.component.ConfirmPanel;  
import org.adempiere.webui.component.Window;  
import org.adempiere.webui.util.ZKUpdateUtil;  
import org.compiere.util.Env;  
import org.zkoss.zk.ui.event.Event;  
import org.zkoss.zk.ui.event.EventListener;  
import org.zkoss.zk.ui.event.Events;  
import org.zkoss.zul.*;  
  
/**  
 * 超发确认弹窗：显示警告信息，要求用户填写超发原因（多行文本，必填）  
 */  
public class OverDeliveryReasonDialog extends Window implements EventListener<Event> {  
  
    private static final long serialVersionUID = 1L;  
  
    private Textbox reasonTextbox;  
    private ConfirmPanel confirmPanel;  
    private Callback<String> callback; // null = 取消，非null = 确认并返回原因  
  
    public OverDeliveryReasonDialog(Callback<String> callback) {  
        this.callback = callback;  
        init();  
    }  
  
    private void init() {  
        setTitle("超发确认");  
        setSclass("popup-dialog");  
        setBorder("normal");  
        setClosable(true);  
        setSizable(true);  
        setShadow(true);  
        setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);  
        ZKUpdateUtil.setWindowWidthX(this, 480);  
        ZKUpdateUtil.setWindowHeightX(this, 320);  
  
        Borderlayout layout = new Borderlayout();  
        layout.setHflex("1");  
        layout.setVflex("1");  
        appendChild(layout);  
  
        // ── 中间内容区 ──────────────────────────────────────  
        Center center = new Center();  
        center.setHflex("1");  
        center.setVflex("1");  
        center.setStyle("padding: 12px 16px;");  
        layout.appendChild(center);  
  
        Vlayout vbox = new Vlayout();  
        vbox.setWidth("100%");  
        vbox.setHeight("100%");  
        center.appendChild(vbox);  
  
        // 警告图标 + 提示文字  
        Hbox warningBox = new Hbox();  
        warningBox.setAlign("center");  
        warningBox.setStyle("margin-bottom: 12px;");  
        vbox.appendChild(warningBox);  
  
        Label warningIcon = new Label();  
        warningIcon.setSclass("z-icon-ExclamationMessageBox");  
        warningIcon.setStyle("font-size: 24px; color: #e6a817; margin-right: 8px;");  
        warningBox.appendChild(warningIcon);  
  
        Label warningMsg = new Label("发货数量已超过订单数量，是否继续发货？");  
        warningMsg.setStyle("font-weight: bold; font-size: 14px;");  
        warningBox.appendChild(warningMsg);  
  
        // 原因标签  
        Label reasonLabel = new Label("超发原因（必填）：");  
        reasonLabel.setStyle("margin-bottom: 4px; font-weight: bold;");  
        vbox.appendChild(reasonLabel);  
  
        // 多行文本框  
        reasonTextbox = new Textbox();  
        reasonTextbox.setMultiline(true);  
        reasonTextbox.setRows(5);  
        reasonTextbox.setPlaceholder("请填写超发原因...");  
        ZKUpdateUtil.setHflex(reasonTextbox, "1");  
        reasonTextbox.setStyle("resize: vertical;");  
        vbox.appendChild(reasonTextbox);  
  
        // ── 底部按钮区 ──────────────────────────────────────  
        South south = new South();  
        south.setSclass("dialog-footer");  
        layout.appendChild(south);  
  
        confirmPanel = new ConfirmPanel(true);  
        confirmPanel.addActionListener(this);  
        south.appendChild(confirmPanel);  
  
        addEventListener(Events.ON_CANCEL, e -> onCancel());  
    }  
  
    @Override  
    public void onEvent(Event event) throws Exception {  
        String targetId = event.getTarget().getId();  
        if (ConfirmPanel.A_OK.equals(targetId)) {  
            onOk();  
        } else if (ConfirmPanel.A_CANCEL.equals(targetId)) {  
            onCancel();  
        }  
    }  
  
    private void onOk() {  
        String reason = reasonTextbox.getText();  
        if (reason == null || reason.trim().isEmpty()) {  
            reasonTextbox.setStyle("border: 1px solid red; resize: vertical;");  
            reasonTextbox.focus();  
            return;  
        }  
        detach();  
        if (callback != null)  
            callback.onCallback(reason.trim());  
    }  
  
    private void onCancel() {  
        detach();  
        if (callback != null)  
            callback.onCallback(null); // null 表示取消  
    }  
}