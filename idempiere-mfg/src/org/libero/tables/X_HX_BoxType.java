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
import org.compiere.model.*;

/** Generated Model for HX_BoxType
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="HX_BoxType")
public class X_HX_BoxType extends PO implements I_HX_BoxType, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260530L;

    /** Standard Constructor */
    public X_HX_BoxType (Properties ctx, int HX_BoxType_ID, String trxName)
    {
      super (ctx, HX_BoxType_ID, trxName);
      /** if (HX_BoxType_ID == 0)
        {
			setHX_BoxType_ID (0);
			setIsValid (false);
// N
			setName (null);
			setValue (null);
			setisnailmouth (false);
// N
			setispluginterface (false);
// N
			setistwobargebox (false);
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
			setName (null);
			setValue (null);
			setisnailmouth (false);
// N
			setispluginterface (false);
// N
			setistwobargebox (false);
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
			setName (null);
			setValue (null);
			setisnailmouth (false);
// N
			setispluginterface (false);
// N
			setistwobargebox (false);
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
			setName (null);
			setValue (null);
			setisnailmouth (false);
// N
			setispluginterface (false);
// N
			setistwobargebox (false);
// N
        } */
    }

    /** Load Constructor */
    public X_HX_BoxType (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_HX_BoxType[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Workflow getAD_Workflow() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Workflow)MTable.get(getCtx(), org.compiere.model.I_AD_Workflow.Table_ID)
			.getPO(getAD_Workflow_ID(), get_TrxName());
	}

	/** Set Workflow.
		@param AD_Workflow_ID Workflow or combination of tasks
	*/
	public void setAD_Workflow_ID (int AD_Workflow_ID)
	{
		if (AD_Workflow_ID < 1)
			set_Value (COLUMNNAME_AD_Workflow_ID, null);
		else
			set_Value (COLUMNNAME_AD_Workflow_ID, Integer.valueOf(AD_Workflow_ID));
	}

	/** Get Workflow.
		@return Workflow or combination of tasks
	  */
	public int getAD_Workflow_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Workflow_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException
	{
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_ID)
			.getPO(getC_UOM_ID(), get_TrxName());
	}

	/** Set UOM.
		@param C_UOM_ID Unit of Measure
	*/
	public void setC_UOM_ID (int C_UOM_ID)
	{
		if (C_UOM_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_UOM_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_UOM_ID, Integer.valueOf(C_UOM_ID));
	}

	/** Get UOM.
		@return Unit of Measure
	  */
	public int getC_UOM_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_ID);
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
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	/** Set &#35745;&#37325;&#20844;&#24335;.
		@param WeightFormula &#35745;&#37325;&#20844;&#24335;
	*/
	public void setWeightFormula (String WeightFormula)
	{
		set_Value (COLUMNNAME_WeightFormula, WeightFormula);
	}

	/** Get &#35745;&#37325;&#20844;&#24335;.
		@return &#35745;&#37325;&#20844;&#24335;	  */
	public String getWeightFormula()
	{
		return (String)get_Value(COLUMNNAME_WeightFormula);
	}

	/** Set ad_workflow_name.
		@param ad_workflow_name ad_workflow_name
	*/
	public void setad_workflow_name (String ad_workflow_name)
	{
		set_Value (COLUMNNAME_ad_workflow_name, ad_workflow_name);
	}

	/** Get ad_workflow_name.
		@return ad_workflow_name	  */
	public String getad_workflow_name()
	{
		return (String)get_Value(COLUMNNAME_ad_workflow_name);
	}

	/** Set ad_workflow_value.
		@param ad_workflow_value ad_workflow_value
	*/
	public void setad_workflow_value (String ad_workflow_value)
	{
		set_Value (COLUMNNAME_ad_workflow_value, ad_workflow_value);
	}

	/** Get ad_workflow_value.
		@return ad_workflow_value	  */
	public String getad_workflow_value()
	{
		return (String)get_Value(COLUMNNAME_ad_workflow_value);
	}

	/** Set areaformula.
		@param areaformula areaformula
	*/
	public void setareaformula (String areaformula)
	{
		set_Value (COLUMNNAME_areaformula, areaformula);
	}

	/** Get areaformula.
		@return areaformula	  */
	public String getareaformula()
	{
		return (String)get_Value(COLUMNNAME_areaformula);
	}

	/** Set horizontalexpandlength.
		@param horizontalexpandlength horizontalexpandlength
	*/
	public void sethorizontalexpandlength (String horizontalexpandlength)
	{
		set_Value (COLUMNNAME_horizontalexpandlength, horizontalexpandlength);
	}

	/** Get horizontalexpandlength.
		@return horizontalexpandlength	  */
	public String gethorizontalexpandlength()
	{
		return (String)get_Value(COLUMNNAME_horizontalexpandlength);
	}

	/** Set horizontalexpandwidth.
		@param horizontalexpandwidth horizontalexpandwidth
	*/
	public void sethorizontalexpandwidth (String horizontalexpandwidth)
	{
		set_Value (COLUMNNAME_horizontalexpandwidth, horizontalexpandwidth);
	}

	/** Get horizontalexpandwidth.
		@return horizontalexpandwidth	  */
	public String gethorizontalexpandwidth()
	{
		return (String)get_Value(COLUMNNAME_horizontalexpandwidth);
	}

	/** Set &#38025;&#21475;.
		@param isnailmouth &#38025;&#21475;
	*/
	public void setisnailmouth (boolean isnailmouth)
	{
		set_Value (COLUMNNAME_isnailmouth, Boolean.valueOf(isnailmouth));
	}

	/** Get &#38025;&#21475;.
		@return &#38025;&#21475;	  */
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

	/** Set ispluginterface.
		@param ispluginterface ispluginterface
	*/
	public void setispluginterface (boolean ispluginterface)
	{
		set_Value (COLUMNNAME_ispluginterface, Boolean.valueOf(ispluginterface));
	}

	/** Get ispluginterface.
		@return ispluginterface	  */
	public boolean ispluginterface()
	{
		Object oo = get_Value(COLUMNNAME_ispluginterface);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set istwobargebox.
		@param istwobargebox istwobargebox
	*/
	public void setistwobargebox (boolean istwobargebox)
	{
		set_Value (COLUMNNAME_istwobargebox, Boolean.valueOf(istwobargebox));
	}

	/** Get istwobargebox.
		@return istwobargebox	  */
	public boolean istwobargebox()
	{
		Object oo = get_Value(COLUMNNAME_istwobargebox);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set paperlengthcrease1.
		@param paperlengthcrease1 paperlengthcrease1
	*/
	public void setpaperlengthcrease1 (String paperlengthcrease1)
	{
		set_Value (COLUMNNAME_paperlengthcrease1, paperlengthcrease1);
	}

	/** Get paperlengthcrease1.
		@return paperlengthcrease1	  */
	public String getpaperlengthcrease1()
	{
		return (String)get_Value(COLUMNNAME_paperlengthcrease1);
	}

	/** Set paperlengthcrease2.
		@param paperlengthcrease2 paperlengthcrease2
	*/
	public void setpaperlengthcrease2 (String paperlengthcrease2)
	{
		set_Value (COLUMNNAME_paperlengthcrease2, paperlengthcrease2);
	}

	/** Get paperlengthcrease2.
		@return paperlengthcrease2	  */
	public String getpaperlengthcrease2()
	{
		return (String)get_Value(COLUMNNAME_paperlengthcrease2);
	}

	/** Set paperlengthcrease3.
		@param paperlengthcrease3 paperlengthcrease3
	*/
	public void setpaperlengthcrease3 (String paperlengthcrease3)
	{
		set_Value (COLUMNNAME_paperlengthcrease3, paperlengthcrease3);
	}

	/** Get paperlengthcrease3.
		@return paperlengthcrease3	  */
	public String getpaperlengthcrease3()
	{
		return (String)get_Value(COLUMNNAME_paperlengthcrease3);
	}

	/** Set paperlengthcrease4.
		@param paperlengthcrease4 paperlengthcrease4
	*/
	public void setpaperlengthcrease4 (String paperlengthcrease4)
	{
		set_Value (COLUMNNAME_paperlengthcrease4, paperlengthcrease4);
	}

	/** Get paperlengthcrease4.
		@return paperlengthcrease4	  */
	public String getpaperlengthcrease4()
	{
		return (String)get_Value(COLUMNNAME_paperlengthcrease4);
	}

	/** Set paperlengthcrease5.
		@param paperlengthcrease5 paperlengthcrease5
	*/
	public void setpaperlengthcrease5 (String paperlengthcrease5)
	{
		set_Value (COLUMNNAME_paperlengthcrease5, paperlengthcrease5);
	}

	/** Get paperlengthcrease5.
		@return paperlengthcrease5	  */
	public String getpaperlengthcrease5()
	{
		return (String)get_Value(COLUMNNAME_paperlengthcrease5);
	}

	/** Set paperwidthcrease1.
		@param paperwidthcrease1 paperwidthcrease1
	*/
	public void setpaperwidthcrease1 (String paperwidthcrease1)
	{
		set_Value (COLUMNNAME_paperwidthcrease1, paperwidthcrease1);
	}

	/** Get paperwidthcrease1.
		@return paperwidthcrease1	  */
	public String getpaperwidthcrease1()
	{
		return (String)get_Value(COLUMNNAME_paperwidthcrease1);
	}

	/** Set paperwidthcrease2.
		@param paperwidthcrease2 paperwidthcrease2
	*/
	public void setpaperwidthcrease2 (String paperwidthcrease2)
	{
		set_Value (COLUMNNAME_paperwidthcrease2, paperwidthcrease2);
	}

	/** Get paperwidthcrease2.
		@return paperwidthcrease2	  */
	public String getpaperwidthcrease2()
	{
		return (String)get_Value(COLUMNNAME_paperwidthcrease2);
	}

	/** Set paperwidthcrease3.
		@param paperwidthcrease3 paperwidthcrease3
	*/
	public void setpaperwidthcrease3 (String paperwidthcrease3)
	{
		set_Value (COLUMNNAME_paperwidthcrease3, paperwidthcrease3);
	}

	/** Get paperwidthcrease3.
		@return paperwidthcrease3	  */
	public String getpaperwidthcrease3()
	{
		return (String)get_Value(COLUMNNAME_paperwidthcrease3);
	}

	/** Set paperwidthcrease4.
		@param paperwidthcrease4 paperwidthcrease4
	*/
	public void setpaperwidthcrease4 (String paperwidthcrease4)
	{
		set_Value (COLUMNNAME_paperwidthcrease4, paperwidthcrease4);
	}

	/** Get paperwidthcrease4.
		@return paperwidthcrease4	  */
	public String getpaperwidthcrease4()
	{
		return (String)get_Value(COLUMNNAME_paperwidthcrease4);
	}

	/** Set paperwidthcrease5.
		@param paperwidthcrease5 paperwidthcrease5
	*/
	public void setpaperwidthcrease5 (String paperwidthcrease5)
	{
		set_Value (COLUMNNAME_paperwidthcrease5, paperwidthcrease5);
	}

	/** Get paperwidthcrease5.
		@return paperwidthcrease5	  */
	public String getpaperwidthcrease5()
	{
		return (String)get_Value(COLUMNNAME_paperwidthcrease5);
	}

	/** Set verticalexpandlength.
		@param verticalexpandlength verticalexpandlength
	*/
	public void setverticalexpandlength (String verticalexpandlength)
	{
		set_Value (COLUMNNAME_verticalexpandlength, verticalexpandlength);
	}

	/** Get verticalexpandlength.
		@return verticalexpandlength	  */
	public String getverticalexpandlength()
	{
		return (String)get_Value(COLUMNNAME_verticalexpandlength);
	}

	/** Set verticalexpandwidth.
		@param verticalexpandwidth verticalexpandwidth
	*/
	public void setverticalexpandwidth (String verticalexpandwidth)
	{
		set_Value (COLUMNNAME_verticalexpandwidth, verticalexpandwidth);
	}

	/** Get verticalexpandwidth.
		@return verticalexpandwidth	  */
	public String getverticalexpandwidth()
	{
		return (String)get_Value(COLUMNNAME_verticalexpandwidth);
	}

	/** Set volumeformula.
		@param volumeformula volumeformula
	*/
	public void setvolumeformula (String volumeformula)
	{
		set_Value (COLUMNNAME_volumeformula, volumeformula);
	}

	/** Get volumeformula.
		@return volumeformula	  */
	public String getvolumeformula()
	{
		return (String)get_Value(COLUMNNAME_volumeformula);
	}
}