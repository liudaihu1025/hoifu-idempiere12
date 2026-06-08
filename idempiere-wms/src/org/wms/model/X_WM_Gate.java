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
package org.wms.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for WM_Gate
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_Gate extends PO implements I_WM_Gate, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170422L;

    /** Standard Constructor */
    public X_WM_Gate (Properties ctx, int WM_Gate_ID, String trxName)
    {
      super (ctx, WM_Gate_ID, trxName);
      /** if (WM_Gate_ID == 0)
        {
			setWM_Gate_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_Gate (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_Gate[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set IsCrossDocking.
		@param IsCrossDocking IsCrossDocking	  */
	public void setIsCrossDocking (boolean IsCrossDocking)
	{
		set_Value (COLUMNNAME_IsCrossDocking, Boolean.valueOf(IsCrossDocking));
	}

	/** Get IsCrossDocking.
		@return IsCrossDocking	  */
	public boolean isCrossDocking () 
	{
		Object oo = get_Value(COLUMNNAME_IsCrossDocking);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsFlowThrough.
		@param IsFlowThrough IsFlowThrough	  */
	public void setIsFlowThrough (boolean IsFlowThrough)
	{
		set_Value (COLUMNNAME_IsFlowThrough, Boolean.valueOf(IsFlowThrough));
	}

	/** Get IsFlowThrough.
		@return IsFlowThrough	  */
	public boolean isFlowThrough () 
	{
		Object oo = get_Value(COLUMNNAME_IsFlowThrough);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsGoodsIssue.
		@param IsGoodsIssue IsGoodsIssue	  */
	public void setIsGoodsIssue (boolean IsGoodsIssue)
	{
		set_Value (COLUMNNAME_IsGoodsIssue, Boolean.valueOf(IsGoodsIssue));
	}

	/** Get IsGoodsIssue.
		@return IsGoodsIssue	  */
	public boolean isGoodsIssue () 
	{
		Object oo = get_Value(COLUMNNAME_IsGoodsIssue);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set IsGoodsReceipts.
		@param IsGoodsReceipts IsGoodsReceipts	  */
	public void setIsGoodsReceipts (boolean IsGoodsReceipts)
	{
		set_Value (COLUMNNAME_IsGoodsReceipts, Boolean.valueOf(IsGoodsReceipts));
	}

	/** Get IsGoodsReceipts.
		@return IsGoodsReceipts	  */
	public boolean isGoodsReceipts () 
	{
		Object oo = get_Value(COLUMNNAME_IsGoodsReceipts);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
    {
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_Name)
			.getPO(getM_Warehouse_ID(), get_TrxName());	}

	/** Set Warehouse.
		@param M_Warehouse_ID 
		Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set StagingArea.
		@param StagingArea StagingArea	  */
	public void setStagingArea (String StagingArea)
	{
		set_Value (COLUMNNAME_StagingArea, StagingArea);
	}

	/** Get StagingArea.
		@return StagingArea	  */
	public String getStagingArea () 
	{
		return (String)get_Value(COLUMNNAME_StagingArea);
	}

	/** Set Gate.
		@param WM_Gate_ID Gate	  */
	public void setWM_Gate_ID (int WM_Gate_ID)
	{
		if (WM_Gate_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_Gate_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_Gate_ID, Integer.valueOf(WM_Gate_ID));
	}

	/** Get Gate.
		@return Gate	  */
	public int getWM_Gate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_Gate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}