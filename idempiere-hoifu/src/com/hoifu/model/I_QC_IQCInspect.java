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

/** Generated Interface for QC_IQCInspect
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_IQCInspect 
{

    /** TableName=QC_IQCInspect */
    public static final String Table_Name = "QC_IQCInspect";

    /** AD_Table_ID=1000210 */
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

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

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

    /** Column name M_InOut_ID */
    public static final String COLUMNNAME_M_InOut_ID = "M_InOut_ID";

	/** Set Shipment/Receipt.
	  * Material Shipment Document
	  */
	public void setM_InOut_ID (int M_InOut_ID);

	/** Get Shipment/Receipt.
	  * Material Shipment Document
	  */
	public int getM_InOut_ID();

	public org.compiere.model.I_M_InOut getM_InOut() throws RuntimeException;

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

    /** Column name QC_IQCInspect_ID */
    public static final String COLUMNNAME_QC_IQCInspect_ID = "QC_IQCInspect_ID";

	/** Set &#25910;
&#36135;
&#29289;
&#26009;
&#26816;
&#39564;
&#34920;
	  */
	public void setQC_IQCInspect_ID (int QC_IQCInspect_ID);

	/** Get &#25910;
&#36135;
&#29289;
&#26009;
&#26816;
&#39564;
&#34920;
	  */
	public int getQC_IQCInspect_ID();

    /** Column name QC_IQCInspect_UU */
    public static final String COLUMNNAME_QC_IQCInspect_UU = "QC_IQCInspect_UU";

	/** Set QC_IQCInspect_UU	  */
	public void setQC_IQCInspect_UU (String QC_IQCInspect_UU);

	/** Get QC_IQCInspect_UU	  */
	public String getQC_IQCInspect_UU();

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

    /** Column name handlemethod */
    public static final String COLUMNNAME_handlemethod = "handlemethod";

	/** Set &#22788;
&#29702;
&#26041;
&#24335;
	  */
	public void sethandlemethod (String handlemethod);

	/** Get &#22788;
&#29702;
&#26041;
&#24335;
	  */
	public String gethandlemethod();

    /** Column name inspectdate */
    public static final String COLUMNNAME_inspectdate = "inspectdate";

	/** Set inspectdate	  */
	public void setinspectdate (Timestamp inspectdate);

	/** Get inspectdate	  */
	public Timestamp getinspectdate();

    /** Column name inspector */
    public static final String COLUMNNAME_inspector = "inspector";

	/** Set inspector	  */
	public void setinspector (String inspector);

	/** Get inspector	  */
	public String getinspector();

    /** Column name inspectresult */
    public static final String COLUMNNAME_inspectresult = "inspectresult";

	/** Set &#26816;
&#39564;
&#32467;
&#26524;
	  */
	public void setinspectresult (boolean inspectresult);

	/** Get &#26816;
&#39564;
&#32467;
&#26524;
	  */
	public boolean isinspectresult();

    /** Column name inspectstatus */
    public static final String COLUMNNAME_inspectstatus = "inspectstatus";

	/** Set &#26816;
&#39564;
&#29366;
&#24577;
	  */
	public void setinspectstatus (boolean inspectstatus);

	/** Get &#26816;
&#39564;
&#29366;
&#24577;
	  */
	public boolean isinspectstatus();

    /** Column name qtyfailed */
    public static final String COLUMNNAME_qtyfailed = "qtyfailed";

	/** Set &#19981;
&#21512;
&#26684;
&#25968;
	  */
	public void setqtyfailed (BigDecimal qtyfailed);

	/** Get &#19981;
&#21512;
&#26684;
&#25968;
	  */
	public BigDecimal getqtyfailed();

    /** Column name qtypassed */
    public static final String COLUMNNAME_qtypassed = "qtypassed";

	/** Set &#21512;
&#26684;
&#25968;
	  */
	public void setqtypassed (BigDecimal qtypassed);

	/** Get &#21512;
&#26684;
&#25968;
	  */
	public BigDecimal getqtypassed();
}
