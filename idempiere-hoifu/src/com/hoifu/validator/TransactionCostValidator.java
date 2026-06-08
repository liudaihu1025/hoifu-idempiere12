package com.hoifu.validator;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.logging.Level;  
  
import org.compiere.model.MAcctSchema;  
import org.compiere.model.MClient;  
import org.compiere.model.MClientInfo;  
import org.compiere.model.ModelValidationEngine;  
import org.compiere.model.ModelValidator;  
import org.compiere.model.PO;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
  
/**  
 * 库存事务成本验证器 - 使用PO对象方法避免类型转换  
 */  
public class TransactionCostValidator implements ModelValidator {  
      
    private CLogger log = CLogger.getCLogger(TransactionCostValidator.class);  
    private int m_AD_Client_ID = -1;  
      
    @Override  
    public void initialize(ModelValidationEngine engine, MClient client) {  
        if (client != null) {  
            m_AD_Client_ID = client.getAD_Client_ID();  
        }  
        engine.addModelChange("M_Transaction", this);  
        log.log(Level.INFO, "验证器已初始化，客户端ID: " + m_AD_Client_ID);  
    }  
      
    @Override  
    public int getAD_Client_ID() {  
        return m_AD_Client_ID;  
    }  
      
    @Override  
    public String modelChange(PO po, int type) throws Exception {  
        // 检查是否为 M_Transaction 表的记录  
        if (po.get_TableName().equals("M_Transaction")) {  
            if (type == TYPE_BEFORE_NEW) {  
                updateTransactionCosts(po);  
            }  
        }  
        return null;  
    }  
      
    /**  
     * 更新事务成本信息 - 使用 PO 对象  
     */  
    private void updateTransactionCosts(PO po) throws Exception {  
        try {  
            // 获取物料ID  
            int m_product_id = po.get_ValueAsInt("M_Product_ID");  
            int m_locator_id = po.get_ValueAsInt("M_Locator_ID");
            int m_asi_id = po.get_ValueAsInt("M_AttributeSetInstance_ID");
            
            log.log(Level.INFO, "开始更新事务成本 - 产品ID: " + m_product_id + 
                    ", 库位ID: " + m_locator_id + 
                    ", ASI ID: " + m_asi_id);
              
            // 获取当前成本价格  
            BigDecimal currentCostPrice = getCurrentCostFromCostHistory(po);  
            
            // 获取当前库存数量  
            BigDecimal currentQtyOnHand = getCurrentQtyOnHand(po);  
            
            // 获取成本明细ID  
            Integer costDetailID = getCurrentCostDetailID(po); 
              
            // 设置事务记录的成本字段 - 直接设置，如果为null就设置为null
            po.set_ValueOfColumn("CurrentCostPrice", currentCostPrice);  
            po.set_ValueOfColumn("CurrentQtyOnHand", currentQtyOnHand);  
            po.set_ValueOfColumn("M_CostDetail_ID", costDetailID);
              
            log.log(Level.INFO, "更新事务成本完成 - 物料ID: " + m_product_id +   
                    ", 成本: " + currentCostPrice + ", 数量: " + currentQtyOnHand + 
                    ", 成本明细ID: " + costDetailID);  
                      
        } catch (Exception e) {  
            log.log(Level.SEVERE, "更新事务成本失败: " + e.getMessage(), e);  
            throw e;  
        }  
    }  
      
