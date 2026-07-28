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

/** Generated Interface for C_PaymentRequestLine
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_C_PaymentRequestLine 
{

    /** TableName=C_PaymentRequestLine */
    public static final String Table_Name = "C_PaymentRequestLine";

    /** AD_Table_ID=1000187 */
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

    /** Column name ApprovedAmt */
    public static final String COLUMNNAME_ApprovedAmt = "ApprovedAmt";

	/** Set &#24050;
&#25209;
&#20934;
&#37329;
&#39069;
	  */
	public void setApprovedAmt (BigDecimal ApprovedAmt);

	/** Get &#24050;
&#25209;
&#20934;
&#37329;
&#39069;
	  */
	public BigDecimal getApprovedAmt();

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

    /** Column name C_PaymentRequestLine_ID */
    public static final String COLUMNNAME_C_PaymentRequestLine_ID = "C_PaymentRequestLine_ID";

	/** Set &#20184;
&#27454;
&#30003;
&#35831;
&#21333;
&#34892;
	  */
	public void setC_PaymentRequestLine_ID (int C_PaymentRequestLine_ID);

	/** Get &#20184;
&#27454;
&#30003;
&#35831;
&#21333;
&#34892;
	  */
	public int getC_PaymentRequestLine_ID();

    /** Column name C_PaymentRequestLine_UU */
    public static final String COLUMNNAME_C_PaymentRequestLine_UU = "C_PaymentRequestLine_UU";

	/** Set &#20184;
&#27454;
&#30003;
&#35831;
&#21333;
&#34892;
UUID	  */
	public void setC_PaymentRequestLine_UU (String C_PaymentRequestLine_UU);

	/** Get &#20184;
&#27454;
&#30003;
&#35831;
&#21333;
&#34892;
UUID	  */
	public String getC_PaymentRequestLine_UU();

    /** Column name C_PaymentRequest_ID */
    public static final String COLUMNNAME_C_PaymentRequest_ID = "C_PaymentRequest_ID";

	/** Set &#20184;
&#27454;
&#30003;
&#35831;
&#21333;
	  */
	public void setC_PaymentRequest_ID (int C_PaymentRequest_ID);

	/** Get &#20184;
&#27454;
&#30003;
&#35831;
&#21333;
	  */
	public int getC_PaymentRequest_ID();

	public I_C_PaymentRequest getC_PaymentRequest() throws RuntimeException;

    /** Column name C_Payment_ID */
    public static final String COLUMNNAME_C_Payment_ID = "C_Payment_ID";

	/** Set Payment.
	  * Payment identifier
	  */
	public void setC_Payment_ID (int C_Payment_ID);

	/** Get Payment.
	  * Payment identifier
	  */
	public int getC_Payment_ID();

	public org.compiere.model.I_C_Payment getC_Payment() throws RuntimeException;

    /** Column name CashierNote */
    public static final String COLUMNNAME_CashierNote = "CashierNote";

	/** Set &#20986;
&#32435;
&#24847;
&#35265;
	  */
	public void setCashierNote (String CashierNote);

	/** Get &#20986;
&#32435;
&#24847;
&#35265;
	  */
	public String getCashierNote();

    /** Column name CashierRemark */
    public static final String COLUMNNAME_CashierRemark = "CashierRemark";

	/** Set &#20986;
&#32435;
&#22791;
&#27880;
	  */
	public void setCashierRemark (String CashierRemark);

	/** Get &#20986;
&#32435;
&#22791;
&#27880;
	  */
	public String getCashierRemark();

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

    /** Column name DifferenceAmt */
    public static final String COLUMNNAME_DifferenceAmt = "DifferenceAmt";

	/** Set Difference.
	  * Difference Amount
	  */
	public void setDifferenceAmt (BigDecimal DifferenceAmt);

	/** Get Difference.
	  * Difference Amount
	  */
	public BigDecimal getDifferenceAmt();

    /** Column name DiscountAmt */
    public static final String COLUMNNAME_DiscountAmt = "DiscountAmt";

	/** Set Discount Amount.
	  * Calculated amount of discount
	  */
	public void setDiscountAmt (BigDecimal DiscountAmt);

	/** Get Discount Amount.
	  * Calculated amount of discount
	  */
	public BigDecimal getDiscountAmt();

    /** Column name DueDate */
    public static final String COLUMNNAME_DueDate = "DueDate";

	/** Set Due Date.
	  * Date when the payment is due
	  */
	public void setDueDate (Timestamp DueDate);

	/** Get Due Date.
	  * Date when the payment is due
	  */
	public Timestamp getDueDate();

    /** Column name GrandTotal */
    public static final String COLUMNNAME_GrandTotal = "GrandTotal";

	/** Set Grand Total.
	  * Total amount of document
	  */
	public void setGrandTotal (BigDecimal GrandTotal);

	/** Get Grand Total.
	  * Total amount of document
	  */
	public BigDecimal getGrandTotal();

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

    /** Column name Line */
    public static final String COLUMNNAME_Line = "Line";

	/** Set Line No.
	  * Unique line for this document
	  */
	public void setLine (int Line);

	/** Get Line No.
	  * Unique line for this document
	  */
	public int getLine();

    /** Column name LineStatus */
    public static final String COLUMNNAME_LineStatus = "LineStatus";

	/** Set &#34892;
&#23457;
&#25209;
&#29366;
&#24577;
	  */
	public void setLineStatus (String LineStatus);

	/** Get &#34892;
&#23457;
&#25209;
&#29366;
&#24577;
	  */
	public String getLineStatus();

    /** Column name PayAmt */
    public static final String COLUMNNAME_PayAmt = "PayAmt";

	/** Set Payment amount.
	  * Amount being paid
	  */
	public void setPayAmt (BigDecimal PayAmt);

	/** Get Payment amount.
	  * Amount being paid
	  */
	public BigDecimal getPayAmt();

    /** Column name PayBank */
    public static final String COLUMNNAME_PayBank = "PayBank";

	/** Set &#25903;
&#20184;
&#38134;
&#34892;
	  */
	public void setPayBank (String PayBank);

	/** Get &#25903;
&#20184;
&#38134;
&#34892;
	  */
	public String getPayBank();

    /** Column name PaymentRule */
    public static final String COLUMNNAME_PaymentRule = "PaymentRule";

	/** Set Payment Rule.
	  * How you pay the invoice
	  */
	public void setPaymentRule (String PaymentRule);

	/** Get Payment Rule.
	  * How you pay the invoice
	  */
	public String getPaymentRule();

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

    /** Column name R_Info */
    public static final String COLUMNNAME_R_Info = "R_Info";

	/** Set Info.
	  * Response info
	  */
	public void setR_Info (String R_Info);

	/** Get Info.
	  * Response info
	  */
	public String getR_Info();

    /** Column name RebateAmt */
    public static final String COLUMNNAME_RebateAmt = "RebateAmt";

	/** Set &#19981;
&#33391;
&#21697;
&#25187;
&#27454;
/&#36820;
&#28857;
	  */
	public void setRebateAmt (BigDecimal RebateAmt);

	/** Get &#19981;
&#33391;
&#21697;
&#25187;
&#27454;
/&#36820;
&#28857;
	  */
	public BigDecimal getRebateAmt();

    /** Column name RequestAmt */
    public static final String COLUMNNAME_RequestAmt = "RequestAmt";

	/** Set Request Amount.
	  * Amount associated with this request
	  */
	public void setRequestAmt (BigDecimal RequestAmt);

	/** Get Request Amount.
	  * Amount associated with this request
	  */
	public BigDecimal getRequestAmt();

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
