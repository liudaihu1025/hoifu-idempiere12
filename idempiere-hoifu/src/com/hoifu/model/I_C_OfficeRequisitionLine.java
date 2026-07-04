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
package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_Locator;
import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;
import org.eevolution.model.I_PP_Order;

/** Generated Interface for C_OfficeRequisitionLine
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_C_OfficeRequisitionLine 
{

    /** TableName=C_OfficeRequisitionLine */
    public static final String Table_Name = "C_OfficeRequisitionLine";

    /** AD_Table_ID=1000142 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 1 - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(1);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name C_Charge_ID */
    public static final String COLUMNNAME_C_Charge_ID = "C_Charge_ID";

	/** Set Charge.
	  * Additional document charges
	  */
	public void setC_Charge_ID (int C_Charge_ID);

	/** Get Charge.
	  * Additional document charges
	  */
	public int getC_Charge_ID();

	public org.compiere.model.I_C_Charge getC_Charge() throws RuntimeException;

    /** Column name C_OfficeRequisitionLine_ID */
    public static final String COLUMNNAME_C_OfficeRequisitionLine_ID = "C_OfficeRequisitionLine_ID";

	/** Set C_OfficeRequisitionLine	  */
	public void setC_OfficeRequisitionLine_ID (int C_OfficeRequisitionLine_ID);

	/** Get C_OfficeRequisitionLine	  */
	public int getC_OfficeRequisitionLine_ID();

    /** Column name C_OfficeRequisitionLine_UU */
    public static final String COLUMNNAME_C_OfficeRequisitionLine_UU = "C_OfficeRequisitionLine_UU";

	/** Set C_OfficeRequisitionLine_UU	  */
	public void setC_OfficeRequisitionLine_UU (String C_OfficeRequisitionLine_UU);

	/** Get C_OfficeRequisitionLine_UU	  */
	public String getC_OfficeRequisitionLine_UU();

    /** Column name C_OfficeRequisition_ID */
    public static final String COLUMNNAME_C_OfficeRequisition_ID = "C_OfficeRequisition_ID";

	/** Set C_OfficeRequisition	  */
	public void setC_OfficeRequisition_ID (int C_OfficeRequisition_ID);

	/** Get C_OfficeRequisition	  */
	public int getC_OfficeRequisition_ID();

	public I_C_OfficeRequisition getC_OfficeRequisition() throws RuntimeException;

    /** Column name C_UOM_ID */
    public static final String COLUMNNAME_C_UOM_ID = "C_UOM_ID";

	/** Set UOM.
	  * Unit of Measure
	  */
	public void setC_UOM_ID (int C_UOM_ID);

	/** Get UOM.
	  * Unit of Measure
	  */
	public int getC_UOM_ID();

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException;

    /** Column name ClaimStatus */
    public static final String COLUMNNAME_ClaimStatus = "ClaimStatus";

	/** Set &#39046;
&#21462;
&#29366;
&#24577;
	  */
	public void setClaimStatus (String ClaimStatus);

	/** Get &#39046;
&#21462;
&#29366;
&#24577;
	  */
	public String getClaimStatus();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name CurrentCostPrice */
    public static final String COLUMNNAME_CurrentCostPrice = "CurrentCostPrice";

	/** Set Current Cost Price.
	  * The currently used cost price
	  */
	public void setCurrentCostPrice (BigDecimal CurrentCostPrice);

	/** Get Current Cost Price.
	  * The currently used cost price
	  */
	public BigDecimal getCurrentCostPrice();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name InventoryType */
    public static final String COLUMNNAME_InventoryType = "InventoryType";

	/** Set Inventory Type.
	  * Type of inventory difference
	  */
	public void setInventoryType (String InventoryType);

	/** Get Inventory Type.
	  * Type of inventory difference
	  */
	public String getInventoryType();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Line */
    public static final String COLUMNNAME_Line = "Line";

	/** Set Line No.
	  * Unique line for this document
	  */
	public void setLine (int Line);

	/** Get Line No.
	  * Unique line for this document
	  */
	public int getLine();

    /** Column name M_AttributeSetInstance_ID */
    public static final String COLUMNNAME_M_AttributeSetInstance_ID = "M_AttributeSetInstance_ID";

	/** Set Attribute Set Instance.
	  * Product Attribute Set Instance
	  */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID);

	/** Get Attribute Set Instance.
	  * Product Attribute Set Instance
	  */
	public int getM_AttributeSetInstance_ID();

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException;

    /** Column name M_Locator_ID */
    public static final String COLUMNNAME_M_Locator_ID = "M_Locator_ID";

	/** Set Locator.
	  * Warehouse Locator
	  */
	public void setM_Locator_ID (int M_Locator_ID);

	/** Get Locator.
	  * Warehouse Locator
	  */
	public int getM_Locator_ID();

	public I_M_Locator getM_Locator() throws RuntimeException;

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;

    /** Column name NewCostPrice */
    public static final String COLUMNNAME_NewCostPrice = "NewCostPrice";

	/** Set New Cost Price.
	  * New current cost price after processing of M_CostDetail
	  */
	public void setNewCostPrice (BigDecimal NewCostPrice);

	/** Get New Cost Price.
	  * New current cost price after processing of M_CostDetail
	  */
	public BigDecimal getNewCostPrice();

    /** Column name PP_Order_ID */
    public static final String COLUMNNAME_PP_Order_ID = "PP_Order_ID";

	/** Set Manufacturing Order.
	  * Manufacturing Order
	  */
	public void setPP_Order_ID (int PP_Order_ID);

	/** Get Manufacturing Order.
	  * Manufacturing Order
	  */
	public int getPP_Order_ID();

	public I_PP_Order getPP_Order() throws RuntimeException;

    /** Column name QtyBook */
    public static final String COLUMNNAME_QtyBook = "QtyBook";

	/** Set Quantity book.
	  * Book Quantity
	  */
	public void setQtyBook (BigDecimal QtyBook);

	/** Get Quantity book.
	  * Book Quantity
	  */
	public BigDecimal getQtyBook();

    /** Column name QtyCount */
    public static final String COLUMNNAME_QtyCount = "QtyCount";

	/** Set Quantity count.
	  * Counted Quantity
	  */
	public void setQtyCount (BigDecimal QtyCount);

	/** Get Quantity count.
	  * Counted Quantity
	  */
	public BigDecimal getQtyCount();

    /** Column name QtyCsv */
    public static final String COLUMNNAME_QtyCsv = "QtyCsv";

	/** Set Qty Csv	  */
	public void setQtyCsv (BigDecimal QtyCsv);

	/** Get Qty Csv	  */
	public BigDecimal getQtyCsv();

    /** Column name QtyDemand */
    public static final String COLUMNNAME_QtyDemand = "QtyDemand";

	/** Set QtyDemand	  */
	public void setQtyDemand (BigDecimal QtyDemand);

	/** Get QtyDemand	  */
	public BigDecimal getQtyDemand();

    /** Column name QtyInternalUse */
    public static final String COLUMNNAME_QtyInternalUse = "QtyInternalUse";

	/** Set Internal Use Qty.
	  * Internal Use Quantity removed from Inventory
	  */
	public void setQtyInternalUse (BigDecimal QtyInternalUse);

	/** Get Internal Use Qty.
	  * Internal Use Quantity removed from Inventory
	  */
	public BigDecimal getQtyInternalUse();

    /** Column name RSupplier */
    public static final String COLUMNNAME_RSupplier = "RSupplier";

	/** Set &#25512;
&#33616;
&#20379;
&#24212;
&#21830;
	  */
	public void setRSupplier (String RSupplier);

	/** Get &#25512;
&#33616;
&#20379;
&#24212;
&#21830;
	  */
	public String getRSupplier();

    /** Column name Specification */
    public static final String COLUMNNAME_Specification = "Specification";

	/** Set &#35268;
&#26684;
	  */
	public void setSpecification (String Specification);

	/** Get &#35268;
&#26684;
	  */
	public String getSpecification();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name quantityPicked */
    public static final String COLUMNNAME_quantityPicked = "quantityPicked";

	/** Set quantityPicked	  */
	public void setquantityPicked (BigDecimal quantityPicked);

	/** Get quantityPicked	  */
	public BigDecimal getquantityPicked();
}
