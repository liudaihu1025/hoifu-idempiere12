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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for HF_Rest_Api_Call_Log
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="HF_Rest_Api_Call_Log")
public class X_HF_Rest_Api_Call_Log extends PO implements I_HF_Rest_Api_Call_Log, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260918L;

    /** Standard Constructor */
    public X_HF_Rest_Api_Call_Log (Properties ctx, int HF_Rest_Api_Call_Log_ID, String trxName)
    {
      super (ctx, HF_Rest_Api_Call_Log_ID, trxName);
      /** if (HF_Rest_Api_Call_Log_ID == 0)
        {
			setHF_Rest_Api_Call_Log_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_Rest_Api_Call_Log (Properties ctx, int HF_Rest_Api_Call_Log_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, HF_Rest_Api_Call_Log_ID, trxName, virtualColumns);
      /** if (HF_Rest_Api_Call_Log_ID == 0)
        {
			setHF_Rest_Api_Call_Log_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_Rest_Api_Call_Log (Properties ctx, String HF_Rest_Api_Call_Log_UU, String trxName)
    {
      super (ctx, HF_Rest_Api_Call_Log_UU, trxName);
      /** if (HF_Rest_Api_Call_Log_UU == null)
        {
			setHF_Rest_Api_Call_Log_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_Rest_Api_Call_Log (Properties ctx, String HF_Rest_Api_Call_Log_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, HF_Rest_Api_Call_Log_UU, trxName, virtualColumns);
      /** if (HF_Rest_Api_Call_Log_UU == null)
        {
			setHF_Rest_Api_Call_Log_ID (0);
        } */
    }

    /** Load Constructor */
    public X_HF_Rest_Api_Call_Log (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, MTable.getTable_ID(Table_Name), get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_HF_Rest_Api_Call_Log[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Role)MTable.get(getCtx(), org.compiere.model.I_AD_Role.Table_ID)
			.getPO(getAD_Role_ID(), get_TrxName());
	}

	/** Set Role.
		@param AD_Role_ID Responsibility Role
	*/
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0)
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Role.
		@return Responsibility Role
	  */
	public int getAD_Role_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set ClientIP.
		@param ClientIP ClientIP
	*/
	public void setClientIP (String ClientIP)
	{
		set_Value (COLUMNNAME_ClientIP, ClientIP);
	}

	/** Get ClientIP.
		@return ClientIP	  */
	public String getClientIP()
	{
		return (String)get_Value(COLUMNNAME_ClientIP);
	}

	/** Set ElapsedMs.
		@param ElapsedMs ElapsedMs
	*/
	public void setElapsedMs (int ElapsedMs)
	{
		set_Value (COLUMNNAME_ElapsedMs, Integer.valueOf(ElapsedMs));
	}

	/** Get ElapsedMs.
		@return ElapsedMs	  */
	public int getElapsedMs()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ElapsedMs);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set HF_Rest_Api_Call_Log.
		@param HF_Rest_Api_Call_Log_ID HF_Rest_Api_Call_Log
	*/
	public void setHF_Rest_Api_Call_Log_ID (int HF_Rest_Api_Call_Log_ID)
	{
		if (HF_Rest_Api_Call_Log_ID < 1)
			set_ValueNoCheck (COLUMNNAME_HF_Rest_Api_Call_Log_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_HF_Rest_Api_Call_Log_ID, Integer.valueOf(HF_Rest_Api_Call_Log_ID));
	}

	/** Get HF_Rest_Api_Call_Log.
		@return HF_Rest_Api_Call_Log	  */
	public int getHF_Rest_Api_Call_Log_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HF_Rest_Api_Call_Log_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set HF_Rest_Api_Call_Log_UU.
		@param HF_Rest_Api_Call_Log_UU HF_Rest_Api_Call_Log_UU
	*/
	public void setHF_Rest_Api_Call_Log_UU (String HF_Rest_Api_Call_Log_UU)
	{
		set_ValueNoCheck (COLUMNNAME_HF_Rest_Api_Call_Log_UU, HF_Rest_Api_Call_Log_UU);
	}

	/** Get HF_Rest_Api_Call_Log_UU.
		@return HF_Rest_Api_Call_Log_UU	  */
	public String getHF_Rest_Api_Call_Log_UU()
	{
		return (String)get_Value(COLUMNNAME_HF_Rest_Api_Call_Log_UU);
	}

	/** Set HttpMethod.
		@param HttpMethod HttpMethod
	*/
	public void setHttpMethod (String HttpMethod)
	{
		set_Value (COLUMNNAME_HttpMethod, HttpMethod);
	}

	/** Get HttpMethod.
		@return HttpMethod	  */
	public String getHttpMethod()
	{
		return (String)get_Value(COLUMNNAME_HttpMethod);
	}

	/** Set QueryString.
		@param QueryString QueryString
	*/
	public void setQueryString (String QueryString)
	{
		set_Value (COLUMNNAME_QueryString, QueryString);
	}

	/** Get QueryString.
		@return QueryString	  */
	public String getQueryString()
	{
		return (String)get_Value(COLUMNNAME_QueryString);
	}

	/** Set RequestBody.
		@param RequestBody RequestBody
	*/
	public void setRequestBody (String RequestBody)
	{
		set_Value (COLUMNNAME_RequestBody, RequestBody);
	}

	/** Get RequestBody.
		@return RequestBody	  */
	public String getRequestBody()
	{
		return (String)get_Value(COLUMNNAME_RequestBody);
	}

	/** Set &#35831;&#27714;&#32534;&#30721;.
		@param RequestCode &#35831;&#27714;&#32534;&#30721;
	*/
	public void setRequestCode (String RequestCode)
	{
		set_Value (COLUMNNAME_RequestCode, RequestCode);
	}

	/** Get &#35831;&#27714;&#32534;&#30721;.
		@return &#35831;&#27714;&#32534;&#30721;	  */
	public String getRequestCode()
	{
		return (String)get_Value(COLUMNNAME_RequestCode);
	}

	/** Set RequestPath.
		@param RequestPath RequestPath
	*/
	public void setRequestPath (String RequestPath)
	{
		set_Value (COLUMNNAME_RequestPath, RequestPath);
	}

	/** Get RequestPath.
		@return RequestPath	  */
	public String getRequestPath()
	{
		return (String)get_Value(COLUMNNAME_RequestPath);
	}

	/** Set ResponseBody.
		@param ResponseBody ResponseBody
	*/
	public void setResponseBody (String ResponseBody)
	{
		set_Value (COLUMNNAME_ResponseBody, ResponseBody);
	}

	/** Get ResponseBody.
		@return ResponseBody	  */
	public String getResponseBody()
	{
		return (String)get_Value(COLUMNNAME_ResponseBody);
	}

	/** Set ResponseStatus.
		@param ResponseStatus ResponseStatus
	*/
	public void setResponseStatus (int ResponseStatus)
	{
		set_Value (COLUMNNAME_ResponseStatus, Integer.valueOf(ResponseStatus));
	}

	/** Get ResponseStatus.
		@return ResponseStatus	  */
	public int getResponseStatus()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ResponseStatus);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}