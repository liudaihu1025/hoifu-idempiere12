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
import org.eevolution.model.I_PP_Order;
import org.eevolution.model.I_PP_Order_BOMLine;
import org.eevolution.model.I_PP_Order_Node;

/** Generated Model for M_InOut_RequisitionLine
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="M_InOut_RequisitionLine")
public class X_M_InOut_RequisitionLine extends PO implements I_M_InOut_RequisitionLine, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260910L;

    /** Standard Constructor */
    public X_M_InOut_RequisitionLine (Properties ctx, int M_InOut_RequisitionLine_ID, String trxName)
    {
      super (ctx, M_InOut_RequisitionLine_ID, trxName);
      /** if (M_InOut_RequisitionLine_ID == 0)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOut_RequisitionLine_ID (0);
			setM_InOut_Requisition_ID (0);
			setM_Product_ID (0);
			setQtyGenerated (Env.ZERO);
			setQtyReject (Env.ZERO);
			setQtyRequested (Env.ZERO);
			setScrappedQty (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_M_InOut_RequisitionLine (Properties ctx, int M_InOut_RequisitionLine_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, M_InOut_RequisitionLine_ID, trxName, virtualColumns);
      /** if (M_InOut_RequisitionLine_ID == 0)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOut_RequisitionLine_ID (0);
			setM_InOut_Requisition_ID (0);
			setM_Product_ID (0);
			setQtyGenerated (Env.ZERO);
			setQtyReject (Env.ZERO);
			setQtyRequested (Env.ZERO);
			setScrappedQty (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_M_InOut_RequisitionLine (Properties ctx, String M_InOut_RequisitionLine_UU, String trxName)
    {
      super (ctx, M_InOut_RequisitionLine_UU, trxName);
      /** if (M_InOut_RequisitionLine_UU == null)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOut_RequisitionLine_ID (0);
			setM_InOut_Requisition_ID (0);
			setM_Product_ID (0);
			setQtyGenerated (Env.ZERO);
			setQtyReject (Env.ZERO);
			setQtyRequested (Env.ZERO);
			setScrappedQty (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_M_InOut_RequisitionLine (Properties ctx, String M_InOut_RequisitionLine_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, M_InOut_RequisitionLine_UU, trxName, virtualColumns);
      /** if (M_InOut_RequisitionLine_UU == null)
        {
			setC_UOM_ID (0);
			setLine (0);
			setM_InOut_RequisitionLine_ID (0);
			setM_InOut_Requisition_ID (0);
			setM_Product_ID (0);
			setQtyGenerated (Env.ZERO);
			setQtyReject (Env.ZERO);
			setQtyRequested (Env.ZERO);
			setScrappedQty (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_M_InOut_RequisitionLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_M_InOut_RequisitionLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Activity getC_Activity() throws RuntimeException
	{
		return (org.compiere.model.I_C_Activity)MTable.get(getCtx(), org.compiere.model.I_C_Activity.Table_ID)
			.getPO(getC_Activity_ID(), get_TrxName());
	}

	/** Set Activity.
		@param C_Activity_ID Business Activity
	*/
	public void setC_Activity_ID (int C_Activity_ID)
	{
		if (C_Activity_ID < 1)
			set_Value (COLUMNNAME_C_Activity_ID, null);
		else
			set_Value (COLUMNNAME_C_Activity_ID, Integer.valueOf(C_Activity_ID));
	}

	/** Get Activity.
		@return Business Activity
	  */
	public int getC_Activity_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Activity_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Campaign getC_Campaign() throws RuntimeException
	{
		return (org.compiere.model.I_C_Campaign)MTable.get(getCtx(), org.compiere.model.I_C_Campaign.Table_ID)
			.getPO(getC_Campaign_ID(), get_TrxName());
	}

	/** Set Campaign.
		@param C_Campaign_ID Marketing Campaign
	*/
	public void setC_Campaign_ID (int C_Campaign_ID)
	{
		if (C_Campaign_ID < 1)
			set_Value (COLUMNNAME_C_Campaign_ID, null);
		else
			set_Value (COLUMNNAME_C_Campaign_ID, Integer.valueOf(C_Campaign_ID));
	}

	/** Get Campaign.
		@return Marketing Campaign
	  */
	public int getC_Campaign_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Campaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_C_OfficeRequisitionLine getC_OfficeRequisitionLine() throws RuntimeException
	{
		return (I_C_OfficeRequisitionLine)MTable.get(getCtx(), I_C_OfficeRequisitionLine.Table_ID)
			.getPO(getC_OfficeRequisitionLine_ID(), get_TrxName());
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

	public org.compiere.model.I_C_Project getC_Project() throws RuntimeException
	{
		return (org.compiere.model.I_C_Project)MTable.get(getCtx(), org.compiere.model.I_C_Project.Table_ID)
			.getPO(getC_Project_ID(), get_TrxName());
	}

	/** Set Project.
		@param C_Project_ID Financial Project
	*/
	public void setC_Project_ID (int C_Project_ID)
	{
		if (C_Project_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Project_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Project_ID, Integer.valueOf(C_Project_ID));
	}

	/** Get Project.
		@return Financial Project
	  */
	public int getC_Project_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Project_ID);
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

	/** CostCollectorType AD_Reference_ID=53287 */
	public static final int COSTCOLLECTORTYPE_AD_Reference_ID=53287;
	/** Material Receipt = 100 */
	public static final String COSTCOLLECTORTYPE_MaterialReceipt = "100";
	/** Component Issue = 110 */
	public static final String COSTCOLLECTORTYPE_ComponentIssue = "110";
	/** Production Return = 115 */
	public static final String COSTCOLLECTORTYPE_ProductionReturn = "115";
	/** Production replenishment = 116 */
	public static final String COSTCOLLECTORTYPE_ProductionReplenishment = "116";
	/** Usage Variance = 120 */
	public static final String COSTCOLLECTORTYPE_UsageVariance = "120";
	/** Method Change Variance = 130 */
	public static final String COSTCOLLECTORTYPE_MethodChangeVariance = "130";
	/** &#22996;&#22806;&#21457;&#26009; = 131 */
	public static final String COSTCOLLECTORTYPE_委外发料 = "131";
	/** &#22996;&#22806;&#36864;&#26009; = 132 */
	public static final String COSTCOLLECTORTYPE_委外退料 = "132";
	/** &#22996;&#22806;&#34917;&#39046; = 133 */
	public static final String COSTCOLLECTORTYPE_委外补领 = "133";
	/** Rate Variance = 140 */
	public static final String COSTCOLLECTORTYPE_RateVariance = "140";
	/** &#29983;&#20135;&#32447;&#36793;&#20179;&#39046;&#26009; = 141 */
	public static final String COSTCOLLECTORTYPE_生产线边仓领料 = "141";
	/** &#29983;&#20135;&#32447;&#36793;&#20179;&#36864;&#26009; = 142 */
	public static final String COSTCOLLECTORTYPE_生产线边仓退料 = "142";
	/** Mix Variance = 150 */
	public static final String COSTCOLLECTORTYPE_MixVariance = "150";
	/** Activity Control = 160 */
	public static final String COSTCOLLECTORTYPE_ActivityControl = "160";
	/** &#38750;&#29983;&#20135;&#25253;&#24037; = 161 */
	public static final String COSTCOLLECTORTYPE_非生产报工 = "161";
	/** &#39046;&#29992;&#21333;&#39046;&#29992; = 999 */
	public static final String COSTCOLLECTORTYPE_领用单领用 = "999";
	/** Set Cost Collector Type.
		@param CostCollectorType Transaction Type for Manufacturing Management
	*/
	public void setCostCollectorType (String CostCollectorType)
	{

		set_ValueNoCheck (COLUMNNAME_CostCollectorType, CostCollectorType);
	}

	/** Get Cost Collector Type.
		@return Transaction Type for Manufacturing Management
	  */
	public String getCostCollectorType()
	{
		return (String)get_Value(COLUMNNAME_CostCollectorType);
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
			set_ValueNoCheck (COLUMNNAME_M_AttributeSetInstance_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_AttributeSetInstance_ID, Integer.valueOf(M_AttributeSetInstance_ID));
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

	/** Set &#20986;&#20837;&#24211;&#30003;&#35831;&#26126;&#32454;.
		@param M_InOut_RequisitionLine_ID &#20986;&#20837;&#24211;&#30003;&#35831;&#26126;&#32454;
	*/
	public void setM_InOut_RequisitionLine_ID (int M_InOut_RequisitionLine_ID)
	{
		if (M_InOut_RequisitionLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOut_RequisitionLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOut_RequisitionLine_ID, Integer.valueOf(M_InOut_RequisitionLine_ID));
	}

	/** Get &#20986;&#20837;&#24211;&#30003;&#35831;&#26126;&#32454;.
		@return &#20986;&#20837;&#24211;&#30003;&#35831;&#26126;&#32454;	  */
	public int getM_InOut_RequisitionLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOut_RequisitionLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_InOut_Requisition getM_InOut_Requisition() throws RuntimeException
	{
		return (I_M_InOut_Requisition)MTable.get(getCtx(), I_M_InOut_Requisition.Table_ID)
			.getPO(getM_InOut_Requisition_ID(), get_TrxName());
	}

	/** Set &#20986;&#20837;&#24211;&#30003;&#35831;&#34920;.
		@param M_InOut_Requisition_ID &#20986;&#20837;&#24211;&#30003;&#35831;&#34920;
	*/
	public void setM_InOut_Requisition_ID (int M_InOut_Requisition_ID)
	{
		if (M_InOut_Requisition_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOut_Requisition_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOut_Requisition_ID, Integer.valueOf(M_InOut_Requisition_ID));
	}

	/** Get &#20986;&#20837;&#24211;&#30003;&#35831;&#34920;.
		@return &#20986;&#20837;&#24211;&#30003;&#35831;&#34920;	  */
	public int getM_InOut_Requisition_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOut_Requisition_ID);
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

	public org.compiere.model.I_M_RMALine getM_RMALine() throws RuntimeException
	{
		return (org.compiere.model.I_M_RMALine)MTable.get(getCtx(), org.compiere.model.I_M_RMALine.Table_ID)
			.getPO(getM_RMALine_ID(), get_TrxName());
	}

	/** Set RMA Line.
		@param M_RMALine_ID Return Material Authorization Line
	*/
	public void setM_RMALine_ID (int M_RMALine_ID)
	{
		if (M_RMALine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_RMALine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_RMALine_ID, Integer.valueOf(M_RMALine_ID));
	}

	/** Get RMA Line.
		@return Return Material Authorization Line
	  */
	public int getM_RMALine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_RMALine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_PP_Order_BOMLine getPP_Order_BOMLine() throws RuntimeException
	{
		return (I_PP_Order_BOMLine)MTable.get(getCtx(), I_PP_Order_BOMLine.Table_ID)
			.getPO(getPP_Order_BOMLine_ID(), get_TrxName());
	}

	/** Set Manufacturing Order BOM Line.
		@param PP_Order_BOMLine_ID Manufacturing Order BOM Line
	*/
	public void setPP_Order_BOMLine_ID (int PP_Order_BOMLine_ID)
	{
		if (PP_Order_BOMLine_ID < 1)
			set_Value (COLUMNNAME_PP_Order_BOMLine_ID, null);
		else
			set_Value (COLUMNNAME_PP_Order_BOMLine_ID, Integer.valueOf(PP_Order_BOMLine_ID));
	}

	/** Get Manufacturing Order BOM Line.
		@return Manufacturing Order BOM Line	  */
	public int getPP_Order_BOMLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_BOMLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_PP_Order_Node getPP_Order_Node() throws RuntimeException
	{
		return (I_PP_Order_Node)MTable.get(getCtx(), I_PP_Order_Node.Table_ID)
			.getPO(getPP_Order_Node_ID(), get_TrxName());
	}

	/** Set Manufacturing Order Activity.
		@param PP_Order_Node_ID Workflow Node (activity), step or process
	*/
	public void setPP_Order_Node_ID (int PP_Order_Node_ID)
	{
		if (PP_Order_Node_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Order_Node_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Order_Node_ID, Integer.valueOf(PP_Order_Node_ID));
	}

	/** Get Manufacturing Order Activity.
		@return Workflow Node (activity), step or process
	  */
	public int getPP_Order_Node_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_Node_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set QtyGenerated.
		@param QtyGenerated QtyGenerated
	*/
	public void setQtyGenerated (BigDecimal QtyGenerated)
	{
		set_Value (COLUMNNAME_QtyGenerated, QtyGenerated);
	}

	/** Get QtyGenerated.
		@return QtyGenerated	  */
	public BigDecimal getQtyGenerated()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyGenerated);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Qty Reject.
		@param QtyReject Qty Reject
	*/
	public void setQtyReject (BigDecimal QtyReject)
	{
		set_ValueNoCheck (COLUMNNAME_QtyReject, QtyReject);
	}

	/** Get Qty Reject.
		@return Qty Reject	  */
	public BigDecimal getQtyReject()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyReject);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set QtyRequested.
		@param QtyRequested QtyRequested
	*/
	public void setQtyRequested (BigDecimal QtyRequested)
	{
		set_Value (COLUMNNAME_QtyRequested, QtyRequested);
	}

	/** Get QtyRequested.
		@return QtyRequested	  */
	public BigDecimal getQtyRequested()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyRequested);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_S_Resource getS_Resource() throws RuntimeException
	{
		return (org.compiere.model.I_S_Resource)MTable.get(getCtx(), org.compiere.model.I_S_Resource.Table_ID)
			.getPO(getS_Resource_ID(), get_TrxName());
	}

	/** Set Resource.
		@param S_Resource_ID Resource
	*/
	public void setS_Resource_ID (int S_Resource_ID)
	{
		if (S_Resource_ID < 1)
			set_ValueNoCheck (COLUMNNAME_S_Resource_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_S_Resource_ID, Integer.valueOf(S_Resource_ID));
	}

	/** Get Resource.
		@return Resource
	  */
	public int getS_Resource_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Resource_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Scrapped Quantity.
		@param ScrappedQty The Quantity scrapped due to QA issues
	*/
	public void setScrappedQty (BigDecimal ScrappedQty)
	{
		set_ValueNoCheck (COLUMNNAME_ScrappedQty, ScrappedQty);
	}

	/** Get Scrapped Quantity.
		@return The Quantity scrapped due to QA issues
	  */
	public BigDecimal getScrappedQty()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ScrappedQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set DB Table Name.
		@param TableName Name of the table in the database
	*/
	public void setTableName (String TableName)
	{
		set_Value (COLUMNNAME_TableName, TableName);
	}

	/** Get DB Table Name.
		@return Name of the table in the database
	  */
	public String getTableName()
	{
		return (String)get_Value(COLUMNNAME_TableName);
	}
}