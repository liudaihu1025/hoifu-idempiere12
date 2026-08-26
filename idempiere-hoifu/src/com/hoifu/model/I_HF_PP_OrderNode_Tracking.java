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

/** Generated Interface for HF_PP_OrderNode_Tracking
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_HF_PP_OrderNode_Tracking 
{

    /** TableName=HF_PP_OrderNode_Tracking */
    public static final String Table_Name = "HF_PP_OrderNode_Tracking";

    /** AD_Table_ID=1000201 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 4 - System 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(4);

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

    /** Column name HF_PP_OrderNode_Tracking_ID */
    public static final String COLUMNNAME_HF_PP_OrderNode_Tracking_ID = "HF_PP_OrderNode_Tracking_ID";

	/** Set HF_PP_OrderNode_Tracking	  */
	public void setHF_PP_OrderNode_Tracking_ID (int HF_PP_OrderNode_Tracking_ID);

	/** Get HF_PP_OrderNode_Tracking	  */
	public int getHF_PP_OrderNode_Tracking_ID();

    /** Column name HF_PP_OrderNode_Tracking_UU */
    public static final String COLUMNNAME_HF_PP_OrderNode_Tracking_UU = "HF_PP_OrderNode_Tracking_UU";

	/** Set HF_PP_OrderNode_Tracking_UU	  */
	public void setHF_PP_OrderNode_Tracking_UU (String HF_PP_OrderNode_Tracking_UU);

	/** Get HF_PP_OrderNode_Tracking_UU	  */
	public String getHF_PP_OrderNode_Tracking_UU();

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


    /** Column name PP_Order_Node_ID */
    public static final String COLUMNNAME_PP_Order_Node_ID = "PP_Order_Node_ID";

	/** Set Manufacturing Order Activity.
	  * Workflow Node (activity), step or process
	  */
	public void setPP_Order_Node_ID (int PP_Order_Node_ID);

	/** Get Manufacturing Order Activity.
	  * Workflow Node (activity), step or process
	  */
	public int getPP_Order_Node_ID();


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

    /** Column name colorsequence */
    public static final String COLUMNNAME_colorsequence = "colorsequence";

	/** Set colorsequence	  */
	public void setcolorsequence (String colorsequence);

	/** Get colorsequence	  */
	public String getcolorsequence();

    /** Column name dieplateno */
    public static final String COLUMNNAME_dieplateno = "dieplateno";

	/** Set dieplateno	  */
	public void setdieplateno (String dieplateno);

	/** Get dieplateno	  */
	public String getdieplateno();

    /** Column name effectname */
    public static final String COLUMNNAME_effectname = "effectname";

	/** Set effectname	  */
	public void seteffectname (String effectname);

	/** Get effectname	  */
	public String geteffectname();

    /** Column name embossplateno */
    public static final String COLUMNNAME_embossplateno = "embossplateno";

	/** Set embossplateno	  */
	public void setembossplateno (String embossplateno);

	/** Get embossplateno	  */
	public String getembossplateno();

    /** Column name embossposition */
    public static final String COLUMNNAME_embossposition = "embossposition";

	/** Set embossposition	  */
	public void setembossposition (String embossposition);

	/** Get embossposition	  */
	public String getembossposition();

    /** Column name foilalumodel */
    public static final String COLUMNNAME_foilalumodel = "foilalumodel";

	/** Set foilalumodel	  */
	public void setfoilalumodel (String foilalumodel);

	/** Get foilalumodel	  */
	public String getfoilalumodel();

    /** Column name foilplateno */
    public static final String COLUMNNAME_foilplateno = "foilplateno";

	/** Set foilplateno	  */
	public void setfoilplateno (String foilplateno);

	/** Get foilplateno	  */
	public String getfoilplateno();

    /** Column name foiltemperature */
    public static final String COLUMNNAME_foiltemperature = "foiltemperature";

	/** Set foiltemperature	  */
	public void setfoiltemperature (String foiltemperature);

	/** Get foiltemperature	  */
	public String getfoiltemperature();

    /** Column name gravurelinecount */
    public static final String COLUMNNAME_gravurelinecount = "gravurelinecount";

	/** Set gravurelinecount	  */
	public void setgravurelinecount (String gravurelinecount);

	/** Get gravurelinecount	  */
	public String getgravurelinecount();

    /** Column name inkformula */
    public static final String COLUMNNAME_inkformula = "inkformula";

	/** Set inkformula	  */
	public void setinkformula (String inkformula);

	/** Get inkformula	  */
	public String getinkformula();

    /** Column name inknamemodel */
    public static final String COLUMNNAME_inknamemodel = "inknamemodel";

	/** Set inknamemodel	  */
	public void setinknamemodel (String inknamemodel);

	/** Get inknamemodel	  */
	public String getinknamemodel();

    /** Column name inkviscosity */
    public static final String COLUMNNAME_inkviscosity = "inkviscosity";

	/** Set inkviscosity	  */
	public void setinkviscosity (String inkviscosity);

	/** Get inkviscosity	  */
	public String getinkviscosity();

    /** Column name lightgroup */
    public static final String COLUMNNAME_lightgroup = "lightgroup";

	/** Set lightgroup	  */
	public void setlightgroup (String lightgroup);

	/** Get lightgroup	  */
	public String getlightgroup();

    /** Column name oilplatematerial */
    public static final String COLUMNNAME_oilplatematerial = "oilplatematerial";

	/** Set oilplatematerial	  */
	public void setoilplatematerial (String oilplatematerial);

	/** Get oilplatematerial	  */
	public String getoilplatematerial();

    /** Column name operationclass_ID */
    public static final String COLUMNNAME_operationclass_ID = "operationclass_ID";

	/** Set &#24037;
&#24207;
&#32452;
	  */
	public void setoperationclass_ID (int operationclass_ID);

	/** Get &#24037;
&#24207;
&#32452;
	  */
	public int getoperationclass_ID();

    /** Column name otherparam */
    public static final String COLUMNNAME_otherparam = "otherparam";

	/** Set otherparam	  */
	public void setotherparam (String otherparam);

	/** Get otherparam	  */
	public String getotherparam();

    /** Column name oventemperature */
    public static final String COLUMNNAME_oventemperature = "oventemperature";

	/** Set oventemperature	  */
	public void setoventemperature (String oventemperature);

	/** Get oventemperature	  */
	public String getoventemperature();

    /** Column name qrcodesize */
    public static final String COLUMNNAME_qrcodesize = "qrcodesize";

	/** Set qrcodesize	  */
	public void setqrcodesize (String qrcodesize);

	/** Get qrcodesize	  */
	public String getqrcodesize();

    /** Column name screenlinecount */
    public static final String COLUMNNAME_screenlinecount = "screenlinecount";

	/** Set screenlinecount	  */
	public void setscreenlinecount (String screenlinecount);

	/** Get screenlinecount	  */
	public String getscreenlinecount();

    /** Column name screenmesh */
    public static final String COLUMNNAME_screenmesh = "screenmesh";

	/** Set screenmesh	  */
	public void setscreenmesh (String screenmesh);

	/** Get screenmesh	  */
	public String getscreenmesh();

    /** Column name verifycodefont */
    public static final String COLUMNNAME_verifycodefont = "verifycodefont";

	/** Set verifycodefont	  */
	public void setverifycodefont (String verifycodefont);

	/** Get verifycodefont	  */
	public String getverifycodefont();
}
