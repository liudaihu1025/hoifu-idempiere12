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
package org.wms.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for WM_InOutLine
 *  @author iDempiere (generated) 
 *  @version Release 6.2
 */
@SuppressWarnings("all")
public interface I_WM_InOutLine 
{

    /** TableName=WM_InOutLine */
    public static final String Table_Name = "WM_InOutLine";

    /** AD_Table_ID=1000035 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name C_DocType_ID */
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	/** Set Document Type.
	  * Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID);

	/** Get Document Type.
	  * Document type or rules
	  */
	public int getC_DocType_ID();

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException;

    /** Column name C_OrderLine_ID */
    public static final String COLUMNNAME_C_OrderLine_ID = "C_OrderLine_ID";

	/** Set Sales Order Line.
	  * Sales Order Line
	  */
	public void setC_OrderLine_ID (int C_OrderLine_ID);

	/** Get Sales Order Line.
	  * Sales Order Line
	  */
	public int getC_OrderLine_ID();

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException;

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

    /** Column name IsPacked */
    public static final String COLUMNNAME_IsPacked = "IsPacked";

	/** Set IsPacked	  */
	public void setIsPacked (boolean IsPacked);

	/** Get IsPacked	  */
	public boolean isPacked();

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

    /** Column name M_InOutLine_ID */
    public static final String COLUMNNAME_M_InOutLine_ID = "M_InOutLine_ID";

	/** Set Shipment/Receipt Line.
	  * Line on Shipment or Receipt document
	  */
	public void setM_InOutLine_ID (int M_InOutLine_ID);

	/** Get Shipment/Receipt Line.
	  * Line on Shipment or Receipt document
	  */
	public int getM_InOutLine_ID();

	public org.compiere.model.I_M_InOutLine getM_InOutLine() throws RuntimeException;

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

    /** Column name M_LocatorOld_ID */
    public static final String COLUMNNAME_M_LocatorOld_ID = "M_LocatorOld_ID";

	/** Set M_LocatorOld_ID	  */
	public void setM_LocatorOld_ID (int M_LocatorOld_ID);

	/** Get M_LocatorOld_ID	  */
	public int getM_LocatorOld_ID();

	public I_M_Locator getM_LocatorOld() throws RuntimeException;

    /** Column name M_MovementLine_ID */
    public static final String COLUMNNAME_M_MovementLine_ID = "M_MovementLine_ID";

	/** Set Move Line.
	  * Inventory Move document Line
	  */
	public void setM_MovementLine_ID (int M_MovementLine_ID);

	/** Get Move Line.
	  * Inventory Move document Line
	  */
	public int getM_MovementLine_ID();

	public org.compiere.model.I_M_MovementLine getM_MovementLine() throws RuntimeException;

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

    /** Column name QtyPicked */
    public static final String COLUMNNAME_QtyPicked = "QtyPicked";

	/** Set QtyPicked	  */
	public void setQtyPicked (BigDecimal QtyPicked);

	/** Get QtyPicked	  */
	public BigDecimal getQtyPicked();

    /** Column name Sequence */
    public static final String COLUMNNAME_Sequence = "Sequence";

	/** Set Sequence	  */
	public void setSequence (BigDecimal Sequence);

	/** Get Sequence	  */
	public BigDecimal getSequence();

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

    /** Column name WM_DeliveryScheduleLine_ID */
    public static final String COLUMNNAME_WM_DeliveryScheduleLine_ID = "WM_DeliveryScheduleLine_ID";

	/** Set DeliveryScheduleLine	  */
	public void setWM_DeliveryScheduleLine_ID (int WM_DeliveryScheduleLine_ID);

	/** Get DeliveryScheduleLine	  */
	public int getWM_DeliveryScheduleLine_ID();

	public I_WM_DeliveryScheduleLine getWM_DeliveryScheduleLine() throws RuntimeException;

    /** Column name WM_HandlingUnit_ID */
    public static final String COLUMNNAME_WM_HandlingUnit_ID = "WM_HandlingUnit_ID";

	/** Set Handling Unit	  */
	public void setWM_HandlingUnit_ID (int WM_HandlingUnit_ID);

	/** Get Handling Unit	  */
	public int getWM_HandlingUnit_ID();

	public I_WM_HandlingUnit getWM_HandlingUnit() throws RuntimeException;

    /** Column name WM_HandlingUnitOld_ID */
    public static final String COLUMNNAME_WM_HandlingUnitOld_ID = "WM_HandlingUnitOld_ID";

	/** Set WM_HandlingUnitOld_ID	  */
	public void setWM_HandlingUnitOld_ID (int WM_HandlingUnitOld_ID);

	/** Get WM_HandlingUnitOld_ID	  */
	public int getWM_HandlingUnitOld_ID();

	public I_WM_HandlingUnit getWM_HandlingUnitOld() throws RuntimeException;

    /** Column name WM_InOut_ID */
    public static final String COLUMNNAME_WM_InOut_ID = "WM_InOut_ID";

	/** Set InOut	  */
	public void setWM_InOut_ID (int WM_InOut_ID);

	/** Get InOut	  */
	public int getWM_InOut_ID();

	public I_WM_InOut getWM_InOut() throws RuntimeException;

    /** Column name WM_InOutLine_ID */
    public static final String COLUMNNAME_WM_InOutLine_ID = "WM_InOutLine_ID";

	/** Set WM_InOutLine_ID	  */
	public void setWM_InOutLine_ID (int WM_InOutLine_ID);

	/** Get WM_InOutLine_ID	  */
	public int getWM_InOutLine_ID();

    /** Column name WM_InOutLine_UU */
    public static final String COLUMNNAME_WM_InOutLine_UU = "WM_InOutLine_UU";

	/** Set WM_InOutLine_UU	  */
	public void setWM_InOutLine_UU (String WM_InOutLine_UU);

	/** Get WM_InOutLine_UU	  */
	public String getWM_InOutLine_UU();
}
