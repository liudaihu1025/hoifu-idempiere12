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
 * Generated Interface for QC_IQCLine
 * 
 * @author iDempiere (generated)
 * @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_IQCLine {

	/** TableName=QC_IQCLine */
	public static final String Table_Name = "QC_IQCLine";

	/** AD_Table_ID=1000107 */
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

	String COLUMNNAME_QC_IQCLine_ID = "QC_IQCLine_ID";

	void setQC_IQCLine_ID(int v);

	int getQC_IQCLine_ID();

	String COLUMNNAME_QC_IQCLine_UU = "QC_IQCLine_UU";

	void setQC_IQCLine_UU(String v);

	String getQC_IQCLine_UU();

	String COLUMNNAME_QC_IQC_ID = "QC_IQC_ID";

	void setQC_IQC_ID(int v);

	int getQC_IQC_ID();

	I_QC_IQC getQC_IQC();

	String COLUMNNAME_QC_Index_ID = "QC_Index_ID";

	void setQC_Index_ID(int v);

	int getQC_Index_ID();

	I_QC_Index getQC_Index();

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

	String COLUMNNAME_MeasuredValMin = "MeasuredValMin";

	void setMeasuredValMin(BigDecimal v);

	BigDecimal getMeasuredValMin();

	String COLUMNNAME_MeasuredValMax = "MeasuredValMax";

	void setMeasuredValMax(BigDecimal v);

	BigDecimal getMeasuredValMax();

	String COLUMNNAME_MeasuredValAvg = "MeasuredValAvg";

	void setMeasuredValAvg(BigDecimal v);

	BigDecimal getMeasuredValAvg();

	String COLUMNNAME_IsQualified = "IsQualified";

	void setIsQualified(boolean v);

	boolean isQualified();

	String COLUMNNAME_CheckComment = "CheckComment";

	void setCheckComment(String v);

	String getCheckComment();

	String COLUMNNAME_CR_Quantity = "CR_Quantity";

	void setCR_Quantity(BigDecimal v);

	BigDecimal getCR_Quantity();

	String COLUMNNAME_MAJ_Quantity = "MAJ_Quantity";

	void setMAJ_Quantity(BigDecimal v);

	BigDecimal getMAJ_Quantity();

	String COLUMNNAME_MIN_Quantity = "MIN_Quantity";

	void setMIN_Quantity(BigDecimal v);

	BigDecimal getMIN_Quantity();

	String COLUMNNAME_Description = "Description";

	void setDescription(String v);

	String getDescription();
}