package com.hoifu.acct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.acct.DocLine;
import org.compiere.acct.Doc_Invoice;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTax;
import org.compiere.model.PO;
import org.compiere.model.ProductCost;
import org.compiere.util.Msg;

/**
 * 应收单(C_Invoice)暂估应收扩展，新增应收单的暂估分录。
 */
public class Doc_Invoice_Accrual extends Doc_Invoice {

	public Doc_Invoice_Accrual(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		// 1. 先执行标准逻辑：应收账款/主营业务收入/销项税分录全部由父类生成，不做任何改动
		ArrayList<Fact> facts = super.createFacts(as);
		if (facts == null || facts.isEmpty()) {
			return facts;
		}

		// 标准 ARI/ARF 分支只生成了一个 Fact，直接取第一个追加
		Fact fact = facts.get(0);

		// 2. 只对"销售应收发票"追加暂估冲减分录，与标准 ARI/ARF 分支保持一致的适用范围
		if (DOCTYPE_ARInvoice.equals(getDocumentType()) || DOCTYPE_ARProForma.equals(getDocumentType())) {
			appendAccrualReversalLines(as, fact);
		}  

		return facts;
	}

	/**
	 * 遍历发票明细行，追加暂估收入分录：
	 */
	private void appendAccrualReversalLines(MAcctSchema as, Fact fact) {
		// 取暂估相关的两个科目：应收账款-暂估、应交税费-待转销项税额
		MAcctSchemaDefault acctDefault = as.getAcctSchemaDefault();
		int receivableAccruedAcctID = acctDefault.get_ValueAsInt("C_Receivable_Accrued_Acct");
		int taxDuePendingAcctID = acctDefault.get_ValueAsInt("T_Due_Pending_Acct");
		if (receivableAccruedAcctID <= 0 || taxDuePendingAcctID <= 0) {
			p_Error = Msg.getMsg(getCtx(), "AcctSchemaDefault 未配置应收暂估/待转销项税额科目");
			log.log(Level.WARNING, p_Error);
			return;
		}
		MAccount receivableAccruedAcct = MAccount.get(as.getCtx(), receivableAccruedAcctID);
		MAccount taxDuePendingAcct = MAccount.get(as.getCtx(), taxDuePendingAcctID);

		BigDecimal totalGrossAmt = BigDecimal.ZERO;
		Map<Integer, BigDecimal> taxAmtByTaxID = new HashMap<Integer, BigDecimal>();
		DocLine anyAccrualLine = null;

		for (int i = 0; i < p_lines.length; i++) {
			DocLine docLine = p_lines[i];
			MInvoiceLine invoiceLine = (MInvoiceLine) docLine.getPO();

			boolean isAccrualApplied = invoiceLine.get_ValueAsBoolean("IsAccrualApplied");
			BigDecimal qty = invoiceLine.getQtyInvoiced();
			if (qty.signum() == 0) {
				continue;
			}

			// ============ 定位发货行 ============
			int shipLineID;
			boolean isFirstNormal = (qty.signum() > 0) && !isAccrualApplied;
			if (isFirstNormal) {
				shipLineID = invoiceLine.getM_InOutLine_ID();
			} else {
				shipLineID = invoiceLine.get_ValueAsInt("AccrualShipLine_ID");
			}
			if (shipLineID <= 0) {
				continue;
			}  
			MInOutLine shipLine = new MInOutLine(getCtx(), shipLineID, getTrxName());

			MAccount revenueAcct = docLine.getAccount(ProductCost.ACCTTYPE_P_Revenue, as);
			if (revenueAcct == null) {
				p_Error = Msg.getMsg(getCtx(), "无法取得主营业务收入科目，跳过本发票行暂估冲减");
				log.log(Level.WARNING, p_Error + " - C_InvoiceLine_ID=" + invoiceLine.get_ID());
				continue;  
			}  

			BigDecimal currentQty;// 本次可冲暂估数量
			BigDecimal grossAmt;// 总额
			BigDecimal netAmt;// 净额
			BigDecimal taxAmt;// 税额
			int taxID = invoiceLine.getC_Tax_ID();
			MTax tax = MTax.get(getCtx(), taxID);

			if (qty.signum() > 0) {
				// ============ 正常发票行 ============
				if (!isAccrualApplied) {
					BigDecimal shipAccrualQty = getBD(shipLine, "AccrualQty");
					BigDecimal shipReversedQty = getBD(shipLine, "AccrualReversedQty");
					BigDecimal remainingQty = shipAccrualQty.subtract(shipReversedQty);
					if (remainingQty.signum() <= 0) {
						continue;
					}
					currentQty = qty.min(remainingQty);

					if (remainingQty.compareTo(currentQty) == 0) {
						// 命中最后一笔：含税总额、收入分别独立倒挤，税额用减法得出，
						// 三者精确满足 grossAmt = netAmt + taxAmt，且累计正好耗尽发货行两个池子
						BigDecimal shipAccrualAmt = getBD(shipLine, "AccrualAmt");
						BigDecimal shipReversedAmt0 = getBD(shipLine, "AccrualReversedAmt");
						grossAmt = shipAccrualAmt.subtract(shipReversedAmt0);

						BigDecimal shipAccrualRevenue = getBD(shipLine, "AccrualRevenue");
						BigDecimal shipReversedRevenue0 = getBD(shipLine, "AccrualReversedRevenue");
						netAmt = shipAccrualRevenue.subtract(shipReversedRevenue0);

						taxAmt = grossAmt.subtract(netAmt);
					} else {
						// 非最后一笔：正常现算，逻辑未变
						int orderLineID = invoiceLine.getC_OrderLine_ID();
						MOrderLine orderLine = new MOrderLine(getCtx(), orderLineID, getTrxName());
						BigDecimal price = orderLine.getPriceActual();
						boolean taxIncluded = orderLine.getParent().isTaxIncluded();
						if (taxIncluded) {
							grossAmt = currentQty.multiply(price).setScale(as.getStdPrecision(), RoundingMode.HALF_UP);
							taxAmt = tax.calculateTax(grossAmt, true, as.getStdPrecision());
							netAmt = grossAmt.subtract(taxAmt);
						} else {
							netAmt = currentQty.multiply(price).setScale(as.getStdPrecision(), RoundingMode.HALF_UP);
							taxAmt = tax.calculateTax(netAmt, false, as.getStdPrecision());
							grossAmt = netAmt.add(taxAmt);
						}
					}

					// 写入本发票行自己的字段
					// 供未来"本行重新过账"/"本行被红冲"直接读取，不再用 calculateTax 反推
					invoiceLine.set_ValueOfColumn("AccrualReversedQty", currentQty);// 已冲暂估数量
					invoiceLine.set_ValueOfColumn("AccrualReversedAmt", grossAmt);// 已冲暂估金额
					invoiceLine.set_ValueOfColumn("AccrualReversedRevenue", netAmt);// 已冲暂估主营收入
					invoiceLine.set_ValueOfColumn("AccrualShipLine_ID", shipLineID);// 发票关联的发货明细行
					invoiceLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);// 标志位
					invoiceLine.saveEx(getTrxName());

					// 更新发货行：正常过账，两个池子（含税总额、收入）分别独立累加
					BigDecimal shipReversedAmt = getBD(shipLine, "AccrualReversedAmt");
					BigDecimal shipReversedRevenue = getBD(shipLine, "AccrualReversedRevenue");
					shipLine.set_ValueOfColumn("AccrualReversedQty", shipReversedQty.add(currentQty));
					shipLine.set_ValueOfColumn("AccrualReversedAmt", shipReversedAmt.add(grossAmt));
					shipLine.set_ValueOfColumn("AccrualReversedRevenue", shipReversedRevenue.add(netAmt));
					shipLine.saveEx(getTrxName());
				} else {
					// 重新过账：直接读本行已锁定的 grossAmt/netAmt 两个快照，taxAmt 用减法得出，
					// 不再调用 calculateTax 反推——避免 true-up 那一笔重新过账时被反推成错误的拆分
					currentQty = getBD(invoiceLine, "AccrualReversedQty");
					grossAmt = getBD(invoiceLine, "AccrualReversedAmt");
					netAmt = getBD(invoiceLine, "AccrualReversedRevenue");
					taxAmt = grossAmt.subtract(netAmt);
				}

				totalGrossAmt = totalGrossAmt.add(grossAmt);
				BigDecimal curTax = taxAmtByTaxID.get(taxID);
				taxAmtByTaxID.put(taxID, curTax == null ? taxAmt : curTax.add(taxAmt));
				anyAccrualLine = docLine;

				FactLine cr = fact.createLine(docLine, revenueAcct, as.getC_Currency_ID(), null, netAmt.negate());
				if (cr != null) {
					cr.setDescription("应收开票-冲减主营业务收入");
				}

			} else {
				// ============ 红冲发票行（qty<0） ============
				if (!isAccrualApplied) {
					// 第一次处理：直接读红冲行自己已复制的两个快照（含税总额、收入），
					// taxAmt 用减法得出，不再反推——这一步和正常行重新过账的原则完全一致
					currentQty = getBD(invoiceLine, "AccrualReversedQty");
					grossAmt = getBD(invoiceLine, "AccrualReversedAmt");
					netAmt = getBD(invoiceLine, "AccrualReversedRevenue");
					if (currentQty.signum() <= 0) {
						invoiceLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);
						invoiceLine.saveEx(getTrxName());
						continue;
					}  
					taxAmt = grossAmt.subtract(netAmt);

					invoiceLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);
					invoiceLine.saveEx(getTrxName());

					// 更新发货行：红冲过账，两个池子分别独立减去（归还额度）
					BigDecimal shipReversedQty = getBD(shipLine, "AccrualReversedQty");
					BigDecimal shipReversedAmt = getBD(shipLine, "AccrualReversedAmt");
					BigDecimal shipReversedRevenue = getBD(shipLine, "AccrualReversedRevenue");
					shipLine.set_ValueOfColumn("AccrualReversedQty", shipReversedQty.subtract(currentQty));
					shipLine.set_ValueOfColumn("AccrualReversedAmt", shipReversedAmt.subtract(grossAmt));
					shipLine.set_ValueOfColumn("AccrualReversedRevenue", shipReversedRevenue.subtract(netAmt));
					shipLine.saveEx(getTrxName());
				} else {
					// 重新过账：不再触碰发货行，同样直接读两个快照，taxAmt 用减法得出
					currentQty = getBD(invoiceLine, "AccrualReversedQty");
					grossAmt = getBD(invoiceLine, "AccrualReversedAmt");
					netAmt = getBD(invoiceLine, "AccrualReversedRevenue");
					taxAmt = grossAmt.subtract(netAmt);
				}

