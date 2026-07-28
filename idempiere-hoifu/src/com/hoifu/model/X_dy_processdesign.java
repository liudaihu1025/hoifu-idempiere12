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
package com.hoifu.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/** Generated Model for dy_processdesign
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_processdesign")
public class X_dy_processdesign extends PO implements I_dy_processdesign, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260717L;

    /** Standard Constructor */
    public X_dy_processdesign (Properties ctx, int dy_processdesign_ID, String trxName)
    {
      super (ctx, dy_processdesign_ID, trxName);
      /** if (dy_processdesign_ID == 0)
        {
			setAD_User_ID (0);
			setdy_graphicdesigneffect_ID (null);
			setdy_processdesign_ID (0);
			setdy_samplingdemand_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_dy_processdesign (Properties ctx, int dy_processdesign_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_processdesign_ID, trxName, virtualColumns);
      /** if (dy_processdesign_ID == 0)
        {
			setAD_User_ID (0);
			setdy_graphicdesigneffect_ID (null);
			setdy_processdesign_ID (0);
			setdy_samplingdemand_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_dy_processdesign (Properties ctx, String dy_processdesign_UU, String trxName)
    {
      super (ctx, dy_processdesign_UU, trxName);
      /** if (dy_processdesign_UU == null)
        {
			setAD_User_ID (0);
			setdy_graphicdesigneffect_ID (null);
			setdy_processdesign_ID (0);
			setdy_samplingdemand_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_dy_processdesign (Properties ctx, String dy_processdesign_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_processdesign_UU, trxName, virtualColumns);
      /** if (dy_processdesign_UU == null)
        {
			setAD_User_ID (0);
			setdy_graphicdesigneffect_ID (null);
			setdy_processdesign_ID (0);
			setdy_samplingdemand_ID (0);
        } */
    }

    /** Load Constructor */
    public X_dy_processdesign (Properties ctx, ResultSet rs, String trxName)
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
      POInfo poi = POInfo.getPOInfo (ctx, MTable.getTable_ID(Table_Name), get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_dy_processdesign[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set End Date.
		@param EndDate Last effective date (inclusive)
	*/
	public void setEndDate (Timestamp EndDate)
	{
		set_Value (COLUMNNAME_EndDate, EndDate);
	}

	/** Get End Date.
		@return Last effective date (inclusive)
	  */
	public Timestamp getEndDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_EndDate);
	}

	/** Set Start Date.
		@param StartDate First effective day (inclusive)
	*/
	public void setStartDate (Timestamp StartDate)
	{
		set_Value (COLUMNNAME_StartDate, StartDate);
	}

	/** Get Start Date.
		@return First effective day (inclusive)
	  */
	public Timestamp getStartDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_StartDate);
	}

	/** Set dy_graphicdesigneffect.
		@param dy_graphicdesigneffect_ID dy_graphicdesigneffect
	*/
	public void setdy_graphicdesigneffect_ID (String dy_graphicdesigneffect_ID)
	{

		set_ValueNoCheck (COLUMNNAME_dy_graphicdesigneffect_ID, dy_graphicdesigneffect_ID);
	}

	/** Get dy_graphicdesigneffect.
		@return dy_graphicdesigneffect	  */
	public String getdy_graphicdesigneffect_ID()
	{
		return (String)get_Value(COLUMNNAME_dy_graphicdesigneffect_ID);
	}

	/** Set dy_processdesign.
		@param dy_processdesign_ID dy_processdesign
	*/
	public void setdy_processdesign_ID (int dy_processdesign_ID)
	{
		if (dy_processdesign_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_processdesign_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_processdesign_ID, Integer.valueOf(dy_processdesign_ID));
	}

	/** Get dy_processdesign.
		@return dy_processdesign	  */
	public int getdy_processdesign_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_processdesign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_processdesign_UU.
		@param dy_processdesign_UU dy_processdesign_UU
	*/
	public void setdy_processdesign_UU (String dy_processdesign_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_processdesign_UU, dy_processdesign_UU);
	}

	/** Get dy_processdesign_UU.
		@return dy_processdesign_UU	  */
	public String getdy_processdesign_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_processdesign_UU);
	}


	/** Set dy_samplingdemand.
		@param dy_samplingdemand_ID dy_samplingdemand
	*/
	public void setdy_samplingdemand_ID (int dy_samplingdemand_ID)
	{
		if (dy_samplingdemand_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingdemand_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingdemand_ID, Integer.valueOf(dy_samplingdemand_ID));
	}

	/** Get dy_samplingdemand.
		@return dy_samplingdemand	  */
	public int getdy_samplingdemand_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingdemand_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getfinalreviewer() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getfinalreviewer_ID(), get_TrxName());
	}

	/** Set finalreviewer_ID.
		@param finalreviewer_ID finalreviewer_ID
	*/
	public void setfinalreviewer_ID (int finalreviewer_ID)
	{
		if (finalreviewer_ID < 1)
			set_Value (COLUMNNAME_finalreviewer_ID, null);
		else
			set_Value (COLUMNNAME_finalreviewer_ID, Integer.valueOf(finalreviewer_ID));
	}

	/** Get finalreviewer_ID.
		@return finalreviewer_ID	  */
	public int getfinalreviewer_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_finalreviewer_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** &#24050;&#23436;&#25104; = CO */
	public static final String PROCESSSTATUS_已完成 = "CO";
	/** &#33609;&#31295; = DR */
	public static final String PROCESSSTATUS_草稿 = "DR";
	/** &#35780;&#23457;&#20013; = RV */
	public static final String PROCESSSTATUS_评审中 = "RV";
	/** &#24453;&#35780;&#23457; = WR */
	public static final String PROCESSSTATUS_待评审 = "WR";
	/** Set processstatus.
		@param processstatus processstatus
	*/
	public void setprocessstatus (String processstatus)
	{

		set_Value (COLUMNNAME_processstatus, processstatus);
	}

	/** Get processstatus.
		@return processstatus	  */
	public String getprocessstatus()
	{
		return (String)get_Value(COLUMNNAME_processstatus);
	}
}