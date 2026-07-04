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
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for HF_LengbieConfig
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="HF_LengbieConfig")
public class X_HF_LengbieConfig extends PO implements I_HF_LengbieConfig, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260530L;

    /** Standard Constructor */
    public X_HF_LengbieConfig (Properties ctx, int HF_LengbieConfig_ID, String trxName)
    {
      super (ctx, HF_LengbieConfig_ID, trxName);
      /** if (HF_LengbieConfig_ID == 0)
        {
			setHF_LengbieConfig_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_LengbieConfig (Properties ctx, int HF_LengbieConfig_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, HF_LengbieConfig_ID, trxName, virtualColumns);
      /** if (HF_LengbieConfig_ID == 0)
        {
			setHF_LengbieConfig_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_LengbieConfig (Properties ctx, String HF_LengbieConfig_UU, String trxName)
    {
      super (ctx, HF_LengbieConfig_UU, trxName);
      /** if (HF_LengbieConfig_UU == null)
        {
			setHF_LengbieConfig_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_LengbieConfig (Properties ctx, String HF_LengbieConfig_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, HF_LengbieConfig_UU, trxName, virtualColumns);
      /** if (HF_LengbieConfig_UU == null)
        {
			setHF_LengbieConfig_ID (0);
        } */
    }

    /** Load Constructor */
    public X_HF_LengbieConfig (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_HF_LengbieConfig[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set &#31995;&#25968;.
		@param CalcRatio &#31995;&#25968;
	*/
	public void setCalcRatio (BigDecimal CalcRatio)
	{
		set_Value (COLUMNNAME_CalcRatio, CalcRatio);
	}

	/** Get &#31995;&#25968;.
		@return &#31995;&#25968;	  */
	public BigDecimal getCalcRatio()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CalcRatio);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set &#26974;&#21035;&#21442;&#25968;.
		@param HF_LengbieConfig_ID &#26974;&#21035;&#21442;&#25968;
	*/
	public void setHF_LengbieConfig_ID (int HF_LengbieConfig_ID)
	{
		if (HF_LengbieConfig_ID < 1)
			set_ValueNoCheck (COLUMNNAME_HF_LengbieConfig_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_HF_LengbieConfig_ID, Integer.valueOf(HF_LengbieConfig_ID));
	}

	/** Get &#26974;&#21035;&#21442;&#25968;.
		@return &#26974;&#21035;&#21442;&#25968;	  */
	public int getHF_LengbieConfig_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HF_LengbieConfig_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set HF_LengbieConfig_UU.
		@param HF_LengbieConfig_UU HF_LengbieConfig_UU
	*/
	public void setHF_LengbieConfig_UU (String HF_LengbieConfig_UU)
	{
		set_ValueNoCheck (COLUMNNAME_HF_LengbieConfig_UU, HF_LengbieConfig_UU);
	}

	/** Get HF_LengbieConfig_UU.
		@return HF_LengbieConfig_UU	  */
	public String getHF_LengbieConfig_UU()
	{
		return (String)get_Value(COLUMNNAME_HF_LengbieConfig_UU);
	}

	/** Set HX_BoxType.
		@param HX_BoxType_ID HX_BoxType
	*/
	public void setHX_BoxType_ID (int HX_BoxType_ID)
	{
		if (HX_BoxType_ID < 1)
			set_ValueNoCheck (COLUMNNAME_HX_BoxType_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_HX_BoxType_ID, Integer.valueOf(HX_BoxType_ID));
	}

	/** Get HX_BoxType.
		@return HX_BoxType	  */
	public int getHX_BoxType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HX_BoxType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** B = LB_B */
	public static final String LENGBIE_B = "LB_B";
	/** BC = LB_BC */
	public static final String LENGBIE_BC = "LB_BC";
	/** C = LB_C */
	public static final String LENGBIE_C = "LB_C";
	/** E = LB_E */
	public static final String LENGBIE_E = "LB_E";
	/** EB = LB_EB */
	public static final String LENGBIE_EB = "LB_EB";
	/** Set &#26974;&#21035;.
		@param Lengbie &#26974;&#21035;
	*/
	public void setLengbie (String Lengbie)
	{

		set_Value (COLUMNNAME_Lengbie, Lengbie);
	}

	/** Get &#26974;&#21035;.
		@return &#26974;&#21035;	  */
	public String getLengbie()
	{
		return (String)get_Value(COLUMNNAME_Lengbie);
	}

	/** Set NailmMouthValue.
		@param NailmMouthValue NailmMouthValue
	*/
	public void setNailmMouthValue (BigDecimal NailmMouthValue)
	{
		set_Value (COLUMNNAME_NailmMouthValue, NailmMouthValue);
	}

	/** Get NailmMouthValue.
		@return NailmMouthValue	  */
	public BigDecimal getNailmMouthValue()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_NailmMouthValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set PlugInterfaceValue.
		@param PlugInterfaceValue PlugInterfaceValue
	*/
	public void setPlugInterfaceValue (BigDecimal PlugInterfaceValue)
	{
		set_Value (COLUMNNAME_PlugInterfaceValue, PlugInterfaceValue);
	}

	/** Get PlugInterfaceValue.
		@return PlugInterfaceValue	  */
	public BigDecimal getPlugInterfaceValue()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PlugInterfaceValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}