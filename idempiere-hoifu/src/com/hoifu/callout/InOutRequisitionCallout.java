package com.hoifu.callout;  
  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.MRMA;  
import org.compiere.util.Env;  
  
import com.hoifu.model.MInOutRequisition;  
  
/**  
 * M_InOut_Requisition（出入库申请单表头）Callout。  
 *  
 * <p>核心职责：以 InOutType 为总开关，联动/清理三组互斥的来源字段  
 * （C_Order_ID / M_RMA_ID / C_OfficeRequisition_ID），并回填 IsSOTrx。</p>  
 */  
@Callout(tableName = "M_InOut_Requisition", columnName = {  
        "InOutType", "C_Order_ID", "M_RMA_ID", "C_OfficeRequisition_ID" })  
public class InOutRequisitionCallout implements IColumnCallout {  
  
    @Override  
    public String start(Properties ctx, int windowNo, GridTab mTab, GridField mField,  
            Object value, Object oldValue) {  
  
        String columnName = mField.getColumnName();  
  
        if ("InOutType".equals(columnName)) {  
            return onInOutTypeChanged(ctx, windowNo, mTab, value);  
        } else if ("C_Order_ID".equals(columnName)) {  
            return onOrderChanged(ctx, windowNo, mTab, value);  
        } else if ("M_RMA_ID".equals(columnName)) {  
            return onRMAChanged(ctx, windowNo, mTab, value);  
        } else if ("C_OfficeRequisition_ID".equals(columnName)) {  
            return onOfficeRequisitionChanged(ctx, windowNo, mTab, value);  
        }  
        return "";  
    }  
  
    /**  
     * InOutType 切换：  
     * 1) 按分组回填 IsSOTrx（RMA_IN=销售退货入库=Y，RMA_OUT=采购退货出库=N，  
     *    ORDER_OUT=销售出库=Y，ORDER_IN=采购入库=N，其余内部单据默认N，具体按你业务口径再调）；  
     * 2) 清空不属于当前分组的来源字段，避免残留脏引用。  
     */  
    private String onInOutTypeChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = value == null ? null : value.toString();  
        if (inOutType == null || inOutType.trim().isEmpty())  
            return "";  
  
        boolean isOrderGroup = MInOutRequisition.IN_ORDER_IN.equals(inOutType)  
                || MInOutRequisition.OUT_ORDER_OUT.equals(inOutType);  
        boolean isRmaGroup = MInOutRequisition.IN_RMA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_RMA_OUT.equals(inOutType);  
        boolean isOfficeReqGroup = MInOutRequisition.IN_INV_IU_IN.equals(inOutType)  
                || MInOutRequisition.OUT_INV_IU_OUT.equals(inOutType)  
                || MInOutRequisition.IN_OTHER_IN.equals(inOutType)  
                || MInOutRequisition.OUT_OTHER_OUT.equals(inOutType);  
        boolean isPPGroup = MInOutRequisition.IN_PP_MATERIA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_PP_MATERIA_OUT.equals(inOutType)  
                || MInOutRequisition.IN_PP_IN.equals(inOutType)  
                || MInOutRequisition.IN_SUBCONTRAC_IN.equals(inOutType);  
  
        // 1) IsSOTrx  
        boolean isSOTrx = MInOutRequisition.IN_RMA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_ORDER_OUT.equals(inOutType);  
        mTab.setValue("IsSOTrx", isSOTrx);  
  
        // 2) 三选一：清空不属于当前分组的来源字段  
        if (!isOrderGroup)  
            mTab.setValue("C_Order_ID", null);  
        if (!isRmaGroup)  
            mTab.setValue("M_RMA_ID", null);  
        if (!isOfficeReqGroup)  
            mTab.setValue("C_OfficeRequisition_ID", null);  
        // PP 场景（PP_MATERIA_IN/OUT、PP_IN、SUBCONTRAC_IN）不使用表头来源字段，  
        // 工单信息落在明细行 PP_Order_ID 上，这里无需处理。  
  
        return "";  
    }  
  
    /** 仅 ORDER_IN / ORDER_OUT 时生效：选中订单后回填仓库/业务伙伴信息 */  
    private String onOrderChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = (String) mTab.getValue("InOutType");  
        boolean isOrderGroup = MInOutRequisition.IN_ORDER_IN.equals(inOutType)  
                || MInOutRequisition.OUT_ORDER_OUT.equals(inOutType);  
        if (!isOrderGroup)  
            return "";  
  
        Integer orderId = (Integer) value;  
        if (orderId == null || orderId <= 0)  
            return "";  
  
        org.compiere.model.MOrder order = new org.compiere.model.MOrder(ctx, orderId, null);  
        mTab.setValue("C_BPartner_ID", order.getC_BPartner_ID());  
        mTab.setValue("C_BPartner_Location_ID", order.getC_BPartner_Location_ID());  
        mTab.setValue("M_Warehouse_ID", order.getM_Warehouse_ID());  
        return "";  
    }  
  
    /** 仅 RMA_IN / RMA_OUT 时生效：选中 RMA 后回填业务伙伴信息 */  
    private String onRMAChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = (String) mTab.getValue("InOutType");  
        boolean isRmaGroup = MInOutRequisition.IN_RMA_IN.equals(inOutType)  
                || MInOutRequisition.OUT_RMA_OUT.equals(inOutType);  
        if (!isRmaGroup)  
            return "";  
  
        Integer rmaId = (Integer) value;  
        if (rmaId == null || rmaId <= 0)  
            return "";  
  
        MRMA rma = new MRMA(ctx, rmaId, null);  
        mTab.setValue("C_BPartner_ID", rma.getC_BPartner_ID());  
        // RMA_IN=销售退货入库=Y，RMA_OUT=采购退货出库=N，与 RMA 自身 isSOTrx 保持一致  
        mTab.setValue("IsSOTrx", rma.isSOTrx());  
        return "";  
    }  
  
    /** 仅 INV_IU_IN/OUT、OTHER_IN/OUT 时生效：选中办公申请单后回填组织/仓库/活动 */  
    private String onOfficeRequisitionChanged(Properties ctx, int windowNo, GridTab mTab, Object value) {  
        String inOutType = (String) mTab.getValue("InOutType");  
        boolean isOfficeReqGroup = MInOutRequisition.IN_INV_IU_IN.equals(inOutType)  
                || MInOutRequisition.OUT_INV_IU_OUT.equals(inOutType)  
                || MInOutRequisition.IN_OTHER_IN.equals(inOutType)  
                || MInOutRequisition.OUT_OTHER_OUT.equals(inOutType);  
        if (!isOfficeReqGroup)  
            return "";  
  
        Integer officeReqId = (Integer) value;  
        if (officeReqId == null || officeReqId <= 0)  
            return "";  
  
        org.compiere.model.PO officeReq = org.compiere.model.MTable  
                .get(ctx, "C_OfficeRequisition")  
                .getPO(officeReqId, null);  
        if (officeReq != null) {  
            Object orgId = officeReq.get_Value("AD_Org_ID");  
            Object whId = officeReq.get_Value("M_Warehouse_ID");  
            Object activityId = officeReq.get_Value("C_Activity_ID");  
            if (orgId != null)  
                mTab.setValue("AD_Org_ID", orgId);  
            if (whId != null)  
                mTab.setValue("M_Warehouse_ID", whId);  
            if (activityId != null)  
                mTab.setValue("C_Activity_ID", activityId);  
        }  
        return "";  
    }  
}