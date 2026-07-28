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

/** Generated Model for C_PaymentRequest
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_PaymentRequest")
public class X_C_PaymentRequest extends PO implements I_C_PaymentRequest, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260718L;

    /** Standard Constructor */
    public X_C_PaymentRequest (Properties ctx, int C_PaymentRequest_ID, String trxName)
    {
      super (ctx, C_PaymentRequest_ID, trxName);
      /** if (C_PaymentRequest_ID == 0)
        {
			setApprovedAmt (Env.ZERO);
			setC_BPartner_ID (0);
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setC_PaymentRequest_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setProcessing (false);
			setRebateAmt (Env.ZERO);
			setRequestAmt (Env.ZERO);
			setTotalAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentRequest (Properties ctx, int C_PaymentRequest_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_PaymentRequest_ID, trxName, virtualColumns);
      /** if (C_PaymentRequest_ID == 0)
        {
			setApprovedAmt (Env.ZERO);
			setC_BPartner_ID (0);
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setC_PaymentRequest_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setProcessing (false);
			setRebateAmt (Env.ZERO);
			setRequestAmt (Env.ZERO);
			setTotalAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentRequest (Properties ctx, String C_PaymentRequest_UU, String trxName)
    {
      super (ctx, C_PaymentRequest_UU, trxName);
      /** if (C_PaymentRequest_UU == null)
        {
			setApprovedAmt (Env.ZERO);
			setC_BPartner_ID (0);
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setC_PaymentRequest_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setProcessing (false);
			setRebateAmt (Env.ZERO);
			setRequestAmt (Env.ZERO);
			setTotalAmt (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_PaymentRequest (Properties ctx, String C_PaymentRequest_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_PaymentRequest_UU, trxName, virtualColumns);
      /** if (C_PaymentRequest_UU == null)
        {
			setApprovedAmt (Env.ZERO);
			setC_BPartner_ID (0);
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setC_PaymentRequest_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDifferenceAmt (Env.ZERO);
			setDiscountAmt (Env.ZERO);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setPayAmt (Env.ZERO);
			setPaymentRule (null);
// T
			setProcessed (false);
// N
			setProcessing (false);
			setRebateAmt (Env.ZERO);
			setRequestAmt (Env.ZERO);
			setTotalAmt (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_C_PaymentRequest (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_C_PaymentRequest[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_Value (COLUMNNAME_AD_User_ID, null);
		else
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getC_BPartner_ID(), get_TrxName());
	}

	/** Set &#24448;&#26469;&#21333;&#20301;.
		@param C_BPartner_ID Identifies a Business Partner
	*/
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1)
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get &#24448;&#26469;&#21333;&#20301;.
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_BPartner_Location getC_BPartner_Location() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner_Location)MTable.get(getCtx(), org.compiere.model.I_C_BPartner_Location.Table_ID)
			.getPO(getC_BPartner_Location_ID(), get_TrxName());
	}

	/** Set Partner Location.
		@param C_BPartner_Location_ID Identifies the (ship to) address for this Business Partner
	*/
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID)
	{
		if (C_BPartner_Location_ID < 1)
			set_Value (COLUMNNAME_C_BPartner_Location_ID, null);
		else
			set_Value (COLUMNNAME_C_BPartner_Location_ID, Integer.valueOf(C_BPartner_Location_ID));
	}

	/** Get Partner Location.
		@return Identifies the (ship to) address for this Business Partner
	  */
	public int getC_BPartner_Location_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_Location_ID);
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

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
	{
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_ID)
			.getPO(getC_DocType_ID(), get_TrxName());
	}

	/** Set Document Type.
		@param C_DocType_ID Document type or rules
	*/
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0)
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set &#20184;&#27454;&#30003;&#35831;&#21333;UUID.
		@param C_PaymentRequest_UU &#20184;&#27454;&#30003;&#35831;&#21333;UUID
	*/
	public void setC_PaymentRequest_UU (String C_PaymentRequest_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_PaymentRequest_UU, C_PaymentRequest_UU);
	}

	/** Get &#20184;&#27454;&#30003;&#35831;&#21333;UUID.
		@return &#20184;&#27454;&#30003;&#35831;&#21333;UUID	  */
	public String getC_PaymentRequest_UU()
	{
		return (String)get_Value(COLUMNNAME_C_PaymentRequest_UU);
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

	/** Set Transaction Date.
		@param DateTrx Transaction Date
	*/
	public void setDateTrx (Timestamp DateTrx)
	{
		set_Value (COLUMNNAME_DateTrx, DateTrx);
	}

	/** Get Transaction Date.
		@return Transaction Date
	  */
	public Timestamp getDateTrx()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateTrx);
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

	/** DocAction AD_Reference_ID=135 */
	public static final int DOCACTION_AD_Reference_ID=135;
	/** &lt;None&gt; = -- */
	public static final String DOCACTION_None = "--";
	/** Approve = AP */
	public static final String DOCACTION_Approve = "AP";
	/** Close = CL */
	public static final String DOCACTION_Close = "CL";
	/** Complete = CO */
	public static final String DOCACTION_Complete = "CO";
	/** Invalidate = IN */
	public static final String DOCACTION_Invalidate = "IN";
	/** Post = PO */
	public static final String DOCACTION_Post = "PO";
	/** Prepare = PR */
	public static final String DOCACTION_Prepare = "PR";
	/** Reverse - Accrual = RA */
	public static final String DOCACTION_Reverse_Accrual = "RA";
	/** Reverse - Correct = RC */
	public static final String DOCACTION_Reverse_Correct = "RC";
	/** Re-activate = RE */
	public static final String DOCACTION_Re_Activate = "RE";
	/** Reject = RJ */
	public static final String DOCACTION_Reject = "RJ";
	/** Void = VO */
	public static final String DOCACTION_Void = "VO";
	/** Wait Complete = WC */
	public static final String DOCACTION_WaitComplete = "WC";
	/** Unlock = XL */
	public static final String DOCACTION_Unlock = "XL";
	/** Set Document Action.
		@param DocAction The targeted status of the document
	*/
	public void setDocAction (String DocAction)
	{

		set_Value (COLUMNNAME_DocAction, DocAction);
	}

	/** Get Document Action.
		@return The targeted status of the document
	  */
	public String getDocAction()
	{
		return (String)get_Value(COLUMNNAME_DocAction);
	}

	/** DocStatus AD_Reference_ID=131 */
	public static final int DOCSTATUS_AD_Reference_ID=131;
	/** Unknown = ?? */
	public static final String DOCSTATUS_Unknown = "??";
	/** Approved = AP */
	public static final String DOCSTATUS_Approved = "AP";
	/** Closed = CL */
	public static final String DOCSTATUS_Closed = "CL";
	/** Completed = CO */
	public static final String DOCSTATUS_Completed = "CO";
	/** Drafted = DR */
	public static final String DOCSTATUS_Drafted = "DR";
	/** Invalid = IN */
	public static final String DOCSTATUS_Invalid = "IN";
	/** In Progress = IP */
	public static final String DOCSTATUS_InProgress = "IP";
	/** Not Approved = NA */
	public static final String DOCSTATUS_NotApproved = "NA";
	/** Reversed = RE */
	public static final String DOCSTATUS_Reversed = "RE";
	/** Voided = VO */
	public static final String DOCSTATUS_Voided = "VO";
	/** Waiting Confirmation = WC */
	public static final String DOCSTATUS_WaitingConfirmation = "WC";
	/** Waiting Payment = WP */
	public static final String DOCSTATUS_WaitingPayment = "WP";
	/** Set Document Status.
		@param DocStatus The current status of the document
	*/
	public void setDocStatus (String DocStatus)
	{

		set_Value (COLUMNNAME_DocStatus, DocStatus);
	}

	/** Get Document Status.
		@return The current status of the document
	  */
	public String getDocStatus()
	{
		return (String)get_Value(COLUMNNAME_DocStatus);
	}

	/** Set Document No.
		@param DocumentNo Document sequence number of the document
	*/
	public void setDocumentNo (String DocumentNo)
	{
		set_ValueNoCheck (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo()
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	/** Set Approved.
		@param IsApproved Indicates if this document requires approval
	*/
	public void setIsApproved (boolean IsApproved)
	{
		set_ValueNoCheck (COLUMNNAME_IsApproved, Boolean.valueOf(IsApproved));
	}

	/** Get Approved.
		@return Indicates if this document requires approval
	  */
	public boolean isApproved()
	{
		Object oo = get_Value(COLUMNNAME_IsApproved);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set OA&#21333;&#21495;.
		@param OAID OA&#21333;&#21495;
	*/
	public void setOAID (String OAID)
	{
		set_Value (COLUMNNAME_OAID, OAID);
	}

	/** Get OA&#21333;&#21495;.
		@return OA&#21333;&#21495;	  */
	public String getOAID()
	{
		return (String)get_Value(COLUMNNAME_OAID);
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

	/** &#36153;&#29992;&#25253;&#38144; = E */
	public static final String PAYMENTCATEGORY_费用报销 = "E";
	/** &#35774;&#22791;&#31867; = F */
	public static final String PAYMENTCATEGORY_设备类 = "F";
	/** &#36824;&#27454;&#19982;&#21033;&#24687; = I */
	public static final String PAYMENTCATEGORY_还款与利息 = "I";
	/** &#39044;&#20184;&#27454;/&#23450;&#37329; = P */
	public static final String PAYMENTCATEGORY_预付款定金 = "P";
	/** &#34218;&#36164;&#25903;&#20184; = S */
	public static final String PAYMENTCATEGORY_薪资支付 = "S";
	/** &#31246;&#36153;&#25903;&#20184; = T */
	public static final String PAYMENTCATEGORY_税费支付 = "T";
	/** &#20379;&#24212;&#21830; = V */
	public static final String PAYMENTCATEGORY_供应商 = "V";
	/** Set &#20184;&#27454;&#31867;&#22411;.
		@param PaymentCategory &#20184;&#27454;&#31867;&#22411;
	*/
	public void setPaymentCategory (String PaymentCategory)
	{

		set_Value (COLUMNNAME_PaymentCategory, PaymentCategory);
	}

	/** Get &#20184;&#27454;&#31867;&#22411;.
		@return &#20184;&#27454;&#31867;&#22411;	  */
	public String getPaymentCategory()
	{
		return (String)get_Value(COLUMNNAME_PaymentCategory);
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

	/** Set Process Now.
		@param Processing Process Now
	*/
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing()
	{
		Object oo = get_Value(COLUMNNAME_Processing);
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
		set_Value (COLUMNNAME_R_Info, R_Info);
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

	/** Set Total Amount.
		@param TotalAmt Total Amount
	*/
	public void setTotalAmt (BigDecimal TotalAmt)
	{
		set_ValueNoCheck (COLUMNNAME_TotalAmt, TotalAmt);
	}

	/** Get Total Amount.
		@return Total Amount
	  */
	public BigDecimal getTotalAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set paymentmonth.
		@param paymentmonth paymentmonth
	*/
	public void setpaymentmonth (String paymentmonth)
	{
		set_Value (COLUMNNAME_paymentmonth, paymentmonth);
	}

	/** Get paymentmonth.
		@return paymentmonth	  */
	public String getpaymentmonth()
	{
		return (String)get_Value(COLUMNNAME_paymentmonth);
	}
}