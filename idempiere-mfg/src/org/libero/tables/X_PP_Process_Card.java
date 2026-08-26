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

/** Generated Model for PP_Process_Card
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="PP_Process_Card")
public class X_PP_Process_Card extends PO implements I_PP_Process_Card, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260814L;

    /** Standard Constructor */
    public X_PP_Process_Card (Properties ctx, int PP_Process_Card_ID, String trxName)
    {
      super (ctx, PP_Process_Card_ID, trxName);
      /** if (PP_Process_Card_ID == 0)
        {
			setAD_User_ID (0);
			setCardNo (null);
			setPP_Order_ID (0);
			setPP_Process_Card_ID (0);
			setPrintDate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
        } */
    }

    /** Standard Constructor */
    public X_PP_Process_Card (Properties ctx, int PP_Process_Card_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Process_Card_ID, trxName, virtualColumns);
      /** if (PP_Process_Card_ID == 0)
        {
			setAD_User_ID (0);
			setCardNo (null);
			setPP_Order_ID (0);
			setPP_Process_Card_ID (0);
			setPrintDate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
        } */
    }

    /** Standard Constructor */
    public X_PP_Process_Card (Properties ctx, String PP_Process_Card_UU, String trxName)
    {
      super (ctx, PP_Process_Card_UU, trxName);
      /** if (PP_Process_Card_UU == null)
        {
			setAD_User_ID (0);
			setCardNo (null);
			setPP_Order_ID (0);
			setPP_Process_Card_ID (0);
			setPrintDate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
        } */
    }

    /** Standard Constructor */
    public X_PP_Process_Card (Properties ctx, String PP_Process_Card_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, PP_Process_Card_UU, trxName, virtualColumns);
      /** if (PP_Process_Card_UU == null)
        {
			setAD_User_ID (0);
			setCardNo (null);
			setPP_Order_ID (0);
			setPP_Process_Card_ID (0);
			setPrintDate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
        } */
    }

    /** Load Constructor */
    public X_PP_Process_Card (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_PP_Process_Card[")
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

	/** Set &#27969;&#31243;&#21345;&#21495;.
		@param CardNo &#27969;&#31243;&#21345;&#21495;
	*/
	public void setCardNo (String CardNo)
	{
		set_Value (COLUMNNAME_CardNo, CardNo);
	}

	/** Get &#27969;&#31243;&#21345;&#21495;.
		@return &#27969;&#31243;&#21345;&#21495;	  */
	public String getCardNo()
	{
		return (String)get_Value(COLUMNNAME_CardNo);
	}

	/** &#27491;&#21697; = QUALIFIED */
	public static final String CARDTYPE_正品 = "QUALIFIED";
	/** &#27425;&#21697; = UNQUALIFIED */
	public static final String CARDTYPE_次品 = "UNQUALIFIED";
	/** Set &#27969;&#31243;&#21345;&#31867;&#22411;.
		@param CardType &#27969;&#31243;&#21345;&#31867;&#22411;
	*/
	public void setCardType (String CardType)
	{

		set_Value (COLUMNNAME_CardType, CardType);
	}

	/** Get &#27969;&#31243;&#21345;&#31867;&#22411;.
		@return &#27969;&#31243;&#21345;&#31867;&#22411;	  */
	public String getCardType()
	{
		return (String)get_Value(COLUMNNAME_CardType);
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

	/** Set PP_Process_Card_UU.
		@param PP_Process_Card_UU PP_Process_Card_UU
	*/
	public void setPP_Process_Card_UU (String PP_Process_Card_UU)
	{
		set_ValueNoCheck (COLUMNNAME_PP_Process_Card_UU, PP_Process_Card_UU);
	}

	/** Get PP_Process_Card_UU.
		@return PP_Process_Card_UU	  */
	public String getPP_Process_Card_UU()
	{
		return (String)get_Value(COLUMNNAME_PP_Process_Card_UU);
	}

	/** Set PrintDate.
		@param PrintDate PrintDate
	*/
	public void setPrintDate (Timestamp PrintDate)
	{
		set_Value (COLUMNNAME_PrintDate, PrintDate);
	}

	/** Get PrintDate.
		@return PrintDate	  */
	public Timestamp getPrintDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_PrintDate);
	}

	/** Set Quantity.
		@param Qty Quantity
	*/
	public void setQty (BigDecimal Qty)
	{
		set_Value (COLUMNNAME_Qty, Qty);
	}

	/** Get Quantity.
		@return Quantity
	  */
	public BigDecimal getQty()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Qty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}