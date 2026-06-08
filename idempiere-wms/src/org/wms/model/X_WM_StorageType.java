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
package org.wms.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for WM_StorageType
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_StorageType extends PO implements I_WM_StorageType, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170510L;

    /** Standard Constructor */
    public X_WM_StorageType (Properties ctx, int WM_StorageType_ID, String trxName)
    {
      super (ctx, WM_StorageType_ID, trxName);
      /** if (WM_StorageType_ID == 0)
        {
			setWM_StorageType_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_StorageType (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_StorageType[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_M_Locator getM_Locator() throws RuntimeException
    {
		return (I_M_Locator)MTable.get(getCtx(), I_M_Locator.Table_Name)
			.getPO(getM_Locator_ID(), get_TrxName());	}

	/** Set Locator.
		@param M_Locator_ID 
		Warehouse Locator
	  */
	public void setM_Locator_ID (int M_Locator_ID)
	{
		if (M_Locator_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Locator_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Locator_ID, Integer.valueOf(M_Locator_ID));
	}

	/** Get Locator.
		@return Warehouse Locator
	  */
	public int getM_Locator_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Locator_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
    {
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_Name)
			.getPO(getM_Warehouse_ID(), get_TrxName());	}

	/** Set Warehouse.
		@param M_Warehouse_ID 
		Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1) 
			set_Value (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Priority.
		@param Priority 
		Indicates if this request is of a high, medium or low priority.
	  */
	public void setPriority (int Priority)
	{
		set_Value (COLUMNNAME_Priority, Integer.valueOf(Priority));
	}

	/** Get Priority.
		@return Indicates if this request is of a high, medium or low priority.
	  */
	public int getPriority () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Priority);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set TypeString.
		@param TypeString TypeString	  */
	public void setTypeString (String TypeString)
	{
		set_Value (COLUMNNAME_TypeString, TypeString);
	}

	/** Get TypeString.
		@return TypeString	  */
	public String getTypeString () 
	{
		return (String)get_Value(COLUMNNAME_TypeString);
	}

	/** Set StorageType.
		@param WM_StorageType_ID StorageType	  */
	public void setWM_StorageType_ID (int WM_StorageType_ID)
	{
		if (WM_StorageType_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_StorageType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_StorageType_ID, Integer.valueOf(WM_StorageType_ID));
	}

	/** Get StorageType.
		@return StorageType	  */
	public int getWM_StorageType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_StorageType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WM_StorageType_UU.
		@param WM_StorageType_UU WM_StorageType_UU	  */
	public void setWM_StorageType_UU (String WM_StorageType_UU)
	{
		set_Value (COLUMNNAME_WM_StorageType_UU, WM_StorageType_UU);
	}

	/** Get WM_StorageType_UU.
		@return WM_StorageType_UU	  */
	public String getWM_StorageType_UU () 
	{
		return (String)get_Value(COLUMNNAME_WM_StorageType_UU);
	}

	public I_WM_Type getWM_Type() throws RuntimeException
    {
		return (I_WM_Type)MTable.get(getCtx(), I_WM_Type.Table_Name)
			.getPO(getWM_Type_ID(), get_TrxName());	}

	/** Set Type.
		@param WM_Type_ID Type	  */
	public void setWM_Type_ID (int WM_Type_ID)
	{
		if (WM_Type_ID < 1) 
			set_Value (COLUMNNAME_WM_Type_ID, null);
		else 
			set_Value (COLUMNNAME_WM_Type_ID, Integer.valueOf(WM_Type_ID));
	}

	/** Get Type.
		@return Type	  */
	public int getWM_Type_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}