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

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

/** Generated Model for PP_Order_Repair_Request
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="PP_Order_Repair_Request")
public class X_PP_Order_Repair_Request extends PO implements I_PP_Order_Repair_Request, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260820L;

    /** Standard Constructor */
    public X_PP_Order_Repair_Request (Properties ctx, int PP_Order_Repair_Request_ID, String trxName)
    {
      super (ctx, PP_Order_Repair_Request_ID, trxName);
      /** if (PP_Order_Repair_Request_ID == 0)
        {
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setM_Product_ID (0);
			setPP_OrderRepairRequest_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Repair_Request_UU (null);
			setProcessed (false);
// N
			setrepairmethod (null);
// WO
			setshortagereason (null);
        } */
    }

    /** Standard Constructor */
    public X_PP_Order_Repair_Request (Properties ctx, int PP_Order_Repair_Request_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Order_Repair_Request_ID, trxName, virtualColumns);
      /** if (PP_Order_Repair_Request_ID == 0)
        {
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setM_Product_ID (0);
			setPP_OrderRepairRequest_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Repair_Request_UU (null);
			setProcessed (false);
// N
			setrepairmethod (null);
// WO
			setshortagereason (null);
        } */
    }

    /** Standard Constructor */
    public X_PP_Order_Repair_Request (Properties ctx, String PP_Order_Repair_Request_UU, String trxName)
    {
      super (ctx, PP_Order_Repair_Request_UU, trxName);
      /** if (PP_Order_Repair_Request_UU == null)
        {
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setM_Product_ID (0);
			setPP_OrderRepairRequest_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Repair_Request_UU (null);
			setProcessed (false);
// N
			setrepairmethod (null);
// WO
			setshortagereason (null);
        } */
    }

    /** Standard Constructor */
    public X_PP_Order_Repair_Request (Properties ctx, String PP_Order_Repair_Request_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Order_Repair_Request_UU, trxName, virtualColumns);
      /** if (PP_Order_Repair_Request_UU == null)
        {
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
// N
			setM_Product_ID (0);
			setPP_OrderRepairRequest_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Repair_Request_UU (null);
			setProcessed (false);
// N
			setrepairmethod (null);
// WO
			setshortagereason (null);
        } */
    }

    /** Load Constructor */
    public X_PP_Order_Repair_Request (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client
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
      StringBuilder sb = new StringBuilder ("X_PP_Order_Repair_Request[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_WF_Process getAD_WF_Process() throws RuntimeException
	{
		return (org.compiere.model.I_AD_WF_Process)MTable.get(getCtx(), org.compiere.model.I_AD_WF_Process.Table_ID)
			.getPO(getAD_WF_Process_ID(), get_TrxName());
	}

	/** Set Workflow Process.
		@param AD_WF_Process_ID Actual Workflow Process Instance
	*/
	public void setAD_WF_Process_ID (int AD_WF_Process_ID)
	{
		if (AD_WF_Process_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_WF_Process_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_WF_Process_ID, Integer.valueOf(AD_WF_Process_ID));
	}

	/** Get Workflow Process.
		@return Actual Workflow Process Instance
	  */
	public int getAD_WF_Process_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_WF_Process_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException
	{
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_ID)
			.getPO(getC_OrderLine_ID(), get_TrxName());
	}

	/** Set Sales Order Line.
		@param C_OrderLine_ID Sales Order Line
	*/
	public void setC_OrderLine_ID (int C_OrderLine_ID)
	{
		if (C_OrderLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_OrderLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_OrderLine_ID, Integer.valueOf(C_OrderLine_ID));
	}

	/** Get Sales Order Line.
		@return Sales Order Line
	  */
	public int getC_OrderLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_OrderLine getC_OrderLine_New() throws RuntimeException
	{
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_ID)
			.getPO(getC_OrderLine_New_ID(), get_TrxName());
	}

	/** Set &#26032;&#38144;&#21806;&#21333;&#21495;.
		@param C_OrderLine_New_ID &#26032;&#38144;&#21806;&#21333;&#21495;
	*/
	public void setC_OrderLine_New_ID (int C_OrderLine_New_ID)
	{
		if (C_OrderLine_New_ID < 1)
			set_Value (COLUMNNAME_C_OrderLine_New_ID, null);
		else
			set_Value (COLUMNNAME_C_OrderLine_New_ID, Integer.valueOf(C_OrderLine_New_ID));
	}

	/** Get &#26032;&#38144;&#21806;&#21333;&#21495;.
		@return &#26032;&#38144;&#21806;&#21333;&#21495;	  */
	public int getC_OrderLine_New_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_New_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
	{
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_ID)
			.getPO(getM_Product_ID(), get_TrxName());
	}

	/** Set Product.
		@param M_Product_ID Product, Service, Item
	*/
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#29983;&#20135;&#34917;&#25968;&#30003;&#35831;&#34920;.
		@param PP_OrderRepairRequest_ID &#29983;&#20135;&#34917;&#25968;&#30003;&#35831;&#34920;
	*/
	public void setPP_OrderRepairRequest_ID (int PP_OrderRepairRequest_ID)
	{
		if (PP_OrderRepairRequest_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_OrderRepairRequest_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_OrderRepairRequest_ID, Integer.valueOf(PP_OrderRepairRequest_ID));
	}

	/** Get &#29983;&#20135;&#34917;&#25968;&#30003;&#35831;&#34920;.
		@return &#29983;&#20135;&#34917;&#25968;&#30003;&#35831;&#34920;	  */
	public int getPP_OrderRepairRequest_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_OrderRepairRequest_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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


	/** Set &#26032;&#24037;&#21333;ID.
		@param PP_Order_New_ID &#26032;&#24037;&#21333;ID
	*/
	public void setPP_Order_New_ID (int PP_Order_New_ID)
	{
		if (PP_Order_New_ID < 1)
			set_Value (COLUMNNAME_PP_Order_New_ID, null);
		else
			set_Value (COLUMNNAME_PP_Order_New_ID, Integer.valueOf(PP_Order_New_ID));
	}

	/** Get &#26032;&#24037;&#21333;ID.
		@return &#26032;&#24037;&#21333;ID	  */
	public int getPP_Order_New_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_New_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PP_Order_Repair_Request_UU.
		@param PP_Order_Repair_Request_UU PP_Order_Repair_Request_UU
	*/
	public void setPP_Order_Repair_Request_UU (String PP_Order_Repair_Request_UU)
	{
		set_ValueNoCheck (COLUMNNAME_PP_Order_Repair_Request_UU, PP_Order_Repair_Request_UU);
	}

	/** Get PP_Order_Repair_Request_UU.
		@return PP_Order_Repair_Request_UU	  */
	public String getPP_Order_Repair_Request_UU()
	{
		return (String)get_Value(COLUMNNAME_PP_Order_Repair_Request_UU);
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

	/** Set datecompleted.
		@param datecompleted datecompleted
	*/
	public void setdatecompleted (Timestamp datecompleted)
	{
		set_Value (COLUMNNAME_datecompleted, datecompleted);
	}

	/** Get datecompleted.
		@return datecompleted	  */
	public Timestamp getdatecompleted()
	{
		return (Timestamp)get_Value(COLUMNNAME_datecompleted);
	}

	/** Set  &#20837;&#24211;&#25968;&#37327;.
		@param qtydeliveredsnap  &#20837;&#24211;&#25968;&#37327;
	*/
	public void setqtydeliveredsnap (BigDecimal qtydeliveredsnap)
	{
		set_Value (COLUMNNAME_qtydeliveredsnap, qtydeliveredsnap);
	}

	/** Get  &#20837;&#24211;&#25968;&#37327;.
		@return  &#20837;&#24211;&#25968;&#37327;	  */
	public BigDecimal getqtydeliveredsnap()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtydeliveredsnap);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set &#35745;&#21010;&#25968;&#37327;.
		@param qtyplanned &#35745;&#21010;&#25968;&#37327;
	*/
	public void setqtyplanned (BigDecimal qtyplanned)
	{
		set_Value (COLUMNNAME_qtyplanned, qtyplanned);
	}

	/** Get &#35745;&#21010;&#25968;&#37327;.
		@return &#35745;&#21010;&#25968;&#37327;	  */
	public BigDecimal getqtyplanned()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtyplanned);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set qtyshortage.
		@param qtyshortage qtyshortage
	*/
	public void setqtyshortage (BigDecimal qtyshortage)
	{
		set_Value (COLUMNNAME_qtyshortage, qtyshortage);
	}

	/** Get qtyshortage.
		@return qtyshortage	  */
	public BigDecimal getqtyshortage()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtyshortage);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set reasondesc.
		@param reasondesc reasondesc
	*/
	public void setreasondesc (String reasondesc)
	{
		set_Value (COLUMNNAME_reasondesc, reasondesc);
	}

	/** Get reasondesc.
		@return reasondesc	  */
	public String getreasondesc()
	{
		return (String)get_Value(COLUMNNAME_reasondesc);
	}

	/** &#38543;&#38144;&#21333;&#34917;&#25968; = SO */
	public static final String REPAIRMETHOD_随销单补数 = "SO";
	/** &#24037;&#21333;&#34917;&#25968; = WO */
	public static final String REPAIRMETHOD_工单补数 = "WO";
	/** Set repairmethod.
		@param repairmethod repairmethod
	*/
	public void setrepairmethod (String repairmethod)
	{

		set_Value (COLUMNNAME_repairmethod, repairmethod);
	}

	/** Get repairmethod.
		@return repairmethod	  */
	public String getrepairmethod()
	{
		return (String)get_Value(COLUMNNAME_repairmethod);
	}

	/** Set repairqty.
		@param repairqty repairqty
	*/
	public void setrepairqty (BigDecimal repairqty)
	{
		set_Value (COLUMNNAME_repairqty, repairqty);
	}

	/** Get repairqty.
		@return repairqty	  */
	public BigDecimal getrepairqty()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_repairqty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** &#35774;&#22791;&#25925;&#38556; = EF */
	public static final String SHORTAGEREASON_设备故障 = "EF";
	/** &#25439;&#32791;&#36229;&#26631; = LH */
	public static final String SHORTAGEREASON_损耗超标 = "LH";
	/** &#26469;&#26009;&#19981;&#33391; = LL */
	public static final String SHORTAGEREASON_来料不良 = "LL";
	/** &#20854;&#20182; = OT */
	public static final String SHORTAGEREASON_其他 = "OT";
	/** &#36136;&#37327;&#24322;&#24120; = QA */
	public static final String SHORTAGEREASON_质量异常 = "QA";
	/** &#25805;&#20316;&#22833;&#35823; = QE */
	public static final String SHORTAGEREASON_操作失误 = "QE";
	/** &#25171;&#26679;&#28040;&#32791; = SC */
	public static final String SHORTAGEREASON_打样消耗 = "SC";
	/** Set shortagereason.
		@param shortagereason shortagereason
	*/
	public void setshortagereason (String shortagereason)
	{

		set_Value (COLUMNNAME_shortagereason, shortagereason);
	}

	/** Get shortagereason.
		@return shortagereason	  */
	public String getshortagereason()
	{
		return (String)get_Value(COLUMNNAME_shortagereason);
	}
}