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

/** Generated Model for C_Bill_Transaction
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_Bill_Transaction")
public class X_C_Bill_Transaction extends PO implements I_C_Bill_Transaction, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260406L;

    /** Standard Constructor */
    public X_C_Bill_Transaction (Properties ctx, int C_Bill_Transaction_ID, String trxName)
    {
      super (ctx, C_Bill_Transaction_ID, trxName);
      /** if (C_Bill_Transaction_ID == 0)
        {
			setAffairType (null);
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillType (null);
			setBusinessStatus (null);
			setC_Bill_Pool_DocumentNo (null);
			setC_Bill_Transaction_ID (0);
			setC_Currency_ID (0);
			setChargeAmt (Env.ZERO);
			setDocumentNo (null);
			setInterestAmt (Env.ZERO);
			setIsTransferable (true);
// Y
			setPosted (false);
			setProcessed (false);
// N
			setSettleAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTransactionType (null);
        } */
    }

    /** Standard Constructor */
    public X_C_Bill_Transaction (Properties ctx, int C_Bill_Transaction_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_Bill_Transaction_ID, trxName, virtualColumns);
      /** if (C_Bill_Transaction_ID == 0)
        {
			setAffairType (null);
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillType (null);
			setBusinessStatus (null);
			setC_Bill_Pool_DocumentNo (null);
			setC_Bill_Transaction_ID (0);
			setC_Currency_ID (0);
			setChargeAmt (Env.ZERO);
			setDocumentNo (null);
			setInterestAmt (Env.ZERO);
			setIsTransferable (true);
// Y
			setPosted (false);
			setProcessed (false);
// N
			setSettleAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTransactionType (null);
        } */
    }

    /** Standard Constructor */
    public X_C_Bill_Transaction (Properties ctx, String C_Bill_Transaction_UU, String trxName)
    {
      super (ctx, C_Bill_Transaction_UU, trxName);
      /** if (C_Bill_Transaction_UU == null)
        {
			setAffairType (null);
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillType (null);
			setBusinessStatus (null);
			setC_Bill_Pool_DocumentNo (null);
			setC_Bill_Transaction_ID (0);
			setC_Currency_ID (0);
			setChargeAmt (Env.ZERO);
			setDocumentNo (null);
			setInterestAmt (Env.ZERO);
			setIsTransferable (true);
// Y
			setPosted (false);
			setProcessed (false);
// N
			setSettleAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTransactionType (null);
        } */
    }

    /** Standard Constructor */
    public X_C_Bill_Transaction (Properties ctx, String C_Bill_Transaction_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_Bill_Transaction_UU, trxName, virtualColumns);
      /** if (C_Bill_Transaction_UU == null)
        {
			setAffairType (null);
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillType (null);
			setBusinessStatus (null);
			setC_Bill_Pool_DocumentNo (null);
			setC_Bill_Transaction_ID (0);
			setC_Currency_ID (0);
			setChargeAmt (Env.ZERO);
			setDocumentNo (null);
			setInterestAmt (Env.ZERO);
			setIsTransferable (true);
// Y
			setPosted (false);
			setProcessed (false);
// N
			setSettleAmt (Env.ZERO);
			setSubPackageAmt (Env.ZERO);
			setTransactionType (null);
        } */
    }

    /** Load Constructor */
    public X_C_Bill_Transaction (Properties ctx, ResultSet rs, String trxName)
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
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_C_Bill_Transaction[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Acceptor_Id.
		@param Acceptor_Id Acceptor_Id
	*/
	public void setAcceptor_Id (String Acceptor_Id)
	{
		set_Value (COLUMNNAME_Acceptor_Id, Acceptor_Id);
	}

	/** Get Acceptor_Id.
		@return Acceptor_Id	  */
	public String getAcceptor_Id()
	{
		return (String)get_Value(COLUMNNAME_Acceptor_Id);
	}

	/** Set Account No.
		@param AccountNo Account Number
	*/
	public void setAccountNo (String AccountNo)
	{
		set_Value (COLUMNNAME_AccountNo, AccountNo);
	}

	/** Get Account No.
		@return Account Number
	  */
	public String getAccountNo()
	{
		return (String)get_Value(COLUMNNAME_AccountNo);
	}

	/** &#24212;&#20184;&#31080;&#25454;&#20107;&#21153; = P */
	public static final String AFFAIRTYPE_应付票据事务 = "P";
	/** &#24212;&#25910;&#31080;&#25454;&#20107;&#21153; = R */
	public static final String AFFAIRTYPE_应收票据事务 = "R";
	/** Set AffairType.
		@param AffairType AffairType
	*/
	public void setAffairType (String AffairType)
	{

		set_Value (COLUMNNAME_AffairType, AffairType);
	}

	/** Get AffairType.
		@return AffairType	  */
	public String getAffairType()
	{
		return (String)get_Value(COLUMNNAME_AffairType);
	}

	/** Set BillAmt.
		@param BillAmt BillAmt
	*/
	public void setBillAmt (BigDecimal BillAmt)
	{
		set_Value (COLUMNNAME_BillAmt, BillAmt);
	}

	/** Get BillAmt.
		@return BillAmt	  */
	public BigDecimal getBillAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BillAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set BillPackageNo.
		@param BillPackageNo BillPackageNo
	*/
	public void setBillPackageNo (String BillPackageNo)
	{
		set_Value (COLUMNNAME_BillPackageNo, BillPackageNo);
	}

	/** Get BillPackageNo.
		@return BillPackageNo	  */
	public String getBillPackageNo()
	{
		return (String)get_Value(COLUMNNAME_BillPackageNo);
	}

	/** Set BillRate.
		@param BillRate BillRate
	*/
	public void setBillRate (BigDecimal BillRate)
	{
		set_Value (COLUMNNAME_BillRate, BillRate);
	}

	/** Get BillRate.
		@return BillRate	  */
	public BigDecimal getBillRate()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BillRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** &#38134;&#31080; = B */
	public static final String BILLTYPE_银票 = "B";
	/** &#21830;&#31080; = C */
	public static final String BILLTYPE_商票 = "C";
	/** &#37329;&#31080; = G */
	public static final String BILLTYPE_金票 = "G";
	/** Set BillType.
		@param BillType BillType
	*/
	public void setBillType (String BillType)
	{

		set_Value (COLUMNNAME_BillType, BillType);
	}

	/** Get BillType.
		@return BillType	  */
	public String getBillType()
	{
		return (String)get_Value(COLUMNNAME_BillType);
	}

	/** Set BusinessDate.
		@param BusinessDate BusinessDate
	*/
	public void setBusinessDate (Timestamp BusinessDate)
	{
		set_Value (COLUMNNAME_BusinessDate, BusinessDate);
	}

	/** Get BusinessDate.
		@return BusinessDate	  */
	public Timestamp getBusinessDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_BusinessDate);
	}

	/** &#24050;&#36148;&#29616; = C */
	public static final String BUSINESSSTATUS_已贴现 = "C";
	/** &#24050;&#32972;&#20070; = D */
	public static final String BUSINESSSTATUS_已背书 = "D";
	/** &#32972;&#20070;&#20013; = E */
	public static final String BUSINESSSTATUS_背书中 = "E";
	/** &#36148;&#29616;&#20013; = G */
	public static final String BUSINESSSTATUS_贴现中 = "G";
	/** &#24050;&#31614;&#25910; = H */
	public static final String BUSINESSSTATUS_已签收 = "H";
	/** &#24050;&#32467;&#28165; = L */
	public static final String BUSINESSSTATUS_已结清 = "L";
	/** &#25105;&#26041;&#25298;&#20184; = M */
	public static final String BUSINESSSTATUS_我方拒付 = "M";
	/** &#24050;&#31614;&#21457; = P */
	public static final String BUSINESSSTATUS_已签发 = "P";
	/** &#24050;&#36864;&#22238; = R */
	public static final String BUSINESSSTATUS_已退回 = "R";
	/** &#32467;&#31639;&#20013; = S */
	public static final String BUSINESSSTATUS_结算中 = "S";
	/** &#23545;&#26041;&#25298;&#20184; = V */
	public static final String BUSINESSSTATUS_对方拒付 = "V";
	/** Set BusinessStatus.
		@param BusinessStatus BusinessStatus
	*/
	public void setBusinessStatus (String BusinessStatus)
	{

		set_Value (COLUMNNAME_BusinessStatus, BusinessStatus);
	}

	/** Get BusinessStatus.
		@return BusinessStatus	  */
	public String getBusinessStatus()
	{
		return (String)get_Value(COLUMNNAME_BusinessStatus);
	}

	public org.compiere.model.I_C_BP_BankAccount getC_BP_BankAccount() throws RuntimeException
	{
		return (org.compiere.model.I_C_BP_BankAccount)MTable.get(getCtx(), org.compiere.model.I_C_BP_BankAccount.Table_ID)
			.getPO(getC_BP_BankAccount_ID(), get_TrxName());
	}

	/** Set Partner Bank Account.
		@param C_BP_BankAccount_ID Bank Account of the Business Partner
	*/
	public void setC_BP_BankAccount_ID (int C_BP_BankAccount_ID)
	{
		if (C_BP_BankAccount_ID < 1)
			set_Value (COLUMNNAME_C_BP_BankAccount_ID, null);
		else
			set_Value (COLUMNNAME_C_BP_BankAccount_ID, Integer.valueOf(C_BP_BankAccount_ID));
	}

	/** Get Partner Bank Account.
		@return Bank Account of the Business Partner
	  */
	public int getC_BP_BankAccount_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BP_BankAccount_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getC_BPartner_ID(), get_TrxName());
	}

	/** Set Business Partner.
		@param C_BPartner_ID Identifies a Business Partner
	*/
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1)
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner.
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_BankAccount getC_BankAccount() throws RuntimeException
	{
		return (org.compiere.model.I_C_BankAccount)MTable.get(getCtx(), org.compiere.model.I_C_BankAccount.Table_ID)
			.getPO(getC_BankAccount_ID(), get_TrxName());
	}

	/** Set Bank Account.
		@param C_BankAccount_ID Account at the Bank
	*/
	public void setC_BankAccount_ID (int C_BankAccount_ID)
	{
		if (C_BankAccount_ID < 1)
			set_Value (COLUMNNAME_C_BankAccount_ID, null);
		else
			set_Value (COLUMNNAME_C_BankAccount_ID, Integer.valueOf(C_BankAccount_ID));
	}

	/** Get Bank Account.
		@return Account at the Bank
	  */
	public int getC_BankAccount_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankAccount_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_Bill_Pool_DocumentNo.
		@param C_Bill_Pool_DocumentNo C_Bill_Pool_DocumentNo
	*/
	public void setC_Bill_Pool_DocumentNo (String C_Bill_Pool_DocumentNo)
	{
		set_Value (COLUMNNAME_C_Bill_Pool_DocumentNo, C_Bill_Pool_DocumentNo);
	}

	/** Get C_Bill_Pool_DocumentNo.
		@return C_Bill_Pool_DocumentNo	  */
	public String getC_Bill_Pool_DocumentNo()
	{
		return (String)get_Value(COLUMNNAME_C_Bill_Pool_DocumentNo);
	}

	public I_C_Bill_Pool getC_Bill_Pool() throws RuntimeException
	{
		return (I_C_Bill_Pool)MTable.get(getCtx(), I_C_Bill_Pool.Table_ID)
			.getPO(getC_Bill_Pool_ID(), get_TrxName());
	}

	/** Set C_Bill_Pool.
		@param C_Bill_Pool_ID C_Bill_Pool
	*/
	public void setC_Bill_Pool_ID (int C_Bill_Pool_ID)
	{
		if (C_Bill_Pool_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Bill_Pool_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Bill_Pool_ID, Integer.valueOf(C_Bill_Pool_ID));
	}

	/** Get C_Bill_Pool.
		@return C_Bill_Pool	  */
	public int getC_Bill_Pool_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Bill_Pool_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_Bill_Transaction.
		@param C_Bill_Transaction_ID C_Bill_Transaction
	*/
	public void setC_Bill_Transaction_ID (int C_Bill_Transaction_ID)
	{
		if (C_Bill_Transaction_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Bill_Transaction_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Bill_Transaction_ID, Integer.valueOf(C_Bill_Transaction_ID));
	}

	/** Get C_Bill_Transaction.
		@return C_Bill_Transaction	  */
	public int getC_Bill_Transaction_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Bill_Transaction_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_Bill_Transaction_UU.
		@param C_Bill_Transaction_UU C_Bill_Transaction_UU
	*/
	public void setC_Bill_Transaction_UU (String C_Bill_Transaction_UU)
	{
		set_Value (COLUMNNAME_C_Bill_Transaction_UU, C_Bill_Transaction_UU);
	}

	/** Get C_Bill_Transaction_UU.
		@return C_Bill_Transaction_UU	  */
	public String getC_Bill_Transaction_UU()
	{
		return (String)get_Value(COLUMNNAME_C_Bill_Transaction_UU);
	}

	public org.compiere.model.I_C_Charge getC_Charge() throws RuntimeException
	{
		return (org.compiere.model.I_C_Charge)MTable.get(getCtx(), org.compiere.model.I_C_Charge.Table_ID)
			.getPO(getC_Charge_ID(), get_TrxName());
	}

	/** Set Charge.
		@param C_Charge_ID Additional document charges
	*/
	public void setC_Charge_ID (int C_Charge_ID)
	{
		if (C_Charge_ID < 1)
			set_Value (COLUMNNAME_C_Charge_ID, null);
		else
			set_Value (COLUMNNAME_C_Charge_ID, Integer.valueOf(C_Charge_ID));
	}

	/** Get Charge.
		@return Additional document charges
	  */
	public int getC_Charge_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Charge_ID);
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

	public org.compiere.model.I_C_Invoice getC_Invoice() throws RuntimeException
	{
		return (org.compiere.model.I_C_Invoice)MTable.get(getCtx(), org.compiere.model.I_C_Invoice.Table_ID)
			.getPO(getC_Invoice_ID(), get_TrxName());
	}

	/** Set Invoice.
		@param C_Invoice_ID Invoice Identifier
	*/
	public void setC_Invoice_ID (int C_Invoice_ID)
	{
		if (C_Invoice_ID < 1)
			set_Value (COLUMNNAME_C_Invoice_ID, null);
		else
			set_Value (COLUMNNAME_C_Invoice_ID, Integer.valueOf(C_Invoice_ID));
	}

	/** Get Invoice.
		@return Invoice Identifier
	  */
	public int getC_Invoice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Invoice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Charge amount.
		@param ChargeAmt Charge Amount
	*/
	public void setChargeAmt (BigDecimal ChargeAmt)
	{
		set_Value (COLUMNNAME_ChargeAmt, ChargeAmt);
	}

	/** Get Charge amount.
		@return Charge Amount
	  */
	public BigDecimal getChargeAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ChargeAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set ContractNo.
		@param ContractNo ContractNo
	*/
	public void setContractNo (String ContractNo)
	{
		set_Value (COLUMNNAME_ContractNo, ContractNo);
	}

	/** Get ContractNo.
		@return ContractNo	  */
	public String getContractNo()
	{
		return (String)get_Value(COLUMNNAME_ContractNo);
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
		set_Value (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo()
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	/** Drawer_Id AD_Reference_ID=200175 */
	public static final int DRAWER_ID_AD_Reference_ID=200175;
	/** Set Drawer_Id.
		@param Drawer_Id Drawer_Id
	*/
	public void setDrawer_Id (String Drawer_Id)
	{

		set_Value (COLUMNNAME_Drawer_Id, Drawer_Id);
	}

	/** Get Drawer_Id.
		@return Drawer_Id	  */
	public String getDrawer_Id()
	{
		return (String)get_Value(COLUMNNAME_Drawer_Id);
	}

	/** Endorsee_Id AD_Reference_ID=200175 */
	public static final int ENDORSEE_ID_AD_Reference_ID=200175;
	/** Set Endorsee_Id.
		@param Endorsee_Id Endorsee_Id
	*/
	public void setEndorsee_Id (String Endorsee_Id)
	{

		set_Value (COLUMNNAME_Endorsee_Id, Endorsee_Id);
	}

	/** Get Endorsee_Id.
		@return Endorsee_Id	  */
	public String getEndorsee_Id()
	{
		return (String)get_Value(COLUMNNAME_Endorsee_Id);
	}

	/** Endorser_Id AD_Reference_ID=200175 */
	public static final int ENDORSER_ID_AD_Reference_ID=200175;
	/** Set Endorser_Id.
		@param Endorser_Id Endorser_Id
	*/
	public void setEndorser_Id (String Endorser_Id)
	{

		set_Value (COLUMNNAME_Endorser_Id, Endorser_Id);
	}

	/** Get Endorser_Id.
		@return Endorser_Id	  */
	public String getEndorser_Id()
	{
		return (String)get_Value(COLUMNNAME_Endorser_Id);
	}

	/** Set Interest Amount.
		@param InterestAmt Interest Amount
	*/
	public void setInterestAmt (BigDecimal InterestAmt)
	{
		set_Value (COLUMNNAME_InterestAmt, InterestAmt);
	}

	/** Get Interest Amount.
		@return Interest Amount
	  */
	public BigDecimal getInterestAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_InterestAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set IsTransferable.
		@param IsTransferable IsTransferable
	*/
	public void setIsTransferable (boolean IsTransferable)
	{
		set_Value (COLUMNNAME_IsTransferable, Boolean.valueOf(IsTransferable));
	}

	/** Get IsTransferable.
		@return IsTransferable	  */
	public boolean isTransferable()
	{
		Object oo = get_Value(COLUMNNAME_IsTransferable);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set MaturityDate.
		@param MaturityDate MaturityDate
	*/
	public void setMaturityDate (Timestamp MaturityDate)
	{
		set_Value (COLUMNNAME_MaturityDate, MaturityDate);
	}

	/** Get MaturityDate.
		@return MaturityDate	  */
	public Timestamp getMaturityDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_MaturityDate);
	}

	/** Set Original Transaction ID.
		@param Orig_TrxID Original Transaction ID
	*/
	public void setOrig_TrxID (String Orig_TrxID)
	{
		set_Value (COLUMNNAME_Orig_TrxID, Orig_TrxID);
	}

	/** Get Original Transaction ID.
		@return Original Transaction ID
	  */
	public String getOrig_TrxID()
	{
		return (String)get_Value(COLUMNNAME_Orig_TrxID);
	}

	/** Set Posted.
		@param Posted Posting status
	*/
	public void setPosted (boolean Posted)
	{
		set_ValueNoCheck (COLUMNNAME_Posted, Boolean.valueOf(Posted));
	}

	/** Get Posted.
		@return Posting status
	  */
	public boolean isPosted()
	{
		Object oo = get_Value(COLUMNNAME_Posted);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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

	/** Set ProjectNo.
		@param ProjectNo ProjectNo
	*/
	public void setProjectNo (String ProjectNo)
	{
		set_Value (COLUMNNAME_ProjectNo, ProjectNo);
	}

	/** Get ProjectNo.
		@return ProjectNo	  */
	public String getProjectNo()
	{
		return (String)get_Value(COLUMNNAME_ProjectNo);
	}

	/** Set Receiver_Id.
		@param Receiver_Id Receiver_Id
	*/
	public void setReceiver_Id (String Receiver_Id)
	{
		set_Value (COLUMNNAME_Receiver_Id, Receiver_Id);
	}

	/** Get Receiver_Id.
		@return Receiver_Id	  */
	public String getReceiver_Id()
	{
		return (String)get_Value(COLUMNNAME_Receiver_Id);
	}

	/** Set Routing No.
		@param RoutingNo Bank Routing Number
	*/
	public void setRoutingNo (String RoutingNo)
	{
		set_Value (COLUMNNAME_RoutingNo, RoutingNo);
	}

	/** Get Routing No.
		@return Bank Routing Number
	  */
	public String getRoutingNo()
	{
		return (String)get_Value(COLUMNNAME_RoutingNo);
	}

	/** Set SettleAmt.
		@param SettleAmt SettleAmt
	*/
	public void setSettleAmt (BigDecimal SettleAmt)
	{
		set_Value (COLUMNNAME_SettleAmt, SettleAmt);
	}

	/** Get SettleAmt.
		@return SettleAmt	  */
	public BigDecimal getSettleAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_SettleAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set SettleCode.
		@param SettleCode SettleCode
	*/
	public void setSettleCode (String SettleCode)
	{
		set_Value (COLUMNNAME_SettleCode, SettleCode);
	}

	/** Get SettleCode.
		@return SettleCode	  */
	public String getSettleCode()
	{
		return (String)get_Value(COLUMNNAME_SettleCode);
	}

	/** Set SubPackageAmt.
		@param SubPackageAmt SubPackageAmt
	*/
	public void setSubPackageAmt (BigDecimal SubPackageAmt)
	{
		set_Value (COLUMNNAME_SubPackageAmt, SubPackageAmt);
	}

	/** Get SubPackageAmt.
		@return SubPackageAmt	  */
	public BigDecimal getSubPackageAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_SubPackageAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** &#31614;&#25910; = A */
	public static final String TRANSACTIONTYPE_签收 = "A";
	/** &#36148;&#29616; = C */
	public static final String TRANSACTIONTYPE_贴现 = "C";
	/** &#36864;&#31080; = D */
	public static final String TRANSACTIONTYPE_退票 = "D";
	/** &#32972;&#20070; = E */
	public static final String TRANSACTIONTYPE_背书 = "E";
	/** &#31614;&#21457; = I */
	public static final String TRANSACTIONTYPE_签发 = "I";
	/** &#20184;&#27454; = P */
	public static final String TRANSACTIONTYPE_付款 = "P";
	/** &#25910;&#27454; = R */
	public static final String TRANSACTIONTYPE_收款 = "R";
	/** Set TransactionType.
		@param TransactionType TransactionType
	*/
	public void setTransactionType (String TransactionType)
	{

		set_Value (COLUMNNAME_TransactionType, TransactionType);
	}

	/** Get TransactionType.
		@return TransactionType	  */
	public String getTransactionType()
	{
		return (String)get_Value(COLUMNNAME_TransactionType);
	}
}