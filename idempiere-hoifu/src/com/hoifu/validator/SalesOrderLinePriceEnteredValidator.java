package com.hoifu.validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.model.MClient;
import org.compiere.model.MOrderLine;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.Env;

  /**
   * 销售订单单价必须大于0（打样订单除外）
   */
public class SalesOrderLinePriceEnteredValidator implements ModelValidator {

    private int m_AD_Client_ID = -1;

    @Override
    public void initialize(ModelValidationEngine engine, MClient client) {
        if (client != null) {
            m_AD_Client_ID = client.getAD_Client_ID();
        }
        // 注册 C_OrderLine 表的验证事件
        engine.addModelChange("C_OrderLine", this);
    }

    @Override
    public int getAD_Client_ID() {
        return m_AD_Client_ID;
    }

    @Override
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
        return null;
    }

    @Override
    public String  modelChange(PO po, int type) throws Exception {
    	if (po instanceof MOrderLine && (type == TYPE_BEFORE_NEW || type == TYPE_BEFORE_CHANGE)) {  
             MOrderLine line = (MOrderLine) po;
             return validatePriceEntered(line);
         }
         return null;
    }

    private String validatePriceEntered(MOrderLine line) {
        // 获取订单的单据类型
        int docTypeID = line.getC_Order().getC_DocTypeTarget_ID();

        String sql = "SELECT IsSOTrx, Name FROM C_DocType WHERE C_DocType_ID=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, line.get_TrxName());
            pstmt.setInt(1, docTypeID);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                boolean isSOTrx = "Y".equals(rs.getString("IsSOTrx"));
                String docTypeName = rs.getString("Name");

                if (isSOTrx) {
                    boolean isSampleOrder = docTypeName != null && (docTypeName.contains("打样订单") || docTypeName.contains("研发单"));
                    
                    // 如果不是打样订单且价格小于等于0，返回错误阻止保存
                    if (!isSampleOrder && line.getPriceEntered().compareTo(Env.ZERO) <= 0) {
                        return "销售订单单价必须大于0（打样订单、研发单除外）";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "单据校验错误！";
        }finally {
            DB.close(rs, pstmt);
        }

        return null; // 验证通过
    }
    // 其他必需的空实现方法
    @Override
    public String docValidate(PO po, int timing) {
        return null;
    }
}