    /**  
     * 从成本明细获取当前成本价格 - 找不到时返回null而不是Env.ZERO  
     */  
    private BigDecimal getCurrentCostFromCostHistory(PO po) {  
        MAcctSchema as = getAcctSchema();  
        if (as == null) {
            log.log(Level.WARNING, "无法获取会计架构");
            return null;  // 返回null而不是Env.ZERO
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ch.NewCostPrice FROM M_CostHistory ch ");
		sql.append("JOIN M_CostDetail cd ON (cd.M_CostDetail_ID = ch.M_CostDetail_ID AND cd.Processed='Y') ");
		sql.append("JOIN C_AcctSchema cas ON (cas.C_AcctSchema_ID = cd.C_AcctSchema_ID) ");
		sql.append("JOIN M_CostElement ce ON (ce.M_CostElement_ID = ch.M_CostElement_ID) ");
		sql.append("WHERE ch.AD_Client_ID=? AND ch.AD_Org_ID=? ");
		sql.append("AND ch.M_Product_ID=? AND (ch.M_AttributeSetInstance_ID=? OR ch.M_AttributeSetInstance_ID=0) ");
		sql.append("AND ch.M_CostType_ID=? AND cd.C_AcctSchema_ID=? ");
		sql.append("AND (ce.CostingMethod IS NULL OR ce.CostingMethod=cas.CostingMethod) ");
		sql.append("AND ce.CostElementType = 'M' ");
		sql.append("ORDER BY ch.Created DESC, ch.M_CostHistory_ID DESC LIMIT 1");

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql.toString(), po.get_TrxName());
			pstmt.setInt(1, po.getAD_Client_ID());
			pstmt.setInt(2, po.getAD_Org_ID());
			pstmt.setInt(3, po.get_ValueAsInt("M_Product_ID"));
			pstmt.setInt(4, po.get_ValueAsInt("M_AttributeSetInstance_ID"));
			pstmt.setInt(5, as.getM_CostType_ID());
			pstmt.setInt(6, as.getC_AcctSchema_ID());

			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal cost = rs.getBigDecimal(1);
				if (!rs.wasNull()) {
					log.log(Level.FINE, "从成本历史SQL找到成本: " + cost + ", 产品ID: " + po.get_ValueAsInt("M_Product_ID"));
					return cost;
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "查询历史成本SQL失败: " + e.getMessage(), e);
			return null;
		} finally {
			DB.close(rs, pstmt);
		}

