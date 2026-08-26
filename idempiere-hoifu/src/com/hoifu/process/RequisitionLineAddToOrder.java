package com.hoifu.process;  
  
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.service.RequisitionLinePOCreateService;  
  
@org.adempiere.base.annotation.Process  
public class RequisitionLineAddToOrder extends SvrProcess {  
  
    private int p_C_Order_ID = 0;  
    private final List<Integer> selectedLineIds = new ArrayList<>();  
  
    @Override  
    protected void prepare() {  
        for (ProcessInfoParameter para : getParameter()) {  
            String name = para.getParameterName();  
            if (name.equals("C_Order_ID"))  
                p_C_Order_ID = para.getParameterAsInt();  
        }  
        // 兼容表单视图直接从当前记录取 C_Order_ID  
        if (p_C_Order_ID == 0 && getRecord_ID() > 0  
                && getTable_ID() == MOrder.Table_ID) {  
            p_C_Order_ID = getRecord_ID();  
        }  
        loadSelectedRequisitionLines();  
    }  
  
    private void loadSelectedRequisitionLines() {  
        String sql = "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID = ?";  
        try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {  
            pstmt.setInt(1, getAD_PInstance_ID());  
            try (ResultSet rs = pstmt.executeQuery()) {  
                while (rs.next())  
                    selectedLineIds.add(rs.getInt(1));  
            }  
        } catch (SQLException e) {  
            throw new IllegalArgumentException("获取申购单明细失败: " + e.getMessage());  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        if (p_C_Order_ID == 0)  
            throw new AdempiereUserError("@FillMandatory@ @C_Order_ID@");  
        if (selectedLineIds.isEmpty())  
            throw new AdempiereUserError("请先勾选要添加的申购单明细");  
  
        MOrder order = new MOrder(getCtx(), p_C_Order_ID, get_TrxName());  
        if (order.get_ID() == 0)  
            throw new AdempiereUserError("采购订单不存在");  
        if (order.isProcessed())  
            throw new AdempiereUserError("采购订单已处理，不能添加明细");  
  
        // 已有明细行按 key 建索引，用于合并  
        Map<String, MOrderLine> existingLineMap = new HashMap<>();  
        int maxLine = 0;  
        for (MOrderLine ol : order.getLines()) {  
            existingLineMap.put(buildKey(ol.getM_Product_ID(), ol.getM_AttributeSetInstance_ID(), ol.getC_Charge_ID()), ol);  
            if (ol.getLine() > maxLine)  
                maxLine = ol.getLine();  
        }  
  
        Set<String> documentNos = new LinkedHashSet<>();
        Set<String> enoSet = new LinkedHashSet<>();

        int created = 0, merged = 0, skipped = 0;
        for (Integer lineId : selectedLineIds) {  
            MRequisitionLine reqLine = new MRequisitionLine(getCtx(), lineId, get_TrxName());  
            if (reqLine.get_ID() == 0)  
                continue;  
            if (reqLine.getC_OrderLine_ID() != 0) {  
                skipped++; // 已处理过，跳过  
                continue;  
            }  
  
            // 收集申购单号与关联单号，用于回填订单头 POReference / ENo
            MRequisition requisition = reqLine.getParent();
            if (requisition != null && requisition.getDocumentNo() != null) {
                documentNos.add(requisition.getDocumentNo());
            }
            enoSet.addAll(RequisitionLinePOCreateService.resolveDocumentNos(reqLine, get_TrxName()));

            String key = buildKey(reqLine.getM_Product_ID(), reqLine.getM_AttributeSetInstance_ID(), reqLine.getC_Charge_ID());
            MOrderLine orderLine = existingLineMap.get(key);  
  
            if (orderLine != null) {  
                // 已存在同物料/费用行 → 合并数量  
                orderLine.setQty(orderLine.getQtyOrdered().add(reqLine.getQty()));  
                orderLine.saveEx();  
                merged++;  
            } else {  
                // 新建订单行  
                maxLine += 10;  
                orderLine = new MOrderLine(order);  
                orderLine.setLine(maxLine);  
                orderLine.setAD_Org_ID(reqLine.getAD_Org_ID());  
                if (reqLine.getM_Product_ID() > 0) {  
                    MProduct product = MProduct.get(getCtx(), reqLine.getM_Product_ID());  
                    orderLine.setProduct(product);  
                    orderLine.setM_AttributeSetInstance_ID(reqLine.getM_AttributeSetInstance_ID());  
                    orderLine.setC_UOM_ID(product.getC_UOM_ID());  
                } else if (reqLine.getC_Charge_ID() > 0) {  
                    orderLine.setC_Charge_ID(reqLine.getC_Charge_ID());  
                }  
                orderLine.setQty(reqLine.getQty());
//				orderLine.setPrice(order.getM_PriceList_ID());

				BigDecimal priceActual = reqLine.getPriceActual(); // 直接拿申购单明细已算好的单价

				orderLine.setPriceActual(priceActual);

				orderLine.setPriceEntered(priceActual);

				BigDecimal lineNetAmt = reqLine.getQty().multiply(priceActual); // 或用

				orderLine.setLineNetAmt(lineNetAmt);

				// TODO标准价格
				// 查询价格表中的标准价格，赋值给自定义字段 PriceStd（逻辑与 OrderLineProductCallout 保持一致）
				int priceListId = order.getM_PriceList_ID();
				Timestamp dateOrdered = order.getDateOrdered();

				BigDecimal priceStd = Env.ZERO;
				if (reqLine.getM_Product_ID() > 0 && priceListId > 0) {
					String priceStdSql = "SELECT pp.PriceStd FROM M_ProductPrice pp "
							+ "JOIN M_PriceList_Version plv ON plv.M_PriceList_Version_ID = pp.M_PriceList_Version_ID "
							+ "JOIN M_PriceList pl ON pl.M_PriceList_ID = plv.M_PriceList_ID "
							+ "WHERE pp.M_Product_ID = ? AND pp.IsActive = 'Y' "
							+ "AND plv.IsActive = 'Y' AND pl.IsActive = 'Y' " + "AND pl.M_PriceList_ID = ? "
							+ "AND plv.ValidFrom <= ? " + "ORDER BY plv.ValidFrom DESC FETCH FIRST 1 ROWS ONLY";
					BigDecimal result = DB.getSQLValueBD(null, priceStdSql, reqLine.getM_Product_ID(), priceListId,
							dateOrdered);
					if (result != null) {
						priceStd = result;
					}
				}
				orderLine.set_CustomColumn("PriceStd", priceStd);

                orderLine.setDatePromised(order.getDatePromised());  
                orderLine.saveEx();  
                existingLineMap.put(key, orderLine);  
                created++;  
            }  
  
            // 回写关联，避免下次重复处理  
            reqLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());  
            reqLine.saveEx();  
        }  
  
        // 回填订单头：申购单号（POReference）与关联单号（ENo）
        if (!documentNos.isEmpty()) {
            order.set_CustomColumn("POReference", String.join(" | ", documentNos));
        }
        if (!enoSet.isEmpty()) {
            order.set_CustomColumn("ENo", String.join(" | ", enoSet));
        }
        order.saveEx();

        return "新增 " + created + " 行，合并 " + merged + " 行"
                + (skipped > 0 ? "，跳过已处理 " + skipped + " 行" : "");  
    }  
  
    private String buildKey(int productId, int asiId, int chargeId) {  
        return productId > 0 ? "P_" + productId + "_" + asiId : "C_" + chargeId;  
    }  
}