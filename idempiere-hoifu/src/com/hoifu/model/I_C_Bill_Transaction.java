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

/** Generated Interface for C_Bill_Transaction
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_C_Bill_Transaction 
{

    /** TableName=C_Bill_Transaction */
    public static final String Table_Name = "C_Bill_Transaction";

    /** AD_Table_ID=1000085 */
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

    /** Column name Acceptor_Id */
    public static final String COLUMNNAME_Acceptor_Id = "Acceptor_Id";

	/** Set Acceptor_Id	  */
	public void setAcceptor_Id (String Acceptor_Id);

	/** Get Acceptor_Id	  */
	public String getAcceptor_Id();

    /** Column name AccountNo */
    public static final String COLUMNNAME_AccountNo = "AccountNo";

	/** Set Account No.
	  * Account Number
	  */
	public void setAccountNo (String AccountNo);

	/** Get Account No.
	  * Account Number
	  */
	public String getAccountNo();

    /** Column name AffairType */
    public static final String COLUMNNAME_AffairType = "AffairType";

	/** Set AffairType	  */
	public void setAffairType (String AffairType);

	/** Get AffairType	  */
	public String getAffairType();

    /** Column name BillAmt */
    public static final String COLUMNNAME_BillAmt = "BillAmt";

	/** Set BillAmt	  */
	public void setBillAmt (BigDecimal BillAmt);

	/** Get BillAmt	  */
	public BigDecimal getBillAmt();

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

    /** Column name BillType */
    public static final String COLUMNNAME_BillType = "BillType";

	/** Set BillType	  */
	public void setBillType (String BillType);

	/** Get BillType	  */
	public String getBillType();

    /** Column name BusinessDate */
    public static final String COLUMNNAME_BusinessDate = "BusinessDate";

	/** Set BusinessDate	  */
	public void setBusinessDate (Timestamp BusinessDate);

	/** Get BusinessDate	  */
	public Timestamp getBusinessDate();

    /** Column name BusinessStatus */
    public static final String COLUMNNAME_BusinessStatus = "BusinessStatus";

	/** Set BusinessStatus	  */
	public void setBusinessStatus (String BusinessStatus);

	/** Get BusinessStatus	  */
	public String getBusinessStatus();

    /** Column name C_BP_BankAccount_ID */
    public static final String COLUMNNAME_C_BP_BankAccount_ID = "C_BP_BankAccount_ID";

	/** Set Partner Bank Account.
	  * Bank Account of the Business Partner
	  */
	public void setC_BP_BankAccount_ID (int C_BP_BankAccount_ID);

	/** Get Partner Bank Account.
	  * Bank Account of the Business Partner
	  */
	public int getC_BP_BankAccount_ID();

	public org.compiere.model.I_C_BP_BankAccount getC_BP_BankAccount() throws RuntimeException;

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner.
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner.
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException;

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

    /** Column name C_Bill_Pool_DocumentNo */
    public static final String COLUMNNAME_C_Bill_Pool_DocumentNo = "C_Bill_Pool_DocumentNo";

	/** Set C_Bill_Pool_DocumentNo	  */
	public void setC_Bill_Pool_DocumentNo (String C_Bill_Pool_DocumentNo);

	/** Get C_Bill_Pool_DocumentNo	  */
	public String getC_Bill_Pool_DocumentNo();

    /** Column name C_Bill_Pool_ID */
    public static final String COLUMNNAME_C_Bill_Pool_ID = "C_Bill_Pool_ID";

	/** Set C_Bill_Pool	  */
	public void setC_Bill_Pool_ID (int C_Bill_Pool_ID);

	/** Get C_Bill_Pool	  */
	public int getC_Bill_Pool_ID();

	public I_C_Bill_Pool getC_Bill_Pool() throws RuntimeException;

    /** Column name C_Bill_Transaction_ID */
    public static final String COLUMNNAME_C_Bill_Transaction_ID = "C_Bill_Transaction_ID";

	/** Set C_Bill_Transaction	  */
	public void setC_Bill_Transaction_ID (int C_Bill_Transaction_ID);

	/** Get C_Bill_Transaction	  */
	public int getC_Bill_Transaction_ID();

    /** Column name C_Bill_Transaction_UU */
    public static final String COLUMNNAME_C_Bill_Transaction_UU = "C_Bill_Transaction_UU";

	/** Set C_Bill_Transaction_UU	  */
	public void setC_Bill_Transaction_UU (String C_Bill_Transaction_UU);

	/** Get C_Bill_Transaction_UU	  */
	public String getC_Bill_Transaction_UU();

    /** Column name C_Charge_ID */
    public static final String COLUMNNAME_C_Charge_ID = "C_Charge_ID";

	/** Set Charge.
	  * Additional document charges
	  */
	public void setC_Charge_ID (int C_Charge_ID);

	/** Get Charge.
	  * Additional document charges
	  */
	public int getC_Charge_ID();

	public org.compiere.model.I_C_Charge getC_Charge() throws RuntimeException;

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

    /** Column name C_Invoice_ID */
    public static final String COLUMNNAME_C_Invoice_ID = "C_Invoice_ID";

	/** Set Invoice.
	  * Invoice Identifier
	  */
	public void setC_Invoice_ID (int C_Invoice_ID);

	/** Get Invoice.
	  * Invoice Identifier
	  */
	public int getC_Invoice_ID();

	public org.compiere.model.I_C_Invoice getC_Invoice() throws RuntimeException;

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

    /** Column name ContractNo */
    public static final String COLUMNNAME_ContractNo = "ContractNo";

	/** Set ContractNo	  */
	public void setContractNo (String ContractNo);

	/** Get ContractNo	  */
	public String getContractNo();

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

    /** Column name DateTrx */
    public static final String COLUMNNAME_DateTrx = "DateTrx";

	/** Set Transaction Date.
	  * Transaction Date
	  */
	public void setDateTrx (Timestamp DateTrx);

	/** Get Transaction Date.
	  * Transaction Date
	  */
	public Timestamp getDateTrx();

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

    /** Column name Endorser_Id */
    public static final String COLUMNNAME_Endorser_Id = "Endorser_Id";

	/** Set Endorser_Id	  */
	public void setEndorser_Id (String Endorser_Id);

	/** Get Endorser_Id	  */
	public String getEndorser_Id();

    /** Column name InterestAmt */
    public static final String COLUMNNAME_InterestAmt = "InterestAmt";

	/** Set Interest Amount.
	  * Interest Amount
	  */
	public void setInterestAmt (BigDecimal InterestAmt);

	/** Get Interest Amount.
	  * Interest Amount
	  */
	public BigDecimal getInterestAmt();

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

    /** Column name IsTransferable */
    public static final String COLUMNNAME_IsTransferable = "IsTransferable";

	/** Set IsTransferable	  */
	public void setIsTransferable (boolean IsTransferable);

	/** Get IsTransferable	  */
	public boolean isTransferable();

    /** Column name MaturityDate */
    public static final String COLUMNNAME_MaturityDate = "MaturityDate";

	/** Set MaturityDate	  */
	public void setMaturityDate (Timestamp MaturityDate);

	/** Get MaturityDate	  */
	public Timestamp getMaturityDate();

    /** Column name Orig_TrxID */
    public static final String COLUMNNAME_Orig_TrxID = "Orig_TrxID";

	/** Set Original Transaction ID.
	  * Original Transaction ID
	  */
	public void setOrig_TrxID (String Orig_TrxID);

	/** Get Original Transaction ID.
	  * Original Transaction ID
	  */
	public String getOrig_TrxID();

    /** Column name Posted */
    public static final String COLUMNNAME_Posted = "Posted";

	/** Set Posted.
	  * Posting status
	  */
	public void setPosted (boolean Posted);

	/** Get Posted.
	  * Posting status
	  */
	public boolean isPosted();

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

    /** Column name ProjectNo */
    public static final String COLUMNNAME_ProjectNo = "ProjectNo";

	/** Set ProjectNo	  */
	public void setProjectNo (String ProjectNo);

	/** Get ProjectNo	  */
	public String getProjectNo();

    /** Column name Receiver_Id */
    public static final String COLUMNNAME_Receiver_Id = "Receiver_Id";

	/** Set Receiver_Id	  */
	public void setReceiver_Id (String Receiver_Id);

	/** Get Receiver_Id	  */
	public String getReceiver_Id();

    /** Column name RoutingNo */
    public static final String COLUMNNAME_RoutingNo = "RoutingNo";

	/** Set Routing No.
	  * Bank Routing Number
	  */
	public void setRoutingNo (String RoutingNo);

	/** Get Routing No.
	  * Bank Routing Number
	  */
	public String getRoutingNo();

    /** Column name SettleAmt */
    public static final String COLUMNNAME_SettleAmt = "SettleAmt";

	/** Set SettleAmt	  */
	public void setSettleAmt (BigDecimal SettleAmt);

	/** Get SettleAmt	  */
	public BigDecimal getSettleAmt();

    /** Column name SettleCode */
    public static final String COLUMNNAME_SettleCode = "SettleCode";

	/** Set SettleCode	  */
	public void setSettleCode (String SettleCode);

	/** Get SettleCode	  */
	public String getSettleCode();

    /** Column name SubPackageAmt */
    public static final String COLUMNNAME_SubPackageAmt = "SubPackageAmt";

	/** Set SubPackageAmt	  */
	public void setSubPackageAmt (BigDecimal SubPackageAmt);

	/** Get SubPackageAmt	  */
	public BigDecimal getSubPackageAmt();

    /** Column name TransactionType */
    public static final String COLUMNNAME_TransactionType = "TransactionType";

	/** Set TransactionType	  */
	public void setTransactionType (String TransactionType);

	/** Get TransactionType	  */
	public String getTransactionType();

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
