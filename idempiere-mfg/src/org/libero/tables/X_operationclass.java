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
package org.libero.tables;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/** Generated Model for operationclass
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="operationclass")
public class X_operationclass extends PO implements I_operationclass, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260724L;

    /** Standard Constructor */
    public X_operationclass (Properties ctx, int operationclass_ID, String trxName)
    {
      super (ctx, operationclass_ID, trxName);
      /** if (operationclass_ID == 0)
        {
			setName (null);
			setValue (null);
			setoperationclass_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_operationclass (Properties ctx, int operationclass_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, operationclass_ID, trxName, virtualColumns);
      /** if (operationclass_ID == 0)
        {
			setName (null);
			setValue (null);
			setoperationclass_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_operationclass (Properties ctx, String operationclass_UU, String trxName)
    {
      super (ctx, operationclass_UU, trxName);
      /** if (operationclass_UU == null)
        {
			setName (null);
			setValue (null);
			setoperationclass_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_operationclass (Properties ctx, String operationclass_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, operationclass_UU, trxName, virtualColumns);
      /** if (operationclass_UU == null)
        {
			setName (null);
			setValue (null);
			setoperationclass_ID (0);
        } */
    }

    /** Load Constructor */
    public X_operationclass (Properties ctx, ResultSet rs, String trxName)
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
      POInfo poi = POInfo.getPOInfo (ctx, MTable.getTable_ID(Table_Name), get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_operationclass[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
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

	/** Set &#24037;&#24207;&#32452;.
		@param operationclass_ID &#24037;&#24207;&#32452;
	*/
	public void setoperationclass_ID (int operationclass_ID)
	{
		if (operationclass_ID < 1)
			set_ValueNoCheck (COLUMNNAME_operationclass_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_operationclass_ID, Integer.valueOf(operationclass_ID));
	}

	/** Get &#24037;&#24207;&#32452;.
		@return &#24037;&#24207;&#32452;	  */
	public int getoperationclass_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_operationclass_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set operationclass_UU.
		@param operationclass_UU operationclass_UU
	*/
	public void setoperationclass_UU (String operationclass_UU)
	{
		set_ValueNoCheck (COLUMNNAME_operationclass_UU, operationclass_UU);
	}

	/** Get operationclass_UU.
		@return operationclass_UU	  */
	public String getoperationclass_UU()
	{
		return (String)get_Value(COLUMNNAME_operationclass_UU);
	}

	/** Set statisticstype.
		@param statisticstype statisticstype
	*/
	public void setstatisticstype (String statisticstype)
	{
		set_Value (COLUMNNAME_statisticstype, statisticstype);
	}

	/** Get statisticstype.
		@return statisticstype	  */
	public String getstatisticstype()
	{
		return (String)get_Value(COLUMNNAME_statisticstype);
	}
}