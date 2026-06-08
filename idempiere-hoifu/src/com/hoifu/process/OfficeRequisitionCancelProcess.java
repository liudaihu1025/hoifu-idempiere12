package com.hoifu.process;  
  
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MInventoryLine;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

@org.adempiere.base.annotation.Process  
public class OfficeRequisitionCancelProcess extends SvrProcess {  
    private List<Integer> selectedLineIds = new ArrayList<>();  

    protected void prepare() {  
		// 加载选中的记录
		loadSelectedLines();
    }  

    protected String doIt() throws Exception {  
		int cancelledCount = 0;
          
		for (Integer inventoryLineId : selectedLineIds) {
			MInventoryLine inventoryLine = new MInventoryLine(getCtx(), inventoryLineId, get_TrxName());
              
			// 获取关联的申购单行
			int requisitionLineId = getRequisitionLineId(inventoryLineId);
			if (requisitionLineId <= 0) {
				continue; // 没有关联的申购单行，跳过
            }  
              
			MRequisitionLine requisitionLine = new MRequisitionLine(getCtx(), requisitionLineId, get_TrxName());
			MRequisition requisition = new MRequisition(getCtx(), requisitionLine.getM_Requisition_ID(), get_TrxName());
              
			// 检查申购单状态
			if (!MRequisition.DOCSTATUS_Drafted.equals(requisition.getDocStatus())) {
				throw new Exception("物料采购中，不支持取消申购");
            }  
              
			// 更新领用状态 - 基于已领数量和需求数量的完整判断
			BigDecimal quantityPicked = (BigDecimal) inventoryLine.get_Value("quantityPicked");
			if (quantityPicked == null) {
				quantityPicked = Env.ZERO;
			}

			// 获取需求数量
			BigDecimal demandQty = (BigDecimal) inventoryLine.get_Value("QtyDemand");
			if (demandQty == null) {
				demandQty = Env.ZERO;
			}
			// 获取申购单状态
			String requisitionStatus = requisition.getDocStatus();

			// 根据已领数量判断状态
			if (quantityPicked.compareTo(demandQty) >= 0) {
				// 已领数量 >= 需求数量，设为已领用
				inventoryLine.set_CustomColumn("ClaimStatus", "YL");
			} else if (quantityPicked.compareTo(Env.ZERO) > 0 && quantityPicked.compareTo(demandQty) < 0) {
				// 0 < 已领数量 < 需求数量，根据申购单状态判断
				if ("DR".equals(requisitionStatus)) {

					inventoryLine.set_CustomColumn("ClaimStatus", "PR");
				} else {

					inventoryLine.set_CustomColumn("ClaimStatus", "YL");
				}
			} else {
				// 已领数量 = 0，设为未领用
				inventoryLine.set_CustomColumn("ClaimStatus", "WL");
			}
			
			inventoryLine.saveEx();
              
			// 删除申购单行
			requisitionLine.deleteEx(true);
              
			// 检查是否是最后一条申购单行
			if (isLastRequisitionLine(requisition.getM_Requisition_ID())) {
				requisition.deleteEx(true);
            }  
              
			cancelledCount++;
        }  
          
		return "成功取消 " + cancelledCount + " 个申购";
	}

	private int getRequisitionLineId(int inventoryLineId) {
		String sql = "SELECT M_RequisitionLine_ID FROM M_RequisitionLine WHERE M_InventoryLine_ID = ?";
		return DB.getSQLValueEx(get_TrxName(), sql, inventoryLineId);
	}

	private boolean isLastRequisitionLine(int requisitionId) {
		String sql = "SELECT COUNT(*) FROM M_RequisitionLine WHERE M_Requisition_ID = ?";
		int count = DB.getSQLValueEx(get_TrxName(), sql, requisitionId);
		return count == 0;
	}
      
	private void loadSelectedLines() {
		String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getAD_PInstance_ID());
			rs = pstmt.executeQuery();

			while (rs.next()) {
				selectedLineIds.add(rs.getInt(1));
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException("获取领用单明细失败");
		} finally {
			DB.close(rs, pstmt);
		}
	}
}