		return null;
    }  
      
    /**  
     * 获取当前库存数量 - 找不到时返回null而不是BigDecimal.ZERO  
     * 修改说明：在历史库存基础上加上当前事务的数量
     */  
    private BigDecimal getCurrentQtyOnHand(PO po) {  
        try {
            // 获取参数值
            int productId = po.get_ValueAsInt("M_Product_ID");
            int locatorId = po.get_ValueAsInt("M_Locator_ID");
            Timestamp created = (Timestamp)po.get_Value("Created");
            int clientId = po.getAD_Client_ID();
            int orgId = po.getAD_Org_ID();
            
            // 空指针检查：检查必要参数
            if (created == null) {
                log.log(Level.WARNING, "Created日期为空，产品ID: " + productId + ", 库位ID: " + locatorId);
                return null;  // 返回null而不是BigDecimal.ZERO
            }
            
            // 与进程中的calculateHistoricalQty保持一致
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT SUM(MovementQty) FROM M_Transaction ");
            sql.append("WHERE M_Product_ID = ? AND M_Locator_ID = ? ");
//            sql.append("AND AD_Client_ID = ? AND AD_Org_ID = ? ");
            sql.append("AND Created <= ? ");
            
            // 传递正确的5个参数
            BigDecimal qty = DB.getSQLValueBDEx(po.get_TrxName(), sql.toString(),  
                productId,  
                locatorId,  
//                clientId,  // 第3个参数：AD_Client_ID
//                orgId,     // 第4个参数：AD_Org_ID
                created);  // 第5个参数：Created
            
            if (qty == null) {
                log.log(Level.FINE, "未找到库存数量记录，产品ID: " + productId + ", 库位ID: " + locatorId);
            } else {
                log.log(Level.FINE, "计算历史库存: Product_ID=" + productId + 
                        ", Locator_ID=" + locatorId + 
//                        ", Client_ID=" + clientId +
//                        ", Org_ID=" + orgId +
                        ", Created=" + created + 
                        ", Qty=" + qty);
            }
            
            // 添加当前事务的数量
            Object movementQtyObj = po.get_Value("MovementQty");
            BigDecimal currentMovementQty = null;
            if (movementQtyObj != null) {
                if (movementQtyObj instanceof BigDecimal) {
                    currentMovementQty = (BigDecimal) movementQtyObj;
                } else {
                    try {
                        currentMovementQty = new BigDecimal(movementQtyObj.toString());
                    } catch (NumberFormatException e) {
                        log.log(Level.WARNING, "无法将MovementQty转换为BigDecimal: " + movementQtyObj);
                    }
                }
            }
            
            // 合并历史库存和当前事务数量
            BigDecimal totalQty = null;
            if (qty == null && currentMovementQty == null) {
                return null;
            } else {
                totalQty = BigDecimal.ZERO;
                if (qty != null) {
                    totalQty = totalQty.add(qty);
                }
                if (currentMovementQty != null) {
                    totalQty = totalQty.add(currentMovementQty);
                }
                log.log(Level.FINE, "总库存数量（包含当前事务）: " + totalQty);
            }
            
            return totalQty;
        } catch (Exception e) {
            log.log(Level.WARNING, "获取库存数量失败: " + e.getMessage(), e);  
            return null;
        }
    }
      
    /**  
     * 获取成本明细ID - 找不到时返回null而不是0  
     */  
    private Integer getCurrentCostDetailID(PO po) {  // 修改：返回类型改为Integer
        MAcctSchema as = getAcctSchema();  
        if (as == null) {
            log.log(Level.WARNING, "无法获取会计架构");
            return null;  // 返回null而不是0
        }
          
        StringBuilder sql = new StringBuilder();
		sql.append("SELECT ch.M_CostDetail_ID FROM M_CostHistory ch ");
		sql.append("JOIN M_CostDetail cd ON (cd.M_CostDetail_ID = ch.M_CostDetail_ID AND cd.Processed='Y') ");
		sql.append("JOIN C_AcctSchema cas ON (cas.C_AcctSchema_ID = cd.C_AcctSchema_ID) ");
		sql.append("JOIN M_CostElement ce ON (ce.M_CostElement_ID = ch.M_CostElement_ID) ");
		sql.append("WHERE ch.AD_Client_ID=? AND ch.AD_Org_ID=? ");
		sql.append("AND ch.M_Product_ID=? AND (ch.M_AttributeSetInstance_ID=? OR ch.M_AttributeSetInstance_ID=0) ");
		sql.append("AND ch.M_CostType_ID=? AND cd.C_AcctSchema_ID=? ");
		sql.append("AND (ce.CostingMethod IS NULL OR ce.CostingMethod=cas.CostingMethod) ");
		sql.append("AND ce.CostElementType = 'M' ");
		sql.append("ORDER BY ch.Created DESC, ch.M_CostHistory_ID DESC LIMIT 1");

        try {  
            // 使用getSQLValueEx而不是getSQLValue，因为前者查询无结果时返回null
            int result = DB.getSQLValueEx(po.get_TrxName(), sql.toString(),  
            	po.getAD_Client_ID(), 
            	po.getAD_Org_ID(), 
                po.get_ValueAsInt("M_Product_ID"),  
                po.get_ValueAsInt("M_AttributeSetInstance_ID"),  
                as.getM_CostType_ID(), 
                as.getC_AcctSchema_ID());
            
            return result < 0 ? null : result;
        } catch (Exception e) {  
            log.log(Level.WARNING, "获取成本明细ID失败: " + e.getMessage(), e);  
            return null;  // 返回null而不是0
        }  
    }  
      
    /**  
     * 获取会计架构  
     */  
    private MAcctSchema getAcctSchema() {  
        try {  
            MClientInfo info = MClientInfo.get(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()), null);  
            if (info != null) {  
                return info.getMAcctSchema1();  
            } else {  
                log.log(Level.WARNING, "未找到客户信息，Client_ID: " + Env.getAD_Client_ID(Env.getCtx()));  
                return null;  
            }  
        } catch (Exception e) {  
            log.log(Level.SEVERE, "获取会计架构失败: " + e.getMessage(), e);  
            return null;  
        }  
    }  
      
    @Override  
    public String docValidate(PO po, int timing) {  
        return null;  
    }  
      
    @Override  
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {  
        return null;  
    }  
}