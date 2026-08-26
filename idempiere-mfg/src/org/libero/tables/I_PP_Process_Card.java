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

/** Generated Interface for PP_Process_Card
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_PP_Process_Card 
{

    /** TableName=PP_Process_Card */
    public static final String Table_Name = "PP_Process_Card";

    /** AD_Table_ID=1000210 */
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

    /** Column name CardNo */
    public static final String COLUMNNAME_CardNo = "CardNo";

	/** Set &#27969;
&#31243;
&#21345;
&#21495;
	  */
	public void setCardNo (String CardNo);

	/** Get &#27969;
&#31243;
&#21345;
&#21495;
	  */
	public String getCardNo();

    /** Column name CardType */
    public static final String COLUMNNAME_CardType = "CardType";

	/** Set &#27969;
&#31243;
&#21345;
&#31867;
&#22411;
	  */
	public void setCardType (String CardType);

	/** Get &#27969;
&#31243;
&#21345;
&#31867;
&#22411;
	  */
	public String getCardType();

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

	public I_PP_Order getPP_Order() throws RuntimeException;

    /** Column name PP_Process_Card_ID */
    public static final String COLUMNNAME_PP_Process_Card_ID = "PP_Process_Card_ID";

	/** Set &#29983;
&#20135;
&#27969;
&#31243;
&#21345;
	  */
	public void setPP_Process_Card_ID (int PP_Process_Card_ID);

	/** Get &#29983;
&#20135;
&#27969;
&#31243;
&#21345;
	  */
	public int getPP_Process_Card_ID();

    /** Column name PP_Process_Card_UU */
    public static final String COLUMNNAME_PP_Process_Card_UU = "PP_Process_Card_UU";

	/** Set PP_Process_Card_UU	  */
	public void setPP_Process_Card_UU (String PP_Process_Card_UU);

	/** Get PP_Process_Card_UU	  */
	public String getPP_Process_Card_UU();

    /** Column name PrintDate */
    public static final String COLUMNNAME_PrintDate = "PrintDate";

	/** Set PrintDate	  */
	public void setPrintDate (Timestamp PrintDate);

	/** Get PrintDate	  */
	public Timestamp getPrintDate();

    /** Column name Qty */
    public static final String COLUMNNAME_Qty = "Qty";

	/** Set Quantity.
	  * Quantity
	  */
	public void setQty (BigDecimal Qty);

	/** Get Quantity.
	  * Quantity
	  */
	public BigDecimal getQty();

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
