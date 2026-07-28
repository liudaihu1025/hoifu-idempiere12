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

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

/** Generated Interface for dy_graphicdesigneffect
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_dy_graphicdesigneffect 
{

    /** TableName=dy_graphicdesigneffect */
    public static final String Table_Name = "dy_graphicdesigneffect";

    /** AD_Table_ID=1000182 */
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

    /** Column name avgscore */
    public static final String COLUMNNAME_avgscore = "avgscore";

	/** Set avgscore	  */
	public void setavgscore (BigDecimal avgscore);

	/** Get avgscore	  */
	public BigDecimal getavgscore();

    /** Column name dy_graphicdesign_ID */
    public static final String COLUMNNAME_dy_graphicdesign_ID = "dy_graphicdesign_ID";

	/** Set dy_graphicdesign	  */
	public void setdy_graphicdesign_ID (int dy_graphicdesign_ID);

	/** Get dy_graphicdesign	  */
	public int getdy_graphicdesign_ID();

	public I_dy_graphicdesign getdy_graphicdesign() throws RuntimeException;

    /** Column name dy_graphicdesigneffect_ID */
    public static final String COLUMNNAME_dy_graphicdesigneffect_ID = "dy_graphicdesigneffect_ID";

	/** Set dy_graphicdesigneffect	  */
	public void setdy_graphicdesigneffect_ID (int dy_graphicdesigneffect_ID);

	/** Get dy_graphicdesigneffect	  */
	public int getdy_graphicdesigneffect_ID();

    /** Column name dy_graphicdesigneffect_UU */
    public static final String COLUMNNAME_dy_graphicdesigneffect_UU = "dy_graphicdesigneffect_UU";

	/** Set dy_graphicdesigneffect_UU	  */
	public void setdy_graphicdesigneffect_UU (String dy_graphicdesigneffect_UU);

	/** Get dy_graphicdesigneffect_UU	  */
	public String getdy_graphicdesigneffect_UU();

    /** Column name dy_samplingdemand_ID */
    public static final String COLUMNNAME_dy_samplingdemand_ID = "dy_samplingdemand_ID";

	/** Set dy_samplingdemand	  */
	public void setdy_samplingdemand_ID (int dy_samplingdemand_ID);

	/** Get dy_samplingdemand	  */
	public int getdy_samplingdemand_ID();


    /** Column name effectdescription */
    public static final String COLUMNNAME_effectdescription = "effectdescription";

	/** Set effectdescription	  */
	public void seteffectdescription (String effectdescription);

	/** Get effectdescription	  */
	public String geteffectdescription();

    /** Column name effectstatus */
    public static final String COLUMNNAME_effectstatus = "effectstatus";

	/** Set effectstatus	  */
	public void seteffectstatus (String effectstatus);

	/** Get effectstatus	  */
	public String geteffectstatus();

    /** Column name reviewresult */
    public static final String COLUMNNAME_reviewresult = "reviewresult";

	/** Set reviewresult	  */
	public void setreviewresult (String reviewresult);

	/** Get reviewresult	  */
	public String getreviewresult();
}
