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
 * Generated Interface for QC_IPQC
 * 
 * @author iDempiere (generated)
 * @version Release 12
 */
@SuppressWarnings("all")
public interface I_QC_IPQC {

	/** TableName=QC_IPQC */
	public static final String Table_Name = "QC_IPQC";

	/** AD_Table_ID=1000109 */
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

	String COLUMNNAME_QC_IPQC_ID = "QC_IPQC_ID";

	void setQC_IPQC_ID(int v);

	int getQC_IPQC_ID();

	String COLUMNNAME_QC_IPQC_UU = "QC_IPQC_UU";

	void setQC_IPQC_UU(String v);

	String getQC_IPQC_UU();

	String COLUMNNAME_DocumentNo = "DocumentNo";

	void setDocumentNo(String v);

	String getDocumentNo();

	String COLUMNNAME_DocStatus = "DocStatus";

	void setDocStatus(String v);

	String getDocStatus();

	String COLUMNNAME_DocAction = "DocAction";

	void setDocAction(String v);

	String getDocAction();

	String COLUMNNAME_IPQCType = "IPQCType";

	void setIPQCType(String v);

	String getIPQCType();

	String COLUMNNAME_QC_Template_ID = "QC_Template_ID";

	void setQC_Template_ID(int v);

	int getQC_Template_ID();

	I_QC_Template getQC_Template();

	String COLUMNNAME_SourceDocID = "SourceDocID";

	void setSourceDocID(int v);

	int getSourceDocID();

	String COLUMNNAME_SourceDocTypeID = "SourceDocTypeID";

	void setSourceDocTypeID(int v);

	int getSourceDocTypeID();

	String COLUMNNAME_SourceDocCode = "SourceDocCode";

	void setSourceDocCode(String v);

	String getSourceDocCode();

	String COLUMNNAME_SourceLineID = "SourceLineID";

	void setSourceLineID(int v);

	int getSourceLineID();

	String COLUMNNAME_PP_Order_ID = "PP_Order_ID";

	void setPP_Order_ID(int v);

	int getPP_Order_ID();

	String COLUMNNAME_Task_ID = "Task_ID";

	void setTask_ID(int v);

	int getTask_ID();

	String COLUMNNAME_S_Resource_ID = "S_Resource_ID";

	void setS_Resource_ID(int v);

	int getS_Resource_ID();

	String COLUMNNAME_Process_ID = "Process_ID";

	void setProcess_ID(int v);

	int getProcess_ID();

	String COLUMNNAME_M_Product_ID = "M_Product_ID";

	void setM_Product_ID(int v);

	int getM_Product_ID();

	String COLUMNNAME_QuantityReceived = "QuantityReceived";

	void setQuantityReceived(BigDecimal v);

	BigDecimal getQuantityReceived();

	String COLUMNNAME_QuantityCheck = "QuantityCheck";

	void setQuantityCheck(BigDecimal v);

	BigDecimal getQuantityCheck();

	String COLUMNNAME_QuantityUnqualified = "QuantityUnqualified";

	void setQuantityUnqualified(BigDecimal v);

	BigDecimal getQuantityUnqualified();

	String COLUMNNAME_QuantityQualified = "QuantityQualified";

	void setQuantityQualified(BigDecimal v);

	BigDecimal getQuantityQualified();

	String COLUMNNAME_CR_Rate = "CR_Rate";

	void setCR_Rate(BigDecimal v);

	BigDecimal getCR_Rate();

	String COLUMNNAME_MAJ_Rate = "MAJ_Rate";

	void setMAJ_Rate(BigDecimal v);

	BigDecimal getMAJ_Rate();

	String COLUMNNAME_MIN_Rate = "MIN_Rate";

	void setMIN_Rate(BigDecimal v);

	BigDecimal getMIN_Rate();

	String COLUMNNAME_CR_Quantity = "CR_Quantity";

	void setCR_Quantity(BigDecimal v);

	BigDecimal getCR_Quantity();

	String COLUMNNAME_MAJ_Quantity = "MAJ_Quantity";

	void setMAJ_Quantity(BigDecimal v);

	BigDecimal getMAJ_Quantity();

	String COLUMNNAME_MIN_Quantity = "MIN_Quantity";

	void setMIN_Quantity(BigDecimal v);

	BigDecimal getMIN_Quantity();

	String COLUMNNAME_CheckResult = "CheckResult";

	void setCheckResult(String v);

	String getCheckResult();

	String COLUMNNAME_InspectDate = "InspectDate";

	void setInspectDate(Timestamp v);

	Timestamp getInspectDate();

	String COLUMNNAME_Inspector = "Inspector";

	void setInspector(String v);

	String getInspector();

	String COLUMNNAME_Description = "Description";

	void setDescription(String v);

	String getDescription();

	String COLUMNNAME_Processed = "Processed";

	void setProcessed(boolean v);

	boolean isProcessed();

	String COLUMNNAME_ProcessedOn = "ProcessedOn";

	void setProcessedOn(BigDecimal v);

	BigDecimal getProcessedOn();

	String COLUMNNAME_Processing = "Processing";

	void setProcessing(boolean v);

	boolean isProcessing();

	String COLUMNNAME_IsApproved = "IsApproved";

	void setIsApproved(boolean v);

	boolean isApproved();

	String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	void setC_DocType_ID(int v);

	int getC_DocType_ID();

	String COLUMNNAME_C_DocTypeTarget_ID = "C_DocTypeTarget_ID";

	void setC_DocTypeTarget_ID(int v);

	int getC_DocTypeTarget_ID();
}