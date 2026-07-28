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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

/** Generated Model for dy_graphicdesigneffect
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_graphicdesigneffect")
public class X_dy_graphicdesigneffect extends PO implements I_dy_graphicdesigneffect, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260716L;

    /** Standard Constructor */
    public X_dy_graphicdesigneffect (Properties ctx, int dy_graphicdesigneffect_ID, String trxName)
    {
      super (ctx, dy_graphicdesigneffect_ID, trxName);
      /** if (dy_graphicdesigneffect_ID == 0)
        {
			setName (null);
			setdy_graphicdesign_ID (0);
			setdy_graphicdesigneffect_ID (0);
			setdy_samplingdemand_ID (0);
			seteffectstatus (null);
// DR
        } */
    }

    /** Standard Constructor */
    public X_dy_graphicdesigneffect (Properties ctx, int dy_graphicdesigneffect_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_graphicdesigneffect_ID, trxName, virtualColumns);
      /** if (dy_graphicdesigneffect_ID == 0)
        {
			setName (null);
			setdy_graphicdesign_ID (0);
			setdy_graphicdesigneffect_ID (0);
			setdy_samplingdemand_ID (0);
			seteffectstatus (null);
// DR
        } */
    }

    /** Standard Constructor */
    public X_dy_graphicdesigneffect (Properties ctx, String dy_graphicdesigneffect_UU, String trxName)
    {
      super (ctx, dy_graphicdesigneffect_UU, trxName);
      /** if (dy_graphicdesigneffect_UU == null)
        {
			setName (null);
			setdy_graphicdesign_ID (0);
			setdy_graphicdesigneffect_ID (0);
			setdy_samplingdemand_ID (0);
			seteffectstatus (null);
// DR
        } */
    }

    /** Standard Constructor */
    public X_dy_graphicdesigneffect (Properties ctx, String dy_graphicdesigneffect_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_graphicdesigneffect_UU, trxName, virtualColumns);
      /** if (dy_graphicdesigneffect_UU == null)
        {
			setName (null);
			setdy_graphicdesign_ID (0);
			setdy_graphicdesigneffect_ID (0);
			setdy_samplingdemand_ID (0);
			seteffectstatus (null);
// DR
        } */
    }

    /** Load Constructor */
    public X_dy_graphicdesigneffect (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_graphicdesigneffect[")
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

	/** Set avgscore.
		@param avgscore avgscore
	*/
	public void setavgscore (BigDecimal avgscore)
	{
		set_Value (COLUMNNAME_avgscore, avgscore);
	}

	/** Get avgscore.
		@return avgscore	  */
	public BigDecimal getavgscore()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_avgscore);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_dy_graphicdesign getdy_graphicdesign() throws RuntimeException
	{
		return (I_dy_graphicdesign)MTable.get(getCtx(), I_dy_graphicdesign.Table_ID)
			.getPO(getdy_graphicdesign_ID(), get_TrxName());
	}

	/** Set dy_graphicdesign.
		@param dy_graphicdesign_ID dy_graphicdesign
	*/
	public void setdy_graphicdesign_ID (int dy_graphicdesign_ID)
	{
		if (dy_graphicdesign_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_graphicdesign_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_graphicdesign_ID, Integer.valueOf(dy_graphicdesign_ID));
	}

	/** Get dy_graphicdesign.
		@return dy_graphicdesign	  */
	public int getdy_graphicdesign_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_graphicdesign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_graphicdesigneffect.
		@param dy_graphicdesigneffect_ID dy_graphicdesigneffect
	*/
	public void setdy_graphicdesigneffect_ID (int dy_graphicdesigneffect_ID)
	{
		if (dy_graphicdesigneffect_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_graphicdesigneffect_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_graphicdesigneffect_ID, Integer.valueOf(dy_graphicdesigneffect_ID));
	}

	/** Get dy_graphicdesigneffect.
		@return dy_graphicdesigneffect	  */
	public int getdy_graphicdesigneffect_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_graphicdesigneffect_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_graphicdesigneffect_UU.
		@param dy_graphicdesigneffect_UU dy_graphicdesigneffect_UU
	*/
	public void setdy_graphicdesigneffect_UU (String dy_graphicdesigneffect_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_graphicdesigneffect_UU, dy_graphicdesigneffect_UU);
	}

	/** Get dy_graphicdesigneffect_UU.
		@return dy_graphicdesigneffect_UU	  */
	public String getdy_graphicdesigneffect_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_graphicdesigneffect_UU);
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

	/** Set effectdescription.
		@param effectdescription effectdescription
	*/
	public void seteffectdescription (String effectdescription)
	{
		set_Value (COLUMNNAME_effectdescription, effectdescription);
	}

	/** Get effectdescription.
		@return effectdescription	  */
	public String geteffectdescription()
	{
		return (String)get_Value(COLUMNNAME_effectdescription);
	}

	/** &#33609;&#31295; = DR */
	public static final String EFFECTSTATUS_草稿 = "DR";
	/** &#24050;&#35780;&#23457; = RE */
	public static final String EFFECTSTATUS_已评审 = "RE";
	/** &#35780;&#23457;&#20013; = RV */
	public static final String EFFECTSTATUS_评审中 = "RV";
	/** &#24453;&#35780;&#23457; = WR */
	public static final String EFFECTSTATUS_待评审 = "WR";
	/** Set effectstatus.
		@param effectstatus effectstatus
	*/
	public void seteffectstatus (String effectstatus)
	{

		set_Value (COLUMNNAME_effectstatus, effectstatus);
	}

	/** Get effectstatus.
		@return effectstatus	  */
	public String geteffectstatus()
	{
		return (String)get_Value(COLUMNNAME_effectstatus);
	}

	/** &#37319;&#32435; = AC */
	public static final String REVIEWRESULT_采纳 = "AC";
	/** &#19981;&#37319;&#32435; = NA */
	public static final String REVIEWRESULT_不采纳 = "NA";
	/** Set reviewresult.
		@param reviewresult reviewresult
	*/
	public void setreviewresult (String reviewresult)
	{

		set_Value (COLUMNNAME_reviewresult, reviewresult);
	}

	/** Get reviewresult.
		@return reviewresult	  */
	public String getreviewresult()
	{
		return (String)get_Value(COLUMNNAME_reviewresult);
	}
}