package com.hoifu.validator;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class ReconciliationStatusValidator implements ModelValidator {  
    
    private static final CLogger log = CLogger.getCLogger(ReconciliationStatusValidator.class);  
    private int m_AD_Client_ID = -1;  
      
    @Override  
    public void initialize(ModelValidationEngine engine, MClient client) {        
    	if(client != null) {
    		m_AD_Client_ID = client.getAD_Client_ID();
    	}
    	// 注册模型变更监听器（直接状态修改）  
    	engine.addModelChange("C_Reconciliation", this);  
    }  
      
    @Override  
    public int getAD_Client_ID() {  
        return m_AD_Client_ID;  
    }  
      
    @Override  
    public String modelChange(PO po, int type) throws Exception {  
    	if (type == ModelValidator.TYPE_AFTER_CHANGE) {  
            if (po.get_TableName().equals("C_Reconciliation")) {  
                String docStatus = (String) po.get_Value("DocStatus");  
                String oldDocStatus = (String) po.get_ValueOld("DocStatus");  
                //判定设置对账单单据状态是否为完成
                if (!"CO".equals(oldDocStatus) && "CO".equals(docStatus)) {  
                    // 验证明细行  
                    String sql = "SELECT COUNT(*) FROM C_ReconciliationLine WHERE C_Reconciliation_ID = ?";  
                    int lineCount = DB.getSQLValueEx(po.get_TrxName(), sql, po.get_ID());  
                      
                    if (lineCount == 0) {  
                        throw new AdempiereException("@NoLines@");  
                    }  
                    return processReconciliationCompletion(po);  
                }  
            }  
        }  
        return null;  
    }  
      
    @Override  
    public String docValidate(PO po, int timing) { 
        return null; 
    }  
      
    private String processReconciliationCompletion(PO po) {  
        try {  
            // 获取对账单明细  
			String sql = "SELECT C_ReconciliationLine_ID, M_InOutLine_ID, C_OrderLine_ID, "
					+ "MovementQty, QtyToReconcile, PriceToReconcile, PriceActual "
					+ "FROM C_ReconciliationLine WHERE C_Reconciliation_ID = ?";
        	
            PreparedStatement pstmt = null;  
            ResultSet rs = null;  
              
            try {  
                pstmt = DB.prepareStatement(sql, po.get_TrxName());  
                pstmt.setInt(1, po.get_ID());  
                rs = pstmt.executeQuery();  
                  
                while (rs.next()) {   
					int inOutLineID = rs.getInt("M_InOutLine_ID");
					int orderLineID = rs.getInt("C_OrderLine_ID");
					BigDecimal qtyToReconcile = rs.getBigDecimal("QtyToReconcile");
					BigDecimal movementQty = rs.getBigDecimal("MovementQty");
					BigDecimal priceToReconcile = rs.getBigDecimal("PriceToReconcile");
					BigDecimal priceActual = rs.getBigDecimal("PriceActual");
                      
                    // 更新收发明细状态  
                    if (inOutLineID > 0) {  
						updateInOutLineStatus(inOutLineID, qtyToReconcile, movementQty, priceToReconcile, priceActual,
								po.get_TrxName());
                    }  
                      
                    // 更新采购明细已对账数量  
                    if (orderLineID > 0) {  
                        updateOrderLineReconciledQty(orderLineID, qtyToReconcile, po.get_TrxName());  
                    }  
                }  
            } finally {  
                DB.close(rs, pstmt);  
            }  
              
        } catch (Exception e) {  
            log.log(Level.SEVERE, "处理对账单完成时出错", e);  
            return "处理对账单完成时出错: " + e.getMessage();  
        }  
          
        return null;  
    }  
      
	private void updateInOutLineStatus(int m_InOutLine_ID, BigDecimal qtyToReconcile, BigDecimal movementQty,
			BigDecimal priceToReconcile, BigDecimal priceActual, String trxName) {

		// 数量差异：qtytoreconcile != movementqty
		boolean hasQtyDiff = qtyToReconcile == null || movementQty == null
				|| qtyToReconcile.compareTo(movementQty) != 0;
		// 价格差异：pricetoreconcile != priceactual
		boolean hasPriceDiff = priceToReconcile == null || priceActual == null
				|| priceToReconcile.compareTo(priceActual) != 0;

		if (!hasQtyDiff && !hasPriceDiff) {
			DB.executeUpdateEx("UPDATE M_InOutLine SET ReconciliationStatus = 'CO' WHERE M_InOutLine_ID = ?",
					new Object[] { m_InOutLine_ID }, trxName);
		}
    }  
      
	private void updateOrderLineReconciledQty(int c_OrderLine_ID, BigDecimal qtyToReconcile, String trxName) {
    	
        String sql = "UPDATE C_OrderLine SET QtyReconciled = COALESCE(QtyReconciled, 0) + ? WHERE C_OrderLine_ID = ?";  
          
        try {  
			DB.executeUpdateEx(sql, new Object[] { qtyToReconcile, c_OrderLine_ID }, trxName);
        } catch (Exception e) {  
            log.log(Level.SEVERE, "更新采购明细已对账数量失败", e);  
        }  
    }  
      
    @Override  
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {  
        return null;  
    }  
}