package org.libero.process;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.compiere.model.MInfoWindow;
import org.compiere.model.MTable;
import org.compiere.model.MUserRoles;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFResponsible;

/**
 * PP_Cost_Collector 工单审批
 */
@org.adempiere.base.annotation.Process
public class PPCostCollectorApprovalProcess extends SvrProcess {

	private boolean p_IsApprove = false;
	private String p_Message = "";

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("IsApprove"))
				p_IsApprove = "Y".equals(para[i].getParameter());
			else if (name.equals("Message"))
				p_Message = para[i].getParameter().toString();
			else
				log.log(Level.SEVERE, "未知参数: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		// 获取选中的pp_cost_collector记录ID
		int[] selectedIds = DB.getIDsEx(get_TrxName(),
				"SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?", getAD_PInstance_ID());

		List<Integer> recordIds = Arrays.stream(selectedIds).boxed().collect(Collectors.toList());

		if (recordIds.isEmpty()) {
			throw new AdempiereUserError("请选择要处理的单据");
		}

		// 通过 ProcessInfo 获取表 ID 与选中的记录 ID 列表
		int infoWindowId = getProcessInfo().getAD_InfoWindow_ID();
		int tableId = 0;
		if (infoWindowId > 0) {
			MInfoWindow infoWindow = new MInfoWindow(getCtx(), infoWindowId, get_TrxName());
			tableId = infoWindow.getAD_Table_ID();
		}
		if (tableId <= 0) {
			throw new AdempiereUserError("信息窗口未关联表，无法获取 Table_ID");
		}

		return processBatchApproval(tableId, recordIds);
	}

	/**
	 * 批量处理工作流活动审批
	 */
	private String processBatchApproval(int tableId, List<Integer> recordIds) {
		validateBatchPermission(tableId, recordIds);
		int successCount = 0;
		int failCount = 0;

		// 预先获取表信息，避免重复查询
		MTable table = new MTable(Env.getCtx(), tableId, null);
		String nameZhCN = table.get_Translation("Name", "zh_CN");

		for (Integer recordId : recordIds) {
			try {
				PO po = MTable.get(getCtx(), tableId).getPO(recordId, get_TrxName());

				// 获取单据单号
				String docNo = po.get_ValueAsString("DocumentNo");

				MWFActivity[] activities = MWFActivity.get(getCtx(), po.get_Table_ID(), po.get_ID(), true);
				if (activities.length == 0) {
					failCount++;
					addBufferLog(0, null, null, nameZhCN + " " + docNo + " 没有待审批的工作流活动", tableId, recordId);
					continue;
				}
				for (MWFActivity activity : activities) {
					if (!activity.isClosed()) {
						try {
							executeActivityApproval(activity);
							successCount++;

							addBufferLog(activity.getAD_WF_Activity_ID(), null, null,
									nameZhCN + " " + docNo + " 已" + (p_IsApprove ? "批准" : "拒绝"),
									activity.getAD_Table_ID(),
									activity.getRecord_ID());
						} catch (Exception e) {
							failCount++;
							String errorMsg = e.getMessage();

							// 使用 addBufferLog 记录错误信息
							addBufferLog(activity.getAD_WF_Activity_ID(), null, null,
									nameZhCN + " " + docNo + " 审批异常: " + (errorMsg != null ? errorMsg : "未知错误"),
									activity.getAD_Table_ID(), activity.getRecord_ID());
						}
					}
				}
			} catch (Exception e) {
				failCount++;

				PO po = MTable.get(getCtx(), tableId).getPO(recordId, get_TrxName());
				String docNo = po.get_ValueAsString("DocumentNo");

				// 记录错误信息
	            addBufferLog(0, null, null,  
	                    nameZhCN + " " + docNo + " 处理异常: " + e.getMessage(),  
	                    tableId, recordId);  
			}
		}

		// 返回统计信息
		return String.format("处理完成 - 成功: %d, 异常: %d", successCount, failCount);
	}

	/**
	 * 验证批量权限
	 */
	private void validateBatchPermission(int tableId, List<Integer> recordIds) {
		for (Integer recordId : recordIds) {
			try {
				PO po = MTable.get(getCtx(), tableId).getPO(recordId, get_TrxName());
				MWFActivity[] activities = MWFActivity.get(getCtx(), po.get_Table_ID(), po.get_ID(), true);
				for (MWFActivity activity : activities) {
					if (!activity.isClosed()) {
						validateActivityPermission(activity);
					}
				}
			} catch (AdempiereUserError e) {
				throw e;
			}
		}
	}

	/**
	 * 验证用户是否有权限处理该工作流活动
	 */
	private void validateActivityPermission(MWFActivity activity) {
		MWFResponsible resp = activity.getResponsible();

		// 检查是否为角色类型责任人
		if (resp.isRole()) {
			// 检查当前用户是否在该角色中
			MUserRoles[] userRoles = MUserRoles.getOfRole(getCtx(), resp.getAD_Role_ID());
			boolean userInRole = false;
			for (MUserRoles userRole : userRoles) {
				if (userRole.getAD_User_ID() == getAD_User_ID() && userRole.isActive()) {
					userInRole = true;
					break;
				}
			}
			if (!userInRole) {
				throw new AdempiereUserError("您无审批权限 - 不在工作流责任人角色中");
			}
		} else if (resp.isHuman()) {
			// 用户类型验证 - 检查是否为指定用户
			if (resp.getAD_User_ID() != getAD_User_ID()) {
				throw new AdempiereUserError("您无审批权限 - 不是指定的审批人");
			}
		} else if (resp.isInvoker()) {
			// 调用者类型 - 允许任何用户
			return;
		} else if (resp.isOrganization()) {
			// 组织类型验证 - 需要检查组织主管权限
			return;
		} else {
			throw new AdempiereUserError("不支持的工作流责任人类型: " + resp.getName());
		}
	}

	/**
	 * 执行工作流活动审批 - 直接操作活动
	 */
	private boolean executeActivityApproval(MWFActivity activity) throws Exception {
		String value = p_IsApprove ? "Y" : "N";
		String msg = " - 审批人：" + Env.getContext(getCtx(), "#AD_User_Name");
		String message;

		// 设置拒绝审批消息
		if (p_Message != null && p_Message.trim().length() > 0) {
			message = p_Message + msg;
		} else {
			message = "审批不通过" + msg;
		}

		// 直接调用，不捕获异常，让异常向上传播
		activity.setUserChoice(getAD_User_ID(), value, DisplayType.YesNo, message);

		// 检查是否有处理消息（错误）
		if (!Util.isEmpty(activity.getProcessMsg(), true)) {
			throw new Exception(activity.getProcessMsg());
		}

		return true;
	}
}