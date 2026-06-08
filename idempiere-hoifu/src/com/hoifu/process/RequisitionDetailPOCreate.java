package com.hoifu.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MWarehouse;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * 1.用户勾选需要创建的申购单明细；
 * 2.点击“创建采购订单”按钮，弹出一个对话框，让用户选择供应商、用途，然后提交，创建采购单
 * 3.相同商品合并，采购单按仓库分组
 */

@org.adempiere.base.annotation.Process
public class RequisitionDetailPOCreate extends SvrProcess{
	
	private int p_C_BPartner_ID = 0;  
	private String p_Purpose = null;  
	private List<Integer> selectedLineIds = new ArrayList<>();  
	  
	@Override  
	protected void prepare() {  
	    ProcessInfoParameter[] para = getParameter();  
	    for (int i = 0; i < para.length; i++) {  
	        String name = para[i].getParameterName();  
	        if (name.equals("C_BPartner_ID"))  
	            p_C_BPartner_ID = para[i].getParameterAsInt();  
	        else if (name.equals("Purpose"))  
	            p_Purpose = para[i].getParameterAsString();  
	    }  
	      

	}  
	  
	private void loadSelectedRequisitionLines() {  
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
	        throw new IllegalArgumentException("获取申购单明细失败");  
	    } finally {  
	        DB.close(rs, pstmt);  
	    }  
	}  

	@Override  
	protected String doIt() throws Exception {  
	    // 验证参数  
	    if (p_C_BPartner_ID == 0) {  
	        throw new AdempiereUserError("@FillMandatory@ @C_BPartner_ID@");  
	    }  
	      
	    // 从T_Selection表获取选中的申购单明细  
	    loadSelectedRequisitionLines();  
	    if (selectedLineIds.isEmpty()) {  
	        throw new AdempiereUserError("@NoSelection@");  
	    }  
	  
	    // 获取供应商信息  
	    MBPartner bpartner = MBPartner.get(getCtx(), p_C_BPartner_ID);  
	      
	    // 按仓库分组申购单明细  
	    HashMap<Integer, List<MRequisitionLine>> warehouseGroups = new HashMap<>();  
	      
	    for (Integer lineId : selectedLineIds) {  
	        MRequisitionLine reqLine = new MRequisitionLine(getCtx(), lineId, get_TrxName());  
	          
	        // 验证明细行状态  
	        if (reqLine.getC_OrderLine_ID() != 0) {  
	            log.warning("跳过已处理的明细行: " + lineId);  
	            continue;  
	        }  
	          
	        // 获取申购单主表的仓库ID  
	        MRequisition requisition = reqLine.getParent();  
	        int warehouseId = requisition.getM_Warehouse_ID();  
	          
	        // 按仓库分组  
	        if (!warehouseGroups.containsKey(warehouseId)) {  
	            warehouseGroups.put(warehouseId, new ArrayList<>());  
	        }  
	        warehouseGroups.get(warehouseId).add(reqLine);  
	    }  
	      
	    // 为每个仓库创建一个采购订单  
	    List<String> createdOrders = new ArrayList<>();  
	      
	    for (Map.Entry<Integer, List<MRequisitionLine>> entry : warehouseGroups.entrySet()) {  
	        int warehouseId = entry.getKey();  
	        List<MRequisitionLine> lines = entry.getValue();  
	          
	        // 获取仓库以获取正确的组织  
	        MWarehouse warehouse = MWarehouse.get(getCtx(), warehouseId);  
	          
	        // 创建采购订单  
	        MOrder order = new MOrder(getCtx(), 0, get_TrxName());  
	        order.setIsSOTrx(false);  
	        order.setC_DocTypeTarget_ID(1000721);  
	        order.setBPartner(bpartner);  
	        order.setAD_Org_ID(warehouse.getAD_Org_ID());  
	        order.setSalesRep_ID(getAD_User_ID());  
	        order.setM_Warehouse_ID(warehouseId);  
	          
	     // 设置DeliveryViaRule为D  (发货)
	        order.setDeliveryViaRule("D");
	        // 设置用途到描述字段  
	        if (p_Purpose != null && !p_Purpose.isEmpty()) {  
	        	order.set_CustomColumn("Purpose", p_Purpose);  
	        }  
	          
	        // 设置订单日期  
	        // 承诺日期为当前时间加7天  
	        Calendar cal = Calendar.getInstance();  
	        cal.setTimeInMillis(System.currentTimeMillis());  
	        cal.add(Calendar.DAY_OF_MONTH, 7);  
	        order.setDateOrdered(new Timestamp(System.currentTimeMillis()));  
	        order.setDatePromised(new Timestamp(cal.getTimeInMillis()));
	        
	        // 获取去重后申购单的DocumentNo并设置到OReference  
	        if (!lines.isEmpty()) {  
	            java.util.Set<String> documentNos = new java.util.HashSet<>();  
	            for (MRequisitionLine reqLine : lines) {  
	                MRequisition requisition = reqLine.getParent();  
	                if (requisition != null && requisition.getDocumentNo() != null) {  
	                    documentNos.add(requisition.getDocumentNo());  
	                }  
	            }  
	              
	            if (!documentNos.isEmpty()) {  
	                String poReference = String.join(" | ", documentNos);  
	                order.set_CustomColumn("POReference", poReference);  
	            }  
	        }

	     // 获取所有M_RequisitionLine中的ENo并分割去重后设置到C_Order.ENo  
	        if (!lines.isEmpty()) {  
	            java.util.Set<String> enoSet = new java.util.HashSet<>();  
	            for (MRequisitionLine reqLine : lines) {  
	                String enoValue = (String) reqLine.get_Value("ENo");  
	                if (enoValue != null && !enoValue.trim().isEmpty()) {  
	                    // 按"、"分割工程单号  
	                    String[] enoArray = enoValue.split("、");  
	                    for (String eno : enoArray) {  
	                        String trimmedEno = eno.trim();  
	                        if (!trimmedEno.isEmpty()) {  
	                            enoSet.add(trimmedEno);  
	                        }  
	                    }  
	                }  
	            }  
	              
	            if (!enoSet.isEmpty()) {  
	                String enoString = String.join(" | ", enoSet);  
	                order.set_CustomColumn("ENo", enoString);  
	            }  
	        }
	        order.saveEx();  
	  
	        // 按产品分组合并申购单明细  
	        Map<String, List<MRequisitionLine>> productGroups = new HashMap<>();  
	          
	        for (MRequisitionLine reqLine : lines) {  
	            String productKey;  
	            if (reqLine.getM_Product_ID() > 0) {  
	                // 产品行 - 使用产品ID和属性实例ID作为键  
	                productKey = "P_" + reqLine.getM_Product_ID() + "_" + reqLine.getM_AttributeSetInstance_ID();  
	            } else {  
	                // 费用行 - 使用费用ID作为键  
	                productKey = "C_" + reqLine.getC_Charge_ID();  
	            }  
	              
	            if (!productGroups.containsKey(productKey)) {  
	                productGroups.put(productKey, new ArrayList<>());  
	            }  
	            productGroups.get(productKey).add(reqLine);  
	        }  
	  
	        int processedLines = 0;  
	          
	        // 为每个产品组创建一个采购订单行  
	        for (Map.Entry<String, List<MRequisitionLine>> productEntry : productGroups.entrySet()) {  
	            List<MRequisitionLine> productLines = productEntry.getValue();  
	            MRequisitionLine firstLine = productLines.get(0);  
	              
	            // 创建采购订单行  
	            MOrderLine orderLine = new MOrderLine(order);  
	            orderLine.setLine((processedLines + 1) * 10);  
	            orderLine.setAD_Org_ID(firstLine.getAD_Org_ID());  
	              
	            if (firstLine.getM_Product_ID() > 0) {  
	                // 产品行  
	                MProduct product = MProduct.get(getCtx(), firstLine.getM_Product_ID());  
	                orderLine.setProduct(product);  
	                orderLine.setM_AttributeSetInstance_ID(firstLine.getM_AttributeSetInstance_ID());  
	                orderLine.setC_UOM_ID(product.getC_UOM_ID());  
	            } else if (firstLine.getC_Charge_ID() > 0) {  
	                // 费用行  
	                orderLine.setC_Charge_ID(firstLine.getC_Charge_ID());  
	            }  
	              
	            // 合并数量  
	            BigDecimal totalQty = Env.ZERO;  
	            BigDecimal totalPrice = Env.ZERO;  
	            Timestamp earliestDate = null;  
	              
	            for (MRequisitionLine reqLine : productLines) {  
	                totalQty = totalQty.add(reqLine.getQty());  
	                totalPrice = totalPrice.add(reqLine.getPriceActual().multiply(reqLine.getQty()));  
	                  
	                if (earliestDate == null || reqLine.getDateRequired().before(earliestDate)) {  
	                    earliestDate = reqLine.getDateRequired();  
	                }  
	            }  
	              
	            // 设置合并后的数量和价格  
	            orderLine.setQty(totalQty);  
//	            if (totalQty.compareTo(Env.ZERO) > 0) {  
//	            	 BigDecimal avgPrice = totalPrice.divide(totalQty, 6, RoundingMode.HALF_UP);  
//	            	    orderLine.setPriceActual(avgPrice);  
//	            	    orderLine.setPriceEntered(avgPrice);  // 设置PriceEntered与PriceActual相同  
//	            } 
	            orderLine.setPrice(order.getM_PriceList_ID());
	            orderLine.setDatePromised(earliestDate);  
	              
	            // 保存订单行  
	            orderLine.saveEx();  
	              
	            // 更新所有申购单明细行的关联  
	            for (MRequisitionLine reqLine : productLines) {  
	                reqLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());  
	                reqLine.saveEx();  
	            }  
	              
	            processedLines++;  
	        }  
	          
	        // 记录日志 - 保持草稿状态，不完成订单  
	        addBufferLog(0, null, order.getGrandTotal(),   
	                     Msg.parseTranslation(getCtx(), "@Created@ @C_Order_ID@ ") + order.getDocumentNo() + " (草稿)",  
	                     MOrder.Table_ID, order.getC_Order_ID());  
	          
	        createdOrders.add(order.getDocumentNo());  
	    }  
	      
	    return "@Created@ " + String.join(", ", createdOrders) + " (草稿状态)";  
	}
}
