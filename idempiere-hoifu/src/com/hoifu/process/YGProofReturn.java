package com.hoifu.process;  
  
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.hoifu.model.X_yg_proofborr;
import com.hoifu.model.X_yg_proofinventory;

/**
 * 样稿归还流程
 * 
 * 功能： 1. 校验所选样稿是否满足归还条件（被领用且非待报废） 2. 找到对应的领用记录（yg_proofborr），填写归还信息 3.
 * 更新样稿台账状态（yg_proofinventory）
 * 
 * 触发方式： - 样稿台账 AD Window 工具栏"归还"按钮（网格视图多选或表单视图单条） - 样稿台账 Info Window 流程按钮（选中行写入
 * T_Selection）
 */  
@org.adempiere.base.annotation.Process  
public class YGProofReturn extends SvrProcess {  
  
    // ==================== 流程参数字段 ====================  
  
	/** 归还时间（必填） */
	private Timestamp p_ReturnDate = null;
  
	/** 归还人 AD_User_ID（必填） */
	private int p_Returner_ID = 0;
  
	/**
	 * 归还数量（必填） AD_Process_Para 中 Mandatory = Y
	 */
	private int p_QtyReturned = 1;
  
	/**
	 * 归还后样品状态（必填） 用户归还时选择，归还后同步更新 yg_proofinventory.samplestatus 例如：OK=合格 / NG=不合格
	 */
	private String p_ReturnSampleStatus = null;
  
    /** 描述/备注（非必填） */  
	private String p_Description = null;
  
	// ==================== 读取流程参数 ====================
  
