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

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for QC_AQL_SampleCode
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="QC_AQL_SampleCode")
public class X_QC_AQL_SampleCode extends PO implements I_QC_AQL_SampleCode, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260522L;

    /** Standard Constructor */
    public X_QC_AQL_SampleCode (Properties ctx, int QC_AQL_SampleCode_ID, String trxName)
    {
      super (ctx, QC_AQL_SampleCode_ID, trxName);
      /** if (QC_AQL_SampleCode_ID == 0)
        {
			setQC_AQL_SampleCode_ID (0);
			setQC_AQL_Standard_ID (0);
			setInspectionLevel (null);
			setLotSizeMax (0);
			setLotSizeMin (0);
			setSampleCode (null);
			setsamplesize (0);
        } */
    }

    /** Standard Constructor */
    public X_QC_AQL_SampleCode (Properties ctx, int QC_AQL_SampleCode_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, QC_AQL_SampleCode_ID, trxName, virtualColumns);
      /** if (QC_AQL_SampleCode_ID == 0)
        {
			setQC_AQL_SampleCode_ID (0);
			setQC_AQL_Standard_ID (0);
			setInspectionLevel (null);
			setLotSizeMax (0);
			setLotSizeMin (0);
			setSampleCode (null);
			setsamplesize (0);
        } */
    }

    /** Standard Constructor */
    public X_QC_AQL_SampleCode (Properties ctx, String QC_AQL_SampleCode_UU, String trxName)
    {
      super (ctx, QC_AQL_SampleCode_UU, trxName);
      /** if (QC_AQL_SampleCode_UU == null)
        {
			setQC_AQL_SampleCode_ID (0);
			setQC_AQL_Standard_ID (0);
			setInspectionLevel (null);
			setLotSizeMax (0);
			setLotSizeMin (0);
			setSampleCode (null);
			setsamplesize (0);
        } */
    }

    /** Standard Constructor */
    public X_QC_AQL_SampleCode (Properties ctx, String QC_AQL_SampleCode_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, QC_AQL_SampleCode_UU, trxName, virtualColumns);
      /** if (QC_AQL_SampleCode_UU == null)
        {
			setQC_AQL_SampleCode_ID (0);
			setQC_AQL_Standard_ID (0);
			setInspectionLevel (null);
			setLotSizeMax (0);
			setLotSizeMin (0);
			setSampleCode (null);
			setsamplesize (0);
        } */
    }

    /** Load Constructor */
    public X_QC_AQL_SampleCode (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_QC_AQL_SampleCode[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set QC_AQL_SampleCode.
		@param QC_AQL_SampleCode_ID QC_AQL_SampleCode
	*/
	public void setQC_AQL_SampleCode_ID (int QC_AQL_SampleCode_ID)
	{
		if (QC_AQL_SampleCode_ID < 1)
			set_ValueNoCheck (COLUMNNAME_QC_AQL_SampleCode_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_QC_AQL_SampleCode_ID, Integer.valueOf(QC_AQL_SampleCode_ID));
	}

	/** Get QC_AQL_SampleCode.
		@return QC_AQL_SampleCode	  */
	public int getQC_AQL_SampleCode_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_QC_AQL_SampleCode_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set QC_AQL_SampleCode_UU.
		@param QC_AQL_SampleCode_UU QC_AQL_SampleCode_UU
	*/
	public void setQC_AQL_SampleCode_UU (String QC_AQL_SampleCode_UU)
	{
		set_ValueNoCheck (COLUMNNAME_QC_AQL_SampleCode_UU, QC_AQL_SampleCode_UU);
	}

	/** Get QC_AQL_SampleCode_UU.
		@return QC_AQL_SampleCode_UU	  */
	public String getQC_AQL_SampleCode_UU()
	{
		return (String)get_Value(COLUMNNAME_QC_AQL_SampleCode_UU);
	}

	public I_QC_AQL_Standard getQC_AQL_Standard() throws RuntimeException
	{
		return (I_QC_AQL_Standard)MTable.get(getCtx(), I_QC_AQL_Standard.Table_ID)
			.getPO(getQC_AQL_Standard_ID(), get_TrxName());
	}

	/** Set QC_AQL_Standard.
		@param QC_AQL_Standard_ID QC_AQL_Standard
	*/
	public void setQC_AQL_Standard_ID (int QC_AQL_Standard_ID)
	{
		if (QC_AQL_Standard_ID < 1)
			set_ValueNoCheck (COLUMNNAME_QC_AQL_Standard_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_QC_AQL_Standard_ID, Integer.valueOf(QC_AQL_Standard_ID));
	}

	/** Get QC_AQL_Standard.
		@return QC_AQL_Standard	  */
	public int getQC_AQL_Standard_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_QC_AQL_Standard_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set InspectionLevel.
		@param InspectionLevel InspectionLevel
	*/
	public void setInspectionLevel (String InspectionLevel)
	{
		set_Value (COLUMNNAME_InspectionLevel, InspectionLevel);
	}

	/** Get InspectionLevel.
		@return InspectionLevel	  */
	public String getInspectionLevel()
	{
		return (String)get_Value(COLUMNNAME_InspectionLevel);
	}

	/** Set LotSizeMax.
		@param LotSizeMax LotSizeMax
	*/
	public void setLotSizeMax (int LotSizeMax)
	{
		set_Value (COLUMNNAME_LotSizeMax, Integer.valueOf(LotSizeMax));
	}

	/** Get LotSizeMax.
		@return LotSizeMax	  */
	public int getLotSizeMax()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LotSizeMax);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LotSizeMin.
		@param LotSizeMin LotSizeMin
	*/
	public void setLotSizeMin (int LotSizeMin)
	{
		set_Value (COLUMNNAME_LotSizeMin, Integer.valueOf(LotSizeMin));
	}

	/** Get LotSizeMin.
		@return LotSizeMin	  */
	public int getLotSizeMin()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LotSizeMin);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SampleCode.
		@param SampleCode SampleCode
	*/
	public void setSampleCode (String SampleCode)
	{
		set_Value (COLUMNNAME_SampleCode, SampleCode);
	}

	/** Get SampleCode.
		@return SampleCode	  */
	public String getSampleCode()
	{
		return (String)get_Value(COLUMNNAME_SampleCode);
	}

	/** Set samplesize.
		@param samplesize samplesize
	*/
	public void setSampleSize (int samplesize)
	{
		set_Value (COLUMNNAME_SampleSize, Integer.valueOf(samplesize));
	}

	/** Get samplesize.
		@return samplesize	  */
	public int getSampleSize()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SampleSize);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}