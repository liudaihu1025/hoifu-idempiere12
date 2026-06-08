package org.libero.process;  
  
import org.adempiere.webui.action.IAction;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.ADWindowContent;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.osgi.service.component.annotations.Component;
import org.zkoss.zul.Toolbarbutton;  
  
@Component(name = "org.libero.process.WFNodeMoveUpAction", service = { IAction.class })
public class WFNodeMoveUpAction implements IAction {  
  
    @Override  
    public void execute(Object target) {  
        if (!(target instanceof ADWindow)) return;  
  
        ADWindow adwindow = (ADWindow) target;  
        ADWindowContent content = adwindow.getADWindowContent();  
  
        // 找工序标签页  
        GridTab wfNodeGridTab = null;  
        for (int i = 0; i < content.getADTab().getTabCount(); i++) {  
            IADTabpanel tabpanel = content.getADTab().getADTabpanel(i);  
            if (MWFNode.Table_Name.equals(tabpanel.getGridTab().getTableName())) {  
                wfNodeGridTab = tabpanel.getGridTab();  
                break;  
            }  
        }  
        if (wfNodeGridTab == null) return;  
  
        int currentRecordId = wfNodeGridTab.getRecord_ID();  
        if (currentRecordId <= 0) return;  
  
		MWFNode node = new MWFNode(Env.getCtx(), currentRecordId, null);
        int workflowId = node.getAD_Workflow_ID();  
        String currentValue = node.getValue();  
  
        // 找上一个工序（Value 比当前小的最大值）  
        int prevNodeId = 0;  
        String prevValue = null;  
        java.sql.PreparedStatement pstmt = null;  
        java.sql.ResultSet rs = null;  
        try {  
            String sql = "SELECT AD_WF_Node_ID, Value FROM AD_WF_Node " +  
                "WHERE AD_Workflow_ID=? AND IsActive='Y' " +  
                "AND CASE WHEN Value ~ '^[0-9]+$' THEN Value::INTEGER ELSE NULL END < " +  
                "CASE WHEN ? ~ '^[0-9]+$' THEN ?::INTEGER ELSE NULL END " +  
                "ORDER BY CASE WHEN Value ~ '^[0-9]+$' THEN Value::INTEGER ELSE NULL END DESC LIMIT 1";  
            pstmt = DB.prepareStatement(sql, null);  
            pstmt.setInt(1, workflowId);  
            pstmt.setString(2, currentValue);  
            pstmt.setString(3, currentValue);  
            rs = pstmt.executeQuery();  
            if (rs.next()) {  
                prevNodeId = rs.getInt("AD_WF_Node_ID");  
                prevValue = rs.getString("Value");  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        } finally {  
            DB.close(rs, pstmt);  
        }  
  
        if (prevNodeId <= 0) return; // 已是第一个  
  
        DB.executeUpdateEx("UPDATE AD_WF_Node SET Value=? WHERE AD_WF_Node_ID=?",  
            new Object[]{prevValue, currentRecordId}, null);  
        DB.executeUpdateEx("UPDATE AD_WF_Node SET Value=? WHERE AD_WF_Node_ID=?",  
            new Object[]{currentValue, prevNodeId}, null);  
  
        wfNodeGridTab.dataRefreshAll(true, true);  
    }  
  
	// 上移 Action
    @Override  
    public String getIconSclass() {  
		return "z-icon-Parent";
	}

	@Override
	public void decorate(Toolbarbutton toolbarButton) {
		toolbarButton.setLabel("上移");
	}

}