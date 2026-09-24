package com.hoifu.acct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.acct.DocLine_InOut;
import org.compiere.acct.Doc_InOut;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMALine;
import org.compiere.model.MTax;
import org.compiere.model.PO;
import org.compiere.model.ProductCost;
import org.compiere.util.Msg;

import com.hoifu.enums.HFSysConfigEnum;

/**
 * 发货单(M_InOut)暂估应收扩展： 在核心 Doc_InOut 标准 CoGS/Inventory 分录基础上，
 * 针对"收入确认类型=发货确认"的行，追加应收账款-暂估分录。
 */
public class Doc_InOut_Accrual extends Doc_InOut {

	public Doc_InOut_Accrual(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
	}

	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		// 1. 执行标准逻辑：CoGS/Inventory、IG 调拨等分录全部由父类生成
		ArrayList<Fact> facts = super.createFacts(as);
		if (facts == null || facts.isEmpty()) {
			return facts; // 标准逻辑失败或返回空，直接透传
		}

		Fact mainFact = facts.get(0);

		// 发货单追加暂估分录
		if (DOCTYPE_MatShipment.equals(getDocumentType()) && isSOTrx()) {
			appendShipmentAccrualLines(as, mainFact);
		}

		// 客户退货单追加冲减分录
		MInOut io = (MInOut) getPO();
		if (MInOut.MOVEMENTTYPE_CustomerReturns.equals(io.getMovementType())) {
			appendReturnAccrualLines(as, mainFact);
		}


