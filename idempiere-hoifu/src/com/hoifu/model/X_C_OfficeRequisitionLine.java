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

import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_Locator;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;
import org.eevolution.model.I_PP_Order;

/** Generated Model for C_OfficeRequisitionLine
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_OfficeRequisitionLine")
public class X_C_OfficeRequisitionLine extends PO implements I_C_OfficeRequisitionLine, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260606L;

    /** Standard Constructor */
    public X_C_OfficeRequisitionLine (Properties ctx, int C_OfficeRequisitionLine_ID, String trxName)
    {
      super (ctx, C_OfficeRequisitionLine_ID, trxName);
      /** if (C_OfficeRequisitionLine_ID == 0)
        {
			setC_OfficeRequisitionLine_ID (0);
			setC_OfficeRequisition_ID (0);
			setInventoryType (null);
// D
			setQtyBook (Env.ZERO);
			setQtyCount (Env.ZERO);
			setQtyCsv (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_OfficeRequisitionLine (Properties ctx, int C_OfficeRequisitionLine_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_OfficeRequisitionLine_ID, trxName, virtualColumns);
      /** if (C_OfficeRequisitionLine_ID == 0)
        {
			setC_OfficeRequisitionLine_ID (0);
			setC_OfficeRequisition_ID (0);
			setInventoryType (null);
// D
			setQtyBook (Env.ZERO);
			setQtyCount (Env.ZERO);
			setQtyCsv (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_OfficeRequisitionLine (Properties ctx, String C_OfficeRequisitionLine_UU, String trxName)
    {
      super (ctx, C_OfficeRequisitionLine_UU, trxName);
      /** if (C_OfficeRequisitionLine_UU == null)
        {
			setC_OfficeRequisitionLine_ID (0);
			setC_OfficeRequisition_ID (0);
			setInventoryType (null);
// D
			setQtyBook (Env.ZERO);
			setQtyCount (Env.ZERO);
			setQtyCsv (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_C_OfficeRequisitionLine (Properties ctx, String C_OfficeRequisitionLine_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_OfficeRequisitionLine_UU, trxName, virtualColumns);
      /** if (C_OfficeRequisitionLine_UU == null)
        {
			setC_OfficeRequisitionLine_ID (0);
			setC_OfficeRequisition_ID (0);
			setInventoryType (null);
// D
			setQtyBook (Env.ZERO);
			setQtyCount (Env.ZERO);
			setQtyCsv (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_C_OfficeRequisitionLine (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org
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
      StringBuilder sb = new StringBuilder ("X_C_OfficeRequisitionLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_Charge getC_Charge() throws RuntimeException
	{
		return (org.compiere.model.I_C_Charge)MTable.get(getCtx(), org.compiere.model.I_C_Charge.Table_ID)
			.getPO(getC_Charge_ID(), get_TrxName());
	}

	/** Set Charge.
		@param C_Charge_ID Additional document charges
	*/
	public void setC_Charge_ID (int C_Charge_ID)
	{
		if (C_Charge_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Charge_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Charge_ID, Integer.valueOf(C_Charge_ID));
	}

	/** Get Charge.
		@return Additional document charges
	  */
	public int getC_Charge_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Charge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_OfficeRequisitionLine.
		@param C_OfficeRequisitionLine_ID C_OfficeRequisitionLine
	*/
	public void setC_OfficeRequisitionLine_ID (int C_OfficeRequisitionLine_ID)
	{
		if (C_OfficeRequisitionLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_OfficeRequisitionLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_OfficeRequisitionLine_ID, Integer.valueOf(C_OfficeRequisitionLine_ID));
	}

	/** Get C_OfficeRequisitionLine.
		@return C_OfficeRequisitionLine	  */
	public int getC_OfficeRequisitionLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OfficeRequisitionLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_OfficeRequisitionLine_UU.
		@param C_OfficeRequisitionLine_UU C_OfficeRequisitionLine_UU
	*/
	public void setC_OfficeRequisitionLine_UU (String C_OfficeRequisitionLine_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_OfficeRequisitionLine_UU, C_OfficeRequisitionLine_UU);
	}

	/** Get C_OfficeRequisitionLine_UU.
		@return C_OfficeRequisitionLine_UU	  */
	public String getC_OfficeRequisitionLine_UU()
	{
		return (String)get_Value(COLUMNNAME_C_OfficeRequisitionLine_UU);
	}

	public I_C_OfficeRequisition getC_OfficeRequisition() throws RuntimeException
	{
		return (I_C_OfficeRequisition)MTable.get(getCtx(), I_C_OfficeRequisition.Table_ID)
			.getPO(getC_OfficeRequisition_ID(), get_TrxName());
	}

	/** Set C_OfficeRequisition.
		@param C_OfficeRequisition_ID C_OfficeRequisition
	*/
	public void setC_OfficeRequisition_ID (int C_OfficeRequisition_ID)
	{
		if (C_OfficeRequisition_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_OfficeRequisition_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_OfficeRequisition_ID, Integer.valueOf(C_OfficeRequisition_ID));
	}

	/** Get C_OfficeRequisition.
		@return C_OfficeRequisition	  */
	public int getC_OfficeRequisition_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OfficeRequisition_ID);
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

	/** &#37319;&#36141;&#20013; = CG */
	public static final String CLAIMSTATUS_采购中 = "CG";
	/** &#37096;&#20998;&#39046;&#29992; = PR */
	public static final String CLAIMSTATUS_部分领用 = "PR";
	/** &#26410;&#39046;&#21462; = WL */
	public static final String CLAIMSTATUS_未领取 = "WL";
	/** &#24050;&#39046;&#21462; = YL */
	public static final String CLAIMSTATUS_已领取 = "YL";
	/** Set &#39046;&#21462;&#29366;&#24577;.
		@param ClaimStatus &#39046;&#21462;&#29366;&#24577;
	*/
	public void setClaimStatus (String ClaimStatus)
	{

		set_ValueNoCheck (COLUMNNAME_ClaimStatus, ClaimStatus);
	}

	/** Get &#39046;&#21462;&#29366;&#24577;.
		@return &#39046;&#21462;&#29366;&#24577;	  */
	public String getClaimStatus()
	{
		return (String)get_Value(COLUMNNAME_ClaimStatus);
	}

	/** Set Current Cost Price.
		@param CurrentCostPrice The currently used cost price
	*/
	public void setCurrentCostPrice (BigDecimal CurrentCostPrice)
	{
		set_Value (COLUMNNAME_CurrentCostPrice, CurrentCostPrice);
	}

	/** Get Current Cost Price.
		@return The currently used cost price
	  */
	public BigDecimal getCurrentCostPrice()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CurrentCostPrice);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** InventoryType AD_Reference_ID=292 */
	public static final int INVENTORYTYPE_AD_Reference_ID=292;
	/** Charge Account = C */
	public static final String INVENTORYTYPE_ChargeAccount = "C";
	/** Inventory Difference = D */
	public static final String INVENTORYTYPE_InventoryDifference = "D";
	/** Set Inventory Type.
		@param InventoryType Type of inventory difference
	*/
	public void setInventoryType (String InventoryType)
	{

		set_ValueNoCheck (COLUMNNAME_InventoryType, InventoryType);
	}

	/** Get Inventory Type.
		@return Type of inventory difference
	  */
	public String getInventoryType()
	{
		return (String)get_Value(COLUMNNAME_InventoryType);
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

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException
	{
		return (I_M_AttributeSetInstance)MTable.get(getCtx(), I_M_AttributeSetInstance.Table_ID)
			.getPO(getM_AttributeSetInstance_ID(), get_TrxName());
	}

	/** Set Attribute Set Instance.
		@param M_AttributeSetInstance_ID Product Attribute Set Instance
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
	public int getM_AttributeSetInstance_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_AttributeSetInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Locator getM_Locator() throws RuntimeException
	{
		return (I_M_Locator)MTable.get(getCtx(), I_M_Locator.Table_ID)
			.getPO(getM_Locator_ID(), get_TrxName());
	}

	/** Set Locator.
		@param M_Locator_ID Warehouse Locator
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
	public int getM_Locator_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Locator_ID);
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

	/** Set New Cost Price.
		@param NewCostPrice New current cost price after processing of M_CostDetail
	*/
	public void setNewCostPrice (BigDecimal NewCostPrice)
	{
		set_ValueNoCheck (COLUMNNAME_NewCostPrice, NewCostPrice);
	}

	/** Get New Cost Price.
		@return New current cost price after processing of M_CostDetail
	  */
	public BigDecimal getNewCostPrice()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_NewCostPrice);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_PP_Order getPP_Order() throws RuntimeException
	{
		return (I_PP_Order)MTable.get(getCtx(), I_PP_Order.Table_ID)
			.getPO(getPP_Order_ID(), get_TrxName());
	}

	/** Set Manufacturing Order.
		@param PP_Order_ID Manufacturing Order
	*/
	public void setPP_Order_ID (int PP_Order_ID)
	{
		if (PP_Order_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, Integer.valueOf(PP_Order_ID));
	}

	/** Get Manufacturing Order.
		@return Manufacturing Order
	  */
	public int getPP_Order_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Quantity book.
		@param QtyBook Book Quantity
	*/
	public void setQtyBook (BigDecimal QtyBook)
	{
		set_ValueNoCheck (COLUMNNAME_QtyBook, QtyBook);
	}

	/** Get Quantity book.
		@return Book Quantity
	  */
	public BigDecimal getQtyBook()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyBook);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Quantity count.
		@param QtyCount Counted Quantity
	*/
	public void setQtyCount (BigDecimal QtyCount)
	{
		set_ValueNoCheck (COLUMNNAME_QtyCount, QtyCount);
	}

	/** Get Quantity count.
		@return Counted Quantity
	  */
	public BigDecimal getQtyCount()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyCount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Qty Csv.
		@param QtyCsv Qty Csv
	*/
	public void setQtyCsv (BigDecimal QtyCsv)
	{
		set_ValueNoCheck (COLUMNNAME_QtyCsv, QtyCsv);
	}

	/** Get Qty Csv.
		@return Qty Csv	  */
	public BigDecimal getQtyCsv()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyCsv);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set QtyDemand.
		@param QtyDemand QtyDemand
	*/
	public void setQtyDemand (BigDecimal QtyDemand)
	{
		set_ValueNoCheck (COLUMNNAME_QtyDemand, QtyDemand);
	}

	/** Get QtyDemand.
		@return QtyDemand	  */
	public BigDecimal getQtyDemand()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyDemand);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Internal Use Qty.
		@param QtyInternalUse Internal Use Quantity removed from Inventory
	*/
	public void setQtyInternalUse (BigDecimal QtyInternalUse)
	{
		set_ValueNoCheck (COLUMNNAME_QtyInternalUse, QtyInternalUse);
	}

	/** Get Internal Use Qty.
		@return Internal Use Quantity removed from Inventory
	  */
	public BigDecimal getQtyInternalUse()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyInternalUse);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set &#25512;&#33616;&#20379;&#24212;&#21830;.
		@param RSupplier &#25512;&#33616;&#20379;&#24212;&#21830;
	*/
	public void setRSupplier (String RSupplier)
	{
		set_Value (COLUMNNAME_RSupplier, RSupplier);
	}

	/** Get &#25512;&#33616;&#20379;&#24212;&#21830;.
		@return &#25512;&#33616;&#20379;&#24212;&#21830;	  */
	public String getRSupplier()
	{
		return (String)get_Value(COLUMNNAME_RSupplier);
	}

	/** Set &#35268;&#26684;.
		@param Specification &#35268;&#26684;
	*/
	public void setSpecification (String Specification)
	{
		set_ValueNoCheck (COLUMNNAME_Specification, Specification);
	}

	/** Get &#35268;&#26684;.
		@return &#35268;&#26684;	  */
	public String getSpecification()
	{
		return (String)get_Value(COLUMNNAME_Specification);
	}

	/** Set quantityPicked.
		@param quantityPicked quantityPicked
	*/
	public void setquantityPicked (BigDecimal quantityPicked)
	{
		set_Value (COLUMNNAME_quantityPicked, quantityPicked);
	}

	/** Get quantityPicked.
		@return quantityPicked	  */
	public BigDecimal getquantityPicked()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_quantityPicked);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}