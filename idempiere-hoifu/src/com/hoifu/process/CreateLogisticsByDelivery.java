package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;

import org.adempiere.base.annotation.Parameter;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;

import com.hoifu.model.MLogistics;
import com.hoifu.model.MLogisticsLine;

@org.adempiere.base.annotation.Process
public class CreateLogisticsByDelivery extends SvrProcess {

    //货币：CNY
    private static final String C_CURRENCY_UU = "979d0818-d020-4c86-a23f-4e8ad93d80ed";
    //发货物流单
    private static final String C_DOCTYPE_UU = "8fbd0dd7-1c1a-4899-bc48-16a7565f3d3b";

    @Parameter(name = MLogistics.COLUMNNAME_M_Shipper_ID)
    private int p_M_Shipper_ID;

    @Override
    protected void prepare() {
        // @Parameter 自动注入，无需手动解析
    }

    @Override
    protected String doIt() throws Exception {

        // 1. 校验必填参数
        if (p_M_Shipper_ID == 0)
            throw new IllegalArgumentException("请选择承运人 M_Shipper_ID");

        // 2. 通过 UUID 查出固定的 C_Currency_ID 和 C_DocType_ID
        int C_Currency_ID = DB.getSQLValue(get_TrxName(),
                "SELECT C_Currency_ID FROM C_Currency WHERE C_Currency_UU=?",
                C_CURRENCY_UU);
        if (C_Currency_ID <= 0)
            throw new IllegalArgumentException("找不到货币记录，UUID=" + C_CURRENCY_UU);

        int C_DocType_ID = DB.getSQLValue(get_TrxName(),
                "SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_UU=?",
                C_DOCTYPE_UU);
        if (C_DocType_ID <= 0)
            throw new IllegalArgumentException("找不到单据类型记录，UUID=" + C_DOCTYPE_UU);

        // 3. 从 T_Selection 取选中的 M_InOutLine_ID，
        //    JOIN 找出这些行所属的所有不重复 M_InOut_ID
        Set<Integer> inOutIds = new LinkedHashSet<>();
        String sql = "SELECT DISTINCT iol.M_InOut_ID "
                + "FROM M_InOutLine iol "
                + "INNER JOIN T_Selection ts ON ts.T_Selection_ID = iol.M_InOutLine_ID "
                + "WHERE ts.AD_PInstance_ID = ?";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
            pstmt.setInt(1, getAD_PInstance_ID());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next())
                    inOutIds.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("查询发货单失败：" + e.getLocalizedMessage(), e);
        }

        if (inOutIds.isEmpty())
            return "未选择任何发货明细记录";

        // 4. 预加载所有涉及的 MInOut，并校验组织是否一致
        //    组织字段应来源于发货单本身，而不是当前会话上下文
        java.util.Map<Integer, MInOut> inOutMap = new java.util.LinkedHashMap<>();
        int commonOrgId = -1;
        for (int M_InOut_ID : inOutIds) {
            MInOut inout = new MInOut(getCtx(), M_InOut_ID, get_TrxName());
            inOutMap.put(M_InOut_ID, inout);

            if (commonOrgId == -1) {
                commonOrgId = inout.getAD_Org_ID();
            } else if (commonOrgId != inout.getAD_Org_ID()) {
                throw new IllegalArgumentException(
                        "所选发货单分属不同组织，请分开处理（发货单 " + inout.getDocumentNo() + " 的组织与其他发货单不一致）");
            }
        }

        // 5. 创建物流单头，组织取自来源发货单
        MLogistics logistics = new MLogistics(getCtx(), 0, get_TrxName());
        logistics.setAD_Org_ID(commonOrgId);
        logistics.setC_DocType_ID(C_DocType_ID);
        logistics.setDateTrx(new Timestamp(System.currentTimeMillis()));
        logistics.setM_Shipper_ID(p_M_Shipper_ID);
        logistics.setC_Currency_ID(C_Currency_ID);
        logistics.setBoxCount(1);
        logistics.setFreightCharges(BigDecimal.ZERO);
        logistics.setWeight(BigDecimal.ZERO);
        logistics.setSurcharges(BigDecimal.ZERO);
        logistics.setLogisticsStatus("Created");
        logistics.saveEx();

        int logisticsId = logistics.get_ID();

        // 6. 遍历每张发货单，只保存第一行，createSiblingLines() 自动处理其余行
        int createdLineCount = 0;
        for (int M_InOut_ID : inOutIds) {

            MInOut inout = inOutMap.get(M_InOut_ID);

            // 只取第一行，afterSave 中 createSiblingLines() 会自动添加其余行
            MInOutLine firstLine = new Query(getCtx(), MInOutLine.Table_Name,
                    "M_InOut_ID=? AND IsActive='Y'", get_TrxName())
                    .setParameters(M_InOut_ID)
                    .setOrderBy(MInOutLine.COLUMNNAME_Line)
                    .first();

            if (firstLine == null)
                continue;

            MLogisticsLine logLine = new MLogisticsLine(getCtx(), 0, get_TrxName());
            logLine.setAD_Org_ID(inout.getAD_Org_ID());
            logLine.setM_Logistics_ID(logisticsId);
            // Line 由 beforeSave.autoSetLine() 自动生成，无需手动设置

            logLine.setM_InOut_ID(M_InOut_ID);
            logLine.setM_InOutLine_ID(firstLine.getM_InOutLine_ID());
            logLine.setBoxCount(1);
            logLine.setWeight(BigDecimal.ZERO);

            logLine.setC_BPartner_ID(inout.getC_BPartner_ID());
            if (inout.getC_BPartner_Location_ID() > 0)
                logLine.setC_BPartner_Location_ID(inout.getC_BPartner_Location_ID());

            if (firstLine.getM_Product_ID() > 0)
                logLine.setM_Product_ID(firstLine.getM_Product_ID());
            if (firstLine.getC_UOM_ID() > 0)
                logLine.setC_UOM_ID(firstLine.getC_UOM_ID());
            logLine.setQtyDelivered(firstLine.getMovementQty());

            int C_OrderLine_ID = firstLine.getC_OrderLine_ID();
            if (C_OrderLine_ID > 0) {
                logLine.setC_OrderLine_ID(C_OrderLine_ID);
                MOrderLine ol = new MOrderLine(getCtx(), C_OrderLine_ID, get_TrxName());
                if (ol.getC_Order_ID() > 0)
                    logLine.setC_Order_ID(ol.getC_Order_ID());
            }

            logLine.saveEx(); // afterSave → createSiblingLines() 自动添加其余行
            createdLineCount++;
        }

        // 6.1 若一行都没有成功创建，回滚生成的空物流单头，避免产生脏数据
        if (createdLineCount == 0) {
            logistics.deleteEx(true);
            throw new IllegalArgumentException("所选发货单均无有效明细行，未生成物流单");
        }

        // 7. 记录日志
        String msg = Msg.parseTranslation(getCtx(), "@M_Logistics_ID@: " + logistics.getDocumentNo());
        addLog(logisticsId, null, null, msg, logistics.get_Table_ID(), logisticsId);

        int totalLines = DB.getSQLValue(get_TrxName(),
                "SELECT COUNT(1) FROM M_LogisticsLine WHERE M_Logistics_ID=?", logisticsId);
        return "@Created@ " + logistics.getDocumentNo() + "，共 " + totalLines + " 行";
    }
}