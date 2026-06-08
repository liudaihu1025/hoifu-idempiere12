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

/** Generated Interface for WM_Gate
 *  @author iDempiere (generated) 
 *  @version Release 4.1
 */
@SuppressWarnings("all")
public interface I_WM_Gate 
{

    /** TableName=WM_Gate */
    public static final String Table_Name = "WM_Gate";

    /** AD_Table_ID=1000013 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization	  */
	public int getAD_Org_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By	  */
	public int getCreatedBy();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active	  */
	public void setIsActive (boolean IsActive);

	/** Get Active	  */
	public boolean isActive();

    /** Column name IsCrossDocking */
    public static final String COLUMNNAME_IsCrossDocking = "IsCrossDocking";

	/** Set IsCrossDocking	  */
	public void setIsCrossDocking (boolean IsCrossDocking);

	/** Get IsCrossDocking	  */
	public boolean isCrossDocking();

    /** Column name IsFlowThrough */
    public static final String COLUMNNAME_IsFlowThrough = "IsFlowThrough";

	/** Set IsFlowThrough	  */
	public void setIsFlowThrough (boolean IsFlowThrough);

	/** Get IsFlowThrough	  */
	public boolean isFlowThrough();

    /** Column name IsGoodsIssue */
    public static final String COLUMNNAME_IsGoodsIssue = "IsGoodsIssue";

	/** Set IsGoodsIssue	  */
	public void setIsGoodsIssue (boolean IsGoodsIssue);

	/** Get IsGoodsIssue	  */
	public boolean isGoodsIssue();

    /** Column name IsGoodsReceipts */
    public static final String COLUMNNAME_IsGoodsReceipts = "IsGoodsReceipts";

	/** Set IsGoodsReceipts	  */
	public void setIsGoodsReceipts (boolean IsGoodsReceipts);

	/** Get IsGoodsReceipts	  */
	public boolean isGoodsReceipts();

    /** Column name M_Warehouse_ID */
    public static final String COLUMNNAME_M_Warehouse_ID = "M_Warehouse_ID";

	/** Set Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID);

	/** Get Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID();

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException;

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

    /** Column name StagingArea */
    public static final String COLUMNNAME_StagingArea = "StagingArea";

	/** Set StagingArea	  */
	public void setStagingArea (String StagingArea);

	/** Get StagingArea	  */
	public String getStagingArea();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By	  */
	public int getUpdatedBy();

    /** Column name WM_Gate_ID */
    public static final String COLUMNNAME_WM_Gate_ID = "WM_Gate_ID";

	/** Set Gate	  */
	public void setWM_Gate_ID (int WM_Gate_ID);

	/** Get Gate	  */
	public int getWM_Gate_ID();
}
