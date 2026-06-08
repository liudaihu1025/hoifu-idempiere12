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

/** Generated Interface for C_CreditLimitChange
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_C_CreditLimitChange 
{

    /** TableName=C_CreditLimitChange */
    public static final String Table_Name = "C_CreditLimitChange";

    /** AD_Table_ID=1000100 */
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

    /** Column name ApplyReason */
    public static final String COLUMNNAME_ApplyReason = "ApplyReason";

	/** Set ApplyReason	  */
	public void setApplyReason (String ApplyReason);

	/** Get ApplyReason	  */
	public String getApplyReason();

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

    /** Column name C_CreditLimitChange_ID */
    public static final String COLUMNNAME_C_CreditLimitChange_ID = "C_CreditLimitChange_ID";

	/** Set C_CreditLimitChange	  */
	public void setC_CreditLimitChange_ID (int C_CreditLimitChange_ID);

	/** Get C_CreditLimitChange	  */
	public int getC_CreditLimitChange_ID();

    /** Column name C_CreditLimitChange_UU */
    public static final String COLUMNNAME_C_CreditLimitChange_UU = "C_CreditLimitChange_UU";

	/** Set C_CreditLimitChange_UU	  */
	public void setC_CreditLimitChange_UU (String C_CreditLimitChange_UU);

	/** Get C_CreditLimitChange_UU	  */
	public String getC_CreditLimitChange_UU();

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

    /** Column name CreditLimitChange */
    public static final String COLUMNNAME_CreditLimitChange = "CreditLimitChange";

	/** Set CreditLimitChange	  */
	public void setCreditLimitChange (BigDecimal CreditLimitChange);

	/** Get CreditLimitChange	  */
	public BigDecimal getCreditLimitChange();

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

    /** Column name SO_CreditLimit_Now */
    public static final String COLUMNNAME_SO_CreditLimit_Now = "SO_CreditLimit_Now";

	/** Set SO_CreditLimit_Now	  */
	public void setSO_CreditLimit_Now (BigDecimal SO_CreditLimit_Now);

	/** Get SO_CreditLimit_Now	  */
	public BigDecimal getSO_CreditLimit_Now();


	/** Column name SO_CreditLimit_Old */
	public static final String COLUMNNAME_SO_CreditLimit_Old = "SO_CreditLimit_Old";

	/** Set SO_CreditLimit_Old	  */
	public void setSO_CreditLimit_Old (BigDecimal SO_CreditLimit_Old);

	/** Get SO_CreditLimit_Old	  */
	public BigDecimal getSO_CreditLimit_Old();


	public static final String COLUMNNAME_TempCreditLimit_Old = "TempCreditLimit_Old";


	public void setTempCreditLimit_Old (BigDecimal TempCreditLimit_Old);


	public BigDecimal getTempCreditLimit_Old();

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

    /** Column name TempCreditLimitChange */
    public static final String COLUMNNAME_TempCreditLimitChange = "TempCreditLimitChange";

	/** Set TempCreditLimitChange	  */
	public void setTempCreditLimitChange (BigDecimal TempCreditLimitChange);

	/** Get TempCreditLimitChange	  */
	public BigDecimal getTempCreditLimitChange();

    /** Column name TempCreditLimitValidFrom */
    public static final String COLUMNNAME_TempCreditLimitValidFrom = "TempCreditLimitValidFrom";

	/** Set &#20020;
&#26102;
&#20449;
&#29992;
&#39069;
&#24230;
&#29983;
&#25928;
&#26085;
&#26399;
	  */
	public void setTempCreditLimitValidFrom (Timestamp TempCreditLimitValidFrom);

	/** Get &#20020;
&#26102;
&#20449;
&#29992;
&#39069;
&#24230;
&#29983;
&#25928;
&#26085;
&#26399;
	  */
	public Timestamp getTempCreditLimitValidFrom();

    /** Column name TempCreditLimitValidTo */
    public static final String COLUMNNAME_TempCreditLimitValidTo = "TempCreditLimitValidTo";

	/** Set &#20020;
&#26102;
&#20449;
&#29992;
&#39069;
&#24230;
&#22833;
&#25928;
&#26085;
&#26399;
	  */
	public void setTempCreditLimitValidTo (Timestamp TempCreditLimitValidTo);

	/** Get &#20020;
&#26102;
&#20449;
&#29992;
&#39069;
&#24230;
&#22833;
&#25928;
&#26085;
&#26399;
	  */
	public Timestamp getTempCreditLimitValidTo();

    /** Column name TempCreditLimit_Now */
    public static final String COLUMNNAME_TempCreditLimit_Now = "TempCreditLimit_Now";

	/** Set TempCreditLimit_Now	  */
	public void setTempCreditLimit_Now (BigDecimal TempCreditLimit_Now);

	/** Get TempCreditLimit_Now	  */
	public BigDecimal getTempCreditLimit_Now();

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
