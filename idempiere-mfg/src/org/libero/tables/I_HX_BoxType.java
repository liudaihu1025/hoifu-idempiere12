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
package org.libero.tables;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

/** Generated Interface for HX_BoxType
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_HX_BoxType 
{

    /** TableName=HX_BoxType */
	public static final String Table_Name = "hx_boxtype";

    /** AD_Table_ID=1000101 */
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

    /** Column name HX_BoxType_ID */
    public static final String COLUMNNAME_HX_BoxType_ID = "HX_BoxType_ID";

	/** Set HX_BoxType	  */
	public void setHX_BoxType_ID (int HX_BoxType_ID);

	/** Get HX_BoxType	  */
	public int getHX_BoxType_ID();

    /** Column name HX_BoxType_UU */
    public static final String COLUMNNAME_HX_BoxType_UU = "HX_BoxType_UU";

	/** Set HX_BoxType_UU	  */
	public void setHX_BoxType_UU (String HX_BoxType_UU);

	/** Get HX_BoxType_UU	  */
	public String getHX_BoxType_UU();

    /** Column name Height */
    public static final String COLUMNNAME_Height = "Height";

	/** Set Height	  */
	public void setHeight (String Height);

	/** Get Height	  */
	public String getHeight();

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

    /** Column name IsValid */
    public static final String COLUMNNAME_IsValid = "IsValid";

	/** Set Valid.
	  * Element is valid
	  */
	public void setIsValid (boolean IsValid);

	/** Get Valid.
	  * Element is valid
	  */
	public boolean isValid();

    /** Column name Length */
    public static final String COLUMNNAME_Length = "Length";

	/** Set Length	  */
	public void setLength (String Length);

	/** Get Length	  */
	public String getLength();

    /** Column name Thickness */
    public static final String COLUMNNAME_Thickness = "Thickness";

	/** Set &#21402;
&#24230;
	  */
	public void setThickness (String Thickness);

	/** Get &#21402;
&#24230;
	  */
	public String getThickness();

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

    /** Column name Width */
    public static final String COLUMNNAME_Width = "Width";

	/** Set Width	  */
	public void setWidth (String Width);

	/** Get Width	  */
	public String getWidth();

    /** Column name isjumpdegree */
    public static final String COLUMNNAME_isjumpdegree = "isjumpdegree";

	/** Set isjumpdegree	  */
	public void setisjumpdegree (boolean isjumpdegree);

	/** Get isjumpdegree	  */
	public boolean isjumpdegree();

    /** Column name isnailmouth */
    public static final String COLUMNNAME_isnailmouth = "isnailmouth";

	/** Set isnailmouth	  */
	public void setisnailmouth (boolean isnailmouth);

	/** Get isnailmouth	  */
	public boolean isnailmouth();

    /** Column name openingmethod */
    public static final String COLUMNNAME_openingmethod = "openingmethod";

	/** Set openingmethod	  */
	public void setopeningmethod (String openingmethod);

	/** Get openingmethod	  */
	public String getopeningmethod();

	/** Column name Value */
	public static final String COLUMNNAME_Value = "Value";

	/**
	 * Set Search Key.
	 * 
	 * @param Value Search key for the record in the format required - must be
	 *              unique
	 */
	public void setValue(String Value);

	/**
	 * Get Search Key.
	 * 
	 * @return Search key for the record in the format required - must be unique
	 */
	public String getValue();

	/** Column name Name */
	public static final String COLUMNNAME_Name = "Name";

	/**
	 * Set Name.
	 * 
	 * @param Name Alphanumeric identifier of the entity
	 */
	public void setName(String Name);

	/**
	 * Get Name.
	 * 
	 * @return Alphanumeric identifier of the entity
	 */
	public String getName();
}
