package com.hoifu.model;  
  
import java.math.BigDecimal;
import java.sql.ResultSet;  
import java.util.List;  
import java.util.Properties;  
  
import org.compiere.model.MInOut;  
import org.compiere.model.MInOutLine;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.Query;  
import org.compiere.util.DB;  
  
public class MLogisticsLine extends X_M_LogisticsLine {  
  
    private static final long serialVersionUID = 1L;  
  
    private static final ThreadLocal<Boolean> s_creating = ThreadLocal.withInitial(() -> Boolean.FALSE);  
    private static final ThreadLocal<Boolean> s_deleting = ThreadLocal.withInitial(() -> Boolean.FALSE);  
  
    public MLogisticsLine(Properties ctx, int M_LogisticsLine_ID, String trxName) {  
        super(ctx, M_LogisticsLine_ID, trxName);  
    }  
  
    public MLogisticsLine(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
    }  
  
    // -------------------------------------------------------------------------  
    // 生命周期方法  
    // -------------------------------------------------------------------------  
  
    @Override  
    protected boolean beforeSave(boolean newRecord) {  
        autoSetLine();  
        if (newRecord && !s_creating.get()) {  
            if (!validateNotAlreadyLinked())
                return false;  
            if (!validateNoDuplicateInOut())  
                return false;  
        }
        return true;  
    }  
  
    @Override  
    protected boolean afterSave(boolean newRecord, boolean success) {  
        if (!success) return false;  
        if (newRecord && !s_creating.get())  
            createSiblingLines();  
        return true;  
    }  
  
    @Override  
    protected boolean beforeDelete() {  
        if (!s_deleting.get())  
            deleteSiblingLines();  
        return true;  
    }  
  
    // -------------------------------------------------------------------------  
    // 私有辅助方法  
    // -------------------------------------------------------------------------  
  
    /**  
     * 检查 M_InOutLine_ID 是否已存在于任何物流单明细  
     */  
    private boolean validateNotAlreadyLinked() {  
        int count = DB.getSQLValue(get_TrxName(),  
                "SELECT COUNT(1) FROM M_LogisticsLine "  
                + "WHERE M_InOutLine_ID=? AND IsActive='Y' AND M_LogisticsLine_ID<>?",  
                getM_InOutLine_ID(), getM_LogisticsLine_ID());  
        if (count > 0) {  
            log.saveError("Error",  
                    "发货单明细 (M_InOutLine_ID=" + getM_InOutLine_ID() + ") 已关联其他物流单，不能重复添加");  
            return false;  
        }  
        return true;  
    }
    
    /** 若行号未赋值，自动取当前物流单最大行号 +10 */  
    private void autoSetLine() {  
        if (getLine() == 0) {  
            String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM M_LogisticsLine WHERE M_Logistics_ID=?";  
            setLine(DB.getSQLValue(get_TrxName(), sql, getM_Logistics_ID()));  
        }  
    }  
  
    /**  
     * 校验同一发货单的明细是否已存在于此物流单。  
     * @return true = 校验通过，false = 已存在（已调用 log.saveError）  
     */  
    private boolean validateNoDuplicateInOut() {  
        int count = DB.getSQLValue(get_TrxName(),  
                "SELECT COUNT(1) FROM M_LogisticsLine "  
                + "WHERE M_Logistics_ID=? AND M_InOut_ID=? AND IsActive='Y'",  
                getM_Logistics_ID(), getM_InOut_ID());  
        if (count > 0) {  
            log.saveError("Error",  
                    "发货单 (M_InOut_ID=" + getM_InOut_ID() + ") 的明细已存在于此物流单，不能重复添加");  
            return false;  
        }  
        return true;  
    }  
  
