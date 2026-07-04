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

/** Generated Model for dy_samplingreviewresult
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_samplingreviewresult")
public class X_dy_samplingreviewresult extends PO implements I_dy_samplingreviewresult, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260622L;

    /** Standard Constructor */
    public X_dy_samplingreviewresult (Properties ctx, int dy_samplingreviewresult_ID, String trxName)
    {
      super (ctx, dy_samplingreviewresult_ID, trxName);
      /** if (dy_samplingreviewresult_ID == 0)
        {
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setdy_samplingreviewresult_ID (0);
			setreviewresult (null);
			setreviewscore (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreviewresult (Properties ctx, int dy_samplingreviewresult_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingreviewresult_ID, trxName, virtualColumns);
      /** if (dy_samplingreviewresult_ID == 0)
        {
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setdy_samplingreviewresult_ID (0);
			setreviewresult (null);
			setreviewscore (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreviewresult (Properties ctx, String dy_samplingreviewresult_UU, String trxName)
    {
      super (ctx, dy_samplingreviewresult_UU, trxName);
      /** if (dy_samplingreviewresult_UU == null)
        {
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setdy_samplingreviewresult_ID (0);
			setreviewresult (null);
			setreviewscore (Env.ZERO);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreviewresult (Properties ctx, String dy_samplingreviewresult_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingreviewresult_UU, trxName, virtualColumns);
      /** if (dy_samplingreviewresult_UU == null)
        {
			setdy_samplingreview_ID (0);
			setdy_samplingreviewline_ID (0);
			setdy_samplingreviewresult_ID (0);
			setreviewresult (null);
			setreviewscore (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_dy_samplingreviewresult (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_samplingreviewresult[")
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
			set_ValueNoCheck (COLUMNNAME_dy_samplingreview_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingreview_ID, Integer.valueOf(dy_samplingreview_ID));
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

	public I_dy_samplingreviewline getdy_samplingreviewline() throws RuntimeException
	{
		return (I_dy_samplingreviewline)MTable.get(getCtx(), I_dy_samplingreviewline.Table_ID)
			.getPO(getdy_samplingreviewline_ID(), get_TrxName());
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

	/** Set dy_samplingreviewresult.
		@param dy_samplingreviewresult_ID dy_samplingreviewresult
	*/
	public void setdy_samplingreviewresult_ID (int dy_samplingreviewresult_ID)
	{
		if (dy_samplingreviewresult_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingreviewresult_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingreviewresult_ID, Integer.valueOf(dy_samplingreviewresult_ID));
	}

	/** Get dy_samplingreviewresult.
		@return dy_samplingreviewresult	  */
	public int getdy_samplingreviewresult_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingreviewresult_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_samplingreviewresult_UU.
		@param dy_samplingreviewresult_UU dy_samplingreviewresult_UU
	*/
	public void setdy_samplingreviewresult_UU (String dy_samplingreviewresult_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_samplingreviewresult_UU, dy_samplingreviewresult_UU);
	}

	/** Get dy_samplingreviewresult_UU.
		@return dy_samplingreviewresult_UU	  */
	public String getdy_samplingreviewresult_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_samplingreviewresult_UU);
	}

	/** Set finalcomment.
		@param finalcomment finalcomment
	*/
	public void setfinalcomment (String finalcomment)
	{
		set_Value (COLUMNNAME_finalcomment, finalcomment);
	}

	/** Get finalcomment.
		@return finalcomment	  */
	public String getfinalcomment()
	{
		return (String)get_Value(COLUMNNAME_finalcomment);
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
}