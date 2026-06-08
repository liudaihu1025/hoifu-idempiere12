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
package org.compiere.model;

import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for Gl_Voucher_Pending
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="Gl_Voucher_Pending")
public class X_Gl_Voucher_Pending extends PO implements I_Gl_Voucher_Pending, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260511L;

    /** Standard Constructor */
    public X_Gl_Voucher_Pending(Properties ctx, int Gl_Voucher_Pending_ID, String trxName)
    {
      super (ctx, Gl_Voucher_Pending_ID, trxName);
      /** if (Gl_Voucher_Pending_ID == 0)
        {
			setAD_Table_ID (0);
			setC_AcctSchema_ID (0);
			setC_Currency_ID (0);
			setC_Period_ID (0);
			setControlAmt (Env.ZERO);
			setDateAcct (new Timestamp( System.currentTimeMillis() ));
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDescription (null);
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setGL_Category_ID (0);
			setGl_Voucher_Pending_ID (0);
			setIsApproved (false);
// N
			setIsPrinted (false);
// N
			setPosted (false);
// N
			setPostingType (null);
			setProcessed (false);
// N
			setRecord_ID (0);
			setTotalCr (Env.ZERO);
// 0
			setTotalDr (Env.ZERO);
// 0
        } */
    }

    /** Standard Constructor */
    public X_Gl_Voucher_Pending(Properties ctx, int Gl_Voucher_Pending_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, Gl_Voucher_Pending_ID, trxName, virtualColumns);
      /** if (Gl_Voucher_Pending_ID == 0)
        {
			setAD_Table_ID (0);
			setC_AcctSchema_ID (0);
			setC_Currency_ID (0);
			setC_Period_ID (0);
			setControlAmt (Env.ZERO);
			setDateAcct (new Timestamp( System.currentTimeMillis() ));
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDescription (null);
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setGL_Category_ID (0);
			setGl_Voucher_Pending_ID (0);
			setIsApproved (false);
// N
			setIsPrinted (false);
// N
			setPosted (false);
// N
			setPostingType (null);
			setProcessed (false);
// N
			setRecord_ID (0);
			setTotalCr (Env.ZERO);
// 0
			setTotalDr (Env.ZERO);
// 0
        } */
    }

    /** Standard Constructor */
    public X_Gl_Voucher_Pending(Properties ctx, String Gl_Voucher_Pending_UU, String trxName)
    {
      super (ctx, Gl_Voucher_Pending_UU, trxName);
      /** if (Gl_Voucher_Pending_UU == null)
        {
			setAD_Table_ID (0);
			setC_AcctSchema_ID (0);
			setC_Currency_ID (0);
			setC_Period_ID (0);
			setControlAmt (Env.ZERO);
			setDateAcct (new Timestamp( System.currentTimeMillis() ));
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDescription (null);
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setGL_Category_ID (0);
			setGl_Voucher_Pending_ID (0);
			setIsApproved (false);
// N
			setIsPrinted (false);
// N
			setPosted (false);
// N
			setPostingType (null);
			setProcessed (false);
// N
			setRecord_ID (0);
			setTotalCr (Env.ZERO);
// 0
			setTotalDr (Env.ZERO);
// 0
        } */
    }

    /** Standard Constructor */
    public X_Gl_Voucher_Pending(Properties ctx, String Gl_Voucher_Pending_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, Gl_Voucher_Pending_UU, trxName, virtualColumns);
      /** if (Gl_Voucher_Pending_UU == null)
        {
			setAD_Table_ID (0);
			setC_AcctSchema_ID (0);
			setC_Currency_ID (0);
			setC_Period_ID (0);
			setControlAmt (Env.ZERO);
			setDateAcct (new Timestamp( System.currentTimeMillis() ));
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDescription (null);
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setGL_Category_ID (0);
			setGl_Voucher_Pending_ID (0);
			setIsApproved (false);
// N
			setIsPrinted (false);
// N
			setPosted (false);
// N
			setPostingType (null);
			setProcessed (false);
// N
			setRecord_ID (0);
			setTotalCr (Env.ZERO);
// 0
			setTotalDr (Env.ZERO);
// 0
        } */
    }

    /** Load Constructor */
    public X_Gl_Voucher_Pending(Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
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
      StringBuilder sb = new StringBuilder ("X_Gl_Voucher_Pending[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Complete = CO */
	public static final String DOCACTION_Complete = "CO";
	/** Approve = AP */
	public static final String DOCACTION_Approve = "AP";
	/** Reject = RJ */
	public static final String DOCACTION_Reject = "RJ";
	/** Post = PO */
	public static final String DOCACTION_Post = "PO";
	/** Void = VO */
	public static final String DOCACTION_Void = "VO";
	/** Close = CL */
	public static final String DOCACTION_Close = "CL";
	/** Reverse - Correct = RC */
	public static final String DOCACTION_Reverse_Correct = "RC";
	/** Reverse - Accrual = RA */
	public static final String DOCACTION_Reverse_Accrual = "RA";
	/** Invalidate = IN */
	public static final String DOCACTION_Invalidate = "IN";
	/** Re-activate = RE */
	public static final String DOCACTION_Re_Activate = "RE";
	/** <None> = -- */
	public static final String DOCACTION_None = "--";
	/** Prepare = PR */
	public static final String DOCACTION_Prepare = "PR";
	/** Unlock = XL */
	public static final String DOCACTION_Unlock = "XL";
	/** Wait Complete = WC */
	public static final String DOCACTION_WaitComplete = "WC";
	/** Set Document Action.
	 @param DocAction
	 The targeted status of the document
	 */
	public void setDocAction (String DocAction)
	{

		set_Value (COLUMNNAME_DocAction, DocAction);
	}

	/** Get Document Action.
	 @return The targeted status of the document
	 */
	public String getDocAction ()
	{
		return (String)get_Value(COLUMNNAME_DocAction);
	}

	public I_AD_Table getAD_Table() throws RuntimeException
	{
		return (I_AD_Table)MTable.get(getCtx(), I_AD_Table.Table_ID)
			.getPO(getAD_Table_ID(), get_TrxName());
	}

	/** Set Table.
		@param AD_Table_ID Database Table information
	*/
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1)
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_User getAD_User() throws RuntimeException
	{
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
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

	/** Set ApprovedBy.
		@param ApprovedBy ApprovedBy
	*/
	public void setApprovedBy (int ApprovedBy)
	{
		set_Value (COLUMNNAME_ApprovedBy, Integer.valueOf(ApprovedBy));
	}

	/** Get ApprovedBy.
		@return ApprovedBy	  */
	public int getApprovedBy()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ApprovedBy);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AttachmentCount.
		@param AttachmentCount AttachmentCount
	*/
	public void setAttachmentCount (int AttachmentCount)
	{
		set_Value (COLUMNNAME_AttachmentCount, Integer.valueOf(AttachmentCount));
	}

	/** Get AttachmentCount.
		@return AttachmentCount	  */
	public int getAttachmentCount()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AttachmentCount);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_AcctSchema getC_AcctSchema() throws RuntimeException
	{
		return (I_C_AcctSchema)MTable.get(getCtx(), I_C_AcctSchema.Table_ID)
			.getPO(getC_AcctSchema_ID(), get_TrxName());
	}

	/** Set Accounting Schema.
		@param C_AcctSchema_ID Rules for accounting
	*/
	public void setC_AcctSchema_ID (int C_AcctSchema_ID)
	{
		if (C_AcctSchema_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_AcctSchema_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_AcctSchema_ID, Integer.valueOf(C_AcctSchema_ID));
	}

	/** Get Accounting Schema.
		@return Rules for accounting
	  */
	public int getC_AcctSchema_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_AcctSchema_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Currency getC_Currency() throws RuntimeException
	{
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_ID)
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

	public I_C_DocType getC_DocType() throws RuntimeException
	{
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_ID)
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

	public I_C_Period getC_Period() throws RuntimeException
	{
		return (I_C_Period)MTable.get(getCtx(), I_C_Period.Table_ID)
			.getPO(getC_Period_ID(), get_TrxName());
	}

	/** Set Period.
		@param C_Period_ID Period of the Calendar
	*/
	public void setC_Period_ID (int C_Period_ID)
	{
		if (C_Period_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Period_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Period_ID, Integer.valueOf(C_Period_ID));
	}

	/** Get Period.
		@return Period of the Calendar
	  */
	public int getC_Period_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Period_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Control Amount.
		@param ControlAmt If not zero, the Debit amount of the document must be equal this amount
	*/
	public void setControlAmt (BigDecimal ControlAmt)
	{
		set_Value (COLUMNNAME_ControlAmt, ControlAmt);
	}

	/** Get Control Amount.
		@return If not zero, the Debit amount of the document must be equal this amount
	  */
	public BigDecimal getControlAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ControlAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Account Date.
		@param DateAcct Accounting Date
	*/
	public void setDateAcct (Timestamp DateAcct)
	{
		set_ValueNoCheck (COLUMNNAME_DateAcct, DateAcct);
	}

	/** Get Account Date.
		@return Accounting Date
	  */
	public Timestamp getDateAcct()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateAcct);
	}

	/** Set Document Date.
		@param DateDoc Date of the Document
	*/
	public void setDateDoc (Timestamp DateDoc)
	{
		set_Value (COLUMNNAME_DateDoc, DateDoc);
	}

	/** Get Document Date.
		@return Date of the Document
	  */
	public Timestamp getDateDoc()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateDoc);
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

	public I_GL_Category getGL_Category() throws RuntimeException
	{
		return (I_GL_Category)MTable.get(getCtx(), I_GL_Category.Table_ID)
			.getPO(getGL_Category_ID(), get_TrxName());
	}

	/** Set GL Category.
		@param GL_Category_ID General Ledger Category
	*/
	public void setGL_Category_ID (int GL_Category_ID)
	{
		if (GL_Category_ID < 1)
			set_ValueNoCheck (COLUMNNAME_GL_Category_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_GL_Category_ID, Integer.valueOf(GL_Category_ID));
	}

	/** Get GL Category.
		@return General Ledger Category
	  */
	public int getGL_Category_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_GL_Category_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** &#35760; = B */
	public static final String GL_VOUCHERTYPE_记 = "B";
	/** &#20184; = P */
	public static final String GL_VOUCHERTYPE_付 = "P";
	/** &#25910; = R */
	public static final String GL_VOUCHERTYPE_收 = "R";
	/** &#36716; = T */
	public static final String GL_VOUCHERTYPE_转 = "T";
	/** Set Gl_VoucherType.
		@param Gl_VoucherType Gl_VoucherType
	*/
	public void setGl_VoucherType (String Gl_VoucherType)
	{

		set_Value (COLUMNNAME_Gl_VoucherType, Gl_VoucherType);
	}

	/** Get Gl_VoucherType.
		@return Gl_VoucherType	  */
	public String getGl_VoucherType()
	{
		return (String)get_Value(COLUMNNAME_Gl_VoucherType);
	}

	/** Set Gl_Voucher_Pending.
		@param Gl_Voucher_Pending_ID Gl_Voucher_Pending
	*/
	public void setGl_Voucher_Pending_ID (int Gl_Voucher_Pending_ID)
	{
		if (Gl_Voucher_Pending_ID < 1)
			set_ValueNoCheck (COLUMNNAME_Gl_Voucher_Pending_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Gl_Voucher_Pending_ID, Integer.valueOf(Gl_Voucher_Pending_ID));
	}

	/** Get Gl_Voucher_Pending.
		@return Gl_Voucher_Pending	  */
	public int getGl_Voucher_Pending_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Gl_Voucher_Pending_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Gl_Voucher_Pending_UU.
		@param Gl_Voucher_Pending_UU Gl_Voucher_Pending_UU
	*/
	public void setGl_Voucher_Pending_UU (String Gl_Voucher_Pending_UU)
	{
		set_ValueNoCheck (COLUMNNAME_Gl_Voucher_Pending_UU, Gl_Voucher_Pending_UU);
	}

	/** Get Gl_Voucher_Pending_UU.
		@return Gl_Voucher_Pending_UU	  */
	public String getGl_Voucher_Pending_UU()
	{
		return (String)get_Value(COLUMNNAME_Gl_Voucher_Pending_UU);
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

	/** Set Printed.
		@param IsPrinted Indicates if this document / line is printed
	*/
	public void setIsPrinted (boolean IsPrinted)
	{
		set_ValueNoCheck (COLUMNNAME_IsPrinted, Boolean.valueOf(IsPrinted));
	}

	/** Get Printed.
		@return Indicates if this document / line is printed
	  */
	public boolean isPrinted()
	{
		Object oo = get_Value(COLUMNNAME_IsPrinted);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public I_AD_User getPoste() throws RuntimeException
	{
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_ID)
			.getPO(getPostedBy(), get_TrxName());
	}

	/** Set PostedBy.
		@param PostedBy PostedBy
	*/
	public void setPostedBy (int PostedBy)
	{
		set_Value (COLUMNNAME_PostedBy, Integer.valueOf(PostedBy));
	}

	/** Get PostedBy.
		@return PostedBy	  */
	public int getPostedBy()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PostedBy);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** PostingType AD_Reference_ID=125 */
	public static final int POSTINGTYPE_AD_Reference_ID=125;
	/** Actual = A */
	public static final String POSTINGTYPE_Actual = "A";
	/** Budget = B */
	public static final String POSTINGTYPE_Budget = "B";
	/** Commitment = E */
	public static final String POSTINGTYPE_Commitment = "E";
	/** Reservation = R */
	public static final String POSTINGTYPE_Reservation = "R";
	/** Statistical = S */
	public static final String POSTINGTYPE_Statistical = "S";
	/** Set Posting Type.
		@param PostingType The type of posted amount for the transaction
	*/
	public void setPostingType (String PostingType)
	{

		set_Value (COLUMNNAME_PostingType, PostingType);
	}

	/** Get Posting Type.
		@return The type of posted amount for the transaction
	  */
	public String getPostingType()
	{
		return (String)get_Value(COLUMNNAME_PostingType);
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

	/** Set Processed On.
		@param ProcessedOn The date+time (expressed in decimal format) when the document has been processed
	*/
	public void setProcessedOn (BigDecimal ProcessedOn)
	{
		set_Value (COLUMNNAME_ProcessedOn, ProcessedOn);
	}

	/** Get Processed On.
		@return The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ProcessedOn);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Record ID.
		@param Record_ID Direct internal record ID
	*/
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0)
			set_ValueNoCheck (COLUMNNAME_Record_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Record ID.
		@return Direct internal record ID
	  */
	public int getRecord_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Sequence.
		@param SeqNo Method of ordering records; lowest number comes first
	*/
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getSource_DocType() throws RuntimeException
	{
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_ID)
			.getPO(getSource_DocType_ID(), get_TrxName());
	}

	/** Set Source_DocType_ID.
		@param Source_DocType_ID Source_DocType_ID
	*/
	public void setSource_DocType_ID (int Source_DocType_ID)
	{
		if (Source_DocType_ID < 1)
			set_Value (COLUMNNAME_Source_DocType_ID, null);
		else
			set_Value (COLUMNNAME_Source_DocType_ID, Integer.valueOf(Source_DocType_ID));
	}

	/** Get Source_DocType_ID.
		@return Source_DocType_ID	  */
	public int getSource_DocType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Source_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Total Credit.
		@param TotalCr Total Credit in document currency
	*/
	public void setTotalCr (BigDecimal TotalCr)
	{
		set_ValueNoCheck (COLUMNNAME_TotalCr, TotalCr);
	}

	/** Get Total Credit.
		@return Total Credit in document currency
	  */
	public BigDecimal getTotalCr()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalCr);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Total Debit.
		@param TotalDr Total debit in document currency
	*/
	public void setTotalDr (BigDecimal TotalDr)
	{
		set_ValueNoCheck (COLUMNNAME_TotalDr, TotalDr);
	}

	/** Get Total Debit.
		@return Total debit in document currency
	  */
	public BigDecimal getTotalDr()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalDr);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set approvedtime.
		@param approvedtime approvedtime
	*/
	public void setapprovedtime (Timestamp approvedtime)
	{
		set_Value (COLUMNNAME_approvedtime, approvedtime);
	}

	/** Get approvedtime.
		@return approvedtime	  */
	public Timestamp getapprovedtime()
	{
		return (Timestamp)get_Value(COLUMNNAME_approvedtime);
	}
}