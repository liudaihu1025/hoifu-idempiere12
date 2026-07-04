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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for M_InOutNoticeLine
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="M_InOutNoticeLine")
public class X_M_InOutNoticeLine extends PO implements I_M_InOutNoticeLine, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260611L;

    /** Standard Constructor */
    public X_M_InOutNoticeLine (Properties ctx, int M_InOutNoticeLine_ID, String trxName)
    {
      super (ctx, M_InOutNoticeLine_ID, trxName);
      /** if (M_InOutNoticeLine_ID == 0)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOutNoticeLine_ID (0);
			setM_InOutNotice_ID (0);
			setM_Product_ID (0);
			setQtyDelivered (Env.ZERO);
			setQtyEntered (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_M_InOutNoticeLine (Properties ctx, int M_InOutNoticeLine_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, M_InOutNoticeLine_ID, trxName, virtualColumns);
      /** if (M_InOutNoticeLine_ID == 0)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOutNoticeLine_ID (0);
			setM_InOutNotice_ID (0);
			setM_Product_ID (0);
			setQtyDelivered (Env.ZERO);
			setQtyEntered (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_M_InOutNoticeLine (Properties ctx, String M_InOutNoticeLine_UU, String trxName)
    {
      super (ctx, M_InOutNoticeLine_UU, trxName);
      /** if (M_InOutNoticeLine_UU == null)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOutNoticeLine_ID (0);
			setM_InOutNotice_ID (0);
			setM_Product_ID (0);
			setQtyDelivered (Env.ZERO);
			setQtyEntered (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_M_InOutNoticeLine (Properties ctx, String M_InOutNoticeLine_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, M_InOutNoticeLine_UU, trxName, virtualColumns);
      /** if (M_InOutNoticeLine_UU == null)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOutNoticeLine_ID (0);
			setM_InOutNotice_ID (0);
			setM_Product_ID (0);
			setQtyDelivered (Env.ZERO);
			setQtyEntered (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_M_InOutNoticeLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_M_InOutNoticeLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException
	{
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_ID)
			.getPO(getC_OrderLine_ID(), get_TrxName());
	}

	/** Set Sales Order Line.
		@param C_OrderLine_ID Sales Order Line
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
	public int getC_OrderLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException
	{
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_ID)
			.getPO(getC_UOM_ID(), get_TrxName());
	}

	/** Set UOM.
		@param C_UOM_ID Unit of Measure
	*/
	public void setC_UOM_ID (int C_UOM_ID)
	{
		if (C_UOM_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_UOM_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_UOM_ID, Integer.valueOf(C_UOM_ID));
	}

	/** Get UOM.
		@return Unit of Measure
	  */
	public int getC_UOM_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Line No.
		@param Line Unique line for this document
	*/
	public void setLine (int Line)
	{
		set_ValueNoCheck (COLUMNNAME_Line, Integer.valueOf(Line));
	}

	/** Get Line No.
		@return Unique line for this document
	  */
	public int getLine()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Line);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_InOutLine getM_InOutLine() throws RuntimeException
	{
		return (org.compiere.model.I_M_InOutLine)MTable.get(getCtx(), org.compiere.model.I_M_InOutLine.Table_ID)
			.getPO(getM_InOutLine_ID(), get_TrxName());
	}

	/** Set Shipment/Receipt Line.
		@param M_InOutLine_ID Line on Shipment or Receipt document
	*/
	public void setM_InOutLine_ID (int M_InOutLine_ID)
	{
		if (M_InOutLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOutLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOutLine_ID, Integer.valueOf(M_InOutLine_ID));
	}

	/** Get Shipment/Receipt Line.
		@return Line on Shipment or Receipt document
	  */
	public int getM_InOutLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#21457;&#36135;&#36890;&#30693;&#26126;&#32454;.
		@param M_InOutNoticeLine_ID &#21457;&#36135;&#36890;&#30693;&#26126;&#32454;
	*/
	public void setM_InOutNoticeLine_ID (int M_InOutNoticeLine_ID)
	{
		if (M_InOutNoticeLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOutNoticeLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOutNoticeLine_ID, Integer.valueOf(M_InOutNoticeLine_ID));
	}

	/** Get &#21457;&#36135;&#36890;&#30693;&#26126;&#32454;.
		@return &#21457;&#36135;&#36890;&#30693;&#26126;&#32454;	  */
	public int getM_InOutNoticeLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutNoticeLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set M_InOutNoticeLine_UU.
		@param M_InOutNoticeLine_UU M_InOutNoticeLine_UU
	*/
	public void setM_InOutNoticeLine_UU (String M_InOutNoticeLine_UU)
	{
		set_ValueNoCheck (COLUMNNAME_M_InOutNoticeLine_UU, M_InOutNoticeLine_UU);
	}

	/** Get M_InOutNoticeLine_UU.
		@return M_InOutNoticeLine_UU	  */
	public String getM_InOutNoticeLine_UU()
	{
		return (String)get_Value(COLUMNNAME_M_InOutNoticeLine_UU);
	}

	public I_M_InOutNotice getM_InOutNotice() throws RuntimeException
	{
		return (I_M_InOutNotice)MTable.get(getCtx(), I_M_InOutNotice.Table_ID)
			.getPO(getM_InOutNotice_ID(), get_TrxName());
	}

	/** Set &#21457;&#36135;&#36890;&#30693;&#21333;.
		@param M_InOutNotice_ID &#21457;&#36135;&#36890;&#30693;&#21333;
	*/
	public void setM_InOutNotice_ID (int M_InOutNotice_ID)
	{
		if (M_InOutNotice_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOutNotice_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOutNotice_ID, Integer.valueOf(M_InOutNotice_ID));
	}

	/** Get &#21457;&#36135;&#36890;&#30693;&#21333;.
		@return &#21457;&#36135;&#36890;&#30693;&#21333;	  */
	public int getM_InOutNotice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutNotice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
	{
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_ID)
			.getPO(getM_Product_ID(), get_TrxName());
	}

	/** Set Product.
		@param M_Product_ID Product, Service, Item
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
	public int getM_Product_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Delivered Quantity.
		@param QtyDelivered Delivered Quantity
	*/
	public void setQtyDelivered (BigDecimal QtyDelivered)
	{
		set_ValueNoCheck (COLUMNNAME_QtyDelivered, QtyDelivered);
	}

	/** Get Delivered Quantity.
		@return Delivered Quantity
	  */
	public BigDecimal getQtyDelivered()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyDelivered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Quantity.
		@param QtyEntered The Quantity Entered is based on the selected UoM
	*/
	public void setQtyEntered (BigDecimal QtyEntered)
	{
		set_ValueNoCheck (COLUMNNAME_QtyEntered, QtyEntered);
	}

	/** Get Quantity.
		@return The Quantity Entered is based on the selected UoM
	  */
	public BigDecimal getQtyEntered()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyEntered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}