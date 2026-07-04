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

/** Generated Model for dy_samplingtask
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_samplingtask")
public class X_dy_samplingtask extends PO implements I_dy_samplingtask, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260613L;

    /** Standard Constructor */
    public X_dy_samplingtask (Properties ctx, int dy_samplingtask_ID, String trxName)
    {
      super (ctx, dy_samplingtask_ID, trxName);
      /** if (dy_samplingtask_ID == 0)
        {
			setName (null);
			setValue (null);
			setcompletetask (null);
			setdy_samplingphase_ID (0);
			setdy_samplingtask_ID (0);
			setqtydesign (Env.ZERO);
// 1
			setstarttask (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingtask (Properties ctx, int dy_samplingtask_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingtask_ID, trxName, virtualColumns);
      /** if (dy_samplingtask_ID == 0)
        {
			setName (null);
			setValue (null);
			setcompletetask (null);
			setdy_samplingphase_ID (0);
			setdy_samplingtask_ID (0);
			setqtydesign (Env.ZERO);
// 1
			setstarttask (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingtask (Properties ctx, String dy_samplingtask_UU, String trxName)
    {
      super (ctx, dy_samplingtask_UU, trxName);
      /** if (dy_samplingtask_UU == null)
        {
			setName (null);
			setValue (null);
			setcompletetask (null);
			setdy_samplingphase_ID (0);
			setdy_samplingtask_ID (0);
			setqtydesign (Env.ZERO);
// 1
			setstarttask (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingtask (Properties ctx, String dy_samplingtask_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingtask_UU, trxName, virtualColumns);
      /** if (dy_samplingtask_UU == null)
        {
			setName (null);
			setValue (null);
			setcompletetask (null);
			setdy_samplingphase_ID (0);
			setdy_samplingtask_ID (0);
			setqtydesign (Env.ZERO);
// 1
			setstarttask (null);
        } */
    }

    /** Load Constructor */
    public X_dy_samplingtask (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_samplingtask[")
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

	/** &#24050;&#23436;&#25104; = CO */
	public static final String TASKSTATUS_已完成 = "CO";
	/** &#36827;&#34892;&#20013; = IP */
	public static final String TASKSTATUS_进行中 = "IP";
	/** Set Task Status.
		@param TaskStatus Status of the Task
	*/
	public void setTaskStatus (String TaskStatus)
	{

		set_ValueNoCheck (COLUMNNAME_TaskStatus, TaskStatus);
	}

	/** Get Task Status.
		@return Status of the Task
	  */
	public String getTaskStatus()
	{
		return (String)get_Value(COLUMNNAME_TaskStatus);
	}

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
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
		set_Value (COLUMNNAME_completetask, completetask);
	}

	/** Get completetask.
		@return completetask	  */
	public String getcompletetask()
	{
		return (String)get_Value(COLUMNNAME_completetask);
	}

	public I_dy_samplingphase getdy_samplingphase() throws RuntimeException
	{
		return (I_dy_samplingphase)MTable.get(getCtx(), I_dy_samplingphase.Table_ID)
			.getPO(getdy_samplingphase_ID(), get_TrxName());
	}

	/** Set dy_samplingphase.
		@param dy_samplingphase_ID dy_samplingphase
	*/
	public void setdy_samplingphase_ID (int dy_samplingphase_ID)
	{
		if (dy_samplingphase_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingphase_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingphase_ID, Integer.valueOf(dy_samplingphase_ID));
	}

	/** Get dy_samplingphase.
		@return dy_samplingphase	  */
	public int getdy_samplingphase_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingphase_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set dy_samplingtask_UU.
		@param dy_samplingtask_UU dy_samplingtask_UU
	*/
	public void setdy_samplingtask_UU (String dy_samplingtask_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_samplingtask_UU, dy_samplingtask_UU);
	}

	/** Get dy_samplingtask_UU.
		@return dy_samplingtask_UU	  */
	public String getdy_samplingtask_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_samplingtask_UU);
	}

	/** Set plannedenddate.
		@param plannedenddate plannedenddate
	*/
	public void setplannedenddate (Timestamp plannedenddate)
	{
		set_Value (COLUMNNAME_plannedenddate, plannedenddate);
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
		set_Value (COLUMNNAME_plannedstartdate, plannedstartdate);
	}

	/** Get plannedstartdate.
		@return plannedstartdate	  */
	public Timestamp getplannedstartdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_plannedstartdate);
	}

	/** Set qtydesign.
		@param qtydesign qtydesign
	*/
	public void setqtydesign (BigDecimal qtydesign)
	{
		set_Value (COLUMNNAME_qtydesign, qtydesign);
	}

	/** Get qtydesign.
		@return qtydesign	  */
	public BigDecimal getqtydesign()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtydesign);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set starttask.
		@param starttask starttask
	*/
	public void setstarttask (String starttask)
	{
		set_Value (COLUMNNAME_starttask, starttask);
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
		set_Value (COLUMNNAME_totalhours, Integer.valueOf(totalhours));
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