    /**  
     * 读取流程参数  
     * 框架在执行 doIt() 之前自动调用此方法  
     */  
    @Override  
    protected void prepare() {  
        for (ProcessInfoParameter p : getParameter()) {  
            String name = p.getParameterName();  
  
			// 统一跳过空值参数
            if (p.getParameter() == null)  
                continue;  
  
			if (name.equals("returndate")) {
				// 归还时间，转为 Timestamp
				p_ReturnDate = p.getParameterAsTimestamp();
  
			} else if (name.equals("Returner_ID")) {
				// 归还人，Search 参数取 int（AD_User_ID）
				p_Returner_ID = p.getParameterAsInt();
  
			} else if (name.equals("qtyreturned")) {
				// 归还数量（必填），取 int
				p_QtyReturned = p.getParameterAsInt();
  
			} else if (name.equals("returnsamplestatus")) {
				// 归还后样品状态，List 参数取 String 值
				p_ReturnSampleStatus = (String) p.getParameter();
  
            } else if (name.equals("Description")) {  
                // 描述/备注（非必填）  
                p_Description = (String) p.getParameter();  
  
            } else {  
                // 未知参数，记录警告（防止 AD_Process_Para 配置与代码不同步）  
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);  
            }  
        }  
    }  
  
	// ==================== 流程主逻辑 ====================

	/**
	 * 流程主逻辑 框架在 prepare() 之后调用此方法，所有数据库操作共享同一事务
	 * 
	 * @return 成功消息，显示在流程结果对话框中
	 * @throws Exception 校验失败或数据库操作失败时抛出，框架自动回滚事务
	 */
    @Override  
    protected String doIt() throws Exception {  

		// ── 步骤 1：参数校验 ─────────────────────────────────────
		if (p_ReturnDate == null)
			throw new AdempiereException("请填写归还时间");
		if (p_Returner_ID <= 0)
			throw new AdempiereException("请选择归还人");
		if (p_QtyReturned <= 0)
			throw new AdempiereException("请填写归还数量，且必须大于 0");
		if (p_ReturnSampleStatus == null || p_ReturnSampleStatus.isEmpty())
			throw new AdempiereException("请选择归还后样品状态");

		X_yg_proofborr borr;
		X_yg_proofinventory inv;

		// ── 步骤 2：根据触发来源分别解析记录 ────────────────────────
		int borrIdFromInfoWindow = resolveBorrId();

		if (borrIdFromInfoWindow > 0) {
			// ── 信息窗口：T_Selection 中存的是 yg_proofborr_id ──────
			borr = new X_yg_proofborr(getCtx(), borrIdFromInfoWindow, get_TrxName());
			if (borr.get_ID() <= 0)
				throw new AdempiereException("领用记录不存在，ID=" + borrIdFromInfoWindow);

			// 从领用记录取主表 ID
			int inventoryId = borr.getyg_proofinventory_ID();
			inv = new X_yg_proofinventory(getCtx(), inventoryId, get_TrxName());
			if (inv.get_ID() <= 0)
				throw new AdempiereException("样稿记录不存在，ID=" + inventoryId);

		} else {
			// ── 普通窗口：Record_ID / Record_IDs 存的是 yg_proofinventory_id ──
			int inventoryId = resolveInventoryId();
			inv = new X_yg_proofinventory(getCtx(), inventoryId, get_TrxName());
			if (inv.get_ID() <= 0)
				throw new AdempiereException("样稿记录不存在，ID=" + inventoryId);

			// 查找该样稿当前"领用中"的 borr 记录
			int borrId = DB.getSQLValue(get_TrxName(),
					"SELECT yg_proofborr_ID FROM yg_proofborr "
							+ "WHERE yg_proofinventory_ID = ? AND borrowstatus = ? AND IsActive = 'Y' "
							+ "ORDER BY Created DESC",
					inventoryId, X_yg_proofborr.BORROWSTATUS_领用中);

			if (borrId <= 0)
				throw new AdempiereException("未找到对应的领用记录，无法归还");

			borr = new X_yg_proofborr(getCtx(), borrId, get_TrxName());
        }  

		// ── 步骤 3：业务状态校验 ─────────────────────────────────
		if (!X_yg_proofinventory.FLOWSTATUS_被领用.equals(inv.getflowstatus()))
			throw new AdempiereException("样稿流转状态不是'被领用'，无法归还。当前状态：" + inv.getflowstatus());
		if (X_yg_proofinventory.SAMPLESTATUS_待报废.equals(inv.getsamplestatus()))
			throw new AdempiereException("样稿状态为'待报废'，无法归还");

		// 信息窗口场景下也要校验 borr 的状态（防止选中已归还的明细）
		if (!X_yg_proofborr.BORROWSTATUS_领用中.equals(borr.getborrowstatus()))
			throw new AdempiereException("该领用记录状态不是'领用中'，无法归还。当前状态：" + borr.getborrowstatus());

		// ── 步骤 4：填写归还信息 ─────────────────────────────────
		borr.setreturndate(p_ReturnDate);
		borr.setReturner_ID(p_Returner_ID);
		borr.setqtyreturned(new BigDecimal(p_QtyReturned));
		borr.setreturnsamplestatus(p_ReturnSampleStatus);
		borr.set_ValueNoCheck("borrowstatus", X_yg_proofborr.BORROWSTATUS_已归还);
		borr.set_ValueNoCheck("isreturned", true);
		if (p_Description != null)
			borr.setDescription(p_Description);
		borr.saveEx();

		// ── 步骤 5：更新样稿台账状态 ─────────────────────────────
		inv.set_ValueNoCheck("flowstatus", X_yg_proofinventory.FLOWSTATUS_在库);
		inv.set_ValueNoCheck("samplestatus", p_ReturnSampleStatus);
		inv.saveEx();

		return "";
	}

	/**
	 * 尝试从信息窗口的 T_Selection 中解析 yg_proofborr_id
	 * 
	 * @return yg_proofborr_id（信息窗口场景），或 0（非信息窗口场景）
	 */
	private int resolveBorrId() throws Exception {
		// 信息窗口判断：AD_InfoWindow_ID > 0 或 T_Selection 有记录
		// 这里直接查 T_Selection，有记录则认为是信息窗口场景
		List<Integer> selectedIds = new ArrayList<>();
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, getAD_PInstance_ID());
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					selectedIds.add(rs.getInt(1));
				}
			}
        }  

		if (selectedIds.isEmpty()) {
			return 0; // 非信息窗口场景
        }  
		if (selectedIds.size() > 1) {
			throw new AdempiereException("只允许选择一条领用记录，当前选中了 " + selectedIds.size() + " 条");
        }  
		return selectedIds.get(0); // 这是 yg_proofborr_id
	}

	/**
	 * 从普通窗口（Record_ID / Record_IDs）解析 yg_proofinventory_id
	 */
	private int resolveInventoryId() throws Exception {
		// 场景 1：网格视图多选
		List<Integer> recordIds = getRecord_IDs();
		if (recordIds != null && !recordIds.isEmpty()) {
			if (recordIds.size() > 1)
				throw new AdempiereException("只允许选择一条样稿记录，当前选中了 " + recordIds.size() + " 条");
			return recordIds.get(0);
        }  

		// 场景 2：表单视图单条
		int recordId = getRecord_ID();
		if (recordId > 0)
			return recordId;

		throw new AdempiereException("未找到选中的样稿记录，请先选择一条样稿");
	}
}