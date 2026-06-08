package com.hoifu.validator;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class PaymentLineValidator implements ModelValidator {  
      
	private static final CLogger log = CLogger.getCLogger(PaymentLineValidator.class);
    private int m_AD_Client_ID = -1;  
      
    @Override  
    public void initialize(ModelValidationEngine engine, MClient client) {  
        if (client != null) {  
            m_AD_Client_ID = client.getAD_Client_ID();  
        }  

		// 注册相关表的模型变更监听
        engine.addModelChange("C_PaymentLine", this);  
		engine.addModelChange("C_Payment", this);
		engine.addModelChange("C_PaymentAllocate", this);

		if (log.isLoggable(Level.INFO)) {
			log.info("PaymentLineValidator initialized for client: " + m_AD_Client_ID);
		}
    }  
      
    @Override  
    public String modelChange(PO po, int type) throws Exception {  
		String tableName = po.get_TableName();

		if ("C_PaymentLine".equals(tableName)) {
            if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE || type == TYPE_AFTER_DELETE) {  
                updatePaymentHeaderTotals(po);  
				updatePaymentDueAmt(po);
				updatePaymentLineRemainingAmt(po);
            }  
        }  
		else if ("C_Payment".equals(tableName)) {
			if (type == TYPE_AFTER_CHANGE && po.is_ValueChanged("C_Invoice_ID")) {
				updatePaymentDueAmt(po);
				updatePaymentLineRemainingAmt(po);
			}
		} else if ("C_PaymentAllocate".equals(tableName)) {
			if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE || type == TYPE_AFTER_DELETE) {
				updatePaymentDueAmt(po);
				updatePaymentLineRemainingAmt(po);
				updatePaymentPayAmtFromAllocate(po);
			}
		}

        return null;  
    }  
      
    /**  
     * 更新支付表头汇总金额  
     */  
    private void updatePaymentHeaderTotals(PO paymentLine) {  
        int C_Payment_ID = paymentLine.get_ValueAsInt("C_Payment_ID");  
        if (C_Payment_ID <= 0) return;  
          
		// 更新明细汇总字段
		String detailSql = "UPDATE C_Payment p " + "SET Amount_Details = (SELECT COALESCE(SUM("
				+ "CASE WHEN pl.TenderType IN ('M','E','B','G') THEN pl.Amount " + "ELSE pl.PayAmt END),0) "
				+ "FROM C_PaymentLine pl " + "WHERE p.C_Payment_ID=pl.C_Payment_ID AND pl.IsActive='Y'), "
				+ "DiscountAmt_Details = (SELECT COALESCE(SUM(pl.DiscountAmt),0) " + "FROM C_PaymentLine pl "
				+ "WHERE p.C_Payment_ID=pl.C_Payment_ID AND pl.IsActive='Y'), "
				+ "WriteoffAmt_Details = (SELECT COALESCE(SUM(pl.WriteoffAmt),0) " + "FROM C_PaymentLine pl "
				+ "WHERE p.C_Payment_ID=pl.C_Payment_ID AND pl.IsActive='Y'), "
				+ "OverUnderAmt_Details = (SELECT COALESCE(SUM(pl.OverUnderAmt),0) " + "FROM C_PaymentLine pl "
				+ "WHERE p.C_Payment_ID=pl.C_Payment_ID AND pl.IsActive='Y') " 
            + "WHERE C_Payment_ID=" + C_Payment_ID;  
          
		DB.executeUpdateEx(detailSql, paymentLine.get_TrxName());

		// 更新表头字段（根据支付方式和费等额支付设置）
		updatePaymentHeaderAmounts(C_Payment_ID, paymentLine.get_TrxName());
	}

	/**
	 * 更新支付表头的WriteoffAmt、DiscountAmt、OverUnderAmt字段
	 * 仅当支付方式不为票据时，并根据是否允许费等额支付决定是否更新OverUnderAmt
	 */
	private void updatePaymentHeaderAmounts(int C_Payment_ID, String trxName) {
		// 获取是否允许费等额支付设置
		boolean isOverUnderPayment = isOverUnderPaymentEnabled(C_Payment_ID, trxName);

		// 查询支付方式不为票据的明细行汇总
		String sql = "SELECT COALESCE(SUM(DiscountAmt),0) as totalDiscount, "
				+ "COALESCE(SUM(WriteoffAmt),0) as totalWriteoff, "
				+ "COALESCE(SUM(OverUnderAmt),0) as totalOverUnder, "
				+ "COALESCE(SUM(PayAmt),0) as totalPayAmt "
				+ "FROM C_PaymentLine " + "WHERE C_Payment_ID = ? AND IsActive='Y' "
				+ "AND TenderType NOT IN ('M','E','B','G')";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_Payment_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				BigDecimal totalDiscount = rs.getBigDecimal("totalDiscount");
				BigDecimal totalWriteoff = rs.getBigDecimal("totalWriteoff");
				BigDecimal totalOverUnder = rs.getBigDecimal("totalOverUnder");
				BigDecimal totalPayAmt = rs.getBigDecimal("totalPayAmt");

				// 当不允许等额支付时，将支付差额汇总到核销金额
				if (!isOverUnderPayment) {
					totalWriteoff = totalWriteoff.add(totalOverUnder);
					totalOverUnder = Env.ZERO;
				}

				// 更新C_Payment表的相关字段
				String updateSql = "UPDATE C_Payment "
						+ "SET DiscountAmt = ?, WriteoffAmt = ?, OverUnderAmt = ?, PayAmt = ? "
						+ "WHERE C_Payment_ID = ?";

				PreparedStatement updateStmt = DB.prepareStatement(updateSql, trxName);
				updateStmt.setBigDecimal(1, totalDiscount);
				updateStmt.setBigDecimal(2, totalWriteoff);
				updateStmt.setBigDecimal(3, totalOverUnder);
				updateStmt.setBigDecimal(4, totalPayAmt);
				updateStmt.setInt(5, C_Payment_ID);
				updateStmt.executeUpdate();
				updateStmt.close();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to update payment header amounts", e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * 更新支付单的DueAmt字段
	 */
	private void updatePaymentDueAmt(PO po) {
		int C_Payment_ID = 0;
		if ("C_PaymentLine".equals(po.get_TableName())) {
			C_Payment_ID = po.get_ValueAsInt("C_Payment_ID");
		} else if ("C_Payment".equals(po.get_TableName())) {
			C_Payment_ID = po.get_ID();
		} else if ("C_PaymentAllocate".equals(po.get_TableName())) {
			C_Payment_ID = po.get_ValueAsInt("C_Payment_ID");
		}

		if (C_Payment_ID <= 0)
			return;

		BigDecimal dueAmt = calculateDueAmt(C_Payment_ID, po.get_TrxName());

		// 更新DueAmt字段
		String updateSql = "UPDATE C_Payment SET DueAmt = ? WHERE C_Payment_ID = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = DB.prepareStatement(updateSql, po.get_TrxName());
			pstmt.setBigDecimal(1, dueAmt);
			pstmt.setInt(2, C_Payment_ID);
			pstmt.executeUpdate();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to update DueAmt", e);
		} finally {
			DB.close(pstmt);
		}

		// 更新所有关联支付明细的DueAmt字段
		updatePaymentLineDueAmt(C_Payment_ID, dueAmt, po.get_TrxName());
	}

	/**
	 * 计算应收金额 优先级：主表直接关联 > 支付分配关联
	 */
	private BigDecimal calculateDueAmt(int C_Payment_ID, String trxName) {
		// 检查主表是否直接关联应收单
		String directSql = "SELECT C_Invoice_ID FROM C_Payment WHERE C_Payment_ID = ? AND C_Invoice_ID > 0";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(directSql, trxName);
			pstmt.setInt(1, C_Payment_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				int C_Invoice_ID = rs.getInt("C_Invoice_ID");
				if (C_Invoice_ID > 0) {
					// 主表直接关联应收单，获取单张应收单金额
					return getInvoiceGrandTotal(C_Invoice_ID, trxName);
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to check direct invoice relation", e);
		} finally {
			DB.close(rs, pstmt);
		}

		// 检查支付分配关联的应收单
		return getInvoicesTotalFromAllocation(C_Payment_ID, trxName);
	}

	/**
	 * 获取单张应收单的总金额
	 */
	private BigDecimal getInvoiceGrandTotal(int C_Invoice_ID, String trxName) {
		String sql = "SELECT GrandTotal FROM C_Invoice WHERE C_Invoice_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_Invoice_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal("GrandTotal");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to get invoice total", e);
		} finally {
			DB.close(rs, pstmt);
		}

		return Env.ZERO;
	}

	/**
	 * 从支付分配中获取关联应收单的总金额
	 */
	private BigDecimal getInvoicesTotalFromAllocation(int C_Payment_ID, String trxName) {
		String sql = "SELECT COALESCE(SUM(Amount), 0) " + "FROM C_PaymentAllocate "
				+ "WHERE C_Payment_ID = ? AND IsActive = 'Y'";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_Payment_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal(1);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to get total amount from allocation", e);
		} finally {
			DB.close(rs, pstmt);
		}

		return Env.ZERO;
	}

	/**
	 * 检查是否允许费等额支付
	 */
	private boolean isOverUnderPaymentEnabled(int C_Payment_ID, String trxName) {
		String sql = "SELECT IsOverUnderPayment FROM C_Payment WHERE C_Payment_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_Payment_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				String isOverUnder = rs.getString("IsOverUnderPayment");
				return "Y".equals(isOverUnder);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to check IsOverUnderPayment", e);
		} finally {
			DB.close(rs, pstmt);
		}

		return false;
	}

	/**
	 * 更新支付明细的DueAmt字段
	 */
	private void updatePaymentLineDueAmt(int C_Payment_ID, BigDecimal dueAmt, String trxName) {
		String updateSql = "UPDATE C_PaymentLine SET DueAmt = ? WHERE C_Payment_ID = ? AND IsActive='Y'";
		PreparedStatement pstmt = null;
		try {
			pstmt = DB.prepareStatement(updateSql, trxName);
			pstmt.setBigDecimal(1, dueAmt);
			pstmt.setInt(2, C_Payment_ID);
			int updatedCount = pstmt.executeUpdate();

			if (log.isLoggable(Level.FINE)) {
				log.fine("Updated DueAmt for " + updatedCount + " payment lines to: " + dueAmt);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to update payment line DueAmt", e);
		} finally {
			DB.close(pstmt);
		}
	}

	/**
	 * 更新支付明细的RemainingAmt剩余金额 规则：表头DueAmt - 支付方式为'M','E','B','G'的Amount汇总 -
	 * 其他支付方式的PayAmt汇总
	 */
	private void updatePaymentLineRemainingAmt(PO po) {
		int C_Payment_ID = 0;
		if ("C_PaymentLine".equals(po.get_TableName())) {
			C_Payment_ID = po.get_ValueAsInt("C_Payment_ID");
		} else if ("C_Payment".equals(po.get_TableName())) {
			C_Payment_ID = po.get_ID();
		}

		if (C_Payment_ID <= 0)
			return;

		// 获取表头的DueAmt
		BigDecimal dueAmt = getPaymentDueAmt(C_Payment_ID, po.get_TrxName());

		// 使用优化的SQL一次性计算所有支付明细的汇总金额
		BigDecimal totalAmount = getTotalPaymentAmount(C_Payment_ID, po.get_TrxName());

		// 计算剩余金额
		BigDecimal remainingAmt = dueAmt.subtract(totalAmount);

		// 更新所有支付明细的RemainingAmt
		updateAllPaymentLinesRemainingAmt(C_Payment_ID, remainingAmt, po.get_TrxName());
	}

	/**
	 * 获取支付表头的DueAmt
	 */
	private BigDecimal getPaymentDueAmt(int C_Payment_ID, String trxName) {
		String sql = "SELECT DueAmt FROM C_Payment WHERE C_Payment_ID = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_Payment_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal("DueAmt");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to get payment DueAmt", e);
		} finally {
			DB.close(rs, pstmt);
		}

		return Env.ZERO;
	}

	/**
	 * 使用优化的SQL获取所有支付明细的汇总金额 支付方式为'M','E','B','G'时使用Amount字段，其他使用PayAmt字段
	 */
	private BigDecimal getTotalPaymentAmount(int C_Payment_ID, String trxName) {
		String sql = "SELECT COALESCE(SUM(CASE WHEN TenderType IN ('M','E','B','G') THEN Amount ELSE PayAmt END), 0) "
				+ "FROM C_PaymentLine WHERE C_Payment_ID = ? AND IsActive = 'Y'";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_Payment_ID);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				return rs.getBigDecimal(1);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to get total payment amount", e);
		} finally {
			DB.close(rs, pstmt);
		}

		return Env.ZERO;
	}

	/**
	 * 更新所有支付明细的RemainingAmt字段
	 */
	private void updateAllPaymentLinesRemainingAmt(int C_Payment_ID, BigDecimal remainingAmt, String trxName) {
		String updateSql = "UPDATE C_PaymentLine SET RemainingAmt = ? WHERE C_Payment_ID = ? AND IsActive='Y'";
		PreparedStatement pstmt = null;

		try {
			pstmt = DB.prepareStatement(updateSql, trxName);
			pstmt.setBigDecimal(1, remainingAmt);
			pstmt.setInt(2, C_Payment_ID);
			int updatedCount = pstmt.executeUpdate();

			if (log.isLoggable(Level.FINE)) {
				log.fine("Updated RemainingAmt for " + updatedCount + " payment lines to: " + remainingAmt);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to update payment line RemainingAmt", e);
		} finally {
			DB.close(pstmt);
		}
	}

	/**
	 * 将C_PaymentAllocate的Amount汇总到C_Payment的PayAmt
	 */
	private void updatePaymentPayAmtFromAllocate(PO po) {
		int C_Payment_ID = po.get_ValueAsInt("C_Payment_ID");
		if (C_Payment_ID <= 0)
			return;

		String sql = "UPDATE C_Payment SET PayAmt = " + "(SELECT COALESCE(SUM(Amount), 0) FROM C_PaymentAllocate "
				+ "WHERE C_Payment_ID = ? AND IsActive = 'Y') " + "WHERE C_Payment_ID = ?";

		PreparedStatement pstmt = null;
		try {
			pstmt = DB.prepareStatement(sql, po.get_TrxName());
			pstmt.setInt(1, C_Payment_ID);
			pstmt.setInt(2, C_Payment_ID);
			pstmt.executeUpdate();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to update PayAmt from PaymentAllocate", e);
		} finally {
			DB.close(pstmt);
		}
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
	public String docValidate(PO po, int timing) {
		return null;
	}
}