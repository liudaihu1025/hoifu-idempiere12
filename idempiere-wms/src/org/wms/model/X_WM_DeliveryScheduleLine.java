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

/** Generated Model for WM_DeliveryScheduleLine
 *  @author iDempiere (generated) 
 *  @version Release 6.2 - $Id$ */
public class X_WM_DeliveryScheduleLine extends PO implements I_WM_DeliveryScheduleLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190311L;

    /** Standard Constructor */
    public X_WM_DeliveryScheduleLine (Properties ctx, int WM_DeliveryScheduleLine_ID, String trxName)
    {
      super (ctx, WM_DeliveryScheduleLine_ID, trxName);
      /** if (WM_DeliveryScheduleLine_ID == 0)
        {
			setWM_DeliveryScheduleLine_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_DeliveryScheduleLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_DeliveryScheduleLine[")
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
			set_Value (COLUMNNAME_C_OrderLine_ID, null);
		else 
			set_Value (COLUMNNAME_C_OrderLine_ID, Integer.valueOf(C_OrderLine_ID));
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

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException
    {
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_Name)
			.getPO(getC_UOM_ID(), get_TrxName());	}

	/** Set UOM.
		@param C_UOM_ID 
		Unit of Measure
	  */
	public void setC_UOM_ID (int C_UOM_ID)
	{
		if (C_UOM_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_ID, Integer.valueOf(C_UOM_ID));
	}

	/** Get UOM.
		@return Unit of Measure
	  */
	public int getC_UOM_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set IsBackOrder.
		@param IsBackOrder IsBackOrder	  */
	public void setIsBackOrder (boolean IsBackOrder)
	{
		set_Value (COLUMNNAME_IsBackOrder, Boolean.valueOf(IsBackOrder));
	}

	/** Get IsBackOrder.
		@return IsBackOrder	  */
	public boolean isBackOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsBackOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException
    {
		return (I_M_AttributeSetInstance)MTable.get(getCtx(), I_M_AttributeSetInstance.Table_Name)
			.getPO(getM_AttributeSetInstance_ID(), get_TrxName());	}

	/** Set Attribute Set Instance.
		@param M_AttributeSetInstance_ID 
		Product Attribute Set Instance
	  */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID)
	{
		if (M_AttributeSetInstance_ID < 0) 
			set_Value (COLUMNNAME_M_AttributeSetInstance_ID, null);
		else 
			set_Value (COLUMNNAME_M_AttributeSetInstance_ID, Integer.valueOf(M_AttributeSetInstance_ID));
	}

	/** Get Attribute Set Instance.
		@return Product Attribute Set Instance
	  */
	public int getM_AttributeSetInstance_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_AttributeSetInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_InOutLine getM_InOutLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_InOutLine)MTable.get(getCtx(), org.compiere.model.I_M_InOutLine.Table_Name)
			.getPO(getM_InOutLine_ID(), get_TrxName());	}

	/** Set Shipment/Receipt Line.
		@param M_InOutLine_ID 
		Line on Shipment or Receipt document
	  */
	public void setM_InOutLine_ID (int M_InOutLine_ID)
	{
		if (M_InOutLine_ID < 1) 
			set_Value (COLUMNNAME_M_InOutLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_InOutLine_ID, Integer.valueOf(M_InOutLine_ID));
	}

	/** Get Shipment/Receipt Line.
		@return Line on Shipment or Receipt document
	  */
	public int getM_InOutLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Delivered Quantity.
		@param QtyDelivered 
		Delivered Quantity
	  */
	public void setQtyDelivered (BigDecimal QtyDelivered)
	{
		set_Value (COLUMNNAME_QtyDelivered, QtyDelivered);
	}

	/** Get Delivered Quantity.
		@return Delivered Quantity
	  */
	public BigDecimal getQtyDelivered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyDelivered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Received.
		@param Received Received	  */
	public void setReceived (boolean Received)
	{
		set_Value (COLUMNNAME_Received, Boolean.valueOf(Received));
	}

	/** Get Received.
		@return Received	  */
	public boolean isReceived () 
	{
		Object oo = get_Value(COLUMNNAME_Received);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_WM_DeliverySchedule getWM_DeliverySchedule() throws RuntimeException
    {
		return (I_WM_DeliverySchedule)MTable.get(getCtx(), I_WM_DeliverySchedule.Table_Name)
			.getPO(getWM_DeliverySchedule_ID(), get_TrxName());	}

	/** Set DeliverySchedule.
		@param WM_DeliverySchedule_ID DeliverySchedule	  */
	public void setWM_DeliverySchedule_ID (int WM_DeliverySchedule_ID)
	{
		if (WM_DeliverySchedule_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_DeliverySchedule_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_DeliverySchedule_ID, Integer.valueOf(WM_DeliverySchedule_ID));
	}

	/** Get DeliverySchedule.
		@return DeliverySchedule	  */
	public int getWM_DeliverySchedule_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_DeliverySchedule_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DeliveryScheduleLine.
		@param WM_DeliveryScheduleLine_ID DeliveryScheduleLine	  */
	public void setWM_DeliveryScheduleLine_ID (int WM_DeliveryScheduleLine_ID)
	{
		if (WM_DeliveryScheduleLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_DeliveryScheduleLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_DeliveryScheduleLine_ID, Integer.valueOf(WM_DeliveryScheduleLine_ID));
	}

	/** Get DeliveryScheduleLine.
		@return DeliveryScheduleLine	  */
	public int getWM_DeliveryScheduleLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_DeliveryScheduleLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WM_DeliveryScheduleLine_UU.
		@param WM_DeliveryScheduleLine_UU WM_DeliveryScheduleLine_UU	  */
	public void setWM_DeliveryScheduleLine_UU (String WM_DeliveryScheduleLine_UU)
	{
		set_Value (COLUMNNAME_WM_DeliveryScheduleLine_UU, WM_DeliveryScheduleLine_UU);
	}

	/** Get WM_DeliveryScheduleLine_UU.
		@return WM_DeliveryScheduleLine_UU	  */
	public String getWM_DeliveryScheduleLine_UU () 
	{
		return (String)get_Value(COLUMNNAME_WM_DeliveryScheduleLine_UU);
	}

	public I_WM_InOutLine getWM_InOutLine() throws RuntimeException
    {
		return (I_WM_InOutLine)MTable.get(getCtx(), I_WM_InOutLine.Table_Name)
			.getPO(getWM_InOutLine_ID(), get_TrxName());	}

	/** Set WM_InOutLine_ID.
		@param WM_InOutLine_ID WM_InOutLine_ID	  */
	public void setWM_InOutLine_ID (int WM_InOutLine_ID)
	{
		if (WM_InOutLine_ID < 1) 
			set_Value (COLUMNNAME_WM_InOutLine_ID, null);
		else 
			set_Value (COLUMNNAME_WM_InOutLine_ID, Integer.valueOf(WM_InOutLine_ID));
	}

	/** Get WM_InOutLine_ID.
		@return WM_InOutLine_ID	  */
	public int getWM_InOutLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_InOutLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}