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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for M_InOutNotice
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="M_InOutNotice")
public class X_M_InOutNotice extends PO implements I_M_InOutNotice, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260611L;

    /** Standard Constructor */
    public X_M_InOutNotice (Properties ctx, int M_InOutNotice_ID, String trxName)
    {
      super (ctx, M_InOutNotice_ID, trxName);
      /** if (M_InOutNotice_ID == 0)
        {
			setC_BPartner_ID (0);
			setC_DocType_ID (0);
			setC_Order_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsSOTrx (false);
// N
			setM_InOutNotice_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
        } */
    }

    /** Standard Constructor */
    public X_M_InOutNotice (Properties ctx, int M_InOutNotice_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, M_InOutNotice_ID, trxName, virtualColumns);
      /** if (M_InOutNotice_ID == 0)
        {
			setC_BPartner_ID (0);
			setC_DocType_ID (0);
			setC_Order_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsSOTrx (false);
// N
			setM_InOutNotice_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
        } */
    }

    /** Standard Constructor */
    public X_M_InOutNotice (Properties ctx, String M_InOutNotice_UU, String trxName)
    {
      super (ctx, M_InOutNotice_UU, trxName);
      /** if (M_InOutNotice_UU == null)
        {
			setC_BPartner_ID (0);
			setC_DocType_ID (0);
			setC_Order_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsSOTrx (false);
// N
			setM_InOutNotice_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
        } */
    }

    /** Standard Constructor */
    public X_M_InOutNotice (Properties ctx, String M_InOutNotice_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, M_InOutNotice_UU, trxName, virtualColumns);
      /** if (M_InOutNotice_UU == null)
        {
			setC_BPartner_ID (0);
			setC_DocType_ID (0);
			setC_Order_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsSOTrx (false);
// N
			setM_InOutNotice_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
			setSalesRep_ID (0);
// @#AD_User_ID@
        } */
    }

    /** Load Constructor */
    public X_M_InOutNotice (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_M_InOutNotice[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set &#23457;&#26680;&#26102;&#38388;.
		@param Approved &#23457;&#26680;&#26102;&#38388;
	*/
	public void setApproved (Timestamp Approved)
	{
		set_ValueNoCheck (COLUMNNAME_Approved, Approved);
	}

	/** Get &#23457;&#26680;&#26102;&#38388;.
		@return &#23457;&#26680;&#26102;&#38388;	  */
	public Timestamp getApproved()
	{
		return (Timestamp)get_Value(COLUMNNAME_Approved);
	}

	public org.compiere.model.I_AD_User getApprove() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getApprovedBy(), get_TrxName());
	}

	/** Set &#23457;&#26680;&#20154;.
		@param ApprovedBy &#23457;&#26680;&#20154;
	*/
	public void setApprovedBy (int ApprovedBy)
	{
		set_ValueNoCheck (COLUMNNAME_ApprovedBy, Integer.valueOf(ApprovedBy));
	}

	/** Get &#23457;&#26680;&#20154;.
		@return &#23457;&#26680;&#20154;	  */
	public int getApprovedBy()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ApprovedBy);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
	{
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_ID)
			.getPO(getC_DocType_ID(), get_TrxName());
	}

	/** Set &#21333;&#25454;&#31867;&#22411;.
		@param C_DocType_ID Document type or rules
	*/
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0)
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get &#21333;&#25454;&#31867;&#22411;.
		@return Document type or rules
	  */
	public int getC_DocType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException
	{
		return (org.compiere.model.I_C_Order)MTable.get(getCtx(), org.compiere.model.I_C_Order.Table_ID)
			.getPO(getC_Order_ID(), get_TrxName());
	}

	/** Set &#35746;&#21333;.
		@param C_Order_ID Order
	*/
	public void setC_Order_ID (int C_Order_ID)
	{
		if (C_Order_ID < 1)
			set_Value (COLUMNNAME_C_Order_ID, null);
		else
			set_Value (COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID));
	}

	/** Get &#35746;&#21333;.
		@return Order
	  */
	public int getC_Order_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Date Promised.
		@param DatePromised Date Order was promised
	*/
	public void setDatePromised (Timestamp DatePromised)
	{
		set_Value (COLUMNNAME_DatePromised, DatePromised);
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
	/** Set &#21333;&#25454;&#29366;&#24577;.
		@param DocStatus The current status of the document
	*/
	public void setDocStatus (String DocStatus)
	{

		set_Value (COLUMNNAME_DocStatus, DocStatus);
	}

	/** Get &#21333;&#25454;&#29366;&#24577;.
		@return The current status of the document
	  */
	public String getDocStatus()
	{
		return (String)get_Value(COLUMNNAME_DocStatus);
	}

	/** Set &#21333;&#21495;.
		@param DocumentNo Document sequence number of the document
	*/
	public void setDocumentNo (String DocumentNo)
	{
		set_ValueNoCheck (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get &#21333;&#21495;.
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

	/** Set Sales Transaction.
		@param IsSOTrx This is a Sales Transaction
	*/
	public void setIsSOTrx (boolean IsSOTrx)
	{
		set_ValueNoCheck (COLUMNNAME_IsSOTrx, Boolean.valueOf(IsSOTrx));
	}

	/** Get Sales Transaction.
		@return This is a Sales Transaction
	  */
	public boolean isSOTrx()
	{
		Object oo = get_Value(COLUMNNAME_IsSOTrx);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set &#21457;&#36135;&#36890;&#30693;&#21333;.
		@param M_InOutNotice_ID &#21457;&#36135;&#36890;&#30693;&#21333;
	*/
	public void setM_InOutNotice_ID (int M_InOutNotice_ID)
	{
		if (M_InOutNotice_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOutNotice_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOutNotice_ID, Integer.valueOf(M_InOutNotice_ID));
	}

	/** Get &#21457;&#36135;&#36890;&#30693;&#21333;.
		@return &#21457;&#36135;&#36890;&#30693;&#21333;	  */
	public int getM_InOutNotice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutNotice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set M_InOutNotice_UU.
		@param M_InOutNotice_UU M_InOutNotice_UU
	*/
	public void setM_InOutNotice_UU (String M_InOutNotice_UU)
	{
		set_ValueNoCheck (COLUMNNAME_M_InOutNotice_UU, M_InOutNotice_UU);
	}

	/** Get M_InOutNotice_UU.
		@return M_InOutNotice_UU	  */
	public String getM_InOutNotice_UU()
	{
		return (String)get_Value(COLUMNNAME_M_InOutNotice_UU);
	}

	public org.compiere.model.I_M_InOut getM_InOut() throws RuntimeException
	{
		return (org.compiere.model.I_M_InOut)MTable.get(getCtx(), org.compiere.model.I_M_InOut.Table_ID)
			.getPO(getM_InOut_ID(), get_TrxName());
	}

	/** Set Shipment/Receipt.
		@param M_InOut_ID Material Shipment Document
	*/
	public void setM_InOut_ID (int M_InOut_ID)
	{
		if (M_InOut_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOut_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOut_ID, Integer.valueOf(M_InOut_ID));
	}

	/** Get Shipment/Receipt.
		@return Material Shipment Document
	  */
	public int getM_InOut_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOut_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#24050;&#22788;&#29702;.
		@param Processed The document has been processed
	*/
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get &#24050;&#22788;&#29702;.
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
}