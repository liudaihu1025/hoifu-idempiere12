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

/** Generated Model for yg_proofinventory
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="yg_proofinventory")
public class X_yg_proofinventory extends PO implements I_yg_proofinventory, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260707L;

    /** Standard Constructor */
    public X_yg_proofinventory (Properties ctx, int yg_proofinventory_ID, String trxName)
    {
      super (ctx, yg_proofinventory_ID, trxName);
      /** if (yg_proofinventory_ID == 0)
        {
			setName (null);
			setValue (null);
			setdatein (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setexpirydate (new Timestamp( System.currentTimeMillis() ));
			setflowstatus (null);
// IN
			setlocation (null);
			setprooftype (null);
			setqtyin (Env.ZERO);
// 1
			setsamplestatus (null);
// OK
			setyg_proofinventory_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_yg_proofinventory (Properties ctx, int yg_proofinventory_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, yg_proofinventory_ID, trxName, virtualColumns);
      /** if (yg_proofinventory_ID == 0)
        {
			setName (null);
			setValue (null);
			setdatein (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setexpirydate (new Timestamp( System.currentTimeMillis() ));
			setflowstatus (null);
// IN
			setlocation (null);
			setprooftype (null);
			setqtyin (Env.ZERO);
// 1
			setsamplestatus (null);
// OK
			setyg_proofinventory_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_yg_proofinventory (Properties ctx, String yg_proofinventory_UU, String trxName)
    {
      super (ctx, yg_proofinventory_UU, trxName);
      /** if (yg_proofinventory_UU == null)
        {
			setName (null);
			setValue (null);
			setdatein (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setexpirydate (new Timestamp( System.currentTimeMillis() ));
			setflowstatus (null);
// IN
			setlocation (null);
			setprooftype (null);
			setqtyin (Env.ZERO);
// 1
			setsamplestatus (null);
// OK
			setyg_proofinventory_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_yg_proofinventory (Properties ctx, String yg_proofinventory_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, yg_proofinventory_UU, trxName, virtualColumns);
      /** if (yg_proofinventory_UU == null)
        {
			setName (null);
			setValue (null);
			setdatein (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setexpirydate (new Timestamp( System.currentTimeMillis() ));
			setflowstatus (null);
// IN
			setlocation (null);
			setprooftype (null);
			setqtyin (Env.ZERO);
// 1
			setsamplestatus (null);
// OK
			setyg_proofinventory_ID (0);
        } */
    }

    /** Load Constructor */
    public X_yg_proofinventory (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_yg_proofinventory[")
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
		set_ValueNoCheck (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
	{
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_ID)
			.getPO(getM_Product_ID(), get_TrxName());
	}

	/** Set Product.
		@param M_Product_ID Product, Service, Item
	*/
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_ValueNoCheck (COLUMNNAME_Name, Name);
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
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	/** Set YG_Borrow.
		@param YG_Borrow YG_Borrow
	*/
	public void setYG_Borrow (String YG_Borrow)
	{
		set_Value (COLUMNNAME_YG_Borrow, YG_Borrow);
	}

	/** Get YG_Borrow.
		@return YG_Borrow	  */
	public String getYG_Borrow()
	{
		return (String)get_Value(COLUMNNAME_YG_Borrow);
	}

	/** Set YG_Return.
		@param YG_Return YG_Return
	*/
	public void setYG_Return (String YG_Return)
	{
		set_Value (COLUMNNAME_YG_Return, YG_Return);
	}

	/** Get YG_Return.
		@return YG_Return	  */
	public String getYG_Return()
	{
		return (String)get_Value(COLUMNNAME_YG_Return);
	}

	/** Set datein.
		@param datein datein
	*/
	public void setdatein (Timestamp datein)
	{
		set_ValueNoCheck (COLUMNNAME_datein, datein);
	}

	/** Get datein.
		@return datein	  */
	public Timestamp getdatein()
	{
		return (Timestamp)get_Value(COLUMNNAME_datein);
	}

	/** Set expirydate.
		@param expirydate expirydate
	*/
	public void setexpirydate (Timestamp expirydate)
	{
		set_ValueNoCheck (COLUMNNAME_expirydate, expirydate);
	}

	/** Get expirydate.
		@return expirydate	  */
	public Timestamp getexpirydate()
	{
		return (Timestamp)get_Value(COLUMNNAME_expirydate);
	}

	/** Set filelist.
		@param filelist filelist
	*/
	public void setfilelist (String filelist)
	{
		set_ValueNoCheck (COLUMNNAME_filelist, filelist);
	}

	/** Get filelist.
		@return filelist	  */
	public String getfilelist()
	{
		return (String)get_Value(COLUMNNAME_filelist);
	}

	/** &#22312;&#24211; = IN */
	public static final String FLOWSTATUS_在库 = "IN";
	/** &#34987;&#39046;&#29992; = OU */
	public static final String FLOWSTATUS_被领用 = "OU";
	/** Set flowstatus.
		@param flowstatus flowstatus
	*/
	public void setflowstatus (String flowstatus)
	{

		set_ValueNoCheck (COLUMNNAME_flowstatus, flowstatus);
	}

	/** Get flowstatus.
		@return flowstatus	  */
	public String getflowstatus()
	{
		return (String)get_Value(COLUMNNAME_flowstatus);
	}

	/** Set location.
		@param location location
	*/
	public void setlocation (String location)
	{
		set_ValueNoCheck (COLUMNNAME_location, location);
	}

	/** Get location.
		@return location	  */
	public String getlocation()
	{
		return (String)get_Value(COLUMNNAME_location);
	}

	/** &#23458;&#25143;&#26679; = CS */
	public static final String PROOFTYPE_客户样 = "CS";
	/** &#23450;&#21046;&#26679; = DS */
	public static final String PROOFTYPE_定制样 = "DS";
	/** &#20869;&#37096;&#26679; = NS */
	public static final String PROOFTYPE_内部样 = "NS";
	/** Set prooftype.
		@param prooftype prooftype
	*/
	public void setprooftype (String prooftype)
	{

		set_ValueNoCheck (COLUMNNAME_prooftype, prooftype);
	}

	/** Get prooftype.
		@return prooftype	  */
	public String getprooftype()
	{
		return (String)get_Value(COLUMNNAME_prooftype);
	}

	/** Set qtyin.
		@param qtyin qtyin
	*/
	public void setqtyin (BigDecimal qtyin)
	{
		set_ValueNoCheck (COLUMNNAME_qtyin, qtyin);
	}

	/** Get qtyin.
		@return qtyin	  */
	public BigDecimal getqtyin()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtyin);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** &#23436;&#22909; = A */
	public static final String SAMPLESTATUS_完好 = "A";
	/** &#19981;&#33391; = B */
	public static final String SAMPLESTATUS_不良 = "B";
	/** &#24453;&#25253;&#24223; = C */
	public static final String SAMPLESTATUS_待报废 = "C";
	/** Set samplestatus.
		@param samplestatus samplestatus
	*/
	public void setsamplestatus (String samplestatus)
	{

		set_ValueNoCheck (COLUMNNAME_samplestatus, samplestatus);
	}

	/** Get samplestatus.
		@return samplestatus	  */
	public String getsamplestatus()
	{
		return (String)get_Value(COLUMNNAME_samplestatus);
	}

	/** Set yg_proofinventory.
		@param yg_proofinventory_ID yg_proofinventory
	*/
	public void setyg_proofinventory_ID (int yg_proofinventory_ID)
	{
		if (yg_proofinventory_ID < 1)
			set_ValueNoCheck (COLUMNNAME_yg_proofinventory_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_yg_proofinventory_ID, Integer.valueOf(yg_proofinventory_ID));
	}

	/** Get yg_proofinventory.
		@return yg_proofinventory	  */
	public int getyg_proofinventory_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_yg_proofinventory_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set yg_proofinventory_UU.
		@param yg_proofinventory_UU yg_proofinventory_UU
	*/
	public void setyg_proofinventory_UU (String yg_proofinventory_UU)
	{
		set_ValueNoCheck (COLUMNNAME_yg_proofinventory_UU, yg_proofinventory_UU);
	}

	/** Get yg_proofinventory_UU.
		@return yg_proofinventory_UU	  */
	public String getyg_proofinventory_UU()
	{
		return (String)get_Value(COLUMNNAME_yg_proofinventory_UU);
	}
}