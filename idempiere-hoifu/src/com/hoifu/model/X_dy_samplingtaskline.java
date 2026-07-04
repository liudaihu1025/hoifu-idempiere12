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
import org.compiere.model.*;

/** Generated Model for dy_samplingtaskline
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_samplingtaskline")
public class X_dy_samplingtaskline extends PO implements I_dy_samplingtaskline, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260626L;

    /** Standard Constructor */
    public X_dy_samplingtaskline (Properties ctx, int dy_samplingtaskline_ID, String trxName)
    {
      super (ctx, dy_samplingtaskline_ID, trxName);
      /** if (dy_samplingtaskline_ID == 0)
        {
			setAD_User_ID (0);
			setLine (0);
			setdy_samplingtask_ID (0);
			setdy_samplingtaskline_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingtaskline (Properties ctx, int dy_samplingtaskline_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingtaskline_ID, trxName, virtualColumns);
      /** if (dy_samplingtaskline_ID == 0)
        {
			setAD_User_ID (0);
			setLine (0);
			setdy_samplingtask_ID (0);
			setdy_samplingtaskline_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingtaskline (Properties ctx, String dy_samplingtaskline_UU, String trxName)
    {
      super (ctx, dy_samplingtaskline_UU, trxName);
      /** if (dy_samplingtaskline_UU == null)
        {
			setAD_User_ID (0);
			setLine (0);
			setdy_samplingtask_ID (0);
			setdy_samplingtaskline_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingtaskline (Properties ctx, String dy_samplingtaskline_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingtaskline_UU, trxName, virtualColumns);
      /** if (dy_samplingtaskline_UU == null)
        {
			setAD_User_ID (0);
			setLine (0);
			setdy_samplingtask_ID (0);
			setdy_samplingtaskline_ID (0);
        } */
    }

    /** Load Constructor */
    public X_dy_samplingtaskline (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_samplingtaskline[")
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

	/** Set Line No.
		@param Line Unique line for this document
	*/
	public void setLine (int Line)
	{
		set_ValueNoCheck (COLUMNNAME_Line, Integer.valueOf(Line));
	}

	/** Get Line No.
		@return Unique line for this document
	  */
	public int getLine()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Line);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** &#24050;&#20851;&#38381; = CL */
	public static final String TASKSTATUS_已关闭 = "CL";
	/** &#24050;&#23436;&#25104; = CO */
	public static final String TASKSTATUS_已完成 = "CO";
	/** &#24050;&#32456;&#23457; = FA */
	public static final String TASKSTATUS_已终审 = "FA";
	/** &#36827;&#34892;&#20013; = IP */
	public static final String TASKSTATUS_进行中 = "IP";
	/** &#24050;&#35780;&#23457; = RD */
	public static final String TASKSTATUS_已评审 = "RD";
	/** &#35780;&#23457;&#20013; = RV */
	public static final String TASKSTATUS_评审中 = "RV";
	/** Set Task Status.
		@param TaskStatus Status of the Task
	*/
	public void setTaskStatus (String TaskStatus)
	{

		set_Value (COLUMNNAME_TaskStatus, TaskStatus);
	}

	/** Get Task Status.
		@return Status of the Task
	  */
	public String getTaskStatus()
	{
		return (String)get_Value(COLUMNNAME_TaskStatus);
	}

	/** Set actualenddate.
		@param actualenddate actualenddate
	*/
	public void setactualenddate (Timestamp actualenddate)
	{
		set_ValueNoCheck (COLUMNNAME_actualenddate, actualenddate);
	}

	/** Get actualenddate.
		@return actualenddate	  */
	public Timestamp getactualenddate()
	{
		return (Timestamp)get_Value(COLUMNNAME_actualenddate);
	}

	/** Set actualstartdate.
		@param actualstartdate actualstartdate
	*/
	public void setactualstartdate (Timestamp actualstartdate)
	{
		set_ValueNoCheck (COLUMNNAME_actualstartdate, actualstartdate);
	}

	/** Get actualstartdate.
		@return actualstartdate	  */
	public Timestamp getactualstartdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_actualstartdate);
	}

	/** Set completetask.
		@param completetask completetask
	*/
	public void setcompletetask (String completetask)
	{
		set_ValueNoCheck (COLUMNNAME_completetask, completetask);
	}

	/** Get completetask.
		@return completetask	  */
	public String getcompletetask()
	{
		return (String)get_Value(COLUMNNAME_completetask);
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
			set_ValueNoCheck (COLUMNNAME_dy_samplingtask_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingtask_ID, Integer.valueOf(dy_samplingtask_ID));
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

	/** Set dy_samplingtaskline.
		@param dy_samplingtaskline_ID dy_samplingtaskline
	*/
	public void setdy_samplingtaskline_ID (int dy_samplingtaskline_ID)
	{
		if (dy_samplingtaskline_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingtaskline_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingtaskline_ID, Integer.valueOf(dy_samplingtaskline_ID));
	}

	/** Get dy_samplingtaskline.
		@return dy_samplingtaskline	  */
	public int getdy_samplingtaskline_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingtaskline_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_samplingtaskline_UU.
		@param dy_samplingtaskline_UU dy_samplingtaskline_UU
	*/
	public void setdy_samplingtaskline_UU (String dy_samplingtaskline_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_samplingtaskline_UU, dy_samplingtaskline_UU);
	}

	/** Get dy_samplingtaskline_UU.
		@return dy_samplingtaskline_UU	  */
	public String getdy_samplingtaskline_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_samplingtaskline_UU);
	}

	/** Set plannedenddate.
		@param plannedenddate plannedenddate
	*/
	public void setplannedenddate (Timestamp plannedenddate)
	{
		set_ValueNoCheck (COLUMNNAME_plannedenddate, plannedenddate);
	}

	/** Get plannedenddate.
		@return plannedenddate	  */
	public Timestamp getplannedenddate()
	{
		return (Timestamp)get_Value(COLUMNNAME_plannedenddate);
	}

	/** Set plannedstartdate.
		@param plannedstartdate plannedstartdate
	*/
	public void setplannedstartdate (Timestamp plannedstartdate)
	{
		set_ValueNoCheck (COLUMNNAME_plannedstartdate, plannedstartdate);
	}

	/** Get plannedstartdate.
		@return plannedstartdate	  */
	public Timestamp getplannedstartdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_plannedstartdate);
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

	/** Set starttask.
		@param starttask starttask
	*/
	public void setstarttask (String starttask)
	{
		set_ValueNoCheck (COLUMNNAME_starttask, starttask);
	}

	/** Get starttask.
		@return starttask	  */
	public String getstarttask()
	{
		return (String)get_Value(COLUMNNAME_starttask);
	}

	/** Set totalhours.
		@param totalhours totalhours
	*/
	public void settotalhours (int totalhours)
	{
		set_ValueNoCheck (COLUMNNAME_totalhours, Integer.valueOf(totalhours));
	}

	/** Get totalhours.
		@return totalhours	  */
	public int gettotalhours()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_totalhours);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}