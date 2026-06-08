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
package org.libero.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Model for C_WorkTeam
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_WorkTeam")
public class X_C_WorkTeam extends PO implements I_C_WorkTeam, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260518L;

    /** Standard Constructor */
    public X_C_WorkTeam (Properties ctx, int C_WorkTeam_ID, String trxName)
    {
      super (ctx, C_WorkTeam_ID, trxName);
      /** if (C_WorkTeam_ID == 0)
        {
			setC_WorkTeam_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_C_WorkTeam (Properties ctx, int C_WorkTeam_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_WorkTeam_ID, trxName, virtualColumns);
      /** if (C_WorkTeam_ID == 0)
        {
			setC_WorkTeam_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_C_WorkTeam (Properties ctx, String C_WorkTeam_UU, String trxName)
    {
      super (ctx, C_WorkTeam_UU, trxName);
      /** if (C_WorkTeam_UU == null)
        {
			setC_WorkTeam_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_C_WorkTeam (Properties ctx, String C_WorkTeam_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_WorkTeam_UU, trxName, virtualColumns);
      /** if (C_WorkTeam_UU == null)
        {
			setC_WorkTeam_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_C_WorkTeam (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_C_WorkTeam[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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

	/** Set C_WorkTeam.
		@param C_WorkTeam_ID C_WorkTeam
	*/
	public void setC_WorkTeam_ID (int C_WorkTeam_ID)
	{
		if (C_WorkTeam_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_WorkTeam_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_WorkTeam_ID, Integer.valueOf(C_WorkTeam_ID));
	}

	/** Get C_WorkTeam.
		@return C_WorkTeam	  */
	public int getC_WorkTeam_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_WorkTeam_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_WorkTeam_UU.
		@param C_WorkTeam_UU C_WorkTeam_UU
	*/
	public void setC_WorkTeam_UU (String C_WorkTeam_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_WorkTeam_UU, C_WorkTeam_UU);
	}

	/** Get C_WorkTeam_UU.
		@return C_WorkTeam_UU	  */
	public String getC_WorkTeam_UU()
	{
		return (String)get_Value(COLUMNNAME_C_WorkTeam_UU);
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

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair()
    {
        return new KeyNamePair(get_ID(), getValue());
    }
}