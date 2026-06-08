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

/** Generated Model for WM_Type
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_Type extends PO implements I_WM_Type, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170422L;

    /** Standard Constructor */
    public X_WM_Type (Properties ctx, int WM_Type_ID, String trxName)
    {
      super (ctx, WM_Type_ID, trxName);
      /** if (WM_Type_ID == 0)
        {
			setWM_Type_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_Type (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_Type[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Delete old/existing records.
		@param DeleteOld 
		Otherwise records will be added
	  */
	public void setDeleteOld (String DeleteOld)
	{
		set_Value (COLUMNNAME_DeleteOld, DeleteOld);
	}

	/** Get Delete old/existing records.
		@return Otherwise records will be added
	  */
	public String getDeleteOld () 
	{
		return (String)get_Value(COLUMNNAME_DeleteOld);
	}

	/** Set ExactMatch.
		@param ExactMatch ExactMatch	  */
	public void setExactMatch (boolean ExactMatch)
	{
		set_Value (COLUMNNAME_ExactMatch, Boolean.valueOf(ExactMatch));
	}

	/** Get ExactMatch.
		@return ExactMatch	  */
	public boolean isExactMatch () 
	{
		Object oo = get_Value(COLUMNNAME_ExactMatch);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Type.
		@param WM_Type_ID Type	  */
	public void setWM_Type_ID (int WM_Type_ID)
	{
		if (WM_Type_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_Type_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_Type_ID, Integer.valueOf(WM_Type_ID));
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