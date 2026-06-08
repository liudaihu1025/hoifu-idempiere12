package com.hoifu.model;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;   
import java.util.Properties;  
  
import org.adempiere.exceptions.DBException;  
import org.compiere.model.MAcctSchema;  
import org.compiere.model.MClientInfo;  
import org.compiere.model.X_M_Transaction;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
/**  
 * 自定义库存事务模型类  
 */ 
@org.adempiere.base.Model(table="M_Transaction") 
public class MTransaction extends X_M_Transaction {  
      
    public MTransaction(Properties ctx, int M_Transaction_ID, String trxName) {  
        super(ctx, M_Transaction_ID, trxName);  
    }  
      
    public MTransaction(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
    }  
      
    @Override  
    protected boolean beforeSave(boolean newRecord) {  
       
        if (newRecord) {  
            updateCurrentCostAndQty();  
        }  
        return super.beforeSave(newRecord);  
    }  
      
    /**  
     * 更新当前成本价格和库存总数  
     */  
    private void updateCurrentCostAndQty() {  
        // 获取当前成本价格  
        BigDecimal currentCost = getCurrentCostFromCostDetail(this);  
        set_ValueOfColumn("CurrentCostPrice", currentCost);  // 直接设置到数据库字段
        
        // 获取当前库存总数（事务发生前）  
        BigDecimal currentQty = getCurrentQtyOnHandBeforeTransaction();  
        set_ValueOfColumn("CurrentQtyOnHand", currentQty);  // 直接设置到数据库字段
        
        
    }  
      
    /**  
     * 获取当前成本价格（从数据库字段读取）
     */  
    public BigDecimal getCurrentCostPrice() {  
        return (BigDecimal)get_Value("CurrentCostPrice");
    }  
      
    /**  
     * 设置当前成本价格（保存到数据库字段）
     */  
    public void setCurrentCostPrice(BigDecimal cost) {  
        set_ValueOfColumn("CurrentCostPrice", cost);
    }  
      
    /**  
     * 获取当前库存数量（从数据库字段读取）
     */  
    public BigDecimal getCurrentQtyOnHand() {  
        return (BigDecimal)get_Value("CurrentQtyOnHand");
    }  
      
    /**  
     * 设置当前库存数量（保存到数据库字段）
     */  
    public void setCurrentQtyOnHand(BigDecimal qty) {  
        set_ValueOfColumn("CurrentQtyOnHand", qty);
    }  
      
    /**  
     * 获取成本明细ID（从数据库字段读取）
     */  
    public Integer getM_CostDetail_ID() {  
        return get_ValueAsInt("M_CostDetail_ID");
    }  
      
    /**  
     * 设置成本明细ID（保存到数据库字段）
     */  
    public void setM_CostDetail_ID(Integer id) {  
        set_ValueOfColumn("M_CostDetail_ID", id);
    }  
      
    /**  
     * 从成本明细表获取当前成本价格  
     */  
    private BigDecimal getCurrentCostFromCostDetail(MTransaction trx) {  
        MAcctSchema as = getAcctSchema();  
        if (as == null) {
            return null;  // 返回null，而不是0
        }
          
        StringBuilder sql = new StringBuilder();  
        sql.append("SELECT CurrentCostPrice FROM M_CostDetail ");  
        sql.append("WHERE AD_Client_ID=? AND AD_Org_ID=? ");  
        sql.append("AND M_Product_ID=? ");  
        sql.append("AND (M_AttributeSetInstance_ID=? OR M_AttributeSetInstance_ID=0) ");  
        sql.append("AND C_AcctSchema_ID=? ");  
        sql.append("AND Processed='Y' ");  
        sql.append("AND DateAcct <= ? ");  
        sql.append("ORDER BY DateAcct DESC, M_CostDetail_ID DESC");  
          
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
          
        try {  
            pstmt = DB.prepareStatement(sql.toString(), trx.get_TrxName());  
            pstmt.setInt(1, trx.getAD_Client_ID());  
            pstmt.setInt(2, trx.getAD_Org_ID());  
            pstmt.setInt(3, trx.getM_Product_ID());  
            pstmt.setInt(4, trx.getM_AttributeSetInstance_ID());  
            pstmt.setInt(5, as.getC_AcctSchema_ID());  
            
            // 使用 MovementDate
            if (trx.getMovementDate() != null) {
                pstmt.setTimestamp(6, trx.getMovementDate());  
            } else {
                pstmt.setTimestamp(6, trx.getCreated());
            }
              
            rs = pstmt.executeQuery();  
            if (rs.next()) {  
                BigDecimal cost = rs.getBigDecimal(1);
                return rs.wasNull() ? null : cost;  // 如果是SQL NULL则返回null
            }  
        } catch (SQLException e) {  
            throw new DBException(e, sql.toString());  
        } finally {  
            DB.close(rs, pstmt);  
        }  
          
        return null;  // 没有记录时返回null
    }  
      
    /**  
     * 获取事务发生前的库存总数  
     */  
    private BigDecimal getCurrentQtyOnHandBeforeTransaction() {  
        StringBuilder sql = new StringBuilder();  
        sql.append("SELECT SUM(MovementQty) FROM M_Transaction ");  // 去掉COALESCE
        sql.append("WHERE M_Product_ID=? AND M_Locator_ID=? ");  
        sql.append("AND M_AttributeSetInstance_ID=? ");  
        sql.append("AND AD_Client_ID=? AND AD_Org_ID=? ");
        sql.append("AND (MovementDate < ? OR (MovementDate = ? AND M_Transaction_ID < ?))");  
          
        try {
            BigDecimal qty = DB.getSQLValueBD(get_TrxName(), sql.toString(),  
                getM_Product_ID(),  
                getM_Locator_ID(),  
                getM_AttributeSetInstance_ID(),  
                getAD_Client_ID(),
                getAD_Org_ID(),
                getMovementDate(),  
                getMovementDate(),  
                get_ID());
            
            // 如果没有记录，返回null
            return qty;
        } catch (Exception e) {
            return null;  // 异常时返回null
        }
    }  
      
    /**  
     * 获取会计架构  
     */  
    private MAcctSchema getAcctSchema() {  
        try {
            MClientInfo info = MClientInfo.get(getCtx(), getAD_Client_ID(), get_TrxName());  
            if (info != null) {
                return info.getMAcctSchema1();
            }
        } catch (Exception e) {
            // 记录错误
        }
        return null;
    }  
    
    /**
     * 安全的获取当前成本价格
     */
    public BigDecimal getSafeCurrentCostPrice() {
        BigDecimal cost = getCurrentCostPrice();
        return cost != null ? cost : BigDecimal.ZERO;
    }
    
    /**
     * 安全的获取当前库存数量
     */
    public BigDecimal getSafeCurrentQtyOnHand() {
        BigDecimal qty = getCurrentQtyOnHand();
        return qty != null ? qty : BigDecimal.ZERO;
    }
    
    /**
     * 安全的获取成本明细ID
     */
    public int getSafeM_CostDetail_ID() {
        Integer id = getM_CostDetail_ID();
        return id != null ? id : 0;
    }
}