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

/** Generated Model for C_Bill_Pool
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_Bill_Pool")
public class X_C_Bill_Pool extends PO implements I_C_Bill_Pool, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260408L;

    /** Standard Constructor */
    public X_C_Bill_Pool (Properties ctx, int C_Bill_Pool_ID, String trxName)
    {
      super (ctx, C_Bill_Pool_ID, trxName);
      /** if (C_Bill_Pool_ID == 0)
        {
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillStatus (null);
			setBillType (null);
			setC_Bill_Pool_ID (0);
			setC_DocType_ID (0);
			setCirculationStatus (null);
// N
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsReceipt (true);
// Y
			setIsRecourse (true);
// Y
			setIsRevocable (true);
// Y
			setIsSplittable (true);
// Y
			setIsTransferable (true);
// Y
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_Bill_Pool (Properties ctx, int C_Bill_Pool_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_Bill_Pool_ID, trxName, virtualColumns);
      /** if (C_Bill_Pool_ID == 0)
        {
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillStatus (null);
			setBillType (null);
			setC_Bill_Pool_ID (0);
			setC_DocType_ID (0);
			setCirculationStatus (null);
// N
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsReceipt (true);
// Y
			setIsRecourse (true);
// Y
			setIsRevocable (true);
// Y
			setIsSplittable (true);
// Y
			setIsTransferable (true);
// Y
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_Bill_Pool (Properties ctx, String C_Bill_Pool_UU, String trxName)
    {
      super (ctx, C_Bill_Pool_UU, trxName);
      /** if (C_Bill_Pool_UU == null)
        {
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillStatus (null);
			setBillType (null);
			setC_Bill_Pool_ID (0);
			setC_DocType_ID (0);
			setCirculationStatus (null);
// N
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsReceipt (true);
// Y
			setIsRecourse (true);
// Y
			setIsRevocable (true);
// Y
			setIsSplittable (true);
// Y
			setIsTransferable (true);
// Y
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_Bill_Pool (Properties ctx, String C_Bill_Pool_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_Bill_Pool_UU, trxName, virtualColumns);
      /** if (C_Bill_Pool_UU == null)
        {
			setBillAmt (Env.ZERO);
			setBillPackageNo (null);
			setBillStatus (null);
			setBillType (null);
			setC_Bill_Pool_ID (0);
			setC_DocType_ID (0);
			setCirculationStatus (null);
// N
			setDocumentNo (null);
			setIsApproved (false);
// N
			setIsReceipt (true);
// Y
			setIsRecourse (true);
// Y
			setIsRevocable (true);
// Y
			setIsSplittable (true);
// Y
			setIsTransferable (true);
// Y
			setProcessed (false);
// N
        } */
    }

    /** Load Constructor */
    public X_C_Bill_Pool (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_C_Bill_Pool[")
        .append(get_ID()).append("]");
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

	/** Set &#25215;&#20817;&#21327;&#35758;&#32534;&#21495; .
		@param AcceptanceAgreementNo &#25215;&#20817;&#21327;&#35758;&#32534;&#21495; 
	*/
	public void setAcceptanceAgreementNo (String AcceptanceAgreementNo)
	{
		set_Value (COLUMNNAME_AcceptanceAgreementNo, AcceptanceAgreementNo);
	}

	/** Get &#25215;&#20817;&#21327;&#35758;&#32534;&#21495; .
		@return &#25215;&#20817;&#21327;&#35758;&#32534;&#21495; 	  */
	public String getAcceptanceAgreementNo()
	{
		return (String)get_Value(COLUMNNAME_AcceptanceAgreementNo);
	}

	/** Set &#25215;&#20817;&#26085;&#26399;.
		@param AcceptanceDate &#25215;&#20817;&#26085;&#26399;
	*/
	public void setAcceptanceDate (Timestamp AcceptanceDate)
	{
		set_Value (COLUMNNAME_AcceptanceDate, AcceptanceDate);
	}

	/** Get &#25215;&#20817;&#26085;&#26399;.
		@return &#25215;&#20817;&#26085;&#26399;	  */
	public Timestamp getAcceptanceDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_AcceptanceDate);
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

	/** Set BillDate.
		@param BillDate BillDate
	*/
	public void setBillDate (Timestamp BillDate)
	{
		set_Value (COLUMNNAME_BillDate, BillDate);
	}

	/** Get BillDate.
		@return BillDate	  */
	public Timestamp getBillDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_BillDate);
	}

	/** Set BillNumber.
		@param BillNumber BillNumber
	*/
	public void setBillNumber (String BillNumber)
	{
		set_Value (COLUMNNAME_BillNumber, BillNumber);
	}

	/** Get BillNumber.
		@return BillNumber	  */
	public String getBillNumber()
	{
		return (String)get_Value(COLUMNNAME_BillNumber);
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

	/** &#24050;&#25215;&#20817; = A */
	public static final String BILLSTATUS_已承兑 = "A";
	/** &#24050;&#20986;&#31080; = I */
	public static final String BILLSTATUS_已出票 = "I";
	/** &#24050;&#21040;&#26399; = M */
	public static final String BILLSTATUS_已到期 = "M";
	/** &#24050;&#25910;&#31080; = R */
	public static final String BILLSTATUS_已收票 = "R";
	/** &#24050;&#32467;&#28165; = S */
	public static final String BILLSTATUS_已结清 = "S";
	/** &#24050;&#32456;&#27490; = T */
	public static final String BILLSTATUS_已终止 = "T";
	/** Set BillStatus.
		@param BillStatus BillStatus
	*/
	public void setBillStatus (String BillStatus)
	{

		set_Value (COLUMNNAME_BillStatus, BillStatus);
	}

	/** Get BillStatus.
		@return BillStatus	  */
	public String getBillStatus()
	{
		return (String)get_Value(COLUMNNAME_BillStatus);
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
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
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

	/** &#24453;&#25910;&#31080; = A */
	public static final String CIRCULATIONSTATUS_待收票 = "A";
	/** &#24453;&#36174;&#22238; = B */
	public static final String CIRCULATIONSTATUS_待赎回 = "B";
	/** &#25176;&#25910;&#22312;&#36884; = C */
	public static final String CIRCULATIONSTATUS_托收在途 = "C";
	/** &#24050;&#32467;&#26463; = E */
	public static final String CIRCULATIONSTATUS_已结束 = "E";
	/** &#24050;&#38145;&#23450; = L */
	public static final String CIRCULATIONSTATUS_已锁定 = "L";
	/** &#21487;&#27969;&#36890; = N */
	public static final String CIRCULATIONSTATUS_可流通 = "N";
	/** &#24050;&#36136;&#25276; = P */
	public static final String CIRCULATIONSTATUS_已质押 = "P";
	/** &#36861;&#32034;&#20013; = R */
	public static final String CIRCULATIONSTATUS_追索中 = "R";
	/** &#19981;&#21487;&#36716;&#35753; = T */
	public static final String CIRCULATIONSTATUS_不可转让 = "T";
	/** Set CirculationStatus.
		@param CirculationStatus CirculationStatus
	*/
	public void setCirculationStatus (String CirculationStatus)
	{

		set_Value (COLUMNNAME_CirculationStatus, CirculationStatus);
	}

	/** Get CirculationStatus.
		@return CirculationStatus	  */
	public String getCirculationStatus()
	{
		return (String)get_Value(COLUMNNAME_CirculationStatus);
	}

	/** Set DeliveryDate.
		@param DeliveryDate DeliveryDate
	*/
	public void setDeliveryDate (Timestamp DeliveryDate)
	{
		set_Value (COLUMNNAME_DeliveryDate, DeliveryDate);
	}

	/** Get DeliveryDate.
		@return DeliveryDate	  */
	public Timestamp getDeliveryDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_DeliveryDate);
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

	/** &#22996;&#25176;&#25910;&#27454; = E */
	public static final String ENDORSERTYPE_委托收款 = "E";
	/** &#26080; = N */
	public static final String ENDORSERTYPE_无 = "N";
	/** &#36136;&#25276; = P */
	public static final String ENDORSERTYPE_质押 = "P";
	/** &#36716;&#35753; = T */
	public static final String ENDORSERTYPE_转让 = "T";
	/** Set EndorserType.
		@param EndorserType EndorserType
	*/
	public void setEndorserType (String EndorserType)
	{

		set_Value (COLUMNNAME_EndorserType, EndorserType);
	}

	/** Get EndorserType.
		@return EndorserType	  */
	public String getEndorserType()
	{
		return (String)get_Value(COLUMNNAME_EndorserType);
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

	public org.compiere.model.I_C_BPartner getHolder() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getHolder_Id(), get_TrxName());
	}

	/** Set Holder_Id.
		@param Holder_Id Holder_Id
	*/
	public void setHolder_Id (int Holder_Id)
	{
		set_Value (COLUMNNAME_Holder_Id, Integer.valueOf(Holder_Id));
	}

	/** Get Holder_Id.
		@return Holder_Id	  */
	public int getHolder_Id()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Holder_Id);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Receipt.
		@param IsReceipt This is a sales transaction (receipt)
	*/
	public void setIsReceipt (boolean IsReceipt)
	{
		set_Value (COLUMNNAME_IsReceipt, Boolean.valueOf(IsReceipt));
	}

	/** Get Receipt.
		@return This is a sales transaction (receipt)
	  */
	public boolean isReceipt()
	{
		Object oo = get_Value(COLUMNNAME_IsReceipt);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsRecourse.
		@param IsRecourse IsRecourse
	*/
	public void setIsRecourse (boolean IsRecourse)
	{
		set_Value (COLUMNNAME_IsRecourse, Boolean.valueOf(IsRecourse));
	}

	/** Get IsRecourse.
		@return IsRecourse	  */
	public boolean isRecourse()
	{
		Object oo = get_Value(COLUMNNAME_IsRecourse);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsRevocable.
		@param IsRevocable IsRevocable
	*/
	public void setIsRevocable (boolean IsRevocable)
	{
		set_Value (COLUMNNAME_IsRevocable, Boolean.valueOf(IsRevocable));
	}

	/** Get IsRevocable.
		@return IsRevocable	  */
	public boolean isRevocable()
	{
		Object oo = get_Value(COLUMNNAME_IsRevocable);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsSplittable.
		@param IsSplittable IsSplittable
	*/
	public void setIsSplittable (boolean IsSplittable)
	{
		set_Value (COLUMNNAME_IsSplittable, Boolean.valueOf(IsSplittable));
	}

	/** Get IsSplittable.
		@return IsSplittable	  */
	public boolean isSplittable()
	{
		Object oo = get_Value(COLUMNNAME_IsSplittable);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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

	/** Set MaturityAmt.
		@param MaturityAmt MaturityAmt
	*/
	public void setMaturityAmt (BigDecimal MaturityAmt)
	{
		set_Value (COLUMNNAME_MaturityAmt, MaturityAmt);
	}

	/** Get MaturityAmt.
		@return MaturityAmt	  */
	public BigDecimal getMaturityAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaturityAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Owner_Org_ID.
		@param Owner_Org_ID Owner_Org_ID
	*/
	public void setOwner_Org_ID (int Owner_Org_ID)
	{
		if (Owner_Org_ID < 1)
			set_Value (COLUMNNAME_Owner_Org_ID, null);
		else
			set_Value (COLUMNNAME_Owner_Org_ID, Integer.valueOf(Owner_Org_ID));
	}

	/** Get Owner_Org_ID.
		@return Owner_Org_ID	  */
	public int getOwner_Org_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Owner_Org_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Payee_Id.
		@param Payee_Id Payee_Id
	*/
	public void setPayee_Id (String Payee_Id)
	{
		set_Value (COLUMNNAME_Payee_Id, Payee_Id);
	}

	/** Get Payee_Id.
		@return Payee_Id	  */
	public String getPayee_Id()
	{
		return (String)get_Value(COLUMNNAME_Payee_Id);
	}

	/** Set PaymentTermDays.
		@param PaymentTermDays PaymentTermDays
	*/
	public void setPaymentTermDays (BigDecimal PaymentTermDays)
	{
		set_Value (COLUMNNAME_PaymentTermDays, PaymentTermDays);
	}

	/** Get PaymentTermDays.
		@return PaymentTermDays	  */
	public BigDecimal getPaymentTermDays()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PaymentTermDays);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set ReceiveDate.
		@param ReceiveDate ReceiveDate
	*/
	public void setReceiveDate (Timestamp ReceiveDate)
	{
		set_Value (COLUMNNAME_ReceiveDate, ReceiveDate);
	}

	/** Get ReceiveDate.
		@return ReceiveDate	  */
	public Timestamp getReceiveDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ReceiveDate);
	}

	/** Set Settle_Org_ID.
		@param Settle_Org_ID Settle_Org_ID
	*/
	public void setSettle_Org_ID (int Settle_Org_ID)
	{
		if (Settle_Org_ID < 1)
			set_Value (COLUMNNAME_Settle_Org_ID, null);
		else
			set_Value (COLUMNNAME_Settle_Org_ID, Integer.valueOf(Settle_Org_ID));
	}

	/** Get Settle_Org_ID.
		@return Settle_Org_ID	  */
	public int getSettle_Org_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Settle_Org_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SubBillEndNo.
		@param SubBillEndNo SubBillEndNo
	*/
	public void setSubBillEndNo (String SubBillEndNo)
	{
		set_Value (COLUMNNAME_SubBillEndNo, SubBillEndNo);
	}

	/** Get SubBillEndNo.
		@return SubBillEndNo	  */
	public String getSubBillEndNo()
	{
		return (String)get_Value(COLUMNNAME_SubBillEndNo);
	}

	/** Set SubBillStartNo.
		@param SubBillStartNo SubBillStartNo
	*/
	public void setSubBillStartNo (String SubBillStartNo)
	{
		set_Value (COLUMNNAME_SubBillStartNo, SubBillStartNo);
	}

	/** Get SubBillStartNo.
		@return SubBillStartNo	  */
	public String getSubBillStartNo()
	{
		return (String)get_Value(COLUMNNAME_SubBillStartNo);
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
}