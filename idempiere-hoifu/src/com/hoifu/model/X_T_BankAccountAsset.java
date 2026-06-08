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
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for T_BankAccountAsset
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="T_BankAccountAsset")
public class X_T_BankAccountAsset extends PO implements I_T_BankAccountAsset, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260416L;

    /** Standard Constructor */
    public X_T_BankAccountAsset (Properties ctx, String T_BankAccountAsset_UU, String trxName)
    {
      super (ctx, T_BankAccountAsset_UU, trxName);
      /** if (T_BankAccountAsset_UU == null)
        {
			setAD_PInstance_ID (0);
			setAssetType (null);
        } */
    }

    /** Standard Constructor */
    public X_T_BankAccountAsset (Properties ctx, String T_BankAccountAsset_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, T_BankAccountAsset_UU, trxName, virtualColumns);
      /** if (T_BankAccountAsset_UU == null)
        {
			setAD_PInstance_ID (0);
			setAssetType (null);
        } */
    }

    /** Load Constructor */
    public X_T_BankAccountAsset (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 4 - System
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
      StringBuilder sb = new StringBuilder ("X_T_BankAccountAsset[")
        .append(get_UUID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_PInstance getAD_PInstance() throws RuntimeException
	{
		return (org.compiere.model.I_AD_PInstance)MTable.get(getCtx(), org.compiere.model.I_AD_PInstance.Table_ID)
			.getPO(getAD_PInstance_ID(), get_TrxName());
	}

	/** Set Process Instance.
		@param AD_PInstance_ID Instance of the process
	*/
	public void setAD_PInstance_ID (int AD_PInstance_ID)
	{
		if (AD_PInstance_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_PInstance_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_PInstance_ID, Integer.valueOf(AD_PInstance_ID));
	}

	/** Get Process Instance.
		@return Instance of the process
	  */
	public int getAD_PInstance_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_PInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AccountName.
		@param AccountName AccountName
	*/
	public void setAccountName (String AccountName)
	{
		set_Value (COLUMNNAME_AccountName, AccountName);
	}

	/** Get AccountName.
		@return AccountName	  */
	public String getAccountName()
	{
		return (String)get_Value(COLUMNNAME_AccountName);
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

	/** Set Account Key.
		@param AccountValue Key of Account Element
	*/
	public void setAccountValue (String AccountValue)
	{
		set_ValueNoCheck (COLUMNNAME_AccountValue, AccountValue);
	}

	/** Get Account Key.
		@return Key of Account Element
	  */
	public String getAccountValue()
	{
		return (String)get_Value(COLUMNNAME_AccountValue);
	}

	/** Set AssetType.
		@param AssetType AssetType
	*/
	public void setAssetType (String AssetType)
	{
		set_Value (COLUMNNAME_AssetType, AssetType);
	}

	/** Get AssetType.
		@return AssetType	  */
	public String getAssetType()
	{
		return (String)get_Value(COLUMNNAME_AssetType);
	}

	/** Set Bank Name.
		@param BankName Bank Name
	*/
	public void setBankName (String BankName)
	{
		set_Value (COLUMNNAME_BankName, BankName);
	}

	/** Get Bank Name.
		@return Bank Name	  */
	public String getBankName()
	{
		return (String)get_Value(COLUMNNAME_BankName);
	}

	/** Set BeginBalance.
		@param BeginBalance BeginBalance
	*/
	public void setBeginBalance (BigDecimal BeginBalance)
	{
		set_Value (COLUMNNAME_BeginBalance, BeginBalance);
	}

	/** Get BeginBalance.
		@return BeginBalance	  */
	public BigDecimal getBeginBalance()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BeginBalance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	public org.compiere.model.I_C_Bank getC_Bank() throws RuntimeException
	{
		return (org.compiere.model.I_C_Bank)MTable.get(getCtx(), org.compiere.model.I_C_Bank.Table_ID)
			.getPO(getC_Bank_ID(), get_TrxName());
	}

	/** Set Bank.
		@param C_Bank_ID Bank
	*/
	public void setC_Bank_ID (int C_Bank_ID)
	{
		if (C_Bank_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Bank_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Bank_ID, Integer.valueOf(C_Bank_ID));
	}

	/** Get Bank.
		@return Bank
	  */
	public int getC_Bank_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Bank_ID);
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

	/** Set PeriodBalance.
		@param PeriodBalance PeriodBalance
	*/
	public void setPeriodBalance (BigDecimal PeriodBalance)
	{
		set_Value (COLUMNNAME_PeriodBalance, PeriodBalance);
	}

	/** Get PeriodBalance.
		@return PeriodBalance	  */
	public BigDecimal getPeriodBalance()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PeriodBalance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set T_BankAccountAsset_UU.
		@param T_BankAccountAsset_UU T_BankAccountAsset_UU
	*/
	public void setT_BankAccountAsset_UU (String T_BankAccountAsset_UU)
	{
		set_ValueNoCheck (COLUMNNAME_T_BankAccountAsset_UU, T_BankAccountAsset_UU);
	}

	/** Get T_BankAccountAsset_UU.
		@return T_BankAccountAsset_UU	  */
	public String getT_BankAccountAsset_UU()
	{
		return (String)get_Value(COLUMNNAME_T_BankAccountAsset_UU);
	}
}