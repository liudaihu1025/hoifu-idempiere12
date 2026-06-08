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
package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for QC_AQL_AcceptPlan
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_AQL_AcceptPlan 
{

    /** TableName=QC_AQL_AcceptPlan */
    public static final String Table_Name = "QC_AQL_AcceptPlan";

    /** AD_Table_ID=1000123 */
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

    /** Column name QC_AQL_AcceptPlan_ID */
    public static final String COLUMNNAME_QC_AQL_AcceptPlan_ID = "QC_AQL_AcceptPlan_ID";

	/** Set QC_AQL_AcceptPlan	  */
	public void setQC_AQL_AcceptPlan_ID (int QC_AQL_AcceptPlan_ID);

	/** Get QC_AQL_AcceptPlan	  */
	public int getQC_AQL_AcceptPlan_ID();

    /** Column name QC_AQL_AcceptPlan_UU */
    public static final String COLUMNNAME_QC_AQL_AcceptPlan_UU = "QC_AQL_AcceptPlan_UU";

	/** Set QC_AQL_AcceptPlan_UU	  */
	public void setQC_AQL_AcceptPlan_UU (String QC_AQL_AcceptPlan_UU);

	/** Get QC_AQL_AcceptPlan_UU	  */
	public String getQC_AQL_AcceptPlan_UU();

    /** Column name QC_AQL_Standard_ID */
    public static final String COLUMNNAME_QC_AQL_Standard_ID = "QC_AQL_Standard_ID";

	/** Set QC_AQL_Standard	  */
	public void setQC_AQL_Standard_ID (int QC_AQL_Standard_ID);

	/** Get QC_AQL_Standard	  */
	public int getQC_AQL_Standard_ID();

	public I_QC_AQL_Standard getQC_AQL_Standard() throws RuntimeException;

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

    /** Column name AcceptQty */
    public static final String COLUMNNAME_AcceptQty = "AcceptQty";

	/** Set AcceptQty	  */
	public void setAcceptQty (int AcceptQty);

	/** Get AcceptQty	  */
	public int getAcceptQty();

    /** Column name AQLValue */
    public static final String COLUMNNAME_AQLValue = "AQLValue";

	/** Set AQLValue	  */
	public void setAQLValue (BigDecimal AQLValue);

	/** Get AQLValue	  */
	public BigDecimal getAQLValue();

    /** Column name DefectType */
    public static final String COLUMNNAME_DefectType = "DefectType";

	/** Set DefectType	  */
	public void setDefectType (String DefectType);

	/** Get DefectType	  */
	public String getDefectType();

    /** Column name RejectQty */
    public static final String COLUMNNAME_RejectQty = "RejectQty";

	/** Set RejectQty	  */
	public void setRejectQty (int RejectQty);

	/** Get RejectQty	  */
	public int getRejectQty();

    /** Column name SampleCode */
    public static final String COLUMNNAME_SampleCode = "SampleCode";

	/** Set SampleCode	  */
	public void setSampleCode (String SampleCode);

	/** Get SampleCode	  */
	public String getSampleCode();
}
