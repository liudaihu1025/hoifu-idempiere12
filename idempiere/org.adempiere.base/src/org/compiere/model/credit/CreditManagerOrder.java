/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.compiere.model.credit;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.CreditStatus;
import org.adempiere.base.ICreditManager;
import org.compiere.model.MBPartner;
import org.compiere.model.MConversionRate;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MSysConfig;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Credit Status Management for Order
 * 
 * @author Logilite Technologies
 * @since  June 25, 2023
 */
public class CreditManagerOrder implements ICreditManager
{

	private MOrder order;

	/**
	 * Order Credit Manager Load Constructor
	 * 
	 * @param po MOrder
	 */
	public CreditManagerOrder(MOrder po)
	{
		this.order = po;
	}

	@Override
	public CreditStatus checkCreditStatus(String docAction)
	{
		String errorMsg = null;
		if (MOrder.DOCACTION_Prepare.equals(docAction) && order.isSOTrx())
		{
			Properties ctx = order.getCtx();
			MDocType dt = MDocType.get(ctx, order.getC_DocTypeTarget_ID());
			if (MDocType.DOCSUBTYPESO_POSOrder.equals(dt.getDocSubTypeSO())
					&& MOrder.PAYMENTRULE_Cash.equals(order.getPaymentRule())
					&& !MSysConfig.getBooleanValue(MSysConfig.CHECK_CREDIT_ON_CASH_POS_ORDER, true, order.getAD_Client_ID(), order.getAD_Org_ID()))
			{
				// ignore -- don't validate for Cash POS Orders depending on sysconfig parameter
			}
			else if (MDocType.DOCSUBTYPESO_PrepayOrder.equals(dt.getDocSubTypeSO())
					&& !MSysConfig.getBooleanValue(MSysConfig.CHECK_CREDIT_ON_PREPAY_ORDER, true, order.getAD_Client_ID(), order.getAD_Org_ID()))
			{
				// ignore -- don't validate Prepay Orders depending on sysconfig parameter
			}
			else
			{
				// bill bp is guaranteed on beforeSave
				MBPartner bp = new MBPartner(ctx, order.getBill_BPartner_ID(), order.get_TrxName());
				// IDEMPIERE-365 - just check credit if is going to increase the debt
				if (order.getGrandTotal().signum() > 0)
				{
					if (MBPartner.SOCREDITSTATUS_CustomCreditCheck.equals(bp.getSOCreditStatus()))
					{
						// 'A' status: use effective credit limit (SO_CreditLimit + valid TempCreditLimit)
						// SO_CreditLimit=0 is also enforced (means zero credit, not unlimited)
						BigDecimal effectiveLimit = bp.getEffectiveCreditLimit();
						BigDecimal grandTotal = MConversionRate.convertBase(ctx, order.getGrandTotal(), order.getC_Currency_ID(),
								order.getDateOrdered(), order.getC_ConversionType_ID(),
								order.getAD_Client_ID(), order.getAD_Org_ID());
						BigDecimal creditUsed = bp.getSO_CreditUsed();
						if (creditUsed == null)
							creditUsed = Env.ZERO;
						if (grandTotal != null && creditUsed.add(grandTotal).compareTo(effectiveLimit) > 0)
						{
							errorMsg = "@BPartnerOverOCreditHold@ - @SO_CreditUsed@=" + creditUsed
									+ ", @GrandTotal@=" + grandTotal
									+ ", @SO_CreditLimit@=" + effectiveLimit;
						}
					}
					else
					{
						if (MBPartner.SOCREDITSTATUS_CreditStop.equals(bp.getSOCreditStatus()))
						{
							errorMsg = "@BPartnerCreditStop@ - @TotalOpenBalance@=" + bp.getTotalOpenBalance()
									+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
						}
						if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus()))
						{
							errorMsg = "@BPartnerCreditHold@ - @TotalOpenBalance@=" + bp.getTotalOpenBalance()
									+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
						}

						BigDecimal grandTotal = MConversionRate.convertBase(ctx, order.getGrandTotal(), order.getC_Currency_ID(),
								order.getDateOrdered(), order.getC_ConversionType_ID(),
								order.getAD_Client_ID(), order.getAD_Org_ID());
						if (MBPartner.SOCREDITSTATUS_CreditHold.equals(bp.getSOCreditStatus(grandTotal)))
						{
							errorMsg = "@BPartnerOverOCreditHold@ - @TotalOpenBalance@=" + bp.getTotalOpenBalance()
									+ ", @GrandTotal@=" + grandTotal
									+ ", @SO_CreditLimit@=" + bp.getSO_CreditLimit();
						}
					}
				}
			}
		}
		else if (MOrder.DOCACTION_Complete.equals(docAction) && order.isSOTrx())
		{
			// 'A' status: increment SO_CreditUsed when order is completed
			Properties ctx = order.getCtx();
			MBPartner bp = new MBPartner(ctx, order.getBill_BPartner_ID(), order.get_TrxName());
			if (MBPartner.SOCREDITSTATUS_CustomCreditCheck.equals(bp.getSOCreditStatus())
					&& order.getGrandTotal().signum() > 0)
			{
				DB.getDatabase().forUpdate(bp, 0);
				BigDecimal grandTotal = MConversionRate.convertBase(ctx, order.getGrandTotal(), order.getC_Currency_ID(),
						order.getDateOrdered(), order.getC_ConversionType_ID(),
						order.getAD_Client_ID(), order.getAD_Org_ID());
				if (grandTotal != null)
				{
					BigDecimal newCreditUsed = bp.getSO_CreditUsed();
					if (newCreditUsed == null)
						newCreditUsed = grandTotal;
					else
						newCreditUsed = newCreditUsed.add(grandTotal);
					bp.setSO_CreditUsed(newCreditUsed);
					bp.saveEx(order.get_TrxName());
				}
			}
		}
		else if (order.isSOTrx()
				&& (MOrder.DOCACTION_Void.equals(docAction)
				|| MOrder.DOCACTION_Close.equals(docAction)
				|| MOrder.DOCACTION_Reverse_Accrual.equals(docAction)
				|| MOrder.DOCACTION_Reverse_Correct.equals(docAction)))
		{
			// 'A' status: recalculate SO_CreditUsed from scratch when order is voided/closed/reversed
			Properties ctx = order.getCtx();
			MBPartner bp = new MBPartner(ctx, order.getBill_BPartner_ID(), order.get_TrxName());
			if (MBPartner.SOCREDITSTATUS_CustomCreditCheck.equals(bp.getSOCreditStatus()))
			{
				bp.setTotalOpenBalance();
				bp.saveEx(order.get_TrxName());
			}
		}
		return new CreditStatus(errorMsg, !Util.isEmpty(errorMsg));
	} // creditCheck
}
