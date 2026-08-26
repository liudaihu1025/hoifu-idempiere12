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
package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

/** Generated Interface for PP_Order_Repair_Request
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_PP_Order_Repair_Request 
{

    /** TableName=PP_Order_Repair_Request */
    public static final String Table_Name = "PP_Order_Repair_Request";

    /** AD_Table_ID=1000202 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name AD_WF_Process_ID */
    public static final String COLUMNNAME_AD_WF_Process_ID = "AD_WF_Process_ID";

	/** Set Workflow Process.
	  * Actual Workflow Process Instance
	  */
	public void setAD_WF_Process_ID (int AD_WF_Process_ID);

	/** Get Workflow Process.
	  * Actual Workflow Process Instance
	  */
	public int getAD_WF_Process_ID();

	public org.compiere.model.I_AD_WF_Process getAD_WF_Process() throws RuntimeException;

    /** Column name C_OrderLine_ID */
    public static final String COLUMNNAME_C_OrderLine_ID = "C_OrderLine_ID";

	/** Set Sales Order Line.
	  * Sales Order Line
	  */
	public void setC_OrderLine_ID (int C_OrderLine_ID);

	/** Get Sales Order Line.
	  * Sales Order Line
	  */
	public int getC_OrderLine_ID();

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException;

    /** Column name C_OrderLine_New_ID */
    public static final String COLUMNNAME_C_OrderLine_New_ID = "C_OrderLine_New_ID";

	/** Set &#26032;
&#38144;
&#21806;
&#21333;
&#21495;
	  */
	public void setC_OrderLine_New_ID (int C_OrderLine_New_ID);

	/** Get &#26032;
&#38144;
&#21806;
&#21333;
&#21495;
	  */
	public int getC_OrderLine_New_ID();

	public org.compiere.model.I_C_OrderLine getC_OrderLine_New() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DateDoc */
    public static final String COLUMNNAME_DateDoc = "DateDoc";

	/** Set Document Date.
	  * Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc);

	/** Get Document Date.
	  * Date of the Document
	  */
	public Timestamp getDateDoc();

    /** Column name DatePromised */
    public static final String COLUMNNAME_DatePromised = "DatePromised";

	/** Set Date Promised.
	  * Date Order was promised
	  */
	public void setDatePromised (Timestamp DatePromised);

	/** Get Date Promised.
	  * Date Order was promised
	  */
	public Timestamp getDatePromised();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name DocAction */
    public static final String COLUMNNAME_DocAction = "DocAction";

	/** Set Document Action.
	  * The targeted status of the document
	  */
	public void setDocAction (String DocAction);

	/** Get Document Action.
	  * The targeted status of the document
	  */
	public String getDocAction();

    /** Column name DocStatus */
    public static final String COLUMNNAME_DocStatus = "DocStatus";

	/** Set Document Status.
	  * The current status of the document
	  */
	public void setDocStatus (String DocStatus);

	/** Get Document Status.
	  * The current status of the document
	  */
	public String getDocStatus();

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsApproved */
    public static final String COLUMNNAME_IsApproved = "IsApproved";

	/** Set Approved.
	  * Indicates if this document requires approval
	  */
	public void setIsApproved (boolean IsApproved);

	/** Get Approved.
	  * Indicates if this document requires approval
	  */
	public boolean isApproved();

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;

    /** Column name PP_OrderRepairRequest_ID */
    public static final String COLUMNNAME_PP_OrderRepairRequest_ID = "PP_OrderRepairRequest_ID";

	/** Set &#29983;
&#20135;
&#34917;
&#25968;
&#30003;
&#35831;
&#34920;
	  */
	public void setPP_OrderRepairRequest_ID (int PP_OrderRepairRequest_ID);

	/** Get &#29983;
&#20135;
&#34917;
&#25968;
&#30003;
&#35831;
&#34920;
	  */
	public int getPP_OrderRepairRequest_ID();

    /** Column name PP_Order_ID */
    public static final String COLUMNNAME_PP_Order_ID = "PP_Order_ID";

	/** Set Manufacturing Order.
	  * Manufacturing Order
	  */
	public void setPP_Order_ID (int PP_Order_ID);

	/** Get Manufacturing Order.
	  * Manufacturing Order
	  */
	public int getPP_Order_ID();



    /** Column name PP_Order_New_ID */
    public static final String COLUMNNAME_PP_Order_New_ID = "PP_Order_New_ID";

	/** Set &#26032;
&#24037;
&#21333;
ID	  */
	public void setPP_Order_New_ID (int PP_Order_New_ID);

	/** Get &#26032;
&#24037;
&#21333;
ID	  */
	public int getPP_Order_New_ID();


    /** Column name PP_Order_Repair_Request_UU */
    public static final String COLUMNNAME_PP_Order_Repair_Request_UU = "PP_Order_Repair_Request_UU";

	/** Set PP_Order_Repair_Request_UU	  */
	public void setPP_Order_Repair_Request_UU (String PP_Order_Repair_Request_UU);

	/** Get PP_Order_Repair_Request_UU	  */
	public String getPP_Order_Repair_Request_UU();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name datecompleted */
    public static final String COLUMNNAME_datecompleted = "datecompleted";

	/** Set datecompleted	  */
	public void setdatecompleted (Timestamp datecompleted);

	/** Get datecompleted	  */
	public Timestamp getdatecompleted();

    /** Column name qtydeliveredsnap */
    public static final String COLUMNNAME_qtydeliveredsnap = "qtydeliveredsnap";

	/** Set  &#20837;
&#24211;
&#25968;
&#37327;
	  */
	public void setqtydeliveredsnap (BigDecimal qtydeliveredsnap);

	/** Get  &#20837;
&#24211;
&#25968;
&#37327;
	  */
	public BigDecimal getqtydeliveredsnap();

    /** Column name qtyplanned */
    public static final String COLUMNNAME_qtyplanned = "qtyplanned";

	/** Set &#35745;
&#21010;
&#25968;
&#37327;
	  */
	public void setqtyplanned (BigDecimal qtyplanned);

	/** Get &#35745;
&#21010;
&#25968;
&#37327;
	  */
	public BigDecimal getqtyplanned();

    /** Column name qtyshortage */
    public static final String COLUMNNAME_qtyshortage = "qtyshortage";

	/** Set qtyshortage	  */
	public void setqtyshortage (BigDecimal qtyshortage);

	/** Get qtyshortage	  */
	public BigDecimal getqtyshortage();

    /** Column name reasondesc */
    public static final String COLUMNNAME_reasondesc = "reasondesc";

	/** Set reasondesc	  */
	public void setreasondesc (String reasondesc);

	/** Get reasondesc	  */
	public String getreasondesc();

    /** Column name repairmethod */
    public static final String COLUMNNAME_repairmethod = "repairmethod";

	/** Set repairmethod	  */
	public void setrepairmethod (String repairmethod);

	/** Get repairmethod	  */
	public String getrepairmethod();

    /** Column name repairqty */
    public static final String COLUMNNAME_repairqty = "repairqty";

	/** Set repairqty	  */
	public void setrepairqty (BigDecimal repairqty);

	/** Get repairqty	  */
	public BigDecimal getrepairqty();

    /** Column name shortagereason */
    public static final String COLUMNNAME_shortagereason = "shortagereason";

	/** Set shortagereason	  */
	public void setshortagereason (String shortagereason);

	/** Get shortagereason	  */
	public String getshortagereason();
}
