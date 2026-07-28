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
import org.compiere.model.*;

/** Generated Model for dy_processdesignitem
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_processdesignitem")
public class X_dy_processdesignitem extends PO implements I_dy_processdesignitem, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260716L;

    /** Standard Constructor */
    public X_dy_processdesignitem (Properties ctx, int dy_processdesignitem_ID, String trxName)
    {
      super (ctx, dy_processdesignitem_ID, trxName);
      /** if (dy_processdesignitem_ID == 0)
        {
			setdy_processdesign_ID (0);
			setdy_processdesignitem_ID (0);
			setexplanation (null);
			settaskitemname (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_processdesignitem (Properties ctx, int dy_processdesignitem_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_processdesignitem_ID, trxName, virtualColumns);
      /** if (dy_processdesignitem_ID == 0)
        {
			setdy_processdesign_ID (0);
			setdy_processdesignitem_ID (0);
			setexplanation (null);
			settaskitemname (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_processdesignitem (Properties ctx, String dy_processdesignitem_UU, String trxName)
    {
      super (ctx, dy_processdesignitem_UU, trxName);
      /** if (dy_processdesignitem_UU == null)
        {
			setdy_processdesign_ID (0);
			setdy_processdesignitem_ID (0);
			setexplanation (null);
			settaskitemname (null);
        } */
    }

    /** Standard Constructor */
    public X_dy_processdesignitem (Properties ctx, String dy_processdesignitem_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_processdesignitem_UU, trxName, virtualColumns);
      /** if (dy_processdesignitem_UU == null)
        {
			setdy_processdesign_ID (0);
			setdy_processdesignitem_ID (0);
			setexplanation (null);
			settaskitemname (null);
        } */
    }

    /** Load Constructor */
    public X_dy_processdesignitem (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_processdesignitem[")
        .append(get_ID()).append("]");
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

	public I_dy_processdesign getdy_processdesign() throws RuntimeException
	{
		return (I_dy_processdesign)MTable.get(getCtx(), I_dy_processdesign.Table_ID)
			.getPO(getdy_processdesign_ID(), get_TrxName());
	}

	/** Set dy_processdesign.
		@param dy_processdesign_ID dy_processdesign
	*/
	public void setdy_processdesign_ID (int dy_processdesign_ID)
	{
		if (dy_processdesign_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_processdesign_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_processdesign_ID, Integer.valueOf(dy_processdesign_ID));
	}

	/** Get dy_processdesign.
		@return dy_processdesign	  */
	public int getdy_processdesign_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_processdesign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_processdesignitem.
		@param dy_processdesignitem_ID dy_processdesignitem
	*/
	public void setdy_processdesignitem_ID (int dy_processdesignitem_ID)
	{
		if (dy_processdesignitem_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_processdesignitem_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_processdesignitem_ID, Integer.valueOf(dy_processdesignitem_ID));
	}

	/** Get dy_processdesignitem.
		@return dy_processdesignitem	  */
	public int getdy_processdesignitem_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_processdesignitem_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_processdesignitem_UU.
		@param dy_processdesignitem_UU dy_processdesignitem_UU
	*/
	public void setdy_processdesignitem_UU (String dy_processdesignitem_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_processdesignitem_UU, dy_processdesignitem_UU);
	}

	/** Get dy_processdesignitem_UU.
		@return dy_processdesignitem_UU	  */
	public String getdy_processdesignitem_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_processdesignitem_UU);
	}

	/** Set explanation.
		@param explanation explanation
	*/
	public void setexplanation (String explanation)
	{
		set_Value (COLUMNNAME_explanation, explanation);
	}

	/** Get explanation.
		@return explanation	  */
	public String getexplanation()
	{
		return (String)get_Value(COLUMNNAME_explanation);
	}

	/** &#24050;&#35780;&#23457; = RE */
	public static final String ITEMSTATUS_已评审 = "RE";
	/** &#35780;&#23457;&#20013; = RV */
	public static final String ITEMSTATUS_评审中 = "RV";
	/** &#24453;&#35780;&#23457; = WR */
	public static final String ITEMSTATUS_待评审 = "WR";
	/** Set itemstatus.
		@param itemstatus itemstatus
	*/
	public void setitemstatus (String itemstatus)
	{

		set_Value (COLUMNNAME_itemstatus, itemstatus);
	}

	/** Get itemstatus.
		@return itemstatus	  */
	public String getitemstatus()
	{
		return (String)get_Value(COLUMNNAME_itemstatus);
	}

	/** Set taskitemname.
		@param taskitemname taskitemname
	*/
	public void settaskitemname (String taskitemname)
	{
		set_Value (COLUMNNAME_taskitemname, taskitemname);
	}

	/** Get taskitemname.
		@return taskitemname	  */
	public String gettaskitemname()
	{
		return (String)get_Value(COLUMNNAME_taskitemname);
	}
}