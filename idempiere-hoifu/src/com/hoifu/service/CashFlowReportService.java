package com.hoifu.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.compiere.util.DB;

/**
 * 
 * @Description: 现金流量报表Service
 * @author ldh
 * @date 2025年12月9日
 */
public class CashFlowReportService {

	/**
	 * 生成期间现金流量表
	 */
	public List<CashFlowReportItem> generatePeriodReport(int periodId) {
		String sql = "SELECT " + "cfi.C_CashFlow_Item_ID, " + "cfi.Name, " + "cfi.CashFlow_Type, " + "cfi.Is_CashIn, "
				+ "cfi.Sequence, " + "SUM(cf.Amount) as Total_Amount " + "FROM Fact_CashFlow cf "
				+ "JOIN C_CashFlow_Item cfi ON cf.C_CashFlow_Item_ID = cfi.C_CashFlow_Item_ID "
				+ "WHERE cf.C_Period_ID = ? " + "AND cf.IsActive = 'Y' "
				+ "GROUP BY cfi.C_CashFlow_Item_ID, cfi.Name, cfi.CashFlow_Type, " + "cfi.Is_CashIn, cfi.Sequence "
				+ "ORDER BY cfi.Sequence";

//        List<Object[]> results = DB.getSQLArrayObjectsEx(null, sql, periodId);  
		List<Object[]> results = DB.getSQLArrayObjectsEx(null, sql, periodId).stream()
				.map(list -> list.toArray(new Object[0])).collect(Collectors.toList());
		List<CashFlowReportItem> items = new ArrayList<>();

		for (Object[] row : results) {
			CashFlowReportItem item = new CashFlowReportItem();
			item.setItemId(((Number) row[0]).intValue());
			item.setItemName((String) row[1]);
			item.setCashFlowType((String) row[2]);
			item.setIsCashIn("Y".equals(row[3]));
			item.setSequence(((Number) row[4]).intValue());
			item.setTotalAmount((BigDecimal) row[5]);
			items.add(item);
		}

		return items;
	}

	/**
	 * 现金流量表项目数据传输对象
	 */
	public static class CashFlowReportItem {
		private int itemId;
		private String itemName;
		private String cashFlowType;
		private boolean isCashIn;
		private int sequence;
		private BigDecimal totalAmount;

		// getters and setters
		public int getItemId() {
			return itemId;
		}

		public void setItemId(int itemId) {
			this.itemId = itemId;
		}

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public String getCashFlowType() {
			return cashFlowType;
		}

		public void setCashFlowType(String cashFlowType) {
			this.cashFlowType = cashFlowType;
		}

		public boolean isCashIn() {
			return isCashIn;
		}

		public void setIsCashIn(boolean isCashIn) {
			this.isCashIn = isCashIn;
		}

		public int getSequence() {
			return sequence;
		}

		public void setSequence(int sequence) {
			this.sequence = sequence;
		}

		public BigDecimal getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}
	}
}