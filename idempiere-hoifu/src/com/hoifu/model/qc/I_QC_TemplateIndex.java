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
 * Generated Interface for QC_TemplateIndex
 * 
 * @author iDempiere (generated)
 * @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_TemplateIndex {

	/** TableName=QC_TemplateIndex */
	public static final String Table_Name = "QC_TemplateIndex";

	/** AD_Table_ID=1000103 */
	public static final int Table_ID = MTable.getTable_ID(Table_Name);

	KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

	/**
	 * AccessLevel = 3 - Client - Org
	 */
	BigDecimal accessLevel = BigDecimal.valueOf(3);

	/** Load Meta Data */

	String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	int getAD_Client_ID();

	String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	void setAD_Org_ID(int v);

	int getAD_Org_ID();

	String COLUMNNAME_IsActive = "IsActive";

	void setIsActive(boolean v);

	boolean isActive();

	String COLUMNNAME_Created = "Created";

	Timestamp getCreated();

	String COLUMNNAME_CreatedBy = "CreatedBy";

	int getCreatedBy();

	String COLUMNNAME_Updated = "Updated";

	Timestamp getUpdated();

	String COLUMNNAME_UpdatedBy = "UpdatedBy";

	int getUpdatedBy();

	String COLUMNNAME_QC_TemplateIndex_ID = "QC_TemplateIndex_ID";

	void setQC_TemplateIndex_ID(int v);

	int getQC_TemplateIndex_ID();

	String COLUMNNAME_QC_TemplateIndex_UU = "QC_TemplateIndex_UU";

	void setQC_TemplateIndex_UU(String v);

	String getQC_TemplateIndex_UU();

	String COLUMNNAME_QC_Template_ID = "QC_Template_ID";

	void setQC_Template_ID(int v);

	int getQC_Template_ID();

	I_QC_Template getQC_Template();

	String COLUMNNAME_QC_Index_ID = "QC_Index_ID";

	void setQC_Index_ID(int v);

	int getQC_Index_ID();

	I_QC_Index getQC_Index();

	String COLUMNNAME_QCTool = "QCTool";

	void setQCTool(String v);

	String getQCTool();

	String COLUMNNAME_CheckMethod = "CheckMethod";

	void setCheckMethod(String v);

	String getCheckMethod();

	String COLUMNNAME_StanderVal = "StanderVal";

	void setStanderVal(BigDecimal v);

	BigDecimal getStanderVal();

	String COLUMNNAME_UnitOfMeasure = "UnitOfMeasure";

	void setUnitOfMeasure(String v);

	String getUnitOfMeasure();

	String COLUMNNAME_ThresholdMax = "ThresholdMax";

	void setThresholdMax(BigDecimal v);

	BigDecimal getThresholdMax();

	String COLUMNNAME_ThresholdMin = "ThresholdMin";

	void setThresholdMin(BigDecimal v);

	BigDecimal getThresholdMin();

	String COLUMNNAME_DocURL = "DocURL";

	void setDocURL(String v);

	String getDocURL();

	String COLUMNNAME_Description = "Description";

	void setDescription(String v);

	String getDescription();
}