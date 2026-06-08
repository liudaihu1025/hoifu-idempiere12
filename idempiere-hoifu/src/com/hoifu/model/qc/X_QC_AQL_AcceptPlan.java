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
/** Generated Model - DO NOT CHANGE */
package com.hoifu.model.qc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/**
 * Generated Model for QC_AQL_AcceptPlan
 * 
 * @author iDempiere (generated)
 * @version Release 12 - $Id$
 */
@org.adempiere.base.Model(table = "QC_AQL_AcceptPlan")
public class X_QC_AQL_AcceptPlan extends PO implements I_QC_AQL_AcceptPlan, I_Persistent {

	/**
	 *
	 */
	private static final long serialVersionUID = 20260522L;

	/** Standard Constructor */
	public X_QC_AQL_AcceptPlan(Properties ctx, int QC_AQL_AcceptPlan_ID, String trxName) {
		super(ctx, QC_AQL_AcceptPlan_ID, trxName);
		/**
		 * if (QC_AQL_AcceptPlan_ID == 0) { setQC_AQL_AcceptPlan_ID (0);
		 * setQC_AQL_Standard_ID (0); setAcceptQty (0); setAQLValue (0); setDefectType
		 * (null); setRejectQty (0); setSampleCode (null); }
		 */
	}

	/** Standard Constructor */
	public X_QC_AQL_AcceptPlan(Properties ctx, int QC_AQL_AcceptPlan_ID, String trxName, String... virtualColumns) {
		super(ctx, QC_AQL_AcceptPlan_ID, trxName, virtualColumns);
		/**
		 * if (QC_AQL_AcceptPlan_ID == 0) { setQC_AQL_AcceptPlan_ID (0);
		 * setQC_AQL_Standard_ID (0); setAcceptQty (0); setAQLValue (0); setDefectType
		 * (null); setRejectQty (0); setSampleCode (null); }
		 */
	}

	/** Standard Constructor */
	public X_QC_AQL_AcceptPlan(Properties ctx, String QC_AQL_AcceptPlan_UU, String trxName) {
		super(ctx, QC_AQL_AcceptPlan_UU, trxName);
		/**
		 * if (QC_AQL_AcceptPlan_UU == null) { setQC_AQL_AcceptPlan_ID (0);
		 * setQC_AQL_Standard_ID (0); setAcceptQty (0); setAQLValue (0); setDefectType
		 * (null); setRejectQty (0); setSampleCode (null); }
		 */
	}

	/** Standard Constructor */
	public X_QC_AQL_AcceptPlan(Properties ctx, String QC_AQL_AcceptPlan_UU, String trxName, String... virtualColumns) {
		super(ctx, QC_AQL_AcceptPlan_UU, trxName, virtualColumns);
		/**
		 * if (QC_AQL_AcceptPlan_UU == null) { setQC_AQL_AcceptPlan_ID (0);
		 * setQC_AQL_Standard_ID (0); setAcceptQty (0); setAQLValue (0); setDefectType
		 * (null); setRejectQty (0); setSampleCode (null); }
		 */
	}

