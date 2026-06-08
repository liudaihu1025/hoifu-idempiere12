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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for WM_EmptyStorage
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_EmptyStorage extends PO implements I_WM_EmptyStorage, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170527L;

    /** Standard Constructor */
    public X_WM_EmptyStorage (Properties ctx, int WM_EmptyStorage_ID, String trxName)
    {
      super (ctx, WM_EmptyStorage_ID, trxName);
      /** if (WM_EmptyStorage_ID == 0)
        {
			setWM_EmptyStorage_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_EmptyStorage (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_EmptyStorage[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set AvailableCapacity.
		@param AvailableCapacity AvailableCapacity	  */
	public void setAvailableCapacity (BigDecimal AvailableCapacity)
	{
		set_Value (COLUMNNAME_AvailableCapacity, AvailableCapacity);
	}

	/** Get AvailableCapacity.
		@return AvailableCapacity	  */
	public BigDecimal getAvailableCapacity () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_AvailableCapacity);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set IsBlocked.
		@param IsBlocked IsBlocked	  */
	public void setIsBlocked (boolean IsBlocked)
	{
		set_Value (COLUMNNAME_IsBlocked, Boolean.valueOf(IsBlocked));
	}

	/** Get IsBlocked.
		@return IsBlocked	  */
	public boolean isBlocked () 
	{
		Object oo = get_Value(COLUMNNAME_IsBlocked);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsFull.
		@param IsFull IsFull	  */
	public void setIsFull (boolean IsFull)
	{
		set_Value (COLUMNNAME_IsFull, Boolean.valueOf(IsFull));
	}

	/** Get IsFull.
		@return IsFull	  */
	public boolean isFull () 
	{
		Object oo = get_Value(COLUMNNAME_IsFull);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Percentage.
		@param Percentage 
		Percent of the entire amount
	  */
	public void setPercentage (BigDecimal Percentage)
	{
		set_Value (COLUMNNAME_Percentage, Percentage);
	}

	/** Get Percentage.
		@return Percent of the entire amount
	  */
	public BigDecimal getPercentage () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Percentage);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set VacantCapacity.
		@param VacantCapacity VacantCapacity	  */
	public void setVacantCapacity (BigDecimal VacantCapacity)
	{
		set_Value (COLUMNNAME_VacantCapacity, VacantCapacity);
	}

	/** Get VacantCapacity.
		@return VacantCapacity	  */
	public BigDecimal getVacantCapacity () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_VacantCapacity);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set EmptyStorage.
		@param WM_EmptyStorage_ID EmptyStorage	  */
	public void setWM_EmptyStorage_ID (int WM_EmptyStorage_ID)
	{
		if (WM_EmptyStorage_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_EmptyStorage_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_EmptyStorage_ID, Integer.valueOf(WM_EmptyStorage_ID));
	}

	/** Get EmptyStorage.
		@return EmptyStorage	  */
	public int getWM_EmptyStorage_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_EmptyStorage_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WM_EmptyStorage_UU.
		@param WM_EmptyStorage_UU WM_EmptyStorage_UU	  */
	public void setWM_EmptyStorage_UU (String WM_EmptyStorage_UU)
	{
		set_Value (COLUMNNAME_WM_EmptyStorage_UU, WM_EmptyStorage_UU);
	}

	/** Get WM_EmptyStorage_UU.
		@return WM_EmptyStorage_UU	  */
	public String getWM_EmptyStorage_UU () 
	{
		return (String)get_Value(COLUMNNAME_WM_EmptyStorage_UU);
	}
}