package com.hoifu.process;  
  
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSequence;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.X_yg_proofborr;
import com.hoifu.model.X_yg_proofinventory;

/**
 * 样稿领用流程
 * 
 * 功能： 1. 校验所选样稿是否满足领用条件（在库且非待报废） 2. 创建领用记录（yg_proofborr） 3.
 * 更新样稿台账状态（yg_proofinventory）
 * 
 * 触发方式： - 样稿台账 AD Window 工具栏"领用"按钮（网格视图多选或表单视图单条） - 样稿台账 Info Window 流程按钮（选中行写入
 * T_Selection）
 */  
@org.adempiere.base.annotation.Process  
public class YGProofBorrow extends SvrProcess {  
  
    // ==================== 流程参数字段 ====================  
  
    /** 领用时间（必填） */  
    private Timestamp p_BorrowDate  = null;  
  
    /** 领用类型：PR=生产领用 / CL=外发客户 / TL=外发检测机构（必填） */  
    private String    p_BorrowType  = null;  
  
    /** 领用人 AD_User_ID（必填） */  
    private int       p_Borrower_ID = 0;  
  
	/**
	 * 领用数量（非必填，默认为 1） AD_Process_Para 中 Mandatory = N
	 */
    private int       p_QtyBorrowed = 1;  
  
	/**
	 * 是否归还（YesNo 类型，始终有值） 默认 true = 需归还。 用户勾选 → true；未勾选 → false（不需要归还，样稿状态将更新为待废弃）
	 */
    private boolean   p_IsReturned  = true;  
  
	/** 应还日期（非必填） */
    private Timestamp p_DueDate     = null;  
  
    /** 领用工单编号（非必填） */  
    private String    p_WorkOrder   = null;  
  
    /** 领用工序，如胶印/烫金/模切（非必填） */  
	private int p_ProcessStep = 0;
  
    /** 描述/备注（非必填） */  
    private String    p_Description = null;  
  
	// ==================== 读取流程参数 ====================
  
