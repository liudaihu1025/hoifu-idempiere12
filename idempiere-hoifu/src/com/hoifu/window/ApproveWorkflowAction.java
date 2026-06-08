package com.hoifu.window;  
  
import java.util.List;  
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.action.IAction;  
import org.adempiere.webui.adwindow.ADWindow;  
import org.adempiere.webui.window.Dialog;  
import org.compiere.model.Query;  
import org.compiere.util.CLogger;  
import org.compiere.util.DisplayType;  
import org.compiere.util.Env;  
import org.compiere.util.Msg;  
import org.compiere.util.Trx;  
import org.compiere.util.Util;  
import org.compiere.wf.MWFActivity;  
import org.compiere.wf.MWFNode;  
import org.compiere.wf.MWFProcess;  
import org.osgi.service.component.annotations.Component;  
import org.zkoss.zk.ui.event.Events;  
import org.zkoss.zul.Button;  
import org.zkoss.zul.Hbox;  
import org.zkoss.zul.Label;  
import org.zkoss.zul.Listbox;  
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vlayout;  
import org.zkoss.zul.Window;  
  
@Component(name = "com.hoifu.window.ApproveWorkflowAction", service = {IAction.class})  
public class ApproveWorkflowAction implements IAction {  
  
    private static final CLogger log = CLogger.getCLogger(ApproveWorkflowAction.class);  
    
    @Override  
    public void execute(Object target) {  
        if (!(target instanceof ADWindow adwindow)) return;  
  
        var gridTab = adwindow.getADWindowContent().getADTab().getSelectedGridTab();  
        int AD_Table_ID = gridTab.getAD_Table_ID();  
        int Record_ID   = gridTab.getRecord_ID();  
        if (Record_ID <= 0) return;  
  
        int AD_User_ID   = Env.getAD_User_ID(Env.getCtx());  
        int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());  
  
        // getWhereUserPendingActivities() 有 6 个占位符（AD_User_ID×5 + AD_Client_ID×1）  
        // 加上 AD_Table_ID 和 Record_ID，共 8 个参数  
        String where = "AD_WF_Activity.AD_Table_ID=? AND AD_WF_Activity.Record_ID=? AND "  
                     + MWFActivity.getWhereUserPendingActivities();  
  
        List<MWFActivity> activities = new Query(Env.getCtx(), MWFActivity.Table_Name, where, null)  
            .setApplyAccessFilter(true, false)  
            .setParameters(AD_Table_ID, Record_ID,  
                           AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID,  
                           AD_Client_ID)  
            .list();  
  
        if (activities.isEmpty()) {  
        	Dialog.info(0, null, "当前记录没有待处理的工作流活动");
            return;  
        }  
  
