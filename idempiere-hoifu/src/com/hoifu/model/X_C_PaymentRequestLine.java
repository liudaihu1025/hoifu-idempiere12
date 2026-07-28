/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for C_PaymentRequestLine
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_PaymentRequestLine")
public class X_C_PaymentRequestLine extends PO implements I_C_PaymentRequestLine, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260718L;

    /** Standard Constructor */
    public X_C_PaymentRequestLine (Properties ctx, int C_PaymentRequestLine_ID, String trxName)
    {
      super (ctx, C_PaymentRequestLine_ID, trxName);
      /** if (C_PaymentRequestLine_ID == 0)
        {
			setC_PaymentRequestLine_ID (0);
			setC_PaymentRequest_ID (0);
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setGrandTotal (Env.ZERO);
			setLine (0);
			setLineStatus (null);
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setRequestAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentRequestLine (Properties ctx, int C_PaymentRequestLine_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_PaymentRequestLine_ID, trxName, virtualColumns);
      /** if (C_PaymentRequestLine_ID == 0)
        {
			setC_PaymentRequestLine_ID (0);
			setC_PaymentRequest_ID (0);
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setGrandTotal (Env.ZERO);
			setLine (0);
			setLineStatus (null);
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setRequestAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentRequestLine (Properties ctx, String C_PaymentRequestLine_UU, String trxName)
    {
      super (ctx, C_PaymentRequestLine_UU, trxName);
      /** if (C_PaymentRequestLine_UU == null)
        {
			setC_PaymentRequestLine_ID (0);
			setC_PaymentRequest_ID (0);
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setGrandTotal (Env.ZERO);
			setLine (0);
			setLineStatus (null);
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setRequestAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentRequestLine (Properties ctx, String C_PaymentRequestLine_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_PaymentRequestLine_UU, trxName, virtualColumns);
      /** if (C_PaymentRequestLine_UU == null)
        {
			setC_PaymentRequestLine_ID (0);
			setC_PaymentRequest_ID (0);
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setGrandTotal (Env.ZERO);
			setLine (0);
			setLineStatus (null);
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setRequestAmt (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_C_PaymentRequestLine (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, MTable.getTable_ID(Table_Name), get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_C_PaymentRequestLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set &#24050;&#25209;&#20934;&#37329;&#39069;.
		@param ApprovedAmt &#24050;&#25209;&#20934;&#37329;&#39069;
	*/
	public void setApprovedAmt (BigDecimal ApprovedAmt)
	{
		set_Value (COLUMNNAME_ApprovedAmt, ApprovedAmt);
	}

	/** Get &#24050;&#25209;&#20934;&#37329;&#39069;.
		@return &#24050;&#25209;&#20934;&#37329;&#39069;	  */
	public BigDecimal getApprovedAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ApprovedAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_C_BP_BankAccount getC_BP_BankAccount() throws RuntimeException
	{
		return (org.compiere.model.I_C_BP_BankAccount)MTable.get(getCtx(), org.compiere.model.I_C_BP_BankAccount.Table_ID)
			.getPO(getC_BP_BankAccount_ID(), get_TrxName());
	}

	/** Set Partner Bank Account.
		@param C_BP_BankAccount_ID Bank Account of the Business Partner
	*/
	public void setC_BP_BankAccount_ID (int C_BP_BankAccount_ID)
	{
		if (C_BP_BankAccount_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_BP_BankAccount_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_BP_BankAccount_ID, Integer.valueOf(C_BP_BankAccount_ID));
	}

	/** Get Partner Bank Account.
		@return Bank Account of the Business Partner
	  */
	public int getC_BP_BankAccount_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BP_BankAccount_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Invoice getC_Invoice() throws RuntimeException
	{
		return (org.compiere.model.I_C_Invoice)MTable.get(getCtx(), org.compiere.model.I_C_Invoice.Table_ID)
			.getPO(getC_Invoice_ID(), get_TrxName());
	}

	/** Set Invoice.
		@param C_Invoice_ID Invoice Identifier
	*/
	public void setC_Invoice_ID (int C_Invoice_ID)
	{
		if (C_Invoice_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Invoice_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Invoice_ID, Integer.valueOf(C_Invoice_ID));
	}

	/** Get Invoice.
		@return Invoice Identifier
	  */
	public int getC_Invoice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Invoice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;.
		@param C_PaymentRequestLine_ID &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;
	*/
	public void setC_PaymentRequestLine_ID (int C_PaymentRequestLine_ID)
	{
		if (C_PaymentRequestLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_PaymentRequestLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_PaymentRequestLine_ID, Integer.valueOf(C_PaymentRequestLine_ID));
	}

	/** Get &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;.
		@return &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;	  */
	public int getC_PaymentRequestLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PaymentRequestLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;UUID.
		@param C_PaymentRequestLine_UU &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;UUID
	*/
	public void setC_PaymentRequestLine_UU (String C_PaymentRequestLine_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_PaymentRequestLine_UU, C_PaymentRequestLine_UU);
	}

	/** Get &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;UUID.
		@return &#20184;&#27454;&#30003;&#35831;&#21333;&#34892;UUID	  */
	public String getC_PaymentRequestLine_UU()
	{
		return (String)get_Value(COLUMNNAME_C_PaymentRequestLine_UU);
	}

	public I_C_PaymentRequest getC_PaymentRequest() throws RuntimeException
	{
		return (I_C_PaymentRequest)MTable.get(getCtx(), I_C_PaymentRequest.Table_ID)
			.getPO(getC_PaymentRequest_ID(), get_TrxName());
	}

	/** Set &#20184;&#27454;&#30003;&#35831;&#21333;.
		@param C_PaymentRequest_ID &#20184;&#27454;&#30003;&#35831;&#21333;
	*/
	public void setC_PaymentRequest_ID (int C_PaymentRequest_ID)
	{
		if (C_PaymentRequest_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_PaymentRequest_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_PaymentRequest_ID, Integer.valueOf(C_PaymentRequest_ID));
	}

	/** Get &#20184;&#27454;&#30003;&#35831;&#21333;.
		@return &#20184;&#27454;&#30003;&#35831;&#21333;	  */
	public int getC_PaymentRequest_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PaymentRequest_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Payment getC_Payment() throws RuntimeException
	{
		return (org.compiere.model.I_C_Payment)MTable.get(getCtx(), org.compiere.model.I_C_Payment.Table_ID)
			.getPO(getC_Payment_ID(), get_TrxName());
	}

	/** Set Payment.
		@param C_Payment_ID Payment identifier
	*/
	public void setC_Payment_ID (int C_Payment_ID)
	{
		if (C_Payment_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Payment_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Payment_ID, Integer.valueOf(C_Payment_ID));
	}

	/** Get Payment.
		@return Payment identifier
	  */
	public int getC_Payment_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Payment_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#20986;&#32435;&#24847;&#35265;.
		@param CashierNote &#20986;&#32435;&#24847;&#35265;
	*/
	public void setCashierNote (String CashierNote)
	{
		set_Value (COLUMNNAME_CashierNote, CashierNote);
	}

	/** Get &#20986;&#32435;&#24847;&#35265;.
		@return &#20986;&#32435;&#24847;&#35265;	  */
	public String getCashierNote()
	{
		return (String)get_Value(COLUMNNAME_CashierNote);
	}

	/** Set &#20986;&#32435;&#22791;&#27880;.
		@param CashierRemark &#20986;&#32435;&#22791;&#27880;
	*/
	public void setCashierRemark (String CashierRemark)
	{
		set_Value (COLUMNNAME_CashierRemark, CashierRemark);
	}

	/** Get &#20986;&#32435;&#22791;&#27880;.
		@return &#20986;&#32435;&#22791;&#27880;	  */
	public String getCashierRemark()
	{
		return (String)get_Value(COLUMNNAME_CashierRemark);
	}

	/** Set Date Promised.
		@param DatePromised Date Order was promised
	*/
	public void setDatePromised (Timestamp DatePromised)
	{
		set_ValueNoCheck (COLUMNNAME_DatePromised, DatePromised);
	}

	/** Get Date Promised.
		@return Date Order was promised
	  */
	public Timestamp getDatePromised()
	{
		return (Timestamp)get_Value(COLUMNNAME_DatePromised);
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Difference.
		@param DifferenceAmt Difference Amount
	*/
	public void setDifferenceAmt (BigDecimal DifferenceAmt)
	{
		set_ValueNoCheck (COLUMNNAME_DifferenceAmt, DifferenceAmt);
	}

	/** Get Difference.
		@return Difference Amount
	  */
	public BigDecimal getDifferenceAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DifferenceAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Discount Amount.
		@param DiscountAmt Calculated amount of discount
	*/
	public void setDiscountAmt (BigDecimal DiscountAmt)
	{
		set_ValueNoCheck (COLUMNNAME_DiscountAmt, DiscountAmt);
	}

	/** Get Discount Amount.
		@return Calculated amount of discount
	  */
	public BigDecimal getDiscountAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DiscountAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Due Date.
		@param DueDate Date when the payment is due
	*/
	public void setDueDate (Timestamp DueDate)
	{
		set_Value (COLUMNNAME_DueDate, DueDate);
	}

	/** Get Due Date.
		@return Date when the payment is due
	  */
	public Timestamp getDueDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_DueDate);
	}

	/** Set Grand Total.
		@param GrandTotal Total amount of document
	*/
	public void setGrandTotal (BigDecimal GrandTotal)
	{
		set_ValueNoCheck (COLUMNNAME_GrandTotal, GrandTotal);
	}

	/** Get Grand Total.
		@return Total amount of document
	  */
	public BigDecimal getGrandTotal()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_GrandTotal);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Line No.
		@param Line Unique line for this document
	*/
	public void setLine (int Line)
	{
		set_ValueNoCheck (COLUMNNAME_Line, Integer.valueOf(Line));
	}

	/** Get Line No.
		@return Unique line for this document
	  */
	public int getLine()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Line);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#34892;&#23457;&#25209;&#29366;&#24577;.
		@param LineStatus &#34892;&#23457;&#25209;&#29366;&#24577;
	*/
	public void setLineStatus (String LineStatus)
	{
		set_Value (COLUMNNAME_LineStatus, LineStatus);
	}

	/** Get &#34892;&#23457;&#25209;&#29366;&#24577;.
		@return &#34892;&#23457;&#25209;&#29366;&#24577;	  */
	public String getLineStatus()
	{
		return (String)get_Value(COLUMNNAME_LineStatus);
	}

	/** Set Payment amount.
		@param PayAmt Amount being paid
	*/
	public void setPayAmt (BigDecimal PayAmt)
	{
		set_ValueNoCheck (COLUMNNAME_PayAmt, PayAmt);
	}

	/** Get Payment amount.
		@return Amount being paid
	  */
	public BigDecimal getPayAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PayAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set &#25903;&#20184;&#38134;&#34892;.
		@param PayBank &#25903;&#20184;&#38134;&#34892;
	*/
	public void setPayBank (String PayBank)
	{
		set_Value (COLUMNNAME_PayBank, PayBank);
	}

	/** Get &#25903;&#20184;&#38134;&#34892;.
		@return &#25903;&#20184;&#38134;&#34892;	  */
	public String getPayBank()
	{
		return (String)get_Value(COLUMNNAME_PayBank);
	}

	/** PaymentRule AD_Reference_ID=195 */
	public static final int PAYMENTRULE_AD_Reference_ID=195;
	/** &#29616;&#37329; = B */
	public static final String PAYMENTRULE_现金 = "B";
	/** &#38134;&#34892;&#25187;&#27454; = D */
	public static final String PAYMENTRULE_银行扣款 = "D";
	/** &#20449;&#29992;&#21345; = K */
	public static final String PAYMENTRULE_信用卡 = "K";
	/** POS &#25903;&#20184; = M */
	public static final String PAYMENTRULE_POS支付 = "M";
	/** &#36170;&#38144; = P */
	public static final String PAYMENTRULE_赊销 = "P";
	/** &#27719;&#31080; = S */
	public static final String PAYMENTRULE_汇票 = "S";
	/** &#38134;&#34892;&#27719;&#27454; = T */
	public static final String PAYMENTRULE_银行汇款 = "T";
	/** Set Payment Rule.
		@param PaymentRule How you pay the invoice
	*/
	public void setPaymentRule (String PaymentRule)
	{

		set_Value (COLUMNNAME_PaymentRule, PaymentRule);
	}

	/** Get Payment Rule.
		@return How you pay the invoice
	  */
	public String getPaymentRule()
	{
		return (String)get_Value(COLUMNNAME_PaymentRule);
	}

	/** Set Processed.
		@param Processed The document has been processed
	*/
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed()
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Info.
		@param R_Info Response info
	*/
	public void setR_Info (String R_Info)
	{
		set_ValueNoCheck (COLUMNNAME_R_Info, R_Info);
	}

	/** Get Info.
		@return Response info
	  */
	public String getR_Info()
	{
		return (String)get_Value(COLUMNNAME_R_Info);
	}

	/** Set &#19981;&#33391;&#21697;&#25187;&#27454;/&#36820;&#28857;.
		@param RebateAmt &#19981;&#33391;&#21697;&#25187;&#27454;/&#36820;&#28857;
	*/
	public void setRebateAmt (BigDecimal RebateAmt)
	{
		set_Value (COLUMNNAME_RebateAmt, RebateAmt);
	}

	/** Get &#19981;&#33391;&#21697;&#25187;&#27454;/&#36820;&#28857;.
		@return &#19981;&#33391;&#21697;&#25187;&#27454;/&#36820;&#28857;	  */
	public BigDecimal getRebateAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_RebateAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Request Amount.
		@param RequestAmt Amount associated with this request
	*/
	public void setRequestAmt (BigDecimal RequestAmt)
	{
		set_Value (COLUMNNAME_RequestAmt, RequestAmt);
	}

	/** Get Request Amount.
		@return Amount associated with this request
	  */
	public BigDecimal getRequestAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_RequestAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}