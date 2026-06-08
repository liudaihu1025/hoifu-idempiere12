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

/** Generated Model for C_CreditLimitChange
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_CreditLimitChange")
public class X_C_CreditLimitChange extends PO implements I_C_CreditLimitChange, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260422L;

    /** Standard Constructor */
    public X_C_CreditLimitChange (Properties ctx, int C_CreditLimitChange_ID, String trxName)
    {
      super (ctx, C_CreditLimitChange_ID, trxName);
      /** if (C_CreditLimitChange_ID == 0)
        {
			setC_BPartner_ID (0);
			setC_CreditLimitChange_ID (0);
			setC_DocType_ID (0);
// @SQL=select C_DocType_ID from C_DocType where C_DocType.Name='信用额度变更单'
			setCreditLimitChange (Env.ZERO);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
			setTempCreditLimitValidFrom (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_C_CreditLimitChange (Properties ctx, int C_CreditLimitChange_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_CreditLimitChange_ID, trxName, virtualColumns);
      /** if (C_CreditLimitChange_ID == 0)
        {
			setC_BPartner_ID (0);
			setC_CreditLimitChange_ID (0);
			setC_DocType_ID (0);
// @SQL=select C_DocType_ID from C_DocType where C_DocType.Name='信用额度变更单'
			setCreditLimitChange (Env.ZERO);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
			setTempCreditLimitValidFrom (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_C_CreditLimitChange (Properties ctx, String C_CreditLimitChange_UU, String trxName)
    {
      super (ctx, C_CreditLimitChange_UU, trxName);
      /** if (C_CreditLimitChange_UU == null)
        {
			setC_BPartner_ID (0);
			setC_CreditLimitChange_ID (0);
			setC_DocType_ID (0);
// @SQL=select C_DocType_ID from C_DocType where C_DocType.Name='信用额度变更单'
			setCreditLimitChange (Env.ZERO);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
			setTempCreditLimitValidFrom (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_C_CreditLimitChange (Properties ctx, String C_CreditLimitChange_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_CreditLimitChange_UU, trxName, virtualColumns);
      /** if (C_CreditLimitChange_UU == null)
        {
			setC_BPartner_ID (0);
			setC_CreditLimitChange_ID (0);
			setC_DocType_ID (0);
// @SQL=select C_DocType_ID from C_DocType where C_DocType.Name='信用额度变更单'
			setCreditLimitChange (Env.ZERO);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
			setTempCreditLimitValidFrom (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_C_CreditLimitChange (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_C_CreditLimitChange[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set ApplyReason.
		@param ApplyReason ApplyReason
	*/
	public void setApplyReason (String ApplyReason)
	{
		set_Value (COLUMNNAME_ApplyReason, ApplyReason);
	}

	/** Get ApplyReason.
		@return ApplyReason	  */
	public String getApplyReason()
	{
		return (String)get_Value(COLUMNNAME_ApplyReason);
	}

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getC_BPartner_ID(), get_TrxName());
	}

	/** Set Business Partner.
		@param C_BPartner_ID Identifies a Business Partner
	*/
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner.
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_CreditLimitChange.
		@param C_CreditLimitChange_ID C_CreditLimitChange
	*/
	public void setC_CreditLimitChange_ID (int C_CreditLimitChange_ID)
	{
		if (C_CreditLimitChange_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_CreditLimitChange_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_CreditLimitChange_ID, Integer.valueOf(C_CreditLimitChange_ID));
	}

	/** Get C_CreditLimitChange.
		@return C_CreditLimitChange	  */
	public int getC_CreditLimitChange_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_CreditLimitChange_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_CreditLimitChange_UU.
		@param C_CreditLimitChange_UU C_CreditLimitChange_UU
	*/
	public void setC_CreditLimitChange_UU (String C_CreditLimitChange_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_CreditLimitChange_UU, C_CreditLimitChange_UU);
	}

	/** Get C_CreditLimitChange_UU.
		@return C_CreditLimitChange_UU	  */
	public String getC_CreditLimitChange_UU()
	{
		return (String)get_Value(COLUMNNAME_C_CreditLimitChange_UU);
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

	/** Set CreditLimitChange.
		@param CreditLimitChange CreditLimitChange
	*/
	public void setCreditLimitChange (BigDecimal CreditLimitChange)
	{
		set_Value (COLUMNNAME_CreditLimitChange, CreditLimitChange);
	}

	/** Get CreditLimitChange.
		@return CreditLimitChange	  */
	public BigDecimal getCreditLimitChange()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CreditLimitChange);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set SO_CreditLimit_Now.
		@param SO_CreditLimit_Now SO_CreditLimit_Now
	*/
	public void setSO_CreditLimit_Now (BigDecimal SO_CreditLimit_Now)
	{
		throw new IllegalArgumentException ("SO_CreditLimit_Now is virtual column");	}

	/** Get SO_CreditLimit_Now.
		@return SO_CreditLimit_Now	  */
	public BigDecimal getSO_CreditLimit_Now()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_SO_CreditLimit_Now);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set SO_CreditLimit_Old.
	 @param SO_CreditLimit_Old SO_CreditLimit_Old
	 */
	public void setSO_CreditLimit_Old (BigDecimal SO_CreditLimit_Old)
	{
		set_Value (COLUMNNAME_SO_CreditLimit_Old, SO_CreditLimit_Old);	}

	/** Get SO_CreditLimit_Old.
	 @return SO_CreditLimit_Old	  */
	public BigDecimal getSO_CreditLimit_Old()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_SO_CreditLimit_Old);
		if (bd == null)
			return Env.ZERO;
		return bd;
	}


	public void setTempCreditLimit_Old (BigDecimal TempCreditLimit_Old)
	{
		set_Value (COLUMNNAME_TempCreditLimit_Old, TempCreditLimit_Old);	}


	public BigDecimal getTempCreditLimit_Old()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TempCreditLimit_Old);
		if (bd == null)
			return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_AD_User getSalesRep() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getSalesRep_ID(), get_TrxName());
	}

	/** Set Sales Representative.
		@param SalesRep_ID Sales Representative or Company Agent
	*/
	public void setSalesRep_ID (int SalesRep_ID)
	{
		if (SalesRep_ID < 1)
			set_Value (COLUMNNAME_SalesRep_ID, null);
		else
			set_Value (COLUMNNAME_SalesRep_ID, Integer.valueOf(SalesRep_ID));
	}

	/** Get Sales Representative.
		@return Sales Representative or Company Agent
	  */
	public int getSalesRep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesRep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set TempCreditLimitChange.
		@param TempCreditLimitChange TempCreditLimitChange
	*/
	public void setTempCreditLimitChange (BigDecimal TempCreditLimitChange)
	{
		set_Value (COLUMNNAME_TempCreditLimitChange, TempCreditLimitChange);
	}

	/** Get TempCreditLimitChange.
		@return TempCreditLimitChange	  */
	public BigDecimal getTempCreditLimitChange()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TempCreditLimitChange);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#29983;&#25928;&#26085;&#26399;.
		@param TempCreditLimitValidFrom &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#29983;&#25928;&#26085;&#26399;
	*/
	public void setTempCreditLimitValidFrom (Timestamp TempCreditLimitValidFrom)
	{
		set_Value (COLUMNNAME_TempCreditLimitValidFrom, TempCreditLimitValidFrom);
	}

	/** Get &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#29983;&#25928;&#26085;&#26399;.
		@return &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#29983;&#25928;&#26085;&#26399;	  */
	public Timestamp getTempCreditLimitValidFrom()
	{
		return (Timestamp)get_Value(COLUMNNAME_TempCreditLimitValidFrom);
	}

	/** Set &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#22833;&#25928;&#26085;&#26399;.
		@param TempCreditLimitValidTo &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#22833;&#25928;&#26085;&#26399;
	*/
	public void setTempCreditLimitValidTo (Timestamp TempCreditLimitValidTo)
	{
		set_Value (COLUMNNAME_TempCreditLimitValidTo, TempCreditLimitValidTo);
	}

	/** Get &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#22833;&#25928;&#26085;&#26399;.
		@return &#20020;&#26102;&#20449;&#29992;&#39069;&#24230;&#22833;&#25928;&#26085;&#26399;	  */
	public Timestamp getTempCreditLimitValidTo()
	{
		return (Timestamp)get_Value(COLUMNNAME_TempCreditLimitValidTo);
	}

	/** Set TempCreditLimit_Now.
		@param TempCreditLimit_Now TempCreditLimit_Now
	*/
	public void setTempCreditLimit_Now (BigDecimal TempCreditLimit_Now)
	{
		throw new IllegalArgumentException ("TempCreditLimit_Now is virtual column");	}

	/** Get TempCreditLimit_Now.
		@return TempCreditLimit_Now	  */
	public BigDecimal getTempCreditLimit_Now()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TempCreditLimit_Now);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}