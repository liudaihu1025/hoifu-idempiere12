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

/** Generated Interface for yg_proofborr
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_yg_proofborr 
{

    /** TableName=yg_proofborr */
    public static final String Table_Name = "yg_proofborr";

    /** AD_Table_ID=1000169 */
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

    /** Column name Borrower_ID */
    public static final String COLUMNNAME_Borrower_ID = "Borrower_ID";

	/** Set Borrower_ID	  */
	public void setBorrower_ID (int Borrower_ID);

	/** Get Borrower_ID	  */
	public int getBorrower_ID();

	public org.compiere.model.I_AD_User getBorrower() throws RuntimeException;

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

    /** Column name DueDate */
    public static final String COLUMNNAME_DueDate = "DueDate";

	/** Set Due Date.
	  * Date when the payment is due
	  */
	public void setDueDate (Timestamp DueDate);

	/** Get Due Date.
	  * Date when the payment is due
	  */
	public Timestamp getDueDate();

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

    /** Column name Returner_ID */
    public static final String COLUMNNAME_Returner_ID = "Returner_ID";

	/** Set Returner_ID	  */
	public void setReturner_ID (int Returner_ID);

	/** Get Returner_ID	  */
	public int getReturner_ID();

	public org.compiere.model.I_AD_User getReturner() throws RuntimeException;

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

    /** Column name borrowdate */
    public static final String COLUMNNAME_borrowdate = "borrowdate";

	/** Set borrowdate	  */
	public void setborrowdate (Timestamp borrowdate);

	/** Get borrowdate	  */
	public Timestamp getborrowdate();

    /** Column name borrowstatus */
    public static final String COLUMNNAME_borrowstatus = "borrowstatus";

	/** Set borrowstatus	  */
	public void setborrowstatus (String borrowstatus);

	/** Get borrowstatus	  */
	public String getborrowstatus();

    /** Column name borrowtype */
    public static final String COLUMNNAME_borrowtype = "borrowtype";

	/** Set borrowtype	  */
	public void setborrowtype (String borrowtype);

	/** Get borrowtype	  */
	public String getborrowtype();

    /** Column name isreturned */
    public static final String COLUMNNAME_isreturned = "isreturned";

	/** Set isreturned	  */
	public void setisreturned (boolean isreturned);

	/** Get isreturned	  */
	public boolean isreturned();

    /** Column name processstep */
    public static final String COLUMNNAME_processstep = "processstep";

	/** Set processstep	  */
	public void setprocessstep (int processstep);

	/** Get processstep	  */
	public int getprocessstep();


    /** Column name qtyborrowed */
    public static final String COLUMNNAME_qtyborrowed = "qtyborrowed";

	/** Set qtyborrowed	  */
	public void setqtyborrowed (BigDecimal qtyborrowed);

	/** Get qtyborrowed	  */
	public BigDecimal getqtyborrowed();

    /** Column name qtyreturned */
    public static final String COLUMNNAME_qtyreturned = "qtyreturned";

	/** Set qtyreturned	  */
	public void setqtyreturned (BigDecimal qtyreturned);

	/** Get qtyreturned	  */
	public BigDecimal getqtyreturned();

    /** Column name returndate */
    public static final String COLUMNNAME_returndate = "returndate";

	/** Set returndate	  */
	public void setreturndate (Timestamp returndate);

	/** Get returndate	  */
	public Timestamp getreturndate();

    /** Column name returnsamplestatus */
    public static final String COLUMNNAME_returnsamplestatus = "returnsamplestatus";

	/** Set returnsamplestatus	  */
	public void setreturnsamplestatus (String returnsamplestatus);

	/** Get returnsamplestatus	  */
	public String getreturnsamplestatus();

    /** Column name workorder */
    public static final String COLUMNNAME_workorder = "workorder";

	/** Set workorder	  */
	public void setworkorder (String workorder);

	/** Get workorder	  */
	public String getworkorder();

    /** Column name yg_proofborr_ID */
    public static final String COLUMNNAME_yg_proofborr_ID = "yg_proofborr_ID";

	/** Set yg_proofborr	  */
	public void setyg_proofborr_ID (int yg_proofborr_ID);

	/** Get yg_proofborr	  */
	public int getyg_proofborr_ID();

    /** Column name yg_proofborr_UU */
    public static final String COLUMNNAME_yg_proofborr_UU = "yg_proofborr_UU";

	/** Set yg_proofborr_UU	  */
	public void setyg_proofborr_UU (String yg_proofborr_UU);

	/** Get yg_proofborr_UU	  */
	public String getyg_proofborr_UU();

    /** Column name yg_proofinventory_ID */
    public static final String COLUMNNAME_yg_proofinventory_ID = "yg_proofinventory_ID";

	/** Set yg_proofinventory	  */
	public void setyg_proofinventory_ID (int yg_proofinventory_ID);

	/** Get yg_proofinventory	  */
	public int getyg_proofinventory_ID();

	public I_yg_proofinventory getyg_proofinventory() throws RuntimeException;
}
