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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for WM_Migration
 *  @author iDempiere (generated) 
 *  @version Release 6.2 - $Id$ */
public class X_WM_Migration extends PO implements I_WM_Migration, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190829L;

    /** Standard Constructor */
    public X_WM_Migration (Properties ctx, int WM_Migration_ID, String trxName)
    {
      super (ctx, WM_Migration_ID, trxName);
      /** if (WM_Migration_ID == 0)
        {
			setWM_Migration_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_Migration (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_Migration[")
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
			set_Value (COLUMNNAME_M_Locator_ID, null);
		else 
			set_Value (COLUMNNAME_M_Locator_ID, Integer.valueOf(M_Locator_ID));
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

	/** Set Movement Date.
		@param MovementDate 
		Date a product was moved in or out of inventory
	  */
	public void setMovementDate (Timestamp MovementDate)
	{
		set_Value (COLUMNNAME_MovementDate, MovementDate);
	}

	/** Get Movement Date.
		@return Date a product was moved in or out of inventory
	  */
	public Timestamp getMovementDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_MovementDate);
	}

	/** Set Movement Quantity.
		@param MovementQty 
		Quantity of a product moved.
	  */
	public void setMovementQty (BigDecimal MovementQty)
	{
		set_Value (COLUMNNAME_MovementQty, MovementQty);
	}

	/** Get Movement Quantity.
		@return Quantity of a product moved.
	  */
	public BigDecimal getMovementQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MovementQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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
			set_Value (COLUMNNAME_M_Product_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
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

	/** Set Price.
		@param Price 
		Price
	  */
	public void setPrice (BigDecimal Price)
	{
		set_Value (COLUMNNAME_Price, Price);
	}

	/** Get Price.
		@return Price
	  */
	public BigDecimal getPrice () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Price);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_WM_HandlingUnit getWM_HandlingUnit() throws RuntimeException
    {
		return (I_WM_HandlingUnit)MTable.get(getCtx(), I_WM_HandlingUnit.Table_Name)
			.getPO(getWM_HandlingUnit_ID(), get_TrxName());	}

	/** Set Handling Unit.
		@param WM_HandlingUnit_ID Handling Unit	  */
	public void setWM_HandlingUnit_ID (int WM_HandlingUnit_ID)
	{
		if (WM_HandlingUnit_ID < 1) 
			set_Value (COLUMNNAME_WM_HandlingUnit_ID, null);
		else 
			set_Value (COLUMNNAME_WM_HandlingUnit_ID, Integer.valueOf(WM_HandlingUnit_ID));
	}

	/** Get Handling Unit.
		@return Handling Unit	  */
	public int getWM_HandlingUnit_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_HandlingUnit_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_WM_HandlingUnit getWM_HandlingUnitOld() throws RuntimeException
    {
		return (I_WM_HandlingUnit)MTable.get(getCtx(), I_WM_HandlingUnit.Table_Name)
			.getPO(getWM_HandlingUnitOld_ID(), get_TrxName());	}

	/** Set HandlingUnit Old.
		@param WM_HandlingUnitOld_ID HandlingUnit Old	  */
	public void setWM_HandlingUnitOld_ID (int WM_HandlingUnitOld_ID)
	{
		if (WM_HandlingUnitOld_ID < 1) 
			set_Value (COLUMNNAME_WM_HandlingUnitOld_ID, null);
		else 
			set_Value (COLUMNNAME_WM_HandlingUnitOld_ID, Integer.valueOf(WM_HandlingUnitOld_ID));
	}

	/** Get HandlingUnit Old.
		@return HandlingUnit Old	  */
	public int getWM_HandlingUnitOld_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_HandlingUnitOld_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Migration.
		@param WM_Migration_ID Migration	  */
	public void setWM_Migration_ID (int WM_Migration_ID)
	{
		if (WM_Migration_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_Migration_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_Migration_ID, Integer.valueOf(WM_Migration_ID));
	}

	/** Get Migration.
		@return Migration	  */
	public int getWM_Migration_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_Migration_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}