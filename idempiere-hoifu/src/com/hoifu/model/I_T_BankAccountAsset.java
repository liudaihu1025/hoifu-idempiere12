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
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for T_BankAccountAsset
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_T_BankAccountAsset 
{

    /** TableName=T_BankAccountAsset */
    public static final String Table_Name = "T_BankAccountAsset";

    /** AD_Table_ID=1000098 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 4 - System 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(4);

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

    /** Column name AD_PInstance_ID */
    public static final String COLUMNNAME_AD_PInstance_ID = "AD_PInstance_ID";

	/** Set Process Instance.
	  * Instance of the process
	  */
	public void setAD_PInstance_ID (int AD_PInstance_ID);

	/** Get Process Instance.
	  * Instance of the process
	  */
	public int getAD_PInstance_ID();

	public org.compiere.model.I_AD_PInstance getAD_PInstance() throws RuntimeException;

    /** Column name AccountName */
    public static final String COLUMNNAME_AccountName = "AccountName";

	/** Set AccountName	  */
	public void setAccountName (String AccountName);

	/** Get AccountName	  */
	public String getAccountName();

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

    /** Column name AccountValue */
    public static final String COLUMNNAME_AccountValue = "AccountValue";

	/** Set Account Key.
	  * Key of Account Element
	  */
	public void setAccountValue (String AccountValue);

	/** Get Account Key.
	  * Key of Account Element
	  */
	public String getAccountValue();

    /** Column name AssetType */
    public static final String COLUMNNAME_AssetType = "AssetType";

	/** Set AssetType	  */
	public void setAssetType (String AssetType);

	/** Get AssetType	  */
	public String getAssetType();

    /** Column name BankName */
    public static final String COLUMNNAME_BankName = "BankName";

	/** Set Bank Name	  */
	public void setBankName (String BankName);

	/** Get Bank Name	  */
	public String getBankName();

    /** Column name BeginBalance */
    public static final String COLUMNNAME_BeginBalance = "BeginBalance";

	/** Set BeginBalance	  */
	public void setBeginBalance (BigDecimal BeginBalance);

	/** Get BeginBalance	  */
	public BigDecimal getBeginBalance();

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

    /** Column name C_Bank_ID */
    public static final String COLUMNNAME_C_Bank_ID = "C_Bank_ID";

	/** Set Bank.
	  * Bank
	  */
	public void setC_Bank_ID (int C_Bank_ID);

	/** Get Bank.
	  * Bank
	  */
	public int getC_Bank_ID();

	public org.compiere.model.I_C_Bank getC_Bank() throws RuntimeException;

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

    /** Column name PeriodBalance */
    public static final String COLUMNNAME_PeriodBalance = "PeriodBalance";

	/** Set PeriodBalance	  */
	public void setPeriodBalance (BigDecimal PeriodBalance);

	/** Get PeriodBalance	  */
	public BigDecimal getPeriodBalance();

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

    /** Column name T_BankAccountAsset_UU */
    public static final String COLUMNNAME_T_BankAccountAsset_UU = "T_BankAccountAsset_UU";

	/** Set T_BankAccountAsset_UU	  */
	public void setT_BankAccountAsset_UU (String T_BankAccountAsset_UU);

	/** Get T_BankAccountAsset_UU	  */
	public String getT_BankAccountAsset_UU();
}