    /**  
     * 读取流程参数  
     * 框架在执行 doIt() 之前自动调用此方法  
     */  
    @Override  
    protected void prepare() {  
        for (ProcessInfoParameter p : getParameter()) {  
            String name = p.getParameterName();  
  
			// 跳过空值参数（YesNo 类型永远不为 null，不会被跳过）
            if (p.getParameter() == null)  
                continue;  

			if (name.equals("borrowdate")) {
                // 领用时间，转为 Timestamp  
                p_BorrowDate = p.getParameterAsTimestamp();  
  
			} else if (name.equals("borrowtype")) {
                // 领用类型，List 参数取 String 值（PR / CL / TL）  
                p_BorrowType = (String) p.getParameter();  
  
            } else if (name.equals("Borrower_ID")) {  
                // 领用人，Search 参数取 int（AD_User_ID）  
                p_Borrower_ID = p.getParameterAsInt();  
  
			} else if (name.equals("qtyborrowed")) {
                // 领用数量（非必填），取 int  
                p_QtyBorrowed = p.getParameterAsInt();  
  
			} else if (name.equals("isreturned")) {
				// YesNo 类型：框架将 Boolean 存为 "Y"/"N" 字符串
				// 读取流程对话框中用户的实际勾选值
                p_IsReturned = "Y".equals(p.getParameter());  
  
            } else if (name.equals("DueDate")) {  
                // 应还日期（非必填）  
                p_DueDate = p.getParameterAsTimestamp();  
  
			} else if (name.equals("workorder")) {
                // 领用工单编号（非必填）  
                p_WorkOrder = (String) p.getParameter();  
  
			} else if (name.equals("processstep")) {
				// 领用工序（非必填）
				p_ProcessStep = p.getParameterAsInt();
  
            } else if (name.equals("Description")) {  
                // 描述/备注（非必填）  
                p_Description = (String) p.getParameter();  
  
            } else {  
                // 未知参数，记录警告（防止 AD_Process_Para 配置与代码不同步）  
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);  
            }  
        }  
    }  
  
	// ==================== 执行流程主逻辑 ====================

	/**
	 * 流程主逻辑
	 * 
	 * @return 成功消息，显示在流程结果对话框中
	 * @throws Exception 校验失败或数据库操作失败时抛出，框架自动回滚事务
	 */
    @Override  
    protected String doIt() throws Exception {  
  
		// ── 步骤 1：解析当前操作的样稿台账 ID ────────────────────────
		int inventoryId = resolveInventoryId();
  
		// ── 步骤 2：加载样稿台账主表记录 ─────────────────────────────
		X_yg_proofinventory inv = new X_yg_proofinventory(getCtx(), inventoryId, get_TrxName());
		if (inv.get_ID() == 0) {
			throw new AdempiereException("样稿记录不存在，ID=" + inventoryId);
        }  
  
		// ── 步骤 3：校验领用条件 ──────────────────────────────────────
  
		// 条件一：样稿流转状态必须为"在库"（IN）
		if (!X_yg_proofinventory.FLOWSTATUS_在库.equals(inv.getflowstatus())) {
			throw new AdempiereException("样稿【" + inv.getValue() + "】当前不在库，无法领用");
        }  
  
		// 条件二：样稿状态不能为"待报废"（C）
		if (X_yg_proofinventory.SAMPLESTATUS_待报废.equals(inv.getsamplestatus())) {
			throw new AdempiereException("样稿【" + inv.getValue() + "】状态为待报废，无法领用");
        }  
  
		// ── 步骤 4：校验必填参数 ──────────────────────────────────────
        if (p_BorrowDate == null) {  
            throw new AdempiereException("请填写领用时间");  
        }  
        if (p_BorrowType == null || p_BorrowType.isEmpty()) {  
            throw new AdempiereException("请选择领用类型");  
        }  
        if (p_Borrower_ID <= 0) {  
            throw new AdempiereException("请选择领用人");  
        }  
  
		// 领用数量非必填，若未填写或填写为 0，使用默认值 1
        if (p_QtyBorrowed <= 0) {  
            p_QtyBorrowed = 1;  
        }  
  
		// ── 步骤 5：新建领用记录（yg_proofborr） ─────────────────────
		X_yg_proofborr borr = new X_yg_proofborr(getCtx(), 0, get_TrxName());
  
		// 设置标准列
		borr.setAD_Org_ID(inv.getAD_Org_ID());
  
		// 关联样稿台账主键
		borr.setyg_proofinventory_ID(inventoryId);
  
		// 必须先设置 BorrowDate，MSequence.getDocumentNo() 才能读取到正确的年月
		borr.setborrowdate(p_BorrowDate);
  
		// ── 步骤 6：生成领用单号（使用 AD_Sequence 机制） ────────────
		// 对应序号名称：DocumentNo_yg_proofborr
		// 前缀配置：LY-@BorrowDate<yyyyMM>@-，按月重置流水号
        String documentNo = MSequence.getDocumentNo(  
				Env.getAD_Client_ID(getCtx()), // 客户 ID
				"yg_proofborr", // 表名
				get_TrxName(), // 当前事务
				borr // PO 对象，用于读取 BorrowDate 年月
        );  
		borr.setDocumentNo(documentNo);
  
		// ── 步骤 7：填写领用业务字段 ──────────────────────────────────
		borr.setborrowtype(p_BorrowType);
		borr.setBorrower_ID(p_Borrower_ID);
		borr.setqtyborrowed(new BigDecimal(p_QtyBorrowed));
  
		// 领用状态设为"领用中"（BO）
		borr.setborrowstatus(X_yg_proofborr.BORROWSTATUS_领用中);
  
		// 是否归还（Boolean 类型，对应 AD_Column Reference = Yes-No）
        // true → 数据库存 'Y'；false → 数据库存 'N'  
		borr.setisreturned(p_IsReturned);
  
        // 非必填字段：有值才写入，避免覆盖数据库列的 DEFAULT 值  
		if (p_DueDate != null)
			borr.setDueDate(p_DueDate);
		if (p_WorkOrder != null)
			borr.setworkorder(p_WorkOrder);
		if (p_ProcessStep > 0)
			borr.setprocessstep(p_ProcessStep);
		if (p_Description != null)
			borr.setDescription(p_Description);
  
        // 保存领用记录  
        // saveEx() 失败时抛出 AdempiereException，框架自动回滚整个事务  
        borr.saveEx();  
  
		// ── 步骤 8：更新样稿台账状态 ─────────────────────────────────
  
        // 流转状态更新为"被领用"（OU）  
		// 使用 set_ValueNoCheck() 绕过 IsUpdateable=N 的限制
		inv.set_ValueNoCheck("flowstatus", X_yg_proofinventory.FLOWSTATUS_被领用);
  
        // 若"是否归还"=否（不需要归还），说明样稿不会归还（如外发客户留样），  
        // 同步将样稿状态更新为"待废弃"（C）
        if (!p_IsReturned) {  
			inv.set_ValueNoCheck("samplestatus", X_yg_proofinventory.SAMPLESTATUS_待报废);
        }  
  
        // 保存样稿台账  
        // saveEx() 失败时抛出 AdempiereException，框架自动回滚整个事务  
        // （包括上面已保存的 yg_proofborr 记录，保证数据一致性）  
        inv.saveEx();  
  
        // 返回成功消息，显示在流程结果对话框中  
        return "领用成功，领用单号：" + documentNo;  
    }  

	// ==================== 工具方法 ====================

	/**
	 * 解析当前操作的样稿台账 ID
	 * 
	 * 兼容三种触发场景： 1. 普通窗口网格视图多选：Record_IDs 有值（只允许选一条） 2. 普通窗口表单视图：getRecord_ID()
	 * 返回当前记录 ID 3. 信息窗口：选中行写入 T_Selection，通过 AD_PInstance_ID 查询
	 * 
	 * @return 样稿台账主键 ID
	 * @throws Exception 未选中记录或选中多条时抛出
	 */
	private int resolveInventoryId() throws Exception {

		// ── 场景 1：普通窗口网格视图多选 ─────────────────────────────
		List<Integer> recordIds = getRecord_IDs();
		if (recordIds != null && !recordIds.isEmpty()) {
			if (recordIds.size() > 1) {
				throw new AdempiereException("只允许选择一条样稿记录，当前选中了 " + recordIds.size() + " 条");
			}
			return recordIds.get(0);
		}

		// ── 场景 2：普通窗口表单视图（单条记录） ─────────────────────
		int recordId = getRecord_ID();
		if (recordId > 0) {
			return recordId;
		}

		// ── 场景 3：信息窗口（从 T_Selection 查询选中行） ────────────
		// InfoPanel.runProcess() 在 ON_BEFORE_RUN_PROCESS 事件中将选中行
		// 写入 T_Selection 表，通过 AD_PInstance_ID 关联当前流程实例
		List<Integer> selectedIds = new ArrayList<>();
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, getAD_PInstance_ID());
			// 修正：ResultSet 放入嵌套 try-with-resources，确保一定被关闭
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					selectedIds.add(rs.getInt(1));
				}
			}
		}

		if (selectedIds.isEmpty()) {
			throw new AdempiereException("未找到选中的样稿记录，请先选择一条样稿");
		}
		if (selectedIds.size() > 1) {
			throw new AdempiereException("只允许选择一条样稿记录，当前选中了 " + selectedIds.size() + " 条");
		}

		return selectedIds.get(0);
	}
}