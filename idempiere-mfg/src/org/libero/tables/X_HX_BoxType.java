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

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/** Generated Model for HX_BoxType
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
public class X_HX_BoxType extends PO implements I_HX_BoxType, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260424L;

    /** Standard Constructor */
    public X_HX_BoxType (Properties ctx, int HX_BoxType_ID, String trxName)
    {
      super (ctx, HX_BoxType_ID, trxName);
      /** if (HX_BoxType_ID == 0)
        {
			setHX_BoxType_ID (0);
			setIsValid (false);
// N
			setisjumpdegree (false);
// N
			setisnailmouth (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_HX_BoxType (Properties ctx, int HX_BoxType_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, HX_BoxType_ID, trxName, virtualColumns);
      /** if (HX_BoxType_ID == 0)
        {
			setHX_BoxType_ID (0);
			setIsValid (false);
// N
			setisjumpdegree (false);
// N
			setisnailmouth (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_HX_BoxType (Properties ctx, String HX_BoxType_UU, String trxName)
    {
      super (ctx, HX_BoxType_UU, trxName);
      /** if (HX_BoxType_UU == null)
        {
			setHX_BoxType_ID (0);
			setIsValid (false);
// N
			setisjumpdegree (false);
// N
			setisnailmouth (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_HX_BoxType (Properties ctx, String HX_BoxType_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, HX_BoxType_UU, trxName, virtualColumns);
      /** if (HX_BoxType_UU == null)
        {
			setHX_BoxType_ID (0);
			setIsValid (false);
// N
			setisjumpdegree (false);
// N
			setisnailmouth (false);
// N
        } */
    }

    /** Load Constructor */
    public X_HX_BoxType (Properties ctx, ResultSet rs, String trxName)
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
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_HX_BoxType[")
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

	/** Set HX_BoxType_UU.
		@param HX_BoxType_UU HX_BoxType_UU
	*/
	public void setHX_BoxType_UU (String HX_BoxType_UU)
	{
		set_ValueNoCheck (COLUMNNAME_HX_BoxType_UU, HX_BoxType_UU);
	}

	/** Get HX_BoxType_UU.
		@return HX_BoxType_UU	  */
	public String getHX_BoxType_UU()
	{
		return (String)get_Value(COLUMNNAME_HX_BoxType_UU);
	}

	/** Set Height.
		@param Height Height
	*/
	public void setHeight (String Height)
	{
		set_Value (COLUMNNAME_Height, Height);
	}

	/** Get Height.
		@return Height	  */
	public String getHeight()
	{
		return (String)get_Value(COLUMNNAME_Height);
	}

	/** Set Valid.
		@param IsValid Element is valid
	*/
	public void setIsValid (boolean IsValid)
	{
		set_Value (COLUMNNAME_IsValid, Boolean.valueOf(IsValid));
	}

	/** Get Valid.
		@return Element is valid
	  */
	public boolean isValid()
	{
		Object oo = get_Value(COLUMNNAME_IsValid);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Length.
		@param Length Length
	*/
	public void setLength (String Length)
	{
		set_Value (COLUMNNAME_Length, Length);
	}

	/** Get Length.
		@return Length	  */
	public String getLength()
	{
		return (String)get_Value(COLUMNNAME_Length);
	}

	/** Set &#21402;&#24230;.
		@param Thickness &#21402;&#24230;
	*/
	public void setThickness (String Thickness)
	{
		set_ValueNoCheck (COLUMNNAME_Thickness, Thickness);
	}

	/** Get &#21402;&#24230;.
		@return &#21402;&#24230;	  */
	public String getThickness()
	{
		return (String)get_Value(COLUMNNAME_Thickness);
	}

	/** Set Width.
		@param Width Width
	*/
	public void setWidth (String Width)
	{
		set_Value (COLUMNNAME_Width, Width);
	}

	/** Get Width.
		@return Width	  */
	public String getWidth()
	{
		return (String)get_Value(COLUMNNAME_Width);
	}

	/** Set isjumpdegree.
		@param isjumpdegree isjumpdegree
	*/
	public void setisjumpdegree (boolean isjumpdegree)
	{
		set_Value (COLUMNNAME_isjumpdegree, Boolean.valueOf(isjumpdegree));
	}

	/** Get isjumpdegree.
		@return isjumpdegree	  */
	public boolean isjumpdegree()
	{
		Object oo = get_Value(COLUMNNAME_isjumpdegree);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set isnailmouth.
		@param isnailmouth isnailmouth
	*/
	public void setisnailmouth (boolean isnailmouth)
	{
		set_Value (COLUMNNAME_isnailmouth, Boolean.valueOf(isnailmouth));
	}

	/** Get isnailmouth.
		@return isnailmouth	  */
	public boolean isnailmouth()
	{
		Object oo = get_Value(COLUMNNAME_isnailmouth);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set openingmethod.
		@param openingmethod openingmethod
	*/
	public void setopeningmethod (String openingmethod)
	{
		set_Value (COLUMNNAME_openingmethod, openingmethod);
	}

	/** Get openingmethod.
		@return openingmethod	  */
	public String getopeningmethod()
	{
		return (String)get_Value(COLUMNNAME_openingmethod);
	}

	/**
	 * Set Value.
	 * 
	 * @param Value Search key
	 */
	public void setValue(String Value) {
		set_Value(COLUMNNAME_Value, Value);
	}

	/**
	 * Get Value.
	 * 
	 * @return Search key
	 */
	public String getValue() {
		return (String) get_Value(COLUMNNAME_Value);
	}

	/**
	 * Set Name.
	 * 
	 * @param Name Alphanumeric identifier of the entity
	 */
	public void setName(String Name) {
		set_Value(COLUMNNAME_Name, Name);
	}

	/**
	 * Get Name.
	 * 
	 * @return Alphanumeric identifier of the entity
	 */
	public String getName() {
		return (String) get_Value(COLUMNNAME_Name);
	}
}