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

/** Generated Model for WM_ProductType
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_ProductType extends PO implements I_WM_ProductType, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170510L;

    /** Standard Constructor */
    public X_WM_ProductType (Properties ctx, int WM_ProductType_ID, String trxName)
    {
      super (ctx, WM_ProductType_ID, trxName);
      /** if (WM_ProductType_ID == 0)
        {
			setWM_ProductType_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_ProductType (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_ProductType[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
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

	/** Set ProductType.
		@param WM_ProductType_ID ProductType	  */
	public void setWM_ProductType_ID (int WM_ProductType_ID)
	{
		if (WM_ProductType_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_ProductType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_ProductType_ID, Integer.valueOf(WM_ProductType_ID));
	}

	/** Get ProductType.
		@return ProductType	  */
	public int getWM_ProductType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_ProductType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WM_ProductType_UU.
		@param WM_ProductType_UU WM_ProductType_UU	  */
	public void setWM_ProductType_UU (String WM_ProductType_UU)
	{
		set_Value (COLUMNNAME_WM_ProductType_UU, WM_ProductType_UU);
	}

	/** Get WM_ProductType_UU.
		@return WM_ProductType_UU	  */
	public String getWM_ProductType_UU () 
	{
		return (String)get_Value(COLUMNNAME_WM_ProductType_UU);
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