		return facts;
	}

	/**
	 * 遍历发货明细行，追加暂估收入分录：
	 * 
	 */
	private void appendShipmentAccrualLines(MAcctSchema as, Fact fact) {
		// 取"发货确认"这个收入确认类型的 ID，用于和客户/物料上配置的收入确认类型比较
		int shipmentConfirmTypeID = getShipmentConfirmTypeID();

		// 取暂估相关的两个科目：应收账款-暂估、应交税费-待转销项税额
		MAcctSchemaDefault acctDefault = as.getAcctSchemaDefault();
		int receivableAccruedAcctID = acctDefault.get_ValueAsInt("C_Receivable_Accrued_Acct");
		int taxDuePendingAcctID = acctDefault.get_ValueAsInt("T_Due_Pending_Acct");
		if (receivableAccruedAcctID <= 0 || taxDuePendingAcctID <= 0) {
			p_Error = Msg.getMsg(getCtx(), "未配置暂估科目(应收账款-暂估/应交税费-待转销项税额)");
			log.log(Level.WARNING, p_Error);
			return;
		}
		MAccount receivableAccruedAcct = MAccount.get(getCtx(), receivableAccruedAcctID);
		MAccount taxDuePendingAcct = MAccount.get(getCtx(), taxDuePendingAcctID);

		// ---------- 正常行（qty>0）的累加器：应收账款-暂估全单合并、税额按税种分组合并 ----------
		BigDecimal normalTotalGrossAmt = BigDecimal.ZERO;
		Map<Integer, BigDecimal> normalTaxAmtByTaxID = new HashMap<Integer, BigDecimal>();
		DocLine_InOut anyNormalLine = null;

		// ---------- 红冲行（qty<0）的累加器，与正常行分开累计、分开生成独立分录 ----------
		BigDecimal reversalTotalGrossAmt = BigDecimal.ZERO;
		Map<Integer, BigDecimal> reversalTaxAmtByTaxID = new HashMap<Integer, BigDecimal>();
		DocLine_InOut anyReversalLine = null;

		// 逐行遍历发货明细
		for (int i = 0; i < p_lines.length; i++) {
			DocLine_InOut line = (DocLine_InOut) p_lines[i];

			// 取本行对应的 M_InOutLine 模型对象
			MInOutLine ioLine = (MInOutLine) line.getPO();

			// 没有关联销售订单行的发货行（比如非销售场景/手工行），没有单价/税率依据，跳过
			int orderLineID = ioLine.getC_OrderLine_ID();
			if (orderLineID <= 0) {
				continue;
			}

			// 加载销售订单行对象，用于取单价、税率、所属订单
			MOrderLine orderLine = new MOrderLine(getCtx(), orderLineID, getTrxName());

			// 取订单行对应的物料
			MProduct product = MProduct.get(getCtx(), orderLine.getM_Product_ID());
			// 物料取不到，跳过本行
			if (product == null) {
				continue;
			}

			// a. 先取客户的收入确认类型
			MBPartner bp = MBPartner.get(getCtx(), getC_BPartner_ID());
			int revRecogTypeID = bp != null ? bp.get_ValueAsInt("C_RevenueRecognition_ID") : 0;

			// b. 客户为空则取物料的收入确认类型
			if (revRecogTypeID <= 0) {
				revRecogTypeID = product.getC_RevenueRecognition_ID();
			}  

			// c. 仍为空，或不等于"发货确认"，跳过本行，不生成暂估
			if (revRecogTypeID <= 0 || revRecogTypeID != shipmentConfirmTypeID) {
				continue;
			}

			// 把主营业务收入科目的获取与判空校验挪到最前面，任何字段写入/累加之前执行。
			MAccount revenueAcct = line.getAccount(ProductCost.ACCTTYPE_P_Revenue, as);
			if (revenueAcct == null) {
				p_Error = Msg.getMsg(getCtx(), "未取得物料收入科目 P_Revenue_Acct: ") + product.getName();
				log.log(Level.WARNING, p_Error);
				continue;
			}

			// 取该订单所用价目表是否含税，决定后续含税/不含税拆分方式
			boolean taxIncluded = orderLine.getParent().isTaxIncluded();
			// 取订单行单价与税率信息
			BigDecimal priceActual = orderLine.getPriceActual();
			MTax tax = MTax.get(getCtx(), orderLine.getC_Tax_ID());
			int precision = MCurrency.getStdPrecision(getCtx(), as.getC_Currency_ID());

			// 当前这一行的数量
			BigDecimal qty = ioLine.getQtyEntered();
			if (qty == null || qty.signum() == 0) {
				continue;  
			}  

			if (qty.signum() > 0) {
				// ================= 一、正常发货行 =================

				// 本行数量 × 订单单价 现算含税总额
				BigDecimal grossAmt;
				BigDecimal netAmt;
				BigDecimal taxAmt;
				if (taxIncluded) {
					// 价目表含税：qty×price 直接就是含税总额，再反拆不含税净额和税额
					grossAmt = priceActual.multiply(qty).setScale(precision, RoundingMode.HALF_UP);
					taxAmt = tax.calculateTax(grossAmt, true, precision);
					netAmt = grossAmt.subtract(taxAmt);
				} else {
					// 价目表不含税：qty×price 是不含税净额，再算税额、加总为含税总额
					netAmt = priceActual.multiply(qty).setScale(precision, RoundingMode.HALF_UP);
					taxAmt = tax.calculateTax(netAmt, false, precision);
					grossAmt = netAmt.add(taxAmt);
				}

				// 正常行是暂估的"起始行"：直接赋值，天然幂等，不需要标记位
				ioLine.set_ValueOfColumn("AccrualQty", ioLine.getQtyEntered());
				ioLine.set_ValueOfColumn("AccrualAmt", grossAmt);
				ioLine.set_ValueOfColumn("AccrualTax", taxAmt);
				ioLine.set_ValueOfColumn("AccrualRevenue", netAmt);
				ioLine.saveEx(getTrxName());

				// 累加进"正常行"分录汇总器
				normalTotalGrossAmt = normalTotalGrossAmt.add(grossAmt);
				normalTaxAmtByTaxID.merge(orderLine.getC_Tax_ID(), taxAmt, BigDecimal::add);
				anyNormalLine = line;

				// 主营业务收入逐行生成，不合并（与发票行过账一样，不同产品可能挂不同收入科目）
				FactLine crRevenue = fact.createLine(line, revenueAcct, as.getC_Currency_ID(), null, netAmt);
				if (crRevenue != null) {
					crRevenue.setAD_Org_ID(line.getAD_Org_ID());
					crRevenue.setC_BPartner_ID(getC_BPartner_ID());
					crRevenue.setDescription("发货确认-主营业务收入(暂估)");
				}  

			} else {
				// ================= 二、红冲发货行 =================

				// 通过 ReversalLine_ID 定位原始发货行
				int reversalLineID = ioLine.getReversalLine_ID();
				if (reversalLineID <= 0) {
					continue;  
				}  
				MInOutLine originalLine = new MInOutLine(getCtx(), reversalLineID, getTrxName());

				// 红冲行自己的所有暂估相关字段都是红冲那一刻从原始行复制过来的只读快照，这里只读，不写回、不覆盖
				// 暂估数量
				BigDecimal frozenAccrualQty = getBD(ioLine, "AccrualQty");
				// 已冲暂估数量
				BigDecimal frozenReversedQty = getBD(ioLine, "AccrualReversedQty");
				// 未冲暂估数量
				BigDecimal frozenRemainingQty = frozenAccrualQty.subtract(frozenReversedQty);

				// 本次红冲最多能冲多少：min(红冲数量绝对值, 红冲行冻结快照算出的余量)
				// 本次红冲永远是"冲销这批发货当前剩余的全部未冲暂估额度"——确定性的最后一批
				BigDecimal currentQty = qty.abs().min(frozenRemainingQty);
				if (currentQty.signum() <= 0) {
					// 快照余量已经是 0（或红冲数量为 0），没有可冲的暂估，跳过本行
					continue;
				}

				// 解决尾差：不再用 数量*订单单价重新计算，而是直接
				// reversalGrossAmt = AccrualAmt暂估总额 - AccrualReversedAmt已冲暂估金额
				// reversalNetAmt = AccrualRevenue已暂估主营收入 - AccrualReversedRevenue已冲暂估主营收入
				// reversalTaxAmt = reversalGrossAmt - reversalNetAmt（用减法得出，保证三者精确相加，不再引入舍入）
				BigDecimal frozenAccrualAmt = getBD(ioLine, "AccrualAmt");
				BigDecimal frozenReversedAmt = getBD(ioLine, "AccrualReversedAmt");
				BigDecimal reversalGrossAmt = frozenAccrualAmt.subtract(frozenReversedAmt);

				BigDecimal frozenAccrualRevenue = getBD(ioLine, "AccrualRevenue");
				BigDecimal frozenReversedRevenue = getBD(ioLine, "AccrualReversedRevenue");
				BigDecimal reversalNetAmt = frozenAccrualRevenue.subtract(frozenReversedRevenue);

				BigDecimal reversalTaxAmt = reversalGrossAmt.subtract(reversalNetAmt);

				// 读取"是否已经把这笔贡献加到原始行"的标记位（只是一个布尔状态，
				// 不承载任何金额/数量，专门用来防止重新过账时对原始行重复累加）
				boolean isAccrualApplied = ioLine.get_ValueAsBoolean("IsAccrualApplied");

				if (!isAccrualApplied) {
					// 第一次过账：把这笔贡献（数量、含税总额、收入）累加到原始行的共享计数器上
					BigDecimal originalReversedQty = getBD(originalLine, "AccrualReversedQty");
					BigDecimal originalReversedAmt = getBD(originalLine, "AccrualReversedAmt");
					BigDecimal originalReversedRevenue = getBD(originalLine, "AccrualReversedRevenue");
					originalLine.set_ValueOfColumn("AccrualReversedQty", originalReversedQty.add(currentQty));
					originalLine.set_ValueOfColumn("AccrualReversedAmt", originalReversedAmt.add(reversalGrossAmt));
					originalLine.set_ValueOfColumn("AccrualReversedRevenue",
							originalReversedRevenue.add(reversalNetAmt));
					originalLine.saveEx(getTrxName());

					// 红冲行本次写库【只置位标记】，不写任何其它字段，
					// 保持这几个字段作为创建时刻的只读快照不被过账代码污染
					ioLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);
					ioLine.saveEx(getTrxName());
				}
				// 已经应用过：不再触碰原始行，也不再写红冲行任何字段，
				// 直接用刚才根据冻结快照重新算出的 reversalGrossAmt/reversalNetAmt/reversalTaxAmt 重建分录即可

				// 累加进"红冲行"分录汇总器（与正常行分开，独立生成一套完整分录，不合并抵消）
				reversalTotalGrossAmt = reversalTotalGrossAmt.add(reversalGrossAmt);
				reversalTaxAmtByTaxID.merge(orderLine.getC_Tax_ID(), reversalTaxAmt, BigDecimal::add);
				anyReversalLine = line;

				// 主营业务收入逐行生成，不合并；金额取负
				FactLine crRevenue = fact.createLine(line, revenueAcct, as.getC_Currency_ID(), null,
						reversalNetAmt.negate());
				if (crRevenue != null) {
					crRevenue.setAD_Org_ID(line.getAD_Org_ID());
					crRevenue.setC_BPartner_ID(getC_BPartner_ID());
					crRevenue.setDescription("发货红冲-冲减主营业务收入(暂估)");
				}
			}
		}

		// ---------- 正常行：应收账款-暂估全单合并一条（docLine=null） ----------
		if (normalTotalGrossAmt.signum() != 0 && anyNormalLine != null) {
			FactLine accrualDr = fact.createLine(null, receivableAccruedAcct, as.getC_Currency_ID(),
					normalTotalGrossAmt, null);
			if (accrualDr != null) {
				accrualDr.setAD_Org_ID(anyNormalLine.getAD_Org_ID());
				accrualDr.setC_BPartner_ID(getC_BPartner_ID());  
				accrualDr.setDescription("发货确认-应收账款(暂估)");
			}
		}
		// 正常行：应交税费-待转销项税额按税种分组合并
		for (Map.Entry<Integer, BigDecimal> entry : normalTaxAmtByTaxID.entrySet()) {
			BigDecimal taxAmt = entry.getValue();
			if (taxAmt == null || taxAmt.signum() == 0) {
				continue;
			}  
			FactLine accrualCrTax = fact.createLine(null, taxDuePendingAcct, as.getC_Currency_ID(), null, taxAmt);
			if (accrualCrTax != null) {
				accrualCrTax.setC_Tax_ID(entry.getKey());
				accrualCrTax.setAD_Org_ID(anyNormalLine.getAD_Org_ID());
				accrualCrTax.setC_BPartner_ID(getC_BPartner_ID());
				accrualCrTax.setDescription("发货确认-应交税费(待转销项税额)");
			}
		}

		// ---------- 红冲行：应收账款-暂估全单合并一条（docLine=null，金额取负） ----------
		if (reversalTotalGrossAmt.signum() != 0 && anyReversalLine != null) {
			FactLine accrualDr = fact.createLine(null, receivableAccruedAcct, as.getC_Currency_ID(),
					reversalTotalGrossAmt.negate(), null);
			if (accrualDr != null) {
				accrualDr.setAD_Org_ID(anyReversalLine.getAD_Org_ID());
				accrualDr.setC_BPartner_ID(getC_BPartner_ID());
				accrualDr.setDescription("发货红冲-冲减应收账款(暂估)");
			}
		}
		// 红冲行：应交税费-待转销项税额按税种分组合并（金额取负）
		for (Map.Entry<Integer, BigDecimal> entry : reversalTaxAmtByTaxID.entrySet()) {
			BigDecimal taxAmt = entry.getValue();
			if (taxAmt == null || taxAmt.signum() == 0) {
				continue;
			}  
			FactLine crTax = fact.createLine(null, taxDuePendingAcct, as.getC_Currency_ID(), null, taxAmt.negate());
			if (crTax != null) {
				crTax.setC_Tax_ID(entry.getKey());
				crTax.setAD_Org_ID(anyReversalLine.getOrder_Org_ID());
				crTax.setC_BPartner_ID(getC_BPartner_ID());
				crTax.setDescription("发货红冲-冲减应交税费(待转销项税额)");
			}
		}
	}

	/**
	 * 客户退货单：冲减暂估收入分录。
	 */  
	private void appendReturnAccrualLines(MAcctSchema as, Fact fact) {
		// 取暂估相关的两个科目：应收账款-暂估、应交税费-待转销项税额
		MAcctSchemaDefault acctDefault = as.getAcctSchemaDefault();  
		int receivableAccruedAcctID = acctDefault.get_ValueAsInt("C_Receivable_Accrued_Acct");  
		int taxDuePendingAcctID = acctDefault.get_ValueAsInt("T_Due_Pending_Acct");  
		if (receivableAccruedAcctID <= 0 || taxDuePendingAcctID <= 0) {  
			log.severe("未配置暂估科目 C_Receivable_Accrued_Acct/T_Due_Pending_Acct，跳过退货冲暂估分录生成");  
			return;  
		}  
		MAccount receivableAccruedAcct = MAccount.get(getCtx(), receivableAccruedAcctID);  
		MAccount taxDuePendingAcct = MAccount.get(getCtx(), taxDuePendingAcctID);  

		// 全单合并：应收账款-暂估 —— 正常行、红冲行分开累计，独立生成分录，不互相抵消
		BigDecimal normalTotalGrossAmt = BigDecimal.ZERO;
		BigDecimal reversalTotalGrossAmt = BigDecimal.ZERO;
		// 按税种合并：应交税费-待转销项税额 —— 正常行、红冲行分开累计
		Map<Integer, BigDecimal> normalTaxAmtByTaxID = new HashMap<Integer, BigDecimal>();
		Map<Integer, BigDecimal> reversalTaxAmtByTaxID = new HashMap<Integer, BigDecimal>();
		// 记录本轮参与冲减的任意一行（正常/红冲各自记一条），供合并分录取组织/客户上下文
		DocLine_InOut anyNormalLine = null;
		DocLine_InOut anyReversalLine = null;

		for (int i = 0; i < p_lines.length; i++) {  
			DocLine_InOut line = (DocLine_InOut) p_lines[i];  
			MInOutLine returnLine = (MInOutLine) line.getPO();

			if (returnLine.getM_RMALine_ID() <= 0) {
				log.warning("退货行未关联 M_RMALine，跳过冲暂估: M_InOutLine_ID=" + returnLine.get_ID());
				continue;  
			}  
			// 退货授权明细行：正常行第一次过账时写入快照；红冲行只读该快照，永不回写
			MRMALine rmaLine = new MRMALine(getCtx(), returnLine.getM_RMALine_ID(), getTrxName());
			MInOutLine shipLine = rmaLine.getShipLine();
			if (shipLine == null || shipLine.getC_OrderLine_ID() <= 0) {
				log.warning("退货行对应原发货明细无关联销售订单行，跳过冲暂估: M_InOutLine_ID=" + returnLine.get_ID());
				continue;  
			}  

			MOrderLine orderLine = new MOrderLine(getCtx(), shipLine.getC_OrderLine_ID(), getTrxName());
			int stdPrecision = MCurrency.getStdPrecision(getCtx(), orderLine.getC_Currency_ID());

			BigDecimal qty = returnLine.getMovementQty();
			if (qty == null || qty.signum() == 0) {
				continue;  
			}  

			BigDecimal priceActual = orderLine.getPriceActual();  
			int taxID = orderLine.getC_Tax_ID();  
			MTax tax = MTax.get(getCtx(), taxID);  

			// 守卫标记：读取退货行自身的 IsAccrualApplied字段，只在第一次过账是更新原发货明细行的相关字段
			boolean isAccrualApplied = returnLine.get_ValueAsBoolean("IsAccrualApplied");

			MProduct product = line.getProduct();
			MAccount revenueAcct = line.getAccount(ProductCost.ACCTTYPE_P_Revenue, as);  
			if (revenueAcct == null) {  
				p_Error = Msg.getMsg(getCtx(), "未取得物料收入科目 P_Revenue_Acct: ")
						+ (product != null ? product.getName() : "");
				log.log(Level.WARNING, p_Error);  
				continue;  
			}  

			if (qty.signum() > 0) {
				// ============ 一、正常退货行（qty>0） ============
				BigDecimal currentQty;
				BigDecimal grossAmt;
				BigDecimal netAmt;
				BigDecimal taxAmt;

				if (!isAccrualApplied) {
					// 第一次过账：现算 本次可冲暂估数量 = min(本行数量绝对值, 发货行剩余可冲额度)，做封顶保护
					BigDecimal shipAccrualQty = getBD(shipLine, "AccrualQty");
					BigDecimal shipReversedQtyOld = getBD(shipLine, "AccrualReversedQty");
					BigDecimal remainingQty = shipAccrualQty.subtract(shipReversedQtyOld);
					// 读取组织级策略：A=优先冲暂估应收，R=优先冲正式应收
					String reversalType = HFSysConfigEnum.HF_ACCRUED_RECEIVABLE_REVERSAL_TYPE
							.getValue(getAD_Client_ID(), returnLine.getAD_Org_ID());

					BigDecimal qtyForAccrual = qty.abs();
					if ("R".equals(reversalType)) {
						// 优先冲正式应收：先用订单行已开票数量 QtyInvoiced 抵扣退货数量，
						// 只有超出已开票数量的部分（说明已开票额度不够抵扣，多出来的这部分才需要冲暂估）才走暂估冲减
						BigDecimal qtyInvoiced = orderLine.getQtyInvoiced();
						qtyForAccrual = qty.abs().subtract(qtyInvoiced).max(BigDecimal.ZERO);
					}
					currentQty = qtyForAccrual.min(remainingQty);
					if (currentQty.signum() <= 0) {
						// 暂估池已被冲完，本行不再生成冲暂估分录（多出部分走标准正式冲销逻辑，与本方法无关）

						// 置位 IsAccrualApplied，防止重新过账时因为 shipLine/订单行状态发生变化而重新算出不同的结果，破坏幂等性
						returnLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);
						returnLine.saveEx(getTrxName());
						continue;
					}

					// 尾差判断：当前可冲暂估数量 = 发货单行可冲暂估剩余量，说明这一笔是把发货行暂估池吃满的直接取金额/收入差值，
					if (remainingQty.compareTo(currentQty) == 0) {
						BigDecimal shipAccrualAmt = getBD(shipLine, "AccrualAmt");
						BigDecimal shipReversedAmtOldForTrueUp = getBD(shipLine, "AccrualReversedAmt");
						grossAmt = shipAccrualAmt.subtract(shipReversedAmtOldForTrueUp);

						// netAmt 直接用"暂估总收入-已冲收入累计"的精确差值算出
						BigDecimal shipAccrualRevenue = getBD(shipLine, "AccrualRevenue");
						BigDecimal shipReversedRevenueOldForTrueUp = getBD(shipLine, "AccrualReversedRevenue");
						netAmt = shipAccrualRevenue.subtract(shipReversedRevenueOldForTrueUp);
						// 税额用减法得出，保证 netAmt+taxAmt 精确等于 grossAmt
						taxAmt = grossAmt.subtract(netAmt);
					} else {
						// 未命中：还可能有其它发票/退货继续消耗，按本次可冲暂估数量现算
						if (orderLine.getParent().isTaxIncluded()) {
							grossAmt = currentQty.multiply(priceActual).setScale(stdPrecision, RoundingMode.HALF_UP);
							// 含税场景：直接用系统标准方法从含税总额反推税额
							taxAmt = tax.calculateTax(grossAmt, true, stdPrecision);
							netAmt = grossAmt.subtract(taxAmt);
						} else {
							netAmt = currentQty.multiply(priceActual).setScale(stdPrecision, RoundingMode.HALF_UP);
							taxAmt = tax.calculateTax(netAmt, false, stdPrecision);
							grossAmt = netAmt.add(taxAmt);
						}
					}
					if (grossAmt.signum() == 0) {
						continue;
					}

					// 二、更新退货授权明细行的字段：写一次，作为后续红冲/重新过账的固定快照，不再变
					rmaLine.set_ValueOfColumn("AccrualReversedQty", currentQty);
					rmaLine.set_ValueOfColumn("AccrualReversedAmt", grossAmt);
					// 把本次冲销的净额也存成快照，供后续重新过账/红冲直接读取，不再反推
					rmaLine.set_ValueOfColumn("AccrualReversedRevenue", netAmt);
					rmaLine.saveEx(getTrxName());

					// 一、更新发货明细行的字段：正常行用加法累加已冲暂估数量/金额/收入
					BigDecimal shipReversedAmtOld = getBD(shipLine, "AccrualReversedAmt");
					BigDecimal shipReversedRevenueOld = getBD(shipLine, "AccrualReversedRevenue");
					shipLine.set_ValueOfColumn("AccrualReversedQty", shipReversedQtyOld.add(currentQty));
					shipLine.set_ValueOfColumn("AccrualReversedAmt", shipReversedAmtOld.add(grossAmt));
					shipLine.set_ValueOfColumn("AccrualReversedRevenue", shipReversedRevenueOld.add(netAmt));
					shipLine.saveEx(getTrxName());

					// 标记本退货行已应用，防止重新过账时对发货行重复累加
					returnLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);
					returnLine.saveEx(getTrxName());
				} else {
					// 重新过账：不再写任何字段，直接读 M_RMALine 已锁定存储的快照值重建分录
					currentQty = getBD(rmaLine, "AccrualReversedQty");
					grossAmt = getBD(rmaLine, "AccrualReversedAmt");
					if (grossAmt.signum() == 0) {
						continue;
					}
					// netAmt 直接读快照，不再用 calculateTax 反推——
					// 避免对 true-up 那一笔重新反推出与首次不一致的错误拆分
					netAmt = getBD(rmaLine, "AccrualReversedRevenue");
					taxAmt = grossAmt.subtract(netAmt);
				}

				if (anyNormalLine == null) {
					anyNormalLine = line;
				}

				// 贷：主营业务收入（不含税），金额取负数 —— 逐行生成，不合并
				FactLine crRevenue = fact.createLine(line, revenueAcct, as.getC_Currency_ID(), null, netAmt.negate());
				if (crRevenue != null) {
					crRevenue.setAD_Org_ID(line.getOrder_Org_ID());
					crRevenue.setC_BPartner_ID(getC_BPartner_ID());
					crRevenue.setDescription("退货-冲减主营业务收入(暂估)");
				}

				// 累加：应收账款-暂估、应交税费-待转销项税额（正常行独立合并）
				normalTotalGrossAmt = normalTotalGrossAmt.add(grossAmt);
				if (taxAmt.signum() != 0) {
					Integer taxKey = Integer.valueOf(taxID);
					BigDecimal existing = normalTaxAmtByTaxID.get(taxKey);
					if (existing == null) {
						existing = BigDecimal.ZERO;
					}
					normalTaxAmtByTaxID.put(taxKey, existing.add(taxAmt));
				}

			} else {
				// ============ 二、红冲退货行（qty<0） ============
				// 直接读取 M_RMALine 上的原值，不重新走 min() 计算
				BigDecimal currentQty = getBD(rmaLine, "AccrualReversedQty");
				BigDecimal grossAmt = getBD(rmaLine, "AccrualReversedAmt");
				if (currentQty.signum() <= 0 || grossAmt.signum() == 0) {
					// 当初正常行没有产生任何冲暂估（比如暂估池已被冲完），红冲自然也不需要生成分录
					continue;
				}

				BigDecimal netAmt = getBD(rmaLine, "AccrualReversedRevenue");
				BigDecimal taxAmt = grossAmt.subtract(netAmt);

				if (!isAccrualApplied) {
					// 一、更新发货明细行的字段：红冲用减法，把之前正常退货贡献的额度恢复回去
					BigDecimal shipReversedQtyOld = getBD(shipLine, "AccrualReversedQty");
					BigDecimal shipReversedAmtOld = getBD(shipLine, "AccrualReversedAmt");
					BigDecimal shipReversedRevenueOld = getBD(shipLine, "AccrualReversedRevenue");
					shipLine.set_ValueOfColumn("AccrualReversedQty", shipReversedQtyOld.subtract(currentQty));
					shipLine.set_ValueOfColumn("AccrualReversedAmt", shipReversedAmtOld.subtract(grossAmt));
					// 发货行同步扣减"已冲收入"这个共享池维度
					shipLine.set_ValueOfColumn("AccrualReversedRevenue", shipReversedRevenueOld.subtract(netAmt));
					shipLine.saveEx(getTrxName());

					// 标记本红冲退货行已应用，防止重新过账时对发货行重复扣减
					returnLine.set_ValueOfColumn("IsAccrualApplied", Boolean.TRUE);
					returnLine.saveEx(getTrxName());

					// 注意：M_RMALine.AccrualReversedQty/AccrualReversedAmt/AccrualReversedRevenue
					// 不做任何修改，它是"当初正常退货冲了多少暂估"的历史事实快照，不因为后续被红冲而改写
				}
				// 重新过账（isAccrualApplied=true）：不再写任何字段，仅用同一份原值重建分录

				if (anyReversalLine == null) {
					anyReversalLine = line;
				}

				// 贷：主营业务收入（不含税）——红冲方向相反，金额取正数（与正常行方向相反）
				FactLine crRevenue = fact.createLine(line, revenueAcct, as.getC_Currency_ID(), null, netAmt);
				if (crRevenue != null) {
					crRevenue.setAD_Org_ID(line.getOrder_Org_ID());
					crRevenue.setC_BPartner_ID(getC_BPartner_ID());
					crRevenue.setDescription("退货红冲-恢复主营业务收入(暂估)");
				}

				// 累加：应收账款-暂估、应交税费-待转销项税额（红冲行独立合并，不与正常行相加抵消）
				reversalTotalGrossAmt = reversalTotalGrossAmt.add(grossAmt);
				if (taxAmt.signum() != 0) {
					Integer taxKey = Integer.valueOf(taxID);
					BigDecimal existing = reversalTaxAmtByTaxID.get(taxKey);
					if (existing == null) {
						existing = BigDecimal.ZERO;
					}
					reversalTaxAmtByTaxID.put(taxKey, existing.add(taxAmt));
				}
			}  
		}

		// ---- 正常行：合并分录 ----
		if (anyNormalLine != null) {
			// 借：应收账款-暂估，金额取负数 —— 正常行全单合并为一条
			if (normalTotalGrossAmt.signum() != 0) {
				FactLine dr = fact.createLine(null, receivableAccruedAcct, as.getC_Currency_ID(),
						normalTotalGrossAmt.negate(), null);
				if (dr == null) {
					p_Error = Msg.getMsg(getCtx(), "FactLine DR not created(应收账款-暂估合并-正常)");
					log.log(Level.WARNING, p_Error);
				} else {
					dr.setAD_Org_ID(anyNormalLine.getOrder_Org_ID());
					dr.setC_BPartner_ID(getC_BPartner_ID());
					dr.setDescription("退货-冲减应收账款(暂估)");
				}
			}  
			// 贷：应交税费-待转销项税额，金额取负数 —— 正常行按税种分组合并
			for (Map.Entry<Integer, BigDecimal> entry : normalTaxAmtByTaxID.entrySet()) {
				BigDecimal taxAmt = entry.getValue();
				if (taxAmt == null || taxAmt.signum() == 0) {
					continue;
				}
				FactLine crTax = fact.createLine(null, taxDuePendingAcct, as.getC_Currency_ID(), null, taxAmt.negate());
				if (crTax != null) {
					crTax.setC_Tax_ID(entry.getKey());
					crTax.setAD_Org_ID(anyNormalLine.getOrder_Org_ID());
					crTax.setC_BPartner_ID(getC_BPartner_ID());
					crTax.setDescription("退货-冲减应交税费(待转销项税额)");
				}
			}  
		}  

		// ---- 红冲行：合并分录（方向与正常行相反，金额取正数） ----
		if (anyReversalLine != null) {
			// 借：应收账款-暂估，金额取正数 —— 红冲行全单合并为一条，与正常行方向相反、独立生成
			if (reversalTotalGrossAmt.signum() != 0) {
				FactLine dr = fact.createLine(null, receivableAccruedAcct, as.getC_Currency_ID(), reversalTotalGrossAmt,
						null);
				if (dr == null) {
					p_Error = Msg.getMsg(getCtx(), "FactLine DR not created(应收账款-暂估合并-红冲)");
					log.log(Level.WARNING, p_Error);
				} else {
					dr.setAD_Org_ID(anyReversalLine.getOrder_Org_ID());
					dr.setC_BPartner_ID(getC_BPartner_ID());
					dr.setDescription("退货红冲-恢复应收账款(暂估)");
				}
			}  
			// 贷：应交税费-待转销项税额，金额取正数 —— 红冲行按税种分组合并
			for (Map.Entry<Integer, BigDecimal> entry : reversalTaxAmtByTaxID.entrySet()) {
				BigDecimal taxAmt = entry.getValue();
				if (taxAmt == null || taxAmt.signum() == 0) {
					continue;
				}
				FactLine crTax = fact.createLine(null, taxDuePendingAcct, as.getC_Currency_ID(), null, taxAmt);
				if (crTax != null) {
					crTax.setC_Tax_ID(entry.getKey());
					crTax.setAD_Org_ID(anyReversalLine.getOrder_Org_ID());
					crTax.setC_BPartner_ID(getC_BPartner_ID());
					crTax.setDescription("退货红冲-恢复应交税费(待转销项税额)");
				}
			}  
		}  
	}

	/*
	 * 获取收入确认类型为"发货确认的"ID
	 */
	private int getShipmentConfirmTypeID() {
		return HFSysConfigEnum.HF_REVENUE_RECOGNITION_SHIPMENT_TYPE_ID.getIntValue(getAD_Client_ID(), getAD_Org_ID());
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