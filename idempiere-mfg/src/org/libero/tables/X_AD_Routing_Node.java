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

/** Generated Model for AD_Routing_Node
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="AD_Routing_Node")
public class X_AD_Routing_Node extends PO implements I_AD_Routing_Node, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260320L;

    /** Standard Constructor */
    public X_AD_Routing_Node (Properties ctx, int AD_Routing_Node_ID, String trxName)
    {
      super (ctx, AD_Routing_Node_ID, trxName);
      /** if (AD_Routing_Node_ID == 0)
        {
			setAD_Routing_Node_ID (0);
			setIsBatchCalculation (false);
// N
			setName (null);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_Routing_Node (Properties ctx, int AD_Routing_Node_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_Routing_Node_ID, trxName, virtualColumns);
      /** if (AD_Routing_Node_ID == 0)
        {
			setAD_Routing_Node_ID (0);
			setIsBatchCalculation (false);
// N
			setName (null);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_Routing_Node (Properties ctx, String AD_Routing_Node_UU, String trxName)
    {
      super (ctx, AD_Routing_Node_UU, trxName);
      /** if (AD_Routing_Node_UU == null)
        {
			setAD_Routing_Node_ID (0);
			setIsBatchCalculation (false);
// N
			setName (null);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_Routing_Node (Properties ctx, String AD_Routing_Node_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_Routing_Node_UU, trxName, virtualColumns);
      /** if (AD_Routing_Node_UU == null)
        {
			setAD_Routing_Node_ID (0);
			setIsBatchCalculation (false);
// N
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_AD_Routing_Node (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client
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
      StringBuilder sb = new StringBuilder ("X_AD_Routing_Node[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set &#24037;&#24207;.
		@param AD_Routing_Node_ID &#24037;&#24207;
	*/
	public void setAD_Routing_Node_ID (int AD_Routing_Node_ID)
	{
		if (AD_Routing_Node_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_Routing_Node_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Routing_Node_ID, Integer.valueOf(AD_Routing_Node_ID));
	}

	/** Get &#24037;&#24207;.
		@return &#24037;&#24207;	  */
	public int getAD_Routing_Node_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Routing_Node_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AD_Routing_Node_UU.
		@param AD_Routing_Node_UU AD_Routing_Node_UU
	*/
	public void setAD_Routing_Node_UU (String AD_Routing_Node_UU)
	{
		set_ValueNoCheck (COLUMNNAME_AD_Routing_Node_UU, AD_Routing_Node_UU);
	}

	/** Get AD_Routing_Node_UU.
		@return AD_Routing_Node_UU	  */
	public String getAD_Routing_Node_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_Routing_Node_UU);
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

	/** Set IsBatchCalculation.
		@param IsBatchCalculation IsBatchCalculation
	*/
	public void setIsBatchCalculation (boolean IsBatchCalculation)
	{
		set_Value (COLUMNNAME_IsBatchCalculation, Boolean.valueOf(IsBatchCalculation));
	}

	/** Get IsBatchCalculation.
		@return IsBatchCalculation	  */
	public boolean isBatchCalculation()
	{
		Object oo = get_Value(COLUMNNAME_IsBatchCalculation);
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

	/** &#21360;&#21047;&#31867; = A */
	public static final String NODETYPE_印刷类 = "A";
	/** &#34920;&#38754;&#22788;&#29702;&#31867; = B */
	public static final String NODETYPE_表面处理类 = "B";
	/** &#25104;&#22411;&#31867; = C */
	public static final String NODETYPE_成型类 = "C";
	/** &#35013;&#37197;&#31867; = D */
	public static final String NODETYPE_装配类 = "D";
	/** Set NodeType.
		@param NodeType NodeType
	*/
	public void setNodeType (String NodeType)
	{

		set_Value (COLUMNNAME_NodeType, NodeType);
	}

	/** Get NodeType.
		@return NodeType	  */
	public String getNodeType()
	{
		return (String)get_Value(COLUMNNAME_NodeType);
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
}