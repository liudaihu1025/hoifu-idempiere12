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
 * Generated Interface for QC_Template
 * 
 * @author iDempiere (generated)
 * @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_Template {

	/** TableName=QC_Template */
	public static final String Table_Name = "QC_Template";

	/** AD_Table_ID=1000102 */
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

	String COLUMNNAME_QC_Template_ID = "QC_Template_ID";

	void setQC_Template_ID(int v);

	int getQC_Template_ID();

	String COLUMNNAME_QC_Template_UU = "QC_Template_UU";

	void setQC_Template_UU(String v);

	String getQC_Template_UU();

	String COLUMNNAME_TemplateCode = "TemplateCode";

	void setTemplateCode(String v);

	String getTemplateCode();

	String COLUMNNAME_TemplateName = "TemplateName";

	void setTemplateName(String v);

	String getTemplateName();

	String COLUMNNAME_QCTypes = "QCTypes";

	void setQCTypes(String v);

	String getQCTypes();

	String COLUMNNAME_Description = "Description";

	void setDescription(String v);

	String getDescription();

	// ---- AQL 相关字段 ----

	String COLUMNNAME_QC_AQL_Standard_ID = "QC_AQL_Standard_ID";

	/** 关联 AQL 标准（必填） */
	void setQC_AQL_Standard_ID(int v);

	int getQC_AQL_Standard_ID();

	String COLUMNNAME_InspectionLevel = "InspectionLevel";

	/** 检验水平：I / II / III / S1 / S2 / S3 / S4（必填） */
	void setInspectionLevel(String v);

	String getInspectionLevel();

	String COLUMNNAME_AQL_CR = "AQL_CR";

	/** CR 缺陷 AQL 值，如 0.065（必填） */
	void setAQL_CR(BigDecimal v);

	BigDecimal getAQL_CR();

	String COLUMNNAME_AQL_MAJ = "AQL_MAJ";

	/** MAJ 缺陷 AQL 值，如 1.0（必填） */
	void setAQL_MAJ(BigDecimal v);

	BigDecimal getAQL_MAJ();

	String COLUMNNAME_AQL_MIN = "AQL_MIN";

	/** MIN 缺陷 AQL 值，如 4.0（必填） */
	void setAQL_MIN(BigDecimal v);

	BigDecimal getAQL_MIN();
}
