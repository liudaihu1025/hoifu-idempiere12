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

/** Generated Model for C_PaymentLine
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_PaymentLine")
public class X_C_PaymentLine extends PO implements I_C_PaymentLine, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260316L;

    /** Standard Constructor */
    public X_C_PaymentLine (Properties ctx, int C_PaymentLine_ID, String trxName)
    {
      super (ctx, C_PaymentLine_ID, trxName);
      /** if (C_PaymentLine_ID == 0)
        {
			setAmount (Env.ZERO);
			setBillAmt (Env.ZERO);
			setC_Currency_ID (0);
			setC_PaymentLine_ID (0);
			setChargeAmt (Env.ZERO);
			setConvertedAmt (Env.ZERO);
			setCurrencyRate (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDueAmt (Env.ZERO);
// @SQL=SELECT DueAmt FROM C_Payment WHERE C_Payment_ID=@C_Payment_ID@
			setEndorsementAmt (Env.ZERO);
			setIsOverrideCurrencyRate (false);
// N
			setLineNo (0);
// @SQL=SELECT COALESCE(MAX(LineNo),0)+10 AS DefaultValue FROM C_PaymentLine WHERE C_Payment_ID=@C_Payment_ID@
			setOverUnderAmt (Env.ZERO);
			setPayAmt (Env.ZERO);
			setProcessed (false);
// N
			setRemainingAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTenderType (null);
			setWriteOffAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentLine (Properties ctx, int C_PaymentLine_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_PaymentLine_ID, trxName, virtualColumns);
      /** if (C_PaymentLine_ID == 0)
        {
			setAmount (Env.ZERO);
			setBillAmt (Env.ZERO);
			setC_Currency_ID (0);
			setC_PaymentLine_ID (0);
			setChargeAmt (Env.ZERO);
			setConvertedAmt (Env.ZERO);
			setCurrencyRate (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDueAmt (Env.ZERO);
// @SQL=SELECT DueAmt FROM C_Payment WHERE C_Payment_ID=@C_Payment_ID@
			setEndorsementAmt (Env.ZERO);
			setIsOverrideCurrencyRate (false);
// N
			setLineNo (0);
// @SQL=SELECT COALESCE(MAX(LineNo),0)+10 AS DefaultValue FROM C_PaymentLine WHERE C_Payment_ID=@C_Payment_ID@
			setOverUnderAmt (Env.ZERO);
			setPayAmt (Env.ZERO);
			setProcessed (false);
// N
			setRemainingAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTenderType (null);
			setWriteOffAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentLine (Properties ctx, String C_PaymentLine_UU, String trxName)
    {
      super (ctx, C_PaymentLine_UU, trxName);
      /** if (C_PaymentLine_UU == null)
        {
			setAmount (Env.ZERO);
			setBillAmt (Env.ZERO);
			setC_Currency_ID (0);
			setC_PaymentLine_ID (0);
			setChargeAmt (Env.ZERO);
			setConvertedAmt (Env.ZERO);
			setCurrencyRate (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDueAmt (Env.ZERO);
// @SQL=SELECT DueAmt FROM C_Payment WHERE C_Payment_ID=@C_Payment_ID@
			setEndorsementAmt (Env.ZERO);
			setIsOverrideCurrencyRate (false);
// N
			setLineNo (0);
// @SQL=SELECT COALESCE(MAX(LineNo),0)+10 AS DefaultValue FROM C_PaymentLine WHERE C_Payment_ID=@C_Payment_ID@
			setOverUnderAmt (Env.ZERO);
			setPayAmt (Env.ZERO);
			setProcessed (false);
// N
			setRemainingAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTenderType (null);
			setWriteOffAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentLine (Properties ctx, String C_PaymentLine_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_PaymentLine_UU, trxName, virtualColumns);
      /** if (C_PaymentLine_UU == null)
        {
			setAmount (Env.ZERO);
			setBillAmt (Env.ZERO);
			setC_Currency_ID (0);
			setC_PaymentLine_ID (0);
			setChargeAmt (Env.ZERO);
			setConvertedAmt (Env.ZERO);
			setCurrencyRate (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDueAmt (Env.ZERO);
// @SQL=SELECT DueAmt FROM C_Payment WHERE C_Payment_ID=@C_Payment_ID@
			setEndorsementAmt (Env.ZERO);
			setIsOverrideCurrencyRate (false);
// N
			setLineNo (0);
// @SQL=SELECT COALESCE(MAX(LineNo),0)+10 AS DefaultValue FROM C_PaymentLine WHERE C_Payment_ID=@C_Payment_ID@
			setOverUnderAmt (Env.ZERO);
			setPayAmt (Env.ZERO);
			setProcessed (false);
// N
			setRemainingAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTenderType (null);
			setWriteOffAmt (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_C_PaymentLine (Properties ctx, ResultSet rs, String trxName)
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
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_C_PaymentLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Account No.
		@param AccountNo Account Number
	*/
	public void setAccountNo (String AccountNo)
	{
		set_Value (COLUMNNAME_AccountNo, AccountNo);
	}

	/** Get Account No.
		@return Account Number
	  */
	public String getAccountNo()
	{
		return (String)get_Value(COLUMNNAME_AccountNo);
	}

	/** Set Amount.
		@param Amount Amount in a defined currency
	*/
	public void setAmount (BigDecimal Amount)
	{
		set_Value (COLUMNNAME_Amount, Amount);
	}

	/** Get Amount.
		@return Amount in a defined currency
	  */
	public BigDecimal getAmount()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Amount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set BillAmt.
		@param BillAmt BillAmt
	*/
	public void setBillAmt (BigDecimal BillAmt)
	{
		set_Value (COLUMNNAME_BillAmt, BillAmt);
	}

	/** Get BillAmt.
		@return BillAmt	  */
	public BigDecimal getBillAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BillAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set BillPackageNo.
		@param BillPackageNo BillPackageNo
	*/
	public void setBillPackageNo (String BillPackageNo)
	{
		set_Value (COLUMNNAME_BillPackageNo, BillPackageNo);
	}

	/** Get BillPackageNo.
		@return BillPackageNo	  */
	public String getBillPackageNo()
	{
		return (String)get_Value(COLUMNNAME_BillPackageNo);
	}

	/** Set BillRate.
		@param BillRate BillRate
	*/
	public void setBillRate (BigDecimal BillRate)
	{
		set_Value (COLUMNNAME_BillRate, BillRate);
	}

	/** Get BillRate.
		@return BillRate	  */
	public BigDecimal getBillRate()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BillRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set BillType.
		@param BillType BillType
	*/
	public void setBillType (String BillType)
	{
		set_Value (COLUMNNAME_BillType, BillType);
	}

	/** Get BillType.
		@return BillType	  */
	public String getBillType()
	{
		return (String)get_Value(COLUMNNAME_BillType);
	}

	/** &#24050;&#36148;&#29616; = C */
	public static final String BUSINESSSTATUS_已贴现 = "C";
	/** &#24050;&#32972;&#20070; = D */
	public static final String BUSINESSSTATUS_已背书 = "D";
	/** &#32972;&#20070;&#20013; = E */
	public static final String BUSINESSSTATUS_背书中 = "E";
	/** &#36148;&#29616;&#20013; = G */
	public static final String BUSINESSSTATUS_贴现中 = "G";
	/** &#24050;&#31614;&#25910; = H */
	public static final String BUSINESSSTATUS_已签收 = "H";
	/** &#24050;&#32467;&#28165; = L */
	public static final String BUSINESSSTATUS_已结清 = "L";
	/** &#25105;&#26041;&#25298;&#20184; = M */
	public static final String BUSINESSSTATUS_我方拒付 = "M";
	/** &#24050;&#31614;&#21457; = P */
	public static final String BUSINESSSTATUS_已签发 = "P";
	/** &#24050;&#36864;&#22238; = R */
	public static final String BUSINESSSTATUS_已退回 = "R";
	/** &#32467;&#31639;&#20013; = S */
	public static final String BUSINESSSTATUS_结算中 = "S";
	/** &#23545;&#26041;&#25298;&#20184; = V */
	public static final String BUSINESSSTATUS_对方拒付 = "V";
	/** Set BusinessStatus.
		@param BusinessStatus BusinessStatus
	*/
	public void setBusinessStatus (String BusinessStatus)
	{

		set_Value (COLUMNNAME_BusinessStatus, BusinessStatus);
	}

	/** Get BusinessStatus.
		@return BusinessStatus	  */
	public String getBusinessStatus()
	{
		return (String)get_Value(COLUMNNAME_BusinessStatus);
	}

	public org.compiere.model.I_C_BankAccount getC_BP_BankAccount() throws RuntimeException
	{
		return (org.compiere.model.I_C_BankAccount)MTable.get(getCtx(), org.compiere.model.I_C_BankAccount.Table_ID)
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

	public org.compiere.model.I_C_BankAccount getC_BankAccount() throws RuntimeException
	{
		return (org.compiere.model.I_C_BankAccount)MTable.get(getCtx(), org.compiere.model.I_C_BankAccount.Table_ID)
			.getPO(getC_BankAccount_ID(), get_TrxName());
	}

	/** Set Bank Account.
		@param C_BankAccount_ID Account at the Bank
	*/
	public void setC_BankAccount_ID (int C_BankAccount_ID)
	{
		if (C_BankAccount_ID < 1)
			set_Value (COLUMNNAME_C_BankAccount_ID, null);
		else
			set_Value (COLUMNNAME_C_BankAccount_ID, Integer.valueOf(C_BankAccount_ID));
	}

	/** Get Bank Account.
		@return Account at the Bank
	  */
	public int getC_BankAccount_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankAccount_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Bill_Pool getC_Bill_Pool() throws RuntimeException
	{
		return (I_C_Bill_Pool)MTable.get(getCtx(), I_C_Bill_Pool.Table_ID)
			.getPO(getC_Bill_Pool_ID(), get_TrxName());
	}

	/** Set C_Bill_Pool.
		@param C_Bill_Pool_ID C_Bill_Pool
	*/
	public void setC_Bill_Pool_ID (int C_Bill_Pool_ID)
	{
		if (C_Bill_Pool_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Bill_Pool_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Bill_Pool_ID, Integer.valueOf(C_Bill_Pool_ID));
	}

	/** Get C_Bill_Pool.
		@return C_Bill_Pool	  */
	public int getC_Bill_Pool_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Bill_Pool_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_ConversionType getC_ConversionType() throws RuntimeException
	{
		return (org.compiere.model.I_C_ConversionType)MTable.get(getCtx(), org.compiere.model.I_C_ConversionType.Table_ID)
			.getPO(getC_ConversionType_ID(), get_TrxName());
	}

	/** Set Currency Type.
		@param C_ConversionType_ID Currency Conversion Rate Type
	*/
	public void setC_ConversionType_ID (int C_ConversionType_ID)
	{
		if (C_ConversionType_ID < 1)
			set_Value (COLUMNNAME_C_ConversionType_ID, null);
		else
			set_Value (COLUMNNAME_C_ConversionType_ID, Integer.valueOf(C_ConversionType_ID));
	}

	/** Get Currency Type.
		@return Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ConversionType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
	{
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_ID)
			.getPO(getC_Currency_ID(), get_TrxName());
	}

	/** Set Currency.
		@param C_Currency_ID The Currency for this record
	*/
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1)
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_PaymentLine.
		@param C_PaymentLine_ID C_PaymentLine
	*/
	public void setC_PaymentLine_ID (int C_PaymentLine_ID)
	{
		if (C_PaymentLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_PaymentLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_PaymentLine_ID, Integer.valueOf(C_PaymentLine_ID));
	}

	/** Get C_PaymentLine.
		@return C_PaymentLine	  */
	public int getC_PaymentLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PaymentLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_PaymentLine_UU.
		@param C_PaymentLine_UU C_PaymentLine_UU
	*/
	public void setC_PaymentLine_UU (String C_PaymentLine_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_PaymentLine_UU, C_PaymentLine_UU);
	}

	/** Get C_PaymentLine_UU.
		@return C_PaymentLine_UU	  */
	public String getC_PaymentLine_UU()
	{
		return (String)get_Value(COLUMNNAME_C_PaymentLine_UU);
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

	/** Set Charge amount.
		@param ChargeAmt Charge Amount
	*/
	public void setChargeAmt (BigDecimal ChargeAmt)
	{
		set_Value (COLUMNNAME_ChargeAmt, ChargeAmt);
	}

	/** Get Charge amount.
		@return Charge Amount
	  */
	public BigDecimal getChargeAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ChargeAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Converted Amount.
		@param ConvertedAmt Converted Amount
	*/
	public void setConvertedAmt (BigDecimal ConvertedAmt)
	{
		set_Value (COLUMNNAME_ConvertedAmt, ConvertedAmt);
	}

	/** Get Converted Amount.
		@return Converted Amount
	  */
	public BigDecimal getConvertedAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ConvertedAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Rate.
		@param CurrencyRate Currency Conversion Rate
	*/
	public void setCurrencyRate (BigDecimal CurrencyRate)
	{
		set_Value (COLUMNNAME_CurrencyRate, CurrencyRate);
	}

	/** Get Rate.
		@return Currency Conversion Rate
	  */
	public BigDecimal getCurrencyRate()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CurrencyRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Discount Amount.
		@param DiscountAmt Calculated amount of discount
	*/
	public void setDiscountAmt (BigDecimal DiscountAmt)
	{
		set_Value (COLUMNNAME_DiscountAmt, DiscountAmt);
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

	/** Set Amount due.
		@param DueAmt Amount of the payment due
	*/
	public void setDueAmt (BigDecimal DueAmt)
	{
		set_Value (COLUMNNAME_DueAmt, DueAmt);
	}

	/** Get Amount due.
		@return Amount of the payment due
	  */
	public BigDecimal getDueAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DueAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Endorsee_Id AD_Reference_ID=200175 */
	public static final int ENDORSEE_ID_AD_Reference_ID=200175;
	/** Set Endorsee_Id.
		@param Endorsee_Id Endorsee_Id
	*/
	public void setEndorsee_Id (String Endorsee_Id)
	{

		set_Value (COLUMNNAME_Endorsee_Id, Endorsee_Id);
	}

	/** Get Endorsee_Id.
		@return Endorsee_Id	  */
	public String getEndorsee_Id()
	{
		return (String)get_Value(COLUMNNAME_Endorsee_Id);
	}

	/** Set EndorsementAmt.
		@param EndorsementAmt EndorsementAmt
	*/
	public void setEndorsementAmt (BigDecimal EndorsementAmt)
	{
		set_Value (COLUMNNAME_EndorsementAmt, EndorsementAmt);
	}

	/** Get EndorsementAmt.
		@return EndorsementAmt	  */
	public BigDecimal getEndorsementAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_EndorsementAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set EndorsementDate.
		@param EndorsementDate EndorsementDate
	*/
	public void setEndorsementDate (Timestamp EndorsementDate)
	{
		set_Value (COLUMNNAME_EndorsementDate, EndorsementDate);
	}

	/** Get EndorsementDate.
		@return EndorsementDate	  */
	public Timestamp getEndorsementDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_EndorsementDate);
	}

	/** Endorser_Id AD_Reference_ID=200175 */
	public static final int ENDORSER_ID_AD_Reference_ID=200175;
	/** Set Endorser_Id.
		@param Endorser_Id Endorser_Id
	*/
	public void setEndorser_Id (String Endorser_Id)
	{

		set_Value (COLUMNNAME_Endorser_Id, Endorser_Id);
	}

	/** Get Endorser_Id.
		@return Endorser_Id	  */
	public String getEndorser_Id()
	{
		return (String)get_Value(COLUMNNAME_Endorser_Id);
	}

	/** Set Override Currency Conversion Rate.
		@param IsOverrideCurrencyRate Override Currency Conversion Rate
	*/
	public void setIsOverrideCurrencyRate (boolean IsOverrideCurrencyRate)
	{
		set_Value (COLUMNNAME_IsOverrideCurrencyRate, Boolean.valueOf(IsOverrideCurrencyRate));
	}

	/** Get Override Currency Conversion Rate.
		@return Override Currency Conversion Rate
	  */
	public boolean isOverrideCurrencyRate()
	{
		Object oo = get_Value(COLUMNNAME_IsOverrideCurrencyRate);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Line.
		@param LineNo Line No
	*/
	public void setLineNo (int LineNo)
	{
		set_Value (COLUMNNAME_LineNo, Integer.valueOf(LineNo));
	}

	/** Get Line.
		@return Line No
	  */
	public int getLineNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LineNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set MaturityDate.
		@param MaturityDate MaturityDate
	*/
	public void setMaturityDate (Timestamp MaturityDate)
	{
		set_Value (COLUMNNAME_MaturityDate, MaturityDate);
	}

	/** Get MaturityDate.
		@return MaturityDate	  */
	public Timestamp getMaturityDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_MaturityDate);
	}

	/** Set Original Transaction ID.
		@param Orig_TrxID Original Transaction ID
	*/
	public void setOrig_TrxID (String Orig_TrxID)
	{
		set_ValueNoCheck (COLUMNNAME_Orig_TrxID, Orig_TrxID);
	}

	/** Get Original Transaction ID.
		@return Original Transaction ID
	  */
	public String getOrig_TrxID()
	{
		return (String)get_Value(COLUMNNAME_Orig_TrxID);
	}

	/** Set Over/Under Payment.
		@param OverUnderAmt Over-Payment (unallocated) or Under-Payment (partial payment) Amount
	*/
	public void setOverUnderAmt (BigDecimal OverUnderAmt)
	{
		set_Value (COLUMNNAME_OverUnderAmt, OverUnderAmt);
	}

	/** Get Over/Under Payment.
		@return Over-Payment (unallocated) or Under-Payment (partial payment) Amount
	  */
	public BigDecimal getOverUnderAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_OverUnderAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Payment amount.
		@param PayAmt Amount being paid
	*/
	public void setPayAmt (BigDecimal PayAmt)
	{
		set_Value (COLUMNNAME_PayAmt, PayAmt);
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

	/** Set Remaining Amt.
		@param RemainingAmt Remaining Amount
	*/
	public void setRemainingAmt (BigDecimal RemainingAmt)
	{
		set_Value (COLUMNNAME_RemainingAmt, RemainingAmt);
	}

	/** Get Remaining Amt.
		@return Remaining Amount
	  */
	public BigDecimal getRemainingAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_RemainingAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Routing No.
		@param RoutingNo Bank Routing Number
	*/
	public void setRoutingNo (String RoutingNo)
	{
		set_Value (COLUMNNAME_RoutingNo, RoutingNo);
	}

	/** Get Routing No.
		@return Bank Routing Number
	  */
	public String getRoutingNo()
	{
		return (String)get_Value(COLUMNNAME_RoutingNo);
	}

	/** Set SubPackageAmt.
		@param SubPackageAmt SubPackageAmt
	*/
	public void setSubPackageAmt (BigDecimal SubPackageAmt)
	{
		set_Value (COLUMNNAME_SubPackageAmt, SubPackageAmt);
	}

	/** Get SubPackageAmt.
		@return SubPackageAmt	  */
	public BigDecimal getSubPackageAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_SubPackageAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** TenderType AD_Reference_ID=214 */
	public static final int TENDERTYPE_AD_Reference_ID=214;
	/** &#30005;&#27719; = A */
	public static final String TENDERTYPE_电汇 = "A";
	/** &#38134;&#34892;&#25215;&#20817;&#27719;&#31080; = B */
	public static final String TENDERTYPE_银行承兑汇票 = "B";
	/** &#20449;&#29992;&#21345; = C */
	public static final String TENDERTYPE_信用卡 = "C";
	/** &#38134;&#34892;&#25187;&#27454; = D */
	public static final String TENDERTYPE_银行扣款 = "D";
	/** &#21830;&#19994;&#25215;&#20817;&#27719;&#31080; = E */
	public static final String TENDERTYPE_商业承兑汇票 = "E";
	/** &#37329;&#21333; = G */
	public static final String TENDERTYPE_金单 = "G";
	/** &#25903;&#31080; = K */
	public static final String TENDERTYPE_支票 = "K";
	/** &#28151;&#21512;&#25903;&#20184; = M */
	public static final String TENDERTYPE_混合支付 = "M";
	/** &#20869;&#37096;&#36134;&#25143; = T */
	public static final String TENDERTYPE_内部账户 = "T";
	/** &#29616;&#37329; = X */
	public static final String TENDERTYPE_现金 = "X";
	/** Set Tender type.
		@param TenderType Method of Payment
	*/
	public void setTenderType (String TenderType)
	{

		set_Value (COLUMNNAME_TenderType, TenderType);
	}

	/** Get Tender type.
		@return Method of Payment
	  */
	public String getTenderType()
	{
		return (String)get_Value(COLUMNNAME_TenderType);
	}

	/** Set Write-off Amount.
		@param WriteOffAmt Amount to write-off
	*/
	public void setWriteOffAmt (BigDecimal WriteOffAmt)
	{
		set_Value (COLUMNNAME_WriteOffAmt, WriteOffAmt);
	}

	/** Get Write-off Amount.
		@return Amount to write-off
	  */
	public BigDecimal getWriteOffAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WriteOffAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}