package org.libero.model;  
  
import java.math.BigDecimal;  
import java.sql.ResultSet;  
import java.sql.Timestamp;  
import java.util.Properties;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MDocType;  
import org.libero.tables.X_PP_Process_Card_Record;  
  
public class MPPProcessCardRecord extends X_PP_Process_Card_Record {  
  
	public static final String CARDSTATUS_Completed = "Completed";  
  
	public MPPProcessCardRecord(Properties ctx, int PP_Process_Card_Record_ID, String trxName) {  
		super(ctx, PP_Process_Card_Record_ID, trxName);  
	}  
  
	public MPPProcessCardRecord(Properties ctx, ResultSet rs, String trxName) {  
		super(ctx, rs, trxName);  
	}  
  
	@Override  
	protected boolean beforeSave(boolean newRecord) {  
  
		// 条件1：尚未关联报工单  
		boolean noCollector = getPP_Cost_Collector_ID() <= 0;  
		// 条件2：卡片状态=已完成  
		boolean isCompleted = CARDSTATUS_Completed.equals(getCardStatus());  
		// 条件3：正品数量 > 0  
		BigDecimal qty = getMovementQty();  
		boolean qtyOk = qty != null && qty.compareTo(BigDecimal.ZERO) > 0;  
		// 条件4：必要关联字段齐全  
		boolean fieldsOk = getPP_Order_ID() > 0  
				&& getPP_Order_Node_ID() > 0  
				&& getDateFinish() != null;  
  
		if (noCollector && isCompleted && qtyOk && fieldsOk) {  
			try {  
				createAndCompleteCostCollector();  
			} catch (Exception e) {  
				log.saveError("Error", "自动创建报工单失败: " + e.getMessage());  
				//throw new AdempiereException("自动创建报工单失败: " + e.getMessage(), e);  
			}  
		}  
  
		return true;  
	}  
  
	/**  
	 * 创建并自动完成一张生产报工单（CostCollectorType='160'）  
	 */  
	private void createAndCompleteCostCollector() {  
		MPPOrder order = new MPPOrder(getCtx(), getPP_Order_ID(), get_TrxName());  
		MPPOrderNode node = new MPPOrderNode(getCtx(), getPP_Order_Node_ID(), get_TrxName());  
  
		MPPCostCollector cc = new MPPCostCollector(getCtx(), 0, get_TrxName());  
  
		// --- 工单关联字段（取自 MPPOrder，参考 PPOrderNodeWorkReportingProcess.doOpen） ---  
		cc.setPP_Order_ID(order.getPP_Order_ID());  
		cc.setAD_Org_ID(getAD_Org_ID());  
		cc.setM_Warehouse_ID(order.getM_Warehouse_ID());  
		cc.setM_Product_ID(order.getM_Product_ID());  
		cc.setC_UOM_ID(order.getC_UOM_ID());  
		cc.setM_AttributeSetInstance_ID(order.getM_AttributeSetInstance_ID());  
		cc.setC_Activity_ID(order.getC_Activity_ID());  
  
		// --- 工序关联字段 ---  
		cc.setPP_Order_Node_ID(getPP_Order_Node_ID());  
		cc.setPP_Order_Workflow_ID(node.getPP_Order_Workflow_ID());  
		cc.set_ValueOfColumn("QtyRequiered", node.getQtyRequiered());      // 标准加工数（从工序带入）
  
		// --- 报工卡带入字段 ---  
		cc.setS_Resource_ID(getS_Resource_ID());  
		cc.setAD_User_ID(getAD_User_ID());  
		cc.set_ValueOfColumn("C_WorkTeam_ID", getC_WorkTeam_ID());  
		cc.setDescription(getDescription());  
		cc.setDateStart(getDateStart());  
		cc.setDateFinish(getDateFinish());  
		cc.setMovementQty(getMovementQty());  
		if (getScrappedQty() != null) {  
			cc.set_ValueOfColumn("ScrappedQty", getScrappedQty());  
		}  
  
		// --- 单据类型 ---  
		cc.setCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl); // "160"  
		cc.setHF_WorkReportType(MPPCostCollector.HF_WORKREPORTTYPE_Produce);  
  
		int docTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_ManufacturingCostCollector);  
		cc.setC_DocType_ID(docTypeId);  
		cc.setC_DocTypeTarget_ID(docTypeId);  
  
		// --- 单据状态：草稿 -> 保存 -> 完成 ---  
		cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);  
		cc.setDocAction(MPPCostCollector.DOCACTION_Prepare);  
		cc.setDateAcct(new Timestamp(System.currentTimeMillis()));  
  
		cc.saveEx(get_TrxName());  
  
		// 计算工时（若 DateStart/DateFinish 均存在）  
		if (getDateStart() != null && getDateFinish() != null) {  
			BigDecimal roundedHours = MPPCostCollector.updateDurationRealFromDates(getDateStart(), getDateFinish());  
			cc.setDurationReal(roundedHours);
			cc.saveEx(get_TrxName());  
		}  
  
		// 走完整文档引擎完成，创建成本明细（对照 ProductionFinishWorkReporting.updateCollectors）  
		if (!cc.processIt(MPPCostCollector.DOCACTION_Complete)) {  
			throw new AdempiereException(cc.getProcessMsg());  
		}  
		cc.saveEx(get_TrxName());  
  
		// 回写报工单 ID，避免重复创建  
		setPP_Cost_Collector_ID(cc.get_ID());  
	}  
}