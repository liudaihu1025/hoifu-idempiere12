package org.libero.form;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;

/**
 * 生产流程卡信息窗口弹窗
 */
@org.idempiere.ui.zk.annotation.Form
public class OpenProcessCardInfo extends ADForm {

	private static final long serialVersionUID = 1L;

	//生产流程卡信息窗口UUID
	private static final String TARGET_INFO_WINDOW_UU = "1f17e0d0-6e63-41bf-bc1c-d19f2dc49cd2";

	@Override
	protected void initForm() {
		ProcessInfo pi = getProcessInfo();
		int pInstanceId = pi != null ? pi.getAD_PInstance_ID() : 0;

		List<Integer> orderIds = new ArrayList<>();
		if (pInstanceId > 0) {
			int[] selIds = DB.getIDsEx(null, "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
					pInstanceId);
			if (selIds != null) {
				for (int id : selIds)
					orderIds.add(id);
			}
		}
		MInfoWindow targetInfoWindow = new MInfoWindow(Env.getCtx(), TARGET_INFO_WINDOW_UU, null);
		int infoWindowId = targetInfoWindow.getAD_InfoWindow_ID();

		MTable table = (MTable) targetInfoWindow.getAD_Table();
		String tableName = table.getTableName();
		String keyColumn = table.isUUIDKeyTable() ? tableName + "_UU" : tableName + "_ID";

		// 必须和下面 new InfoWindow(...) 传入的 WindowNo 保持一致，
		// 这样 Env.parseContext 在解析 AD_InfoColumn.DefaultValue='@DocumentNo@' 时
		// 才能在同一个 WindowNo 上下文里查到这里设置的值。
		int infoWindowNo = SessionManager.getAppDesktop().registerWindow(new Object());

		// 根据当前选中的 PP_Order 记录，取第一条的 DocumentNo，
		// 作为弹窗信息窗口上 IsQueryCriteria='true' 字段的默认值。
		if (!orderIds.isEmpty()) {
			MPPOrder order = new MPPOrder(Env.getCtx(), orderIds.get(0), null);
			String documentNo = order.getDocumentNo();
			if (documentNo != null) {
				// 必须在 new InfoWindow(...) 之前设置，
				// 因为 DefaultValue 是在构造函数内部渲染查询条件字段时就被读取的。
				Env.setContext(Env.getCtx(), infoWindowNo, "DocumentNo", documentNo);
			}
		}

		// lookup=false -> InfoWindow 构造函数会调用 initInfoProcess()，
		// 把 AD_InfoProcess 上配置的流程渲染成按钮，这是能否跑流程的关键开关。
		InfoWindow infoWindow = new InfoWindow(infoWindowNo, tableName, keyColumn, null, false, null, infoWindowId, false) {  
		    private static final long serialVersionUID = 1L;  
		  
		    @Override  
		    public void zoom() {  
		        super.zoom();   // 保留原有跳转逻辑  
		        this.detach();  // 跳转后主动关闭这个弹窗，不再依赖 isLookup()  
		    }  
		};

		
		// 流程执行成功后自动关闭这个信息窗口弹窗。
		// InfoPanel.runProcess() 里，只有 isCloseAfterExecutionOfProcess()==true 时，
		// 流程执行成功（!m_pi.isError() 且未取消）才会 detach() 这个 InfoWindow
		// （如果有日志，会先弹 ProcessInfoDialog，等它关闭后再 detach）。
		infoWindow.setCloseAfterExecutionOfProcess(true);

		// InfoPanel.init() 在 lookup=false 时默认把 MODE_KEY 设为 MODE_EMBEDDED（会被当成新 Tab）。
		// 这里手动覆盖成 HIGHLIGHTED，并照抄 lookup
		// 分支/AbstractADWindowContent.executionButtonInfoWindow0()
		// 里同样的 80%/85% 居中尺寸/边框设置，只要这个覆盖发生在 showWindow(...) 调用之前，
		// AbstractDesktop.showWindow() 读取到的就是 HIGHLIGHTED，会走 showHighlighted()，而不是
		// showEmbedded()。
		infoWindow.setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);
		infoWindow.setBorder("normal");
		infoWindow.setClosable(true);

		int height = ClientInfo.get().desktopHeight;
		int width = ClientInfo.get().desktopWidth;
		if (width <= ClientInfo.MEDIUM_WIDTH) {
			ZKUpdateUtil.setWidth(infoWindow, "100%");
			ZKUpdateUtil.setHeight(infoWindow, "100%");
		} else {
			height = height * 85 / 100;
			width = width * 80 / 100;
			ZKUpdateUtil.setWidth(infoWindow, width + "px");
			ZKUpdateUtil.setHeight(infoWindow, height + "px");
		}
		infoWindow.setContentStyle("overflow: auto");
		infoWindow.setSizable(true);
		infoWindow.setMaximizable(true);

		infoWindow.addEventListener(DialogEvents.ON_WINDOW_CLOSE, e -> {
			Events.postEvent(new Event(DialogEvents.ON_WINDOW_CLOSE, OpenProcessCardInfo.this, null)); 
			OpenProcessCardInfo.this.detach();
		});

		SessionManager.getAppDesktop().showWindow(infoWindow, "center");
		// 打开后自动执行一次查询，等价于用户点击了"查询/刷新"按钮（ConfirmPanel.A_REFRESH）。
		infoWindow.onUserQuery();
	}

	@Override
	public Mode getWindowMode() {
		return Mode.HIGHLIGHTED;
	}

}