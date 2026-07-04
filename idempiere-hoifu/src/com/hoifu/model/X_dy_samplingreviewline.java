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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for dy_samplingreviewline
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_samplingreviewline")
public class X_dy_samplingreviewline extends PO implements I_dy_samplingreviewline, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260622L;

    /** Standard Constructor */
    public X_dy_samplingreviewline (Properties ctx, int dy_samplingreviewline_ID, String trxName)
    {
      super (ctx, dy_samplingreviewline_ID, trxName);
      /** if (dy_samplingreviewline_ID == 0)
        {
			setAD_User_ID (0);
			setName (null);
			setValue (null);
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setreviewstatus (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreviewline (Properties ctx, int dy_samplingreviewline_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingreviewline_ID, trxName, virtualColumns);
      /** if (dy_samplingreviewline_ID == 0)
        {
			setAD_User_ID (0);
			setName (null);
			setValue (null);
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setreviewstatus (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreviewline (Properties ctx, String dy_samplingreviewline_UU, String trxName)
    {
      super (ctx, dy_samplingreviewline_UU, trxName);
      /** if (dy_samplingreviewline_UU == null)
        {
			setAD_User_ID (0);
			setName (null);
			setValue (null);
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setreviewstatus (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreviewline (Properties ctx, String dy_samplingreviewline_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingreviewline_UU, trxName, virtualColumns);
      /** if (dy_samplingreviewline_UU == null)
        {
			setAD_User_ID (0);
			setName (null);
			setValue (null);
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setreviewstatus (null);
        } */
    }

    /** Load Constructor */
    public X_dy_samplingreviewline (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_samplingreviewline[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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
			set_Value (COLUMNNAME_AD_User_ID, null);
		else
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
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

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_ValueNoCheck (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	public I_dy_samplingreview getdy_samplingreview() throws RuntimeException
	{
		return (I_dy_samplingreview)MTable.get(getCtx(), I_dy_samplingreview.Table_ID)
			.getPO(getdy_samplingreview_ID(), get_TrxName());
	}

	/** Set dy_samplingreview.
		@param dy_samplingreview_ID dy_samplingreview
	*/
	public void setdy_samplingreview_ID (int dy_samplingreview_ID)
	{
		if (dy_samplingreview_ID < 1)
			set_Value (COLUMNNAME_dy_samplingreview_ID, null);
		else
			set_Value (COLUMNNAME_dy_samplingreview_ID, Integer.valueOf(dy_samplingreview_ID));
	}

	/** Get dy_samplingreview.
		@return dy_samplingreview	  */
	public int getdy_samplingreview_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingreview_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_samplingreviewline.
		@param dy_samplingreviewline_ID dy_samplingreviewline
	*/
	public void setdy_samplingreviewline_ID (int dy_samplingreviewline_ID)
	{
		if (dy_samplingreviewline_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingreviewline_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingreviewline_ID, Integer.valueOf(dy_samplingreviewline_ID));
	}

	/** Get dy_samplingreviewline.
		@return dy_samplingreviewline	  */
	public int getdy_samplingreviewline_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingreviewline_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_samplingreviewline_UU.
		@param dy_samplingreviewline_UU dy_samplingreviewline_UU
	*/
	public void setdy_samplingreviewline_UU (String dy_samplingreviewline_UU)
	{
		set_Value (COLUMNNAME_dy_samplingreviewline_UU, dy_samplingreviewline_UU);
	}

	/** Get dy_samplingreviewline_UU.
		@return dy_samplingreviewline_UU	  */
	public String getdy_samplingreviewline_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_samplingreviewline_UU);
	}

	public I_dy_samplingtask getdy_samplingtask() throws RuntimeException
	{
		return (I_dy_samplingtask)MTable.get(getCtx(), I_dy_samplingtask.Table_ID)
			.getPO(getdy_samplingtask_ID(), get_TrxName());
	}

	/** Set dy_samplingtask.
		@param dy_samplingtask_ID dy_samplingtask
	*/
	public void setdy_samplingtask_ID (int dy_samplingtask_ID)
	{
		if (dy_samplingtask_ID < 1)
			set_Value (COLUMNNAME_dy_samplingtask_ID, null);
		else
			set_Value (COLUMNNAME_dy_samplingtask_ID, Integer.valueOf(dy_samplingtask_ID));
	}

	/** Get dy_samplingtask.
		@return dy_samplingtask	  */
	public int getdy_samplingtask_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingtask_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set finalreviewdate.
		@param finalreviewdate finalreviewdate
	*/
	public void setfinalreviewdate (Timestamp finalreviewdate)
	{
		set_Value (COLUMNNAME_finalreviewdate, finalreviewdate);
	}

	/** Get finalreviewdate.
		@return finalreviewdate	  */
	public Timestamp getfinalreviewdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_finalreviewdate);
	}

	/** Set reviewcomment.
		@param reviewcomment reviewcomment
	*/
	public void setreviewcomment (String reviewcomment)
	{
		set_Value (COLUMNNAME_reviewcomment, reviewcomment);
	}

	/** Get reviewcomment.
		@return reviewcomment	  */
	public String getreviewcomment()
	{
		return (String)get_Value(COLUMNNAME_reviewcomment);
	}

	/** Set reviewdate.
		@param reviewdate reviewdate
	*/
	public void setreviewdate (Timestamp reviewdate)
	{
		set_Value (COLUMNNAME_reviewdate, reviewdate);
	}

	/** Get reviewdate.
		@return reviewdate	  */
	public Timestamp getreviewdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_reviewdate);
	}

	/** Set reviewremark.
		@param reviewremark reviewremark
	*/
	public void setreviewremark (String reviewremark)
	{
		set_Value (COLUMNNAME_reviewremark, reviewremark);
	}

	/** Get reviewremark.
		@return reviewremark	  */
	public String getreviewremark()
	{
		return (String)get_Value(COLUMNNAME_reviewremark);
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

	/** Set reviewscore.
		@param reviewscore reviewscore
	*/
	public void setreviewscore (BigDecimal reviewscore)
	{
		set_Value (COLUMNNAME_reviewscore, reviewscore);
	}

	/** Get reviewscore.
		@return reviewscore	  */
	public BigDecimal getreviewscore()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_reviewscore);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** &#24050;&#20851;&#38381; = CL */
	public static final String REVIEWSTATUS_已关闭 = "CL";
	/** &#24050;&#32456;&#23457; = FA */
	public static final String REVIEWSTATUS_已终审 = "FA";
	/** &#24050;&#35780;&#23457; = RD */
	public static final String REVIEWSTATUS_已评审 = "RD";
	/** &#35780;&#23457;&#20013; = RV */
	public static final String REVIEWSTATUS_评审中 = "RV";
	/** Set reviewstatus.
		@param reviewstatus reviewstatus
	*/
	public void setreviewstatus (String reviewstatus)
	{

		set_Value (COLUMNNAME_reviewstatus, reviewstatus);
	}

	/** Get reviewstatus.
		@return reviewstatus	  */
	public String getreviewstatus()
	{
		return (String)get_Value(COLUMNNAME_reviewstatus);
	}
}