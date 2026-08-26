package org.libero.process;

import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * 无需补数流程
 * 批量将选中的欠数工单补数状态更新为"无需补数"(NR)
 * 参照 COfficeRequisitionCloseProcess 的批量 UPDATE 模式
 */
@org.adempiere.base.annotation.Process
public class PPOrderNoRepairProcess extends SvrProcess {

    @Override
    protected void prepare() {
        // 无参数
    }

    @Override
    protected String doIt() throws Exception {
        // 批量更新：将选中的"待处理"(PD)状态工单更新为"无需补数"(NR)
        String sql = "UPDATE PP_Order SET RepairStatus='NR', Updated=now(), UpdatedBy=? "
                + "WHERE PP_Order_ID IN (SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?) "
				+ "AND RepairStatus='DP'";
        int updated = DB.executeUpdateEx(sql,
                new Object[] { getAD_User_ID(), getAD_PInstance_ID() }, get_TrxName());
        return "@Updated@ " + updated;
    }
}