        // 取第一个（如有多个可后续扩展为列表选择）  
        showApprovalDialog(adwindow, activities.get(0));  
    }  
  
    private void showApprovalDialog(ADWindow adwindow, MWFActivity activity) {  
        MWFNode node = activity.getNode();  
        boolean isApproval = node.isUserApproval();  
  
        Window win = new Window();  
        win.setTitle(Msg.getMsg(Env.getCtx(), "WorkFlow") + " - " + activity.getNodeName());  
        win.setBorder("normal");  
        win.setWidth("400px");  
        win.setClosable(true);  
  
        Vlayout layout = new Vlayout();  
        layout.setStyle("padding:12px;gap:8px");  
  
        // 节点描述（可选显示）  
        String desc = activity.getNodeDescription();  
        if (!Util.isEmpty(desc, true)) {  
            Label lblDesc = new Label(desc);  
            lblDesc.setStyle("color:#666;font-size:12px");  
            layout.appendChild(lblDesc);  
        }  
  
        // 操作选择下拉  
        layout.appendChild(new Label(isApproval ? "审批结果:" : "操作:"));  
        Listbox actionList = new Listbox();  
        actionList.setMold("select");  
        actionList.setWidth("100%");  
        if (isApproval) {  
        	actionList.appendItem("同意", "Y");  
        	actionList.appendItem("不同意", "N");
        } else {  
            actionList.appendItem(Msg.getMsg(Env.getCtx(), "OK"), "OK");  
        }  
        actionList.setSelectedIndex(0);  
        layout.appendChild(actionList);  
  
        // 审批意见输入框  
        layout.appendChild(new Label("审批意见:"));  
        Textbox txtMsg = new Textbox();  
        txtMsg.setMultiline(true);  
        txtMsg.setRows(3);  
        txtMsg.setWidth("100%");  
        layout.appendChild(txtMsg);  
  
        // 按钮行  
        Hbox btnBox = new Hbox();  
        btnBox.setStyle("margin-top:10px");  
        Button btnOK     = new Button(Msg.getMsg(Env.getCtx(), "OK"));  
        Button btnCancel = new Button(Msg.getMsg(Env.getCtx(), "Cancel"));  
  
        btnOK.addEventListener(Events.ON_CLICK,  
            e -> doApprove(adwindow, activity, isApproval, actionList, txtMsg, win));  
        btnCancel.addEventListener(Events.ON_CLICK,  
            e -> win.detach());  
  
        btnBox.appendChild(btnOK);  
        btnBox.appendChild(btnCancel);  
        layout.appendChild(btnBox);  
  
        win.appendChild(layout);  
        adwindow.getComponent().getParent().appendChild(win);  
        LayoutUtils.openOverlappedWindow(adwindow.getComponent(), win, "middle_center");  
    }  
  
    private void doApprove(ADWindow adwindow, MWFActivity activity, boolean isApproval,  
                           Listbox actionList, Textbox txtMsg, Window win) {  
        int AD_User_ID = Env.getAD_User_ID(Env.getCtx());  
        int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());  
  
        // 二次权限校验  
        String checkWhere = "AD_WF_Activity.AD_WF_Activity_ID=? AND "  
                          + MWFActivity.getWhereUserPendingActivities();  
        int count = new Query(Env.getCtx(), MWFActivity.Table_Name, checkWhere, null)  
            .setApplyAccessFilter(true, false)  
            .setParameters(activity.getAD_WF_Activity_ID(),  
                           AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID,  
                           AD_Client_ID)  
            .count();  
        if (count == 0) {  
            Dialog.error(0, "AccessTableNoUpdate");  
            return;  
        }  
  
        // 读取备注内容（空则传 null）  
        String textMsg = txtMsg.getValue();  
        if (Util.isEmpty(textMsg, true)) textMsg = null;  
  
        Trx trx = Trx.get(Trx.createTrxName("APWF"), true);  
        trx.setDisplayName(getClass().getName() + "_doApprove");  
        try {  
            activity.set_TrxName(trx.getTrxName());  
  
            if (isApproval) {  
                // 审批节点：同意(Y) / 不同意(N)  
                String value = actionList.getSelectedItem() != null  
                    ? actionList.getSelectedItem().getValue().toString()  
                    : "Y";  
                activity.setUserChoice(AD_User_ID, value, DisplayType.YesNo, textMsg);  
            } else {  
                // 确认节点  
                activity.setUserConfirmation(AD_User_ID, textMsg);  
            }  
  
            // 必须调用，否则工作流不会推进到下一节点  
            new MWFProcess(activity.getCtx(),  
                    activity.getAD_WF_Process_ID(), activity.get_TrxName())  
                .checkCloseActivities(activity.get_TrxName());  
  
            if (!Util.isEmpty(activity.getProcessMsg(), true))  
                Dialog.error(0, activity.getProcessMsg());  
  
            trx.commit();  
            win.detach();  
  
            // 审批完成后自动刷新单据窗口数据  
            adwindow.getADWindowContent().onRefresh(true, false);  
  
        } catch (Exception ex) {  
            log.log(Level.SEVERE, "ApproveWorkflow", ex);  
            Dialog.error(0, "Error", ex.getLocalizedMessage());  
            trx.rollback();  
        } finally {  
            trx.close();  
        }  
    }  
  
    @Override  
    public String getIconSclass() {  
        return "z-icon-Workflow";  
    }  
}