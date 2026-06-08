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

/** Generated Interface for C_Bill_Pool
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_C_Bill_Pool 
{

    /** TableName=C_Bill_Pool */
    public static final String Table_Name = "C_Bill_Pool";

    /** AD_Table_ID=1000083 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 1 - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(1);

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

    /** Column name AcceptanceAgreementNo */
    public static final String COLUMNNAME_AcceptanceAgreementNo = "AcceptanceAgreementNo";

	/** Set &#25215;
&#20817;
&#21327;
&#35758;
&#32534;
&#21495;
 	  */
	public void setAcceptanceAgreementNo (String AcceptanceAgreementNo);

	/** Get &#25215;
&#20817;
&#21327;
&#35758;
&#32534;
&#21495;
 	  */
	public String getAcceptanceAgreementNo();

    /** Column name AcceptanceDate */
    public static final String COLUMNNAME_AcceptanceDate = "AcceptanceDate";

	/** Set &#25215;
&#20817;
&#26085;
&#26399;
	  */
	public void setAcceptanceDate (Timestamp AcceptanceDate);

	/** Get &#25215;
&#20817;
&#26085;
&#26399;
	  */
	public Timestamp getAcceptanceDate();

    /** Column name Acceptor_Id */
    public static final String COLUMNNAME_Acceptor_Id = "Acceptor_Id";

	/** Set Acceptor_Id	  */
	public void setAcceptor_Id (String Acceptor_Id);

	/** Get Acceptor_Id	  */
	public String getAcceptor_Id();

    /** Column name BillAmt */
    public static final String COLUMNNAME_BillAmt = "BillAmt";

	/** Set BillAmt	  */
	public void setBillAmt (BigDecimal BillAmt);

	/** Get BillAmt	  */
	public BigDecimal getBillAmt();

    /** Column name BillDate */
    public static final String COLUMNNAME_BillDate = "BillDate";

	/** Set BillDate	  */
	public void setBillDate (Timestamp BillDate);

	/** Get BillDate	  */
	public Timestamp getBillDate();

    /** Column name BillNumber */
    public static final String COLUMNNAME_BillNumber = "BillNumber";

	/** Set BillNumber	  */
	public void setBillNumber (String BillNumber);

	/** Get BillNumber	  */
	public String getBillNumber();

    /** Column name BillPackageNo */
    public static final String COLUMNNAME_BillPackageNo = "BillPackageNo";

	/** Set BillPackageNo	  */
	public void setBillPackageNo (String BillPackageNo);

	/** Get BillPackageNo	  */
	public String getBillPackageNo();

    /** Column name BillRate */
    public static final String COLUMNNAME_BillRate = "BillRate";

	/** Set BillRate	  */
	public void setBillRate (BigDecimal BillRate);

	/** Get BillRate	  */
	public BigDecimal getBillRate();

    /** Column name BillStatus */
    public static final String COLUMNNAME_BillStatus = "BillStatus";

	/** Set BillStatus	  */
	public void setBillStatus (String BillStatus);

	/** Get BillStatus	  */
	public String getBillStatus();

    /** Column name BillType */
    public static final String COLUMNNAME_BillType = "BillType";

	/** Set BillType	  */
	public void setBillType (String BillType);

	/** Get BillType	  */
	public String getBillType();

    /** Column name BusinessStatus */
    public static final String COLUMNNAME_BusinessStatus = "BusinessStatus";

	/** Set BusinessStatus	  */
	public void setBusinessStatus (String BusinessStatus);

	/** Get BusinessStatus	  */
	public String getBusinessStatus();

    /** Column name C_BankAccount_ID */
    public static final String COLUMNNAME_C_BankAccount_ID = "C_BankAccount_ID";

	/** Set Bank Account.
	  * Account at the Bank
	  */
	public void setC_BankAccount_ID (int C_BankAccount_ID);

	/** Get Bank Account.
	  * Account at the Bank
	  */
	public int getC_BankAccount_ID();

	public org.compiere.model.I_C_BankAccount getC_BankAccount() throws RuntimeException;

    /** Column name C_Bill_Pool_ID */
    public static final String COLUMNNAME_C_Bill_Pool_ID = "C_Bill_Pool_ID";

	/** Set C_Bill_Pool	  */
	public void setC_Bill_Pool_ID (int C_Bill_Pool_ID);

	/** Get C_Bill_Pool	  */
	public int getC_Bill_Pool_ID();

    /** Column name C_Currency_ID */
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";

	/** Set Currency.
	  * The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID);

	/** Get Currency.
	  * The Currency for this record
	  */
	public int getC_Currency_ID();

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException;

    /** Column name C_DocType_ID */
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	/** Set Document Type.
	  * Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID);

	/** Get Document Type.
	  * Document type or rules
	  */
	public int getC_DocType_ID();

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException;

    /** Column name CirculationStatus */
    public static final String COLUMNNAME_CirculationStatus = "CirculationStatus";

	/** Set CirculationStatus	  */
	public void setCirculationStatus (String CirculationStatus);

	/** Get CirculationStatus	  */
	public String getCirculationStatus();

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

    /** Column name DeliveryDate */
    public static final String COLUMNNAME_DeliveryDate = "DeliveryDate";

	/** Set DeliveryDate	  */
	public void setDeliveryDate (Timestamp DeliveryDate);

	/** Get DeliveryDate	  */
	public Timestamp getDeliveryDate();

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

    /** Column name Drawer_Id */
    public static final String COLUMNNAME_Drawer_Id = "Drawer_Id";

	/** Set Drawer_Id	  */
	public void setDrawer_Id (String Drawer_Id);

	/** Get Drawer_Id	  */
	public String getDrawer_Id();

    /** Column name Endorsee_Id */
    public static final String COLUMNNAME_Endorsee_Id = "Endorsee_Id";

	/** Set Endorsee_Id	  */
	public void setEndorsee_Id (String Endorsee_Id);

	/** Get Endorsee_Id	  */
	public String getEndorsee_Id();

    /** Column name EndorserType */
    public static final String COLUMNNAME_EndorserType = "EndorserType";

	/** Set EndorserType	  */
	public void setEndorserType (String EndorserType);

	/** Get EndorserType	  */
	public String getEndorserType();

    /** Column name Endorser_Id */
    public static final String COLUMNNAME_Endorser_Id = "Endorser_Id";

	/** Set Endorser_Id	  */
	public void setEndorser_Id (String Endorser_Id);

	/** Get Endorser_Id	  */
	public String getEndorser_Id();

    /** Column name Holder_Id */
    public static final String COLUMNNAME_Holder_Id = "Holder_Id";

	/** Set Holder_Id	  */
	public void setHolder_Id (int Holder_Id);

	/** Get Holder_Id	  */
	public int getHolder_Id();

	public org.compiere.model.I_C_BPartner getHolder() throws RuntimeException;

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

    /** Column name IsReceipt */
    public static final String COLUMNNAME_IsReceipt = "IsReceipt";

	/** Set Receipt.
	  * This is a sales transaction (receipt)
	  */
	public void setIsReceipt (boolean IsReceipt);

	/** Get Receipt.
	  * This is a sales transaction (receipt)
	  */
	public boolean isReceipt();

    /** Column name IsRecourse */
    public static final String COLUMNNAME_IsRecourse = "IsRecourse";

	/** Set IsRecourse	  */
	public void setIsRecourse (boolean IsRecourse);

	/** Get IsRecourse	  */
	public boolean isRecourse();

    /** Column name IsRevocable */
    public static final String COLUMNNAME_IsRevocable = "IsRevocable";

	/** Set IsRevocable	  */
	public void setIsRevocable (boolean IsRevocable);

	/** Get IsRevocable	  */
	public boolean isRevocable();

    /** Column name IsSplittable */
    public static final String COLUMNNAME_IsSplittable = "IsSplittable";

	/** Set IsSplittable	  */
	public void setIsSplittable (boolean IsSplittable);

	/** Get IsSplittable	  */
	public boolean isSplittable();

    /** Column name IsTransferable */
    public static final String COLUMNNAME_IsTransferable = "IsTransferable";

	/** Set IsTransferable	  */
	public void setIsTransferable (boolean IsTransferable);

	/** Get IsTransferable	  */
	public boolean isTransferable();

    /** Column name MaturityAmt */
    public static final String COLUMNNAME_MaturityAmt = "MaturityAmt";

	/** Set MaturityAmt	  */
	public void setMaturityAmt (BigDecimal MaturityAmt);

	/** Get MaturityAmt	  */
	public BigDecimal getMaturityAmt();

    /** Column name MaturityDate */
    public static final String COLUMNNAME_MaturityDate = "MaturityDate";

	/** Set MaturityDate	  */
	public void setMaturityDate (Timestamp MaturityDate);

	/** Get MaturityDate	  */
	public Timestamp getMaturityDate();

    /** Column name Owner_Org_ID */
    public static final String COLUMNNAME_Owner_Org_ID = "Owner_Org_ID";

	/** Set Owner_Org_ID	  */
	public void setOwner_Org_ID (int Owner_Org_ID);

	/** Get Owner_Org_ID	  */
	public int getOwner_Org_ID();

    /** Column name Payee_Id */
    public static final String COLUMNNAME_Payee_Id = "Payee_Id";

	/** Set Payee_Id	  */
	public void setPayee_Id (String Payee_Id);

	/** Get Payee_Id	  */
	public String getPayee_Id();

    /** Column name PaymentTermDays */
    public static final String COLUMNNAME_PaymentTermDays = "PaymentTermDays";

	/** Set PaymentTermDays	  */
	public void setPaymentTermDays (BigDecimal PaymentTermDays);

	/** Get PaymentTermDays	  */
	public BigDecimal getPaymentTermDays();

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

    /** Column name ProcessedOn */
    public static final String COLUMNNAME_ProcessedOn = "ProcessedOn";

	/** Set Processed On.
	  * The date+time (expressed in decimal format) when the document has been processed
	  */
	public void setProcessedOn (BigDecimal ProcessedOn);

	/** Get Processed On.
	  * The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn();

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name ReceiveDate */
    public static final String COLUMNNAME_ReceiveDate = "ReceiveDate";

	/** Set ReceiveDate	  */
	public void setReceiveDate (Timestamp ReceiveDate);

	/** Get ReceiveDate	  */
	public Timestamp getReceiveDate();

    /** Column name Settle_Org_ID */
    public static final String COLUMNNAME_Settle_Org_ID = "Settle_Org_ID";

	/** Set Settle_Org_ID	  */
	public void setSettle_Org_ID (int Settle_Org_ID);

	/** Get Settle_Org_ID	  */
	public int getSettle_Org_ID();

    /** Column name SubBillEndNo */
    public static final String COLUMNNAME_SubBillEndNo = "SubBillEndNo";

	/** Set SubBillEndNo	  */
	public void setSubBillEndNo (String SubBillEndNo);

	/** Get SubBillEndNo	  */
	public String getSubBillEndNo();

    /** Column name SubBillStartNo */
    public static final String COLUMNNAME_SubBillStartNo = "SubBillStartNo";

	/** Set SubBillStartNo	  */
	public void setSubBillStartNo (String SubBillStartNo);

	/** Get SubBillStartNo	  */
	public String getSubBillStartNo();

    /** Column name SubPackageAmt */
    public static final String COLUMNNAME_SubPackageAmt = "SubPackageAmt";

	/** Set SubPackageAmt	  */
	public void setSubPackageAmt (BigDecimal SubPackageAmt);

	/** Get SubPackageAmt	  */
	public BigDecimal getSubPackageAmt();

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
}