	/** Load Constructor */
	public X_QC_AQL_AcceptPlan(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * AccessLevel
	 * 
	 * @return 3 - Client - Org
	 */
	protected int get_AccessLevel() {
		return accessLevel.intValue();
	}

	/** Load Meta Data */
	protected POInfo initPO(Properties ctx) {
		POInfo poi = POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
		return poi;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("X_QC_AQL_AcceptPlan[").append(get_ID()).append("]");
		return sb.toString();
	}

	/**
	 * Set QC_AQL_AcceptPlan.
	 * 
	 * @param QC_AQL_AcceptPlan_ID QC_AQL_AcceptPlan
	 */
	public void setQC_AQL_AcceptPlan_ID(int QC_AQL_AcceptPlan_ID) {
		if (QC_AQL_AcceptPlan_ID < 1)
			set_ValueNoCheck(COLUMNNAME_QC_AQL_AcceptPlan_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_AQL_AcceptPlan_ID, Integer.valueOf(QC_AQL_AcceptPlan_ID));
	}

	/**
	 * Get QC_AQL_AcceptPlan.
	 * 
	 * @return QC_AQL_AcceptPlan
	 */
	public int getQC_AQL_AcceptPlan_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_AQL_AcceptPlan_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	/**
	 * Set QC_AQL_AcceptPlan_UU.
	 * 
	 * @param QC_AQL_AcceptPlan_UU QC_AQL_AcceptPlan_UU
	 */
	public void setQC_AQL_AcceptPlan_UU(String QC_AQL_AcceptPlan_UU) {
		set_ValueNoCheck(COLUMNNAME_QC_AQL_AcceptPlan_UU, QC_AQL_AcceptPlan_UU);
	}

	/**
	 * Get QC_AQL_AcceptPlan_UU.
	 * 
	 * @return QC_AQL_AcceptPlan_UU
	 */
	public String getQC_AQL_AcceptPlan_UU() {
		return (String) get_Value(COLUMNNAME_QC_AQL_AcceptPlan_UU);
	}

	public I_QC_AQL_Standard getQC_AQL_Standard() throws RuntimeException {
		return (I_QC_AQL_Standard) MTable.get(getCtx(), I_QC_AQL_Standard.Table_ID).getPO(getQC_AQL_Standard_ID(),
				get_TrxName());
	}

	/**
	 * Set QC_AQL_Standard.
	 * 
	 * @param QC_AQL_Standard_ID QC_AQL_Standard
	 */
	public void setQC_AQL_Standard_ID(int QC_AQL_Standard_ID) {
		if (QC_AQL_Standard_ID < 1)
			set_ValueNoCheck(COLUMNNAME_QC_AQL_Standard_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_QC_AQL_Standard_ID, Integer.valueOf(QC_AQL_Standard_ID));
	}

	/**
	 * Get QC_AQL_Standard.
	 * 
	 * @return QC_AQL_Standard
	 */
	public int getQC_AQL_Standard_ID() {
		Integer ii = (Integer) get_Value(COLUMNNAME_QC_AQL_Standard_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	/**
	 * Set AcceptQty.
	 * 
	 * @param AcceptQty AcceptQty
	 */
	public void setAcceptQty(int AcceptQty) {
		set_Value(COLUMNNAME_AcceptQty, Integer.valueOf(AcceptQty));
	}

	/**
	 * Get AcceptQty.
	 * 
	 * @return AcceptQty
	 */
	public int getAcceptQty() {
		BigDecimal bd = (BigDecimal) get_Value(COLUMNNAME_AcceptQty);
		return bd == null ? 0 : bd.intValue();
	}

	@Override
	public void setAQLValue(BigDecimal v) {
		set_Value(COLUMNNAME_AQLValue, v);
	}

	@Override
	public BigDecimal getAQLValue() {
		return (BigDecimal) get_Value(COLUMNNAME_AQLValue);
	}

	/**
	 * Set DefectType.
	 * 
	 * @param DefectType DefectType
	 */
	public void setDefectType(String DefectType) {
		set_Value(COLUMNNAME_DefectType, DefectType);
	}

	/**
	 * Get DefectType.
	 * 
	 * @return DefectType
	 */
	public String getDefectType() {
		return (String) get_Value(COLUMNNAME_DefectType);
	}

	/**
	 * Set RejectQty.
	 * 
	 * @param RejectQty RejectQty
	 */
	public void setRejectQty(int RejectQty) {
		set_Value(COLUMNNAME_RejectQty, Integer.valueOf(RejectQty));
	}

	/**
	 * Get RejectQty.
	 * 
	 * @return RejectQty
	 */

	public int getRejectQty() {
		BigDecimal bd = (BigDecimal) get_Value(COLUMNNAME_RejectQty);
		return bd == null ? 0 : bd.intValue();
	}

	/**
	 * Set SampleCode.
	 * 
	 * @param SampleCode SampleCode
	 */
	public void setSampleCode(String SampleCode) {
		set_Value(COLUMNNAME_SampleCode, SampleCode);
	}

	/**
	 * Get SampleCode.
	 * 
	 * @return SampleCode
	 */
	public String getSampleCode() {
		return (String) get_Value(COLUMNNAME_SampleCode);
	}
}