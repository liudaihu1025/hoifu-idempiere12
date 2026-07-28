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
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for yg_proofinventory
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_yg_proofinventory 
{

    /** TableName=yg_proofinventory */
    public static final String Table_Name = "yg_proofinventory";

    /** AD_Table_ID=1000167 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

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

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

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

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

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

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();

    /** Column name YG_Borrow */
    public static final String COLUMNNAME_YG_Borrow = "YG_Borrow";

	/** Set YG_Borrow	  */
	public void setYG_Borrow (String YG_Borrow);

	/** Get YG_Borrow	  */
	public String getYG_Borrow();

    /** Column name YG_Return */
    public static final String COLUMNNAME_YG_Return = "YG_Return";

	/** Set YG_Return	  */
	public void setYG_Return (String YG_Return);

	/** Get YG_Return	  */
	public String getYG_Return();

    /** Column name datein */
    public static final String COLUMNNAME_datein = "datein";

	/** Set datein	  */
	public void setdatein (Timestamp datein);

	/** Get datein	  */
	public Timestamp getdatein();

    /** Column name expirydate */
    public static final String COLUMNNAME_expirydate = "expirydate";

	/** Set expirydate	  */
	public void setexpirydate (Timestamp expirydate);

	/** Get expirydate	  */
	public Timestamp getexpirydate();

    /** Column name filelist */
    public static final String COLUMNNAME_filelist = "filelist";

	/** Set filelist	  */
	public void setfilelist (String filelist);

	/** Get filelist	  */
	public String getfilelist();

    /** Column name flowstatus */
    public static final String COLUMNNAME_flowstatus = "flowstatus";

	/** Set flowstatus	  */
	public void setflowstatus (String flowstatus);

	/** Get flowstatus	  */
	public String getflowstatus();

    /** Column name location */
    public static final String COLUMNNAME_location = "location";

	/** Set location	  */
	public void setlocation (String location);

	/** Get location	  */
	public String getlocation();

    /** Column name prooftype */
    public static final String COLUMNNAME_prooftype = "prooftype";

	/** Set prooftype	  */
	public void setprooftype (String prooftype);

	/** Get prooftype	  */
	public String getprooftype();

    /** Column name qtyin */
    public static final String COLUMNNAME_qtyin = "qtyin";

	/** Set qtyin	  */
	public void setqtyin (BigDecimal qtyin);

	/** Get qtyin	  */
	public BigDecimal getqtyin();

    /** Column name samplestatus */
    public static final String COLUMNNAME_samplestatus = "samplestatus";

	/** Set samplestatus	  */
	public void setsamplestatus (String samplestatus);

	/** Get samplestatus	  */
	public String getsamplestatus();

    /** Column name yg_proofinventory_ID */
    public static final String COLUMNNAME_yg_proofinventory_ID = "yg_proofinventory_ID";

	/** Set yg_proofinventory	  */
	public void setyg_proofinventory_ID (int yg_proofinventory_ID);

	/** Get yg_proofinventory	  */
	public int getyg_proofinventory_ID();

    /** Column name yg_proofinventory_UU */
    public static final String COLUMNNAME_yg_proofinventory_UU = "yg_proofinventory_UU";

	/** Set yg_proofinventory_UU	  */
	public void setyg_proofinventory_UU (String yg_proofinventory_UU);

	/** Get yg_proofinventory_UU	  */
	public String getyg_proofinventory_UU();
}
