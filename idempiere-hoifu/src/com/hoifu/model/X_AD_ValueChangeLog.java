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
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for AD_ValueChangeLog
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="AD_ValueChangeLog")
public class X_AD_ValueChangeLog extends PO implements I_AD_ValueChangeLog, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260609L;

    /** Standard Constructor */
    public X_AD_ValueChangeLog (Properties ctx, int AD_ValueChangeLog_ID, String trxName)
    {
      super (ctx, AD_ValueChangeLog_ID, trxName);
      /** if (AD_ValueChangeLog_ID == 0)
        {
			setAD_ValueChangeLog_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_AD_ValueChangeLog (Properties ctx, int AD_ValueChangeLog_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_ValueChangeLog_ID, trxName, virtualColumns);
      /** if (AD_ValueChangeLog_ID == 0)
        {
			setAD_ValueChangeLog_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_AD_ValueChangeLog (Properties ctx, String AD_ValueChangeLog_UU, String trxName)
    {
      super (ctx, AD_ValueChangeLog_UU, trxName);
      /** if (AD_ValueChangeLog_UU == null)
        {
			setAD_ValueChangeLog_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_AD_ValueChangeLog (Properties ctx, String AD_ValueChangeLog_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_ValueChangeLog_UU, trxName, virtualColumns);
      /** if (AD_ValueChangeLog_UU == null)
        {
			setAD_ValueChangeLog_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_ValueChangeLog (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
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
      StringBuilder sb = new StringBuilder ("X_AD_ValueChangeLog[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set AD_ValueChangeLog.
		@param AD_ValueChangeLog_ID AD_ValueChangeLog
	*/
	public void setAD_ValueChangeLog_ID (int AD_ValueChangeLog_ID)
	{
		if (AD_ValueChangeLog_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_ValueChangeLog_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_ValueChangeLog_ID, Integer.valueOf(AD_ValueChangeLog_ID));
	}

	/** Get AD_ValueChangeLog.
		@return AD_ValueChangeLog	  */
	public int getAD_ValueChangeLog_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_ValueChangeLog_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AD_ValueChangeLog_UU.
		@param AD_ValueChangeLog_UU AD_ValueChangeLog_UU
	*/
	public void setAD_ValueChangeLog_UU (String AD_ValueChangeLog_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AD_ValueChangeLog_UU, AD_ValueChangeLog_UU);
	}

	/** Get AD_ValueChangeLog_UU.
		@return AD_ValueChangeLog_UU	  */
	public String getAD_ValueChangeLog_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_ValueChangeLog_UU);
	}

	/** Set DB Column Name.
		@param ColumnName Name of the column in the database
	*/
	public void setColumnName (String ColumnName)
	{
		set_Value (COLUMNNAME_ColumnName, ColumnName);
	}

	/** Get DB Column Name.
		@return Name of the column in the database
	  */
	public String getColumnName()
	{
		return (String)get_Value(COLUMNNAME_ColumnName);
	}

	/** Set New Value.
		@param NewValue New field value
	*/
	public void setNewValue (String NewValue)
	{
		set_Value (COLUMNNAME_NewValue, NewValue);
	}

	/** Get New Value.
		@return New field value
	  */
	public String getNewValue()
	{
		return (String)get_Value(COLUMNNAME_NewValue);
	}

	/** Set Old Value.
		@param OldValue The old file data
	*/
	public void setOldValue (String OldValue)
	{
		set_Value (COLUMNNAME_OldValue, OldValue);
	}

	/** Get Old Value.
		@return The old file data
	  */
	public String getOldValue()
	{
		return (String)get_Value(COLUMNNAME_OldValue);
	}

	/** Set Record ID.
		@param Record_ID Direct internal record ID
	*/
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0)
			set_ValueNoCheck (COLUMNNAME_Record_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Record ID.
		@return Direct internal record ID
	  */
	public int getRecord_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DB Table Name.
		@param TableName Name of the table in the database
	*/
	public void setTableName (String TableName)
	{
		set_Value (COLUMNNAME_TableName, TableName);
	}

	/** Get DB Table Name.
		@return Name of the table in the database
	  */
	public String getTableName()
	{
		return (String)get_Value(COLUMNNAME_TableName);
	}
}