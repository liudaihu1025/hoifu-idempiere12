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
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/** Generated Model for HF_PP_OrderNode_Tracking
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="HF_PP_OrderNode_Tracking")
public class X_HF_PP_OrderNode_Tracking extends PO implements I_HF_PP_OrderNode_Tracking, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260819L;

    /** Standard Constructor */
    public X_HF_PP_OrderNode_Tracking (Properties ctx, int HF_PP_OrderNode_Tracking_ID, String trxName)
    {
      super (ctx, HF_PP_OrderNode_Tracking_ID, trxName);
      /** if (HF_PP_OrderNode_Tracking_ID == 0)
        {
			setHF_PP_OrderNode_Tracking_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setoperationclass_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_PP_OrderNode_Tracking (Properties ctx, int HF_PP_OrderNode_Tracking_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, HF_PP_OrderNode_Tracking_ID, trxName, virtualColumns);
      /** if (HF_PP_OrderNode_Tracking_ID == 0)
        {
			setHF_PP_OrderNode_Tracking_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setoperationclass_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_PP_OrderNode_Tracking (Properties ctx, String HF_PP_OrderNode_Tracking_UU, String trxName)
    {
      super (ctx, HF_PP_OrderNode_Tracking_UU, trxName);
      /** if (HF_PP_OrderNode_Tracking_UU == null)
        {
			setHF_PP_OrderNode_Tracking_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setoperationclass_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_HF_PP_OrderNode_Tracking (Properties ctx, String HF_PP_OrderNode_Tracking_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, HF_PP_OrderNode_Tracking_UU, trxName, virtualColumns);
      /** if (HF_PP_OrderNode_Tracking_UU == null)
        {
			setHF_PP_OrderNode_Tracking_ID (0);
			setPP_Order_ID (0);
			setPP_Order_Node_ID (0);
			setoperationclass_ID (0);
        } */
    }

    /** Load Constructor */
    public X_HF_PP_OrderNode_Tracking (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 4 - System
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
      StringBuilder sb = new StringBuilder ("X_HF_PP_OrderNode_Tracking[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set HF_PP_OrderNode_Tracking.
		@param HF_PP_OrderNode_Tracking_ID HF_PP_OrderNode_Tracking
	*/
	public void setHF_PP_OrderNode_Tracking_ID (int HF_PP_OrderNode_Tracking_ID)
	{
		if (HF_PP_OrderNode_Tracking_ID < 1)
			set_ValueNoCheck (COLUMNNAME_HF_PP_OrderNode_Tracking_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_HF_PP_OrderNode_Tracking_ID, Integer.valueOf(HF_PP_OrderNode_Tracking_ID));
	}

	/** Get HF_PP_OrderNode_Tracking.
		@return HF_PP_OrderNode_Tracking	  */
	public int getHF_PP_OrderNode_Tracking_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HF_PP_OrderNode_Tracking_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set HF_PP_OrderNode_Tracking_UU.
		@param HF_PP_OrderNode_Tracking_UU HF_PP_OrderNode_Tracking_UU
	*/
	public void setHF_PP_OrderNode_Tracking_UU (String HF_PP_OrderNode_Tracking_UU)
	{
		set_ValueNoCheck (COLUMNNAME_HF_PP_OrderNode_Tracking_UU, HF_PP_OrderNode_Tracking_UU);
	}

	/** Get HF_PP_OrderNode_Tracking_UU.
		@return HF_PP_OrderNode_Tracking_UU	  */
	public String getHF_PP_OrderNode_Tracking_UU()
	{
		return (String)get_Value(COLUMNNAME_HF_PP_OrderNode_Tracking_UU);
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

	/** Set colorsequence.
		@param colorsequence colorsequence
	*/
	public void setcolorsequence (String colorsequence)
	{
		set_Value (COLUMNNAME_colorsequence, colorsequence);
	}

	/** Get colorsequence.
		@return colorsequence	  */
	public String getcolorsequence()
	{
		return (String)get_Value(COLUMNNAME_colorsequence);
	}

	/** Set dieplateno.
		@param dieplateno dieplateno
	*/
	public void setdieplateno (String dieplateno)
	{
		set_Value (COLUMNNAME_dieplateno, dieplateno);
	}

	/** Get dieplateno.
		@return dieplateno	  */
	public String getdieplateno()
	{
		return (String)get_Value(COLUMNNAME_dieplateno);
	}

	/** Set effectname.
		@param effectname effectname
	*/
	public void seteffectname (String effectname)
	{
		set_ValueNoCheck (COLUMNNAME_effectname, effectname);
	}

	/** Get effectname.
		@return effectname	  */
	public String geteffectname()
	{
		return (String)get_Value(COLUMNNAME_effectname);
	}

	/** Set embossplateno.
		@param embossplateno embossplateno
	*/
	public void setembossplateno (String embossplateno)
	{
		set_Value (COLUMNNAME_embossplateno, embossplateno);
	}

	/** Get embossplateno.
		@return embossplateno	  */
	public String getembossplateno()
	{
		return (String)get_Value(COLUMNNAME_embossplateno);
	}

	/** Set embossposition.
		@param embossposition embossposition
	*/
	public void setembossposition (String embossposition)
	{
		set_Value (COLUMNNAME_embossposition, embossposition);
	}

	/** Get embossposition.
		@return embossposition	  */
	public String getembossposition()
	{
		return (String)get_Value(COLUMNNAME_embossposition);
	}

	/** Set foilalumodel.
		@param foilalumodel foilalumodel
	*/
	public void setfoilalumodel (String foilalumodel)
	{
		set_Value (COLUMNNAME_foilalumodel, foilalumodel);
	}

	/** Get foilalumodel.
		@return foilalumodel	  */
	public String getfoilalumodel()
	{
		return (String)get_Value(COLUMNNAME_foilalumodel);
	}

	/** Set foilplateno.
		@param foilplateno foilplateno
	*/
	public void setfoilplateno (String foilplateno)
	{
		set_Value (COLUMNNAME_foilplateno, foilplateno);
	}

	/** Get foilplateno.
		@return foilplateno	  */
	public String getfoilplateno()
	{
		return (String)get_Value(COLUMNNAME_foilplateno);
	}

	/** Set foiltemperature.
		@param foiltemperature foiltemperature
	*/
	public void setfoiltemperature (String foiltemperature)
	{
		set_Value (COLUMNNAME_foiltemperature, foiltemperature);
	}

	/** Get foiltemperature.
		@return foiltemperature	  */
	public String getfoiltemperature()
	{
		return (String)get_Value(COLUMNNAME_foiltemperature);
	}

	/** Set gravurelinecount.
		@param gravurelinecount gravurelinecount
	*/
	public void setgravurelinecount (String gravurelinecount)
	{
		set_Value (COLUMNNAME_gravurelinecount, gravurelinecount);
	}

	/** Get gravurelinecount.
		@return gravurelinecount	  */
	public String getgravurelinecount()
	{
		return (String)get_Value(COLUMNNAME_gravurelinecount);
	}

	/** Set inkformula.
		@param inkformula inkformula
	*/
	public void setinkformula (String inkformula)
	{
		set_Value (COLUMNNAME_inkformula, inkformula);
	}

	/** Get inkformula.
		@return inkformula	  */
	public String getinkformula()
	{
		return (String)get_Value(COLUMNNAME_inkformula);
	}

	/** Set inknamemodel.
		@param inknamemodel inknamemodel
	*/
	public void setinknamemodel (String inknamemodel)
	{
		set_Value (COLUMNNAME_inknamemodel, inknamemodel);
	}

	/** Get inknamemodel.
		@return inknamemodel	  */
	public String getinknamemodel()
	{
		return (String)get_Value(COLUMNNAME_inknamemodel);
	}

	/** Set inkviscosity.
		@param inkviscosity inkviscosity
	*/
	public void setinkviscosity (String inkviscosity)
	{
		set_Value (COLUMNNAME_inkviscosity, inkviscosity);
	}

	/** Get inkviscosity.
		@return inkviscosity	  */
	public String getinkviscosity()
	{
		return (String)get_Value(COLUMNNAME_inkviscosity);
	}

	/** Set lightgroup.
		@param lightgroup lightgroup
	*/
	public void setlightgroup (String lightgroup)
	{
		set_Value (COLUMNNAME_lightgroup, lightgroup);
	}

	/** Get lightgroup.
		@return lightgroup	  */
	public String getlightgroup()
	{
		return (String)get_Value(COLUMNNAME_lightgroup);
	}

	/** Set oilplatematerial.
		@param oilplatematerial oilplatematerial
	*/
	public void setoilplatematerial (String oilplatematerial)
	{
		set_Value (COLUMNNAME_oilplatematerial, oilplatematerial);
	}

	/** Get oilplatematerial.
		@return oilplatematerial	  */
	public String getoilplatematerial()
	{
		return (String)get_Value(COLUMNNAME_oilplatematerial);
	}


	/** Set &#24037;&#24207;&#32452;.
		@param operationclass_ID &#24037;&#24207;&#32452;
	*/
	public void setoperationclass_ID (int operationclass_ID)
	{
		if (operationclass_ID < 1)
			set_Value (COLUMNNAME_operationclass_ID, null);
		else
			set_Value (COLUMNNAME_operationclass_ID, Integer.valueOf(operationclass_ID));
	}

	/** Get &#24037;&#24207;&#32452;.
		@return &#24037;&#24207;&#32452;	  */
	public int getoperationclass_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_operationclass_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set otherparam.
		@param otherparam otherparam
	*/
	public void setotherparam (String otherparam)
	{
		set_Value (COLUMNNAME_otherparam, otherparam);
	}

	/** Get otherparam.
		@return otherparam	  */
	public String getotherparam()
	{
		return (String)get_Value(COLUMNNAME_otherparam);
	}

	/** Set oventemperature.
		@param oventemperature oventemperature
	*/
	public void setoventemperature (String oventemperature)
	{
		set_Value (COLUMNNAME_oventemperature, oventemperature);
	}

	/** Get oventemperature.
		@return oventemperature	  */
	public String getoventemperature()
	{
		return (String)get_Value(COLUMNNAME_oventemperature);
	}

	/** Set qrcodesize.
		@param qrcodesize qrcodesize
	*/
	public void setqrcodesize (String qrcodesize)
	{
		set_Value (COLUMNNAME_qrcodesize, qrcodesize);
	}

	/** Get qrcodesize.
		@return qrcodesize	  */
	public String getqrcodesize()
	{
		return (String)get_Value(COLUMNNAME_qrcodesize);
	}

	/** Set screenlinecount.
		@param screenlinecount screenlinecount
	*/
	public void setscreenlinecount (String screenlinecount)
	{
		set_Value (COLUMNNAME_screenlinecount, screenlinecount);
	}

	/** Get screenlinecount.
		@return screenlinecount	  */
	public String getscreenlinecount()
	{
		return (String)get_Value(COLUMNNAME_screenlinecount);
	}

	/** Set screenmesh.
		@param screenmesh screenmesh
	*/
	public void setscreenmesh (String screenmesh)
	{
		set_Value (COLUMNNAME_screenmesh, screenmesh);
	}

	/** Get screenmesh.
		@return screenmesh	  */
	public String getscreenmesh()
	{
		return (String)get_Value(COLUMNNAME_screenmesh);
	}

	/** Set verifycodefont.
		@param verifycodefont verifycodefont
	*/
	public void setverifycodefont (String verifycodefont)
	{
		set_Value (COLUMNNAME_verifycodefont, verifycodefont);
	}

	/** Get verifycodefont.
		@return verifycodefont	  */
	public String getverifycodefont()
	{
		return (String)get_Value(COLUMNNAME_verifycodefont);
	}
}