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
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for HF_LengbieConfig
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_HF_LengbieConfig 
{

    /** TableName=HF_LengbieConfig */
    public static final String Table_Name = "HF_LengbieConfig";

    /** AD_Table_ID=1000136 */
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

    /** Column name CalcRatio */
    public static final String COLUMNNAME_CalcRatio = "CalcRatio";

	/** Set &#31995;
&#25968;
	  */
	public void setCalcRatio (BigDecimal CalcRatio);

	/** Get &#31995;
&#25968;
	  */
	public BigDecimal getCalcRatio();

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

    /** Column name HF_LengbieConfig_ID */
    public static final String COLUMNNAME_HF_LengbieConfig_ID = "HF_LengbieConfig_ID";

	/** Set &#26974;
&#21035;
&#21442;
&#25968;
	  */
	public void setHF_LengbieConfig_ID (int HF_LengbieConfig_ID);

	/** Get &#26974;
&#21035;
&#21442;
&#25968;
	  */
	public int getHF_LengbieConfig_ID();

    /** Column name HF_LengbieConfig_UU */
    public static final String COLUMNNAME_HF_LengbieConfig_UU = "HF_LengbieConfig_UU";

	/** Set HF_LengbieConfig_UU	  */
	public void setHF_LengbieConfig_UU (String HF_LengbieConfig_UU);

	/** Get HF_LengbieConfig_UU	  */
	public String getHF_LengbieConfig_UU();

    /** Column name HX_BoxType_ID */
    public static final String COLUMNNAME_HX_BoxType_ID = "HX_BoxType_ID";

	/** Set HX_BoxType	  */
	public void setHX_BoxType_ID (int HX_BoxType_ID);

	/** Get HX_BoxType	  */
	public int getHX_BoxType_ID();

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

    /** Column name Lengbie */
    public static final String COLUMNNAME_Lengbie = "Lengbie";

	/** Set &#26974;
&#21035;
	  */
	public void setLengbie (String Lengbie);

	/** Get &#26974;
&#21035;
	  */
	public String getLengbie();

    /** Column name NailmMouthValue */
    public static final String COLUMNNAME_NailmMouthValue = "NailmMouthValue";

	/** Set NailmMouthValue	  */
	public void setNailmMouthValue (BigDecimal NailmMouthValue);

	/** Get NailmMouthValue	  */
	public BigDecimal getNailmMouthValue();

    /** Column name PlugInterfaceValue */
    public static final String COLUMNNAME_PlugInterfaceValue = "PlugInterfaceValue";

	/** Set PlugInterfaceValue	  */
	public void setPlugInterfaceValue (BigDecimal PlugInterfaceValue);

	/** Get PlugInterfaceValue	  */
	public BigDecimal getPlugInterfaceValue();

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
}