				totalGrossAmt = totalGrossAmt.subtract(grossAmt);
				BigDecimal curTax = taxAmtByTaxID.get(taxID);
				taxAmtByTaxID.put(taxID, curTax == null ? taxAmt.negate() : curTax.subtract(taxAmt));
				anyAccrualLine = docLine;

				FactLine cr = fact.createLine(docLine, revenueAcct, as.getC_Currency_ID(), null, netAmt);
				if (cr != null) {
					cr.setDescription("应收开票红冲-恢复主营业务收入");
				}
			}
		}

		if (anyAccrualLine == null) {
			return;
		}
		// 借：应收账款-暂估 全单合并为一条
		if (totalGrossAmt.signum() != 0) {
			FactLine dr = fact.createLine(null, receivableAccruedAcct, as.getC_Currency_ID(), totalGrossAmt.negate(),
					null);
			if (dr == null) {
				p_Error = Msg.getMsg(getCtx(), "FactLine DR not created for 应收账款-暂估(合并)");
				log.log(Level.WARNING, p_Error);
			} else {  
				dr.setDescription("应收开票-冲减应收账款(暂估)");
			}
		}

		// 借：应收账款-暂估 按税种分组合并
		for (Map.Entry<Integer, BigDecimal> entry : taxAmtByTaxID.entrySet()) {
			BigDecimal taxAmt = entry.getValue();
			if (taxAmt == null || taxAmt.signum() == 0) {
				continue;
			}
			FactLine crTax = fact.createLine(null, taxDuePendingAcct, as.getC_Currency_ID(), null, taxAmt.negate());
			if (crTax != null) {
				crTax.setC_Tax_ID(entry.getKey());
				crTax.setDescription("应收开票-冲减应交税费(待转销项税额)");
			}
		}
	}

	/**
	 * 通用辅助方法：从未重新生成模型类的自定义 BigDecimal 列中取值，null 安全，兜底为 ZERO。
	 */
	private BigDecimal getBD(PO po, String columnName) {
		BigDecimal bd = (BigDecimal) po.get_Value(columnName);
		if (bd == null) {
			return BigDecimal.ZERO;
		}  
		return bd;
	}  
}