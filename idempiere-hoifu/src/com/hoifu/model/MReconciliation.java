
package com.hoifu.model;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;  
  
@org.adempiere.base.Model(table="c_reconciliation")  
public class MReconciliation extends X_C_Reconciliation {  
  
    /**  
     * generated serial id  
     */  
    private static final long serialVersionUID = 123456789L;  
    
    /**  
     * UUID based Constructor  
     * @param ctx Context  
     * @param C_Reconciliation_UU UUID key  
     * @param trxName Transaction  
     */  
    public MReconciliation(Properties ctx, String C_Reconciliation_UU, String trxName) {  
        super(ctx, C_Reconciliation_UU, trxName);  
    }  
  
    /**  
     * Standard Constructor  
     * @param ctx context  
     * @param C_Reconciliation_ID id  
     * @param trxName transaction  
     */  
    public MReconciliation(Properties ctx, int C_Reconciliation_ID, String trxName) {  
        super(ctx, C_Reconciliation_ID, trxName);  
    }  
  
    /**  
     * Load Constructor  
     * @param ctx context  
     * @param rs result set  
     * @param trxName transaction  
     */  
    public MReconciliation(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
    } 
    
    /**
    * 设置单据状态
    * @param docStatus 单据状态
    */
   public void setDocStatus(String docStatus) {
       set_Value("DocStatus", docStatus);
   }
   
   /**
    * 获取单据状态
    * @return 单据状态
    */
   public String getDocStatus() {
       return (String)get_Value("DocStatus");
   }
   
   /**
    * 设置对账总额
    * @param totalReconAmt 对账总额
    */
   public void setTotalReconAmt(BigDecimal totalReconAmt) {
       set_Value("TotalReconAmt", totalReconAmt);
   }
   
   /**
    * 获取对账总额
    * @return 对账总额
    */
   public BigDecimal getTotalReconAmt() {
       BigDecimal bd = (BigDecimal)get_Value("TotalReconAmt");
       if (bd == null)
           return Env.ZERO;
       return bd;
   }
   
   /**
    * 设置对账总数量
    * @param totalReconQty 对账总数量
    */
   public void setTotalReconQty(BigDecimal totalReconQty) {
       set_Value("TotalReconQty", totalReconQty);
   }
   
   /**
    * 获取对账总数量
    * @return 对账总数量
    */
   public BigDecimal getTotalReconQty() {
       BigDecimal bd = (BigDecimal)get_Value("TotalReconQty");
       if (bd == null)
           return Env.ZERO;
       return bd;
   }
   
   /**
    * 设置期初余额
    * @param beginningBalance 期初余额
    */
   public void setBeginningBalance(BigDecimal beginningBalance) {
       set_Value("BeginningBalance", beginningBalance);
   }
   
   /**
    * 获取期初余额
    * @return 期初余额
    */
   public BigDecimal getBeginningBalance() {
       BigDecimal bd = (BigDecimal)get_Value("BeginningBalance");
       if (bd == null)
           return Env.ZERO;
       return bd;
   }
   
   /**
    * 设置期末余额
    * @param endingBalance 期末余额
    */
   public void setEndingBalance(BigDecimal endingBalance) {
       set_Value("EndingBalance", endingBalance);
   }
   
   /**
    * 获取期末余额
    * @return 期末余额
    */
   public BigDecimal getEndingBalance() {
       BigDecimal bd = (BigDecimal)get_Value("EndingBalance");
       if (bd == null)
           return Env.ZERO;
       return bd;
   }
    
    /**
     * 准备操作 - 计算所有金额
     */
    public boolean prepareIt() {
        if (isProcessed()) {
            log.saveError("DocumentProcessed", "此对账单已处理");
            return false;
        }
        
        // 计算明细行金额
        List<MReconciliationLine> lines = getLines();
        for (MReconciliationLine line : lines) {
            line.calculateAmounts(this.getC_Currency_ID());
            line.saveEx(get_TrxName());
        }
        
        // 计算总额
        calculateTotals();
        
        setDocStatus("IP");
        setProcessed(true);
        saveEx(get_TrxName());
        
        return true;
    }
    
    /**
     * 完成操作
     */
    public boolean completeIt() {
        if (!isProcessed()) {
            prepareIt();
        }
        
        // 验证所有明细行
        List<MReconciliationLine> lines = getLines();
        for (MReconciliationLine line : lines) {
            if (!line.isValid()) {
                log.saveError("Error", "明细行" + line.getLine() + "无效");
                return false;
            }
        }
        
        setDocStatus("CO");
        setProcessed(true);
        saveEx(get_TrxName());
        
        return true;
    }
    
    /**
     * 获取所有明细行
     */
    public List<MReconciliationLine> getLines() {
        return new Query(getCtx(), MReconciliationLine.Table_Name, 
            "C_Reconciliation_ID=?", get_TrxName())
            .setParameters(getC_Reconciliation_ID())
            .setOrderBy("Line")
            .list();
    }
    
    /**
     * 计算总额
     */
    private void calculateTotals() {
        List<MReconciliationLine> lines = getLines();
        
        BigDecimal totalReconAmt = BigDecimal.ZERO;
        BigDecimal totalReconQty = BigDecimal.ZERO;
        
        for (MReconciliationLine line : lines) {
            if (line.getReconciledAmt() != null) {
                totalReconAmt = totalReconAmt.add(line.getReconciledAmt());
            }
            if (line.getQtyReconciled() != null) {
                totalReconQty = totalReconQty.add(line.getQtyReconciled());
            }
        }
        
        setTotalReconAmt(totalReconAmt);
        setTotalReconQty(totalReconQty);
        
        // 期末余额 = 期初余额 + 对账总额
        BigDecimal beginningBalance = getBeginningBalance() != null ? getBeginningBalance() : BigDecimal.ZERO;
        BigDecimal endingBalance = beginningBalance.add(totalReconAmt);
        setEndingBalance(endingBalance);
    }

    public void setIsApproved(boolean IsApproved) {
        // 检查是否有这个字段
        try {
            set_Value("IsApproved", Boolean.valueOf(IsApproved));
        } catch (Exception e) {
            // 字段不存在，忽略
            log.warning("C_Reconciliation 表没有 IsApproved 字段");
        }
    }
    
    /**
     * 获取是否已审批
     */
    public boolean isApproved() {
        try {
            Object oo = get_Value("IsApproved");
            if (oo != null) {
                if (oo instanceof Boolean)
                    return ((Boolean)oo).booleanValue();
                return "Y".equals(oo);
            }
        } catch (Exception e) {
            // 字段不存在
            log.warning("C_Reconciliation 表没有 IsApproved 字段");
        }
        return false;
    }
    
    
}