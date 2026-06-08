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
package org.libero.tables;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

/** Generated Model for PP_Engineering_Change_Notice
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="PP_Engineering_Change_Notice")
public class X_PP_Engineering_Change_Notice extends PO implements I_PP_Engineering_Change_Notice, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260325L;

    /** Standard Constructor */
    public X_PP_Engineering_Change_Notice (Properties ctx, int PP_Engineering_Change_Notice_ID, String trxName)
    {
      super (ctx, PP_Engineering_Change_Notice_ID, trxName);
      /** if (PP_Engineering_Change_Notice_ID == 0)
        {
			setC_DocType_ID (0);
			setChangeSource (null);
			setDetailInfo (null);
			setECNStatus (null);
			setECNType (null);
			setIsApproved (false);
// N
			setMaterialDisposal (null);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
			setName (null);
			setPP_Engineering_Change_Notice_ID (0);
			setPP_Order_ID (0);
			setPriority (0);
			setProcessed (false);
// N
			setWipDisposal (null);
        } */
    }

    /** Standard Constructor */
    public X_PP_Engineering_Change_Notice (Properties ctx, int PP_Engineering_Change_Notice_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Engineering_Change_Notice_ID, trxName, virtualColumns);
      /** if (PP_Engineering_Change_Notice_ID == 0)
        {
			setC_DocType_ID (0);
			setChangeSource (null);
			setDetailInfo (null);
			setECNStatus (null);
			setECNType (null);
			setIsApproved (false);
// N
			setMaterialDisposal (null);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
			setName (null);
			setPP_Engineering_Change_Notice_ID (0);
			setPP_Order_ID (0);
			setPriority (0);
			setProcessed (false);
// N
			setWipDisposal (null);
        } */
    }

    /** Standard Constructor */
    public X_PP_Engineering_Change_Notice (Properties ctx, String PP_Engineering_Change_Notice_UU, String trxName)
    {
      super (ctx, PP_Engineering_Change_Notice_UU, trxName);
      /** if (PP_Engineering_Change_Notice_UU == null)
        {
			setC_DocType_ID (0);
			setChangeSource (null);
			setDetailInfo (null);
			setECNStatus (null);
			setECNType (null);
			setIsApproved (false);
// N
			setMaterialDisposal (null);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
			setName (null);
			setPP_Engineering_Change_Notice_ID (0);
			setPP_Order_ID (0);
			setPriority (0);
			setProcessed (false);
// N
			setWipDisposal (null);
        } */
    }

    /** Standard Constructor */
    public X_PP_Engineering_Change_Notice (Properties ctx, String PP_Engineering_Change_Notice_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Engineering_Change_Notice_UU, trxName, virtualColumns);
      /** if (PP_Engineering_Change_Notice_UU == null)
        {
			setC_DocType_ID (0);
			setChangeSource (null);
			setDetailInfo (null);
			setECNStatus (null);
			setECNType (null);
			setIsApproved (false);
// N
			setMaterialDisposal (null);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
			setName (null);
			setPP_Engineering_Change_Notice_ID (0);
			setPP_Order_ID (0);
			setPriority (0);
			setProcessed (false);
// N
			setWipDisposal (null);
        } */
    }

    /** Load Constructor */
    public X_PP_Engineering_Change_Notice (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_PP_Engineering_Change_Notice[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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

	/** Set ChangeSource.
		@param ChangeSource ChangeSource
	*/
	public void setChangeSource (String ChangeSource)
	{
		set_Value (COLUMNNAME_ChangeSource, ChangeSource);
	}

	/** Get ChangeSource.
		@return ChangeSource	  */
	public String getChangeSource()
	{
		return (String)get_Value(COLUMNNAME_ChangeSource);
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

	/** Set Detail Information.
		@param DetailInfo Additional Detail Information
	*/
	public void setDetailInfo (String DetailInfo)
	{
		set_Value (COLUMNNAME_DetailInfo, DetailInfo);
	}

	/** Get Detail Information.
		@return Additional Detail Information
	  */
	public String getDetailInfo()
	{
		return (String)get_Value(COLUMNNAME_DetailInfo);
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

	/** Set ECNStatus.
		@param ECNStatus ECNStatus
	*/
	public void setECNStatus (String ECNStatus)
	{
		set_Value (COLUMNNAME_ECNStatus, ECNStatus);
	}

	/** Get ECNStatus.
		@return ECNStatus	  */
	public String getECNStatus()
	{
		return (String)get_Value(COLUMNNAME_ECNStatus);
	}

	/** Set ECNType.
		@param ECNType ECNType
	*/
	public void setECNType (String ECNType)
	{
		set_Value (COLUMNNAME_ECNType, ECNType);
	}

	/** Get ECNType.
		@return ECNType	  */
	public String getECNType()
	{
		return (String)get_Value(COLUMNNAME_ECNType);
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

	/** Set MaterialDisposal.
		@param MaterialDisposal MaterialDisposal
	*/
	public void setMaterialDisposal (String MaterialDisposal)
	{
		set_Value (COLUMNNAME_MaterialDisposal, MaterialDisposal);
	}

	/** Get MaterialDisposal.
		@return MaterialDisposal	  */
	public String getMaterialDisposal()
	{
		return (String)get_Value(COLUMNNAME_MaterialDisposal);
	}

	/** Set Movement Date.
		@param MovementDate Date a product was moved in or out of inventory
	*/
	public void setMovementDate (Timestamp MovementDate)
	{
		set_ValueNoCheck (COLUMNNAME_MovementDate, MovementDate);
	}

	/** Get Movement Date.
		@return Date a product was moved in or out of inventory
	  */
	public Timestamp getMovementDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_MovementDate);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set PP_Engineering_Change_Notice.
		@param PP_Engineering_Change_Notice_ID PP_Engineering_Change_Notice
	*/
	public void setPP_Engineering_Change_Notice_ID (int PP_Engineering_Change_Notice_ID)
	{
		if (PP_Engineering_Change_Notice_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Engineering_Change_Notice_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Engineering_Change_Notice_ID, Integer.valueOf(PP_Engineering_Change_Notice_ID));
	}

	/** Get PP_Engineering_Change_Notice.
		@return PP_Engineering_Change_Notice	  */
	public int getPP_Engineering_Change_Notice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Engineering_Change_Notice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PP_Engineering_Change_Notice_UU.
		@param PP_Engineering_Change_Notice_UU PP_Engineering_Change_Notice_UU
	*/
	public void setPP_Engineering_Change_Notice_UU (String PP_Engineering_Change_Notice_UU)
	{
		set_ValueNoCheck (COLUMNNAME_PP_Engineering_Change_Notice_UU, PP_Engineering_Change_Notice_UU);
	}

	/** Get PP_Engineering_Change_Notice_UU.
		@return PP_Engineering_Change_Notice_UU	  */
	public String getPP_Engineering_Change_Notice_UU()
	{
		return (String)get_Value(COLUMNNAME_PP_Engineering_Change_Notice_UU);
	}

	public I_PP_Order getPP_Order() throws RuntimeException
	{
		return (I_PP_Order)MTable.get(getCtx(), I_PP_Order.Table_ID)
			.getPO(getPP_Order_ID(), get_TrxName());
	}

	/** Set Manufacturing Order.
		@param PP_Order_ID Manufacturing Order
	*/
	public void setPP_Order_ID (int PP_Order_ID)
	{
		if (PP_Order_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, Integer.valueOf(PP_Order_ID));
	}

	/** Get Manufacturing Order.
		@return Manufacturing Order
	  */
	public int getPP_Order_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Priority.
		@param Priority Indicates if this request is of a high, medium or low priority.
	*/
	public void setPriority (int Priority)
	{
		set_Value (COLUMNNAME_Priority, Integer.valueOf(Priority));
	}

	/** Get Priority.
		@return Indicates if this request is of a high, medium or low priority.
	  */
	public int getPriority()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Priority);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set WipDisposal.
		@param WipDisposal WipDisposal
	*/
	public void setWipDisposal (String WipDisposal)
	{
		set_Value (COLUMNNAME_WipDisposal, WipDisposal);
	}

	/** Get WipDisposal.
		@return WipDisposal	  */
	public String getWipDisposal()
	{
		return (String)get_Value(COLUMNNAME_WipDisposal);
	}

	/** Column name AD_WF_Process_ID */
	public static final String COLUMNNAME_AD_WF_Process_ID = "AD_WF_Process_ID";

	/**
	 * Set Workflow Process.
	 * 
	 * @param AD_WF_Process_ID Workflow Process
	 */
	public void setAD_WF_Process_ID(int AD_WF_Process_ID) {
		if (AD_WF_Process_ID < 1)
			set_Value(COLUMNNAME_AD_WF_Process_ID, null);
		else
			set_Value(COLUMNNAME_AD_WF_Process_ID, Integer.valueOf(AD_WF_Process_ID));
	}

	/**
	 * Get Workflow Process.
	 * 
	 * @return Workflow Process
	 */
	public int getAD_WF_Process_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_AD_WF_Process_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}
}