    /**  
     * 把同一发货单的其他所有明细行也加入当前物流单。  
     * 由 afterSave(newRecord=true) 调用，使用 s_creating 防止递归。  
     */  
    private void createSiblingLines() {  
        s_creating.set(Boolean.TRUE);  
        try {  
            MInOut inout = new MInOut(getCtx(), getM_InOut_ID(), get_TrxName());  
  
            List<MInOutLine> allLines = new Query(getCtx(), MInOutLine.Table_Name,  
                    "M_InOut_ID=? AND IsActive='Y'", get_TrxName())  
                    .setParameters(getM_InOut_ID())  
                    .setOrderBy(MInOutLine.COLUMNNAME_Line)  
                    .list();  
  
            for (MInOutLine iol : allLines) {  
                if (iol.getM_InOutLine_ID() == getM_InOutLine_ID())  
                    continue; // 跳过当前已保存的行  
  
                int exists = DB.getSQLValue(get_TrxName(),  
                        "SELECT COUNT(1) FROM M_LogisticsLine "  
                        + "WHERE M_Logistics_ID=? AND M_InOutLine_ID=?",  
                        getM_Logistics_ID(), iol.getM_InOutLine_ID());  
                if (exists > 0)  
                    continue; // 已存在则跳过  
  
                createSiblingLine(inout, iol).saveEx();  
            }  
        } finally {  
            s_creating.set(Boolean.FALSE);  
        }  
    }  
  
    /**  
     * 根据发货单头和发货明细行构造一条兄弟 MLogisticsLine（未保存）。  
     */  
    private MLogisticsLine createSiblingLine(MInOut inout, MInOutLine iol) {  
        MLogisticsLine sibling = new MLogisticsLine(getCtx(), 0, get_TrxName());  
        sibling.setAD_Org_ID(getAD_Org_ID());  
        sibling.setM_Logistics_ID(getM_Logistics_ID());  
        // Line 由 beforeSave.autoSetLine() 自动生成  
  
        sibling.setM_InOut_ID(getM_InOut_ID());  
        sibling.setM_InOutLine_ID(iol.getM_InOutLine_ID());  
        sibling.setBoxCount(1);  
        sibling.setWeight(BigDecimal.ZERO);  
        sibling.setC_BPartner_ID(inout.getC_BPartner_ID());  
        if (inout.getC_BPartner_Location_ID() > 0)  
            sibling.setC_BPartner_Location_ID(inout.getC_BPartner_Location_ID());  
  
        if (iol.getM_Product_ID() > 0)  
            sibling.setM_Product_ID(iol.getM_Product_ID());  
        if (iol.getC_UOM_ID() > 0)  
            sibling.setC_UOM_ID(iol.getC_UOM_ID());  
        sibling.setQtyDelivered(iol.getMovementQty());  
  
        int C_OrderLine_ID = iol.getC_OrderLine_ID();  
        if (C_OrderLine_ID > 0) {  
            sibling.setC_OrderLine_ID(C_OrderLine_ID);  
            MOrderLine ol = new MOrderLine(getCtx(), C_OrderLine_ID, get_TrxName());  
            if (ol.getC_Order_ID() > 0)  
                sibling.setC_Order_ID(ol.getC_Order_ID());  
        }  
  
        return sibling;  
    }  
  
    /**  
     * 删除同一物流单中同一发货单的其他所有明细行。  
     * 由 beforeDelete() 调用，使用 s_deleting 防止递归。  
     */  
    private void deleteSiblingLines() {  
        s_deleting.set(Boolean.TRUE);  
        try {  
            List<MLogisticsLine> siblings = new Query(getCtx(), Table_Name,  
                    "M_Logistics_ID=? AND M_InOut_ID=? AND M_LogisticsLine_ID<>?",  
                    get_TrxName())  
                    .setParameters(getM_Logistics_ID(), getM_InOut_ID(), getM_LogisticsLine_ID())  
                    .list();  
  
            for (MLogisticsLine sibling : siblings)  
                sibling.deleteEx(true, get_TrxName());  
        } finally {  
            s_deleting.set(Boolean.FALSE);  
        }  
    }  
}