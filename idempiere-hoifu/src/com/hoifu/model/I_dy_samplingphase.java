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

/** Generated Interface for dy_samplingphase
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_dy_samplingphase 
{

    /** TableName=dy_samplingphase */
    public static final String Table_Name = "dy_samplingphase";

    /** AD_Table_ID=1000149 */
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

    /** Column name actualenddate */
    public static final String COLUMNNAME_actualenddate = "actualenddate";

	/** Set actualenddate	  */
	public void setactualenddate (Timestamp actualenddate);

	/** Get actualenddate	  */
	public Timestamp getactualenddate();

    /** Column name actualstartdate */
    public static final String COLUMNNAME_actualstartdate = "actualstartdate";

	/** Set actualstartdate	  */
	public void setactualstartdate (Timestamp actualstartdate);

	/** Get actualstartdate	  */
	public Timestamp getactualstartdate();

    /** Column name dy_samplingphase_ID */
    public static final String COLUMNNAME_dy_samplingphase_ID = "dy_samplingphase_ID";

	/** Set dy_samplingphase	  */
	public void setdy_samplingphase_ID (int dy_samplingphase_ID);

	/** Get dy_samplingphase	  */
	public int getdy_samplingphase_ID();

    /** Column name dy_samplingphase_UU */
    public static final String COLUMNNAME_dy_samplingphase_UU = "dy_samplingphase_UU";

	/** Set dy_samplingphase_UU	  */
	public void setdy_samplingphase_UU (String dy_samplingphase_UU);

	/** Get dy_samplingphase_UU	  */
	public String getdy_samplingphase_UU();

    /** Column name dy_samplingrequest_ID */
    public static final String COLUMNNAME_dy_samplingrequest_ID = "dy_samplingrequest_ID";

	/** Set dy_samplingrequest	  */
	public void setdy_samplingrequest_ID (int dy_samplingrequest_ID);

	/** Get dy_samplingrequest	  */
	public int getdy_samplingrequest_ID();

	public I_dy_samplingrequest getdy_samplingrequest() throws RuntimeException;

    /** Column name phasestatus */
    public static final String COLUMNNAME_phasestatus = "phasestatus";

	/** Set phasestatus	  */
	public void setphasestatus (String phasestatus);

	/** Get phasestatus	  */
	public String getphasestatus();

    /** Column name phasetype */
    public static final String COLUMNNAME_phasetype = "phasetype";

	/** Set phasetype	  */
	public void setphasetype (String phasetype);

	/** Get phasetype	  */
	public String getphasetype();

    /** Column name plannedenddate */
    public static final String COLUMNNAME_plannedenddate = "plannedenddate";

	/** Set plannedenddate	  */
	public void setplannedenddate (Timestamp plannedenddate);

	/** Get plannedenddate	  */
	public Timestamp getplannedenddate();

    /** Column name plannedstartdate */
    public static final String COLUMNNAME_plannedstartdate = "plannedstartdate";

	/** Set plannedstartdate	  */
	public void setplannedstartdate (Timestamp plannedstartdate);

	/** Get plannedstartdate	  */
	public Timestamp getplannedstartdate();

    /** Column name qtydesign */
    public static final String COLUMNNAME_qtydesign = "qtydesign";

	/** Set qtydesign	  */
	public void setqtydesign (BigDecimal qtydesign);

	/** Get qtydesign	  */
	public BigDecimal getqtydesign();

    /** Column name totalhours */
    public static final String COLUMNNAME_totalhours = "totalhours";

	/** Set totalhours	  */
	public void settotalhours (int totalhours);

	/** Get totalhours	  */
	public int gettotalhours();
}
