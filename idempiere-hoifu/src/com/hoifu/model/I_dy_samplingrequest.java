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
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for dy_samplingrequest
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_dy_samplingrequest 
{

    /** TableName=dy_samplingrequest */
    public static final String Table_Name = "dy_samplingrequest";

    /** AD_Table_ID=1000148 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

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

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

    /** Column name Approver_ID */
    public static final String COLUMNNAME_Approver_ID = "Approver_ID";

	/** Set Approver_ID	  */
	public void setApprover_ID (int Approver_ID);

	/** Get Approver_ID	  */
	public int getApprover_ID();

	public org.compiere.model.I_AD_User getApprover() throws RuntimeException;

    /** Column name BizAssistant_ID */
    public static final String COLUMNNAME_BizAssistant_ID = "BizAssistant_ID";

	/** Set BizAssistant_ID	  */
	public void setBizAssistant_ID (int BizAssistant_ID);

	/** Get BizAssistant_ID	  */
	public int getBizAssistant_ID();

	public org.compiere.model.I_AD_User getBizAssistant() throws RuntimeException;

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set &#24448;
&#26469;
&#21333;
&#20301;
.
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get &#24448;
&#26469;
&#21333;
&#20301;
.
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException;

    /** Column name C_Order_ID */
    public static final String COLUMNNAME_C_Order_ID = "C_Order_ID";

	/** Set &#35746;
&#21333;
.
	  * Order
	  */
	public void setC_Order_ID (int C_Order_ID);

	/** Get &#35746;
&#21333;
.
	  * Order
	  */
	public int getC_Order_ID();

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException;

    /** Column name ChargeAmt */
    public static final String COLUMNNAME_ChargeAmt = "ChargeAmt";

	/** Set Charge amount.
	  * Charge Amount
	  */
	public void setChargeAmt (BigDecimal ChargeAmt);

	/** Get Charge amount.
	  * Charge Amount
	  */
	public BigDecimal getChargeAmt();

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

    /** Column name DesignReceiver_ID */
    public static final String COLUMNNAME_DesignReceiver_ID = "DesignReceiver_ID";

	/** Set DesignReceiver_ID	  */
	public void setDesignReceiver_ID (int DesignReceiver_ID);

	/** Get DesignReceiver_ID	  */
	public int getDesignReceiver_ID();

	public org.compiere.model.I_AD_User getDesignReceiver() throws RuntimeException;

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

    /** Column name EngReceiver_ID */
    public static final String COLUMNNAME_EngReceiver_ID = "EngReceiver_ID";

	/** Set EngReceiver_ID	  */
	public void setEngReceiver_ID (int EngReceiver_ID);

	/** Get EngReceiver_ID	  */
	public int getEngReceiver_ID();

	public org.compiere.model.I_AD_User getEngReceiver() throws RuntimeException;

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

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name SalesRep_ID */
    public static final String COLUMNNAME_SalesRep_ID = "SalesRep_ID";

	/** Set Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID);

	/** Get Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public int getSalesRep_ID();

	public org.compiere.model.I_AD_User getSalesRep() throws RuntimeException;

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

    /** Column name approvedate */
    public static final String COLUMNNAME_approvedate = "approvedate";

	/** Set approvedate	  */
	public void setapprovedate (Timestamp approvedate);

	/** Get approvedate	  */
	public Timestamp getapprovedate();

    /** Column name approveresult */
    public static final String COLUMNNAME_approveresult = "approveresult";

	/** Set approveresult	  */
	public void setapproveresult (String approveresult);

	/** Get approveresult	  */
	public String getapproveresult();

    /** Column name bizassistant_phone */
    public static final String COLUMNNAME_bizassistant_phone = "bizassistant_phone";

	/** Set bizassistant_phone	  */
	public void setbizassistant_phone (String bizassistant_phone);

	/** Get bizassistant_phone	  */
	public String getbizassistant_phone();

    /** Column name customercategory */
    public static final String COLUMNNAME_customercategory = "customercategory";

	/** Set customercategory	  */
	public void setcustomercategory (String customercategory);

	/** Get customercategory	  */
	public String getcustomercategory();

    /** Column name customerrequirement */
    public static final String COLUMNNAME_customerrequirement = "customerrequirement";

	/** Set customerrequirement	  */
	public void setcustomerrequirement (String customerrequirement);

	/** Get customerrequirement	  */
	public String getcustomerrequirement();

    /** Column name designreceivedate */
    public static final String COLUMNNAME_designreceivedate = "designreceivedate";

	/** Set designreceivedate	  */
	public void setdesignreceivedate (Timestamp designreceivedate);

	/** Get designreceivedate	  */
	public Timestamp getdesignreceivedate();

    /** Column name designtype */
    public static final String COLUMNNAME_designtype = "designtype";

	/** Set designtype	  */
	public void setdesigntype (String designtype);

	/** Get designtype	  */
	public String getdesigntype();

    /** Column name dy_samplingrequest_ID */
    public static final String COLUMNNAME_dy_samplingrequest_ID = "dy_samplingrequest_ID";

	/** Set dy_samplingrequest	  */
	public void setdy_samplingrequest_ID (int dy_samplingrequest_ID);

	/** Get dy_samplingrequest	  */
	public int getdy_samplingrequest_ID();

    /** Column name dy_samplingrequest_UU */
    public static final String COLUMNNAME_dy_samplingrequest_UU = "dy_samplingrequest_UU";

	/** Set dy_samplingrequest_UU	  */
	public void setdy_samplingrequest_UU (String dy_samplingrequest_UU);

	/** Get dy_samplingrequest_UU	  */
	public String getdy_samplingrequest_UU();

    /** Column name engreceivedate */
    public static final String COLUMNNAME_engreceivedate = "engreceivedate";

	/** Set engreceivedate	  */
	public void setengreceivedate (Timestamp engreceivedate);

	/** Get engreceivedate	  */
	public Timestamp getengreceivedate();

    /** Column name ischarge */
    public static final String COLUMNNAME_ischarge = "ischarge";

	/** Set ischarge	  */
	public void setischarge (boolean ischarge);

	/** Get ischarge	  */
	public boolean ischarge();

    /** Column name publishsampling */
    public static final String COLUMNNAME_publishsampling = "publishsampling";

	/** Set publishsampling	  */
	public void setpublishsampling (String publishsampling);

	/** Get publishsampling	  */
	public String getpublishsampling();

    /** Column name qtydesign */
    public static final String COLUMNNAME_qtydesign = "qtydesign";

	/** Set qtydesign	  */
	public void setqtydesign (BigDecimal qtydesign);

	/** Get qtydesign	  */
	public BigDecimal getqtydesign();

    /** Column name qtydesignplan */
    public static final String COLUMNNAME_qtydesignplan = "qtydesignplan";

	/** Set qtydesignplan	  */
	public void setqtydesignplan (BigDecimal qtydesignplan);

	/** Get qtydesignplan	  */
	public BigDecimal getqtydesignplan();

    /** Column name salesrep_phone */
    public static final String COLUMNNAME_salesrep_phone = "salesrep_phone";

	/** Set salesrep_phone	  */
	public void setsalesrep_phone (String salesrep_phone);

	/** Get salesrep_phone	  */
	public String getsalesrep_phone();

    /** Column name samplingstatus */
    public static final String COLUMNNAME_samplingstatus = "samplingstatus";

	/** Set samplingstatus	  */
	public void setsamplingstatus (String samplingstatus);

	/** Get samplingstatus	  */
	public String getsamplingstatus();
}
