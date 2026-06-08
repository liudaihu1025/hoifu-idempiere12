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

/** Generated Model for QC_AQL_Standard
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="QC_AQL_Standard")
public class X_QC_AQL_Standard extends PO implements I_QC_AQL_Standard, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260522L;

    /** Standard Constructor */
    public X_QC_AQL_Standard (Properties ctx, int QC_AQL_Standard_ID, String trxName)
    {
      super (ctx, QC_AQL_Standard_ID, trxName);
      /** if (QC_AQL_Standard_ID == 0)
        {
			setName (null);
			setQC_AQL_Standard_ID (0);
			setStandardCode (null);
        } */
    }

    /** Standard Constructor */
    public X_QC_AQL_Standard (Properties ctx, int QC_AQL_Standard_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, QC_AQL_Standard_ID, trxName, virtualColumns);
      /** if (QC_AQL_Standard_ID == 0)
        {
			setName (null);
			setQC_AQL_Standard_ID (0);
			setStandardCode (null);
        } */
    }

    /** Standard Constructor */
    public X_QC_AQL_Standard (Properties ctx, String QC_AQL_Standard_UU, String trxName)
    {
      super (ctx, QC_AQL_Standard_UU, trxName);
      /** if (QC_AQL_Standard_UU == null)
        {
			setName (null);
			setQC_AQL_Standard_ID (0);
			setStandardCode (null);
        } */
    }

    /** Standard Constructor */
    public X_QC_AQL_Standard (Properties ctx, String QC_AQL_Standard_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, QC_AQL_Standard_UU, trxName, virtualColumns);
      /** if (QC_AQL_Standard_UU == null)
        {
			setName (null);
			setQC_AQL_Standard_ID (0);
			setStandardCode (null);
        } */
    }

    /** Load Constructor */
    public X_QC_AQL_Standard (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_QC_AQL_Standard[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
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

	/** Set QC_AQL_Standard_UU.
		@param QC_AQL_Standard_UU QC_AQL_Standard_UU
	*/
	public void setQC_AQL_Standard_UU (String QC_AQL_Standard_UU)
	{
		set_ValueNoCheck (COLUMNNAME_QC_AQL_Standard_UU, QC_AQL_Standard_UU);
	}

	/** Get QC_AQL_Standard_UU.
		@return QC_AQL_Standard_UU	  */
	public String getQC_AQL_Standard_UU()
	{
		return (String)get_Value(COLUMNNAME_QC_AQL_Standard_UU);
	}

	/** Set StandardCode.
		@param StandardCode StandardCode
	*/
	public void setStandardCode (String StandardCode)
	{
		set_Value (COLUMNNAME_StandardCode, StandardCode);
	}

	/** Get StandardCode.
		@return StandardCode	  */
	public String getStandardCode()
	{
		return (String)get_Value(COLUMNNAME_StandardCode);
	}
}