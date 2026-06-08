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

/** Generated Model for WM_WarehousePick
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_WarehousePick extends PO implements I_WM_WarehousePick, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170525L;

    /** Standard Constructor */
    public X_WM_WarehousePick (Properties ctx, int WM_WarehousePick_ID, String trxName)
    {
      super (ctx, WM_WarehousePick_ID, trxName);
      /** if (WM_WarehousePick_ID == 0)
        {
			setWM_WarehousePick_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_WarehousePick (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_WarehousePick[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException
    {
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_Name)
			.getPO(getC_OrderLine_ID(), get_TrxName());	}

	/** Set Sales Order Line.
		@param C_OrderLine_ID 
		Sales Order Line
	  */
	public void setC_OrderLine_ID (int C_OrderLine_ID)
	{
		if (C_OrderLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_OrderLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_OrderLine_ID, Integer.valueOf(C_OrderLine_ID));
	}

	/** Get Sales Order Line.
		@return Sales Order Line
	  */
	public int getC_OrderLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
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

	public org.compiere.model.I_M_Shipper getM_Shipper() throws RuntimeException
    {
		return (org.compiere.model.I_M_Shipper)MTable.get(getCtx(), org.compiere.model.I_M_Shipper.Table_Name)
			.getPO(getM_Shipper_ID(), get_TrxName());	}

	/** Set Shipper.
		@param M_Shipper_ID 
		Method or manner of product delivery
	  */
	public void setM_Shipper_ID (int M_Shipper_ID)
	{
		if (M_Shipper_ID < 1) 
			set_Value (COLUMNNAME_M_Shipper_ID, null);
		else 
			set_Value (COLUMNNAME_M_Shipper_ID, Integer.valueOf(M_Shipper_ID));
	}

	/** Get Shipper.
		@return Method or manner of product delivery
	  */
	public int getM_Shipper_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Shipper_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set List Price.
		@param PriceList 
		List Price
	  */
	public void setPriceList (String PriceList)
	{
		set_Value (COLUMNNAME_PriceList, PriceList);
	}

	/** Get List Price.
		@return List Price
	  */
	public String getPriceList () 
	{
		return (String)get_Value(COLUMNNAME_PriceList);
	}

	/** Set Ordered Quantity.
		@param QtyOrdered 
		Ordered Quantity
	  */
	public void setQtyOrdered (BigDecimal QtyOrdered)
	{
		set_Value (COLUMNNAME_QtyOrdered, QtyOrdered);
	}

	/** Get Ordered Quantity.
		@return Ordered Quantity
	  */
	public BigDecimal getQtyOrdered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyOrdered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_WM_EmptyStorageLine getWM_EmptyStorageLine() throws RuntimeException
    {
		return (I_WM_EmptyStorageLine)MTable.get(getCtx(), I_WM_EmptyStorageLine.Table_Name)
			.getPO(getWM_EmptyStorageLine_ID(), get_TrxName());	}

	/** Set EmptyStorageLine.
		@param WM_EmptyStorageLine_ID EmptyStorageLine	  */
	public void setWM_EmptyStorageLine_ID (int WM_EmptyStorageLine_ID)
	{
		if (WM_EmptyStorageLine_ID < 1) 
			set_Value (COLUMNNAME_WM_EmptyStorageLine_ID, null);
		else 
			set_Value (COLUMNNAME_WM_EmptyStorageLine_ID, Integer.valueOf(WM_EmptyStorageLine_ID));
	}

	/** Get EmptyStorageLine.
		@return EmptyStorageLine	  */
	public int getWM_EmptyStorageLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_EmptyStorageLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_WM_HandlingUnit getWM_HandlingUnit() throws RuntimeException
    {
		return (I_WM_HandlingUnit)MTable.get(getCtx(), I_WM_HandlingUnit.Table_Name)
			.getPO(getWM_HandlingUnit_ID(), get_TrxName());	}

	/** Set WM_HandlingUnit_ID.
		@param WM_HandlingUnit_ID WM_HandlingUnit_ID	  */
	public void setWM_HandlingUnit_ID (int WM_HandlingUnit_ID)
	{
		if (WM_HandlingUnit_ID < 1) 
			set_Value (COLUMNNAME_WM_HandlingUnit_ID, null);
		else 
			set_Value (COLUMNNAME_WM_HandlingUnit_ID, Integer.valueOf(WM_HandlingUnit_ID));
	}

	/** Get WM_HandlingUnit_ID.
		@return WM_HandlingUnit_ID	  */
	public int getWM_HandlingUnit_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_HandlingUnit_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WarehousePick.
		@param WM_WarehousePick_ID WarehousePick	  */
	public void setWM_WarehousePick_ID (int WM_WarehousePick_ID)
	{
		if (WM_WarehousePick_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_WarehousePick_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_WarehousePick_ID, Integer.valueOf(WM_WarehousePick_ID));
	}

	/** Get WarehousePick.
		@return WarehousePick	  */
	public int getWM_WarehousePick_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_WarehousePick_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}