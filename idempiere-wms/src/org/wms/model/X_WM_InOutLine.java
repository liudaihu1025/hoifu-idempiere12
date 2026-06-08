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

/** Generated Model for WM_InOutLine
 *  @author iDempiere (generated) 
 *  @version Release 6.2 - $Id$ */
public class X_WM_InOutLine extends PO implements I_WM_InOutLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190704L;

    /** Standard Constructor */
    public X_WM_InOutLine (Properties ctx, int WM_InOutLine_ID, String trxName)
    {
      super (ctx, WM_InOutLine_ID, trxName);
      /** if (WM_InOutLine_ID == 0)
        {
			setWM_InOutLine_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_InOutLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_InOutLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
    {
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_Name)
			.getPO(getC_DocType_ID(), get_TrxName());	}

	/** Set Document Type.
		@param C_DocType_ID 
		Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0) 
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else 
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set IsPacked.
		@param IsPacked IsPacked	  */
	public void setIsPacked (boolean IsPacked)
	{
		set_Value (COLUMNNAME_IsPacked, Boolean.valueOf(IsPacked));
	}

	/** Get IsPacked.
		@return IsPacked	  */
	public boolean isPacked () 
	{
		Object oo = get_Value(COLUMNNAME_IsPacked);
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

	public I_M_Locator getM_LocatorOld() throws RuntimeException
    {
		return (I_M_Locator)MTable.get(getCtx(), I_M_Locator.Table_Name)
			.getPO(getM_LocatorOld_ID(), get_TrxName());	}

	/** Set M_LocatorOld_ID.
		@param M_LocatorOld_ID M_LocatorOld_ID	  */
	public void setM_LocatorOld_ID (int M_LocatorOld_ID)
	{
		if (M_LocatorOld_ID < 1) 
			set_Value (COLUMNNAME_M_LocatorOld_ID, null);
		else 
			set_Value (COLUMNNAME_M_LocatorOld_ID, Integer.valueOf(M_LocatorOld_ID));
	}

	/** Get M_LocatorOld_ID.
		@return M_LocatorOld_ID	  */
	public int getM_LocatorOld_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_LocatorOld_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_MovementLine getM_MovementLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_MovementLine)MTable.get(getCtx(), org.compiere.model.I_M_MovementLine.Table_Name)
			.getPO(getM_MovementLine_ID(), get_TrxName());	}

	/** Set Move Line.
		@param M_MovementLine_ID 
		Inventory Move document Line
	  */
	public void setM_MovementLine_ID (int M_MovementLine_ID)
	{
		if (M_MovementLine_ID < 1) 
			set_Value (COLUMNNAME_M_MovementLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_MovementLine_ID, Integer.valueOf(M_MovementLine_ID));
	}

	/** Get Move Line.
		@return Inventory Move document Line
	  */
	public int getM_MovementLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_MovementLine_ID);
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

	/** Set QtyPicked.
		@param QtyPicked QtyPicked	  */
	public void setQtyPicked (BigDecimal QtyPicked)
	{
		set_Value (COLUMNNAME_QtyPicked, QtyPicked);
	}

	/** Get QtyPicked.
		@return QtyPicked	  */
	public BigDecimal getQtyPicked () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyPicked);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Sequence.
		@param Sequence Sequence	  */
	public void setSequence (BigDecimal Sequence)
	{
		set_Value (COLUMNNAME_Sequence, Sequence);
	}

	/** Get Sequence.
		@return Sequence	  */
	public BigDecimal getSequence () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Sequence);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_WM_DeliveryScheduleLine getWM_DeliveryScheduleLine() throws RuntimeException
    {
		return (I_WM_DeliveryScheduleLine)MTable.get(getCtx(), I_WM_DeliveryScheduleLine.Table_Name)
			.getPO(getWM_DeliveryScheduleLine_ID(), get_TrxName());	}

	/** Set DeliveryScheduleLine.
		@param WM_DeliveryScheduleLine_ID DeliveryScheduleLine	  */
	public void setWM_DeliveryScheduleLine_ID (int WM_DeliveryScheduleLine_ID)
	{
		if (WM_DeliveryScheduleLine_ID < 1) 
			set_Value (COLUMNNAME_WM_DeliveryScheduleLine_ID, null);
		else 
			set_Value (COLUMNNAME_WM_DeliveryScheduleLine_ID, Integer.valueOf(WM_DeliveryScheduleLine_ID));
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

	/** Set WM_HandlingUnitOld_ID.
		@param WM_HandlingUnitOld_ID WM_HandlingUnitOld_ID	  */
	public void setWM_HandlingUnitOld_ID (int WM_HandlingUnitOld_ID)
	{
		if (WM_HandlingUnitOld_ID < 1) 
			set_Value (COLUMNNAME_WM_HandlingUnitOld_ID, null);
		else 
			set_Value (COLUMNNAME_WM_HandlingUnitOld_ID, Integer.valueOf(WM_HandlingUnitOld_ID));
	}

	/** Get WM_HandlingUnitOld_ID.
		@return WM_HandlingUnitOld_ID	  */
	public int getWM_HandlingUnitOld_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_HandlingUnitOld_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_WM_InOut getWM_InOut() throws RuntimeException
    {
		return (I_WM_InOut)MTable.get(getCtx(), I_WM_InOut.Table_Name)
			.getPO(getWM_InOut_ID(), get_TrxName());	}

	/** Set InOut.
		@param WM_InOut_ID InOut	  */
	public void setWM_InOut_ID (int WM_InOut_ID)
	{
		if (WM_InOut_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_InOut_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_InOut_ID, Integer.valueOf(WM_InOut_ID));
	}

	/** Get InOut.
		@return InOut	  */
	public int getWM_InOut_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_InOut_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WM_InOutLine_ID.
		@param WM_InOutLine_ID WM_InOutLine_ID	  */
	public void setWM_InOutLine_ID (int WM_InOutLine_ID)
	{
		if (WM_InOutLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_InOutLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_InOutLine_ID, Integer.valueOf(WM_InOutLine_ID));
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

	/** Set WM_InOutLine_UU.
		@param WM_InOutLine_UU WM_InOutLine_UU	  */
	public void setWM_InOutLine_UU (String WM_InOutLine_UU)
	{
		set_Value (COLUMNNAME_WM_InOutLine_UU, WM_InOutLine_UU);
	}

	/** Get WM_InOutLine_UU.
		@return WM_InOutLine_UU	  */
	public String getWM_InOutLine_UU () 
	{
		return (String)get_Value(COLUMNNAME_WM_InOutLine_UU);
	}
}