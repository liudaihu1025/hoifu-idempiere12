package org.libero.process;  
  
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
  
@Component(name = "org.libero.process.WFNodeMoveDownAction", service = { IAction.class })
public class WFNodeMoveDownAction implements IAction {
  
	@Override
	public void execute(Object target) {
		if (!(target instanceof ADWindow))
			return;

		ADWindow adwindow = (ADWindow) target;
		ADWindowContent content = adwindow.getADWindowContent();

		// 找到工序标签页
		IADTabpanel wfNodeTabpanel = null;
		for (int i = 0; i < content.getADTab().getTabCount(); i++) {
			IADTabpanel tabpanel = content.getADTab().getADTabpanel(i);
			if (MWFNode.Table_Name.equals(tabpanel.getTableName())) {
				wfNodeTabpanel = tabpanel;
				break;
			}
		}

		if (wfNodeTabpanel == null)
			return;

		GridTab wfNodeGridTab = wfNodeTabpanel.getGridTab();

		// 在 try 块外定义，确保后续代码都能访问
		int nodeId = wfNodeGridTab.getRecord_ID();
		if (nodeId <= 0)
			return;

		MWFNode node = new MWFNode(Env.getCtx(), nodeId, null);
		int workflowId = node.getAD_Workflow_ID();
		String currentValue = node.getValue();

		int nextNodeId = 0;
		String nextValue = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT AD_WF_Node_ID, Value FROM AD_WF_Node " + "WHERE AD_Workflow_ID=? AND IsActive='Y' "
					+ "AND CASE WHEN Value ~ '^[0-9]+$' THEN Value::INTEGER ELSE 99999 END "
					+ "> CASE WHEN ? ~ '^[0-9]+$' THEN ?::INTEGER ELSE 99999 END "
					+ "ORDER BY CASE WHEN Value ~ '^[0-9]+$' THEN Value::INTEGER ELSE 99999 END ASC LIMIT 1";
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, workflowId);
			pstmt.setString(2, currentValue);
			pstmt.setString(3, currentValue);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				nextNodeId = rs.getInt("AD_WF_Node_ID");
				nextValue = rs.getString("Value");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
		}

		if (nextNodeId <= 0)
			return;

		DB.executeUpdateEx("UPDATE AD_WF_Node SET Value=? WHERE AD_WF_Node_ID=?", new Object[] { nextValue, nodeId },
				null);
		DB.executeUpdateEx("UPDATE AD_WF_Node SET Value=? WHERE AD_WF_Node_ID=?",
				new Object[] { currentValue, nextNodeId }, null);

		wfNodeGridTab.dataRefreshAll(true, true);
	}
  
	@Override
	public String getIconSclass() {
		return "z-icon-Detail";
	}

	@Override
	public void decorate(Toolbarbutton toolbarButton) {
		toolbarButton.setLabel("下移");
	}
}