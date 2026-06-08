package org.libero.process;  
  
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;  
  
public class PPOrderMarkShippedProcess extends SvrProcess {  
  
	@Override
    protected void prepare() {  
		// 无参数
    }  
  
	@Override
    protected String doIt() throws Exception {  
		// 通过 T_Selection 获取多选记录
		String whereClause = "EXISTS (SELECT 1 FROM T_Selection WHERE AD_PInstance_ID=? "
				+ "AND T_Selection_ID=PP_Order_ID)";

		java.util.List<MPPOrder> orders = new Query(Env.getCtx(), MPPOrder.Table_Name, whereClause, get_TrxName())
				.setParameters(getAD_PInstance_ID()).list();
  
		int updated = 0;
		for (MPPOrder order : orders) {
			// 校验：只有 Stored 状态才能更新
			if (!"Stored".equals(order.get_ValueAsString("OrderStatus"))) {
				addLog("工单 " + order.getDocumentNo() + " 状态非已入库，跳过");
				continue;
            }  
			order.set_ValueOfColumn("OrderStatus", "Shipped");
			order.saveEx();
			updated++;
        }  
		return "@Updated@ #" + updated;
    }  
}