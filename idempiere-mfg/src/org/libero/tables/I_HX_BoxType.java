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

/** Generated Interface for HX_BoxType
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_HX_BoxType 
{

    /** TableName=HX_BoxType */
    public static final String Table_Name = "HX_BoxType";

    /** AD_Table_ID=1000086 */
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

    /** Column name AD_Workflow_ID */
    public static final String COLUMNNAME_AD_Workflow_ID = "AD_Workflow_ID";

	/** Set Workflow.
	  * Workflow or combination of tasks
	  */
	public void setAD_Workflow_ID (int AD_Workflow_ID);

	/** Get Workflow.
	  * Workflow or combination of tasks
	  */
	public int getAD_Workflow_ID();

	public org.compiere.model.I_AD_Workflow getAD_Workflow() throws RuntimeException;

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

    /** Column name WeightFormula */
    public static final String COLUMNNAME_WeightFormula = "WeightFormula";

	/** Set &#35745;
&#37325;
&#20844;
&#24335;
	  */
	public void setWeightFormula (String WeightFormula);

	/** Get &#35745;
&#37325;
&#20844;
&#24335;
	  */
	public String getWeightFormula();

    /** Column name ad_workflow_name */
    public static final String COLUMNNAME_ad_workflow_name = "ad_workflow_name";

	/** Set ad_workflow_name	  */
	public void setad_workflow_name (String ad_workflow_name);

	/** Get ad_workflow_name	  */
	public String getad_workflow_name();

    /** Column name ad_workflow_value */
    public static final String COLUMNNAME_ad_workflow_value = "ad_workflow_value";

	/** Set ad_workflow_value	  */
	public void setad_workflow_value (String ad_workflow_value);

	/** Get ad_workflow_value	  */
	public String getad_workflow_value();

    /** Column name areaformula */
    public static final String COLUMNNAME_areaformula = "areaformula";

	/** Set areaformula	  */
	public void setareaformula (String areaformula);

	/** Get areaformula	  */
	public String getareaformula();

    /** Column name horizontalexpandlength */
    public static final String COLUMNNAME_horizontalexpandlength = "horizontalexpandlength";

	/** Set horizontalexpandlength	  */
	public void sethorizontalexpandlength (String horizontalexpandlength);

	/** Get horizontalexpandlength	  */
	public String gethorizontalexpandlength();

    /** Column name horizontalexpandwidth */
    public static final String COLUMNNAME_horizontalexpandwidth = "horizontalexpandwidth";

	/** Set horizontalexpandwidth	  */
	public void sethorizontalexpandwidth (String horizontalexpandwidth);

	/** Get horizontalexpandwidth	  */
	public String gethorizontalexpandwidth();

    /** Column name isnailmouth */
    public static final String COLUMNNAME_isnailmouth = "isnailmouth";

	/** Set &#38025;
&#21475;
	  */
	public void setisnailmouth (boolean isnailmouth);

	/** Get &#38025;
&#21475;
	  */
	public boolean isnailmouth();

    /** Column name ispluginterface */
    public static final String COLUMNNAME_ispluginterface = "ispluginterface";

	/** Set ispluginterface	  */
	public void setispluginterface (boolean ispluginterface);

	/** Get ispluginterface	  */
	public boolean ispluginterface();

    /** Column name istwobargebox */
    public static final String COLUMNNAME_istwobargebox = "istwobargebox";

	/** Set istwobargebox	  */
	public void setistwobargebox (boolean istwobargebox);

	/** Get istwobargebox	  */
	public boolean istwobargebox();

    /** Column name paperlengthcrease1 */
    public static final String COLUMNNAME_paperlengthcrease1 = "paperlengthcrease1";

	/** Set paperlengthcrease1	  */
	public void setpaperlengthcrease1 (String paperlengthcrease1);

	/** Get paperlengthcrease1	  */
	public String getpaperlengthcrease1();

    /** Column name paperlengthcrease2 */
    public static final String COLUMNNAME_paperlengthcrease2 = "paperlengthcrease2";

	/** Set paperlengthcrease2	  */
	public void setpaperlengthcrease2 (String paperlengthcrease2);

	/** Get paperlengthcrease2	  */
	public String getpaperlengthcrease2();

    /** Column name paperlengthcrease3 */
    public static final String COLUMNNAME_paperlengthcrease3 = "paperlengthcrease3";

	/** Set paperlengthcrease3	  */
	public void setpaperlengthcrease3 (String paperlengthcrease3);

	/** Get paperlengthcrease3	  */
	public String getpaperlengthcrease3();

    /** Column name paperlengthcrease4 */
    public static final String COLUMNNAME_paperlengthcrease4 = "paperlengthcrease4";

	/** Set paperlengthcrease4	  */
	public void setpaperlengthcrease4 (String paperlengthcrease4);

	/** Get paperlengthcrease4	  */
	public String getpaperlengthcrease4();

    /** Column name paperlengthcrease5 */
    public static final String COLUMNNAME_paperlengthcrease5 = "paperlengthcrease5";

	/** Set paperlengthcrease5	  */
	public void setpaperlengthcrease5 (String paperlengthcrease5);

	/** Get paperlengthcrease5	  */
	public String getpaperlengthcrease5();

    /** Column name paperwidthcrease1 */
    public static final String COLUMNNAME_paperwidthcrease1 = "paperwidthcrease1";

	/** Set paperwidthcrease1	  */
	public void setpaperwidthcrease1 (String paperwidthcrease1);

	/** Get paperwidthcrease1	  */
	public String getpaperwidthcrease1();

    /** Column name paperwidthcrease2 */
    public static final String COLUMNNAME_paperwidthcrease2 = "paperwidthcrease2";

	/** Set paperwidthcrease2	  */
	public void setpaperwidthcrease2 (String paperwidthcrease2);

	/** Get paperwidthcrease2	  */
	public String getpaperwidthcrease2();

    /** Column name paperwidthcrease3 */
    public static final String COLUMNNAME_paperwidthcrease3 = "paperwidthcrease3";

	/** Set paperwidthcrease3	  */
	public void setpaperwidthcrease3 (String paperwidthcrease3);

	/** Get paperwidthcrease3	  */
	public String getpaperwidthcrease3();

    /** Column name paperwidthcrease4 */
    public static final String COLUMNNAME_paperwidthcrease4 = "paperwidthcrease4";

	/** Set paperwidthcrease4	  */
	public void setpaperwidthcrease4 (String paperwidthcrease4);

	/** Get paperwidthcrease4	  */
	public String getpaperwidthcrease4();

    /** Column name paperwidthcrease5 */
    public static final String COLUMNNAME_paperwidthcrease5 = "paperwidthcrease5";

	/** Set paperwidthcrease5	  */
	public void setpaperwidthcrease5 (String paperwidthcrease5);

	/** Get paperwidthcrease5	  */
	public String getpaperwidthcrease5();

    /** Column name verticalexpandlength */
    public static final String COLUMNNAME_verticalexpandlength = "verticalexpandlength";

	/** Set verticalexpandlength	  */
	public void setverticalexpandlength (String verticalexpandlength);

	/** Get verticalexpandlength	  */
	public String getverticalexpandlength();

    /** Column name verticalexpandwidth */
    public static final String COLUMNNAME_verticalexpandwidth = "verticalexpandwidth";

	/** Set verticalexpandwidth	  */
	public void setverticalexpandwidth (String verticalexpandwidth);

	/** Get verticalexpandwidth	  */
	public String getverticalexpandwidth();

    /** Column name volumeformula */
    public static final String COLUMNNAME_volumeformula = "volumeformula";

	/** Set volumeformula	  */
	public void setvolumeformula (String volumeformula);

	/** Get volumeformula	  */
	public String getvolumeformula();
}
