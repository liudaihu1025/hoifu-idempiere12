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

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

/**
 * Generated Interface for QC_Index
 * 
 * @author iDempiere (generated)
 * @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_Index {

	/** TableName=QC_Index */
	public static final String Table_Name = "QC_Index";

	/** AD_Table_ID=1000101 */
	public static final int Table_ID = MTable.getTable_ID(Table_Name);

	KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

	/**
	 * AccessLevel = 3 - Client - Org
	 */
	BigDecimal accessLevel = BigDecimal.valueOf(3);

	String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	int getAD_Client_ID();

	String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	void setAD_Org_ID(int AD_Org_ID);

	int getAD_Org_ID();

	String COLUMNNAME_IsActive = "IsActive";

	void setIsActive(boolean IsActive);

	boolean isActive();

	String COLUMNNAME_Created = "Created";

	Timestamp getCreated();

	String COLUMNNAME_CreatedBy = "CreatedBy";

	int getCreatedBy();

	String COLUMNNAME_Updated = "Updated";

	Timestamp getUpdated();

	String COLUMNNAME_UpdatedBy = "UpdatedBy";

	int getUpdatedBy();

	String COLUMNNAME_QC_Index_ID = "QC_Index_ID";

	void setQC_Index_ID(int QC_Index_ID);

	int getQC_Index_ID();

	String COLUMNNAME_QC_Index_UU = "QC_Index_UU";

	void setQC_Index_UU(String QC_Index_UU);

	String getQC_Index_UU();

	String COLUMNNAME_IndexCode = "IndexCode";

	void setIndexCode(String IndexCode);

	String getIndexCode();

	String COLUMNNAME_IndexName = "IndexName";

	void setIndexName(String IndexName);

	String getIndexName();

	String COLUMNNAME_IndexType = "IndexType";

	void setIndexType(String IndexType);

	String getIndexType();

	String COLUMNNAME_IndexValueType = "IndexValueType";

	void setIndexValueType(String IndexValueType);

	String getIndexValueType();

	String COLUMNNAME_QCTool = "QCTool";

	void setQCTool(String QCTool);

	String getQCTool();

	String COLUMNNAME_Description = "Description";

	void setDescription(String Description);

	String getDescription();
}
