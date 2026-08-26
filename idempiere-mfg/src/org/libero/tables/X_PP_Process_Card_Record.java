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
package org.libero.tables;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for PP_Process_Card_Record
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="PP_Process_Card_Record")
public class X_PP_Process_Card_Record extends PO implements I_PP_Process_Card_Record, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260814L;

    /** Standard Constructor */
    public X_PP_Process_Card_Record (Properties ctx, int PP_Process_Card_Record_ID, String trxName)
    {
      super (ctx, PP_Process_Card_Record_ID, trxName);
      /** if (PP_Process_Card_Record_ID == 0)
        {
			setAD_User_ID (0);
			setC_WorkTeam_ID (0);
			setDateFinish (new Timestamp( System.currentTimeMillis() ));
			setDateStart (new Timestamp( System.currentTimeMillis() ));
			setIsReported (false);
// N
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setPP_Process_Card_ID (0);
			setPP_Process_Card_Record_ID (0);
			setS_Resource_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_PP_Process_Card_Record (Properties ctx, int PP_Process_Card_Record_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Process_Card_Record_ID, trxName, virtualColumns);
      /** if (PP_Process_Card_Record_ID == 0)
        {
			setAD_User_ID (0);
			setC_WorkTeam_ID (0);
			setDateFinish (new Timestamp( System.currentTimeMillis() ));
			setDateStart (new Timestamp( System.currentTimeMillis() ));
			setIsReported (false);
// N
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setPP_Process_Card_ID (0);
			setPP_Process_Card_Record_ID (0);
			setS_Resource_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_PP_Process_Card_Record (Properties ctx, String PP_Process_Card_Record_UU, String trxName)
    {
      super (ctx, PP_Process_Card_Record_UU, trxName);
      /** if (PP_Process_Card_Record_UU == null)
        {
			setAD_User_ID (0);
			setC_WorkTeam_ID (0);
			setDateFinish (new Timestamp( System.currentTimeMillis() ));
			setDateStart (new Timestamp( System.currentTimeMillis() ));
			setIsReported (false);
// N
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setPP_Process_Card_ID (0);
			setPP_Process_Card_Record_ID (0);
			setS_Resource_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_PP_Process_Card_Record (Properties ctx, String PP_Process_Card_Record_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Process_Card_Record_UU, trxName, virtualColumns);
      /** if (PP_Process_Card_Record_UU == null)
        {
			setAD_User_ID (0);
			setC_WorkTeam_ID (0);
			setDateFinish (new Timestamp( System.currentTimeMillis() ));
			setDateStart (new Timestamp( System.currentTimeMillis() ));
			setIsReported (false);
// N
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setPP_Process_Card_ID (0);
			setPP_Process_Card_Record_ID (0);
			setS_Resource_ID (0);
        } */
    }

    /** Load Constructor */
    public X_PP_Process_Card_Record (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org
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
      StringBuilder sb = new StringBuilder ("X_PP_Process_Card_Record[")
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

	public void setPP_Cost_Collector_ID (int PP_Cost_Collector_ID)
	{
		if (PP_Cost_Collector_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Cost_Collector_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Cost_Collector_ID, Integer.valueOf(PP_Cost_Collector_ID));
	}


	public int getPP_Cost_Collector_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Cost_Collector_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	
	public I_C_WorkTeam getC_WorkTeam() throws RuntimeException
	{
		return (I_C_WorkTeam)MTable.get(getCtx(), I_C_WorkTeam.Table_ID)
			.getPO(getC_WorkTeam_ID(), get_TrxName());
	}

	/** Set C_WorkTeam.
		@param C_WorkTeam_ID C_WorkTeam
	*/
	public void setC_WorkTeam_ID (int C_WorkTeam_ID)
	{
		if (C_WorkTeam_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_WorkTeam_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_WorkTeam_ID, Integer.valueOf(C_WorkTeam_ID));
	}

	/** Get C_WorkTeam.
		@return C_WorkTeam	  */
	public int getC_WorkTeam_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_WorkTeam_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** &#24050;&#23436;&#25104; = Completed */
	public static final String CARDSTATUS_已完成 = "Completed";
	/** &#29983;&#20135;&#20013; = InProgress */
	public static final String CARDSTATUS_生产中 = "InProgress";
	/** Set &#27969;&#31243;&#21345;&#29366;&#24577;.
		@param CardStatus &#27969;&#31243;&#21345;&#29366;&#24577;
	*/
	public void setCardStatus (String CardStatus)
	{

		set_Value (COLUMNNAME_CardStatus, CardStatus);
	}

	/** Get &#27969;&#31243;&#21345;&#29366;&#24577;.
		@return &#27969;&#31243;&#21345;&#29366;&#24577;	  */
	public String getCardStatus()
	{
		return (String)get_Value(COLUMNNAME_CardStatus);
	}

	/** Set Finish Date.
		@param DateFinish Finish or (planned) completion date
	*/
	public void setDateFinish (Timestamp DateFinish)
	{
		set_ValueNoCheck (COLUMNNAME_DateFinish, DateFinish);
	}

	/** Get Finish Date.
		@return Finish or (planned) completion date
	  */
	public Timestamp getDateFinish()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateFinish);
	}

	/** Set Date Start.
		@param DateStart Date Start for this Order
	*/
	public void setDateStart (Timestamp DateStart)
	{
		set_ValueNoCheck (COLUMNNAME_DateStart, DateStart);
	}

	/** Get Date Start.
		@return Date Start for this Order
	  */
	public Timestamp getDateStart()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateStart);
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

	/** Set &#24050;&#25171;&#21360;.
		@param IsReported &#24050;&#25171;&#21360;
	*/
	public void setIsReported (boolean IsReported)
	{
		set_Value (COLUMNNAME_IsReported, Boolean.valueOf(IsReported));
	}

	/** Get &#24050;&#25171;&#21360;.
		@return &#24050;&#25171;&#21360;	  */
	public boolean isReported()
	{
		Object oo = get_Value(COLUMNNAME_IsReported);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Movement Quantity.
		@param MovementQty Quantity of a product moved.
	*/
	public void setMovementQty (BigDecimal MovementQty)
	{
		set_ValueNoCheck (COLUMNNAME_MovementQty, MovementQty);
	}

	/** Get Movement Quantity.
		@return Quantity of a product moved.
	  */
	public BigDecimal getMovementQty()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MovementQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_PP_Order getPP_Order() throws RuntimeException
	{
		return (I_PP_Order)MTable.get(getCtx(), I_PP_Order.Table_ID)
			.getPO(getPP_Order_ID(), get_TrxName());
	}

	/** Set Manufacturing Order.
		@param PP_Order_ID Manufacturing Order
	*/
	public void setPP_Order_ID (int PP_Order_ID)
	{
		if (PP_Order_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, Integer.valueOf(PP_Order_ID));
	}

	/** Get Manufacturing Order.
		@return Manufacturing Order
	  */
	public int getPP_Order_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_PP_Order_Node getPP_Order_Node() throws RuntimeException
	{
		return (I_PP_Order_Node)MTable.get(getCtx(), I_PP_Order_Node.Table_ID)
			.getPO(getPP_Order_Node_ID(), get_TrxName());
	}

	/** Set Manufacturing Order Activity.
		@param PP_Order_Node_ID Workflow Node (activity), step or process
	*/
	public void setPP_Order_Node_ID (int PP_Order_Node_ID)
	{
		if (PP_Order_Node_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Order_Node_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Order_Node_ID, Integer.valueOf(PP_Order_Node_ID));
	}

	/** Get Manufacturing Order Activity.
		@return Workflow Node (activity), step or process
	  */
	public int getPP_Order_Node_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_Node_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_PP_Process_Card getPP_Process_Card() throws RuntimeException
	{
		return (I_PP_Process_Card)MTable.get(getCtx(), I_PP_Process_Card.Table_ID)
			.getPO(getPP_Process_Card_ID(), get_TrxName());
	}

	/** Set &#29983;&#20135;&#27969;&#31243;&#21345;.
		@param PP_Process_Card_ID &#29983;&#20135;&#27969;&#31243;&#21345;
	*/
	public void setPP_Process_Card_ID (int PP_Process_Card_ID)
	{
		if (PP_Process_Card_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Process_Card_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Process_Card_ID, Integer.valueOf(PP_Process_Card_ID));
	}

	/** Get &#29983;&#20135;&#27969;&#31243;&#21345;.
		@return &#29983;&#20135;&#27969;&#31243;&#21345;	  */
	public int getPP_Process_Card_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Process_Card_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#27969;&#31243;&#21345;&#29983;&#20135;&#35760;&#24405;.
		@param PP_Process_Card_Record_ID &#27969;&#31243;&#21345;&#29983;&#20135;&#35760;&#24405;
	*/
	public void setPP_Process_Card_Record_ID (int PP_Process_Card_Record_ID)
	{
		if (PP_Process_Card_Record_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PP_Process_Card_Record_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PP_Process_Card_Record_ID, Integer.valueOf(PP_Process_Card_Record_ID));
	}

	/** Get &#27969;&#31243;&#21345;&#29983;&#20135;&#35760;&#24405;.
		@return &#27969;&#31243;&#21345;&#29983;&#20135;&#35760;&#24405;	  */
	public int getPP_Process_Card_Record_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Process_Card_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PP_Process_Card_Record_UU.
		@param PP_Process_Card_Record_UU PP_Process_Card_Record_UU
	*/
	public void setPP_Process_Card_Record_UU (String PP_Process_Card_Record_UU)
	{
		set_ValueNoCheck (COLUMNNAME_PP_Process_Card_Record_UU, PP_Process_Card_Record_UU);
	}

	/** Get PP_Process_Card_Record_UU.
		@return PP_Process_Card_Record_UU	  */
	public String getPP_Process_Card_Record_UU()
	{
		return (String)get_Value(COLUMNNAME_PP_Process_Card_Record_UU);
	}

	public org.compiere.model.I_S_Resource getS_Resource() throws RuntimeException
	{
		return (org.compiere.model.I_S_Resource)MTable.get(getCtx(), org.compiere.model.I_S_Resource.Table_ID)
			.getPO(getS_Resource_ID(), get_TrxName());
	}

	/** Set Resource.
		@param S_Resource_ID Resource
	*/
	public void setS_Resource_ID (int S_Resource_ID)
	{
		if (S_Resource_ID < 1)
			set_ValueNoCheck (COLUMNNAME_S_Resource_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_S_Resource_ID, Integer.valueOf(S_Resource_ID));
	}

	/** Get Resource.
		@return Resource
	  */
	public int getS_Resource_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Resource_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Scrapped Quantity.
		@param ScrappedQty The Quantity scrapped due to QA issues
	*/
	public void setScrappedQty (BigDecimal ScrappedQty)
	{
		set_Value (COLUMNNAME_ScrappedQty, ScrappedQty);
	}

	/** Get Scrapped Quantity.
		@return The Quantity scrapped due to QA issues
	  */
	public BigDecimal getScrappedQty()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ScrappedQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}