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

/** Generated Model for M_Logistics
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="M_Logistics")
public class X_M_Logistics extends PO implements I_M_Logistics, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260630L;

    /** Standard Constructor */
    public X_M_Logistics (Properties ctx, int M_Logistics_ID, String trxName)
    {
      super (ctx, M_Logistics_ID, trxName);
      /** if (M_Logistics_ID == 0)
        {
			setBoxCount (0);
// 1
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setFreightCharges (Env.ZERO);
			setIsSOTrx (false);
// N
			setLogisticsStatus (null);
// Created
			setM_Logistics_ID (0);
			setM_Shipper_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
        } */
    }

    /** Standard Constructor */
    public X_M_Logistics (Properties ctx, int M_Logistics_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, M_Logistics_ID, trxName, virtualColumns);
      /** if (M_Logistics_ID == 0)
        {
			setBoxCount (0);
// 1
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setFreightCharges (Env.ZERO);
			setIsSOTrx (false);
// N
			setLogisticsStatus (null);
// Created
			setM_Logistics_ID (0);
			setM_Shipper_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
        } */
    }

    /** Standard Constructor */
    public X_M_Logistics (Properties ctx, String M_Logistics_UU, String trxName)
    {
      super (ctx, M_Logistics_UU, trxName);
      /** if (M_Logistics_UU == null)
        {
			setBoxCount (0);
// 1
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setFreightCharges (Env.ZERO);
			setIsSOTrx (false);
// N
			setLogisticsStatus (null);
// Created
			setM_Logistics_ID (0);
			setM_Shipper_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
        } */
    }

    /** Standard Constructor */
    public X_M_Logistics (Properties ctx, String M_Logistics_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, M_Logistics_UU, trxName, virtualColumns);
      /** if (M_Logistics_UU == null)
        {
			setBoxCount (0);
// 1
			setC_Currency_ID (0);
			setC_DocType_ID (0);
			setDateTrx (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setFreightCharges (Env.ZERO);
			setIsSOTrx (false);
// N
			setLogisticsStatus (null);
// Created
			setM_Logistics_ID (0);
			setM_Shipper_ID (0);
			setProcessed (false);
// N
			setProcessing (false);
        } */
    }

    /** Load Constructor */
    public X_M_Logistics (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_M_Logistics[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Box Count.
		@param BoxCount Box Count
	*/
	public void setBoxCount (int BoxCount)
	{
		set_Value (COLUMNNAME_BoxCount, Integer.valueOf(BoxCount));
	}

	/** Get Box Count.
		@return Box Count	  */
	public int getBoxCount()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_BoxCount);
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

	/** Set Transaction Date.
		@param DateTrx Transaction Date
	*/
	public void setDateTrx (Timestamp DateTrx)
	{
		set_ValueNoCheck (COLUMNNAME_DateTrx, DateTrx);
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

	/** Set Freight Charges.
		@param FreightCharges Freight Charges
	*/
	public void setFreightCharges (BigDecimal FreightCharges)
	{
		set_Value (COLUMNNAME_FreightCharges, FreightCharges);
	}

	/** Get Freight Charges.
		@return Freight Charges	  */
	public BigDecimal getFreightCharges()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_FreightCharges);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** &#24050;&#21019;&#24314; = Created */
	public static final String LOGISTICSSTATUS_已创建 = "Created";
	/** &#24050;&#31614;&#22238;&#21333; = ReceiptSigned */
	public static final String LOGISTICSSTATUS_已签回单 = "ReceiptSigned";
	/** &#24050;&#21457;&#36135; = Shipped */
	public static final String LOGISTICSSTATUS_已发货 = "Shipped";
	/** &#24050;&#31614;&#25910; = Signed */
	public static final String LOGISTICSSTATUS_已签收 = "Signed";
	/** Set LogisticsStatus.
		@param LogisticsStatus LogisticsStatus
	*/
	public void setLogisticsStatus (String LogisticsStatus)
	{

		set_Value (COLUMNNAME_LogisticsStatus, LogisticsStatus);
	}

	/** Get LogisticsStatus.
		@return LogisticsStatus	  */
	public String getLogisticsStatus()
	{
		return (String)get_Value(COLUMNNAME_LogisticsStatus);
	}

	/** Set &#29289;&#27969;&#21333;.
		@param M_Logistics_ID &#29289;&#27969;&#21333;
	*/
	public void setM_Logistics_ID (int M_Logistics_ID)
	{
		if (M_Logistics_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_Logistics_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_Logistics_ID, Integer.valueOf(M_Logistics_ID));
	}

	/** Get &#29289;&#27969;&#21333;.
		@return &#29289;&#27969;&#21333;	  */
	public int getM_Logistics_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Logistics_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set M_Logistics_UU.
		@param M_Logistics_UU M_Logistics_UU
	*/
	public void setM_Logistics_UU (String M_Logistics_UU)
	{
		set_ValueNoCheck (COLUMNNAME_M_Logistics_UU, M_Logistics_UU);
	}

	/** Get M_Logistics_UU.
		@return M_Logistics_UU	  */
	public String getM_Logistics_UU()
	{
		return (String)get_Value(COLUMNNAME_M_Logistics_UU);
	}

	public org.compiere.model.I_M_Shipper getM_Shipper() throws RuntimeException
	{
		return (org.compiere.model.I_M_Shipper)MTable.get(getCtx(), org.compiere.model.I_M_Shipper.Table_ID)
			.getPO(getM_Shipper_ID(), get_TrxName());
	}

	/** Set Shipper.
		@param M_Shipper_ID Method or manner of product delivery
	*/
	public void setM_Shipper_ID (int M_Shipper_ID)
	{
		if (M_Shipper_ID < 1)
			set_Value (COLUMNNAME_M_Shipper_ID, null);
		else
			set_Value (COLUMNNAME_M_Shipper_ID, Integer.valueOf(M_Shipper_ID));
	}

	/** Get Shipper.
		@return Method or manner of product delivery
	  */
	public int getM_Shipper_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Shipper_ID);
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

	/** Set Shipper Account Number.
		@param ShipperAccount Shipper Account Number
	*/
	public void setShipperAccount (String ShipperAccount)
	{
		set_Value (COLUMNNAME_ShipperAccount, ShipperAccount);
	}

	/** Get Shipper Account Number.
		@return Shipper Account Number	  */
	public String getShipperAccount()
	{
		return (String)get_Value(COLUMNNAME_ShipperAccount);
	}

	/** Set Surcharges.
		@param Surcharges Surcharges
	*/
	public void setSurcharges (BigDecimal Surcharges)
	{
		set_Value (COLUMNNAME_Surcharges, Surcharges);
	}

	/** Get Surcharges.
		@return Surcharges	  */
	public BigDecimal getSurcharges()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Surcharges);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Total Price.
		@param TotalPrice Total Price
	*/
	public void setTotalPrice (BigDecimal TotalPrice)
	{
		set_Value (COLUMNNAME_TotalPrice, TotalPrice);
	}

	/** Get Total Price.
		@return Total Price	  */
	public BigDecimal getTotalPrice()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalPrice);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Tracking No.
		@param TrackingNo Number to track the shipment
	*/
	public void setTrackingNo (String TrackingNo)
	{
		set_Value (COLUMNNAME_TrackingNo, TrackingNo);
	}

	/** Get Tracking No.
		@return Number to track the shipment
	  */
	public String getTrackingNo()
	{
		return (String)get_Value(COLUMNNAME_TrackingNo);
	}

	/** Set Weight.
		@param Weight Weight of a product
	*/
	public void setWeight (BigDecimal Weight)
	{
		set_Value (COLUMNNAME_Weight, Weight);
	}

	/** Get Weight.
		@return Weight of a product
	  */
	public BigDecimal getWeight()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Weight);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}